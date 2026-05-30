package com.tranphuloi.neon.ui.game.stage

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EndlessProvider escalation caps + (Wave 13d) softened curve & theme rotation.
 *
 * Wave 13d retuned the caps DOWN so stage 100+ isn't instant-death (on-device
 * log showed ~6-hit deaths at the old caps). Current caps:
 *   HP     × 1.08^c cap 4.0
 *   impact × 1.05^c cap 1.8   ← main instant-death fix
 *   speed  × 1.03^c cap 1.6
 *   spawn  × 0.96^c floor 0.55 (or 300ms hard min, whichever greater)
 */
class EndlessEscalationTest {

    private val provider = EndlessProvider()

    @Test
    fun `cycle 0 returns unscaled base stage`() {
        val s0 = provider.getAt(0) as StageGame
        val t = s0.enemyType as RegularEnemyType
        assertTrue("Cycle 0 baseline hp > 0", t.hp > 0f)
        assertTrue("Cycle 0 baseline impact > 0", t.impactPower > 0f)
    }

    @Test
    fun `cycle 5 escalation visible but under cap`() {
        val baseT = (provider.getAt(0) as StageGame).enemyType as RegularEnemyType
        val c5T = (provider.getAt(5 * chapter1GameStages.size) as StageGame).enemyType as RegularEnemyType
        assertTrue("HP grew", c5T.hp > baseT.hp)
        assertTrue("Impact grew", c5T.impactPower > baseT.impactPower)
        assertTrue("Speed grew", c5T.yOffsetSpeed > baseT.yOffsetSpeed)
        assertTrue("HP under 4×", c5T.hp <= baseT.hp * 4.0f)
        assertTrue("Impact under 1.8×", c5T.impactPower <= baseT.impactPower * 1.8f)
        assertTrue("Speed under 1.6×", c5T.yOffsetSpeed <= baseT.yOffsetSpeed * 1.6f)
    }

    @Test
    fun `cycle 30 hits the softened caps`() {
        val baseT = (provider.getAt(0) as StageGame).enemyType as RegularEnemyType
        val c30T = (provider.getAt(30 * chapter1GameStages.size) as StageGame).enemyType as RegularEnemyType

        assertEquals("HP capped at 4×", baseT.hp * 4.0f, c30T.hp, 0.01f)
        assertEquals("Impact capped at 1.8× (instant-death fix)", baseT.impactPower * 1.8f, c30T.impactPower, 0.01f)
        assertEquals("Speed capped at 1.6×", baseT.yOffsetSpeed * 1.6f, c30T.yOffsetSpeed, 0.01f)

        val baseRate = (baseT.enemySpawnRate as Millis).timeMillis
        val c30Rate = (c30T.enemySpawnRate as Millis).timeMillis
        val expectedFloor = (baseRate * 0.55f).toInt().coerceAtLeast(300)
        assertEquals("Spawn rate floored at 0.55× (or 300ms min)", expectedFloor, c30Rate)
    }

    @Test
    fun `cycle 100 stays capped (no late-game blowup)`() {
        val baseT = (provider.getAt(0) as StageGame).enemyType as RegularEnemyType
        val c100T = (provider.getAt(100 * chapter1GameStages.size) as StageGame).enemyType as RegularEnemyType
        assertEquals(baseT.hp * 4.0f, c100T.hp, 0.01f)
        assertEquals(baseT.impactPower * 1.8f, c100T.impactPower, 0.01f)
        assertEquals(baseT.yOffsetSpeed * 1.6f, c100T.yOffsetSpeed, 0.01f)
    }

    @Test
    fun `impact ramp is gentler than the old curve (instant-death regression guard)`() {
        // Old: impact ×1.08^c cap 2.5. New must be strictly lower at a mid cycle
        // so a future revert to the harsh curve fails this test.
        val baseT = (provider.getAt(0) as StageGame).enemyType as RegularEnemyType
        val c10T = (provider.getAt(10 * chapter1GameStages.size) as StageGame).enemyType as RegularEnemyType
        val oldImpactAt10 = baseT.impactPower * Math.pow(1.08, 10.0).toFloat() // ~2.16×
        assertTrue("new impact must be gentler than old 1.08^10", c10T.impactPower < oldImpactAt10)
    }

    @Test
    fun `monotonic — higher cycle never produces weaker stats`() {
        val baseT = (provider.getAt(0) as StageGame).enemyType as RegularEnemyType
        var prevHp = baseT.hp; var prevImpact = baseT.impactPower; var prevSpeed = baseT.yOffsetSpeed
        for (cycle in 1..30) {
            val t = (provider.getAt(cycle * chapter1GameStages.size) as StageGame).enemyType as RegularEnemyType
            assertTrue("cycle=$cycle HP regressed", t.hp >= prevHp)
            assertTrue("cycle=$cycle impact regressed", t.impactPower >= prevImpact)
            assertTrue("cycle=$cycle speed regressed", t.yOffsetSpeed >= prevSpeed)
            prevHp = t.hp; prevImpact = t.impactPower; prevSpeed = t.yOffsetSpeed
        }
    }

    // ── Wave 13d — theme rotation ──

    @Test
    fun `EndlessTheme rotates chapters 1 through 5 round-robin`() {
        assertEquals(1, EndlessTheme.chapterIdForCycle(0))
        assertEquals(2, EndlessTheme.chapterIdForCycle(1))
        assertEquals(5, EndlessTheme.chapterIdForCycle(4))
        assertEquals(1, EndlessTheme.chapterIdForCycle(5)) // wraps
        assertEquals(3, EndlessTheme.chapterIdForCycle(12))
    }

    @Test
    fun `provider chapterAt rotates with cycle (was stuck at 1)`() {
        val size = chapter1GameStages.size
        assertEquals(1, provider.chapterAt(0))
        assertEquals(2, provider.chapterAt(size))       // cycle 1
        assertEquals(1, provider.chapterAt(5 * size))   // cycle 5 wraps to chapter 1
    }

    @Test
    fun `getAt past cycle 0 stamps the rotated chapter id and its hazard`() {
        val size = chapter1GameStages.size
        // cycle 1 → chapter 2 (NEBULA_CLOUD, hazard NEBULA_FOG).
        val s = provider.getAt(size) as StageGame
        assertEquals(2, s.chapterId)
        val ch2Hazard = Chapter.entries.first { it.id == 2 }.hazard
        assertEquals(ch2Hazard, s.hazard)
    }
}
