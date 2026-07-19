package com.tranphuloi.neon.ui.game.stage

import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.common.Never
import com.tranphuloi.neon.ui.game.common.RepeatTime
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyFormation
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.Row
import com.tranphuloi.neon.ui.game.enemy.ship.model.SineWave
import com.tranphuloi.neon.ui.game.enemy.ship.model.VFormation
import com.tranphuloi.neon.ui.game.enemy.ship.model.ZigZag
import com.tranphuloi.neon.ui.game.enemy.ship.model.ZigZagInitialPosition
import java.io.Serializable
import java.util.UUID

@androidx.annotation.Keep
sealed class Stage(val durationSec: Int) : Serializable

data class StageMessage(
    val message: String,
    val durationMillis: Int,
    /** 32d Wave 4 — chapter the message belongs to. 0 = unspecified (legacy/global). */
    val chapterId: Int = 0,
) : Stage(durationMillis)

data class StageGame(
    val spaceRockSpawnRateMillis: RepeatTime = Never,
    val enemyType: EnemyType,
    val durationTimeSec: Int,
    /** 32d hazard tag — drives GameScreen visual cue + GameState mechanic. Null = no hazard. */
    val hazard: HazardType? = null,
    /** Chapter id (1..5) — used for stageTint mapping in GameScreen. */
    val chapterId: Int = 1,
) : Stage(durationTimeSec)

data class StageBoss(
    val bossId: String,
    val enemyType: EnemyType,
    val chapterId: Int = 1,
) : Stage(1)

object StageBreak : Stage(3)

/**
 * 31d Wave 4 — procedural stage script generator.
 * Layout per chapter (~20 stages each, ~100 total):
 *   - 3 intro messages (Chapter X / Name / GO!)
 *   - 12 game stages (mixed formations, scaled difficulty)
 *   - 2 mid-boss insertions at game-stages 6 + 12 (3 entries each: warning + boss + outro)
 *   - 4 chapter-boss intro/outro (3 messages + StageBoss + outro)
 *
 * Final chapter (GALAXY_CORE) skips mid-bosses — it goes straight into FinalBoss.
 */
val stages: List<Stage> = buildStageScript()

private fun buildStageScript(): List<Stage> {
    val list = mutableListOf<Stage>()
    Chapter.values().forEach { chapter ->
        // Chapter intro (3 messages, all tagged with chapter.id so stageTint shifts
        // correctly as soon as a new chapter begins — without the chapterId tag,
        // walk-back logic would resolve to the previous chapter's last boss).
        list.add(StageMessage(message = "Chương ${chapter.id}", durationMillis = 3, chapterId = chapter.id))
        list.add(StageMessage(message = chapter.displayName, durationMillis = 3, chapterId = chapter.id))
        list.add(StageMessage(message = "Bắt đầu!", durationMillis = 1, chapterId = chapter.id))

        // 12 game stages, mid-boss inserted at game-stage 6 + 12 + extras.
        // Tier scales difficulty across the chapter: 0=early, 1=mid, 2=late.
        // Round 82 — iterate midBossTypes list. Game-stage 6 = midBossTypes[0],
        // game-stage 12 = midBossTypes[1], game-stage 10 = midBossTypes[2],
        // game-stage 8 = midBossTypes[3]. Insert extra mid-bosses for chapters
        // với midBossTypes.size > 2 (R81 wire: each chapter có 3-4 mini-bosses).
        val midBossSlotsByStage = mapOf(
            6 to chapter.midBossTypes.getOrNull(0),
            8 to chapter.midBossTypes.getOrNull(3),
            10 to chapter.midBossTypes.getOrNull(2),
            12 to chapter.midBossTypes.getOrNull(1),
        )
        for (gameStage in 1..12) {
            val tier = (gameStage - 1) / 4
            list.add(buildGameStage(chapter, gameStage, tier))

            val midBossForStage = midBossSlotsByStage[gameStage]
            if (midBossForStage != null) {
                list.add(StageMessage(message = "Nguy hiểm", durationMillis = 2, chapterId = chapter.id))
                list.add(
                    StageBoss(
                        bossId = UUID.randomUUID().toString(),
                        enemyType = midBossForStage,
                        chapterId = chapter.id,
                    )
                )
                list.add(StageMessage(message = "Tiếp tục!", durationMillis = 3, chapterId = chapter.id))
            }
        }

        // Chapter final boss intro (3 messages + boss)
        list.add(StageMessage(message = "Trận boss", durationMillis = 3, chapterId = chapter.id))
        list.add(StageMessage(message = "Sẵn sàng!", durationMillis = 3, chapterId = chapter.id))
        list.add(StageMessage(message = "Bắt đầu!", durationMillis = 1, chapterId = chapter.id))
        list.add(
            StageBoss(
                bossId = UUID.randomUUID().toString(),
                enemyType = chapter.finalBossType,
                chapterId = chapter.id,
            )
        )
        list.add(
            StageMessage(
                message = if (chapter.id == 5) "Chiến thắng!" else "Hoàn thành chương!",
                durationMillis = 3,
                chapterId = chapter.id,
            )
        )
    }
    return list
}

/**
 * Build a single StageGame for [chapter] at [gameStage] with difficulty tier 0..2.
 * Cycles through enemy drawables in the chapter palette + alternates ZigZag/Row formations.
 */
