package com.tranphuloi.neon.data

import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonPalette
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.NeonViolet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ColorBlindModeTest {

    @Test
    fun `enum has 2 modes`() {
        assertEquals(2, ColorBlindMode.entries.size)
    }

    @Test
    fun `fromKey roundtrips for every mode`() {
        ColorBlindMode.entries.forEach {
            assertEquals(it, ColorBlindMode.fromKey(it.key))
        }
    }

    @Test
    fun `fromKey returns NORMAL for null`() {
        assertEquals(ColorBlindMode.NORMAL, ColorBlindMode.fromKey(null))
    }

    @Test
    fun `fromKey returns NORMAL for unknown key`() {
        assertEquals(ColorBlindMode.NORMAL, ColorBlindMode.fromKey("not_a_mode"))
    }

    @Test
    fun `NeonPalette_NORMAL matches the top-level Neon constants`() {
        val p = NeonPalette.NORMAL
        assertEquals(NeonCyan, p.cyan)
        assertEquals(NeonMagenta, p.magenta)
        assertEquals(NeonViolet, p.violet)
        assertEquals(NeonGold, p.gold)
        assertEquals(NeonRedAlert, p.redAlert)
    }

    @Test
    fun `NeonPalette_COLORBLIND_SAFE differs from NORMAL on red-green channels`() {
        val normal = NeonPalette.NORMAL
        val cb = NeonPalette.COLORBLIND_SAFE
        // The most-affected channels under deuteranopia/protanopia are the
        // red-confusable hues (magenta, redAlert). These must change.
        assertNotEquals(normal.magenta, cb.magenta)
        assertNotEquals(normal.redAlert, cb.redAlert)
    }

    @Test
    fun `NeonPalette_COLORBLIND_SAFE has 5 distinct colors`() {
        val cb = NeonPalette.COLORBLIND_SAFE
        val all = listOf(cb.cyan, cb.magenta, cb.violet, cb.gold, cb.redAlert)
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun `NeonPalette presets are non-null`() {
        assertNotNull(NeonPalette.NORMAL)
        assertNotNull(NeonPalette.COLORBLIND_SAFE)
    }
}
