package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Txn
import kotlin.math.abs

/**
 * Decides whether a newly parsed transaction is another channel's report of one already
 * in the ledger.
 *
 * Banks such as Sanima send an SMS and an email for the same movement, and the email can
 * trail by twenty minutes. The two share no reference number, so they are matched on what
 * they do share: amount, direction and roughly when. The remaining rules exist to keep two
 * genuine payments of the same amount apart:
 *
 * - A copy must come from a different sender. Two messages from the same channel are two
 *   transactions; a bank does not text the same withdrawal twice.
 * - A transaction absorbs at most one copy per sender, so two real payments of one amount
 *   in the same hour, each texted and emailed, still make two rows.
 * - Where both carry a remark, the remarks must lead with the same token. Sanima's SMS
 *   truncates the remark the email prints in full, so only the head is compared.
 *
 * Pure, so the rules are unit-tested without a database.
 */
object DuplicateMatcher {

    /** Emails have been seen up to 21 minutes behind the SMS; an hour leaves headroom. */
    const val WINDOW_MILLIS = 60 * 60 * 1000L

    data class Candidate(
        val txn: Txn,
        /** Normalised sender of the message the candidate was parsed from. */
        val senderKey: String,
        /** Normalised senders of the copies already folded into it. */
        val copySenderKeys: Set<String>
    )

    /** The existing transaction [txn] duplicates, nearest in time, or null if none. */
    fun findOriginal(txn: Txn, senderKey: String, candidates: List<Candidate>): Txn? =
        candidates
            .filter { c ->
                c.txn.id != txn.id &&
                    !c.txn.isManual &&
                    c.txn.amountMinor == txn.amountMinor &&
                    c.txn.direction == txn.direction &&
                    abs(c.txn.occurredAt - txn.occurredAt) <= WINDOW_MILLIS &&
                    c.senderKey != senderKey &&
                    senderKey !in c.copySenderKeys &&
                    // A hand-edited remark is the user's words, not the bank's, so it
                    // says nothing about whether the messages match.
                    (c.txn.userEdited || remarksCompatible(c.txn.remark, txn.remark))
            }
            .minByOrNull { abs(it.txn.occurredAt - txn.occurredAt) }
            ?.txn

    fun remarksCompatible(a: String?, b: String?): Boolean {
        val x = leadToken(a) ?: return true
        val y = leadToken(b) ?: return true
        return x.equals(y, ignoreCase = true)
    }

    private val SEPARATORS = Regex("""[,/:;\s]+""")

    private fun leadToken(remark: String?): String? =
        remark?.split(SEPARATORS)?.firstOrNull { it.isNotBlank() }
}