private fun buildGameStage(chapter: Chapter, gameStage: Int, tier: Int): StageGame {
    val drawable = chapter.regularEnemyDrawables[(gameStage - 1) % chapter.regularEnemyDrawables.size]
    // Enemy stats scale with tier + chapter index (later chapters tougher).
    val hpScale = 1f + tier * 0.18f + (chapter.id - 1) * 0.12f
    val baseHp = 180f
    val baseImpact = 60f
    val isZigZag = gameStage % 2 == 1
    val rowCount = 3 + tier.coerceAtMost(2)                          // 3..5
    val zigZagPosition = if (gameStage % 4 < 2) ZigZagInitialPosition.RIGHT
    else ZigZagInitialPosition.LEFT

    // 28x Wave 5 — from chapter 2 onwards (~stage 25+ absolute), inject V and
    // SineWave procedurally. Selection seed = chapter.id * 12 + gameStage so it's
    // deterministic per slot. Chapter 1 keeps the original ZigZag/Row alternation
    // (matches existing early-game ramp pace).
    //
    // Pixel-2 #4 — derive a per-stage `xAnchorShift` in [-60, +60] so
    // identical formation types spawn at different starting X positions across
    // stages. Without this, every Row felt visually identical at the same X
    // anchor. Deterministic per gameStage so save/load preserves the pattern.
    val anchorShift = ((chapter.id * 12 + gameStage) * 37 % 121 - 60).toFloat()
    val formation: EnemyFormation = if (chapter.id < 2) {
        // Audit-5 P2 fix — ZigZag now also varies start position per stage.
        if (isZigZag) ZigZag(position = zigZagPosition, xAnchorShift = anchorShift)
        else Row(rowCount = rowCount, xAnchorShift = anchorShift)
    } else {
        val seed = chapter.id * 12 + gameStage
        // Distribution: 35% ZigZag, 30% Row, 17% VFormation, 18% SineWave.
        when (seed % 6) {
            0, 1 -> ZigZag(position = zigZagPosition, xAnchorShift = anchorShift)
            2, 3 -> Row(rowCount = rowCount, xAnchorShift = anchorShift)
            4 -> VFormation(count = 5, xAnchorShift = anchorShift)
            else -> SineWave(count = 4, xAnchorShift = anchorShift)
        }
    }

    // Round 74 (R73d) — Wave 9a: apply EnemyFamily stat profile.
    // Family derived từ drawable (Scout/Fighter/Heavy/Elite/Berserker).
    val family = com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyFamily.fromDrawableId(drawable)
    val enemyType = RegularEnemyType(
        drawableId = drawable,
        width = 40f + tier * 3f,
        height = 40f + tier * 3f,
        hp = baseHp * hpScale * family.hpMul,
        impactPower = (baseImpact + tier * 5f) * family.impactMul,
        formation = formation,
        xOffsetSpeed = (0.5f + tier * 0.1f) * family.speedMul,
        yOffsetSpeed = (0.5f + tier * 0.1f) * family.speedMul,
        enemySpawnRate = Millis(1000 - tier * 100),                  // 1000/900/800ms
        // Task 09 — 5 địch chủ đề có đòn RIÊNG; địch cũ mặc định SINGLE.
        attackKind = com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyAttackKind.forDrawable(drawable),
        // Task 27 — Amip Vũ Trụ: tách đôi khi chết (mitosis-on-death).
        splitsOnDeath = drawable == R.drawable.enemy_amoeba,
    )

    // Chapter hazard determines spaceRock spawn cadence.
    val rockRate: RepeatTime = when (chapter.hazard) {
        HazardType.ASTEROID_STORM -> Millis(1500)                    // frequent rocks
        HazardType.NEBULA_FOG -> Millis(3500)                        // rare
        HazardType.ICE_PATCHES -> Millis(2500)
        null -> if (tier >= 1) Millis(2200) else Never               // standard cadence
    }

    return StageGame(
        spaceRockSpawnRateMillis = rockRate,
        enemyType = enemyType,
        durationTimeSec = 5 + tier * 2,                              // 5..9s
        hazard = chapter.hazard,
        chapterId = chapter.id,
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Task: QoL Practice mode — helper thuần (testable) trên script `stages`.
// ─────────────────────────────────────────────────────────────────────────

private fun stageChapterOf(s: Stage): Int = when (s) {
    is StageMessage -> s.chapterId
    is StageGame -> s.chapterId
    is StageBoss -> s.chapterId
    is StageBreak -> 0
}

/** Chỉ số stage ĐẦU TIÊN của [chapterId] trong `stages` (0 nếu không thấy). */
fun chapterStartIndex(chapterId: Int): Int =
    stages.indexOfFirst { stageChapterOf(it) == chapterId }.coerceAtLeast(0)

/**
 * Chương cao nhất ĐÃ MỞ để luyện tập, suy từ checkpoint campaign (walk-back qua
 * StageBreak). Luôn ≥ 1 (chương 1 luôn luyện được).
 */
fun maxUnlockedChapter(campaignCheckpointIndex: Int): Int {
    if (campaignCheckpointIndex <= 0 || stages.isEmpty()) return 1
    val i = campaignCheckpointIndex.coerceIn(0, stages.size - 1)
    for (j in i downTo 0) {
        val c = stageChapterOf(stages[j])
        if (c >= 1) return c
    }
    return 1
}
