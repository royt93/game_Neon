package com.tranphuloi.neon.ui.game.status

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.LightningChain
import com.tranphuloi.neon.utils.Logger

/**
 * Task 18 Slice 1 — lọc các địch (trừ [excludeEnemyId]) nằm trong [radius] quanh
 * ([originX], [originY]). Pure function, không side-effect — dùng cho status chain-spread
 * và unit test độc lập. [radius] mặc định dùng chung [LightningChain.RADIUS] (cùng 120f)
 * để tránh 2 hằng số bán kính lệch nhau khi tune balance sau này.
 */
internal fun enemiesInChainRadius(
    enemies: List<Enemy>,
    originX: Float,
    originY: Float,
    excludeEnemyId: String,
    radius: Float = LightningChain.RADIUS,
): List<Enemy> = enemies.filter { enemy ->
    if (enemy.enemyId == excludeEnemyId) return@filter false
    if (enemy.destroyed || enemy.hp <= 0f) return@filter false
    val ex = enemy.xOffset + enemy.width / 2
    val ey = enemy.yOffset + enemy.height / 2
    val dx = ex - originX
    val dy = ey - originY
    kotlin.math.sqrt(dx * dx + dy * dy) <= radius
}

/**
 * Wave 4 (41x) round 34 — manages active status effects per enemy.
 *
 * Storage: `Map<enemyId, MutableList<ActiveStatusEffect>>`. Re-applying same
 * type to same enemy refreshes the expiry (doesn't stack damage).
 *
 * Tick: called from game loop periodically. Drops expired effects + applies
 * BURN damage. Returns delta to apply to enemy hp.
 *
 * Concurrency: not thread-safe but game loop is single-threaded (IO dispatcher
 * sequential tinker calls).
 */
class StatusEffectController {

    private val effectsByEnemy: MutableMap<String, MutableList<ActiveStatusEffect>> =
        mutableMapOf()

    /**
     * Task 18 Slice 1 — implementation of the neighbor-spread step, wired in by
     * GameState once the live `enemies` list is in scope (same "assign the real
     * behavior later" idea as the game loop's other deferred refs). `applyWithChain`
     * invokes this for chainable effects; left `null` it's simply a no-op (e.g. in
     * unit tests that only exercise [apply]).
     */
    var chainSpreadHandler: ((enemyId: String, type: StatusEffect, hitX: Float, hitY: Float, nowMillis: Long) -> Unit)? = null

    init {
        Logger.d("StatusEffectController init")
    }

    /**
     * Apply [type] to [enemy]. If already active, refresh expiry but don't stack.
     */
    fun apply(enemyId: String, type: StatusEffect, nowMillis: Long) {
        val list = effectsByEnemy.getOrPut(enemyId) { mutableListOf() }
        val existing = list.indexOfFirst { it.type == type }
        val refreshed = ActiveStatusEffect(
            type = type,
            expiresAtMillis = nowMillis + type.durationMs,
            lastBurnTickAtMillis = nowMillis,
        )
        if (existing >= 0) {
            list[existing] = refreshed
            Logger.v { "StatusEffect: refresh $type on enemy=${enemyId.take(6)} (expires=${refreshed.expiresAtMillis})" }
        } else {
            list.add(refreshed)
            Logger.v { "StatusEffect: apply $type on enemy=${enemyId.take(6)} (expires=${refreshed.expiresAtMillis})" }
        }
    }

    /**
     * Task 18 Slice 1 — primary entrypoint for hit-driven status application.
     * Applies [type] to [enemyId], then — only when [type] is [StatusEffect.chainable] —
     * runs [chainSpreadHandler] once to probabilistically spread it to nearby enemies.
     * This is the only public path that can trigger chain-spread; the neighbor applies
     * inside [chainSpreadHandler] call [apply] directly (not this method) so a single
     * hit only ever spreads 1 hop, never re-chains.
     */
    fun applyWithChain(enemyId: String, type: StatusEffect, nowMillis: Long, hitX: Float, hitY: Float) {
        apply(enemyId, type, nowMillis)
        if (type.chainable) chainSpreadHandler?.invoke(enemyId, type, hitX, hitY, nowMillis)
    }

