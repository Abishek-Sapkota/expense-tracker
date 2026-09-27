package com.abi.expensetracker.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The current date, emitted again just after each midnight.
 *
 * Anything that turns "Today" or "this month" into a date range has to follow this rather
 * than read the clock once: a ledger left open overnight otherwise kept showing yesterday,
 * and hid the morning's new transactions. It sleeps until midnight rather than polling,
 * and only runs while something collects it.
 */
fun todayFlow(): Flow<LocalDate> = flow {
    while (true) {
        val now = LocalDateTime.now()
        emit(now.toLocalDate())
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        delay(Duration.between(now, nextMidnight).toMillis() + 1_000)
    }
}.distinctUntilChanged()
