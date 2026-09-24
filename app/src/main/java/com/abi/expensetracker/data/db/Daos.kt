package com.abi.expensetracker.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.abi.expensetracker.data.model.Bank
import com.abi.expensetracker.data.model.BankApp
import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.MessageFlag
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Rule
import com.abi.expensetracker.data.model.SenderLink
import com.abi.expensetracker.data.model.Split
import com.abi.expensetracker.data.model.Txn
import com.abi.expensetracker.data.model.TxnCopy
import kotlinx.coroutines.flow.Flow

@Dao
interface RawMessageDao {

    /** IGNORE, not REPLACE: a row already present is the same message by construction
     *  of the id, and replacing it would cascade-delete its parsed transaction. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<RawMessage>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: RawMessage): Long

    @Query("SELECT * FROM raw_messages ORDER BY sentAt ASC")
    suspend fun all(): List<RawMessage>

    @Query("SELECT * FROM raw_messages ORDER BY sentAt ASC LIMIT :limit OFFSET :offset")
    suspend fun page(limit: Int, offset: Int): List<RawMessage>

    @Query("SELECT COUNT(*) FROM raw_messages")
    suspend fun count(): Int

    @Query(
        "SELECT sender AS sender, COUNT(*) AS messageCount FROM raw_messages " +
            "GROUP BY sender ORDER BY messageCount DESC"
    )
    fun observeSenders(): Flow<List<SenderCount>>

    /**
     * Senders whose id or message text contains [text].
     *
     * Body as well as sender, because a short code says nothing: a user looking for eSewa
     * types "esewa" and finds the 5-digit sender whose messages say it. LIKE over the body
     * has no index behind it and scans, which is why this runs per search rather than as a
     * flow the screen keeps open.
     */
    @Query(
        "SELECT DISTINCT sender FROM raw_messages " +
            "WHERE sender LIKE '%' || :text || '%' OR body LIKE '%' || :text || '%'"
    )
    suspend fun sendersMatching(text: String): List<String>

    /**
     * An existing copy of this exact message stored within [from]..[to], or null.
     *
     * Same text from the same sender at nearly the same time is the same message reaching
     * the app twice, not a bank that billed twice: the live broadcast timestamps a message
     * by the SMSC clock and the inbox by the phone's receipt clock, so the two paths
     * disagree by seconds over one SMS.
     */
    @Query(
        "SELECT id FROM raw_messages " +
            "WHERE sender = :sender AND body = :body AND sentAt BETWEEN :from AND :to LIMIT 1"
    )
    suspend fun duplicateOf(sender: String, body: String, from: Long, to: Long): String?

    /** Drops copies stored before the guard existed, keeping the earliest of each. */
    @Query(
        "DELETE FROM raw_messages WHERE EXISTS (" +
            "SELECT 1 FROM raw_messages o " +
            "WHERE o.sender = raw_messages.sender AND o.body = raw_messages.body " +
            "AND o.sentAt < raw_messages.sentAt " +
            "AND raw_messages.sentAt - o.sentAt <= :windowMillis)"
    )
    suspend fun deleteNearDuplicates(windowMillis: Long): Int

    @Query("SELECT * FROM raw_messages WHERE id = :id")
    suspend fun byId(id: String): RawMessage?

    /**
     * Newest messages that produced nothing: no transaction, not folded in as a duplicate,
     * not deleted by the user. The template screen offers the financial ones from linked
     * senders as starters.
     */
    @Query(
        "SELECT * FROM raw_messages WHERE " +
            "id NOT IN (SELECT rawId FROM transactions WHERE rawId IS NOT NULL) " +
            "AND id NOT IN (SELECT rawId FROM txn_copies) " +
            "AND id NOT IN (SELECT rawId FROM message_flags WHERE deleted = 1) " +
            "ORDER BY sentAt DESC LIMIT :limit"
    )
    fun observeUnparsed(limit: Int): Flow<List<RawMessage>>

    @Query("DELETE FROM raw_messages")
    suspend fun deleteAll()
}

