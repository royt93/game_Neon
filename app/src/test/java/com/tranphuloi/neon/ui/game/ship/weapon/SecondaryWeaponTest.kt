package com.tranphuloi.neon.ui.game.ship.weapon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecondaryWeaponTest {

    @Test
    fun `enum has 3 entries`() {
        assertEquals(3, SecondaryWeapon.entries.size)
    }

    @Test
    fun `every weapon has positive cooldown`() {
        SecondaryWeapon.entries.forEach {
            assertTrue("$it cooldown must be > 0", it.cooldownMs > 0L)
        }
    }

    @Test
    fun `every weapon has unique glyph`() {
        val glyphs = SecondaryWeapon.entries.map { it.glyph }
        assertEquals(glyphs.size, glyphs.toSet().size)
    }

    @Test
    fun `every weapon has non-empty displayName`() {
        SecondaryWeapon.entries.forEach {
            assertNotEquals("$it has empty displayName", "", it.displayName)
        }
    }

    @Test
    fun `cooldown ordering reflects power MISSILE less than MINE less than BURST`() {
        // Burst is instant + AoE → longest cooldown. Mine is delayed but big AoE
        // → middle. Missile is single homing → shortest.
        assertTrue(
            "expected MISSILE.cooldown < MINE.cooldown < BURST.cooldown",
            SecondaryWeapon.MISSILE.cooldownMs <
                SecondaryWeapon.MINE.cooldownMs &&
                SecondaryWeapon.MINE.cooldownMs <
                SecondaryWeapon.BURST.cooldownMs
        )
    }

    @Test
    fun `fromName roundtrips for every weapon`() {
        SecondaryWeapon.entries.forEach {
            assertEquals(it, SecondaryWeapon.fromName(it.name))
        }
    }

    @Test
    fun `fromName returns MISSILE for null`() {
        assertEquals(SecondaryWeapon.MISSILE, SecondaryWeapon.fromName(null))
    }

    @Test
    fun `fromName returns MISSILE for unknown name`() {
        assertEquals(SecondaryWeapon.MISSILE, SecondaryWeapon.fromName("WHATEVER"))
    }
}
