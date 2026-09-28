package com.abi.expensetracker.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import com.abi.expensetracker.ui.theme.PillShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.atan2
import kotlin.math.sqrt
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
import com.abi.expensetracker.ui.components.countUpMinor
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
    val uncategorised by vm.uncategorisedCount.collectAsStateWithLifecycle()
    val chart by vm.chart.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var sorting by rememberSaveable { mutableStateOf(false) }

    // Tapping Trends while on it: current month, breakdown closed, top of the page.
    OnTabReselect(resetSignal) {
        sorting = false
        vm.openCategory(null)
        vm.resetMonth()
        listState.scrollToItem(0)
    }

    if (sorting) {
        SortScreen(onBack = { sorting = false }, nepaliDates = window.nepali)
        return
    }

    // A tapped category takes over the tab with the ledger itself, filtered to that
    // category and month: same rows, edit popup, selection and delete. Back returns.
    openCategory?.let { slice ->
        // Keyed per category and month, and put in category mode before anything collects
        // it: one shared ledger showed the previous category's rows (or today's whole
        // ledger) for its first frames, and select-all could act on them.
        val ledger: HomeViewModel =
            viewModel(key = "trends-category-${slice.categoryId}-${window.range.startMillis}")
        remember(ledger) { ledger.showCategory(window.range, slice.categoryId) }
        // The live total, so deleting or re-filing a row in the drill-down updates it.
        val total = state.categories.firstOrNull { it.categoryId == slice.categoryId }?.amountMinor ?: 0L
        HomeScreen(
            onOpenSettings = {},
            showAddDialog = false,
            onAddDialogClose = {},
            vm = ledger,
            categoryView = CategoryView(slice.name, window.label, total),
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
            item { ChartCard(state, window, chart, onChart = vm::setChart) }
            // The chevrons already say the rows open; the loans/splits note lives once, in
            // the total card, and only when it changed the number.
            item { SectionHeader(title = "By category") }
            item { CategoryCard(state.categories, onOpen = vm::openCategory) }
            // Last on the page, centred: a follow-up to the breakdown, not part of it.
            // Counted over all time, like the queue it opens, since a filed month would
            // otherwise hide an unfiled one.
            if (uncategorised > 0) item {
                Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    OutlinedButton(onClick = { sorting = true }, shape = PillShape) {
                        Text("Sort $uncategorised uncategorised payment${if (uncategorised == 1) "" else "s"}")
                    }
                }
            }
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
                Money.format(countUpMinor("trends-total", state.totalMinor) ?: state.totalMinor),
                style = MaterialTheme.typography.displayMedium.merge(LocalTabularStyle.current),
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
            if (state.loanExcludedMinor > 0 || state.recoveredMinor > 0 || state.transferExcludedMinor > 0) {
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
                            state.transferExcludedMinor.takeIf { it > 0 }?.let { "${Money.format(it)} moved to your own wallets" },
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

/**
 * The month drawn one of two ways, switched by the pill in the header: when the money went
 * (a bar per day) or what it went on (a donut of categories). One card rather than two, so
 * the page does not grow and the switch sits right next to what it changes.
 */
@Composable
private fun ChartCard(
    state: TrendsState,
    window: com.abi.expensetracker.data.MonthWindow,
    chart: TrendsChart,
    onChart: (TrendsChart) -> Unit
) {
    LedgerCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (chart == TrendsChart.PIE) Icons.Filled.PieChart else Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        if (chart == TrendsChart.PIE) "By category" else "Daily spend",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (chart == TrendsChart.BARS) {
                        Text(
                            "${state.recordedDays} days recorded",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ChartSwitch(chart, onChart)
            }
            AnimatedContent(
                targetState = chart,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "trends chart"
            ) { shown ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (shown == TrendsChart.PIE) CategoryDonut(state, window)
                    else DailyBars(state, window)
                }
            }
        }
    }
}

