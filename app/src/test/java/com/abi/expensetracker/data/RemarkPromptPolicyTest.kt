package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Txn
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemarkPromptPolicyTest {

    private val optedIn = setOf("ESEWA", "NABIL")

    private fun txn(
        direction: Direction = Direction.DEBIT,
        remark: String? = null,
        categoryId: Long? = null
    ) = Txn(
        id = "t", rawId = "r", amountMinor = 25_000, direction = direction,
        accountTail = null, merchant = "ESEWA", remark = remark, refNumber = null,
        balanceMinor = null, occurredAt = 0L, categoryId = categoryId
    )

    @Test
    fun `asks about a payment from an opted-in sender with no remark`() {
        assertTrue(RemarkPromptPolicy.shouldAsk(txn(), "ESEWA", optedIn))
    }

    @Test
    fun `stays quiet for a sender that was not opted in`() {
        assertFalse(RemarkPromptPolicy.shouldAsk(txn(), "KHALTI", optedIn))
    }

    @Test
    fun `stays quiet when nothing is opted in`() {
        assertFalse(RemarkPromptPolicy.shouldAsk(txn(), "ESEWA", emptySet()))
    }

    @Test
    fun `never asks about money coming in`() {
        assertFalse(
            RemarkPromptPolicy.shouldAsk(txn(direction = Direction.CREDIT), "ESEWA", optedIn)
        )
    }

    @Test
    fun `stays quiet when the message already said what it was for`() {
        assertFalse(RemarkPromptPolicy.shouldAsk(txn(remark = "Khaja"), "ESEWA", optedIn))
    }

    @Test
    fun `a blank remark counts as no remark`() {
        assertTrue(RemarkPromptPolicy.shouldAsk(txn(remark = "   "), "ESEWA", optedIn))
    }

    @Test
    fun `the sender key is matched exactly, not by prefix`() {
        // "ESEWANEPAL" is a different sender; opting into ESEWA must not opt into it.
        assertFalse(RemarkPromptPolicy.shouldAsk(txn(), "ESEWANEPAL", optedIn))
    }

    @Test
    fun `an uncategorised payment is asked about from any sender`() {
        assertTrue(RemarkPromptPolicy.shouldAsk(txn(), "SANIMA", emptySet(), askUncategorised = true))
    }

    @Test
    fun `an uncategorised payment is asked about even when the message had a remark`() {
        // A bank's remark like "16682242fx7C,2222…" says nothing a category can match.
        assertTrue(RemarkPromptPolicy.shouldAsk(txn(remark = "16682242fx7C"), "SANIMA", emptySet(), askUncategorised = true))
    }

    @Test
    fun `a categorised payment is not asked about for being uncategorised`() {
        assertFalse(RemarkPromptPolicy.shouldAsk(txn(categoryId = 3), "SANIMA", emptySet(), askUncategorised = true))
    }

    @Test
    fun `money in is never asked about`() {
        assertFalse(RemarkPromptPolicy.shouldAsk(txn(direction = Direction.CREDIT), "SANIMA", emptySet(), askUncategorised = true))
    }

    @Test
    fun `with the setting off only opted-in senders are asked`() {
        assertFalse(RemarkPromptPolicy.shouldAsk(txn(), "SANIMA", emptySet(), askUncategorised = false))
    }
}
