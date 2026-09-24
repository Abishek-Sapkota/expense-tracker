package com.abi.expensetracker.sms

import android.content.Context
import android.provider.Telephony
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Source

/**
 * Reads the phone's SMS inbox. This is the history backfill — the inbox holds only what
 * the OS has retained, so whatever it returns today may be gone in a year. Every row it
 * yields is copied into the app's own storage and kept from then on.
 */
object SmsInboxReader {

    /**
     * @param since epoch millis watermark, exclusive. 0 reads everything available.
     * @param onBatch called per chunk so a multi-year inbox is never held in memory at once.
     */
    suspend fun read(
        context: Context,
        since: Long = 0L,
        batchSize: Int = 500,
        onBatch: suspend (List<RawMessage>) -> Unit
    ): Int {
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.Sms.DATE} > ?",
            arrayOf(since.toString()),
            "${Telephony.Sms.DATE} ASC"
        ) ?: return 0

        var total = 0
        cursor.use { c ->
            val iAddress = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

            val now = System.currentTimeMillis()
            val batch = ArrayList<RawMessage>(batchSize)

            while (c.moveToNext()) {
                val sender = c.getString(iAddress) ?: continue
                val body = c.getString(iBody) ?: continue
                val sentAt = c.getLong(iDate)

                batch += RawMessage(
                    id = RawMessage.idFor(sender, body, sentAt),
                    sender = sender,
                    body = body,
                    sentAt = sentAt,
                    source = Source.SMS,
                    importedAt = now
                )

                if (batch.size >= batchSize) {
                    onBatch(batch.toList())
                    total += batch.size
                    batch.clear()
                }
            }

            if (batch.isNotEmpty()) {
                onBatch(batch.toList())
                total += batch.size
            }
        }
        return total
    }
}
