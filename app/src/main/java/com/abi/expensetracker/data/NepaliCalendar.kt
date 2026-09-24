package com.abi.expensetracker.data

import java.time.LocalDate

/**
 * The Bikram Sambat calendar, enough of it to answer "when does this month start and end".
 *
 * Nepali months do not line up with Gregorian ones — Ashoj 2083 runs from mid-September to
 * mid-October — so a budget set "per month" by someone who thinks in BS resets on the wrong
 * day if the app only knows January to December.
 *
 * Month lengths are astronomical, not calculated: there is no rule to derive them from, so
 * they ship as a table. The values come from the published Bikram Sambat tables (as also
 * carried by the MIT-licensed `bikram-sambat` dataset) and are encoded as one digit per
 * month, the days above 28 — every BS month is 28 to 32 days.
 *
 * Outside [FIRST_YEAR]..[LAST_YEAR] the table says nothing and the callers fall back to the
 * Gregorian month, which is wrong by a fortnight but never crashes and never silently
 * reports a window that does not exist.
 */
object NepaliCalendar {

    /** 1 Baishakh [FIRST_YEAR] BS, the day the table starts counting from. */
    private val EPOCH: LocalDate = LocalDate.of(1943, 4, 14)

    const val FIRST_YEAR = 2000
    const val LAST_YEAR = 2110

    /** English spellings as Nepali banks and calendars print them. */
    val MONTH_NAMES = listOf(
        "Baishakh", "Jestha", "Ashadh", "Shrawan", "Bhadra", "Ashwin",
        "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"
    )

    /** Days per month, one digit each, offset from 28. Index 0 is [FIRST_YEAR]. */
    private val MONTH_DAYS = arrayOf(
    "243432221213", // 2000
    "334333212122", // 2001
    "334432212122", // 2002
    "343432221123", // 2003
    "243432221213", // 2004
    "334333212122", // 2005
    "334432212122", // 2006
    "343432221123", // 2007
    "333433122113", // 2008
    "334333212122", // 2009
    "334432212122", // 2010
    "343432221123", // 2011
    "333433122122", // 2012
    "334333212122", // 2013
    "334432212122", // 2014
    "343432221123", // 2015
    "333433122122", // 2016
    "334333212122", // 2017
    "343432212122", // 2018
    "343432221213", // 2019
    "333433212122", // 2020
    "334333212122", // 2021
    "343432221122", // 2022
    "343432221213", // 2023
    "333433212122", // 2024
    "334333212122", // 2025
    "343432221123", // 2026
    "243432221213", // 2027
    "334333212122", // 2028
    "334342212122", // 2029
    "343432221123", // 2030
    "243432221213", // 2031
    "334333212122", // 2032
    "334432212122", // 2033
    "343432221123", // 2034
    "243433122113", // 2035
    "334333212122", // 2036
    "334432212122", // 2037
    "343432221123", // 2038
    "333433122122", // 2039
    "334333212122", // 2040
    "334432212122", // 2041
    "343432221123", // 2042
    "333433122122", // 2043
    "334333212122", // 2044
    "343432212122", // 2045
    "343432221123", // 2046
    "333433212122", // 2047
    "334333212122", // 2048
    "343432221122", // 2049
    "343432221213", // 2050
    "333433212122", // 2051
    "334333212122", // 2052
    "343432221122", // 2053
    "343432221213", // 2054
    "334333212122", // 2055
    "334342212122", // 2056
    "343432221123", // 2057
    "243432221213", // 2058
    "334333212122", // 2059
    "334432212122", // 2060
    "343432221123", // 2061
    "333433121213", // 2062
    "334333212122", // 2063
    "334432212122", // 2064
    "343432221123", // 2065
    "333433122113", // 2066
    "334333212122", // 2067
    "334432212122", // 2068
    "343432221123", // 2069
    "333433122122", // 2070
    "334333212122", // 2071
    "343432212122", // 2072
    "343432221123", // 2073
    "333433212122", // 2074
    "334333212122", // 2075
    "343432221122", // 2076
    "343432221213", // 2077
    "333433212122", // 2078
    "334333212122", // 2079
    "343432221122", // 2080
    "343432221213", // 2081
    "334333212122", // 2082
    "334333212122", // 2083
    "343432221123", // 2084
    "243432221213", // 2085
    "334333212122", // 2086
    "334432212122", // 2087
    "343432221123", // 2088
    "243432221213", // 2089
    "334333212122", // 2090
    "334432212122", // 2091
    "343432221123", // 2092
    "333433122113", // 2093
    "334333212122", // 2094
    "334432212122", // 2095
    "343432221123", // 2096
    "333433122122", // 2097
    "334333212122", // 2098
    "334432212122", // 2099
    "343432221123", // 2100
    "333433122122", // 2101
    "334333212122", // 2102
    "343432212122", // 2103
    "343432221213", // 2104
    "333433212122", // 2105
    "334333212122", // 2106
    "343432221122", // 2107
    "343432221213", // 2108
    "333433212122", // 2109
    "334333212122", // 2110
    )

