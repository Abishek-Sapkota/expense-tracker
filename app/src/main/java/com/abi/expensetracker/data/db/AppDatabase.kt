package com.abi.expensetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abi.expensetracker.data.model.Bank
import com.abi.expensetracker.data.model.BankApp
import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.MessageFlag
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Rule
import com.abi.expensetracker.data.model.SenderLink
import com.abi.expensetracker.data.model.Split
import com.abi.expensetracker.data.model.Txn
import com.abi.expensetracker.data.model.TxnCopy

@Database(
    entities = [
        RawMessage::class,
        Txn::class,
        Rule::class,
        Category::class,
        Bank::class,
        SenderLink::class,
        TxnCopy::class,
        MessageFlag::class,
        LoanEntry::class,
        Split::class,
        BankApp::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rawMessageDao(): RawMessageDao
    abstract fun txnDao(): TxnDao
    abstract fun ruleDao(): RuleDao
    abstract fun categoryDao(): CategoryDao
    abstract fun bankDao(): BankDao
    abstract fun senderLinkDao(): SenderLinkDao
    abstract fun txnCopyDao(): TxnCopyDao
    abstract fun messageFlagDao(): MessageFlagDao
    abstract fun loanDao(): LoanDao
    abstract fun splitDao(): SplitDao
    abstract fun bankAppDao(): BankAppDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /**
         * Adds bank/sender mapping, and makes `transactions.rawId` nullable so a manually
         * entered expense can exist without an SMS behind it.
         *
         * SQLite cannot relax a NOT NULL column in place, so the table is rebuilt and
         * copied. Written out rather than using destructive fallback: by the time this
         * runs the database may hold years of SMS that the phone's own inbox has since
         * dropped, which no reinstall could recover.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `banks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sender_links` (" +
                        "`senderKey` TEXT NOT NULL, " +
                        "`bankId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`senderKey`))"
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transactions_new` (" +
                        "`id` TEXT NOT NULL, " +
                        "`rawId` TEXT, " +
                        "`amountMinor` INTEGER NOT NULL, " +
                        "`direction` TEXT NOT NULL, " +
                        "`accountTail` TEXT, " +
                        "`merchant` TEXT, " +
                        "`refNumber` TEXT, " +
                        "`balanceMinor` INTEGER, " +
                        "`occurredAt` INTEGER NOT NULL, " +
                        "`categoryId` INTEGER, " +
                        "`needsReview` INTEGER NOT NULL, " +
                        "`ruleId` INTEGER, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`rawId`) REFERENCES `raw_messages`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "INSERT INTO `transactions_new` " +
                        "(`id`, `rawId`, `amountMinor`, `direction`, `accountTail`, `merchant`, " +
                        "`refNumber`, `balanceMinor`, `occurredAt`, `categoryId`, `needsReview`, `ruleId`) " +
                        "SELECT `id`, `rawId`, `amountMinor`, `direction`, `accountTail`, `merchant`, " +
                        "`refNumber`, `balanceMinor`, `occurredAt`, `categoryId`, `needsReview`, `ruleId` " +
                        "FROM `transactions`"
                )
                db.execSQL("DROP TABLE `transactions`")
                db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_rawId` ON `transactions` (`rawId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurredAt` ON `transactions` (`occurredAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
            }
        }

        /** Adds the user-written template a rule was compiled from. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `rules` ADD COLUMN `template` TEXT")
            }
        }

        /**
         * Adds a bank icon, and the flag that marks a parsed transaction the user has
         * corrected by hand so a reparse leaves it alone.
         *
         * Plain ALTERs: both columns are additive, so the years of stored messages this
         * table sits beside are never rewritten.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `banks` ADD COLUMN `icon` TEXT")
                db.execSQL(
                    "ALTER TABLE `transactions` ADD COLUMN `userEdited` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** Adds the remark: what a transaction was for, beside who it was with. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `remark` TEXT")
            }
        }

        /**
         * Gives categories an icon and the keywords that auto-assign them.
         *
         * Additive, so the seeded rows survive; [com.abi.expensetracker.data.DefaultCategories]
         * is re-seeded on next start for a database that had none.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `icon` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `keywords` TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Records messages folded into an existing transaction from another channel. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `txn_copies` (`rawId` TEXT NOT NULL, " +
                        "`txnId` TEXT NOT NULL, PRIMARY KEY(`rawId`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_txn_copies_txnId` ON `txn_copies` (`txnId`)"
                )
            }
        }

        /** Per-message user decisions: deleted, or not a duplicate. */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `message_flags` (`rawId` TEXT NOT NULL, " +
                        "`notDuplicate` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`rawId`))"
                )
            }
        }

        /** Loans lent to and borrowed from people, optionally tied to a transaction. */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `loan_entries` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `person` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, " +
                        "`occurredAt` INTEGER NOT NULL, `note` TEXT, `txnId` TEXT)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_loan_entries_person` ON `loan_entries` (`person`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_loan_entries_txnId` ON `loan_entries` (`txnId`)"
                )
            }
        }

        /** Split bills, and the link from a loan entry to the split it belongs to. */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `splits` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `txnId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `totalMinor` INTEGER NOT NULL, " +
                        "`myShareMinor` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_splits_txnId` ON `splits` (`txnId`)")
                db.execSQL("ALTER TABLE `loan_entries` ADD COLUMN `splitId` INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_loan_entries_splitId` ON `loan_entries` (`splitId`)"
                )
            }
        }

        /** Which apps' notifications belong to which bank. */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bank_apps` (`packageName` TEXT NOT NULL, " +
                        "`bankId` INTEGER NOT NULL, PRIMARY KEY(`packageName`, `bankId`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bank_apps_bankId` ON `bank_apps` (`bankId`)")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expenses.db"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11
                    )
                    .build()
                    .also { instance = it }
            }
    }
}
