package com.abi.expensetracker.ui

import com.abi.expensetracker.ui.components.SyncSmsControl
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ManageSearch
import com.abi.expensetracker.ui.theme.AppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.abi.expensetracker.data.AppIconRef
import com.abi.expensetracker.data.InstalledApp
import com.abi.expensetracker.data.installedLauncherApps
import com.abi.expensetracker.data.appLabel
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.model.Bank
import com.abi.expensetracker.data.model.LimitBasis
import com.abi.expensetracker.data.model.LimitStatus
import com.abi.expensetracker.data.model.SpendingLimit
import com.abi.expensetracker.ui.components.LedgerCard
import com.abi.expensetracker.ui.components.AddFab
import com.abi.expensetracker.ui.components.Monogram
import com.abi.expensetracker.ui.components.SearchableDropdown
import com.abi.expensetracker.ui.components.SectionHeader
import com.abi.expensetracker.ui.components.StatusChip
import com.abi.expensetracker.ui.theme.AccentColor
import com.abi.expensetracker.ui.theme.ChipShape
import com.abi.expensetracker.ui.theme.PillShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    vm: AccountsViewModel = viewModel()
) {
    val banks by vm.banks.collectAsStateWithLifecycle()
    val senders by vm.senders.collectAsStateWithLifecycle()
    val linkedSenders by vm.linkedSenders.collectAsStateWithLifecycle()
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()
    val senderQuery by vm.senderQuery.collectAsStateWithLifecycle()
    /** Whether the search sheet is open. */
    var senderSearchOpen by remember { mutableStateOf(false) }
    var newBankName by remember { mutableStateOf("") }
    var newBankIcon by remember { mutableStateOf<String?>(null) }
    /** The saved bank whose icon is being picked, or null when that picker is closed. */
    var iconPickerFor by remember { mutableStateOf<Bank?>(null) }
    /** The bank an app is being added to, or null when that picker is closed. */
    var appPickerFor by remember { mutableStateOf<Bank?>(null) }
    val bankApps by vm.bankApps.collectAsStateWithLifecycle()
    val notificationApps by vm.notificationApps.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val syncStatus by vm.syncStatus.collectAsStateWithLifecycle()
    /** The same picker for the bank still being typed into the form above. */
    var pickingNewBankIcon by remember { mutableStateOf(false) }
    var addingBank by remember { mutableStateOf(false) }

    val pendingCount = senders.count { it.bankId == null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { AddFab(onClick = { addingBank = true }) },
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                title = { Text("Accounts", style = MaterialTheme.typography.headlineSmall) },
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
            item {
                SectionHeader(
                    "Banks & services",
                    trailing = if (banks.isEmpty()) null else "${banks.size} active"
                )
            }

            if (banks.isEmpty()) {
                item {
                    LedgerCard {
                        Text(
                            "No accounts yet. Tap Add to create one, e.g. Nabil or eSewa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(banks, key = { it.id }) { bank ->
                BankRow(
                    bank = bank,
                    apps = bankApps.filter { it.bankId == bank.id }.map { it.packageName },
                    onPickIcon = { iconPickerFor = bank },
                    onAddApp = { appPickerFor = bank },
                    onRemoveApp = { vm.removeApp(bank, it) },
                    onDelete = { vm.deleteBank(bank.id) },
                    // Said once, on the first account, rather than on every card.
                    showSharedAppHint = bank == banks.first()
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader(
                    title = "Linked senders",
                    trailing = if (pendingCount > 0) "$pendingCount unlinked" else null
                )
                Text(
                    "Only the senders you linked. Everything else your phone receives stays " +
                        "out of the way until you search for it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    onClick = { senderSearchOpen = true },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) {
                            Icon(
                                Icons.Filled.ManageSearch, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Find a sender", style = MaterialTheme.typography.titleMedium)
                            if (pendingCount > 0) {
                                Text(
                                    "$pendingCount unlinked SMS senders",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.finance.debit
                                )
                            }
                        }
                        Text("Review  ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (linkedSenders.isEmpty()) {
                item {
                    LedgerCard {
                        Text(
                            if (senders.isEmpty())
                                "No messages read yet. Open Find a sender and tap Sync SMS."
                            else
                                "No sender is linked yet. Find one and point it at a bank.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(linkedSenders, key = { it.senderKey }) { entry ->
                SenderCard(
                    entry = entry,
                    banks = banks,
                    onLink = { bankId -> vm.link(entry.senderKey, bankId) },
                    onUnlink = { vm.unlink(entry.senderKey) },
                    onAsksWhatFor = { vm.setAsksWhatFor(entry.senderKey, it) }
                )
            }

            item {
                Column(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = ChipShape, color = MaterialTheme.colorScheme.surfaceContainer) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Lock, contentDescription = null,
                                tint = AppTheme.finance.credit, modifier = Modifier.size(14.dp)
                            )
                            Text("Zero-cloud architecture", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text(
                        "Sender mapping and notification filters are stored on this phone only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    if (senderSearchOpen) {
        SenderSearchSheet(
            query = senderQuery,
            onQuery = vm::setSenderQuery,
            results = searchResults,
            banks = banks,
            totalSenders = senders.size,
            onLink = { key, bankId -> vm.link(key, bankId) },
            onUnlink = { key -> vm.unlink(key) },
            onAsksWhatFor = { key, on -> vm.setAsksWhatFor(key, on) },
            syncing = syncing,
            syncStatus = syncStatus,
            onSync = vm::syncSms,
            onDismiss = { senderSearchOpen = false; vm.setSenderQuery("") }
        )
    }

    if (addingBank) {
        AlertDialog(
            onDismissRequest = { addingBank = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = { Text("Add account", style = MaterialTheme.typography.titleLarge) },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButtonSwatch(
                        icon = newBankIcon,
                        fallback = newBankName,
                        onClick = { pickingNewBankIcon = true }
                    )
                    OutlinedTextField(
                        value = newBankName,
                        onValueChange = { newBankName = it },
                        label = { Text("Name, e.g. Nabil or eSewa") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.addBank(newBankName, newBankIcon)
                        newBankName = ""
                        newBankIcon = null
                        addingBank = false
                    },
                    enabled = newBankName.isNotBlank(),
                    shape = PillShape
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addingBank = false }) { Text("Cancel") } }
        )
    }

    if (pickingNewBankIcon) {
        IconPickerDialog(
            selected = newBankIcon,
            onDismiss = { pickingNewBankIcon = false },
            onPick = { newBankIcon = it; pickingNewBankIcon = false }
        )
    }

    appPickerFor?.let { bank ->
        AppChooserDialog(
            seen = notificationApps,
            onDismiss = { appPickerFor = null },
            onPick = { vm.addApp(bank, it); appPickerFor = null }
        )
    }

    iconPickerFor?.let { bank ->
        IconPickerDialog(
            selected = bank.icon,
            onDismiss = { iconPickerFor = null },
            onPick = { vm.setBankIcon(bank, it); iconPickerFor = null }
        )
    }
}

/**
 * The icons offered for a bank.
 *
 * Emoji, so nothing ships as an asset and a backup carries the choice as plain text.
 * A short curated list rather than a full keyboard: these are the shapes that read as a
 * bank, a wallet or a service at 40dp.
 */
private val BANK_ICONS = listOf(
    "\uD83C\uDFE6", "\uD83C\uDFE7", "\uD83D\uDCB3", "\uD83D\uDCB5", "\uD83D\uDCB0",
    "\uD83E\uDE99", "\uD83D\uDCF1", "\uD83D\uDCBC", "\uD83E\uDDFE", "\uD83D\uDCCA",
    "\uD83D\uDED2", "\uD83C\uDFE0", "\u26A1", "\u2708\uFE0F", "\uD83D\uDE8C",
    "\uD83C\uDF93", "\uD83E\uDE7A", "\uD83C\uDF7D\uFE0F", "\uD83C\uDF81", "\uD83D\uDCF6"
)

/** The 40dp circle that stands in for the icon and opens the picker when tapped. */
@Composable
private fun IconButtonSwatch(icon: String?, fallback: String, onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick)) {
        Monogram(text = fallback.ifBlank { "+" }, glyph = icon)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconPickerDialog(
    selected: String?,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit
) {
    // Opens on whichever tab the current icon came from, so re-opening a bank that wears
    // its app's icon does not look like the choice was lost.
    var showingApps by remember { mutableStateOf(AppIconRef.packageOf(selected) != null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text("Pick an icon", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Shown on this bank's transactions in the ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !showingApps,
                        onClick = { showingApps = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Emoji") }
                    SegmentedButton(
                        selected = showingApps,
                        onClick = { showingApps = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("App icon") }
                }
                if (showingApps) {
                    AppIconPicker(selected = selected, onPick = onPick)
                } else {
                    EmojiIconPicker(selected = selected, onPick = onPick)
                }
            }
        },
        confirmButton = {
            // Clearing is the same action as picking, with no icon: the ledger then falls
            // back to the merchant monogram it showed before.
            TextButton(onClick = { onPick(null) }) { Text("No icon") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EmojiIconPicker(selected: String?, onPick: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BANK_ICONS.chunked(5).forEach { rowIcons ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowIcons.forEach { icon ->
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (icon == selected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .clickable { onPick(icon) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

/**
 * Picks the launcher icon of an app installed on this phone — eSewa's icon for the eSewa
 * wallet, the bank's own icon for the bank.
 *
 * The list is read once per open rather than cached in a view model: it is a few hundred
 * rows off the package manager, and a stale list would miss an app installed since.
 */
@Composable
private fun AppIconPicker(selected: String?, onPick: (String?) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val apps by produceState(initialValue = emptyList<InstalledApp>(), context) {
        value = withContext(Dispatchers.IO) { installedLauncherApps(context) }
    }
    val selectedPackage = AppIconRef.packageOf(selected)
    val matches = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps
        else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search apps") },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        )
        when {
            apps.isEmpty() -> Text(
                "Reading installed apps…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            matches.isEmpty() -> Text(
                "No app matches that name.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> LazyColumn(
                modifier = Modifier.heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(matches, key = { it.packageName }) { app ->
                    val isSelected = app.packageName == selectedPackage
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable { onPick(AppIconRef.of(app.packageName)) }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Monogram(text = app.label, glyph = AppIconRef.of(app.packageName))
                        Text(app.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BankRow(
    bank: Bank,
    apps: List<String>,
    onPickIcon: () -> Unit,
    onAddApp: () -> Unit,
    onRemoveApp: (String) -> Unit,
    onDelete: () -> Unit,
    showSharedAppHint: Boolean = false
) {
    val context = LocalContext.current
    LedgerCard {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButtonSwatch(icon = bank.icon, fallback = bank.name, onClick = onPickIcon)
                Text(
                    bank.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onPickIcon) {
                    Text("Icon", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", style = MaterialTheme.typography.labelMedium, color = AppTheme.finance.debit)
                }
            }
            // The apps whose notifications are this account's. The ledger then shows the
            // account's own name and icon for them, never the app's.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "NOTIFICATIONS FROM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (apps.isNotEmpty()) {
                    Text("Automatic parse", style = MaterialTheme.typography.labelSmall, color = AppTheme.finance.credit)
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                apps.forEach { pkg ->
                    val label = remember(pkg) { appLabel(context, pkg) ?: pkg }
                    InputChip(
                        selected = false,
                        onClick = { onRemoveApp(pkg) },
                        label = { Text(label) },
                        avatar = {
                            Monogram(
                                text = label,
                                glyph = AppIconRef.of(pkg),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, contentDescription = "Remove $label", Modifier.size(16.dp))
                        },
                        shape = ChipShape
                    )
                }
                AssistChip(
                    onClick = onAddApp,
                    label = { Text("App") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(16.dp)) },
                    shape = ChipShape
                )
            }
            if (showSharedAppHint) {
                Text(
                    "The same app (e.g. Gmail) may be added to several accounts; the account " +
                        "matching the message is used.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Picks the app whose notifications belong to an account. Apps that have already sent a
 * notification the app read come first; any installed app can be chosen ahead of its
 * first one.
 */
@Composable
private fun AppChooserDialog(seen: List<InstalledApp>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text("Notifications from", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (seen.isNotEmpty()) {
                    Text("Already sent notifications", style = MaterialTheme.typography.labelMedium)
                    seen.forEach { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onPick(app.packageName) }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Monogram(text = app.label, glyph = AppIconRef.of(app.packageName))
                            Text(app.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("All apps", style = MaterialTheme.typography.labelMedium)
                }
                AppIconPicker(selected = null) { ref -> AppIconRef.packageOf(ref)?.let(onPick) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Search for a sender and point it at a bank, without the tab having to list every sender
 * on the phone.
 *
 * A sheet rather than a dialog or its own screen: linking is a short errand — search, tap,
 * pick a bank, done — and a sheet keeps the tab underneath in view, rises with the
 * keyboard, and closes with a swipe. It stays open after a link so several senders can be
 * cleared in one sitting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SenderSearchSheet(
    query: String,
    onQuery: (String) -> Unit,
    results: List<SenderEntry>,
    banks: List<Bank>,
    totalSenders: Int,
    onLink: (String, Long) -> Unit,
    onUnlink: (String) -> Unit,
    onAsksWhatFor: (String, Boolean) -> Unit,
    syncing: Boolean,
    syncStatus: String?,
    onSync: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searching = query.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Find a sender", style = MaterialTheme.typography.titleLarge)
            Text(
                "Searches sender ids and the text of the messages themselves, so a 5-digit " +
                    "short code is found by the wallet name its messages use.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Right here, so a sender that has not been read yet is one tap away.
            SyncSmsControl(syncing = syncing, status = syncStatus, onSync = onSync)
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                label = { Text("Search messages and senders") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searching) {
                        IconButton(onClick = { onQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                if (searching) "Matching senders"
                else "Senders with no bank yet",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (results.isEmpty()) {
                LedgerCard {
                    Text(
                        when {
                            searching -> "No sender matches that. Try a word the message " +
                                "itself uses, like the wallet's name."
                            totalSenders == 0 -> "No messages read yet. Tap Sync SMS above."
                            else -> "Every sender is linked. Search to find one and change " +
                                "where it points."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    // Capped rather than filling: the sheet keeps the tab it came from
                    // visible above it, which is what makes it read as a detour and not a
                    // new screen.
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(results, key = { it.senderKey }) { entry ->
                        SenderCard(
                            entry = entry,
                            banks = banks,
                            onLink = { bankId -> onLink(entry.senderKey, bankId) },
                            onUnlink = { onUnlink(entry.senderKey) },
                            onAsksWhatFor = { onAsksWhatFor(entry.senderKey, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SenderCard(
    entry: SenderEntry,
    banks: List<Bank>,
    onLink: (Long) -> Unit,
    onUnlink: () -> Unit,
    onAsksWhatFor: (Boolean) -> Unit
) {
    val linked = entry.bankId != null

    LedgerCard {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (linked) {
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = AppTheme.finance.creditSurface) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle, contentDescription = null,
                                    tint = AppTheme.finance.credit, modifier = Modifier.size(12.dp)
                                )
                                Text("Linked", style = MaterialTheme.typography.labelSmall, color = AppTheme.finance.credit)
                            }
                        }
                    } else {
                        StatusChip(
                            text = "Needs bank",
                            container = AppTheme.finance.debitSurface,
                            content = AppTheme.finance.debit
                        )
                    }
                    Text(
                        "${entry.messageCount} messages",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (linked) {
                    TextButton(onClick = onUnlink) {
                        Text("Unlink", style = MaterialTheme.typography.labelMedium, color = AppTheme.finance.debit)
                    }
                }
            }

            // The raw sender id belongs here and nowhere else: this screen exists to
            // turn it into a name the rest of the app can show.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.surfaceContainer) {
                    Text(
                        entry.senderKey,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (linked) {
                    Icon(
                        Icons.Filled.Bolt, contentDescription = "Parsed automatically",
                        tint = AppTheme.finance.credit, modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (entry.rawSenders.size > 1 || entry.rawSenders.firstOrNull() != entry.senderKey) {
                Text(
                    "Variants: " + entry.rawSenders.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Account", style = MaterialTheme.typography.bodyMedium)
                SearchableDropdown(
                    selectedLabel = when {
                        linked -> entry.bankName ?: "Linked"
                        banks.isEmpty() -> "Add a bank first"
                        else -> "Link to bank"
                    },
                    items = banks,
                    itemLabel = { it.name },
                    onSelect = { onLink(it.id) },
                    enabled = banks.isNotEmpty(),
                    placeholder = "Search banks",
                    emptyText = "No bank matches that",
                    modifier = Modifier.weight(1f)
                )
            }

            // Per sender, because the answer differs per sender: a bank that already
            // writes the merchant into its message has nothing to ask about, and a wallet
            // that only ever says "payment successful" has everything to ask about.
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Ask what it was for",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "A notification you can reply to, right after a payment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = entry.asksWhatFor, onCheckedChange = onAsksWhatFor)
            }
        }
    }
}
