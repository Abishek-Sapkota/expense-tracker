package com.abi.expensetracker.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.abi.expensetracker.data.ExpenseRepository
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.data.db.AppDatabase
import com.abi.expensetracker.data.model.Bank
import com.abi.expensetracker.data.model.Category
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.LoanKind
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.SenderLink
import com.abi.expensetracker.data.model.Source
import com.abi.expensetracker.parser.DefaultRules
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Export, then import into a fresh or a different database, through the real code. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRoundTripTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val settings = SettingsStore(context)
    private val opened = mutableListOf<AppDatabase>()

    private fun newDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
            .also { opened += it }

    @After
    fun tearDown() = opened.forEach { it.close() }

    /** A phone with one account, one category, one booked SMS, its email copy and a loan. */
    private suspend fun seeded(): AppDatabase {
        val db = newDb()
        db.ruleDao().insertAll(DefaultRules.ALL)
        db.bankDao().insert(Bank(id = 1, name = "NIC Asia"))
        db.senderLinkDao().upsert(SenderLink("NICASIA", 1))
        db.categoryDao().insert(Category(id = 1, name = "Dining", icon = "", keywords = "cafe"))
        val repo = ExpenseRepository(context, db, settings)
        repo.ingest(
            listOf(
                RawMessage("s1", "NICASIA", "Rs.500.00 debited from a/c XX1234 to CAFE. Ref 11223344.", 1_000_000, Source.SMS, 0),
                RawMessage("e1", "com.google.android.gm", "NPR 500.00 withdrawn. Txn No 11223344", 1_300_000, Source.NOTIFICATION, 0)
            ),
            prompt = false
        )
        val txn = db.txnDao().all().single()
        db.loanDao().upsert(LoanEntry(person = "Ram", kind = LoanKind.LENT, amountMinor = 50_000, occurredAt = 0, txnId = txn.id))
        return db
    }

    private suspend fun exportOf(db: AppDatabase): File =
        BackupManager(context, db, settings).exportSafetyCopy("round-trip-${System.nanoTime()}.json")

    @Test
    fun `replace restores everything, copies included`() = runBlocking {
        val source = seeded()
        val file = exportOf(source)
        val target = newDb()
        BackupManager(context, target, settings).importFrom(Uri.fromFile(file), ImportMode.REPLACE)

        assertEquals(source.txnDao().all(), target.txnDao().all())
        assertEquals(listOf("e1"), target.txnCopyDao().allRawIds())
        assertEquals(1, target.loanDao().all().size)
        assertEquals(source.bankDao().all(), target.bankDao().all())
    }

    @Test
    fun `merge does not overwrite a local row that shares an id`() = runBlocking {
        val file = exportOf(seeded())
        val here = newDb()
        here.bankDao().insert(Bank(id = 1, name = "Nabil"))
        here.senderLinkDao().upsert(SenderLink("NABIL_ALERT", 1))
        here.categoryDao().insert(Category(id = 1, name = "Rent", icon = "", keywords = "rent"))

        BackupManager(context, here, settings).importFrom(Uri.fromFile(file), ImportMode.MERGE)

        val banks = here.bankDao().all().associateBy { it.name }
        assertEquals(1L, banks.getValue("Nabil").id)
        assertTrue("NIC Asia" in banks)
        val links = here.senderLinkDao().all().associate { it.senderKey to it.bankId }
        assertEquals(1L, links["NABIL_ALERT"])
        assertEquals(banks.getValue("NIC Asia").id, links["NICASIA"])
        val categories = here.categoryDao().all().associateBy { it.name }
        assertEquals(1L, categories.getValue("Rent").id)
        // The imported row points at the imported Dining, not the local Rent.
        val txn = here.txnDao().all().single()
        assertEquals(categories.getValue("Dining").id, txn.categoryId)
    }

    @Test
    fun `a truncated file leaves the database as it was`() = runBlocking {
        val file = exportOf(seeded())
        val cut = File(file.parentFile, "cut.json").apply {
            writeText(file.readText().let { it.substring(0, it.length * 2 / 3) })
        }
        val here = seeded()
        val before = here.txnDao().all()
        val failed = runCatching {
            BackupManager(context, here, settings).importFrom(Uri.fromFile(cut), ImportMode.REPLACE)
        }.isFailure
        assertTrue(failed)
        assertEquals(before, here.txnDao().all())
        assertEquals(1, here.loanDao().all().size)
    }
}
