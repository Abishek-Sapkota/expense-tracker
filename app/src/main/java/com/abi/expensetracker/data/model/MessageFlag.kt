package com.abi.expensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A decision the user made about one stored message, which parsing must respect.
 *
 * Transactions are rebuilt from messages on every reparse, so a choice made on a
 * transaction row would be lost with it. Keyed on the message instead, it survives:
 *
 * - [deleted]: the user removed the transaction; the message must not produce it again.
 * - [notDuplicate]: the user said this message is its own transaction, not a copy of
 *   another channel's report, so the duplicate matcher skips it.
 */
@Entity(tableName = "message_flags")
data class MessageFlag(
    @PrimaryKey val rawId: String,
    val notDuplicate: Boolean = false,
    val deleted: Boolean = false
)
