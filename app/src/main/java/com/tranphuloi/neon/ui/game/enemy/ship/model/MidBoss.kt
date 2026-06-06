package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.Keep
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.enemy.laser.EnemyLaser
import com.tranphuloi.neon.ui.game.enemy.laser.LaserMotion
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * 33c+d Mid-boss with phase transition at HP < 50%.
 * - Phase 1 (HP >= 50%): standard attack pattern per variant
 * - Phase 2 (HP < 50%): boosted attack — variant-specific behavior
 *
 * Variants (see MidBossType):
 *   OFFENSIVE — moderate HP, sine-wave horizontal patrol, fast aimed lasers; Phase 2 = triple spread
 *   DEFENSIVE — high HP, slow horizontal patrol, single laser; Phase 2 = laser barrage (5-wave)
 *   SWARM     — low HP, fast erratic movement, frequent lasers; Phase 2 = doubled fire rate
 */
@Keep
data class MidBoss(
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val variant: MidBossType,
    private val getShip: () -> Ship,
    /** Round 79 (#1) — chapter-aware bossKind override. */
    private val bossKindOverride: BossKind? = null,
) : Enemy {

    override val enemyId: String = UUID.randomUUID().toString()
    // Wave 16 — SIZE biến thiên theo baseHp (HP cao = boss to hơn) thay vì
    // 130×90 cố định cho mọi variant. baseHp 1200→×0.8, 3300→×1.3 (kẹp).
    // Tương quan size↔độ trâu → boss khác nhau rõ về kích cỡ.
    // Wave 17h — nới biên tương phản size (0.8–1.3 → 0.62–1.65) để boss máu thấp
    // NHỎ rõ (~80px) vs trùm máu cao TO rõ (~215px) → dễ phân biệt kích cỡ.
    // Wave 17j — đọc từ MODEL (MidBossType.sizeScale) thay vì tính tại chỗ.
    private val sizeScale: Float = variant.sizeScale
    override val width: Float = 130f * sizeScale
    override val height: Float = 90f * sizeScale
    override var hp: Float = variant.baseHp
    override val initialHp: Float = hp
    override val impactPower: Float = 10f
    override val minerals: Int = 5
    override var destroyed: Boolean = false
        private set
    override var outOfScreen: Boolean = false
        private set
    override val drawableId: Int = variant.drawableId
    override var lastImpactMillis: Long = 0L
    override val isBoss: Boolean = true               // reuses Boss HP bar + rank overlay
    // Round 71 (Issue 4d) — MidBoss variants map to 2 unique kinds (ORB, FRACTAL).
    // Round 79 (#1) — bossKindOverride (set theo chapter trong EnemyFactory) cho
    // phép Ch4Mid OFFENSIVE reuse render HELL_LORD, Ch3Mid SWARM render
    // HAUNTED_KID — eliminate visual duplicate giữa các chapter.
    // Round 82 — data-driven via variant.defaultBossKind. 12 new variants tự
    // mang BossKind riêng → KHÔNG cần hardcode dispatch. bossKindOverride vẫn
    // ưu tiên cao nhất cho chapter-aware reuse (Ch4 OFFENSIVE→HELL_LORD, etc.).
    override val bossKind: BossKind = bossKindOverride ?: variant.defaultBossKind
    // Round 84 audit — read displayName from resolved bossKind (Vietnamese name).
    // Khi factory override (Ch4 OFFENSIVE → HELL_LORD), banner hiển "Chúa Tể
    // Địa Ngục" thay generic "TIỂU BOSS TẤN CÔNG". Khớp visual với InfoScreen.
    override val displayName: String = bossKind.displayName

    override var xOffset: Float = (screenWidth - width) / 2f
    override var yOffset: Float = -height
    private val entryTargetY: Float = 80f
    private val entrySpeed: Float = 2.5f
    override val isInEntryPhase: Boolean get() = yOffset < entryTargetY

    // Wave 17 — knockback dạng OFFSET bền (px, âm = giật lùi lên), cộng sau
    // movement-base mỗi frame rồi suy giảm về 0 → recoil đồng đều mọi pattern.
    private var knockbackY: Float = 0f
    private var movementTime: Float = 0f              // accumulates per process() call
    // Wave 16 — increments each generateLasers() call; drives rotating /
    // alternating signature patterns (e.g. SKULL spinning bone fan).
    private var fireTick: Int = 0

    // ── Wave 16 Wave B — MARQUEE mechanics (vài boss đỉnh) ──
    // Wave 17j — đọc từ MODEL (variant.specialAbility) thay vì liệt kê tại chỗ.
    /** SHIELD: bất tử 1 cửa sổ khi vào phase 2 (Bạch Long, Bao Cao Su). */
    private val hasShield: Boolean = variant.specialAbility == BossAbility.SHIELD
    /** TELEPORT: nhảy chỗ định kỳ + giữ vị trí (Venom, Tham Nhũng, Hell Lord/OFFENSIVE). */
    private val hasTeleport: Boolean = variant.specialAbility == BossAbility.TELEPORT
    private var shieldedUntilMillis: Long = 0L
    private var lastTeleportMillis: Long = 0L
    private var teleportHoldUntil: Long = 0L
    private var teleportCount: Int = 0

    /** External (render/HP-bar) — true khi đang bất tử nhờ shield. */
    fun isShielded(now: Long = System.currentTimeMillis()): Boolean = now < shieldedUntilMillis

    /** True once the phase transition flash has been triggered (one-shot gate). */
    private var phase2Engaged: Boolean = false

    /** External readers (banner): expose phase index for cinematic feedback. */
    val phase: Int get() = if (hp < initialHp * 0.5f) 2 else 1

    override fun enemyRect(): Rect = Rect(
        center = Offset(x = xOffset + width / 2, y = yOffset + height / 2),
        radius = width / 2,
    )

    override fun process() {
        if (yOffset < entryTargetY) {
            yOffset = (yOffset + entrySpeed).coerceAtMost(entryTargetY)
            if (hp <= 0) destroyed = true
            return
        }

        // Movement pattern by variant. Round 82 — 12 new variants tái sử dụng
        // movement pattern của variant gốc gần nhất theo behavior:
        // - Aggressive (HEN/BUFFALO/TIGER/TROLL/DIVA/SATAN/SICKLE/TYCOON) → sine
        // - Defensive (RAT/DRAGON/MONEY/SUMMITS/GLOBES) → slow patrol
        movementTime += 1f
        // Wave 17 — 7 movement pattern (trước chỉ 3) gán theo TÍNH CÁCH boss để
        // mỗi con di chuyển khác nhau rõ rệt, không còn 7 boss đi sine y hệt:
        //   0 sine ngang · 1 patrol quét · 2 figure-8 · 3 lao-bổ (dive) ·
        //   4 lướt-giật (strafe) · 5 vòng-lượn (loop) · 6 trôi-thấp ì ạch.
        val patternForVariant: Int = when (variant) {
            MidBossType.OFFENSIVE -> 0           // sine
            MidBossType.WHITE_DRAGON -> 0        // sine uốn lượn (rồng)
            MidBossType.HAMMER_SICKLE -> 0       // sine
            MidBossType.DEFENSIVE -> 1           // patrol thủ
            MidBossType.TROLL_TOWER -> 1         // patrol (tháp ì)
            MidBossType.COSMIC_CENTIPEDE -> 1    // patrol (rết bò dài)
            MidBossType.SWARM -> 2               // figure-8
            MidBossType.VAMPIRE -> 2             // figure-8 (dơi lượn)
            MidBossType.VENOM_SPIDER -> 2        // figure-8 (nhện giật)
            MidBossType.BUFFALO_RAGE -> 3        // lao-bổ (trâu húc)
            MidBossType.FIERCE_TIGER -> 3        // lao-bổ (cọp vồ)
            MidBossType.SKULL_CROSSBONES -> 3    // lao-bổ (cướp biển xông)
            MidBossType.SEXY_DIVA -> 4           // lướt-giật (nhảy múa)
            MidBossType.GOLDEN_TYCOON -> 4       // lướt-giật
            MidBossType.DUMB_RAT -> 4            // lướt-giật (chuột chạy lắt nhắt)
            MidBossType.HEN_MOTHER -> 5          // vòng-lượn (gà xòe)
            MidBossType.TWIN_SUMMITS -> 5        // vòng-lượn
            MidBossType.VOID_GLOBES -> 5         // vòng-lượn (cầu quay)
            MidBossType.MONEY_TYCOON -> 6        // trôi-thấp
            MidBossType.GIANT_CONDOM -> 6        // trôi-thấp ì ạch
            MidBossType.CORRUPTION -> 6          // trôi-thấp (béo ục ịch)
            // Wave 18 batch 1
            MidBossType.TRAFFIC_JAM -> 6         // trôi-thấp (xe kẹt ì ạch)
            MidBossType.KPI_BOSS -> 1            // patrol (sếp đi qua lại soi)
            MidBossType.TIKTOKER -> 4            // lướt-giật (nhảy nhót quay clip)
            // Wave 19 batch 2
            MidBossType.ATM_BANKRUPT -> 1        // patrol (máy ATM đứng)
            MidBossType.NOKIA_BRICK -> 6         // trôi-thấp (cục gạch nặng)
            MidBossType.INFLATION_STORM -> 4     // lướt-giật (giá nhảy loạn)
            // Wave 20 batch 3
            MidBossType.SOCIAL_DRAMA -> 2        // figure-8 (drama lượn loạn)
            MidBossType.PYRAMID_SCHEME -> 1      // patrol (trùm đứng chỉ tay)
            MidBossType.FORTUNE_TELLER -> 5      // vòng-lượn (huyền bí)
            MidBossType.KITCHEN_GOD -> 0         // sine (cưỡi cá bơi)
            // Wave 21 batch 4
            MidBossType.CRYPTO_BRO -> 4          // lướt-giật (giá biến động)
            MidBossType.TOXIC_KID -> 2           // figure-8 (trẻ trâu lăng xăng)
            // Wave 22 batch 5
            MidBossType.KARAOKE_BOSS -> 5        // vòng-lượn (lắc lư hát)
            MidBossType.FLASHY_TYCOON -> 4       // lướt-giật (phô diễn)
            // Wave 23 batch 6
            MidBossType.DR_GOOGLE -> 1           // patrol (ngồi tra cứu)
            MidBossType.CAT_EMPEROR -> 5         // vòng-lượn (mèo dạo bệ rồng)
            // Wave 24 batch 7
            MidBossType.SALE_FANATIC -> 3        // lao-bổ (đổ xô mua)
            MidBossType.GHOST_MONTH -> 2         // figure-8 (hồn vật vờ)
        }
        // Marquee TELEPORT — nhảy sang một bên (luân phiên) + GIỮ vị trí một
        // lúc (chặn movement trong cửa sổ hold) để cú nhảy "ăn" được.
        val now = System.currentTimeMillis()
        if (hasTeleport && now - lastTeleportMillis > TELEPORT_INTERVAL_MS) {
            lastTeleportMillis = now
            teleportHoldUntil = now + TELEPORT_HOLD_MS
            val side = if (teleportCount % 2 == 0) 0.18f else 0.82f
            teleportCount++
            xOffset = (screenWidth * side - width / 2f).coerceIn(0f, screenWidth - width)
        }
        // Chỉ chạy movement thường khi KHÔNG đang giữ vị trí teleport.
        if (now >= teleportHoldUntil) {
            when (patternForVariant) {
                0 -> {
                    // Sine wave horizontal: ±100px around center, 3s cycle.
                    val t = movementTime / 600f
                    val targetX = (screenWidth - width) / 2f + sin(t.toDouble()).toFloat() * 100f
                    xOffset += (targetX - xOffset) * 0.05f
                    yOffset = entryTargetY                         // base y (recoil cộng sau)
                }
                1 -> {
                    // Slow horizontal patrol — left ↔ right.
                    val cycle = (movementTime / 1200f) % 2f
                    val phase = if (cycle < 1f) cycle else 2f - cycle
                    xOffset = phase * (screenWidth - width)
                    yOffset = entryTargetY                         // base y (recoil cộng sau)
                }
                2 -> {
                    // Erratic figure-8 motion.
                    val t = movementTime / 400f
                    xOffset = (screenWidth - width) / 2f + sin(t.toDouble()).toFloat() * 120f
                    yOffset = entryTargetY + cos((t * 2.0)).toFloat() * 40f
                }
                3 -> {
                    // Lao-bổ (dive): lao xuống sâu rồi rút lên, đảo ngang chậm —
                    // hung hãn (trâu/cọp/cướp biển). y: 0→220 mượt theo cos.
                    val t = movementTime / 460f
                    val targetX = (screenWidth - width) / 2f + sin(t.toDouble()).toFloat() * 70f
                    xOffset += (targetX - xOffset) * 0.04f
                    yOffset = entryTargetY + (0.5f - 0.5f * cos((t * 1.6))).toFloat() * 230f
                }
                4 -> {
                    // Lướt-giật (strafe): biên độ ngang lớn, TẦN SỐ cao → giật sang
                    // 2 bên dứt khoát (diva nhảy / chuột chạy). Giữ y trên cao.
                    val t = movementTime / 200f
                    xOffset = (screenWidth - width) / 2f + sin(t.toDouble()).toFloat() * 135f
                    yOffset = entryTargetY
                }
                5 -> {
                    // Vòng-lượn (loop): x sine + y sine khác tần → quỹ đạo bầu dục.
                    val t = movementTime / 500f
                    xOffset = (screenWidth - width) / 2f + sin(t.toDouble()).toFloat() * 110f
                    yOffset = entryTargetY + 70f + sin((t * 2.0)).toFloat() * 60f
                }
                6 -> {
                    // Trôi-thấp ì ạch: ngồi THẤP hơn, trôi ngang rất chậm (nặng nề).
                    val t = movementTime / 900f
                    xOffset = (screenWidth - width) / 2f + sin(t.toDouble()).toFloat() * 70f
                    yOffset = entryTargetY + 120f + sin((t * 0.5)).toFloat() * 30f
                }
            }
            // Wave 17 — knockback (giật lùi khi trúng đòn) là OFFSET BỀN cộng SAU
            // movement-base + tự suy giảm về 0. Trước đây các pattern set thẳng
            // yOffset (erratic/dive/loop/hover) ghi đè knockback → 12 boss không
            // giật lùi. Nay mọi boss đều recoil đồng đều.
            if (knockbackY != 0f) {
                yOffset += knockbackY
                knockbackY *= 0.85f
                if (kotlin.math.abs(knockbackY) < 0.1f) knockbackY = 0f
            }
        }

        // Coerce horizontal bounds.
        xOffset = xOffset.coerceIn(0f, screenWidth - width)

        // Engage phase 2 transition once.
        if (!phase2Engaged && hp < initialHp * 0.5f) {
            phase2Engaged = true
            lastImpactMillis = System.currentTimeMillis()    // re-uses flash trigger for visual cue
            // Marquee SHIELD — vào phase 2 thì bất tử 1 cửa sổ (vảy rồng / bong bóng).
            if (hasShield) shieldedUntilMillis = System.currentTimeMillis() + SHIELD_MS
        }

        if (yOffset + height > screenHeight) outOfScreen = true
        if (hp <= 0) destroyed = true
    }

    override fun generateLasers(): List<Laser> {
        val ship: Ship = getShip()
        val phase2 = phase == 2
        fireTick++

        // Wave 16 — every mid-boss variant now has a SIGNATURE attack pattern
        // (the old shared aimed/spread/barrage trio is gone). Exhaustive over the
        // sealed MidBossType so a new variant MUST declare its skill (else compile
        // error — no silent fallback to a generic pattern).
        return when (variant) {
            MidBossType.SKULL_CROSSBONES -> spinningBoneFan(phase2)
            MidBossType.VAMPIRE -> batSwarmLifesteal(ship, phase2)
            MidBossType.COSMIC_CENTIPEDE -> poisonSprayArc(phase2)
            MidBossType.HEN_MOTHER -> eggLobCluster(phase2)
            MidBossType.BUFFALO_RAGE -> hornCharge(ship, phase2)
            MidBossType.FIERCE_TIGER -> roarWall(phase2)
            MidBossType.WHITE_DRAGON -> fireBreath(phase2)
            MidBossType.HAMMER_SICKLE -> hammerSickle(phase2)
            MidBossType.MONEY_TYCOON -> moneyRain(phase2)
            MidBossType.GOLDEN_TYCOON -> dollarSpiral(phase2)
            MidBossType.SEXY_DIVA -> hairWhip(phase2)
            MidBossType.TROLL_TOWER -> towerBeam(phase2)
            MidBossType.OFFENSIVE -> eyeBeam(ship, phase2)
            MidBossType.DEFENSIVE -> atomOrbit(phase2)
            MidBossType.SWARM -> hauntScatter(phase2)
            MidBossType.DUMB_RAT -> ratNibble(phase2)
            MidBossType.TWIN_SUMMITS -> twinColumns(phase2)
            MidBossType.VOID_GLOBES -> voidOrbs(phase2)
            // Wave 16 batch 2
            MidBossType.GIANT_CONDOM -> inflateBurst(phase2)
            MidBossType.VENOM_SPIDER -> venomWeb(phase2)
            MidBossType.CORRUPTION -> corruptionWall(phase2)
            // Wave 18 batch 1
            MidBossType.TRAFFIC_JAM -> trafficGridlock(phase2)
            MidBossType.KPI_BOSS -> kpiColumns(ship, phase2)
            MidBossType.TIKTOKER -> heartSpam(phase2)
            // Wave 19 batch 2
            MidBossType.ATM_BANKRUPT -> atmCashSpit(phase2)
            MidBossType.NOKIA_BRICK -> brickToss(phase2)
            MidBossType.INFLATION_STORM -> inflationWave(phase2)
            // Wave 20 batch 3
            MidBossType.SOCIAL_DRAMA -> dramaPileOn(phase2)
            MidBossType.PYRAMID_SCHEME -> pyramidScheme(phase2)
            MidBossType.FORTUNE_TELLER -> prophecyFan(ship, phase2)
            MidBossType.KITCHEN_GOD -> carpLeap(phase2)
            // Wave 21 batch 4
            MidBossType.CRYPTO_BRO -> cryptoVolatility(phase2)
            MidBossType.TOXIC_KID -> toxicSpam(phase2)
            // Wave 22 batch 5
            MidBossType.KARAOKE_BOSS -> soundWaves(phase2)
            MidBossType.FLASHY_TYCOON -> flexBurst(phase2)
            // Wave 23 batch 6
            MidBossType.DR_GOOGLE -> misdiagnosisX(phase2)
            MidBossType.CAT_EMPEROR -> royalPaws(phase2)
            // Wave 24 batch 7
            MidBossType.SALE_FANATIC -> flashSale(phase2)
            MidBossType.GHOST_MONTH -> wanderingSpirits(phase2)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Wave 16 — per-boss signature attack patterns (batch 1). Each spawns
    // EnemyLasers with a distinct geometry/behaviour so bosses no longer share
    // the generic aimed/spread/barrage trio.
    // ────────────────────────────────────────────────────────────────────────

    /** Spawn one downward EnemyLaser. [angleDeg] = 0 is straight down; +/- tilts. */
    private fun bossBullet(
        xSpeed: Float,
        ySpeed: Float,
        w: Float = 26f,
        spawnX: Float = xOffset + width / 2f - w / 2f,
        drawable: Int = R.drawable.ic_laser_red_8,
        // Wave 17 — quỹ đạo phi tuyến (HOMING/ACCEL/CURVE) cho skill boss khác biệt.
        motion: com.tranphuloi.neon.ui.game.enemy.laser.LaserMotion =
            com.tranphuloi.neon.ui.game.enemy.laser.LaserMotion.LINEAR,
    ): EnemyLaser = EnemyLaser(
        xOffset = spawnX,
        yOffset = yOffset + height,
        yRange = screenHeight,
        width = w,
        height = w,
        xOffsetMovementSpeed = xSpeed,
        yOffsetMovementSpeed = ySpeed,
        drawableId = drawable,
        motion = motion,
    )

    /** Đầu Lâu Xương Chéo — quạt "xương" xoay trái-phải theo thời gian. */
    private fun spinningBoneFan(phase2: Boolean): List<Laser> {
        val n = if (phase2) 7 else 5
        val spreadDeg = 80f
        val sweep = (kotlin.math.sin(fireTick * 0.5) * 25.0).toFloat()    // rotating fan
        return (0 until n).map { i ->
            val frac = if (n == 1) 0.5f else i.toFloat() / (n - 1)
            val rad = Math.toRadians((-spreadDeg / 2f + frac * spreadDeg + sweep).toDouble())
            bossBullet(
                xSpeed = (kotlin.math.sin(rad) * 0.9).toFloat(),
                ySpeed = (kotlin.math.cos(rad) * 0.9).toFloat().coerceAtLeast(0.35f),
            )
        }
    }

    /** Ma Cà Rồng — bầy "dơi" hướng về tàu nhưng bay LƯỢN (CURVE) như dơi thật +
     *  HÚT MÁU (hồi HP, chặn ở HP spawn). Wave 17 — thêm CURVE để tách bạch khỏi
     *  ratNibble (ngắm thẳng lệch) — không còn chung archetype "ngắm tàu". */
    private fun batSwarmLifesteal(ship: Ship, phase2: Boolean): List<Laser> {
        hp = (hp + if (phase2) 28f else 16f).coerceAtMost(initialHp)     // lifesteal
        val count = if (phase2) 5 else 3
        val baseDx = ship.xOffset - xOffset
        val baseDy = (ship.yOffset - yOffset).coerceAtLeast(1f)
        val aim = baseDx / baseDy
        return (0 until count).map { i ->
            val frac = if (count == 1) 0.5f else i.toFloat() / (count - 1)
            bossBullet(
                xSpeed = aim * 0.7f,
                ySpeed = 0.75f,
                w = 22f,
                spawnX = xOffset + width * (0.15f + 0.7f * frac),
                motion = LaserMotion.CURVE,                  // bay lượn như dơi
            )
        }
    }

    /** Con Rết Vũ Trụ — phun vòng cung "độc" rộng + chậm (như đám mây độc trôi). */
    private fun poisonSprayArc(phase2: Boolean): List<Laser> {
        val n = if (phase2) 9 else 6
        val spreadDeg = 130f
        return (0 until n).map { i ->
            val frac = i.toFloat() / (n - 1)
            val rad = Math.toRadians((-spreadDeg / 2f + frac * spreadDeg).toDouble())
            bossBullet(
                xSpeed = (kotlin.math.sin(rad) * 0.5).toFloat(),
                ySpeed = (kotlin.math.cos(rad) * 0.5).toFloat().coerceAtLeast(0.3f),
                w = 20f,
            )
        }
    }

    /** Gà Mái Dầu — "ổ trứng": cụm trứng TO, CHẬM, chụm sát nhau rơi gần thẳng
     *  (khác hauntScatter: ít + to + chậm + có trật tự, không tản loạn). */
    private fun eggLobCluster(phase2: Boolean): List<Laser> {
        val n = if (phase2) 5 else 3
        return (0 until n).map { i ->
            val frac = if (n == 1) 0.5f else i.toFloat() / (n - 1)
            bossBullet(
                xSpeed = (frac - 0.5f) * 0.12f,                   // chụm, gần thẳng
                ySpeed = 0.45f,                                   // trứng nặng = chậm
                w = 32f,                                          // trứng TO
                spawnX = xOffset + width * (0.32f + 0.36f * frac),
            )
        }
    }

    /** Trâu Hung Hãn — "húc sừng GIA TỐC": tia nặng nhắm tàu, càng bay càng NHANH
     *  (ACCEL) như cú húc lao tới → khó né lúc cận. (Wave 17 — quỹ đạo phi tuyến.) */
    private fun hornCharge(ship: Ship, phase2: Boolean): List<Laser> {
        val baseDx = ship.xOffset - xOffset
        val baseDy = (ship.yOffset - yOffset).coerceAtLeast(1f)
        val aim = baseDx / baseDy
        val horns = if (phase2) listOf(-0.18f, -0.06f, 0.06f, 0.18f) else listOf(-0.12f, 0.12f)
        return horns.map { off ->
            bossBullet(
                xSpeed = aim * 1.3f,
                ySpeed = 1.3f,
                w = 34f,
                spawnX = xOffset + width * (0.5f + off) - 17f,
                motion = LaserMotion.ACCEL,
            )
        }
    }

    /** Cọp Hung Tợn — "gầm": bức tường ngang gần kín màn, 1-2 khe để né. */
    private fun roarWall(phase2: Boolean): List<Laser> {
        val count = 8
        val gaps = if (phase2) {
            setOf((Math.random() * count).toInt())                       // 1 gap = harder
        } else {
            setOf((Math.random() * count).toInt(), (Math.random() * count).toInt())
        }
        val spacing = (screenWidth * 0.9f) / count
        val startX = screenWidth * 0.05f
        return (0 until count).filter { it !in gaps }.map { i ->
            bossBullet(
                xSpeed = 0f,
                ySpeed = if (phase2) 1.1f else 0.9f,
                w = 28f,
                spawnX = startX + i * spacing,
            )
        }
    }

    // ── Wave 16 batch 2 ──

    /** Bạch Long Mắt Lam — "thét lửa": luồng hẹp, nhanh, hơi rung (flame breath). */
    private fun fireBreath(phase2: Boolean): List<Laser> {
        val n = if (phase2) 5 else 3
        val flicker = (kotlin.math.sin(fireTick * 0.9) * 0.12).toFloat()
        return (0 until n).map { i ->
            val frac = if (n == 1) 0f else (i.toFloat() / (n - 1) - 0.5f)   // -0.5..0.5
            bossBullet(xSpeed = frac * 0.3f + flicker, ySpeed = 1.1f, w = 24f)
        }
    }

    /** Cộng Sản Lên Ngôi — quăng "búa & liềm": 2 vật nặng văng 2 bên + phase 2
     *  thêm cú "đập búa" CHÍNH GIỮA rơi thẳng nặng (Wave 17 — bỏ tia ngắm-tàu để
     *  không dính archetype "ngắm tàu"). */
    private fun hammerSickle(phase2: Boolean): List<Laser> {
        val out = mutableListOf<Laser>(
            bossBullet(xSpeed = -0.6f, ySpeed = 0.7f, w = 32f, spawnX = xOffset + width * 0.30f - 16f),
            bossBullet(xSpeed = 0.6f, ySpeed = 0.7f, w = 32f, spawnX = xOffset + width * 0.70f - 16f),
        )
        if (phase2) {
            // Đập búa giữa: tạ nặng rơi THẲNG, nhanh (không ngắm).
            out.add(bossBullet(xSpeed = 0f, ySpeed = 1.2f, w = 38f))
        }
        return out
    }

    /** Tư Bản Bóc Lột — "mưa tiền": đạn nhẹ rơi rải khắp bề ngang màn hình. */
    private fun moneyRain(phase2: Boolean): List<Laser> {
        val n = if (phase2) 7 else 5
        return (0 until n).map {
            bossBullet(
                xSpeed = (Math.random().toFloat() - 0.5f) * 0.15f,
                ySpeed = 0.7f + Math.random().toFloat() * 0.3f,
                w = 18f,
                spawnX = screenWidth * (0.05f + 0.9f * Math.random().toFloat()),
            )
        }
    }

    /** Tycoon Vàng — "đô la xoáy": dòng xoắn ốc quay theo thời gian (fireTick). */
    private fun dollarSpiral(phase2: Boolean): List<Laser> {
        val arms = if (phase2) 3 else 2
        val base = fireTick * 22f
        return (0 until arms).map { k ->
            val rad = Math.toRadians((base + k * 360f / arms).toDouble())
            bossBullet(
                xSpeed = (kotlin.math.sin(rad) * 0.75).toFloat(),
                ySpeed = (kotlin.math.cos(rad) * 0.75).toFloat().coerceAtLeast(0.3f),
                w = 22f,
            )
        }
    }

    /** Cô Gái Sexy — "quất tóc UỐN LƯỢN": loạt đạn bay CONG hình sin (CURVE), đổi
     *  bên luân phiên mỗi nhịp → lọn tóc quật ngoằn ngoèo, khó đoán đường rơi.
     *  (Wave 17 — quỹ đạo phi tuyến.) */
    private fun hairWhip(phase2: Boolean): List<Laser> {
        val side = if (fireTick % 2 == 0) -1f else 1f
        val n = if (phase2) 4 else 3
        return (0 until n).map { i ->
            val frac = i.toFloat() / n
            bossBullet(
                xSpeed = side * (0.25f + frac * 0.5f),
                ySpeed = 0.7f,
                w = 22f,
                motion = LaserMotion.CURVE,
            )
        }
    }

    /** Tháp Tinh Quỷ — "tia từ đỉnh": cột dọc dày ở tâm + 2 tia rìa nghiêng. */
    private fun towerBeam(phase2: Boolean): List<Laser> {
        val out = mutableListOf<Laser>()
        val cols = if (phase2) listOf(-0.06f, 0f, 0.06f) else listOf(0f)
        cols.forEach { off ->
            out.add(
                bossBullet(
                    xSpeed = 0f, ySpeed = 1.2f, w = 26f,
                    spawnX = xOffset + width * (0.5f + off) - 13f,
                ),
            )
        }
        out.add(bossBullet(xSpeed = -0.4f, ySpeed = 0.7f, w = 20f))
        out.add(bossBullet(xSpeed = 0.4f, ySpeed = 0.7f, w = 20f))
        return out
    }

    // ── Wave 16 batch 3 (final) ──

    /** Lính Gác Mắt Sát Thủ / Chúa Tể Địa Ngục — "tia mắt SĂN ĐUỔI": phóng về
     *  phía tàu rồi TỰ BÁM theo (HOMING) → buộc người chơi né liên tục, khác hẳn
     *  mọi đòn bay thẳng. (Wave 17 — quỹ đạo phi tuyến.) */
    private fun eyeBeam(ship: Ship, phase2: Boolean): List<Laser> {
        val dx = ship.xOffset - xOffset
        val dy = (ship.yOffset - yOffset).coerceAtLeast(1f)
        val aim = dx / dy
        val n = if (phase2) 4 else 3
        val out = (0 until n).map { i ->
            bossBullet(
                xSpeed = aim * 1.0f,
                ySpeed = 1.0f,
                w = 22f,
                spawnX = xOffset + width / 2f - 11f + (i - n / 2) * 6f,
                motion = LaserMotion.HOMING,
            )
        }.toMutableList()
        if (phase2) {
            out.add(bossBullet(xSpeed = aim - 0.3f, ySpeed = 0.9f, w = 20f, motion = LaserMotion.HOMING))
            out.add(bossBullet(xSpeed = aim + 0.3f, ySpeed = 0.9f, w = 20f, motion = LaserMotion.HOMING))
        }
        return out
    }

    /** Hộ Vệ Nguyên Tử — "quỹ đạo": vòng đạn quay (atomic orbit), trôi dần xuống. */
    private fun atomOrbit(phase2: Boolean): List<Laser> {
        val n = if (phase2) 8 else 6
        val base = fireTick * 15f
        return (0 until n).map { i ->
            val rad = Math.toRadians((base + i * 360f / n).toDouble())
            bossBullet(
                xSpeed = (kotlin.math.cos(rad) * 0.6).toFloat(),
                // y amplitude < bias so EVERY orbit bullet still drifts downward
                // (min = -0.45 + 0.55 = +0.10); upward bullets would just fly off
                // the top and waste the volley.
                ySpeed = (kotlin.math.sin(rad) * 0.45).toFloat() + 0.55f,
                w = 22f,
            )
        }
    }

    /** Hồn Ma Trẻ Em — "ám khắp màn": nhiều viên NHỎ, NHANH, toé NGANG mạnh,
     *  hiện ra từ vị trí ngẫu nhiên KHẮP ĐỈNH màn (không phải từ thân boss) →
     *  cảm giác ma trơi vây quanh. Khác eggLob (ít/to/chậm/chụm) và moneyRain
     *  (rơi gần thẳng); haunt bay xiên mạnh. */
    private fun hauntScatter(phase2: Boolean): List<Laser> {
        val n = if (phase2) 9 else 6
        return (0 until n).map {
            bossBullet(
                xSpeed = (Math.random().toFloat() - 0.5f) * 1.6f,    // toé ngang MẠNH
                ySpeed = 0.55f + Math.random().toFloat() * 0.5f,     // nhanh
                w = 15f,                                             // nhỏ
                spawnX = screenWidth * Math.random().toFloat(),       // khắp đỉnh màn
            )
        }
    }

    /** Chuột Ngu Si — "gặm nhấm": vài viên NHỎ, NHANH, thất thường rơi gần thẳng
     *  (Wave 17 — bỏ ngắm-tàu, thành erratic thuần → ra khỏi archetype "ngắm tàu".
     *  Khác hauntScatter: ít hơn, hẹp hơn, tỏa từ thân boss). */
    private fun ratNibble(phase2: Boolean): List<Laser> {
        val n = if (phase2) 4 else 2
        return (0 until n).map {
            bossBullet(
                xSpeed = (Math.random().toFloat() - 0.5f) * 0.5f,   // jitter hẹp, KHÔNG ngắm
                ySpeed = 0.9f + Math.random().toFloat() * 0.3f,     // tốc độ thất thường
                w = 16f,                                            // răng chuột nhỏ
            )
        }
    }

    /** Đôi Đỉnh Sinh Hoa — "tia kép": 2 cột thẳng song song cách xa (twin beams). */
    private fun twinColumns(phase2: Boolean): List<Laser> {
        val cols = if (phase2) listOf(0.25f, 0.40f, 0.60f, 0.75f) else listOf(0.3f, 0.7f)
        return cols.map { frac ->
            bossBullet(xSpeed = 0f, ySpeed = 1.0f, w = 28f, spawnX = screenWidth * frac - 14f)
        }
    }

    /** Đôi Cầu Hư Vô — "xé hư vô": 2 cầu bắn chéo nhau tạo hình X. */
    private fun voidOrbs(phase2: Boolean): List<Laser> {
        val out = mutableListOf<Laser>()
        val n = if (phase2) 3 else 2
        repeat(n) { i ->
            val f = if (n == 1) 0f else i.toFloat() / (n - 1)
            out.add(bossBullet(xSpeed = 0.2f + f * 0.4f, ySpeed = 0.7f, w = 22f,
                spawnX = xOffset + width * 0.2f))
            out.add(bossBullet(xSpeed = -(0.2f + f * 0.4f), ySpeed = 0.7f, w = 22f,
                spawnX = xOffset + width * 0.8f))
        }
        return out
    }

    // ── Wave 16 batch 2 (3 boss user nêu đích danh, nốt) ──

    /** Bao Cao Su Khổng Lồ — "sóng xung kích": HAI vòng đồng tâm bắn cùng lúc,
     *  vòng trong CHẬM + vòng ngoài NHANH → lan ra như nổ. Khác hẳn atomOrbit
     *  (1 vòng quay đều) và venomWeb (lưới nan cố định). */
    private fun inflateBurst(phase2: Boolean): List<Laser> {
        val out = mutableListOf<Laser>()
        val inner = if (phase2) 10 else 7
        val outer = if (phase2) 14 else 10
        // Vòng trong: dày, chậm (sóng đầu).
        for (i in 0 until inner) {
            val rad = Math.toRadians((i * 360f / inner).toDouble())
            out.add(
                bossBullet(
                    xSpeed = (kotlin.math.cos(rad) * 0.32).toFloat(),
                    ySpeed = (kotlin.math.sin(rad) * 0.28).toFloat() + 0.42f,
                    w = 24f,
                ),
            )
        }
        // Vòng ngoài: lệch pha 12°, nhanh hơn ~2.4× → tách khỏi vòng trong khi bay.
        for (i in 0 until outer) {
            val rad = Math.toRadians((i * 360f / outer + 12f).toDouble())
            out.add(
                bossBullet(
                    xSpeed = (kotlin.math.cos(rad) * 0.8).toFloat(),
                    ySpeed = (kotlin.math.sin(rad) * 0.6).toFloat() + 0.6f,
                    w = 18f,
                ),
            )
        }
        return out
    }

    /** Nhện Venom — "tơ độc": 8 nan cố định toả ra (như chân nhện) + phase2 nhả thẳng. */
    private fun venomWeb(phase2: Boolean): List<Laser> {
        val spokes = 8
        val out = (0 until spokes).map { i ->
            val rad = Math.toRadians((i * 360f / spokes - 90f).toDouble())
            bossBullet(
                xSpeed = (kotlin.math.cos(rad) * 0.45).toFloat(),
                ySpeed = (kotlin.math.sin(rad) * 0.45).toFloat() + 0.5f,
                w = 20f,
            )
        }.toMutableList()
        if (phase2) out.add(bossBullet(xSpeed = 0f, ySpeed = 0.95f, w = 24f))
        return out
    }

    /** Tham Nhũng — "tiền đè": tường ngang DÀY, CHẬM, gần kín. Khe an toàn DI
     *  CHUYỂN tuần tự mỗi loạt (fireTick) → ép người chơi rượt theo làn, không
     *  đứng yên được. Khác roarWall (khe NGẪU NHIÊN, tường nhanh). */
    private fun corruptionWall(phase2: Boolean): List<Laser> {
        val count = if (phase2) 10 else 7
        val spacing = (screenWidth * 0.94f) / count
        val startX = screenWidth * 0.03f
        // Khe quét qua-lại: 0,1,2,…,count-1,count-2,… (tam giác) theo fireTick.
        val period = (count - 1) * 2
        val t = if (period == 0) 0 else fireTick % period
        val gap = if (t < count) t else period - t
        return (0 until count).filter { it != gap }.map { i ->
            bossBullet(
                xSpeed = 0f,
                ySpeed = if (phase2) 0.7f else 0.55f,                  // chậm = "đè"
                w = 24f,
                spawnX = startX + i * spacing,
            )
        }
    }

    // ── Wave 18 batch 1 — 3 chiêu trào phúng ──

    /** Trùm Kẹt Xe — "tắc đường": lấp DÀY một NỬA màn (trái/phải luân phiên theo
     *  fireTick), nửa kia để trống làm làn thoát → ép người chơi đổi làn liên tục.
     *  Khác corruptionWall (tường KÍN cả màn, khe nhỏ quét) — đây bỏ trống hẳn 1 nửa. */
    private fun trafficGridlock(phase2: Boolean): List<Laser> {
        val count = if (phase2) 7 else 5
        val leftLane = fireTick % 2 == 0
        val laneStart = if (leftLane) screenWidth * 0.02f else screenWidth * 0.52f
        val span = screenWidth * 0.46f
        val spacing = span / count
        return (0 until count).map { i ->
            bossBullet(
                xSpeed = 0f,
                ySpeed = if (phase2) 0.6f else 0.48f,                  // xe bò chậm
                w = 22f,
                spawnX = laneStart + i * spacing,
            )
        }
    }

    /** Sếp KPI — "chỉ tiêu": 3-4 cột đạn TĂNG TỐC (ACCEL = KPI leo dốc) rơi thẳng;
     *  phase2 thêm 1 "deadline" HOMING đuổi theo người chơi. */
    private fun kpiColumns(ship: Ship, phase2: Boolean): List<Laser> {
        val cols = if (phase2) 4 else 3
        val out = mutableListOf<Laser>()
        for (i in 0 until cols) {
            val frac = if (cols == 1) 0.5f else i.toFloat() / (cols - 1)
            val fx = screenWidth * (0.18f + 0.64f * frac)
            out.add(
                bossBullet(
                    xSpeed = 0f, ySpeed = 0.32f, w = 22f, spawnX = fx,
                    motion = com.tranphuloi.neon.ui.game.enemy.laser.LaserMotion.ACCEL,
                ),
            )
        }
        if (phase2) {
            val aim = ((ship.xOffset + ship.width / 2f) - (xOffset + width / 2f)) / screenHeight
            out.add(
                bossBullet(
                    xSpeed = aim, ySpeed = 0.85f, w = 26f,
                    motion = com.tranphuloi.neon.ui.game.enemy.laser.LaserMotion.HOMING,
                ),
            )
        }
        return out
    }

    /** Hot TikToker — "spam tim": quạt đối xứng nhiều đạn BAY CONG (CURVE = tim lượn
     *  qua lại); phase2 dày hơn. Khác hairWhip (Diva, CURVE 1 chiều quất) — đây toả đều 2 bên. */
    private fun heartSpam(phase2: Boolean): List<Laser> {
        val n = if (phase2) 7 else 5
        return (0 until n).map { i ->
            val frac = if (n == 1) 0.5f else i.toFloat() / (n - 1)
            val xs = (frac - 0.5f) * 0.7f                              // toả đều quanh trục
            bossBullet(
                xSpeed = xs, ySpeed = 0.55f, w = 22f,
                motion = com.tranphuloi.neon.ui.game.enemy.laser.LaserMotion.CURVE,
            )
        }
    }

    // ── Wave 19 batch 2 — 3 chiêu trào phúng (nốt) ──

    /** ATM Hết Tiền — "nhả tiền": luồng đạn HẸP rơi thẳng dồn dập 4 loạt liền, rồi
     *  KẸT (1 loạt rỗng = "hết tiền") theo chu kỳ fireTick. Khác mọi chiều rộng-toả. */
    private fun atmCashSpit(phase2: Boolean): List<Laser> {
        // Chu kỳ 5: 4 nhịp nhả + 1 nhịp kẹt (rỗng).
        if (fireTick % 5 == 4) return emptyList()
        val lanes = if (phase2) 3 else 2
        val gap = width * 0.22f
        return (0 until lanes).map { i ->
            val dx = (i - (lanes - 1) / 2f) * gap
            bossBullet(xSpeed = 0f, ySpeed = 0.95f, w = 18f,
                spawnX = xOffset + width / 2f - 9f + dx)            // luồng hẹp, nhanh
        }
    }

    /** Cục Gạch Nokia — "ném gạch": 2-3 viên CỰC TO (w=42) + CHẬM, lệch nhau → ít khe
     *  nhưng né được bằng đi ngang. Khác hẳn các đạn nhỏ-nhanh. */
    private fun brickToss(phase2: Boolean): List<Laser> {
        val n = if (phase2) 3 else 2
        return (0 until n).map { i ->
            val frac = if (n == 1) 0.5f else i.toFloat() / (n - 1)
            bossBullet(
                xSpeed = (frac - 0.5f) * 0.25f,                       // toả nhẹ
                ySpeed = 0.34f,                                       // nặng = chậm
                w = 42f,
                spawnX = screenWidth * (0.2f + 0.6f * frac) - 21f,
            )
        }
    }

    /** Bão Giá Lạm Phát — "lạm phát": SỐ đạn TĂNG DẦN mỗi loạt (4→7 theo fireTick) +
     *  TĂNG TỐC (ACCEL). Càng về sau càng dày = giá leo thang. */
    private fun inflationWave(phase2: Boolean): List<Laser> {
        val base = if (phase2) 6 else 4
        val n = base + (fireTick % 4)                                // 4..7 (hoặc 6..9)
        return (0 until n).map { i ->
            val frac = if (n == 1) 0.5f else i.toFloat() / (n - 1)
            bossBullet(
                xSpeed = (frac - 0.5f) * 0.6f,
                ySpeed = 0.3f,                                        // chậm lúc đầu, ACCEL tăng tốc sau
                w = 20f,
                motion = com.tranphuloi.neon.ui.game.enemy.laser.LaserMotion.ACCEL,
            )
        }
    }

    // ── Wave 20 batch 3 — 4 chiêu trào phúng (hết batch 1) ──

    /** Drama MXH — "ném đá hội đồng": 2 CỤM lệch hướng theo fireTick (góc xoay) →
     *  cảm giác bị vây từ nhiều phía, thất thường. */
    private fun dramaPileOn(phase2: Boolean): List<Laser> {
        val perCluster = if (phase2) 4 else 3
        val baseAng = (fireTick * 37) % 360                          // hướng cụm xoay mỗi loạt
        val out = mutableListOf<Laser>()
        for (c in 0..1) {
            val centerDeg = baseAng + c * 180                        // 2 cụm đối nhau
            for (i in 0 until perCluster) {
                val deg = centerDeg + (i - (perCluster - 1) / 2) * 14
                val rad = Math.toRadians(deg.toDouble())
                out.add(
                    bossBullet(
                        xSpeed = (kotlin.math.cos(rad) * 0.5).toFloat(),
                        ySpeed = (kotlin.math.sin(rad) * 0.4).toFloat() + 0.45f,  // luôn trôi xuống
                        w = 18f,
                    ),
                )
            }
        }
        return out
    }

    /** Trùm Đa Cấp — "tuyến dưới": spread hình KIM TỰ THÁP — mỗi loạt phình 1 tầng
     *  rộng hơn (1,2,3 viên… theo fireTick) toả xuống. */
    private fun pyramidScheme(phase2: Boolean): List<Laser> {
        val tiers = (fireTick % (if (phase2) 4 else 3)) + 1          // 1..3 (hoặc 1..4) tầng
        val out = mutableListOf<Laser>()
        for (row in 0 until tiers) {
            val count = row + 1                                       // tầng dưới rộng hơn
            for (i in 0 until count) {
                val frac = if (count == 1) 0.5f else i.toFloat() / (count - 1)
                out.add(
                    bossBullet(
                        xSpeed = (frac - 0.5f) * 0.5f * (row + 1),    // tầng càng dưới toả càng rộng
                        ySpeed = 0.55f,
                        w = 18f,
                    ),
                )
            }
        }
        return out
    }

    /** Thầy Bói Online — "tiên tri": quạt đối xứng 5/7 viên LINEAR + 1 viên HOMING
     *  "lời tiên tri" đoán đường người chơi. */
    private fun prophecyFan(ship: Ship, phase2: Boolean): List<Laser> {
        val n = if (phase2) 7 else 5
        val out = (0 until n).map { i ->
            val frac = if (n == 1) 0.5f else i.toFloat() / (n - 1)
            bossBullet(xSpeed = (frac - 0.5f) * 0.8f, ySpeed = 0.6f, w = 18f)
        }.toMutableList()
        val aim = ((ship.xOffset + ship.width / 2f) - (xOffset + width / 2f)) / screenHeight
        out.add(
            bossBullet(
                xSpeed = aim, ySpeed = 0.85f, w = 24f,
                motion = com.tranphuloi.neon.ui.game.enemy.laser.LaserMotion.HOMING,
            ),
        )
        return out
    }

    /** Ông Táo Cưỡi Cá Chép — "cá nhảy + lửa": vài viên BAY CONG (CURVE = cá vượt vũ
     *  môn) 2 bên + 1 luồng lửa thẳng giữa. */
    private fun carpLeap(phase2: Boolean): List<Laser> {
        val side = if (phase2) 3 else 2
        val out = mutableListOf<Laser>()
        for (s in listOf(-1, 1)) {
            for (i in 0 until side) {
                out.add(
                    bossBullet(
                        xSpeed = s * (0.2f + i * 0.12f),
                        ySpeed = 0.5f, w = 20f,
                        motion = com.tranphuloi.neon.ui.game.enemy.laser.LaserMotion.CURVE,
                    ),
                )
            }
        }
        out.add(bossBullet(xSpeed = 0f, ySpeed = 0.9f, w = 22f))      // luồng lửa thẳng giữa
        return out
    }

    // ── Wave 21 batch 4 — 2 chiêu trào phúng (batch 2 mở màn) ──

    /** Ông Chú Crypto — "pump & dump": loạt CHẴN dồn dày 1 cột nhanh (pump), loạt
     *  LẺ tản loạn toả rộng chậm (dump). Xen kẽ theo fireTick → nhịp thất thường. */
    private fun cryptoVolatility(phase2: Boolean): List<Laser> {
        return if (fireTick % 2 == 0) {
            // Pump: cột dồn dày, nhanh.
            val n = if (phase2) 5 else 4
            (0 until n).map { bossBullet(xSpeed = 0f, ySpeed = 0.95f, w = 16f) }
        } else {
            // Dump: tản rộng, chậm.
            val n = if (phase2) 9 else 7
            (0 until n).map { i ->
                val frac = if (n == 1) 0.5f else i.toFloat() / (n - 1)
                bossBullet(xSpeed = (frac - 0.5f) * 1.1f, ySpeed = 0.4f, w = 18f)
            }
        }
    }

    /** Trẻ Trâu Toxic — "khẩu nghiệp": spam đạn nhỏ NHANH hướng PSEUDO-NGẪU theo
     *  fireTick (góc xoay theo công thức), cảm giác cãi loạn xạ. */
    private fun toxicSpam(phase2: Boolean): List<Laser> {
        val n = if (phase2) 6 else 4
        return (0 until n).map { i ->
            // Góc xoay theo fireTick + index → trải khắp nửa dưới, thất thường.
            val deg = 60 + ((fireTick * 53 + i * 91) % 60)            // 60..119° (toả xuống)
            val rad = Math.toRadians(deg.toDouble())
            bossBullet(
                xSpeed = (kotlin.math.cos(rad) * 0.7).toFloat(),
                ySpeed = (kotlin.math.sin(rad) * 0.7).toFloat() + 0.25f,
                w = 14f,
            )
        }
    }

    // ── Wave 22 batch 5 — 2 chiêu trào phúng ──

    /** Trùm Karaoke Lạc Tông — "sóng âm": 1 VÒNG tròn đều toả ra, lệch pha mỗi loạt
     *  (fireTick) → nhiều vòng đan nhau như gợn sóng. Khác inflateBurst (2 vòng cùng
     *  lúc) — đây 1 vòng/loạt nhưng liên tục tạo gợn. */
    private fun soundWaves(phase2: Boolean): List<Laser> {
        val n = if (phase2) 14 else 11
        val phase = (fireTick % 2) * (180f / n)                      // lệch nửa bước mỗi loạt
        return (0 until n).map { i ->
            val rad = Math.toRadians((i * 360f / n + phase).toDouble())
            bossBullet(
                xSpeed = (kotlin.math.cos(rad) * 0.5).toFloat(),
                ySpeed = (kotlin.math.sin(rad) * 0.42).toFloat() + 0.45f,   // bias xuống
                w = 18f,
            )
        }
    }

    /** Đại Gia Phông Bạt — "flex": quạt CỰC RỘNG gần ngang (phô trương), 2 nhịp lệch
     *  pha. Bao trùm bề ngang → ép người chơi xuống thấp. */
    private fun flexBurst(phase2: Boolean): List<Laser> {
        val n = if (phase2) 11 else 8
        val skew = if (fireTick % 2 == 0) -0.06f else 0.06f          // lệch pha 2 nhịp
        return (0 until n).map { i ->
            val frac = if (n == 1) 0.5f else i.toFloat() / (n - 1)
            bossBullet(
                xSpeed = (frac - 0.5f) * 1.4f + skew,                // toả rất rộng
                ySpeed = 0.4f,
                w = 18f,
            )
        }
    }

    // ── Wave 23 batch 6 — 2 chiêu trào phúng ──

    /** Bác Sĩ Google — "chẩn đoán bừa": 4 luồng chéo hình X (2 trái-2 phải) + phase2
     *  thêm 2 luồng dọc. Kết quả lung tung khắp nơi. */
    private fun misdiagnosisX(phase2: Boolean): List<Laser> {
        val out = mutableListOf<Laser>()
        // 4 tia chéo: ±35°, ±60° quanh phương xuống.
        for (deg in listOf(55, 75, 105, 125)) {
            val rad = Math.toRadians(deg.toDouble())
            out.add(
                bossBullet(
                    xSpeed = (kotlin.math.cos(rad) * 0.7).toFloat(),
                    ySpeed = (kotlin.math.sin(rad) * 0.7).toFloat() + 0.2f,
                    w = 18f,
                ),
            )
        }
        if (phase2) {
            out.add(bossBullet(xSpeed = -0.12f, ySpeed = 0.8f, w = 16f))
            out.add(bossBullet(xSpeed = 0.12f, ySpeed = 0.8f, w = 16f))
        }
        return out
    }

    /** Hoàng Thượng Mèo — "vuốt mèo": 2 CỤM 3 tia sát nhau (vết vuốt) lệch nhau, luân
     *  phiên 2 bên theo fireTick → như móng cào chéo. */
    private fun royalPaws(phase2: Boolean): List<Laser> {
        val side = if (fireTick % 2 == 0) -1f else 1f
        val centers = if (phase2) listOf(side * 0.18f, side * 0.45f) else listOf(side * 0.3f)
        val out = mutableListOf<Laser>()
        for (c in centers) {
            for (k in -1..1) {                                       // 3 tia/cụm (vết vuốt)
                out.add(bossBullet(xSpeed = c + k * 0.08f, ySpeed = 0.62f, w = 16f))
            }
        }
        return out
    }

    // ── Wave 24 batch 7 — 2 chiêu trào phúng (HẾT 18) ──

    /** Thánh Cuồng Sale — "flash sale": chu kỳ 3 — 2 nhịp BÙNG dồn dày (đổ xô mua)
     *  rồi 1 nhịp NGHỈ rỗng (hết sale). Khác atmCashSpit (luồng hẹp 4-nhả-1-kẹt) —
     *  đây dồn RỘNG dày từng đợt. */
    private fun flashSale(phase2: Boolean): List<Laser> {
        if (fireTick % 3 == 2) return emptyList()                    // nhịp nghỉ
        val n = if (phase2) 9 else 7
        return (0 until n).map { i ->
            val frac = if (n == 1) 0.5f else i.toFloat() / (n - 1)
            bossBullet(xSpeed = (frac - 0.5f) * 0.9f, ySpeed = 0.75f, w = 16f)   // dồn dày, nhanh
        }
    }

    /** Cô Hồn Tháng 7 — "hồn lang thang": ÍT đạn (3-4) BAY CONG (CURVE) CHẬM, lệch
     *  hướng nhẹ theo fireTick → vật vờ, khó đoán. Khác heartSpam (quạt đối xứng nhanh). */
    private fun wanderingSpirits(phase2: Boolean): List<Laser> {
        val n = if (phase2) 4 else 3
        return (0 until n).map { i ->
            val drift = ((fireTick * 31 + i * 67) % 100) / 100f - 0.5f   // -0.5..0.5 vật vờ
            bossBullet(
                xSpeed = drift * 0.5f, ySpeed = 0.32f, w = 22f,             // chậm
                motion = com.tranphuloi.neon.ui.game.enemy.laser.LaserMotion.CURVE,
            )
        }
    }

    override fun onObjectImpact(impactPower: Float) {
        // Marquee SHIELD — bất tử trong cửa sổ shield: nháy (deflect) nhưng KHÔNG mất máu.
        if (System.currentTimeMillis() < shieldedUntilMillis) {
            lastImpactMillis = System.currentTimeMillis()
            return
        }
        hp -= impactPower
        lastImpactMillis = System.currentTimeMillis()
        if (!isInEntryPhase) {
            // Cộng dồn offset giật lùi (âm = lên), kẹp -22px; suy giảm 0.85/frame.
            knockbackY = (knockbackY - 6f).coerceAtLeast(-22f)
        }
    }

    companion object {
        /** Marquee SHIELD — độ dài cửa sổ bất tử khi vào phase 2 (ms). */
        const val SHIELD_MS: Long = 1800L
        /** Marquee TELEPORT — khoảng cách giữa 2 lần nhảy (ms). */
        const val TELEPORT_INTERVAL_MS: Long = 4000L
        /** Marquee TELEPORT — giữ vị trí sau nhảy (ms), chặn movement đè. */
        const val TELEPORT_HOLD_MS: Long = 450L
    }
}
