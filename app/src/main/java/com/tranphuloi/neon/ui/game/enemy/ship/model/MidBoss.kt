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
    private val sizeScale: Float =
        (0.8f + (variant.baseHp - 1200f) / 2100f * 0.5f).coerceIn(0.8f, 1.3f)
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
    /** SHIELD: bất tử 1 cửa sổ khi vào phase 2 (Bạch Long, Bao Cao Su). */
    private val hasShield: Boolean =
        variant == MidBossType.WHITE_DRAGON || variant == MidBossType.GIANT_CONDOM
    /** TELEPORT: nhảy chỗ định kỳ + giữ vị trí (Venom, Tham Nhũng, Hell Lord/OFFENSIVE). */
    private val hasTeleport: Boolean =
        variant == MidBossType.VENOM_SPIDER || variant == MidBossType.CORRUPTION ||
            variant == MidBossType.OFFENSIVE
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
            MidBossType.HAMMER_SICKLE -> hammerSickle(ship, phase2)
            MidBossType.MONEY_TYCOON -> moneyRain(phase2)
            MidBossType.GOLDEN_TYCOON -> dollarSpiral(phase2)
            MidBossType.SEXY_DIVA -> hairWhip(phase2)
            MidBossType.TROLL_TOWER -> towerBeam(phase2)
            MidBossType.OFFENSIVE -> eyeBeam(ship, phase2)
            MidBossType.DEFENSIVE -> atomOrbit(phase2)
            MidBossType.SWARM -> hauntScatter(phase2)
            MidBossType.DUMB_RAT -> ratNibble(ship, phase2)
            MidBossType.TWIN_SUMMITS -> twinColumns(phase2)
            MidBossType.VOID_GLOBES -> voidOrbs(phase2)
            // Wave 16 batch 2
            MidBossType.GIANT_CONDOM -> inflateBurst(phase2)
            MidBossType.VENOM_SPIDER -> venomWeb(phase2)
            MidBossType.CORRUPTION -> corruptionWall(phase2)
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

    /** Ma Cà Rồng — bầy "dơi" hội tụ về tàu + HÚT MÁU (hồi HP, chặn ở HP spawn). */
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

    /** Cộng Sản Lên Ngôi — quăng "búa & liềm": 2 vật nặng văng 2 bên (+ tâm ở phase 2). */
    private fun hammerSickle(ship: Ship, phase2: Boolean): List<Laser> {
        val out = mutableListOf<Laser>(
            bossBullet(xSpeed = -0.6f, ySpeed = 0.7f, w = 32f, spawnX = xOffset + width * 0.30f - 16f),
            bossBullet(xSpeed = 0.6f, ySpeed = 0.7f, w = 32f, spawnX = xOffset + width * 0.70f - 16f),
        )
        if (phase2) {
            val dx = ship.xOffset - xOffset
            val dy = (ship.yOffset - yOffset).coerceAtLeast(1f)
            out.add(bossBullet(xSpeed = (dx / dy) * 0.8f, ySpeed = 0.9f, w = 30f))
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

    /** Chuột Ngu Si — "gặm": nhắm tàu nhưng lệch lung tung (ngắm dở). */
    private fun ratNibble(ship: Ship, phase2: Boolean): List<Laser> {
        val dx = ship.xOffset - xOffset
        val dy = (ship.yOffset - yOffset).coerceAtLeast(1f)
        val aim = dx / dy
        val n = if (phase2) 3 else 2
        return (0 until n).map {
            val misaim = (Math.random().toFloat() - 0.5f) * 0.6f
            bossBullet(xSpeed = aim * 0.7f + misaim, ySpeed = 0.8f, w = 24f)
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
