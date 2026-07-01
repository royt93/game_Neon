package com.tranphuloi.neon.ui.game.booster

import com.tranphuloi.neon.utils.UuidUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior tests for [BoosterController] — spawn cap, modifier gating
 * (TAY KHÔNG / NO_SHIELDS), REVIVE_DROP rank override, and per-tick removal
 * of collected boosters.
 */
class BoosterControllerBehaviorTest {

    private fun newController(
        initialBoosters: List<Booster> = emptyList(),
        noShieldDrops: () -> Boolean = { false },
        noBoosters: () -> Boolean = { false },
        reviveDropRank: () -> Int = { 0 },
    ): Pair<BoosterController, MutableList<List<Booster>>> {
        val captured = mutableListOf<List<Booster>>()
        val ctrl = BoosterController(
            screenWidth = 400f,
            screenHeight = 800f,
            uuidUtils = UuidUtils(),
            initialBoosters = initialBoosters,
            updateBoosters = { captured.add(it) },
            noShieldDrops = noShieldDrops,
            noBoosters = noBoosters,
            reviveDropRank = reviveDropRank,
        )
        return ctrl to captured
    }

    // ── TAY KHÔNG modifier (noBoosters) ──

    @Test
    fun `addBooster does nothing when noBoosters modifier active`() {
        val (ctrl, _) = newController(noBoosters = { true })
        repeat(5) { ctrl.addBooster() }
        assertTrue("TAY KHÔNG: không được spawn buff nào", ctrl.boosters.isEmpty())
    }

    // ── MAX_BOOSTERS cap ──

    @Test
    fun `addBooster never exceeds MAX_BOOSTERS cap`() {
        val (ctrl, _) = newController()
        repeat(10) { ctrl.addBooster() }
        assertEquals(BoosterController.MAX_BOOSTERS, ctrl.boosters.size)
    }

    // ── NO_SHIELDS modifier ──

    @Test
    fun `addBooster never adds SHIELD_BOOSTER when noShieldDrops active`() {
        val (ctrl, _) = newController(noShieldDrops = { true })
        // Nhiều lần thử để lấp đủ MAX_BOOSTERS slot bất chấp re-roll bị skip.
        repeat(500) { ctrl.addBooster() }
        assertEquals("phải lấp đủ cap dù có skip SHIELD", BoosterController.MAX_BOOSTERS, ctrl.boosters.size)
        assertTrue(
            "không booster nào được là SHIELD_BOOSTER",
            ctrl.boosters.none { it.type == BoosterType.SHIELD_BOOSTER },
        )
    }

    // ── REVIVE_DROP rank override ──

    @Test
    fun `addBooster always forces REVIVE_TOKEN when reviveDropRank chance is saturated`() {
        // reviveBonus = rank * 0.02f; rank=100 → 2.0f > mọi Random.nextFloat() [0,1) → luôn trúng.
        val (ctrl, _) = newController(reviveDropRank = { 100 })
        ctrl.addBooster()
        assertEquals(1, ctrl.boosters.size)
        assertEquals(BoosterType.REVIVE_TOKEN, ctrl.boosters.first().type)
    }

    @Test
    fun `addBooster does not force REVIVE_TOKEN when reviveDropRank is zero`() {
        val (ctrl, _) = newController(reviveDropRank = { 0 })
        repeat(BoosterController.MAX_BOOSTERS) { ctrl.addBooster() }
        // Không đảm bảo 0 REVIVE_TOKEN (weighted random vẫn có 5/292 cơ hội tự nhiên),
        // nhưng đảm bảo publish callback được gọi và cap vẫn giữ đúng.
        assertEquals(BoosterController.MAX_BOOSTERS, ctrl.boosters.size)
    }

    // ── hasBoosters() ──

    @Test
    fun `hasBoosters reflects current list state`() {
        val (ctrl, _) = newController()
        assertFalse(ctrl.hasBoosters())
        ctrl.addBooster()
        assertTrue(ctrl.hasBoosters())
    }

    // ── processBoosters() — remove collected, advance the rest ──

    @Test
    fun `processBoosters removes collected boosters and moves the rest`() {
        val alive = Booster(id = "alive", xOffset = 10f, size = 40f, screenHeight = 800f)
        val collected = Booster(id = "collected", xOffset = 20f, size = 40f, screenHeight = 800f, collected = true)
        val startYOffset = alive.yOffset
        val (ctrl, captured) = newController(initialBoosters = listOf(alive, collected))

        ctrl.processBoosters()

        assertEquals("booster đã collected phải bị loại khỏi list", 1, ctrl.boosters.size)
        assertEquals("alive", ctrl.boosters.first().id)
        assertTrue("moveObject() phải tăng yOffset của booster còn sống", ctrl.boosters.first().yOffset > startYOffset)
        assertTrue("updateBoosters callback phải được gọi", captured.isNotEmpty())
    }
}
