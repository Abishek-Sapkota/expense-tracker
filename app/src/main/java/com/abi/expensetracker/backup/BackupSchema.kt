package com.abi.expensetracker.backup

object BackupSchema {
    /**
     * Bump on any change to the exported shape, and handle the older number on import.
     * Without this field the first schema change would orphan every backup already
     * written — which is exactly when a backup matters most.
     */
    const val CURRENT_VERSION = 10

    const val FIELD_SCHEMA_VERSION = "schemaVersion"
    const val FIELD_EXPORTED_AT = "exportedAt"
    const val FIELD_APP_VERSION = "appVersionCode"
    const val FIELD_RAW_MESSAGES = "rawMessages"
    const val FIELD_TRANSACTIONS = "transactions"
    const val FIELD_RULES = "rules"
    const val FIELD_CATEGORIES = "categories"
    const val FIELD_BANKS = "banks"
    const val FIELD_SENDER_LINKS = "senderLinks"
    /** Since 7. Per-message deletions and "not a duplicate" decisions. */
    const val FIELD_MESSAGE_FLAGS = "messageFlags"
    /** Since 8. Loans lent to and borrowed from people. */
    const val FIELD_LOANS = "loans"
    /** Since 9. Written before [FIELD_LOANS], whose entries refer to these ids. */
    const val FIELD_SPLITS = "splits"
    /** Since 10. Apps whose notifications belong to a bank. */
    const val FIELD_BANK_APPS = "bankApps"
    const val FIELD_SETTINGS = "settings"
}

enum class ImportMode {
    /** Wipe everything, then load the file. For moving to a new phone. */
    REPLACE,

    /** Keep what is here, add what is missing. For combining two devices' history. */
    MERGE
}

data class ImportResult(
    val schemaVersion: Int,
    val rawMessages: Int,
    val transactions: Int,
    val rules: Int,
    val categories: Int,
    val banks: Int = 0,
    val senderLinks: Int = 0
)

data class ExportResult(
    val rawMessages: Int,
    val transactions: Int,
    val rules: Int,
    val categories: Int,
    val banks: Int = 0,
    val senderLinks: Int = 0
)
