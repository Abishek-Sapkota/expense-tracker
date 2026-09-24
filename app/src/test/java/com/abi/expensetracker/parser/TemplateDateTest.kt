package com.abi.expensetracker.parser

import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Rule
import com.abi.expensetracker.data.model.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * `{date}` end to end: the template captures it, the parser uses it in place of the
 * message timestamp, and a nonsense date falls back rather than corrupting the ledger.
 */
class TemplateDateTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kathmandu")

    private fun ruleFor(template: String): Rule {
        val compiled = TemplateCompiler.compile(template) as TemplateCompiler.Outcome.Ok
        return Rule(
            id = 1,
            name = "test",
            senderPattern = ".*",
            bodyPattern = compiled.regex,
            direction = Direction.DEBIT,
            template = template,
            priority = 10,
            builtIn = false
        )
    }

    private fun messageAt(body: String, sentAt: LocalDateTime): RawMessage {
        val millis = sentAt.atZone(zone).toInstant().toEpochMilli()
        return RawMessage(
            id = RawMessage.idFor("AX-NABIL", body, millis),
            sender = "AX-NABIL",
            body = body,
            sentAt = millis,
            source = Source.SMS,
            importedAt = 0L
        )
    }

    private fun parseDate(template: String, body: String, sentAt: LocalDateTime): LocalDateTime {
        val parser = SmsParser(listOf(ruleFor(template)), zone)
        val outcome = parser.parse(messageAt(body, sentAt)) as ParseOutcome.Parsed
        return Instant.ofEpochMilli(outcome.txn.occurredAt).atZone(zone).toLocalDateTime()
    }

    @Test
    fun `the written date is used instead of when the message arrived`() {
        // The message lands on the 22nd, but states the transaction was on the 20th.
        val result = parseDate(
            "Rs.{amount} debited on {date} to {merchant}",
            "Rs.450.00 debited on 20-09-2026 to SWIGGY",
            LocalDateTime.of(2026, 9, 22, 14, 30)
        )
        assertEquals(LocalDate.of(2026, 9, 20), result.toLocalDate())
    }

    @Test
    fun `the time of day still comes from the message`() {
        // Bank SMS rarely carries a time, and keeping the arrival time is what preserves
        // the order of several transactions on the same day.
        val result = parseDate(
            "Rs.{amount} debited on {date} to {merchant}",
            "Rs.450.00 debited on 20-09-2026 to SWIGGY",
            LocalDateTime.of(2026, 9, 22, 14, 30)
        )
        assertEquals(14, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun `a template without a date falls back to the message timestamp`() {
        val sentAt = LocalDateTime.of(2026, 9, 22, 14, 30)
        val result = parseDate("Rs.{amount} debited to {merchant}", "Rs.450.00 debited to SWIGGY", sentAt)
        assertEquals(sentAt, result)
    }

    @Test
    fun `a date the parser cannot read falls back to the message timestamp`() {
        val sentAt = LocalDateTime.of(2026, 9, 22, 14, 30)
        // 32-01-2026 matches the date pattern but is not a real date.
        val result = parseDate(
            "Rs.{amount} debited on {date} to {merchant}",
            "Rs.450.00 debited on 32-01-2026 to SWIGGY",
            sentAt
        )
        assertEquals(sentAt, result)
    }

    @Test
    fun `a date years from the message is rejected as a misread`() {
        // A card expiry or mangled reference caught by the pattern must not file the
        // transaction a decade away, where it would vanish from every period view.
        val sentAt = LocalDateTime.of(2026, 9, 22, 14, 30)
        val result = parseDate(
            "Rs.{amount} debited on {date} to {merchant}",
            "Rs.450.00 debited on 20-09-2049 to SWIGGY",
            sentAt
        )
        assertEquals(sentAt, result)
    }

    @Test
    fun `a date a few months back is accepted`() {
        val result = parseDate(
            "Rs.{amount} debited on {date} to {merchant}",
            "Rs.450.00 debited on 15-06-2026 to SWIGGY",
            LocalDateTime.of(2026, 9, 22, 14, 30)
        )
        assertEquals(LocalDate.of(2026, 6, 15), result.toLocalDate())
    }

    @Test
    fun `named-month dates work in a template`() {
        val result = parseDate(
            "Rs.{amount} spent on {date} at {merchant}",
            "Rs.99.00 spent on 20-Sep-2026 at ZOMATO",
            LocalDateTime.of(2026, 9, 21, 9, 0)
        )
        assertEquals(LocalDate.of(2026, 9, 20), result.toLocalDate())
    }

    @Test
    fun `date sits correctly between other tokens`() {
        val template = "Rs.{amount} debited from a/c XX{acct} on {date} to {merchant}. Ref {ref}"
        val body = "Rs.450.00 debited from a/c XX1234 on 20-09-2026 to SWIGGY. Ref 123456789"
        val parser = SmsParser(listOf(ruleFor(template)), zone)
        val message = messageAt(body, LocalDateTime.of(2026, 9, 21, 10, 0))
        val txn = (parser.parse(message) as ParseOutcome.Parsed).txn

        assertEquals(45_000L, txn.amountMinor)
        assertEquals("1234", txn.accountTail)
        assertEquals("SWIGGY", txn.merchant)
        assertEquals("123456789", txn.refNumber)
        assertEquals(
            LocalDate.of(2026, 9, 20),
            Instant.ofEpochMilli(txn.occurredAt).atZone(zone).toLocalDate()
        )
    }

    @Test
    fun `date is offered as a placeholder in the editor help`() {
        assertTrue(TemplateCompiler.PLACEHOLDERS.containsKey("date"))
    }

    @Test
    fun `a template may not use date twice`() {
        val outcome = TemplateCompiler.compile("Rs.{amount} on {date} and {date}")
        assertTrue(outcome is TemplateCompiler.Outcome.Invalid)
    }
}
