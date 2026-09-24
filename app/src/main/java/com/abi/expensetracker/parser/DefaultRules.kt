package com.abi.expensetracker.parser

import com.abi.expensetracker.data.model.Direction
import com.abi.expensetracker.data.model.Rule

/**
 * Seed rules. Deliberately generic: they cover the shape most bank SMS share rather than
 * trying to enumerate banks. Bank-specific rules get added from the review queue against
 * real messages, at a lower priority number so they win.
 *
 * senderPattern matches the normalised sender, so it is unaffected by any operator
 * prefix a carrier prepends.
 */
object DefaultRules {

    private const val ANY_SENDER = """.*"""
    private const val AMOUNT = """(?<amount>[\d,]+(?:\.\d{1,2})?)"""
    private const val CUR = """(?:rs\.?|npr|रु|रू)"""
    private const val AUX = """(?:(?:has|have|is|was|been)\s+)*"""

    /**
     * An amount written with no currency in front of it.
     *
     * Needs the decimals and a boundary before the digits, so "0#1100000730" and a date
     * do not read as money the way a bare `[\d,]+` would.
     */
    private const val BARE_AMOUNT = """(?<![\d.,#])(?<amount>[\d,]+\.\d{2})"""

    val ALL: List<Rule> = listOf(
        Rule(
            name = "Generic debit (amount first)",
            senderPattern = ANY_SENDER,
            bodyPattern = """(?i)$CUR\s*$AMOUNT\s+$AUX(?:debited|spent|withdrawn|paid|deducted|transferred)""",
            direction = Direction.DEBIT,
            priority = 900,
            builtIn = true
        ),
        Rule(
            name = "Generic debit (verb first)",
            senderPattern = ANY_SENDER,
            bodyPattern = """(?i)(?:debited|spent|withdrawn|paid|deducted)\s+(?:by|for|with|of)?\s*$CUR\s*$AMOUNT""",
            direction = Direction.DEBIT,
            priority = 901,
            builtIn = true
        ),
        Rule(
            name = "Generic credit (amount first)",
            senderPattern = ANY_SENDER,
            bodyPattern = """(?i)$CUR\s*$AMOUNT\s+$AUX(?:credited|received|deposited|refunded)""",
            direction = Direction.CREDIT,
            priority = 910,
            builtIn = true
        ),
        Rule(
            name = "Generic credit (verb first)",
            senderPattern = ANY_SENDER,
            bodyPattern = """(?i)(?:credited|received|deposited|refunded)\s+(?:with|by|of)?\s*$CUR\s*$AMOUNT""",
            direction = Direction.CREDIT,
            priority = 911,
            builtIn = true
        ),
        // Sanima and others write the amount with no currency at all: "155.00 deposited
        // in your A/C". The verb has to follow the number immediately, which is what
        // keeps an account number or a date from reading as an amount.
        Rule(
            name = "Bare amount debit",
            senderPattern = ANY_SENDER,
            bodyPattern = """(?i)$BARE_AMOUNT\s+$AUX(?:debited|withdrawn|spent|paid|deducted|transferred)\b""",
            direction = Direction.DEBIT,
            priority = 920,
            builtIn = true
        ),
        Rule(
            name = "Bare amount credit",
            senderPattern = ANY_SENDER,
            bodyPattern = """(?i)$BARE_AMOUNT\s+$AUX(?:credited|deposited|received|refunded)\b""",
            direction = Direction.CREDIT,
            priority = 921,
            builtIn = true
        ),
        // Wallets announce a payment without ever saying "debited": eSewa's Fonepay
        // message is "Your transaction of Rs. 20.0 ... has been successfully completed".
        Rule(
            name = "Wallet payment completed",
            senderPattern = ANY_SENDER,
            bodyPattern = """(?i)transaction\s+of\s+$CUR\s*$AMOUNT\b(?=[\s\S]{0,120}?(?:success|complete))""",
            direction = Direction.DEBIT,
            priority = 930,
            builtIn = true
        )
    )
}
