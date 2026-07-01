package com.tranphuloi.neon.ui.game.mineral.controller

import com.tranphuloi.neon.ui.game.mineral.model.Mineral
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior tests for [MineralsController] — magnet-radius pickup logic,
 * consumePickedThisTick bookkeeping, and MINERAL_SUPERCHARGE flush.
 */
class MineralsControllerBehaviorTest {

    private fun newController(
        shipCenter: Pair<Float, Float> = 0f to 0f,
        magnetRadius: Float = 0f,
    ): Triple<MineralsController, MutableList<List<Mineral>>, MutableList<Int>> {
        val updates = mutableListOf<List<Mineral>>()
        val earned = mutableListOf<Int>()
        val ctrl = MineralsController(
            initialMinerals = emptyList(),
            updateMinerals = { updates.add(it) },
            updateMineralsEarnedTotal = { earned.add(it) },
            getShipCenter = { shipCenter },
            getMagnetRadius = { magnetRadius },
        )
        return Triple(ctrl, updates, earned)
    }

    @Test
    fun `addMinerals publishes new mineral and awards amount`() {
        val (ctrl, updates, earned) = newController()
        ctrl.addMinerals(xOffset = 10f, yOffset = 10f, width = 20f, mineralAmount = 3)
        assertEquals(1, updates.last().size)
        assertEquals(listOf(3), earned)
    }

    // ── Magnet-radius pickup ──

    @Test
    fun `processMinerals picks up mineral within pickup distance of ship`() {
        val (ctrl, updates, _) = newController(shipCenter = 100f to 100f, magnetRadius = 50f)
        ctrl.addMinerals(xOffset = 100f, yOffset = 100f, width = 20f, mineralAmount = 1)

        ctrl.processMinerals()

        assertTrue("mineral trùng vị trí ship phải bị hút và biến mất", updates.last().isEmpty())
        assertEquals("phải ghi nhận 1 lượt picked", 1, ctrl.consumePickedThisTick())
    }

    @Test
    fun `processMinerals leaves mineral outside magnet radius drifting normally`() {
        val (ctrl, updates, _) = newController(shipCenter = 100f to 100f, magnetRadius = 0f)
        ctrl.addMinerals(xOffset = 100f, yOffset = 500f, width = 20f, mineralAmount = 1)

        ctrl.processMinerals()

        assertEquals("mineral vẫn còn trên màn (chưa hút, chưa hết đường)", 1, updates.last().size)
        assertEquals(0, ctrl.consumePickedThisTick())
    }

    @Test
    fun `consumePickedThisTick resets counter after read`() {
        val (ctrl, _, _) = newController(shipCenter = 5f to 5f, magnetRadius = 50f)
        ctrl.addMinerals(xOffset = 5f, yOffset = 5f, width = 20f, mineralAmount = 1)
        ctrl.processMinerals()
        assertEquals(1, ctrl.consumePickedThisTick())
        assertEquals("đọc lần 2 phải về 0", 0, ctrl.consumePickedThisTick())
    }

    // ── flushAllToShip (MINERAL_SUPERCHARGE) ──

    @Test
    fun `flushAllToShip instant-collects all minerals and awards bonus`() {
        val (ctrl, updates, earned) = newController()
        ctrl.addMinerals(xOffset = 0f, yOffset = 0f, width = 20f, mineralAmount = 1)
        ctrl.addMinerals(xOffset = 0f, yOffset = 0f, width = 20f, mineralAmount = 1)

        ctrl.flushAllToShip()

        assertTrue("tất cả mineral bị thu ngay lập tức", updates.last().isEmpty())
        assertEquals("bonus = count * 5 = 10", 10, earned.last())
        assertEquals("consumePickedThisTick phải phản ánh 2 mineral vừa flush", 2, ctrl.consumePickedThisTick())
    }

    @Test
    fun `flushAllToShip is a no-op when there are no minerals`() {
        val (ctrl, updates, earned) = newController()
        ctrl.flushAllToShip()
        assertTrue("không có mineral → không publish gì", updates.isEmpty())
        assertTrue(earned.isEmpty())
    }
}
