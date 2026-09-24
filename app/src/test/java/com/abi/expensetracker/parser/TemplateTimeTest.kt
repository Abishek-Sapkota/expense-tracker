package com.abi.expensetracker.parser

import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Rule
import com.abi.expensetracker.data.model.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** `{time}` end to end, and the time reader on its own. */
class TemplateTimeTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kathmandu")

    private fun parseAt(template: String, body: String, sentAt: LocalDateTime): LocalDateTime {
        val compiled = TemplateCompiler.compile(template) as TemplateCompiler.Outcome.Ok
        val rule = Rule(
            id = 1, name = "t", senderPattern = ".*", bodyPattern = compiled.regex,
            direction = Direction.DEBIT, template = template, priority = 10, builtIn = false
        )
        val millis = sentAt.atZone(zone).toInstant().toEpochMilli()
        val message = RawMessage(RawMessage.idFor("S", body, millis), "S", body, millis, Source.SMS, 0L)
        val txn = (SmsParser(listOf(rule), zone).parse(message) as ParseOutcome.Parsed).txn
        return Instant.ofEpochMilli(txn.occurredAt).atZone(zone).toLocalDateTime()
    }

    @Test
    fun `reads 24-hour and 12-hour times`() {
        assertEquals(LocalTime.of(21, 0), TimeParser.parse("21:00"))
        assertEquals(LocalTime.of(8, 41, 37), TimeParser.parse("8:41:37 AM"))
        assertEquals(LocalTime.of(13, 20), TimeParser.parse("1:20PM"))
        assertEquals(LocalTime.of(0, 5), TimeParser.parse("12:05 am"))
        assertEquals(LocalTime.of(9, 5), TimeParser.parse("9.05 a.m."))
    }

    @Test
    fun `rejects times that do not exist`() {
        assertNull(TimeParser.parse("25:00"))
        assertNull(TimeParser.parse("10:75"))
        assertNull(TimeParser.parse("13:00 PM"))
    }

    @Test
    fun `written date and time replace the arrival time`() {
        // Sanima's email arrives 21 minutes after the withdrawal it reports.
        val result = parseAt(
            "NPR {amount} withdrawn from A/C {any} On {date} {time}.{any}",
            "NPR 175.00 withdrawn from A/C 00701110*****30 On 9/24/2026 8:41:37 AM. Remarks: x",
            LocalDateTime.of(2026, 9, 24, 9, 2)
        )
        // 9/24/2026 is not day-first, so the date falls back to the arrival day; the time
        // is what the bank wrote.
        assertEquals(LocalDateTime.of(2026, 9, 24, 8, 41, 37), result)
    }

    @Test
    fun `a time alone lands on the arrival day`() {
        val result = parseAt(
            "{amount} withdrawn at {time}.",
            "2,060.00 withdrawn at 21:00.",
            LocalDateTime.of(2026, 9, 23, 21, 1)
        )
        assertEquals(LocalDateTime.of(2026, 9, 23, 21, 0), result)
    }

    @Test
    fun `a late-night time in a message after midnight goes on the day before`() {
        val result = parseAt(
            "{amount} withdrawn at {time}.",
            "500.00 withdrawn at 23:58.",
            LocalDateTime.of(2026, 9, 24, 0, 3)
        )
        assertEquals(LocalDateTime.of(2026, 9, 23, 23, 58), result)
    }
}
