# Google Stitch prompt

Paste into Google Stitch to generate a minimal UI for the app as it exists today. Every
screen, state and action below is implemented, so what Stitch returns maps onto real
behaviour rather than flows that would have to be thrown away.

Stitch works best with **one screen per prompt**: paste the shared context, then one
screen section. Ask for both the light and the dark variant of each screen.

---

## Shared context (include with every screen prompt)

```
Design a minimal Android app called "Expense Tracker", Material 3.

What it does: reads the user's bank SMS and bank/wallet app notifications on the phone
and turns them into a spending ledger. Fully offline: no login, no cloud, no network.
One person, Nepal. Money is Nepali rupees written रु with lakh grouping (रु1,23,456.78).

Style:
- Minimal. Plenty of whitespace, few borders, no gradients, no illustrations, no logos.
- Light and dark variants of every screen.
- One accent colour, user-selectable (terracotta by default; also sage, slate teal,
  indigo, plum, or a custom colour). The accent is used only for the primary action,
  selected states and the period total. Show the design with the default accent and note
  where the accent applies.
- Money out and money in use fixed red/green tints that do NOT change with the accent,
  and always carry a sign (−रु450.00 / +रु5,000.00) so colour is never the only cue.
- Amounts use tabular figures, right-aligned in lists.
- Typography carries hierarchy; the period total is the largest element on the ledger.
- Lists of transactions read as one grouped card divided by hairlines, not separate cards.
- Rows are the tap target; avoid rows full of buttons.

Navigation: bottom navigation bar with five labelled tabs:
Ledger, Trends, Loans, Accounts, Settings. Tabs are swipeable pages. No drawer.
A floating "Add new" button appears on the Ledger tab only.
Dates can be shown on the Gregorian or the Nepali (Bikram Sambat) calendar, per a setting.
```

---

## 1. Ledger (home tab)

```
Screen: Ledger.

Top bar: title "Ledger"; on the right a text button "Duplicates" and a small "Offline" badge.

Period chips (single select, horizontally scrollable): Today (default), Yesterday,
Last 7 days, Last 30 days, Custom (opens a date-range picker).

Hero card for the selected period:
- label with the period name ("Today", or "2026-09-01 to 2026-09-15" for custom)
- very large total spent, e.g. "रु1,240.00" with the word "spent"
- spending-limit bar when a limit is set: "रु14,418.00 of रु15,000.00 monthly limit",
  "रु582.00 left"; turns to a warning style when over ("रु49,418.00 over your monthly limit")
- a divider, then "Total inflow  +रु5,000.00 received"

Optional banner when SMS permission is missing: "SMS access needed — Grant it in Settings".
Optional status strip after an action ("Updated रु175.00. Filed under Dining.") with Dismiss.

Section header "Transactions" with "12 items".

Transaction row:
- left: round icon — the category emoji, else the account's icon/app icon, else a
  letter monogram; a small ↑/↓ badge for money in/out
- title: merchant, else the user's remark, else "Unknown"
- optional second line: remark when there is also a merchant
- meta line: account name (or "Unlinked sender" / "Added by you") · "Today, 2:15 PM"
  · "··1234" account tail · extra tags like "Lent · Ram" or "Split · रु1,800 pending"
- right: signed amount, and a small status chip: Parsed / Review / Edited / Manual /
  Loan / Share / Split ("Review" uses an error-tinted chip)

Interactions:
- Tap a row: opens the Edit transaction dialog.
- Long-press a row: enters selection mode; the row turns tinted with a check icon.
  In selection mode taps add/remove rows; the top bar becomes
  "✕  3 selected  🗑". Back or ✕ clears. 🗑 asks "Delete 3 transactions? Their messages
  are kept but will not be booked again" with a destructive Delete button.

Empty state card: "Nothing in this period — Add an expense, or sync your messages."
Footer: small lock row "Zero cloud uploads. Messages and parsing stay on this handset."
```

## 2. Add / Edit transaction dialog

