package com.tranphuloi.neon.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tranphuloi.neon.ui.game.controls.BossRank
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.metaDataStore by preferencesDataStore(name = "neon_meta")

private val LIFETIME_MINERALS_KEY = intPreferencesKey("lifetime_minerals")
private val LIFETIME_ENEMY_KILLS_KEY = longPreferencesKey("lifetime_enemy_kills")
private const val NODE_PREFIX = "node_"
private const val BULLET_KILL_PREFIX = "bullet_kill_"
private const val BOSS_KILL_PREFIX = "boss_kill_"
private const val SHIP_TIME_PREFIX = "ship_time_"
private const val RANK_DIST_PREFIX = "rank_dist_"

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

    // ── Wave 11b — DB metrics persistence ──
    //
    // Granularity:
    //   - per BulletType (12): how many enemies each bullet killed.
    //   - per BossKind (21): how many times each boss was defeated.
    //   - per ShipSkin (5): millis played with that skin (sums RUN durations).
    //   - per BossRank (5): how many times each rank (S/A/B/C/D) was achieved.
    //   - regular enemy lifetime kills (single Long): aggregate, no per-drawable
    //     breakdown — drawableIds aren't stable across builds, and named bosses
    //     already cover the per-kind detail.
    //
    // Recording is batched at GAME_OVER (see GameState.flushRunMetrics) to keep
    // hot-path DataStore writes off the loop. Each suspend method below is a
    // single atomic `edit { }` so concurrent flush calls don't tear values.

    val lifetimeEnemyKills: Flow<Long> = appContext.metaDataStore.data.map {
        it[LIFETIME_ENEMY_KILLS_KEY] ?: 0L
    }

    fun bulletKills(type: BulletType): Flow<Int> = appContext.metaDataStore.data.map {
        it[intPreferencesKey(BULLET_KILL_PREFIX + type.name)] ?: 0
    }

    fun bossKills(kind: BossKind): Flow<Int> = appContext.metaDataStore.data.map {
        it[intPreferencesKey(BOSS_KILL_PREFIX + kind.name)] ?: 0
    }

    fun shipTimeMillis(skin: ShipSkin): Flow<Long> = appContext.metaDataStore.data.map {
        it[longPreferencesKey(SHIP_TIME_PREFIX + skin.key)] ?: 0L
    }

    fun rankCount(rank: BossRank): Flow<Int> = appContext.metaDataStore.data.map {
        it[intPreferencesKey(RANK_DIST_PREFIX + rank.letter)] ?: 0
    }

    /** Aggregate snapshot of all bullet kill counts, keyed by BulletType. */
    val allBulletKills: Flow<Map<BulletType, Int>> = appContext.metaDataStore.data.map { prefs ->
        BulletType.entries.associateWith {
            prefs[intPreferencesKey(BULLET_KILL_PREFIX + it.name)] ?: 0
        }
    }

    /** Aggregate snapshot of all boss kill counts, keyed by BossKind. */
    val allBossKills: Flow<Map<BossKind, Int>> = appContext.metaDataStore.data.map { prefs ->
        BossKind.entries.associateWith {
            prefs[intPreferencesKey(BOSS_KILL_PREFIX + it.name)] ?: 0
        }
    }

    /** Aggregate snapshot of all ship time-played, keyed by ShipSkin. */
    val allShipTimeMillis: Flow<Map<ShipSkin, Long>> = appContext.metaDataStore.data.map { prefs ->
        ShipSkin.entries.associateWith {
            prefs[longPreferencesKey(SHIP_TIME_PREFIX + it.key)] ?: 0L
        }
    }

    /** Aggregate snapshot of all rank counts, keyed by BossRank. */
    val allRankCounts: Flow<Map<BossRank, Int>> = appContext.metaDataStore.data.map { prefs ->
        BossRank.entries.associateWith {
            prefs[intPreferencesKey(RANK_DIST_PREFIX + it.letter)] ?: 0
        }
    }

    /**
     * Batched flush of one run's metrics. Single atomic edit so a snapshot
     * read between fields can never see a partially-applied run.
     */
    suspend fun recordRunMetrics(
        regularEnemyKills: Int,
        bulletKills: Map<BulletType, Int>,
        bossKills: Map<BossKind, Int>,
        shipTimeMillisBySkin: Map<ShipSkin, Long>,
        ranksAchieved: List<BossRank>,
    ) {
        appContext.metaDataStore.edit { prefs ->
            if (regularEnemyKills > 0) {
                val before = prefs[LIFETIME_ENEMY_KILLS_KEY] ?: 0L
                prefs[LIFETIME_ENEMY_KILLS_KEY] = before + regularEnemyKills
            }
            bulletKills.forEach { (type, count) ->
                if (count > 0) {
                    val key = intPreferencesKey(BULLET_KILL_PREFIX + type.name)
                    prefs[key] = (prefs[key] ?: 0) + count
                }
            }
            bossKills.forEach { (kind, count) ->
                if (count > 0) {
                    val key = intPreferencesKey(BOSS_KILL_PREFIX + kind.name)
                    prefs[key] = (prefs[key] ?: 0) + count
                }
            }
            shipTimeMillisBySkin.forEach { (skin, millis) ->
                if (millis > 0L) {
                    val key = longPreferencesKey(SHIP_TIME_PREFIX + skin.key)
                    prefs[key] = (prefs[key] ?: 0L) + millis
                }
            }
            ranksAchieved.forEach { rank ->
                val key = intPreferencesKey(RANK_DIST_PREFIX + rank.letter)
                prefs[key] = (prefs[key] ?: 0) + 1
            }
            Logger.d(
                "MetaProgressionRepository.recordRunMetrics: regular=$regularEnemyKills " +
                    "bulletKinds=${bulletKills.count { it.value > 0 }} " +
                    "bossKinds=${bossKills.count { it.value > 0 }} " +
                    "skinKinds=${shipTimeMillisBySkin.count { it.value > 0 }} " +
                    "ranks=${ranksAchieved.size}"
            )
        }
    }
}

val LocalMetaProgression =
    androidx.compose.runtime.staticCompositionLocalOf<MetaProgressionRepository> {
        error("MetaProgressionRepository not provided.")
    }
