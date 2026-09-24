package com.abi.expensetracker.ui

import com.abi.expensetracker.data.SenderNormalizer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Rule
import com.abi.expensetracker.parser.TemplateCompiler
import com.abi.expensetracker.ui.components.LedgerCard
import com.abi.expensetracker.ui.components.SearchableDropdown
import com.abi.expensetracker.ui.components.SectionHeader
import com.abi.expensetracker.ui.components.StatusChip
import com.abi.expensetracker.ui.theme.ChipShape
import com.abi.expensetracker.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TemplatesScreen(
    onBack: (() -> Unit)? = null,
    vm: TemplatesViewModel = viewModel()
) {
    val userRules by vm.userRules.collectAsStateWithLifecycle()
    val builtInRules by vm.builtInRules.collectAsStateWithLifecycle()
    val senderKeys by vm.senderKeys.collectAsStateWithLifecycle()
    val template by vm.template.collectAsStateWithLifecycle()
    val starters by vm.starters.collectAsStateWithLifecycle()
    var templateField by remember { mutableStateOf(TextFieldValue(template)) }
    var templateFocused by remember { mutableStateOf(false) }
    val sample by vm.sample.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val needsReparse by vm.needsReparse.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(Direction.DEBIT) }
    var senderKey by remember { mutableStateOf<String?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    val canSave = preview is TemplatePreview.Matched || preview is TemplatePreview.Compiles
    val tokenTransformation = rememberTokenTransformation()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                title = { Text("Templates", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to settings")
                        }
                    }
                },
                actions = { TextButton(onClick = { showHelp = true }) { Text("Help") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (starters.isNotEmpty()) {
                item {
                    StartersCard(
                        starters = starters,
                        nepaliDates = false,
                        onPick = { message ->
                            // Fill everything the message already tells us: the text, the
                            // sender to scope to, and which way the money went.
                            vm.startFromSample(message.body)
                            senderKey = SenderNormalizer.normalize(message.sender)
                            direction = if (CREDIT_WORDS.containsMatchIn(message.body)) Direction.CREDIT
                            else Direction.DEBIT
                        }
                    )
                }
            }

            item {
                LedgerCard {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("New template", style = MaterialTheme.typography.titleMedium)
                            StatusChip("Editor")
                        }
                        Text(
                            "Paste a real transaction SMS, then replace the parts that " +
                                "change with tokens like {amount}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = sample,
                            onValueChange = vm::onSampleChanged,
                            label = { Text("Sample message from your bank") },
                            minLines = 2,
                            shape = MaterialTheme.shapes.small,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedButton(
                            onClick = { vm.startFromSample(sample) },
                            enabled = sample.isNotBlank(),
                            shape = ChipShape,
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Copy to template") }

                        // Held as a TextFieldValue so a tapped token lands at the cursor; the
                        // view model keeps the plain text, and a reset from it (Copy to
                        // template, Save) moves the cursor to the end.
                        LaunchedEffect(template) {
                            if (template != templateField.text) {
                                templateField = TextFieldValue(template, TextRange(template.length))
                            }
                        }
                        OutlinedTextField(
                            value = templateField,
                            onValueChange = {
                                templateField = it
                                if (it.text != template) vm.onTemplateChanged(it.text)
                            },
                            label = { Text("Pattern match rule") },
                            minLines = 2,
                            shape = MaterialTheme.shapes.small,
                            // Tokens are tinted inline so the shape of the rule is visible
                            // at a glance without leaving the field.
                            visualTransformation = tokenTransformation,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { templateFocused = it.isFocused }
                        )

                        // Every token, while the rule is being typed: tapping one puts it at
                        // the cursor, replacing any selection — usually the literal amount
                        // just highlighted in the pasted sample.
                        // One scrollable line of small token pills. A token already in the
                        // rule drops out of the row, since each field is captured once —
                        // except {any}, which can skip text in several places.
                        val unused = TemplateCompiler.PLACEHOLDERS.keys.filter { key ->
                            key == "any" || "{$key}" !in templateField.text
                        }
                        if (templateFocused && unused.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                unused.forEach { key ->
                                    val token = "{$key}"
                                    Surface(
                                        onClick = {
                                            val sel = templateField.selection
                                            val start = minOf(sel.start, sel.end)
                                            val end = maxOf(sel.start, sel.end)
                                            val text = templateField.text.replaceRange(start, end, token)
                                            templateField = TextFieldValue(text, TextRange(start + token.length))
                                            vm.onTemplateChanged(text)
                                        },
                                        shape = ChipShape,
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            token,
                                            fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        PreviewCard(preview)

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Template name") },
                            placeholder = { Text("e.g. Nabil card spend") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            "Transaction nature",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoicePill(
                                label = "Debit (Expense)",
                                selected = direction == Direction.DEBIT,
                                onClick = { direction = Direction.DEBIT }
                            )
                            ChoicePill(
                                label = "Credit (Income)",
                                selected = direction == Direction.CREDIT,
                                onClick = { direction = Direction.CREDIT }
                            )
                        }

                        Text(
                            "Scope SMS sender",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SearchableDropdown(
                            selectedLabel = senderKey?.let { "$it (Sender header)" }
                                ?: "Any sender",
                            // Null is a real row rather than a separate button: "any
                            // sender" is one of the choices, not the absence of one.
                            items = listOf<String?>(null) + senderKeys,
                            itemLabel = { it ?: "Any sender" },
                            onSelect = { senderKey = it },
                            placeholder = "Search senders",
                            emptyText = "No sender matches that",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { vm.save(name, senderKey, direction); name = "" },
                            enabled = canSave,
                            shape = PillShape,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Save template", style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }

            status?.let { message ->
                item {
                    LedgerCard {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                message,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (needsReparse) {
                                Button(
                                    onClick = vm::reparse,
                                    enabled = !busy,
                                    shape = PillShape
                                ) { Text("Reparse") }
                            } else {
                                TextButton(onClick = vm::clearStatus) { Text("Dismiss") }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Your templates",
                    trailing = "${userRules.count { it.enabled }} active"
                )
            }

            if (userRules.isEmpty()) {
                item {
                    LedgerCard {
                        Text(
                            "None yet. The built-in fallbacks below handle common wording.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(userRules, key = { it.id }) { rule ->
                UserRuleCard(
                    rule = rule,
                    onToggle = { vm.setEnabled(rule, it) },
                    onDelete = { vm.delete(rule) }
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader("Built-in rules")
                Text(
                    "Generic fallbacks. Anything these catch is flagged for review.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(builtInRules, key = { it.id }) { rule ->
                LedgerCard {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            rule.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = rule.enabled, onCheckedChange = { vm.setEnabled(rule, it) })
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text("Tokens") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TemplateCompiler.PLACEHOLDERS.forEach { (key, description) ->
                        Text(
                            "{$key} — $description",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Everything else is matched exactly, so punctuation is safe to " +
                            "leave in. Spacing is flexible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun UserRuleCard(rule: Rule, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    LedgerCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    rule.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(
                    text = if (rule.direction == Direction.DEBIT) "Debit" else "Credit",
                    container = if (rule.direction == Direction.DEBIT)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                    content = if (rule.direction == Direction.DEBIT)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }
            rule.template?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            TextButton(onClick = onDelete) {
                Text("Delete", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PreviewCard(preview: TemplatePreview) {
    val matched = preview is TemplatePreview.Matched
    val tone = when (preview) {
        is TemplatePreview.Matched -> MaterialTheme.colorScheme.secondaryContainer
        is TemplatePreview.Invalid, is TemplatePreview.NoMatch ->
            MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val onTone = when (preview) {
        is TemplatePreview.Matched -> MaterialTheme.colorScheme.onSecondaryContainer
        is TemplatePreview.Invalid, is TemplatePreview.NoMatch ->
            MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = tone,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (preview) {
                is TemplatePreview.Empty -> Text(
                    "Type a template to see what it would pick out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = onTone
                )

                is TemplatePreview.Invalid -> Text(
                    preview.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = onTone
                )

                is TemplatePreview.NoMatch -> Text(
                    preview.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = onTone
                )

                is TemplatePreview.Compiles -> {
                    Text(
                        "Template is valid",
                        style = MaterialTheme.typography.titleSmall,
                        color = onTone
                    )
                    Text(
                        "Paste a sample message above to check what it extracts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = onTone
                    )
                }

                is TemplatePreview.Matched -> {
                    Text(
                        "Matched correctly",
                        style = MaterialTheme.typography.titleSmall,
                        color = onTone,
                        fontWeight = FontWeight.SemiBold
                    )
                    preview.fields.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { (label, value) ->
                                FieldTile(label, value, Modifier.weight(1f))
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/**
 * Tints `{token}` spans inside the template field.
 *
 * Purely visual: the text length is unchanged, so cursor positions map one to one and
 * editing behaves exactly as it would in a plain field.
 */
@Composable
private fun rememberTokenTransformation(): VisualTransformation {
    val known = MaterialTheme.colorScheme.primaryContainer
    val onKnown = MaterialTheme.colorScheme.onPrimaryContainer
    val unknown = MaterialTheme.colorScheme.errorContainer
    val onUnknown = MaterialTheme.colorScheme.onErrorContainer

    return remember(known, unknown) {
        VisualTransformation { text ->
            TransformedText(
                highlightTokens(text.text, known, onKnown, unknown, onUnknown),
                OffsetMapping.Identity
            )
        }
    }
}

private val TOKEN = Regex("""\{[a-zA-Z]*\}""")

private fun highlightTokens(
    text: String,
    known: Color,
    onKnown: Color,
    unknown: Color,
    onUnknown: Color
): AnnotatedString = buildAnnotatedString {
    append(text)
    TOKEN.findAll(text).forEach { match ->
        val name = match.value.removeSurrounding("{", "}").lowercase()
        val recognised = TemplateCompiler.PLACEHOLDERS.containsKey(name)
        addStyle(
            SpanStyle(
                background = if (recognised) known else unknown,
                color = if (recognised) onKnown else onUnknown,
                fontWeight = FontWeight.SemiBold
            ),
            match.range.first,
            match.range.last + 1
        )
    }
}

/** Words that mark money coming in, used to preset a starter's direction. */
private val CREDIT_WORDS = Regex("""(?i)\b(credited|deposited|received|refunded)\b""")

/**
 * Messages from linked accounts that looked like transactions but that no rule could read.
 * Tapping one loads it into the editor as both sample and starting rule.
 */
@Composable
private fun StartersCard(
    starters: List<Pair<com.abi.expensetracker.data.model.RawMessage, String>>,
    nepaliDates: Boolean,
    onPick: (com.abi.expensetracker.data.model.RawMessage) -> Unit
) {
    var showAll by remember { mutableStateOf(false) }
    val shown = if (showAll) starters else starters.take(3)
    LedgerCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Messages no rule could read",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusChip("${starters.size}")
            }
            Text(
                "From your linked accounts. Tap one to start a template from it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            shown.forEach { (message, account) ->
                Surface(
                    onClick = { onPick(message) },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            account + " · " + relativeWhen(message.sentAt, nepali = nepaliDates),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            message.body,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (starters.size > 3) {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(if (showAll) "Show fewer" else "Show all ${starters.size}")
                }
            }
        }
    }
}