```
Dialog, used for "Add expense" (from the floating button) and "Edit transaction".

Fields, top to bottom:
- Amount (large, रु prefix, decimal keyboard)
- "Spent on" (the remark), placeholder "e.g. Auto fare"
- Two pills: Spent / Received
- Edit only — Category: a dropdown field that becomes a search box when opened, first
  option "No category", each option "🥛 Dairy". Saved as soon as it is picked.
  If the user leaves it alone and changes the remark, the app files the transaction by
  keywords on Save.
- Edit only — one row of link actions, depending on the transaction:
    money out: [Split bill] [Mark as loan]
    money in:  [Share of split] [Mark as loan]
  or, once linked, a single line with an action:
    "Loan: lent · Ram"             [Change]
    "Split with 3 · रु1,800 pending" [Edit]
    "Share of "Dinner" from Ram"   [Change]
- Edit only — source message block(s): label "Text message from SanimaBank" or
  "App notification from Gmail", then the original message text in a small scrollable,
  selectable box. Extra blocks for duplicate reports:
  "Also reported by app notification from Gmail, not counted again".
- Date line (e.g. "Ashwin 8, 2083 · 24 Sept 2026") with a "Change date" text button.
- Edit only, parsed rows — note: "Parsed from a message. Your edit is kept when messages
  are reparsed."
Buttons: Cancel, primary "Add" / "Save" (disabled until an amount is entered).
The dialog body scrolls.
```

## 3. Duplicates (reached from the Ledger top bar)

```
Screen replacing the ledger content, bottom bar still visible.
Top bar: back arrow, title "Duplicates".

Same period chips as the ledger, default Today.
Section header: period name and "3 folded".

Explanation of the concept: when a bank reports one transaction twice (e.g. an SMS and an
email notification), the later report is folded into the first instead of being counted.

Card per folded report:
- signed amount, large
- "Duplicate: app notification from Gmail", time, the message text (4 lines max)
- divider
- "Counted as: text message from SanimaBank", time, that message text
- right-aligned outlined button "Not a duplicate" — moves it to the ledger as its own
  transaction; a status strip confirms.

Empty state: "No duplicates in this period".
```

## 4. Trends tab

```
Screen: Trends. Top bar title "Trends".

Month switcher row: "← Previous", month name centered ("Ashwin 2083" or "September 2026"),
"Next →" (disabled on the current month).

Card "Daily spend" with "7 days recorded": a simple line/area chart of spend per day of
the month, and beneath it "Busiest day: 7 · रु52,080.00".

"Total spent this month" with a very large amount, and a comparison line:
"25.5% higher than last month (रु51,347.00)" (higher = warning tint, lower = calm tint).

Section "By category" with "5 categories": one card per category — emoji, name,
"32.4% of total", amount on the right, a thin progress bar showing the share.
"Uncategorised" appears as its own bucket.

Totals exclude money marked as a loan, and a split bill counts the full bill minus what
friends have already paid back.

Empty state: "Nothing recorded this month".
```

## 5. Loans tab

```
Screen: Loans. Top bar title "Loans", text action "Add".

Balance card:
- label "Balance", large line: "You are owed रु5,000.00" / "You owe रु2,000.00" /
  "Even, रु0.00 net" (when both sides have open balances that cancel) / "All settled"
- divider, then two tappable totals side by side: "Owed to you रु50,000.00" (green),
  "You owe रु50,000.00" (red). Tapping one filters the list below.

Filter chips with counts: All · 2, Owe you · 1, You owe · 1, Settled · 0, Splits · 1.

List (All / Owe you / You owe / Settled): header shows the filter name and, for the two
money filters, the outstanding sum. Grouped rows: letter monogram, person name,
"3 entries", right side "Owes you रु3,000.00" / "You owe रु600.00" / "Settled".
Tap a person → Person screen.

Splits filter: one card per split bill — title and total, "Your share रु600 · रु1,800
pending", then one line per friend: name, "Owes रु600 of रु600" or "Paid रु600", and a
"Paid cash" text button for anyone who still owes.

Empty states: "No loans yet — Tap Add for cash, or open a bank transaction in the ledger
and mark it as a loan so it stops counting as spending."
```

## 6. Person (inside Loans)

```
Top bar: back arrow, the person's name, "Add".
Card with the balance line ("Owes you रु3,000.00").
Section "History · 4": grouped rows, newest first — kind ("Lent", "Got back", "Borrowed",
"Paid back", or "Share of Dinner" / "Paid for Dinner"), date · "Bank transaction" or
"Cash", optional note, signed amount on the right (out = red −, in = green +).
Tap an entry → Loan entry dialog in edit mode.
```

