package com.abi.expensetracker.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.abi.expensetracker.data.ExpenseRepository
import com.abi.expensetracker.data.SettingsStore
import com.abi.expensetracker.data.db.AppDatabase
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.LoanEntry
import com.abi.expensetracker.data.model.LoanKind
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Source
import com.abi.expensetracker.data.model.Split
import com.abi.expensetracker.parser.DefaultRules
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The repository against a real (in-memory) Room database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: ExpenseRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = ExpenseRepository(context, db, SettingsStore(context))
        db.ruleDao().insertAll(DefaultRules.ALL)
    }

    @After
    fun tearDown() = db.close()

    private fun sms(body: String, at: Long, sender: String = "NABIL_ALERT") = RawMessage(
        id = RawMessage.idFor(sender, body, at), sender = sender, body = body,
        sentAt = at, source = Source.SMS, importedAt = 0
    )

    private val hour = 3_600_000L

    @Test
    fun `the same message ingested twice books once`() = runBlocking {
        val m = sms("Rs.450.00 debited from a/c XX1234 to SWIGGY. Ref 123456789.", 1_000 * hour)
        repo.ingest(listOf(m), prompt = false)
        repo.ingest(listOf(m), prompt = false)
        assertEquals(1, db.txnDao().all().size)
    }

    @Test
    fun `two messages sharing a ref are two rows unless another channel`() = runBlocking {
        // Same sender, same ref text, a day apart: two payments, never one replacing the other.
        repo.ingest(listOf(sms("Rs.500.00 debited from a/c XX1234. Ref 99887766.", 1_000 * hour)), prompt = false)
        repo.ingest(listOf(sms("Rs.500.00 debited from a/c XX1234. Ref 99887766. Again", 1_024 * hour)), prompt = false)
        assertEquals(2, db.txnDao().all().size)
    }

    @Test
    fun `an email copy of an SMS folds into it`() = runBlocking {
        repo.ingest(listOf(sms("Rs.500.00 debited from a/c XX1234. Ref 99887766.", 1_000 * hour)), prompt = false)
        val email = RawMessage(
            id = "email", sender = "com.google.android.gm",
            body = "NPR 500.00 withdrawn from your account. Txn No 99887766", sentAt = 1_000 * hour + 600_000,
            source = Source.NOTIFICATION, importedAt = 0
        )
        repo.ingest(listOf(email), prompt = false)
        assertEquals(1, db.txnDao().all().size)
        assertEquals(listOf("email"), db.txnCopyDao().allRawIds())
    }

    @Test
    fun `reparse keeps a row linked to a loan`() = runBlocking {
        repo.ingest(listOf(sms("Rs.2,000.00 debited from a/c XX1234 to RAM.", 1_000 * hour)), prompt = false)
        val txn = db.txnDao().all().single()
        db.loanDao().upsert(
            LoanEntry(person = "Ram", kind = LoanKind.LENT, amountMinor = 200_000, occurredAt = txn.occurredAt, txnId = txn.id)
        )
        repo.reparseAll()
        val after = db.txnDao().all()
        assertEquals(1, after.size)
        assertEquals(txn.id, after.single().id)
    }

    @Test
    fun `deleting a split bill removes the split and keeps a loan as cash`() = runBlocking {
        repo.ingest(
            listOf(
                sms("Rs.900.00 debited from a/c XX1234 to CAFE.", 1_000 * hour),
                sms("Rs.300.00 debited from a/c XX1234 to HARI.", 1_001 * hour)
            ),
            prompt = false
        )
        val (bill, lent) = db.txnDao().all().sortedBy { it.occurredAt }
        val splitId = db.splitDao().upsert(Split(txnId = bill.id, title = "Cafe", totalMinor = 90_000, myShareMinor = 30_000, createdAt = 0))
        db.loanDao().upsert(LoanEntry(person = "Sita", kind = LoanKind.LENT, amountMinor = 60_000, occurredAt = 0, splitId = splitId))
        db.loanDao().upsert(LoanEntry(person = "Hari", kind = LoanKind.LENT, amountMinor = 30_000, occurredAt = 0, txnId = lent.id))

        repo.deleteTransactions(listOf(bill, lent))

        assertNull(db.splitDao().byTxnId(bill.id))
        val loans = db.loanDao().all()
        assertEquals(listOf("Hari"), loans.map { it.person })
        assertNull(loans.single().txnId)
    }

    @Test
    fun `a failed wallet payment is not booked`() = runBlocking {
        repo.ingest(listOf(sms("Your transaction of Rs. 500.00 to ABC Store was unsuccessful.", 1_000 * hour, "eSewa")), prompt = false)
        assertEquals(0, db.txnDao().all().size)
        assertNotNull(db.rawMessageDao().byId(RawMessage.idFor("eSewa", "Your transaction of Rs. 500.00 to ABC Store was unsuccessful.", 1_000 * hour)))
    }

    @Test
    fun `search finds by text and by exact amount across all time`() = runBlocking {
        repo.ingest(
            listOf(
                sms("Rs.450.00 debited from a/c XX1234 to DARAZ.", 10 * hour),
                sms("Rs.1,200.00 debited from a/c XX1234 to SWIGGY.", 5_000 * hour)
            ),
            prompt = false
        )
        assertEquals(listOf("DARAZ"), repo.observeSearch("daraz").first().map { it.txn.merchant })
        assertEquals(listOf("SWIGGY"), repo.observeSearch("1200").first().map { it.txn.merchant })
        assertEquals(0, repo.observeSearch("100%").first().size)
    }

    @Test
    fun `filing with a keyword files the others like it`() = runBlocking {
        val dining = db.categoryDao().insert(
            com.abi.expensetracker.data.model.Category(name = "Dining", icon = "", keywords = "")
        )
        repo.ingest(
            listOf(
                sms("Rs.300.00 debited from a/c XX1234 to FOODMANDU.", 10 * hour),
                sms("Rs.500.00 debited from a/c XX1234 to FOODMANDU.", 20 * hour)
            ),
            prompt = false
        )
        val first = db.txnDao().all().first()
        val more = repo.fileAs(first, dining, "foodmandu")
        assertEquals(1, more)
        assertEquals(listOf(dining, dining), db.txnDao().all().map { it.categoryId })
        assertEquals(dining, repo.likelyCategory(db.txnDao().all().last()))
    }

    @Test
    fun `setting a wallet ID turns loads to it into transfers, out of spending`() = runBlocking {
        val esewa = db.bankDao().insert(com.abi.expensetracker.data.model.Bank(name = "Esewa"))
        repo.ingest(
            listOf(
                sms("Your  Esewa Wallet Load for 9866550884 of 3200.00 is successful on 13-Sep-2026 20:07:33 .", 10 * hour, "NMB_ALERT"),
                sms("Your  Esewa Wallet Load for 9823083036 of 300.00 is successful on 13-Sep-2026 21:07:33 .", 11 * hour, "NMB_ALERT")
            ),
            prompt = false
        )
        // No template for these in the default rules: add the one the owner uses.
        if (db.txnDao().all().isEmpty()) {
            repo.addTemplateRule("NMB wallet load", "NMB_ALERT",
                "Your {merchant} load for {any} of {amount} is successful on {date} {time}", Direction.DEBIT)
            repo.reparseAll()
        }
        val bank = db.bankDao().all().first { it.id == esewa }
        assertEquals(1, repo.setWalletIds(bank, "9866550884"))
        val range = com.abi.expensetracker.data.DateRange(0, Long.MAX_VALUE)
        assertEquals(30_000L, repo.observeSpentBetween(range).first())
        assertEquals(320_000L, repo.observeTransfersBetween(range).first())
        // A new load to the same wallet is a transfer from the start.
        repo.ingest(listOf(sms("Your  Esewa Wallet Load for 9866550884 of 50.00 is successful on 13-Sep-2026 22:07:33 .", 12 * hour, "NMB_ALERT")), prompt = false)
        assertEquals(325_000L, repo.observeTransfersBetween(range).first())
    }
}