@Dao
interface TxnDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txns: List<Txn>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(txn: Txn)

    @Update
    suspend fun update(txn: Txn)

    @Delete
    suspend fun delete(txn: Txn)

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<Txn>>

    @Query("SELECT * FROM transactions WHERE needsReview = 1 ORDER BY occurredAt DESC")
    fun observeNeedingReview(): Flow<List<Txn>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt ASC")
    suspend fun all(): List<Txn>

    // Totals leave out transactions marked as a loan: money lent or borrowed is owed, not
    // spent or earned. The rows themselves still list in the ledger.

    /** Half-open range: [from, to). An inclusive end would double-count midnight. */
    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM transactions " +
            "WHERE direction = 'DEBIT' AND occurredAt >= :from AND occurredAt < :to " +
            "AND id NOT IN (SELECT txnId FROM loan_entries WHERE txnId IS NOT NULL)"
    )
    fun observeSpentBetween(from: Long, to: Long): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM transactions " +
            "WHERE direction = 'CREDIT' AND occurredAt >= :from AND occurredAt < :to " +
            "AND id NOT IN (SELECT txnId FROM loan_entries WHERE txnId IS NOT NULL)"
    )
    fun observeReceivedBetween(from: Long, to: Long): Flow<Long>

    // LEFT JOIN, not INNER: a manually entered transaction has no raw message, and an
    // inner join would hide it from the list and the totals it is counted in.
    @Query(
        "SELECT t.*, r.sender AS sender, r.body AS body FROM transactions t " +
            "LEFT JOIN raw_messages r ON r.id = t.rawId " +
            "WHERE t.occurredAt >= :from AND t.occurredAt < :to " +
            "ORDER BY t.occurredAt DESC"
    )
    fun observeBetweenWithSender(from: Long, to: Long): Flow<List<TxnWithSender>>

    /** Debits only, oldest first: the series the trends chart plots. */
    @Query(
        "SELECT * FROM transactions " +
            "WHERE direction = 'DEBIT' AND occurredAt >= :from AND occurredAt < :to " +
            "AND id NOT IN (SELECT txnId FROM loan_entries WHERE txnId IS NOT NULL)" + " ORDER BY occurredAt ASC"
    )
    fun observeDebitsBetween(from: Long, to: Long): Flow<List<Txn>>

    /** Spend per category over a window. A null categoryId row is the uncategorised total. */
    @Query(
        "SELECT categoryId AS categoryId, COALESCE(SUM(amountMinor), 0) AS totalMinor " +
            "FROM transactions " +
            "WHERE direction = 'DEBIT' AND occurredAt >= :from AND occurredAt < :to " +
            "AND id NOT IN (SELECT txnId FROM loan_entries WHERE txnId IS NOT NULL)" + " GROUP BY categoryId ORDER BY totalMinor DESC"
    )
    fun observeCategoryTotals(from: Long, to: Long): Flow<List<CategoryTotal>>

    /** Debits in [from, to) marked as a loan, which the spending totals leave out. */
    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM transactions " +
            "WHERE direction = 'DEBIT' AND occurredAt >= :from AND occurredAt < :to " +
            "AND id IN (SELECT txnId FROM loan_entries WHERE txnId IS NOT NULL)"
    )
    fun observeLoanDebitsBetween(from: Long, to: Long): Flow<Long>

    /** Rows with no category yet, for a re-run after the keyword lists change. */
    @Query("SELECT * FROM transactions WHERE categoryId IS NULL")
    suspend fun uncategorized(): List<Txn>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: String): Txn?

    /** Ids a reparse must not touch, so an edit is never silently reverted. */
    /** Drops a deleted category from the rows that carried it. */
    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)

    @Query("SELECT id FROM transactions WHERE userEdited = 1")
    suspend fun editedIds(): List<String>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    /**
     * Reparse rebuilds only what was derived from messages. Manual entries have no
     * message behind them, and hand-corrected rows would lose the correction, so both
     * are kept.
     */
    @Query("DELETE FROM transactions WHERE rawId IS NOT NULL AND userEdited = 0")
    suspend fun deleteParsed()

    /** Parsed rows that a new message of this amount and direction could be a copy of. */
    @Query(
        "SELECT t.*, r.sender AS sender, r.body AS body FROM transactions t " +
            "JOIN raw_messages r ON r.id = t.rawId " +
            "WHERE t.amountMinor = :amountMinor AND t.direction = :direction " +
            "AND t.occurredAt BETWEEN :from AND :to"
    )
    suspend fun copyCandidates(
        amountMinor: Long,
        direction: String,
        from: Long,
        to: Long
    ): List<TxnWithSender>
}

