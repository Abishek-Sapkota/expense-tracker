package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Txn
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemarkPromptPolicyTest {

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
    fun `an uncategorised payment is asked about`() {
        assertTrue(RemarkPromptPolicy.shouldAsk(txn(), askUncategorised = true))
    }

    @Test
    fun `a remark the bank wrote does not stop the question when nothing filed it`() {
        assertTrue(RemarkPromptPolicy.shouldAsk(txn(remark = "16682242fx7C"), askUncategorised = true))
    }

    @Test
    fun `a payment the keywords already filed is not asked about`() {
        assertFalse(RemarkPromptPolicy.shouldAsk(txn(categoryId = 3), askUncategorised = true))
    }

    @Test
    fun `never asks about money coming in`() {
        assertFalse(RemarkPromptPolicy.shouldAsk(txn(direction = Direction.CREDIT), askUncategorised = true))
    }

    @Test
    fun `the switch off means no question at all`() {
        assertFalse(RemarkPromptPolicy.shouldAsk(txn(), askUncategorised = false))
    }
}