    fun supportsYear(bsYear: Int): Boolean = bsYear in FIRST_YEAR..LAST_YEAR

    /** Days in a BS month, or null when the table does not reach that year. */
    fun daysInMonth(bsYear: Int, bsMonth: Int): Int? {
        if (!supportsYear(bsYear) || bsMonth !in 1..12) return null
        return MONTH_DAYS[bsYear - FIRST_YEAR][bsMonth - 1].digitToInt() + 28
    }

    /** Days in a whole BS year, or null when the table does not reach it. */
    private fun daysInYear(bsYear: Int): Int? {
        if (!supportsYear(bsYear)) return null
        return MONTH_DAYS[bsYear - FIRST_YEAR].sumOf { it.digitToInt() + 28 }
    }

    /** The BS date for a Gregorian one, or null outside the table. */
    fun fromGregorian(date: LocalDate): NepaliDate? {
        var remaining = java.time.temporal.ChronoUnit.DAYS.between(EPOCH, date).toInt()
        if (remaining < 0) return null

        var year = FIRST_YEAR
        while (true) {
            val yearDays = daysInYear(year) ?: return null
            if (remaining < yearDays) break
            remaining -= yearDays
            year++
        }

        var month = 1
        while (true) {
            val monthDays = daysInMonth(year, month) ?: return null
            if (remaining < monthDays) break
            remaining -= monthDays
            month++
        }

        return NepaliDate(year, month, remaining + 1)
    }

    /** The Gregorian date for a BS one, or null outside the table. */
    fun toGregorian(bsYear: Int, bsMonth: Int, bsDay: Int): LocalDate? {
        val monthDays = daysInMonth(bsYear, bsMonth) ?: return null
        if (bsDay !in 1..monthDays) return null

        var days = 0L
        for (year in FIRST_YEAR until bsYear) days += daysInYear(year) ?: return null
        for (month in 1 until bsMonth) days += daysInMonth(bsYear, month) ?: return null
        return EPOCH.plusDays(days + bsDay - 1)
    }
}

/** A date on the Bikram Sambat calendar. */
data class NepaliDate(val year: Int, val month: Int, val day: Int) {

    val monthName: String get() = NepaliCalendar.MONTH_NAMES[month - 1]

    /** "Ashoj 7, 2083" — the order a Nepali calendar prints. */
    override fun toString(): String = "$monthName $day, $year"

    /** The first day of this month as a Gregorian date, or null outside the table. */
    fun firstDayOfMonth(): LocalDate? = NepaliCalendar.toGregorian(year, month, 1)

    /** The last day of this month as a Gregorian date, or null outside the table. */
    fun lastDayOfMonth(): LocalDate? {
        val days = NepaliCalendar.daysInMonth(year, month) ?: return null
        return NepaliCalendar.toGregorian(year, month, days)
    }
}
