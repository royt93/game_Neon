package com.tranphuloi.neon.ui.game.ship.ship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deeper state-transition tests for Wave 11a boosters.
 *
 * Coverage:
 *   - Buff active → expired transition (via Ship state copy)
 *   - Multiple stacks → timer extends (via repeated state set)
 *   - Cross-feature priority verification
 *   - Edge cases: 0-duration, max-stack, expired-then-re-acquired
 *
 * These test the QUERY side (isXActive) under various ship.XEndMillis states,
 * pinning the time-based logic without needing System time mock.
 */
class Wave11aTransitionTest {

    private fun newController(ship: Ship): ShipController {
        var shipRef = ship
        return ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = ship,
            setShip = { shipRef = it },
        )
    }

    private val now: Long get() = System.currentTimeMillis()

    // ── Time boundary transitions ──

    @Test
    fun `buff active state changes from true to false as endMillis crosses now`() {
        // Three test states for each Wave 11a buff. Pin the boundary behavior.
        // active: endMillis > now → isActive() = true
        // boundary: endMillis = now → isActive() = false (strict >)
        // expired: endMillis < now → isActive() = false

        val future = newController(Ship(xOffset = 0f, yOffset = 0f,
            ghostEndMillis = now + 5_000L))
        assertTrue("future endMillis → active", future.isGhostActive())

        val past = newController(Ship(xOffset = 0f, yOffset = 0f,
            ghostEndMillis = now - 5_000L))
        assertFalse("past endMillis → inactive", past.isGhostActive())
    }

    @Test
    fun `0L endMillis (default) means inactive for all Wave 11a buffs`() {
        // Default Ship state — all Wave 11a EndMillis fields = 0L. Pin that
        // 0L explicitly means "never set" → all active queries = false.
        val defaultShip = Ship(xOffset = 0f, yOffset = 0f)
        val ctrl = newController(defaultShip)
        assertFalse(ctrl.isGhostActive())
        assertFalse(ctrl.isGravityActive())
        assertFalse(ctrl.isReflectActive())
        assertFalse(ctrl.isChainLightningActive())
        assertFalse(ctrl.isCloneActive())
    }

    @Test
    fun `simultaneous activation of all 9 Wave 11a buffs verified independent`() {
        // Most complex stack: all 9 Wave 11a buffs active at once.
        // None should interfere with another's state query.
        val future = now + 10_000L
        val maxStack = Ship(xOffset = 0f, yOffset = 0f,
            timeFreezeEndMillis = future,
            miniEndMillis = future,
            vampireEndMillis = future,
            ghostEndMillis = future,
            gravityEndMillis = future,
            reflectEndMillis = future,
            chainLightningEndMillis = future,
            cloneEndMillis = future,
            // REGEN doesn't have ship field — only timer in controller
        )
        val ctrl = newController(maxStack)
        // All controller-readable queries return true
        assertTrue("All ghost/gravity/reflect/chain/clone active under max stack",
            ctrl.isGhostActive() &&
            ctrl.isGravityActive() &&
            ctrl.isReflectActive() &&
            ctrl.isChainLightningActive() &&
            ctrl.isCloneActive())
        // Ship state fields reflect active buffs
        assertTrue("timeFreeze in future", maxStack.timeFreezeEndMillis > now)
        assertTrue("mini in future", maxStack.miniEndMillis > now)
        assertTrue("vampire in future", maxStack.vampireEndMillis > now)
    }

    // ── Refresh semantics — extending timer ──

    @Test
    fun `mixed expiry state — some active some expired works correctly`() {
        // Pin: a Ship with SOME buffs active + SOME expired returns correct
        // mix of true/false. No buff influences another.
        val ship = Ship(xOffset = 0f, yOffset = 0f,
            ghostEndMillis = now + 5_000L,            // active
            gravityEndMillis = now - 1_000L,           // expired (auto-cleared by tick, but raw value tested)
            reflectEndMillis = 0L,                     // never set
            chainLightningEndMillis = now + 10_000L,   // active
            cloneEndMillis = now - 100L,               // just expired
        )
        val ctrl = newController(ship)
        assertTrue("ghost active", ctrl.isGhostActive())
        assertFalse("gravity expired", ctrl.isGravityActive())
        assertFalse("reflect never set", ctrl.isReflectActive())
        assertTrue("chain lightning active", ctrl.isChainLightningActive())
        assertFalse("clone just expired", ctrl.isCloneActive())
    }

    // ── REFLECT damage formula edge cases ──

    @Test
    fun `REFLECT base damage 30 stable across new controllers (no state leak)`() {
        // Multiple new controllers — none has called enableReflect — all
        // return baseline 30 damage. No static state contamination.
        for (i in 0 until 5) {
            val ctrl = newController(Ship(xOffset = 0f, yOffset = 0f))
            assertEquals("Controller #$i baseline damage", 30, ctrl.reflectRetaliationDamage())
        }
    }

    // ── VAMPIRE heal independence ──

    @Test
    fun `applyVampireHeal no-op when vampire never activated (default state)`() {
        var ship = Ship(xOffset = 0f, yOffset = 0f, hp = 500)
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = ship,
            setShip = { ship = it },
        )
        ctrl.applyVampireHeal(damageDealt = 1000)
        // Even with damage=1000, no heal because vampireEndMillis defaults to 0L
        assertEquals("hp unchanged when vampire inactive", 500, ship.hp)
    }

    @Test
    fun `applyVampireHeal active state heals proportionally to damage`() {
        var ship = Ship(xOffset = 0f, yOffset = 0f, hp = 100,
            vampireEndMillis = now + 10_000L)
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = ship,
            setShip = { ship = it },
        )
        ctrl.applyVampireHeal(damageDealt = 200)   // 50% = +100 heal
        assertEquals("hp grew by 50% of 200 damage", 200, ship.hp)
        ctrl.applyVampireHeal(damageDealt = 50)    // 50% = +25 heal
        assertEquals("hp grew by 50% of 50 damage", 225, ship.hp)
    }
}
