package com.abi.expensetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A bill the user paid and shared with friends.
 *
 * The bill's transaction stays in the ledger as spending. Each friend's share is a
 * [LoanEntry] of kind [LoanKind.LENT] carrying this split's id, so it shows in Loans as
 * money owed; repayments are [LoanKind.RECEIVED_BACK] entries with the same id, and as they
 * arrive the bill's spending shrinks towards [myShareMinor], on the bill's own date and in
 * its category.
 */
@Entity(tableName = "splits", indices = [Index(value = ["txnId"], unique = true)])
data class Split(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val txnId: String,
    val title: String,
    val totalMinor: Long,
    /** Zero when the user paid for others without having a share themselves. */
    val myShareMinor: Long,
    val createdAt: Long
)
