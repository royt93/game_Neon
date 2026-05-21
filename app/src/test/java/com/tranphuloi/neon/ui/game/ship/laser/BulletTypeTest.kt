package com.tranphuloi.neon.ui.game.ship.laser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BulletTypeTest {

    @Test
    fun `NORMAL is the inert default`() {
        val n = BulletType.NORMAL
        assertEquals(0L, n.activeDurationMillis)
        assertEquals(1f, n.damageMultiplier, 0f)
        assertEquals(0, n.pierceCount)
        assertEquals(0f, n.aoeRadius, 0f)
    }

    @Test
    fun `PIERCING pierces up to 3 enemies at base damage`() {
        val p = BulletType.PIERCING
        assertEquals(3, p.pierceCount)
        assertEquals(1f, p.damageMultiplier, 0f)
        assertEquals(0f, p.aoeRadius, 0f)
        assertTrue("timed booster must have positive duration", p.activeDurationMillis > 0L)
    }

    @Test
    fun `PLASMA has AoE radius and damage multiplier`() {
        val p = BulletType.PLASMA
        assertEquals(0, p.pierceCount)
        assertTrue("plasma should be >100% damage", p.damageMultiplier > 1f)
        assertTrue("plasma should have AoE", p.aoeRadius > 0f)
        assertTrue("timed booster must have positive duration", p.activeDurationMillis > 0L)
    }

    @Test
    fun `each type has a unique glyph`() {
        val glyphs = BulletType.entries.map { it.glyph }
        assertEquals(glyphs.size, glyphs.toSet().size)
    }

    @Test
    fun `non-NORMAL types differ from NORMAL on at least one combat axis`() {
        BulletType.entries
            .filter { it != BulletType.NORMAL }
            .forEach { t ->
                val diff = t.damageMultiplier != BulletType.NORMAL.damageMultiplier ||
                        t.pierceCount != BulletType.NORMAL.pierceCount ||
                        t.aoeRadius != BulletType.NORMAL.aoeRadius
                assertTrue("$t is indistinguishable from NORMAL on combat axes", diff)
            }
    }

    @Test
    fun `display names are non-empty`() {
        BulletType.entries.forEach {
            assertNotEquals("$it has empty displayName", "", it.displayName)
        }
    }
}
