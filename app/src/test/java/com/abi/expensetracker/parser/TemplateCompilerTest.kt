package com.abi.expensetracker.parser

import com.abi.expensetracker.data.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateCompilerTest {

    private fun compile(template: String): TemplateCompiler.Outcome.Ok =
        TemplateCompiler.compile(template) as TemplateCompiler.Outcome.Ok

    private fun invalid(template: String): String =
        (TemplateCompiler.compile(template) as TemplateCompiler.Outcome.Invalid).message

    private fun match(template: String, message: String): MatchResult? =
        Regex(compile(template).regex).find(message)

    @Test
    fun `extracts every field from a real message shape`() {
        val m = match(
            "Rs.{amount} debited from a/c XX{acct} to {merchant}. Ref {ref}",
            "Rs.450.00 debited from a/c XX1234 to SWIGGY. Ref 123456789."
        )!!
        assertEquals(45_000L, Money.parseToMinor(m.namedOrNull("amount")!!))
        assertEquals("1234", m.namedOrNull("acct"))
        assertEquals("SWIGGY", m.namedOrNull("merchant"))
        assertEquals("123456789", m.namedOrNull("ref"))
    }

    @Test
    fun `captures a remark alongside the merchant`() {
        val m = match(
            "Rs.{amount} sent to {merchant}. Remarks: {remark}",
            "Rs.250.00 sent to KHALTI. Remarks: Khaja"
        )!!
        assertEquals("KHALTI", m.namedOrNull("merchant"))
        assertEquals("Khaja", m.namedOrNull("remark"))
    }

    @Test
    fun `punctuation in the template is literal, not regex`() {
        // "Rs." must not let the dot match any character, or "RsX" would parse.
        val regex = Regex(compile("Rs.{amount} spent").regex)
        assertTrue(regex.containsMatchIn("Rs.99 spent"))
        assertNull(regex.find("RsX99 spent"))
    }

    @Test
    fun `brackets and other metacharacters are safe to type`() {
        val m = match("Spent (Rs.{amount}) [card {acct}]", "Spent (Rs.250.50) [card 4321]")!!
        assertEquals(25_050L, Money.parseToMinor(m.namedOrNull("amount")!!))
        assertEquals("4321", m.namedOrNull("acct"))
    }

    @Test
    fun `spacing differences do not break a match`() {
        val regex = Regex(compile("Rs.{amount} debited from {merchant}").regex)
        assertTrue(regex.containsMatchIn("Rs.100  debited   from AMAZON"))
        assertTrue(regex.containsMatchIn("Rs.100 debited\nfrom AMAZON"))
    }

    @Test
    fun `matching ignores case`() {
        assertTrue(Regex(compile("Rs.{amount} DEBITED").regex).containsMatchIn("rs.75 debited"))
    }

    @Test
    fun `a trailing merchant captures the whole name, not one character`() {
        // A lazy group with nothing after it would stop at "S".
        val m = match("Rs.{amount} spent at {merchant}", "Rs.20 spent at STARBUCKS COFFEE")!!
        assertEquals("STARBUCKS COFFEE", m.namedOrNull("merchant"))
    }

    @Test
    fun `a merchant followed by more template stops at the next literal`() {
        val m = match(
            "Rs.{amount} debited to {merchant} on {any}",
            "Rs.20 debited to SWIGGY on 20-09-26"
        )!!
        assertEquals("SWIGGY", m.namedOrNull("merchant"))
    }

    @Test
    fun `any skips over text without capturing it`() {
        val m = match("Rs.{amount} {any} Ref {ref}", "Rs.310.00 debited somewhere Ref AB1234")!!
        assertEquals(31_000L, Money.parseToMinor(m.namedOrNull("amount")!!))
        assertEquals("AB1234", m.namedOrNull("ref"))
    }

    @Test
    fun `lakh grouped amounts parse`() {
        val m = match("NPR {amount} debited", "NPR 1,23,456.78 debited")!!
        assertEquals(12_345_678L, Money.parseToMinor(m.namedOrNull("amount")!!))
    }

    @Test
    fun `a template without amount is rejected`() {
        assertTrue(invalid("debited from a/c XX{acct}").contains("{amount}"))
    }

    @Test
    fun `an unknown placeholder names the valid ones`() {
        val message = invalid("Rs.{amount} to {payee}")
        assertTrue(message.contains("{payee}"))
        assertTrue(message.contains("{merchant}"))
    }

    @Test
    fun `an unclosed brace is reported rather than silently mangled`() {
        assertTrue(invalid("Rs.{amount debited").contains("brace"))
    }

    @Test
    fun `a repeated placeholder is rejected`() {
        // Java regex cannot declare the same group name twice; catching it here gives a
        // readable message instead of a PatternSyntaxException.
        assertTrue(invalid("Rs.{amount} and Rs.{amount}").contains("more than once"))
    }

    @Test
    fun `repeated any is allowed because it captures nothing`() {
        val m = match("Rs.{amount} {any} card {acct} {any}", "Rs.50 spent on card 9999 today")!!
        assertEquals("9999", m.namedOrNull("acct"))
    }

    @Test
    fun `an empty template is rejected`() {
        assertTrue(invalid("   ").contains("empty"))
    }

    @Test
    fun `a template built from a whole real message still matches that message`() {
        // The path the UI takes: paste a message, swap the amount for a placeholder.
        val real = "Your a/c XX9876 is credited with NPR 5,000.00 on 01-09-26."
        val template = real.replace("5,000.00", "{amount}").replace("9876", "{acct}")
        val m = match(template, real)!!
        assertEquals(500_000L, Money.parseToMinor(m.namedOrNull("amount")!!))
        assertEquals("9876", m.namedOrNull("acct"))
    }
}
