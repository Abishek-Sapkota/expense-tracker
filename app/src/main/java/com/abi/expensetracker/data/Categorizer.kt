package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.Txn

/**
 * Picks a category for a transaction from its merchant and remark.
 *
 * Pure, so it is unit-testable on the desktop for the same reason the parser is: which
 * keyword wins is the kind of thing that only shows up against real merchant names.
 *
 * Matching is substring, case-insensitive, and the longest matching keyword wins. Longest
 * rather than first because keyword lists overlap — "nepal oil" and "oil" would otherwise
 * be decided by the order categories happen to be stored in.
 */
class Categorizer(private val categories: List<Category>) {

    /**
     * Keywords match whole words only: as a substring "nea" filed "lunch near office"
     * under Utilities, "bus" filed "business lunch" under Transport and "rent" caught
     * "current account". A keyword may still span words ("bhat bhateni"), and the edges
     * are any non-letter-or-digit, so "MOS/eSewa/khaja" still finds "khaja".
     */
    private val index: List<Pair<Regex, Long>> = categories
        .flatMap { category -> category.keywordList.map { it to category.id } }
        // Longest first, so the most specific keyword is tested before a shorter one
        // that happens to be contained in the same text.
        .sortedByDescending { it.first.length }
        .map { (keyword, id) ->
            // A trailing "s"/"es" still counts, so "momos" is Dining. \p{M} keeps a
            // Devanagari vowel sign from reading as a word edge.
            Regex("""(?<![\p{L}\p{M}\p{N}])${Regex.escape(keyword)}(?:e?s)?(?![\p{L}\p{M}\p{N}])""") to id
        }

    fun categoryIdFor(txn: Txn): Long? {
        if (index.isEmpty()) return null
        val haystack = listOfNotNull(txn.merchant, txn.remark)
            .joinToString(" ")
            .lowercase()
        if (haystack.isBlank()) return null
        return index.firstOrNull { (keyword, _) -> keyword.containsMatchIn(haystack) }?.second
    }

    /**
     * Fills in the category on rows that do not have one, leaving the rest untouched.
     *
     * A row that already carries a category was either categorised by an earlier run with
     * the same rules, or set by hand — and a hand-set category is exactly the thing that
     * must not be overwritten by a keyword guess.
     */
    fun apply(txns: List<Txn>): List<Txn> = txns.map { txn ->
        if (txn.categoryId != null) txn else txn.copy(categoryId = categoryIdFor(txn))
    }
}
