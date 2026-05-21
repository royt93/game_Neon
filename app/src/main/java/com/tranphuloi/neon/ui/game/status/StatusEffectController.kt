package com.tranphuloi.neon.ui.game.status

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.utils.Logger

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
            Logger.d("StatusEffect: refresh $type on enemy=${enemyId.take(6)} (expires=${refreshed.expiresAtMillis})")
        } else {
            list.add(refreshed)
            Logger.d("StatusEffect: apply $type on enemy=${enemyId.take(6)} (expires=${refreshed.expiresAtMillis})")
        }
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
                    Logger.d("StatusEffect: expire ${eff.type} on enemy=${enemyId.take(6)}")
                    continue
                }
                if (eff.type == StatusEffect.BURN) {
                    val sinceLastTick = nowMillis - eff.lastBurnTickAtMillis
                    if (sinceLastTick >= StatusEffect.BURN_TICK_INTERVAL_MS) {
                        burnDamage[enemyId] =
                            (burnDamage[enemyId] ?: 0f) + StatusEffect.BURN_TICK_DAMAGE
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
            Logger.d("StatusEffect: cleared all effects for enemy=${enemyId.take(6)} (destroyed/removed)")
        }
    }

    /** Snapshot for UI/debug. Size = total active effects across all enemies. */
    fun totalActive(): Int = effectsByEnemy.values.sumOf { it.size }
}
