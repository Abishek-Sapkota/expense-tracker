package com.abi.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PeriodTest {

    private val zone = ZoneId.of("Asia/Kathmandu")
    private val today = LocalDate.of(2026, 9, 20)

    private fun daysIn(range: DateRange): Long =
        (range.endMillis - range.startMillis) / (24 * 60 * 60 * 1000L)

    @Test
    fun `today covers exactly one day`() {
        assertEquals(1L, daysIn(Period.TODAY.range(zone, today)))
    }

    @Test
    fun `yesterday ends where today begins`() {
        val yesterday = Period.YESTERDAY.range(zone, today)
        val todayRange = Period.TODAY.range(zone, today)
        assertEquals(todayRange.startMillis, yesterday.endMillis)
        assertEquals(1L, daysIn(yesterday))
    }

    @Test
    fun `last 7 days includes today, so it spans seven days not eight`() {
        assertEquals(7L, daysIn(Period.LAST_7_DAYS.range(zone, today)))
    }

    @Test
    fun `last 30 days includes today`() {
        val range = Period.LAST_30_DAYS.range(zone, today)
        assertEquals(30L, daysIn(range))
        assertEquals(Period.TODAY.range(zone, today).endMillis, range.endMillis)
    }

    @Test
    fun `range boundaries sit on local midnight, not UTC midnight`() {
        val range = Period.TODAY.range(zone, today)
        val startLocal = java.time.Instant.ofEpochMilli(range.startMillis).atZone(zone)
        assertEquals(0, startLocal.hour)
        assertEquals(0, startLocal.minute)
        assertEquals(today, startLocal.toLocalDate())
    }

    @Test
    fun `a single-day custom range is one day, not zero`() {
        assertEquals(1L, daysIn(Period.rangeOfDays(today, today, zone)))
    }

    @Test
    fun `custom range is inclusive of both ends`() {
        val range = Period.rangeOfDays(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), zone)
        assertEquals(30L, daysIn(range))
    }
}
