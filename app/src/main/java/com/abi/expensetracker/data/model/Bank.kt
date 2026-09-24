package com.abi.expensetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A bank or service the user linked senders to.
 *
 * [icon] is what the user picked, or null for none: either a single emoji, or a reference
 * to an installed app's launcher icon in the "app:<package>" form
 * [com.abi.expensetracker.data.AppIconRef] writes. Text either way rather than a bundled
 * glyph set or a stored bitmap: it needs no assets, survives a backup as plain text, and
 * the user picks whatever reads as "their" bank — usually that bank's own app icon.
 */
@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String? = null
)

/**
 * Maps a normalised SMS sender to a bank.
 *
 * Keyed on the normalised sender rather than the raw one: the same bank reaches the same
 * phone as "AX-NABIL", "VM-NABIL" or "BP-NABIL-S" depending on carrier routing and
 * routing, and mapping each variant separately would mean re-linking the same bank
 * repeatedly. See [com.abi.expensetracker.data.SenderNormalizer].
 */
@Entity(tableName = "sender_links")
data class SenderLink(
    @PrimaryKey val senderKey: String,
    val bankId: Long
)

/**
 * An app whose notifications belong to a bank: the bank's own app, a wallet, or Gmail for
 * a bank that emails its alerts.
 *
 * One app can serve several banks — Sanima and NMB both write through Gmail — so this is
 * a pair table rather than a column. When more than one bank claims an app, the message
 * decides: the bank whose name it mentions wins. See
 * [com.abi.expensetracker.data.BankResolver].
 */
@Entity(tableName = "bank_apps", primaryKeys = ["packageName", "bankId"], indices = [Index("bankId")])
data class BankApp(
    val packageName: String,
    val bankId: Long
)
