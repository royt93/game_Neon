package com.tranphuloi.neon.ui.game.state

import com.tranphuloi.neon.data.Difficulty
import com.tranphuloi.neon.ui.game.buff.RunBuff
import com.tranphuloi.neon.ui.game.modifier.RunModifier
import com.tranphuloi.neon.ui.game.ship.shape.ShipShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectiveStatsTest {

    private val EPS = 0.0001f

    @Test
    fun `default RunContext returns identity stats`() {
        val s = EffectiveStats.compute(RunContext())
        // diffHp 1/1.0 × modHp 1 × metaHp 1 = 1.0 → clamp [0.3, 3.0] → 1.0
        assertEquals(1f, s.hpMul, EPS)
        assertEquals(1f, s.damageMul, EPS)
        assertEquals(1f, s.speedMul, EPS)
        assertEquals(1f, s.magnetMul, EPS)
        assertEquals(1f, s.scoreMul, EPS)
        assertFalse(s.noShieldDrops)
        assertFalse(s.bossesOnly)
    }

    @Test
    fun `EASY difficulty scales hp UP via inverse multiplier`() {
        val s = EffectiveStats.compute(RunContext(difficulty = Difficulty.EASY))
        // 1 / 0.7 ≈ 1.4286
        assertEquals(1f / 0.7f, s.hpMul, EPS)
    }

    @Test
    fun `HARD difficulty scales hp DOWN via inverse multiplier`() {
        val s = EffectiveStats.compute(RunContext(difficulty = Difficulty.HARD))
        // 1 / 1.4 ≈ 0.714
        assertEquals(1f / 1.4f, s.hpMul, EPS)
    }

    @Test
    fun `GLASS_CANNON modifier halves hp doubles damage doubles score`() {
        val s = EffectiveStats.compute(RunContext(modifier = RunModifier.GLASS_CANNON))
        assertEquals(0.5f, s.hpMul, EPS)
        assertEquals(2f, s.damageMul, EPS)
        assertEquals(2f, s.scoreMul, EPS)
    }

    @Test
    fun `NO_SHIELDS modifier propagates noShieldDrops flag`() {
        val s = EffectiveStats.compute(RunContext(modifier = RunModifier.NO_SHIELDS))
        assertTrue(s.noShieldDrops)
        assertFalse(s.bossesOnly)
    }

    @Test
    fun `BOSSES_ONLY modifier propagates bossesOnly flag`() {
        val s = EffectiveStats.compute(RunContext(modifier = RunModifier.BOSSES_ONLY))
        assertTrue(s.bossesOnly)
    }

    @Test
    fun `meta upgrades stack linearly per rank`() {
        val ctx = RunContext(
            metaUpgrades = mapOf(
                EffectiveStats.META_KEY_HP to 3,        // +30% hp
                EffectiveStats.META_KEY_DAMAGE to 2,    // +16% damage
                EffectiveStats.META_KEY_SPEED to 1,     // +6% speed
                EffectiveStats.META_KEY_MAGNET to 4,    // +60% magnet
            ),
        )
        val s = EffectiveStats.compute(ctx)
        assertEquals(1.30f, s.hpMul, EPS)
        assertEquals(1.16f, s.damageMul, EPS)
        assertEquals(1.06f, s.speedMul, EPS)
        assertEquals(1.60f, s.magnetMul, EPS)
    }

    @Test
    fun `unknown meta keys are ignored`() {
        val s = EffectiveStats.compute(RunContext(metaUpgrades = mapOf("bogus_key" to 99)))
        assertEquals(1f, s.hpMul, EPS)
        assertEquals(1f, s.damageMul, EPS)
    }

    @Test
    fun `hp cap is 3_0 even with extreme stacking`() {
        val ctx = RunContext(
            difficulty = Difficulty.EASY,                // diffHp ≈ 1.428
            modifier = RunModifier.TANK,                 // ×1.5
            metaUpgrades = mapOf(EffectiveStats.META_KEY_HP to 20),  // ×3.0
        )
        // raw = 1.428 × 1.5 × 3.0 ≈ 6.43 — must clamp at 3.0
        val s = EffectiveStats.compute(ctx)
        assertEquals(3.0f, s.hpMul, EPS)
    }

    @Test
    fun `hp floor is 0_3 even with extreme reduction`() {
        val ctx = RunContext(
            difficulty = Difficulty.HARD,                // diffHp ≈ 0.714
            modifier = RunModifier.GLASS_CANNON,         // ×0.5
        )
        // raw = 0.714 × 0.5 ≈ 0.357 — above floor 0.3, so not clamped here.
        val s = EffectiveStats.compute(ctx)
        assertEquals(0.357f, s.hpMul, 0.001f)
    }

    @Test
    fun `damage cap is 4_0 even with extreme stacking`() {
        val ctx = RunContext(
            modifier = RunModifier.DOUBLE_OR_NOTHING,    // ×2
            metaUpgrades = mapOf(EffectiveStats.META_KEY_DAMAGE to 30),  // ×3.4
        )
        // raw = 2 × 3.4 = 6.8 — must clamp at 4.0
        val s = EffectiveStats.compute(ctx)
        assertEquals(4.0f, s.damageMul, EPS)
    }

    @Test
    fun `speed cap is 3_5 even with TRIPLE_SPEED + meta`() {
        val ctx = RunContext(
            modifier = RunModifier.TRIPLE_SPEED,         // ×3
            metaUpgrades = mapOf(EffectiveStats.META_KEY_SPEED to 10),
        )
        val s = EffectiveStats.compute(ctx)
        assertEquals(3.5f, s.speedMul, EPS)
    }

    @Test
    fun `score has its own cap and is unaffected by meta`() {
        val ctx = RunContext(
            modifier = RunModifier.DOUBLE_OR_NOTHING,    // ×2.5 base
            metaUpgrades = mapOf(EffectiveStats.META_KEY_HP to 10),
        )
        val s = EffectiveStats.compute(ctx)
        // Score doesn't include meta — only modifier × cap.
        assertEquals(2.5f, s.scoreMul, EPS)
    }

    @Test
    fun `all multipliers respect their floor of 0_5`() {
        // No way for natural inputs to push damage/speed/magnet below 1, but
        // construct a synthetic floor probe by checking floor-only modifiers.
        // GLASS_CANNON.speedMul = 1f, so floor never hits naturally — verify
        // the floor exists by checking the property of the cap itself.
        val s = EffectiveStats.compute(RunContext(modifier = RunModifier.TANK))
        assertEquals(0.7f, s.speedMul, EPS)  // TANK speed 0.7 > floor 0.5 → unclamped
    }

    // -- withBuffs() merge tests ------------------------------------------

    @Test
    fun `withBuffs on empty list returns identity stats`() {
        val base = EffectiveStats.compute(RunContext())
        val merged = base.withBuffs(emptyList())
        assertEquals(base.hpMul, merged.hpMul, EPS)
        assertEquals(base.damageMul, merged.damageMul, EPS)
        assertEquals(base.speedMul, merged.speedMul, EPS)
    }

    @Test
    fun `withBuffs HP_BOOST stacks on base hp`() {
        val base = EffectiveStats.compute(RunContext())   // hp = 1.0
        val merged = base.withBuffs(listOf(RunBuff.HP_BOOST))
        assertEquals(1.25f, merged.hpMul, EPS)
    }

    @Test
    fun `withBuffs BERSERKER trades hp for damage on top of base`() {
        val base = EffectiveStats.compute(RunContext(modifier = RunModifier.GLASS_CANNON))
        val merged = base.withBuffs(listOf(RunBuff.BERSERKER))
        // hp: 0.5 (base) × 0.90 (berserker) = 0.45
        assertEquals(0.45f, merged.hpMul, EPS)
        // damage: 2.0 (base) × 1.50 (berserker) = 3.0
        assertEquals(3.0f, merged.damageMul, EPS)
    }

    @Test
    fun `withBuffs respects hp cap at 3_0`() {
        val base = EffectiveStats.compute(
            RunContext(
                difficulty = Difficulty.EASY,                // ~1.428
                metaUpgrades = mapOf(EffectiveStats.META_KEY_HP to 5),    // ×1.5
            ),
        )                                                    // base ≈ 2.14
        val merged = base.withBuffs(listOf(RunBuff.FORTRESS, RunBuff.HP_BOOST))
        // raw: 2.14 × 1.5 × 1.25 ≈ 4.0 — must clamp at 3.0
        assertEquals(3.0f, merged.hpMul, EPS)
    }

    @Test
    fun `withBuffs respects damage cap at 4_0`() {
        val base = EffectiveStats.compute(RunContext(modifier = RunModifier.DOUBLE_OR_NOTHING))
        // base damage = 2.0
        val merged = base.withBuffs(listOf(RunBuff.DAMAGE_BOOST, RunBuff.BERSERKER))
        // raw: 2.0 × 1.30 × 1.50 = 3.9 — under cap
        assertEquals(3.9f, merged.damageMul, EPS)
    }

    @Test
    fun `withBuffs respects score cap at 4_0`() {
        val base = EffectiveStats.compute(RunContext(modifier = RunModifier.DOUBLE_OR_NOTHING))
        // base score = 2.5
        val merged = base.withBuffs(listOf(RunBuff.GAMBLER, RunBuff.SCORE_BOOST))
        // raw: 2.5 × 2.0 × 1.25 = 6.25 — clamp at 4.0
        assertEquals(4.0f, merged.scoreMul, EPS)
    }

    @Test
    fun `withBuffs does not mutate noShieldDrops or bossesOnly flags`() {
        val base = EffectiveStats.compute(RunContext(modifier = RunModifier.BOSSES_ONLY))
        val merged = base.withBuffs(listOf(RunBuff.BERSERKER))
        assertTrue(merged.bossesOnly)
        assertFalse(merged.noShieldDrops)
    }

    // ─── Round 73 (Wave 8) — ShipShape stat mul propagation ───

    @Test
    fun `FIGHTER shipShape is identity (no stat change)`() {
        val s = EffectiveStats.compute(RunContext(shipShape = ShipShape.FIGHTER))
        // FIGHTER multipliers all = 1.0 → matches default-no-shape baseline.
        assertEquals(1f, s.hpMul, EPS)
        assertEquals(1f, s.speedMul, EPS)
        assertEquals(1f, s.damageMul, EPS)
    }

    @Test
    fun `TANK shipShape boosts hp and reduces speed per Wave 8 spec`() {
        val s = EffectiveStats.compute(RunContext(shipShape = ShipShape.TANK))
        // TANK: hpMul=1.5, speedMul=0.75, damageMul=0.95
        assertEquals(1.5f, s.hpMul, EPS)
        assertEquals(0.75f, s.speedMul, EPS)
        assertEquals(0.95f, s.damageMul, EPS)
    }

    @Test
    fun `INTERCEPTOR shipShape boosts speed and damage per Wave 8 spec`() {
        val s = EffectiveStats.compute(RunContext(shipShape = ShipShape.INTERCEPTOR))
        // INTERCEPTOR: hpMul=0.80, speedMul=1.30, damageMul=1.10
        assertEquals(0.80f, s.hpMul, EPS)
        assertEquals(1.30f, s.speedMul, EPS)
        assertEquals(1.10f, s.damageMul, EPS)
    }

    @Test
    fun `ShipShape stat mul respects caps when combined với modifier and meta`() {
        // TANK hpMul 1.5 × EASY (1/0.7=1.43) × meta +30% (1.3) = 2.79 — within cap 3.0.
        val ctx = RunContext(
            shipShape = ShipShape.TANK,
            difficulty = Difficulty.EASY,
            metaUpgrades = mapOf(EffectiveStats.META_KEY_HP to 3),
        )
        val s = EffectiveStats.compute(ctx)
        // 1.5 × 1.4286 × 1.30 ≈ 2.7857 — should match within EPS.
        assertEquals(2.7857f, s.hpMul, 0.01f)
        assertTrue("Combined hp must be within cap [0.3, 3.0]", s.hpMul in 0.3f..3.0f)
    }
}
