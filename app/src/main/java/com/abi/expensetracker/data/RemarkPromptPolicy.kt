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
     * Asks about spending only (nobody annotates their salary), and only when no
     * category's keywords matched: the answer is what will file it, so "coffee" lands in
     * the coffee category. One switch for every account; a payment the keywords already
     * filed has nothing to ask about.
     */
    fun shouldAsk(txn: Txn, askUncategorised: Boolean): Boolean =
        askUncategorised && txn.direction == Direction.DEBIT && txn.categoryId == null
}
