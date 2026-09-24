package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Bank
import com.abi.expensetracker.data.model.BankApp
import com.abi.expensetracker.data.model.SenderLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankResolverTest {

    private val banks = listOf(Bank(1, "Nabil Bank"), Bank(2, "eSewa"))
    private val links = listOf(SenderLink("NABIL", 1), SenderLink("ESEWA", 2))
    private val resolver = BankResolver(banks, links)

    @Test
    fun `maps a linked sender to its bank name`() {
        assertEquals("Nabil Bank", resolver.nameFor("AX-NABIL"))
    }

    @Test
    fun `one link covers every carrier variant of that sender`() {
        assertEquals("Nabil Bank", resolver.nameFor("VM-NABIL"))
        assertEquals("Nabil Bank", resolver.nameFor("BP-NABIL-S"))
    }

    @Test
    fun `an unlinked sender resolves to null rather than leaking the sender id`() {
        assertNull(resolver.nameFor("AX-UNKNOWNBK"))
    }

    @Test
    fun `a manual transaction has no sender and no bank`() {
        assertNull(resolver.nameFor(null))
    }

    @Test
    fun `reports whether a sender still needs linking`() {
        assertTrue(resolver.isMapped("AX-NABIL"))
        assertFalse(resolver.isMapped("AX-UNKNOWNBK"))
    }

    @Test
    fun `a link pointing at a deleted bank does not resolve to a stale name`() {
        val stale = BankResolver(emptyList(), links)
        assertNull(stale.nameFor("AX-NABIL"))
    }

    private val gmail = "com.google.android.gm"
    private val sanima = Bank(3, "Sanima")
    private val nmb = Bank(4, "NMB")
    private val esewa = Bank(5, "eSewa")
    private val appResolver = BankResolver(
        listOf(sanima, nmb, esewa),
        emptyList(),
        listOf(BankApp(gmail, 3), BankApp(gmail, 4), BankApp("com.f1soft.esewa", 5))
    )

    @Test
    fun `an app claimed by one bank is that bank`() {
        assertEquals(esewa, appResolver.bankFor("com.f1soft.esewa", "Paid Rs. 20"))
    }

    @Test
    fun `a shared app goes to the bank the message names`() {
        assertEquals(sanima, appResolver.bankFor(gmail, "Sanima Bank Transaction Alert NPR 175.00"))
        assertEquals(nmb, appResolver.bankFor(gmail, "NMB e-Notification Dear customer"))
    }

    @Test
    fun `a shared app with no bank named stays unresolved`() {
        assertNull(appResolver.bankFor(gmail, "Your order has shipped"))
        assertNull(appResolver.bankFor(gmail, null))
    }
}
