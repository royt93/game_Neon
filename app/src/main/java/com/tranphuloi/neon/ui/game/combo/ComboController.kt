package com.tranphuloi.neon.ui.game.combo

import com.tranphuloi.neon.utils.Logger

/**
 * Tracks consecutive enemy kills within a sliding time window. Resets if the
 * gap between kills exceeds [resetWindowMillis]. Tier increases at thresholds.
 */
class ComboController(
    private val resetWindowMillis: Long = 2000L,
    private val onTierAdvance: (tier: ComboTier) -> Unit = {},
) {
    var count: Int = 0
        private set
    var lastKillMillis: Long = 0L
        private set
    private var lastTier: ComboTier = ComboTier.NONE

    fun onEnemyKilled(): ComboTier {
        val now = System.currentTimeMillis()
        if (now - lastKillMillis > resetWindowMillis) {
            if (count > 0) Logger.d("ComboController: reset (gap=${now - lastKillMillis}ms)")
            count = 0
        }
        count++
        lastKillMillis = now
        val tier = ComboTier.forCount(count)
        if (tier != lastTier && tier != ComboTier.NONE) {
            Logger.d("ComboController: tier advance ${lastTier.name} → ${tier.name} (count=$count)")
            onTierAdvance(tier)
        }
        lastTier = tier
        return tier
    }

    fun checkExpiry() {
        if (count == 0) return
        val now = System.currentTimeMillis()
        if (now - lastKillMillis > resetWindowMillis) {
            Logger.d("ComboController: expired (count was=$count)")
            count = 0
            lastTier = ComboTier.NONE
        }
    }

    /** Multiplier applied to score per kill at this tier. */
    fun multiplier(): Int = ComboTier.forCount(count).multiplier

    fun currentTier(): ComboTier = ComboTier.forCount(count)
}

enum class ComboTier(val threshold: Int, val multiplier: Int, val displayLabel: String) {
    NONE(0, 1, ""),
    DOUBLE(2, 2, "DOUBLE KILL"),
    TRIPLE(3, 3, "TRIPLE KILL"),
    RAMPAGE(5, 4, "RAMPAGE"),
    UNSTOPPABLE(8, 5, "UNSTOPPABLE"),
    GODLIKE(12, 8, "GODLIKE");

    companion object {
        fun forCount(count: Int): ComboTier {
            return when {
                count >= GODLIKE.threshold -> GODLIKE
                count >= UNSTOPPABLE.threshold -> UNSTOPPABLE
                count >= RAMPAGE.threshold -> RAMPAGE
                count >= TRIPLE.threshold -> TRIPLE
                count >= DOUBLE.threshold -> DOUBLE
                else -> NONE
            }
        }
    }
}
