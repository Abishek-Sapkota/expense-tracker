package com.abi.expensetracker.notification

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Source
import com.abi.expensetracker.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Books transactions from bank and wallet notifications.
 *
 * Many services in Nepal have stopped paying for SMS and now only push a notification, so
 * an SMS-only ledger quietly misses them. A notification carries the same sentence the SMS
 * did, so it runs through the same rules: this class only turns a posted notification into
 * a [RawMessage] and hands it to the repository, which stores and parses it exactly as it
 * does an SMS.
 *
 * Nothing leaves the device, and the strict filter in [NotificationIngest] means the great
 * majority of notifications are dropped without ever being written to the database.
 */
class TxnNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return

        // Our own notifications would be a feedback loop, and an ongoing one (a download,
        // a media player) is a live status rather than an event that happened once.
        if (sbn.packageName == packageName) return
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        // The group summary repeats what the individual notifications already said.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras ?: return
        val body = NotificationIngest.composeBody(
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        )
        if (!NotificationIngest.looksFinancial(body)) return

        val message = RawMessage(
            // Content-addressed rather than keyed on the post time: an app that updates or
            // re-posts the same notification would otherwise book the transaction twice.
            id = RawMessage.idForContent(sbn.packageName, body),
            // The package name. Settings folds senders onto banks, so the user names it
            // once there and the ledger shows the bank from then on.
            sender = sbn.packageName,
            body = body,
            sentAt = sbn.postTime,
            source = Source.NOTIFICATION,
            importedAt = System.currentTimeMillis()
        )

        val repository = ServiceLocator.repository(applicationContext)
        scope.launch { repository.ingest(listOf(message)) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {

        /** Whether the user has granted this app notification access in system settings. */
        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)

        /**
         * The system screen where the access is granted. There is no runtime-permission
         * dialog for notification access; the user has to toggle it themselves.
         */
        fun settingsIntent(): Intent =
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
