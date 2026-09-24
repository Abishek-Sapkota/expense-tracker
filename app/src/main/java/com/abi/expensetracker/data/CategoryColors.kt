package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Category

/**
 * The colours a category can wear in Trends: distinct from each other, readable on white
 * and on charcoal, and kept clear of the pure red and green that mean money out and in.
 */
object CategoryColors {

    val PALETTE: List<Int> = listOf(
        0xFF4F6BED.toInt(), // blue
        0xFFE8853D.toInt(), // orange
        0xFF8E5BD6.toInt(), // violet
        0xFF1FA2A8.toInt(), // teal
        0xFFD6457A.toInt(), // pink
        0xFFC9A227.toInt(), // mustard
        0xFF5A8F3E.toInt(), // olive
        0xFF8C6A4F.toInt(), // brown
        0xFF3E7CB1.toInt(), // steel
        0xFFB65FC4.toInt(), // orchid
        0xFF2E9E6E.toInt(), // jade
        0xFF6B7280.toInt()  // slate
    )

    /** Uncategorised spending: a neutral grey, so it never reads as a category of its own. */
    const val UNCATEGORISED: Int = 0xFFA1A1AA.toInt()

    /** The colour picked for [category], or a stable one from the palette by id. */
    fun of(category: Category?): Int = when {
        category == null -> UNCATEGORISED
        category.color != null -> category.color
        else -> PALETTE[(category.id % PALETTE.size).toInt()]
    }
}
