# Expense Tracker

Offline Android expense tracker. Reads bank SMS on the device, parses transactions from
the message text, and stores everything locally. No network permission is declared, so
the app cannot make a network call even by accident.

Sideload-only by design: `READ_SMS` is restricted on the Play Store to apps that are the
device's default SMS handler.

## Build

Requires JDK 17 and the Android SDK (API 35, build-tools 35.0.0). The Gradle wrapper is
checked in and pins Gradle 8.9 — AGP 8.7 does not support Gradle 9.

```
./gradlew test           # parser unit tests, no device needed
./gradlew assembleDebug  # -> app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` points at the SDK and is machine-specific (gitignored); recreate it
elsewhere with `sdk.dir=/path/to/Android/Sdk`.

If `java` is not JDK 17 by default, prefix commands with
`JAVA_HOME=/usr/lib/jvm/java-17-openjdk`.

## Install on a phone

1. Settings → About phone → tap Build number seven times.
2. Settings → Developer options → enable USB debugging.
3. Connect the phone and accept the RSA prompt; `adb devices` should list it.
4. `adb install -r app/build/outputs/apk/debug/app-debug.apk`

Debug and release builds are signed differently and cannot replace each other. Switching
between them requires an uninstall, which erases the database — export a backup first, or
stay on one build type.

Wireless instead of a cable (Android 11+):

```
adb pair <phone-ip>:<pair-port>
adb connect <phone-ip>:<port>
```

Reset a permission that was denied permanently during testing:

```
adb shell pm revoke com.abi.expensetracker android.permission.READ_SMS
```

## Layout

```
data/model      Entities: RawMessage, Txn, Rule, Category, Bank, SenderLink
data/db         Room database, DAOs, migrations
data/           Money, StableId, SettingsStore, Period, SenderNormalizer,
                BankResolver, ExpenseRepository
parser/         SmsParser, FieldExtractors, DefaultRules — pure Kotlin, unit-tested
sms/            SmsInboxReader (history backfill), SmsReceiver (live messages)
backup/         BackupManager — streaming JSON export and import
ui/             HomeScreen, ToolsScreen, TemplatesScreen, SettingsScreen, the drawer
                and bottom bar, and their ViewModels
ui/theme/       Calm Terracotta Ledger: colour, type, shape, selectable accent
ui/components/  Hero card, transaction tile, chips, banners
docs/           stitch-prompt.md — prompts for generating Material 3 designs
```

## Design

Implements `design/calm_terracotta_ledger/DESIGN.md` — earthen clay neutrals, tonal
surface steps instead of drop shadows, and a hero period total that dominates the screen.

Amounts are Nepali rupees, written `रु`, with lakh grouping (`रु1,23,456.78`), tabular
figures so decimals line up down a column, and an explicit leading sign (`−रु450.00`,
`+रु5,000.00`) so money in and out never depend on colour alone.

The accent colour is selectable in Accounts — terracotta, sage, slate teal, indigo or
plum. It changes the primary role only; debit and credit tints are fixed, because those
carry meaning.

Two departures from the design, both noted rather than silently absorbed:
- The bottom bar sketches a **Trends** tab. Nothing computes trends yet, so the bar
  carries the four destinations that exist. Add Trends when there is an analytics screen
  behind it.
- Roboto Flex is specified but no font file ships here, so the app uses the platform
  Roboto. Drop a variable Roboto Flex into `res/font` and point `Ledger` in `Type.kt` at
  it to match exactly.

## Navigation

The bar is a floating pill of icons with no labels, and the round **+** raised through
its middle is **Add new** — it jumps to the ledger and opens the entry dialog from any
tab. The selected tab sits in a tinted pill. Labels live on `Destination` and are spoken
as each icon's content description, so dropping them costs a screen reader nothing.

A bottom bar carries Ledger, Templates, Accounts and Tools, and the tabs are swipeable —
they are pages of a `HorizontalPager`, not entries on a back stack, so the bar and the
screen cannot disagree. Back from any tab returns to Ledger. There is no drawer; the
bottom bar is the only navigation.

`Destination` in `ui/Navigation.kt` is the single source of truth. When a fifth thing
earns a tab, Tools drops off the bar and Settings takes its place, absorbing the
sync/reparse/backup actions and the Appearance section now on Accounts — that swap is one
edit to the enum.

Home is only the spending view; syncing, reparsing and backup live in Tools.

## Parser templates

Templates are written as the bank's own message with the changing parts replaced:

    Rs.{amount} debited from a/c XX{acct} to {merchant}. Ref {ref}

Placeholders: `{amount}` (required), `{acct}`, `{merchant}`, `{remark}`, `{ref}`,
`{date}`, `{time}`, `{balance}`, and `{any}` to skip text. Everything else matches literally — punctuation and brackets are
quoted, so a template cannot be malformed the way a hand-written regex can. Whitespace
runs match flexibly.

The editor compiles as you type and, given a pasted sample message, shows exactly which
fields it would extract. Saved templates run at priority 100, above the seeded generic
rules at 900+, so a user's own template always wins. Both the template text and the
compiled pattern are stored, so a rule is shown as it was written.

