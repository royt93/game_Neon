package com.tranphuloi.neon.ui.game.stage

/**
 * Wave 5 (43x) — mode-specific StageProvider variants.
 * StaticListProvider for CAMPAIGN lives in StageProvider.kt.
 *
 * All providers below derive from the chapter-1 slice of [stages] (the
 * procedural campaign script) so balance + visuals stay consistent.
 */

/** Memoized chapter-1 game-stage slice (no messages/boss/break entries). */
// Audit-5 — `internal` (was private) so EndlessEscalationTest can size the
// cycle multiplier correctly. Still scoped to module.
internal val chapter1GameStages: List<StageGame> by lazy {
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
        add(StageMessage(message = "CHIẾN BOSS", durationMillis = 3, chapterId = 1))
        add(StageMessage(message = "Sẵn sàng!", durationMillis = 2, chapterId = 1))
        allBosses.forEachIndexed { i, boss ->
            add(boss)
            if (i < allBosses.size - 1) {
                add(StageMessage(message = BOSS_RUSH_GAP_MESSAGE, durationMillis = 2, chapterId = boss.chapterId))
            }
        }
        add(StageMessage(message = "VƯỢT ẢI!", durationMillis = 3, chapterId = 5))
    }

    companion object {
        /** Sentinel: GameState matches this exact string to trigger between-boss heal. */
        const val BOSS_RUSH_GAP_MESSAGE = "Tiếp!"
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
        // Pixel-2 #4 — escalating difficulty per cycle. Prior code only
        // scaled HP × 1.10^cycle + spawn rate × 0.95^cycle (faster). User
        // reported "level càng tăng thì enemy càng mạnh" — meaning damage +
        // speed should also escalate. Adding impact × 1.08^cycle (mild,
        // doesn't 1-shot the player) + yOffsetSpeed × 1.05^cycle (faster
        // descent → less reaction time).
        //
        // Audit-5 P2 fix — caps. At cycle=30 the unclamped exponentials
        // produced HP×17.4 / impact×10 / speed×4.3 → mathematically
        // unplayable. Caps chosen so cycle=30 is a tough but finite ceiling:
        //   HP cap 5.0     — ship can still 2-3-shot enemies with strong bullet
        //   impact cap 2.5 — enemy laser hits hurt but don't 1-shot baseline ship
        //   speed cap 2.5  — enemies fall fast but player reactions feasible
        //   spawn cap 0.5  — cadence floor (avoid 0ms spawns)
        // Reached around cycle ~17-23 depending on category. Beyond cap the
        // run still escalates via more enemies on screen (spawn rate caps at
        // 0.5×) and chapter rotation, but per-enemy power plateaus.
        val hpScale = Math.pow(1.10, cycle.toDouble()).toFloat().coerceAtMost(5.0f)
        val spawnScale = Math.pow(0.95, cycle.toDouble()).toFloat().coerceAtLeast(0.5f)
        val impactScale = Math.pow(1.08, cycle.toDouble()).toFloat().coerceAtMost(2.5f)
        val speedScale = Math.pow(1.05, cycle.toDouble()).toFloat().coerceAtMost(2.5f)
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
                impactPower = enemyType.impactPower * impactScale,
                yOffsetSpeed = enemyType.yOffsetSpeed * speedScale,
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
