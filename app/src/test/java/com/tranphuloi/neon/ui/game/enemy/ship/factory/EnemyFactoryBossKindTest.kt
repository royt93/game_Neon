package com.tranphuloi.neon.ui.game.enemy.ship.factory

import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Round 81 audit — verifies [EnemyFactory.resolveBossKindForChapter] returns
 * the correct chapter-aware [BossKind] override. Pure unit test on internal
 * function — no factory instantiation required side-effects.
 *
 * Mapping (Round 79):
 *   Ch1Mid (OFFENSIVE, ch=1) → null (default ORB → Eye)
 *   Ch4Mid (OFFENSIVE, ch=4) → HELL_LORD
 *   Ch2Mid (DEFENSIVE, ch=2) → null (default FRACTAL → Atom)
 *   Ch3Mid (SWARM,     ch=3) → HAUNTED_KID
 *   Ch1End (LevelOne,  ch=1) → null (default STAR → Sun)
 *   Ch3End (LevelOne,  ch=3) → DEATH_MOON
 *   Ch2End (LevelTwo,  ch=2) → null (default CROSS)
 *   Ch4End (LevelTwo,  ch=4) → SATAN_GLYPH
 *   Ch5    (FinalBoss, ch=5) → null (default SPIDER)
 */
class EnemyFactoryBossKindTest {

    private fun factoryAt(chapter: Int): EnemyFactory =
        EnemyFactory(screenWidth = 400f, screenHeight = 800f).apply {
            currentChapterId = chapter
        }

    @Test
    fun `Ch1 OFFENSIVE returns null (default ORB stays)`() {
        assertNull(factoryAt(1).resolveBossKindForChapter(MidBossType.OFFENSIVE))
    }

    @Test
    fun `Ch4 OFFENSIVE returns HELL_LORD`() {
        assertEquals(
            BossKind.HELL_LORD,
            factoryAt(4).resolveBossKindForChapter(MidBossType.OFFENSIVE),
        )
    }

    @Test
    fun `Ch2 DEFENSIVE returns null`() {
        assertNull(factoryAt(2).resolveBossKindForChapter(MidBossType.DEFENSIVE))
    }

    @Test
    fun `Any chapter SWARM returns HAUNTED_KID`() {
        // SWARM only used in Ch3, but resolver is chapter-agnostic for SWARM.
        for (ch in 1..5) {
            assertEquals(
                "Ch$ch SWARM should return HAUNTED_KID",
                BossKind.HAUNTED_KID,
                factoryAt(ch).resolveBossKindForChapter(MidBossType.SWARM),
            )
        }
    }

    @Test
    fun `Ch1 LevelOne returns null (default STAR)`() {
        assertNull(factoryAt(1).resolveBossKindForChapter(LevelOneBossType))
    }

    @Test
    fun `Ch3 LevelOne (reuse) returns DEATH_MOON`() {
        assertEquals(
            BossKind.DEATH_MOON,
            factoryAt(3).resolveBossKindForChapter(LevelOneBossType),
        )
    }

    @Test
    fun `Ch2 LevelTwo returns null (default CROSS)`() {
        assertNull(factoryAt(2).resolveBossKindForChapter(LevelTwoBossType))
    }

    @Test
    fun `Ch4 LevelTwo (reuse) returns SATAN_GLYPH`() {
        assertEquals(
            BossKind.SATAN_GLYPH,
            factoryAt(4).resolveBossKindForChapter(LevelTwoBossType),
        )
    }

    @Test
    fun `Ch5 FinalBoss returns null (default SPIDER)`() {
        assertNull(factoryAt(5).resolveBossKindForChapter(FinalBossType))
    }

    @Test
    fun `spawnXMargin defaults to zero`() {
        assertEquals(0f, EnemyFactory(400f, 800f).spawnXMargin, 1e-6f)
    }

    @Test
    fun `currentChapterId defaults to 1`() {
        assertEquals(1, EnemyFactory(400f, 800f).currentChapterId)
    }
}
