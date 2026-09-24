package com.abi.expensetracker.data

import com.abi.expensetracker.data.DuplicateMatcher.Candidate
import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Txn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateMatcherTest {

    private val minute = 60_000L

    private fun txn(
        id: String,
        at: Long = 0L,
        amountMinor: Long = 17_500,
        direction: Direction = Direction.DEBIT,
        remark: String? = null,
        rawId: String? = "raw-$id",
        userEdited: Boolean = false
    ) = Txn(
        id = id, rawId = rawId, amountMinor = amountMinor, direction = direction,
        accountTail = null, merchant = null, remark = remark, refNumber = null,
        balanceMinor = null, occurredAt = at, userEdited = userEdited
    )

    private val sms = txn("sms", remark = "16682242fx7C,2222160005194089/90801352")

    private fun find(incoming: Txn, sender: String, vararg candidates: Candidate) =
        DuplicateMatcher.findOriginal(incoming, sender, candidates.toList())

    @Test
    fun `an email trailing the SMS by twenty minutes is a copy`() {
        val email = txn("email", at = 21 * minute, remark = "16682242fx7C,2222160005194089/90801352150002")
        assertEquals(sms, find(email, "COM.GOOGLE.ANDROID.GM", Candidate(sms, "SANIMABANK", emptySet())))
    }

    @Test
    fun `a second message from the same sender is a new transaction`() {
        val again = txn("again", at = 5 * minute)
        assertNull(find(again, "SANIMABANK", Candidate(sms, "SANIMABANK", emptySet())))
    }

    @Test
    fun `a transaction takes one copy per sender`() {
        val email = txn("email2", at = 10 * minute)
        val taken = Candidate(sms, "SANIMABANK", setOf("COM.GOOGLE.ANDROID.GM"))
        assertNull(find(email, "COM.GOOGLE.ANDROID.GM", taken))
    }

    @Test
    fun `outside the window is a new transaction`() {
        val late = txn("late", at = 61 * minute)
        assertNull(find(late, "COM.GOOGLE.ANDROID.GM", Candidate(sms, "SANIMABANK", emptySet())))
    }

    @Test
    fun `different amount or direction is a new transaction`() {
        val other = Candidate(sms, "SANIMABANK", emptySet())
        assertNull(find(txn("a", amountMinor = 17_600), "GM", other))
        assertNull(find(txn("b", direction = Direction.CREDIT), "GM", other))
    }

    @Test
    fun `remarks that lead differently keep two transactions apart`() {
        val email = txn("email", at = 3 * minute, remark = "99999999abcd,123")
        assertNull(find(email, "GM", Candidate(sms, "SANIMABANK", emptySet())))
    }

    @Test
    fun `a hand-edited remark does not block the match`() {
        val edited = sms.copy(remark = "Momo", userEdited = true)
        val email = txn("email", at = 3 * minute, remark = "16682242fx7C,2222")
        assertEquals(edited, find(email, "GM", Candidate(edited, "SANIMABANK", emptySet())))
    }

    @Test
    fun `manual entries are never matched`() {
        val cash = txn("cash", rawId = null)
        assertNull(find(txn("email", at = minute), "GM", Candidate(cash, "MANUAL", emptySet())))
    }

    @Test
    fun `the nearest candidate wins`() {
        val far = txn("far", at = -50 * minute)
        val near = txn("near", at = -2 * minute)
        val result = find(
            txn("email"), "GM",
            Candidate(far, "SANIMABANK", emptySet()),
            Candidate(near, "NMB", emptySet())
        )
        assertEquals(near, result)
    }

    @Test
    fun `missing remark on either side is compatible`() {
        assertTrue(DuplicateMatcher.remarksCompatible(null, "x"))
        assertTrue(DuplicateMatcher.remarksCompatible("coffee,0070", "COFFEE"))
        assertFalse(DuplicateMatcher.remarksCompatible("coffee", "khaja"))
    }
}