/** Both choices always visible, the current one filled with the accent. */
@Composable
private fun ChartSwitch(chart: TrendsChart, onChart: (TrendsChart) -> Unit) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, PillShape)
            .padding(2.dp)
    ) {
        listOf(
            Triple(TrendsChart.BARS, Icons.Filled.BarChart, "Daily bars"),
            Triple(TrendsChart.PIE, Icons.Filled.PieChart, "Category pie")
        ).forEach { (option, icon, label) ->
            val on = option == chart
            Box(
                Modifier
                    .size(width = 38.dp, height = 30.dp)
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent, PillShape)
                    .selectable(selected = on, role = Role.Tab, onClick = { onChart(option) }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, contentDescription = label,
                    tint = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** One bar per day of the month, the busiest in the accent, the rest in its pale tint. */
@Composable
private fun DailyBars(state: TrendsState, window: com.abi.expensetracker.data.MonthWindow) {
    SpendBars(state.daily, state.busiestDay?.day, Modifier.fillMaxWidth().height(150.dp), growKey = window.firstDay)
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

/**
 * The month's categories as a donut, with the total in the hole. Past the sixth, the
 * slices get too thin to see or tap, so the rest share one "Other" slice; the breakdown
 * below still lists every category.
 *
 * No list of its own: the breakdown right below names every colour already. A tapped
 * slice is picked out instead: the others dim and the hole shows its name, amount and
 * share. Tapping it again, or the hole, goes back to the total.
 */
@Composable
private fun CategoryDonut(state: TrendsState, window: com.abi.expensetracker.data.MonthWindow) {
    val sorted = state.categories.sortedByDescending { it.amountMinor }
    val otherColor = MaterialTheme.colorScheme.outline.toArgb()
    val slices = remember(sorted, otherColor) {
        if (sorted.size <= 7) sorted
        else {
            val rest = sorted.drop(6)
            sorted.take(6) + CategorySlice(
                categoryId = null,
                name = "Other",
                amountMinor = rest.sumOf { it.amountMinor },
                shareOfTotal = rest.sumOf { it.shareOfTotal.toDouble() }.toFloat(),
                color = otherColor
            )
        }
    }
    // Cleared with the month: an index into last month's slices means nothing in this one.
    var picked by rememberSaveable(window.firstDay) { mutableStateOf<Int?>(null) }
    val pick = { i: Int? -> picked = if (i == picked) null else i }
    val sum = slices.sumOf { it.amountMinor }.coerceAtLeast(1L).toFloat()

    val sweep = remember(window.firstDay) { Animatable(0f) }
    LaunchedEffect(window.firstDay) { sweep.animateTo(1f, tween(durationMillis = 450)) }

    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(200.dp)
                .clearAndSetSemantics { }
                .pointerInput(slices) {
                    detectTapGestures { at ->
                        val dx = at.x - size.width / 2f
                        val dy = at.y - size.height / 2f
                        val outer = minOf(size.width, size.height) / 2f
                        val distance = sqrt(dx * dx + dy * dy)
                        if (distance < outer * 0.6f || distance > outer) {
                            picked = null
                            return@detectTapGestures
                        }
                        // Clockwise from twelve o'clock, the way the slices are laid out.
                        val angle = (Math.toDegrees(atan2(dx, -dy).toDouble()).toFloat() + 360f) % 360f
                        var start = 0f
                        slices.forEachIndexed { i, slice ->
                            val end = start + slice.amountMinor / sum * 360f
                            if (angle < end) { pick(i); return@detectTapGestures }
                            start = end
                        }
                    }
                }
        ) {
            val outer = size.minDimension / 2f - 4.dp.toPx()
            val ring = outer * 0.38f
            // A thin gap between slices keeps neighbours with close colours apart.
            val gap = if (slices.size > 1) 1.2f else 0f
            var start = -90f
            slices.forEachIndexed { i, slice ->
                val extent = slice.amountMinor / sum * 360f * sweep.value
                val on = picked == null || picked == i
                // The picked slice grows outward a little, so it reads as lifted.
                val grow = if (picked == i) 4.dp.toPx() else 0f
                val radius = outer - ring / 2f + grow / 2f
                if (extent > gap) {
                    drawArc(
                        color = Color(slice.color).copy(alpha = if (on) 1f else 0.35f),
                        startAngle = start + gap / 2f,
                        sweepAngle = extent - gap,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = ring + grow)
                    )
                }
                start += extent
            }
        }
        val chosen = picked?.let { slices.getOrNull(it) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                chosen?.name ?: "Spent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp)
            )
            Text(
                Money.format(chosen?.amountMinor ?: state.totalMinor),
                style = MaterialTheme.typography.titleLarge.merge(LocalTabularStyle.current),
                color = chosen?.let { Color(it.color) } ?: MaterialTheme.colorScheme.onSurface
            )
            if (chosen != null) {
                Text(
                    "%.1f%%".format(chosen.shareOfTotal * 100),
                    style = MaterialTheme.typography.labelSmall.merge(LocalTabularStyle.current),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun SpendBars(
    daily: List<DailySpend>,
    busiestDay: Int?,
    modifier: Modifier = Modifier,
    /** The month shown: the bars grow in when it changes, not on every new transaction. */
    growKey: Any? = null
) {
    val grow = remember(growKey) { Animatable(0f) }
    LaunchedEffect(growKey) { grow.animateTo(1f, tween(durationMillis = 450)) }
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
            val barHeight = point.amountMinor.toFloat() / peak * size.height * grow.value
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
