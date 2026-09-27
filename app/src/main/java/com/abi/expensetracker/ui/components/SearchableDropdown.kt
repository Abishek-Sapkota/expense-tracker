package com.abi.expensetracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.delay
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    // A tap opens the whole list as a menu, with no keyboard: the keyboard used to come
    // up with it and cover half the rows. Typing is a second, deliberate step from the
    // search icon, and then the matches show inline under the field instead of in a
    // popup, inside the dialog or page that already scrolls above the keyboard. (The
    // menu popup takes keyboard focus unless its field was tapped as editable, so typing
    // into it from the icon never reached the field.)
    var searching by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(searching) { if (searching) focus.requestFocus() }
    // The dialog shrinks to the space above the keyboard, so the matches under the field
    // start out of sight; scroll them into view as the keyboard opens and as they change.
    val results = remember { BringIntoViewRequester() }

    val matches = remember(items, query) {
        val text = query.trim()
        if (text.isEmpty()) items
        else items.filter { itemLabel(it).contains(text, ignoreCase = true) }
    }
    val typed = query.trim()
    val offerCreate = onCreate != null && typed.isNotEmpty() &&
        items.none { itemLabel(it).equals(typed, ignoreCase = true) }

    fun pick(item: T) {
        onSelect(item)
        expanded = false
        searching = false
    }

    fun create() {
        onCreate?.invoke(typed)
        expanded = false
        searching = false
    }

    Column(modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded && !searching,
            onExpandedChange = {
                if (!enabled || searching) return@ExposedDropdownMenuBox
                expanded = it
            }
        ) {
            OutlinedTextField(
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                // Closed, it states the choice; searching, it is the search box.
                value = if (searching) query else selectedLabel,
                onValueChange = { query = it },
                enabled = enabled,
                readOnly = !searching,
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                placeholder = { Text(placeholder) },
                leadingIcon = if (searching) {
                    { Icon(Icons.Default.Search, contentDescription = null) }
                } else null,
                trailingIcon = {
                    if (searching) {
                        IconButton(onClick = { searching = false; query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Stop searching")
                        }
                    } else {
                        IconButton(
                            onClick = { query = ""; expanded = false; searching = true },
                            enabled = enabled
                        ) {
                            Icon(Icons.Default.Search, contentDescription = placeholder)
                        }
                    }
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled && !searching)
                    .focusRequester(focus)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded && !searching,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(text = { Text(itemLabel(item)) }, onClick = { pick(item) })
                }
            }
        }

        if (searching) {
            LaunchedEffect(query) {
                // After the keyboard has finished sliding in and the dialog resized.
                delay(250)
                results.bringIntoView()
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).bringIntoViewRequester(results)
            ) {
                // Capped and scrollable: a short filtered list, not the whole page.
                Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                    if (offerCreate) {
                        DropdownMenuItem(
                            text = { Text("Create \u201c$typed\u201d", color = MaterialTheme.colorScheme.primary) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = ::create
                        )
                    }
                    if (matches.isEmpty() && !offerCreate) {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                emptyText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    matches.forEach { item ->
                        DropdownMenuItem(text = { Text(itemLabel(item)) }, onClick = { pick(item) })
                    }
                }
            }
        }
    }
}
