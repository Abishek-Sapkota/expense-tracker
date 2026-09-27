package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Txn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeywordSuggesterTest {

    private fun txn(merchant: String? = null, remark: String? = null) = Txn(
        id = "t", rawId = null, amountMinor = 100, direction = Direction.DEBIT, accountTail = null,
        merchant = merchant, remark = remark, refNumber = null, balanceMinor = null, occurredAt = 0
    )

    @Test
    fun `prefers the merchant`() = assertEquals("swiggy", KeywordSuggester.suggest(txn("SWIGGY", "x")))

    @Test
    fun `takes the typed part of a bank remark`() =
        assertEquals("khaja", KeywordSuggester.suggest(txn(remark = "MOS/eSewa/9851180816/khaja")))

    @Test
    fun `skips codes and channel tags`() {
        assertNull(KeywordSuggester.suggest(txn(remark = "MOS/9851180816")))
        assertNull(KeywordSuggester.suggest(txn(remark = "16676793UJ5G,2222170004284518/90801352")))
    }
}
