package com.abi.expensetracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abi.expensetracker.data.StableId
import java.util.UUID

/**
 * One transaction. Parsed ones are derived from a [RawMessage] and safe to wipe and
 * rebuild; manually entered ones are not.
 *
 * [rawId] is null exactly when the user typed the entry in by hand. That is the only
 * distinction between the two, and it is what makes manual entries survive a reparse:
 * nothing can regenerate them.
 *
 * Amounts are minor units (paise) as Long. Never Double: 0.1 + 0.2 in binary floating
 * point is not 0.3, and a ledger that cannot add up is worse than no ledger.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = RawMessage::class,
            parentColumns = ["id"],
            childColumns = ["rawId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("rawId"), Index("occurredAt"), Index("categoryId")]
)
data class Txn(
    @PrimaryKey val id: String,
    val rawId: String?,
    val amountMinor: Long,
    val direction: Direction,
    val accountTail: String?,
    val merchant: String?,
    /**
     * What the money went on, as the message or the user states it.
     *
     * Separate from [merchant]: the merchant is who was paid ("KHALTI", "NIC ASIA"), the
     * remark is what for ("Khaja", "Rent"). Banks print both, and collapsing them would
     * lose whichever the ledger happens to show second.
     */
    val remark: String?,
    val refNumber: String?,
    val balanceMinor: Long?,
    val occurredAt: Long,
    val categoryId: Long? = null,
    /** True when no rule matched, or a rule matched weakly. Surfaces in the review queue. */
    val needsReview: Boolean = false,
    val ruleId: Long? = null,
    /**
     * True once the user has corrected this row by hand.
     *
     * A parsed transaction is otherwise disposable — reparse deletes and rebuilds it from
     * the message. An edit is the one thing no rule can regenerate, so an edited row is
     * kept through a reparse and never overwritten by the parser again.
     */
    val userEdited: Boolean = false
) {
    val isManual: Boolean get() = rawId == null

    companion object {
        /**
         * Prefer the bank's own reference number: the same transaction often arrives
         * twice (SMS plus app notification) with different wording but one ref.
         */
        fun idFor(refNumber: String?, amountMinor: Long, rawId: String): String =
            if (!refNumber.isNullOrBlank()) StableId.sha256("ref|$refNumber|$amountMinor")
            else StableId.sha256("raw|$rawId")

        /**
         * Random, not content-derived: two genuine cash expenses of the same amount on
         * the same day are two expenses, and a content hash would silently merge them.
         */
        fun manualId(): String = StableId.sha256("manual|${UUID.randomUUID()}")
    }
}
