package com.abi.expensetracker.ui

import androidx.compose.material.icons.outlined.ContentCopy
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abi.expensetracker.data.CalendarDates
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.Period
import com.abi.expensetracker.data.SplitSummary
import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.LoanKind
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Source
import com.abi.expensetracker.ui.components.*
import com.abi.expensetracker.ui.theme.AppTheme
import com.abi.expensetracker.ui.theme.ChipShape
import com.abi.expensetracker.ui.theme.LocalTabularStyle
import com.abi.expensetracker.ui.theme.PillShape
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    /** Driven by the add button on the bottom bar, which is outside this screen. */
    showAddDialog: Boolean,
    onAddDialogClose: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val rows by vm.rows.collectAsStateWithLifecycle()
    val spent by vm.spentMinor.collectAsStateWithLifecycle()
    val received by vm.receivedMinor.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val limitStatus by vm.limitStatus.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val nepaliDates by vm.useNepaliCalendar.collectAsStateWithLifecycle()

    val hasSmsPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }
    var editing by remember { mutableStateOf<com.abi.expensetracker.data.model.Txn?>(null) }
    var showRangePicker by remember { mutableStateOf(false) }
    var showDuplicates by remember { mutableStateOf(false) }
    val loans by vm.loans.collectAsStateWithLifecycle()
    val duplicateCount by vm.duplicateCount.collectAsStateWithLifecycle()
    var loanDraft by remember { mutableStateOf<LoanEntry?>(null) }
    val splits by vm.splits.collectAsStateWithLifecycle()
    var splitOpen by remember { mutableStateOf(false) }
    var sharePaymentOpen by remember { mutableStateOf(false) }
    // Ids, not rows, so a row that refreshes underneath stays selected. Only the ids still
    // on screen count, so switching period cannot delete rows the user can no longer see.
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val selectedTxns = rows.map { it.txn }.filter { it.id in selectedIds }
    val selecting = selectedTxns.isNotEmpty()
    var confirmDelete by remember { mutableStateOf(false) }
    fun toggle(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    BackHandler(enabled = selecting) { selectedIds = emptySet() }

    // Drawn in place of the ledger rather than as a tab: it is a review queue reached from
    // the ledger, and the bottom bar stays where it is.
    if (showDuplicates) {
        DuplicatesScreen(onBack = { showDuplicates = false })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selecting) {
                // The contextual bar is a dark neutral block in either theme, so selection
                // mode is unmistakable at a glance.
                val cabColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF242424) else Color(0xFF191C1D)
                TopAppBar(
                    expandedHeight = 52.dp,
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection")
                        }
                    },
                    title = {
                        Text("${selectedTxns.size} selected", style = MaterialTheme.typography.titleLarge)
                    },
                    actions = {
                        TextButton(onClick = { selectedIds = rows.map { it.txn.id }.toSet() }) {
                            Text("Select all", color = Color.White)
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = AppTheme.finance.debit
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = cabColor,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            } else {
                TopAppBar(
                    expandedHeight = 52.dp,
                    title = { Text("Ledger", style = MaterialTheme.typography.headlineSmall) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            // No global gap: transaction rows butt together to read as one card, and
            // everything else carries its own bottom padding instead.
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OfflineBadge()
                    AssistChip(
                        onClick = { showDuplicates = true },
                        label = { Text("Duplicates ($duplicateCount)") },
                        leadingIcon = {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, Modifier.size(16.dp))
                        },
                        shape = ChipShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
            item {
                Box(Modifier.padding(bottom = 12.dp)) { PeriodChips(
                    selected = selection.period,
                    onSelect = { period ->
                        if (period == Period.CUSTOM) showRangePicker = true else vm.selectPeriod(period)
                    }
                ) }
            }

            item {
                PeriodHeroCard(
                    // A single day also names its date, on the calendar the user reads.
                    periodLabel = when (selection.period) {
                        Period.TODAY -> "Today · " + CalendarDates.fullDateLabel(LocalDate.now(), nepaliDates)
                        Period.YESTERDAY -> "Yesterday · " +
                            CalendarDates.fullDateLabel(LocalDate.now().minusDays(1), nepaliDates)
                        else -> selection.label
                    },
                    spentMinor = spent,
                    receivedMinor = received,
                    // Measured over the limit's own window, not the selected period: a
                    // monthly limit means nothing against whatever range the chips show.
                    limitStatus = limitStatus,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (!hasSmsPermission) {
                item {
                  Box(Modifier.padding(bottom = 12.dp)) {
                    NudgeBanner(
                        title = "SMS access needed",
                        subtitle = "Grant it in Settings to start reading bank messages",
                        onClick = onOpenSettings
                    )
                  }
                }
            }

            status?.let { message ->
                item {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Info, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                            )
                            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = vm::clearStatus) { Text("Dismiss") }
                        }
                    }
                }
            }

            item {
                Box(Modifier.padding(bottom = 12.dp)) {
                    SectionHeader(
                        title = "Transactions",
                        trailing = if (rows.isEmpty()) null else "${rows.size} " + if (rows.size == 1) "item" else "items"
                    )
                }
            }

            if (rows.isEmpty()) {
                item {
                    LedgerCard(Modifier.padding(bottom = 12.dp)) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Nothing in this period",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Add an expense, or sync your messages from Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(rows, key = { _, row -> row.txn.id }) { index, row ->
                    TransactionTile(
                        nepaliDates = nepaliDates,
                        row = row,
                        position = GroupPosition.of(index, rows.size),
                        selected = row.txn.id in selectedIds,
                        // Once anything is selected a tap extends the selection; editing
                        // waits until the selection is cleared.
                        onClick = { if (selecting) toggle(row.txn.id) else editing = row.txn },
                        onLongClick = { toggle(row.txn.id) }
                    )
                }
            }

            item { OfflineFooter(Modifier.padding(top = 16.dp)) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (confirmDelete && selecting) {
        val count = selectedTxns.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text(if (count == 1) "Delete transaction?" else "Delete $count transactions?") },
            text = {
                Text(
                    // A parsed row is rebuilt on reparse unless its message is flagged, so
                    // saying it stays gone is the promise the flag keeps.
                    "This removes them from the ledger. Their messages are kept, but will " +
                        "not be booked again on sync or reparse.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.delete(selectedTxns)
                        selectedIds = emptySet()
                        confirmDelete = false
                    },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    if (showAddDialog) {
        ExpenseDialog(
            nepaliDates = nepaliDates,
            title = "Add expense",
            confirmLabel = "Add",
            onDismiss = onAddDialogClose,
            onConfirm = { amount, date, direction, remark ->
                vm.addManualExpense(amount, date, direction, remark)
                onAddDialogClose()
            }
        )
    }

    editing?.let { txn ->
        var categoryPicked by remember(txn.id) { mutableStateOf(false) }
        val sources by produceState(emptyList<RawMessage>(), txn.id) {
            value = vm.sourceMessages(txn)
        }
        val loan = loans.firstOrNull { it.txnId == txn.id }
        val split = splits.firstOrNull { it.split.txnId == txn.id }
        val people = loans.map { it.person.trim() }.distinctBy { it.lowercase() }
        if (splitOpen) {
            SplitBillDialog(
                totalMinor = txn.amountMinor,
                initialTitle = txn.merchant ?: txn.remark ?: "Bill",
                existing = split,
                people = people,
                onDismiss = { splitOpen = false },
                onSave = { title, mine, shares -> vm.saveSplit(txn, title, mine, shares); splitOpen = false },
                onDelete = split?.let { s -> { vm.deleteSplit(s.split); splitOpen = false } }
            )
        }
        if (sharePaymentOpen) {
            SplitPaymentDialog(
                splits = splits,
                fixedAmountMinor = txn.amountMinor,
                onDismiss = { sharePaymentOpen = false },
                onSave = { s, person, amount ->
                    vm.recordSplitPayment(s, person, amount, txn); sharePaymentOpen = false
                }
            )
        }
        if (loanDraft != null) {
            LoanEntryDialog(
                initial = loanDraft!!,
                people = loans.map { it.person.trim() }.distinctBy { it.lowercase() },
                nepaliDates = nepaliDates,
                kinds = LoanKind.forDirection(txn.direction),
                onDismiss = { loanDraft = null },
                onSave = { vm.saveLoan(it); loanDraft = null },
                onDelete = loan?.let { existing -> { vm.deleteLoan(existing); loanDraft = null } }
            )
        }
        ExpenseDialog(
            nepaliDates = nepaliDates,
            title = "Edit transaction",
            categories = categories,
            initialCategoryId = txn.categoryId,
            onCategoryChange = { categoryPicked = true; vm.setCategory(txn, it) },
            sourceMessages = sources,
            extras = {
                TxnLinkControls(
                    txn = txn,
                    loan = loan,
                    split = split,
                    onLoan = {
                        loanDraft = loan ?: LoanEntry(
                            person = "",
                            kind = LoanKind.forDirection(txn.direction).first(),
                            amountMinor = txn.amountMinor,
                            occurredAt = txn.occurredAt,
                            txnId = txn.id
                        )
                    },
                    onSplit = { splitOpen = true },
                    onSharePayment = { sharePaymentOpen = true }
                )
            },
            confirmLabel = "Save",
            initialAmount = Money.toPlainAmount(txn.amountMinor),
            initialRemark = txn.remark.orEmpty(),
            initialDirection = txn.direction,
            initialDate = Instant.ofEpochMilli(txn.occurredAt)
                .atZone(ZoneId.systemDefault()).toLocalDate(),
            // Only a parsed row can drift from its message; saying so is what makes the
            // "kept through a reparse" promise visible where the edit is made.
            note = if (txn.isManual) null
            else "Parsed from a message. Your edit is kept when messages are reparsed.",
            onDismiss = { editing = null },
            onConfirm = { amount, date, direction, remark ->
                // A category picked in this dialog is the user's; keywords leave it be.
                vm.editTransaction(txn, amount, date, direction, remark, autoCategorize = !categoryPicked)
                editing = null
            }
        )
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
private fun OfflineBadge() {
    Surface(
        shape = ChipShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle, contentDescription = null,
                tint = AppTheme.finance.credit, modifier = Modifier.size(14.dp)
            )
            Text(
                "Offline storage",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodChips(selected: Period, onSelect: (Period) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Period.entries.forEach { period ->
            LedgerChip(
                label = if (period == Period.CUSTOM) "Date range" else period.label,
                selected = selected == period,
                onClick = { onSelect(period) }
            )
        }
    }
}

@Composable
private fun TransactionTile(
    nepaliDates: Boolean,
    row: TxnRow,
    position: GroupPosition,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val txn = row.txn
    val finance = AppTheme.finance
    val isCredit = txn.direction == Direction.CREDIT
    val when_ = remember(txn.occurredAt, nepaliDates) {
        relativeWhen(txn.occurredAt, nepali = nepaliDates)
    }

    // The source is the bank the user linked — never the raw sender id, which means
    // nothing to anyone reading their own ledger.
    val source = when {
        row.isManual -> "Added by you"
        row.bankName != null -> row.bankName
        else -> "Unlinked sender"
    }
    val statusLabel = when {
        row.loan?.splitId != null -> "Share"
        row.loan != null -> "Loan"
        row.split != null -> "Split"
        row.isManual -> "Manual"
        txn.userEdited -> "Edited"
        txn.needsReview -> "Review"
        else -> "Parsed"
    }

    // The row is the whole affordance: tap to edit, hold to select for deleting. Buttons
    // would crowd a space that is already two lines of text and an amount.
    GroupedRow(position = position, onClick = onClick, onLongClick = onLongClick, selected = selected) {
        Row(
            Modifier.padding(12.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selected) {
                // Same footprint as the monogram, so selecting does not shift the row.
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            } else {
                DirectionalMonogram(
                    text = txn.merchant ?: txn.remark ?: "?",
                    isCredit = isCredit,
                    // What the money went on, then who it went through, then the initial.
                    // The category is the most useful of the three at a glance, and the one
                    // that repeats across banks.
                    glyph = row.categoryIcon ?: row.bankIcon
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // The remark stands in as the title when the message named no merchant:
                // "Khaja" says more about the row than "Unknown" ever does.
                Text(
                    txn.merchant ?: txn.remark ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                txn.remark?.takeIf { it != txn.merchant && txn.merchant != null }?.let { note ->
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    buildString {
                        append(source)
                        // Who the loan is with says more than the bank does once it is one.
                        row.loan?.let { append(" · ${it.kind.label} · ${it.person}") }
                        row.split?.let {
                            append(if (it.isSettled) " · Split, all paid" else " · Split · ${Money.format(it.pendingMinor)} pending")
                        }
                        append(" · ")
                        append(when_)
                        txn.accountTail?.let { append(" · ··$it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    Money.formatSigned(txn.amountMinor, isCredit),
                    // Tabular figures: this is a right-aligned column, and proportional
                    // digits make the decimal points wander from row to row.
                    style = MaterialTheme.typography.titleMedium
                        .merge(LocalTabularStyle.current),
                    color = if (isCredit) finance.credit else finance.debit,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                // Rows that are something other than plain spending, or need a look,
                // wear the debit tint; the rest stay neutral.
                val flagged = statusLabel in setOf("Review", "Loan", "Split", "Share")
                StatusChip(
                    text = statusLabel,
                    container = if (flagged) finance.debitSurface
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = if (flagged) finance.debit
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * One dialog for adding an expense and for correcting an existing one.
 *
 * The same fields either way, so an edit can reach everything the add form can set.
 *
 * There is no merchant field: what a transaction was for is written in "Spent on", and a
 * merchant parsed out of a bank message is left as the message stated it.
 */
/**
 * "Today, 2:15 PM", "Yesterday, 4:45 PM", or "20 Sep, 1:20 PM".
 *
 * A bank message usually arrives within seconds of the swipe, so the time of day is the
 * part that tells one of today's three coffees from another. The year is dropped: in a
 * list already filtered to a period it is the same on every row.
 */
internal fun relativeWhen(
    millis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    nepali: Boolean = false
): String {
    val moment = Instant.ofEpochMilli(millis).atZone(zone)
    val date = moment.toLocalDate()
    val today = LocalDate.now(zone)
    val day = when (date) {
        // Today and Yesterday are the same day on either calendar, and naming them beats
        // any date at all — the Nepali date for them is one tap away in the editor.
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> CalendarDates.dayLabel(date, nepali)
    }
    val time = moment.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    return "$day, $time"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDialog(
    nepaliDates: Boolean,
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: String,
        date: LocalDate,
        direction: Direction,
        remark: String
    ) -> Unit,
    categories: List<Category> = emptyList(),
    initialCategoryId: Long? = null,
    onCategoryChange: (Long?) -> Unit = {},
    sourceMessages: List<RawMessage> = emptyList(),
    /** Loan and split controls for an existing transaction, drawn under the category. */
    extras: (@Composable () -> Unit)? = null,
    initialAmount: String = "",
    initialRemark: String = "",
    initialDirection: Direction = Direction.DEBIT,
    initialDate: LocalDate = LocalDate.now(),
    note: String? = null
) {
    var amount by remember { mutableStateOf(initialAmount) }
    var remark by remember { mutableStateOf(initialRemark) }
    var direction by remember { mutableStateOf(initialDirection) }
    var date by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }


    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            // Scrolls because a row reported on several channels shows every message.
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    // The rupee sign is a fixed adornment, not something to retype.
                    prefix = {
                        Text(Money.RUPEE, style = MaterialTheme.typography.titleLarge)
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.headlineSmall,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = { Text("Spent on") },
                    placeholder = { Text("e.g. Auto fare") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill(
                        label = "Spent",
                        selected = direction == Direction.DEBIT,
                        onClick = { direction = Direction.DEBIT }
                    )
                    ChoicePill(
                        label = "Received",
                        selected = direction == Direction.CREDIT,
                        onClick = { direction = Direction.CREDIT }
                    )
                }

                // Saved on tap rather than with the rest of the form: the category is the
                // one field a keyword guess also writes, and applying it immediately is
                // what marks the row as hand-set so the guess never comes back over it.
                if (categories.isNotEmpty()) {
                    var chosen by remember(initialCategoryId) {
                        mutableStateOf(initialCategoryId)
                    }
                    Text(
                        "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val label: (Category?) -> String = { category ->
                        category?.let { "${it.icon} ${it.name}" } ?: "No category"
                    }
                    SearchableDropdown(
                        selectedLabel = label(categories.firstOrNull { it.id == chosen }),
                        // A leading null row is the way back to "no category".
                        items = listOf<Category?>(null) + categories,
                        itemLabel = label,
                        onSelect = { category ->
                            chosen = category?.id
                            onCategoryChange(chosen)
                        },
                        placeholder = "Search categories",
                        emptyText = "No category matches that",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                extras?.invoke()
                sourceMessages.forEachIndexed { index, message ->
                    SourceMessage(message, isCopy = index > 0)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        CalendarDates.fullDateLabel(date, nepaliDates),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Change date", style = MaterialTheme.typography.labelMedium)
                    }
                }
                note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount, date, direction, remark) },
                enabled = amount.isNotBlank(),
                shape = PillShape
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // The picker reports UTC midnight; read it back as a calendar date
                        // so a date picked east or west of UTC is not off by a day.
                        state.selectedDateMillis?.let { date = it.toUtcLocalDate() }
                        showDatePicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

/**
 * What a transaction is besides spending or income: a loan, a split bill, or a friend
 * paying their share of one. Each takes the row out of the plain totals in its own way,
 * so only one applies at a time.
 */
@Composable
private fun TxnLinkControls(
    txn: com.abi.expensetracker.data.model.Txn,
    loan: LoanEntry?,
    split: SplitSummary?,
    onLoan: () -> Unit,
    onSplit: () -> Unit,
    onSharePayment: () -> Unit
) {
    when {
        loan != null && loan.splitId != null -> LinkLine(
            "Share of \"${loan.note ?: "a split"}\" from ${loan.person}", "Change", onLoan
        )
        loan != null -> LinkLine("Loan: ${loan.kind.label.lowercase()} · ${loan.person}", "Change", onLoan)
        split != null -> LinkLine(
            "Split with ${split.shares.size} · " +
                if (split.isSettled) "all paid" else "${Money.format(split.pendingMinor)} pending",
            "Edit", onSplit
        )
        else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (txn.direction == Direction.DEBIT) {
                OutlinedButton(onClick = onSplit, shape = PillShape) { Text("Split bill") }
            } else {
                OutlinedButton(onClick = onSharePayment, shape = PillShape) { Text("Share of split") }
            }
            OutlinedButton(onClick = onLoan, shape = PillShape) { Text("Mark as loan") }
        }
    }
}

@Composable
private fun LinkLine(text: String, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action) }
    }
}

/**
 * The message a parsed row was read from, verbatim, so a wrong amount or merchant can be
 * checked against what the bank actually sent without leaving the editor.
 */
@Composable
private fun SourceMessage(message: RawMessage, isCopy: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val via = when (message.source) {
            Source.SMS -> "Text message"
            Source.NOTIFICATION -> "App notification"
        }
        Text(
            // A copy is the same transaction reported again on another channel; saying
            // so explains why its amount is not counted twice.
            if (isCopy) "Also reported by ${via.lowercase()} from ${message.sender}, not counted again"
            else "$via from ${message.sender}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Capped and scrollable: a long notification would otherwise push the
            // Save button off the dialog. Selectable, so a ref number can be copied.
            SelectionContainer(
                Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState())
            ) {
                Text(
                    message.body,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit) =
    LedgerChip(label = label, selected = selected, onClick = onClick)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateRangeDialog(onDismiss: () -> Unit, onPick: (LocalDate, LocalDate) -> Unit) {
    val state = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis ?: start
                    if (start != null && end != null) {
                        // The picker reports UTC midnight; read it back as a calendar date
                        // so a range picked east or west of UTC is not off by a day.
                        onPick(start.toUtcLocalDate(), end.toUtcLocalDate())
                    }
                },
                enabled = state.selectedStartDateMillis != null
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DateRangePicker(state = state, showModeToggle = false, modifier = Modifier.weight(1f))
    }
}

internal fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate()
