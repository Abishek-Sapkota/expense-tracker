package com.abi.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SenderNormalizerTest {

    @Test
    fun `strips a carrier operator prefix`() {
        assertEquals("NABIL", SenderNormalizer.normalize("AX-NABIL"))
        assertEquals("NABIL", SenderNormalizer.normalize("VM-NABIL"))
        assertEquals("NICASIA", SenderNormalizer.normalize("JD-NICASIA"))
    }

    @Test
    fun `strips a trailing category marker`() {
        assertEquals("NABIL", SenderNormalizer.normalize("BP-NABIL-S"))
        assertEquals("NMBBNK", SenderNormalizer.normalize("AD-NMBBNK-T"))
    }

    @Test
    fun `every carrier variant of one bank collapses to the same key`() {
        val variants = listOf("AX-NABIL", "VM-NABIL", "BP-NABIL-S", "JM-NABIL-T")
        assertEquals(1, variants.map { SenderNormalizer.normalize(it) }.distinct().size)
    }

    @Test
    fun `numeric senders are left alone`() {
        assertEquals("+919876543210", SenderNormalizer.normalize("+919876543210"))
    }

    @Test
    fun `bare sender id with no prefix survives`() {
        assertEquals("NABIL", SenderNormalizer.normalize("NABIL"))
        assertEquals("NABIL", SenderNormalizer.normalize("nabil"))
    }

    @Test
    fun `does not strip a meaningful token down to nothing`() {
        assertEquals("PAYTM", SenderNormalizer.normalize("PAYTM"))
        assertEquals("X", SenderNormalizer.normalize("X"))
    }
}
