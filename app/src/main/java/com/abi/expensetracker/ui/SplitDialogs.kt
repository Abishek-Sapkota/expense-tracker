package com.abi.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.SplitSummary
import com.abi.expensetracker.data.Splits
import com.abi.expensetracker.data.model.Split
import com.abi.expensetracker.ui.theme.PillShape

/**
 * Share a bill the user paid between friends.
 *
 * Equal by default, to the paisa, with the odd paisa on the user's own share. Custom lets
 * each friend's part be typed; the user's share is then whatever is left, so the parts
 * can never add up to more or less than the bill.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SplitBillDialog(
    totalMinor: Long,
    initialTitle: String,
    existing: SplitSummary?,
    people: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, myShareMinor: Long, shares: List<Pair<String, Long>>) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(existing?.split?.title ?: initialTitle) }
    var includeMe by remember { mutableStateOf(existing?.split?.myShareMinor?.let { it > 0 } ?: true) }
    val friends = remember { mutableStateListOf<String>().apply { existing?.shares?.forEach { add(it.person) } } }
    val custom = remember {
        mutableStateMapOf<String, String>().apply {
            existing?.shares?.forEach { put(Splits.personKey(it.person), Money.toPlainAmount(it.shareMinor)) }
        }
    }
    var customMode by remember {
        mutableStateOf(
            existing != null && existing.shares.isNotEmpty() &&
                Splits.equalShares(totalMinor, existing.shares.size, existing.split.myShareMinor > 0).second !=
                existing.shares.map { it.shareMinor }
        )
    }
    var adding by remember { mutableStateOf("") }
    fun add(name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty() && friends.none { Splits.personKey(it) == Splits.personKey(clean) }) friends += clean
        adding = ""
    }
    val pickContact = rememberContactPicker { add(it) }

    val suggestions = people.filter { p ->
        friends.none { Splits.personKey(it) == Splits.personKey(p) } &&
            (adding.isBlank() || p.contains(adding.trim(), ignoreCase = true))
    }.take(6)

    // The shares as they would be saved, and whether they add up.
    val (myShare, friendShares, valid) = if (!customMode) {
        val (mine, theirs) = Splits.equalShares(totalMinor, friends.size, includeMe)
        Triple(mine, theirs, friends.isNotEmpty())
    } else {
        val theirs = friends.map { Money.parseToMinor(custom[Splits.personKey(it)].orEmpty()) ?: -1L }
        val sum = theirs.filter { it > 0 }.sum()
        val ok = friends.isNotEmpty() && theirs.all { it > 0 } &&
            if (includeMe) sum <= totalMinor else sum == totalMinor
        Triple(if (includeMe) totalMinor - sum else 0L, theirs, ok)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text(if (existing == null) "Split bill" else "Edit split", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What for") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = adding,
                    onValueChange = { adding = it },
                    label = { Text("Add a person") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add(adding) }),
                    trailingIcon = {
                        Row {
                            if (adding.isNotBlank()) {
                                IconButton(onClick = { add(adding) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add person")
                                }
                            }
                            IconButton(onClick = pickContact) {
                                Icon(Icons.Default.Person, contentDescription = "Pick from contacts")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { name -> SuggestionChip(onClick = { add(name) }, label = { Text(name) }) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeMe, onCheckedChange = { includeMe = it })
                    Text("I had a share too", style = MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoicePill("Equal", selected = !customMode, onClick = { customMode = false })
                    ChoicePill("Custom", selected = customMode, onClick = {
                        // Start custom from the equal split, so only the odd one needs typing.
                        if (!customMode) friends.forEachIndexed { i, f ->
                            custom.putIfAbsent(Splits.personKey(f), Money.toPlainAmount(friendShares.getOrElse(i) { 0L }.coerceAtLeast(0L)))
                        }
                        customMode = true
                    })
                }
                friends.forEachIndexed { index, friend ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(friend, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (customMode) {
                            OutlinedTextField(
                                value = custom[Splits.personKey(friend)].orEmpty(),
                                onValueChange = { custom[Splits.personKey(friend)] = it },
                                prefix = { Text(Money.RUPEE) },
                                singleLine = true,
                                shape = MaterialTheme.shapes.small,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.width(140.dp)
                            )
                        } else {
                            Text(Money.format(friendShares.getOrElse(index) { 0L }), style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { friends.removeAt(index) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove $friend")
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    when {
                        friends.isEmpty() -> "Add at least one person to split with."
                        customMode && !valid && includeMe -> "Friends' shares are more than the bill."
                        customMode && !valid -> "Shares must add up to ${Money.format(totalMinor)}."
                        else -> "Your share ${Money.format(myShare)} of ${Money.format(totalMinor)}. " +
                            "Spending drops as friends pay you back."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (valid || friends.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
                )
                onDelete?.let { delete ->
                    TextButton(onClick = delete) {
                        Text("Remove split", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, myShare, friends.zip(friendShares)) },
                enabled = valid,
                shape = PillShape
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Record a friend paying their share of a split.
 *
 * With [fixedAmountMinor] the payment is an incoming bank credit and its amount is the
 * credit's; without, it is cash and defaults to what the friend still owes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SplitPaymentDialog(
    splits: List<SplitSummary>,
    fixedAmountMinor: Long?,
    onDismiss: () -> Unit,
    onSave: (split: Split, person: String, amountMinor: Long) -> Unit,
    initialPerson: String? = null,
    initialSplitId: Long? = null
) {
    // Everyone who still owes on some split, largest debt first.
    val debtors = splits.flatMap { s -> s.shares.filter { it.remainingMinor > 0 } }
        .groupBy { Splits.personKey(it.person) }
        .map { (_, list) -> list.first().person to list.sumOf { it.remainingMinor } }
        .sortedByDescending { it.second }
    var person by remember { mutableStateOf(initialPerson ?: debtors.singleOrNull()?.first) }
    val theirSplits = splits.filter { s ->
        s.shares.any { Splits.personKey(it.person) == Splits.personKey(person.orEmpty()) && it.remainingMinor > 0 }
    }
    var splitId by remember(person) {
        mutableStateOf(initialSplitId ?: theirSplits.singleOrNull()?.split?.id)
    }
    val chosen = splits.firstOrNull { it.split.id == splitId }
    val remaining = chosen?.shares
        ?.firstOrNull { Splits.personKey(it.person) == Splits.personKey(person.orEmpty()) }
        ?.remainingMinor ?: 0L
    var amount by remember(splitId, person) {
        mutableStateOf(Money.toPlainAmount(fixedAmountMinor ?: remaining))
    }
    val amountMinor = fixedAmountMinor ?: Money.parseToMinor(amount)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text("Share of a split", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (debtors.isEmpty() && initialPerson == null) {
                    Text(
                        "No one owes you on a split. Split a bill from its transaction first.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    return@Column
                }
                Text("Who paid", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    debtors.forEach { (name, owed) ->
                        ChoicePill(
                            "$name · ${Money.format(owed)}",
                            selected = Splits.personKey(name) == Splits.personKey(person.orEmpty()),
                            onClick = { person = name }
                        )
                    }
                }
                if (person != null && theirSplits.isNotEmpty()) {
                    Text("For", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        theirSplits.forEach { s ->
                            val left = s.shares.first { Splits.personKey(it.person) == Splits.personKey(person!!) }.remainingMinor
                            ChoicePill(
                                "${s.split.title} · ${Money.format(left)}",
                                selected = s.split.id == splitId,
                                onClick = { splitId = s.split.id }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text(Money.RUPEE) },
                    singleLine = true,
                    enabled = fixedAmountMinor == null,
                    shape = MaterialTheme.shapes.small,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (fixedAmountMinor != null) {
                    Text(
                        "The amount is the transaction's. It stops counting as money received " +
                            "and brings the bill's spending down instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { chosen?.let { onSave(it.split, person!!, amountMinor!!) } },
                enabled = chosen != null && person != null && amountMinor != null && amountMinor > 0,
                shape = PillShape
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
