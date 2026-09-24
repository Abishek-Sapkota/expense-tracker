package com.abi.expensetracker.data.model

enum class Direction { DEBIT, CREDIT }

/** Where a raw message came from. SMS backfill and live SMS are the same shape. */
enum class Source { SMS, NOTIFICATION }
