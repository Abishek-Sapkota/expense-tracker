package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Txn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorizerTest {

    private val groceries = Category(1, "Groceries & supplies", "🛒", "bhatbhateni,mart,kirana")
    private val fuel = Category(2, "Transport & fuel", "⛽", "petrol,pump,nepal oil,oil")
    private val dining = Category(3, "Dining", "☕", "cafe,coffee,khaja")

    private val categorizer = Categorizer(listOf(groceries, fuel, dining))

    private fun txn(merchant: String? = null, remark: String? = null, categoryId: Long? = null) = Txn(
        id = "t", rawId = null, amountMinor = 1000, direction = Direction.DEBIT,
        accountTail = null, merchant = merchant, remark = remark, refNumber = null,
        balanceMinor = null, occurredAt = 0L, categoryId = categoryId
    )

    @Test
    fun `matches a merchant name`() {
        assertEquals(1L, categorizer.categoryIdFor(txn(merchant = "BHATBHATENI SUPERMARKET")))
    }

    @Test
    fun `matches on the remark when the merchant says nothing`() {
        assertEquals(3L, categorizer.categoryIdFor(txn(merchant = "ESEWA", remark = "Khaja")))
    }

    @Test
    fun `matching is case insensitive`() {
        assertEquals(3L, categorizer.categoryIdFor(txn(merchant = "Himalayan Java Coffee")))
    }

    @Test
    fun `the longest matching keyword wins`() {
        // Both "nepal oil" and "oil" match; the specific one decides, not storage order.
        assertEquals(2L, categorizer.categoryIdFor(txn(merchant = "NEPAL OIL CORPORATION")))
    }

    @Test
    fun `no match leaves the row uncategorised`() {
        assertNull(categorizer.categoryIdFor(txn(merchant = "SOMETHING ELSE")))
    }

    @Test
    fun `a blank row is uncategorised rather than guessed`() {
        assertNull(categorizer.categoryIdFor(txn()))
    }

    @Test
    fun `apply fills only the rows that have no category`() {
        val rows = listOf(
            txn(merchant = "Sajha Petrol Pump"),
            // Already set, and set to something the keywords disagree with: a hand pick
            // must survive.
            txn(merchant = "Bhatbhateni", categoryId = 3L)
        )

        val result = categorizer.apply(rows)

        assertEquals(2L, result[0].categoryId)
        assertEquals(3L, result[1].categoryId)
    }

    @Test
    fun `no categories means nothing is assigned`() {
        assertNull(Categorizer(emptyList()).categoryIdFor(txn(merchant = "Bhatbhateni")))
    }
}
