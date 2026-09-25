package com.abi.expensetracker.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import com.abi.expensetracker.ui.components.GroupPosition
import com.abi.expensetracker.ui.components.GroupedRow
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import com.abi.expensetracker.ui.theme.ChipShape
import com.abi.expensetracker.data.CalendarDates
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.ui.components.LedgerCard
import com.abi.expensetracker.ui.components.Monogram
import com.abi.expensetracker.ui.components.SectionHeader
import com.abi.expensetracker.ui.theme.AppTheme
import com.abi.expensetracker.ui.theme.LocalTabularStyle
import java.util.Locale
import kotlin.math.abs

/**
 * Where the month went: one line for when, one list for what on.
 *
 * Both answer questions the ledger cannot. The ledger is a list of events, and a list of
 * events is the wrong shape for "is this month worse than last" — that needs a total, a
 * comparison and a breakdown, which is all this screen is.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(vm: TrendsViewModel = viewModel(), resetSignal: Int = 0) {
    val state by vm.state.collectAsStateWithLifecycle()
    val window by vm.window.collectAsStateWithLifecycle()
    val canGoForward by vm.canGoForward.collectAsStateWithLifecycle()
    val openCategory by vm.openCategory.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Tapping Trends while on it: current month, breakdown closed, top of the page.
    OnTabReselect(resetSignal) {
        vm.openCategory(null)
        vm.resetMonth()
        listState.scrollToItem(0)
    }

    // A tapped category takes over the tab with the ledger itself, filtered to that
    // category and month: same rows, edit popup, selection and delete. Back returns.
    openCategory?.let { slice ->
        val ledger: HomeViewModel = viewModel(key = "trends-category")
        LaunchedEffect(slice.categoryId, window) { ledger.showCategory(window.range, slice.categoryId) }
        HomeScreen(
            onOpenSettings = {},
            showAddDialog = false,
            onAddDialogClose = {},
            vm = ledger,
            categoryView = CategoryView(slice.name, window.label, slice.amountMinor),
            onBack = { vm.openCategory(null) }
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                title = { Text("Trends", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MonthSwitcher(
                    label = window.label,
                    // The other calendar's month in which this one starts, underneath.
                    otherLabel = CalendarDates.monthLabel(window.firstDay, !window.nepali),
                    canGoForward = canGoForward,
                    onPrevious = vm::previousMonth,
                    onNext = vm::nextMonth
                )
            }

            if (state.isEmpty) {
                item {
                    LedgerCard {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Nothing recorded this month",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Once messages are synced, this shows what the month went on.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            item { MonthTotalCard(state) }
            item { DailySpendCard(state, window) }
            // The chevrons already say the rows open; the loans/splits note lives once, in
            // the total card, and only when it changed the number.
            item { SectionHeader(title = "By category") }
            item { CategoryCard(state.categories, onOpen = vm::openCategory) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Previous / month / next, with the other calendar's month name under the current one. */
@Composable
private fun MonthSwitcher(
    label: String,
    otherLabel: String,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    // A bare row rather than a card: it is navigation, not content, and a filled box
    // here weighed as much as the total it sits above.
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                otherLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
        }
    }
}

