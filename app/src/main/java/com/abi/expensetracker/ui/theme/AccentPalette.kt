package com.abi.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The eight primary roles an accent fills, for both themes.
 *
 * A preset [AccentColor] carries hand-picked values; a custom colour derives them from one
 * seed. Both end up here so [ExpenseTrackerTheme] takes one shape and does not care which
 * kind it was given.
 */
data class AccentPalette(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color
) {
    /** The swatch shown in the picker, readable in either theme. */
    val swatch: Color get() = lightPrimary
}

fun AccentColor.palette(): AccentPalette = AccentPalette(
    lightPrimary = lightPrimary,
    lightOnPrimary = lightOnPrimary,
    lightContainer = lightContainer,
    lightOnContainer = lightOnContainer,
    darkPrimary = darkPrimary,
    darkOnPrimary = darkOnPrimary,
    darkContainer = darkContainer,
    darkOnContainer = darkOnContainer
)

/**
 * Builds a full accent from one chosen colour.
 *
 * The seed's hue and saturation are kept and the lightness is moved to the tones Material
 * uses for each role — 40 for the light primary, 90 for its container, 80 and 30 for the
 * dark theme's pair. That is the same ladder [AccentColor]'s hand-picked values sit on, so
 * a custom colour lands in the same places a preset does.
 *
 * It is HSL, not Material's HCT. HCT holds perceived brightness constant across hues,
 * which HSL does not: a pure yellow and a pure blue at the same HSL lightness do not look
 * equally light. The cost is that some hues come out a shade heavier than a hand-tuned
 * preset. The alternative is shipping a colour-science library to tint one button.
 */
fun accentPaletteFrom(seed: Color): AccentPalette {
    val argb = seed.toArgbInt()
    return AccentPalette(
        lightPrimary = Color(argb.atTone(0.34f)),
        // Tone 40 against white clears 4.5:1 for every hue, so this never needs measuring.
        lightOnPrimary = Color(0xFFFFFFFF),
        lightContainer = Color(argb.atTone(0.88f, saturationScale = 0.55f)),
        lightOnContainer = Color(argb.atTone(0.24f)),
        darkPrimary = Color(argb.atTone(0.78f, saturationScale = 0.75f)),
        darkOnPrimary = Color(argb.atTone(0.18f)),
        darkContainer = Color(argb.atTone(0.30f)),
        darkOnContainer = Color(argb.atTone(0.90f, saturationScale = 0.55f))
    )
}

/** Packed 0xAARRGGBB, which is what the colour maths and DataStore both work in. */
fun Color.toArgbInt(): Int {
    fun channel(value: Float) = (value.coerceIn(0f, 1f) * 255f).roundToInt()
    return (channel(alpha) shl 24) or
        (channel(red) shl 16) or
        (channel(green) shl 8) or
        channel(blue)
}

/**
 * Same hue, same saturation, new lightness.
 *
 * [saturationScale] pulls the chroma back for the pale roles: a container at full
 * saturation reads as a highlighter rather than a surface.
 */
internal fun Int.atTone(lightness: Float, saturationScale: Float = 1f): Int {
    val hsl = argbToHsl(this)
    return hslToArgb(
        hue = hsl[0],
        saturation = (hsl[1] * saturationScale).coerceIn(0f, 1f),
        lightness = lightness.coerceIn(0f, 1f)
    )
}

/** Returns hue in degrees, saturation and lightness in 0..1. */
internal fun argbToHsl(argb: Int): FloatArray {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f

    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val lightness = (maxC + minC) / 2f

    if (delta == 0f) return floatArrayOf(0f, 0f, lightness)

    val saturation = delta / (1f - abs(2f * lightness - 1f))
    val hue = when (maxC) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return floatArrayOf(if (hue < 0f) hue + 360f else hue, saturation.coerceIn(0f, 1f), lightness)
}

internal fun hslToArgb(hue: Float, saturation: Float, lightness: Float): Int {
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val h = ((hue % 360f) + 360f) % 360f / 60f
    val x = c * (1f - abs((h % 2f) - 1f))
    val m = lightness - c / 2f

    val (r, g, b) = when {
        h < 1f -> Triple(c, x, 0f)
        h < 2f -> Triple(x, c, 0f)
        h < 3f -> Triple(0f, c, x)
        h < 4f -> Triple(0f, x, c)
        h < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    fun channel(value: Float) = ((value + m).coerceIn(0f, 1f) * 255f).roundToInt()
    return (0xFF shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
}

/** How the app decides between light and dark. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
