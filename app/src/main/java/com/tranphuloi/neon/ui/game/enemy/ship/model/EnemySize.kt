package com.tranphuloi.neon.ui.game.enemy.ship.model

/**
 * Wave 13c — per-spawn-group size variety for regular enemies. A whole
 * formation shares ONE size (picked once per `EnemyFactory.invoke`) so rows /
 * V / sine waves stay visually uniform, but different groups differ:
 *
 *   - SMALL  (0.7×): smaller hitbox, less HP, FASTER — harder to hit, swarmy.
 *   - NORMAL (1.0×): baseline.
 *   - LARGE  (1.4×): bigger hitbox, more HP, SLOWER — a tankier target, rarer,
 *     drops more minerals.
 *
 * HP scales with size and movement speed scales inversely, so big = slow tank /
 * small = fast gnat. Hitbox follows `width` (= type.width × scale) in
 * `RegularEnemy.enemyRect()`, so collision stays honest (no hit-miss bug).
 *
 * Pure logic — `pick(roll)` takes a [0,1) roll so it's deterministic/testable.
 */
object EnemySize {
    const val SMALL = 0.7f
    const val NORMAL = 1.0f
    const val LARGE = 1.4f

    // Spawn weights: 20% small / 65% normal / 15% large. Large is intentionally
    // the minority so the screen isn't crowded with big sprites.
    const val SMALL_THRESHOLD = 0.20f
    const val LARGE_THRESHOLD = 0.85f // roll >= this → LARGE

    /** Pick a size scale from a uniform roll in [0,1). */
    fun pick(roll: Float): Float = when {
        roll < SMALL_THRESHOLD -> SMALL
        roll < LARGE_THRESHOLD -> NORMAL
        else -> LARGE
    }

    /** HP multiplier: larger = tankier. Linear in scale (0.7× / 1.0× / 1.4×). */
    fun hpFactor(scale: Float): Float = scale

    /**
     * Movement-speed multiplier: larger = slower, smaller = faster. `1/scale`
     * (small 0.7→1.43×, large 1.4→0.71×), clamped so extremes stay sane.
     */
    fun speedFactor(scale: Float): Float = (1f / scale).coerceIn(0.6f, 1.6f)

    /** Minerals dropped: large enemies are worth more. */
    fun mineralReward(scale: Float): Int = if (scale >= LARGE) 2 else 1
}
