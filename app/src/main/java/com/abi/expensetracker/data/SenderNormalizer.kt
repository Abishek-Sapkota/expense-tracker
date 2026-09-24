package com.abi.expensetracker.data

/**
 * Reduces an SMS sender id to the part that identifies the bank.
 *
 * Carriers may wrap a sender id as `<operator>-<entity>[-<category>]`, e.g. "AX-NABIL"
 * or "BP-NABIL-S". The operator prefix changes with the routing and the category suffix
 * with the message type, so neither identifies who sent it — only the middle does. A
 * plain sender id such as "NICASIA" passes through untouched.
 *
 * Numeric senders have no such structure and are returned as-is.
 */
object SenderNormalizer {

    fun normalize(sender: String): String {
        val trimmed = sender.trim().uppercase()
        if (trimmed.isEmpty()) return trimmed
        if (!trimmed.contains('-')) return trimmed

        var parts = trimmed.split('-').filter { it.isNotBlank() }
        if (parts.isEmpty()) return trimmed

        // Leading operator code: one or two characters, e.g. AX, VM, BP.
        if (parts.size > 1 && parts.first().length <= 2) parts = parts.drop(1)
        // Trailing category marker: a single character, e.g. -S, -T, -P, -G.
        if (parts.size > 1 && parts.last().length == 1) parts = parts.dropLast(1)

        return parts.joinToString("-").ifBlank { trimmed }
    }
}
