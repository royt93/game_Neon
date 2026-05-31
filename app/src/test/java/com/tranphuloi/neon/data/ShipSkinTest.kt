package com.tranphuloi.neon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShipSkinTest {

    @Test
    fun `enum has 8 aura color entries`() {
        assertEquals(8, ShipSkin.entries.size)
    }

    @Test
    fun `every skin has a unique key`() {
        val keys = ShipSkin.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `every skin has a unique glow color`() {
        val colors = ShipSkin.entries.map { it.glowColorHex }
        assertEquals(colors.size, colors.toSet().size)
    }

    @Test
    fun `every skin has non-empty display name`() {
        ShipSkin.entries.forEach {
            assertNotEquals("$it has empty displayName", "", it.displayName)
        }
    }

    @Test
    fun `glow colors are fully opaque ARGB`() {
        ShipSkin.entries.forEach {
            // High byte (alpha) must be 0xFF for fully opaque rendering.
            val alpha = (it.glowColorHex ushr 24) and 0xFF
            assertEquals("$it alpha should be 0xFF", 0xFFL, alpha)
        }
    }

    @Test
    fun `fromKey roundtrips for every skin`() {
        ShipSkin.entries.forEach {
            assertEquals(it, ShipSkin.fromKey(it.key))
        }
    }

    @Test
    fun `fromKey returns AURA_CYAN for null`() {
        assertEquals(ShipSkin.AURA_CYAN, ShipSkin.fromKey(null))
    }

    @Test
    fun `fromKey returns AURA_CYAN for unknown key`() {
        assertEquals(ShipSkin.AURA_CYAN, ShipSkin.fromKey("not_a_skin"))
    }

    @Test
    fun `fromKey migrates old regular and boosted keys to AURA_CYAN`() {
        // Round 38 migration: old persisted keys silently fall back.
        assertEquals(ShipSkin.AURA_CYAN, ShipSkin.fromKey("regular"))
        assertEquals(ShipSkin.AURA_CYAN, ShipSkin.fromKey("boosted"))
    }
}
