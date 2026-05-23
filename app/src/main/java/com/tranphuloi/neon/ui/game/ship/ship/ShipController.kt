package com.tranphuloi.neon.ui.game.ship.ship

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.booster.Booster
import com.tranphuloi.neon.ui.game.booster.BoosterType
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObject
import com.tranphuloi.neon.utils.Logger
import java.util.*
import kotlin.math.PI
import kotlin.math.sin

// init log placed in init {} block below

class ShipController(
    private val screenWidth: Float,
    screenHeight: Float,
    private var ship: Ship,
    private val setShip: (Ship) -> Unit,
    private val onShipDestroyed: () -> Unit = {},
    private val onShipDamaged: () -> Unit = {},
    private val onBoosterPickedUp: (xOffset: Float, yOffset: Float) -> Unit = { _, _ -> },
    private val onSpaceObjectHitShip: (xOffset: Float, yOffset: Float) -> Unit = { _, _ -> },
    private val onShipRevived: () -> Unit = {},
    private val damageMultiplier: () -> Float = { 1f },
    /**
     * Wave 5 (25x / 48x) — ship movement speed multiplier. Combines
     * RunModifier (TRIPLE_SPEED, TANK) + AGILITY skill node.
     */
    private val speedMultiplier: () -> Float = { 1f },
    /**
     * Round 34 (44x) — ICE_PATCHES hazard: ship glides ~200ms after release.
     * GameState passes lambda checking current stage's hazard type.
     */
    private val isIceHazardActive: () -> Boolean = { false },
    /**
     * Round 58 — hint when a bullet-type buff (PIERCING/PLASMA) is activated
     * via booster pickup. Caller spawns a visible PickupPopup with the
     * effective stats (pierce count / AoE radius) so the player can verify
     * round 52 rarity scaling without consulting logs.
     */
    private val onBulletTypeActivated: (
        type: com.tranphuloi.neon.ui.game.ship.laser.BulletType,
        rarity: com.tranphuloi.neon.ui.game.booster.BoosterRarity,
        xOffset: Float,
        yOffset: Float,
    ) -> Unit = { _, _, _, _ -> },
    /**
     * Round 60 (38x) — MINERAL_SUPERCHARGE one-shot effect. Fired on pickup;
     * caller (GameState) drains MineralsController.flushAllToShip() so the
     * player collects every on-screen mineral instantly.
     */
    private val onMineralSupercharge: () -> Unit = {},
) {

    init {
        Logger.d("ShipController init: screen=${screenWidth}x${screenHeight}, ship hp=${ship.hp}")
    }

    private val spaceShipCollidePower: Float = 100f
    private val movementSpeed: Float = 2f
    private val maxYOffset: Float = screenHeight - 140

    var movingLeft = false
    var movingRight = false

    // Round 34 (44x) — ICE_PATCHES slip: residual horizontal velocity that
    // decays each tick. Set on movement release; while > 0 ship continues
    // gliding briefly. Only applied when isIceHazardActive() == true.
    private var slipVelocityX: Float = 0f

    // 19b Charge shot — auto-charge while no damage taken. Every CHARGE_FILL_MS
    // (8s) without taking a hit, fires an ultimate laser. Reset on damage.
    // Replaces previous hold-both-arrows mechanic which was illogical (cancelled
    // movement, sitting duck during charge, asymmetric bank rotation).
    private var chargeStartMillis: Long = System.currentTimeMillis()
    /** Returns 0..1 charge progress. Reaches 1.0 when ready to fire. */
    fun chargeProgress(): Float {
        val held = System.currentTimeMillis() - chargeStartMillis
        return (held.toFloat() / CHARGE_FILL_MS).coerceIn(0f, 1f)
    }
    /** Called by GameState tinker loop. True when charge full → fire mega. */
    fun consumeChargeShot(): Boolean {
        if (chargeProgress() < 1f) return false
        chargeStartMillis = System.currentTimeMillis()             // reset for next charge
        Logger.d("ChargeShot auto-fired (no-damage charge complete)")
        return true
    }
    /**
     * Round 53 — soft decay (was: hard reset to 0%).
     *
     * Previously every damage hit wiped the full 20s charge buildup, which
     * made [consumeChargeShot] effectively dead in endless combat (player
     * gets hit every <20s → charge never fills). See audit in round 53.
     *
     * New behaviour: each damage tick pushes [chargeStartMillis] forward by
     *   3000ms base + (damageAmount × 20ms, capped at +2000ms).
     * Light hits (25 dmg ≈ 3.5s penalty), heavy hits (≥100 dmg → 5s penalty).
     * Floor at "now" so charge can drop to 0% but not negative — repeated
     * heavy hits still produce a full reset, just over multiple ticks.
     *
     * Shield-blocked damage already skips this path (updateHp early-returns
     * when effective == 0), so shield still preserves charge buildup.
     *
     * [damageAmount] = absolute hp removed (positive integer).
     */
    private fun resetCharge(damageAmount: Int) {
        chargeStartMillis = computeChargeStartAfterDamage(
            chargeStartMillis = chargeStartMillis,
            damageAmount = damageAmount,
            nowMillis = System.currentTimeMillis(),
        )
    }

    // Cinematic spawn animation: bottom → fly up to center → sway → fly down to play.
    // Total 3s. During spawn: damage absorbed (see updateHp), player input ignored,
    // ship position fully driven by the choreographed path below.
    private val spawnStartMillis: Long = System.currentTimeMillis()
    private val spawnFlyUpMillis: Long = 800L
    private val spawnSwayMillis: Long = 1400L
    private val spawnFlyDownMillis: Long = 800L
    private val spawnTotalMillis: Long =
        spawnFlyUpMillis + spawnSwayMillis + spawnFlyDownMillis
    private val spawnStartY: Float = screenHeight + 240f       // off-screen below
    private val spawnCenterY: Float = screenHeight * 0.35f
    private val spawnCenterX: Float = screenWidth / 2f - 85f / 2f

    val moveShipId = UUID.randomUUID().toString()
    val moveShipRepeatTime = Millis(3)

    fun moveShip() {
        val elapsed = System.currentTimeMillis() - spawnStartMillis
        if (elapsed < spawnTotalMillis) {
            applySpawnPath(elapsed)
            return
        }
        var newX = ship.xOffset
        var newY = ship.yOffset
        // 25x/48x — effective movement speed = base × modifier × meta. Recomputed
        // per tick so reactive multiplier changes (none currently, but cheap).
        val effSpeed = movementSpeed * speedMultiplier()
        // Settle to play position bi-directionally. Activity recreate (config change,
        // theme switch, etc.) preserves Ship.yOffset via rememberSaveable but resets
        // ShipController.spawnStartMillis. If user pauses mid-spawn then resumes
        // after spawnTotalMillis elapses, spawn anim is skipped — ship would be
        // stuck wherever spawn left it. Pull it back to maxYOffset from either side.
        if (newY > maxYOffset) {
            newY = (newY - effSpeed).coerceAtLeast(maxYOffset)
        } else if (newY < maxYOffset) {
            newY = (newY + effSpeed).coerceAtMost(maxYOffset)
        }
        // Symmetric bounds: left allows ship overlap by width/4 → right matches with
        // ship.width * 0.75. Was asymmetric (-21px vs +29px overlap, ~8px diff).
        val leftLimit = -ship.width / 4f
        val rightLimit = screenWidth - ship.width * 0.75f
        // Round 34 (44x) — ICE_PATCHES slip. Snapshot whether ice active this
        // tick, then apply residual velocity for ~200ms after movement release.
        val iceActive = isIceHazardActive()
        if (movingLeft && ship.xOffset > leftLimit) {
            newX -= effSpeed
            if (iceActive) slipVelocityX = -effSpeed * 0.85f
        } else if (movingLeft) {
            movingLeft = false
        }
        if (movingRight && ship.xOffset < rightLimit) {
            newX += effSpeed
            if (iceActive) slipVelocityX = effSpeed * 0.85f
        } else if (movingRight) {
            movingRight = false
        }
        // Apply slip velocity (only meaningful when no input + ice active).
        if (iceActive && !movingLeft && !movingRight && kotlin.math.abs(slipVelocityX) > 0.01f) {
            newX += slipVelocityX
            slipVelocityX *= 0.93f                       // decay ~7%/tick → ~200ms half-life
            newX = newX.coerceIn(leftLimit, rightLimit)
        } else if (!iceActive) {
            slipVelocityX = 0f                           // reset when leaving ice zone
        }
        // Bank rotation lerp toward target (-16° / 0° / +16°), smoothing 0.18.
        // Single ship.copy() per tick to avoid 3 setShip allocations.
        val bankTarget = when {
            movingLeft -> -16f
            movingRight -> 16f
            else -> 0f
        }
        val newBankRot = ship.bankRotation + (bankTarget - ship.bankRotation) * 0.18f
        if (newX != ship.xOffset || newY != ship.yOffset ||
            kotlin.math.abs(newBankRot - ship.bankRotation) > 0.05f) {
            ship = ship.copy(xOffset = newX, yOffset = newY, bankRotation = newBankRot)
            setShip(ship)
        }
    }

    private fun applySpawnPath(elapsed: Long) {
        val phase1End = spawnFlyUpMillis
        val phase2End = phase1End + spawnSwayMillis

        var x: Float
        var y: Float
        var alpha: Float
        var scale: Float
        var rotation: Float

        when {
            elapsed < phase1End -> {
                // Phase 1: fly up bottom → center with overshoot back-out easing.
                // Materialize: alpha 0→1, scale 0.6→1.0, slight initial tilt that
                // straightens out as ship reaches center.
                val t = elapsed / phase1End.toFloat()
                // back-out: 1 + c*(t-1)^3 + c2*(t-1)^2  with c=2.7, c2=1.7 (gentle overshoot)
                val tt = t - 1f
                val tEase = 1f + 2.7f * tt * tt * tt + 1.7f * tt * tt
                x = spawnCenterX
                y = spawnStartY + (spawnCenterY - spawnStartY) * tEase
                alpha = (t * 1.4f).coerceAtMost(1f)              // fade-in 0..0.71
                scale = 0.6f + 0.4f * (1f - (1f - t) * (1f - t)) // ease-out quadratic 0.6→1.0
                rotation = -8f * (1f - t)                         // initial -8° tilt → 0°
            }
            elapsed < phase2End -> {
                // Phase 2: sway side-to-side at center, damped sine wave.
                // Banking rotation: tilt INTO the sway direction (cinematic flight feel).
                // Subtle vertical bob.
                val swayElapsed = elapsed - phase1End
                val swayT = swayElapsed / spawnSwayMillis.toFloat()
                val swayPhase = swayT * (PI.toFloat() * 2f) * 1.5f   // 1.5 oscillations
                val damp = 1f - swayT * 0.4f
                val swayX = sin(swayPhase) * 90f * damp
                val bobY = sin(swayPhase * 2f) * 6f * damp
                x = spawnCenterX + swayX
                y = spawnCenterY + bobY
                alpha = 1f
                scale = 1f
                // Banking turn: cos of swayPhase peaks when swayX is at extrema → tilt.
                rotation = -kotlin.math.cos(swayPhase) * 14f * damp
            }
            else -> {
                // Phase 3: fly down center → play position, smoothstep + tilt return to 0.
                val downElapsed = elapsed - phase2End
                val downT = (downElapsed / spawnFlyDownMillis.toFloat()).coerceIn(0f, 1f)
                val tEase = downT * downT * (3f - 2f * downT)
                x = spawnCenterX
                y = spawnCenterY + (maxYOffset - spawnCenterY) * tEase
                alpha = 1f
                scale = 1f
                // Whatever rotation phase 2 ended at — smoothly settle to 0.
                rotation = 0f
            }
        }

        // Single ship.copy() per frame — avoid 3 separate setShip allocations.
        ship = ship.copy(
            xOffset = x,
            yOffset = y,
            spawnAlpha = alpha,
            spawnScale = scale,
            spawnRotation = rotation,
        )
        setShip(ship)
    }

    fun isSpawning(): Boolean =
        System.currentTimeMillis() - spawnStartMillis < spawnTotalMillis

    private var shieldBoosterStartMillis: Long = 0
    private val shieldBoosterTimeMillis: Long = 10000
    private var shieldEndDurationMillis: Long = 0
    /**
     * Round 43 (39x) — [multiplier] scales the active duration by booster rarity
     * (1.0 / 1.5 / 2.0). 1.0 = baseline COMMON behavior.
     *
     * Round 55 — refresh logic: when picking up a buff while one is already
     * active, use `max(oldEnd, newEnd)` to prevent silent downgrade (e.g.,
     * Rare ×1.5 22.5s remaining → Common ×1.0 15s used to overwrite). Logs
     * `REFRESHED` when newEnd > oldEnd, `WASTED` otherwise so the player can
     * see what the pickup actually did.
     */
    private fun enableShield(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val dur = (shieldBoosterTimeMillis * multiplier).toLong()
        val newEnd = now + dur
        logBoosterTransition(
            name = "shield",
            wasEnabled = ship.shieldEnabled,
            enable = enable,
            oldEndMillis = shieldEndDurationMillis,
            newEndMillis = newEnd,
            now = now,
            dur = dur,
            multiplier = multiplier,
        )
        if (enable) {
            shieldBoosterStartMillis = now
            shieldEndDurationMillis = maxOf(shieldEndDurationMillis, newEnd)
        }
        updateShieldEnabled(enable)
    }

    private var laserBoosterStartMillis: Long = 0
    private val laserBoosterTimeMillis: Long = 15000
    private var laserBoosterEndDurationMillis: Long = 0
    private fun enableLaserBooster(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val dur = (laserBoosterTimeMillis * multiplier).toLong()
        val newEnd = now + dur
        logBoosterTransition(
            name = "laser",
            wasEnabled = ship.laserBoosterEnabled,
            enable = enable,
            oldEndMillis = laserBoosterEndDurationMillis,
            newEndMillis = newEnd,
            now = now,
            dur = dur,
            multiplier = multiplier,
        )
        if (enable) {
            laserBoosterStartMillis = now
            laserBoosterEndDurationMillis = maxOf(laserBoosterEndDurationMillis, newEnd)
        }
        updateLaserBoosterEnabled(enable)
    }

    private var tripleLaserBoosterStartMillis: Long = 0
    private val tripleLaserBoosterTimeMillis: Long = 20000
    private var tripleLaserBoosterEndDurationMillis: Long = 0
    private fun enableTripleLaserBooster(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val dur = (tripleLaserBoosterTimeMillis * multiplier).toLong()
        val newEnd = now + dur
        logBoosterTransition(
            name = "triple-laser",
            wasEnabled = ship.tripleLaserBoosterEnabled,
            enable = enable,
            oldEndMillis = tripleLaserBoosterEndDurationMillis,
            newEndMillis = newEnd,
            now = now,
            dur = dur,
            multiplier = multiplier,
        )
        if (enable) {
            tripleLaserBoosterStartMillis = now
            tripleLaserBoosterEndDurationMillis = maxOf(tripleLaserBoosterEndDurationMillis, newEnd)
        }
        updateTripleLaserBoosterEnabled(enable)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Round 60 (38x) — 8 timed buff state + enable methods for new boosters.
    // Pattern mirrors enableShield/enableLaserBooster: private endDurationMillis
    // refreshed with maxOf to prevent silent downgrade (Round 55 fix), public
    // getter for other controllers to read multiplier each tick, tick decay
    // hooked at the bottom of monitorShipCollisions.
    //
    // Naming convention:
    //   - xxxEndDurationMillis: private Long timer
    //   - xxxBoosterTimeMillis: private val base duration
    //   - enableXxx(enable, multiplier): activation/expire entry point
    //   - xxxMul()/isXxxActive(): public read API
    // ──────────────────────────────────────────────────────────────────────

    private val magnetBoostTimeMillis: Long = 15_000
    private var magnetBoostEndDurationMillis: Long = 0
    private fun enableMagnetBoost(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (magnetBoostTimeMillis * multiplier).toLong() else 0L
        if (enable) magnetBoostEndDurationMillis = maxOf(magnetBoostEndDurationMillis, newEnd)
        else magnetBoostEndDurationMillis = 0L
        Logger.d("Booster: magnet-boost ${if (enable) "ON (+${newEnd - now}ms)" else "OFF"}")
    }
    /** 1.0 (off) or 2.0 (active). MineralsController multiplies magnetRadius by this. */
    fun magnetBoostMul(): Float =
        if (System.currentTimeMillis() < magnetBoostEndDurationMillis) 2f else 1f

    private val critSurgeTimeMillis: Long = 8_000
    private var critSurgeEndDurationMillis: Long = 0
    private fun enableCritSurge(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (critSurgeTimeMillis * multiplier).toLong() else 0L
        if (enable) critSurgeEndDurationMillis = maxOf(critSurgeEndDurationMillis, newEnd)
        else critSurgeEndDurationMillis = 0L
        updateCritSurgeEnabled(enable)
        Logger.d("Booster: crit-surge ${if (enable) "ON (+${newEnd - now}ms)" else "OFF"}")
    }
    /** 1.0 (off) or 3.0 (active). LasersController multiplies laser.impactPower at fire time. */
    fun critSurgeMul(): Float =
        if (System.currentTimeMillis() < critSurgeEndDurationMillis) 3f else 1f

    private val spreadShotTimeMillis: Long = 10_000
    private var spreadShotEndDurationMillis: Long = 0
    private fun enableSpreadShot(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (spreadShotTimeMillis * multiplier).toLong() else 0L
        if (enable) spreadShotEndDurationMillis = maxOf(spreadShotEndDurationMillis, newEnd)
        else spreadShotEndDurationMillis = 0L
        updateSpreadShotEnabled(enable)
        Logger.d("Booster: spread-shot ${if (enable) "ON (+${newEnd - now}ms)" else "OFF"}")
    }

    private val berserkTimeMillis: Long = 12_000
    private var berserkEndDurationMillis: Long = 0
    private fun enableBerserk(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (berserkTimeMillis * multiplier).toLong() else 0L
        if (enable) berserkEndDurationMillis = maxOf(berserkEndDurationMillis, newEnd)
        else berserkEndDurationMillis = 0L
        updateBerserkEnabled(enable)
        Logger.d("Booster: berserk ${if (enable) "ON (+${newEnd - now}ms, dmg×2 / take×1.5)" else "OFF"}")
    }
    /** 1.0 (off) or 2.0 (active). LasersController multiplies laser.impactPower. */
    fun berserkDamageMul(): Float =
        if (System.currentTimeMillis() < berserkEndDurationMillis) 2f else 1f
    /** 1.0 (off) or 1.5 (active). Applied inside updateHp before iframe gate. */
    private fun berserkTakeDamageMul(): Float =
        if (System.currentTimeMillis() < berserkEndDurationMillis) 1.5f else 1f

    // PHASE_SHIELD piggybacks on iframesEndMillis (no dedicated state field
    // needed). Pickup: iframesEndMillis = max(it, now + 5000 × multiplier).
    // Ghost visual: future polish — for now player just won't take damage.
    private val phaseShieldTimeMillis: Long = 5_000

    private val scoreX3TimeMillis: Long = 15_000
    private var scoreX3EndDurationMillis: Long = 0
    private fun enableScoreX3(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (scoreX3TimeMillis * multiplier).toLong() else 0L
        if (enable) scoreX3EndDurationMillis = maxOf(scoreX3EndDurationMillis, newEnd)
        else scoreX3EndDurationMillis = 0L
        Logger.d("Booster: score-x3 ${if (enable) "ON (+${newEnd - now}ms)" else "OFF"}")
    }
    /** 1.0 (off) or 3.0 (active). MineralsController multiplies mineralsEarned. */
    fun scoreMul(): Float =
        if (System.currentTimeMillis() < scoreX3EndDurationMillis) 3f else 1f

    private val healingAuraTimeMillis: Long = 10_000
    private var healingAuraEndDurationMillis: Long = 0
    private fun enableHealingAura(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (healingAuraTimeMillis * multiplier).toLong() else 0L
        if (enable) healingAuraEndDurationMillis = maxOf(healingAuraEndDurationMillis, newEnd)
        else healingAuraEndDurationMillis = 0L
        Logger.d("Booster: healing-aura ${if (enable) "ON (+${newEnd - now}ms, +5HP/sec)" else "OFF"}")
    }
    private var healingAuraLastTickMillis: Long = 0L

    private val doubleFireTimeMillis: Long = 10_000
    private var doubleFireEndDurationMillis: Long = 0
    private fun enableDoubleFire(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (doubleFireTimeMillis * multiplier).toLong() else 0L
        if (enable) doubleFireEndDurationMillis = maxOf(doubleFireEndDurationMillis, newEnd)
        else doubleFireEndDurationMillis = 0L
        updateDoubleFireEnabled(enable)
        Logger.d("Booster: double-fire ${if (enable) "ON (+${newEnd - now}ms)" else "OFF"}")
    }

    // Updaters for the 4 boosters that surface a Boolean on Ship.
    private fun updateSpreadShotEnabled(enable: Boolean) {
        ship = ship.copy(spreadShotEnabled = enable); setShip(ship)
    }
    private fun updateDoubleFireEnabled(enable: Boolean) {
        ship = ship.copy(doubleFireEnabled = enable); setShip(ship)
    }
    private fun updateCritSurgeEnabled(enable: Boolean) {
        ship = ship.copy(critSurgeEnabled = enable); setShip(ship)
    }
    private fun updateBerserkEnabled(enable: Boolean) {
        ship = ship.copy(berserkEnabled = enable); setShip(ship)
    }

    /**
     * Round 55 — single source-of-truth for booster activation/refresh logs.
     * Distinguishes 4 cases so the gameplay actually surfaces in logcat:
     *   - OFF (was enabled, now disabling)
     *   - ON  (was disabled, now enabling — first pickup)
     *   - REFRESHED (was enabled, new pickup extends the timer)
     *   - WASTED (was enabled, new pickup would have shortened — kept existing)
     */
    private fun logBoosterTransition(
        name: String,
        wasEnabled: Boolean,
        enable: Boolean,
        oldEndMillis: Long,
        newEndMillis: Long,
        now: Long,
        dur: Long,
        multiplier: Float,
    ) {
        when {
            !enable -> {
                if (wasEnabled) Logger.d("Booster: $name OFF")
            }
            !wasEnabled -> {
                Logger.d("Booster: $name ON (+${dur}ms, mul=$multiplier)")
            }
            newEndMillis > oldEndMillis -> {
                val gain = newEndMillis - oldEndMillis
                val oldRemaining = (oldEndMillis - now).coerceAtLeast(0L)
                Logger.d("Booster: $name REFRESHED (+${gain}ms, was=${oldRemaining}ms remaining, mul=$multiplier)")
            }
            else -> {
                val oldRemaining = (oldEndMillis - now).coerceAtLeast(0L)
                Logger.d("Booster: $name WASTED (offered=${dur}ms mul=$multiplier < remaining=${oldRemaining}ms — kept existing buff)")
            }
        }
    }

    val monitorShipCollisionsId = UUID.randomUUID().toString()
    val monitorShipCollisionsRepeatTime = Millis(100)
    fun monitorShipCollisions(
        spaceObjects: List<SpaceObject>,
        boosters: List<Booster>,
        enemies: List<Enemy>,
        enemyLasers: List<Laser>,
        fileUltimateLaser: () -> Unit,
    ) {
        val shipRect by lazy {
            Rect(
                offset = Offset(x = ship.xOffset, y = ship.yOffset),
                size = Size(width = ship.width, height = ship.height)
            )
        }
        val shipShieldRect by lazy {
            Rect(
                center = Offset(
                    x = ship.xOffset + ship.width / 2,
                    y = ship.yOffset + ship.height / 2
                ),
                radius = ship.shieldRadius
            )
        }

        spaceObjects.forEachIndexed { spaceObjectIndex, spaceObject ->
            val spaceRect by lazy {
                Rect(
                    offset = Offset(x = spaceObject.xOffset, y = spaceObject.yOffset),
                    size = Size(width = spaceObject.size, height = spaceObject.size)
                )
            }
            if (spaceRect.overlaps(if (ship.shieldEnabled) shipShieldRect else shipRect)) {
                Logger.d("Collision: ship ↔ spaceObject (shield=${ship.shieldEnabled}, impactPower=${spaceObject.impactPower})")
                spaceObjects[spaceObjectIndex].onObjectImpact(spaceShipCollidePower)
                // Visual feedback at rock center — sparks + mini explosion. Skipped for
                // pickup-style space objects (boosters via spaceObject path) by checking
                // impactPower > 0 (only damaging rocks have impactPower).
                if (spaceObject.impactPower > 0) {
                    val hitX = spaceObject.xOffset + spaceObject.size / 2f
                    val hitY = spaceObject.yOffset + spaceObject.size / 2f
                    onSpaceObjectHitShip(hitX, hitY)
                }

                val hpImpact: Int = when (ship.shieldEnabled && spaceObject.impactPower > 0) {
                    true -> 0
                    false -> spaceObject.impactPower
                }
                updateHp(-hpImpact)

                when (spaceObject.drawableId) {
                    BoosterType.ULTIMATE_WEAPON_BOOSTER.drawableId -> fileUltimateLaser()
                    BoosterType.SHIELD_BOOSTER.drawableId -> enableShield(enable = true)
                    BoosterType.LASER_BOOSTER.drawableId -> enableLaserBooster(enable = true)
                    BoosterType.TRIPLE_LASER_BOOSTER.drawableId -> enableTripleLaserBooster(enable = true)
                }
            }
        }

        boosters.forEachIndexed { boosterIndex, booster ->
            val boosterRect by lazy {
                Rect(
                    offset = Offset(x = booster.xOffset, y = booster.yOffset),
                    size = Size(width = booster.size, height = booster.size)
                )
            }
            if (boosterRect.overlaps(if (ship.shieldEnabled) shipShieldRect else shipRect)) {
                Logger.d("Collision: ship ↔ booster type=${booster.type} (shield=${ship.shieldEnabled})")
                boosters[boosterIndex].collect()
                onBoosterPickedUp(
                    booster.xOffset + booster.size / 2f,
                    booster.yOffset + booster.size / 2f,
                )
                // Round 43 (39x) — apply rarity multiplier to duration/amount effects.
                val mul = booster.rarity.multiplier
                when (booster.type) {
                    BoosterType.ULTIMATE_WEAPON_BOOSTER -> fileUltimateLaser()
                    BoosterType.SHIELD_BOOSTER -> enableShield(enable = true, multiplier = mul)
                    BoosterType.LASER_BOOSTER -> enableLaserBooster(enable = true, multiplier = mul)
                    BoosterType.TRIPLE_LASER_BOOSTER ->
                        enableTripleLaserBooster(enable = true, multiplier = mul)
                    BoosterType.HEALTH_BOOSTER -> updateHp((100 * mul).toInt())
                    BoosterType.REVIVE_TOKEN -> {
                        if (!ship.hasReviveToken) {
                            ship = ship.copy(hasReviveToken = true)
                            setShip(ship)
                            Logger.d("Booster: REVIVE_TOKEN stored (consumed on next hp→0)")
                        } else {
                            Logger.d("Booster: REVIVE_TOKEN ignored — already holding one")
                        }
                    }
                    // Round 35 (35x) — activate bullet type for 10s × rarity multiplier.
                    // Round 52 (40x) — pass rarity through so PIERCING/PLASMA can
                    // tier-up combat behaviour (pierce count + AoE radius).
                    BoosterType.PIERCING_BOOSTER -> setBulletType(
                        com.tranphuloi.neon.ui.game.ship.laser.BulletType.PIERCING,
                        multiplier = mul,
                        rarity = booster.rarity,
                    )
                    BoosterType.PLASMA_BOOSTER -> setBulletType(
                        com.tranphuloi.neon.ui.game.ship.laser.BulletType.PLASMA,
                        multiplier = mul,
                        rarity = booster.rarity,
                    )
                    // Round 60 (38x) — 10 new boosters dispatch. 8 are timed
                    // buffs (enable + auto-expire), 2 are one-shot (QUICK_HEAL
                    // applies HP directly, MINERAL_SUPERCHARGE delegates to
                    // MineralsController via callback).
                    BoosterType.MAGNET_BOOST -> enableMagnetBoost(enable = true, multiplier = mul)
                    BoosterType.CRIT_SURGE -> enableCritSurge(enable = true, multiplier = mul)
                    BoosterType.SPREAD_SHOT -> enableSpreadShot(enable = true, multiplier = mul)
                    BoosterType.BERSERK -> enableBerserk(enable = true, multiplier = mul)
                    BoosterType.PHASE_SHIELD -> {
                        val now = System.currentTimeMillis()
                        val ext = (phaseShieldTimeMillis * mul).toLong()
                        iframesEndMillis = maxOf(iframesEndMillis, now + ext)
                        // Round 61 — surface phase-shield specifically on Ship state
                        // so GameWorld can render a translucent ghost overlay. Without
                        // this, Round 60 was visually identical to taking-damage
                        // iframes (600ms flash) → player couldn't see the buff.
                        val newEnd = maxOf(ship.phaseShieldEndMillis, now + ext)
                        ship = ship.copy(phaseShieldEndMillis = newEnd)
                        setShip(ship)
                        Logger.d("Booster: phase-shield ON (+${ext}ms iframes, mul=$mul)")
                    }
                    BoosterType.SCORE_X3 -> enableScoreX3(enable = true, multiplier = mul)
                    BoosterType.QUICK_HEAL -> updateHp((250 * mul).toInt())
                    BoosterType.MINERAL_SUPERCHARGE -> {
                        Logger.d("Booster: mineral-supercharge ONE-SHOT (mul=$mul)")
                        onMineralSupercharge()
                    }
                    BoosterType.HEALING_AURA -> {
                        enableHealingAura(enable = true, multiplier = mul)
                        healingAuraLastTickMillis = System.currentTimeMillis()
                    }
                    BoosterType.DOUBLE_FIRE -> enableDoubleFire(enable = true, multiplier = mul)
                }
            }
        }
        enemies.forEachIndexed { enemyIndex, enemy ->
            val enemyRect by lazy {
                Rect(
                    offset = Offset(x = enemy.xOffset, y = enemy.yOffset),
                    size = Size(width = enemy.width, height = enemy.height)
                )
            }
            if (enemyRect.overlaps(if (ship.shieldEnabled) shipShieldRect else shipRect)) {
                Logger.d("Collision: ship ↔ enemy id=${enemy.enemyId.take(6)} (shield=${ship.shieldEnabled})")
                enemies[enemyIndex].onObjectImpact(spaceShipCollidePower)

                val hpImpact: Int = when (ship.shieldEnabled && enemy.impactPower > 0) {
                    true -> 0
                    false -> enemy.impactPower.toInt()
                }
                updateHp(-hpImpact)
            }
        }
        enemyLasers.forEachIndexed { enemyIndex, enemyLaser ->
            val enemyLaserRect by lazy {
                Rect(
                    offset = Offset(x = enemyLaser.xOffset, y = enemyLaser.yOffset),
                    size = Size(width = enemyLaser.width, height = enemyLaser.height)
                )
            }
            if (enemyLaserRect.overlaps(if (ship.shieldEnabled) shipShieldRect else shipRect)) {
                Logger.d("Collision: ship ↔ enemyLaser (shield=${ship.shieldEnabled}, impactPower=${enemyLaser.impactPower.toInt()})")
                enemyLasers[enemyIndex].destroyed = true

                val hpImpact: Float = when (ship.shieldEnabled && enemyLaser.impactPower > 0) {
                    true -> 0f
                    false -> enemyLaser.impactPower
                }
                updateHp(-hpImpact.toInt())
            }
        }

        val currentTime = System.currentTimeMillis()
        if (shieldEndDurationMillis < currentTime) enableShield(enable = false)
        if (laserBoosterEndDurationMillis < currentTime) enableLaserBooster(enable = false)
        if (tripleLaserBoosterEndDurationMillis < currentTime) enableTripleLaserBooster(enable = false)
        // Round 60 (38x) — tick decay for 8 timed buffs. monitorShipCollisions
        // runs at 100ms cadence so expiry granularity is ±100ms — acceptable.
        if (magnetBoostEndDurationMillis in 1..currentTime) enableMagnetBoost(enable = false)
        if (critSurgeEndDurationMillis in 1..currentTime) enableCritSurge(enable = false)
        if (spreadShotEndDurationMillis in 1..currentTime) enableSpreadShot(enable = false)
        if (berserkEndDurationMillis in 1..currentTime) enableBerserk(enable = false)
        if (scoreX3EndDurationMillis in 1..currentTime) enableScoreX3(enable = false)
        if (doubleFireEndDurationMillis in 1..currentTime) enableDoubleFire(enable = false)
        if (healingAuraEndDurationMillis in 1..currentTime) enableHealingAura(enable = false)
        // Round 61 — clear PHASE_SHIELD ghost flag at expiry. State lives on Ship
        // (not in a controller-private timer) so GameWorld's ghost overlay reads
        // it directly. Iframes already expire on their own; this just hides the
        // visual.
        if (ship.phaseShieldEndMillis in 1..currentTime) {
            ship = ship.copy(phaseShieldEndMillis = 0L)
            setShip(ship)
            Logger.d("Booster: phase-shield OFF")
        }
        // Round 60 — HEALING_AURA continuous regen: +5 HP/sec while active.
        // Use lastTick to compute elapsed since last heal call so it's framerate-
        // independent (works even if monitorShipCollisions skips a tick).
        if (currentTime < healingAuraEndDurationMillis) {
            val sinceTick = currentTime - healingAuraLastTickMillis
            if (sinceTick >= 200L) {                                 // heal in 200ms chunks
                val heal = (sinceTick / 200L).toInt()                // 1 hp per 200ms = 5/sec
                if (heal > 0 && ship.hp > 0) {
                    // Round 65 — silent=true gates the per-tick log to Logger.v.
                    // Without this, runtime log got 50 lines per HEALING_AURA
                    // pickup (5 heals/sec × 10s). The ON/OFF events + damage
                    // events still log at Logger.d.
                    updateHp(heal, silent = true)
                    healingAuraLastTickMillis = currentTime
                }
            }
        }
        // Round 35 (35x) — expire active bullet type.
        if (ship.bulletTypeEndMillis > 0L && currentTime >= ship.bulletTypeEndMillis) {
            Logger.d("BulletType: ${ship.activeBulletType} expired → revert to NORMAL")
            ship = ship.copy(
                activeBulletType = com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL,
                bulletTypeEndMillis = 0L,
            )
            setShip(ship)
        }
    }

    /**
     * Round 35 (35x) — activate [type] bullet for its activeDurationMillis.
     * Picking up another bullet-type booster overrides any existing one.
     */
    private fun setBulletType(
        type: com.tranphuloi.neon.ui.game.ship.laser.BulletType,
        multiplier: Float = 1f,
        // Round 52 (40x Item combos) — rarity tier of the activating booster.
        // Stored on Ship so LasersController can read it when firing PIERCING
        // (pierce count) or PLASMA (AoE radius). Defaults to COMMON so the
        // round 45 Loadout head-start (no booster involved) keeps baseline behaviour.
        rarity: com.tranphuloi.neon.ui.game.booster.BoosterRarity =
            com.tranphuloi.neon.ui.game.booster.BoosterRarity.COMMON,
    ) {
        val dur = (type.activeDurationMillis * multiplier).toLong()
        val endMillis = System.currentTimeMillis() + dur
        ship = ship.copy(
            activeBulletType = type,
            bulletTypeEndMillis = endMillis,
            activeBulletTypeRarity = rarity,
        )
        setShip(ship)
        Logger.d("BulletType: activated $type rarity=$rarity for ${dur}ms (mul=$multiplier, ends @ $endMillis)")
        // Round 58 — visible hint popup at ship center so player sees the
        // tier-up info from round 52 rarity scaling (pierce 3/4/5 or AoE
        // 80/110/140px). Skipped for NORMAL since head-start applies it
        // silently and the popup would be noise.
        if (type != com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL) {
            onBulletTypeActivated(
                type,
                rarity,
                ship.xOffset + ship.width / 2f,
                ship.yOffset,
            )
        }
    }

    private fun updateShieldEnabled(enable: Boolean) {
        ship = ship.copy(
            shieldEnabled = enable,
            shieldEndMillis = if (enable) shieldEndDurationMillis else 0L,
        )
        setShip(ship)
    }

    private fun resolveShipDrawable(ship: Ship): Int =
        if (ship.laserBoosterEnabled) R.drawable.ship_boosted_laser
        else R.drawable.ship_regular_laser

    private fun updateLaserBoosterEnabled(enable: Boolean) {
        val updated = ship.copy(
            laserBoosterEnabled = enable,
            laserBoosterEndMillis = if (enable) laserBoosterEndDurationMillis else 0L,
        )
        ship = updated.copy(drawableId = resolveShipDrawable(updated))
        setShip(ship)
    }

    private fun updateTripleLaserBoosterEnabled(enable: Boolean) {
        val updated = ship.copy(
            tripleLaserBoosterEnabled = enable,
            tripleLaserBoosterEndMillis = if (enable) tripleLaserBoosterEndDurationMillis else 0L,
        )
        ship = updated.copy(drawableId = resolveShipDrawable(updated))
        setShip(ship)
    }

    private fun updateXOffset(xOffset: Float) {
        ship = ship.copy(xOffset = xOffset)
        setShip(ship)
    }

    private fun updateYOffset(yOffset: Float) {
        ship = ship.copy(yOffset = yOffset)
        setShip(ship)
    }

    private var iframesEndMillis: Long = 0L

    /**
     * @param silent Round 65 — suppress the per-call Logger.d "Ship hp: X→Y"
     *               line for high-frequency passive heals (HEALING_AURA fires
     *               this ~5×/sec for 10s = 50 lines per pickup). Demoted to
     *               Logger.v which is gated by VERBOSE flag. Still mutates hp
     *               + setShip normally. Default false (damage + pickup heals
     *               still log at Logger.d for runtime auditing).
     */
    private fun updateHp(hpChange: Int, silent: Boolean = false) {
        if (ship.hp <= 0) return
        // Damage absorption: i-frames OR spawn animation. Healing (hpChange > 0) always applies.
        if (hpChange < 0 && (System.currentTimeMillis() < iframesEndMillis || isSpawning())) {
            Logger.v { "Ship hp: damage Δ=$hpChange ABSORBED (iframes or spawn)" }
            return
        }
        // Round 60 (38x) — BERSERK take ×1.5 damage stacks multiplicatively on
        // top of the run-modifier multiplier (Hard mode + BERSERK = 1.4 × 1.5 = ×2.1
        // incoming damage). Healing (hpChange > 0) unaffected.
        val multiplier = damageMultiplier() * if (hpChange < 0) berserkTakeDamageMul() else 1f
        val effective = if (hpChange < 0) -((-hpChange) * multiplier).toInt() else hpChange
        val before = ship.hp
        val newHp = (ship.hp + effective).coerceAtLeast(0)
        ship = ship.copy(hp = newHp)
        if (effective != 0) {
            if (silent) {
                Logger.v { "Ship hp: $before → ${ship.hp} (Δ=$effective, passive heal)" }
            } else {
                Logger.d("Ship hp: $before → ${ship.hp} (Δ=$effective, raw=$hpChange, multiplier=$multiplier)")
            }
        }
        setShip(ship)
        if (effective < 0) {
            iframesEndMillis = System.currentTimeMillis() + IFRAMES_DURATION_MILLIS
            Logger.v { "Ship i-frames: ON until $iframesEndMillis (+${IFRAMES_DURATION_MILLIS}ms)" }
            // Round 53 — soft decay (was: full reset). See [resetCharge] kdoc.
            resetCharge(damageAmount = -effective)
            onShipDamaged()
        }
        if (before > 0 && newHp == 0) {
            // 14c Auto-revive: if a REVIVE_TOKEN was stored, consume it instead of dying.
            // Restore hp=300 + 1.5s i-frames so player has a fair recovery window.
            if (ship.hasReviveToken) {
                ship = ship.copy(hp = REVIVE_HP, hasReviveToken = false)
                setShip(ship)
                iframesEndMillis = System.currentTimeMillis() + REVIVE_IFRAMES_MILLIS
                Logger.w("Ship REVIVED via auto-revive token: hp=$REVIVE_HP iframes=${REVIVE_IFRAMES_MILLIS}ms")
                onShipRevived()
                return
            }
            Logger.w("Ship destroyed (hp=0) → onShipDestroyed()")
            onShipDestroyed()
        }
    }

    companion object {
        const val TRIPLE_LASER_SIDE_OFFSET: Float = 20f
        const val IFRAMES_DURATION_MILLIS: Long = 600L
        // 20s no-damage → auto charge fire (was 8s — too spammy combined with
        // ULTIMATE_WEAPON_BOOSTER pickups). Round 53 paired with soft decay
        // below so the 20s window is achievable in moderate combat.
        const val CHARGE_FILL_MS: Long = 20000L
        // Round 53 — soft decay tuning. Each damage hit shaves this much off
        // the charge buildup instead of resetting it to zero. Pulled out as
        // constants so the gameplay knob is greppable + adjustable.
        const val CHARGE_DAMAGE_BASE_PENALTY_MS: Long = 3000L          // every hit, regardless of size
        const val CHARGE_DAMAGE_SCALE_MS_PER_HP: Long = 20L            // +20ms per hp damage (25 dmg → +500ms)
        const val CHARGE_DAMAGE_SCALE_PENALTY_MAX_MS: Long = 2000L     // scaled penalty caps at +2s (100+ dmg)
        const val REVIVE_HP: Int = 300                  // 14c: hp restored when auto-revive token consumed
        const val REVIVE_IFRAMES_MILLIS: Long = 1500L   // 14c: longer than normal 600ms iframes — fair recovery

        /**
         * Round 53 — pure soft-decay math, exposed for unit tests.
         *
         * Returns the new [chargeStartMillis] after a damage hit:
         *   newStart = min(now, currentStart + 3s + clamp(dmg × 20ms, 0, 2s))
         *
         * Floor at `now` ensures charge progress can drop to 0% but never
         * go negative — repeated heavy hits drain the buildup over several
         * ticks instead of one tick wiping it.
         */
        internal fun computeChargeStartAfterDamage(
            chargeStartMillis: Long,
            damageAmount: Int,
            nowMillis: Long,
        ): Long {
            val scaledPenalty = (damageAmount.toLong() * CHARGE_DAMAGE_SCALE_MS_PER_HP)
                .coerceIn(0L, CHARGE_DAMAGE_SCALE_PENALTY_MAX_MS)
            val newStart = chargeStartMillis + CHARGE_DAMAGE_BASE_PENALTY_MS + scaledPenalty
            return newStart.coerceAtMost(nowMillis)
        }
    }
}