## 7. Loan entry dialog

```
Title: "Add loan" / "Mark as loan" (when opened from a bank transaction) / "Edit loan".
- Person text field with a contact-picker icon button; below it suggestion chips of
  people already used, filtered as you type.
- Kind pills: Lent, Got back, Borrowed, Paid back (only the two that fit the money's
  direction when linked to a transaction), and a one-line explanation of the chosen kind.
- Amount (रु prefix). Read-only when linked to a bank transaction.
- Date line with "Change date" (hidden when linked).
- Note field.
- When linked: helper text "Amount and date come from the bank transaction. It stays in
  the ledger but no longer counts as spent or received."
- Destructive text button in edit mode: "Delete entry" or "Not a loan".
Buttons: Cancel, Save (disabled until person and amount are valid).
```

## 8. Split bill dialog (from a money-out transaction)

```
Title "Split bill" / "Edit split".
- "What for" field, prefilled from the transaction.
- "Add a person" field with add and contact-picker icons; suggestion chips below.
- Checkbox "I had a share too" (on by default).
- Pills: Equal / Custom.
- One line per added friend: name, their share (text in Equal, an amount field in Custom),
  and a remove ✕.
- Summary line: "Your share रु600.00 of रु2,400.00. Spending drops as friends pay you
  back." Error-tinted when custom shares exceed or don't add up to the bill.
- Edit mode: destructive "Remove split".
Buttons: Cancel, Save.
```

## 9. Share of split dialog (from a money-in transaction, or "Paid cash")

```
Title "Share of a split".
- "Who paid": chips of people who still owe on any split, largest debt first,
  e.g. "Ram · रु600.00".
- "For": chips of that person's open splits, e.g. "Dinner · रु600.00" (auto-selected when
  there is only one).
- Amount: fixed to the bank credit's amount (read-only), or editable for cash, defaulting
  to what they still owe.
- Empty case: "No one owes you on a split. Split a bill from its transaction first."
Buttons: Cancel, Save.
```

## 10. Accounts tab

```
Screen: Accounts. Top bar title "Accounts".

Section "Banks & services":
- Add row: round "+" icon swatch (opens icon picker), name field "Name, e.g. Nabil or
  eSewa", "Add" button.
- One card per account:
  - top row: round icon (emoji or an installed app's icon; tap to change), name,
    text buttons "Icon" and "Delete"
  - label "Notifications from", then chips of apps whose notifications belong to this
    account (app icon + app name + ✕ to remove), and a "+ App" chip.
    The same app (e.g. Gmail) may be added to several accounts; the account whose name
    appears in the message is used.

Section "Linked senders" with "126 unlinked":
- helper text: only senders the user linked are listed here
- primary button "Find a sender (126 unlinked)" → search sheet
- one card per linked SMS sender: "Linked" chip, "687 messages", sender id (e.g.
  GBIME_ALERT) with raw variants below, a searchable dropdown to pick the account, an
  "Unlink" text button, and a switch row "Ask what it was for — A notification you can
  reply to, right after a payment."
```

## 11. Find a sender (bottom sheet)

```
Modal bottom sheet, title "Find a sender".
Helper: searches sender ids and message text, so a 5-digit short code is found by the
wallet name its messages use.
Search field with clear ✕. Header "Unlinked senders" (empty query) or "Matching senders".
Same sender cards as the Accounts tab ("Needs bank" chip when unlinked). Stays open after
linking so several senders can be cleared in one go.
```

## 12. App picker dialog ("+ App" on an account)

```
Title "Notifications from".
Section "Already sent notifications": rows with app icon + name.
Divider, section "All apps": search field and a scrollable list of installed apps.
Tap a row to add it. If the account had no icon, it takes the app's icon.
Cancel button.
```

## 13. Settings tab

