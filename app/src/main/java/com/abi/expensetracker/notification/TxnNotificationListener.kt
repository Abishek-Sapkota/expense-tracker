package com.abi.expensetracker.notification

import android.app.Notification
import android.content.ComponentName
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
        val message = toMessage(sbn) ?: return
        val repository = ServiceLocator.repository(applicationContext)
        scope.launch {
            // Only apps the user picked in Accounts. Checked after the cheap text filter,
            // so the settings read happens for money-looking notifications alone.
            if (!repository.readsNotificationsFrom(sbn.packageName)) return@launch
            repository.ingest(listOf(message))
        }
    }

    /**
     * Books what is still in the shade when the system (re)binds the listener.
     *
     * While unbound the listener hears nothing, and a bank alert posted in that gap would
     * otherwise be lost for good even though it is sitting right there. Anything already
     * stored is dropped by the ingest duplicate guard before parsing, so a rebind neither
     * books a row twice nor asks about it again.
     */
    override fun onListenerConnected() {
        val active = try {
            activeNotifications
        } catch (e: SecurityException) {
            // Access revoked between the bind and this call.
            return
        } ?: return
        val repository = ServiceLocator.repository(applicationContext)
        val candidates = active.mapNotNull { sbn -> toMessage(sbn)?.let { sbn.packageName to it } }
        if (candidates.isEmpty()) return
        scope.launch {
            val messages = candidates
                .filter { (pkg, _) -> repository.readsNotificationsFrom(pkg) }
                .map { it.second }
            if (messages.isNotEmpty()) repository.ingest(messages)
        }
    }

    /**
     * The system unbinds a listener when its process dies or the app is updated, and does
     * not always bind it again: access stays granted in settings while nothing is heard.
     * Asking for the rebind here covers the case where the system tells us.
     */
    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, TxnNotificationListener::class.java))
    }

    /** The notification as a [RawMessage], or null when it is not worth keeping. */
    private fun toMessage(sbn: StatusBarNotification): RawMessage? {
        val notification = sbn.notification ?: return null

        // Our own notifications would be a feedback loop, and an ongoing one (a download,
        // a media player) is a live status rather than an event that happened once.
        if (sbn.packageName == packageName) return null
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return null
        // The group summary repeats what the individual notifications already said.
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null

        val extras = notification.extras ?: return null
        val body = NotificationIngest.composeBody(
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        )
        if (!NotificationIngest.looksFinancial(body)) return null

        return RawMessage(
            // Keyed on the post time as well as the text: a wallet sends the same sentence
            // for two same-price payments days apart, and a content-only id made the second
            // one the first. A re-post of one notification keeps its post time when the
            // listener reconnects, and a quick update is caught by the ingest guard.
            id = RawMessage.idFor(sbn.packageName, body, sbn.postTime),
            // The package name. Settings folds senders onto banks, so the user names it
            // once there and the ledger shows the bank from then on.
            sender = sbn.packageName,
            body = body,
            sentAt = sbn.postTime,
            source = Source.NOTIFICATION,
            importedAt = System.currentTimeMillis()
        )
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
         * Asks the system to bind the listener if access is granted.
         *
         * Called on every process start: after an update or a process kill the system can
         * leave a granted listener unbound, and then no notification reaches the app until
         * the user toggles access off and on. A request for a listener that is already
         * bound is ignored by the system, so this costs one binder call and no scan.
         */
        fun ensureBound(context: Context) {
            if (!isEnabled(context)) return
            try {
                requestRebind(ComponentName(context, TxnNotificationListener::class.java))
            } catch (e: SecurityException) {
                // Access revoked in the meantime; nothing to bind.
            }
        }

        /**
         * The system screen where the access is granted. There is no runtime-permission
         * dialog for notification access; the user has to toggle it themselves.
         */
        fun settingsIntent(): Intent =
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
