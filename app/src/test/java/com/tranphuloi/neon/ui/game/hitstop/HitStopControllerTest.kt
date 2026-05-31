package com.tranphuloi.neon.ui.game.hitstop

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 16 — pin the boss-intro freeze added to [HitStopController].
 *
 * The full-screen cinematic intro reuses the existing loop gate
 * (`isFrozen()` at the top of the game loop) to truly pause the simulation.
 * These tests use the injectable `now` parameter on [HitStopController.isFrozen]
 * so they're deterministic (no Thread.sleep).
 */
class HitStopControllerTest {

    @Test
    fun `freezeForBossIntro keeps the sim frozen for the full duration`() {
        val c = HitStopController()
        // Deadline is set from System.currentTimeMillis() (= "now + dur"); probe
        // it via the injectable `now` parameter relative to the call time.
        val before = System.currentTimeMillis()
        c.freezeForBossIntro(HitStopController.BOSS_INTRO_FREEZE_MS)
        // Frozen right now.
        assertTrue("must be frozen immediately after freeze call", c.isFrozen())
        // Frozen just before the deadline.
        assertTrue(
            "must still be frozen near the end of the window",
            c.isFrozen(before + HitStopController.BOSS_INTRO_FREEZE_MS - 50L),
        )
        // Unfrozen comfortably after the deadline.
        assertFalse(
            "must be unfrozen after the window elapses",
            c.isFrozen(before + HitStopController.BOSS_INTRO_FREEZE_MS + 1000L),
        )
    }

    @Test
    fun `endBossIntroFreeze unfreezes immediately (skip)`() {
        val c = HitStopController()
        c.freezeForBossIntro(HitStopController.BOSS_INTRO_FREEZE_MS)
        assertTrue(c.isFrozen())
        c.endBossIntroFreeze()
        assertFalse("skip must end the freeze at once", c.isFrozen())
    }

    @Test
    fun `boss-intro freeze is far longer than the micro hit-stops`() {
        // Guards the "feel" intent: the cinematic hold must dwarf the kill/hit
        // freezes (else the intro would resume mid-animation).
        assertTrue(
            HitStopController.BOSS_INTRO_FREEZE_MS > HitStopController.BOSS_FREEZE_MS * 5,
        )
        assertTrue(
            HitStopController.BOSS_INTRO_FREEZE_MS > HitStopController.ENEMY_FREEZE_MS * 10,
        )
    }

    @Test
    fun `a stale micro hit-stop does not shorten the boss-intro freeze`() {
        val c = HitStopController()
        c.freezeForEnemyKill()                       // short 60ms freeze
        c.freezeForBossIntro(HitStopController.BOSS_INTRO_FREEZE_MS)  // hard SET, longer
        val before = System.currentTimeMillis()
        assertTrue(
            "boss-intro freeze must win over a prior micro freeze",
            c.isFrozen(before + HitStopController.BOSS_FREEZE_MS + 500L),
        )
    }
}