    /** Active effect types currently affecting [enemyId]. Empty if none. */
    fun effectsFor(enemyId: String): Set<StatusEffect> {
        val list = effectsByEnemy[enemyId] ?: return emptySet()
        return list.map { it.type }.toSet()
    }

    /** True if enemy is currently slowed → movement controllers should apply SLOW_MOVEMENT_MUL. */
    fun isSlowed(enemyId: String): Boolean =
        effectsByEnemy[enemyId]?.any { it.type == StatusEffect.SLOW } == true

    /** True if enemy is stunned → skip generateLasers call. */
    fun isStunned(enemyId: String): Boolean =
        effectsByEnemy[enemyId]?.any { it.type == StatusEffect.STUN } == true

    /**
     * Tick all active effects against [enemies]. Apply BURN damage, drop
     * expired entries, clean up entries for destroyed/missing enemies.
     *
     * Returns a Map<enemyId, totalBurnDamageThisTick> for callers to apply
     * to enemy.hp.
     */
    fun processTick(enemies: List<Enemy>, nowMillis: Long): Map<String, Float> {
        if (effectsByEnemy.isEmpty()) return emptyMap()
        val burnDamage = mutableMapOf<String, Float>()
        val aliveIds = enemies.asSequence().map { it.enemyId }.toSet()
        val iter = effectsByEnemy.entries.iterator()
        while (iter.hasNext()) {
            val (enemyId, list) = iter.next()
            if (enemyId !in aliveIds) {
                iter.remove()
                continue
            }
            val effIter = list.iterator()
            while (effIter.hasNext()) {
                val eff = effIter.next()
                if (nowMillis >= eff.expiresAtMillis) {
                    effIter.remove()
                    Logger.v { "StatusEffect: expire ${eff.type} on enemy=${enemyId.take(6)}" }
                    continue
                }
                // Wave 18 — BURN (Lửa) + CORROSION (Nước Mắm) đều là DoT cùng nhịp
                // 500ms, khác lượng sát thương mỗi tick. Dùng chung lastBurnTickAtMillis
                // (mỗi instance là 1 type riêng → không lẫn nhịp).
                if (eff.type == StatusEffect.BURN || eff.type == StatusEffect.CORROSION) {
                    val sinceLastTick = nowMillis - eff.lastBurnTickAtMillis
                    if (sinceLastTick >= StatusEffect.BURN_TICK_INTERVAL_MS) {
                        val tickDmg = if (eff.type == StatusEffect.CORROSION) {
                            StatusEffect.CORROSION_TICK_DAMAGE
                        } else {
                            StatusEffect.BURN_TICK_DAMAGE
                        }
                        burnDamage[enemyId] = (burnDamage[enemyId] ?: 0f) + tickDmg
                        // Mutate the active effect's lastBurnTickAtMillis
                        val idx = list.indexOf(eff)
                        list[idx] = eff.copy(lastBurnTickAtMillis = nowMillis)
                    }
                }
            }
            if (list.isEmpty()) iter.remove()
        }
        return burnDamage
    }

    /** Drop all effects for [enemyId] (e.g. enemy destroyed). */
    fun clearFor(enemyId: String) {
        if (effectsByEnemy.remove(enemyId) != null) {
            Logger.v { "StatusEffect: cleared all effects for enemy=${enemyId.take(6)} (destroyed/removed)" }
        }
    }

    /** Snapshot for UI/debug. Size = total active effects across all enemies. */
    fun totalActive(): Int = effectsByEnemy.values.sumOf { it.size }

    companion object {
        /** Task 18 Slice 1 — per-neighbor probability of spreading a chainable effect. */
        const val CHAIN_SPREAD_CHANCE: Float = 0.35f
    }
}
