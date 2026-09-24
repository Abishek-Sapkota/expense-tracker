package com.abi.expensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A parsing rule, stored as data rather than compiled in, so new bank formats are
 * added at runtime from the review queue without shipping a new build.
 *
 * [bodyPattern] uses named groups. All optional except `amount`:
 *   amount, acct, merchant, remark, ref, balance
 */
@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val senderPattern: String,
    val bodyPattern: String,
    val direction: Direction,
    val enabled: Boolean = true,
    /**
     * The template the user typed, when this rule came from one. Kept alongside the
     * compiled pattern so the rule can be shown and edited as they wrote it rather than
     * as the regex it became; null for the seeded generic rules.
     */
    val template: String? = null,
    /** Lower runs first. Specific rules should sit above generic fallbacks. */
    val priority: Int = 100,
    val builtIn: Boolean = false
)
