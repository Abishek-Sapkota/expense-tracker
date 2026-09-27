package com.abi.expensetracker.ui

import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.outlined.ContentCopy
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abi.expensetracker.data.CalendarDates
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.Period
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.LoanKind
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.ui.components.*
import com.abi.expensetracker.ui.theme.AppTheme
import com.abi.expensetracker.ui.theme.LocalTabularStyle
import com.abi.expensetracker.ui.theme.PillShape
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the Trends drill-down shows: one category over one month. */
data class CategoryView(
    val title: String,
    val subtitle: String,
    val totalMinor: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    /** Driven by the add button on the bottom bar, which is outside this screen. */
    showAddDialog: Boolean,
    onAddDialogClose: () -> Unit,
    vm: HomeViewModel = viewModel(),
    /** Set for the Trends drill-down: a titled, filtered ledger with a back arrow. */
    categoryView: CategoryView? = null,
    onBack: () -> Unit = {},
    resetSignal: Int = 0
) {
    val context = LocalContext.current
    val rows by vm.rows.collectAsStateWithLifecycle()
    val spent by vm.spentMinor.collectAsStateWithLifecycle()
    val received by vm.receivedMinor.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val limitStatus by vm.limitStatus.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val banks by vm.banks.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val nepaliDates by vm.useNepaliCalendar.collectAsStateWithLifecycle()

    val hasSmsPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }
    // The id, saveable, so the editor survives a rotation; the row is looked up afresh.
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = remember(rows, editingId) { rows.firstOrNull { it.txn.id == editingId }?.txn }
    var showRangePicker by rememberSaveable { mutableStateOf(false) }
    // Saveable, so leaving the tab and coming back finds Duplicates still open.
    var showDuplicates by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val loans by vm.loans.collectAsStateWithLifecycle()
    val duplicateCount by vm.duplicateCount.collectAsStateWithLifecycle()
    var loanDraft by remember { mutableStateOf<LoanEntry?>(null) }
    val splits by vm.splits.collectAsStateWithLifecycle()
    var splitOpen by rememberSaveable { mutableStateOf(false) }
    var sharePaymentOpen by rememberSaveable { mutableStateOf(false) }
    // Ids, not rows, so a row that refreshes underneath stays selected. Only the ids still
    // on screen count, so switching period cannot delete rows the user can no longer see.
    var selectedIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    // Grouped once per new list, not on every recomposition of the list builder.
    val days = remember(rows) {
        rows.groupBy {
            Instant.ofEpochMilli(it.txn.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    val selectedTxns = remember(rows, selectedIds) { rows.map { it.txn }.filter { it.id in selectedIds } }
    val selecting = selectedTxns.isNotEmpty()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    fun toggle(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    BackHandler(enabled = selecting) { selectedIds = emptySet() }
    BackHandler(enabled = categoryView != null && !selecting, onBack = onBack)

    // Tapping Ledger while on it returns it to how it opens: Today, top of the list.
    val query by vm.query.collectAsStateWithLifecycle()
    val searching = query != null && categoryView == null
    BackHandler(enabled = searching && !selecting) { vm.closeSearch() }

    OnTabReselect(resetSignal) {
        vm.closeSearch()
        showDuplicates = false
        selectedIds = emptySet()
        vm.selectPeriod(Period.TODAY)
        listState.scrollToItem(0)
    }

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
            } else if (searching) {
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }
                TopAppBar(
                    expandedHeight = 52.dp,
                    navigationIcon = {
                        IconButton(onClick = vm::closeSearch) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    title = {
                        TextField(
                            value = query.orEmpty(),
                            onValueChange = vm::setQuery,
                            placeholder = { Text("Search merchant, remark or amount") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Search
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus)
                        )
                    },
                    actions = {
                        if (!query.isNullOrEmpty()) {
                            IconButton(onClick = { vm.setQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            } else {
                TopAppBar(
                    expandedHeight = 52.dp,
                    title = { Text(categoryView?.title ?: "Ledger", style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        if (categoryView != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    // Only when this period folded something: a permanent "Duplicates (0)"
                    // took a whole row above the chips to say there was nothing to review.
                    actions = {
                        if (categoryView == null) {
                            IconButton(onClick = vm::openSearch) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                        if (categoryView == null && duplicateCount > 0) {
                            IconButton(onClick = { showDuplicates = true }) {
                                BadgedBox(badge = { Badge { Text("$duplicateCount") } }) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicates")
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            // No global gap: transaction rows butt together to read as one card, and
            // everything else carries its own bottom padding instead.
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (categoryView != null) item {
                SectionHeader(
                    title = categoryView.subtitle,
                    trailing = Money.format(categoryView.totalMinor),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            // Searching spans all time, so the period chips and its totals step aside.
            if (searching) item {
                SectionHeader(
                    title = if (query.isNullOrBlank()) "Type to search every transaction"
                    else "${rows.size}${if (rows.size >= 300) "+" else ""} found",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            if (categoryView == null && !searching) item {
                Box(Modifier.padding(bottom = 12.dp)) { PeriodChips(
                    selected = selection.period,
                    onSelect = { period ->
                        if (period == Period.CUSTOM) showRangePicker = true else vm.selectPeriod(period)
                    }
                ) }
            }

            if (categoryView == null && !searching) item {
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

            if (!hasSmsPermission && categoryView == null) {
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


            if (rows.isEmpty() && searching) {
                if (!query.isNullOrBlank()) item {
                    Text(
                        "No transaction matches \"${query.orEmpty().trim()}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else if (rows.isEmpty()) {
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
                // One grouped card per day under a date header, days separated by a gap.
                // Display only: the data stays one list, so totals and selection are
                // unaffected.
                days.forEach { (day, dayRows) ->
                    item(key = "day-$day") {
                        DayHeader(day, dayRows, nepaliDates)
                    }
                    itemsIndexed(dayRows, key = { _, row -> row.txn.id }) { index, row ->
                    TransactionTile(
                        nepaliDates = nepaliDates,
                        row = row,
                        position = GroupPosition.of(index, dayRows.size),
                        selected = row.txn.id in selectedIds,
                        // Once anything is selected a tap extends the selection; editing
                        // waits until the selection is cleared.
                        onClick = { if (selecting) toggle(row.txn.id) else editingId = row.txn.id },
                        onLongClick = { toggle(row.txn.id) }
                    )
                    }
                }
            }

            // Tall enough that the last row scrolls clear of the Add button.
            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    if (confirmDelete && selecting) {
        val count = selectedTxns.size
        val selectedRows = rows.filter { it.txn.id in selectedIds }
        val hasSplit = selectedRows.any { it.split != null }
        val hasLoan = selectedRows.any { it.loan != null }
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
                        "not be booked again on sync or reparse." +
                        // Said up front because both change what Loans shows.
                        (if (hasSplit) " Split bills among them are removed, with what friends owe on them." else "") +
                        (if (hasLoan) " Loans linked to them stay in Loans as cash entries." else ""),
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
            accounts = banks,
            onDismiss = onAddDialogClose,
            onConfirm = { amount, date, direction, remark, bankId ->
                vm.addManualExpense(amount, date, direction, remark, bankId)
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
            onCreateCategory = { name, done -> vm.createCategory(name, done) },
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
            // A parsed row's account is its sender's; only a manual one is picked here.
            accounts = if (txn.isManual) banks else null,
            initialBankId = txn.bankId,
            onDismiss = { editingId = null },
            onConfirm = { amount, date, direction, remark, bankId ->
                // A category picked in this dialog is the user's; keywords leave it be.
                vm.editTransaction(
                    txn, amount, date, direction, remark,
                    autoCategorize = !categoryPicked, bankId = bankId
                )
                editingId = null
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
    // A manual row keeps saying so even with an account picked: it was typed in, not
    // read from the bank, and that is worth seeing when the numbers disagree.
    val source = when {
        row.isManual && row.bankName != null -> "${row.bankName} · Added by you"
        row.isManual -> "Added by you"
        row.bankName != null -> row.bankName
        else -> "Unlinked sender"
    }
    // Only rows that are something other than plain spending get a tag. "Parsed",
    // "Edited" and "Manual" were on nearly every row and changed nothing the reader does.
    val statusLabel = when {
        row.loan?.splitId != null -> "Share"
        row.loan != null -> "Loan"
        row.split != null -> "Split"
        txn.needsReview -> "Review"
        else -> null
    }
    // A loan row with nothing typed on it is named by who it is with, not "Unknown".
    val loanTitle = row.loan?.let { loanPhrase(it) }
    val title = txn.merchant ?: txn.remark ?: loanTitle ?: row.categoryName ?: "Unknown"

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
                    modifier = Modifier.size(40.dp)
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
                    text = title,
                    isCredit = isCredit,
                    // The account's own icon (usually its app icon), else the initial.
                    glyph = row.bankIcon
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // The remark stands in as the title when the message named no merchant:
                // "Khaja" says more about the row than "Unknown" ever does.
                Text(
                    // With no title at all, the loan or the category names the row.
                    title,
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
                        if (title != loanTitle) row.loan?.let { append(" · ${loanPhrase(it)}") }
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
                statusLabel?.let {
                    StatusChip(text = it, container = finance.debitSurface, content = finance.debit)
                }
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
/**
 * "Today", "Yesterday" or the date, with that day's net on the right: the header that
 * opens each day's group in the ledger.
 */
@Composable
private fun DayHeader(day: LocalDate, rows: List<TxnRow>, nepaliDates: Boolean) {
    val today = LocalDate.now()
    val label = when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> CalendarDates.dayLabel(day, nepaliDates)
    }
    // Loans are left out, as in every total; the rows still list them.
    val spent = rows.filter { it.txn.direction == Direction.DEBIT && it.loan == null }.sumOf { it.txn.amountMinor }
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (spent > 0) {
            Text(
                Money.format(spent) + " spent",
                style = MaterialTheme.typography.labelMedium.merge(LocalTabularStyle.current),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "Lent to Asha", "Borrowed from Asha": the loan read as a sentence. */
private fun loanPhrase(loan: LoanEntry): String = when (loan.kind) {
    LoanKind.LENT -> "Lent to ${loan.person}"
    LoanKind.RECEIVED_BACK -> "Got back from ${loan.person}"
    LoanKind.BORROWED -> "Borrowed from ${loan.person}"
    LoanKind.PAID_BACK -> "Paid back to ${loan.person}"
}

/** Built once: a formatter per row per recomposition was measurable garbage while scrolling. */
private val TIME_OF_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

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
    val time = moment.format(TIME_OF_DAY)
    return "$day, $time"
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
