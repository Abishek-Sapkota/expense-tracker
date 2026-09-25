package com.abi.expensetracker.ui.components

import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import com.abi.expensetracker.data.AppIconRef
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.model.LimitBasis
import com.abi.expensetracker.data.model.LimitStatus
import com.abi.expensetracker.ui.theme.AppTheme
import com.abi.expensetracker.ui.theme.CardShape
import com.abi.expensetracker.ui.theme.ChipShape
import com.abi.expensetracker.ui.theme.PillShape
import com.abi.expensetracker.ui.theme.HeroShape
import com.abi.expensetracker.ui.theme.LocalTabularStyle

/**
 * Shared pieces of the Utilitarian Ledger design.
 *
 * Elevation here is tonal — a surface container step plus a hairline outline — rather
 * than drop shadows, which is what keeps the app reading as paper instead of glass.
 */

/** Level 1: feed surfaces. */
@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    tone: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable ColumnScope.() -> Unit
) {
    // Level 1: white (charcoal in dark) with a mandatory 1px hairline and no shadow.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = tone,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(content = content)
    }
}

/**
 * The hero period card: the largest number on the screen, by a wide margin.
 *
 * A Level 1 card like the rest, set apart by type rather than by inversion: the period
 * total is the accent-coloured display figure. When a limit is set its progress sits under
 * the total — "spent" and "spent against what" are one thought — and inflow closes the
 * card in a tinted band so money in never reads as part of the spend.
 */
@Composable
fun PeriodHeroCard(
    periodLabel: String,
    spentMinor: Long,
    receivedMinor: Long,
    modifier: Modifier = Modifier,
    limitStatus: LimitStatus? = null
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    LedgerCard(modifier) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                periodLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = muted
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    Money.format(spentMinor),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    " spent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            limitStatus?.let { status ->
                Spacer(Modifier.height(8.dp))
                HeroLimitBar(status)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.SouthWest, contentDescription = null,
                tint = AppTheme.finance.credit, modifier = Modifier.size(18.dp)
            )
            Text(
                "Total inflow received",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                modifier = Modifier.weight(1f)
            )
            Text(
                Money.formatSigned(receivedMinor, isCredit = true),
                style = MaterialTheme.typography.titleMedium.merge(LocalTabularStyle.current),
                color = AppTheme.finance.credit
            )
        }
    }
}

/**
 * Progress against the limit, under the hero total.
 *
 * Over budget the two amounts collapse into one sentence — "रु2,140.00 — रु140.00 over
 * your daily limit" — because at that point "left" is the wrong word and a second number
 * on the right would still read as headroom.
 */
@Composable
private fun HeroLimitBar(status: LimitStatus) {
    val window = when (status.limit.basis) {
        LimitBasis.DAY -> "daily"
        LimitBasis.MONTH -> "monthly"
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val bar = if (status.isOver) AppTheme.finance.debit else MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (status.isOver) {
            Text(
                Money.format(status.spentMinor) + " \u2014 " +
                    Money.format(abs(status.remainingMinor)) + " over your $window limit",
                style = MaterialTheme.typography.bodySmall.merge(LocalTabularStyle.current),
                color = AppTheme.finance.debit
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    Money.format(status.spentMinor) + " of " +
                        Money.format(status.limit.amountMinor) + " $window limit",
                    style = MaterialTheme.typography.bodySmall.merge(LocalTabularStyle.current),
                    color = muted
                )
                Text(
                    Money.format(status.remainingMinor) + " left",
                    style = MaterialTheme.typography.bodySmall.merge(LocalTabularStyle.current),
                    color = if (status.isClose) AppTheme.finance.debit else MaterialTheme.colorScheme.primary
                )
            }
        }
        // The amounts above say the same thing precisely.
        LinearProgressIndicator(
            progress = { status.fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp).clearAndSetSemantics { },
            color = bar,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            drawStopIndicator = {}
        )
    }
}

/** Small status pill: "Parsed", "Manual", "Needs bank", "Auto-matched". */
@Composable
fun StatusChip(
    text: String,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraSmall, color = container) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** A tappable banner that points at something needing attention, without alarm styling. */
