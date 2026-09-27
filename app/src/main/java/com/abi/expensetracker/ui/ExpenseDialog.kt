package com.abi.expensetracker.ui

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.abi.expensetracker.data.CalendarDates
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.SplitSummary
import com.abi.expensetracker.data.model.Bank
import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Source
import com.abi.expensetracker.ui.components.*
import com.abi.expensetracker.ui.theme.PillShape
import java.time.LocalDate
import java.time.ZoneId

/*
 * The add/edit transaction dialog and the pieces only it draws, kept apart from the
 * ledger screen that opens it.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseDialog(
    nepaliDates: Boolean,
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: String,
        date: LocalDate,
        direction: Direction,
        remark: String,
        bankId: Long?
    ) -> Unit,
    /** The accounts to pick from; null hides the picker (a parsed row's is its sender's). */
    accounts: List<Bank>? = null,
    initialBankId: Long? = null,
    categories: List<Category> = emptyList(),
    initialCategoryId: Long? = null,
    onCategoryChange: (Long?) -> Unit = {},
    /** Creates a category from the typed name and hands back its id. */
    onCreateCategory: ((String, (Long) -> Unit) -> Unit)? = null,
    sourceMessages: List<RawMessage> = emptyList(),
    /** Loan and split controls for an existing transaction, drawn under the category. */
    extras: (@Composable () -> Unit)? = null,
    initialAmount: String = "",
    initialRemark: String = "",
    initialDirection: Direction = Direction.DEBIT,
    initialDate: LocalDate = LocalDate.now(),
    note: String? = null
) {
    var amount by rememberSaveable { mutableStateOf(initialAmount) }
    var remark by rememberSaveable { mutableStateOf(initialRemark) }
    var direction by rememberSaveable { mutableStateOf(initialDirection) }
    var date by rememberSaveable { mutableStateOf(initialDate) }
    var bankId by rememberSaveable { mutableStateOf(initialBankId) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }


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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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

                if (accounts != null) {
                    Text(
                        if (direction == Direction.DEBIT) "Paid from" else "Received into",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val label: (Bank?) -> String = { bank -> bank?.name ?: "Cash / none" }
                    SearchableDropdown(
                        selectedLabel = label(accounts.firstOrNull { it.id == bankId }),
                        // The leading null row is cash, or an account not worth naming.
                        items = listOf<Bank?>(null) + accounts,
                        itemLabel = label,
                        onSelect = { bankId = it?.id },
                        placeholder = "Search accounts",
                        emptyText = "No account matches that",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Saved on tap rather than with the rest of the form: the category is the
                // one field a keyword guess also writes, and applying it immediately is
                // what marks the row as hand-set so the guess never comes back over it.
                if (categories.isNotEmpty() || onCreateCategory != null) {
                    var chosen by remember(initialCategoryId) {
                        mutableStateOf(initialCategoryId)
                    }
                    Text(
                        "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val label: (Category?) -> String = { category ->
                        category?.name ?: "No category"
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
                        placeholder = "Search or create a category",
                        emptyText = "No category matches that",
                        // Creating from here saves a trip to Settings mid-edit; the new
                        // category is picked for this transaction as soon as it exists.
                        onCreate = onCreateCategory?.let { create ->
                            { name ->
                                create(name) { id ->
                                    chosen = id
                                    onCategoryChange(id)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                extras?.invoke()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        // The year only when it is not this one, so the date fits one line.
                        if (date.year == LocalDate.now().year) CalendarDates.dayLabel(date, nepaliDates)
                        else CalendarDates.fullDateLabel(date, nepaliDates),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Change date", style = MaterialTheme.typography.labelMedium)
                    }
                }
                // Folded away: the bank's text is for checking a wrong amount, which is the
                // rare edit, and it pushed the fields most edits touch into a scroll.
                if (sourceMessages.isNotEmpty()) {
                    var showMessages by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { showMessages = !showMessages },
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        Text(
                            (if (showMessages) "Hide" else "Show") + " original message" +
                                if (sourceMessages.size > 1) "s (${sourceMessages.size})" else "",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (showMessages) {
                        sourceMessages.forEachIndexed { index, message ->
                            SourceMessage(message, isCopy = index > 0)
                        }
                        note?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amount, date, direction, remark, bankId) },
                // Only an amount that will actually save: a bad one used to close the
                // dialog with a status line, and everything else typed was lost.
                enabled = Money.parseToMinor(amount)?.let { it > 0 } == true,
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
internal fun TxnLinkControls(
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
