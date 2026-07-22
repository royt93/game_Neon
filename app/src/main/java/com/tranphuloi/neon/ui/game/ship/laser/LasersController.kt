package com.tranphuloi.neon.ui.game.ship.laser

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.ship.laser.ShipBoostedLaser.Companion.SHIP_BOOSTED_LASER_WIDTH
import com.tranphuloi.neon.ui.game.ship.laser.ShipLaser.Companion.SHIP_LASER_WIDTH
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.ui.game.ship.ship.ShipController.Companion.TRIPLE_LASER_SIDE_OFFSET
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObject
import com.tranphuloi.neon.ui.game.state.EffectiveStats
import com.tranphuloi.neon.utils.Logger
import com.tranphuloi.neon.utils.UuidUtils

class LasersController(
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val uuidUtils: UuidUtils,
    initialShipLasers: List<Laser> = listOf(),
    initialUltimateLasers: List<Laser> = listOf(),
    private val setShipLasers: (List<Laser>) -> Unit,
    private val setUltimateLasers: (List<Laser>) -> Unit,
    // Wave 11c — onLaserHit carries [bulletType] for precise telemetry
    // attribution (replaces prior ship.activeBulletType approximation in
    // GameState.onEnemyKilled). PLASMA AoE splash forwards the source
    // laser's bulletType for every splash victim, so AoE kills attribute
    // back to PLASMA, not to the spread effect.
    private val onLaserHit: (targetId: String, damage: Int, x: Float, y: Float, isBoss: Boolean, bulletType: BulletType) -> Unit = { _, _, _, _, _, _ -> },
    /**
     * Wave 5 (25x / 48x) — damage multiplier applied at hit time. Combines
     * RunModifier (GLASS_CANNON, BERSERKER, DOUBLE_OR_NOTHING) + skill tree
     * FIREPOWER node. Default 1.0 = no boost.
     */
    private val damageMultiplier: () -> Float = { 1f },
    /** Task 24 — FIRE_RATE skill node rank; -5%/rank on fire interval. */
    private val fireRateRank: () -> Int = { 0 },
    /** Task 24 — PIERCE_CHANCE skill node rank; +10%/rank chance a NORMAL bullet also pierces once. */
    private val pierceChanceRank: () -> Int = { 0 },
) {

    init {
        Logger.d("LasersController init: initialShipLasers=${initialShipLasers.size}, initialUltimateLasers=${initialUltimateLasers.size}")
    }

    private var shipLasers: List<Laser> = initialShipLasers
    private var ultimateLasers: List<Laser> = initialUltimateLasers

    // Wave 11d Bug #1 fix — camera-zoom FAR extends the visible x range past
    // [0, screenWidth] into [-extraXSpan, screenWidth + extraXSpan]. Enemies
    // spawn into that extended band, so the UltimateLaser sweep must cover it
    // too — otherwise enemies sitting in the right-edge margin survive the
    // sweep entirely. GameState wires this on camera-zoom change (mirrors
    // enemyController.setSpawnXMargin).
    // Audit-7 hardening — @Volatile ensures Main-thread write (LaunchedEffect)
    // is visible to IO-thread read (game loop) without relying on luck. Float
    // read/write was already atomic; this adds the memory-barrier visibility
    // guarantee.
    @Volatile
    private var extraXSpan: Float = 0f

    fun setExtraXSpan(margin: Float) {
        if (extraXSpan != margin) {
            Logger.d("LasersController.setExtraXSpan: $extraXSpan → $margin (UltimateLaser sweep extended)")
            extraXSpan = margin
        }
    }

    /**
     * Pixel-3 #2 deep-audit fix — Y-axis extension companion to setExtraXSpan.
     *
     * Pre-fix UltimateLaser beams spawned at `yOffset = screenHeight` (= 891
     * game-coord), which at FAR camera zoom (scale=0.7) renders at device-y
     * 757. Device shows game-y down to ~1081, so the bottom band 757..891 on
     * device NEVER saw a beam pass through. User's "laser effect zone không
     * phủ full screen at zoom xa" complaint.
     *
     * With extraYSpan = 190 at FAR, beam starts at game-y 1081 → renders at
     * device-y 891 (device-bottom). Sweep covers the full visible band.
     */
    @Volatile
    private var extraYSpan: Float = 0f

    fun setExtraYSpan(margin: Float) {
        if (extraYSpan != margin) {
            Logger.d("LasersController.setExtraYSpan: $extraYSpan → $margin (UltimateLaser start extended)")
            extraYSpan = margin
        }
    }

    val fireLaserId = uuidUtils.getUuid()
    // Wave 14a — `var` so the Gói Bắn Nhanh consumable can shorten the fire
    // interval for the whole run (read each tick by the game loop's tinker).
    var fireLaserRepeatTime: com.tranphuloi.neon.ui.game.common.RepeatTime = Millis(100)

    // Wave 18b — hệ số nhân nhịp bắn (Gói Bắn Nhanh đặt <1 để bắn nhanh hơn).
    // Nhân với BulletType.fireIntervalMillis trong fireLasers → cadence cuối.
    var rapidFireMultiplier: Float = 1f
    fun fireLasers(ship: Ship) {
        // Round 47 — cap so laser-booster spam + triple-laser at firing rate 100ms
        // doesn't allow the in-flight list to grow unbounded during heavy waves.
        // 25 ≈ ~2.5s of fire at max rate; off-screen scroll keeps the list flowing.
        if (shipLasers.size >= MAX_SHIP_LASERS) return

        // Round 61 — unified spread + double pipeline. Pre-round-61 the bullet-type
        // path (PIERCING/PLASMA) early-returned so SPREAD_SHOT + DOUBLE_FIRE buffs
        // had no effect while a bullet-type buff was active — picking up SPREAD_SHOT
        // during PLASMA's 10s head-start would silently waste the buff. New flow:
        //   1. Compute x-offset list (spread > triple > single)
        //   2. Compute y-offset list (double = 2 stacked, else single)
        //   3. Build one laser per (dx, dy) combo via [buildOneLaser], which
        //      dispatches on activeBulletType (PIERCING/PLASMA/NORMAL).
        // Every buff combination now layers correctly.
        val xShifts: List<Float> = when {
            ship.spreadShotEnabled -> {
                // 5-way fan, ~(TRIPLE_LASER_SIDE_OFFSET + 4) spacing for visual clarity.
                val step = TRIPLE_LASER_SIDE_OFFSET + 4f
                listOf(-2f * step, -step, 0f, step, 2f * step)
            }
            ship.tripleLaserBoosterEnabled -> {
                listOf(-TRIPLE_LASER_SIDE_OFFSET, 0f, TRIPLE_LASER_SIDE_OFFSET)
            }
            else -> listOf(0f)
        }
        // Wave 18b — SỐ VIÊN mỗi loạt theo item đạn (salvoCount), xếp chồng dọc;
        // double-fire (nếu bật) nhân thêm. salvo=1 (mặc định) → giữ nguyên 1 hàng.
        val baseY: List<Float> = if (ship.doubleFireEnabled) listOf(0f, 22f) else listOf(0f)
        val salvo = ship.activeBulletType.salvoCount.coerceAtLeast(1)
        val yShifts: List<Float> = (0 until salvo).flatMap { s -> baseY.map { it + s * 14f } }

        // Wave 11a Phase 4 — CLONE_BOOSTER spawns phantom-twin ship at +50dp
        // offset firing alongside main. Each laser column duplicates at the
        // clone offset. Compound with SPREAD_SHOT + TRIPLE_LASER + DOUBLE_FIRE
        // so a fully-buffed player can output 2 × 5 × 2 = 20 lasers/shot.
        // MAX_SHIP_LASERS cap (line 47) prevents in-flight bloat.
        val cloneActive = ship.cloneEndMillis > System.currentTimeMillis()
        val cloneOffset = 50f
        val effectiveXShifts: List<Float> = if (cloneActive) {
            xShifts.flatMap { x -> listOf(x, x + cloneOffset) }
        } else {
            xShifts
        }

        val newLasers = buildList {
            for (dy in yShifts) for (dx in effectiveXShifts) {
                add(buildOneLaser(ship, dx, dy))
            }
        }

        shipLasers = shipLasers + newLasers
        updateShipLasersUI()

        // Wave 18b — NHỊP BẮN tiếp theo do ITEM ĐẠN quy định (mạnh→thưa). Đọc bởi
        // game-loop tinker ở lần kế. Gói Bắn Nhanh thu nhỏ qua rapidFireMultiplier.
        // Sàn 20ms để không bao giờ thành bắn-vô-hạn.
        // Task 24 — FIRE_RATE skill node nhân thêm hệ số vào nhịp bắn, sàn 0.5x
        // (không cho phép cộng dồn Gói Bắn Nhanh + FIRE_RATE làm bắn liên thanh vô hạn).
        val fireRateFactor = (1f - fireRateRank() * EffectiveStats.META_FIRE_RATE_PER_RANK).coerceAtLeast(0.5f)
        fireLaserRepeatTime = Millis(
            (ship.activeBulletType.fireIntervalMillis * rapidFireMultiplier * fireRateFactor).toInt().coerceAtLeast(20),
        )
    }

    /**
     * Round 61 — single point of laser construction. Picks the correct subclass
     * based on `ship.activeBulletType` + `ship.laserBoosterEnabled`, applies
     * spread (dx) + double (dy) offsets, and re-runs the per-bullet-type setup
     * (pierce count / AoE radius) inside `.also { }`.
     *
     * Why a helper instead of inline branches: the spread × double × bullet-type
     * × laser-booster matrix has 24 combos. Centralising removes risk of one
     * branch drifting from another (e.g. PIERCING + DOUBLE_FIRE forgetting
     * `pierceRemaining`).
     */
    private fun buildOneLaser(ship: Ship, dx: Float, dy: Float): Laser {
        // Wave 17q — SIZE đạn là SINGLE-SOURCE từ model (BulletType.bodyWidth):
        // ép width + căn giữa sau when nên đổi size chỉ ở model, buildOneLaser
        // không còn hardcode lệch. (width từng nhánh dưới chỉ là khởi tạo, bị ghi đè.)
        val bw = ship.activeBulletType.bodyWidth
        val laser = (when (ship.activeBulletType) {
            BulletType.PIERCING -> PiercingShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 3f + dx,
                yOffset = ship.yOffset - 22f + dy,
                yRange = screenHeight,
            ).also {
                it.pierceRemaining =
                    BulletType.pierceCountForRarity(ship.activeBulletTypeRarity)
            }
            BulletType.PLASMA -> PlasmaShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 22f / 2 + dx,
                yOffset = ship.yOffset - 34f + dy,                  // -22 - 12
                yRange = screenHeight,
                width = 22f,                             // Wave 17h — orb plasma rõ
            ).also {
                it.aoeRadiusMultiplier =
                    BulletType.plasmaAoeMultiplierForRarity(ship.activeBulletTypeRarity)
            }
            // Round 67 — FIRE bullet: reuses ShipLaser body. Behavior via
            // BURN status applied in onLaserHit (GameState wiring).
            BulletType.FIRE -> if (ship.laserBoosterEnabled) {
                ShipBoostedLaser(
                    id = uuidUtils.getUuid(),
                    xOffset = ship.xOffset + ship.width / 2 - SHIP_BOOSTED_LASER_WIDTH / 2 + dx,
                    yOffset = ship.yOffset - 25f + dy,
                    yRange = screenHeight,
                    bulletType = BulletType.FIRE,
                )
            } else {
                ShipLaser(
                    id = uuidUtils.getUuid(),
                    xOffset = ship.xOffset + ship.width / 2 - 7f / 2 + dx,
                    yOffset = ship.yOffset - 20f + dy,
                    yRange = screenHeight,
                    width = 7f,                          // Wave 17 — size riêng (lửa hơi bự)
                    bulletType = BulletType.FIRE,
                )
            }
            // Round 67 — HOMING: reuse MissileLaser for ship lasers (already
            // tracks nearest enemy per round 40 pattern). Width 8 = same as
            // ShipLaser so visual proportions match other bullets.
            BulletType.HOMING -> MissileLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 4f + dx,
                yOffset = ship.yOffset - 24f + dy,
                yRange = screenHeight,
            )
            // Round 67 — BOUNCE: ShipLaser with bounceRemaining=3 ricochet
            // tracking via BounceShipLaser subclass.
            BulletType.BOUNCE -> BounceShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 9f / 2 + dx,
                yOffset = ship.yOffset - 20f + dy,
                yRange = screenHeight,
                width = 9f,                              // Wave 17 — size riêng
                screenWidth = screenWidth,
            )
            // Round 67.5 — GIANT: ×2 size + ×2 damage (mul applied via
            // BulletType.damageMultiplier in damageMultiplier lambda).
            BulletType.GIANT -> GiantShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 17f + dx,   // căn giữa theo width 34
                yOffset = ship.yOffset - 40f + dy,
                yRange = screenHeight,
            )
            // Wave 14 — KAMEHAMEHA: pierce-all (reuse PiercingShipLaser body with
            // its own bulletType → beam shape + pierce collision). pierceRemaining
            // = KAMEHAMEHA.pierceCount (99) set in ctor.
            BulletType.KAMEHAMEHA -> PiercingShipLaser(
                id = uuidUtils.getUuid(),
                // Wave 17 — BEAM to bản (width 22 vs PIERCING 6) → đọc rõ là "chùm
                // sóng" xuyên-tất (×3 dmg, pierce 99), khác hẳn PIERCING thân mảnh.
                xOffset = ship.xOffset + ship.width / 2 - 18f + dx,
                yOffset = ship.yOffset - 22f + dy,
                yRange = screenHeight,
                width = 36f,                             // Wave 17h — beam bản RẤT RỘNG
                bulletType = BulletType.KAMEHAMEHA,
            )
            // Wave 14 — ATOMIC: AoE 150 (reuse PlasmaShipLaser body with its own
            // bulletType → atomic shape + AoE uses ATOMIC.aoeRadius=150).
            BulletType.ATOMIC -> PlasmaShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 28f / 2 + dx,
                yOffset = ship.yOffset - 34f + dy,
                yRange = screenHeight,
                width = 28f,                             // Wave 17h — to nhất nhóm nổ
                bulletType = BulletType.ATOMIC,
            )
            // Wave 16 (Slice 3) — ZIGZAG: sine-weaving path (dedicated class).
            BulletType.ZIGZAG -> ZigZagShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 3f / 2 + dx,
                yOffset = ship.yOffset - 20f + dy,
                yRange = screenHeight,
                width = 3f,                              // Wave 17h — kim mảnh nhất
            )
            // Wave 16 (Slice 3) — SMOKE: slow fat puff, small AoE on hit
            // (splash handled in the PLASMA/ATOMIC/SMOKE collision arm).
            BulletType.SMOKE -> SmokeShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 38f / 2 + dx,
                yOffset = ship.yOffset - 25f + dy,
                yRange = screenHeight,
                width = 38f,                             // Wave 17h — cuộn khói bự nhất
            )
            // Wave 16 (Slice 3) — SPLIT: normal body; on hit it spawns 3 NORMAL
            // children (handled in the SPLIT collision arm). Keeps booster look.
            BulletType.SPLIT -> if (ship.laserBoosterEnabled) {
                ShipBoostedLaser(
                    id = uuidUtils.getUuid(),
                    xOffset = ship.xOffset + ship.width / 2 - SHIP_BOOSTED_LASER_WIDTH / 2 + dx,
                    yOffset = ship.yOffset - 25f + dy,
                    yRange = screenHeight,
                    bulletType = BulletType.SPLIT,
                )
            } else {
                ShipLaser(
                    id = uuidUtils.getUuid(),
                    xOffset = ship.xOffset + ship.width / 2 - 11f / 2 + dx,
                    yOffset = ship.yOffset - 20f + dy,
                    yRange = screenHeight,
                    width = 11f,                         // Wave 17 — size riêng
                    bulletType = BulletType.SPLIT,
                )
            }
            // Task 15 — AIRBURST: thân thường; trúng địch nổ 8 con toả quạt (arm collision).
            BulletType.AIRBURST -> if (ship.laserBoosterEnabled) {
                ShipBoostedLaser(
                    id = uuidUtils.getUuid(),
                    xOffset = ship.xOffset + ship.width / 2 - SHIP_BOOSTED_LASER_WIDTH / 2 + dx,
                    yOffset = ship.yOffset - 25f + dy,
                    yRange = screenHeight,
                    bulletType = BulletType.AIRBURST,
                )
            } else {
                ShipLaser(
                    id = uuidUtils.getUuid(),
                    xOffset = ship.xOffset + ship.width / 2 - 12f / 2 + dx,
                    yOffset = ship.yOffset - 20f + dy,
                    yRange = screenHeight,
                    width = 12f,
                    bulletType = BulletType.AIRBURST,
                )
            }
            // Wave 16 — Vé Số: random damage mỗi viên (0.3×–3× base 25).
            BulletType.LOTTERY -> ShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 13f / 2 + dx,
                yOffset = ship.yOffset - 20f + dy,
                yRange = screenHeight,
                width = 13f,                             // Wave 17 — size riêng (vé số bản to)
                bulletType = BulletType.LOTTERY,
            ).also { it.impactPower = 25f * (0.3f + kotlin.random.Random.nextFloat() * 2.7f) }
            // Wave 16 — Pháo Hoa: nổ chùm AoE (reuse Plasma body + FIREWORK.aoeRadius=130).
            BulletType.FIREWORK -> PlasmaShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 24f / 2 + dx,
                yOffset = ship.yOffset - 34f + dy,
                yRange = screenHeight,
                width = 24f,                             // Wave 17h
                bulletType = BulletType.FIREWORK,
            )
            // Wave 16 — Cục Gạch: to + nặng (damage ×2.2 via BulletType), thân rộng.
            BulletType.BRICK -> ShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 14f / 2 + dx,
                yOffset = ship.yOffset - 24f + dy,
                yRange = screenHeight,
                width = 14f,
                bulletType = BulletType.BRICK,
            )
            // Wave 16 batch 2 — Bánh Mì: xuyên (reuse Piercing body).
            BulletType.BANH_MI -> PiercingShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 15f / 2 + dx,
                yOffset = ship.yOffset - 22f + dy,
                yRange = screenHeight,
                width = 15f,                             // Wave 17 — size riêng (ổ bánh mì bản to)
                bulletType = BulletType.BANH_MI,
            )
            // Wave 16 batch 2 — Sầu Riêng: nổ mùi AoE 110 (reuse Plasma body).
            BulletType.DURIAN -> PlasmaShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 26f / 2 + dx,
                yOffset = ship.yOffset - 34f + dy,
                yRange = screenHeight,
                width = 26f,                             // Wave 17h — gai sầu riêng to
                bulletType = BulletType.DURIAN,
            )
            // Wave 16 batch 2 — Like/Tim: tự đuổi (reuse Missile homing body).
            BulletType.HEART -> MissileLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 12f / 2 + dx,
                yOffset = ship.yOffset - 24f + dy,
                yRange = screenHeight,
                width = 12f,                             // Wave 17 — size riêng (tim bự hơn tên lửa HOMING w8)
                bulletType = BulletType.HEART,
            )
            // Wave 18 — Trà Sữa: nổ AoE "trân châu" (reuse Plasma body + AoE 100)
            // rồi văng 3 đạn con (xử lý ở collision arm BUBBLE_TEA).
            BulletType.BUBBLE_TEA -> PlasmaShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 20f / 2 + dx,
                yOffset = ship.yOffset - 30f + dy,
                yRange = screenHeight,
                width = 20f,
                bulletType = BulletType.BUBBLE_TEA,
            )
            // Wave 18 — Nước Mắm: đạn thẳng, gây CORROSION (DoT) ở onLaserHit.
            BulletType.FISH_SAUCE -> ShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 10f / 2 + dx,
                yOffset = ship.yOffset - 20f + dy,
                yRange = screenHeight,
                width = 10f,
                bulletType = BulletType.FISH_SAUCE,
            )
            // Wave 18 — Dép Lào: boomerang bay lên rồi quay về (subclass riêng).
            BulletType.SANDAL -> BoomerangShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 16f / 2 + dx,
                yOffset = ship.yOffset - 20f + dy,
                yRange = screenHeight,
                width = 16f,
            )
            // Wave 18 — Mã QR: đạn thẳng, gây SLOW + STUN ("đơ máy") ở onLaserHit.
            BulletType.QR_CODE -> ShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 17f / 2 + dx,
                yOffset = ship.yOffset - 20f + dy,
                yRange = screenHeight,
                width = 17f,
                bulletType = BulletType.QR_CODE,
            )
            // Task 02 — Sét Chain: thân ShipLaser thường, cơ chế chain xử lý ở
            // collision arm + onLaserHit (GameState). width ép về bw (8f) ở .also.
            BulletType.LIGHTNING -> ShipLaser(
                id = uuidUtils.getUuid(),
                xOffset = ship.xOffset + ship.width / 2 - 8f / 2 + dx,
                yOffset = ship.yOffset - 20f + dy,
                yRange = screenHeight,
                width = 8f,
                bulletType = BulletType.LIGHTNING,
            )
            BulletType.NORMAL -> if (ship.laserBoosterEnabled) {
                ShipBoostedLaser(
                    id = uuidUtils.getUuid(),
                    xOffset = ship.xOffset + ship.width / 2 - SHIP_BOOSTED_LASER_WIDTH / 2 + dx,
                    yOffset = ship.yOffset - 25f + dy,
                    yRange = screenHeight,
                )
            } else {
                ShipLaser(
                    id = uuidUtils.getUuid(),
                    xOffset = ship.xOffset + ship.width / 2 - SHIP_LASER_WIDTH / 2 + dx,
                    yOffset = ship.yOffset - 20f + dy,
                    yRange = screenHeight,
                )
            }
        }).also {
            it.width = bw
            it.xOffset = ship.xOffset + ship.width / 2f - bw / 2f + dx
        }
        // Task 24 — PIERCE_CHANCE skill node: chỉ roll khi laser chưa có pierce
        // sẵn từ chính bulletType (PIERCING/KAMEHAMEHA/BANH_MI/GIANT đã set > 0
        // ở trên) — tránh chồng pierce lên các đạn vốn đã xuyên.
        if (laser.pierceRemaining <= 0 && pierceChanceRank() > 0 &&
            kotlin.random.Random.nextFloat() < pierceChanceRank() * EffectiveStats.META_PIERCE_CHANCE_PER_RANK
        ) {
            laser.pierceRemaining = 1
        }
        return laser
    }

    val processShipLasersId = uuidUtils.getUuid()
    val processShipLasersRepeatTime = Millis(5)
    /**
     * Round 40 (29x) — [enemies] threaded in so [MissileLaser] instances can
     * update their `targetX` each tick before moveLaser. Non-missile lasers
     * ignore it. Pass `emptyList()` if no enemies are alive — missiles will
     * fly straight up.
     */
    fun processShipLasers(enemies: List<Enemy> = emptyList()) {
        // Homing update: nearest enemy x for any MissileLaser. O(L × E) but
        // both lists are tiny (<20 each) so cheap at 5ms tick.
        if (shipLasers.any { it is MissileLaser } && enemies.isNotEmpty()) {
            shipLasers.forEach { laser ->
                if (laser is MissileLaser) {
                    val nearest = enemies.minByOrNull {
                        val dx = (it.xOffset + it.width / 2f) - laser.xOffset
                        val dy = it.yOffset - laser.yOffset
                        dx * dx + dy * dy
                    }
                    laser.targetX = nearest?.let { it.xOffset + it.width / 2f }
                }
            }
        } else {
            shipLasers.forEach { (it as? MissileLaser)?.targetX = null }
        }
        shipLasers.forEach {
            it.moveLaser()
            // Cleanup once laser scrolls fully off the top of the screen.
            // (Coord system is now TopStart; laser leaves top when yOffset < -height.)
            // Wave 16 fix — ở camera zoom XA (pixelScale<1), đỉnh device hiển thị
            // tới game-y ≈ -extraYSpan, nên cull ở -100f làm đạn BIẾN MẤT giữa
            // vùng nhìn thay vì bay hẳn ra ngoài (bug user báo). Trừ extraYSpan để
            // đạn chỉ bị xoá khi đã thực sự khuất mép trên ở mọi mức zoom.
            if (it.yOffset < -100f - extraYSpan || it.destroyed) destroyShipLaser(it)
        }
        updateShipLasersUI()
    }

    /**
     * Round 40 (29x) — fire one homing missile from the ship's nose.
     * targetX seeds the initial homing target so the missile doesn't lurch
     * sideways on its first tick.
     */
    fun fireMissile(ship: Ship, initialTargetX: Float?) {
        val missile = MissileLaser(
            id = uuidUtils.getUuid(),
            xOffset = ship.xOffset + ship.width / 2f - 4f,           // 4 = half of width 8
            yOffset = ship.yOffset - 28f,                            // height = 28
            yRange = screenHeight,
        ).also { it.targetX = initialTargetX }
        Logger.d("LasersController.fireMissile: spawned at (${missile.xOffset.toInt()},${missile.yOffset.toInt()}) target=$initialTargetX")
        shipLasers = shipLasers + missile
        updateShipLasersUI()
    }

    /**
     * Task 01 (Slice 3) — bơm đạn drone vào HỆ LASER CHUNG. Dùng list `shipLasers`
     * để tái dùng move + cleanup ([processShipLasers]) + collision
     * ([monitorLaserCollision]). Tôn trọng [MAX_SHIP_LASERS] để không phình list
     * khi drone + tàu bắn dồn dập.
     */
    fun addDroneLasers(lasers: List<Laser>) {
        if (lasers.isEmpty() || shipLasers.size >= MAX_SHIP_LASERS) return
        shipLasers = shipLasers + lasers
        updateShipLasersUI()
    }

    fun hasShipLasers() = shipLasers.isNotEmpty()

    private fun destroyShipLaser(laser: Laser) {
        shipLasers = shipLasers - laser
        updateShipLasersUI()
    }

    fun fireUltimateLaser() {
        // Wave 11d Bug #1 fix — sweep the extended camera-FAR x range, not just
        // [0, screenWidth]. Beams now span [-extraXSpan, screenWidth + extraXSpan]
        // so enemies that spawned into the extended right-edge margin (or moved
        // there via formation drift) are caught by the sweep. With extraXSpan=0
        // (MEDIUM/NEAR zoom) the math reduces to the prior raw-screen behavior.
        val totalSpan = screenWidth + extraXSpan * 2f
        val horizontalLaserDistance = totalSpan / ULTIMATE_LASERS_COUNT
        val startX = -extraXSpan
        Logger.d("LasersController.fireUltimateLaser: spawning $ULTIMATE_LASERS_COUNT vertical beams (sweep bottom→top, existing=${ultimateLasers.size}, span=$totalSpan startX=$startX extra=$extraXSpan)")
        val ultimateLaserList = mutableListOf<UltimateLaser>()
        // Pixel-3 #2 deep-audit fix — beam start now `screenHeight + extraYSpan`
        // so the bottom band visible at FAR zoom (device-y 757..891) also gets
        // swept by the beam. Pre-fix beam started at raw screenHeight, leaving
        // 134dp of visible device-bottom uncovered.
        val startY = screenHeight + extraYSpan
        for (i in 0..ULTIMATE_LASERS_COUNT) {
            val ultimateLaser = UltimateLaser(
                id = uuidUtils.getUuid(),
                xOffset = startX + horizontalLaserDistance * i,
                yOffset = startY,
                yRange = screenHeight,
            )
            ultimateLaserList.add(ultimateLaser)
        }
        // Append instead of replace — was `ultimateLasers = ultimateLaserList`, which
        // wiped the previous in-flight batch when a new fire (ChargeShot auto + booster
        // pickup) triggered within ~10s of each other. Caused beams to "disappear at
        // halfway height" visually. Now both batches coexist until they fly off-screen.
        // Round 80 (#2 fix from user log) — cap ultimate laser pool. Trước fix:
        // chuỗi pickup ULTIMATE_WEAPON_BOOSTER liên tiếp → existing=10 + 9 new = 19
        // beams in flight → 19×N enemies collision checks/frame → FPS rớt xuống 31.
        // Cap pool: nếu len mới vượt MAX_ULTIMATE_POOL, drop oldest (front of list).
        val combined = ultimateLasers + ultimateLaserList
        ultimateLasers = if (combined.size > MAX_ULTIMATE_POOL) {
            combined.takeLast(MAX_ULTIMATE_POOL)
        } else {
            combined
        }
        updateUltimateLasers()
    }

    val processLasersId = uuidUtils.getUuid()
    val processLasersRepeatTime = Millis(40)
    fun processLasers() {
        ultimateLasers.forEach {
            it.moveLaser()
            if (it.yOffset < -screenHeight || it.destroyed) destroyUltimateShipLaser(it)
        }
        updateUltimateLasers()
    }

    fun hasUltimateLasers() = ultimateLasers.isNotEmpty()

    private fun destroyUltimateShipLaser(laser: Laser) {
        ultimateLasers = ultimateLasers - laser
        updateUltimateLasers()
    }

    val monitorLaserCollisionId = uuidUtils.getUuid()
    val monitorLaserCollisionRepeatTime = Millis(1)
    fun monitorLaserCollision(
        spaceObjects: List<SpaceObject>,
        enemies: List<Enemy>,
        // Wave 18 — clock cho Boomerang re-hit cooldown. Default = wall-clock
        // (game loop); test bơm thời gian tường minh để xác định.
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val lasers = shipLasers + ultimateLasers
        val spaceObjectRectList = spaceObjects.map { it.spaceObjectRect() }
        val enemyRectList = enemies.map { it.enemyRect() }
        lasers.forEach { laser ->

            // Round 13 regression FIX: was `y = yOffset + screenHeight - height`
            // — that was an OLD BottomStart→TopStart convert. After round 13 made
            // lasers natively TopStart, this convert pushed rect off-screen → no
            // collisions for ship laser. Use raw yOffset directly now.
            val laserRect = Rect(
                offset = Offset(x = laser.xOffset, y = laser.yOffset),
                size = Size(width = laser.width, height = laser.height)
            )

            // 25x/48x — apply damage multiplier (modifier + meta) per hit.
            val dmgMul = damageMultiplier()
            val effectiveDamage = laser.impactPower * dmgMul
            if (spaceObjectRectList.any { it.overlaps(laserRect) }) {
                val index = spaceObjectRectList.indexOfFirst { it.overlaps(laserRect) }
                val target = spaceObjects[index]
                val hitX = target.xOffset + target.size / 2f
                val hitY = target.yOffset + target.size / 2f
                target.onObjectImpact(effectiveDamage)
                // Trigger same impact feedback as enemy hits — sparks + mini explosion +
                // damage number + hit-stop freeze. Rocks are non-boss so isBoss=false.
                onLaserHit(target.id, effectiveDamage.toInt(), hitX, hitY, false, laser.bulletType)
                destroyShipLaser(laser)
                updateShipLasersUI()
            }
            if (enemyRectList.any { it.overlaps(laserRect) }) {
                val index = enemyRectList.indexOfFirst { it.overlaps(laserRect) }
                val target = enemies[index]
                // Wave 17m — ĐIỂM VA CHẠM thật = vị trí đạn (tâm-x + mép trên đang
                // bay lên), KHÔNG phải tâm/đỉnh boss. Trước đây nổ ở đỉnh boss nên
                // lệch khỏi nơi đạn thực sự trúng (user báo). Dùng cho explosion.
                val laserHitX = laser.xOffset + laser.width / 2f
                val laserHitY = laser.yOffset
                // Wave 14b — ChargeShot ultimate must NOT one-shot bosses.
                // The beam persists (never destroyed on hit) and re-overlaps
                // every Millis(1) tick, so at impactPower=1000 a boss in its
                // path evaporated instantly (user bug report). A boss instead
                // takes a small flat chip ONCE per beam; mobs are unchanged.
                if (laser is UltimateLaser && target.isBoss) {
                    if (laser.hitBossIds.add(target.enemyId)) {
                        // Cap at min(flat × dmgMul, 8% of spawn HP) so even a
                        // maxed-out damage run can't restore the one-shot.
                        val bossDmg = UltimateLaser.bossChipDamage(dmgMul, target.initialHp)
                        target.onObjectImpact(bossDmg)
                        onLaserHit(
                            target.enemyId,
                            bossDmg.toInt(),
                            laserHitX,
                            laserHitY,
                            true,
                            laser.bulletType,
                        )
                    }
                    return@forEach
                }
                // Wave 18 — Boomerang (Dép Lào): bỏ qua nếu địch này còn trong
                // cooldown re-hit. Nếu không, đạn cắm cả 4 hit vào 1 địch trong
                // vài ms (collision chạy Millis(1)) → chết tại chỗ, không kịp lên
                // đỉnh & quay về. Cooldown để mỗi địch chỉ ăn 1 hit/lượt → dép
                // thật sự "đánh 2 chiều".
                if (laser is BoomerangShipLaser && !laser.canHit(target.enemyId, nowMillis)) {
                    return@forEach
                }
                target.onObjectImpact(effectiveDamage)
                onLaserHit(
                    target.enemyId,
                    effectiveDamage.toInt(),
                    laserHitX,
                    laserHitY,
                    target.isBoss,
                    laser.bulletType,
                )
                // Round 35 (35x) — PIERCING / PLASMA collision behavior.
                when (laser.bulletType) {
                    // Wave 14 — KAMEHAMEHA pierces like PIERCING (pierceRemaining=99).
                    // Wave 17 — GIANT cũng xuyên (pierceRemaining=4 = "cày" qua đội hình).
                    BulletType.PIERCING, BulletType.KAMEHAMEHA, BulletType.BANH_MI, BulletType.GIANT -> {
                        // Decrement pierce; destroy only when exhausted.
                        // Round 37 — removed per-hit Logger.d (fired inside Millis(1) tick;
                        // during a PIERCING run through enemy formations this spammed dozens
                        // of lines per second). onLaserHit upstream already records the hit.
                        laser.pierceRemaining = laser.pierceRemaining - 1
                        if (laser.pierceRemaining <= 0) {
                            destroyShipLaser(laser)
                        }
                    }
                    // Wave 14 — ATOMIC reuses PLASMA AoE; radius from ATOMIC.aoeRadius=150.
                    // Wave 16 (Slice 3) — SMOKE shares the splash arm (its own
                    // aoeRadius=60 → a small puff; not a PlasmaShipLaser so the
                    // rarity multiplier defaults to 1.0).
                    // Wave 17 — splash THUẦN (radius khác nhau + hiệu ứng phụ riêng
                    // qua onLaserHit ở GameState: ATOMIC→BURN phóng xạ, DURIAN→SLOW).
                    // FIREWORK tách ra arm riêng bên dưới (splash + bắn đạn con).
                    BulletType.PLASMA, BulletType.ATOMIC, BulletType.SMOKE, BulletType.DURIAN -> {
                        applyAoeSplash(laser, index, enemies, effectiveDamage)
                        destroyShipLaser(laser)
                    }
                    // Wave 17 — Pháo Hoa: nổ splash 130 + BẮN RA chùm 5 đạn con toả
                    // rộng (vừa sát thương vùng vừa tái-bắn). Khác PLASMA (chỉ splash)
                    // và SPLIT (chỉ đẻ con, không splash).
                    BulletType.FIREWORK -> {
                        applyAoeSplash(laser, index, enemies, effectiveDamage)
                        val children = listOf(-32f, -16f, 0f, 16f, 32f).map { ox ->
                            ShipLaser(
                                id = uuidUtils.getUuid(),
                                xOffset = laser.xOffset + ox,
                                yOffset = laser.yOffset,
                                yRange = screenHeight,
                                bulletType = BulletType.NORMAL,         // con NORMAL → không đệ quy
                            )
                        }
                        shipLasers = shipLasers + children
                        destroyShipLaser(laser)
                    }
                    // Task 15 — Nổ chùm (Airburst): trúng địch → nổ 8 đạn con NORMAL
                    // toả quạt hướng lên (vận tốc ngang+dọc), khác SPLIT/FIREWORK bắn thẳng.
                    BulletType.AIRBURST -> {
                        val shards = com.tranphuloi.neon.ui.game.ship.laser.Airburst.velocities()
                            .map { (vx, vy) ->
                                ShipLaser(
                                    id = uuidUtils.getUuid(),
                                    xOffset = laser.xOffset,
                                    yOffset = laser.yOffset,
                                    yRange = screenHeight,
                                    bulletType = BulletType.NORMAL,     // con NORMAL → không đệ quy
                                    xOffsetMovementSpeed = vx,
                                    yOffsetMovementSpeed = vy,
                                )
                            }
                        shipLasers = shipLasers + shards
                        destroyShipLaser(laser)
                    }
                    // Wave 18 — Trà Sữa: nổ AoE 100 + văng 3 "trân châu" (đạn con
                    // NORMAL). Như Pháo Hoa nhưng nhẹ hơn (3 con thay 5).
                    BulletType.BUBBLE_TEA -> {
                        applyAoeSplash(laser, index, enemies, effectiveDamage)
                        val pearls = listOf(-18f, 0f, 18f).map { ox ->
                            ShipLaser(
                                id = uuidUtils.getUuid(),
                                xOffset = laser.xOffset + ox,
                                yOffset = laser.yOffset,
                                yRange = screenHeight,
                                bulletType = BulletType.NORMAL,         // con NORMAL → không đệ quy
                            )
                        }
                        shipLasers = shipLasers + pearls
                        destroyShipLaser(laser)
                    }
                    // Wave 18 — Dép Lào: boomerang đánh 2 chiều, trừ hitsRemaining
                    // mỗi lần trúng (giống BOUNCE), không huỷ tới khi hết lượt.
                    BulletType.SANDAL -> {
                        val boomerang = laser as? BoomerangShipLaser
                        if (boomerang != null) {
                            boomerang.registerHit(target.enemyId, nowMillis)
                            boomerang.hitsRemaining = boomerang.hitsRemaining - 1
                            if (boomerang.hitsRemaining <= 0) destroyShipLaser(laser)
                        } else {
                            destroyShipLaser(laser)
                        }
                    }
                    // Task 24 — PIERCE_CHANCE có thể set pierceRemaining=1 lên đạn
                    // NORMAL tại buildOneLaser(); decrement/destroy giống nhánh PIERCING.
                    BulletType.NORMAL -> {
                        if (laser.pierceRemaining > 0) {
                            laser.pierceRemaining = laser.pierceRemaining - 1
                            if (laser.pierceRemaining <= 0) destroyShipLaser(laser)
                        } else {
                            destroyShipLaser(laser)
                        }
                    }
                    // Task 02 — Sét Chain: huỷ khi trúng 1 địch; chuỗi lan xử lý
                    // upstream ở onLaserHit (GameState) khi bulletType==LIGHTNING.
                    BulletType.LIGHTNING -> destroyShipLaser(laser)
                    // Round 67 — FIRE: destroy on hit, BURN status applied in
                    // onLaserHit upstream (GameState).
                    BulletType.FIRE -> destroyShipLaser(laser)
                    // Round 67 — HOMING: MissileLaser is destroyed normally
                    // on hit. Tracking happens in processShipLasers update.
                    BulletType.HOMING, BulletType.HEART -> destroyShipLaser(laser)
                    // Round 67 — BOUNCE: don't destroy on hit (keep bouncing
                    // until bounceRemaining=0 or off-screen). Decrement
                    // pierce-style counter on the BounceShipLaser instead.
                    BulletType.BOUNCE -> {
                        val bounce = laser as? BounceShipLaser
                        if (bounce != null) {
                            bounce.hitsRemaining = bounce.hitsRemaining - 1
                            if (bounce.hitsRemaining <= 0) destroyShipLaser(laser)
                        } else {
                            destroyShipLaser(laser)
                        }
                    }
                    // Round 68 stub — destroy on hit. Behaviors thật Round 69+.
                    // Wave 16 (Slice 3) — ZIGZAG: weaving normal shot, single hit.
                    // Wave 16 — LOTTERY (random dmg) + BRICK (heavy) also single-hit.
                    // Wave 18 — Nước Mắm + Mã QR: đạn thẳng 1 hit; hiệu ứng
                    // (CORROSION / SLOW+STUN) áp ở onLaserHit (GameState).
                    BulletType.ZIGZAG, BulletType.LOTTERY, BulletType.BRICK,
                    BulletType.FISH_SAUCE, BulletType.QR_CODE -> destroyShipLaser(laser)
                    // Wave 16 (Slice 3) — SPLIT: burst into 3 NORMAL children that
                    // keep flying up in a small spread, then destroy the parent.
                    // Children are NORMAL so they can't split again (no recursion).
                    BulletType.SPLIT -> {
                        val children = listOf(-16f, 0f, 16f).map { ox ->
                            ShipLaser(
                                id = uuidUtils.getUuid(),
                                xOffset = laser.xOffset + ox,
                                yOffset = laser.yOffset,
                                yRange = screenHeight,
                                bulletType = BulletType.NORMAL,
                            )
                        }
                        shipLasers = shipLasers + children
                        destroyShipLaser(laser)
                    }
                }
                updateShipLasersUI()
            }
        }
    }

    /**
     * Wave 17 — AoE splash dùng chung cho các đạn nổ-vùng (PLASMA/ATOMIC/SMOKE/
     * DURIAN/FIREWORK). Địch trong bán kính ăn 50% sát thương. Radius lấy từ
     * [BulletType.aoeRadius] × rarity multiplier (PlasmaShipLaser). Tách helper
     * để FIREWORK tái dùng phần splash rồi cộng thêm cơ chế đẻ đạn con.
     */
    private fun applyAoeSplash(laser: Laser, hitIndex: Int, enemies: List<Enemy>, effectiveDamage: Float) {
        val rarityMul = (laser as? PlasmaShipLaser)?.aoeRadiusMultiplier ?: 1f
        val aoeRadius = laser.bulletType.aoeRadius * rarityMul
        val target = enemies[hitIndex]
        val hitCenterX = target.xOffset + target.width / 2f
        val hitCenterY = target.yOffset + target.height / 2f
        val aoeDmg = effectiveDamage * 0.5f
        enemies.forEachIndexed { i, other ->
            if (i == hitIndex) return@forEachIndexed
            val dx = (other.xOffset + other.width / 2f) - hitCenterX
            val dy = (other.yOffset + other.height / 2f) - hitCenterY
            if (dx * dx + dy * dy <= aoeRadius * aoeRadius) {
                other.onObjectImpact(aoeDmg)
                onLaserHit(
                    other.enemyId,
                    aoeDmg.toInt(),
                    other.xOffset + other.width / 2f,
                    other.yOffset,
                    other.isBoss,
                    laser.bulletType,
                )
            }
        }
    }

    private fun updateShipLasersUI() {
        setShipLasers(shipLasers)
    }

    private fun updateUltimateLasers() {
        setUltimateLasers(ultimateLasers)
    }

    companion object {
        const val ULTIMATE_LASERS_COUNT = 9
        /** Round 47 — max in-flight ship lasers. New shots beyond this are dropped. */
        const val MAX_SHIP_LASERS = 25
        /**
         * Round 80 (#2 perf from user log) — cap ultimate laser pool. Per-frame
         * cost ~O(N×enemies); pickup chains rapidly stacked 19-27 beams → FPS
         * drops to 31 under combat pressure. Cap at 18 = 2 stacked batches.
         * New batches push out oldest beams (FIFO).
         */
        const val MAX_ULTIMATE_POOL = 18
    }
}
