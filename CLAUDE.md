# CLAUDE.md

Offline Android expense tracker (Kotlin, Jetpack Compose, Material 3, Room). Reads bank SMS
and app notifications, parses transactions, stores locally. No network permission. See
README.md for design rationale, parser template syntax and install steps.

Keep this file current: when a change adds/moves/renames a file, screen, table, or
convention listed here, update the matching line in the same change.

## Build / run

- JDK 17: prefix Gradle with `JAVA_HOME=/usr/lib/jvm/java-17-openjdk`.
- Gradle wrapper pins 8.9 (AGP 8.7.2, Kotlin 2.0.21, Compose BOM 2024.10.01, Room 2.6.1).
- `./gradlew compileDebugKotlin` — fastest compile check.
- `./gradlew test` — JVM unit tests (parser, data), no device.
- `./gradlew installDebug` — build + install to the adb-connected device.
- `./gradlew assembleRelease` — R8-minified release (≈2 MB), much smoother than debug;
  install with `adb install -r app/build/outputs/apk/release/app-release.apk`. Signed from
  a gitignored `keystore.properties` if present, else with the debug key (so it replaces
  the debug build without an uninstall). `profileinstaller` applies library baseline
  profiles. Judge performance on release builds only.
- `design/` is gitignored (local only, not on GitHub).

## Source map (`app/src/main/java/com/abi/expensetracker/`)

- `data/model/` — Room entities: `Txn` (transactions; `rawId == null` means manual,
  `userEdited` survives reparse), `RawMessage` (verbatim SMS/notification, never deleted),
  `Rule`, `Category`, `Bank`, `SenderLink`, `SpendingLimit`, `Enums` (`Direction`, `Source`),
  `TxnCopy` (message folded into an existing txn as a cross-channel duplicate),
  `MessageFlag` (per-message user decision: `deleted`, `notDuplicate`; respected by
  `insertParsed` so it survives reparse; included in backup since schema 7),
  `LoanEntry` + `LoanKind` (LENT/RECEIVED_BACK/BORROWED/PAID_BACK; free-text `person`;
  optional unique `txnId` link; `splitId` for split shares/repayments; backup since schema 8),
  `Split` (bill txn split with friends: title, total, myShare; backup since schema 9),
  `BankApp` (packageName+bankId: apps whose notifications belong to a bank; one app may
  serve several banks; backup since schema 10).
- `data/db/` — `AppDatabase` (version 12, migrations 1→12 inline; schemas in
  `app/schemas/`), `Daos.kt` (all DAOs; spent/received/debits/category-total queries exclude
  txns linked to a loan entry), `TxnWithSender` + query result classes.
