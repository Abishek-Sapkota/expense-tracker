package com.abi.expensetracker.parser

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Compiles a human-written message template into the regex the parser runs.
 *
 * A template is the bank's message with the varying parts replaced by placeholders:
 *
 *     Rs.{amount} debited from a/c XX{acct} on {date} to {merchant}. Ref {ref}
 *
 * Everything outside a placeholder is matched literally, so a template cannot be
 * malformed the way a hand-written regex can — a stray bracket or dot is just text.
 * Runs of whitespace match flexibly, because banks are inconsistent about spacing.
 */
object TemplateCompiler {

    /** Placeholders a template may use, and what each one accepts. */
    val PLACEHOLDERS: Map<String, String> = mapOf(
        "amount" to "the transaction amount — required",
        "acct" to "last digits of the account or card",
        "merchant" to "who was paid, or who paid you",
        "remark" to "what the money was spent on, as the message words it",
        "ref" to "reference or transaction number",
        "date" to "the date written in the message",
        "time" to "the time written in the message, e.g. 21:00 or 8:41 AM",
        "balance" to "balance left after the transaction",
        "any" to "skip over anything here"
    )

    private const val AMOUNT = """[\d,]+(?:\.\d{1,2})?"""

    /** Covers 20-09-26, 20/09/2026, 2026-09-20, 20-Sep-2026 and "20 Sep 2026". */
    private const val DATE =
        """(?:\d{1,2}[-/.\s]{1,2}[A-Za-z]{3,9}[-/.\s,]{1,2}\d{2,4}|\d{1,4}[-/.]\d{1,2}[-/.]\d{2,4})"""

    /** 21:00, 8:41:37 AM, 1:20PM, 9.05 a.m. */
    private const val TIME =
        """\d{1,2}[:.]\d{2}(?:[:.]\d{2})?(?:\s?[AaPp]\.?[Mm]\.?)?"""

    private fun capture(name: String, isLast: Boolean): String = when (name) {
        "amount" -> """(?<amount>$AMOUNT)"""
        "balance" -> """(?<balance>$AMOUNT)"""
        "acct" -> """(?<acct>\d{3,6})"""
        "ref" -> """(?<ref>[A-Za-z0-9]{4,24})"""
        "date" -> """(?<date>$DATE)"""
        "time" -> """(?<time>$TIME)"""
        // Lazy while more template follows, greedy at the end. A lazy match with nothing
        // after it to anchor against would capture a single character.
        "merchant" -> if (isLast) """(?<merchant>.+)""" else """(?<merchant>.+?)"""
        // Same lazy/greedy split as merchant: free text with no shape of its own, so the
        // literal that follows it in the template is the only thing that can end it.
        "remark" -> if (isLast) """(?<remark>.+)""" else """(?<remark>.+?)"""
        "any" -> if (isLast) """.*""" else """.*?"""
        else -> throw IllegalArgumentException(name)
    }

    sealed interface Outcome {
        data class Ok(val regex: String, val placeholders: List<String>) : Outcome
        data class Invalid(val message: String) : Outcome
    }

    fun compile(template: String): Outcome {
        val text = template.trim()
        if (text.isEmpty()) return Outcome.Invalid("Template is empty.")

        val tokens = tokenize(text) ?: return Outcome.Invalid(
            "Unbalanced braces. Every placeholder needs a closing brace."
        )

        val used = tokens.filterIsInstance<Token.Placeholder>().map { it.name }
        val unknown = used.filterNot { PLACEHOLDERS.containsKey(it) }
        if (unknown.isNotEmpty()) {
            return Outcome.Invalid(
                "Unknown placeholder {${unknown.first()}}. Use: " +
                    PLACEHOLDERS.keys.joinToString(", ") { "{$it}" }
            )
        }
        if ("amount" !in used) {
            return Outcome.Invalid(
                "Template must include {amount} — without it there is nothing to record."
            )
        }
        val duplicate = used.filter { it != "any" }
            .groupBy { it }.entries.firstOrNull { it.value.size > 1 }
        if (duplicate != null) {
            return Outcome.Invalid(
                "{${duplicate.key}} appears more than once. Each placeholder may be used once."
            )
        }

        val lastCapturingIndex = tokens.indexOfLast { it is Token.Placeholder }
        val regex = buildString {
            append("(?i)")
            tokens.forEachIndexed { index, token ->
                when (token) {
                    is Token.Placeholder -> append(capture(token.name, index == lastCapturingIndex))
                    is Token.Literal -> append(literalToRegex(token.text))
                }
            }
        }

        return try {
            Pattern.compile(regex)
            Outcome.Ok(regex, used)
        } catch (e: PatternSyntaxException) {
            Outcome.Invalid("Could not build a pattern from this template.")
        }
    }

    private sealed interface Token {
        data class Literal(val text: String) : Token
        data class Placeholder(val name: String) : Token
    }

    private fun tokenize(text: String): List<Token>? {
        val tokens = mutableListOf<Token>()
        val literal = StringBuilder()
        var i = 0
        while (i < text.length) {
            when (text[i]) {
                '{' -> {
                    val close = text.indexOf('}', i)
                    if (close == -1) return null
                    if (literal.isNotEmpty()) {
                        tokens += Token.Literal(literal.toString())
                        literal.clear()
                    }
                    tokens += Token.Placeholder(text.substring(i + 1, close).trim().lowercase())
                    i = close + 1
                }
                '}' -> return null
                else -> {
                    literal.append(text[i])
                    i++
                }
            }
        }
        if (literal.isNotEmpty()) tokens += Token.Literal(literal.toString())
        return tokens
    }

    /**
     * Literal text is quoted so regex metacharacters in it stay literal, and whitespace
     * runs become a flexible separator so a template written with one space still matches
     * a message that uses two, or a line break.
     */
    private fun literalToRegex(text: String): String {
        val parts = text.split(Regex("""\s+""")).filter { it.isNotEmpty() }
        val leading = if (text.first().isWhitespace()) """\s+""" else ""
        val trailing = if (text.last().isWhitespace()) """\s+""" else ""
        if (parts.isEmpty()) return """\s+"""
        return leading + parts.joinToString("""\s+""") { Pattern.quote(it) } + trailing
    }
}
