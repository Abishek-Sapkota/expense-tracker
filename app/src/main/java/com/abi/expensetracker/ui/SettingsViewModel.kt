package com.abi.expensetracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abi.expensetracker.backup.ImportMode
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.LimitBasis
import com.abi.expensetracker.data.model.LimitStatus
import com.abi.expensetracker.data.model.SpendingLimit
import com.abi.expensetracker.di.ServiceLocator
import com.abi.expensetracker.ui.theme.AccentColor
import com.abi.expensetracker.ui.theme.NeutralPalette
import com.abi.expensetracker.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Permissions, appearance, the spending limit, and the occasional maintenance actions. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository(app)
    private val backup = ServiceLocator.backupManager(app)

    private val settings = SettingsStore(app)

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .map { ThemeMode.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val neutralPalette: StateFlow<NeutralPalette> = settings.neutralPalette
        .map { NeutralPalette.fromName(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NeutralPalette.DEFAULT)

    fun setNeutralPalette(palette: NeutralPalette) =
        viewModelScope.launch { settings.setNeutralPalette(palette.name) }

    fun setThemeMode(mode: ThemeMode) =
        viewModelScope.launch { settings.setThemeMode(mode.name) }

    /** The chosen preset, or null while a custom colour is in use. */
    val accentPreset: StateFlow<AccentColor?> = settings.accentName
        .map { name ->
            if (name == SettingsStore.CUSTOM_ACCENT) null else AccentColor.fromName(name)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccentColor.DEFAULT)

    /** The mixed colour, kept even while a preset is active so Custom can return to it. */
    val customAccentArgb: StateFlow<Int?> = settings.customAccentArgb
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val usingCustomAccent: StateFlow<Boolean> = combine(
        settings.accentName,
        settings.customAccentArgb
    ) { name, argb -> name == SettingsStore.CUSTOM_ACCENT && argb != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setAccent(preset: AccentColor) =
        viewModelScope.launch { settings.setAccentName(preset.name) }

    fun setCustomAccent(argb: Int) =
        viewModelScope.launch { settings.setCustomAccent(argb) }

    /** The saved limit, or null when none is set. */
    val spendingLimit: StateFlow<SpendingLimit?> = settings.spendingLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Save a typed limit. A blank or unparseable amount clears it rather than failing:
     * emptying the field is how a user says they no longer want one.
     */
    fun setSpendingLimit(amountText: String, basis: LimitBasis) = viewModelScope.launch {
        val minor = Money.parseToMinor(amountText)
        if (minor == null || minor <= 0L) settings.clearSpendingLimit()
        else settings.setSpendingLimit(minor, basis)
    }

    fun clearSpendingLimit() = viewModelScope.launch { settings.clearSpendingLimit() }

    /**
     * The limit with its spend so far, so this screen can preview exactly what the ledger
     * will show. Without it the user sets a number and has to go and look.
     */
    val limitStatus: StateFlow<LimitStatus?> =
        combine(settings.spendingLimit, settings.useNepaliCalendar) { limit, nepali ->
            limit to nepali
        }.flatMapLatest { (limit, nepali) ->
            if (limit == null) flowOf(null)
            else repository.observeSpentBetween(limit.range(nepaliMonths = nepali))
                .map { spent -> LimitStatus(limit, spent) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)


    /**
     * Whether the whole app reads dates on the Bikram Sambat calendar.
     *
     * Lives beside the other display settings rather than inside the limit editor: it
     * changes the ledger, the trends and the budget together, which is the only way a
     * "month" means one thing across the app.
     */
    val useNepaliCalendar: StateFlow<Boolean> = settings.useNepaliCalendar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setUseNepaliCalendar(enabled: Boolean) =
        viewModelScope.launch { settings.setUseNepaliCalendar(enabled) }

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(name: String, keywords: String, color: Int?) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addCategory(name, keywords, color) }
    }

    fun updateCategory(category: Category) = viewModelScope.launch {
        repository.updateCategory(category)
    }

    fun deleteCategory(categoryId: Long) = viewModelScope.launch {
        repository.deleteCategory(categoryId)
    }

    /**
     * Runs the keywords over everything still uncategorised.
     *
     * Only the uncategorised, so a category set by hand is never overwritten by a keyword
     * someone added afterwards.
     */
    fun applyKeywords() = viewModelScope.launch {
        _busy.value = true
        val count = repository.categorizeUncategorized()
        _busy.value = false
        _status.value = when (count) {
            0 -> "Nothing left to categorise"
            1 -> "1 transaction categorised"
            else -> "$count transactions categorised"
        }
    }

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun backfill(fromScratch: Boolean) = launchTask("Reading SMS inbox") {
        val result = repository.backfillFromInbox(fromScratch)
        "Read ${result.messagesRead} new messages. ${result.totalStored} stored in total."
    }

    fun reparse() = launchTask("Reparsing") {
        "Reparsed stored messages into ${repository.reparseAll()} transactions."
    }

    fun exportBackup(uri: Uri) = launchTask("Exporting") {
        val r = backup.exportTo(uri)
        "Exported ${r.rawMessages} messages, ${r.transactions} transactions, " +
            "${r.rules} rules, ${r.banks} banks."
    }

    fun importBackup(uri: Uri, mode: ImportMode) = launchTask("Importing") {
        val safety = if (mode == ImportMode.REPLACE) {
            backup.exportToCache("pre-import-${System.currentTimeMillis()}.json").name
        } else null

        val r = backup.importFrom(uri, mode)
        // A restored backup can bring rows with no category; file them now rather than
        // leaving it to startup, which no longer runs this on every launch.
        repository.categorizeUncategorized()
        buildString {
            append("Imported ${r.rawMessages} messages, ${r.transactions} transactions.")
            if (safety != null) append(" Previous data saved to cache as $safety.")
        }
    }

    /** Sends the user back through the first-run guide on the next frame. */
    fun rerunSetupGuide() = viewModelScope.launch { settings.setOnboardingDone(false) }

    val askUncategorised: StateFlow<Boolean> = settings.askUncategorised
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setAskUncategorised(enabled: Boolean) = viewModelScope.launch { settings.setAskUncategorised(enabled) }

    fun clearStatus() { _status.value = null }

    private fun launchTask(label: String, block: suspend () -> String) {
        viewModelScope.launch {
            _busy.value = true
            _status.value = "$label…"
            _status.value = try {
                block()
            } catch (e: Exception) {
                "$label failed: ${e.message ?: e::class.simpleName}"
            } finally {
                _busy.value = false
            }
        }
    }
}
