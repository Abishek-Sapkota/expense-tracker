package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.LoanKind
import com.abi.expensetracker.data.model.Split

/** One friend's part of a split: what they owe for it and what they have paid. */
data class ShareStatus(val person: String, val shareMinor: Long, val paidMinor: Long) {
    val remainingMinor: Long get() = (shareMinor - paidMinor).coerceAtLeast(0L)
}

data class SplitSummary(val split: Split, val shares: List<ShareStatus>) {
    val pendingMinor: Long get() = shares.sumOf { it.remainingMinor }
    val isSettled: Boolean get() = pendingMinor == 0L
}

/** Pure split arithmetic, kept off Android so it is unit-tested on the desktop. */
object Splits {

    /**
     * Divides [totalMinor] evenly, to the paisa.
     *
     * Whatever does not divide goes to the user's own share when they have one, otherwise
     * to the first friend, so the shares always add back up to the bill exactly.
     *
     * Returns the user's share and one share per friend.
     */
    fun equalShares(totalMinor: Long, friends: Int, includeMe: Boolean): Pair<Long, List<Long>> {
        val parts = friends + if (includeMe) 1 else 0
        if (parts == 0) return totalMinor to emptyList()
        val base = totalMinor / parts
        val leftover = totalMinor - base * parts
        return if (includeMe) {
            (base + leftover) to List(friends) { base }
        } else {
            0L to List(friends) { if (it == 0) base + leftover else base }
        }
    }

    fun personKey(name: String): String = name.trim().lowercase()

    /** Each split with every friend's share and repayments, from the loan entries. */
    fun summarise(splits: List<Split>, loans: List<LoanEntry>): List<SplitSummary> {
        val bySplit = loans.filter { it.splitId != null }.groupBy { it.splitId }
        return splits.map { split ->
            val entries = bySplit[split.id].orEmpty()
            val paid = entries.filter { it.kind == LoanKind.RECEIVED_BACK }
                .groupBy { personKey(it.person) }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
            val shares = entries.filter { it.kind == LoanKind.LENT }.map { share ->
                ShareStatus(share.person.trim(), share.amountMinor, paid[personKey(share.person)] ?: 0L)
            }
            SplitSummary(split, shares)
        }
    }
}