/** The month's total, how it compares with last month, and what it leaves out. */
@Composable
private fun MonthTotalCard(state: TrendsState) {
    val finance = AppTheme.finance
    val change = state.changePercent
    LedgerCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "TOTAL SPENT THIS MONTH",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                Money.format(state.totalMinor),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (change != null) {
                val lower = change < 0f
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Spending less is the good direction, so the credit colour carries
                    // it; the words say which way regardless.
                    Icon(
                        if (lower) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = if (lower) finance.credit else finance.debit,
                        modifier = Modifier.size(18.dp).padding(end = 4.dp)
                    )
                    Text(
                        "%.1f%% %s".format(abs(change), if (lower) "lower" else "higher"),
                        style = MaterialTheme.typography.bodyMedium.merge(LocalTabularStyle.current),
                        color = if (lower) finance.credit else finance.debit
                    )
                    Text(
                        "  than last month (${Money.format(state.previousTotalMinor)})",
                        style = MaterialTheme.typography.bodyMedium.merge(LocalTabularStyle.current),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "No spending recorded last month to compare against.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.loanExcludedMinor > 0 || state.recoveredMinor > 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Outlined.Info, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)
                        )
                        // Only the parts that moved the total: "₹0.00 recovered" is noise.
                        val parts = listOfNotNull(
                            state.loanExcludedMinor.takeIf { it > 0 }?.let { "${Money.format(it)} in loans" },
                            state.recoveredMinor.takeIf { it > 0 }?.let { "${Money.format(it)} recovered from splits" }
                        )
                        Text(
                            "Excludes " + parts.joinToString(" and "),
                            style = MaterialTheme.typography.bodySmall.merge(LocalTabularStyle.current),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** One bar per day of the month, the busiest in the accent, the rest in its pale tint. */
@Composable
private fun DailySpendCard(state: TrendsState, window: com.abi.expensetracker.data.MonthWindow) {
    LedgerCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Filled.BarChart, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
                    )
                    Text("Daily spend", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "${state.recordedDays} days recorded",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SpendBars(state.daily, state.busiestDay?.day, Modifier.fillMaxWidth().height(150.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Every fifth day labelled; thirty labels would not fit and say no more.
                listOf(1, 5, 10, 15, 20, 25, state.daily.size).distinct().forEach { d ->
                    Text(
                        "$d",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (d == state.busiestDay?.day) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            state.busiestDay?.let { peak ->
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("●  ", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Busiest day: " + CalendarDates.dayLabel(window.firstDay.plusDays(peak.day - 1L), window.nepali)
                                .substringBefore(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            Money.format(peak.amountMinor),
                            style = MaterialTheme.typography.titleSmall.merge(LocalTabularStyle.current),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bars on a Canvas. No charting library: one series of at most 32 bars is a few rects, and
 * a dependency for that would outweigh the whole UI layer.
 *
 * Quiet days are real zeroes, drawn as a sliver, so a week of nothing does not look like a
 * steady trickle.
 */
@Composable
private fun SpendBars(daily: List<DailySpend>, busiestDay: Int?, modifier: Modifier = Modifier) {
    val guide = MaterialTheme.colorScheme.outlineVariant
    val empty = MaterialTheme.colorScheme.surfaceContainerHigh
    val peak = daily.maxOfOrNull { it.amountMinor } ?: 0L

    // The breakdown beneath carries the same information precisely.
    Canvas(modifier.clearAndSetSemantics { }) {
        if (daily.isEmpty() || peak <= 0L) return@Canvas
        listOf(0.25f, 0.6f).forEach { at ->
            drawLine(guide, Offset(0f, size.height * at), Offset(size.width, size.height * at), strokeWidth = 1f)
        }
        val slot = size.width / daily.size
        val barWidth = slot * 0.62f
        daily.forEachIndexed { index, point ->
            val left = index * slot + (slot - barWidth) / 2f
            if (point.amountMinor <= 0L || point.segments.isEmpty()) {
                // A quiet day is a real zero, drawn as a sliver.
                drawRect(empty, Offset(left, size.height - 2f), Size(barWidth, 2f))
                return@forEachIndexed
            }
            // Stacked by category, biggest at the bottom, in each category's colour.
            val barHeight = point.amountMinor.toFloat() / peak * size.height
            val sum = point.segments.sumOf { it.second }.toFloat()
            var bottom = size.height
            point.segments.forEach { (argb, amount) ->
                val h = amount / sum * barHeight
                drawRect(Color(argb), Offset(left, bottom - h), Size(barWidth, h))
                bottom -= h
            }
        }
    }
}

/** Every category in one grouped card: icon, name, share, amount and a share bar. */
@Composable
private fun CategoryCard(slices: List<CategorySlice>, onOpen: (CategorySlice) -> Unit) {
    LedgerCard {
        slices.forEachIndexed { index, slice ->
            Column(
                Modifier.clickable { onOpen(slice) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.size(12.dp).background(Color(slice.color), CircleShape))
                    Column(Modifier.weight(1f)) {
                        Text(slice.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "%.1f%% of total".format(slice.shareOfTotal * 100),
                            style = MaterialTheme.typography.bodySmall.merge(LocalTabularStyle.current),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        Money.format(slice.amountMinor),
                        style = MaterialTheme.typography.titleMedium.merge(LocalTabularStyle.current)
                    )
                    Icon(
                        Icons.Filled.ChevronRight, contentDescription = "Show transactions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { slice.shareOfTotal.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clearAndSetSemantics { },
                    color = Color(slice.color),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    drawStopIndicator = {}
                )
            }
            if (index < slices.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
