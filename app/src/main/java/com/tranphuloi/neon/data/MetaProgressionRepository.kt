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

// `internal` (was private) so the Robolectric integration test can clear state
// between cases — the DataStore is a process singleton keyed by file name.
internal val Context.metaDataStore by preferencesDataStore(name = "neon_meta")

private val LIFETIME_MINERALS_KEY = intPreferencesKey("lifetime_minerals")
private val LIFETIME_ENEMY_KILLS_KEY = longPreferencesKey("lifetime_enemy_kills")
// Wave 21 (#4 kinh tế khoáng) — điểm danh hằng ngày: ngày claim gần nhất
// (epoch-day) + chuỗi ngày liên tiếp (streak) để thưởng tăng dần.
private val LAST_DAILY_CLAIM_KEY = longPreferencesKey("last_daily_claim_day")
private val DAILY_STREAK_KEY = intPreferencesKey("daily_streak")
private const val NODE_PREFIX = "node_"
private const val BULLET_KILL_PREFIX = "bullet_kill_"
private const val BOSS_KILL_PREFIX = "boss_kill_"
private const val SHIP_TIME_PREFIX = "ship_time_"
private const val RANK_DIST_PREFIX = "rank_dist_"
// Task 03 — XP tích luỹ mỗi tàu (key = shipShape.key). Level suy ra qua ShipXpLevels.
private const val SHIP_XP_PREFIX = "shipxp_"
// Wave 12 round 2 — shop persistence. Rank-based items reuse NODE_PREFIX
// (each shop item appears as a "node" in DataStore with prefix `node_shop_*`),
// CONSUMABLE items get their own STOCKPILE_PREFIX namespace so a consumable
// stock counter can never collide with a skill-tree rank counter.
internal const val STOCKPILE_PREFIX = "stockpile_"

/**
 * Pure spend predicate — extracted so tests can pin the decision logic
 * without standing up DataStore. Used inside `spendOnNode`'s atomic edit.
 */
internal fun canSpendOnNode(balance: Int, cost: Int, currentRank: Int, maxRank: Int): Boolean =
    cost >= 0 && balance >= cost && currentRank < maxRank

/** Same idea for stockpile spend (consumables). */
internal fun canSpendOnStockpile(balance: Int, cost: Int, addAmount: Int): Boolean =
    cost >= 0 && balance >= cost && addAmount > 0

/**
 * Wave 21 (#4) — thưởng điểm danh theo chuỗi ngày. Base 50◇, +25◇/ngày liên
 * tiếp, trần ở ngày 7 (200◇). Hàm thuần để test pin con số.
 */
internal fun dailyRewardFor(streak: Int): Int = 50 + (streak.coerceIn(1, 7) - 1) * 25

/**
 * Wave 22 (#4) — thưởng mốc màn cuối run: mỗi 5 màn đạt = +30◇, trần 300◇
 * (màn 50+). Cộng vào banking ở GAME_OVER, thưởng đi xa. Hàm thuần để test.
 */
