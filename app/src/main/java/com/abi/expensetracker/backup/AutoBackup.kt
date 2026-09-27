package com.abi.expensetracker.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * A full backup, written weekly into a folder the user picked once.
 *
 * The app cannot reach the network and keeps Android's own backup off, so until now the
 * only copy of years of history was whatever the user last remembered to export. Writing
 * through the document tree the user granted means no storage permission is needed and
 * the file lands where a sync app, a PC over USB or a Drive folder can see it.
 */
object AutoBackup {

    private const val WORK_NAME = "auto-backup"
    private const val PREFIX = "expenses-auto-"

    /** Enough history to step back past a bad week, few enough not to fill the folder. */
    private const val KEEP = 4

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<Worker>(7, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Writes one backup now and prunes the oldest beyond [KEEP]. Records the outcome so
     * Settings can say when the last one ran, or why it did not.
     */
    suspend fun runNow(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val settings = SettingsStore(context)
        val folder = settings.autoBackupFolder.first()?.let(Uri::parse)
            ?: return@withContext Result.failure(IllegalStateException("No backup folder chosen"))
        val outcome = runCatching {
            val resolver = context.contentResolver
            val parent = DocumentsContract.buildDocumentUriUsingTree(
                folder, DocumentsContract.getTreeDocumentId(folder)
            )
            val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
            val name = "$PREFIX$stamp.json"
            val file = DocumentsContract.createDocument(resolver, parent, "application/json", name)
                ?: error("The folder refused a new file")
            ServiceLocator.backupManager(context).exportTo(file)
            prune(context, folder)
            name
        }
        settings.recordAutoBackup(
            at = if (outcome.isSuccess) System.currentTimeMillis() else null,
            error = outcome.exceptionOrNull()?.let { it.message ?: it::class.simpleName }
        )
        outcome
    }

    /** Deletes this app's automatic backups beyond the newest [KEEP]; nothing else. */
    private fun prune(context: Context, folder: Uri) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            folder, DocumentsContract.getTreeDocumentId(folder)
        )
        val ours = mutableListOf<Pair<String, String>>()
        context.contentResolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(1) ?: continue
                if (name.startsWith(PREFIX) && name.endsWith(".json")) ours += c.getString(0) to name
            }
        }
        // The name carries the date, so sorting by it is sorting by age.
        ours.sortedByDescending { it.second }.drop(KEEP).forEach { (id, _) ->
            DocumentsContract.deleteDocument(
                context.contentResolver, DocumentsContract.buildDocumentUriUsingTree(folder, id)
            )
        }
    }

    /** A readable name for the chosen folder: "Download" rather than a document-tree URI. */
    fun folderLabel(uri: String): String =
        Uri.parse(uri).let { runCatching { DocumentsContract.getTreeDocumentId(it) }.getOrNull() }
            ?.substringAfter(':')?.ifBlank { "Internal storage" } ?: uri

    class Worker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result =
            // Retried later rather than failed for good: the folder may be on an SD card
            // that is out, or a sync app's folder that is briefly unavailable.
            if (runNow(applicationContext).isSuccess) Result.success() else Result.retry()
    }
}
