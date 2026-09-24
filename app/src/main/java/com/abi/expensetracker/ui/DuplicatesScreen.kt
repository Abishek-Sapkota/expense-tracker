package com.abi.expensetracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.Period
import com.abi.expensetracker.data.db.DuplicateRow
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Source
import com.abi.expensetracker.ui.components.LedgerCard
import com.abi.expensetracker.ui.components.SectionHeader
import com.abi.expensetracker.ui.theme.AppTheme
import com.abi.expensetracker.ui.theme.LocalTabularStyle
import com.abi.expensetracker.ui.theme.PillShape

/**
 * Messages the app decided were another channel's report of a transaction already in the
 * ledger, so the user can overrule a wrong match.
 *
 * Each card shows both sides of the match — the copy and the message the transaction was
 * booked from — because the only way to judge a match is to read the two together.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(onBack: () -> Unit, vm: DuplicatesViewModel = viewModel()) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val nepaliDates by vm.useNepaliCalendar.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    var showRangePicker by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                title = { Text("Duplicates", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PeriodChips(
                    selected = selection.period,
                    onSelect = { period ->
                        if (period == Period.CUSTOM) showRangePicker = true else vm.selectPeriod(period)
                    }
                )
            }

            status?.let { message ->
                item {
                    LedgerCard {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = vm::clearStatus) { Text("Dismiss") }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = selection.label,
                    trailing = if (rows.isEmpty()) null else "${rows.size} folded"
                )
            }

            if (rows.isEmpty()) {
                item {
                    LedgerCard {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No duplicates in this period", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "When a bank reports one transaction by text and by email, " +
                                    "the later report is listed here instead of counted twice.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(rows, key = { it.rawId }) { row ->
                    DuplicateCard(row, nepaliDates, onNotDuplicate = { vm.markNotDuplicate(row) })
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showRangePicker) {
        DateRangeDialog(
            onDismiss = { showRangePicker = false },
            onPick = { start, end ->
                vm.selectCustomRange(start, end)
                showRangePicker = false
            }
        )
    }
}

@Composable
private fun DuplicateCard(row: DuplicateRow, nepaliDates: Boolean, onNotDuplicate: () -> Unit) {
    val isCredit = row.direction == Direction.CREDIT
    LedgerCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                Money.formatSigned(row.amountMinor, isCredit),
                style = MaterialTheme.typography.titleMedium.merge(LocalTabularStyle.current),
                color = if (isCredit) AppTheme.finance.credit else AppTheme.finance.debit,
                fontWeight = FontWeight.SemiBold
            )
            MessageBlock(
                heading = "Duplicate: ${channel(row.source)} from ${row.sender}",
                at = relativeWhen(row.sentAt, nepali = nepaliDates),
                body = row.body
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MessageBlock(
                heading = "Counted as: " + if (row.originalSender == null) "a transaction"
                else "${channel(row.originalSource)} from ${row.originalSender}",
                at = relativeWhen(row.originalAt, nepali = nepaliDates),
                body = row.originalBody
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                OutlinedButton(onClick = onNotDuplicate, shape = PillShape) {
                    Text("Not a duplicate")
                }
            }
        }
    }
}

@Composable
private fun MessageBlock(heading: String, at: String, body: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(heading, style = MaterialTheme.typography.labelMedium)
        Text(
            at,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        body?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun channel(source: Source?): String = when (source) {
    Source.SMS -> "text message"
    Source.NOTIFICATION -> "app notification"
    null -> "message"
}
