package com.abi.expensetracker.ui

import com.abi.expensetracker.ui.components.CompactTextField
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import com.abi.expensetracker.ui.theme.AppTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import com.abi.expensetracker.data.CategoryColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.ui.components.LedgerCard
import com.abi.expensetracker.ui.components.Monogram
import com.abi.expensetracker.ui.theme.ChipShape
import com.abi.expensetracker.ui.theme.PillShape

/**
 * The categories spending falls into, and the keywords that sort it.
 *
 * Keywords are the whole point of the screen: a category with no keywords is a bucket the
 * user has to fill by hand, and "biryani" typed into an expense should land in Dining
 * without anyone deciding it twice.
 */
@Composable
fun CategorySettings(
    categories: List<Category>,
    onAdd: (name: String, keywords: String, color: Int?) -> Unit,
    onUpdate: (Category) -> Unit,
    onDelete: (Long) -> Unit,
    onApplyKeywords: () -> Unit,
    busy: Boolean,
    /** Driven by the screen's Add button, which lives outside this list. */
    adding: Boolean,
    onAddingChange: (Boolean) -> Unit
) {
    /** The category being edited, or null when that editor is closed. */
    var editing by remember { mutableStateOf<Category?>(null) }
    /** The category whose delete is waiting on a confirm. */
    var deleting by remember { mutableStateOf<Category?>(null) }
    var confirmApply by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "A transaction is filed by the first keyword that appears in its merchant or " +
                "remark, longest match first. Keywords are separated by commas and are not " +
                "case-sensitive.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LedgerCard {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Apply to uncategorised", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "New keywords only file new transactions. This files the older " +
                            "ones that have no category yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { confirmApply = true },
                    enabled = !busy,
                    shape = PillShape
                ) { Text("Apply") }
            }
        }

        categories.forEach { category ->
            CategoryRow(
                category = category,
                onClick = { editing = category },
                onDelete = { deleting = category }
            )
        }

        if (categories.isEmpty()) {
            LedgerCard {
                Text(
                    "No categories yet. Add one and give it a few keywords.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    if (adding) {
        CategoryEditorDialog(
            category = null,
            existing = categories,
            onDismiss = { onAddingChange(false) },
            onSave = { name, keywords, color ->
                onAdd(name, keywords, color)
                onAddingChange(false)
            },
        )
    }

    editing?.let { category ->
        CategoryEditorDialog(
            category = category,
            onDismiss = { editing = null },
            onSave = { name, keywords, color ->
                onUpdate(category.copy(name = name, icon = "", keywords = keywords, color = color))
                editing = null
            }
        )
    }

    // Asked first because it writes to many rows at once, and those rows then carry
    // categories until each is changed by hand.
    if (confirmApply) {
        AlertDialog(
            onDismissRequest = { confirmApply = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text("Apply keywords?") },
            text = {
                Text(
                    "Every transaction without a category is checked against your keywords " +
                        "and filed under the category whose keyword it contains. Transactions " +
                        "that already have a category are not changed, and ones no keyword " +
                        "matches stay uncategorised."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyKeywords()
                        confirmApply = false
                    },
                    shape = PillShape
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { confirmApply = false }) { Text("Cancel") } }
        )
    }

    // Asked first because the button sits on the list, one slip from a row tap, and a
    // delete strips the category from every transaction filed under it.
    deleting?.let { category ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text("Delete ${category.name}?") },
            text = { Text("Transactions filed under it become uncategorised.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(category.id)
                        deleting = null
                    },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit, onDelete: () -> Unit) {
    LedgerCard(Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(14.dp).background(Color(CategoryColors.of(category)), CircleShape))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(category.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    category.keywordList.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ")
                        ?: "No keywords — nothing files here on its own",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "${category.keywordList.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, ChipShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${category.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Add and edit are the same form; only the title differs. Delete lives on the list row. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    category: Category?,
    existing: List<Category> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String, keywords: String, color: Int?) -> Unit
) {
    var name by remember { mutableStateOf(category?.name.orEmpty()) }
    var color by remember { mutableStateOf(category?.let { CategoryColors.of(it) } ?: CategoryColors.nextFree(existing)) }
    var keywords by remember { mutableStateOf(category?.keywords.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = {
            Text(
                if (category == null) "New category" else "Edit category",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactTextField(
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    value = name,
                    onValueChange = { name = it },
                    label = "Name",
                    modifier = Modifier.fillMaxWidth()
                )
                // The colour it wears in Trends' breakdown and daily bars.
                Text("Colour", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryColors.PALETTE.forEach { option ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(Color(option), CircleShape)
                                .border(
                                    if (option == color) 3.dp else 0.dp,
                                    if (option == color) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { color = option }
                        )
                    }
                }
                CompactTextField(
                    singleLine = false,
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = "Keywords, comma separated",
                    placeholder = "biryani, momo, restaurant",
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "A keyword matches anywhere in the text, so \"momo\" also catches " +
                        "\"Momo Hut\". Existing transactions keep the category they already " +
                        "have; use \"Apply to uncategorised\" for the rest.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), keywords.trim(), color) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
