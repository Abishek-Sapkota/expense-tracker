package com.abi.expensetracker.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * How dates are written once the app knows which calendar the user reads.
 *
 * Nepali mode shows both dates rather than replacing one with the other: a bank message,
 * a receipt and a bank statement are all in AD, so a ledger that only said "Ashwin 7"
 * could not be checked against any of them.
 */
object CalendarDates {

    private val dayMonth = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
    private val dayMonthYear = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    /** "Ashwin 7 · 23 Sep" in Nepali mode, "23 Sep" otherwise. */
    fun dayLabel(date: LocalDate, nepali: Boolean): String {
        val gregorian = date.format(dayMonth)
        val bs = if (nepali) NepaliCalendar.fromGregorian(date) else null
        return if (bs == null) gregorian else "${bs.monthName} ${bs.day} · $gregorian"
    }

    /** The same with the year on both, for the places that state a full date. */
    fun fullDateLabel(date: LocalDate, nepali: Boolean): String {
        val gregorian = date.format(dayMonthYear)
        val bs = if (nepali) NepaliCalendar.fromGregorian(date) else null
        return if (bs == null) gregorian else "${bs.monthName} ${bs.day}, ${bs.year} · $gregorian"
    }

    /** "Ashwin 2083" or "September 2026" — the title of a month window. */
    fun monthLabel(monthStart: LocalDate, nepali: Boolean): String {
        val bs = if (nepali) NepaliCalendar.fromGregorian(monthStart) else null
        return if (bs == null) monthStart.format(monthYear) else "${bs.monthName} ${bs.year}"
    }
}

/**
 * One month of the calendar the user reads, as the days it actually covers.
 *
 * Held as a first day plus a length rather than a `YearMonth`, because a Nepali month is
 * neither: Ashwin 2083 starts on 17 September and runs 31 days across two Gregorian
 * months. Everything downstream — the range to query, the bars to draw, the step to the
 * previous month — is the same arithmetic on either calendar once expressed this way.
 */
data class MonthWindow(
    val firstDay: LocalDate,
    val dayCount: Int,
    val nepali: Boolean
) {
    val range: DateRange get() = Period.rangeOfDays(firstDay, firstDay.plusDays(dayCount - 1L))

    val previous: MonthWindow get() = of(firstDay.minusDays(1), nepali)

    val next: MonthWindow get() = of(firstDay.plusDays(dayCount.toLong()), nepali)

    val label: String get() = CalendarDates.monthLabel(firstDay, nepali)

    /** 1-based day of this month, or null for a date outside it. */
    fun dayOf(date: LocalDate): Int? {
        val offset = java.time.temporal.ChronoUnit.DAYS.between(firstDay, date).toInt()
        return if (offset in 0 until dayCount) offset + 1 else null
    }

    /** Whether [date] falls in this window. */
    operator fun contains(date: LocalDate): Boolean = dayOf(date) != null

    companion object {
        /** The month containing [date], on whichever calendar is in use. */
        fun of(date: LocalDate, nepali: Boolean): MonthWindow {
            if (nepali) {
                val bs = NepaliCalendar.fromGregorian(date)
                val first = bs?.firstDayOfMonth()
                val days = bs?.let { NepaliCalendar.daysInMonth(it.year, it.month) }
                // Outside the BS table the Gregorian month stands in, so a user scrolling
                // back through decades of trends runs out of Nepali months rather than
                // hitting a blank screen.
                if (first != null && days != null) return MonthWindow(first, days, true)
            }
            return MonthWindow(date.withDayOfMonth(1), date.lengthOfMonth(), false)
        }
    }
}