@Dao
interface TxnCopyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(copy: TxnCopy)

    @Query("SELECT rawId FROM txn_copies")
    suspend fun allRawIds(): List<String>

    @Query(
        "SELECT c.txnId AS txnId, r.sender AS sender FROM txn_copies c " +
            "JOIN raw_messages r ON r.id = c.rawId WHERE c.txnId IN (:txnIds)"
    )
    suspend fun sendersFor(txnIds: List<String>): List<CopySender>

    @Query(
        "SELECT r.* FROM raw_messages r JOIN txn_copies c ON c.rawId = r.id " +
            "WHERE c.txnId = :txnId ORDER BY r.sentAt ASC"
    )
    suspend fun messagesFor(txnId: String): List<RawMessage>

    /** Copies whose transaction is gone, after a reparse or a delete. */
    @Query("DELETE FROM txn_copies WHERE txnId NOT IN (SELECT id FROM transactions)")
    suspend fun deleteOrphans()

    @Query("DELETE FROM txn_copies")
    suspend fun deleteAll()

    @Query("SELECT rawId FROM txn_copies WHERE txnId = :txnId")
    suspend fun rawIdsFor(txnId: String): List<String>

    @Query("DELETE FROM txn_copies WHERE rawId = :rawId")
    suspend fun delete(rawId: String)

    /** Copies whose message arrived in [from, to), with the transaction each was folded into. */
    @Query(
        "SELECT c.rawId AS rawId, r.sender AS sender, r.body AS body, r.source AS source, " +
            "r.sentAt AS sentAt, t.id AS txnId, t.amountMinor AS amountMinor, " +
            "t.direction AS direction, o.sender AS originalSender, o.body AS originalBody, " +
            "o.source AS originalSource, t.occurredAt AS originalAt " +
            "FROM txn_copies c " +
            "JOIN raw_messages r ON r.id = c.rawId " +
            "JOIN transactions t ON t.id = c.txnId " +
            "LEFT JOIN raw_messages o ON o.id = t.rawId " +
            "WHERE r.sentAt >= :from AND r.sentAt < :to ORDER BY r.sentAt DESC"
    )
    fun observeBetween(from: Long, to: Long): Flow<List<DuplicateRow>>
}

@Dao
interface MessageFlagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(flag: MessageFlag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(flags: List<MessageFlag>)

    @Query("SELECT * FROM message_flags")
    suspend fun all(): List<MessageFlag>

    @Query("SELECT * FROM message_flags WHERE rawId = :rawId")
    suspend fun byId(rawId: String): MessageFlag?

    @Query("DELETE FROM message_flags")
    suspend fun deleteAll()
}

@Dao
interface RuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<Rule>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: Rule): Long

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY priority ASC, id ASC")
    suspend fun enabled(): List<Rule>

    @Query("SELECT * FROM rules ORDER BY priority ASC, id ASC")
    fun observeAll(): Flow<List<Rule>>

    @Query("SELECT * FROM rules ORDER BY priority ASC, id ASC")
    suspend fun all(): List<Rule>

    @Query("SELECT COUNT(*) FROM rules")
    suspend fun count(): Int

    @Update
    suspend fun update(rule: Rule)

    @Query("DELETE FROM rules WHERE id = :ruleId")
    suspend fun delete(ruleId: Long)

    @Query("DELETE FROM rules")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun delete(categoryId: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun all(): List<Category>

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface BankDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bank: Bank): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(banks: List<Bank>)

    @Update
    suspend fun update(bank: Bank)

    @Query("SELECT * FROM banks ORDER BY name ASC")
    fun observeAll(): Flow<List<Bank>>

    @Query("SELECT * FROM banks ORDER BY id ASC")
    suspend fun all(): List<Bank>

    @Query("DELETE FROM banks WHERE id = :bankId")
    suspend fun delete(bankId: Long)

    @Query("DELETE FROM banks")
    suspend fun deleteAll()
}

