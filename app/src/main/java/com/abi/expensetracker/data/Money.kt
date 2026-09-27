package com.abi.expensetracker.data

import java.math.BigDecimal
import java.math.RoundingMode

/** Amounts live as minor units (paise). Parsing and formatting both go through here. */
object Money {

    /** U+2212 MINUS SIGN, not an ASCII hyphen: it aligns with the digits and reads as maths. */
    const val MINUS = "−"
    const val PLUS = "+"
    const val RUPEE = "रु"

    /**
     * Accepts what banks actually write: "1,234.50", "1234", "12.5", "1,23,456.78".
     * Returns null rather than guessing when the text is not a number.
     */
    fun parseToMinor(raw: String): Long? {
        val trimmed = raw.replace(" ", "").trim()
        // Digits with optional grouping commas and at most two decimals, nothing else.
        // BigDecimal alone took "1e5" as a lakh, rounded "1.234" without saying, and read
        // a comma-decimal "12,50" as 1,250.
        if (!PLAIN_AMOUNT.matches(trimmed)) return null
        return try {
            BigDecimal(trimmed.replace(",", ""))
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        } catch (e: ArithmeticException) {
            // More than a Long of paise: not an amount anyone typed or a bank sent.
            null
        }
    }

    /**
     * Plain digits, or grouped as banks group them: lakh style "1,23,456" or western
     * "123,456", so the last group is always three digits. "12,50" fits neither and is
     * refused rather than guessed at.
     */
    private val PLAIN_AMOUNT = Regex("""(?:\d+|\d{1,3}(?:,\d{2,3})*,\d{3})(?:\.\d{1,2})?""")

    /**
     * Plain editable text, e.g. `1234.50`: no symbol, no grouping.
     *
     * This is what goes back into an amount field, so that reading a stored amount and
     * saving it unchanged is a round trip rather than a reparse of formatted output.
     */
    fun toPlainAmount(minor: Long): String =
        BigDecimal(kotlin.math.abs(minor)).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
            .toPlainString()

    /** Unsigned, e.g. `रु1,23,456.78`. */
    fun format(minor: Long): String = RUPEE + groupInLakhs(minor)

    /**
     * Signed, e.g. `−रु450.00` or `+रु5,000.00`.
     *
     * The sign is always present and always leads, so money in and money out are
     * distinguishable without relying on color.
     */
    fun formatSigned(minor: Long, isCredit: Boolean): String {
        val sign = if (isCredit) PLUS else MINUS
        return sign + RUPEE + groupInLakhs(kotlin.math.abs(minor))
    }

    /**
     * Lakh grouping: the last three digits, then pairs — 1,23,456.78 rather than the
     * western 123,456.78. Rupee amounts are read in lakhs and crores off the comma
     * positions, so western grouping is actively misleading here.
     */
    private fun groupInLakhs(minor: Long): String {
        val value = BigDecimal(minor).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
        val plain = value.abs().toPlainString()
        val dot = plain.indexOf('.')
        val whole = if (dot >= 0) plain.substring(0, dot) else plain
        val fraction = if (dot >= 0) plain.substring(dot) else ".00"

        if (whole.length <= 3) return whole + fraction

        val lastThree = whole.substring(whole.length - 3)
        val rest = whole.substring(0, whole.length - 3)

        val grouped = StringBuilder()
        var index = rest.length
        while (index > 2) {
            grouped.insert(0, "," + rest.substring(index - 2, index))
            index -= 2
        }
        grouped.insert(0, rest.substring(0, index))

        return "$grouped,$lastThree$fraction"
    }
}
