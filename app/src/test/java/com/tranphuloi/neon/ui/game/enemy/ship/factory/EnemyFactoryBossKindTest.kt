package com.tranphuloi.neon.ui.game.enemy.ship.factory

import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies [EnemyFactory.resolveBossKind] (now a delegate to
 * [com.tranphuloi.neon.ui.game.enemy.ship.model.BossKindResolver]) returns
 * the canonical chapter-aware [BossKind] for every boss type. Pure unit test
 * — no factory side-effects required.
 *
 * Behavior shift from earlier audit: previously the factory returned `null`
 * for default cases (e.g. Ch1 OFFENSIVE) and let the boss constructor fall
 * back to `variant.defaultBossKind`. After consolidation, the resolver
 * returns the canonical kind directly, so the constructor receives a
 * non-null override that equals the prior fallback. Same in-game outcome,
 * but tests now assert the explicit kind instead of null.
 *
 * Mapping:
 *   Ch1 OFFENSIVE  → ORB             (was null fallback)
 *   Ch4 OFFENSIVE  → HELL_LORD       (override)
 *   Ch2 DEFENSIVE  → FRACTAL         (was null fallback)
 *   Any chapter SWARM → HAUNTED_KID  (chapter-agnostic override)
 *   Ch1 LevelOne   → STAR            (was null fallback)
 *   Ch3 LevelOne   → DEATH_MOON      (override)
 *   Ch2 LevelTwo   → CROSS           (was null fallback)
 *   Ch4 LevelTwo   → SATAN_GLYPH     (override)
 *   Ch5 FinalBoss  → SPIDER          (was null fallback)
 */
class EnemyFactoryBossKindTest {

    private fun factoryAt(chapter: Int): EnemyFactory =
        EnemyFactory(screenWidth = 400f, screenHeight = 800f).apply {
            currentChapterId = chapter
        }

    @Test
    fun `Ch1 OFFENSIVE returns canonical ORB (was null fallback)`() {
        assertEquals(
            BossKind.ORB,
            factoryAt(1).resolveBossKind(MidBossType.OFFENSIVE),
        )
    }

    @Test
    fun `Ch4 OFFENSIVE returns HELL_LORD`() {
        assertEquals(
            BossKind.HELL_LORD,
            factoryAt(4).resolveBossKind(MidBossType.OFFENSIVE),
        )
    }

    @Test
    fun `Ch2 DEFENSIVE returns canonical FRACTAL`() {
        assertEquals(
            BossKind.FRACTAL,
            factoryAt(2).resolveBossKind(MidBossType.DEFENSIVE),
        )
    }

    @Test
    fun `Any chapter SWARM returns HAUNTED_KID`() {
        for (ch in 1..5) {
            assertEquals(
                "Ch$ch SWARM should return HAUNTED_KID",
                BossKind.HAUNTED_KID,
                factoryAt(ch).resolveBossKind(MidBossType.SWARM),
            )
        }
    }

    @Test
    fun `Ch1 LevelOne returns canonical STAR`() {
        assertEquals(
            BossKind.STAR,
            factoryAt(1).resolveBossKind(LevelOneBossType),
        )
    }

    @Test
    fun `Ch3 LevelOne (reuse) returns DEATH_MOON`() {
        assertEquals(
            BossKind.DEATH_MOON,
            factoryAt(3).resolveBossKind(LevelOneBossType),
        )
    }

    @Test
    fun `Ch2 LevelTwo returns canonical CROSS`() {
        assertEquals(
            BossKind.CROSS,
            factoryAt(2).resolveBossKind(LevelTwoBossType),
        )
    }

    @Test
    fun `Ch4 LevelTwo (reuse) returns SATAN_GLYPH`() {
        assertEquals(
            BossKind.SATAN_GLYPH,
            factoryAt(4).resolveBossKind(LevelTwoBossType),
        )
    }

    @Test
    fun `Ch5 FinalBoss returns canonical SPIDER`() {
        assertEquals(
            BossKind.SPIDER,
            factoryAt(5).resolveBossKind(FinalBossType),
        )
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