internal fun stageMilestoneBonus(stagesReached: Int): Int =
    ((stagesReached.coerceAtLeast(0) / 5) * 30).coerceAtMost(300)

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

    // ── Task 03 — XP tích luỹ mỗi tàu ──

    /** XP tích luỹ của 1 tàu (theo `shipShape.key`). 0 nếu chưa có. */
    fun shipXp(shipKey: String): Flow<Int> = appContext.metaDataStore.data.map {
        it[intPreferencesKey(SHIP_XP_PREFIX + shipKey)] ?: 0
    }

    /** Toàn bộ XP tàu (key = shipShape.key → xp) — cho UI shop hiển thị nhiều tàu. */
    val allShipXp: Flow<Map<String, Int>> = appContext.metaDataStore.data.map { prefs ->
        prefs.asMap()
            .filterKeys { it.name.startsWith(SHIP_XP_PREFIX) }
            .mapKeys { it.key.name.removePrefix(SHIP_XP_PREFIX) }
            .mapValues { (it.value as? Int) ?: 0 }
    }

    /** Cộng [amount] XP cho tàu [shipKey] (atomic). No-op nếu amount ≤ 0. */
    suspend fun addShipXp(shipKey: String, amount: Int) {
        if (amount <= 0) return
        appContext.metaDataStore.edit { prefs ->
            val key = intPreferencesKey(SHIP_XP_PREFIX + shipKey)
            val before = prefs[key] ?: 0
            prefs[key] = before + amount
            Logger.d("MetaProgressionRepository.addShipXp[$shipKey] +$amount → ${before + amount}")
        }
    }

    // ── Wave 21 (#4) — điểm danh hằng ngày ──

    /** Streak hiện tại (số ngày liên tiếp đã điểm danh). */
    val dailyStreak: Flow<Int> = appContext.metaDataStore.data.map { it[DAILY_STREAK_KEY] ?: 0 }

    /** Còn quà điểm danh hôm nay không (chưa claim trong ngày [today]). */
    fun dailyClaimAvailable(today: Long): Flow<Boolean> =
        appContext.metaDataStore.data.map { (it[LAST_DAILY_CLAIM_KEY] ?: -1L) != today }

    /**
     * Điểm danh ngày [today] (epoch-day). Trả về số khoáng được thưởng (0 nếu đã
     * claim hôm nay). Streak +1 nếu claim đúng ngày kế tiếp, ngược lại reset về 1.
     * Atomic: cộng thẳng vào balance trong cùng edit.
     */
    suspend fun claimDaily(today: Long): Int {
        var granted = 0
        appContext.metaDataStore.edit { prefs ->
            val last = prefs[LAST_DAILY_CLAIM_KEY] ?: -1L
            if (last == today) return@edit                       // đã điểm danh hôm nay
            val prevStreak = prefs[DAILY_STREAK_KEY] ?: 0
            val newStreak = if (last == today - 1L) prevStreak + 1 else 1
            val reward = dailyRewardFor(newStreak)
            prefs[LAST_DAILY_CLAIM_KEY] = today
            prefs[DAILY_STREAK_KEY] = newStreak
            prefs[LIFETIME_MINERALS_KEY] = (prefs[LIFETIME_MINERALS_KEY] ?: 0) + reward
            granted = reward
            Logger.d("MetaProgressionRepository.claimDaily day=$today streak=$newStreak +$reward◇")
        }
        return granted
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
            if (canSpendOnNode(balance, cost, currentRank, maxRank)) {
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

    /**
     * Wave 13a — grant a node ownership flag WITHOUT charging (rank → 1), only
     * if not already owned. Used by the ship-unlock migration (threshold-gate →
     * purchase): the player's currently-selected ship is auto-granted so it
     * keeps working. Idempotent — a node already at rank>0 is left untouched, so
     * this is safe to call on every ship-tab load. Returns true if it granted.
     */
    suspend fun grantNodeFree(nodeKey: String): Boolean {
        var granted = false
        appContext.metaDataStore.edit { prefs ->
            val nodePrefKey = intPreferencesKey(NODE_PREFIX + nodeKey)
            val currentRank = prefs[nodePrefKey] ?: 0
            if (currentRank == 0) {
                prefs[nodePrefKey] = 1
                granted = true
                Logger.d("MetaProgressionRepository.grantNodeFree($nodeKey): rank 0 → 1 (migration, no charge)")
            }
        }
        return granted
    }

    /**
     * Wave 12 round 2 — shop CONSUMABLE category. Atomic deduction +
     * stockpile increment. Returns true on success, false if balance < cost
     * (or `addAmount <= 0`, defensive). Stockpile reads via [stockpileCount].
     */
    suspend fun spendOnStockpile(stockpileKey: String, cost: Int, addAmount: Int): Boolean {
        var success = false
        appContext.metaDataStore.edit { prefs ->
            val balance = prefs[LIFETIME_MINERALS_KEY] ?: 0
            val prefKey = intPreferencesKey(STOCKPILE_PREFIX + stockpileKey)
            val current = prefs[prefKey] ?: 0
            if (canSpendOnStockpile(balance, cost, addAmount)) {
                prefs[LIFETIME_MINERALS_KEY] = balance - cost
                prefs[prefKey] = current + addAmount
                success = true
                Logger.d("MetaProgressionRepository.spendOnStockpile($stockpileKey, cost=$cost, +$addAmount): stock $current → ${current + addAmount}, balance $balance → ${balance - cost}")
            } else {
                Logger.d("MetaProgressionRepository.spendOnStockpile($stockpileKey, cost=$cost) DENIED — balance=$balance addAmount=$addAmount")
            }
        }
        return success
    }

    /** Current stockpile count for a consumable key. */
    fun stockpileCount(stockpileKey: String): Flow<Int> = appContext.metaDataStore.data.map {
        it[intPreferencesKey(STOCKPILE_PREFIX + stockpileKey)] ?: 0
    }

    /**
     * Wave 12 round 3 — decrement a consumable stockpile when it's spent into a
     * run (e.g., bombs folded into the run's starting count, a revive token
     * granted at spawn). Clamped at 0 so a double-fire can never push the count
     * negative. No-op for `amount <= 0`.
     */
    suspend fun consumeStockpile(stockpileKey: String, amount: Int) {
        if (amount <= 0) return
        appContext.metaDataStore.edit { prefs ->
            val prefKey = intPreferencesKey(STOCKPILE_PREFIX + stockpileKey)
            val current = prefs[prefKey] ?: 0
            val next = (current - amount).coerceAtLeast(0)
            prefs[prefKey] = next
            Logger.d("MetaProgressionRepository.consumeStockpile($stockpileKey, -$amount): $current → $next")
        }
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
