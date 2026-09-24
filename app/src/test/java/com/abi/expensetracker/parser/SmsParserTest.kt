package com.abi.expensetracker.parser

import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Real message shapes belong here. When a bank format slips through on the phone, copy
 * the text in (with account numbers changed) and fix the rule against this test — that
 * keeps every format that ever worked working.
 */
class SmsParserTest {

    private val parser = SmsParser(DefaultRules.ALL)

    private fun message(body: String, sender: String = "AX-NABIL", sentAt: Long = 1_700_000_000_000L) =
        RawMessage(
            id = RawMessage.idFor(sender, body, sentAt),
            sender = sender,
            body = body,
            sentAt = sentAt,
            source = Source.SMS,
            importedAt = 0L
        )

    private fun parse(body: String) = parser.parse(message(body))

    @Test
    fun `parses a plain debit`() {
        val out = parse("Rs.450.00 debited from a/c XX1234 on 20-09-26 to SWIGGY. Ref 123456789.")
        assertTrue(out is ParseOutcome.Parsed)
        val txn = (out as ParseOutcome.Parsed).txn
        assertEquals(45_000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("1234", txn.accountTail)
        assertEquals("SWIGGY", txn.merchant)
        assertEquals("123456789", txn.refNumber)
    }

    @Test
    fun `reads a remark the bank prints`() {
        val txn = (parse(
            "Rs.250.00 debited from a/c XX1234 to KHALTI. Remarks: Khaja. Avl Bal Rs.900.00"
        ) as ParseOutcome.Parsed).txn
        assertEquals("KHALTI", txn.merchant)
        assertEquals("Khaja", txn.remark)
    }

    @Test
    fun `reads Sanima's Re abbreviation as the remark`() {
        val txn = (parse(
            "2,060.00 withdrawn from A/C 0#1100000730 on 23/09/2026 21:00.Re:16676793UJ5G,2222170004284518/90801352. ."
        ) as ParseOutcome.Parsed).txn
        assertEquals("16676793UJ5G,2222170004284518/90801352", txn.remark)
    }

    @Test
    fun `remark is null when the message carries none`() {
        val txn = (parse(
            "Rs.450.00 debited from a/c XX1234 on 20-09-26 to SWIGGY. Ref 123456789."
        ) as ParseOutcome.Parsed).txn
        assertNull(txn.remark)
    }

    @Test
    fun `parses a credit`() {
        val out = parse("Your a/c XX9876 is credited with NPR 5,000.00 on 01-09-26. Avl Bal Rs.12,340.55")
        val txn = (out as ParseOutcome.Parsed).txn
        assertEquals(500_000L, txn.amountMinor)
        assertEquals(Direction.CREDIT, txn.direction)
        assertEquals("9876", txn.accountTail)
        assertEquals(1_234_055L, txn.balanceMinor)
    }

    @Test
    fun `handles lakh-style grouping`() {
        val txn = (parse("NPR 1,23,456.78 debited from a/c XX1111") as ParseOutcome.Parsed).txn
        assertEquals(12_345_678L, txn.amountMinor)
    }

    @Test
    fun `verb-first wording still parses`() {
        val txn = (parse("You have spent Rs 250 at BIGBASKET using card xx4321") as ParseOutcome.Parsed).txn
        assertEquals(25_000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
        assertEquals("4321", txn.accountTail)
    }

    @Test
    fun `non-transaction message does not become a transaction`() {
        assertEquals(ParseOutcome.NoMatch, parse("Your OTP is 445566. Do not share it with anyone."))
    }

    @Test
    fun `promotional message mentioning rupees is not a transaction`() {
        assertEquals(
            ParseOutcome.NoMatch,
            parse("Get a personal loan of Rs.5,00,000 at low interest. Call now!")
        )
    }

    @Test
    fun `same reference number yields the same id so duplicates collapse`() {
        val fromSms = parse("Rs.450.00 debited from a/c XX1234 to SWIGGY. Ref 123456789.")
        val fromApp = parse("NPR 450.00 spent at SWIGGY. Reference No 123456789")
        val a = (fromSms as ParseOutcome.Parsed).txn
        val b = (fromApp as ParseOutcome.Parsed).txn
        assertEquals(a.id, b.id)
    }

    @Test
    fun `transaction time comes from the message timestamp`() {
        val sentAt = 1_726_800_000_000L
        val out = parser.parse(message("Rs.99.00 debited from a/c XX1234 to ZOMATO", sentAt = sentAt))
        assertEquals(sentAt, (out as ParseOutcome.Parsed).txn.occurredAt)
    }

    @Test
    fun `unparseable merchant is flagged for review rather than guessed`() {
        val txn = (parse("Rs.75.00 debited from a/c XX1234") as ParseOutcome.Parsed).txn
        assertNull(txn.merchant)
        assertTrue(txn.needsReview)
    }

    @Test
    fun `broken user rule does not stop other rules from working`() {
        val broken = DefaultRules.ALL.first().copy(id = 1, bodyPattern = "(unclosed[", priority = 1)
        val tolerant = SmsParser(listOf(broken) + DefaultRules.ALL)
        val out = tolerant.parse(message("Rs.450.00 debited from a/c XX1234 to SWIGGY"))
        assertTrue(out is ParseOutcome.Parsed)
    }

    @Test
    fun `parses an amount written with no currency`() {
        // Sanima, 23 Sep 2026. Account number changed.
        val out = parse(
            "155.00 deposited in your A/C 0#1100000730 on 23/09/2026 16:02." +
                "Re:coffee,007011100000730/903011210000019. ."
        )
        assertTrue(out is ParseOutcome.Parsed)
        val txn = (out as ParseOutcome.Parsed).txn
        assertEquals(15_500L, txn.amountMinor)
        assertEquals(Direction.CREDIT, txn.direction)
    }

    @Test
    fun `parses a bare amount withdrawal`() {
        val out = parse("2,500.00 withdrawn from A/C 0#1100000730 on 23/09/2026 16:02.")
        assertTrue(out is ParseOutcome.Parsed)
        val txn = (out as ParseOutcome.Parsed).txn
        assertEquals(250_000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
    }

    @Test
    fun `parses an esewa fonepay payment`() {
        val out = parse(
            "Fonepay Payment Dear Muna, Your transaction of Rs. 20.0 for Fonepay Payment " +
                "has been successfully completed. Thank you. eSewa.",
            )
        assertTrue(out is ParseOutcome.Parsed)
        val txn = (out as ParseOutcome.Parsed).txn
        assertEquals(2_000L, txn.amountMinor)
        assertEquals(Direction.DEBIT, txn.direction)
    }

    @Test
    fun `an account number alone is not an amount`() {
        val out = parse("Your A/C 0#1100000730 statement for 23/09/2026 is ready.")
        assertFalse(out is ParseOutcome.Parsed)
    }
}

class MoneyTest {

    @Test
    fun `parses the formats banks actually use`() {
        assertEquals(45_000L, Money.parseToMinor("450.00"))
        assertEquals(45_000L, Money.parseToMinor("450"))
        assertEquals(123_456_78L, Money.parseToMinor("1,23,456.78"))
        assertEquals(1_250L, Money.parseToMinor("12.5"))
    }

    @Test
    fun `rejects text that is not a number instead of returning zero`() {
        assertNull(Money.parseToMinor("abc"))
        assertNull(Money.parseToMinor(""))
    }

    @Test
    fun `formats back to two decimal places`() {
        assertNotNull(Money.format(45_000L))
        assertEquals("रु450.00", Money.format(45_000L))
    }
}
