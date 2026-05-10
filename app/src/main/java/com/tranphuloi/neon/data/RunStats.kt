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
)

val LocalRunStats =
    compositionLocalOf<MutableState<RunStats?>> { error("LocalRunStats not provided") }
