package com.abi.expensetracker.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import com.abi.expensetracker.ui.theme.ChipShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.foundation.horizontalScroll
import kotlin.math.abs
import com.abi.expensetracker.ui.components.StatusChip
import com.abi.expensetracker.ui.components.AddFab
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.FilledTonalButton
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abi.expensetracker.data.CalendarDates
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.SplitSummary
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.LoanKind
import com.abi.expensetracker.ui.components.GroupPosition
import com.abi.expensetracker.ui.components.GroupedRow
import com.abi.expensetracker.ui.components.LedgerCard
import com.abi.expensetracker.ui.components.Monogram
import com.abi.expensetracker.ui.components.SectionHeader
import com.abi.expensetracker.ui.theme.AppTheme
import com.abi.expensetracker.ui.theme.LocalTabularStyle
import com.abi.expensetracker.ui.theme.PillShape
import java.time.Instant
import java.time.ZoneId

/**
 * Money lent to and borrowed from people: who owes whom, and how it got there.
 *
 * The list is people, not entries, because the question the screen answers is "who still
 * owes me"; the entries behind a balance are one tap further in.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoansScreen(vm: LoansViewModel = viewModel(), resetSignal: Int = 0) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nepaliDates by vm.useNepaliCalendar.collectAsStateWithLifecycle()
    var openPerson by rememberSaveable { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf(LoanFilter.ALL) }
    val shown = state.people.filter(filter.matches)
    /** The entry being edited, or a blank one being added. */
    var editing by remember { mutableStateOf<LoanEntry?>(null) }
    /** A split and the friend paying it in cash, while that dialog is open. */
    var cashPayment by remember { mutableStateOf<Pair<SplitSummary, String>?>(null) }

    val person = openPerson?.let { name ->
        state.people.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
    if (openPerson != null) BackHandler { openPerson = null }
    val listState = rememberLazyListState()
    // Tapping Loans while on it: back to the people list, All, top.
    OnTabReselect(resetSignal) {
        openPerson = null
        filter = LoanFilter.ALL
        listState.scrollToItem(0)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Same place and shape as the ledger's Add new, so adding reads the same everywhere.
        floatingActionButton = {
            AddFab(onClick = { editing = blankEntry(person?.name.orEmpty()) })
        },
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                title = {
                    Text(person?.name ?: "Loans", style = MaterialTheme.typography.headlineSmall)
                },
                navigationIcon = {
                    if (openPerson != null) {
                        IconButton(onClick = { openPerson = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to loans")
                        }
                    }
                },

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
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (person == null) {
                item {
                    LoanTotals(
                        state,
                        onFilter = { filter = if (filter == it) LoanFilter.ALL else it },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                if (state.people.isNotEmpty() || state.splits.isNotEmpty()) {
                    item {
                        // One scrollable line: five filters overflow a phone's width, and
                        // wrapping them would push the list down a second row.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp).horizontalScroll(rememberScrollState())
                        ) {
                            LoanFilter.entries.forEach { f ->
                                val count = if (f == LoanFilter.SPLITS) state.splits.size
                                else state.people.count(f.matches)
                                ChoicePill(
                                    label = "${f.label} · $count",
                                    selected = f == filter,
                                    onClick = { filter = f }
                                )
                            }
                        }
                    }
                }
                item {
                    SectionHeader(
                        title = filter.heading,
                        // What is left in this view, so "You owe" answers "how much" too.
                        trailing = when (filter) {
                            LoanFilter.OWED_TO_ME -> Money.format(shown.sumOf { it.balanceMinor })
                            LoanFilter.I_OWE -> Money.format(-shown.sumOf { it.balanceMinor })
                            LoanFilter.ALL -> if (shown.isEmpty()) null
                            else "${Money.format(abs(state.owedToMeMinor - state.iOweMinor))} net outstanding"
                            LoanFilter.SPLITS -> state.splits.sumOf { it.pendingMinor }
                                .takeIf { it > 0 }?.let { "${Money.format(it)} pending" }
                            else -> if (shown.isEmpty()) null else "${shown.size}"
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                if (filter == LoanFilter.SPLITS) {
                    if (state.splits.isEmpty()) item {
                        EmptyCard(
                            filter.emptyTitle,
                            "Open a bill you paid in the ledger and tap Split bill."
                        )
                    }
                    items(state.splits.size, key = { "split-" + state.splits[it].split.id }) { i ->
                        SplitCard(
                            state.splits[i],
                            onCashPayment = { person -> cashPayment = state.splits[i] to person },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                } else if (state.people.isEmpty()) {
                    item {
                        EmptyCard(
                            "No loans yet",
                            "Tap Add for cash, or open a bank transaction in the ledger and " +
                                "mark it as a loan so it stops counting as spending."
                        )
                    }
                } else if (shown.isEmpty()) {
                    item { EmptyCard(filter.emptyTitle, "Nothing in this view. Pick another filter above.") }
                }
                itemsIndexed(shown, key = { _, p -> p.name.lowercase() }) { index, p ->
                    PersonRow(p, nepaliDates, GroupPosition.of(index, shown.size)) { openPerson = p.name }
                }
                // All also lists the splits still waiting on someone, under their own header.
                val openSplits = state.splits.filterNot { it.isSettled }
                if (filter == LoanFilter.ALL && openSplits.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Splits",
                            trailing = "${openSplits.size} pending",
                            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
                        )
                    }
                    items(openSplits.size, key = { "open-split-" + openSplits[it].split.id }) { i ->
                        SplitCard(
                            openSplits[i],
                            onCashPayment = { who -> cashPayment = openSplits[i] to who },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            } else {
                item {
                    BalanceLine(person.balanceMinor, Modifier.padding(bottom = 16.dp))
                }
                item {
                    SectionHeader(
                        title = "History",
                        trailing = "${person.entries.size}",
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                itemsIndexed(person.entries, key = { _, e -> e.id }) { index, entry ->
                    EntryRow(entry, nepaliDates, GroupPosition.of(index, person.entries.size)) {
                        editing = entry
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    cashPayment?.let { (summary, friend) ->
        SplitPaymentDialog(
            splits = listOf(summary),
            fixedAmountMinor = null,
            initialPerson = friend,
            initialSplitId = summary.split.id,
            onDismiss = { cashPayment = null },
            onSave = { split, person, amount -> vm.recordCashPayment(split, person, amount); cashPayment = null }
        )
    }

    editing?.let { entry ->
        LoanEntryDialog(
            initial = entry,
            people = state.people.map { it.name },
            nepaliDates = nepaliDates,
            onDismiss = { editing = null },
            onSave = { vm.save(it); editing = null },
            onDelete = if (entry.id == 0L) null else ({ vm.delete(entry); editing = null })
        )
    }
}

/** Which people the list shows, by where their balance stands. */
private enum class LoanFilter(
    val label: String,
    val heading: String,
    val emptyTitle: String,
    val matches: (PersonLoans) -> Boolean
) {
    ALL("All", "Active balances", "Everyone is settled", { it.balanceMinor != 0L }),
    OWED_TO_ME("Owe you", "Owe you", "No one owes you", { it.balanceMinor > 0 }),
    I_OWE("You owe", "You owe", "You owe no one", { it.balanceMinor < 0 }),
    SETTLED("Settled", "Settled", "No settled loans", { it.balanceMinor == 0L }),
    /** Lists split bills rather than people; see [SplitCard]. */
    SPLITS("Splits", "Split bills", "No split bills", { false })
}

private fun blankEntry(person: String) = LoanEntry(
    person = person,
    kind = LoanKind.LENT,
    amountMinor = 0L,
    occurredAt = System.currentTimeMillis()
)

@Composable
private fun LoanTotals(state: LoansState, onFilter: (LoanFilter) -> Unit, modifier: Modifier = Modifier) {
    // Net first: "where do I stand overall" is the question; the tiles below explain it.
    val net = state.owedToMeMinor - state.iOweMinor
    val finance = AppTheme.finance
    val open = state.people.filter { it.balanceMinor != 0L }
    LedgerCard(modifier) {
        Column(Modifier.padding(20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "NET BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                when {
                    net > 0 -> StatusChip("In your favor", container = finance.creditSurface, content = finance.credit)
                    net < 0 -> StatusChip("You owe more", container = finance.debitSurface, content = finance.debit)
                }
            }
            run {
                val (lead, amount) = when {
                    net > 0 -> "You are owed " to Money.format(net)
                    net < 0 -> "You owe " to Money.format(-net)
                    // Zero net is not settled while someone still owes: the two sides
                    // only cancel out on paper.
                    state.owedToMeMinor > 0 -> "Even, " to "${Money.format(0)} net"
                    else -> "All settled" to ""
                }
                // One text run, so the words and the amount share a baseline.
                val amountColor = balanceColor(net)
                Text(
                    buildAnnotatedString {
                        append(lead)
                        withStyle(SpanStyle(color = amountColor, fontFeatureSettings = "tnum")) { append(amount) }
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            if (open.isNotEmpty()) {
                Text(
                    "Across ${open.size} " + if (open.size == 1) "person" else "people",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
            // Tapping a tile filters the list to the people behind it.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TotalTile(
                    Icons.Filled.ArrowDownward, "Owed to you", "+" + Money.format(state.owedToMeMinor), finance.credit,
                    state.people.count { it.balanceMinor > 0 },
                    Modifier.weight(1f)
                ) { onFilter(LoanFilter.OWED_TO_ME) }
                TotalTile(
                    Icons.Filled.ArrowUpward, "You owe", "\u2212" + Money.format(state.iOweMinor), finance.debit,
                    state.people.count { it.balanceMinor < 0 },
                    Modifier.weight(1f)
                ) { onFilter(LoanFilter.I_OWE) }
            }
        }
    }
}

@Composable
private fun TotalTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
    people: Int,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                amount,
                style = MaterialTheme.typography.titleLarge.merge(LocalTabularStyle.current),
                color = color
            )
            Text(
                "$people " + if (people == 1) "person" else "people",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "Owes you रु5,000", "You owe रु2,000", or "Settled" — words, never colour alone. */
private fun balanceText(balanceMinor: Long): String = when {
    balanceMinor > 0 -> "Owes you ${Money.format(balanceMinor)}"
    balanceMinor < 0 -> "You owe ${Money.format(-balanceMinor)}"
    else -> "Settled"
}

@Composable
private fun balanceColor(balanceMinor: Long) = when {
    balanceMinor > 0 -> AppTheme.finance.credit
    balanceMinor < 0 -> AppTheme.finance.debit
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun BalanceLine(balanceMinor: Long, modifier: Modifier = Modifier) {
    LedgerCard(modifier) {
        Text(
            balanceText(balanceMinor),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge.merge(LocalTabularStyle.current),
            color = balanceColor(balanceMinor),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PersonRow(person: PersonLoans, nepaliDates: Boolean, position: GroupPosition, onClick: () -> Unit) {
    val finance = AppTheme.finance
    GroupedRow(position = position, onClick = onClick) {
        Row(
            Modifier.padding(12.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Monogram(
                text = person.name,
                // Neutral for everyone: the balance text already says who owes whom.
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                content = MaterialTheme.colorScheme.onSurface
            )
            Column(Modifier.weight(1f)) {
                Text(person.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${person.entries.size} " + (if (person.entries.size == 1) "entry" else "entries") +
                        " · " + relativeWhen(person.lastActivity, nepali = nepaliDates).substringBefore(","),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                balanceText(person.balanceMinor),
                style = MaterialTheme.typography.titleSmall.merge(LocalTabularStyle.current),
                color = balanceColor(person.balanceMinor)
            )
            Icon(
                Icons.Filled.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EntryRow(entry: LoanEntry, nepaliDates: Boolean, position: GroupPosition, onClick: () -> Unit) {
    GroupedRow(position = position, onClick = onClick) {
        Row(
            Modifier.padding(12.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    // A split share reads as what it was for, not as a bare "Lent".
                    when {
                        entry.splitId == null -> entry.kind.label
                        entry.kind == LoanKind.LENT -> "Share of ${entry.note ?: "a split"}"
                        else -> "Paid for ${entry.note ?: "a split"}"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    buildString {
                        append(relativeWhen(entry.occurredAt, nepali = nepaliDates))
                        append(if (entry.txnId != null) " · Bank transaction" else " · Cash")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                entry.note?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(
                Money.formatSigned(entry.amountMinor, isCredit = !entry.kind.isOutflow),
                style = MaterialTheme.typography.titleSmall.merge(LocalTabularStyle.current),
                color = if (entry.kind.isOutflow) AppTheme.finance.debit else AppTheme.finance.credit,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    LedgerCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Add or edit one loan entry.
 *
 * The name is typed freely, with people already used offered as chips, or picked from the
 * phone's contacts. The system picker hands back only the contact chosen, so the app never
 * needs permission to read the whole address book.
 *
 * An entry tied to a bank transaction takes its amount and date from it, and may only be
 * a kind that matches the money's direction: a debit can lend or repay, never borrow.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LoanEntryDialog(
    initial: LoanEntry,
    people: List<String>,
    nepaliDates: Boolean,
    onDismiss: () -> Unit,
    onSave: (LoanEntry) -> Unit,
    onDelete: (() -> Unit)?,
    kinds: List<LoanKind> = LoanKind.entries
) {
    val linked = initial.txnId != null
    var person by remember { mutableStateOf(initial.person) }
    var kind by remember { mutableStateOf(initial.kind) }
    var amount by remember {
        mutableStateOf(if (initial.amountMinor > 0) Money.toPlainAmount(initial.amountMinor) else "")
    }
    var date by remember {
        mutableStateOf(Instant.ofEpochMilli(initial.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate())
    }
    var note by remember { mutableStateOf(initial.note.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val pickContact = rememberContactPicker { person = it }

    val suggestions = remember(person, people) {
        val typed = person.trim()
        people.filter {
            !it.equals(typed, ignoreCase = true) && (typed.isEmpty() || it.contains(typed, ignoreCase = true))
        }.take(6)
    }
    val amountMinor = Money.parseToMinor(amount)
    val valid = person.isNotBlank() && amountMinor != null && amountMinor > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = {
            Text(
                if (initial.id == 0L) (if (linked) "Mark as loan" else "Add loan") else "Edit loan",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text("Person") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    trailingIcon = {
                        IconButton(onClick = pickContact) {
                            Icon(Icons.Default.Person, contentDescription = "Pick from contacts")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { name ->
                            SuggestionChip(onClick = { person = name }, label = { Text(name) })
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    kinds.forEach { k ->
                        ChoicePill(label = k.label, selected = k == kind, onClick = { kind = k })
                    }
                }
                Text(
                    when (kind) {
                        LoanKind.LENT -> "You gave them money. They owe you."
                        LoanKind.RECEIVED_BACK -> "They returned money you lent."
                        LoanKind.BORROWED -> "They gave you money. You owe them."
                        LoanKind.PAID_BACK -> "You returned money you borrowed."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text(Money.RUPEE) },
                    singleLine = true,
                    enabled = !linked,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        CalendarDates.fullDateLabel(date, nepaliDates),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (!linked) {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text("Change date", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                if (linked) {
                    Text(
                        "Amount and date come from the bank transaction. It stays in the " +
                            "ledger but no longer counts as spent or received.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                onDelete?.let { delete ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TextButton(onClick = delete) {
                        Text(
                            if (linked) "Not a loan" else "Delete entry",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val at = if (linked) initial.occurredAt
                    // Midday, as manual expenses: the entry stays inside its day whichever
                    // way the timezone shifts later.
                    else date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onSave(
                        initial.copy(
                            person = person.trim(),
                            kind = kind,
                            amountMinor = amountMinor ?: 0L,
                            occurredAt = at,
                            note = note.trim().ifBlank { null }
                        )
                    )
                },
                enabled = valid,
                shape = PillShape
            ) { Text("Save") }
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
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = it.toUtcLocalDate() }
                    showDatePicker = false
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

/**
 * Opens the phone's contact picker and hands back the chosen name.
 *
 * The system picker grants access to the one contact picked, so the app never asks to
 * read the whole address book. Returns the action that opens it.
 */
@Composable
internal fun rememberContactPicker(onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()?.let(onPicked)
    }
    return { runCatching { launcher.launch(null) } }
}

/** One split bill: the total, the user's share, and each friend's part and what is left. */
@Composable
private fun SplitCard(summary: SplitSummary, onCashPayment: (String) -> Unit, modifier: Modifier = Modifier) {
    val split = summary.split
    val finance = AppTheme.finance
    LedgerCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Icon(
                        Icons.Filled.Restaurant, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(10.dp).size(20.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(split.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${Money.format(split.totalMinor)} total" +
                            if (split.myShareMinor > 0) " · Your share ${Money.format(split.myShareMinor)}" else "",
                        style = MaterialTheme.typography.bodySmall.merge(LocalTabularStyle.current),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (summary.isSettled) {
                    StatusChip("All paid", container = finance.creditSurface, content = finance.credit)
                } else {
                    StatusChip(
                        "${Money.format(summary.pendingMinor)} pending",
                        container = finance.debitSurface, content = finance.debit
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            summary.shares.forEach { share ->
                val paid = share.remainingMinor == 0L
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Monogram(
                        text = share.person,
                        modifier = Modifier.size(32.dp),
                        container = if (paid) finance.creditSurface else MaterialTheme.colorScheme.surfaceContainerHigh,
                        content = MaterialTheme.colorScheme.onSurface
                    )
                    Column(Modifier.weight(1f)) {
                        Text(share.person, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (paid) "Paid ${Money.format(share.paidMinor)}"
                            else "Owes ${Money.format(share.remainingMinor)} of ${Money.format(share.shareMinor)}",
                            style = MaterialTheme.typography.bodySmall.merge(LocalTabularStyle.current),
                            color = if (paid) finance.credit else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (paid) {
                        Surface(shape = ChipShape, color = finance.creditSurface) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle, contentDescription = null,
                                    tint = finance.credit, modifier = Modifier.size(14.dp)
                                )
                                Text("Paid", style = MaterialTheme.typography.labelMedium, color = finance.credit)
                            }
                        }
                    } else {
                        FilledTonalButton(onClick = { onCashPayment(share.person) }, shape = PillShape) {
                            Text("Paid cash")
                        }
                    }
                }
            }
        }
    }
}
