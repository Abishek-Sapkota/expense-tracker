package com.abi.expensetracker.data.db

import androidx.room.Embedded
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Source
import com.abi.expensetracker.data.model.Txn

/**
 * A transaction together with the sender of the SMS it came from.
 *
 * The bank is resolved from [sender] when displayed rather than stored on the
 * transaction, so linking a bank in settings labels existing history immediately
 * instead of requiring a reparse.
 *
 * [sender] is null for manually entered transactions.
 */
data class TxnWithSender(
    @Embedded val txn: Txn,
    val sender: String?,
    /** The message text, which picks the bank when an app such as Gmail serves several. */
    val body: String? = null
)

/**
 * A message folded into an existing transaction as another channel's copy, together with
 * that transaction and the message it was parsed from, for the duplicates screen.
 */
data class DuplicateRow(
    val rawId: String,
    val sender: String,
    val body: String,
    val source: Source,
    val sentAt: Long,
    val txnId: String,
    val amountMinor: Long,
    val direction: Direction,
    val originalSender: String?,
    val originalBody: String?,
    val originalSource: Source?,
    val originalAt: Long
)

/**
 * How much of one split bill friends have paid back, capped at their shares: the amount
 * taken off the bill's spending, on its date and in its category.
 */
data class SplitRecovery(
    val txnId: String,
    val categoryId: Long?,
    val occurredAt: Long,
    val recoveredMinor: Long
)

/** The sender of a message folded into [txnId] as a copy. */
data class CopySender(
    val txnId: String,
    val sender: String
)

/** A distinct sender seen in stored messages, with how many messages came from it. */
data class SenderCount(
    val sender: String,
    val messageCount: Int
)

/** Spend in one category over a window. [categoryId] is null for uncategorised rows. */
data class CategoryTotal(
    val categoryId: Long?,
    val totalMinor: Long
)
