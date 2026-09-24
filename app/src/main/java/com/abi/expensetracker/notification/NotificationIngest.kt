package com.abi.expensetracker.notification

/**
 * Decides which notifications are worth keeping and flattens one into a single text body.
 *
 * Pure text-to-text, no Android types, so it is unit-testable on the desktop — the same
 * reason [com.abi.expensetracker.parser.SmsParser] avoids them.
 *
 * The filter is deliberately strict. A notification listener sees every notification on
 * the phone, and storing them all would turn the raw table — the one table that is never
 * deleted — into a log of the user's entire life. Only text that reads like a money
 * movement is kept; everything else is dropped without ever being written down.
 */
object NotificationIngest {

    /** Currency-tagged amount, in the forms Nepali banks and wallets actually post. */
    private val AMOUNT = Regex(
        """(?i)(?:(?:rs\.?|npr|inr|रु|रू|₹)\s*[\d,]+(?:\.\d{1,2})?""" +
            """|[\d,]+(?:\.\d{1,2})?\s*(?:rs\.?|npr|रु|रू))"""
    )

    /** A word that makes the amount a transaction rather than a price or an OTP. */
    private val MONEY_VERB = Regex(
        """(?i)\b(debited|credited|spent|withdrawn|paid|payment|deducted|transferred|""" +
            """received|deposited|refunded|txn|transaction|balance)\b"""
    )

    /**
     * An OTP or a login alert can carry both an amount and a verb ("Rs. 5000 transaction
     * OTP is 123456"), and acting on one would book a transaction that never happened.
     */
    private val ONE_TIME_CODE = Regex("""(?i)\b(otp|one[- ]time (?:password|code)|verification code)\b""")

    fun looksFinancial(body: String): Boolean =
        AMOUNT.containsMatchIn(body) &&
            MONEY_VERB.containsMatchIn(body) &&
            !ONE_TIME_CODE.containsMatchIn(body)

    /**
     * Join the notification's parts into one line for the parser.
     *
     * Parts are joined with a space rather than a newline because a bank's phrasing is
     * regularly split across title and text ("Nabil Bank" / "Rs. 500 debited..."), and a
     * pattern spanning the break has to still match.
     *
     * [bigText] replaces [text] when it is present and longer: a collapsed notification
     * truncates with an ellipsis, and the truncated copy can cut an amount in half.
     */
    fun composeBody(title: String?, text: String?, bigText: String? = null): String {
        val longest = listOfNotNull(text, bigText).maxByOrNull { it.length }
        return listOfNotNull(title?.trim(), longest?.trim())
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
    }
}
