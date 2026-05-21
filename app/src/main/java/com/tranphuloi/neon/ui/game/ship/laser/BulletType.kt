package com.tranphuloi.neon.ui.game.ship.laser

import androidx.compose.runtime.Immutable

/**
 * Wave 4 (35x) round 35 — bullet type the ship is currently firing.
 *
 * - NORMAL: existing ShipLaser / ShipBoostedLaser flow. Single-hit destroy on impact.
 * - PIERCING: laser passes through enemies, max 3 hits then destroy.
 * - PLASMA: bigger laser, +60% damage, AoE explosion radius 80dp on impact.
 *   Hit enemies in radius take 50% laser damage.
 *
 * Activated via booster pickup (PIERCING_BOOSTER / PLASMA_BOOSTER). Each
 * activation lasts [activeDurationMillis]. NORMAL is the default fallback.
 *
 * HOMING bullet (auto-target nearest enemy) was scoped but deferred — needs
 * per-tick target tracking via x/yVelocity refactor of Laser.
 */
@Immutable
enum class BulletType(
    val displayName: String,
    val activeDurationMillis: Long,
    val damageMultiplier: Float,
    val pierceCount: Int,
    val aoeRadius: Float,
    val glyph: String,
) {
    NORMAL(
        displayName = "Đạn thường",
        activeDurationMillis = 0L,                  // not timed; default
        damageMultiplier = 1f,
        pierceCount = 0,
        aoeRadius = 0f,
        glyph = "•",
    ),
    PIERCING(
        displayName = "Xuyên",
        activeDurationMillis = 10_000L,
        damageMultiplier = 1f,
        pierceCount = 3,                            // hits up to 3 enemies before destroy
        aoeRadius = 0f,
        glyph = "→",
    ),
    PLASMA(
        displayName = "Plasma",
        activeDurationMillis = 10_000L,
        damageMultiplier = 1.6f,
        pierceCount = 0,
        aoeRadius = 80f,                            // damage radius on impact
        glyph = "◯",
    );
}
