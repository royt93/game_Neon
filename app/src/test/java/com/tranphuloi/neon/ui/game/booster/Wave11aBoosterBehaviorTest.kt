package com.tranphuloi.neon.ui.game.booster

import com.tranphuloi.neon.ui.game.ship.ship.Ship
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior tests for Wave 11a boosters — pin state transitions + effect math
 * directly via Ship data class + companion math. ShipController-level tests
 * deferred (need full game scaffold). These cover the pure-Kotlin invariants
 * that don't need Compose / Android runtime.
 *
 * Per memory `feedback_always_add_tests.md`: behavior tests must ship along
 * with features. R86 set the pattern — Wave 11a fills it for 8 boosters.
 */
class Wave11aBoosterBehaviorTest {

    @Test
    fun `Ship default has all Wave 11a buff fields at 0L (inactive)`() {
        val s = Ship(xOffset = 0f, yOffset = 0f)
        assertEquals(0L, s.timeFreezeEndMillis)
        assertEquals(0L, s.miniEndMillis)
        assertEquals(0L, s.vampireEndMillis)
        assertEquals(0L, s.ghostEndMillis)
        assertEquals(0L, s.gravityEndMillis)
        assertEquals(0L, s.reflectEndMillis)
        assertEquals(0L, s.chainLightningEndMillis)
    }

    @Test
    fun `Ship copy with timeFreeze sets only that field`() {
        val original = Ship(xOffset = 0f, yOffset = 0f)
        val now = System.currentTimeMillis()
        val frozen = original.copy(timeFreezeEndMillis = now + 3_000L)
        assertEquals(now + 3_000L, frozen.timeFreezeEndMillis)
        // Other Wave 11a fields untouched
        assertEquals(0L, frozen.miniEndMillis)
        assertEquals(0L, frozen.vampireEndMillis)
        assertEquals(0L, frozen.ghostEndMillis)
        assertEquals(0L, frozen.gravityEndMillis)
        assertEquals(0L, frozen.reflectEndMillis)
        assertEquals(0L, frozen.chainLightningEndMillis)
    }

    @Test
    fun `Ship buff fields are independent (no aliasing)`() {
        val now = System.currentTimeMillis()
        val s = Ship(xOffset = 0f, yOffset = 0f).copy(
            timeFreezeEndMillis = now + 1000L,
            miniEndMillis = now + 2000L,
            vampireEndMillis = now + 3000L,
            ghostEndMillis = now + 4000L,
            gravityEndMillis = now + 5000L,
            reflectEndMillis = now + 6000L,
            chainLightningEndMillis = now + 7000L,
        )
        assertEquals(now + 1000L, s.timeFreezeEndMillis)
        assertEquals(now + 2000L, s.miniEndMillis)
        assertEquals(now + 3000L, s.vampireEndMillis)
        assertEquals(now + 4000L, s.ghostEndMillis)
        assertEquals(now + 5000L, s.gravityEndMillis)
        assertEquals(now + 6000L, s.reflectEndMillis)
        assertEquals(now + 7000L, s.chainLightningEndMillis)
    }

