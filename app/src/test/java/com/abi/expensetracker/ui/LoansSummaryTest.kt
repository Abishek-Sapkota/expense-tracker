package com.abi.expensetracker.ui

import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.LoanKind
import org.junit.Assert.assertEquals
import org.junit.Test

class LoansSummaryTest {

    private fun entry(person: String, kind: LoanKind, amount: Long, at: Long = 0L) =
        LoanEntry(person = person, kind = kind, amountMinor = amount, occurredAt = at)

    @Test
    fun `lending then partial repayment leaves the rest owed`() {
        val state = LoansViewModel.summarise(
            listOf(entry("Ram", LoanKind.LENT, 5_000_00), entry("Ram", LoanKind.RECEIVED_BACK, 2_000_00))
        )
        assertEquals(3_000_00, state.people.single().balanceMinor)
        assertEquals(3_000_00, state.owedToMeMinor)
        assertEquals(0L, state.iOweMinor)
    }

    @Test
    fun `borrowing is owed the other way`() {
        val state = LoansViewModel.summarise(
            listOf(entry("Sita", LoanKind.BORROWED, 1_000_00), entry("Sita", LoanKind.PAID_BACK, 400_00))
        )
        assertEquals(-600_00, state.people.single().balanceMinor)
        assertEquals(600_00, state.iOweMinor)
    }

    @Test
    fun `names group ignoring case and spaces, newest spelling shown`() {
        val state = LoansViewModel.summarise(
            listOf(entry("ram ", LoanKind.LENT, 100, at = 1), entry("Ram", LoanKind.LENT, 100, at = 2))
        )
        assertEquals("Ram", state.people.single().name)
        assertEquals(200L, state.people.single().balanceMinor)
    }

    @Test
    fun `open balances list before settled ones`() {
        val state = LoansViewModel.summarise(
            listOf(
                entry("Done", LoanKind.LENT, 100, at = 9),
                entry("Done", LoanKind.RECEIVED_BACK, 100, at = 9),
                entry("Open", LoanKind.LENT, 50, at = 1)
            )
        )
        assertEquals(listOf("Open", "Done"), state.people.map { it.name })
    }
}
