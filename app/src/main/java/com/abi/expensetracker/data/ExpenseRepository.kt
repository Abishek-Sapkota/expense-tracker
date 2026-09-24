package com.abi.expensetracker.data

import com.abi.expensetracker.notification.NotificationIngest
import android.content.Context
import com.abi.expensetracker.data.db.AppDatabase
import com.abi.expensetracker.data.db.CategoryTotal
import com.abi.expensetracker.data.db.DuplicateRow
import com.abi.expensetracker.data.db.SenderCount
import com.abi.expensetracker.data.db.SplitRecovery
import com.abi.expensetracker.data.db.TxnWithSender
import com.abi.expensetracker.data.model.Bank
import com.abi.expensetracker.data.model.BankApp
import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.LoanKind
import com.abi.expensetracker.data.model.Split
import com.abi.expensetracker.data.model.MessageFlag
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.SenderLink
import com.abi.expensetracker.data.model.Txn
import com.abi.expensetracker.data.model.TxnCopy
import com.abi.expensetracker.data.model.Rule
import com.abi.expensetracker.parser.DefaultRules
import com.abi.expensetracker.parser.ParseOutcome
import com.abi.expensetracker.notification.RemarkPrompt
import com.abi.expensetracker.parser.SmsParser
import com.abi.expensetracker.parser.TemplateCompiler
import com.abi.expensetracker.sms.SmsInboxReader
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * How far apart two identical messages may be and still count as one.
 *
 * Six hours, not a few seconds: a message received while the phone was off reaches the
 * inbox with the receipt time and the broadcast with the SMSC time, and those can sit
 * hours apart. Two genuinely separate payments carry different reference numbers or a
 * different balance, so their text is not identical to the byte.
 */
private const val DUPLICATE_WINDOW_MILLIS = 6 * 60 * 60 * 1000L

/**
 * Bump when parsing or de-duplication logic changes, so the next start reparses stored
 * messages under the new logic instead of leaving history as the old code read it.
 */
const val PARSER_VERSION = 1

class ExpenseRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore
) {

    fun observeTransactions(): Flow<List<Txn>> = db.txnDao().observeAll()
    fun observeReviewQueue(): Flow<List<Txn>> = db.txnDao().observeNeedingReview()

    /**
     * Debits in [range], less what friends have paid back on split bills dated in it: a
     * shared dinner costs the user their share once the others have paid.
     */
    fun observeSpentBetween(range: DateRange): Flow<Long> = combine(
        db.txnDao().observeSpentBetween(range.startMillis, range.endMillis),
        observeSplitRecoveries(range)
    ) { spent, recoveries -> spent - recoveries.sumOf { it.recoveredMinor } }

    fun observeLoanDebitsBetween(range: DateRange): Flow<Long> =
        db.txnDao().observeLoanDebitsBetween(range.startMillis, range.endMillis)

    fun observeSplitRecoveries(range: DateRange): Flow<List<SplitRecovery>> =
        db.splitDao().observeRecoveries(range.startMillis, range.endMillis)

    fun observeReceivedBetween(range: DateRange): Flow<Long> =
        db.txnDao().observeReceivedBetween(range.startMillis, range.endMillis)

    fun observeBetween(range: DateRange): Flow<List<TxnWithSender>> =
        db.txnDao().observeBetweenWithSender(range.startMillis, range.endMillis)

    fun observeBanks(): Flow<List<Bank>> = db.bankDao().observeAll()
    fun observeCategories(): Flow<List<Category>> = db.categoryDao().observeAll()

    fun observeDebitsBetween(range: DateRange): Flow<List<Txn>> =
        db.txnDao().observeDebitsBetween(range.startMillis, range.endMillis)

    fun observeCategoryTotals(range: DateRange): Flow<List<CategoryTotal>> =
        db.txnDao().observeCategoryTotals(range.startMillis, range.endMillis)
    fun observeSenderLinks(): Flow<List<SenderLink>> = db.senderLinkDao().observeAll()
    fun observeSenders(): Flow<List<SenderCount>> = db.rawMessageDao().observeSenders()

    /**
     * Every message behind a transaction: the one it was parsed from, then any copies of
     * it that arrived from another channel. Empty for a manual entry.
     */
    suspend fun sourceMessages(txn: Txn): List<RawMessage> = withContext(Dispatchers.IO) {
        val primary = txn.rawId?.let { db.rawMessageDao().byId(it) } ?: return@withContext emptyList()
        listOf(primary) + db.txnCopyDao().messagesFor(txn.id)
    }

    suspend fun parserOutdated(): Boolean = settings.parserVersionOnce() < PARSER_VERSION

    /**
     * Normalised sender keys whose id or message text contains [text].
     *
     * Normalised, so a hit on the raw "AX-NABIL" matches the one row the settings screen
     * shows for Nabil rather than a variant it does not list.
     */
    suspend fun senderKeysMatching(text: String): Set<String> = withContext(Dispatchers.IO) {
        db.rawMessageDao().sendersMatching(text)
            .map { SenderNormalizer.normalize(it) }
            .toSet()
    }

    suspend fun bankCount(): Int = withContext(Dispatchers.IO) { db.bankDao().all().size }

    suspend fun addBank(name: String, icon: String? = null): Long = withContext(Dispatchers.IO) {
        db.bankDao().insert(Bank(name = name.trim(), icon = icon?.takeIf { it.isNotBlank() }))
    }

    suspend fun setBankIcon(bank: Bank, icon: String?) = withContext(Dispatchers.IO) {
        db.bankDao().update(bank.copy(icon = icon?.takeIf { it.isNotBlank() }))
    }

    suspend fun deleteBank(bankId: Long) = withContext(Dispatchers.IO) {
        db.bankAppDao().deleteForBank(bankId)
        // Drop the links first: leaving them would silently re-attach every mapped sender
        // to whatever bank next reuses that autoincrement id.
        db.senderLinkDao().unlinkAllFor(bankId)
        db.bankDao().delete(bankId)
    }

    /** [sender] is a raw sender id; it is normalised here so every circle variant maps. */
    fun observeBankApps(): Flow<List<BankApp>> = db.bankAppDao().observeAll()

    /**
     * Claims an app's notifications for [bank]. A bank with no icon yet takes the app's,
     * since that icon is how the user recognises the account.
     */
    suspend fun addBankApp(bank: Bank, packageName: String) = withContext(Dispatchers.IO) {
        db.bankAppDao().insert(BankApp(packageName, bank.id))
        if (bank.icon == null) db.bankDao().update(bank.copy(icon = AppIconRef.of(packageName)))
    }

    suspend fun removeBankApp(bank: Bank, packageName: String) = withContext(Dispatchers.IO) {
        db.bankAppDao().delete(packageName, bank.id)
    }

    suspend fun linkSender(sender: String, bankId: Long) = withContext(Dispatchers.IO) {
        db.senderLinkDao().upsert(SenderLink(SenderNormalizer.normalize(sender), bankId))
    }

    suspend fun unlinkSender(sender: String) = withContext(Dispatchers.IO) {
        db.senderLinkDao().unlink(SenderNormalizer.normalize(sender))
    }

    // ---------- Parser templates ----------

    fun observeRules(): Flow<List<Rule>> = db.ruleDao().observeAll()

    /**
     * @param senderKey normalised sender this template applies to, or null for any sender.
     * @return the new rule id, or null when the template does not compile.
     */
    suspend fun addTemplateRule(
        name: String,
        senderKey: String?,
        template: String,
        direction: Direction
    ): Long? = withContext(Dispatchers.IO) {
        val compiled = TemplateCompiler.compile(template) as? TemplateCompiler.Outcome.Ok
            ?: return@withContext null

        db.ruleDao().insert(
            Rule(
                name = name.trim().ifBlank { senderKey ?: "Custom template" },
                // Matched against the raw sender, which still contains the circle prefix,
                // so a normalised key finds every variant of it.
                senderPattern = senderKey?.let { Regex.escape(it) } ?: ".*",
                bodyPattern = compiled.regex,
                direction = direction,
                // Below the seeded generics (900+) so a user's own template always wins.
                priority = 100,
                builtIn = false,
                template = template.trim()
            )
        )
    }

    suspend fun setRuleEnabled(rule: Rule, enabled: Boolean) = withContext(Dispatchers.IO) {
        db.ruleDao().update(rule.copy(enabled = enabled))
    }

    suspend fun deleteRule(ruleId: Long) = withContext(Dispatchers.IO) {
        db.ruleDao().delete(ruleId)
    }

    suspend fun addManualExpense(
        amountMinor: Long,
        occurredAt: Long,
        direction: Direction = Direction.DEBIT,
        remark: String = ""
    ) = withContext(Dispatchers.IO) {
        val txn = Txn(
            id = Txn.manualId(),
            rawId = null,
            amountMinor = amountMinor,
            direction = direction,
            accountTail = null,
            // No merchant is recorded by hand any more, so the ledger falls back to
            // the remark for this row's title — "Auto fare" says more than "Cash".
            merchant = null,
            remark = remark.trim().ifBlank { null },
            refNumber = null,
            balanceMinor = null,
            occurredAt = occurredAt,
            needsReview = false
        )
        // Through the keywords, same as a parsed row: typing "biryani" should land in
        // Dining without the user then having to say so.
        db.txnDao().insert(txn.copy(categoryId = categorizer().categoryIdFor(txn)))
    }

    /**
     * Apply a hand correction to any transaction, parsed or manual.
     *
     * The row is flagged [Txn.userEdited] so a later reparse keeps it: the whole point of
     * correcting a misparsed amount is that it stays corrected.
     */
    suspend fun editTransaction(
        txn: Txn,
        amountMinor: Long,
        occurredAt: Long,
        direction: Direction,
        remark: String = "",
        autoCategorize: Boolean = true
    ): String? = withContext(Dispatchers.IO) {
        // Re-read, not the caller's copy: the editor saves the category the moment it is
        // picked, and writing back the snapshot the dialog opened with would undo it.
        val current = db.txnDao().byId(txn.id) ?: txn
        val edited = current.copy(
                amountMinor = amountMinor,
                // Left as parsed: the form no longer asks for it, so an edit must not
                // silently blank a merchant the message did state.
                remark = remark.trim().ifBlank { null },
                occurredAt = occurredAt,
                direction = direction,
                // The user has said what this row is, so it no longer needs review.
                needsReview = false,
                userEdited = true
        )
        // A new remark is a new chance for the keywords: "biryani" typed over a blank
        // should land in Dining without a second tap. Only when the category is still the
        // keywords' own — none, or what the old text matched — so a category the user
        // picked by hand is never overwritten.
        val categorizer = categorizer()
        val keywordsOwnIt = current.categoryId == null ||
            categorizer.categoryIdFor(current) == current.categoryId
        val categoryId = if (autoCategorize && keywordsOwnIt) {
            categorizer.categoryIdFor(edited) ?: current.categoryId
        } else current.categoryId
        db.txnDao().update(edited.copy(categoryId = categoryId))
        // The category's name when the edit changed it, for the confirmation line.
        if (categoryId != current.categoryId) {
            db.categoryDao().all().firstOrNull { it.id == categoryId }?.name
        } else null
    }

    /**
     * Deletes a transaction for good.
     *
     * A parsed row would be rebuilt from its message on the next reparse, so its message,
     * and any copies folded into it, are flagged deleted first; otherwise a copy would
     * surface as the transaction in its place.
     */
    suspend fun deleteTransaction(txn: Txn) = withContext(Dispatchers.IO) {
        db.withTransaction {
            txn.rawId?.let { rawId ->
                (listOf(rawId) + db.txnCopyDao().rawIdsFor(txn.id)).forEach { id ->
                    val flag = db.messageFlagDao().byId(id) ?: MessageFlag(id)
                    db.messageFlagDao().upsert(flag.copy(deleted = true))
                }
            }
            db.txnDao().delete(txn)
            db.txnCopyDao().deleteOrphans()
        }
    }

    /**
     * Messages from linked senders that read like a transaction but that no rule parsed,
     * newest first, each with the account it belongs to: the starters for a new template.
     */
    fun observeUnparsedFromLinked(): Flow<List<Pair<RawMessage, String>>> = combine(
        db.rawMessageDao().observeUnparsed(400),
        db.bankDao().observeAll(),
        db.senderLinkDao().observeAll(),
        db.bankAppDao().observeAll()
    ) { messages, banks, links, apps ->
        val resolver = BankResolver(banks, links, apps)
        messages.mapNotNull { m ->
            val bank = resolver.bankFor(m.sender, m.body) ?: return@mapNotNull null
            if (!NotificationIngest.looksFinancial(m.body)) return@mapNotNull null
            m to bank.name
        }
    }

    fun observeLoans(): Flow<List<LoanEntry>> = db.loanDao().observeAll()

    /** Adds or updates a loan entry; linking one to a transaction takes it out of totals. */
    suspend fun saveLoan(entry: LoanEntry) = withContext(Dispatchers.IO) {
        db.loanDao().upsert(entry.copy(person = entry.person.trim()))
    }

    suspend fun deleteLoan(entry: LoanEntry) = withContext(Dispatchers.IO) {
        db.loanDao().delete(entry)
    }

    fun observeSplits(): Flow<List<Split>> = db.splitDao().observeAll()

    /**
     * Creates or rewrites the split on [txn]. Shares are replaced wholesale; repayments
     * already recorded against the split are kept.
     */
    suspend fun saveSplit(
        txn: Txn,
        title: String,
        myShareMinor: Long,
        shares: List<Pair<String, Long>>
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val existing = db.splitDao().byTxnId(txn.id)
            val split = Split(
                id = existing?.id ?: 0,
                txnId = txn.id,
                title = title.trim().ifBlank { "Bill" },
                totalMinor = txn.amountMinor,
                myShareMinor = myShareMinor,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            val id = if (existing != null) {
                db.splitDao().upsert(split); existing.id
            } else db.splitDao().upsert(split)
            db.loanDao().deleteShares(id)
            db.loanDao().insertAll(shares.filter { it.second > 0 }.map { (person, amount) ->
                LoanEntry(
                    person = person.trim(), kind = LoanKind.LENT, amountMinor = amount,
                    occurredAt = txn.occurredAt, note = split.title, splitId = id
                )
            })
        }
    }

    /** The bill goes back to full spending; repayments become plain loan repayments. */
    suspend fun deleteSplit(split: Split) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.loanDao().deleteShares(split.id)
            db.loanDao().detachFromSplit(split.id)
            db.splitDao().delete(split)
        }
    }

    /**
     * A friend paid their share. Linked to the credit that carried it when there was one,
     * which also stops that credit counting as money received.
     */
    suspend fun recordSplitPayment(split: Split, person: String, amountMinor: Long, via: Txn?) =
        withContext(Dispatchers.IO) {
            db.loanDao().upsert(
                LoanEntry(
                    person = person.trim(), kind = LoanKind.RECEIVED_BACK, amountMinor = amountMinor,
                    occurredAt = via?.occurredAt ?: System.currentTimeMillis(),
                    note = split.title, txnId = via?.id, splitId = split.id
                )
            )
        }

    fun observeDuplicates(range: DateRange): Flow<List<DuplicateRow>> =
        db.txnCopyDao().observeBetween(range.startMillis, range.endMillis)

    /**
     * The user says a message folded in as a copy is a transaction of its own.
     *
     * Flagged so the matcher never folds it again, then parsed and booked like any new
     * message. Returns false when no rule parses it any more.
     */
    suspend fun markNotDuplicate(rawId: String): Boolean = withContext(Dispatchers.IO) {
        val message = db.rawMessageDao().byId(rawId) ?: return@withContext false
        val flag = db.messageFlagDao().byId(rawId) ?: MessageFlag(rawId)
        db.messageFlagDao().upsert(flag.copy(notDuplicate = true))
        db.txnCopyDao().delete(rawId)
        val txn = (SmsParser(db.ruleDao().enabled()).parse(message) as? ParseOutcome.Parsed)?.txn
            ?: return@withContext false
        insertParsed(categorizer().apply(listOf(txn)).map { it to message.sender }).isNotEmpty()
    }

    /**
     * Insert parsed rows without trampling hand-corrected ones.
     *
     * The parser produces the same stable id for the same message, so a plain REPLACE
     * would overwrite an edit with the parse it was correcting.
     */
    /**
     * A message folded into an existing row from another channel is recorded as a
     * [TxnCopy] rather than inserted, so the money is booked once. Rows go in oldest
     * first, so the earliest report of a transaction is the one kept.
     *
     * Takes each transaction with the sender of its message, and returns the rows actually
     * inserted.
     */
    private suspend fun insertParsed(parsed: List<Pair<Txn, String>>): List<Txn> {
        if (parsed.isEmpty()) return emptyList()
        return db.withTransaction {
            val edited = db.txnDao().editedIds().toSet()
            // Copies attached to an edited row survive a reparse; their messages must not
            // come back as transactions of their own.
            val absorbed = db.txnCopyDao().allRawIds().toMutableSet()
            val flags = db.messageFlagDao().all().associateBy { it.rawId }
            val inserted = mutableListOf<Txn>()
            for ((txn, sender) in parsed.sortedBy { it.first.occurredAt }) {
                val rawId = txn.rawId ?: continue
                if (txn.id in edited || rawId in absorbed) continue
                val flag = flags[rawId]
                if (flag?.deleted == true) continue
                val original = if (flag?.notDuplicate == true) null else findOriginal(txn, sender)
                if (original != null) {
                    db.txnCopyDao().insert(TxnCopy(rawId = rawId, txnId = original.id))
                    absorbed += rawId
                } else {
                    db.txnDao().insert(txn)
                    inserted += txn
                }
            }
            inserted
        }
    }

    private suspend fun findOriginal(txn: Txn, sender: String): Txn? {
        val window = DuplicateMatcher.WINDOW_MILLIS
        val rows = db.txnDao().copyCandidates(
            txn.amountMinor, txn.direction.name, txn.occurredAt - window, txn.occurredAt + window
        )
        if (rows.isEmpty()) return null
        val copySenders = db.txnCopyDao().sendersFor(rows.map { it.txn.id })
            .groupBy({ it.txnId }, { SenderNormalizer.normalize(it.sender) })
        val candidates = rows.mapNotNull { row ->
            val rowSender = row.sender ?: return@mapNotNull null
            DuplicateMatcher.Candidate(
                txn = row.txn,
                senderKey = SenderNormalizer.normalize(rowSender),
                copySenderKeys = copySenders[row.txn.id].orEmpty().toSet()
            )
        }
        return DuplicateMatcher.findOriginal(txn, SenderNormalizer.normalize(sender), candidates)
    }

    suspend fun seedRulesIfEmpty() = withContext(Dispatchers.IO) {
        if (db.ruleDao().count() == 0) db.ruleDao().insertAll(DefaultRules.ALL)
    }

    /**
     * Brings the built-in rules on an existing install up to date with [DefaultRules].
     *
     * Seeding alone only ever ran on an empty table, so a phone that had been syncing for
     * a week never saw a rule added later — its messages simply went on not parsing. Rules
     * are matched by name; a user's own templates are never touched, and a built-in the
     * user switched off stays off.
     *
     * Returns true when anything changed, so the caller can reparse and show the user the
     * transactions the new rules find.
     */
    /**
     * Removes copies of a message that the two ingest paths stored under different times.
     *
     * Returns how many went, so the caller can reparse and drop the transactions they had
     * produced. Cheap and idempotent: once the ingest guard has been in place for a sync
     * cycle it finds nothing.
     */
    suspend fun removeDuplicateMessages(): Int = withContext(Dispatchers.IO) {
        db.rawMessageDao().deleteNearDuplicates(DUPLICATE_WINDOW_MILLIS)
    }

    suspend fun syncBuiltInRules(): Boolean = withContext(Dispatchers.IO) {
        val existing = db.ruleDao().all().filter { it.builtIn }.associateBy { it.name }
        val merged = DefaultRules.ALL.map { seed ->
            val current = existing[seed.name] ?: return@map seed
            seed.copy(id = current.id, enabled = current.enabled)
        }
        val changed = merged.any { seed ->
            val current = existing[seed.name]
            current == null ||
                current.bodyPattern != seed.bodyPattern ||
                current.senderPattern != seed.senderPattern ||
                current.direction != seed.direction ||
                current.priority != seed.priority
        }
        if (changed) db.ruleDao().insertAll(merged)
        changed
    }

    suspend fun seedCategoriesIfEmpty() = withContext(Dispatchers.IO) {
        if (db.categoryDao().count() == 0) db.categoryDao().insertAll(DefaultCategories.ALL)
    }

    private suspend fun categorizer(): Categorizer = Categorizer(db.categoryDao().all())

    /**
     * Categorise rows that have none, leaving anything already categorised alone.
     *
     * Run on start after seeding, so a ledger that existed before categories did gets
     * them without the user asking, and so does history imported from a backup.
     */
    suspend fun categorizeUncategorized(): Int = withContext(Dispatchers.IO) {
        val pending = db.txnDao().uncategorized()
        if (pending.isEmpty()) return@withContext 0
        val updated = categorizer().apply(pending).filter { it.categoryId != null }
        if (updated.isNotEmpty()) db.txnDao().insertAll(updated)
        updated.size
    }

    suspend fun addCategory(name: String, icon: String, keywords: String): Long =
        withContext(Dispatchers.IO) {
            db.categoryDao().insert(
                Category(name = name.trim(), icon = icon.trim(), keywords = keywords.trim())
            )
        }

    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        db.categoryDao().update(
            category.copy(
                name = category.name.trim(),
                icon = category.icon.trim(),
                keywords = category.keywords.trim()
            )
        )
    }

    /**
     * Deletes a category and unsets it on every row that carried it.
     *
     * Leaving the id behind would show those rows as belonging to a category that no
     * longer exists, and the next category to take that autoincrement id would inherit
     * them.
     */
    suspend fun deleteCategory(categoryId: Long) = withContext(Dispatchers.IO) {
        db.txnDao().clearCategory(categoryId)
        db.categoryDao().delete(categoryId)
    }

    /** Sets the category by hand, which a keyword guess must never overwrite later. */
    suspend fun setCategory(txn: Txn, categoryId: Long?) = withContext(Dispatchers.IO) {
        db.txnDao().update(
            txn.copy(categoryId = categoryId, needsReview = false, userEdited = true)
        )
    }

    /**
     * Store raw messages, then parse them. Storing always happens, even if parsing fails.
     *
     * [prompt] is false for a bulk backfill: asking what each of four hundred historical
     * transactions was for would post four hundred notifications, and nobody remembers
     * last March anyway. Live messages ask; history does not.
     */
    suspend fun ingest(messages: List<RawMessage>, prompt: Boolean = true): Int =
        withContext(Dispatchers.IO) {
            val messages = messages.filter { message ->
                db.rawMessageDao().duplicateOf(
                    sender = message.sender,
                    body = message.body,
                    from = message.sentAt - DUPLICATE_WINDOW_MILLIS,
                    to = message.sentAt + DUPLICATE_WINDOW_MILLIS
                ) == null
            }
            db.rawMessageDao().insertAll(messages)
            val parser = SmsParser(db.ruleDao().enabled())
            val bySender = mutableMapOf<String, String>()
            val parsed = messages.mapNotNull { message ->
                val txn = (parser.parse(message) as? ParseOutcome.Parsed)?.txn
                if (txn != null) bySender[txn.id] = message.sender
                txn
            }
            val categorized = categorizer().apply(parsed)
            val inserted = insertParsed(categorized.map { it to bySender.getValue(it.id) })
            // A copy is a transaction already asked about, or about to be.
            if (prompt) askWhatFor(inserted, bySender)
            inserted.size
        }

    /**
     * Asks, for the rows worth asking about.
     *
     * The rules live in [RemarkPromptPolicy]; this only resolves the sender and the bank
     * name the notification shows.
     */
    private suspend fun askWhatFor(parsed: List<Txn>, senderById: Map<String, String>) {
        if (parsed.isEmpty()) return
        val optedIn = settings.remarkPromptSendersOnce()
        if (optedIn.isEmpty()) return

        val banks = db.bankDao().all()
        val links = db.senderLinkDao().all()
        val resolver = BankResolver(banks, links, db.bankAppDao().all())

        parsed.forEach { txn ->
            val sender = senderById[txn.id] ?: return@forEach
            val key = SenderNormalizer.normalize(sender)
            if (!RemarkPromptPolicy.shouldAsk(txn, key, optedIn)) return@forEach

            RemarkPrompt.ask(context, txn, resolver.bankFor(sender)?.name)
        }
    }

    /**
     * Applies an answer typed into the notification.
     *
     * Marked [Txn.userEdited] so a reparse keeps it — the row is being annotated precisely
     * because no rule could have produced this text.
     */
    suspend fun setRemarkFromPrompt(txnId: String, remark: String) = withContext(Dispatchers.IO) {
        val txn = db.txnDao().byId(txnId) ?: return@withContext
        db.txnDao().update(
            txn.copy(
                remark = remark.trim().ifBlank { null },
                needsReview = false,
                userEdited = true
            )
        )
    }

    /**
     * Pull SMS history into the app. Safe to run repeatedly: the watermark skips what was
     * already read, and stable ids make any overlap a no-op rather than a duplicate.
     */
    suspend fun backfillFromInbox(fromScratch: Boolean = false): BackfillResult =
        withContext(Dispatchers.IO) {
            val since = if (fromScratch) 0L else settings.lastSyncedSmsDateOnce()
            var read = 0
            var newest = since

            SmsInboxReader.read(context, since) { batch ->
                read += batch.size
                batch.maxOfOrNull { it.sentAt }?.let { if (it > newest) newest = it }
                ingest(batch, prompt = false)
            }

            if (newest > since) settings.setLastSyncedSmsDate(newest)
            BackfillResult(messagesRead = read, totalStored = db.rawMessageDao().count())
        }

    /**
     * Re-run parsing over every stored message. This is the payoff for keeping raw text:
     * a rule fixed today retroactively corrects years of history.
     */
    suspend fun reparseAll(): Int = withContext(Dispatchers.IO) {
        val parser = SmsParser(db.ruleDao().enabled())
        // Read once rather than per page: the category list does not change mid-reparse.
        val categorizing = categorizer()
        // Only the derived rows. Manual entries have no message to rebuild them from.
        db.txnDao().deleteParsed()
        db.txnCopyDao().deleteOrphans()

        var offset = 0
        val pageSize = 500
        var parsedCount = 0
        while (true) {
            val page = db.rawMessageDao().page(pageSize, offset)
            if (page.isEmpty()) break
            val parsed = page.mapNotNull { message ->
                (parser.parse(message) as? ParseOutcome.Parsed)?.txn?.let { it to message.sender }
            }
            val categorized = categorizing.apply(parsed.map { it.first })
            parsedCount += insertParsed(categorized.zip(parsed) { txn, (_, sender) -> txn to sender }).size
            offset += page.size
        }
        settings.setParserVersion(PARSER_VERSION)
        parsedCount
    }

    suspend fun markReviewed(txn: Txn) = withContext(Dispatchers.IO) {
        db.txnDao().update(txn.copy(needsReview = false))
    }
}

data class BackfillResult(val messagesRead: Int, val totalStored: Int)
