package com.abi.expensetracker.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import com.abi.expensetracker.ui.components.CompactTextField
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abi.expensetracker.data.KeywordSuggester
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.ui.components.LedgerCard
import com.abi.expensetracker.ui.components.LedgerChip
import com.abi.expensetracker.ui.theme.AppTheme

/**
 * Files uncategorised spending one payment at a time: tap a category and the next one
 * comes up. Drawn in place of Trends, which is where the uncategorised share shows.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SortScreen(onBack: () -> Unit, nepaliDates: Boolean, vm: SortViewModel = viewModel()) {
    val queue by vm.queue.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val suggestion by vm.suggestion.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to trends")
                    }
                },
                title = { Text("Sort payments", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp).padding(top = 4.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val item = queue.firstOrNull()
            Text(
                if (item == null) "Every payment has a category."
                else "${queue.size} payment${if (queue.size == 1) "" else "s"} without a category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (item == null) return@Column

            val txn = item.txn
            // The next payment slides in from the right as one is filed or skipped.
            AnimatedContent(
                targetState = item,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInHorizontally(tween(220)) { it / 6 }) togetherWith fadeOut(tween(100))
                },
                contentKey = { it.txn.id },
                label = "payment card"
            ) { item ->
                val txn = item.txn
                LedgerCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            Money.format(txn.amountMinor),
                            style = MaterialTheme.typography.headlineSmall,
                            color = AppTheme.finance.debit
                        )
                        Text(
                            txn.merchant ?: txn.remark ?: "No merchant or remark",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            listOfNotNull(item.bankName, relativeWhen(txn.occurredAt, nepali = nepaliDates))
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // The remark in full when the title is the merchant: it is often the
                        // only hint of what the money was for.
                        if (txn.merchant != null && txn.remark != null) {
                            Text(txn.remark, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Keyed on the payment, so each one starts from its own suggestion.
            val suggested = KeywordSuggester.suggest(txn)
            var keyword by rememberSaveable(txn.id) { mutableStateOf(suggested.orEmpty()) }
            var useKeyword by rememberSaveable(txn.id) { mutableStateOf(suggested != null) }

            val sorted = suggestion?.let { id -> categories.sortedByDescending { it.id == id } } ?: categories
            Text(
                if (suggestion != null) "Category (first is what you chose for this before)" else "Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sorted.forEach { category ->
                    LedgerChip(
                        label = category.name,
                        selected = category.id == suggestion,
                        onClick = { vm.file(item, category, keyword.takeIf { useKeyword }) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useKeyword, onCheckedChange = { useKeyword = it })
                CompactTextField(
                    value = keyword,
                    onValueChange = { keyword = it; if (it.isBlank()) useKeyword = false },
                    label = "Also file future payments with",
                    enabled = useKeyword,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                "The word is added to the category's keywords, so matching payments, past and " +
                    "future, file themselves.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { vm.skip(item) }) { Text("Skip for now") }
        }
    }
}
