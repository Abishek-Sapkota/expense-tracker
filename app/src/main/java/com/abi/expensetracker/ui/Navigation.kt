package com.abi.expensetracker.ui

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * The tabs, in swipe order.
 *
 * This enum is the single source of truth: the pager, the bottom bar and any
 * screen-to-screen jump all read it, so changing the tab set is one edit here.
 *
 * Four tabs: only what is opened weekly gets one. Accounts moved into Settings because it
 * is set up once. Five is the ceiling; the pager, the bar and the back handler all derive
 * from this list, and [HomeScreen] reaches Settings through a callback rather than
 * assuming it is on the bar.
 */
/**
 * The design's Material Symbols as vectors: filled on the selected tab, outlined on the
 * rest, per the spec's bottom-bar states.
 */
enum class Destination(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("Ledger", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    TRENDS("Trends", Icons.Outlined.QueryStats, Icons.Filled.QueryStats),
    LOANS("Loans", Icons.Outlined.SwapHoriz, Icons.Filled.SwapHoriz),
    SETTINGS("Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
}

/**
 * The standard bar: icon over label, one row, no gap.
 *
 * Labelled: icons alone, one of which is a chart, are not learned in a day.
 *
 * Adding is no longer on the bar. It is the one action here that writes something, and it
 * belongs beside the list it writes to rather than among the things that only navigate.
 */
@Composable
fun LedgerBottomBar(
    selected: Destination,
    onSelect: (Destination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Destination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        imageVector = if (destination == selected) destination.selectedIcon else destination.icon,
                        // Null, not the label: the label is already visible beside it, and
                        // a description would have a screen reader say it twice.
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        destination.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Runs [onReselect] when the user taps the tab they are already on, [signal] being a count
 * of such taps. The handled count is saved with the page, so a page that was disposed while
 * off screen does not replay an old reset when it comes back.
 */
@Composable
fun OnTabReselect(signal: Int, onReselect: suspend () -> Unit) {
    var handled by rememberSaveable { mutableIntStateOf(signal) }
    LaunchedEffect(signal) {
        if (signal != handled) {
            handled = signal
            onReselect()
        }
    }
}
