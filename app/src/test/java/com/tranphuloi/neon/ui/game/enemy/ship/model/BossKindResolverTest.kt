package com.tranphuloi.neon.ui.game.enemy.ship.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins [BossKindResolver] semantics directly — independent of any caller
 * (EnemyFactory / GameState / StoryRegistry) delegate that might mask a
 * regression. If someone refactors a delegate caller and accidentally
 * shadows the resolver, these tests fail.
 *
 * Behavior contract:
 *   1. Chapter-specific overrides for the 4 reused boss slots
 *      (DEATH_MOON / SATAN_GLYPH / HELL_LORD / HAUNTED_KID).
 *   2. Default chapter (chapter != 3 for LevelOne, etc.) returns canonical
 *      BossKind (STAR / CROSS / SPIDER / ORB / FRACTAL).
 *   3. MidBoss R81 variants self-describe via [MidBossType.defaultBossKind].
 *   4. RegularEnemyType (non-boss) returns null.
 */
class BossKindResolverTest {

    @Test
    fun `LevelOneBoss returns DEATH_MOON only in chapter 3`() {
        assertEquals(BossKind.STAR, BossKindResolver.resolve(LevelOneBossType, 1))
        assertEquals(BossKind.STAR, BossKindResolver.resolve(LevelOneBossType, 2))
        assertEquals(BossKind.DEATH_MOON, BossKindResolver.resolve(LevelOneBossType, 3))
        assertEquals(BossKind.STAR, BossKindResolver.resolve(LevelOneBossType, 4))
        assertEquals(BossKind.STAR, BossKindResolver.resolve(LevelOneBossType, 5))
    }

    @Test
    fun `LevelTwoBoss returns SATAN_GLYPH only in chapter 4`() {
        assertEquals(BossKind.CROSS, BossKindResolver.resolve(LevelTwoBossType, 1))
        assertEquals(BossKind.CROSS, BossKindResolver.resolve(LevelTwoBossType, 2))
        assertEquals(BossKind.CROSS, BossKindResolver.resolve(LevelTwoBossType, 3))
        assertEquals(BossKind.SATAN_GLYPH, BossKindResolver.resolve(LevelTwoBossType, 4))
        assertEquals(BossKind.CROSS, BossKindResolver.resolve(LevelTwoBossType, 5))
    }

    @Test
    fun `FinalBoss always returns SPIDER (chapter-agnostic)`() {
        for (ch in 1..5) {
            assertEquals(
                "Ch$ch FinalBoss should return SPIDER",
                BossKind.SPIDER, BossKindResolver.resolve(FinalBossType, ch),
            )
        }
    }

    @Test
    fun `MidBossType OFFENSIVE returns HELL_LORD only in chapter 4`() {
        assertEquals(BossKind.ORB, BossKindResolver.resolve(MidBossType.OFFENSIVE, 1))
        assertEquals(BossKind.ORB, BossKindResolver.resolve(MidBossType.OFFENSIVE, 3))
        assertEquals(BossKind.HELL_LORD, BossKindResolver.resolve(MidBossType.OFFENSIVE, 4))
        assertEquals(BossKind.ORB, BossKindResolver.resolve(MidBossType.OFFENSIVE, 5))
    }

    @Test
    fun `MidBossType SWARM always returns HAUNTED_KID (chapter-agnostic)`() {
        for (ch in 1..5) {
            assertEquals(
                "Ch$ch SWARM should return HAUNTED_KID",
                BossKind.HAUNTED_KID, BossKindResolver.resolve(MidBossType.SWARM, ch),
            )
        }
    }

    @Test
    fun `MidBossType DEFENSIVE returns canonical FRACTAL (no chapter override)`() {
        for (ch in 1..5) {
            assertEquals(BossKind.FRACTAL, BossKindResolver.resolve(MidBossType.DEFENSIVE, ch))
        }
    }

    @Test
    fun `MidBoss R81 variants resolve to their defaultBossKind`() {
        val pairs = listOf(
            MidBossType.HEN_MOTHER to BossKind.HEN_MOTHER,
            MidBossType.BUFFALO_RAGE to BossKind.BUFFALO_RAGE,
            MidBossType.DUMB_RAT to BossKind.DUMB_RAT,
            MidBossType.FIERCE_TIGER to BossKind.FIERCE_TIGER,
            MidBossType.SEXY_DIVA to BossKind.SEXY_DIVA,
            MidBossType.TROLL_TOWER to BossKind.TROLL_TOWER,
            MidBossType.TWIN_SUMMITS to BossKind.TWIN_SUMMITS,
            MidBossType.VOID_GLOBES to BossKind.VOID_GLOBES,
            MidBossType.WHITE_DRAGON to BossKind.WHITE_DRAGON,
            MidBossType.HAMMER_SICKLE to BossKind.HAMMER_SICKLE,
            MidBossType.MONEY_TYCOON to BossKind.MONEY_TYCOON,
            MidBossType.GOLDEN_TYCOON to BossKind.GOLDEN_TYCOON,
            // Wave 15 batch 1
            MidBossType.SKULL_CROSSBONES to BossKind.SKULL_CROSSBONES,
            MidBossType.VAMPIRE to BossKind.VAMPIRE,
            MidBossType.COSMIC_CENTIPEDE to BossKind.COSMIC_CENTIPEDE,
            // Wave 16 batch 2
            MidBossType.GIANT_CONDOM to BossKind.GIANT_CONDOM,
            MidBossType.VENOM_SPIDER to BossKind.VENOM_SPIDER,
            MidBossType.CORRUPTION to BossKind.CORRUPTION,
        )
        for ((variant, expectedKind) in pairs) {
            assertEquals(
                "Variant $variant should resolve to ${expectedKind.name}",
                expectedKind, BossKindResolver.resolve(variant, chapterId = 5),
            )
        }
    }

    @Test
    fun `RegularEnemyType returns null (not a boss)`() {
        // Minimal regular enemy — resolver should return null for any
        // non-boss type. Values don't matter; only the type discriminates.
        val regular = RegularEnemyType(
            drawableId = 0,
            width = 0f,
            height = 0f,
            hp = 1f,
            impactPower = 0f,
            formation = ZigZag(position = ZigZagInitialPosition.LEFT),
            xOffsetSpeed = 0f,
            yOffsetSpeed = 0f,
            enemySpawnRate = com.tranphuloi.neon.ui.game.common.Once,
        )
        for (ch in 1..5) {
            assertNull(
                "Ch$ch regular enemy should NOT resolve to a BossKind",
                BossKindResolver.resolve(regular, ch),
            )
        }
    }
}
