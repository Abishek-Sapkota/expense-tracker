package com.abi.expensetracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.abi.expensetracker.MainActivity
import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.model.Txn

/**
 * Asks, in a notification, what a transaction was for.
 *
 * A bank message names who was paid and almost never what for: "NIC ASIA" tells you
 * nothing in a month's time, "Khaja" tells you everything. The moment to ask is right
 * after the payment, while the user still remembers — an hour later it is archaeology, and
 * a screen that asks about forty old rows at once never gets opened twice.
 *
 * The answer is typed straight into the notification shade. Opening the app to fill in one
 * word is the friction that makes people stop bothering.
 */
object RemarkPrompt {

    private const val CHANNEL_ID = "remark_prompts"
    const val EXTRA_TXN_ID = "txnId"
    const val KEY_REMARK = "remark"

    /**
     * Created once at startup rather than lazily: a channel made at post time is a channel
     * the user cannot find in system settings until the first notification has already
     * arrived.
     */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "What was it for?",
            // Default, not high: this is a question that can wait, not an alert. It
            // appears in the shade without taking over the screen.
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Asks what a new transaction was spent on, so the ledger can name it."
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    /**
     * One notification per transaction, keyed on the transaction id so a reply lands on
     * the right row and a re-post replaces rather than stacks.
     */
    fun ask(context: Context, txn: Txn, sourceName: String?) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val remoteInput = RemoteInput.Builder(KEY_REMARK)
            .setLabel("e.g. Khaja, Auto fare")
            .build()

        val replyIntent = Intent(context, RemarkReplyReceiver::class.java)
            .putExtra(EXTRA_TXN_ID, txn.id)
        val replyPending = PendingIntent.getBroadcast(
            context,
            txn.id.hashCode(),
            replyIntent,
            // MUTABLE is required: the system writes the typed text into this intent.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val openApp = PendingIntent.getActivity(
            context,
            txn.id.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val who = txn.merchant ?: sourceName ?: "a new transaction"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("${Money.format(txn.amountMinor)} at $who")
            .setContentText("What was it for?")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_edit,
                    "What was it for?",
                    replyPending
                ).addRemoteInput(remoteInput).build()
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            manager.notify(notificationId(txn.id), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check above and here. Nothing to do:
            // the transaction is already stored, only the question is lost.
        }
    }

    fun notificationId(txnId: String): Int = txnId.hashCode()
}
