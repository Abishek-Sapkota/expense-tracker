package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Txn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferDetectorTest {

    private val own = listOf("9866550884")

    private fun debit(rawId: String? = "r") = Txn(
        id = "t", rawId = rawId, amountMinor = 100, direction = Direction.DEBIT, accountTail = null,
        merchant = null, remark = null, refNumber = null, balanceMinor = null, occurredAt = 0
    )

    @Test
    fun `a load to your own wallet is a transfer`() {
        assertTrue(TransferDetector.isTransfer(debit(), "Your  Esewa Wallet Load for 9866550884 of 3200.00 is successful", own))
        assertTrue(TransferDetector.isTransfer(debit(), "Payment successful to eSewa with amount 500.00 and remarks MOS/eSewa/9866550884/self", own))
    }

    @Test
    fun `a load to someone else's wallet is spending`() {
        assertFalse(TransferDetector.isTransfer(debit(), "Your  Esewa Wallet Load for 9823083036 of 300.00 is successful", own))
    }

    @Test
    fun `the ID must stand alone, not inside a longer number`() {
        assertFalse(TransferDetector.isTransfer(debit(), "Ref 198665508841 withdrawn", own))
    }

    @Test
    fun `credits and manual entries are never transfers`() {
        assertFalse(TransferDetector.isTransfer(debit().copy(direction = Direction.CREDIT), "9866550884", own))
        assertFalse(TransferDetector.isTransfer(debit(rawId = null), "9866550884", own))
    }

    @Test
    fun `parses what the user typed`() {
        assertEquals(listOf("9866550884", "9812345678"), TransferDetector.parseIds(" 986-655-0884, 9812345678,12 "))
    }
}
