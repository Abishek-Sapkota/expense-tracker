package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.LoanKind
import com.abi.expensetracker.data.model.Split
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitsTest {

    @Test
    fun `equal split puts the odd paisa on my share`() {
        val (mine, theirs) = Splits.equalShares(1000_01, friends = 2, includeMe = true)
        assertEquals(listOf(333_33L, 333_33L), theirs)
        assertEquals(333_35L, mine)
        assertEquals(1000_01L, mine + theirs.sum())
    }

    @Test
    fun `without me the first friend takes the odd paisa`() {
        val (mine, theirs) = Splits.equalShares(100_01, friends = 2, includeMe = false)
        assertEquals(0L, mine)
        assertEquals(listOf(50_01L, 50_00L), theirs)
    }

    private val split = Split(id = 7, txnId = "t", title = "Dinner", totalMinor = 2400_00, myShareMinor = 600_00, createdAt = 0)

    private fun entry(person: String, kind: LoanKind, amount: Long, splitId: Long? = 7) =
        LoanEntry(person = person, kind = kind, amountMinor = amount, occurredAt = 0, splitId = splitId)

    @Test
    fun `repayments count against the right friend, ignoring case`() {
        val summary = Splits.summarise(
            listOf(split),
            listOf(
                entry("Ram", LoanKind.LENT, 600_00),
                entry("Sita", LoanKind.LENT, 1200_00),
                entry("ram ", LoanKind.RECEIVED_BACK, 600_00),
                entry("Sita", LoanKind.RECEIVED_BACK, 200_00),
                // A plain loan with Sita is not part of this split.
                entry("Sita", LoanKind.RECEIVED_BACK, 999_00, splitId = null)
            )
        ).single()
        assertEquals(0L, summary.shares.first { it.person == "Ram" }.remainingMinor)
        assertEquals(1000_00L, summary.shares.first { it.person == "Sita" }.remainingMinor)
        assertEquals(1000_00L, summary.pendingMinor)
    }

    @Test
    fun `overpaying settles without going negative`() {
        val summary = Splits.summarise(
            listOf(split),
            listOf(entry("Ram", LoanKind.LENT, 600_00), entry("Ram", LoanKind.RECEIVED_BACK, 700_00))
        ).single()
        assertTrue(summary.isSettled)
        assertEquals(0L, summary.pendingMinor)
    }
}
