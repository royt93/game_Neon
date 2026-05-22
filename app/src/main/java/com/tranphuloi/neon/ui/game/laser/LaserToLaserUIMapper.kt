package com.tranphuloi.neon.ui.game.laser

import com.tranphuloi.neon.ui.game.ship.laser.LaserUI

/**
 * Round 48 — id-keyed memoization. Identical pattern to
 * [com.tranphuloi.neon.ui.game.enemy.ship.mapper.EnemyToEnemyUIMapper] —
 * compare field-by-field against the cached snapshot before allocating a new
 * [LaserUI]. Most lasers move every tick → cache miss every tick. The win is
 * for stationary lasers (e.g., mid-flight pierce-pending) and for the cleanup
 * tick where the list contains lasers that haven't moved yet.
 */
class LaserToLaserUIMapper {

    /**
     * Round 48 audit — LRU [LinkedHashMap] (access-order) auto-evicts eldest
     * when size > [CACHE_CAPACITY]. Combined laser caps (ship 25 + ultimate ~9
     * + enemy 30 = ~64 alive) + headroom for recently-killed lasers fits
     * comfortably in 128. Lasers die fast so the LRU drops them quickly.
     */
    private val cache: LinkedHashMap<String, LaserUI> =
        object : LinkedHashMap<String, LaserUI>(128, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, LaserUI>?): Boolean =
                size > CACHE_CAPACITY
        }

    operator fun invoke(laser: Laser): LaserUI {
        val cached = cache[laser.id]
        if (cached != null &&
            cached.xOffset == laser.xOffset &&
            cached.yOffset == laser.yOffset &&
            cached.width == laser.width &&
            cached.height == laser.height &&
            cached.rotation == laser.rotation &&
            cached.drawableId == laser.drawableId
        ) {
            return cached
        }
        val newUi = with(laser) {
            LaserUI(
                id = id,
                xOffset = xOffset,
                yOffset = yOffset,
                width = width,
                height = height,
                rotation = rotation,
                drawableId = drawableId
            )
        }
        cache[laser.id] = newUi
        return newUi
    }

    /** Round 48 — release entries for ids not currently active. Optional given
     *  the LRU auto-eviction; left for callers who want tighter bounds. */
    fun trimDead(aliveIds: Set<String>) {
        if (cache.isEmpty()) return
        val it = cache.entries.iterator()
        while (it.hasNext()) {
            if (it.next().key !in aliveIds) it.remove()
        }
    }

    companion object {
        /** Round 48 — LRU bound. Combined ship + ultimate + enemy lasers ≤ ~64. */
        const val CACHE_CAPACITY = 128
    }
}
