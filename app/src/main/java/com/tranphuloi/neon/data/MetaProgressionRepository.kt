package com.tranphuloi.neon.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.metaDataStore by preferencesDataStore(name = "neon_meta")

private val LIFETIME_MINERALS_KEY = intPreferencesKey("lifetime_minerals")
private val NODE_PREFIX = "node_"

/**
 * Wave 5 (48x) — permanent meta progression. Tracks:
 *  - lifetime minerals (currency earned across all runs),
 *  - per-skill-node ranks (0 = locked / not bought, 1..maxRank).
 *
 * Each node is stored as int pref "node_<id>". Lifetime minerals tick up on
 * every GAME_OVER via `addMinerals(amount)` called from DialogGameOver.
 *
 * Spending is atomic: `spendOnNode` returns true only if balance is sufficient
 * AND prerequisite ranks are met.
 */
class MetaProgressionRepository(private val appContext: Context) {

    init {
        Logger.d("MetaProgressionRepository init")
    }

    val lifetimeMinerals: Flow<Int> = appContext.metaDataStore.data.map {
        it[LIFETIME_MINERALS_KEY] ?: 0
    }

    fun nodeRank(nodeKey: String): Flow<Int> = appContext.metaDataStore.data.map {
        it[intPreferencesKey(NODE_PREFIX + nodeKey)] ?: 0
    }

    /** Returns a snapshot map of all known node ranks (used by RunContext). */
    val allRanks: Flow<Map<String, Int>> = appContext.metaDataStore.data.map { prefs ->
        prefs.asMap()
            .filterKeys { it.name.startsWith(NODE_PREFIX) }
            .mapKeys { it.key.name.removePrefix(NODE_PREFIX) }
            .mapValues { (it.value as? Int) ?: 0 }
    }

    suspend fun addMinerals(amount: Int) {
        if (amount <= 0) return
        appContext.metaDataStore.edit { prefs ->
            val before = prefs[LIFETIME_MINERALS_KEY] ?: 0
            prefs[LIFETIME_MINERALS_KEY] = before + amount
            Logger.d("MetaProgressionRepository.addMinerals +$amount → ${before + amount}")
        }
    }

    /**
     * Atomic spend + rank-up. Returns true if successful, false if insufficient
     * balance or already at max rank.
     */
    suspend fun spendOnNode(nodeKey: String, cost: Int, maxRank: Int): Boolean {
        var success = false
        appContext.metaDataStore.edit { prefs ->
            val balance = prefs[LIFETIME_MINERALS_KEY] ?: 0
            val nodePrefKey = intPreferencesKey(NODE_PREFIX + nodeKey)
            val currentRank = prefs[nodePrefKey] ?: 0
            if (balance >= cost && currentRank < maxRank) {
                prefs[LIFETIME_MINERALS_KEY] = balance - cost
                prefs[nodePrefKey] = currentRank + 1
                success = true
                Logger.d("MetaProgressionRepository.spendOnNode($nodeKey, cost=$cost): rank ${currentRank} → ${currentRank + 1}, balance ${balance} → ${balance - cost}")
            } else {
                Logger.d("MetaProgressionRepository.spendOnNode($nodeKey, cost=$cost) DENIED — balance=$balance rank=$currentRank/$maxRank")
            }
        }
        return success
    }
}

val LocalMetaProgression =
    androidx.compose.runtime.staticCompositionLocalOf<MetaProgressionRepository> {
        error("MetaProgressionRepository not provided.")
    }
