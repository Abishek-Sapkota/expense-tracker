package com.abi.expensetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Which way money moved between the user and a person, as a loan rather than spending.
 *
 * [sign] is the effect on what the person owes the user: lending raises it, being paid
 * back lowers it, and borrowing takes it below zero, where the user owes them.
 */
enum class LoanKind(val label: String, val sign: Int, val isOutflow: Boolean) {
    LENT("Lent", +1, isOutflow = true),
    RECEIVED_BACK("Got back", -1, isOutflow = false),
    BORROWED("Borrowed", -1, isOutflow = false),
    PAID_BACK("Paid back", +1, isOutflow = true);

    companion object {
        /** The kinds that fit a transaction's direction: money out can only lend or repay. */
        fun forDirection(direction: Direction): List<LoanKind> =
            entries.filter { it.isOutflow == (direction == Direction.DEBIT) }
    }
}

/**
 * One movement of a loan with a person: money lent, borrowed, or paid back.
 *
 * [person] is free text, typed or taken from a contact, and is what entries are grouped
 * by; there is no people table to keep in step.
 *
 * [txnId] links the entry to the bank transaction that carried the money, when there was
 * one. A linked transaction is left in the ledger but no longer counts as spent or
 * received: lending a friend रु5,000 is not spending it. The id survives a reparse because
 * transaction ids are derived from their message. Null for cash.
 */
@Entity(
    tableName = "loan_entries",
    indices = [Index("person"), Index(value = ["txnId"], unique = true), Index("splitId")]
)
data class LoanEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val kind: LoanKind,
    val amountMinor: Long,
    val occurredAt: Long,
    val note: String? = null,
    val txnId: String? = null,
    /** Set on a friend's share of a [Split] and on their repayments of it. */
    val splitId: Long? = null
) {
    /** This entry's effect on what [person] owes the user. */
    val signedMinor: Long get() = kind.sign * amountMinor
}
