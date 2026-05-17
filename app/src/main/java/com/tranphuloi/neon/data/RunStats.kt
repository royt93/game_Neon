package com.tranphuloi.neon.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf

/**
 * 15c: End-of-run stats snapshot, captured by GameScreen on GAME_OVER and read by
 * DialogGameOver. Persists for the duration of one navigation hop only.
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
)

val LocalRunStats =
    compositionLocalOf<MutableState<RunStats?>> { error("LocalRunStats not provided") }
