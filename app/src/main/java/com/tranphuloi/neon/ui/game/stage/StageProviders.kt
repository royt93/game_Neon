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

/**
 * Wave 16 — Boss Rush roster ĐẦY ĐỦ. Trước đây = `stages.filterIsInstance<StageBoss>()`
 * → chỉ ~2-3 boss cuối chương; 21 mid-boss variant (spawn qua Chapter.midBossTypes
 * giữa chương, KHÔNG phải StageBoss) bị bỏ sót khỏi boss-rush (user báo). Nay dựng
 * trực tiếp: 21 mid-boss + boss cuối/biến-thể-chương → đủ 27 BossKind.
 */
private val allBosses: List<StageBoss> by lazy {
    buildList {
        com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType.ALL.forEachIndexed { i, mb ->
            // OFFENSIVE giữ chapter 1 (→ ORB); HELL_LORD (OFFENSIVE@ch4) thêm riêng bên dưới.
            val ch = if (mb == com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType.OFFENSIVE) 1 else (i % 5) + 1
            add(StageBoss(bossId = "rush_${mb.displayName}", enemyType = mb, chapterId = ch))
        }
        // Boss cuối chương + biến thể chương (phủ nốt STAR/DEATH_MOON/CROSS/SATAN/HELL_LORD/SPIDER).
        add(StageBoss("rush_star", com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType, chapterId = 1))
        add(StageBoss("rush_deathmoon", com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType, chapterId = 3))
        add(StageBoss("rush_cross", com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType, chapterId = 2))
        add(StageBoss("rush_satan", com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType, chapterId = 4))
        add(StageBoss("rush_hell", com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType.OFFENSIVE, chapterId = 4))
        add(StageBoss("rush_final", com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType, chapterId = 5))
    }
}

/** Wave 17 — số boss thật trong Boss Rush (cho nhãn UI khỏi hardcode lệch). */
val bossRushRosterSize: Int by lazy { allBosses.size }

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
        // Wave 13d — rotate the 5 chapter themes (tint + hazard) per cycle so an
        // endless run beyond stage 100 doesn't stay visually stuck in chapter 1.
        val rotatedChapterId = EndlessTheme.chapterIdForCycle(cycle)
        if (cycle == 0) return base                                   // first pass = chapter 1, untouched
        val themedHazard = Chapter.entries.firstOrNull { it.id == rotatedChapterId }?.hazard
        // Wave 13d — SOFTENED difficulty curve. On-device log (Pixel 7 Pro) at
        // stage ~109 (cycle ~9) showed the ship dying in ~6 hits — old impact
        // ×1.08^c (cap 2.5) + speed ×1.05^c (cap 2.5) made stage 100+ feel
        // instant-death. Damage + descent speed are what actually kill the
        // player, so those now ramp gentler + cap lower (HP stays tanky —
        // tankiness prolongs, it doesn't kill):
        //   HP     × 1.08^c cap 4.0    (was 1.10 / 5.0)
        //   impact × 1.05^c cap 1.8    (was 1.08 / 2.5)  ← main instant-death fix
        //   speed  × 1.03^c cap 1.6    (was 1.05 / 2.5)  ← more reaction time
        //   spawn  × 0.96^c floor 0.55 (was 0.95 / 0.5)
        val hpScale = Math.pow(1.08, cycle.toDouble()).toFloat().coerceAtMost(4.0f)
        val spawnScale = Math.pow(0.96, cycle.toDouble()).toFloat().coerceAtLeast(0.55f)
        val impactScale = Math.pow(1.05, cycle.toDouble()).toFloat().coerceAtMost(1.8f)
        val speedScale = Math.pow(1.03, cycle.toDouble()).toFloat().coerceAtMost(1.6f)
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
        return base.copy(enemyType = scaledType, chapterId = rotatedChapterId, hazard = themedHazard)
    }

    // Wave 13d — tint/hazard theme rotates 1..5 per full pass through ch-1 stages.
    override fun chapterAt(index: Int): Int =
        EndlessTheme.chapterIdForCycle(index / chapter1GameStages.size)

    override fun size(): Int = -1                                     // infinite
}

/** Wave 13d — pure helper: endless theme cycling (testable). */
internal object EndlessTheme {
    const val CHAPTER_COUNT = 5

    /** cycle 0→1, 1→2, … 4→5, 5→1, … (chapters 1..5 round-robin). */
    fun chapterIdForCycle(cycle: Int): Int = (cycle.coerceAtLeast(0) % CHAPTER_COUNT) + 1
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