@Dao
interface BankAppDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: BankApp)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(apps: List<BankApp>)

    @Query("DELETE FROM bank_apps WHERE packageName = :packageName AND bankId = :bankId")
    suspend fun delete(packageName: String, bankId: Long)

    @Query("DELETE FROM bank_apps WHERE bankId = :bankId")
    suspend fun deleteForBank(bankId: Long)

    @Query("SELECT * FROM bank_apps")
    fun observeAll(): Flow<List<BankApp>>

    @Query("SELECT * FROM bank_apps")
    suspend fun all(): List<BankApp>

    @Query("DELETE FROM bank_apps")
    suspend fun deleteAll()
}

@Dao
interface SenderLinkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: SenderLink)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<SenderLink>)

    @Query("SELECT * FROM sender_links")
    fun observeAll(): Flow<List<SenderLink>>

    @Query("SELECT * FROM sender_links")
    suspend fun all(): List<SenderLink>

    @Query("DELETE FROM sender_links WHERE senderKey = :senderKey")
    suspend fun unlink(senderKey: String)

    @Query("DELETE FROM sender_links WHERE bankId = :bankId")
    suspend fun unlinkAllFor(bankId: Long)

    @Query("DELETE FROM sender_links")
    suspend fun deleteAll()
}

@Dao
interface LoanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LoanEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LoanEntry>)

    @Delete
    suspend fun delete(entry: LoanEntry)

    @Query("SELECT * FROM loan_entries ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<LoanEntry>>

    @Query("SELECT * FROM loan_entries ORDER BY occurredAt ASC, id ASC")
    suspend fun all(): List<LoanEntry>

    @Query("DELETE FROM loan_entries")
    suspend fun deleteAll()

    /** A split's shares, before rewriting them. Repayments are kept. */
    @Query("DELETE FROM loan_entries WHERE splitId = :splitId AND kind = 'LENT'")
    suspend fun deleteShares(splitId: Long)

    /** Repayments of a deleted split stay as plain loan repayments. */
    @Query("UPDATE loan_entries SET splitId = NULL WHERE splitId = :splitId")
    suspend fun detachFromSplit(splitId: Long)
}

@Dao
interface SplitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(split: Split): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(splits: List<Split>)

    @Delete
    suspend fun delete(split: Split)

    @Query("SELECT * FROM splits ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Split>>

    @Query("SELECT * FROM splits ORDER BY id ASC")
    suspend fun all(): List<Split>

    @Query("SELECT * FROM splits WHERE txnId = :txnId")
    suspend fun byTxnId(txnId: String): Split?

    @Query("DELETE FROM splits")
    suspend fun deleteAll()

    /**
     * Friends' repayments per split bill dated in [from, to), capped at what they owed so
     * an overpayment never takes spending below the user's own share.
     */
    @Query(
        "SELECT t.id AS txnId, t.categoryId AS categoryId, t.occurredAt AS occurredAt, " +
            "MIN(COALESCE(SUM(l.amountMinor), 0), s.totalMinor - s.myShareMinor) AS recoveredMinor " +
            "FROM splits s JOIN transactions t ON t.id = s.txnId " +
            "LEFT JOIN loan_entries l ON l.splitId = s.id AND l.kind = 'RECEIVED_BACK' " +
            "WHERE t.direction = 'DEBIT' AND t.occurredAt >= :from AND t.occurredAt < :to " +
            "GROUP BY s.id"
    )
    fun observeRecoveries(from: Long, to: Long): Flow<List<SplitRecovery>>
}
