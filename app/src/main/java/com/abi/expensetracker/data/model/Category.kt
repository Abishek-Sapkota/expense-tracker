package com.abi.expensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * What a transaction was for, as a bucket the trends screen can sum.
 *
 * [keywords] is what makes categorising automatic rather than a chore: a comma-separated
 * list matched against the merchant and remark. Bank messages name the same handful of
 * merchants over and over, so a short keyword list categorises most of a ledger without
 * the user touching anything.
 *
 * [icon] is no longer shown (ledger rows wear the account's icon); the column stays so old
 * backups restore. [color] is the ARGB the category wears in Trends; null means a stable
 * default from [com.abi.expensetracker.data.CategoryColors].
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "",
    val keywords: String = "",
    val color: Int? = null
) {
    /** Lowercased, blank entries dropped. */
    val keywordList: List<String>
        get() = keywords.split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
}
