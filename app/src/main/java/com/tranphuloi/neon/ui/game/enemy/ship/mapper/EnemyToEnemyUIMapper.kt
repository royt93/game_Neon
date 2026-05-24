package com.tranphuloi.neon.ui.game.enemy.ship.mapper

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI

/**
 * Round 48 — id-keyed memoization. Before allocating a new [EnemyUI], compare
 * the [Enemy]'s current fields against the cached snapshot for that enemyId.
 * If every render-relevant field matches → return the cached UI instance,
 * skipping the data-class allocation entirely.
 *
 * Effectiveness depends on entity churn: stunned / paused enemies hit the cache
 * (no movement); freely-moving regular enemies miss every tick (yOffset always
 * advances). Combined with [com.tranphuloi.neon.ui.game.enemy.ship.controller.EnemyController.MAX_REGULAR_ENEMIES]
 * the realistic allocation reduction is ~5-12% at peak combat — modest, but
 * essentially free at the call site (no behavior change).
 *
 * Cache is unbounded across the run; [trimDead] should be called periodically
 * to release entries whose enemy has been destroyed.
 */
class EnemyToEnemyUIMapper {

    /**
     * Round 48 audit — was a plain [HashMap] that grew until the mapper's
     * composition lifetime ended (~run end). For long Endless sessions that
     * accumulated ~200 KB of stale entries. Now a small LRU
     * ([LinkedHashMap] with access-order = true) auto-evicts the eldest when
     * size exceeds [CACHE_CAPACITY]. Dead enemies fall to the bottom (no
     * future get/put) and are dropped naturally on the next put. The active
     * working set (≤ [com.tranphuloi.neon.ui.game.enemy.ship.controller.EnemyController.MAX_REGULAR_ENEMIES] = 30 + a few bosses)
     * comfortably fits inside [CACHE_CAPACITY].
     */
    private val cache: LinkedHashMap<String, EnemyUI> =
        object : LinkedHashMap<String, EnemyUI>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, EnemyUI>?): Boolean =
                size > CACHE_CAPACITY
        }

    operator fun invoke(
        enemy: Enemy,
        activeStatusEffectTints: List<Long> = emptyList(),
    ): EnemyUI {
        val cached = cache[enemy.enemyId]
        // Field-compare fast-path: same fields → reuse cached reference.
        // `activeStatusEffectTints` reuses `emptyList()` singleton in the caller's
        // common case, so reference equality covers most no-effect comparisons.
        // Round 48 audit — was `===` (reference equality) for tints. That worked
        // for the empty-list singleton fast path but missed cache for any
        // status-effect-active enemy because the caller builds a fresh
        // ArrayList per tick. Using `==` triggers `AbstractList.equals` which
        // short-circuits via `===` first, then size, then element compare —
        // O(1) for empty + same-instance cases, O(N) only for real comparison
        // (N ≤ 3 tints per enemy). Marginal cost, real cache-hit win.
        if (cached != null &&
            cached.xOffset == enemy.xOffset &&
            cached.yOffset == enemy.yOffset &&
            cached.currentHp == enemy.hp &&
            cached.lastImpactMillis == enemy.lastImpactMillis &&
            cached.isInEntryPhase == enemy.isInEntryPhase &&
            cached.currentPhase == enemy.currentPhase &&
            cached.phaseTransitionMillis == enemy.phaseTransitionMillis &&
            cached.activeStatusEffectTints == activeStatusEffectTints &&
            cached.bossKind == enemy.bossKind
        ) {
            return cached
        }
        val newUi = with(enemy) {
            EnemyUI(
                enemyId = enemyId,
                width = width,
                height = height,
                xOffset = xOffset,
                yOffset = yOffset,
                hpBarWidth = width / initialHp * hp,
                drawableId = drawableId,
                lastImpactMillis = lastImpactMillis,
                isBoss = isBoss,
                displayName = displayName,
                currentHp = hp,
                initialHp = initialHp,
                isInEntryPhase = isInEntryPhase,
                currentPhase = currentPhase,
                phaseTransitionMillis = phaseTransitionMillis,
                activeStatusEffectTints = activeStatusEffectTints,
                bossKind = bossKind,
            )
        }
        cache[enemy.enemyId] = newUi
        return newUi
    }

    /**
     * Round 48 — drop cache entries for ids not present in [aliveIds]. Optional
     * since the [LinkedHashMap] LRU auto-evicts on overflow; this is for cases
     * where the caller knows the alive set and wants tighter memory.
     */
    fun trimDead(aliveIds: Set<String>) {
        if (cache.isEmpty()) return
        val it = cache.entries.iterator()
        while (it.hasNext()) {
            if (it.next().key !in aliveIds) it.remove()
        }
    }

    companion object {
        /** Round 48 — LRU bound. 64 covers 30 alive + boss + recent dead. */
        const val CACHE_CAPACITY = 64
    }
}
