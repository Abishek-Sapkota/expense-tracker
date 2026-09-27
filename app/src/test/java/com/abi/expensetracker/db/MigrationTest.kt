package com.abi.expensetracker.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.abi.expensetracker.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every exported schema migrates to the current one and matches what Room expects.
 *
 * A migration that is wrong only shows up on a phone that still has the old database,
 * which is the user's phone; this is the one place it can be caught first.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun `every schema migrates to the current one`() {
        val latest = AppDatabase.MIGRATIONS.last().endVersion
        for (start in 1 until latest) {
            val name = "migrate-from-$start"
            helper.createDatabase(name, start).close()
            helper.runMigrationsAndValidate(name, latest, true, *AppDatabase.MIGRATIONS).close()
        }
    }

    @Test
    fun `a transaction survives the account column being added`() {
        helper.createDatabase("rows", 12).use { db ->
            db.execSQL(
                "INSERT INTO raw_messages (id, sender, body, sentAt, source, importedAt) " +
                    "VALUES ('r1', 'NABIL', 'Rs 5 debited', 1, 'SMS', 1)"
            )
            db.execSQL(
                "INSERT INTO transactions (id, rawId, amountMinor, direction, occurredAt, " +
                    "needsReview, userEdited) VALUES ('t1', 'r1', 500, 'DEBIT', 1, 0, 1)"
            )
        }
        helper.runMigrationsAndValidate("rows", 13, true, *AppDatabase.MIGRATIONS).use { db ->
            db.query("SELECT amountMinor, bankId FROM transactions WHERE id = 't1'").use { c ->
                c.moveToFirst()
                assertEquals(500L, c.getLong(0))
                assertEquals(true, c.isNull(1))
            }
        }
    }
}
