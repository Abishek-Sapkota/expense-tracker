package com.abi.expensetracker.ui

import com.abi.expensetracker.data.CategoryColors
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abi.expensetracker.data.BankResolver
import com.abi.expensetracker.data.DateRange
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.SplitSummary
import com.abi.expensetracker.data.Splits
import com.abi.expensetracker.data.Period
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.LimitStatus
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.Split
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Txn
import com.abi.expensetracker.di.ServiceLocator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** One row as the home screen shows it: never a raw sender id. */
data class TxnRow(
    val txn: Txn,
    /** Linked bank or service name; null when the sender is not linked yet. */
    val bankName: String?,
    /** The linked bank's emoji, when it has one. Falls back to a merchant monogram. */
    val bankIcon: String?,
    val categoryName: String?,
    val isManual: Boolean,
    /** Set when the row is marked as a loan, which takes it out of the totals. */
    val loan: LoanEntry? = null,
    /** Set when the row is a bill split with friends. */
    val split: SplitSummary? = null
)

data class PeriodSelection(
    val period: Period = Period.TODAY,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null
) {
    val range: DateRange
        get() = if (period == Period.CUSTOM && customStart != null && customEnd != null) {
            Period.rangeOfDays(customStart, customEnd)
        } else {
            period.range()
        }

    val label: String
        get() = if (period == Period.CUSTOM && customStart != null && customEnd != null) {
            "$customStart to $customEnd"
        } else {
            period.label
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository(app)
    private val settings = SettingsStore(app)

    private val _selection = MutableStateFlow(PeriodSelection())
    val selection: StateFlow<PeriodSelection> = _selection.asStateFlow()

    private val resolver = combine(
        repository.observeBanks(),
        repository.observeSenderLinks(),
        repository.observeBankApps()
    ) { banks, links, apps -> BankResolver(banks, links, apps) }

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * When set, the rows are one category's spending over a range instead of the selected
     * period: the Trends drill-down, which reuses this whole ledger.
     */
    private val _category = MutableStateFlow<Pair<DateRange, Long?>?>(null)

    fun showCategory(range: DateRange, categoryId: Long?) { _category.value = range to categoryId }

    val loans: StateFlow<List<LoanEntry>> = repository.observeLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val splits: StateFlow<List<SplitSummary>> = combine(
        repository.observeSplits(),
        repository.observeLoans()
    ) { splitList, loanList -> Splits.summarise(splitList, loanList) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rows: StateFlow<List<TxnRow>> = combine(
        combine(_selection, _category) { sel, cat -> sel to cat }.flatMapLatest { (sel, cat) ->
            if (cat != null) repository.observeCategoryDebits(cat.first, cat.second)
            else repository.observeBetween(sel.range)
        },
        resolver,
        repository.observeCategories(),
        repository.observeLoans(),
        splits
    ) { withSenders, bankResolver, categoryList, loanList, splitList ->
        val splitByTxn = splitList.associateBy { it.split.txnId }
        val categoryById = categoryList.associateBy { it.id }
        val loanByTxn = loanList.filter { it.txnId != null }.associateBy { it.txnId }
        withSenders.map { row ->
            val bank = bankResolver.bankFor(row.sender, row.body)
            val category = row.txn.categoryId?.let { categoryById[it] }
            TxnRow(
                txn = row.txn,
                bankName = bank?.name,
                bankIcon = bank?.icon,
                categoryName = category?.name,
                isManual = row.txn.isManual,
                loan = loanByTxn[row.txn.id],
                split = splitByTxn[row.txn.id]
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Reports folded as duplicates in the selected period, for the ledger's chip. */
    val duplicateCount: StateFlow<Int> = _selection
        .flatMapLatest { repository.observeDuplicates(it.range) }
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val spentMinor: StateFlow<Long> = _selection
        .flatMapLatest { repository.observeSpentBetween(it.range) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val receivedMinor: StateFlow<Long> = _selection
        .flatMapLatest { repository.observeReceivedBetween(it.range) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /**
     * The current date, re-checked while the screen is watched.
     *
     * A daily limit has to reset at midnight even for a phone that was left on the ledger
     * overnight, and the window is otherwise fixed at the moment the flow was built. It only
     * runs while something is collecting (the app is on screen) and wakes once a day.
     */
    private val today = flow {
        while (true) {
            val now = java.time.LocalDateTime.now()
            emit(now.toLocalDate())
            // Sleep until just past midnight instead of waking every minute: the date is
            // all this feeds, and it changes once a day.
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            delay(java.time.Duration.between(now, nextMidnight).toMillis() + 1_000)
        }
    }.distinctUntilChanged()

    /** The spending limit and its progress, or null when no limit is set. */
    /** The app-wide calendar setting, which the ledger's dates and the limit both read. */
    val useNepaliCalendar: StateFlow<Boolean> = settings.useNepaliCalendar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val limitStatus: StateFlow<LimitStatus?> =
        combine(settings.spendingLimit, today, settings.useNepaliCalendar) { limit, date, nepali ->
            Triple(limit, date, nepali)
        }
            .flatMapLatest { (limit, date, nepali) ->
                if (limit == null) {
                    flowOf(null)
                } else {
                    repository.observeSpentBetween(limit.range(today = date, nepaliMonths = nepali))
                        .map { spent -> LimitStatus(limit, spent) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun selectPeriod(period: Period) {
        _selection.value = if (period == Period.CUSTOM) {
            _selection.value.copy(period = period)
        } else {
            PeriodSelection(period = period)
        }
    }

    fun selectCustomRange(start: LocalDate, end: LocalDate) {
        val (first, last) = if (start <= end) start to end else end to start
        _selection.value = PeriodSelection(Period.CUSTOM, first, last)
    }

    fun addManualExpense(
        amountText: String,
        date: LocalDate,
        direction: Direction,
        remark: String = ""
    ) {
        val amountMinor = Money.parseToMinor(amountText)
        if (amountMinor == null || amountMinor <= 0L) {
            _status.value = "Enter an amount greater than zero."
            return
        }
        viewModelScope.launch {
            // Midday, not midnight: a manual entry then stays inside its own day whichever
            // way the device timezone shifts later.
            val at = date.atTime(12, 0).atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            repository.addManualExpense(amountMinor, at, direction, remark)
            _status.value = "Added ${Money.format(amountMinor)}."
        }
    }

    /**
     * Correct any transaction by hand, including one parsed from a message.
     *
     * The edit sticks through a reparse; see [com.abi.expensetracker.data.model.Txn.userEdited].
     */
    fun editTransaction(
        txn: Txn,
        amountText: String,
        date: LocalDate,
        direction: Direction,
        remark: String = "",
        autoCategorize: Boolean = true
    ) {
        val amountMinor = Money.parseToMinor(amountText)
        if (amountMinor == null || amountMinor <= 0L) {
            _status.value = "Enter an amount greater than zero."
            return
        }
        viewModelScope.launch {
            // Keep the original time of day so same-day transactions stay in the order
            // they happened; only the calendar date moves.
            val zone = java.time.ZoneId.systemDefault()
            val time = java.time.Instant.ofEpochMilli(txn.occurredAt).atZone(zone).toLocalTime()
            val at = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
            val category = repository.editTransaction(txn, amountMinor, at, direction, remark, autoCategorize)
            _status.value = "Updated ${Money.format(amountMinor)}." +
                (category?.let { " Filed under $it." } ?: "")
        }
    }

    fun delete(txns: List<Txn>) = viewModelScope.launch {
        txns.forEach { repository.deleteTransaction(it) }
        _status.value = if (txns.size == 1) "Deleted 1 transaction." else "Deleted ${txns.size} transactions."
    }

    fun setCategory(txn: Txn, categoryId: Long?) =
        viewModelScope.launch { repository.setCategory(txn, categoryId) }

    /** The messages behind a row, primary first; empty for a manual entry. */
    suspend fun sourceMessages(txn: Txn): List<RawMessage> = repository.sourceMessages(txn)

    fun saveLoan(entry: LoanEntry) = viewModelScope.launch {
        repository.saveLoan(entry)
        _status.value = "Marked as a loan with ${entry.person.trim()}. It no longer counts as spent or received."
    }

    fun deleteLoan(entry: LoanEntry) = viewModelScope.launch { repository.deleteLoan(entry) }

    fun saveSplit(txn: Txn, title: String, myShareMinor: Long, shares: List<Pair<String, Long>>) =
        viewModelScope.launch {
            repository.saveSplit(txn, title, myShareMinor, shares)
            _status.value = "Split with ${shares.size}. Your share ${Money.format(myShareMinor)}; " +
                "spending drops as they pay you back."
        }

    fun deleteSplit(split: Split) = viewModelScope.launch { repository.deleteSplit(split) }

    fun recordSplitPayment(split: Split, person: String, amountMinor: Long, via: Txn?) =
        viewModelScope.launch {
            repository.recordSplitPayment(split, person, amountMinor, via)
            _status.value = "${person.trim()} paid ${Money.format(amountMinor)} for ${split.title}."
        }

    /**
     * A category made from the edit popup: its own name as its first keyword, so the next
     * matching transaction files itself, and a colour no other category is using.
     */
    fun createCategory(name: String, onCreated: (Long) -> Unit) = viewModelScope.launch {
        val id = repository.addCategory(
            name, keywords = name.trim().lowercase(),
            color = CategoryColors.nextFree(categories.value)
        )
        _status.value = "Created category ${name.trim()}."
        onCreated(id)
    }

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
