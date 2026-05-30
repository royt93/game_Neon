package com.tranphuloi.neon.ui.game.stage

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 11d audit-5 P2 fix — pin EndlessProvider escalation caps so a long
 * Endless run never produces mathematically-unplayable enemies. Pre-fix
 * `1.10^30` etc. had no cap; cycle=30 produced HP×17.4 / impact×10 / speed×4.3.
 *
 * Caps chosen so cycle=30 hits a finite ceiling:
 *   HP cap     5.0  — reached around cycle 17
 *   impact cap 2.5  — reached around cycle 12
 *   speed cap  2.5  — reached around cycle 19
 *   spawn cap  0.5  — reached around cycle 14 (FLOOR — spawn cadence min)
 */
class EndlessEscalationTest {

    private val provider = EndlessProvider()

    @Test
    fun `cycle 0 returns unscaled base stage`() {
        // Cycle 0 short-circuits in getAt() — verify the base passes through.
        val s0 = provider.getAt(0)
        val s0Base = s0 as StageGame
        val t = s0Base.enemyType as RegularEnemyType
        // Reference: chapter1GameStages[0] tier=0 width=40, hp from family
        // multiplier. We don't pin exact values (drift if Stage.kt is tuned)
        // but verify cycle-0 path returns a regular enemy type unchanged.
        assertTrue("Cycle 0 baseline hp > 0", t.hp > 0f)
        assertTrue("Cycle 0 baseline impact > 0", t.impactPower > 0f)
    }

    @Test
    fun `cycle 5 escalation visible but under cap`() {
        // cycle=5: hp×1.61, impact×1.47, speed×1.28, spawn×0.77
        val base = provider.getAt(0) as StageGame
        val c5 = provider.getAt(5 * chapter1GameStages.size) as StageGame
        val baseT = base.enemyType as RegularEnemyType
        val c5T = c5.enemyType as RegularEnemyType
        assertTrue("HP grew", c5T.hp > baseT.hp)
        assertTrue("Impact grew", c5T.impactPower > baseT.impactPower)
        assertTrue("Speed grew", c5T.yOffsetSpeed > baseT.yOffsetSpeed)
        // All under caps at cycle 5
        assertTrue("HP under 5×", c5T.hp < baseT.hp * 5.0f)
        assertTrue("Impact under 2.5×", c5T.impactPower < baseT.impactPower * 2.5f)
        assertTrue("Speed under 2.5×", c5T.yOffsetSpeed < baseT.yOffsetSpeed * 2.5f)
    }

    @Test
    fun `cycle 30 hits all caps (regression — pre-fix would have hp×17 etc)`() {
        val base = provider.getAt(0) as StageGame
        val c30 = provider.getAt(30 * chapter1GameStages.size) as StageGame
        val baseT = base.enemyType as RegularEnemyType
        val c30T = c30.enemyType as RegularEnemyType

        // HP capped at 5.0× — pre-fix 1.10^30 = 17.45×
        assertEquals(
            "HP capped at 5× — if higher, escalation cap missing/broken",
            baseT.hp * 5.0f, c30T.hp, 0.01f,
        )
        // Impact capped at 2.5× — pre-fix 1.08^30 = 10.06×
        assertEquals(
            "Impact capped at 2.5× — if higher, escalation cap missing/broken",
            baseT.impactPower * 2.5f, c30T.impactPower, 0.01f,
        )
        // Speed capped at 2.5× — pre-fix 1.05^30 = 4.32×
        assertEquals(
            "Speed capped at 2.5× — if higher, escalation cap missing/broken",
            baseT.yOffsetSpeed * 2.5f, c30T.yOffsetSpeed, 0.01f,
        )
        // Spawn rate FLOORED at 0.5× — pre-fix 0.95^30 = 0.21×
        val baseRate = (baseT.enemySpawnRate as Millis).timeMillis
        val c30Rate = (c30T.enemySpawnRate as Millis).timeMillis
        // Expected floor = baseRate × 0.5 (also coerceAtLeast(300) in the
        // copy block; pick whichever is greater).
        val expectedFloor = (baseRate * 0.5f).toInt().coerceAtLeast(300)
        assertEquals(
            "Spawn rate floored at 0.5× of base (or 300ms min) — if lower, regression",
            expectedFloor, c30Rate,
        )
    }

    @Test
    fun `cycle 100 stays capped (no late-game blowup)`() {
        // Defensive — at extreme cycle, caps still hold. Math.pow(1.10, 100)
        // is astronomical (~13780×) and would have produced absurd values
        // pre-cap. After cap, behaves identical to cycle 30.
        val base = provider.getAt(0) as StageGame
        val c100 = provider.getAt(100 * chapter1GameStages.size) as StageGame
        val baseT = base.enemyType as RegularEnemyType
        val c100T = c100.enemyType as RegularEnemyType
        assertEquals(baseT.hp * 5.0f, c100T.hp, 0.01f)
        assertEquals(baseT.impactPower * 2.5f, c100T.impactPower, 0.01f)
        assertEquals(baseT.yOffsetSpeed * 2.5f, c100T.yOffsetSpeed, 0.01f)
    }

    @Test
    fun `monotonic — higher cycle never produces weaker stats`() {
        val base = provider.getAt(0) as StageGame
        val baseT = base.enemyType as RegularEnemyType
        var prevHp = baseT.hp
        var prevImpact = baseT.impactPower
        var prevSpeed = baseT.yOffsetSpeed
        for (cycle in 1..30) {
            val s = provider.getAt(cycle * chapter1GameStages.size) as StageGame
            val t = s.enemyType as RegularEnemyType
            assertTrue("cycle=$cycle HP regressed", t.hp >= prevHp)
            assertTrue("cycle=$cycle impact regressed", t.impactPower >= prevImpact)
            assertTrue("cycle=$cycle speed regressed", t.yOffsetSpeed >= prevSpeed)
            prevHp = t.hp
            prevImpact = t.impactPower
            prevSpeed = t.yOffsetSpeed
        }
    }
}
