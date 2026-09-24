package com.abi.expensetracker.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateParserTest {

    @Test
    fun `reads the dash-separated form banks use most`() {
        assertEquals(LocalDate.of(2026, 9, 20), DateParser.parse("20-09-26"))
        assertEquals(LocalDate.of(2026, 9, 20), DateParser.parse("20-09-2026"))
    }

    @Test
    fun `separator does not matter`() {
        val expected = LocalDate.of(2026, 9, 20)
        assertEquals(expected, DateParser.parse("20/09/2026"))
        assertEquals(expected, DateParser.parse("20.09.2026"))
        assertEquals(expected, DateParser.parse("20 09 2026"))
    }

    @Test
    fun `all-numeric dates are read day-first`() {
        // 01/02/26 is 1 February, not 2 January. Getting this backwards would file a
        // month of spending under the wrong month without anything looking wrong.
        assertEquals(LocalDate.of(2026, 2, 1), DateParser.parse("01/02/26"))
        assertEquals(LocalDate.of(2026, 1, 2), DateParser.parse("02/01/26"))
    }

    @Test
    fun `a four-digit year in front is read as year-month-day`() {
        assertEquals(LocalDate.of(2026, 9, 20), DateParser.parse("2026-09-20"))
    }

    @Test
    fun `month names are understood, short and long`() {
        val expected = LocalDate.of(2026, 9, 20)
        assertEquals(expected, DateParser.parse("20-Sep-2026"))
        assertEquals(expected, DateParser.parse("20 Sep 2026"))
        assertEquals(expected, DateParser.parse("20-September-2026"))
        assertEquals(expected, DateParser.parse("20 SEP 26"))
    }

    @Test
    fun `single-digit days and months parse`() {
        assertEquals(LocalDate.of(2026, 3, 5), DateParser.parse("5-3-26"))
    }

    @Test
    fun `two-digit years land in this century`() {
        assertEquals(2026, DateParser.parse("20-09-26")!!.year)
        assertEquals(2001, DateParser.parse("20-09-01")!!.year)
    }

    @Test
    fun `text that is not a date returns null rather than a guess`() {
        assertNull(DateParser.parse("SWIGGY"))
        assertNull(DateParser.parse(""))
        assertNull(DateParser.parse("123456789"))
    }

    @Test
    fun `an impossible date is rejected, not rolled over`() {
        // 32 January must not silently become 1 February.
        assertNull(DateParser.parse("32-01-26"))
        assertNull(DateParser.parse("20-13-26"))
    }
}