`{date}` takes the transaction date from the message text instead of the arrival time.
`{time}` does the same for the time of day (`21:00`, `8:41:37 AM`, `1:20PM`); a time with no
date goes on the arrival day, or the day before when it is clearly after the message
arrived (a 23:58 payment reported at 00:03).
The time of day still comes from the message, which keeps same-day transactions in order.
A date that will not parse, or that lands more than two years from the message, is
discarded in favour of the arrival time — a misread date can be wrong by decades, while
the arrival time is wrong by at most a day.

Dates are read day-first (`01/02/26` is 1 February) and Gregorian only. **Bikram Sambat
dates are not converted**: a BS date such as `2082-06-05` reads as a far-future Gregorian
year, trips the two-year guard and falls back to the arrival time. That is safe, not
correct — BS support is not implemented.

`{remark}` is what the money went on, as against `{merchant}`, who it went to:

    Rs.{amount} sent to {merchant}. Remarks: {remark}

Both are kept, and both show on the row — "Khalti · Khaja". Where no template captures
one, "Remarks:", "Narration", "Purpose" and "Particulars" are read generically, up to the
end of that sentence. A row with neither a merchant nor a remark is flagged for review.

New templates apply to stored history only after **Reparse**.

## Banks and senders

Settings lists every sender found in stored messages and links each to a bank or service
you name. Raw sender ids appear only there; the home screen shows the linked name, or
"Unlinked sender" pointing you back to settings.

Links are keyed on a normalised sender, because one bank reaches the same phone as
"AX-HDFCBK", "VM-HDFCBK" or "BP-HDFCBK-S" depending on telecom circle — the operator
prefix and DLT suffix are stripped so each bank is linked once.

The bank is resolved when a row is displayed, not stored on the transaction. Linking a
bank labels existing history immediately, with no reparse.

Each bank can carry an icon, picked from a short list of emoji in Accounts. It shows in
place of the merchant monogram on every transaction from that bank's senders, so an
account is identifiable down a column without reading the name. Emoji rather than a
bundled glyph set: nothing ships as an asset, and a backup carries the choice as text.

## Periods

Home defaults to today's spend. Yesterday, last 7 days, last 30 days and a custom range
are all available. "Last 7/30 days" are rolling windows ending today, not calendar weeks
or months — on the 2nd of a month a calendar "last month" would report two days of
spending, which reads as a bug.

## Manual expenses

**Add new** records cash or anything else no SMS covers. Manual entries have a null
`rawId`, which is what distinguishes them: **Reparse** rebuilds only rows derived from
messages, so hand-entered expenses survive it. They can be deleted from their row.

## Editing a transaction

Tapping any row — parsed or manual — opens it for editing: amount, who was paid, what it
was spent on, date and direction. A parsed row keeps the time of day from its message, so same-day transactions
stay in the order they happened; only the calendar date moves.

An edited row is flagged `userEdited` and is then treated like a manual one: **Reparse**
neither deletes nor overwrites it, and re-ingesting the same message leaves it alone.
Correcting a misparsed amount is pointless if the next sync reverts it. Fixing the rule
behind the misparse is still the better repair — an edited row stops tracking its message
for good.

Editing also clears `needsReview`: the user has said what the row is.

Delete stays manual-only, and is the only button on a row. A parsed row would return on
the next sync, so a delete button on it would look broken.

## How it works

Raw message text is stored first and never deleted. Parsing runs off those stored rows,
so a rule fixed today re-parses years of history via **Reparse**. The phone's SMS inbox
drops old messages over time; once a message is in this database it is the only copy left.

Rules live in a database table rather than in code, so a new bank format is added at
runtime. Each rule needs a regex with a named `amount` group; `acct`, `merchant`, `ref`
and `balance` are optional and fall back to generic extractors. Lower `priority` wins, so
bank-specific rules should sit below the seeded generic ones (900+).

Amounts are stored as `Long` minor units (paise), never `Double`.

Transactions that matched only a generic rule, or whose merchant could not be read, are
flagged `needsReview` instead of being presented as clean data.

The same transaction often arrives twice — once by SMS, once by a banking app. Where the
bank prints a reference number, the transaction id is derived from it, so the duplicate
collapses onto the same row.

## Backup format

One JSON file holding raw messages, transactions, rules, categories and the sync
watermark, carrying a `schemaVersion`. Both directions stream, so a multi-year inbox does
not have to fit in memory.

Import offers two modes:

- **Merge** — adds what is missing. Ids are content hashes, so importing the same file
  twice changes nothing.
- **Replace** — wipes the database first. The file's header is validated before anything
  is deleted, and the current data is written to the app cache beforehand. The cache is
  not durable storage; export first if the data matters.

## Status

Compiles and packages. `./gradlew test` passes 80 unit tests; `./gradlew assembleDebug`
produces an 11 MB APK (minSdk 26, targetSdk 35). The built APK was checked for declared
permissions: `READ_SMS` and `RECEIVE_SMS` only, no `INTERNET`.

The schema migrations (1→2→3→4→5) were verified by executing them in sequence against a real
SQLite database built from the exported v1 schema and seeded with data: columns, indices
and foreign keys match what Room expects at runtime, existing rows survive, a null-`rawId`
manual expense inserts, and a template rule inserts.

Not run on a device yet — nothing here has seen a real bank SMS.

Not yet implemented: the review-queue and rule-editor screens (the data layer and
`needsReview` flag behind them are in place), category assignment in the UI, backdating a
manual expense (it is always dated today), and the notification listener for bank apps
that never send SMS.
