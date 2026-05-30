package com.tranphuloi.neon.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import com.tranphuloi.neon.ui.game.controls.BossRank
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.ship.laser.BulletType

/**
 * 15c: End-of-run stats snapshot, captured by GameScreen on GAME_OVER and read by
 * DialogGameOver. Persists for the duration of one navigation hop only.
 *
 * Wave 11b — breakdown maps (bulletKills/bossKills/ranksAchieved) carry the
 * per-run telemetry that gets persisted into MetaProgressionRepository at
 * GAME_OVER. Default empty preserves back-compat with non-game-over call sites
 * (e.g. test fixtures).
 */
data class RunStats(
    val score: Int,                 // mineralsEarnedTotal
    val timeSec: Long,              // gameTimeSec
    val enemiesKilled: Int,
    val bossesDefeated: Int,
    val maxCombo: Int,
    val stagesReached: Int,         // stageController.currentIndex()
    /** 34d Wave 4 — true if player defeated the FinalBoss (Galaxy Overlord). */
    val victoryAchieved: Boolean = false,
    /** Wave 5 (43x) — mode key the run was played in. "campaign" default for back-compat. */
    val gameModeKey: String = "campaign",
    /** Wave 11b — per-bullet kill breakdown (non-boss + boss kills both count). */
    val bulletKills: Map<BulletType, Int> = emptyMap(),
    /** Wave 11b — per-boss-kind kills. Sum across map equals bossesDefeated. */
    val bossKills: Map<BossKind, Int> = emptyMap(),
    /** Wave 11b — every boss-kill rank achieved this run, in kill order. */
    val ranksAchieved: List<BossRank> = emptyList(),
    /** Wave 11b — ship skin used + millis spent in RUNNING for this run. */
    val shipSkin: ShipSkin = ShipSkin.AURA_CYAN,
    val shipTimeMillis: Long = 0L,
)

val LocalRunStats =
    compositionLocalOf<MutableState<RunStats?>> { error("LocalRunStats not provided") }
