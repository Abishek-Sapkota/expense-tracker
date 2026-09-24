package com.abi.expensetracker.data

import com.abi.expensetracker.data.model.Bank
import com.abi.expensetracker.data.model.BankApp
import com.abi.expensetracker.data.model.SenderLink

/**
 * Turns a raw SMS sender into the bank or service the user linked it to.
 *
 * Raw sender ids ("AX-NABIL") are an implementation detail of how the message arrived
 * and are never shown on the home screen — only in settings, where they exist to be
 * mapped. An unmapped sender resolves to null and the UI labels it as unlinked, which
 * points at the fix instead of showing a code that means nothing.
 */
class BankResolver(banks: List<Bank>, links: List<SenderLink>, apps: List<BankApp> = emptyList()) {

    private val bankById: Map<Long, Bank> = banks.associateBy { it.id }
    private val bankIdBySenderKey: Map<String, Long> = links.associate { it.senderKey to it.bankId }
    private val banksByApp: Map<String, List<Bank>> = apps.groupBy({ it.packageName }, { bankById[it.bankId] })
        .mapValues { (_, list) -> list.filterNotNull() }

    /**
     * The linked bank, or null when this sender has not been linked yet.
     *
     * A notification's sender is its app's package. An app claimed by one bank is that
     * bank; one claimed by several (Gmail) goes to the bank whose name appears in [body],
     * and stays unresolved when none does rather than guessing.
     */
    fun bankFor(sender: String?, body: String? = null): Bank? {
        if (sender == null) return null
        bankIdBySenderKey[SenderNormalizer.normalize(sender)]?.let { return bankById[it] }
        val claimants = banksByApp[sender].orEmpty()
        if (claimants.size <= 1) return claimants.firstOrNull()
        val text = body?.lowercase() ?: return null
        return claimants.firstOrNull { text.contains(it.name.trim().lowercase()) }
    }

    /** Bank name, or null when this sender has not been linked yet. */
    fun nameFor(sender: String?): String? = bankFor(sender)?.name

    fun isMapped(sender: String): Boolean =
        bankIdBySenderKey.containsKey(SenderNormalizer.normalize(sender))
}
