package com.abi.expensetracker.backup

import android.content.Context
import android.net.Uri
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.data.db.AppDatabase
import com.abi.expensetracker.data.model.Bank
import com.abi.expensetracker.data.model.BankApp
import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Rule
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.LoanKind
import com.abi.expensetracker.data.model.MessageFlag
import com.abi.expensetracker.data.model.SenderLink
import com.abi.expensetracker.data.model.Split
import com.abi.expensetracker.data.model.Source
import com.abi.expensetracker.data.model.Txn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * Whole-database export and import as one JSON file.
 *
 * Streamed in both directions with [JsonWriter] / [JsonReader]: several years of SMS is
 * tens of megabytes, and building that as a single in-memory string is a reliable way to
 * get an OutOfMemoryError on a mid-range phone — at the exact moment the user is trying
 * to rescue their data.
 */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsStore
) {

    // ---------- Export ----------

    suspend fun exportTo(uri: Uri): ExportResult = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri)
            ?: error("Could not open $uri for writing")
        stream.use { write(it) }
    }

    /** Written before a destructive import so there is always a way back. */
    suspend fun exportToCache(fileName: String): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, fileName)
        file.outputStream().use { write(it) }
        file
    }

    private suspend fun write(out: OutputStream): ExportResult {
        val rawMessages = db.rawMessageDao().all()
        val transactions = db.txnDao().all()
        val rules = db.ruleDao().all()
        val categories = db.categoryDao().all()
        val banks = db.bankDao().all()
        val senderLinks = db.senderLinkDao().all()
        val bankApps = db.bankAppDao().all()
        val messageFlags = db.messageFlagDao().all()
        val loans = db.loanDao().all()
        val splits = db.splitDao().all()
        val lastSynced = settings.lastSyncedSmsDateOnce()

        JsonWriter(OutputStreamWriter(out, Charsets.UTF_8)).use { w ->
            w.setIndent("  ")
            w.beginObject()
            w.name(BackupSchema.FIELD_SCHEMA_VERSION).value(BackupSchema.CURRENT_VERSION)
            w.name(BackupSchema.FIELD_EXPORTED_AT).value(System.currentTimeMillis())

            w.name(BackupSchema.FIELD_RAW_MESSAGES).beginArray()
            rawMessages.forEach { m ->
                w.beginObject()
                w.name("id").value(m.id)
                w.name("sender").value(m.sender)
                w.name("body").value(m.body)
                w.name("sentAt").value(m.sentAt)
                w.name("source").value(m.source.name)
                w.name("importedAt").value(m.importedAt)
                w.endObject()
            }
            w.endArray()

            w.name(BackupSchema.FIELD_TRANSACTIONS).beginArray()
            transactions.forEach { t ->
                w.beginObject()
                w.name("id").value(t.id)
                w.name("rawId").valueOrNull(t.rawId)
                w.name("amountMinor").value(t.amountMinor)
                w.name("direction").value(t.direction.name)
                w.name("accountTail").valueOrNull(t.accountTail)
                w.name("merchant").valueOrNull(t.merchant)
                w.name("remark").valueOrNull(t.remark)
                w.name("refNumber").valueOrNull(t.refNumber)
                w.name("balanceMinor").valueOrNull(t.balanceMinor)
                w.name("occurredAt").value(t.occurredAt)
                w.name("categoryId").valueOrNull(t.categoryId)
                w.name("needsReview").value(t.needsReview)
                w.name("ruleId").valueOrNull(t.ruleId)
                w.name("userEdited").value(t.userEdited)
                w.endObject()
            }
            w.endArray()

            w.name(BackupSchema.FIELD_RULES).beginArray()
            rules.forEach { r ->
                w.beginObject()
                w.name("id").value(r.id)
                w.name("name").value(r.name)
                w.name("senderPattern").value(r.senderPattern)
                w.name("bodyPattern").value(r.bodyPattern)
                w.name("direction").value(r.direction.name)
                w.name("enabled").value(r.enabled)
                w.name("priority").value(r.priority.toLong())
                w.name("builtIn").value(r.builtIn)
                w.name("template").valueOrNull(r.template)
                w.endObject()
            }
            w.endArray()

            w.name(BackupSchema.FIELD_CATEGORIES).beginArray()
            categories.forEach { c ->
                w.beginObject()
                w.name("id").value(c.id)
                w.name("name").value(c.name)
                // Both added in schema 6. Before that a restore silently returned every
                // category to no icon and no keywords, which undid every keyword the user
                // had typed.
                w.name("icon").value(c.icon)
                w.name("keywords").value(c.keywords)
                w.endObject()
            }
            w.endArray()

            w.name(BackupSchema.FIELD_BANKS).beginArray()
            banks.forEach { b ->
                w.beginObject()
                w.name("id").value(b.id)
                w.name("name").value(b.name)
                w.name("icon").valueOrNull(b.icon)
                w.endObject()
            }
            w.endArray()

            w.name(BackupSchema.FIELD_BANK_APPS).beginArray()
            bankApps.forEach { a ->
                w.beginObject()
                w.name("packageName").value(a.packageName)
                w.name("bankId").value(a.bankId)
                w.endObject()
            }
            w.endArray()

            w.name(BackupSchema.FIELD_SENDER_LINKS).beginArray()
            senderLinks.forEach { l ->
                w.beginObject()
                w.name("senderKey").value(l.senderKey)
                w.name("bankId").value(l.bankId)
                w.endObject()
            }
            w.endArray()

            w.name(BackupSchema.FIELD_MESSAGE_FLAGS).beginArray()
            messageFlags.forEach { f ->
                w.beginObject()
                w.name("rawId").value(f.rawId)
                w.name("notDuplicate").value(f.notDuplicate)
                w.name("deleted").value(f.deleted)
                w.endObject()
            }
            w.endArray()

            w.name(BackupSchema.FIELD_SPLITS).beginArray()
            splits.forEach { s ->
                w.beginObject()
                w.name("id").value(s.id)
                w.name("txnId").value(s.txnId)
                w.name("title").value(s.title)
                w.name("totalMinor").value(s.totalMinor)
                w.name("myShareMinor").value(s.myShareMinor)
                w.name("createdAt").value(s.createdAt)
                w.endObject()
            }
            w.endArray()

            w.name(BackupSchema.FIELD_LOANS).beginArray()
            loans.forEach { l ->
                w.beginObject()
                w.name("person").value(l.person)
                w.name("kind").value(l.kind.name)
                w.name("amountMinor").value(l.amountMinor)
                w.name("occurredAt").value(l.occurredAt)
                w.name("note").value(l.note)
                w.name("txnId").value(l.txnId)
                w.name("splitId").value(l.splitId)
                w.endObject()
            }
            w.endArray()

            w.name(BackupSchema.FIELD_SETTINGS).beginObject()
            w.name("lastSyncedSmsDate").value(lastSynced)
            w.endObject()

            w.endObject()
            w.flush()
        }

        return ExportResult(
            rawMessages = rawMessages.size,
            transactions = transactions.size,
            rules = rules.size,
            categories = categories.size,
            banks = banks.size,
            senderLinks = senderLinks.size
        )
    }

    // ---------- Import ----------

    suspend fun importFrom(uri: Uri, mode: ImportMode): ImportResult =
        withContext(Dispatchers.IO) {
            // Read and validate the header on its own pass first. REPLACE wipes the
            // database, and wiping it only to then discover the file is unreadable would
            // destroy the user's data on the strength of a file we never verified.
            val declared = context.contentResolver.openInputStream(uri)
                ?.use { peekSchemaVersion(it) }
                ?: error("Could not open $uri for reading")

            require(declared in 1..BackupSchema.CURRENT_VERSION) {
                "Unsupported backup schemaVersion $declared; this build understands up to " +
                    "${BackupSchema.CURRENT_VERSION}"
            }

            val stream = context.contentResolver.openInputStream(uri)
                ?: error("Could not open $uri for reading")
            stream.use { read(it, mode, declared) }
        }

    /** Scans only until schemaVersion is found, skipping the bulk arrays. */
    private fun peekSchemaVersion(input: InputStream): Int {
        JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { r ->
            r.beginObject()
            while (r.hasNext()) {
                if (r.nextName() == BackupSchema.FIELD_SCHEMA_VERSION) return r.nextInt()
                r.skipValue()
            }
        }
        return 0
    }

    private suspend fun read(
        input: InputStream,
        mode: ImportMode,
        schemaVersion: Int
    ): ImportResult {
        var rawCount = 0
        var txnCount = 0
        var ruleCount = 0
        var categoryCount = 0
        var bankCount = 0
        var senderLinkCount = 0
        var lastSynced: Long? = null

        if (mode == ImportMode.REPLACE) {
            // Transactions cascade from raw messages, but clearing explicitly keeps the
            // intent obvious and does not depend on FK behaviour.
            db.txnDao().deleteAll()
            db.txnCopyDao().deleteAll()
            db.messageFlagDao().deleteAll()
            db.loanDao().deleteAll()
            db.splitDao().deleteAll()
            db.rawMessageDao().deleteAll()
            db.ruleDao().deleteAll()
            db.categoryDao().deleteAll()
            db.senderLinkDao().deleteAll()
            db.bankAppDao().deleteAll()
            db.bankDao().deleteAll()
        }

        JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { r ->
            // File split ids to the ids they got here; loans are read after and refer to them.
            var splitIds = emptyMap<Long, Long>()
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    BackupSchema.FIELD_SCHEMA_VERSION -> r.skipValue() // already validated
                    BackupSchema.FIELD_RAW_MESSAGES -> rawCount = readRawMessages(r)
                    BackupSchema.FIELD_TRANSACTIONS -> txnCount = readTransactions(r)
                    BackupSchema.FIELD_RULES -> ruleCount = readRules(r)
                    BackupSchema.FIELD_CATEGORIES -> categoryCount = readCategories(r)
                    BackupSchema.FIELD_BANKS -> bankCount = readBanks(r)
                    BackupSchema.FIELD_SENDER_LINKS -> senderLinkCount = readSenderLinks(r)
                    BackupSchema.FIELD_BANK_APPS -> readBankApps(r)
                    BackupSchema.FIELD_MESSAGE_FLAGS -> readMessageFlags(r)
                    BackupSchema.FIELD_SPLITS -> splitIds = readSplits(r)
                    BackupSchema.FIELD_LOANS -> readLoans(r, mode, splitIds)
                    BackupSchema.FIELD_SETTINGS -> lastSynced = readSettings(r)
                    else -> {
                        // Unknown field: a newer app version wrote it. Skip rather than
                        // fail, so a backup stays importable by an older build.
                        r.skipValue()
                    }
                }
            }
            r.endObject()
        }

        lastSynced?.let { settings.setLastSyncedSmsDate(it) }

        return ImportResult(
            schemaVersion, rawCount, txnCount, ruleCount, categoryCount, bankCount, senderLinkCount
        )
    }

    private suspend fun readRawMessages(r: JsonReader): Int {
        var count = 0
        val batch = ArrayList<RawMessage>(BATCH)
        r.beginArray()
        while (r.hasNext()) {
            var id = ""; var sender = ""; var body = ""
            var sentAt = 0L; var source = Source.SMS; var importedAt = 0L
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "id" -> id = r.nextString()
                    "sender" -> sender = r.nextString()
                    "body" -> body = r.nextString()
                    "sentAt" -> sentAt = r.nextLong()
                    "source" -> source = runCatching { Source.valueOf(r.nextString()) }
                        .getOrDefault(Source.SMS)
                    "importedAt" -> importedAt = r.nextLong()
                    else -> r.skipValue()
                }
            }
            r.endObject()
            if (id.isNotEmpty()) {
                batch += RawMessage(id, sender, body, sentAt, source, importedAt)
                count++
            }
            if (batch.size >= BATCH) {
                db.rawMessageDao().insertAll(batch.toList()); batch.clear()
            }
        }
        r.endArray()
        if (batch.isNotEmpty()) db.rawMessageDao().insertAll(batch.toList())
        return count
    }

    private suspend fun readTransactions(r: JsonReader): Int {
        var count = 0
        val batch = ArrayList<Txn>(BATCH)
        r.beginArray()
        while (r.hasNext()) {
            var id = ""; var rawId: String? = null; var amountMinor = 0L
            var direction = Direction.DEBIT
            var accountTail: String? = null; var merchant: String? = null
            var remark: String? = null
            var refNumber: String? = null; var balanceMinor: Long? = null
            var occurredAt = 0L; var categoryId: Long? = null
            var needsReview = false; var ruleId: Long? = null
            var userEdited = false

            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "id" -> id = r.nextString()
                    "rawId" -> rawId = r.nextStringOrNull()
                    "amountMinor" -> amountMinor = r.nextLong()
                    "direction" -> direction = runCatching { Direction.valueOf(r.nextString()) }
                        .getOrDefault(Direction.DEBIT)
                    "accountTail" -> accountTail = r.nextStringOrNull()
                    "merchant" -> merchant = r.nextStringOrNull()
                    // Absent in schema 4 and older, which had no remark.
                    "remark" -> remark = r.nextStringOrNull()
                    "refNumber" -> refNumber = r.nextStringOrNull()
                    "balanceMinor" -> balanceMinor = r.nextLongOrNull()
                    "occurredAt" -> occurredAt = r.nextLong()
                    "categoryId" -> categoryId = r.nextLongOrNull()
                    "needsReview" -> needsReview = r.nextBoolean()
                    "ruleId" -> ruleId = r.nextLongOrNull()
                    // Absent in schema 3 and older, where nothing could be edited.
                    "userEdited" -> userEdited = r.nextBoolean()
                    else -> r.skipValue()
                }
            }
            r.endObject()

            // rawId may legitimately be null: that is a manually entered expense.
            if (id.isNotEmpty()) {
                // Named, not positional: these fields are nearly all nullable strings,
                // so a new column in the middle would otherwise shift values silently.
                batch += Txn(
                    id = id,
                    rawId = rawId,
                    amountMinor = amountMinor,
                    direction = direction,
                    accountTail = accountTail,
                    merchant = merchant,
                    remark = remark,
                    refNumber = refNumber,
                    balanceMinor = balanceMinor,
                    occurredAt = occurredAt,
                    categoryId = categoryId,
                    needsReview = needsReview,
                    ruleId = ruleId,
                    userEdited = userEdited
                )
                count++
            }
            if (batch.size >= BATCH) {
                db.txnDao().insertAll(batch.toList()); batch.clear()
            }
        }
        r.endArray()
        if (batch.isNotEmpty()) db.txnDao().insertAll(batch.toList())
        return count
    }

    private suspend fun readRules(r: JsonReader): Int {
        val rules = ArrayList<Rule>()
        r.beginArray()
        while (r.hasNext()) {
            var id = 0L; var name = ""; var senderPattern = ".*"; var bodyPattern = ""
            var direction = Direction.DEBIT; var enabled = true
            var priority = 100; var builtIn = false; var template: String? = null
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "id" -> id = r.nextLong()
                    "name" -> name = r.nextString()
                    "senderPattern" -> senderPattern = r.nextString()
                    "bodyPattern" -> bodyPattern = r.nextString()
                    "direction" -> direction = runCatching { Direction.valueOf(r.nextString()) }
                        .getOrDefault(Direction.DEBIT)
                    "enabled" -> enabled = r.nextBoolean()
                    "priority" -> priority = r.nextInt()
                    "builtIn" -> builtIn = r.nextBoolean()
                    "template" -> template = r.nextStringOrNull()
                    else -> r.skipValue()
                }
            }
            r.endObject()
            if (bodyPattern.isNotEmpty()) {
                rules += Rule(
                    id, name, senderPattern, bodyPattern, direction, enabled, template,
                    priority, builtIn
                )
            }
        }
        r.endArray()
        if (rules.isNotEmpty()) db.ruleDao().insertAll(rules)
        return rules.size
    }

    private suspend fun readCategories(r: JsonReader): Int {
        val categories = ArrayList<Category>()
        r.beginArray()
        while (r.hasNext()) {
            var id = 0L; var name = ""; var icon = ""; var keywords = ""
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "id" -> id = r.nextLong()
                    "name" -> name = r.nextString()
                    // Absent in schema 5 and older, where a category carried neither.
                    "icon" -> icon = r.nextStringOrNull().orEmpty()
                    "keywords" -> keywords = r.nextStringOrNull().orEmpty()
                    else -> r.skipValue()
                }
            }
            r.endObject()
            if (name.isNotEmpty()) categories += Category(id, name, icon, keywords)
        }
        r.endArray()
        if (categories.isNotEmpty()) db.categoryDao().insertAll(categories)
        return categories.size
    }

    private suspend fun readBanks(r: JsonReader): Int {
        val banks = ArrayList<Bank>()
        r.beginArray()
        while (r.hasNext()) {
            var id = 0L; var name = ""; var icon: String? = null
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "id" -> id = r.nextLong()
                    "name" -> name = r.nextString()
                    // Absent in schema 3 and older, where a bank had no icon.
                    "icon" -> icon = r.nextStringOrNull()
                    else -> r.skipValue()
                }
            }
            r.endObject()
            if (name.isNotEmpty()) banks += Bank(id, name, icon)
        }
        r.endArray()
        if (banks.isNotEmpty()) db.bankDao().insertAll(banks)
        return banks.size
    }

    private suspend fun readBankApps(r: JsonReader) {
        val apps = ArrayList<BankApp>()
        r.beginArray()
        while (r.hasNext()) {
            var pkg = ""; var bankId = 0L
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "packageName" -> pkg = r.nextString()
                    "bankId" -> bankId = r.nextLong()
                    else -> r.skipValue()
                }
            }
            r.endObject()
            if (pkg.isNotEmpty()) apps += BankApp(pkg, bankId)
        }
        r.endArray()
        if (apps.isNotEmpty()) db.bankAppDao().insertAll(apps)
    }

    private suspend fun readSenderLinks(r: JsonReader): Int {
        val links = ArrayList<SenderLink>()
        r.beginArray()
        while (r.hasNext()) {
            var senderKey = ""; var bankId = 0L
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "senderKey" -> senderKey = r.nextString()
                    "bankId" -> bankId = r.nextLong()
                    else -> r.skipValue()
                }
            }
            r.endObject()
            if (senderKey.isNotEmpty()) links += SenderLink(senderKey, bankId)
        }
        r.endArray()
        if (links.isNotEmpty()) db.senderLinkDao().insertAll(links)
        return links.size
    }

    private suspend fun readMessageFlags(r: JsonReader) {
        val flags = ArrayList<MessageFlag>()
        r.beginArray()
        while (r.hasNext()) {
            var rawId = ""; var notDuplicate = false; var deleted = false
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "rawId" -> rawId = r.nextString()
                    "notDuplicate" -> notDuplicate = r.nextBoolean()
                    "deleted" -> deleted = r.nextBoolean()
                    else -> r.skipValue()
                }
            }
            r.endObject()
            if (rawId.isNotEmpty()) flags += MessageFlag(rawId, notDuplicate, deleted)
        }
        r.endArray()
        if (flags.isNotEmpty()) db.messageFlagDao().insertAll(flags)
    }

    /**
     * Ids are not carried: they are local autoincrements. On a merge an entry already here
     * (same person, kind, amount and time) is skipped, so importing twice does not double
     * what anyone owes.
     */
    /**
     * A split already here for the same transaction is reused rather than duplicated, so a
     * merge maps the file's split onto it.
     */
    private suspend fun readSplits(r: JsonReader): Map<Long, Long> {
        val ids = mutableMapOf<Long, Long>()
        r.beginArray()
        while (r.hasNext()) {
            var id = 0L; var txnId = ""; var title = ""; var total = 0L; var mine = 0L; var created = 0L
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "id" -> id = r.nextLong()
                    "txnId" -> txnId = r.nextString()
                    "title" -> title = r.nextString()
                    "totalMinor" -> total = r.nextLong()
                    "myShareMinor" -> mine = r.nextLong()
                    "createdAt" -> created = r.nextLong()
                    else -> r.skipValue()
                }
            }
            r.endObject()
            if (txnId.isEmpty()) continue
            val here = db.splitDao().byTxnId(txnId)?.id
                ?: db.splitDao().upsert(Split(txnId = txnId, title = title, totalMinor = total, myShareMinor = mine, createdAt = created))
            ids[id] = here
        }
        r.endArray()
        return ids
    }

    private suspend fun readLoans(r: JsonReader, mode: ImportMode, splitIds: Map<Long, Long>) {
        val existing = if (mode == ImportMode.MERGE) {
            db.loanDao().all().map { listOf(it.person, it.kind, it.amountMinor, it.occurredAt) }.toSet()
        } else emptySet()
        val loans = ArrayList<LoanEntry>()
        r.beginArray()
        while (r.hasNext()) {
            var person = ""; var kind: LoanKind? = null; var amount = 0L; var at = 0L
            var note: String? = null; var txnId: String? = null; var splitId: Long? = null
            r.beginObject()
            while (r.hasNext()) {
                when (r.nextName()) {
                    "person" -> person = r.nextString()
                    "kind" -> kind = runCatching { LoanKind.valueOf(r.nextString()) }.getOrNull()
                    "amountMinor" -> amount = r.nextLong()
                    "occurredAt" -> at = r.nextLong()
                    "note" -> note = r.nextStringOrNull()
                    "txnId" -> txnId = r.nextStringOrNull()
                    "splitId" -> splitId = if (r.peek() == JsonToken.NULL) { r.nextNull(); null } else r.nextLong()
                    else -> r.skipValue()
                }
            }
            r.endObject()
            val k = kind ?: continue
            if (person.isEmpty() || listOf(person, k, amount, at) in existing) continue
            loans += LoanEntry(
                person = person, kind = k, amountMinor = amount, occurredAt = at, note = note,
                txnId = txnId, splitId = splitId?.let { splitIds[it] }
            )
        }
        r.endArray()
        // A transaction carries one loan entry; where the file and this phone both link
        // the same one, REPLACE lets the file's entry win rather than failing the import.
        if (loans.isNotEmpty()) db.loanDao().insertAll(loans)
    }

    private fun readSettings(r: JsonReader): Long? {
        var lastSynced: Long? = null
        r.beginObject()
        while (r.hasNext()) {
            when (r.nextName()) {
                "lastSyncedSmsDate" -> lastSynced = r.nextLongOrNull()
                else -> r.skipValue()
            }
        }
        r.endObject()
        return lastSynced
    }

    private companion object {
        const val BATCH = 500
    }
}

private fun JsonWriter.valueOrNull(value: String?) {
    if (value == null) nullValue() else value(value)
}

private fun JsonWriter.valueOrNull(value: Long?) {
    if (value == null) nullValue() else value(value)
}

private fun JsonReader.nextStringOrNull(): String? =
    if (peek() == JsonToken.NULL) { nextNull(); null } else nextString()

private fun JsonReader.nextLongOrNull(): Long? =
    if (peek() == JsonToken.NULL) { nextNull(); null } else nextLong()
