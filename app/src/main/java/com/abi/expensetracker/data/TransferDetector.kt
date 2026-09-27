package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Txn

/**
 * Whether a debit moved money into one of the user's own wallets rather than spending it.
 *
 * Loading your own eSewa from the bank is not spending: the money is still yours, and
 * whatever it later buys is booked again from the wallet's side. Loading a merchant's or a
 * friend's eSewa is spending. The difference is whose wallet ID the bank printed, and
 * banks that print it at all put it in the text ("Esewa Wallet Load for 98XXXXXXXX",
 * "MOS/eSewa/98XXXXXXXX/..."), so a list of the user's own IDs, set on each wallet account,
 * decides it without a template per bank.
 *
 * Pure, so the rule is unit-tested without a database.
 */
object TransferDetector {

    /** The IDs as the user typed them: digits only, short fragments dropped. */
    fun parseIds(text: String?): List<String> =
        text.orEmpty().split(',', ' ', ';', '\n')
            .map { part -> part.filter(Char::isDigit) }
            .filter { it.length >= 6 }
            .distinct()

    fun isTransfer(txn: Txn, body: String?, walletIds: Collection<String>): Boolean {
        if (txn.direction != Direction.DEBIT || txn.isManual || body == null) return false
        // Whole numbers only: 9866550884 must not match inside 19866550884 or a longer ref.
        return walletIds.any { id -> Regex("""(?<!\d)$id(?!\d)""").containsMatchIn(body) }
    }
}
