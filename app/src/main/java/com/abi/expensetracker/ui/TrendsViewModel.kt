package com.abi.expensetracker.ui

import com.abi.expensetracker.data.CategoryColors
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abi.expensetracker.data.MonthWindow
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.data.todayFlow
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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Spend on one day of the window. [day] is 1-based, matching the calendar. */
data class DailySpend(
    val day: Int,
    val amountMinor: Long,
    /** The day's spend split by category colour (ARGB to amount), largest category first. */
    val segments: List<Pair<Int, Long>> = emptyList()
)

/** One bar of the category breakdown. A null [categoryId] is the uncategorised bucket. */
data class CategorySlice(
    val categoryId: Long?,
    val name: String,
    val amountMinor: Long,
    val shareOfTotal: Float,
    /** ARGB from [com.abi.expensetracker.data.CategoryColors]. */
    val color: Int
)

/** The two ways the Trends chart card draws the month. */
enum class TrendsChart { BARS, PIE }

data class TrendsState(
    val totalMinor: Long = 0L,
    val previousTotalMinor: Long = 0L,
    val daily: List<DailySpend> = emptyList(),
    val categories: List<CategorySlice> = emptyList(),
    val recordedDays: Int = 0,
    /** Money out this month that was marked as a loan, and so is not in [totalMinor]. */
    val loanExcludedMinor: Long = 0L,
    /** Money moved into the user's own wallets this month, also not in [totalMinor]. */
    val transferExcludedMinor: Long = 0L,
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

    /**
     * Stored rather than held in the screen, so the tab opens on whichever chart the user
     * switched to last, across restarts.
     */
    val chart: StateFlow<TrendsChart> = settings.trendsChart
        .map { name -> TrendsChart.entries.firstOrNull { it.name == name } ?: TrendsChart.BARS }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TrendsChart.BARS)

    fun setChart(chart: TrendsChart) = viewModelScope.launch { settings.setTrendsChart(chart.name) }

    /** Uncategorised spending over all time: what the sorting screen will offer. */
    val uncategorisedCount: StateFlow<Int> = repository.observeUncategorisedDebits()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)


    /**
     * The month on screen, on whichever calendar the user reads.
     *
     * Rebuilt when the calendar setting changes rather than converted: the Nepali month
     * containing today is the honest answer to "which month am I looking at", and keeping
     * the old boundaries would show a window that belongs to neither calendar.
     */
    // Null means "the month containing today", which moves with the date: an anchor fixed
    // when the screen opened kept showing last month after the month turned.
    private val _anchor = MutableStateFlow<LocalDate?>(null)
    private val today = todayFlow()

    val window: StateFlow<MonthWindow> =
        combine(_anchor, settings.useNepaliCalendar, today) { anchor, nepali, day ->
            MonthWindow.of(anchor ?: day, nepali)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MonthWindow.of(LocalDate.now(), nepali = false)
        )

    /** Back to the month containing today. */
    fun resetMonth() { _anchor.value = null }

    fun previousMonth() {
        _anchor.value = window.value.previous.firstDay
    }

    /** Never past the current month: there is nothing recorded in the future to show. */
    fun nextMonth() {
        val next = window.value.next
        val now = LocalDate.now(zone)
        if (next.firstDay.isAfter(now)) return
        _anchor.value = if (now in next) null else next.firstDay
    }

    val canGoForward: StateFlow<Boolean> = combine(window, today) { w, day -> day !in w }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val state: StateFlow<TrendsState> = window.flatMapLatest { window ->
        combine(
            repository.observeDebitsBetween(window.range),
            repository.observeSpentBetween(window.previous.range),
            repository.observeCategoryTotals(window.range),
            repository.observeCategories(),
            combine(
                repository.observeSplitRecoveries(window.range),
                repository.observeLoanDebitsBetween(window.range),
                repository.observeTransfersBetween(window.range)
            ) { recoveries, loans, transfers -> Triple(recoveries, loans, transfers) }
        ) { debits, previousTotal, rawTotals, categoryList, (recoveries, loanDebits, transfers) ->
            // What friends paid back on a split bill comes off that bill's day and
            // category, so Dining shows the user's own share once everyone has paid.
            val recoveredByCategory = recoveries.groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.recoveredMinor } }
            val totals = rawTotals.map { it.copy(totalMinor = it.totalMinor - (recoveredByCategory[it.categoryId] ?: 0L)) }
            // Bucketed here rather than in SQL: grouping by local calendar day in SQLite
            // means embedding a timezone offset in the query, which is wrong twice a year
            // and wrong permanently for anyone who travels.
            val byDay = LongArray(window.dayCount)
            // The same days split by category, for the stacked bars.
            val byDayCategory = Array(window.dayCount) { mutableMapOf<Long?, Long>() }
            debits.forEach { txn ->
                // Day of the window, not day of the Gregorian month: a Nepali month
                // starts mid-month and would otherwise scatter its spending across the
                // wrong bars.
                val date = Instant.ofEpochMilli(txn.occurredAt).atZone(zone).toLocalDate()
                window.dayOf(date)?.let { day ->
                    byDay[day - 1] += txn.amountMinor
                    val m = byDayCategory[day - 1]
                    m[txn.categoryId] = (m[txn.categoryId] ?: 0L) + txn.amountMinor
                }
            }
            recoveries.forEach { r ->
                val date = Instant.ofEpochMilli(r.occurredAt).atZone(zone).toLocalDate()
                window.dayOf(date)?.let { day ->
                    byDay[day - 1] -= r.recoveredMinor
                    val m = byDayCategory[day - 1]
                    m[r.categoryId] = (m[r.categoryId] ?: 0L) - r.recoveredMinor
                }
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
                        shareOfTotal = if (total <= 0L) 0f else row.totalMinor.toFloat() / total,
                        color = CategoryColors.of(category)
                    )
                }
            // Stack each day in the breakdown's order, so the biggest category sits at the
            // bottom of every bar and colours line up across days.
            val rank = slices.mapIndexed { i, s -> s.categoryId to i }.toMap()
            val colorOf = { id: Long? -> CategoryColors.of(id?.let { categoryById[it] }) }

            TrendsState(
                totalMinor = total,
                previousTotalMinor = previousTotal,
                daily = byDay.mapIndexed { index, amount ->
                    val parts = byDayCategory[index].toList()
                        .filter { (_, value) -> value > 0 }
                        .sortedBy { (id, _) -> rank[id] ?: Int.MAX_VALUE }
                        .map { (id, value) -> colorOf(id) to value }
                    DailySpend(index + 1, amount, parts)
                },
                categories = slices,
                recordedDays = byDay.count { it > 0L },
                loanExcludedMinor = loanDebits,
                transferExcludedMinor = transfers,
                recoveredMinor = recoveries.sumOf { it.recoveredMinor }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrendsState())
}
