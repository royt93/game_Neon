package com.tranphuloi.neon.ui.game.ship.shape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 12 (đợt 4) — mastery passive: map 22 tàu đủ + độc nhất; activeFor khoá
 * dưới max level, mở tại max level; magnitude hợp lệ.
 */
class ShipPassiveTest {

    @Test
    fun `mỗi ShipShape map tới đúng 1 passive`() {
        ShipShape.values().forEach { s ->
            assertNotNull("ship $s phải có passive", ShipPassive.forShip(s))
        }
    }

    @Test
    fun `22 tàu cho 22 passive ĐỘC NHẤT`() {
        val passives = ShipShape.values().map { ShipPassive.forShip(it) }
        assertEquals("mỗi tàu 1 passive", ShipShape.values().size, passives.size)
        assertEquals("passive không trùng giữa các tàu", passives.size, passives.toSet().size)
    }

    @Test
    fun `activeFor KHOÁ khi dưới max level`() {
        for (lvl in 1 until ShipXpLevels.MAX_LEVEL) {
            assertNull("level $lvl < max ⇒ passive khoá", ShipPassive.activeFor(ShipShape.FIGHTER, lvl))
        }
    }

    @Test
    fun `activeFor MỞ đúng passive tại max level`() {
        val p = ShipPassive.activeFor(ShipShape.TANK, ShipXpLevels.MAX_LEVEL)
        assertEquals("TANK max level ⇒ passive của TANK", ShipPassive.forShip(ShipShape.TANK), p)
    }

    @Test
    fun `mọi passive có magnitude dương`() {
        ShipPassive.values().forEach {
            assertTrue("${it.name} magnitude phải > 0", it.magnitude > 0f)
        }
    }

    @Test
    fun `có đủ cả stat lẫn hook trong 22 passive`() {
        val effects = ShipPassive.values().map { it.effect }.toSet()
        assertTrue("có passive stat", effects.any { it.isStat })
        assertTrue("có passive hook (non-stat)", effects.any { !it.isStat })
    }
}
