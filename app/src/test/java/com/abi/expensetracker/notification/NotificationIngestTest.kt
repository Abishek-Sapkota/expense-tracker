package com.abi.expensetracker.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationIngestTest {

    @Test
    fun `keeps a debit alert`() {
        assertTrue(
            NotificationIngest.looksFinancial(
                "Nabil Bank Rs. 1,250.00 debited from A/C XXXX1234 for ESEWA LOAD"
            )
        )
    }

    @Test
    fun `keeps a credit alert with the amount after the number`() {
        assertTrue(NotificationIngest.looksFinancial("NPR 5,000 credited to your wallet"))
    }

    @Test
    fun `drops a chat message`() {
        assertFalse(NotificationIngest.looksFinancial("Ram: are you coming tonight?"))
    }

    @Test
    fun `drops a promotion with a price but no transaction`() {
        assertFalse(NotificationIngest.looksFinancial("Flat 20% off, plans from Rs. 499"))
    }

    @Test
    fun `drops an OTP even when it names an amount`() {
        assertFalse(
            NotificationIngest.looksFinancial(
                "OTP for your transaction of Rs. 5000 is 483920"
            )
        )
    }

    @Test
    fun `joins title and text into one line`() {
        assertEquals(
            "Nabil Bank Rs. 500 debited",
            NotificationIngest.composeBody("Nabil Bank", "Rs. 500 debited")
        )
    }

    @Test
    fun `prefers the expanded text over the truncated one`() {
        val body = NotificationIngest.composeBody(
            title = "eSewa",
            text = "Rs. 1,2…",
            bigText = "Rs. 1,250 paid to Daraz. Balance Rs. 4,000"
        )
        assertEquals("eSewa Rs. 1,250 paid to Daraz. Balance Rs. 4,000", body)
    }

    @Test
    fun `does not repeat a title that equals the text`() {
        assertEquals("Rs. 500 debited", NotificationIngest.composeBody("Rs. 500 debited", "Rs. 500 debited"))
    }
}
