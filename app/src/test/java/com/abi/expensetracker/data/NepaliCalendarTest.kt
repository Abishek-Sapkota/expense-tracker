package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.LimitBasis
import com.abi.expensetracker.data.model.SpendingLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NepaliCalendarTest {

    private val kathmandu: ZoneId = ZoneId.of("Asia/Kathmandu")

    @Test
    fun `epoch day converts both ways`() {
        assertEquals(NepaliDate(2000, 1, 1), NepaliCalendar.fromGregorian(LocalDate.of(1943, 4, 14)))
        assertEquals(LocalDate.of(1943, 4, 14), NepaliCalendar.toGregorian(2000, 1, 1))
    }

    @Test
    fun `new year and a mid-year date convert`() {
        assertEquals(NepaliDate(2083, 1, 1), NepaliCalendar.fromGregorian(LocalDate.of(2026, 4, 14)))
        assertEquals(NepaliDate(2083, 6, 7), NepaliCalendar.fromGregorian(LocalDate.of(2026, 9, 23)))
    }

    @Test
    fun `month lengths come from the table, not a rule`() {
        assertEquals(32, NepaliCalendar.daysInMonth(2083, 3))
        assertEquals(29, NepaliCalendar.daysInMonth(2083, 8))
        assertNull(NepaliCalendar.daysInMonth(1999, 1))
        assertNull(NepaliCalendar.daysInMonth(2111, 1))
    }

    @Test
    fun `every day of a decade round-trips`() {
        var date = LocalDate.of(2070, 1, 1).let { NepaliCalendar.toGregorian(2070, 1, 1)!! }
        val end = NepaliCalendar.toGregorian(2080, 1, 1)!!
        while (date.isBefore(end)) {
            val bs = NepaliCalendar.fromGregorian(date)!!
            assertEquals(date, NepaliCalendar.toGregorian(bs.year, bs.month, bs.day))
            date = date.plusDays(1)
        }
    }

    @Test
    fun `monthly limit follows the nepali month when that calendar is on`() {
        val limit = SpendingLimit(500_000, LimitBasis.MONTH)
        val range = limit.range(kathmandu, LocalDate.of(2026, 9, 23), nepaliMonths = true)

        // Ashoj 2083 is 17 September to 17 October 2026, not 1 to 30 September.
        assertEquals(
            LocalDate.of(2026, 9, 17).atStartOfDay(kathmandu).toInstant().toEpochMilli(),
            range.startMillis
        )
        assertEquals(
            LocalDate.of(2026, 10, 18).atStartOfDay(kathmandu).toInstant().toEpochMilli(),
            range.endMillis
        )
    }

    @Test
    fun `month window walks nepali months, not gregorian ones`() {
        val ashwin = MonthWindow.of(LocalDate.of(2026, 9, 23), nepali = true)
        assertEquals(LocalDate.of(2026, 9, 17), ashwin.firstDay)
        assertEquals(31, ashwin.dayCount)
        assertEquals("Ashwin 2083", ashwin.label)
        assertEquals(1, ashwin.dayOf(LocalDate.of(2026, 9, 17)))
        assertEquals(31, ashwin.dayOf(LocalDate.of(2026, 10, 17)))
        assertNull(ashwin.dayOf(LocalDate.of(2026, 10, 18)))

        // Bhadra before it, Kartik after — both whole BS months, never a half step.
        assertEquals("Bhadra 2083", ashwin.previous.label)
        assertEquals("Kartik 2083", ashwin.next.label)
        assertEquals(LocalDate.of(2026, 10, 18), ashwin.next.firstDay)
    }

    @Test
    fun `month window stays gregorian when the calendar is off`() {
        val september = MonthWindow.of(LocalDate.of(2026, 9, 23), nepali = false)
        assertEquals(LocalDate.of(2026, 9, 1), september.firstDay)
        assertEquals(30, september.dayCount)
        assertEquals("August 2026", september.previous.label)
    }
}
