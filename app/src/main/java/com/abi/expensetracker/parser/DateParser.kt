package com.abi.expensetracker.parser

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Reads the date a bank wrote into a message.
 *
 * Banks are inconsistent: "20-09-26", "20/09/2026", "20-Sep-2026", "2026-09-20" and
 * "20 Sep 2026" all turn up, sometimes from the same bank on different message types.
 *
 * Day-first is assumed for all-numeric dates — "01/02/26" is 1 February, not 2 January.
 * An ISO-looking date (four-digit year first) is read as year-month-day, since that
 * ordering is unambiguous.
 *
 * Gregorian only. A Bikram Sambat date such as "2082-06-05" parses as a Gregorian year
 * 2082, which [SmsParser] then rejects as implausibly far from the message, so the
 * message timestamp is used instead. That is the safe outcome, not a correct one:
 * BS-to-AD conversion is not implemented.
 */
object DateParser {

    private val DAY_FIRST = listOf("d-M-yyyy", "d-M-yy")
    private val YEAR_FIRST = listOf("yyyy-M-d")
    private val NAMED_MONTH = listOf("d-MMM-yyyy", "d-MMM-yy", "d-MMMM-yyyy", "d-MMMM-yy")

    /** Returns null when the text is not a date this understands, rather than guessing. */
    fun parse(raw: String): LocalDate? {
        // Separators carry no meaning here, so they are normalised away and each candidate
        // ordering is tried against a single canonical shape.
        val normalised = raw.trim()
            .replace(Regex("""[/.\s,]+"""), "-")
            .replace(Regex("""-+"""), "-")
            .trim('-')
        if (normalised.isEmpty()) return null

        val firstPart = normalised.substringBefore('-')
        val looksYearFirst = firstPart.length == 4 && firstPart.all { it.isDigit() }
        val hasMonthName = normalised.any { it.isLetter() }

        val patterns = when {
            hasMonthName -> NAMED_MONTH
            looksYearFirst -> YEAR_FIRST
            else -> DAY_FIRST
        }

        for (pattern in patterns) {
            val formatter = formatterFor(pattern)
            try {
                return LocalDate.parse(normalised, formatter)
            } catch (e: DateTimeParseException) {
                // Try the next ordering.
            }
        }
        return null
    }

    /**
     * Case-insensitive, because senders write "20-SEP-26" as readily as "20-Sep-26" and a
     * strict formatter would silently reject the shouted form.
     */
    private fun formatterFor(pattern: String): DateTimeFormatter =
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern(pattern)
            .toFormatter(Locale.ENGLISH)
}

/**
 * Reads the time of day a bank wrote into a message: 24-hour ("21:00", "21:00:05") or
 * 12-hour with a marker ("8:41 AM", "1:20PM", "9.05 a.m."). Returns null rather than
 * guessing when the text is not a real time.
 */
object TimeParser {

    private val SHAPE = Regex("""^(\d{1,2})[:.](\d{2})(?:[:.](\d{2}))?\s*([AaPp])?\.?(?:[Mm]\.?)?$""")

    fun parse(raw: String): LocalTime? {
        val m = SHAPE.matchEntire(raw.trim()) ?: return null
        var hour = m.groupValues[1].toInt()
        val minute = m.groupValues[2].toInt()
        val second = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt() ?: 0
        val marker = m.groupValues[4].lowercase()
        if (minute > 59 || second > 59) return null
        when (marker) {
            "a" -> { if (hour !in 1..12) return null; if (hour == 12) hour = 0 }
            "p" -> { if (hour !in 1..12) return null; if (hour != 12) hour += 12 }
            else -> if (hour > 23) return null
        }
        return LocalTime.of(hour, minute, second)
    }
}
