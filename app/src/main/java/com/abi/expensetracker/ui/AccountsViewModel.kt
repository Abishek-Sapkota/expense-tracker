package com.abi.expensetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abi.expensetracker.data.InstalledApp
import com.abi.expensetracker.data.SenderNormalizer
import com.abi.expensetracker.data.appLabel
import com.abi.expensetracker.data.model.Bank
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.di.ServiceLocator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A sender as settings presents it: the normalised key, every raw variant folded into it,
 * and the bank it currently points at.
 */
data class SenderEntry(
    val senderKey: String,
    val rawSenders: List<String>,
    val messageCount: Int,
    val bankId: Long?,
    val bankName: String?,
    /** The installed app behind a notification sender, which SMS senders never have. */
    val appPackage: String? = null,
    val appLabel: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository(app)
    private val settings = SettingsStore(app)

    val banks: StateFlow<List<Bank>> = repository.observeBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Packages whose notifications are read, app-wide rather than per account. */
    val readApps: StateFlow<List<String>> = repository.observeNotificationApps()
        .map { it.sortedBy { pkg -> appLabel(getApplication(), pkg)?.lowercase() ?: pkg } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The one "ask what it was for" switch, for payments no category matched. */
    val askUncategorised: StateFlow<Boolean> = settings.askUncategorised
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setAskUncategorised(enabled: Boolean) =
        viewModelScope.launch { settings.setAskUncategorised(enabled) }

    /** Every sender, notification apps included, before the sender list drops the apps. */
    private val allSenders: StateFlow<List<SenderEntry>> = combine(
        repository.observeSenders(),
        repository.observeBanks(),
        repository.observeSenderLinks()
    ) { senderCounts, bankList, links ->
        val bankById = bankList.associateBy { it.id }
        val bankIdByKey = links.associate { it.senderKey to it.bankId }

        senderCounts
            // Carrier variants of one bank ("AX-NABIL", "VM-NABIL") collapse to a single
            // row so the user links each bank once rather than once per operator.
            .groupBy { SenderNormalizer.normalize(it.sender) }
            .map { (key, group) ->
                val bankId = bankIdByKey[key]
                // A notification's sender is its package; SMS ids never resolve to an app.
                val app = group.firstNotNullOfOrNull { c ->
                    appLabel(getApplication(), c.sender)?.let { c.sender to it }
                }
                SenderEntry(
                    senderKey = key,
                    rawSenders = group.map { it.sender }.distinct().sorted(),
                    messageCount = group.sumOf { it.messageCount },
                    bankId = bankId,
                    bankName = bankId?.let { bankById[it]?.name },
                    appPackage = app?.first,
                    appLabel = app?.second
                )
            }
            // Unlinked first, then noisiest: the rows that need attention sit at the top.
            .sortedWith(compareBy({ it.bankId != null }, { -it.messageCount }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * SMS senders only. Apps are picked in the app-wide notifications list instead, so they are
     * picked by name and icon rather than found by package id in a list of short codes.
     */
    val senders: StateFlow<List<SenderEntry>> = allSenders
        .map { all -> all.filter { it.appPackage == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Apps whose notifications the app has read, offered first in the app picker. */
    val notificationApps: StateFlow<List<InstalledApp>> = allSenders
        .map { all ->
            all.mapNotNull { e -> e.appPackage?.let { InstalledApp(it, e.appLabel ?: it) } }
                .sortedBy { it.label.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addReadApp(packageName: String) =
        viewModelScope.launch { repository.setNotificationApp(packageName, true) }

    fun removeReadApp(packageName: String) =
        viewModelScope.launch { repository.setNotificationApp(packageName, false) }

    private val _senderQuery = MutableStateFlow("")

    /** What the user typed into the sender search box. */
    val senderQuery: StateFlow<String> = _senderQuery.asStateFlow()

    fun setSenderQuery(text: String) {
        _senderQuery.value = text
    }

    /**
     * Sender keys whose id or message text contains the query, or null when the box is
     * empty.
     *
     * Re-queried per keystroke rather than debounced: the search is a single LIKE on a
     * table this app writes once per message, and a debounce would make short queries feel
     * slower than they are.
     */
    private val matchingKeys: Flow<Set<String>?> = _senderQuery.flatMapLatest { query ->
        val text = query.trim()
        if (text.isEmpty()) flowOf(null)
        else flow { emit(repository.senderKeysMatching(text)) }
    }

    /**
     * The senders the Accounts tab lists: the ones the user linked, and nothing else.
     *
     * A phone carries hundreds of senders — OTPs, promos, the dentist — and all but a few
     * will never be a bank. Listing them turns the tab into an inbox dump, so the tab shows
     * only what the user chose and the rest is reached through the search sheet.
     */
    val linkedSenders: StateFlow<List<SenderEntry>> = senders
        .map { all -> all.filter { it.bankId != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * What the search sheet lists.
     *
     * Empty box shows the unlinked ones — the plausible work queue — and typing searches
     * every sender, linked ones included, so a wrong link can be found and moved.
     */
    val searchResults: StateFlow<List<SenderEntry>> =
        combine(senders, _senderQuery, matchingKeys) { all, query, matched ->
            val text = query.trim()
            if (text.isEmpty()) all.filter { it.bankId == null }
            else all.filter { entry ->
                matched?.contains(entry.senderKey) == true ||
                    entry.senderKey.contains(text, ignoreCase = true) ||
                    entry.rawSenders.any { it.contains(text, ignoreCase = true) } ||
                    entry.bankName?.contains(text, ignoreCase = true) == true
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addBank(name: String, icon: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addBank(name, icon) }
    }

    fun setBankIcon(bank: Bank, icon: String?) =
        viewModelScope.launch { repository.setBankIcon(bank, icon) }

    fun deleteBank(bankId: Long) = viewModelScope.launch { repository.deleteBank(bankId) }

    fun link(senderKey: String, bankId: Long) =
        viewModelScope.launch { repository.linkSender(senderKey, bankId) }

    fun unlink(senderKey: String) =
        viewModelScope.launch { repository.unlinkSender(senderKey) }

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()
    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    /** Reads new inbox messages so their senders can be linked; the caller checks permission. */
    fun syncSms() = viewModelScope.launch {
        _syncing.value = true
        _syncStatus.value = runCatching { repository.backfillFromInbox() }
            .fold(
                { "Read ${it.messagesRead} new messages · ${it.totalStored} stored." },
                { "Could not read messages: ${it.message}" }
            )
        _syncing.value = false
    }

}