@Composable
fun NudgeBanner(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "!",
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A circular monogram for a merchant or bank.
 *
 * [glyph] is drawn whole when given — an emoji is more than one char, so the initial-letter
 * path would cut it in half — and the first letter of [text] is the fallback. A glyph that
 * names an installed app (see [com.abi.expensetracker.data.AppIconRef]) draws that app's
 * launcher icon instead, filling the circle: adaptive icons carry their own background, so
 * masking them to the circle is what the launcher itself does.
 */
@Composable
fun Monogram(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    glyph: String? = null
) {
    val appIcon = rememberAppIcon(AppIconRef.packageOf(glyph))
    Box(
        modifier.size(40.dp).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center
    ) {
        if (appIcon != null) {
            Image(
                bitmap = appIcon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                // An app glyph whose app is gone falls through to the letter, not to the
                // raw "app:com.x" string.
                glyph?.takeIf { AppIconRef.packageOf(it) == null }?.trim()?.takeIf { it.isNotEmpty() }
                    ?: text.trim().take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.titleMedium,
                color = content,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Section heading with an optional count on the right. */
@Composable
fun SectionHeader(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        // Plain muted text, not a pill: nearly every section carries a count, and a pill
        // on each one made every header compete with the content under it.
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelMedium.merge(LocalTabularStyle.current),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The filter and selection chip of the design: 32dp pill, hairline and muted when off,
 * solid accent with a leading check when on. One look for period filters, loan filters
 * and every either/or choice in a form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = ChipShape,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

/** Where a row sits in a grouped list, which decides its corners and its divider. */
enum class GroupPosition { FIRST, MIDDLE, LAST, ONLY;

    companion object {
        fun of(index: Int, size: Int): GroupPosition = when {
            size <= 1 -> ONLY
            index == 0 -> FIRST
            index == size - 1 -> LAST
            else -> MIDDLE
        }
    }
}

/**
 * One row of a list that reads as a single card.
 *
 * Separate cards per transaction turn a day's spending into a stack of competing objects;
 * one card divided by hairlines reads as a ledger page, which is what it is. The rows stay
 * separate items in the lazy list — the grouping is only shape and a divider, so a long
 * period still renders the rows it can see rather than all of them.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupedRow(
    position: GroupPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    content: @Composable () -> Unit
) {
    val radius = 16.dp
    val zero = 0.dp
    val top = if (position == GroupPosition.FIRST || position == GroupPosition.ONLY) radius else zero
    val bottom = if (position == GroupPosition.LAST || position == GroupPosition.ONLY) radius else zero

    val shape = RoundedCornerShape(
        topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom
    )
    val hairline = MaterialTheme.colorScheme.outlineVariant
    val hasTop = position == GroupPosition.FIRST || position == GroupPosition.ONLY
    val hasBottom = position == GroupPosition.LAST || position == GroupPosition.ONLY
    Surface(
        // Clipped before the click so the ripple follows the rounded ends.
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .drawWithCache {
                // One 1px outline around the whole group: each row draws its share of
                // it, with the edges it shares with a neighbour pushed out of its clip.
                // Built once per size rather than on every frame of a scroll.
                val stroke = 1.dp.toPx()
                val extra = stroke * 4
                val top = if (hasTop) 0f else -extra
                val bottom = if (hasBottom) size.height else size.height + extra
                val outline = shape.createOutline(Size(size.width, bottom - top), layoutDirection, this)
                val style = Stroke(stroke * 2)
                onDrawWithContent {
                    drawContent()
                    clipRect {
                        translate(top = top) { drawOutline(outline, hairline, style = style) }
                    }
                }
            },
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column {
            content()
            // Inset to the text column, so the rule never pierces the icon lane or the
            // card's outer edge. None under the last row: the card edge ends the list.
            if (!hasBottom) {
                HorizontalDivider(Modifier.padding(start = 68.dp), color = hairline)
            }
        }
    }
}

/**
 * The row icon: the account's own icon, or an initial.
 *
 * No direction badge on it: the amount beside it already says which way the money went
 * with its sign as well as its colour, so a red-green colour deficiency still reads it,
 * and a third marker for the same fact was clutter on every row.
 */
@Composable
fun DirectionalMonogram(
    text: String,
    isCredit: Boolean,
    modifier: Modifier = Modifier,
    glyph: String? = null
) {
    val finance = AppTheme.finance
    Monogram(
        text = text,
        modifier = modifier,
        // Money in wears the credit tint; money out stays neutral, so a page of
        // ordinary spending reads calm and an inflow stands out.
        container = if (isCredit) finance.creditSurface else MaterialTheme.colorScheme.surfaceContainerHigh,
        content = if (isCredit) finance.credit else MaterialTheme.colorScheme.onSurfaceVariant,
        glyph = glyph
    )
}

/**
 * The one Add button. Same place (bottom right), shape, colour and word on every screen
 * that adds something, so adding never has to be looked for.
 */
@Composable
fun AddFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        shape = PillShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Add") }
    )
}