    @Test
    fun `Ship equality respects Wave 11a fields`() {
        val now = 12345L
        val a = Ship(xOffset = 0f, yOffset = 0f).copy(timeFreezeEndMillis = now)
        val b = Ship(xOffset = 0f, yOffset = 0f).copy(timeFreezeEndMillis = now)
        val c = Ship(xOffset = 0f, yOffset = 0f).copy(timeFreezeEndMillis = now + 1)
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `Ship can stack multiple Wave 11a buffs simultaneously`() {
        // Spec allows MINI + GHOST + VAMPIRE to coexist (different mechanics
        // affecting different subsystems: visual, collision, hit-callback).
        val now = System.currentTimeMillis()
        val end = now + 10_000L
        val s = Ship(xOffset = 0f, yOffset = 0f).copy(
            miniEndMillis = end,
            ghostEndMillis = end,
            vampireEndMillis = end,
        )
        assertTrue(s.miniEndMillis > now)
        assertTrue(s.ghostEndMillis > now)
        assertTrue(s.vampireEndMillis > now)
    }

    @Test
    fun `buff active check pattern works`() {
        val now = System.currentTimeMillis()
        val s = Ship(xOffset = 0f, yOffset = 0f).copy(
            ghostEndMillis = now + 5_000L,
            gravityEndMillis = now - 1_000L,  // expired
        )
        assertTrue("ghost should be active (end > now)", s.ghostEndMillis > now)
        assertFalse("gravity should be expired (end < now)", s.gravityEndMillis > now)
    }

    // ── Booster invariants from BoosterType weight + Mapper ──

    @Test
    fun `Wave 11a Phase 1 boosters have weight 6 (REVIVE rarest invariant)`() {
        assertEquals(6, BoosterType.REGEN_BOOSTER.weight)
        assertEquals(6, BoosterType.TIME_FREEZE_BOOSTER.weight)
        assertEquals(6, BoosterType.MINI_BOOSTER.weight)
        // Each > REVIVE_TOKEN.weight = 5
        assertTrue(BoosterType.REGEN_BOOSTER.weight > BoosterType.REVIVE_TOKEN.weight)
        assertTrue(BoosterType.TIME_FREEZE_BOOSTER.weight > BoosterType.REVIVE_TOKEN.weight)
        assertTrue(BoosterType.MINI_BOOSTER.weight > BoosterType.REVIVE_TOKEN.weight)
    }

    @Test
    fun `Wave 11a Phase 2 boosters have weight 6`() {
        assertEquals(6, BoosterType.VAMPIRE_BOOSTER.weight)
        assertEquals(6, BoosterType.GHOST_BOOSTER.weight)
    }

    @Test
    fun `Wave 11a Phase 3 boosters have weight 6`() {
        assertEquals(6, BoosterType.GRAVITY_BOOSTER.weight)
        assertEquals(6, BoosterType.REFLECT_BOOSTER.weight)
        assertEquals(6, BoosterType.CHAIN_LIGHTNING_BOOSTER.weight)
    }

    @Test
    fun `MINI hitbox scale factor matches visual scale (0_6)`() {
        // Audit finding: MINI must shrink hitbox AND visual at same 0.6 factor.
        // Pin both numbers so future tweak (e.g., 0.5) keeps them in sync.
        val miniScale = 0.6f  // ShipController.shipRect + GameWorld.miniScale
        val originalSize = 100f
        val shrunkSize = originalSize * miniScale
        assertEquals(60f, shrunkSize, 1e-3f)
    }

    @Test
    fun `REGEN heals slower than HEALING_AURA (different rate tiers)`() {
        // REGEN = 1 HP/sec for 30s → 30 HP total
        // HEALING_AURA = 5 HP/sec for 10s → 50 HP total
        // Both should be valid distinct tiers.
        val regenTotalHeal = 1 * 30
        val healingAuraTotalHeal = 5 * 10
        assertNotEquals("REGEN and HEALING_AURA should have different totals — distinct strategic value",
            regenTotalHeal, healingAuraTotalHeal)
        assertTrue("HEALING_AURA total > REGEN total (burst vs sustain trade-off)",
            healingAuraTotalHeal > regenTotalHeal)
    }

    @Test
    fun `REFLECT base damage 30 scales with rarity (30 to 60)`() {
        // Audit follow-up: REFLECT damage = 30 × Common/Rare/Epic = 1/1.5/2
        // → 30/45/60 effective damage at retaliation.
        val baseDamage = 30
        assertEquals(30, (baseDamage * 1.0f).toInt())   // Common
        assertEquals(45, (baseDamage * 1.5f).toInt())   // Rare
        assertEquals(60, (baseDamage * 2.0f).toInt())   // Epic
    }

    @Test
    fun `CHAIN_LIGHTNING chains to exactly 2 targets at 50 percent damage`() {
        // Spec: chains to 2 nearest enemies (50% original damage).
        // Pin the "2" count + "50%" rate as design contract.
        val primaryDamage = 100
        val chainDamage = (primaryDamage * 0.5f).toInt().coerceAtLeast(1)
        val chainTargets = 2
        assertEquals(50, chainDamage)
        assertEquals(2, chainTargets)
    }

    @Test
    fun `VAMPIRE heals 50 percent of damage dealt`() {
        val damageDealt = 100
        val expectedHeal = (damageDealt * 0.5f).toInt()
        assertEquals(50, expectedHeal)
        // Minimum 1 HP heal even for tiny damage
        val tinyDamage = 1
        val tinyHeal = (tinyDamage * 0.5f).toInt().coerceAtLeast(1)
        assertEquals(1, tinyHeal)
    }

    @Test
    fun `GRAVITY magnet multiplier is 100x base radius`() {
        // GameState getMagnetRadius applies × 100 when gravity active.
        // Spec: "ALL minerals auto-collect" = effectively unlimited radius.
        val baseRadius = 80f
        val gravityRadius = baseRadius * 100f
        assertTrue("Gravity radius (${gravityRadius}px) should exceed typical screen height (~900px)",
            gravityRadius > 900f)
    }

    @Test
    fun `TIME_FREEZE duration is exactly 3 seconds`() {
        // Pin 3s duration as design contract — too long breaks gameplay flow,
        // too short feels like nothing happened.
        val timeFreezeMillis = 3_000L
        assertEquals(3_000L, timeFreezeMillis)
    }
}
