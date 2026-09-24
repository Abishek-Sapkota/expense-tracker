package com.abi.expensetracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat

/**
 * Utilitarian Ledger (design/utilitarian_ledger/DESIGN.md).
 *
 * A pure white canvas (the user's choice over the spec's #FAF9F6 paper tone) with white
 * cards bounded by 1px hairlines, and an
 * OLED-friendly charcoal stack in dark. Depth is tonal layering plus hairlines, never
 * diffuse shadow. The accent is user-selectable (see [AccentColor]); everything else is
 * fixed, because inflow/outflow semantics must not move with a cosmetic setting.
 *
 * The neutral stack is user-selectable too (see [NeutralPalette] and its role mapping in
 * [toScheme]); Neutral grey is the default.
 */
/**
 * Debit and credit tints, which Material's color scheme has no slot for.
 *
 * Plainly red and green: this is the one place in the app where the convention everyone
 * already knows beats a subtler palette, and money out being unmistakable at arm's length
 * is the whole job of a ledger row.
 *
 * Still never the only signal. Every amount carries an explicit sign and every row icon
 * carries a direction arrow, so the distinction survives a red-green colour deficiency
 * and a greyscale screenshot.
 */
data class FinanceColors(
    val debit: Color,
    val credit: Color,
    val neutral: Color,
    val creditSurface: Color,
    val debitSurface: Color
)

private val LightFinance = FinanceColors(
    debit = Color(0xFFD32F2F),
    credit = Color(0xFF2E7D32),
    neutral = Color(0xFF706E69),
    creditSurface = Color(0xFFC8F2C4),
    debitSurface = Color(0xFFFFDAD6)
)

private val DarkFinance = FinanceColors(
    debit = Color(0xFFE57373),
    credit = Color(0xFF81C784),
    neutral = Color(0xFF9E9C96),
    creditSurface = Color(0xFF1E3A20),
    debitSurface = Color(0xFF4A2220)
)

val LocalFinanceColors = staticCompositionLocalOf { LightFinance }

/** Monospace-ish tabular styling for anything that has to line up in a column. */
val LocalTabularStyle = staticCompositionLocalOf { TextStyle.Default }

@Composable
fun ExpenseTrackerTheme(
    accent: AccentPalette = AccentColor.DEFAULT.palette(),
    neutrals: NeutralPalette = NeutralPalette.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val base = (if (darkTheme) neutrals.dark else neutrals.light).toScheme(darkTheme)
    val colorScheme = if (darkTheme) {
        base.copy(
            primary = accent.darkPrimary,
            onPrimary = accent.darkOnPrimary,
            primaryContainer = accent.darkContainer,
            onPrimaryContainer = accent.darkOnContainer,
            inversePrimary = accent.lightPrimary,
            surfaceTint = accent.darkPrimary
        )
    } else {
        base.copy(
            primary = accent.lightPrimary,
            onPrimary = accent.lightOnPrimary,
            primaryContainer = accent.lightContainer,
            onPrimaryContainer = accent.lightOnContainer,
            inversePrimary = accent.darkPrimary,
            surfaceTint = accent.lightPrimary
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            (context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view)
                    .isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalFinanceColors provides if (darkTheme) DarkFinance else LightFinance,
        LocalTabularStyle provides TabularStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LedgerTypography,
            shapes = LedgerShapes,
            content = content
        )
    }
}

/** Convenience accessors so screens read `AppTheme.finance.debit`. */
object AppTheme {
    val finance: FinanceColors
        @Composable get() = LocalFinanceColors.current
}

internal val LedgerTypography: Typography
    get() = ledgerTypography()
