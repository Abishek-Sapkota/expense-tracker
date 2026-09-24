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

    fun shouldAsk(txn: Txn, senderKey: String, optedInSenders: Set<String>): Boolean {
        // Not opted in: the user has said this sender's messages are clear enough.
        if (senderKey !in optedInSenders) return false
        // Nobody annotates their salary. The question is about spending.
        if (txn.direction != Direction.DEBIT) return false
        // The message already said what it was for.
        if (!txn.remark.isNullOrBlank()) return false
        return true
    }
}
