package com.abi.expensetracker.ui

import com.abi.expensetracker.data.BankResolver
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abi.expensetracker.data.MonthWindow
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.di.ServiceLocator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Spend on one day of the window. [day] is 1-based, matching the calendar. */
data class DailySpend(val day: Int, val amountMinor: Long)

/** One bar of the category breakdown. A null [categoryId] is the uncategorised bucket. */
data class CategorySlice(
    val categoryId: Long?,
    val name: String,
    val amountMinor: Long,
    val shareOfTotal: Float
)

data class TrendsState(
    val totalMinor: Long = 0L,
    val previousTotalMinor: Long = 0L,
    val daily: List<DailySpend> = emptyList(),
    val categories: List<CategorySlice> = emptyList(),
    val recordedDays: Int = 0,
    /** Money out this month that was marked as a loan, and so is not in [totalMinor]. */
    val loanExcludedMinor: Long = 0L,
    /** What friends have paid back on this month's split bills, taken off [totalMinor]. */
    val recoveredMinor: Long = 0L
) {
    /** Null when there is no previous month to compare against. */
    val changePercent: Float?
        get() = if (previousTotalMinor <= 0L) null
        else (totalMinor - previousTotalMinor) * 100f / previousTotalMinor

    val busiestDay: DailySpend? get() = daily.maxByOrNull { it.amountMinor }?.takeIf { it.amountMinor > 0 }

    val isEmpty: Boolean get() = totalMinor == 0L
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrendsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository(app)
    private val zone: ZoneId = ZoneId.systemDefault()

    private val settings = SettingsStore(app)

    /** The category tapped in the breakdown, whose transactions are listed; null for none. */
    private val _openCategory = MutableStateFlow<CategorySlice?>(null)
    val openCategory: StateFlow<CategorySlice?> = _openCategory.asStateFlow()

    fun openCategory(slice: CategorySlice?) { _openCategory.value = slice }

    private val resolver = combine(
        repository.observeBanks(), repository.observeSenderLinks(), repository.observeBankApps()
    ) { banks, links, apps -> BankResolver(banks, links, apps) }

    /** The open category's spending this month, each with the account it came through. */
    // Lazy: it reads [window], which is declared further down.
    val categoryRows: StateFlow<List<Pair<com.abi.expensetracker.data.db.TxnWithSender, String?>>> by lazy {
        combine(window, _openCategory) { w, c -> w to c }
            .flatMapLatest { (w, c) ->
                if (c == null) kotlinx.coroutines.flow.flowOf(emptyList())
                else combine(repository.observeCategoryDebits(w.range, c.categoryId), resolver) { rows, r ->
                    rows.map { it to r.bankFor(it.sender, it.body)?.name }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    /**
     * The month on screen, on whichever calendar the user reads.
     *
     * Rebuilt when the calendar setting changes rather than converted: the Nepali month
     * containing today is the honest answer to "which month am I looking at", and keeping
     * the old boundaries would show a window that belongs to neither calendar.
     */
    private val _anchor = MutableStateFlow(LocalDate.now())

    val window: StateFlow<MonthWindow> =
        combine(_anchor, settings.useNepaliCalendar) { anchor, nepali ->
            MonthWindow.of(anchor, nepali)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MonthWindow.of(LocalDate.now(), nepali = false)
        )

    fun previousMonth() {
        _anchor.value = window.value.previous.firstDay
    }

    /** Never past the current month: there is nothing recorded in the future to show. */
    fun nextMonth() {
        val next = window.value.next
        if (!next.firstDay.isAfter(LocalDate.now(zone))) _anchor.value = next.firstDay
    }

    val canGoForward: StateFlow<Boolean> = window
        .map { LocalDate.now(zone) !in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val state: StateFlow<TrendsState> = window.flatMapLatest { window ->
        combine(
            repository.observeDebitsBetween(window.range),
            repository.observeSpentBetween(window.previous.range),
            repository.observeCategoryTotals(window.range),
            repository.observeCategories(),
            combine(
                repository.observeSplitRecoveries(window.range),
                repository.observeLoanDebitsBetween(window.range)
            ) { recoveries, loans -> recoveries to loans }
        ) { debits, previousTotal, rawTotals, categoryList, (recoveries, loanDebits) ->
            // What friends paid back on a split bill comes off that bill's day and
            // category, so Dining shows the user's own share once everyone has paid.
            val recoveredByCategory = recoveries.groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.recoveredMinor } }
            val totals = rawTotals.map { it.copy(totalMinor = it.totalMinor - (recoveredByCategory[it.categoryId] ?: 0L)) }
            // Bucketed here rather than in SQL: grouping by local calendar day in SQLite
            // means embedding a timezone offset in the query, which is wrong twice a year
            // and wrong permanently for anyone who travels.
            val byDay = LongArray(window.dayCount)
            debits.forEach { txn ->
                // Day of the window, not day of the Gregorian month: a Nepali month
                // starts mid-month and would otherwise scatter its spending across the
                // wrong bars.
                val date = Instant.ofEpochMilli(txn.occurredAt).atZone(zone).toLocalDate()
                window.dayOf(date)?.let { day -> byDay[day - 1] += txn.amountMinor }
            }
            recoveries.forEach { r ->
                val date = Instant.ofEpochMilli(r.occurredAt).atZone(zone).toLocalDate()
                window.dayOf(date)?.let { day -> byDay[day - 1] -= r.recoveredMinor }
            }

            val total = debits.sumOf { it.amountMinor } - recoveries.sumOf { it.recoveredMinor }
            val categoryById = categoryList.associateBy { it.id }
            val slices = totals
                .filter { it.totalMinor > 0L }
                .map { row ->
                    val category = row.categoryId?.let { categoryById[it] }
                    CategorySlice(
                        categoryId = row.categoryId,
                        name = category?.name ?: "Uncategorised",
                        amountMinor = row.totalMinor,
                        shareOfTotal = if (total <= 0L) 0f else row.totalMinor.toFloat() / total
                    )
                }

            TrendsState(
                totalMinor = total,
                previousTotalMinor = previousTotal,
                daily = byDay.mapIndexed { index, amount -> DailySpend(index + 1, amount) },
                categories = slices,
                recordedDays = byDay.count { it > 0L },
                loanExcludedMinor = loanDebits,
                recoveredMinor = recoveries.sumOf { it.recoveredMinor }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrendsState())
}
