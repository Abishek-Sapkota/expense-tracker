package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Txn

/**
 * Whether a freshly parsed transaction is worth asking the user about.
 *
 * Pure, and separate from the repository, because these are the rules that decide whether
 * the app interrupts someone — the one place where being wrong is not a display bug but an
 * annoyance, and so the one place most worth testing directly.
 */
object RemarkPromptPolicy {

    /**
     * Two reasons to ask, for spending only (nobody annotates their salary):
     * - the sender is opted in and the message did not say what the money was for, or
     * - [askUncategorised] is on and no category's keywords matched, so the answer is what
     *   will file it: "coffee" lands in the coffee category.
     */
    fun shouldAsk(
        txn: Txn,
        senderKey: String,
        optedInSenders: Set<String>,
        askUncategorised: Boolean = false
    ): Boolean {
        if (txn.direction != Direction.DEBIT) return false
        val optedIn = senderKey in optedInSenders && txn.remark.isNullOrBlank()
        val uncategorised = askUncategorised && txn.categoryId == null
        return optedIn || uncategorised
    }
}
