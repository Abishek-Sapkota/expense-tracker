package com.abi.expensetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A message that reported a transaction already in the ledger, from another channel.
 *
 * Sanima texts and emails the same withdrawal minutes apart; the second report is kept as
 * a raw message but must not book the money twice. This row records which transaction it
 * was folded into, so the editor can show every message behind a row and a reparse does
 * not turn the copy back into a transaction of its own.
 *
 * No foreign keys: transactions are written with REPLACE, which deletes and reinserts, and
 * a cascade would silently drop the copies on every re-ingest. Orphans are cleared
 * explicitly instead.
 */
@Entity(tableName = "txn_copies", indices = [Index("txnId")])
data class TxnCopy(
    @PrimaryKey val rawId: String,
    val txnId: String
)
