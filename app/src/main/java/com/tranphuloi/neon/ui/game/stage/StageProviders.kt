package com.tranphuloi.neon.ui.game.stage

/**
 * Wave 5 (43x) — mode-specific StageProvider variants.
 * StaticListProvider for CAMPAIGN lives in StageProvider.kt.
 *
 * All providers below derive from the chapter-1 slice of [stages] (the
 * procedural campaign script) so balance + visuals stay consistent.
 */

/** Memoized chapter-1 game-stage slice (no messages/boss/break entries). */
private val chapter1GameStages: List<StageGame> by lazy {
    stages.filterIsInstance<StageGame>().filter { it.chapterId == 1 }
}

/** All StageBoss entries across the entire campaign, in order. */
private val allBosses: List<StageBoss> by lazy {
    stages.filterIsInstance<StageBoss>()
}

/**
 * Survival: chapter-1 stages cycled forever. Each cycle bumps spawn rate and
 * enemy HP slightly so the run gets gradually harder. No bosses, no story.
 * Survival difficulty must come from cycle count, not chapter progression.
 */
class SurvivalProvider : StageProvider {
    override fun hasAt(index: Int): Boolean = chapter1GameStages.isNotEmpty()

    override fun getAt(index: Int): Stage {
        val cycle = index / chapter1GameStages.size
        val pos = index % chapter1GameStages.size
        val base = chapter1GameStages[pos]
        if (cycle == 0) return base
        val scale = 1f + cycle * 0.15f                                // +15% per cycle
        // Increase enemy HP via the existing RegularEnemyType wrapper. We can't
        // mutate enemy directly (immutable), so reconstruct enemyType with
        // scaled hp.
        val enemyType = base.enemyType
        val scaledType = if (enemyType is com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType) {
            enemyType.copy(hp = enemyType.hp * scale)
        } else enemyType
        return base.copy(enemyType = scaledType, durationTimeSec = (base.durationTimeSec + 2))
    }

    override fun chapterAt(index: Int): Int = 1
    override fun size(): Int = -1                                     // infinite
}

/**
 * Boss Rush: all StageBoss entries from the campaign, played back-to-back.
 * Intersperse short "READY?" StageMessage between bosses so the player gets
 * a beat to breathe (and the HUD has time to clear lasers/explosions).
 */
class BossRushProvider : StageProvider {
    private val script: List<Stage> = buildList {
        add(StageMessage(message = "BOSS RUSH", durationMillis = 3, chapterId = 1))
        add(StageMessage(message = "Get ready!", durationMillis = 2, chapterId = 1))
        allBosses.forEachIndexed { i, boss ->
            add(boss)
            if (i < allBosses.size - 1) {
                add(StageMessage(message = "Next!", durationMillis = 2, chapterId = boss.chapterId))
            }
        }
        add(StageMessage(message = "ALL CLEAR!", durationMillis = 3, chapterId = 5))
    }

    override fun hasAt(index: Int): Boolean = index in script.indices
    override fun getAt(index: Int): Stage = script[index]
    override fun chapterAt(index: Int): Int {
        if (index !in script.indices) return -1
        return when (val s = script[index]) {
            is StageGame -> s.chapterId
            is StageBoss -> s.chapterId
            is StageMessage -> s.chapterId
            else -> 0
        }
    }
    override fun size(): Int = script.size
}

/**
 * Endless: chapter-1 stages cycled with aggressive per-wave scaling.
 * spawn × 1.05^wave, hp × 1.10^wave, both cycled per "wave" = one full pass
 * through chapter-1 stages. Score = survival time (seconds), submitted to
 * the endless leaderboard on GAME_OVER. Difficulty ramps faster than Survival
 * so reaching ~3-5 minutes feels like an achievement.
 */
class EndlessProvider : StageProvider {
    override fun hasAt(index: Int): Boolean = chapter1GameStages.isNotEmpty()

    override fun getAt(index: Int): Stage {
        val cycle = index / chapter1GameStages.size                   // wave count
        val pos = index % chapter1GameStages.size
        val base = chapter1GameStages[pos]
        if (cycle == 0) return base
        val hpScale = Math.pow(1.10, cycle.toDouble()).toFloat()
        val spawnScale = Math.pow(0.95, cycle.toDouble()).toFloat()   // ×1.05^wave faster → /1.05 = ×0.95 cadence
        val enemyType = base.enemyType
        val scaledType = if (enemyType is com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType) {
            val baseRate = enemyType.enemySpawnRate
            val scaledRate =
                if (baseRate is com.tranphuloi.neon.ui.game.common.Millis) {
                    com.tranphuloi.neon.ui.game.common.Millis(
                        (baseRate.timeMillis * spawnScale).toInt().coerceAtLeast(300)
                    )
                } else baseRate
            enemyType.copy(
                hp = enemyType.hp * hpScale,
                enemySpawnRate = scaledRate,
            )
        } else enemyType
        return base.copy(enemyType = scaledType)
    }

    override fun chapterAt(index: Int): Int = 1
    override fun size(): Int = -1                                     // infinite
}

/**
 * Time Attack: chapter-1 stages cycled while a strict 60s wall clock counts
 * down at the GameState level. Provider just supplies stages; GameState ends
 * the run when the timer expires (force GAME_OVER).
 */
class TimeAttackProvider : StageProvider {
    /** Wall-clock duration the player has to score. GameState reads this. */
    val timeLimitSec: Int = 60

    override fun hasAt(index: Int): Boolean = chapter1GameStages.isNotEmpty()

    override fun getAt(index: Int): Stage {
        // Cycle chapter-1 game stages with mild scaling, similar to Survival.
        val cycle = index / chapter1GameStages.size
        val pos = index % chapter1GameStages.size
        val base = chapter1GameStages[pos]
        if (cycle == 0) return base
        val scale = 1f + cycle * 0.10f
        val enemyType = base.enemyType
        val scaledType = if (enemyType is com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType) {
            enemyType.copy(hp = enemyType.hp * scale)
        } else enemyType
        return base.copy(enemyType = scaledType)
    }

    override fun chapterAt(index: Int): Int = 1
    override fun size(): Int = -1                                     // infinite (gated by timer)
}
