package com.abi.expensetracker.data

import java.time.LocalDate
import java.time.ZoneId

/** Half-open instant range: [startMillis, endMillis). */
data class DateRange(val startMillis: Long, val endMillis: Long)

/**
 * "Last week" and "last month" are deliberately rolling windows of 7 and 30 days ending
 * today, not calendar weeks or months. On the 2nd of a month a calendar "last month"
 * would report two days of spending, which reads as a bug to anyone glancing at it.
 */
enum class Period(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 days"),
    LAST_30_DAYS("Last 30 days"),
    CUSTOM("Custom");

    fun range(zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): DateRange =
        when (this) {
            TODAY -> rangeOfDays(today, today, zone)
            YESTERDAY -> rangeOfDays(today.minusDays(1), today.minusDays(1), zone)
            LAST_7_DAYS -> rangeOfDays(today.minusDays(6), today, zone)
            LAST_30_DAYS -> rangeOfDays(today.minusDays(29), today, zone)
            // Meaningless without user-chosen dates; the caller supplies those instead.
            CUSTOM -> rangeOfDays(today, today, zone)
        }

    companion object {
        /** Inclusive of both dates, expanded to the local day boundaries around them. */
        fun rangeOfDays(first: LocalDate, last: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DateRange {
            val start = first.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = last.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return DateRange(start, end)
        }
    }
}