- `data/ExpenseRepository.kt` — single data API used by ViewModels (ingest, reparse,
  categorise, edit, backfill). `insertParsed` de-duplicates across channels via
  `DuplicateMatcher`; `PARSER_VERSION` bump forces a reparse on next app start.
  `observeSpentBetween` = debits − split recoveries (repayments on split bills, capped,
  dated on the bill); Trends subtracts `observeSplitRecoveries` per day/category.
  `deleteTransaction` flags parsed messages (+ copies) deleted; `markNotDuplicate` flags,
  unfolds and books a copy. `editTransaction` re-reads the row (category saves on pick)
  and re-runs keywords on the new remark unless the category was hand-picked (picked in
  that dialog, or not what the old text's keywords gave).
- `data/` misc — `Money` (paise Long, `रु` lakh formatting), `Period`/`CalendarDates`/
  `NepaliCalendar`, `Splits` (equal-share math, split summaries from loan entries),
  `AppIconRef` (`app:<pkg>` icons, `appLabel()` resolves notification package → app name),
  `CategoryColors` (24-colour palette; `Category.color` or a stable default by id;
  `nextFree` for new ones; Uncategorised is grey), `Categorizer` (keyword auto-category), `DuplicateMatcher` (SMS vs
  email/notification copy of same txn: same amount+direction, ±60 min, different sender,
  one copy per sender, remark lead token must agree; user-edited rows are never merged
  by reparse). One SMS can also arrive as a `com.google.android.apps.messaging`
  notification, and banks email via `com.google.android.gm`. `BankResolver` (sender link first; else notification package via `BankApp`; a shared
  app like Gmail resolves by bank name appearing in the message body),
  `SenderNormalizer`, `SettingsStore` (DataStore), `StableId` (sha256 ids).
- `parser/` — pure Kotlin: `SmsParser`, `FieldExtractors`, `Regexes`, `DateParser`,
  `TemplateCompiler` (`{amount}` style templates to regex; `{date}`/`{time}` override the
  arrival timestamp via `DateParser`/`TimeParser` in `SmsParser.occurredAt`), `DefaultRules`.
- `sms/` — `SmsInboxReader` (history backfill), `SmsReceiver` (live).
- `notification/` — `TxnNotificationListener`, `NotificationIngest`, `RemarkPrompt` +
  `RemarkReplyReceiver` (inline reply to add a remark). Asked per `RemarkPromptPolicy`: opted-in
  sender with no remark, or (setting `askUncategorised`, default on, toggle in Categories)
  any new debit no category matched; the reply becomes the remark and is run through keywords.
- `backup/` — `BackupManager`, `BackupSchema` (streaming JSON export/import).
- `di/ServiceLocator.kt` — `repository(context)`, `backupManager(context)`.
- `ui/Navigation.kt` — `Destination` enum = bottom bar tabs (HOME/Ledger, TRENDS, LOANS,
  ACCOUNTS, SETTINGS); tabs are `HorizontalPager` pages. Icons everywhere are the mockups'
  Material Symbols via `material-icons-extended` (tabs: ReceiptLong, QueryStats, SwapHoriz,
  AccountBalance, Settings; filled when selected). Templates lives in Settings
  (`SettingsSection.TEMPLATES` renders `TemplatesScreen(onBack)` full screen).
- `ui/LoansScreen.kt` + `LoansViewModel.kt` — people list with balances (+ = owes me),
  filter chips All/Owe you/You owe/Settled/Splits (split cards with "Paid cash") (totals tap-to-filter), person detail history,
  `LoanEntryDialog` (typed name + suggestion chips + system
  contact picker, no READ_CONTACTS). Edit popup "Mark as loan" links a txn.
- `ui/HomeScreen.kt` + `HomeViewModel.kt` — ledger list grouped by day (`DayHeader` with the
  day's spend, one grouped card per day; display only), row icon = account icon (categories
  have no icons; `Category.icon` column is unused), tap = edit, long-press = multi-select
  (selection top bar with Delete + confirm), period chips, hero card,
  `ExpenseDialog` (shared add/edit dialog: amount, remark, direction, category dropdown with
  "Create …" for a typed name (`HomeViewModel.createCategory`: name as keyword, free colour),
  every source message (primary + cross-channel copies) with channel + sender, date;
  `extras` slot → `TxnLinkControls`: Split bill / Share of split / Mark as loan).
- `ui/OnboardingScreen.kt` (+ `OnboardingViewModel`) — first-run guide shown by
  `MainActivity` while `SettingsStore.onboardingDone` is false: Welcome → Permissions →
  Sync inbox → Accounts (embeds the real `AccountsScreen`) → Done. Auto-marked done for
  installs that already have accounts; Settings has "Run setup guide again".
- `ui/components/Permissions.kt` `SyncSmsControl` — Sync SMS button (asks SMS permission
  first; inbox query without it throws), used in onboarding and Find a sender.
- `ui/SplitDialogs.kt` — `SplitBillDialog` (equal/custom shares, include me),
  `SplitPaymentDialog` (friend pays share; bank credit = fixed amount, cash = editable).
- `ui/DuplicatesScreen.kt` + `DuplicatesViewModel.kt` — folded copies by arrival date
  (default Today, `PeriodChips`/`DateRangeDialog` reused from HomeScreen), "Not a
  duplicate" button. Opened from Ledger top bar, drawn in place of HomeScreen.
- `ui/TrendsScreen.kt` — tapping a category row opens the real ledger (`HomeScreen` with
  `categoryView`, a keyed `HomeViewModel` put in category mode by `showCategory(range, id)`
  → `observeCategoryDebits`, loans excluded): same rows, edit popup, select/delete; Back
  returns. Daily bars are stacked by category colour; breakdown bars use category colour.
- `ui/TrendsScreen`, `TemplatesScreen`, `AccountsScreen`, `SettingsScreen` (+ ViewModels),
  `CategorySettings.kt` (category editor: name, colour, keywords).
- `ui/components/` — `Ledger.kt` (LedgerCard, PeriodHeroCard, GroupedRow, SectionHeader,
  Monogram, banners), `SearchableDropdown` (generic filterable dropdown; nullable item
  for "none" row; optional `onCreate` row), `AppIcon`, `Permissions`.
- `ui/AccountsScreen.kt` + VM — banks; each bank box has "Notifications from" app chips
  (+ App → `AppChooserDialog`, seen apps first; adding sets bank icon if none). Sender
  list/search is SMS senders only (package senders filtered out).
- `ui/TemplatesScreen.kt` — "Messages no rule could read" starters card
  (`observeUnparsedFromLinked`: unparsed, non-copy, non-deleted money messages that pass
  `NotificationIngest.looksLikeTransaction` (also bare amounts + success words), filter chips
  Your accounts / All, always shown); tapping one fills sample,
  rule, sender scope and direction. While the rule field is focused, one scrollable line of
  small token pills shows the tokens not yet used ({any} always); tap inserts at cursor.
- `ui/theme/` — Utilitarian Ledger structure (`design/utilitarian_ledger/DESIGN.md`): white
  page, white hairline cards, fixed `AppTheme.finance` debit/credit colours, 8/16dp + pill
  shapes. Two user choices in Appearance: neutral palette (`Neutrals.kt`
  `NeutralPalette`: Neutral grey default, Cool slate, Warm stone, Mist, White only; mapped to
  Material roles in `toScheme`) and accent (`Accent`, `AccentPalette`).

Tests: `app/src/test/java/com/abi/expensetracker/{data,parser,notification,ui}/`.

## Conventions

- Money is `Long` minor units (paise), never Double.
- Insets: edge-to-edge. Outer Scaffold in `MainActivity` has zero `contentWindowInsets` and
  consumes its padding; each tab's own Scaffold/TopAppBar handles the status bar.
- Tab reselect: `MainActivity` counts taps on the already-open tab and passes
  `resetSignal` to each screen; `OnTabReselect` (Navigation.kt) resets sub-pages, filters and
  scroll. Switching tabs keeps state (sub-page flags are `rememberSaveable`).
- Top bars: `TopAppBar(expandedHeight = 52.dp)` and lists use 4dp top content padding, so
  content sits right under the title (no gap). Keep this on new screens.
- ViewModels are `AndroidViewModel`, get repository via `ServiceLocator`, expose
  `StateFlow` collected with `collectAsStateWithLifecycle`.
- Schema change: bump `AppDatabase.version`, add a `Migration`, keep exported schema JSON.
- Parsing/dedup logic change: bump `PARSER_VERSION` in `ExpenseRepository.kt`.
- Battery: `ExpenseApp` startup maintenance (dedupe, rule sync, reparse, categorize) runs
  once per install/update (`maintenanceStamp` = package `lastUpdateTime`), not on every
  process start — the process also starts in background for each SMS/notification. Never
  add per-start scans there; no polling loops (the ledger's date flow wakes at midnight).
- Device DB for debugging (debug build): `adb exec-out run-as com.abi.expensetracker cat
  databases/expenses.db` (also `-wal`, `-shm`), then inspect with `sqlite3`.
- Comments explain *why* (design reasoning), in full prose; match that density.
- Design reference: `design/utilitarian_ledger/DESIGN.md` (colours/type/components) and the
  per-tab mockups `design/{ledger_home,trends_tab,loans_tab,accounts_tab,settings_tab}/`.
  Old designs live in `design/old_designs/`. Mockups contain decorative claims the app does
  not have (encryption, vault stats, search/avatar) — do not implement those.
- Shared components: `LedgerCard` (white + 1dp hairline), `GroupedRow` (one hairline
  around the group, 68dp-inset dividers), `LedgerChip` (pill filter chip, accent + check
  when selected), `StatusChip` (uppercase tag), `SectionHeader` (title + count pill), `AddFab` (the only Add button: bottom-right pill
  "Add"; used on Ledger, Loans, Accounts (opens Add account dialog), Settings→Categories).
