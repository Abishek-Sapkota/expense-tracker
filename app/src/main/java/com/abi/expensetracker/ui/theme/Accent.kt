package com.abi.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Accent choices for the app.
 *
 * Only the primary role changes. The neutral clay surfaces, the typography and the
 * debit/credit colors stay fixed, because those carry meaning — swapping them per accent
 * would make "red means money out" depend on a cosmetic setting.
 *
 * Terracotta is the Utilitarian Ledger default: the spec's #C85A32 / #D86B43 accent,
 * deepened one step in light so white text on it stays legible.
 */
enum class AccentColor(
    val label: String,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color
) {
    TERRACOTTA(
        label = "Terracotta",
        lightPrimary = Color(0xFFA8431D),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFFFDBCF),
        lightOnContainer = Color(0xFF822801),
        darkPrimary = Color(0xFFD86B43),
        darkOnPrimary = Color(0xFFFFFFFF),
        darkContainer = Color(0xFF5C2410),
        darkOnContainer = Color(0xFFFFDBCF)
    ),
    SAGE(
        label = "Sage",
        lightPrimary = Color(0xFF44603A),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFDCE7C7),
        lightOnContainer = Color(0xFF404A33),
        darkPrimary = Color(0xFFC0CBAC),
        darkOnPrimary = Color(0xFF2A331E),
        darkContainer = Color(0xFF404A33),
        darkOnContainer = Color(0xFFDCE7C7)
    ),
    TEAL(
        label = "Slate teal",
        lightPrimary = Color(0xFF265456),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFBCEBED),
        lightOnContainer = Color(0xFF1F4D50),
        darkPrimary = Color(0xFFA0CFD1),
        darkOnPrimary = Color(0xFF00373A),
        darkContainer = Color(0xFF1F4D50),
        darkOnContainer = Color(0xFFBCEBED)
    ),
    INDIGO(
        label = "Indigo",
        lightPrimary = Color(0xFF3A4A7A),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFDCE1FF),
        lightOnContainer = Color(0xFF334272),
        darkPrimary = Color(0xFFB6C4FF),
        darkOnPrimary = Color(0xFF1F2E5E),
        darkContainer = Color(0xFF334272),
        darkOnContainer = Color(0xFFDCE1FF)
    ),
    PLUM(
        label = "Plum",
        lightPrimary = Color(0xFF6C3A5C),
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(0xFFFFD7F0),
        lightOnContainer = Color(0xFF633455),
        darkPrimary = Color(0xFFFFACE0),
        darkOnPrimary = Color(0xFF4F1E42),
        darkContainer = Color(0xFF633455),
        darkOnContainer = Color(0xFFFFD7F0)
    );

    /** The swatch shown in the picker, readable in either theme. */
    val swatch: Color get() = lightPrimary

    companion object {
        val DEFAULT = TERRACOTTA

        fun fromName(name: String?): AccentColor =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
