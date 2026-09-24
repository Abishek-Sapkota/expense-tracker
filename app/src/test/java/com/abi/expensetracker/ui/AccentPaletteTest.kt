package com.abi.expensetracker.ui

import com.abi.expensetracker.ui.theme.argbToHsl
import com.abi.expensetracker.ui.theme.atTone
import com.abi.expensetracker.ui.theme.hslToArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AccentPaletteTest {

    private fun rgb(argb: Int) = Triple(
        (argb shr 16) and 0xFF,
        (argb shr 8) and 0xFF,
        argb and 0xFF
    )

    /** Perceived lightness, used only to assert one colour is lighter than another. */
    private fun luminance(argb: Int): Double {
        val (r, g, b) = rgb(argb)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    @Test
    fun `hsl round trips a saturated colour`() {
        val original = 0xFF823521.toInt()
        val hsl = argbToHsl(original)
        val back = hslToArgb(hsl[0], hsl[1], hsl[2])

        val (r1, g1, b1) = rgb(original)
        val (r2, g2, b2) = rgb(back)
        // One unit of rounding either way is the most the 8-bit round trip can lose.
        assertTrue(abs(r1 - r2) <= 1 && abs(g1 - g2) <= 1 && abs(b1 - b2) <= 1)
    }

    @Test
    fun `grey has no hue and no saturation`() {
        val hsl = argbToHsl(0xFF808080.toInt())

        assertEquals(0f, hsl[0], 0.001f)
        assertEquals(0f, hsl[1], 0.001f)
        assertEquals(0.502f, hsl[2], 0.01f)
    }

    @Test
    fun `hue survives a change of tone`() {
        val seed = 0xFF2E7D32.toInt()
        val lightened = seed.atTone(0.8f)

        assertEquals(argbToHsl(seed)[0], argbToHsl(lightened)[0], 2f)
    }

    @Test
    fun `the container tone is lighter than the primary tone`() {
        val seed = 0xFF823521.toInt()

        assertTrue(luminance(seed.atTone(0.88f)) > luminance(seed.atTone(0.34f)))
    }

    @Test
    fun `the dark theme primary is lighter than the light theme primary`() {
        // Inverted on purpose: a dark surface needs a light accent to sit on it.
        val seed = 0xFF3A4A7A.toInt()

        assertTrue(luminance(seed.atTone(0.78f)) > luminance(seed.atTone(0.34f)))
    }

    @Test
    fun `tones are clamped rather than wrapped`() {
        val seed = 0xFF823521.toInt()

        assertEquals(0xFFFFFFFF.toInt(), seed.atTone(2f))
        assertEquals(0xFF000000.toInt(), seed.atTone(-1f))
    }

    @Test
    fun `pulling saturation back leaves the hue alone`() {
        val seed = 0xFF6C3A5C.toInt()
        val muted = seed.atTone(0.88f, saturationScale = 0.55f)

        assertEquals(argbToHsl(seed)[0], argbToHsl(muted)[0], 2f)
        assertTrue(argbToHsl(muted)[1] < argbToHsl(seed)[1])
    }

    @Test
    fun `every derived tone is fully opaque`() {
        val seed = 0xFF265456.toInt()

        listOf(0.24f, 0.34f, 0.78f, 0.88f).forEach { tone ->
            assertEquals(0xFF, (seed.atTone(tone) shr 24) and 0xFF)
        }
    }
}
