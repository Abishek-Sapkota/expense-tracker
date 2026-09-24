package com.abi.expensetracker.ui

import com.abi.expensetracker.data.model.RawMessage
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.SenderNormalizer
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Rule
import com.abi.expensetracker.di.ServiceLocator
import com.abi.expensetracker.parser.DateParser
import com.abi.expensetracker.parser.TimeParser
import com.abi.expensetracker.parser.TemplateCompiler
import com.abi.expensetracker.parser.namedOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the template editor shows about the template as it is typed. */
sealed interface TemplatePreview {
    data object Empty : TemplatePreview
    data class Invalid(val message: String) : TemplatePreview
    /** Compiles, but no sample message has been pasted to try it against yet. */
    data class Compiles(val placeholders: List<String>) : TemplatePreview
    data class Matched(val fields: List<Pair<String, String>>) : TemplatePreview
    data class NoMatch(val reason: String) : TemplatePreview
}

class TemplatesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository(app)

    val userRules: StateFlow<List<Rule>> = repository.observeRules()
        .map { rules -> rules.filterNot { it.builtIn } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val builtInRules: StateFlow<List<Rule>> = repository.observeRules()
        .map { rules -> rules.filter { it.builtIn } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Senders seen in stored messages, offered so a template can be scoped to one bank. */
    val senderKeys: StateFlow<List<String>> = repository.observeSenders()
        .map { counts -> counts.map { SenderNormalizer.normalize(it.sender) }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Unparsed money messages from linked senders, offered as template starters. */
    val starters: StateFlow<List<Pair<RawMessage, String?>>> = repository.observeUnparsedFromLinked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _template = MutableStateFlow("")
    val template: StateFlow<String> = _template.asStateFlow()

    private val _sample = MutableStateFlow("")
    val sample: StateFlow<String> = _sample.asStateFlow()

    private val _preview = MutableStateFlow<TemplatePreview>(TemplatePreview.Empty)
    val preview: StateFlow<TemplatePreview> = _preview.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    /**
     * Whether something changed that only a reparse will show.
     *
     * A saved or toggled template changes how stored messages read, but nothing rereads
     * them on its own — so the status card offers the reparse instead of telling the user
     * to go and find it on another screen.
     */
    private val _needsReparse = MutableStateFlow(false)
    val needsReparse: StateFlow<Boolean> = _needsReparse.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun onTemplateChanged(value: String) {
        _template.value = value
        recompute()
    }

    fun onSampleChanged(value: String) {
        _sample.value = value
        recompute()
    }

    /** Seeds the editor from a real message so the user edits rather than types from scratch. */
    fun startFromSample(message: String) {
        _sample.value = message
        _template.value = message
        recompute()
    }

    private fun recompute() {
        val templateText = _template.value
        if (templateText.isBlank()) {
            _preview.value = TemplatePreview.Empty
            return
        }

        when (val outcome = TemplateCompiler.compile(templateText)) {
            is TemplateCompiler.Outcome.Invalid -> {
                _preview.value = TemplatePreview.Invalid(outcome.message)
            }
            is TemplateCompiler.Outcome.Ok -> {
                val sampleText = _sample.value
                if (sampleText.isBlank()) {
                    _preview.value = TemplatePreview.Compiles(outcome.placeholders)
                    return
                }
                val match = Regex(outcome.regex).find(sampleText)
                if (match == null) {
                    _preview.value = TemplatePreview.NoMatch(
                        "This template does not match the sample message."
                    )
                    return
                }
                val fields = buildList {
                    match.namedOrNull("amount")?.let { raw ->
                        val minor = Money.parseToMinor(raw)
                        add("Amount" to (minor?.let { Money.format(it) } ?: raw))
                    }
                    match.namedOrNull("acct")?.let { add("Account" to it) }
                    match.namedOrNull("merchant")?.let { add("Merchant" to it) }
                    match.namedOrNull("remark")?.let { add("Remark" to it) }
                    match.namedOrNull("ref")?.let { add("Reference" to it) }
                    match.namedOrNull("date")?.let { raw ->
                        val parsed = DateParser.parse(raw)
                        // Showing what the date resolved to, not just the text matched:
                        // day-first parsing is the whole thing worth checking here.
                        add("Date" to (parsed?.toString() ?: "$raw (unreadable)"))
                    }
                    match.namedOrNull("time")?.let { raw ->
                        val parsed = TimeParser.parse(raw)
                        add("Time" to (parsed?.toString() ?: "$raw (unreadable)"))
                    }
                    match.namedOrNull("balance")?.let { raw ->
                        val minor = Money.parseToMinor(raw)
                        add("Balance" to (minor?.let { Money.format(it) } ?: raw))
                    }
                }
                _preview.value = TemplatePreview.Matched(fields)
            }
        }
    }

    fun save(name: String, senderKey: String?, direction: Direction) {
        val templateText = _template.value
        viewModelScope.launch {
            val id = repository.addTemplateRule(name, senderKey, templateText, direction)
            if (id == null) {
                _status.value = "Template did not compile; nothing was saved."
            } else {
                _status.value = "Template saved. Reparse to apply it to stored messages."
                _needsReparse.value = true
                _template.value = ""
                _sample.value = ""
                _preview.value = TemplatePreview.Empty
            }
        }
    }

    fun setEnabled(rule: Rule, enabled: Boolean) = viewModelScope.launch {
        repository.setRuleEnabled(rule, enabled)
        _status.value = if (enabled) "Template turned on. Reparse to apply it."
        else "Template turned off. Reparse to drop what it parsed."
        _needsReparse.value = true
    }

    fun delete(rule: Rule) = viewModelScope.launch {
        repository.deleteRule(rule.id)
        _status.value = "Template deleted. Reparse to drop what it parsed."
        _needsReparse.value = true
    }

    /** Rebuilds every parsed transaction with the templates as they now stand. */
    fun reparse() = viewModelScope.launch {
        _busy.value = true
        _status.value = "Reparsing…"
        val count = runCatching { repository.reparseAll() }
        _busy.value = false
        _status.value = count.fold(
            onSuccess = { "Reparsed stored messages into $it transactions." },
            onFailure = { "Reparse failed: ${it.message ?: "unknown error"}" }
        )
        // Cleared either way: a failed reparse is not fixed by pressing it again, and the
        // message says what happened.
        _needsReparse.value = false
    }

    fun clearStatus() { _status.value = null }
}
