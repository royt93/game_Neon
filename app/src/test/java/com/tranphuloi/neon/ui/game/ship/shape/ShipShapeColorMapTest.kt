package com.tranphuloi.neon.ui.game.ship.shape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins [ShipShapeColorMap] semantics so DialogShipPicker and InfoScreen
 * stay in lockstep. Was two mirrored `when` tables that drifted on
 * `CHET_CHOC` (0xFF606060 vs 0xFF808080) + `TU_THAN` (0xFF000000 vs
 * 0xFF404040) — invariant test prevents regression.
 */
class ShipShapeColorMapTest {

    @Test
    fun `every ShipShape has a fully-opaque accent color (alpha = 0xFF)`() {
        for (shape in ShipShape.entries) {
            val argb = ShipShapeColorMap.argbFor(shape)
            val alpha = (argb ushr 24) and 0xFFL
            assertEquals(
                "ShipShape $shape accent color must be fully opaque, got alpha=0x${alpha.toString(16)}",
                0xFFL, alpha,
            )
        }
    }

    @Test
    fun `every ShipShape returns a non-zero ARGB (exhaustive when guard)`() {
        // Calling argbFor for every entry must not throw — exhaustive when
        // returns Long for each branch. If a new ShipShape is added without
        // declaring an accent color, this test fails at compile.
        for (shape in ShipShape.entries) {
            val argb = ShipShapeColorMap.argbFor(shape)
            assertTrue(
                "ShipShape $shape returned suspicious zero ARGB (note: TU_THAN is 0xFF000000 not 0x00000000)",
                argb != 0L,
            )
        }
    }

    @Test
    fun `R86 audit drift fix — CHET_CHOC = 0xFF606060 (gray reaper steel)`() {
        assertEquals(0xFF606060L, ShipShapeColorMap.argbFor(ShipShape.CHET_CHOC))
    }

    @Test
    fun `R86 audit drift fix — TU_THAN = 0xFF000000 (black void)`() {
        assertEquals(0xFF000000L, ShipShapeColorMap.argbFor(ShipShape.TU_THAN))
    }

    @Test
    fun `base 5 ships use theme palette colors`() {
        // Sanity-check that the 5 founding ships reference the Neon theme
        // palette (cyan/gold/violet/orange/magenta). Hex values pinned so a
        // future palette tweak doesn't accidentally shift the picker UI.
        assertEquals(0xFF00F0FFL, ShipShapeColorMap.argbFor(ShipShape.FIGHTER))     // NeonCyan
        assertEquals(0xFFFFCB47L, ShipShapeColorMap.argbFor(ShipShape.BOMBER))      // NeonGold
        assertEquals(0xFFB14CFFL, ShipShapeColorMap.argbFor(ShipShape.STEALTH))     // NeonViolet
        assertEquals(0xFFFF6020L, ShipShapeColorMap.argbFor(ShipShape.TANK))        // orange
        assertEquals(0xFFFF2DE0L, ShipShapeColorMap.argbFor(ShipShape.INTERCEPTOR)) // NeonMagenta
    }
}
