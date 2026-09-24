package com.abi.expensetracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The neutral stack for one theme: every surface, hairline and text tone that is not the
 * accent or a money colour.
 *
 * [fill] is what the nav bar, month switcher, tiles, chips and dialogs sit on; [card] is
 * the grouped-list card, set apart from the page by [hairline].
 */
data class NeutralTones(
    val page: Color,
    val card: Color,
    val fill: Color,
    val fillHigh: Color,
    val fillHighest: Color,
    val hairline: Color,
    val text: Color,
    val muted: Color
)

/**
 * User-selectable background palettes, the neutral counterpart to [AccentColor].
 *
 * Only neutrals change. The accent is chosen separately, and debit/credit colours are fixed
 * because they carry meaning.
 */
enum class NeutralPalette(val label: String, val light: NeutralTones, val dark: NeutralTones) {
    GREY(
        "Neutral grey",
        light = NeutralTones(
            page = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF), fill = Color(0xFFF4F4F5),
            fillHigh = Color(0xFFE4E4E7), fillHighest = Color(0xFFD4D4D8), hairline = Color(0xFFE4E4E7),
            text = Color(0xFF18181B), muted = Color(0xFF71717A)
        ),
        dark = NeutralTones(
            page = Color(0xFF0A0A0A), card = Color(0xFF18181B), fill = Color(0xFF27272A),
            fillHigh = Color(0xFF3F3F46), fillHighest = Color(0xFF52525B), hairline = Color(0xFF27272A),
            text = Color(0xFFFAFAFA), muted = Color(0xFFA1A1AA)
        )
    ),
    SLATE(
        "Cool slate",
        light = NeutralTones(
            page = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF), fill = Color(0xFFF1F5F9),
            fillHigh = Color(0xFFE2E8F0), fillHighest = Color(0xFFCBD5E1), hairline = Color(0xFFE2E8F0),
            text = Color(0xFF0F172A), muted = Color(0xFF64748B)
        ),
        dark = NeutralTones(
            page = Color(0xFF020617), card = Color(0xFF0F172A), fill = Color(0xFF1E293B),
            fillHigh = Color(0xFF334155), fillHighest = Color(0xFF475569), hairline = Color(0xFF1E293B),
            text = Color(0xFFF1F5F9), muted = Color(0xFF94A3B8)
        )
    ),
    STONE(
        "Warm stone",
        light = NeutralTones(
            page = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF), fill = Color(0xFFF5F3EE),
            fillHigh = Color(0xFFEFEEEB), fillHighest = Color(0xFFE9E8E5), hairline = Color(0xFFE6E3DC),
            text = Color(0xFF191C1D), muted = Color(0xFF706E69)
        ),
        dark = NeutralTones(
            page = Color(0xFF121212), card = Color(0xFF1E1E1E), fill = Color(0xFF242424),
            fillHigh = Color(0xFF2A2A2A), fillHighest = Color(0xFF333333), hairline = Color(0xFF2E2E2E),
            text = Color(0xFFE3E2DE), muted = Color(0xFF9E9C96)
        )
    ),
    MIST(
        "Mist",
        light = NeutralTones(
            page = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF), fill = Color(0xFFF2F5F3),
            fillHigh = Color(0xFFE3E9E5), fillHighest = Color(0xFFCFD8D2), hairline = Color(0xFFE3E9E5),
            text = Color(0xFF16201B), muted = Color(0xFF66736B)
        ),
        dark = NeutralTones(
            page = Color(0xFF0B0F0D), card = Color(0xFF151B18), fill = Color(0xFF1E2622),
            fillHigh = Color(0xFF2B3530), fillHighest = Color(0xFF3A4640), hairline = Color(0xFF1E2622),
            text = Color(0xFFE8EFEA), muted = Color(0xFF9AA8A0)
        )
    ),
    WHITE(
        "White only",
        light = NeutralTones(
            page = Color(0xFFFFFFFF), card = Color(0xFFFFFFFF), fill = Color(0xFFFFFFFF),
            fillHigh = Color(0xFFF5F5F5), fillHighest = Color(0xFFE5E5E5), hairline = Color(0xFFE5E5E5),
            text = Color(0xFF171717), muted = Color(0xFF737373)
        ),
        dark = NeutralTones(
            page = Color(0xFF000000), card = Color(0xFF0A0A0A), fill = Color(0xFF0A0A0A),
            fillHigh = Color(0xFF171717), fillHighest = Color(0xFF262626), hairline = Color(0xFF262626),
            text = Color(0xFFFAFAFA), muted = Color(0xFFA3A3A3)
        )
    );

    companion object {
        val DEFAULT = GREY

        fun fromName(name: String?): NeutralPalette = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Maps a palette's tones onto Material roles; accent roles are filled in by the theme. */
internal fun NeutralTones.toScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        background = page,
        onBackground = text,
        surface = page,
        onSurface = text,
        surfaceVariant = fillHigh,
        onSurfaceVariant = muted,
        surfaceDim = if (dark) page else fillHigh,
        surfaceBright = if (dark) fillHigh else page,
        surfaceContainerLowest = card,
        surfaceContainerLow = fill,
        surfaceContainer = fill,
        surfaceContainerHigh = fillHigh,
        surfaceContainerHighest = fillHighest,
        inverseSurface = if (dark) text else Color(0xFF27272A),
        inverseOnSurface = if (dark) page else Color(0xFFFAFAFA),
        outline = muted,
        outlineVariant = hairline,
        secondary = muted,
        onSecondary = page,
        secondaryContainer = fillHigh,
        onSecondaryContainer = text,
        tertiary = if (dark) Color(0xFF88D982) else Color(0xFF186A22),
        onTertiary = if (dark) Color(0xFF003909) else Color(0xFFFFFFFF),
        tertiaryContainer = if (dark) Color(0xFF005312) else Color(0xFFA3F69C),
        onTertiaryContainer = if (dark) Color(0xFFA3F69C) else Color(0xFF005312),
        error = if (dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
        onError = if (dark) Color(0xFF690005) else Color(0xFFFFFFFF),
        errorContainer = if (dark) Color(0xFF93000A) else Color(0xFFFFDAD6),
        onErrorContainer = if (dark) Color(0xFFFFDAD6) else Color(0xFF93000A)
    )
}
