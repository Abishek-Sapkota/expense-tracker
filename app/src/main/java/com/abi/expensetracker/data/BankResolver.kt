package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Bank
import com.abi.expensetracker.data.model.SenderLink

/**
 * Turns a raw SMS sender into the bank or service the user linked it to.
 *
 * Raw sender ids ("AX-NABIL") are an implementation detail of how the message arrived
 * and are never shown on the home screen — only in settings, where they exist to be
 * mapped. An unmapped sender resolves to null and the UI labels it as unlinked, which
 * points at the fix instead of showing a code that means nothing.
 */
class BankResolver(
    banks: List<Bank>,
    links: List<SenderLink>,
    /** An installed app's name from its package, or null; the resolver itself stays pure. */
    private val appLabel: (String) -> String? = { null }
) {

    private val bankById: Map<Long, Bank> = banks.associateBy { it.id }
    private val bankIdBySenderKey: Map<String, Long> = links.associate { it.senderKey to it.bankId }
    // Longest name first, so "Global IME" wins over a bank called "Global".
    private val banksByName: List<Pair<String, Bank>> = banks
        .map { it.name.trim().lowercase() to it }
        .filter { it.first.isNotEmpty() }
        .sortedByDescending { it.first.length }

    /**
     * The linked bank, or null when this sender has not been linked yet.
     *
     * An SMS sender is linked by hand. A notification's sender is its app's package, and
     * no app is tied to an account: an app named after one ("NMB Mobile Banking", "eSewa")
     * is that account, and any other app (Gmail, Messages) goes to the account whose name
     * appears in [body]. The app's name is tried first, because a wallet's own message can
     * name the bank the money came from. Unresolved rather than guessed when neither names
     * an account.
     */
    fun bankFor(sender: String?, body: String? = null): Bank? {
        if (sender == null) return null
        bankIdBySenderKey[SenderNormalizer.normalize(sender)]?.let { return bankById[it] }
        if (!isPackage(sender)) return null
        appLabel(sender)?.lowercase()?.let { label -> named(label)?.let { return it } }
        return body?.lowercase()?.let(::named)
    }

    private fun named(text: String): Bank? = banksByName.firstOrNull { text.contains(it.first) }?.second

    /** Bank name, or null when this sender has not been linked yet. */
    fun nameFor(sender: String?): String? = bankFor(sender)?.name

    fun isMapped(sender: String): Boolean =
        bankIdBySenderKey.containsKey(SenderNormalizer.normalize(sender))

    private companion object {
        /** "com.google.android.gm": dotted, no spaces. SMS ids ("NMB_ALERT") never have a dot. */
        val PACKAGE = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")

        fun isPackage(sender: String) = PACKAGE.matches(sender)
    }
}
