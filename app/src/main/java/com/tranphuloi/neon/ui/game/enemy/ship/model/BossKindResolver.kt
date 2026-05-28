package com.tranphuloi.neon.ui.game.enemy.ship.model

/**
 * Single source of truth for resolving an [EnemyType] + chapter to a
 * [BossKind]. Was duplicated as three near-identical mirrors:
 *   - EnemyFactory.resolveBossKind (used `currentChapterId`)
 *   - GameState.resolveBossKindForBanner (chapter-aware banner)
 *   - StoryRegistry.resolveBossKind (chapter-aware taunt speaker)
 *
 * Chapter-specific overrides exist because the same boss slot reuses art
 * across chapters: e.g. Ch3 LevelOneBoss visually becomes Death Moon, Ch4
 * LevelTwoBoss becomes Satan Glyph. The default boss for each slot (chapter
 * 1) falls through to the slot's canonical kind (STAR/CROSS/SPIDER). MidBoss
 * variants self-describe via [MidBossType.defaultBossKind] except for the
 * three legacy generic types (OFFENSIVE/DEFENSIVE/SWARM) where chapter or
 * type-id forces a specific kind.
 *
 * Returns `null` only when [type] isn't a boss type (i.e. RegularEnemyType).
 */
object BossKindResolver {

    fun resolve(type: EnemyType, chapterId: Int): BossKind? = when {
        type is LevelOneBossType && chapterId == 3 -> BossKind.DEATH_MOON
        type is LevelOneBossType -> BossKind.STAR
        type is LevelTwoBossType && chapterId == 4 -> BossKind.SATAN_GLYPH
        type is LevelTwoBossType -> BossKind.CROSS
        type is FinalBossType -> BossKind.SPIDER
        type == MidBossType.OFFENSIVE && chapterId == 4 -> BossKind.HELL_LORD
        type == MidBossType.SWARM -> BossKind.HAUNTED_KID
        type is MidBossType -> type.defaultBossKind
        else -> null
    }
}
