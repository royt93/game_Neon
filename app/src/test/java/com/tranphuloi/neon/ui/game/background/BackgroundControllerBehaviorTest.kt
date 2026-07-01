package com.tranphuloi.neon.ui.game.background

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior tests for [BackgroundController] — verifies parallax entity spawn
 * counts, twinkle-only-near-layer perf rule, and comet spawn interval gating.
 */
class BackgroundControllerBehaviorTest {

    private fun newController(): Pair<BackgroundController, MutableList<BackgroundState>> {
        val captured = mutableListOf<BackgroundState>()
        val ctrl = BackgroundController(
            screenWidth = 400f,
            screenHeight = 800f,
            updateState = { captured.add(it) },
        )
        return ctrl to captured
    }

    // ── Star spawn ──

    @Test
    fun `init spawns 53 stars across 5 layers (20+14+10+6+3)`() {
        val (ctrl, captured) = newController()
        ctrl.init()
        assertEquals("tổng số star phải đúng 53", 53, captured.last().stars.size)
    }

    @Test
    fun `only layer 4 (nearest) stars twinkle`() {
        val (ctrl, captured) = newController()
        ctrl.init()
        val stars = captured.last().stars
        stars.forEach { star ->
            if (star.layer == 4) {
                assertTrue("layer 4 phải twinkle", star.twinkles)
            } else {
                assertFalse("layer ${star.layer} KHÔNG được twinkle (perf rule)", star.twinkles)
            }
        }
        assertTrue("phải có ít nhất 1 star layer 4", stars.any { it.layer == 4 })
    }

    @Test
    fun `init spawns 15 dust particles and 4 nebula blobs`() {
        val (ctrl, captured) = newController()
        ctrl.init()
        val state = captured.last()
        assertEquals(15, state.dust.size)
        assertEquals("4 nebula blob cố định (3 màu + 1 warm top-right)", 4, state.nebula.size)
    }

    @Test
    fun `galaxy is disabled (replaced by warm nebula)`() {
        val (ctrl, captured) = newController()
        ctrl.init()
        assertNull("galaxy phải null — đã bị thay bằng nebula ấm", captured.last().galaxy)
    }

    // ── Comet spawn interval (rare event, 6-12s) ──

    @Test
    fun `comet is null immediately after init (min spawn delay 6s)`() {
        val (ctrl, captured) = newController()
        ctrl.init()
        assertNull("comet không xuất hiện ngay sau init", captured.last().comet)
    }

    @Test
    fun `comet stays null across several ticks right after init`() {
        val (ctrl, captured) = newController()
        ctrl.init()
        repeat(5) { ctrl.tick() }
        assertNull("comet vẫn null trong vài tick đầu vì spawn window tối thiểu 6000ms", captured.last().comet)
    }

    @Test
    fun `tick always republishes state`() {
        val (ctrl, captured) = newController()
        ctrl.init()
        val countAfterInit = captured.size
        ctrl.tick()
        assertTrue("tick() phải gọi updateState thêm lần nữa", captured.size > countAfterInit)
    }
}