```
Screen: Settings. A menu of cards, each with a title, one-line summary and chevron.
Opening one replaces the list, with a back arrow in the top bar.

- Spending limit — "A daily or monthly cap on spending": amount field, Daily / Monthly
  pills, live progress preview, Save / Remove limit.
- Categories & keywords — see section 14.
- Calendar — "Read dates on the English or the Nepali calendar": two options.
- Permissions — "What the app is allowed to read": SMS access and Notification access,
  each with status and a Grant / Open settings button.
- Messages & parsing — "Sync the inbox, rescan it, or rebuild transactions": buttons
  "Sync SMS" and "Full rescan" (with a note that rescanning is safe to repeat), then
  "Reparse" (rebuilds transactions from stored messages with the current templates;
  hand-added expenses untouched); a status line with the result.
- Parser templates — see section 15.
- Appearance — "Theme and accent colour": System / Light / Dark pills; accent swatches
  (terracotta, sage, slate teal, indigo, plum, custom colour picker); a small live preview
  of the primary, debit and credit roles.
- Backup — "Export or import everything as one file": Export button; Import button that
  asks Replace (wipe and load — for a new phone) or Merge (keep and add missing).
```

## 14. Categories & keywords

```
Helper text: a transaction is filed by the first keyword that appears in its merchant or
remark, longest match first; keywords are comma separated and not case-sensitive.
Buttons: primary "Add category", outlined "Apply to uncategorised".
One card per category: emoji icon, name, its keywords (2 lines max), keyword count chip.
Tap → editor dialog:
- Icon field (small) + Name field
- a grid of one-tap emoji: 🛒 🍽️ ☕ 🥛 🍞 🍺 🏍️ 🚗 ⛽ 🚌 ⚡ 📱 🏠 🧰 💊 🏥 🎓 👕 🎬 🎁 ✈️ 💇 🐾 💳
  (selected one tinted with the accent)
- Keywords, comma separated (multi-line), placeholder "biryani, momo, restaurant"
- helper: keywords match anywhere in the text; existing transactions keep their category
- "Delete category" (destructive) in edit mode
Buttons: Cancel, Save.
```

## 15. Parser templates

```
Screen replacing Settings, back arrow, title "Templates", "Help" action.

Card "New template" (tag "Editor"):
- helper: paste a real transaction SMS, then replace the parts that change with tokens
- "Sample message from your bank" (multi-line)
- outlined "Copy to template"
- "Pattern match rule" (monospace, tokens like {amount} tinted inline). While this field
  is focused, a row of token chips appears under it:
  {amount} {acct} {merchant} {remark} {ref} {date} {balance} {any}
  Tapping a chip inserts it at the cursor (replacing any selection).
- Live preview card: "Type a template to see what it would pick out", or the extracted
  fields (Amount, Account, Merchant, Remark, Ref, Date, Balance) from the sample, or an
  error like "Missing {amount}".
- "Template name"
- "Transaction nature": Debit (Expense) / Credit (Income) pills
- "Scope SMS sender": searchable dropdown, default "Any sender"
- full-width "Save template" (disabled until valid)

Below: section "Your templates" with "2 active" — each: name, the template text in
monospace, direction chip, sender scope, enable switch, Delete. Empty: "None yet. The
built-in fallbacks below handle common wording."
Section "Built-in rules": "Generic fallbacks. Anything these catch is flagged for review."
— name and an enable switch each.
After saving, a status strip offers "Reparse" to apply the template to stored messages.
"Tokens" help dialog: what each token accepts.
```

## 16. First-run permission dialog

```
Dialog shown once on first launch when permissions are missing.
Title: "Your spending, read from your own phone".
Line: "Nothing is uploaded. There is no account and no network — the app cannot reach one."
Two rows, each with title, reason, status and a button:
- "Read messages" — "Bank SMS already on this phone become your ledger, going back as far
  as your inbox does." Button "Allow messages".
- "Read notifications" — "Banks and wallets that no longer send an SMS still post a
  notification. Only ones naming an amount are kept." Button "Open settings", footnote
  "Android has no popup for this one — it is a switch in a system list."
Dismiss: "Not now".
```

## 17. "What was it for?" notification (system notification, for reference)

```
After a payment from a sender with "Ask what it was for" on, the app posts a notification:
title "रु450.00 at Nabil", text "What was it for?", with an inline reply field. The reply becomes the
transaction's remark. Design the in-app icon/branding only; the rest is system UI.
```
