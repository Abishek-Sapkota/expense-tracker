package com.abi.expensetracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Utilitarian Ledger spec names Inter. No font file ships with the app (and it has no
 * network to download one), so this uses the platform default; weights and sizes follow
 * the spec. Dropping Inter into res/font and pointing [Ledger] at it is the only change
 * needed to match exactly. Older note: the design used to call for Roboto Flex. No font file ships with the app, so this uses the
 * platform default, which is Roboto on Android — same family, fixed axes. Dropping a
 * variable Roboto Flex into res/font and pointing [Ledger] at it is the only change
 * needed to match exactly.
 */
private val Ledger = FontFamily.Default

/**
 * Tabular, lining figures. Without this, proportional digits make decimal points wander
 * from row to row and a column of amounts becomes unreadable.
 */
internal val TabularStyle = TextStyle(fontFeatureSettings = "tnum, lnum")

private const val TABULAR = "tnum, lnum"

internal fun ledgerTypography() = Typography(
    displayLarge = TextStyle(
        fontFamily = Ledger,
        fontSize = 48.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 56.sp,
        letterSpacing = (-0.25).sp,
        fontFeatureSettings = TABULAR
    ),
    displayMedium = TextStyle(
        fontFamily = Ledger,
        fontSize = 36.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 44.sp,
        letterSpacing = (-0.72).sp,
        fontFeatureSettings = TABULAR
    ),
    displaySmall = TextStyle(
        fontFamily = Ledger,
        fontSize = 30.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
        fontFeatureSettings = TABULAR
    ),
    headlineLarge = TextStyle(
        fontFamily = Ledger,
        fontSize = 32.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Ledger,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp,
        letterSpacing = (-0.24).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Ledger,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp,
        letterSpacing = (-0.24).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Ledger,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Ledger,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Ledger,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Ledger,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Ledger,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Ledger,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Ledger,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
        letterSpacing = (0.14).sp
    ),
    labelMedium = TextStyle(
        fontFamily = Ledger,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = (0.2).sp
    ),
    labelSmall = TextStyle(
        fontFamily = Ledger,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
        letterSpacing = (0.33).sp
    )
)
