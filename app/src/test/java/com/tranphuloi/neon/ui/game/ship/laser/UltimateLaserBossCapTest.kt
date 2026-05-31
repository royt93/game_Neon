package com.tranphuloi.neon.ui.game.ship.laser

import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 14b — pin the ChargeShot auto-ultimate boss-damage cap.
 *
 * User bug report: "1 loạt đạn màu vàng bắn lên từ bottom … quét sạch toàn
 * bộ enemy VÀ boss". The yellow beams are [UltimateLaser]. They sweep the
 * whole screen and are never destroyed on hit, so the Millis(1) collision
 * tick re-applied [UltimateLaser.impactPower] (1000) to a boss every frame
 * of overlap → bosses evaporated. The chosen fix (user pick): a boss takes a
 * small flat chip [UltimateLaser.BOSS_DAMAGE_PER_HIT] exactly ONCE per beam,
 * tracked via [UltimateLaser.hitBossIds]; regular mobs are unaffected.
 *
 * These tests pin the numeric invariants + the once-per-beam guard so a
 * future tweak that re-enables boss one-shotting breaks visibly.
 */
class UltimateLaserBossCapTest {

    private fun beam() = UltimateLaser(id = "u", xOffset = 0f, yRange = 100f)

    @Test
    fun `mob impact power is unchanged at 1000`() {
        // Mobs must still die instantly to the sweep — the cap is boss-only.
        assertEquals(1000f, beam().impactPower, 0f)
    }

    @Test
    fun `boss chip damage is much smaller than mob impact power`() {
        assertEquals(250f, UltimateLaser.BOSS_DAMAGE_PER_HIT, 0f)
        assertTrue(
            "Boss chip must be far below the mob impact power",
            UltimateLaser.BOSS_DAMAGE_PER_HIT < beam().impactPower / 2f,
        )
    }

    @Test
    fun `a single beam never one-shots even the weakest boss`() {
        // SWARM is the weakest mid-boss (verified by grep: min baseHp across
        // all MidBossType variants = 1200). MidBossType is a sealed class with
        // no registry list, so reference the known-weakest object directly; if
        // a future variant drops below it, the pin below surfaces the drift.
        val weakestBossHp = MidBossType.SWARM.baseHp
        assertEquals(1200f, weakestBossHp, 0f)
        assertTrue(
            "One ultimate chip (${UltimateLaser.BOSS_DAMAGE_PER_HIT}) must not " +
                "kill the weakest boss ($weakestBossHp)",
            UltimateLaser.BOSS_DAMAGE_PER_HIT < weakestBossHp,
        )
    }

    @Test
    fun `a full sweep's plausible overlap does not one-shot the weakest boss`() {
        // Spatially only ~2-3 of the 9 beams can overlap a single boss hitbox
        // at once. Even a generous 3-beam overlap (at base damage multiplier)
        // must leave the weakest boss alive so the fight is preserved.
        val weakestBossHp = MidBossType.SWARM.baseHp
        val generousOverlapDamage = 3 * UltimateLaser.BOSS_DAMAGE_PER_HIT
        assertTrue(
            "A 3-beam overlap ($generousOverlapDamage) must not one-shot the " +
                "weakest boss ($weakestBossHp)",
            generousOverlapDamage < weakestBossHp,
        )
    }

    @Test
    fun `hitBossIds guards against re-applying chip every collision tick`() {
        val beam = beam()
        // First overlap with this boss id → add returns true (apply chip).
        assertTrue("first overlap should register the boss", beam.hitBossIds.add("boss-1"))
        // Subsequent overlaps in later Millis(1) ticks → add returns false
        // (skip — this is what prevents the per-frame stacking that evaporated
        // bosses).
        assertFalse("second overlap must be ignored", beam.hitBossIds.add("boss-1"))
        assertFalse("third overlap must be ignored", beam.hitBossIds.add("boss-1"))
        // A different boss is still chipped once.
        assertTrue("a different boss is chipped once", beam.hitBossIds.add("boss-2"))
    }

    // ── Wave 14b audit fix — % -of-spawn-HP cap (bossChipDamage) ──

    @Test
    fun `chip is capped at 8 percent of spawn HP for a weak boss`() {
        // 0.08 × 1200 = 96 < flat 250 → the % cap dominates.
        assertEquals(96f, UltimateLaser.bossChipDamage(dmgMul = 1f, bossInitialHp = 1200f), 0.001f)
    }

    @Test
    fun `flat ceiling dominates for a very tanky boss`() {
        // 0.08 × 22500 = 1800 > flat 250 → the flat ceiling dominates so a
        // single beam never deletes a huge slice of the final boss either.
        assertEquals(250f, UltimateLaser.bossChipDamage(dmgMul = 1f, bossInitialHp = 22500f), 0.001f)
    }

    @Test
    fun `high damage multiplier can never exceed the 8 percent cap`() {
        // The edge the audit flagged: a ×10 damage run must NOT scale the chip
        // up past the spawn-HP fraction. min(250×10=2500, 0.08×1200=96) = 96.
        assertEquals(96f, UltimateLaser.bossChipDamage(dmgMul = 10f, bossInitialHp = 1200f), 0.001f)
    }

    @Test
    fun `a full sweep can never one-shot any boss regardless of upgrades`() {
        // Up to 3 beams overlap a boss; each is ≤ 8% of spawn HP, so a sweep
        // removes ≤ 24% < 100% for ANY boss HP at ANY damage multiplier.
        val hps = listOf(1200f, 1500f, 2500f, 3200f, 22500f)
        val multipliers = listOf(1f, 3f, 10f, 100f)
        hps.forEach { hp ->
            multipliers.forEach { mul ->
                val sweep = 3 * UltimateLaser.bossChipDamage(mul, hp)
                assertTrue(
                    "sweep $sweep must stay below spawn HP $hp at dmgMul $mul",
                    sweep < hp,
                )
                assertTrue("sweep must be ≤ 24% of spawn HP", sweep <= hp * 0.24f + 0.001f)
            }
        }
    }

    @Test
    fun `each beam tracks its own hit set independently`() {
        // Two beams of the same sweep must not share a hit set — otherwise the
        // first beam would block the rest from chipping the boss at all.
        val a = beam()
        val b = beam()
        assertTrue(a.hitBossIds.add("boss-1"))
        assertTrue(
            "a second beam must still be able to chip the same boss",
            b.hitBossIds.add("boss-1"),
        )
    }
}
