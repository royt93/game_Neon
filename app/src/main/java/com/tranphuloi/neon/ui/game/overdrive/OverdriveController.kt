package com.tranphuloi.neon.ui.game.overdrive

import com.tranphuloi.neon.utils.Logger

/**
 * Task 31 — Overdrive bullet-time. Tracks an independent kill counter (does
 * NOT decay like [com.tranphuloi.neon.ui.game.combo.ComboController.count] —
 * dodging instead of attacking must not drain Overdrive progress). Once
 * [threshold] kills accumulate, activates a fixed-duration window during
 * which enemy/enemy-laser/space-object tinker ticks are partially skipped
 * (see `GameState.kt`'s `overdriveSkipTick`) for a bullet-time feel.
 */
class OverdriveController(
    private val threshold: Int = 20,
    private val durationMillis: Long = 4000L,
) {
    var killCount: Int = 0
        private set
    var activeUntilMillis: Long = 0L
        private set
    private var wasActive: Boolean = false

    /** Returns true exactly on the tick Overdrive triggers (caller fires start SFX). */
    fun onEnemyKilled(now: Long): Boolean {
        killCount++
        if (killCount >= threshold) {
            killCount = 0
            activeUntilMillis = now + durationMillis
            wasActive = true
            Logger.d("OverdriveController: TRIGGERED, active until $activeUntilMillis")
            return true
        }
        return false
    }

    fun isActive(now: Long): Boolean = now < activeUntilMillis

    /** Returns true exactly on the tick Overdrive's active window ends (caller fires end SFX). */
    fun checkExpiry(now: Long): Boolean {
        val active = isActive(now)
        val justExpired = wasActive && !active
        if (justExpired) {
            Logger.d("OverdriveController: expired")
        }
        wasActive = active
        return justExpired
    }
}
