package com.abi.expensetracker.data

import kotlinx.coroutines.sync.Mutex

/**
 * One writer at a time for the jobs that read the database, decide, then write.
 *
 * Ingest checks for a stored copy before inserting, reparse rebuilds every parsed row, and
 * an import wipes or merges tables. Two of them interleaving is how one SMS was booked
 * twice (a live broadcast and an inbox sync both passing the duplicate check) or a live
 * message was read first mid-reparse and its own original then folded as a copy of it.
 * They are rare and short, so a single process-wide lock costs nothing noticeable.
 */
object DbWrites {
    val lock = Mutex()
}
