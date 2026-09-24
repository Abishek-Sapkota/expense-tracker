package com.abi.expensetracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.abi.expensetracker.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Saves what the user typed into the "what was it for?" notification.
 *
 * The reply is written as the transaction's remark and the row is marked hand-edited, so a
 * later reparse keeps it: the whole point of answering was that the message did not say.
 */
class RemarkReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val txnId = intent.getStringExtra(RemarkPrompt.EXTRA_TXN_ID) ?: return
        val reply = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(RemarkPrompt.KEY_REMARK)
            ?.toString()
            ?.trim()

        // Dismiss either way. An empty reply is still an answer — "I do not want to say"
        // — and leaving the question in the shade would make it nagging rather than handy.
        NotificationManagerCompat.from(context)
            .cancel(RemarkPrompt.notificationId(txnId))
        if (reply.isNullOrBlank()) return

        val pending = goAsync()
        val repository = ServiceLocator.repository(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.setRemarkFromPrompt(txnId, reply)
            } finally {
                pending.finish()
            }
        }
    }
}
