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
 * [icon] is an emoji for the same reason bank icons are: nothing ships as an asset, and a
 * backup carries the choice as plain text.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "",
    val keywords: String = ""
) {
    /** Lowercased, blank entries dropped. */
    val keywordList: List<String>
        get() = keywords.split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
}
