package com.tranphuloi.neon.ui.game.status

import androidx.compose.runtime.Immutable

/**
 * Wave 4 (41x) round 34 — status effect types applied to enemies.
 *
 * - BURN: damage-over-time, ticks every 500ms for [burnTickDamage] HP.
 * - SLOW: enemy movement speed multiplied by [slowMul] for duration.
 * - STUN: enemy can't fire lasers (frozen attack) for duration.
 *
 * Effects are NOT mutually exclusive — same enemy can be Burning + Slowed + Stunned simultaneously.
 *
 * Round 34: applied randomly via LasersController.onLaserHit (10% chance, random effect)
 * as initial wiring. Future: tied to specific bullet types (Cluster=burn,
 * Frost=slow, Lightning=stun) when BulletType refactor lands.
 */
@Immutable
enum class StatusEffect(
    val durationMs: Long,
    val tintColorArgb: Long,                // 0xAARRGGBB tint overlay color
) {
    BURN(durationMs = 3000L, tintColorArgb = 0x55FF6020),   // orange-red translucent
    SLOW(durationMs = 2500L, tintColorArgb = 0x4400D4FF),   // cyan translucent
    STUN(durationMs = 1200L, tintColorArgb = 0x55FFD400);   // yellow translucent

    companion object {
        const val BURN_TICK_DAMAGE: Float = 5f               // hp per 500ms tick
        const val BURN_TICK_INTERVAL_MS: Long = 500L
        const val SLOW_MOVEMENT_MUL: Float = 0.5f            // enemies move 50% speed while slowed
    }
}

/**
 * Active effect instance — one per (enemy, effect type) tuple. Re-apply
 * refreshes [expiresAtMillis] but does NOT stack damage.
 */
@Immutable
data class ActiveStatusEffect(
    val type: StatusEffect,
    val expiresAtMillis: Long,
    val lastBurnTickAtMillis: Long = 0L,
)
