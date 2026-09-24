package com.abi.expensetracker.ui

import com.abi.expensetracker.ui.theme.AppTheme
import com.abi.expensetracker.ui.components.rememberPostNotificationsState
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
    onAddingChange: (Boolean) -> Unit,
    askUncategorised: Boolean,
    onAskUncategorised: (Boolean) -> Unit
) {
    /** The category being edited, or null when that editor is closed. */
    var editing by remember { mutableStateOf<Category?>(null) }

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
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Ask about uncategorised spending", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "When a new payment matches no category, a notification asks what it was " +
                            "for. Your reply becomes its title and files it by keywords.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = askUncategorised, onCheckedChange = onAskUncategorised)
            }
            // The question is a notification, so without that permission it would never
            // show; say so where the switch is instead of failing silently.
            val post = rememberPostNotificationsState()
            if (askUncategorised && !post.granted) {
                Row(
                    Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Notifications are off for this app, so nothing will be asked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.finance.debit,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = post.request) { Text("Allow notifications") }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onApplyKeywords,
                enabled = !busy,
                shape = PillShape
            ) { Text("Apply to uncategorised") }
        }

        categories.forEach { category ->
            CategoryRow(category = category, onClick = { editing = category })
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
            onDelete = null
        )
    }

    editing?.let { category ->
        CategoryEditorDialog(
            category = category,
            onDismiss = { editing = null },
            onSave = { name, keywords, color ->
                onUpdate(category.copy(name = name, icon = "", keywords = keywords, color = color))
                editing = null
            },
            onDelete = {
                onDelete(category.id)
                editing = null
            }
        )
    }
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit) {
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
        }
    }
}

/** Add and edit are the same form; only the delete button and the title differ. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    category: Category?,
    existing: List<Category> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String, keywords: String, color: Int?) -> Unit,
    onDelete: (() -> Unit)?
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
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
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keywords, comma separated") },
                    placeholder = { Text("biryani, momo, restaurant") },
                    shape = MaterialTheme.shapes.small,
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
                if (onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete category", color = MaterialTheme.colorScheme.error)
                    }
                }
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
