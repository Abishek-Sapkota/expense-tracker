package com.abi.expensetracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Source
import com.abi.expensetracker.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Catches messages as they arrive, so the app stays current between backfills. */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        // A long SMS arrives split across several PDUs. Joining the bodies rebuilds the
        // original text; parsing a fragment would read a truncated amount.
        val sender = parts.first().originatingAddress ?: return
        val sentAt = parts.first().timestampMillis
        val body = parts.joinToString("") { it.messageBody ?: "" }
        if (body.isBlank()) return

        val message = RawMessage(
            id = RawMessage.idFor(sender, body, sentAt),
            sender = sender,
            body = body,
            sentAt = sentAt,
            source = Source.SMS,
            importedAt = System.currentTimeMillis()
        )

        val pending = goAsync()
        val repository = ServiceLocator.repository(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.ingest(listOf(message))
            } finally {
                pending.finish()
            }
        }
    }
}
