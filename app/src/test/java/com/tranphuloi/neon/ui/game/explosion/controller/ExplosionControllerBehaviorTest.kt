package com.tranphuloi.neon.ui.game.explosion.controller

import com.tranphuloi.neon.ui.game.explosion.model.Explosion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior tests for [ExplosionController] — spawn transform, [ExplosionController.MAX_ACTIVE]
 * cap (drop oldest), and expiry-driven removal in [ExplosionController.processExplosions].
 *
 * [Explosion] has no time-injection point (`process()` reads `System.currentTimeMillis()`
 * directly), so the expiry test uses a real `Thread.sleep` past the 450ms lifetime.
 */
class ExplosionControllerBehaviorTest {

    private fun newController(
        initial: List<Explosion> = emptyList(),
    ): Pair<ExplosionController, MutableList<List<Explosion>>> {
        val captured = mutableListOf<List<Explosion>>()
        val ctrl = ExplosionController(
            initialExplosions = initial,
            updateExplosions = { captured.add(it) },
        )
        return ctrl to captured
    }

    // ── addExplosion() spawn transform ──

    @Test
    fun `addExplosion centers explosion on impact point using the larger of width or height`() {
        val (ctrl, captured) = newController()
        ctrl.addExplosion(xOffset = 100f, yOffset = 100f, width = 10f, height = 20f)

        val explosion = captured.last().single()
        assertEquals("size phải là max(width, height)", 40f, explosion.size)
        assertEquals("xOffset lệch để center: x - size", 80f, explosion.xOffset)
        assertEquals("yOffset lệch để center: y - size", 80f, explosion.yOffset)
    }

    // ── MAX_ACTIVE cap ──

    @Test
    fun `addExplosion caps active list at MAX_ACTIVE and drops the oldest`() {
        val (ctrl, captured) = newController()
        repeat(10) { i -> ctrl.addExplosion(xOffset = i * 100f, yOffset = 0f, width = 10f, height = 10f) }

        val finalList = captured.last()
        assertEquals(ExplosionController.MAX_ACTIVE, finalList.size)
        assertEquals(
            "2 explosion cũ nhất (index 0,1) phải bị drop, sống sót đầu tiên là index 2",
            200f - 10f,
            finalList.first().xOffset,
        )
    }

    // ── processExplosions() expiry ──

    @Test
    fun `processExplosions does not touch or republish explosions still within their lifetime`() {
        val (ctrl, captured) = newController()
        ctrl.addExplosion(xOffset = 0f, yOffset = 0f, width = 10f, height = 10f)
        val callsAfterSpawn = captured.size

        ctrl.processExplosions()

        assertEquals("chưa hết 450ms → không được publish thêm lần nào", callsAfterSpawn, captured.size)
    }

    @Test
    fun `processExplosions removes explosions once their lifetime has elapsed`() {
        val (ctrl, captured) = newController()
        ctrl.addExplosion(xOffset = 0f, yOffset = 0f, width = 10f, height = 10f)

        Thread.sleep(500L) // vượt qua endTimeMillis = startTimeMillis + 450, không flaky.
        ctrl.processExplosions()

        assertTrue("explosion hết hạn phải bị loại và publish list rỗng", captured.last().isEmpty())
    }

    @Test
    fun `processExplosions is a no-op when there are no explosions`() {
        val (ctrl, captured) = newController()
        ctrl.processExplosions()
        assertTrue("không có explosion nào thì không được gọi updateExplosions", captured.isEmpty())
    }
}
