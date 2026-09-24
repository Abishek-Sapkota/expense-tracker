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
        remark: String? = null
    ) = Txn(
        id = "t", rawId = "r", amountMinor = 25_000, direction = direction,
        accountTail = null, merchant = "ESEWA", remark = remark, refNumber = null,
        balanceMinor = null, occurredAt = 0L
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
}
