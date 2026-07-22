package com.tranphuloi.neon.ui.game.ship.ship

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.tranphuloi.neon.R
import java.io.Serializable

@Keep
@Immutable
data class Ship(
    val width: Float = 85f,
    val height: Float = 90f,
    val shieldSize: Float = height * 2,
    val shieldEnabled: Boolean = false,
    val laserBoosterEnabled: Boolean = false,
    val tripleLaserBoosterEnabled: Boolean = false,
    val shieldEndMillis: Long = 0L,
    val laserBoosterEndMillis: Long = 0L,
    val tripleLaserBoosterEndMillis: Long = 0L,
    // Round 35 (35x) — active bullet type override. Default NORMAL = existing
    // ShipLaser/ShipBoostedLaser flow. Set by PIERCING_BOOSTER / PLASMA_BOOSTER
    // pickup with timed end-millis. ShipController clears back to NORMAL on expiry.
    val activeBulletType: com.tranphuloi.neon.ui.game.ship.laser.BulletType =
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL,
    val bulletTypeEndMillis: Long = 0L,
    /**
     * Wave 14 — the loadout-selected bullet, applied for the WHOLE run (no
     * 10s head-start expiry). A bullet BOOSTER temporarily overrides
     * [activeBulletType] with a timed window; when it expires ShipController
     * reverts to this base (not NORMAL), so the player keeps their chosen weapon.
     */
    val baseBulletType: com.tranphuloi.neon.ui.game.ship.laser.BulletType =
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL,
    /**
     * Round 52 (40x Item combos) — rarity of the booster that activated the
     * current [activeBulletType]. Used to tier-up combat behaviour:
     *  - PIERCING: pierceCount scales 3/4/5 for Common/Rare/Epic.
     *  - PLASMA: AoE radius scales ×1.0 / ×1.375 / ×1.75.
     * Loadout head-start (round 45) defaults to COMMON.
     */
    val activeBulletTypeRarity: com.tranphuloi.neon.ui.game.booster.BoosterRarity =
        com.tranphuloi.neon.ui.game.booster.BoosterRarity.COMMON,
    val xOffset: Float,
    val yOffset: Float,
    val hp: Int = 1000,
    // Spawn cinematic transforms — driven by ShipController during fly-in.
    // Default values (1, 0, 1) are the "live" steady state once spawn finishes.
    val spawnAlpha: Float = 0f,
    val spawnRotation: Float = 0f,
    val spawnScale: Float = 0.6f,
    // Banking rotation — tilts ±16° when player is moving left/right.
    // Lerped each tick by ShipController for smooth feel.
    val bankRotation: Float = 0f,
    // Set when ship is destroyed (hp→0). Drives implosion animation.
    val destroyedAtMillis: Long = 0L,
    // Explicit gate to remove ship sprite + shield + glow + flame from composition.
    // Why: time-based `destroyTickMillis` tick can be smart-skipped if ship reference
    // is unchanged, so the visible Ship lingers. Mutating this flag changes the Ship
    // reference itself → guaranteed recompose + Box removed via `if (!hidden)`.
    val shipSpriteHidden: Boolean = false,
    // Task 05 — kỹ năng chủ động: mốc hết hiệu ứng (wall-clock millis, 0 = tắt).
    // Timer RIÊNG của ability (khác field booster buff shieldEnabled/berserk…):
    //  - invuln: bỏ qua sát thương địch (BULWARK/PHASE_DASH/DECOY)
    //  - overdrive: nhân sát thương đạn (OVERDRIVE)
    //  - crit: nhân sát thương mạnh hơn (CRIT_FRENZY)
    //  - freeze: đóng băng địch di chuyển + bắn (TIME_DILATION)
    val abilityInvulnEndMillis: Long = 0L,
    val abilityOverdriveEndMillis: Long = 0L,
    val abilityCritEndMillis: Long = 0L,
    val abilityFreezeEndMillis: Long = 0L,
    // 14c Auto-revive token — set when REVIVE_TOKEN booster picked up. On hp→0,
    // controller consumes the token: hp restored to 300 + 1.5s i-frames + banner.
    // Max one stored at a time; further pickups while held are wasted (rare anyway).
    val hasReviveToken: Boolean = false,
    // Task 24 — SECOND_WIND skill node one-time revive. Reset to false every
    // run since Ship is constructed fresh each time Game.route is entered.
    val secondWindUsed: Boolean = false,
    // Round 60 (38x) — 4 timed flags surfaced on Ship so render/laser code can
    // observe them without reaching into ShipController's private state. End
    // timestamps are kept private in ShipController; the booleans are flipped
    // by enable/expire methods. All default false (no buff active).
    val spreadShotEnabled: Boolean = false,
    val doubleFireEnabled: Boolean = false,
    val critSurgeEnabled: Boolean = false,
    val berserkEnabled: Boolean = false,
    /**
     * Round 61 — wall-clock millis when PHASE_SHIELD buff expires. 0 = inactive.
     * Drives a translucent cyan ghost overlay in GameWorld so player can see
     * the buff is active (Round 60 only extended `iframesEndMillis` silently —
     * indistinguishable from the 600ms damage iframes that flash on every hit).
     */
    val phaseShieldEndMillis: Long = 0L,
    /**
     * Wave 11a — wall-clock millis when TIME_FREEZE buff expires. 0 = inactive.
     * While active, enemies + enemy-lasers pause processing (gated in their
     * controllers by reading this field via GameState).
     */
    val timeFreezeEndMillis: Long = 0L,
    /**
     * Wave 11a — wall-clock millis when MINI buff expires. 0 = inactive.
     * GameWorld reads this to render ship at 0.6× scale; ShipController.moveShip
     * multiplies movementSpeed by 1.3 when active.
     */
    val miniEndMillis: Long = 0L,
    /**
     * Wave 11a Phase 2 — wall-clock millis when VAMPIRE buff expires. 0 = inactive.
     * LasersController.onLaserHit callback reads this; heals ship by 50% of
     * damage dealt while active. Drives a faint blood-mist visual cue (TBD).
     */
    val vampireEndMillis: Long = 0L,
    /**
     * Wave 11a Phase 2 — wall-clock millis when GHOST buff expires. 0 = inactive.
     * ShipController skips enemy + enemyLaser collision checks while active.
     * GameWorld renders ship at 0.5 alpha (visually translucent).
     */
    val ghostEndMillis: Long = 0L,
    /**
     * Wave 11a Phase 3 — wall-clock millis when GRAVITY buff expires. 0 = inactive.
     * GameState getMagnetRadius lambda multiplies radius × 100 while active —
     * effectively pulls every on-screen mineral into the ship.
     */
    val gravityEndMillis: Long = 0L,
    /**
     * Wave 11a Phase 3 — wall-clock millis when REFLECT buff expires. 0 = inactive.
     * When enemyLaser overlaps ship while active: laser absorbed (no damage to
     * ship) + 30 dmg dealt to nearest enemy via callback in GameState.
     */
    val reflectEndMillis: Long = 0L,
    /**
     * Wave 11a Phase 3 — wall-clock millis when CHAIN_LIGHTNING buff expires.
     * 0 = inactive. GameState.onLaserHit callback finds nearest 2 enemies to
     * the hit point and deals 50% damage to each (3-target chain total).
     *
     * Cross-feature interactions (Wave 11a):
     * - **MINI + GHOST + REFLECT** can all stack — each affects different
     *   subsystem (visual scale / collision skip / laser absorb) so no clash.
     * - **VAMPIRE + CHAIN_LIGHTNING**: VAMPIRE heals only from PRIMARY laser
     *   hit (where damage is reported via onLaserHit). Chain damage is dealt
     *   directly via onObjectImpact (no callback) → not lifesteal-eligible.
     *   Intentional balance: lifesteal scope = direct laser hits only.
     * - **MINI + SHIELD**: post-audit fix, shipShieldRect now also scales ×
     *   miniMul so SHIELD doesn't bypass MINI's hitbox shrink.
     * - **GRAVITY + MAGNET_BOOST**: both increase magnet radius (× 100 and ×
     *   N respectively). They compound multiplicatively.
     * - **TIME_FREEZE freezes enemies + their lasers** but ship lasers + ship
     *   collision still run. Ship can dance through frozen enemy bodies
     *   (taking damage from collision) but enemy AI is paused.
     */
    val chainLightningEndMillis: Long = 0L,
    /**
     * Wave 11a Phase 4 — wall-clock millis when CLONE buff expires. 0 = inactive.
     * Spawns a phantom-twin ship sprite at +50dp offset that fires alongside the
     * main ship (LasersController duplicates each laser column at clone offset).
     * Visual: translucent 0.55 alpha ship sprite rendered next to main ship.
     */
    val cloneEndMillis: Long = 0L,
    @DrawableRes val drawableId: Int = R.drawable.ship_regular_laser,
) : Serializable {
    val shieldRadius: Float get() = shieldSize / 2
}
