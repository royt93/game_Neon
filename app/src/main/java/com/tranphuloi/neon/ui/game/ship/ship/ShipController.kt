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
    /** Wave 17o — test injection: ép thời điểm bắt đầu spawn (default = now). */
    private val spawnStartMillisOverride: Long? = null,
    private val onShipDestroyed: () -> Unit = {},
    private val onShipDamaged: () -> Unit = {},
    private val onBoosterPickedUp: (xOffset: Float, yOffset: Float) -> Unit = { _, _ -> },
    // Task 01 (Slice 4) — nhặt DRONE_BOOSTER → GameState spawn 1 drone companion.
    private val onDroneBoosterPickedUp: () -> Unit = {},
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
    /**
     * Round 75 (R75a/b) — meta upgrade rank lookups. GameState passes these so
     * ShipController applies meta bonuses (BULLET_DURATION extend, SHIELD
     * duration extend, DASH iframe bonus) without thread-routing the full map.
     */
    private val bulletDurationRank: () -> Int = { 0 },
    private val shieldDurationRank: () -> Int = { 0 },
    private val dashRank: () -> Int = { 0 },
    /**
     * Round 75 (R75c) — SHIELD_BURST callback. Khi shield expire, gọi callback
     * với position ship để GameState spawn mini explosion AoE damage enemies
     * trong bán kính. Max rank 2 = scale damage/radius.
     */
    private val shieldBurstRank: () -> Int = { 0 },
    private val onShieldExpireBurst: (xOffset: Float, yOffset: Float, rank: Int) -> Unit = { _, _, _ -> },
    /**
     * Wave 11a Phase 3 — REFLECT_BOOSTER retaliation. Fired when an enemy laser
     * overlaps ship while reflect is active. GameState binds this to find the
     * nearest enemy and apply 30 damage (visualized via existing damage number
     * + impact spark). x/y = enemy laser impact position (used for VFX origin).
     */
    private val onReflectAbsorb: (xOffset: Float, yOffset: Float) -> Unit = { _, _ -> },
) {

    init {
        Logger.d("ShipController init: screen=${screenWidth}x${screenHeight}, ship hp=${ship.hp}")
    }

    private val spaceShipCollidePower: Float = 100f
    private val movementSpeed: Float = 2f
    private val maxYOffset: Float = screenHeight - 140

    var movingLeft = false
    var movingRight = false

    /**
     * Round 77 (R77h) — direct touch position from hold+drag gesture. When set,
     * moveShip() snaps ship xOffset to this target instead of incremental L/R.
     * null = no active drag.
     */
    var dragTargetX: Float? = null
    var dragTargetY: Float? = null

    /**
     * Round 78 (#4 edge drag fix) — playfield extension applied at FAR/MEDIUM
     * camera zoom. `graphicsLayer.scale` shrinks the visual world to inner X%
     * of the screen leaving margins; to let the ship reach the actual screen
     * edges (which the user expects), drag bounds expand by inverse-zoom factor.
     *
     * GameState recomputes these whenever the user picks a different zoom in
     * Settings, so we don't need to query CameraZoom from inside the controller.
     * Default 0 = no extension (NEAR zoom).
     */
    // Audit-7 hardening — @Volatile cho cross-thread visibility. LaunchedEffect
    // on Main updates these when user changes camera zoom; the game loop reads
    // them on IO inside moveShip() + setDragTarget(). Without @Volatile JVM
    // memory model doesn't guarantee the IO thread sees the update.
    @Volatile
    var dragBoundsExtensionX: Float = 0f
    @Volatile
    var dragBoundsExtensionY: Float = 0f

    /** Round 77 (R77h) — clamp target into screen. Round 78 — extension at FAR zoom. */
    fun setDragTarget(x: Float, y: Float) {
        // Ship draws from top-left. Adjust để finger ở center-bottom of ship +
        // offset 50dp up để finger không che ship.
        val xMin = -dragBoundsExtensionX
        val xMax = screenWidth - ship.width + dragBoundsExtensionX
        val yMin = -dragBoundsExtensionY
        val yMax = maxYOffset + dragBoundsExtensionY
        dragTargetX = (x - ship.width / 2f).coerceIn(xMin, xMax)
        dragTargetY = (y - ship.height / 2f - 80f).coerceIn(yMin, yMax)
    }

    fun clearDragTarget() {
        dragTargetX = null
        dragTargetY = null
    }

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
    private val spawnStartMillis: Long = spawnStartMillisOverride ?: System.currentTimeMillis()
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
        // Wave 17o — FIX "ship luôn nghiêng phải": spawn-anim (sway phase 2 dùng
        // -cos*14) có thể bị GIÁN ĐOẠN (boss-intro freeze / continue) trước khi
        // phase 3 đặt rotation=0 → spawnRotation kẹt ở giá trị nghiêng. Phần
        // movement sau spawn KHÔNG bao giờ chạm spawnRotation → nghiêng vĩnh viễn.
        // Ép về 0 một lần khi spawn đã xong.
        if (ship.spawnRotation != 0f) {
            ship = ship.copy(spawnRotation = 0f)
            setShip(ship)
        }
        var newX = ship.xOffset
        var newY = ship.yOffset
        // Round 77 (R77h) — direct drag override. When dragTargetX != null,
        // ship snaps về target position (smooth lerp 30%). Skip movement
        // buttons + slip ice (drag wins).
        if (dragTargetX != null) {
            val targetX = dragTargetX!!
            val targetY = dragTargetY ?: newY
            // Wave 16 — bank theo DRAG (trước đây ép 0f → kéo tàu KHÔNG đảo cánh,
            // đúng phàn nàn của user). Độ nghiêng ∝ khoảng cách ngang còn lại tới
            // ngón tay (= hướng + tốc độ di chuyển), kẹp ±26°, lerp 0.22 cho mượt.
            // Kết hợp scaleX squash ở GameWorld → wing-roll giả-3D.
            val dxToTarget = targetX - newX
            // Smooth lerp 0.30 mỗi tick → ~5 tick để converge
            newX += (targetX - newX) * 0.30f
            newY += (targetY - newY) * 0.30f
            val dragBankTarget = (dxToTarget * 0.6f).coerceIn(-26f, 26f)
            val newBankRot = ship.bankRotation + (dragBankTarget - ship.bankRotation) * 0.22f
            ship = ship.copy(xOffset = newX, yOffset = newY, bankRotation = newBankRot)
            setShip(ship)
            return
        }
        // Wave 11a — MINI buff multiplies speed × 1.3 while active.
        val miniSpeedMul = if (ship.miniEndMillis > System.currentTimeMillis()) 1.3f else 1f
        val effSpeed = movementSpeed * speedMultiplier() * miniSpeedMul
        // Pixel-3 round 5 — removed auto-pull-to-maxYOffset entirely per user
        // feedback "tại sao position của ship player luôn bị kéo về vị trí
        // bottom". Pre-fix the auto-pull was intended only as activity-recreate
        // fallback (spawn anim skipped after pause-resume mid-spawn) but it
        // ran every tick post-spawn too, dragging the ship back to anchor
        // whenever user released drag. Now ship stays where user placed it.
        // Initial spawn anim (line 203) still positions ship correctly on
        // first frame; activity-recreate edge case re-handled via the spawn
        // anim's bi-directional lerp inside the `if (elapsed < spawnTotalMillis)`
        // block above.
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
        // Bank rotation lerp toward target. Wave 16 — góc ±16°→±26° + lerp
        // 0.18→0.22 (bẻ lái rõ + bám tay hơn); kết hợp scaleX squash ở GameWorld
        // render → "đảo cánh" wing-roll giả-3D thay vì chỉ xoay phẳng.
        // Single ship.copy() per tick to avoid 3 setShip allocations.
        val bankTarget = when {
            movingLeft -> -26f
            movingRight -> 26f
            else -> 0f
        }
        val newBankRot = ship.bankRotation + (bankTarget - ship.bankRotation) * 0.22f
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
        // Round 75 (R75a) — BASE_SHIELD meta upgrade: +1.5s mỗi rank (max 4 = +6s)
        // cộng vào base duration TRƯỚC khi nhân multiplier (rarity scale).
        val metaShieldExtensionMs = shieldDurationRank() * 1500L
        val dur = ((shieldBoosterTimeMillis + metaShieldExtensionMs) * multiplier).toLong()
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

    // ── Wave 11a — 3 new timed buffs ──

    /** REGEN: passive +1 HP/sec for 30s. Slower + longer than HEALING_AURA. */
    private val regenTimeMillis: Long = 30_000
    private var regenEndDurationMillis: Long = 0L
    private var regenLastTickMillis: Long = 0L
    private fun enableRegen(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (regenTimeMillis * multiplier).toLong() else 0L
        if (enable) regenEndDurationMillis = maxOf(regenEndDurationMillis, newEnd)
        else regenEndDurationMillis = 0L
        Logger.d("Booster: regen ${if (enable) "ON (+${newEnd - now}ms, +1HP/sec)" else "OFF"}")
    }

    /** TIME_FREEZE: 3s. GameState reads ship.timeFreezeEndMillis to gate enemy ticks. */
    private val timeFreezeTimeMillis: Long = 3_000
    private fun enableTimeFreeze(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (timeFreezeTimeMillis * multiplier).toLong() else 0L
        ship = ship.copy(timeFreezeEndMillis = if (enable) maxOf(ship.timeFreezeEndMillis, newEnd) else 0L)
        setShip(ship)
        Logger.d("Booster: time-freeze ${if (enable) "ON (+${newEnd - now}ms)" else "OFF"}")
    }

    /** MINI: ship 0.6× scale + 1.3× speed for 12s. GameWorld + moveShip read ship.miniEndMillis. */
    private val miniTimeMillis: Long = 12_000
    private fun enableMini(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (miniTimeMillis * multiplier).toLong() else 0L
        ship = ship.copy(miniEndMillis = if (enable) maxOf(ship.miniEndMillis, newEnd) else 0L)
        setShip(ship)
        Logger.d("Booster: mini ${if (enable) "ON (+${newEnd - now}ms, scale=0.6 speed=1.3)" else "OFF"}")
    }

    /** VAMPIRE: 50% lifesteal for 10s. LasersController callback queries ship.vampireEndMillis. */
    private val vampireTimeMillis: Long = 10_000
    private fun enableVampire(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (vampireTimeMillis * multiplier).toLong() else 0L
        ship = ship.copy(vampireEndMillis = if (enable) maxOf(ship.vampireEndMillis, newEnd) else 0L)
        setShip(ship)
        Logger.d("Booster: vampire ${if (enable) "ON (+${newEnd - now}ms, 50% lifesteal)" else "OFF"}")
    }

    /** GHOST: pass through enemies for 5s. ShipController collision check reads ship.ghostEndMillis. */
    private val ghostTimeMillis: Long = 5_000
    private fun enableGhost(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (ghostTimeMillis * multiplier).toLong() else 0L
        ship = ship.copy(ghostEndMillis = if (enable) maxOf(ship.ghostEndMillis, newEnd) else 0L)
        setShip(ship)
        Logger.d("Booster: ghost ${if (enable) "ON (+${newEnd - now}ms, enemy collision bypass)" else "OFF"}")
    }

    /**
     * VAMPIRE callback hook: called from LasersController.onLaserHit lambda
     * (wired in GameState) with damage dealt. While ship.vampireEndMillis > now,
     * heals ship by `damage * 0.5` (silent — no log spam per hit).
     */
    /**
     * Wave 17r — heal QUA controller (cập nhật ship NỘI BỘ + setShip). GameState
     * KHÔNG được `ship.copy(hp=...)` trực tiếp: tick moveShip kế tiếp ghi đè bằng
     * internal ship → heal MẤT (cùng class bug loadout). Dùng cho BANH_MI heal.
     */
    fun healCapped(amount: Int, maxHp: Int = 1000) {
        if (ship.hp <= 0 || amount <= 0) return
        ship = ship.copy(hp = (ship.hp + amount).coerceIn(0, maxHp))
        setShip(ship)
    }

    /**
     * Task 05 — bật timer hiệu ứng kỹ năng chủ động (duration-based). Chỉ set mốc
     * hết hạn trên [Ship]; áp dụng đọc lazy ở damage lambda / freeze gate / collision.
     * Instant effect (NOVA/REPAIR/MAGNET/LASER_STORM) xử lý ở GameState, không qua đây.
     */
    fun activateAbilityTimer(
        effect: com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect,
        nowMillis: Long,
        durationMs: Long,
    ) {
        if (durationMs <= 0L) return
        val end = nowMillis + durationMs
        ship = when (effect) {
            com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.BULWARK,
            com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.PHASE_DASH,
            com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.DECOY ->
                ship.copy(abilityInvulnEndMillis = end)
            com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.OVERDRIVE ->
                ship.copy(abilityOverdriveEndMillis = end)
            com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.CRIT_FRENZY ->
                ship.copy(abilityCritEndMillis = end)
            com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect.TIME_DILATION ->
                ship.copy(abilityFreezeEndMillis = end)
            else -> ship
        }
        setShip(ship)
    }

    /** Wave 17r — đặt thẳng hp (vd BOSS_RUSH hồi đầy giữa boss) qua controller. */
    fun setHp(value: Int) {
        if (ship.hp <= 0) return
        ship = ship.copy(hp = value.coerceAtLeast(0))
        setShip(ship)
    }

    fun applyVampireHeal(damageDealt: Int) {
        if (ship.vampireEndMillis <= System.currentTimeMillis() || damageDealt <= 0) return
        val heal = (damageDealt * 0.5f).toInt().coerceAtLeast(1)
        updateHp(heal, silent = true)
    }

    /** Public read for GameWorld + collision gates. True when ghost active. */
    fun isGhostActive(): Boolean = ship.ghostEndMillis > System.currentTimeMillis()

    /** GRAVITY: ALL minerals auto-collect for 10s. GameState magnet lambda × 100 while active. */
    private val gravityTimeMillis: Long = 10_000
    private fun enableGravity(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (gravityTimeMillis * multiplier).toLong() else 0L
        ship = ship.copy(gravityEndMillis = if (enable) maxOf(ship.gravityEndMillis, newEnd) else 0L)
        setShip(ship)
        Logger.d("Booster: gravity ${if (enable) "ON (+${newEnd - now}ms, magnet ×100)" else "OFF"}")
    }

    /** Public read for GameState magnet lambda. */
    fun isGravityActive(): Boolean = ship.gravityEndMillis > System.currentTimeMillis()

    /** REFLECT: absorb enemy lasers + retaliate 30 dmg × rarity for 8s. */
    private val reflectTimeMillis: Long = 8_000
    private var reflectDamageMul: Float = 1f
    private fun enableReflect(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (reflectTimeMillis * multiplier).toLong() else 0L
        ship = ship.copy(reflectEndMillis = if (enable) maxOf(ship.reflectEndMillis, newEnd) else 0L)
        // P2 audit fix — REFLECT downgrade asymmetry: timer uses max(old, new)
        // (never downgrades), so damage mul should also never downgrade. Was:
        // Epic active → Common pickup kept Epic timer but dropped damage 60→30.
        if (enable) reflectDamageMul = maxOf(reflectDamageMul, multiplier)
        else reflectDamageMul = 1f  // reset on expiry so next pickup starts clean
        setShip(ship)
        Logger.d("Booster: reflect ${if (enable) "ON (+${newEnd - now}ms, ${(30 * reflectDamageMul).toInt()}dmg back)" else "OFF"}")
    }

    /** Public read for collision gate. */
    fun isReflectActive(): Boolean = ship.reflectEndMillis > System.currentTimeMillis()

    /** Rarity-scaled retaliation damage (30 base × Common/Rare/Epic mul). */
    fun reflectRetaliationDamage(): Int = (30 * reflectDamageMul).toInt().coerceAtLeast(30)

    /**
     * Task 13 (đợt 4) — Parry: mở cửa sổ reflect tới [endMillis] (không ngắn hơn
     * hiện tại). Tái dùng nhánh absorb+retaliate của REFLECT trong collision →
     * parry phản đạn + phản đòn địch gần + miễn thương đạn hấp thụ, không cần code mới.
     */
    fun grantReflectUntil(endMillis: Long) {
        ship = ship.copy(reflectEndMillis = maxOf(ship.reflectEndMillis, endMillis))
    }

    /** CHAIN_LIGHTNING: each laser hit chains to 2 more nearest enemies (50% dmg) for 10s. */
    private val chainLightningTimeMillis: Long = 10_000
    private fun enableChainLightning(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (chainLightningTimeMillis * multiplier).toLong() else 0L
        ship = ship.copy(chainLightningEndMillis = if (enable) maxOf(ship.chainLightningEndMillis, newEnd) else 0L)
        setShip(ship)
        Logger.d("Booster: chain-lightning ${if (enable) "ON (+${newEnd - now}ms, 3-target chain)" else "OFF"}")
    }

    /** Public read for GameState onLaserHit chain dispatch. */
    fun isChainLightningActive(): Boolean = ship.chainLightningEndMillis > System.currentTimeMillis()

    /** CLONE: phantom-twin ship firing alongside for 8s. LasersController + GameWorld read ship.cloneEndMillis. */
    private val cloneTimeMillis: Long = 8_000
    private fun enableClone(enable: Boolean, multiplier: Float = 1f) {
        val now = System.currentTimeMillis()
        val newEnd = if (enable) now + (cloneTimeMillis * multiplier).toLong() else 0L
        ship = ship.copy(cloneEndMillis = if (enable) maxOf(ship.cloneEndMillis, newEnd) else 0L)
        setShip(ship)
        Logger.d("Booster: clone ${if (enable) "ON (+${newEnd - now}ms, phantom firing)" else "OFF"}")
    }

    /** Public read for LasersController + GameWorld. */
    fun isCloneActive(): Boolean = ship.cloneEndMillis > System.currentTimeMillis()

    /**
     * Wave 17l — FIX "đổi đạn vẫn y hệt": áp đạn loadout vào ship NỘI BỘ của
     * controller. Trước đây GameState set `ship.copy(...)` TRỰC TIẾP lên state
     * của nó, nhưng controller giữ `private var ship` riêng → tick movement/
     * iframes kế tiếp ghi đè NORMAL trở lại. Phải đi qua đây để cả 2 đồng bộ.
     */
    /**
     * Wave 17q — san phẳng tilt spawn (spawnRotation=0) NGAY, không qua moveShip.
     * Dùng khi boss-intro freeze chặn moveShip → reset post-spawn không chạy →
     * ship kẹt nghiêng suốt cinematic. Gọi lúc trigger intro.
     */
    fun settleSpawnRotation() {
        if (ship.spawnRotation != 0f) {
            ship = ship.copy(spawnRotation = 0f)
            setShip(ship)
        }
    }

    fun setLoadoutBullet(type: com.tranphuloi.neon.ui.game.ship.laser.BulletType) {
        ship = ship.copy(
            baseBulletType = type,
            activeBulletType = type,
            bulletTypeEndMillis = 0L,
        )
        setShip(ship)
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
        // Wave 11a Phase 1 — MINI_BOOSTER hitbox audit fix. Visual ship scales
        // 0.6× via graphicsLayer in GameWorld; hitbox now also scales toward
        // center so spec "hitbox bé hơn, né dễ hơn" delivers. Pivot 0.5/0.5 →
        // offset shifted by half the shrink amount.
        val miniMul = if (ship.miniEndMillis > System.currentTimeMillis()) 0.6f else 1f
        // Task 05 — kỹ năng bất tử (BULWARK/PHASE_DASH/DECOY): coi như khiên → 0 dmg.
        val abilityInvuln = ship.abilityInvulnEndMillis > System.currentTimeMillis()
        val shipRect by lazy {
            val w = ship.width * miniMul
            val h = ship.height * miniMul
            val dx = (ship.width - w) / 2f
            val dy = (ship.height - h) / 2f
            Rect(
                offset = Offset(x = ship.xOffset + dx, y = ship.yOffset + dy),
                size = Size(width = w, height = h),
            )
        }
        // Audit follow-up: shield rect now also scales with MINI so the
        // SHIELD+MINI combo respects spec ("smaller hitbox") instead of full-
        // size shield silently overriding mini scale.
        val shipShieldRect by lazy {
            Rect(
                center = Offset(
                    x = ship.xOffset + ship.width / 2,
                    y = ship.yOffset + ship.height / 2
                ),
                radius = ship.shieldRadius * miniMul
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

                val hpImpact: Int = when ((ship.shieldEnabled || abilityInvuln) && spaceObject.impactPower > 0) {
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
                    // Round 67 (Wave 10a) — 3 bullet-type boosters.
                    BoosterType.FIRE_BOOSTER -> setBulletType(
                        com.tranphuloi.neon.ui.game.ship.laser.BulletType.FIRE,
                        multiplier = mul,
                        rarity = booster.rarity,
                    )
                    BoosterType.HOMING_BOOSTER -> setBulletType(
                        com.tranphuloi.neon.ui.game.ship.laser.BulletType.HOMING,
                        multiplier = mul,
                        rarity = booster.rarity,
                    )
                    BoosterType.BOUNCE_BOOSTER -> setBulletType(
                        com.tranphuloi.neon.ui.game.ship.laser.BulletType.BOUNCE,
                        multiplier = mul,
                        rarity = booster.rarity,
                    )
                    BoosterType.GIANT_BOOSTER -> setBulletType(
                        com.tranphuloi.neon.ui.game.ship.laser.BulletType.GIANT,
                        multiplier = mul,
                        rarity = booster.rarity,
                    )
                    // Round 68 (Wave 10 finish) — 5 bullets stub dispatch.
                    BoosterType.SMOKE_BOOSTER -> setBulletType(
                        com.tranphuloi.neon.ui.game.ship.laser.BulletType.SMOKE,
                        multiplier = mul, rarity = booster.rarity,
                    )
                    BoosterType.ZIGZAG_BOOSTER -> setBulletType(
                        com.tranphuloi.neon.ui.game.ship.laser.BulletType.ZIGZAG,
                        multiplier = mul, rarity = booster.rarity,
                    )
                    BoosterType.KAMEHAMEHA_BOOSTER -> setBulletType(
                        com.tranphuloi.neon.ui.game.ship.laser.BulletType.KAMEHAMEHA,
                        multiplier = mul, rarity = booster.rarity,
                    )
                    BoosterType.ATOMIC_BOOSTER -> setBulletType(
                        com.tranphuloi.neon.ui.game.ship.laser.BulletType.ATOMIC,
                        multiplier = mul, rarity = booster.rarity,
                    )
                    BoosterType.SPLIT_BOOSTER -> setBulletType(
                        com.tranphuloi.neon.ui.game.ship.laser.BulletType.SPLIT,
                        multiplier = mul, rarity = booster.rarity,
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
                    // Wave 11a — 3 new timed buffs.
                    BoosterType.REGEN_BOOSTER -> {
                        enableRegen(enable = true, multiplier = mul)
                        regenLastTickMillis = System.currentTimeMillis()
                    }
                    BoosterType.TIME_FREEZE_BOOSTER -> enableTimeFreeze(enable = true, multiplier = mul)
                    BoosterType.MINI_BOOSTER -> enableMini(enable = true, multiplier = mul)
                    BoosterType.VAMPIRE_BOOSTER -> enableVampire(enable = true, multiplier = mul)
                    BoosterType.GHOST_BOOSTER -> enableGhost(enable = true, multiplier = mul)
                    BoosterType.GRAVITY_BOOSTER -> enableGravity(enable = true, multiplier = mul)
                    BoosterType.REFLECT_BOOSTER -> enableReflect(enable = true, multiplier = mul)
                    BoosterType.CHAIN_LIGHTNING_BOOSTER -> enableChainLightning(enable = true, multiplier = mul)
                    BoosterType.CLONE_BOOSTER -> enableClone(enable = true, multiplier = mul)
                    // Task 01 (Slice 4) — spawn drone companion (logic ở DroneController qua GameState).
                    BoosterType.DRONE_BOOSTER -> onDroneBoosterPickedUp()
                }
            }
        }
        // Wave 11a Phase 2 — GHOST_BOOSTER skips enemy collisions while active.
        // Player can drift through enemy ships unscathed. Enemy lasers still hit
        // (intentional balance — ghost ≠ invulnerable).
        val ghostActive = isGhostActive()
        enemies.forEachIndexed { enemyIndex, enemy ->
            val enemyRect by lazy {
                Rect(
                    offset = Offset(x = enemy.xOffset, y = enemy.yOffset),
                    size = Size(width = enemy.width, height = enemy.height)
                )
            }
            // P0 audit fix — TIME_FREEZE_BOOSTER now also skips ship↔enemy ram
            // collision (was only gating enemy.process tick). Spec "đóng băng
            // thời gian" = world halt, ship phases through frozen bodies.
            val timeFrozen = ship.timeFreezeEndMillis > System.currentTimeMillis()
            if (!ghostActive && !timeFrozen && enemyRect.overlaps(if (ship.shieldEnabled) shipShieldRect else shipRect)) {
                Logger.d("Collision: ship ↔ enemy id=${enemy.enemyId.take(6)} (shield=${ship.shieldEnabled})")
                enemies[enemyIndex].onObjectImpact(spaceShipCollidePower)

                val hpImpact: Int = when ((ship.shieldEnabled || abilityInvuln) && enemy.impactPower > 0) {
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
                enemyLasers[enemyIndex].destroyed = true
                // Wave 11a Phase 3 — REFLECT_BOOSTER absorbs the laser + retaliates.
                // Reflect takes priority over normal damage path (still respects
                // shield-no-damage rule via the same conditional below).
                if (isReflectActive()) {
                    Logger.v { "Collision: ship ↔ enemyLaser ABSORBED by reflect → retaliate" }
                    onReflectAbsorb(enemyLaser.xOffset + enemyLaser.width / 2f,
                                    enemyLaser.yOffset + enemyLaser.height / 2f)
                    return@forEachIndexed
                }
                Logger.d("Collision: ship ↔ enemyLaser (shield=${ship.shieldEnabled}, impactPower=${enemyLaser.impactPower.toInt()})")
                val hpImpact: Float = when ((ship.shieldEnabled || abilityInvuln) && enemyLaser.impactPower > 0) {
                    true -> 0f
                    false -> enemyLaser.impactPower
                }
                updateHp(-hpImpact.toInt())
            }
        }

        val currentTime = System.currentTimeMillis()
        // Round 75 (R75c) — detect shield expiry edge để fire SHIELD_BURST.
        // Track previous wasShieldEnabled so we only fire ON transition (not every tick).
        val wasShielded = ship.shieldEnabled
        if (shieldEndDurationMillis < currentTime) enableShield(enable = false)
        // Edge-detect: shield was ON, now OFF → trigger burst.
        if (wasShielded && !ship.shieldEnabled) {
            val rank = shieldBurstRank()
            if (rank > 0) {
                val shipCx = ship.xOffset + ship.width / 2f
                val shipCy = ship.yOffset + ship.height / 2f
                Logger.d("ShipController: SHIELD_BURST fire @ ($shipCx, $shipCy) rank=$rank")
                onShieldExpireBurst(shipCx, shipCy, rank)
            }
        }
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
        // Wave 11a — REGEN +1 HP/sec passive while active (200ms-chunk like
        // HEALING_AURA but slower rate). Silent log to avoid 30 lines/pickup.
        if (regenEndDurationMillis in 1..currentTime) enableRegen(enable = false)
        if (currentTime < regenEndDurationMillis) {
            val sinceTick = currentTime - regenLastTickMillis
            if (sinceTick >= 1000L) {                              // heal in 1s chunks (1 hp/sec)
                val heal = (sinceTick / 1000L).toInt()
                if (heal > 0 && ship.hp > 0) {
                    updateHp(heal, silent = true)
                    regenLastTickMillis = currentTime
                }
            }
        }
        // Wave 11a — TIME_FREEZE expiry: clear flag on Ship state.
        if (ship.timeFreezeEndMillis in 1..currentTime) {
            ship = ship.copy(timeFreezeEndMillis = 0L)
            setShip(ship)
            Logger.d("Booster: time-freeze OFF")
        }
        // Wave 11a — MINI expiry: clear flag on Ship state (GameWorld scale + speed mul revert).
        if (ship.miniEndMillis in 1..currentTime) {
            ship = ship.copy(miniEndMillis = 0L)
            setShip(ship)
            Logger.d("Booster: mini OFF")
        }
        // Wave 11a Phase 2 — VAMPIRE expiry.
        if (ship.vampireEndMillis in 1..currentTime) {
            ship = ship.copy(vampireEndMillis = 0L)
            setShip(ship)
            Logger.d("Booster: vampire OFF")
        }
        // Wave 11a Phase 2 — GHOST expiry.
        if (ship.ghostEndMillis in 1..currentTime) {
            ship = ship.copy(ghostEndMillis = 0L)
            setShip(ship)
            Logger.d("Booster: ghost OFF")
        }
        // Wave 11a Phase 3 — GRAVITY / REFLECT / CHAIN_LIGHTNING expiries.
        if (ship.gravityEndMillis in 1..currentTime) {
            ship = ship.copy(gravityEndMillis = 0L)
            setShip(ship)
            Logger.d("Booster: gravity OFF")
        }
        if (ship.reflectEndMillis in 1..currentTime) {
            ship = ship.copy(reflectEndMillis = 0L)
            setShip(ship)
            Logger.d("Booster: reflect OFF")
        }
        if (ship.chainLightningEndMillis in 1..currentTime) {
            ship = ship.copy(chainLightningEndMillis = 0L)
            setShip(ship)
            Logger.d("Booster: chain-lightning OFF")
        }
        if (ship.cloneEndMillis in 1..currentTime) {
            ship = ship.copy(cloneEndMillis = 0L)
            setShip(ship)
            Logger.d("Booster: clone OFF")
        }
        // Round 35 (35x) — expire active bullet type.
        // Wave 14 — revert to the loadout base bullet (whole-run weapon), not
        // NORMAL, so a temporary booster expiring doesn't strip the chosen gun.
        if (ship.bulletTypeEndMillis > 0L && currentTime >= ship.bulletTypeEndMillis) {
            Logger.d("BulletType: ${ship.activeBulletType} expired → revert to base ${ship.baseBulletType}")
            ship = ship.copy(
                activeBulletType = ship.baseBulletType,
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
        // Round 75 (R75a) — BULLET_DURATION meta upgrade: +10% per rank cộng dồn.
        val metaDurMul = 1f + bulletDurationRank() * 0.10f
        val dur = (type.activeDurationMillis * multiplier * metaDurMul).toLong()
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
    /** Round 75 (R75b) — REGEN meta: timestamp of last hp damage. */
    private var lastDamagedMillis: Long = 0L

    /**
     * Round 75 (R75b) — REGEN SkillNode tick. Called from game loop @ 1s rate.
     * Hồi +5 HP / rank nếu (now - lastDamagedMillis) > 3000ms (3s no damage).
     * Tự cap khỏi vượt initialHp (passed in).
     */
    fun regenTick(rank: Int, initialHp: Int) {
        if (rank <= 0) return
        if (ship.hp <= 0 || ship.hp >= initialHp) return
        val now = System.currentTimeMillis()
        if (now - lastDamagedMillis < 3000L) return
        // Don't stack với HEALING_AURA — let aura priority.
        if (now < healingAuraEndDurationMillis) return
        val regenAmount = rank * 5
        val newHp = (ship.hp + regenAmount).coerceAtMost(initialHp)
        if (newHp > ship.hp) {
            ship = ship.copy(hp = newHp)
            setShip(ship)
            Logger.v { "ShipController.regenTick: hp ${ship.hp - regenAmount}→${ship.hp} (rank=$rank)" }
        }
    }

    /**
     * Round 75 (R75b) — DASH SkillNode: post-hit extra iframe window.
     * Called from updateHp when damaged. +200ms/rank extra iframes beyond
     * the baseline IFRAMES_DURATION_MILLIS (600ms).
     */
    fun applyDashIframes(rank: Int) {
        if (rank <= 0) return
        val bonusMs = rank * 200L
        iframesEndMillis = maxOf(iframesEndMillis, System.currentTimeMillis() + bonusMs + IFRAMES_DURATION_MILLIS)
        Logger.v { "ShipController.applyDashIframes: rank=$rank → +${bonusMs}ms iframes" }
    }

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
            // Round 75 (R75b) — track damage time cho REGEN tick + DASH bonus.
            lastDamagedMillis = System.currentTimeMillis()
            val dashBonusMs = dashRank() * 200L
            iframesEndMillis = System.currentTimeMillis() + IFRAMES_DURATION_MILLIS + dashBonusMs
            Logger.v { "Ship i-frames: ON until $iframesEndMillis (+${IFRAMES_DURATION_MILLIS + dashBonusMs}ms, dashRank=${dashRank()})" }
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
