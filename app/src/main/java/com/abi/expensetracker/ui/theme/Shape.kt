package com.abi.expensetracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Utilitarian Ledger curvature: defined, durable forms rather than bubbly ones.
 * 8dp for inputs and list items, 16dp for cards, sheets and dialogs, full pills for
 * chips, monograms and action triggers.
 */
internal val LedgerShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),         // inputs, list items, context cards
    medium = RoundedCornerShape(16.dp),       // cards
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp)    // dialogs, bottom sheets
)

/** Full pill, for buttons and action triggers. */
val PillShape = RoundedCornerShape(percent = 50)

/** The hero period card: a card like any other in this system. */
val HeroShape = RoundedCornerShape(16.dp)

/** Standard card. */
val CardShape = RoundedCornerShape(16.dp)

/** Filter chips, selection pills and status chips: fully pill-shaped. */
val ChipShape = RoundedCornerShape(percent = 50)
