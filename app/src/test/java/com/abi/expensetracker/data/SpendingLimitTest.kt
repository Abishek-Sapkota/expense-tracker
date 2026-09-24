package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.LimitBasis
import com.abi.expensetracker.data.model.LimitStatus
import com.abi.expensetracker.data.model.SpendingLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SpendingLimitTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kathmandu")

    @Test
    fun `daily limit covers exactly the one day`() {
        val limit = SpendingLimit(50_000, LimitBasis.DAY)
        val range = limit.range(zone, LocalDate.of(2026, 9, 22))

        assertEquals(
            Period.rangeOfDays(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 22), zone),
            range
        )
    }

    @Test
    fun `monthly limit runs from the first to the last day of the calendar month`() {
        val limit = SpendingLimit(500_000, LimitBasis.MONTH)
        // Mid-month: the window must still start on the 1st, not 30 days back.
        val range = limit.range(zone, LocalDate.of(2026, 9, 22))

        assertEquals(
            Period.rangeOfDays(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), zone),
            range
        )
    }

    @Test
    fun `monthly limit resets on the first of the month`() {
        val limit = SpendingLimit(500_000, LimitBasis.MONTH)
        val september = limit.range(zone, LocalDate.of(2026, 9, 30))
        val october = limit.range(zone, LocalDate.of(2026, 10, 1))

        assertEquals(september.endMillis, october.startMillis)
    }

    @Test
    fun `february length is respected`() {
        val limit = SpendingLimit(500_000, LimitBasis.MONTH)
        val range = limit.range(zone, LocalDate.of(2028, 2, 10))

        assertEquals(
            Period.rangeOfDays(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29), zone),
            range
        )
    }

    @Test
    fun `remaining counts down and the bar fills`() {
        val status = LimitStatus(SpendingLimit(100_000, LimitBasis.DAY), spentMinor = 25_000)

        assertEquals(75_000, status.remainingMinor)
        assertEquals(0.25f, status.fraction, 0.001f)
        assertFalse(status.isOver)
        assertFalse(status.isClose)
    }

    @Test
    fun `four fifths in is close but not over`() {
        val status = LimitStatus(SpendingLimit(100_000, LimitBasis.DAY), spentMinor = 80_000)

        assertTrue(status.isClose)
        assertFalse(status.isOver)
    }

    @Test
    fun `spending exactly the limit is not over`() {
        val status = LimitStatus(SpendingLimit(100_000, LimitBasis.DAY), spentMinor = 100_000)

        assertFalse(status.isOver)
        assertEquals(0L, status.remainingMinor)
        assertEquals(1f, status.fraction, 0.001f)
    }

    @Test
    fun `over the limit reports the overshoot and stops the bar at full`() {
        val status = LimitStatus(SpendingLimit(100_000, LimitBasis.DAY), spentMinor = 130_000)

        assertTrue(status.isOver)
        assertEquals(-30_000, status.remainingMinor)
        assertEquals(1f, status.fraction, 0.001f)
    }
}
