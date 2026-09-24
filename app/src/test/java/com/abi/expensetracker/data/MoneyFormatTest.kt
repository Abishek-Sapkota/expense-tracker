package com.abi.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lakh digit grouping and sign placement.
 *
 * Anyone reading a rupee amount reads lakhs and crores off the comma positions, so
 * western grouping is not merely unidiomatic here — it misstates the magnitude at a
 * glance.
 */
class MoneyFormatTest {

    @Test
    fun `groups the last three digits, then pairs`() {
        assertEquals("रु1,23,456.78", Money.format(1_23_456_78L))
        assertEquals("रु12,34,567.89", Money.format(12_34_567_89L))
    }

    @Test
    fun `a crore groups as two pairs and a triple`() {
        assertEquals("रु1,00,00,000.00", Money.format(1_00_00_000_00L))
        assertEquals("रु12,34,56,789.00", Money.format(12_34_56_789_00L))
    }

    @Test
    fun `amounts under a thousand take no separator`() {
        assertEquals("रु450.00", Money.format(45_000L))
        assertEquals("रु9.99", Money.format(999L))
        assertEquals("रु0.00", Money.format(0L))
    }

    @Test
    fun `the first separator appears at four digits`() {
        assertEquals("रु999.00", Money.format(999_00L))
        assertEquals("रु1,000.00", Money.format(1_000_00L))
        assertEquals("रु99,999.00", Money.format(99_999_00L))
        assertEquals("रु1,00,000.00", Money.format(1_00_000_00L))
    }

    @Test
    fun `paise are always shown to two places`() {
        assertEquals("रु5.00", Money.format(500L))
        assertEquals("रु5.05", Money.format(505L))
        assertEquals("रु5.50", Money.format(550L))
    }

    @Test
    fun `signed amounts always lead with a sign, never colour alone`() {
        assertEquals("−रु450.00", Money.formatSigned(45_000L, isCredit = false))
        assertEquals("+रु5,000.00", Money.formatSigned(5_00_000L, isCredit = true))
    }

    @Test
    fun `signing uses a real minus sign, not an ASCII hyphen`() {
        val formatted = Money.formatSigned(45_000L, isCredit = false)
        assertTrue(formatted.startsWith("−"))
        assertFalse(formatted.startsWith("-"))
    }

    @Test
    fun `a negative stored amount does not produce a double sign`() {
        assertEquals("−रु450.00", Money.formatSigned(-45_000L, isCredit = false))
        assertEquals("+रु450.00", Money.formatSigned(-45_000L, isCredit = true))
    }

    @Test
    fun `parsing then formatting round-trips a lakh-grouped amount`() {
        val text = "1,23,456.78"
        assertEquals("रु$text", Money.format(Money.parseToMinor(text)!!))
    }

    @Test
    fun `grouping survives the largest amounts a ledger will see`() {
        assertEquals("रु1,00,00,00,000.00", Money.format(1_00_00_00_000_00L))
    }
}
