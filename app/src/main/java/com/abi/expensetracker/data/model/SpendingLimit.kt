package com.abi.expensetracker.data.model

import com.abi.expensetracker.data.DateRange
import com.abi.expensetracker.data.NepaliCalendar
import com.abi.expensetracker.data.Period
import java.time.LocalDate
import java.time.ZoneId

/** Whether a limit is a daily allowance or a monthly budget. */
enum class LimitBasis(val label: String) {
    DAY("Per day"),
    MONTH("Per month");

    companion object {
        fun fromName(value: String?): LimitBasis =
            entries.firstOrNull { it.name == value } ?: DAY
    }
}

/**
 * A cap on spending, and the window it applies to.
 *
 * The window is deliberately a calendar one — today, or the calendar month — unlike the
 * rolling windows the home screen filters by (see [Period]). A budget is something the
 * user set against a salary date and a rent day; a monthly limit that quietly meant "the
 * last 30 days" would never reset, and the number it showed on the 1st would still carry
 * last month's spending.
 */
data class SpendingLimit(
    val amountMinor: Long,
    val basis: LimitBasis
) {
    /**
     * The window this limit is measured over.
     *
     * [nepaliMonths] comes from the app-wide calendar setting: with it on, "per month"
     * means Ashwin, not September. A day is a day on either calendar, so it is unaffected.
     */
    fun range(
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone),
        nepaliMonths: Boolean = false
    ): DateRange = when (basis) {
        LimitBasis.DAY -> Period.rangeOfDays(today, today, zone)
        // Falls back to the Gregorian month when the BS table does not reach this date: a
        // window a fortnight off beats a limit that stops working in 2110 BS.
        LimitBasis.MONTH -> (if (nepaliMonths) nepaliMonthRange(today, zone) else null)
            ?: Period.rangeOfDays(
                today.withDayOfMonth(1),
                today.withDayOfMonth(today.lengthOfMonth()),
                zone
            )
    }

    private fun nepaliMonthRange(today: LocalDate, zone: ZoneId): DateRange? {
        val bs = NepaliCalendar.fromGregorian(today) ?: return null
        val first = bs.firstDayOfMonth() ?: return null
        val last = bs.lastDayOfMonth() ?: return null
        return Period.rangeOfDays(first, last, zone)
    }

    /** When the window rolls over and spending starts from zero again. */
    fun resetsAtMillis(
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone),
        nepaliMonths: Boolean = false
    ): Long = range(zone, today, nepaliMonths).endMillis
}

/**
 * A limit together with what has been spent inside its window.
 *
 * [spentMinor] counts debits only, the same figure the period total on the home screen
 * shows: money received is not a smaller grocery bill, and netting it off would let a
 * salary hide a month of overspending.
 */
data class LimitStatus(
    val limit: SpendingLimit,
    val spentMinor: Long
) {
    /** Negative once the limit is passed; the UI states the overshoot rather than 0. */
    val remainingMinor: Long get() = limit.amountMinor - spentMinor

    val isOver: Boolean get() = spentMinor > limit.amountMinor

    /**
     * How full the bar is, clamped to 1. Past the limit the bar is full and the colour
     * and wording carry the overshoot — a bar drawn past its end says nothing extra.
     */
    val fraction: Float
        get() = when {
            limit.amountMinor <= 0L -> 0f
            else -> (spentMinor.toFloat() / limit.amountMinor.toFloat()).coerceIn(0f, 1f)
        }

    /** Four fifths in is the point worth a colour change but not an alarm. */
    val isClose: Boolean get() = !isOver && fraction >= 0.8f
}
