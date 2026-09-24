package com.abi.expensetracker.ui.components

import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A dropdown that opens from its own control, at its own width, and filters as you type.
 *
 * The plain [androidx.compose.material3.DropdownMenu] pops up beside the button at
 * whatever width its longest row happens to need, and has no way to narrow a list — which
 * is unusable once the list is every sender on the phone. This one anchors to the field,
 * so the menu is the field: same edge, same width, and the text you type is the filter.
 *
 * Generic over the item so the same control serves senders, banks and anything else; a
 * nullable [T] lets a caller offer an "any"/"none" row as a real item rather than as a
 * special case inside here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchableDropdown(
    selectedLabel: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "Search",
    emptyText: String = "No match",
    /** When set, typing a name no item has offers a "Create" row that calls this. */
    onCreate: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val matches = remember(items, query) {
        val text = query.trim()
        if (text.isEmpty()) items
        else items.filter { itemLabel(it).contains(text, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (!enabled) return@ExposedDropdownMenuBox
            expanded = it
            // Every open starts from the whole list: a filter left over from last time
            // reads as a list that has lost rows.
            if (it) query = ""
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            // Closed, it states the choice; open, it is the search box. One control, so
            // there is no moment where the thing under your finger changes meaning.
            value = if (expanded) query else selectedLabel,
            onValueChange = { query = it; expanded = true },
            enabled = enabled,
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            placeholder = { Text(placeholder) },
            leadingIcon = if (expanded) {
                { Icon(Icons.Default.Search, contentDescription = null) }
            } else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, enabled)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val typed = query.trim()
            if (onCreate != null && typed.isNotEmpty() &&
                items.none { itemLabel(it).equals(typed, ignoreCase = true) }
            ) {
                DropdownMenuItem(
                    text = { Text("Create \u201c$typed\u201d", color = MaterialTheme.colorScheme.primary) },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        onCreate(typed)
                        expanded = false
                    }
                )
            }
            if (matches.isEmpty() && onCreate == null) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        emptyText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            matches.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onSelect(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
