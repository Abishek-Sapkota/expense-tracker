package com.abi.expensetracker.ui

import com.abi.expensetracker.ui.theme.ChipShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.luminance
import com.abi.expensetracker.ui.theme.NeutralPalette
import com.abi.expensetracker.ui.theme.AppTheme
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.IntegrationInstructions
import androidx.compose.material.icons.outlined.MarkChatRead
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abi.expensetracker.backup.ImportMode
import com.abi.expensetracker.data.CalendarDates
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.NepaliCalendar
import com.abi.expensetracker.data.NepaliDate
import com.abi.expensetracker.data.model.LimitBasis
import com.abi.expensetracker.data.model.LimitStatus
import com.abi.expensetracker.data.model.SpendingLimit
import com.abi.expensetracker.ui.components.LedgerCard
import com.abi.expensetracker.ui.components.AddFab
import com.abi.expensetracker.ui.components.PermissionCard
import com.abi.expensetracker.ui.components.rememberNotificationAccessState
import com.abi.expensetracker.ui.components.rememberPostNotificationsState
import com.abi.expensetracker.ui.components.StatusChip
import com.abi.expensetracker.ui.components.rememberSmsPermissionState
import com.abi.expensetracker.ui.theme.AccentColor
import com.abi.expensetracker.ui.theme.PillShape
import com.abi.expensetracker.ui.theme.ThemeMode
import com.abi.expensetracker.ui.theme.accentPaletteFrom
import com.abi.expensetracker.ui.theme.argbToHsl
import com.abi.expensetracker.ui.theme.hslToArgb
import com.abi.expensetracker.ui.theme.toArgbInt
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val status by vm.status.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val spendingLimit by vm.spendingLimit.collectAsStateWithLifecycle()
    val limitStatus by vm.limitStatus.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val accentPreset by vm.accentPreset.collectAsStateWithLifecycle()
    val customAccentArgb by vm.customAccentArgb.collectAsStateWithLifecycle()
    val usingCustomAccent by vm.usingCustomAccent.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val useNepaliCalendar by vm.useNepaliCalendar.collectAsStateWithLifecycle()
    val neutralPalette by vm.neutralPalette.collectAsStateWithLifecycle()

    /** Which sub-screen is open, or null for the menu itself. */
    var openSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }

    // Granting lives here now: permissions are what the app may read, which is a setting,
    // and Accounts is about which bank a sender belongs to.
    val sms = rememberSmsPermissionState()
    val notifications = rememberNotificationAccessState()
    val postNotifications = rememberPostNotificationsState()
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(vm::exportBackup) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingImportUri = uri }

    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val section = openSection
    var addingCategory by remember { mutableStateOf(false) }
    if (section != null) BackHandler { openSection = null }

    // Templates is a full screen with its own list and top bar, so it replaces Settings
    // rather than nesting a scrolling list inside this scrolling column.
    if (section == SettingsSection.TEMPLATES) {
        TemplatesScreen(onBack = { openSection = null })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (section == SettingsSection.CATEGORIES) AddFab(onClick = { addingCategory = true })
        },
        topBar = {
            TopAppBar(
                expandedHeight = 52.dp,
                title = {
                    Text(
                        section?.title ?: "Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    if (section != null) {
                        IconButton(onClick = { openSection = null }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to settings"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (section) {
                // The menu. One screen per subject rather than one long scroll: the
                // things here are unrelated errands — a budget, a permission, a rescan —
                // and stacking them meant scrolling past four of them to reach the fifth.
                null -> {
                    // Live one-line summaries, so the menu answers "what is set" without
                    // opening anything.
                    val summaryOf: (SettingsSection) -> String = { section ->
                        when (section) {
                            SettingsSection.SPENDING_LIMIT -> spendingLimit?.let { limit ->
                                val window = if (limit.basis == LimitBasis.DAY) "Daily" else "Monthly"
                                "$window limit of ${Money.format(limit.amountMinor)}" +
                                    (limitStatus?.let { st ->
                                        if (st.isOver) " · over" else " · ${Money.format(st.remainingMinor)} left"
                                    } ?: "")
                            } ?: "No limit set"
                            SettingsSection.CATEGORIES -> "${categories.size} categories, keyword auto-filing on"
                            SettingsSection.CALENDAR ->
                                if (useNepaliCalendar) "Nepali (Bikram Sambat) with Gregorian" else "Gregorian"
                            SettingsSection.PERMISSIONS -> listOf(
                                if (sms.granted) "SMS access granted" else "SMS access off",
                                if (notifications.granted) "Notification listener on" else "Notifications off"
                            ).joinToString(" · ")
                            SettingsSection.APPEARANCE ->
                                "${themeMode.label} mode · ${neutralPalette.label} · Accent: " +
                                    (if (usingCustomAccent) "Custom" else accentPreset?.label ?: "Terracotta")
                            else -> section.summary
                        }
                    }
                    SettingsGroup.entries.forEach { group ->
                        Text(
                            group.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                        val sections = SettingsSection.entries.filter { it.group == group }
                        LedgerCard {
                            sections.forEachIndexed { index, entry ->
                                SettingsMenuRow(entry, summaryOf(entry)) { openSection = entry }
                                if (index < sections.lastIndex) {
                                    HorizontalDivider(
                                        Modifier.padding(start = 68.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                    OfflineCard()
                }

                SettingsSection.PERMISSIONS -> {
                    Text(
                        "What the app is allowed to read. Turning one on now works exactly " +
                            "as it would have at first launch \u2014 past messages are read too, " +
                            "not just new ones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PermissionCard(
                        title = "Read messages",
                        body = "Bank SMS already on this phone become your ledger, going " +
                            "back as far as your inbox does.",
                        state = sms,
                        actionLabel = "Allow messages"
                    )
                    PermissionCard(
                        title = "Read notifications",
                        body = "Banks and wallets that no longer send an SMS still post a " +
                            "notification. Only ones naming an amount and a transaction are " +
                            "kept; everything else is discarded as it arrives.",
                        state = notifications,
                        actionLabel = "Open settings",
                        footnote = "Android has no popup for this one \u2014 it is a switch in a " +
                            "system list."
                    )
                    PermissionCard(
                        title = "Ask what a payment was for",
                        body = "Right after a payment, a notification asks what it was on. " +
                            "Reply in the shade and the ledger names the row. Choose which " +
                            "senders should ask under Linked senders on the Accounts tab.",
                        state = postNotifications,
                        actionLabel = "Allow notifications"
                    )
                }

                SettingsSection.SPENDING_LIMIT -> SpendingLimitEditor(
                    limit = spendingLimit,
                    status = limitStatus,
                    nepali = useNepaliCalendar,
                    onSave = { amount, basis -> vm.setSpendingLimit(amount, basis) },
                    onClear = { vm.clearSpendingLimit() }
                )

                SettingsSection.CATEGORIES -> CategorySettings(
                    categories = categories,
                    onAdd = vm::addCategory,
                    onUpdate = vm::updateCategory,
                    onDelete = vm::deleteCategory,
                    onApplyKeywords = vm::applyKeywords,
                    busy = busy,
                    adding = addingCategory,
                    onAddingChange = { addingCategory = it }
                )

                SettingsSection.APPEARANCE -> AppearanceControls(
                    themeMode = themeMode,
                    onThemeMode = vm::setThemeMode,
                    preset = accentPreset,
                    customArgb = customAccentArgb,
                    usingCustom = usingCustomAccent,
                    onPreset = vm::setAccent,
                    onCustom = vm::setCustomAccent,
                    neutrals = neutralPalette,
                    onNeutrals = vm::setNeutralPalette
                )

                SettingsSection.MESSAGES -> {
                    Text(
                        "Sync reads messages that arrived since the last run. Full rescan " +
                            "re-reads the whole inbox; it is safe to repeat and will not " +
                            "duplicate anything.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.backfill(fromScratch = false) },
                            enabled = sms.granted && !busy,
                            shape = PillShape
                        ) { Text("Sync SMS") }
                        OutlinedButton(
                            onClick = { vm.backfill(fromScratch = true) },
                            enabled = sms.granted && !busy,
                            shape = PillShape
                        ) { Text("Full rescan") }
                    }
                    if (!sms.granted) {
                        Text(
                            "Reading messages is off. Turn it on under Permissions first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    // Parsing lives here rather than on its own screen: reading messages
                    // and turning them into transactions are two halves of one job, and
                    // the reparse button is the thing you reach for right after a sync.
                    Text(
                        "Reparse rebuilds transactions from the messages already stored, " +
                            "using the current templates. Expenses you added by hand are " +
                            "untouched.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = vm::reparse,
                        enabled = !busy,
                        shape = PillShape
                    ) { Text("Reparse") }
                }

                SettingsSection.TEMPLATES -> Unit
                SettingsSection.CALENDAR -> CalendarSetting(
                    useNepali = useNepaliCalendar,
                    onChange = vm::setUseNepaliCalendar
                )

                SettingsSection.BACKUP -> {
                    Text(
                        "Everything is written to one JSON file: messages, transactions, " +
                            "templates, banks and sender links.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportLauncher.launch("expenses-$today.json") },
                            enabled = !busy,
                            shape = PillShape
                        ) { Text("Export") }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            enabled = !busy,
                            shape = PillShape
                        ) { Text("Import") }
                    }
                }
            }

            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            status?.let { message ->
                LedgerCard {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(message, Modifier.weight(1f))
                        TextButton(onClick = vm::clearStatus) { Text("Dismiss") }
                    }
                }
            }
        }
    }

    pendingImportUri?.let { uri ->
        ImportModeDialog(
            onDismiss = { pendingImportUri = null },
            onPick = { mode -> pendingImportUri = null; vm.importBackup(uri, mode) }
        )
    }
}

/**
 * The sub-screens of Settings, in menu order.
 *
 * Ordered by how often a setting is touched rather than by subject: the budget and the
 * categories are things a user revisits, the permissions and the backup are set once.
 */
private enum class SettingsGroup(val title: String) {
    BUDGET("BUDGET & CATEGORIES"),
    ENGINE("MESSAGES & PERMISSIONS"),
    SYSTEM("SYSTEM & STORAGE")
}

private enum class SettingsSection(val title: String, val summary: String) {
    SPENDING_LIMIT("Spending limit", "A daily or monthly cap on spending"),
    CATEGORIES("Categories & keywords", "The buckets spending falls into, and the words that sort it"),
    CALENDAR("Calendar", "Read dates on the English or the Nepali calendar"),
    PERMISSIONS("Permissions", "What the app is allowed to read"),
    MESSAGES("Messages & parsing", "Sync the inbox, rescan it, or rebuild transactions"),
    TEMPLATES("Parser templates", "Teach the app how your bank words its messages"),
    APPEARANCE("Appearance", "Theme and accent colour"),
    BACKUP("Backup", "Export or import everything as one file")
}

/**
 * The one switch that decides which calendar the whole app reads.
 *
 * Shows today on both calendars rather than only naming the option: "Nepali calendar" is
 * abstract until you see that today is Ashwin 7, 2083.
 */
@Composable
private fun CalendarSetting(useNepali: Boolean, onChange: (Boolean) -> Unit) {
    val today = remember { LocalDate.now() }
    val bs = remember(today) { NepaliCalendar.fromGregorian(today) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Nepali dates appear beside the English ones in the ledger, and months — the " +
                "trends window and a monthly spending limit — run Baishakh to Chaitra " +
                "instead of January to December.",
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
                    Text("Use the Nepali calendar", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Bikram Sambat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = useNepali, onCheckedChange = onChange)
            }
        }
        LedgerCard {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "TODAY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    CalendarDates.fullDateLabel(today, nepali = true),
                    style = MaterialTheme.typography.titleSmall
                )
                if (bs == null) {
                    Text(
                        "This date is outside the Nepali calendar table, so English dates " +
                            "are used for it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private val SettingsSection.group: SettingsGroup
    get() = when (this) {
        SettingsSection.SPENDING_LIMIT, SettingsSection.CATEGORIES, SettingsSection.CALENDAR -> SettingsGroup.BUDGET
        SettingsSection.PERMISSIONS, SettingsSection.MESSAGES, SettingsSection.TEMPLATES -> SettingsGroup.ENGINE
        SettingsSection.APPEARANCE, SettingsSection.BACKUP -> SettingsGroup.SYSTEM
    }

private val SettingsSection.icon: ImageVector
    get() = when (this) {
        SettingsSection.SPENDING_LIMIT -> Icons.Outlined.AccountBalanceWallet
        SettingsSection.CATEGORIES -> Icons.AutoMirrored.Outlined.Label
        SettingsSection.CALENDAR -> Icons.Outlined.CalendarMonth
        SettingsSection.PERMISSIONS -> Icons.Outlined.Security
        SettingsSection.MESSAGES -> Icons.Outlined.MarkChatRead
        SettingsSection.TEMPLATES -> Icons.Outlined.IntegrationInstructions
        SettingsSection.APPEARANCE -> Icons.Outlined.Palette
        SettingsSection.BACKUP -> Icons.Outlined.SettingsBackupRestore
    }

@Composable
private fun SettingsMenuRow(section: SettingsSection, summary: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(section.title, style = MaterialTheme.typography.titleMedium)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * States the privacy model plainly. Only what is true: the manifest declares no network
 * permission, so nothing can leave the phone. The data is not encrypted at rest beyond
 * what Android's app sandbox provides, so this does not claim it is.
 */
@Composable
private fun OfflineCard() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppTheme.finance.credit)
                Text("100% offline & private", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "This app declares no internet permission, so it cannot send anything " +
                    "anywhere. Messages, transactions and backups stay on this phone, inside " +
                    "Android's per-app sandbox.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun ImportModeDialog(onDismiss: () -> Unit, onPick: (ImportMode) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Merge keeps what is on this phone and adds anything missing from the file.")
                Text(
                    "Replace deletes every message, transaction and template on this phone " +
                        "first. A copy of the current data is written to the app cache " +
                        "beforehand, but the cache is not permanent — export first if the " +
                        "data matters."
                )
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        confirmButton = {
            Button(onClick = { onPick(ImportMode.MERGE) }, shape = PillShape) { Text("Merge") }
        },
        // Replace is destructive, so it is the quiet option in the error colour rather
        // than the default action sitting where a thumb lands.
        dismissButton = {
            TextButton(
                onClick = { onPick(ImportMode.REPLACE) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Replace") }
        }
    )
}

/**
 * Theme and accent.
 *
 * Appearance lives here rather than on Accounts because Accounts is about the user's banks
 * and what the app may read — the two questions that change what the ledger *says*. How it
 * looks is maintenance, and maintenance is this tab.
 */
@Composable
private fun AppearanceControls(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    preset: AccentColor?,
    customArgb: Int?,
    usingCustom: Boolean,
    onPreset: (AccentColor) -> Unit,
    onCustom: (Int) -> Unit,
    neutrals: NeutralPalette,
    onNeutrals: (NeutralPalette) -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }

    LedgerCard {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Theme", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ChoicePill(
                        label = mode.label,
                        selected = mode == themeMode,
                        onClick = { onThemeMode(mode) }
                    )
                }
            }
            Text(
                "System follows the phone's own light and dark setting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Text("Background palette", style = MaterialTheme.typography.titleSmall)
            Text(
                "The page, cards, fills and text tones. Shown here in the current light or dark mode.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            NeutralPalette.entries.forEach { option ->
                NeutralPaletteRow(
                    palette = option,
                    tones = if (dark) option.dark else option.light,
                    selected = option == neutrals,
                    onClick = { onNeutrals(option) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Text("Accent colour", style = MaterialTheme.typography.titleSmall)
            Text(
                "Changes the highlight only. Debit and credit stay red and green so money " +
                    "in and out never depend on this choice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentColor.entries.forEach { option ->
                    AccentSwatch(
                        color = option.swatch,
                        selected = !usingCustom && option == preset,
                        onClick = { onPreset(option) }
                    )
                }
                // The mixed colour if there is one, otherwise a neutral well that opens
                // the picker: the slot exists before anything has been mixed into it.
                AccentSwatch(
                    color = customArgb?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.surfaceContainerHighest,
                    selected = usingCustom,
                    onClick = { pickerOpen = true }
                )
            }
            TextButton(onClick = { pickerOpen = true }) {
                Text(if (customArgb == null) "Mix a colour" else "Edit custom colour")
            }
        }
    }

    if (pickerOpen) {
        ColorPickerDialog(
            initialArgb = customArgb ?: (preset ?: AccentColor.DEFAULT).swatch.toArgbInt(),
            onDismiss = { pickerOpen = false },
            onPick = { onCustom(it); pickerOpen = false }
        )
    }
}

/**
 * One palette as a miniature: page, a card with its hairline, a fill chip and the two text
 * tones, so the choice is made by looking rather than by reading hex values.
 */
@Composable
private fun NeutralPaletteRow(
    palette: NeutralPalette,
    tones: com.abi.expensetracker.ui.theme.NeutralTones,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = tones.page,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else tones.hairline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = tones.card,
                border = BorderStroke(1.dp, tones.hairline),
                modifier = Modifier.weight(1f)
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(palette.label, style = MaterialTheme.typography.titleSmall, color = tones.text)
                    Text("Muted text", style = MaterialTheme.typography.bodySmall, color = tones.muted)
                }
            }
            Surface(shape = ChipShape, color = tones.fill) {
                Text(
                    "Fill",
                    style = MaterialTheme.typography.labelMedium,
                    color = tones.muted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text("✓", color = Color.White, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/**
 * Three sliders and a preview.
 *
 * Hue, saturation and lightness rather than a two-dimensional gradient field: a field is
 * nicer to play with and much harder to land a specific colour on, and the preview below
 * shows the four roles the choice actually produces — which is the thing worth checking
 * before committing to it.
 */
@Composable
private fun ColorPickerDialog(
    initialArgb: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit
) {
    val initial = remember(initialArgb) { argbToHsl(initialArgb) }
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var saturation by remember { mutableFloatStateOf(initial[1]) }
    var lightness by remember { mutableFloatStateOf(initial[2]) }

    val argb = hslToArgb(hue, saturation, lightness)
    val palette = accentPaletteFrom(Color(argb))

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text("Mix a colour", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // What the accent becomes in each theme, not just the seed: a colour
                    // that looks right as a swatch can still be wrong as a button.
                    RolePreview("Light", palette.lightPrimary, palette.lightOnPrimary)
                    RolePreview("Chip", palette.lightContainer, palette.lightOnContainer)
                    RolePreview("Dark", palette.darkPrimary, palette.darkOnPrimary)
                }

                SliderRow("Hue", hue, 0f..360f) { hue = it }
                SliderRow("Saturation", saturation, 0f..1f) { saturation = it }
                SliderRow("Lightness", lightness, 0f..1f) { lightness = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(argb) }) { Text("Use this colour") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RolePreview(label: String, container: Color, content: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(MaterialTheme.shapes.small).background(container),
            contentAlignment = Alignment.Center
        ) {
            Text("रु", color = content, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

/**
 * Sets the one spending limit.
 *
 * The window is a choice rather than two separate limits: a daily and a monthly cap that
 * disagree ("रु500 a day" against "रु5,000 a month") gives the user two answers to the
 * same question, and no way to tell which one the ledger is warning about.
 */
@Composable
private fun SpendingLimitEditor(
    limit: SpendingLimit?,
    status: LimitStatus?,
    nepali: Boolean,
    onSave: (String, LimitBasis) -> Unit,
    onClear: () -> Unit
) {
    // Keyed on the saved limit so the field fills in once DataStore has read it, and
    // resets to the stored value after a save.
    var amount by remember(limit) {
        mutableStateOf(limit?.let { Money.toPlainAmount(it.amountMinor) }.orEmpty())
    }
    var basis by remember(limit) { mutableStateOf(limit?.basis ?: LimitBasis.DAY) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Exactly what the ledger will show, so setting a number and going to look at it
        // is not a separate step.
        status?.let { LimitPreviewCard(it, nepali) }

        LedgerCard {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Set a limit", style = MaterialTheme.typography.titleSmall)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LimitBasis.entries.forEach { option ->
                        ChoicePill(
                            label = option.label,
                            selected = option == basis,
                            onClick = { basis = option }
                        )
                    }
                }

                Text(
                    "THRESHOLD AMOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    prefix = { Text(Money.RUPEE, style = MaterialTheme.typography.titleLarge) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.headlineSmall,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    when (basis) {
                        LimitBasis.DAY -> "Resets every day at midnight"
                        // Named on the Nepali calendar, because "the 1st" then falls
                        // mid-September and the difference is the point of the setting.
                        LimitBasis.MONTH -> if (nepali) {
                            nepaliToday()?.let {
                                "Resets on the 1st of each Nepali month — this one is " +
                                    "${it.monthName} ${it.year}"
                            } ?: "Resets on the 1st of each Nepali month"
                        } else "Resets on the 1st of each month"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { onSave(amount, basis) },
                    enabled = Money.parseToMinor(amount)?.let { it > 0L } == true,
                    shape = PillShape,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save limit") }

                if (limit != null) {
                    TextButton(
                        onClick = { amount = ""; onClear() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Turn off limit", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/** The live limit, shown the way the ledger shows it, plus when it next resets. */
@Composable
private fun LimitPreviewCard(status: LimitStatus, nepali: Boolean) {
    val accent = when {
        status.isOver -> MaterialTheme.colorScheme.error
        status.isClose -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    LedgerCard {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    when (status.limit.basis) {
                        LimitBasis.DAY -> "TODAY'S SPEND"
                        LimitBasis.MONTH -> if (nepali) {
                            nepaliToday()?.let { "${it.monthName.uppercase()}'S SPEND" }
                                ?: "THIS NEPALI MONTH'S SPEND"
                        } else "THIS MONTH'S SPEND"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusChip(
                    text = "Active",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    Money.format(status.spentMinor),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    " / " + Money.format(status.limit.amountMinor),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            LinearProgressIndicator(
                progress = { status.fraction },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (status.isOver) {
                        "Over by " + Money.format(-status.remainingMinor)
                    } else {
                        "Remaining: " + Money.format(status.remainingMinor)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = accent
                )
                Text(
                    "Resets in " + untilReset(status.limit.resetsAtMillis()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Coarse by design: "9h 45m", "12d". The exact second a budget rolls over is not a thing
 * anyone needs, and a ticking clock on a settings screen invites watching it.
 */
private fun untilReset(resetAtMillis: Long): String {
    val remaining = (resetAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    val minutes = remaining / 60_000L
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days >= 1 -> "${days}d"
        hours >= 1 -> "${hours}h ${minutes % 60}m"
        else -> "${minutes}m"
    }
}

private fun basisWords(basis: LimitBasis): String = when (basis) {
    LimitBasis.DAY -> "per day"
    LimitBasis.MONTH -> "per month"
}

/** Today on the Bikram Sambat calendar, or null when the table does not reach it. */
private fun nepaliToday(): NepaliDate? = NepaliCalendar.fromGregorian(LocalDate.now())
