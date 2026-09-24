package com.abi.expensetracker.parser

import com.abi.expensetracker.data.Money

/**
 * Generic extractors, applied only for fields the matched rule did not capture.
 *
 * Splitting these out means a bank rule only has to pin down the amount and direction
 * reliably; the boilerplate fields are found the same way across every bank instead of
 * being re-encoded in each rule's regex.
 */
object FieldExtractors {

    private val ACCOUNT = Regex(
        """(?i)\b(?:a/c|a/c no|ac|acct|account|card|card no)\b\s*(?:number\s*)?[:.\s]*(?:x+|\*+|X+)?\s*(\d{3,6})\b"""
    )

    private val REF = Regex(
        """(?i)\b(?:ref(?:erence)?(?:\s*(?:no|num|number|id))?|txn\s*(?:id|no|number)|transaction\s*id|upi\s*ref(?:\s*no)?)\b[:.\s#]*([A-Za-z0-9]{4,24})\b"""
    )

    private val BALANCE = Regex(
        """(?i)\b(?:avl|avbl|available|clear|closing)\s*(?:bal|balance)\b[:.\s]*(?:rs\.?|npr|रु|रू)?\s*([\d,]+(?:\.\d{1,2})?)"""
    )

    /**
     * "Remarks: Khaja", "Narration - Rent", "Purpose: fees".
     *
     * Ends at a sentence break rather than running to the end of the message, because
     * what follows a remark in bank SMS is boilerplate about balances and helplines.
     */
    private val REMARK = Regex(
        """(?i)\b(?:remarks?|narration|purpose|particulars)\b[:.\s-]*([^.;\n]{1,60})"""
    )

    /**
     * Sanima's SMS abbreviation: "on 23/09/2026 21:00.Re:coffee,0070…". Only a fallback
     * behind [REMARK], and only with the colon, since "re" alone is an ordinary word and
     * an email subject's "Re:" should lose to a real "Remarks:" in the same text.
     */
    private val REMARK_SHORT = Regex("""(?i)\bre:\s*([^.;\n]{1,60})""")

    private val MERCHANT = Regex(
        """(?i)\b(?:to|at|towards|in favour of|vpa)\s+([A-Za-z0-9][A-Za-z0-9@._&'\-]*(?:\s+[A-Za-z0-9@._&'\-]+){0,3})"""
    )

    /** Words that signal the merchant name has ended and the next clause has begun. */
    private val MERCHANT_STOP = setOf(
        "on", "ref", "refno", "upi", "avl", "avbl", "available", "bal", "balance",
        "info", "not", "if", "call", "sms", "dated", "date", "txn", "transaction",
        "your", "a/c", "ac", "from", "thru", "through", "via",
        // The remark is its own field now, so its heading ends the merchant name.
        "remark", "remarks", "narration", "purpose", "particulars"
    )

    fun accountTail(body: String): String? = ACCOUNT.find(body)?.groupValues?.get(1)

    fun remark(body: String): String? =
        (REMARK.find(body) ?: REMARK_SHORT.find(body))
            ?.groupValues?.get(1)?.trim(' ', '-', ',', ':')?.ifBlank { null }

    fun refNumber(body: String): String? = REF.find(body)?.groupValues?.get(1)

    fun balanceMinor(body: String): Long? =
        BALANCE.find(body)?.groupValues?.get(1)?.let { Money.parseToMinor(it) }

    /**
     * Best-effort. Bank SMS has no delimiter around merchant names, so this trims at the
     * first word that starts a new clause. Misses land in the review queue by design —
     * a wrong merchant silently attached to a transaction is worse than a blank one.
     */
    fun merchant(body: String): String? {
        val captured = MERCHANT.find(body)?.groupValues?.get(1) ?: return null
        val words = captured.split(" ")
        val kept = words.takeWhile { word ->
            val bare = word.trim('.', ',', ';', ':').lowercase()
            bare.isNotEmpty() && bare !in MERCHANT_STOP
        }
        if (kept.isEmpty()) return null
        return kept.joinToString(" ").trim('.', ',', ';', ':', '-').ifBlank { null }
    }
}
