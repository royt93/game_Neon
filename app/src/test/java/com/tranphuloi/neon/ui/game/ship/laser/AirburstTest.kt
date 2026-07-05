package com.tranphuloi.neon.ui.game.ship.laser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Task 15 (đợt 4) — vận tốc đạn con Airburst: 8 con, quạt hướng LÊN, toả trái↔phải.
 */
class AirburstTest {

    @Test
    fun `mặc định 8 con`() {
        assertEquals(8, Airburst.velocities().size)
    }

    @Test
    fun `mọi con có vy dương (bay lên) để cull off-top`() {
        Airburst.velocities().forEach { (_, vy) ->
            assertTrue("vy phải > 0 (nếu không sẽ leak vì cull chỉ off-top)", vy > 0f)
        }
    }

    @Test
    fun `toả quạt — có con lệch phải, lệch trái, và gần thẳng`() {
        val vxs = Airburst.velocities().map { it.first }
        assertTrue("có con lệch phải", vxs.any { it > 1f })
        assertTrue("có con lệch trái", vxs.any { it < -1f })
        assertTrue("có con gần thẳng đứng", vxs.any { abs(it) < 1.5f })
    }

    @Test
    fun `count tùy biến`() {
        assertEquals(4, Airburst.velocities(4).size)
        assertEquals(12, Airburst.velocities(12).size)
    }
}
