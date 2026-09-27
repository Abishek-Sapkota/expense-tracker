package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Txn

/**
 * The word to offer as a category keyword when the user files a payment by hand.
 *
 * The merchant when there is one: "SWIGGY" filed under Dining should file every Swiggy
 * order. Otherwise the part of the remark the user typed, which banks put last after
 * their own codes: "MOS/eSewa/9851180816/khaja" → "khaja". Codes, phone numbers and the
 * banks' channel tags are skipped, because a keyword that matches every eSewa transfer
 * files hundreds of rows wrongly at once.
 */
object KeywordSuggester {

    private val SEGMENTS = Regex("""[/,:;|]+""")

    /** Channel and scheme tags banks prefix remarks with; never what the money was for. */
    private val GENERIC = setOf("mos", "ibs", "ips", "fonepay", "qr", "trf", "transfer", "payment", "mobile")

    fun suggest(txn: Txn): String? {
        txn.merchant?.let { cleaned(it) }?.let { return it }
        val remark = txn.remark ?: return null
        return remark.split(SEGMENTS).mapNotNull { cleaned(it) }.lastOrNull()
    }

    private fun cleaned(text: String): String? {
        val word = text.trim().trim('.', '-', '_').lowercase()
        val letters = word.count { it.isLetter() }
        return word.takeIf { letters >= 3 && letters * 2 >= word.length && it !in GENERIC }
    }
}
