package com.abi.expensetracker.parser

import com.abi.expensetracker.data.Money
import com.abi.expensetracker.data.model.RawMessage
import com.abi.expensetracker.data.model.Rule
import com.abi.expensetracker.data.model.Txn
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

sealed interface ParseOutcome {
    data class Parsed(val txn: Txn) : ParseOutcome
    /** Nothing matched. The raw message is kept and listed as unparsed. */
    data object NoMatch : ParseOutcome
}

/**
 * Pure text-to-object. No Android types, so the whole thing is unit-testable on the
 * desktop — which is where parser work actually gets done, since real bank formats are
 * discovered one surprise at a time.
 */
class SmsParser(
    rules: List<Rule>,
    private val zone: ZoneId = ZoneId.systemDefault()
) {

    private data class Compiled(val rule: Rule, val sender: Regex, val body: Regex)

    private val compiled: List<Compiled> = rules
        .sortedWith(compareBy({ it.priority }, { it.id }))
        .mapNotNull { rule ->
            try {
                Compiled(rule, Regex(rule.senderPattern), Regex(rule.bodyPattern))
            } catch (e: Exception) {
                // A user-authored rule with bad regex must not take down parsing of
                // every other message. Skip it; the rules screen shows it as invalid.
                null
            }
        }

    fun parse(message: RawMessage): ParseOutcome {
        for (c in compiled) {
            if (!c.sender.containsMatchIn(message.sender)) continue
            val match = c.body.find(message.body) ?: continue

            val amountMinor = match.namedOrNull("amount")
                ?.let { Money.parseToMinor(it) }
                ?: continue

            val body = message.body
            val accountTail = match.namedOrNull("acct") ?: FieldExtractors.accountTail(body)
            val merchant = match.namedOrNull("merchant") ?: FieldExtractors.merchant(body)
            val remark = match.namedOrNull("remark") ?: FieldExtractors.remark(body)
            val refNumber = match.namedOrNull("ref") ?: FieldExtractors.refNumber(body)
            val balanceMinor = match.namedOrNull("balance")?.let { Money.parseToMinor(it) }
                ?: FieldExtractors.balanceMinor(body)

            return ParseOutcome.Parsed(
                Txn(
                    id = Txn.idFor(refNumber, amountMinor, message.id),
                    rawId = message.id,
                    amountMinor = amountMinor,
                    direction = c.rule.direction,
                    accountTail = accountTail,
                    merchant = merchant,
                    remark = remark,
                    refNumber = refNumber,
                    balanceMinor = balanceMinor,
                    occurredAt = occurredAt(match, message),
                    // A built-in generic rule matched, or the row says nothing about who
                    // or what it was: worth a human glance before it counts as clean data.
                    needsReview = c.rule.builtIn || (merchant == null && remark == null),
                    ruleId = c.rule.id
                )
            )
        }
        return ParseOutcome.NoMatch
    }

    /**
     * The date and time the bank stated, where a template captured them; otherwise the
     * moment the message arrived.
     *
     * The message timestamp is the better default — banks write dates a dozen ways and
     * usually send within seconds of the swipe. But a message can arrive late (emails
     * trail by minutes), or restate an earlier transaction, and then only what the bank
     * wrote is right.
     *
     * A written time without a written date belongs to the arrival day — unless that would
     * put it clearly after the message arrived, which means the transaction was just before
     * midnight and the message came just after, so it goes on the day before.
     */
    private fun occurredAt(match: MatchResult, message: RawMessage): Long {
        val arrived = Instant.ofEpochMilli(message.sentAt).atZone(zone)
        val writtenDate = match.namedOrNull("date")?.let { DateParser.parse(it) }
        val writtenTime = match.namedOrNull("time")?.let { TimeParser.parse(it) }
        if (writtenDate == null && writtenTime == null) return message.sentAt

        var candidate = (writtenDate ?: arrived.toLocalDate())
            .atTime(writtenTime ?: arrived.toLocalTime())
            .atZone(zone).toInstant().toEpochMilli()
        if (writtenDate == null && candidate > message.sentAt + LATE_NIGHT_SLACK_MILLIS) {
            candidate -= 24L * 60 * 60 * 1000
        }

        // A date years away from the message is almost certainly not the transaction date
        // — a card expiry or a mis-scanned reference caught by the pattern. The arrival
        // time is wrong by at most a day; a date read wrongly can be wrong by decades.
        return if (abs(candidate - message.sentAt) > MAX_DATE_DRIFT_MILLIS) {
            message.sentAt
        } else {
            candidate
        }
    }

    private companion object {
        /** Two years either side of the message. */
        const val MAX_DATE_DRIFT_MILLIS = 730L * 24 * 60 * 60 * 1000

        /** A written time this far after arrival is read as the previous day's. */
        const val LATE_NIGHT_SLACK_MILLIS = 60L * 60 * 1000
    }
}
