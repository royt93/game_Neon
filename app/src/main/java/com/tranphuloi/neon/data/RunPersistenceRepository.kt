package com.tranphuloi.neon.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.runPersistDataStore by preferencesDataStore(name = "neon_run_persist")

/**
 * Round 25 — per-mode checkpoint persistence. Stores `stageIndex` for each
 * GameMode so users can resume after cold launches.
 *
 * Design: checkpoint-style (NOT full state snapshot). Each save records the
 * stageIndex the player had reached. On resume:
 *   - StageController starts at that index
 *   - Ship state resets to fresh (full HP, default smartBombs, etc.)
 *   - Run-stats counters (enemiesKilled / bossesDefeated / maxCombo) reset
 *
 * This keeps the model simple and gives the player a "checkpoint" experience.
 * Score-attack modes (TIME_ATTACK / BOSS_RUSH / SURVIVAL / ENDLESS) still get
 * checkpoints; resume = continue at that wave/boss with fresh ship.
 *
 * Last-played mode is also tracked so the menu can offer Continue with the
 * right mode context.
 *
 * Player meta data (achievements, leaderboard, lifetime minerals, settings) is
 * persisted by its own repository — this file is run-progress only.
 */
class RunPersistenceRepository(private val appContext: Context) {

    init {
        Logger.d("RunPersistenceRepository init")
    }

    /** Last GameMode the user played. Default empty = no run yet. */
    val lastPlayedModeKey: Flow<String> = appContext.runPersistDataStore.data.map {
        it[KEY_LAST_PLAYED_MODE] ?: ""
    }

    /** Last saved stageIndex for [modeKey]. 0 = no checkpoint (fresh run). */
    fun checkpointFor(modeKey: String): Flow<Int> =
        appContext.runPersistDataStore.data.map {
            it[checkpointKey(modeKey)] ?: 0
        }

    /** Last save timestamp for [modeKey] — used by menu to show "1 phút trước". */
    fun savedAtFor(modeKey: String): Flow<Long> =
        appContext.runPersistDataStore.data.map {
            it[savedAtKey(modeKey)] ?: 0L
        }

    /**
     * Save the current stageIndex for [modeKey]. Call after every stage
     * advance (or after meaningful in-run milestone). Quick DataStore write
     * is acceptable on the IO loop's tick boundary.
     */
    suspend fun saveCheckpoint(modeKey: String, stageIndex: Int) {
        appContext.runPersistDataStore.edit { prefs ->
            prefs[checkpointKey(modeKey)] = stageIndex
            prefs[savedAtKey(modeKey)] = System.currentTimeMillis()
            prefs[KEY_LAST_PLAYED_MODE] = modeKey
        }
        Logger.d("RunPersistenceRepository.saveCheckpoint($modeKey, $stageIndex)")
    }

    /**
     * Clear the checkpoint for [modeKey]. Called on:
     *   - real death (run over),
     *   - victory (FinalBoss killed, time-attack ended, boss-rush cleared),
     *   - user pressing "Bắt đầu mới" / "Restart".
     */
    suspend fun clearCheckpoint(modeKey: String) {
        appContext.runPersistDataStore.edit { prefs ->
            prefs.remove(checkpointKey(modeKey))
            prefs.remove(savedAtKey(modeKey))
        }
        Logger.d("RunPersistenceRepository.clearCheckpoint($modeKey)")
    }

    companion object {
        private val KEY_LAST_PLAYED_MODE = androidx.datastore.preferences.core.stringPreferencesKey("last_played_mode")
        private fun checkpointKey(modeKey: String) = intPreferencesKey("checkpoint_$modeKey")
        private fun savedAtKey(modeKey: String) = longPreferencesKey("saved_at_$modeKey")
    }
}

val LocalRunPersistence =
    androidx.compose.runtime.staticCompositionLocalOf<RunPersistenceRepository> {
        error("RunPersistenceRepository not provided.")
    }
