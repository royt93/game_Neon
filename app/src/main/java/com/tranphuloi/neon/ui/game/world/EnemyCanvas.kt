package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.tranphuloi.neon.common.PathPool
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.drawSoftHalo
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Round 66b — Pure-vector enemy rendering. Replaces 14 webp drawables with
 * geometric polygon shapes per family. Faction visual identity is encoded
 * in shape + color (kept from original sprite hue):
 *
 *   enemy_light_blue_* (5 variants) → cyan dart triangles (small fast attackers).
 *   enemy_green_*      (4 variants) → green hexagons (medium balanced).
 *   enemy_red_*        (3 variants) → red diamonds (heavy hitters).
 *   enemy_*_boss       (2 variants) → large 8-pointed star / multi-layer hex.
 *
 * Variant number within family modulates either rotation angle or detail
 * count so 5 light-blue enemies don't all look identical. Player still reads
 * "alien fleet" through family color + general silhouette.
 *
 * Hit flash + status effect tints preserved via Color.mix on the body color.
 * Boss entry trail (4 fading copies) preserved via repeated draw at offset.
 */
@Composable
fun EnemyCanvas(
    enemies: List<EnemyUI>,
    nowMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (enemies.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (enemy in enemies) {
            drawEnemy(enemy, nowMillis, density)
        }
    }
}

/** Round 66b — sprite preload no longer needed; stub kept for back-compat. */
@Immutable
@Deprecated("Round 66b — pure-vector EnemyCanvas no longer needs sprites.")
data class EnemySprites(val byDrawableId: Map<Int, Any> = emptyMap())

@Composable
@Deprecated("Round 66b — pure-vector EnemyCanvas no longer needs sprites.")
fun rememberEnemySprites(): EnemySprites = EnemySprites()

private const val HP_BAR_VERTICAL_SPACE_DP = 5

// Family colors — match original sprite palette intent.
private val LIGHT_BLUE_BODY = Color(0xFF4FD4FF)
private val GREEN_BODY = Color(0xFF6EFFAA)
private val RED_BODY = Color(0xFFFF5555)
private val LIGHT_BLUE_ACCENT = Color(0xFF1799CC)
private val GREEN_ACCENT = Color(0xFF24B86E)
private val RED_ACCENT = Color(0xFFCC1144)

private fun DrawScope.drawEnemy(
    enemy: EnemyUI,
    nowMillis: Long,
    density: Density,
) {
    with(density) {
        val xPx = enemy.xOffset.dp.toPx()
        val hpBarOffsetPx = if (enemy.isBoss) 0f else HP_BAR_VERTICAL_SPACE_DP.dp.toPx()
        val yPx = enemy.yOffset.dp.toPx() + hpBarOffsetPx
        val wPx = enemy.width.dp.toPx()
        val hPx = enemy.height.dp.toPx()
        val cx = xPx + wPx / 2f
        val cy = yPx + hPx / 2f

        // Hit flash factor (0..1, decays over 120ms).
        val sinceHit = nowMillis - enemy.lastImpactMillis
        val hitFlash = if (enemy.lastImpactMillis > 0L && sinceHit in 0..120) {
            (1f - sinceHit / 120f).coerceIn(0f, 1f)
        } else 0f

        // Glow halo — Magenta. Round 77 (R77d): boss có 2 aura layers + corner markers.
        // Wave 17h — halo ĐẬM hơn cho boss (0.45→0.66) để màu nhận diện theo kind
        // nổi bật, "viền màu đậm" dễ thấy.
        val glowIntensity = (if (enemy.isBoss) 0.66f else 0.45f) + hitFlash * 0.4f
        val glowRadiusFactor = if (enemy.isBoss) 2.5f + hitFlash * 0.4f
            else 1.4f + hitFlash * 0.4f                    // Boss aura BIGGER halo
        val glowR = (minOf(wPx, hPx) / 2f) * glowRadiusFactor
        // Wave 17h — halo boss theo BossKind (trước MỌI boss cùng đỏ 0xFFFF2D55 →
        // nhìn giống nhau). Mỗi boss 1 tông riêng → phân biệt từ xa.
        val haloColor = if (enemy.isBoss && enemy.bossKind != null) {
            bossColorFor(enemy.bossKind)
        } else if (enemy.isBoss) {
            Color(0xFFFF2D55)
        } else {
            NeonMagenta
        }
        // Round 78 (#6 perf) — was Brush.radialGradient per-enemy per-frame.
        // 30 enemies × ~5 allocs each → 150 allocs/frame just for halo. Replaced
        // with drawSoftHalo (3 drawCircles, no Brush/Shader). Visually equivalent
        // at typical halo sizes.
        drawSoftHalo(haloColor, glowIntensity, glowR, Offset(cx, cy))
        // Wave 17 — ĐÃ BỎ vòng aura phụ (1.35×) + 4 kim cương góc (1.55×) của boss.
        // Chúng vẽ theo bán kính TRÒN quanh tâm nên với boss KHÔNG tròn (cọp/rồng/
        // búa-liềm) trông như shape lạ tách rời, lại SÁNG BỪNG khi trúng đạn
        // (hitFlash) → user báo "boss bị overlay shape khi trúng đạn". Giờ chỉ
        // còn halo mềm ôm theo thân → sạch, đọc rõ boss.

        // Body color, blended with hit flash white + status effect tint.
        // Wave 17h — boss lấy màu thân theo BossKind (trước chỉ đỏ/xanh theo
        // drawable → 27 boss chỉ 2 màu). Địch thường vẫn theo drawable.
        val baseBody = if (enemy.isBoss && enemy.bossKind != null) {
            bossColorFor(enemy.bossKind)
        } else {
            bodyColorFor(enemy.drawableId)
        }
        val accent = accentColorFor(enemy.drawableId)
        val bodyColor = blendForFlash(baseBody, hitFlash, enemy.activeStatusEffectTints, nowMillis)

        // Boss entry-phase thrust trail — 4 fading copies stacked upward.
        if (enemy.isBoss && enemy.isInEntryPhase) {
            val trailDy = 30.dp.toPx()
            for (i in 1..4) {
                val trailAlpha = (1f - i * 0.22f).coerceIn(0f, 1f) * 0.55f
                if (trailAlpha <= 0f) continue
                drawEnemyShape(
                    drawableId = enemy.drawableId,
                    cx = cx,
                    cy = cy - i * trailDy,
                    wPx = wPx,
                    hPx = hPx,
                    body = bodyColor.copy(alpha = trailAlpha),
                    accent = accent.copy(alpha = trailAlpha),
                    isBoss = enemy.isBoss,
                    bossKind = enemy.bossKind,
                )
            }
        }

        // Main body shape per family / boss kind.
        drawEnemyShape(
            drawableId = enemy.drawableId,
            cx = cx,
            cy = cy,
            wPx = wPx,
            hPx = hPx,
            body = bodyColor,
            accent = accent,
            isBoss = enemy.isBoss,
            bossKind = enemy.bossKind,
        )

        // Wave 16 — status overlay RÕ RÀNG (trước chỉ tint mờ vào thân → user
        // "đạn lửa chả thấy lửa"). BURN → ngọn lửa nhấp nháy; SLOW → tinh thể
        // băng; STUN → vòng sao xoay. Vẽ trên thân để đọc được hiệu ứng.
        if (enemy.activeStatusEffectTints.isNotEmpty()) {
            drawStatusOverlay(enemy.activeStatusEffectTints, cx, cy, wPx, hPx, nowMillis)
        }
    }
}

/** Wave 16 — hiệu ứng status vẽ đè lên địch (lửa / băng / sao) theo tint. */
private fun DrawScope.drawStatusOverlay(
    tints: List<Long>,
    cx: Float, cy: Float, wPx: Float, hPx: Float, nowMillis: Long,
) {
    val burn = com.tranphuloi.neon.ui.game.status.StatusEffect.BURN.tintColorArgb
    val slow = com.tranphuloi.neon.ui.game.status.StatusEffect.SLOW.tintColorArgb
    val stun = com.tranphuloi.neon.ui.game.status.StatusEffect.STUN.tintColorArgb
    if (tints.any { it == burn }) {
        // Ngọn lửa nhấp nháy ở mép trên.
        val topY = cy - hPx * 0.42f
        val n = 4
        for (i in 0 until n) {
            val fx = cx + (i - (n - 1) / 2f) * wPx * 0.28f
            val flick = 0.7f + 0.3f * sin(nowMillis / 90.0 + i * 1.7).toFloat()
            val h = hPx * 0.45f * flick
            val w = wPx * 0.18f
            val flame = PathPool.acquire().apply {
                moveTo(fx, topY - h)
                cubicTo(fx + w, topY - h * 0.4f, fx + w * 0.5f, topY, fx, topY)
                cubicTo(fx - w * 0.5f, topY, fx - w, topY - h * 0.4f, fx, topY - h)
                close()
            }
            drawPath(flame, Color(0xFFFF6A00).copy(alpha = 0.9f))
            PathPool.release(flame)
            drawCircle(Color(0xFFFFD040), w * 0.45f, Offset(fx, topY - h * 0.35f))
        }
    }
    if (tints.any { it == slow }) {
        // Tinh thể băng 6 cánh ở tâm.
        val r = minOf(wPx, hPx) * 0.32f
        for (i in 0 until 6) {
            val a = i * Math.PI / 3.0
            drawLine(
                Color(0xFF8AE6FF).copy(alpha = 0.85f), Offset(cx, cy),
                Offset(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat()),
                strokeWidth = wPx * 0.05f, cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
        drawCircle(Color.White.copy(alpha = 0.8f), wPx * 0.06f, Offset(cx, cy))
    }
    if (tints.any { it == stun }) {
        // Vòng sao xoay quanh đầu.
        val ringR = minOf(wPx, hPx) * 0.5f
        val base = nowMillis / 220.0
        for (i in 0 until 3) {
            val a = base + i * 2.094
            val sx = cx + (ringR * cos(a)).toFloat()
            val sy = cy - hPx * 0.4f + (ringR * 0.4f * sin(a)).toFloat()
            drawCircle(Color(0xFFFFE34D), wPx * 0.07f, Offset(sx, sy))
        }
    }
}

/**
 * Map drawableId → primary body color. Preserves original sprite family hue.
 */
/**
 * Wave 17h — màu nhận diện RIÊNG cho từng boss (dùng cho cả halo + thân) để 27
 * boss không còn chung tông đỏ/xanh. Exhaustive over BossKind (thiếu = lỗi
 * compile). Tông gắn theo chủ đề từng boss.
 */
// Wave 25c — đọc màu từ bảng dữ liệu chung `bossMetaFor` (gom 4 when → 1).
internal fun bossColorFor(kind: BossKind): Color =
    Color(com.tranphuloi.neon.ui.game.enemy.ship.model.bossMetaFor(kind).colorArgb)

private fun bodyColorFor(drawableId: Int): Color = when (drawableId) {
    R.drawable.enemy_light_blue_1, R.drawable.enemy_light_blue_2,
    R.drawable.enemy_light_blue_3, R.drawable.enemy_light_blue_4,
    R.drawable.enemy_light_blue_5 -> LIGHT_BLUE_BODY
    R.drawable.enemy_green_1, R.drawable.enemy_green_2,
    R.drawable.enemy_green_3, R.drawable.enemy_green_4,
    R.drawable.enemy_green_boss -> GREEN_BODY
    R.drawable.enemy_red_1, R.drawable.enemy_red_2,
    R.drawable.enemy_red_3, R.drawable.enemy_red_boss -> RED_BODY
    // Round 74 (R73d) — ELITE family = violet, BERSERKER = orange.
    R.drawable.enemy_cross_1, R.drawable.enemy_cross_2,
    R.drawable.enemy_orb_1, R.drawable.enemy_orb_2 -> Color(0xFFB14CFF)        // violet ELITE
    R.drawable.enemy_chevron_1, R.drawable.enemy_chevron_2,
    R.drawable.enemy_spike_1, R.drawable.enemy_spike_2 -> Color(0xFFFF9020)    // orange BERSERKER
    // Round 82 — 10 R81 enemies, each unique body color (themed).
    R.drawable.enemy_spinning_saw -> Color(0xFFCCCCCC)               // steel
    R.drawable.enemy_tentacle_squid -> Color(0xFF60E0C0)             // teal
    R.drawable.enemy_mine_layer -> Color(0xFF707080)                 // gray
    R.drawable.enemy_shield_drone -> Color(0xFF40A0FF)               // sky blue
    R.drawable.enemy_sniper -> Color(0xFF8B5C2E)                     // brown wood
    R.drawable.enemy_bomber_crawler -> Color(0xFF606040)             // olive
    R.drawable.enemy_mirror_twin -> Color(0xFFFF60A0)                // pink twin
    R.drawable.enemy_phantom -> Color(0xFFB14CFF)                    // violet
    R.drawable.enemy_healer -> Color(0xFF60FFAA)                     // mint
    R.drawable.enemy_kamikaze -> Color(0xFFFFE040)                   // warning yellow
    // Task 09 (đợt 3) — 5 địch chủ đề, mỗi con 1 màu signature.
    R.drawable.enemy_splitter -> Color(0xFFE8C020)                   // gold (phân thân)
    R.drawable.enemy_repulsor -> Color(0xFFD040FF)                   // magenta (đẩy lùi)
    R.drawable.enemy_jammer -> Color(0xFF40E0FF)                     // cyan (nhiễu)
    R.drawable.enemy_missileer -> Color(0xFFFF7020)                  // cam (pháo)
    R.drawable.enemy_predator -> Color(0xFF2E9E5B)                   // xanh lá đậm (săn)
    else -> Color(0xFFCCCCCC)
}

private fun accentColorFor(drawableId: Int): Color = when (drawableId) {
    R.drawable.enemy_light_blue_1, R.drawable.enemy_light_blue_2,
    R.drawable.enemy_light_blue_3, R.drawable.enemy_light_blue_4,
    R.drawable.enemy_light_blue_5 -> LIGHT_BLUE_ACCENT
    R.drawable.enemy_green_1, R.drawable.enemy_green_2,
    R.drawable.enemy_green_3, R.drawable.enemy_green_4,
    R.drawable.enemy_green_boss -> GREEN_ACCENT
    R.drawable.enemy_red_1, R.drawable.enemy_red_2,
    R.drawable.enemy_red_3, R.drawable.enemy_red_boss -> RED_ACCENT
    R.drawable.enemy_cross_1, R.drawable.enemy_cross_2,
    R.drawable.enemy_orb_1, R.drawable.enemy_orb_2 -> Color(0xFF7020CC)
    R.drawable.enemy_chevron_1, R.drawable.enemy_chevron_2,
    R.drawable.enemy_spike_1, R.drawable.enemy_spike_2 -> Color(0xFFCC5000)
    // Round 82 — accents for 10 R81 enemies.
    R.drawable.enemy_spinning_saw -> Color(0xFFFF2D55)
    R.drawable.enemy_tentacle_squid -> Color(0xFFFFD040)
    R.drawable.enemy_mine_layer -> Color(0xFFFF2D55)
    R.drawable.enemy_shield_drone -> Color(0xFF80E0FF)
    R.drawable.enemy_sniper -> Color(0xFF404040)
    R.drawable.enemy_bomber_crawler -> Color(0xFFFFE040)
    R.drawable.enemy_mirror_twin -> Color(0xFFFFFFFF)
    R.drawable.enemy_phantom -> Color(0xFFE8E8F0)
    R.drawable.enemy_healer -> Color(0xFFFFFFFF)
    R.drawable.enemy_kamikaze -> Color(0xFFFF6020)
    // Task 09 (đợt 3) — accent 5 địch chủ đề.
    R.drawable.enemy_splitter -> Color(0xFFFFF080)                   // gold sáng
    R.drawable.enemy_repulsor -> Color(0xFFF0A0FF)                   // magenta nhạt
    R.drawable.enemy_jammer -> Color(0xFFFF4040)                     // đỏ cảnh báo
    R.drawable.enemy_missileer -> Color(0xFFFFFFFF)                  // trắng mũi
    R.drawable.enemy_predator -> Color(0xFF40FFD0)                   // teal glow
    else -> Color(0xFF666666)
}

/**
 * Blend base body color with (a) white tint from hit flash and (b) per-effect
 * pulsing status color. Order of preference: status effects > hit flash > base.
 * Multiple status effects mix by averaging their ARGB values.
 */
private fun blendForFlash(
    base: Color,
    hitFlash: Float,
    statusTintsArgb: List<Long>,
    nowMillis: Long,
): Color {
    var result = base
    if (statusTintsArgb.isNotEmpty()) {
        val pulse = (0.50f + 0.35f * kotlin.math.sin(nowMillis / 160.0).toFloat()).coerceIn(0f, 1f)
        // Average the status colors then mix at pulse weight.
        var r = 0f; var g = 0f; var b = 0f
        statusTintsArgb.forEach { argb ->
            val c = Color(argb.toInt())
            r += c.red; g += c.green; b += c.blue
        }
        val n = statusTintsArgb.size
        val avg = Color(r / n, g / n, b / n, 1f)
        result = Color(
            red = result.red + (avg.red - result.red) * pulse,
            green = result.green + (avg.green - result.green) * pulse,
            blue = result.blue + (avg.blue - result.blue) * pulse,
            alpha = 1f,
        )
    }
    if (hitFlash > 0f) {
        result = Color(
            red = result.red + (1f - result.red) * hitFlash,
            green = result.green + (1f - result.green) * hitFlash,
            blue = result.blue + (1f - result.blue) * hitFlash,
            alpha = 1f,
        )
    }
    return result
}

/**
 * Dispatch table — pick shape recipe by drawableId. Each shape inscribed in
 * `(cx, cy)` ± `wPx × hPx`. Variant within family modulates a small detail
 * (rotation / vertex count) to break up monotony.
 */
/**
 * Wave 18 — shape THẬT của boss theo [kind], trích từ dispatch trong
 * [drawEnemyShape] để dùng chung: vừa render in-game vừa preview ở Bách Khoa
 * (trước Bách Khoa chỉ vẽ hình tròn → mọi boss "shape y chang nhau"). Exhaustive
 * over BossKind nên thêm boss mới buộc khai báo shape (compile error nếu sót).
 */
internal fun DrawScope.drawBossShapeByKind(
    kind: com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind,
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    when (kind) {
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.STAR ->
            drawBossSun(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.CROSS ->
            drawBossCross(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.ORB ->
            drawBossEye(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.FRACTAL ->
            drawBossAtom(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.SPIDER ->
            drawBossSpider(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.DEATH_MOON ->
            drawBossDeathMoon(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.HAUNTED_KID ->
            drawBossHauntedKid(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.HELL_LORD ->
            drawBossHellLord(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.SATAN_GLYPH ->
            drawBossSatanGlyph(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.HEN_MOTHER ->
            drawBossHenMother(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.BUFFALO_RAGE ->
            drawBossBuffalo(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.DUMB_RAT ->
            drawBossRat(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.FIERCE_TIGER ->
            drawBossTiger(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.SEXY_DIVA ->
            drawBossDiva(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.TROLL_TOWER ->
            drawBossTrollTower(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.TWIN_SUMMITS ->
            drawBossTwinSummits(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.VOID_GLOBES ->
            drawBossVoidGlobes(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.WHITE_DRAGON ->
            drawBossWhiteDragon(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.HAMMER_SICKLE ->
            drawBossHammerSickle(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.MONEY_TYCOON ->
            drawBossMoneyTycoon(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.GOLDEN_TYCOON ->
            drawBossGoldenTycoon(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.SKULL_CROSSBONES ->
            drawBossSkull(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.VAMPIRE ->
            drawBossVampire(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.COSMIC_CENTIPEDE ->
            drawBossCentipede(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.GIANT_CONDOM ->
            drawBossCondom(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.VENOM_SPIDER ->
            drawBossVenomSpider(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.CORRUPTION ->
            drawBossCorruption(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.TRAFFIC_JAM ->
            drawBossTrafficJam(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.KPI_BOSS ->
            drawBossKpi(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.TIKTOKER ->
            drawBossTiktoker(cx, cy, wPx, hPx, body, accent)
        // Wave 19 batch 2.
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.ATM_BANKRUPT ->
            drawBossAtm(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.NOKIA_BRICK ->
            drawBossNokia(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.INFLATION_STORM ->
            drawBossInflation(cx, cy, wPx, hPx, body, accent)
        // Wave 20 batch 3.
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.SOCIAL_DRAMA ->
            drawBossDrama(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.PYRAMID_SCHEME ->
            drawBossPyramid(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.FORTUNE_TELLER ->
            drawBossFortune(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.KITCHEN_GOD ->
            drawBossKitchenGod(cx, cy, wPx, hPx, body, accent)
        // Wave 21 batch 4.
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.CRYPTO_BRO ->
            drawBossCrypto(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.TOXIC_KID ->
            drawBossToxicKid(cx, cy, wPx, hPx, body, accent)
        // Wave 22 batch 5.
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.KARAOKE_BOSS ->
            drawBossKaraoke(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.FLASHY_TYCOON ->
            drawBossFlashy(cx, cy, wPx, hPx, body, accent)
        // Wave 23 batch 6.
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.DR_GOOGLE ->
            drawBossDrGoogle(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.CAT_EMPEROR ->
            drawBossCatEmperor(cx, cy, wPx, hPx, body, accent)
        // Wave 24 batch 7.
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.SALE_FANATIC ->
            drawBossSale(cx, cy, wPx, hPx, body, accent)
        com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.GHOST_MONTH ->
            drawBossGhostMonth(cx, cy, wPx, hPx, body, accent)
    }
}

private fun DrawScope.drawEnemyShape(
    drawableId: Int,
    cx: Float, cy: Float, wPx: Float, hPx: Float,
    body: Color, accent: Color, isBoss: Boolean,
    bossKind: com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind? = null,
) {
    // Round 71 (Issue 4d) — Boss dispatch via bossKind nếu boss, else family
    // dispatch theo drawableId.
    if (isBoss && bossKind != null) {
        drawBossShapeByKind(bossKind, cx, cy, wPx, hPx, body, accent)
        return
    }
    when (drawableId) {
        // Light blue family — Round 78 spec-follow-up: 5 fully distinct silhouettes
        // (HEART/TRIANGLE/CLUB/CIRCLE/SPADE). User: "cơ, chuồn, bích" = card suits.
        R.drawable.enemy_light_blue_1 -> drawHeart(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_light_blue_2 -> drawTriangle(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_light_blue_3 -> drawCardClub(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_light_blue_4 -> drawCircleEnemy(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_light_blue_5 -> drawCardSpade(cx, cy, wPx, hPx, body, accent)
        // Green family — Round 78: HEXAGON + VIRUS + EYE + CARD_DIAMOND.
        // User: "rô" = card diamond suit (vertical rhombus).
        R.drawable.enemy_green_1 -> drawHexagon(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_green_2 -> drawVirus(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_green_3 -> drawEye(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_green_4 -> drawCardDiamond(cx, cy, wPx, hPx, body, accent)
        // Red family — Round 78: DIAMOND + HEART (red) + CIRCLE.
        // Heart in red = playing-card "cơ" tribute. Circle covers user's "circle" spec.
        R.drawable.enemy_red_1 -> drawDiamond(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_red_2 -> drawHeart(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_red_3 -> drawCircleEnemy(cx, cy, wPx, hPx, body, accent)
        // Round 74 (R73d) + Round 80 (#0) — ELITE/BERSERKER variants. Variant 1
        // của mỗi family được remap sang "weird flying ship" silhouettes per user
        // feedback "share enemy thêm các shape là tàu bay kì quặc". Front faces
        // DOWN toward player → reads as face-to-face confrontation.
        // ELITE: cross + UFO (violet).
        R.drawable.enemy_cross_1 -> drawCross(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_cross_2 -> drawEnemyUFO(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_orb_1 -> drawOrb(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_orb_2 -> drawEnemyDrone(cx, cy, wPx, hPx, body, accent)
        // BERSERKER: chevron + MantaRay / Mech (orange).
        R.drawable.enemy_chevron_1 -> drawChevron(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_chevron_2 -> drawEnemyMantaRay(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_spike_1 -> drawSpike(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_spike_2 -> drawEnemyMech(cx, cy, wPx, hPx, body, accent)
        // Round 82 — 10 R81 new enemy in-game dispatch.
        R.drawable.enemy_spinning_saw -> drawEnemySpinningSaw(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_tentacle_squid -> drawEnemyTentacleSquid(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_mine_layer -> drawEnemyMineLayer(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_shield_drone -> drawEnemyShieldDrone(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_sniper -> drawEnemySniper(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_bomber_crawler -> drawEnemyBomberCrawler(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_mirror_twin -> drawEnemyMirrorTwin(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_phantom -> drawEnemyPhantom(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_healer -> drawEnemyHealer(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_kamikaze -> drawEnemyKamikaze(cx, cy, wPx, hPx, body, accent)
        // Task 09 (đợt 3) — 5 địch chủ đề, shape RIÊNG.
        R.drawable.enemy_splitter -> drawEnemySplitter(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_repulsor -> drawEnemyRepulsor(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_jammer -> drawEnemyJammer(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_missileer -> drawEnemyMissileer(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_predator -> drawEnemyPredator(cx, cy, wPx, hPx, body, accent)
        // Boss drawables fallback (bossKind null — defensive).
        R.drawable.enemy_green_boss, R.drawable.enemy_red_boss ->
            drawBossStar(cx, cy, wPx, hPx, body, accent)
        else -> drawHexagon(cx, cy, wPx, hPx, body, accent, variant = 0)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Round 74 (R73d) — Wave 9a: 4 new shape recipes (2 variant mỗi).
// ─────────────────────────────────────────────────────────────────────────

/** ELITE: 4-arm cross with center disc + tip glow. variant changes arm length. */
private fun DrawScope.drawCross(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color, variant: Int,
) {
    val armWFactor = 0.20f + variant * 0.05f
    val armW = minOf(wPx, hPx) * armWFactor
    val armLen = minOf(wPx, hPx) * (0.40f + variant * 0.05f)
    val centerR = minOf(wPx, hPx) * 0.20f
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armW / 2, cy - armLen),
        size = androidx.compose.ui.geometry.Size(armW, armLen * 2))
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armLen, cy - armW / 2),
        size = androidx.compose.ui.geometry.Size(armLen * 2, armW))
    drawCircle(body, centerR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, centerR * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
    // Tip dots
    drawCircle(accent, armW * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy - armLen))
    drawCircle(accent, armW * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy + armLen))
    drawCircle(accent, armW * 0.55f, androidx.compose.ui.geometry.Offset(cx - armLen, cy))
    drawCircle(accent, armW * 0.55f, androidx.compose.ui.geometry.Offset(cx + armLen, cy))
}

/** ELITE: Floating orb with 1 or 2 orbital rings. variant=0 single, variant=1 double. */
private fun DrawScope.drawOrb(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color, variant: Int,
) {
    val r = minOf(wPx, hPx) * 0.40f
    drawCircle(body, r, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, r * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.22f, androidx.compose.ui.geometry.Offset(cx, cy))
    // Orbital ring(s)
    drawCircle(
        color = accent,
        radius = r * 1.20f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = wPx * 0.05f),
    )
    if (variant == 1) {
        drawCircle(
            color = accent.copy(alpha = 0.6f),
            radius = r * 1.45f,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = wPx * 0.035f),
        )
    }
}

/** BERSERKER: Chevron arrow pointing DOWN (toward player). variant=0 single, variant=1 double-stack. */
private fun DrawScope.drawChevron(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color, variant: Int,
) {
    val halfW = wPx * 0.42f
    val drawOne: (Float) -> Unit = { yShift ->
        val path = PathPool.acquire().apply {
            moveTo(cx - halfW, cy - hPx * 0.25f + yShift)
            lineTo(cx, cy + hPx * 0.20f + yShift)
            lineTo(cx + halfW, cy - hPx * 0.25f + yShift)
            lineTo(cx + halfW * 0.7f, cy - hPx * 0.30f + yShift)
            lineTo(cx, cy + hPx * 0.10f + yShift)
            lineTo(cx - halfW * 0.7f, cy - hPx * 0.30f + yShift)
            close()
        }
        drawPath(path, body)
    }
    drawOne(0f)
    if (variant == 1) drawOne(hPx * 0.35f)
    // Pip center
    drawCircle(accent, wPx * 0.07f, androidx.compose.ui.geometry.Offset(cx, cy))
}

/** BERSERKER: Spiked star — 8 pointy spikes radiating. variant changes spike length. */
private fun DrawScope.drawSpike(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color, variant: Int,
) {
    val outerR = minOf(wPx, hPx) * (0.42f + variant * 0.05f)
    val innerR = outerR * 0.40f
    val spikes = 8
    val path = PathPool.acquire().apply {
        for (i in 0 until spikes * 2) {
            val angle = -Math.PI / 2 + i * Math.PI / spikes
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, body)
    drawPath(path, accent,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = wPx * 0.04f))
    PathPool.release(path)
    drawCircle(accent, innerR * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
}

// ─────────────────────────────────────────────────────────────────────────
// Round 71 (Issue 4d) — 4 new boss silhouettes (STAR đã có).
// ─────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawBossCross(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    // 4-arm cross spinner (LevelTwoBoss). Center disc + 4 arms với hollow tip.
    val armW = minOf(wPx, hPx) * 0.18f
    val armLen = minOf(wPx, hPx) * 0.5f
    val centerR = minOf(wPx, hPx) * 0.25f
    // Vertical arm
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armW / 2, cy - armLen),
        size = androidx.compose.ui.geometry.Size(armW, armLen * 2),
    )
    // Horizontal arm
    drawRect(body,
        topLeft = androidx.compose.ui.geometry.Offset(cx - armLen, cy - armW / 2),
        size = androidx.compose.ui.geometry.Size(armLen * 2, armW),
    )
    // Center disc
    drawCircle(body, centerR, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, centerR * 0.5f, androidx.compose.ui.geometry.Offset(cx, cy))
    // Tip circles
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx, cy - armLen))
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx, cy + armLen))
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx - armLen, cy))
    drawCircle(accent, armW * 0.6f, androidx.compose.ui.geometry.Offset(cx + armLen, cy))
}

private fun DrawScope.drawBossOrb(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    // Large orb + 3 ring satellites (MidBoss OFFENSIVE).
    val r = minOf(wPx, hPx) * 0.4f
    drawCircle(body, r, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(accent, r * 0.55f, androidx.compose.ui.geometry.Offset(cx, cy))
    drawCircle(
        color = body,
        radius = r * 0.85f,
        center = androidx.compose.ui.geometry.Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.08f),
    )
    // 3 orbiting satellite discs
    for (i in 0 until 3) {
        val a = i * 120.0 * Math.PI / 180.0
        val sx = cx + (r * 1.15f * kotlin.math.cos(a)).toFloat()
        val sy = cy + (r * 1.15f * kotlin.math.sin(a)).toFloat()
        drawCircle(accent, r * 0.18f, androidx.compose.ui.geometry.Offset(sx, sy))
    }
}

private fun DrawScope.drawBossFractal(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    // Recursive triangle (MidBoss DEFENSIVE / SWARM).
    val r = minOf(wPx, hPx) * 0.45f
    // Outer triangle pointing DOWN (toward player)
    val outer = PathPool.acquire().apply {
        moveTo(cx, cy + r)
        lineTo(cx - r * 0.866f, cy - r * 0.5f)
        lineTo(cx + r * 0.866f, cy - r * 0.5f)
        close()
    }
    drawPath(outer, body)
    drawPath(outer, accent, style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.08f))
    PathPool.release(outer)
    // Inner upward triangle (Sierpinski child)
    val innerR = r * 0.5f
    val inner = PathPool.acquire().apply {
        moveTo(cx, cy - innerR)
        lineTo(cx - innerR * 0.866f, cy + innerR * 0.5f)
        lineTo(cx + innerR * 0.866f, cy + innerR * 0.5f)
        close()
    }
    drawPath(inner, accent)
    PathPool.release(inner)
    // Center dot
    drawCircle(body, r * 0.15f, androidx.compose.ui.geometry.Offset(cx, cy))
}

private fun DrawScope.drawBossSpider(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    // 8 legs radiating from oval body (FinalBoss).
    val bodyR = minOf(wPx, hPx) * 0.3f
    val legLen = minOf(wPx, hPx) * 0.55f
    val legW = bodyR * 0.18f
    // 8 legs at 45° spacing
    for (i in 0 until 8) {
        val a = i * 45.0 * Math.PI / 180.0
        val ex = cx + (legLen * kotlin.math.cos(a)).toFloat()
        val ey = cy + (legLen * kotlin.math.sin(a)).toFloat()
        drawLine(
            color = body,
            start = androidx.compose.ui.geometry.Offset(cx, cy),
            end = androidx.compose.ui.geometry.Offset(ex, ey),
            strokeWidth = legW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Joint glow
        drawCircle(accent, legW * 0.6f, androidx.compose.ui.geometry.Offset(ex, ey))
    }
    // Body — oval shape (use draw circle with scaled = horizontal)
    drawCircle(body, bodyR, androidx.compose.ui.geometry.Offset(cx, cy))
    // 2 "eyes" indicating it's looking
    drawCircle(accent, bodyR * 0.18f,
        androidx.compose.ui.geometry.Offset(cx - bodyR * 0.4f, cy - bodyR * 0.1f))
    drawCircle(accent, bodyR * 0.18f,
        androidx.compose.ui.geometry.Offset(cx + bodyR * 0.4f, cy - bodyR * 0.1f))
}

// ─────────── Wave 15 batch 1 — 3 boss user nêu đích danh ───────────

/** Đầu lâu + xương chéo (X) phía sau. */
private fun DrawScope.drawBossSkull(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val r = minOf(wPx, hPx) * 0.30f
    val black = Color.Black.copy(alpha = 0.85f)
    // Crossbones (X) phía sau — 2 thanh chéo + đầu xương tròn.
    val half = minOf(wPx, hPx) * 0.31f
    val diag = half * 0.7071f
    val boneW = r * 0.24f
    val bones = listOf(
        Offset(cx - diag, cy - diag) to Offset(cx + diag, cy + diag),
        Offset(cx - diag, cy + diag) to Offset(cx + diag, cy - diag),
    )
    bones.forEach { (s, e) ->
        drawLine(accent, s, e, strokeWidth = boneW,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(accent, boneW * 0.85f, s)
        drawCircle(accent, boneW * 0.85f, e)
    }
    val sx = cx; val sy = cy - r * 0.10f
    // Sọ (cranium) + hàm (trapezoid).
    drawCircle(body, r, Offset(sx, sy))
    val jaw = PathPool.acquire().apply {
        moveTo(sx - r * 0.55f, sy + r * 0.50f)
        lineTo(sx + r * 0.55f, sy + r * 0.50f)
        lineTo(sx + r * 0.34f, sy + r * 1.04f)
        lineTo(sx - r * 0.34f, sy + r * 1.04f)
        close()
    }
    drawPath(jaw, body)
    PathPool.release(jaw)
    // Hốc mắt + đốm sáng accent.
    val eyeR = r * 0.27f
    for (sgn in listOf(-1f, 1f)) {
        val ex = sx + sgn * r * 0.40f
        val ey = sy - r * 0.05f
        drawCircle(black, eyeR, Offset(ex, ey))
        drawCircle(accent, eyeR * 0.42f, Offset(ex, ey))
    }
    // Mũi (tam giác đen).
    val nose = PathPool.acquire().apply {
        moveTo(sx, sy + r * 0.12f)
        lineTo(sx - r * 0.14f, sy + r * 0.44f)
        lineTo(sx + r * 0.14f, sy + r * 0.44f)
        close()
    }
    drawPath(nose, black)
    PathPool.release(nose)
    // Răng (3 khe dọc).
    for (i in -1..1) {
        val tx = sx + i * r * 0.27f
        drawLine(black, Offset(tx, sy + r * 0.58f), Offset(tx, sy + r * 1.00f),
            strokeWidth = r * 0.07f)
    }
}

/** Ma cà rồng — 2 cánh dơi + đầu tối + mắt đỏ + 2 răng nanh trắng. */
private fun DrawScope.drawBossVampire(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val r = minOf(wPx, hPx) * 0.26f
    val span = minOf(wPx, hPx) * 0.52f
    // 2 cánh dơi (scallop kép) — mirror qua tâm.
    for (sgn in listOf(-1f, 1f)) {
        val wing = PathPool.acquire().apply {
            moveTo(cx + sgn * r * 0.35f, cy - r * 0.25f)
            lineTo(cx + sgn * span, cy - r * 0.55f)
            lineTo(cx + sgn * span * 0.82f, cy + r * 0.10f)
            lineTo(cx + sgn * span * 0.60f, cy - r * 0.05f)
            lineTo(cx + sgn * span * 0.66f, cy + r * 0.55f)
            lineTo(cx + sgn * span * 0.38f, cy + r * 0.20f)
            lineTo(cx + sgn * r * 0.30f, cy + r * 0.40f)
            close()
        }
        drawPath(wing, body)
        drawPath(wing, accent, style = Stroke(width = wPx * 0.012f))
        PathPool.release(wing)
    }
    // Đầu/thân tròn tối.
    drawCircle(body, r, Offset(cx, cy))
    drawCircle(accent, r, Offset(cx, cy), style = Stroke(width = wPx * 0.018f))
    // 2 mắt đỏ rực.
    for (sgn in listOf(-1f, 1f)) {
        drawCircle(accent, r * 0.24f, Offset(cx + sgn * r * 0.40f, cy - r * 0.08f))
        drawCircle(Color.White, r * 0.07f, Offset(cx + sgn * r * 0.46f, cy - r * 0.14f))
    }
    // 2 răng nanh trắng (tam giác chỉ xuống).
    for (sgn in listOf(-1f, 1f)) {
        val fx = cx + sgn * r * 0.18f
        val fang = PathPool.acquire().apply {
            moveTo(fx - r * 0.08f, cy + r * 0.38f)
            lineTo(fx + r * 0.08f, cy + r * 0.38f)
            lineTo(fx, cy + r * 0.88f)
            close()
        }
        drawPath(fang, Color.White)
        PathPool.release(fang)
    }
}

/** Con rết vũ trụ — đầu + chuỗi đốt thân uốn lượn + chân + râu + càng. */
private fun DrawScope.drawBossCentipede(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val seg = unit * 0.12f
    val n = 6
    val spanH = unit * 0.74f
    val topY = cy - spanH * 0.5f
    val stepY = spanH / (n + 1)
    val sway = unit * 0.13f
    // Chuỗi đốt thân (uốn sin) + chân 2 bên mỗi đốt.
    for (i in 1..n) {
        val segY = topY + stepY * i
        val segX = cx + sway * sin(i.toDouble()).toFloat()
        for (sgn in listOf(-1f, 1f)) {
            drawLine(
                accent,
                Offset(segX + sgn * seg, segY),
                Offset(segX + sgn * seg * 2.0f, segY - seg * 0.55f),
                strokeWidth = seg * 0.20f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
        drawCircle(body, seg, Offset(segX, segY))
        drawCircle(accent, seg, Offset(segX, segY), style = Stroke(width = wPx * 0.016f))
    }
    // Đầu (to hơn) ở trên cùng — mắt + râu + càng.
    val headR = seg * 1.35f
    val hx = cx + sway * sin(0.0).toFloat()
    val hy = topY
    drawCircle(body, headR, Offset(hx, hy))
    for (sgn in listOf(-1f, 1f)) {
        drawCircle(accent, headR * 0.30f, Offset(hx + sgn * headR * 0.42f, hy - headR * 0.10f))
        // Râu.
        drawLine(accent,
            Offset(hx + sgn * headR * 0.30f, hy - headR * 0.70f),
            Offset(hx + sgn * headR * 0.95f, hy - headR * 1.65f),
            strokeWidth = seg * 0.16f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Càng (mandible).
        drawLine(accent,
            Offset(hx + sgn * headR * 0.30f, hy + headR * 0.70f),
            Offset(hx + sgn * headR * 0.85f, hy + headR * 1.30f),
            strokeWidth = seg * 0.22f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

// ─────────── Wave 16 batch 2 — 3 boss user nêu đích danh (nốt) ───────────

/** Bao cao su khổng lồ — túi phình + núm chứa trên đỉnh + vòng cuộn ở đáy. */
private fun DrawScope.drawBossCondom(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val bodyR = unit * 0.34f
    val bcy = cy + unit * 0.05f
    drawCircle(body, bodyR, Offset(cx, bcy))
    drawCircle(accent, bodyR, Offset(cx, bcy), style = Stroke(width = unit * 0.035f))
    // Núm chứa (reservoir tip) trên đỉnh.
    drawCircle(body, unit * 0.10f, Offset(cx, bcy - bodyR + unit * 0.01f))
    drawCircle(accent, unit * 0.10f, Offset(cx, bcy - bodyR + unit * 0.01f), style = Stroke(width = unit * 0.03f))
    // Vòng cuộn ở miệng (đáy) — ellipse stroke.
    drawOval(
        color = accent,
        topLeft = Offset(cx - bodyR * 0.85f, bcy + bodyR * 0.7f),
        size = Size(bodyR * 1.7f, unit * 0.16f),
        style = Stroke(width = unit * 0.06f),
    )
    // Sheen highlight.
    drawCircle(Color.White.copy(alpha = 0.5f), bodyR * 0.16f, Offset(cx - bodyR * 0.35f, bcy - bodyR * 0.25f))
}

/** Nhện Venom — 8 chân + thân 2 đốt + dấu độc + mắt đỏ + nanh trắng. */
private fun DrawScope.drawBossVenomSpider(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val bodyR = unit * 0.24f
    val legLen = unit * 0.50f
    val capR = androidx.compose.ui.graphics.StrokeCap.Round
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val ex = cx + (legLen * cos(a)).toFloat()
        val ey = cy + (legLen * sin(a)).toFloat()
        drawLine(body, Offset(cx, cy), Offset(ex, ey), strokeWidth = bodyR * 0.18f, cap = capR)
        drawCircle(accent, bodyR * 0.10f, Offset(ex, ey))
    }
    // Thân: đầu-ngực + bụng.
    drawCircle(body, bodyR, Offset(cx, cy))
    drawCircle(body, bodyR * 0.75f, Offset(cx, cy + bodyR * 0.95f))
    drawCircle(accent, bodyR * 0.32f, Offset(cx, cy + bodyR * 0.95f))   // dấu độc
    // Mắt đỏ.
    for (s in listOf(-1f, 1f)) {
        drawCircle(accent, bodyR * 0.18f, Offset(cx + s * bodyR * 0.42f, cy - bodyR * 0.2f))
    }
    // Nanh.
    for (s in listOf(-1f, 1f)) {
        val fx = cx + s * bodyR * 0.2f
        val fang = PathPool.acquire().apply {
            moveTo(fx - bodyR * 0.06f, cy + bodyR * 0.42f)
            lineTo(fx + bodyR * 0.06f, cy + bodyR * 0.42f)
            lineTo(fx, cy + bodyR * 0.82f)
            close()
        }
        drawPath(fang, Color.White)
        PathPool.release(fang)
    }
}

/** Tham Nhũng — túi tiền béo: thân phình + cổ thắt + ký hiệu $ + 2 mắt tham. */
private fun DrawScope.drawBossCorruption(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val r = unit * 0.32f
    val sack = PathPool.acquire().apply {
        moveTo(cx - r * 0.4f, cy - r * 0.5f)
        cubicTo(cx - r * 1.1f, cy, cx - r * 0.9f, cy + r * 0.95f, cx, cy + r)
        cubicTo(cx + r * 0.9f, cy + r * 0.95f, cx + r * 1.1f, cy, cx + r * 0.4f, cy - r * 0.5f)
        close()
    }
    drawPath(sack, body)
    drawPath(sack, accent, style = Stroke(width = unit * 0.03f))
    PathPool.release(sack)
    // Cổ thắt.
    drawLine(
        accent, Offset(cx - r * 0.45f, cy - r * 0.5f), Offset(cx + r * 0.45f, cy - r * 0.5f),
        strokeWidth = unit * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
    // Ký hiệu $ trên bụng.
    val by = cy + r * 0.25f
    drawLine(accent, Offset(cx, by - r * 0.42f), Offset(cx, by + r * 0.42f), strokeWidth = unit * 0.04f)
    drawArc(
        color = accent, startAngle = 50f, sweepAngle = 250f, useCenter = false,
        topLeft = Offset(cx - r * 0.24f, by - r * 0.38f), size = Size(r * 0.48f, r * 0.36f),
        style = Stroke(width = unit * 0.045f),
    )
    drawArc(
        color = accent, startAngle = 230f, sweepAngle = 250f, useCenter = false,
        topLeft = Offset(cx - r * 0.24f, by - r * 0.02f), size = Size(r * 0.48f, r * 0.36f),
        style = Stroke(width = unit * 0.045f),
    )
    // 2 mắt tham.
    for (s in listOf(-1f, 1f)) {
        drawCircle(accent, r * 0.10f, Offset(cx + s * r * 0.26f, cy - r * 0.38f))
    }
}

// ─────────── Wave 18 batch 1 — 3 boss trào phúng ───────────

/** Trùm Kẹt Xe — chiếc ô tô: thân + nóc + 2 bánh + 2 đèn pha. */
private fun DrawScope.drawBossTrafficJam(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val bw = unit * 0.62f
    val bh = unit * 0.30f
    // Thân xe.
    drawRect(body, topLeft = Offset(cx - bw / 2f, cy - bh / 2f), size = Size(bw, bh))
    drawRect(accent, topLeft = Offset(cx - bw / 2f, cy - bh / 2f), size = Size(bw, bh),
        style = Stroke(width = unit * 0.03f))
    // Nóc/cabin (hình thang đơn giản bằng path).
    val roof = PathPool.acquire().apply {
        moveTo(cx - bw * 0.26f, cy - bh / 2f)
        lineTo(cx - bw * 0.14f, cy - bh / 2f - unit * 0.16f)
        lineTo(cx + bw * 0.14f, cy - bh / 2f - unit * 0.16f)
        lineTo(cx + bw * 0.26f, cy - bh / 2f)
        close()
    }
    drawPath(roof, body)
    drawPath(roof, accent, style = Stroke(width = unit * 0.025f))
    PathPool.release(roof)
    // 2 bánh.
    for (s in listOf(-1f, 1f)) {
        drawCircle(accent, unit * 0.09f, Offset(cx + s * bw * 0.3f, cy + bh / 2f))
    }
    // 2 đèn pha (phía dưới = hướng tấn công).
    for (s in listOf(-1f, 1f)) {
        drawCircle(accent, unit * 0.05f, Offset(cx + s * bw * 0.4f, cy + bh * 0.2f))
    }
}

/** Sếp KPI — biểu đồ cột tăng dần + mũi tên đi lên. */
private fun DrawScope.drawBossKpi(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val baseY = cy + unit * 0.28f
    val barW = unit * 0.14f
    val heights = listOf(0.20f, 0.34f, 0.48f)            // cột tăng dần
    heights.forEachIndexed { i, hf ->
        val bx = cx + (i - 1) * (barW + unit * 0.06f)
        val bh = unit * hf
        drawRect(body, topLeft = Offset(bx - barW / 2f, baseY - bh), size = Size(barW, bh))
        drawRect(accent, topLeft = Offset(bx - barW / 2f, baseY - bh), size = Size(barW, bh),
            style = Stroke(width = unit * 0.02f))
    }
    // Trục đáy.
    drawLine(accent, Offset(cx - unit * 0.34f, baseY), Offset(cx + unit * 0.34f, baseY),
        strokeWidth = unit * 0.03f)
    // Mũi tên đi lên (KPI tăng) phía trên cột cao nhất.
    val ax = cx + (barW + unit * 0.06f)
    val ay = baseY - unit * 0.48f - unit * 0.04f
    val arrow = PathPool.acquire().apply {
        moveTo(ax, ay - unit * 0.12f)
        lineTo(ax - unit * 0.09f, ay + unit * 0.02f)
        lineTo(ax + unit * 0.09f, ay + unit * 0.02f)
        close()
    }
    drawPath(arrow, accent)
    PathPool.release(arrow)
}

/** Hot TikToker — đèn ring (vòng) + trái tim + điện thoại nhỏ. */
private fun DrawScope.drawBossTiktoker(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    // Đèn ring (vòng tròn rỗng).
    drawCircle(body, unit * 0.34f, Offset(cx, cy), style = Stroke(width = unit * 0.07f))
    drawCircle(accent, unit * 0.34f, Offset(cx, cy), style = Stroke(width = unit * 0.02f))
    // Trái tim ở giữa.
    val r = unit * 0.16f
    val heart = PathPool.acquire().apply {
        moveTo(cx, cy + r * 0.9f)
        cubicTo(cx - r * 1.4f, cy - r * 0.2f, cx - r * 0.5f, cy - r * 1.1f, cx, cy - r * 0.35f)
        cubicTo(cx + r * 0.5f, cy - r * 1.1f, cx + r * 1.4f, cy - r * 0.2f, cx, cy + r * 0.9f)
        close()
    }
    drawPath(heart, accent)
    PathPool.release(heart)
    // Điện thoại nhỏ góc dưới (đang livestream).
    val pw = unit * 0.12f
    val ph = unit * 0.20f
    drawRect(accent, topLeft = Offset(cx + unit * 0.18f, cy + unit * 0.16f), size = Size(pw, ph),
        style = Stroke(width = unit * 0.02f))
}

// ─────────── Wave 19 batch 2 — 3 boss trào phúng (nốt) ───────────

/** ATM Hết Tiền — máy ATM: thân + màn hình + khe thẻ + bàn phím + ký hiệu $. */
private fun DrawScope.drawBossAtm(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val bw = unit * 0.5f
    val bh = unit * 0.62f
    // Thân máy.
    drawRect(body, topLeft = Offset(cx - bw / 2f, cy - bh / 2f), size = Size(bw, bh))
    drawRect(accent, topLeft = Offset(cx - bw / 2f, cy - bh / 2f), size = Size(bw, bh),
        style = Stroke(width = unit * 0.03f))
    // Màn hình (trên).
    drawRect(accent, topLeft = Offset(cx - bw * 0.32f, cy - bh * 0.40f), size = Size(bw * 0.64f, bh * 0.26f),
        style = Stroke(width = unit * 0.02f))
    // Ký hiệu $ trên màn hình.
    drawLine(accent, Offset(cx, cy - bh * 0.40f), Offset(cx, cy - bh * 0.14f), strokeWidth = unit * 0.025f)
    // Khe thẻ (giữa).
    drawLine(accent, Offset(cx - bw * 0.26f, cy + bh * 0.02f), Offset(cx + bw * 0.26f, cy + bh * 0.02f),
        strokeWidth = unit * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // Bàn phím 2×3 (dưới).
    for (r in 0..1) for (c in 0..2) {
        drawCircle(accent, unit * 0.022f,
            Offset(cx + (c - 1) * bw * 0.22f, cy + bh * 0.18f + r * bh * 0.14f))
    }
}

/** Cục Gạch Nokia — điện thoại "cục gạch": thân bo + màn hình + bàn phím + ăng-ten. */
private fun DrawScope.drawBossNokia(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val bw = unit * 0.4f
    val bh = unit * 0.66f
    val corner = androidx.compose.ui.geometry.CornerRadius(unit * 0.08f)
    // Thân máy (bo góc).
    drawRoundRect(body, topLeft = Offset(cx - bw / 2f, cy - bh / 2f), size = Size(bw, bh), cornerRadius = corner)
    drawRoundRect(accent, topLeft = Offset(cx - bw / 2f, cy - bh / 2f), size = Size(bw, bh),
        cornerRadius = corner, style = Stroke(width = unit * 0.03f))
    // Màn hình.
    drawRect(accent, topLeft = Offset(cx - bw * 0.3f, cy - bh * 0.38f), size = Size(bw * 0.6f, bh * 0.22f),
        style = Stroke(width = unit * 0.02f))
    // Bàn phím 3×3.
    for (r in 0..2) for (c in 0..2) {
        drawCircle(accent, unit * 0.02f,
            Offset(cx + (c - 1) * bw * 0.26f, cy + bh * 0.02f + r * bh * 0.15f))
    }
    // Ăng-ten cụt trên đỉnh.
    drawLine(accent, Offset(cx + bw * 0.3f, cy - bh / 2f), Offset(cx + bw * 0.3f, cy - bh * 0.62f),
        strokeWidth = unit * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
}

/** Bão Giá Lạm Phát — mũi tên ĐI LÊN to + thẻ giá (% lạm phát). */
private fun DrawScope.drawBossInflation(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val r = unit * 0.36f
    // Mũi tên đi lên (thân + đầu) = giá tăng.
    drawLine(body, Offset(cx, cy + r * 0.7f), Offset(cx, cy - r * 0.4f),
        strokeWidth = unit * 0.12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    val head = PathPool.acquire().apply {
        moveTo(cx, cy - r * 0.85f)
        lineTo(cx - r * 0.42f, cy - r * 0.25f)
        lineTo(cx + r * 0.42f, cy - r * 0.25f)
        close()
    }
    drawPath(head, body)
    drawPath(head, accent, style = Stroke(width = unit * 0.025f))
    PathPool.release(head)
    // Thẻ giá nhỏ (góc) + lỗ treo.
    val tw = unit * 0.26f
    val th = unit * 0.18f
    val tag = PathPool.acquire().apply {
        moveTo(cx + r * 0.5f, cy + r * 0.15f)
        lineTo(cx + r * 0.5f + tw, cy + r * 0.15f)
        lineTo(cx + r * 0.5f + tw, cy + r * 0.15f + th)
        lineTo(cx + r * 0.5f + th * 0.5f, cy + r * 0.15f + th)
        close()
    }
    drawPath(tag, accent)
    PathPool.release(tag)
    drawCircle(body, unit * 0.022f, Offset(cx + r * 0.5f + th * 0.5f, cy + r * 0.15f + th * 0.35f))
}

// ─────────── Wave 20 batch 3 — 4 boss trào phúng (hết batch 1) ───────────

/** Drama MXH — bong bóng chat + dấu "!" (drama bùng). */
private fun DrawScope.drawBossDrama(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val bw = unit * 0.62f
    val bh = unit * 0.46f
    val corner = androidx.compose.ui.geometry.CornerRadius(unit * 0.14f)
    // Thân bong bóng.
    drawRoundRect(body, topLeft = Offset(cx - bw / 2f, cy - bh / 2f - unit * 0.05f),
        size = Size(bw, bh), cornerRadius = corner)
    drawRoundRect(accent, topLeft = Offset(cx - bw / 2f, cy - bh / 2f - unit * 0.05f),
        size = Size(bw, bh), cornerRadius = corner, style = Stroke(width = unit * 0.03f))
    // Đuôi bong bóng (tam giác dưới-trái).
    val tail = PathPool.acquire().apply {
        moveTo(cx - bw * 0.22f, cy + bh / 2f - unit * 0.05f)
        lineTo(cx - bw * 0.34f, cy + bh / 2f + unit * 0.14f)
        lineTo(cx - bw * 0.04f, cy + bh / 2f - unit * 0.05f)
        close()
    }
    drawPath(tail, body)
    PathPool.release(tail)
    // Dấu "!" giữa bong bóng.
    val ey = cy - unit * 0.05f
    drawLine(accent, Offset(cx, ey - bh * 0.28f), Offset(cx, ey + bh * 0.08f), strokeWidth = unit * 0.05f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawCircle(accent, unit * 0.03f, Offset(cx, ey + bh * 0.24f))
}

/** Trùm Đa Cấp — kim tự tháp 3 tầng + $ trên đỉnh (mô hình tuyến dưới). */
private fun DrawScope.drawBossPyramid(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val r = unit * 0.4f
    val tri = PathPool.acquire().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r * 0.95f, cy + r * 0.7f)
        lineTo(cx - r * 0.95f, cy + r * 0.7f)
        close()
    }
    drawPath(tri, body)
    drawPath(tri, accent, style = Stroke(width = unit * 0.03f))
    PathPool.release(tri)
    // 2 đường tầng ngang.
    drawLine(accent, Offset(cx - r * 0.32f, cy - r * 0.05f), Offset(cx + r * 0.32f, cy - r * 0.05f),
        strokeWidth = unit * 0.025f)
    drawLine(accent, Offset(cx - r * 0.64f, cy + r * 0.32f), Offset(cx + r * 0.64f, cy + r * 0.32f),
        strokeWidth = unit * 0.025f)
    // $ trên đỉnh.
    drawLine(accent, Offset(cx, cy - r * 0.85f), Offset(cx, cy - r * 0.4f), strokeWidth = unit * 0.022f)
}

/** Thầy Bói Online — quả cầu pha lê (vòng) + ngôi sao bên trong + chân đế. */
private fun DrawScope.drawBossFortune(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val rad = unit * 0.32f
    // Quả cầu.
    drawCircle(body.copy(alpha = 0.55f), rad, Offset(cx, cy - unit * 0.04f))
    drawCircle(accent, rad, Offset(cx, cy - unit * 0.04f), style = Stroke(width = unit * 0.03f))
    // Chân đế (hình thang dưới).
    val base = PathPool.acquire().apply {
        moveTo(cx - rad * 0.5f, cy + rad * 0.85f)
        lineTo(cx + rad * 0.5f, cy + rad * 0.85f)
        lineTo(cx + rad * 0.7f, cy + rad * 1.15f)
        lineTo(cx - rad * 0.7f, cy + rad * 1.15f)
        close()
    }
    drawPath(base, accent)
    PathPool.release(base)
    // Ngôi sao 4 cánh (tia tiên tri) bên trong.
    val sy = cy - unit * 0.04f
    for (a in 0 until 4) {
        val ang = Math.toRadians((a * 90.0))
        drawLine(accent,
            Offset(cx, sy),
            Offset(cx + (kotlin.math.cos(ang) * rad * 0.6).toFloat(), sy + (kotlin.math.sin(ang) * rad * 0.6).toFloat()),
            strokeWidth = unit * 0.02f)
    }
}

/** Ông Táo Cưỡi Cá Chép — cá chép (thân + đuôi) + mũ cánh chuồn + lửa nhỏ. */
private fun DrawScope.drawBossKitchenGod(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val r = unit * 0.34f
    // Thân cá (ellipse ~ dùng path cong).
    val fish = PathPool.acquire().apply {
        moveTo(cx - r, cy + r * 0.3f)
        cubicTo(cx - r * 0.4f, cy - r * 0.5f, cx + r * 0.4f, cy - r * 0.5f, cx + r, cy + r * 0.3f)
        cubicTo(cx + r * 0.4f, cy + r * 0.9f, cx - r * 0.4f, cy + r * 0.9f, cx - r, cy + r * 0.3f)
        close()
    }
    drawPath(fish, body)
    drawPath(fish, accent, style = Stroke(width = unit * 0.03f))
    PathPool.release(fish)
    // Đuôi cá (tam giác trái).
    val tail = PathPool.acquire().apply {
        moveTo(cx - r, cy + r * 0.3f)
        lineTo(cx - r * 1.4f, cy)
        lineTo(cx - r * 1.4f, cy + r * 0.7f)
        close()
    }
    drawPath(tail, body)
    PathPool.release(tail)
    // Mắt cá.
    drawCircle(accent, unit * 0.03f, Offset(cx + r * 0.45f, cy + r * 0.15f))
    // Mũ Ông Táo (cánh chuồn) trên lưng cá.
    drawLine(accent, Offset(cx - r * 0.3f, cy - r * 0.45f), Offset(cx + r * 0.3f, cy - r * 0.45f),
        strokeWidth = unit * 0.05f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawCircle(accent, unit * 0.05f, Offset(cx, cy - r * 0.6f))
}

// ─────────── Wave 21 batch 4 — 2 boss trào phúng (batch 2 mở màn) ───────────

/** Ông Chú Crypto — đồng coin tròn + ký hiệu ₿ + mũi tên biến động nhỏ. */
private fun DrawScope.drawBossCrypto(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val rad = unit * 0.34f
    // Đồng xu.
    drawCircle(body, rad, Offset(cx, cy))
    drawCircle(accent, rad, Offset(cx, cy), style = Stroke(width = unit * 0.04f))
    // Ký hiệu ₿ (thân dọc + 2 bụng + 2 gạch trên/dưới).
    drawLine(accent, Offset(cx - rad * 0.12f, cy - rad * 0.5f), Offset(cx - rad * 0.12f, cy + rad * 0.5f),
        strokeWidth = unit * 0.045f)
    for (yy in listOf(-0.22f, 0.22f)) {
        drawArc(
            color = accent, startAngle = -90f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(cx - rad * 0.12f, cy + rad * yy - rad * 0.22f),
            size = Size(rad * 0.42f, rad * 0.44f), style = Stroke(width = unit * 0.04f),
        )
    }
    // 2 gạch nhô (chân/đầu chữ ₿).
    for (yy in listOf(-0.62f, 0.62f)) {
        drawLine(accent, Offset(cx - rad * 0.04f, cy + rad * yy), Offset(cx - rad * 0.04f, cy + rad * (yy * 0.7f)),
            strokeWidth = unit * 0.035f)
    }
}

/** Trẻ Trâu Toxic — mặt giận: đầu tròn + 2 chân mày chéo + miệng cau + "!!". */
private fun DrawScope.drawBossToxicKid(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val rad = unit * 0.34f
    // Đầu.
    drawCircle(body, rad, Offset(cx, cy))
    drawCircle(accent, rad, Offset(cx, cy), style = Stroke(width = unit * 0.03f))
    // 2 chân mày chéo (giận dữ) ‾\ /‾.
    drawLine(accent, Offset(cx - rad * 0.55f, cy - rad * 0.18f), Offset(cx - rad * 0.12f, cy - rad * 0.02f),
        strokeWidth = unit * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawLine(accent, Offset(cx + rad * 0.55f, cy - rad * 0.18f), Offset(cx + rad * 0.12f, cy - rad * 0.02f),
        strokeWidth = unit * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // 2 mắt.
    drawCircle(accent, unit * 0.028f, Offset(cx - rad * 0.32f, cy + rad * 0.12f))
    drawCircle(accent, unit * 0.028f, Offset(cx + rad * 0.32f, cy + rad * 0.12f))
    // Miệng cau (cung ngược).
    drawArc(
        color = accent, startAngle = 20f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx - rad * 0.34f, cy + rad * 0.62f), size = Size(rad * 0.68f, rad * 0.5f),
        style = Stroke(width = unit * 0.035f),
    )
}

// ─────────── Wave 22 batch 5 — 2 boss trào phúng ───────────

/** Trùm Karaoke Lạc Tông — micro (đầu tròn + cán) + 2 sóng âm vòng cung. */
private fun DrawScope.drawBossKaraoke(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val headR = unit * 0.16f
    val hx = cx - unit * 0.06f
    val hy = cy - unit * 0.12f
    // Đầu micro.
    drawCircle(body, headR, Offset(hx, hy))
    drawCircle(accent, headR, Offset(hx, hy), style = Stroke(width = unit * 0.025f))
    // Lưới micro (2 gạch ngang).
    for (g in listOf(-0.4f, 0.4f)) {
        drawLine(accent, Offset(hx - headR * 0.7f, hy + headR * g), Offset(hx + headR * 0.7f, hy + headR * g),
            strokeWidth = unit * 0.015f)
    }
    // Cán micro (chéo xuống phải).
    drawLine(body, Offset(hx + headR * 0.5f, hy + headR * 0.7f),
        Offset(hx + unit * 0.22f, hy + unit * 0.34f),
        strokeWidth = unit * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // 2 sóng âm vòng cung (bên phải, lan ra).
    for (i in 1..2) {
        drawArc(
            color = accent.copy(alpha = 1f - i * 0.25f), startAngle = -50f, sweepAngle = 100f, useCenter = false,
            topLeft = Offset(cx + unit * 0.06f - unit * 0.1f * i, cy - unit * 0.2f - unit * 0.04f * i),
            size = Size(unit * 0.2f * i, unit * 0.4f * i),
            style = Stroke(width = unit * 0.025f),
        )
    }
}

/** Đại Gia Phông Bạt — kính râm (2 mắt kính + cầu) + dây chuyền $ phô trương. */
private fun DrawScope.drawBossFlashy(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val lensR = unit * 0.16f
    val ly = cy - unit * 0.1f
    // 2 mắt kính.
    for (s in listOf(-1f, 1f)) {
        drawCircle(body, lensR, Offset(cx + s * unit * 0.2f, ly))
        drawCircle(accent, lensR, Offset(cx + s * unit * 0.2f, ly), style = Stroke(width = unit * 0.03f))
    }
    // Cầu nối kính.
    drawLine(accent, Offset(cx - unit * 0.04f, ly), Offset(cx + unit * 0.04f, ly), strokeWidth = unit * 0.03f)
    // Càng kính 2 bên.
    drawLine(accent, Offset(cx - unit * 0.36f, ly), Offset(cx - unit * 0.44f, ly - unit * 0.04f), strokeWidth = unit * 0.025f)
    drawLine(accent, Offset(cx + unit * 0.36f, ly), Offset(cx + unit * 0.44f, ly - unit * 0.04f), strokeWidth = unit * 0.025f)
    // Dây chuyền $ (cung dưới + ký hiệu).
    drawArc(
        color = accent, startAngle = 20f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx - unit * 0.22f, ly + unit * 0.12f), size = Size(unit * 0.44f, unit * 0.34f),
        style = Stroke(width = unit * 0.025f),
    )
    drawLine(accent, Offset(cx, ly + unit * 0.34f), Offset(cx, ly + unit * 0.5f), strokeWidth = unit * 0.03f)
}

// ─────────── Wave 23 batch 6 — 2 boss trào phúng ───────────

/** Bác Sĩ Google — chữ thập y tế + kính lúp (tra cứu). */
private fun DrawScope.drawBossDrGoogle(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val arm = unit * 0.22f
    val th = unit * 0.12f
    // Chữ thập (2 thanh).
    drawRect(body, topLeft = Offset(cx - th / 2f, cy - arm), size = Size(th, arm * 2f))
    drawRect(body, topLeft = Offset(cx - arm, cy - th / 2f), size = Size(arm * 2f, th))
    drawRect(accent, topLeft = Offset(cx - th / 2f, cy - arm), size = Size(th, arm * 2f),
        style = Stroke(width = unit * 0.02f))
    drawRect(accent, topLeft = Offset(cx - arm, cy - th / 2f), size = Size(arm * 2f, th),
        style = Stroke(width = unit * 0.02f))
    // Kính lúp (vòng + cán) góc dưới-phải = tra Google.
    val gx = cx + unit * 0.24f
    val gy = cy + unit * 0.24f
    drawCircle(accent, unit * 0.12f, Offset(gx, gy), style = Stroke(width = unit * 0.03f))
    drawLine(accent, Offset(gx + unit * 0.085f, gy + unit * 0.085f),
        Offset(gx + unit * 0.2f, gy + unit * 0.2f),
        strokeWidth = unit * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
}

/** Hoàng Thượng Mèo — đầu mèo (tròn + 2 tai tam giác) + ria + vương miện nhỏ. */
private fun DrawScope.drawBossCatEmperor(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val rad = unit * 0.3f
    // Đầu.
    drawCircle(body, rad, Offset(cx, cy + unit * 0.04f))
    drawCircle(accent, rad, Offset(cx, cy + unit * 0.04f), style = Stroke(width = unit * 0.03f))
    // 2 tai tam giác.
    for (s in listOf(-1f, 1f)) {
        val ear = PathPool.acquire().apply {
            moveTo(cx + s * rad * 0.55f, cy - rad * 0.7f)
            lineTo(cx + s * rad * 0.95f, cy - rad * 1.25f)
            lineTo(cx + s * rad * 1.0f, cy - rad * 0.5f)
            close()
        }
        drawPath(ear, body)
        drawPath(ear, accent, style = Stroke(width = unit * 0.02f))
        PathPool.release(ear)
    }
    // 2 mắt.
    for (s in listOf(-1f, 1f)) drawCircle(accent, unit * 0.028f, Offset(cx + s * rad * 0.35f, cy))
    // Ria (2 bên × 2 sợi).
    for (s in listOf(-1f, 1f)) for (dy in listOf(-0.06f, 0.06f)) {
        drawLine(accent, Offset(cx + s * rad * 0.2f, cy + rad * 0.25f + unit * dy),
            Offset(cx + s * rad * 0.95f, cy + rad * 0.18f + unit * dy * 2f), strokeWidth = unit * 0.012f)
    }
    // Vương miện nhỏ trên đỉnh (3 chóp).
    val cw = rad * 0.8f
    val cyTop = cy - rad * 1.1f
    val crown = PathPool.acquire().apply {
        moveTo(cx - cw / 2f, cyTop + unit * 0.08f)
        lineTo(cx - cw / 2f, cyTop)
        lineTo(cx - cw * 0.25f, cyTop + unit * 0.05f)
        lineTo(cx, cyTop - unit * 0.04f)
        lineTo(cx + cw * 0.25f, cyTop + unit * 0.05f)
        lineTo(cx + cw / 2f, cyTop)
        lineTo(cx + cw / 2f, cyTop + unit * 0.08f)
        close()
    }
    drawPath(crown, accent)
    PathPool.release(crown)
}

// ─────────── Wave 24 batch 7 — 2 boss trào phúng (HẾT 18) ───────────

/** Thánh Cuồng Sale — thẻ giá (có lỗ treo) + "%" to (giảm giá). */
private fun DrawScope.drawBossSale(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val w = unit * 0.6f
    val h = unit * 0.5f
    // Thẻ giá (ngũ giác: chữ nhật + mũi nhọn trái).
    val tag = PathPool.acquire().apply {
        moveTo(cx - w * 0.5f, cy)
        lineTo(cx - w * 0.18f, cy - h * 0.5f)
        lineTo(cx + w * 0.5f, cy - h * 0.5f)
        lineTo(cx + w * 0.5f, cy + h * 0.5f)
        lineTo(cx - w * 0.18f, cy + h * 0.5f)
        close()
    }
    drawPath(tag, body)
    drawPath(tag, accent, style = Stroke(width = unit * 0.03f))
    PathPool.release(tag)
    // Lỗ treo.
    drawCircle(accent, unit * 0.035f, Offset(cx - w * 0.28f, cy))
    // "%" : 2 chấm + gạch chéo.
    drawCircle(accent, unit * 0.045f, Offset(cx + w * 0.05f, cy - h * 0.2f))
    drawCircle(accent, unit * 0.045f, Offset(cx + w * 0.3f, cy + h * 0.2f))
    drawLine(accent, Offset(cx + w * 0.32f, cy - h * 0.28f), Offset(cx + w * 0.03f, cy + h * 0.28f),
        strokeWidth = unit * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
}

/** Cô Hồn Tháng 7 — hồn lửa (giọt nước ngược + đáy lượn sóng) + 2 mắt rỗng. */
private fun DrawScope.drawBossGhostMonth(
    cx: Float, cy: Float, wPx: Float, hPx: Float, body: Color, accent: Color,
) {
    val unit = minOf(wPx, hPx)
    val r = unit * 0.3f
    // Thân hồn (đỉnh nhọn, phình dưới) + đáy lượn 3 múi.
    val ghost = PathPool.acquire().apply {
        moveTo(cx, cy - r * 1.2f)                                  // đỉnh nhọn
        cubicTo(cx + r * 1.1f, cy - r * 0.6f, cx + r, cy + r * 0.6f, cx + r, cy + r * 0.9f)
        // đáy lượn sóng (3 múi).
        lineTo(cx + r * 0.55f, cy + r * 0.6f)
        lineTo(cx + r * 0.2f, cy + r * 0.95f)
        lineTo(cx - r * 0.2f, cy + r * 0.6f)
        lineTo(cx - r * 0.55f, cy + r * 0.95f)
        lineTo(cx - r, cy + r * 0.9f)
        cubicTo(cx - r, cy + r * 0.6f, cx - r * 1.1f, cy - r * 0.6f, cx, cy - r * 1.2f)
        close()
    }
    drawPath(ghost, body.copy(alpha = 0.85f))
    drawPath(ghost, accent, style = Stroke(width = unit * 0.025f))
    PathPool.release(ghost)
    // 2 mắt rỗng (tối).
    for (s in listOf(-1f, 1f)) {
        drawCircle(Color(0xFF0A0A18), unit * 0.05f, Offset(cx + s * r * 0.4f, cy - r * 0.1f))
    }
}

// ─────────── Shape recipes ───────────

/**
 * Dart shape — triangle pointing DOWN (enemies attack downward).
 * Round 77 (R77e) — Mỗi variant 0..4 có unique additive modifier:
 *   v0 baseline / v1 thruster trail / v2 side-spikes / v3 swept-wings / v4 heavy armor pip.
 */
private fun DrawScope.drawDart(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color, variant: Int,
) {
    val halfW = w * 0.42f
    val halfH = h * 0.45f
    val tipY = cy + halfH
    val baseY = cy - halfH
    val notch = h * (0.10f + variant * 0.04f)
    val path = PathPool.acquire().apply {
        moveTo(cx, tipY)
        lineTo(cx + halfW, baseY)
        lineTo(cx, baseY + notch)
        lineTo(cx - halfW, baseY)
        close()
    }
    drawPath(path = path, color = body)
    drawPath(path = path, color = accent, style = Stroke(width = w * 0.06f))
    PathPool.release(path)
    drawCircle(accent, w * 0.10f, Offset(cx, cy + h * 0.10f))
    // Round 77 — additive modifier per variant.
    when (variant) {
        1 -> {
            // Thruster trail behind (upward extra lines)
            drawLine(accent, Offset(cx - w * 0.10f, baseY), Offset(cx - w * 0.15f, baseY - h * 0.18f),
                strokeWidth = w * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(accent, Offset(cx + w * 0.10f, baseY), Offset(cx + w * 0.15f, baseY - h * 0.18f),
                strokeWidth = w * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        2 -> {
            // Side-spikes — 2 nhỏ ở mép cánh
            drawCircle(accent, w * 0.06f, Offset(cx + halfW, baseY + h * 0.05f))
            drawCircle(accent, w * 0.06f, Offset(cx - halfW, baseY + h * 0.05f))
        }
        3 -> {
            // Swept-wings — extra triangle nhỏ phía mép cánh
            val sweepW = w * 0.18f
            val sweepH = h * 0.18f
            val swp = PathPool.acquire().apply {
                moveTo(cx + halfW, baseY)
                lineTo(cx + halfW + sweepW, baseY + sweepH * 0.5f)
                lineTo(cx + halfW, baseY + sweepH)
                close()
            }
            drawPath(swp, accent)
            PathPool.release(swp)
            val swpL = PathPool.acquire().apply {
                moveTo(cx - halfW, baseY)
                lineTo(cx - halfW - sweepW, baseY + sweepH * 0.5f)
                lineTo(cx - halfW, baseY + sweepH)
                close()
            }
            drawPath(swpL, accent)
            PathPool.release(swpL)
        }
        4 -> {
            // Heavy armor pip — extra inner triangle layer
            val innerScale = 0.6f
            val innerPath = PathPool.acquire().apply {
                moveTo(cx, tipY - h * 0.05f)
                lineTo(cx + halfW * innerScale, baseY + h * 0.05f)
                lineTo(cx, baseY + notch + h * 0.05f)
                lineTo(cx - halfW * innerScale, baseY + h * 0.05f)
                close()
            }
            drawPath(innerPath, accent.copy(alpha = 0.65f))
            PathPool.release(innerPath)
            // Center heavy pip
            drawCircle(Color.White.copy(alpha = 0.65f), w * 0.07f, Offset(cx, cy))
        }
    }
}

/**
 * Hexagon — flat-top, slightly elongated vertically. variant rotates 10° each.
 * Round 77 (R77e) — Mỗi variant 0..3 unique additive:
 *   v0 baseline / v1 inner-hex / v2 corner orbs / v3 center cross.
 */
private fun DrawScope.drawHexagon(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color, variant: Int,
) {
    val rx = w * 0.45f
    val ry = h * 0.50f
    val baseAngle = Math.toRadians((variant * 10).toDouble())
    val path = PathPool.acquire().apply {
        for (i in 0 until 6) {
            val a = baseAngle + 2.0 * Math.PI * i / 6.0
            val x = cx + (rx * cos(a)).toFloat()
            val y = cy + (ry * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = path, color = body)
    drawPath(path = path, color = accent, style = Stroke(width = w * 0.06f))
    PathPool.release(path)
    drawCircle(accent, w * 0.12f, Offset(cx, cy))
    when (variant) {
        1 -> {
            // Inner hex shell
            val ix = rx * 0.55f; val iy = ry * 0.55f
            val ip = PathPool.acquire().apply {
                for (i in 0 until 6) {
                    val a = baseAngle + 2.0 * Math.PI * i / 6.0
                    val x = cx + (ix * cos(a)).toFloat()
                    val y = cy + (iy * sin(a)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(ip, accent, style = Stroke(width = w * 0.04f))
            PathPool.release(ip)
        }
        2 -> {
            // 6 corner orbs
            for (i in 0 until 6) {
                val a = baseAngle + 2.0 * Math.PI * i / 6.0
                val ox = cx + (rx * cos(a)).toFloat()
                val oy = cy + (ry * sin(a)).toFloat()
                drawCircle(accent, w * 0.05f, Offset(ox, oy))
            }
        }
        3 -> {
            // Center cross (4-tip mini star)
            drawLine(accent, Offset(cx, cy - ry * 0.45f), Offset(cx, cy + ry * 0.45f),
                strokeWidth = w * 0.05f)
            drawLine(accent, Offset(cx - rx * 0.45f, cy), Offset(cx + rx * 0.45f, cy),
                strokeWidth = w * 0.05f)
        }
    }
}

/**
 * Diamond — vertical, wider than tall, with 4-spike accent.
 * Round 77 audit fix — additive modifier per variant 0..2:
 *   v0 baseline / v1 horizontal spikes (đối xứng L-R) / v2 corner mini-diamonds.
 */
private fun DrawScope.drawDiamond(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color, variant: Int,
) {
    val halfW = w * 0.45f
    val halfH = h * 0.50f
    val path = PathPool.acquire().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy)
        close()
    }
    drawPath(path = path, color = body)
    drawPath(path = path, color = accent, style = Stroke(width = w * 0.07f))
    PathPool.release(path)
    val spikeR = w * 0.10f
    drawCircle(color = accent, radius = spikeR, center = Offset(cx, cy))
    when (variant) {
        1 -> {
            // Horizontal spikes — 2 pips trái + phải để biến tấu visual.
            drawCircle(accent, spikeR * 0.7f, Offset(cx - halfW * 0.5f, cy))
            drawCircle(accent, spikeR * 0.7f, Offset(cx + halfW * 0.5f, cy))
            // Plus vertical center pip
            drawCircle(accent, spikeR * 0.5f, Offset(cx, cy - halfH * 0.6f))
        }
        2 -> {
            // Corner mini-diamonds at 4 cardinal points
            val miniR = w * 0.07f
            for (i in 0 until 4) {
                val a = i * 90.0 * Math.PI / 180.0
                val mx = cx + (halfW * 0.75f * kotlin.math.cos(a)).toFloat()
                val my = cy + (halfH * 0.75f * kotlin.math.sin(a)).toFloat()
                val mini = PathPool.acquire().apply {
                    moveTo(mx, my - miniR)
                    lineTo(mx + miniR, my)
                    lineTo(mx, my + miniR)
                    lineTo(mx - miniR, my)
                    close()
                }
                drawPath(mini, accent)
                PathPool.release(mini)
            }
        }
    }
}

/** Boss shape — large 8-pointed star with inner hexagon detail. */
private fun DrawScope.drawBossStar(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val outerR = minOf(w, h) * 0.50f
    val innerR = outerR * 0.55f
    val path = PathPool.acquire().apply {
        val step = Math.PI / 8.0                                       // 16 vertices, 8-pointed star
        for (i in 0 until 16) {
            val a = -Math.PI / 2.0 + i * step
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * cos(a)).toFloat()
            val y = cy + (r * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = path, color = body)
    drawPath(path = path, color = accent, style = Stroke(width = w * 0.05f))
    PathPool.release(path)
    // Inner hexagon core
    val coreR = outerR * 0.40f
    val core = PathPool.acquire().apply {
        for (i in 0 until 6) {
            val a = 2.0 * Math.PI * i / 6.0
            val x = cx + (coreR * cos(a)).toFloat()
            val y = cy + (coreR * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = core, color = accent)
    PathPool.release(core)
    drawCircle(color = Color.White.copy(alpha = 0.75f), radius = coreR * 0.30f, center = Offset(cx, cy))
}

// ─────────────────────────────────────────────────────────────────────────
// Round 78 (#1+#2 shape diversity) — new visually distinct enemy shapes
// to break up family monotony. Each replaces one drawable in the dispatcher
// so within a family (cyan/green/red) different individuals look different.
// ─────────────────────────────────────────────────────────────────────────

/** Heart shape — symmetric lobes top, point at bottom. */
private fun DrawScope.drawHeart(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val halfW = w * 0.45f
    val halfH = h * 0.45f
    // Build heart via 2 circles + downward triangle merge.
    val lobeR = halfW * 0.55f
    drawCircle(body, lobeR, Offset(cx - halfW * 0.45f, cy - halfH * 0.30f))
    drawCircle(body, lobeR, Offset(cx + halfW * 0.45f, cy - halfH * 0.30f))
    val triPath = PathPool.acquire().apply {
        moveTo(cx - halfW, cy - halfH * 0.18f)
        lineTo(cx + halfW, cy - halfH * 0.18f)
        lineTo(cx, cy + halfH)
        close()
    }
    drawPath(triPath, body)
    PathPool.release(triPath)
    // Accent outline (stroke approximation: redraw rim accents)
    drawCircle(accent, lobeR * 0.35f, Offset(cx - halfW * 0.45f, cy - halfH * 0.30f))
    drawCircle(accent, lobeR * 0.35f, Offset(cx + halfW * 0.45f, cy - halfH * 0.30f))
    drawCircle(accent, w * 0.07f, Offset(cx, cy + halfH * 0.5f))
}

/** Simple downward triangle (distinct from drawDart which has notch). */
private fun DrawScope.drawTriangle(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val halfW = w * 0.45f
    val halfH = h * 0.48f
    val path = PathPool.acquire().apply {
        moveTo(cx - halfW, cy - halfH)
        lineTo(cx + halfW, cy - halfH)
        lineTo(cx, cy + halfH)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent, style = Stroke(width = w * 0.06f))
    PathPool.release(path)
    // Center pip + 2 side dots for "alien glyph" feel.
    drawCircle(accent, w * 0.08f, Offset(cx, cy - halfH * 0.30f))
    drawCircle(accent, w * 0.05f, Offset(cx - halfW * 0.30f, cy + halfH * 0.20f))
    drawCircle(accent, w * 0.05f, Offset(cx + halfW * 0.30f, cy + halfH * 0.20f))
}

/** Eye shape — oval body, iris circle, pupil dot. Creepy "watching you" vibe. */
private fun DrawScope.drawEye(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val rx = w * 0.48f
    val ry = h * 0.30f
    // Outer eye outline (lens shape via 2 overlapping arcs approximated as ellipse).
    drawOval(
        color = body,
        topLeft = Offset(cx - rx, cy - ry),
        size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
    )
    drawOval(
        color = accent,
        topLeft = Offset(cx - rx, cy - ry),
        size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
        style = Stroke(width = w * 0.05f),
    )
    // Iris — white ring
    drawCircle(Color.White.copy(alpha = 0.85f), ry * 0.85f, Offset(cx, cy))
    // Pupil — dark accent core
    drawCircle(accent, ry * 0.50f, Offset(cx, cy))
    // Highlight glint
    drawCircle(Color.White, ry * 0.18f, Offset(cx + ry * 0.25f, cy - ry * 0.25f))
}

/** Virus shape — central blob with 8 protrusions radiating outward (biological). */
private fun DrawScope.drawVirus(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val coreR = minOf(w, h) * 0.30f
    val spineLen = minOf(w, h) * 0.22f
    val tipR = minOf(w, h) * 0.07f
    val spines = 8
    for (i in 0 until spines) {
        val a = i * 2.0 * Math.PI / spines
        val ex = cx + ((coreR + spineLen) * cos(a)).toFloat()
        val ey = cy + ((coreR + spineLen) * sin(a)).toFloat()
        drawLine(
            color = body,
            start = Offset(cx, cy),
            end = Offset(ex, ey),
            strokeWidth = w * 0.07f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        // Knob at tip
        drawCircle(accent, tipR, Offset(ex, ey))
    }
    // Central blob with darker core
    drawCircle(body, coreR, Offset(cx, cy))
    drawCircle(accent, coreR * 0.50f, Offset(cx, cy))
    // Inner dots — virus "RNA"
    drawCircle(Color.White.copy(alpha = 0.8f), coreR * 0.18f,
        Offset(cx - coreR * 0.30f, cy - coreR * 0.15f))
    drawCircle(Color.White.copy(alpha = 0.8f), coreR * 0.15f,
        Offset(cx + coreR * 0.25f, cy + coreR * 0.20f))
}

/** Simple Circle enemy — disc with concentric ring + center pip. */
private fun DrawScope.drawCircleEnemy(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val r = minOf(w, h) * 0.42f
    drawCircle(body, r, Offset(cx, cy))
    drawCircle(accent, r, Offset(cx, cy), style = Stroke(width = w * 0.06f))
    drawCircle(accent, r * 0.40f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.18f, Offset(cx - r * 0.15f, cy - r * 0.15f))
}

/** Spade ♠ — heart shape inverted with stem at bottom (cards suit "bích"). */
private fun DrawScope.drawCardSpade(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val halfW = w * 0.40f
    val halfH = h * 0.40f
    val lobeR = halfW * 0.55f
    // Inverted heart (point up, lobes down) + stem
    drawCircle(body, lobeR, Offset(cx - halfW * 0.45f, cy + halfH * 0.20f))
    drawCircle(body, lobeR, Offset(cx + halfW * 0.45f, cy + halfH * 0.20f))
    val triPath = PathPool.acquire().apply {
        moveTo(cx - halfW, cy + halfH * 0.30f)
        lineTo(cx + halfW, cy + halfH * 0.30f)
        lineTo(cx, cy - halfH)
        close()
    }
    drawPath(triPath, body)
    PathPool.release(triPath)
    // Stem at bottom
    drawRect(body,
        topLeft = Offset(cx - w * 0.05f, cy + halfH * 0.30f),
        size = Size(w * 0.10f, h * 0.20f))
    // Triangle stem base
    val baseTri = PathPool.acquire().apply {
        moveTo(cx - w * 0.15f, cy + halfH * 0.50f)
        lineTo(cx + w * 0.15f, cy + halfH * 0.50f)
        lineTo(cx, cy + halfH * 0.30f)
        close()
    }
    drawPath(baseTri, body)
    drawCircle(accent, w * 0.06f, Offset(cx, cy + halfH * 0.05f))
}

/** Club ♣ — 3 circles cluster + stem (cards suit "chuồn"). */
private fun DrawScope.drawCardClub(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val lobeR = minOf(w, h) * 0.20f
    val offsetD = lobeR * 0.85f
    // 3 lobes: top + bottom-left + bottom-right
    drawCircle(body, lobeR, Offset(cx, cy - offsetD))
    drawCircle(body, lobeR, Offset(cx - offsetD, cy + offsetD * 0.55f))
    drawCircle(body, lobeR, Offset(cx + offsetD, cy + offsetD * 0.55f))
    // Stem connecting to bottom (small rect)
    drawRect(body,
        topLeft = Offset(cx - w * 0.05f, cy + offsetD * 0.30f),
        size = Size(w * 0.10f, h * 0.25f))
    val baseTri = PathPool.acquire().apply {
        moveTo(cx - w * 0.15f, cy + h * 0.45f)
        lineTo(cx + w * 0.15f, cy + h * 0.45f)
        lineTo(cx, cy + offsetD * 0.30f)
        close()
    }
    drawPath(baseTri, body)
    PathPool.release(baseTri)
    // Accent in center
    drawCircle(accent, lobeR * 0.40f, Offset(cx, cy - offsetD))
    drawCircle(accent, lobeR * 0.40f, Offset(cx - offsetD, cy + offsetD * 0.55f))
    drawCircle(accent, lobeR * 0.40f, Offset(cx + offsetD, cy + offsetD * 0.55f))
}

/** Card Diamond ♦ — vertical rhombus (cards suit "rô"). Same as drawDiamond v0 but
 * with thinner aspect to read as the playing-card glyph. */
private fun DrawScope.drawCardDiamond(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val halfW = w * 0.32f
    val halfH = h * 0.48f
    val path = PathPool.acquire().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy)
        close()
    }
    drawPath(path, body)
    drawPath(path, accent, style = Stroke(width = w * 0.07f))
    PathPool.release(path)
    drawCircle(Color.White.copy(alpha = 0.85f), w * 0.08f, Offset(cx, cy))
}

/** Boss SUN — central disc + outer corona ring + 12 radial flares (R78 #2). */
private fun DrawScope.drawBossSun(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val coreR = minOf(w, h) * 0.30f
    val coronaR = minOf(w, h) * 0.42f
    val flareInner = coronaR * 1.05f
    val flareOuter = coronaR * 1.35f
    // 12 radial flares (alternating long/short for dynamic sun-look).
    for (i in 0 until 12) {
        val a = i * 30.0 * Math.PI / 180.0
        val outerR = if (i % 2 == 0) flareOuter else flareInner * 1.10f
        val sx = cx + (flareInner * kotlin.math.cos(a)).toFloat()
        val sy = cy + (flareInner * kotlin.math.sin(a)).toFloat()
        val ex = cx + (outerR * kotlin.math.cos(a)).toFloat()
        val ey = cy + (outerR * kotlin.math.sin(a)).toFloat()
        drawLine(
            color = accent,
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = w * (if (i % 2 == 0) 0.06f else 0.04f),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
    // Corona ring (slightly translucent, gives the "atmosphere" look).
    drawCircle(accent.copy(alpha = 0.55f), coronaR, Offset(cx, cy),
        style = Stroke(width = w * 0.05f))
    // Main disc body
    drawCircle(body, coreR, Offset(cx, cy))
    // Inner brightness (white-ish center hot spot)
    drawCircle(Color.White.copy(alpha = 0.85f), coreR * 0.55f, Offset(cx, cy))
    drawCircle(Color.White, coreR * 0.18f, Offset(cx - coreR * 0.20f, cy - coreR * 0.20f))
}

/** Boss ATOM — nucleus + 3 elliptical electron orbits at 60° rotation (R78 #2). */
private fun DrawScope.drawBossAtom(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val nucR = minOf(w, h) * 0.18f
    val orbitRx = minOf(w, h) * 0.48f
    val orbitRy = minOf(w, h) * 0.18f
    // 3 elliptical orbits — at 0°, 60°, 120°. Manual rotation per orbit by
    // computing the bounding box after rotation. Compose Canvas doesn't have a
    // rotate-around-pivot for DrawScope (only top-level), so we use the
    // canvas-saved rotate via `withTransform`.
    for (i in 0 until 3) {
        val deg = i * 60f
        withTransform(
            transformBlock = { rotate(degrees = deg, pivot = Offset(cx, cy)) },
            drawBlock = {
                drawOval(
                    color = accent,
                    topLeft = Offset(cx - orbitRx, cy - orbitRy),
                    size = Size(orbitRx * 2, orbitRy * 2),
                    style = Stroke(width = w * 0.04f),
                )
                drawCircle(body, w * 0.05f, Offset(cx + orbitRx, cy))
            },
        )
    }
    // Central nucleus — 2-layer disc
    drawCircle(body, nucR, Offset(cx, cy))
    drawCircle(accent, nucR * 0.55f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.8f), nucR * 0.30f, Offset(cx, cy))
}

/** Boss EYE — large menacing eye with iris + pupil + outer eyelid. Replaces drawBossOrb. */
private fun DrawScope.drawBossEye(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val outerR = minOf(w, h) * 0.48f
    val midR = outerR * 0.75f
    val innerR = outerR * 0.45f
    val pupilR = outerR * 0.22f
    // Outer "eyelid" body
    drawCircle(body, outerR, Offset(cx, cy))
    drawCircle(accent, outerR, Offset(cx, cy), style = Stroke(width = w * 0.04f))
    // White sclera
    drawCircle(Color.White.copy(alpha = 0.92f), midR, Offset(cx, cy))
    // Iris (body color)
    drawCircle(body, innerR, Offset(cx, cy))
    drawCircle(accent, innerR, Offset(cx, cy), style = Stroke(width = w * 0.03f))
    // Pupil — black/dark accent
    drawCircle(accent, pupilR, Offset(cx, cy))
    drawCircle(Color.Black.copy(alpha = 0.85f), pupilR * 0.85f, Offset(cx, cy))
    // Highlight
    drawCircle(Color.White, pupilR * 0.35f, Offset(cx + pupilR * 0.40f, cy - pupilR * 0.45f))
    // 6 radial "lashes" around outer rim — eldritch detail
    for (i in 0 until 6) {
        val a = i * 60.0 * Math.PI / 180.0
        val sx = cx + ((outerR + outerR * 0.08f) * kotlin.math.cos(a)).toFloat()
        val sy = cy + ((outerR + outerR * 0.08f) * kotlin.math.sin(a)).toFloat()
        val ex = cx + ((outerR + outerR * 0.30f) * kotlin.math.cos(a)).toFloat()
        val ey = cy + ((outerR + outerR * 0.30f) * kotlin.math.sin(a)).toFloat()
        drawLine(accent, Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.04f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Round 79 (#1) — 4 new boss shape recipes to give each chapter encounter a
// unique silhouette (was 5 distinct over 9 encounters).
// ─────────────────────────────────────────────────────────────────────────

/** Boss DEATH MOON — full moon disc with skull-crater details + crack fissures. */
private fun DrawScope.drawBossDeathMoon(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val r = minOf(w, h) * 0.45f
    // Main moon body
    drawCircle(body, r, Offset(cx, cy))
    drawCircle(accent, r, Offset(cx, cy), style = Stroke(width = w * 0.04f))
    // 2 large craters (skull eye sockets)
    val socketR = r * 0.18f
    drawCircle(Color.Black.copy(alpha = 0.75f), socketR,
        Offset(cx - r * 0.30f, cy - r * 0.15f))
    drawCircle(Color.Black.copy(alpha = 0.75f), socketR,
        Offset(cx + r * 0.30f, cy - r * 0.15f))
    // Nose (triangular dark)
    val nosePath = PathPool.acquire().apply {
        moveTo(cx, cy + r * 0.05f)
        lineTo(cx - r * 0.10f, cy + r * 0.20f)
        lineTo(cx + r * 0.10f, cy + r * 0.20f)
        close()
    }
    drawPath(nosePath, Color.Black.copy(alpha = 0.70f))
    PathPool.release(nosePath)
    // Teeth row (mouth grin)
    val teethCount = 5
    val teethY = cy + r * 0.40f
    val teethStartX = cx - r * 0.30f
    val teethWidth = r * 0.60f
    val teethStep = teethWidth / teethCount
    val teethH = r * 0.12f
    for (i in 0 until teethCount) {
        drawRect(Color.Black.copy(alpha = 0.70f),
            topLeft = Offset(teethStartX + i * teethStep + teethStep * 0.10f, teethY),
            size = Size(teethStep * 0.80f, teethH))
    }
    // Crack fissures (4 jagged lines radiating from center)
    for (i in 0 until 3) {
        val ang = (i * 70.0 + 100.0) * Math.PI / 180.0
        val sx = cx + (r * 0.55f * kotlin.math.cos(ang)).toFloat()
        val sy = cy + (r * 0.55f * kotlin.math.sin(ang)).toFloat()
        val ex = cx + (r * 0.95f * kotlin.math.cos(ang)).toFloat()
        val ey = cy + (r * 0.95f * kotlin.math.sin(ang)).toFloat()
        drawLine(accent.copy(alpha = 0.6f), Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.025f)
    }
    // Highlight glow upper-left
    drawCircle(Color.White.copy(alpha = 0.20f), r * 0.30f,
        Offset(cx - r * 0.40f, cy - r * 0.45f))
}

/** Boss HAUNTED KID — ghost child silhouette with hollow eyes + wavy bottom. */
private fun DrawScope.drawBossHauntedKid(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val headR = minOf(w, h) * 0.25f
    val bodyW = w * 0.55f
    val bodyH = h * 0.50f
    // Body — bell-shape sheet from top-rounded to wavy bottom
    val bodyPath = PathPool.acquire().apply {
        // Top arc (head connects)
        moveTo(cx - bodyW / 2f, cy - bodyH * 0.10f)
        // Left side down
        lineTo(cx - bodyW / 2f, cy + bodyH * 0.40f)
        // Wavy bottom — 5 waves
        val waveCount = 5
        val waveStep = bodyW / waveCount
        for (i in 0..waveCount) {
            val wx = cx - bodyW / 2f + i * waveStep
            val wy = if (i % 2 == 0) cy + bodyH * 0.50f else cy + bodyH * 0.35f
            lineTo(wx, wy)
        }
        // Right side up
        lineTo(cx + bodyW / 2f, cy - bodyH * 0.10f)
        // Top arc (close via head — go up)
        lineTo(cx - bodyW / 2f, cy - bodyH * 0.10f)
        close()
    }
    drawPath(bodyPath, body.copy(alpha = 0.85f))
    PathPool.release(bodyPath)
    // Head — circle on top
    drawCircle(body.copy(alpha = 0.85f), headR, Offset(cx, cy - bodyH * 0.30f))
    // Hollow eyes (2 dark sockets)
    val eyeR = headR * 0.30f
    drawCircle(Color.Black.copy(alpha = 0.85f), eyeR,
        Offset(cx - headR * 0.35f, cy - bodyH * 0.30f - headR * 0.10f))
    drawCircle(Color.Black.copy(alpha = 0.85f), eyeR,
        Offset(cx + headR * 0.35f, cy - bodyH * 0.30f - headR * 0.10f))
    // Glowing eye-pupil
    drawCircle(accent, eyeR * 0.40f,
        Offset(cx - headR * 0.35f, cy - bodyH * 0.30f - headR * 0.10f))
    drawCircle(accent, eyeR * 0.40f,
        Offset(cx + headR * 0.35f, cy - bodyH * 0.30f - headR * 0.10f))
    // Open mouth (small "O" of horror)
    val mouthY = cy - bodyH * 0.30f + headR * 0.30f
    drawCircle(Color.Black.copy(alpha = 0.85f), headR * 0.20f, Offset(cx, mouthY))
    // Outline highlight
    drawCircle(Color.White.copy(alpha = 0.3f), headR * 0.5f,
        Offset(cx - headR * 0.25f, cy - bodyH * 0.30f - headR * 0.30f))
}

/** Boss HELL LORD — devil head with 2 curved horns + glowing eyes + fangs. */
private fun DrawScope.drawBossHellLord(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val headR = minOf(w, h) * 0.36f
    // Main head (oval-ish, slightly elongated down)
    drawOval(body,
        topLeft = Offset(cx - headR, cy - headR * 0.90f),
        size = Size(headR * 2, headR * 2.0f))
    drawOval(accent,
        topLeft = Offset(cx - headR, cy - headR * 0.90f),
        size = Size(headR * 2, headR * 2.0f),
        style = Stroke(width = w * 0.035f))
    // 2 curved horns
    for (sign in intArrayOf(-1, 1)) {
        val baseX = cx + sign * headR * 0.70f
        val baseY = cy - headR * 0.70f
        val midX = cx + sign * headR * 1.10f
        val midY = cy - headR * 1.30f
        val tipX = cx + sign * headR * 0.85f
        val tipY = cy - headR * 1.70f
        val hornPath = PathPool.acquire().apply {
            moveTo(baseX, baseY)
            cubicTo(midX, midY, midX * 1.05f, midY, tipX, tipY)
            // Inner curve back
            cubicTo(midX * 0.85f, midY * 1.10f, baseX + sign * headR * 0.10f, baseY - headR * 0.05f, baseX, baseY)
            close()
        }
        drawPath(hornPath, body)
        drawPath(hornPath, accent, style = Stroke(width = w * 0.03f))
        PathPool.release(hornPath)
    }
    // Glowing eyes (2 red slits)
    val eyeY = cy - headR * 0.10f
    drawOval(accent.copy(alpha = 0.95f),
        topLeft = Offset(cx - headR * 0.55f, eyeY - headR * 0.08f),
        size = Size(headR * 0.40f, headR * 0.18f))
    drawOval(accent.copy(alpha = 0.95f),
        topLeft = Offset(cx + headR * 0.15f, eyeY - headR * 0.08f),
        size = Size(headR * 0.40f, headR * 0.18f))
    // Eye glints
    drawCircle(Color.White, headR * 0.06f,
        Offset(cx - headR * 0.40f, eyeY - headR * 0.03f))
    drawCircle(Color.White, headR * 0.06f,
        Offset(cx + headR * 0.30f, eyeY - headR * 0.03f))
    // Mouth grin with fangs
    val mouthY = cy + headR * 0.40f
    drawArc(color = Color.Black.copy(alpha = 0.80f),
        startAngle = 20f, sweepAngle = 140f,
        useCenter = true,
        topLeft = Offset(cx - headR * 0.45f, mouthY - headR * 0.20f),
        size = Size(headR * 0.90f, headR * 0.45f))
    // 4 fangs (white triangles dropping from upper lip)
    for (i in 0 until 4) {
        val fx = cx - headR * 0.30f + i * headR * 0.20f
        val fpath = PathPool.acquire().apply {
            moveTo(fx - headR * 0.05f, mouthY)
            lineTo(fx + headR * 0.05f, mouthY)
            lineTo(fx, mouthY + headR * 0.15f)
            close()
        }
        drawPath(fpath, Color.White.copy(alpha = 0.85f))
        PathPool.release(fpath)
    }
}

/** Boss SATAN GLYPH — inverted pentagram with all-seeing eye in center. */
private fun DrawScope.drawBossSatanGlyph(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val outerR = minOf(w, h) * 0.45f
    val innerR = outerR * 0.40f
    // Outer protective ring
    drawCircle(body, outerR * 1.10f, Offset(cx, cy),
        style = Stroke(width = w * 0.045f))
    drawCircle(accent.copy(alpha = 0.55f), outerR * 1.10f, Offset(cx, cy))
    // Inverted pentagram (5-point star, point DOWN)
    val starPath = PathPool.acquire().apply {
        val rotation = Math.PI / 2.0  // start pointing down
        for (i in 0 until 10) {
            val a = rotation + i * Math.PI / 5
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(starPath, body)
    drawPath(starPath, Color.White.copy(alpha = 0.65f),
        style = Stroke(width = w * 0.025f))
    PathPool.release(starPath)
    // Central all-seeing eye (oval body + iris + pupil)
    val eyeRx = innerR * 0.85f
    val eyeRy = innerR * 0.55f
    drawOval(Color.White,
        topLeft = Offset(cx - eyeRx, cy - eyeRy),
        size = Size(eyeRx * 2, eyeRy * 2))
    drawOval(accent,
        topLeft = Offset(cx - eyeRx, cy - eyeRy),
        size = Size(eyeRx * 2, eyeRy * 2),
        style = Stroke(width = w * 0.025f))
    drawCircle(accent, eyeRy * 0.65f, Offset(cx, cy))
    drawCircle(Color.Black, eyeRy * 0.35f, Offset(cx, cy))
    drawCircle(Color.White, eyeRy * 0.10f, Offset(cx + eyeRy * 0.20f, cy - eyeRy * 0.15f))
    // 5 small candle dots at outer star tips
    for (i in 0 until 5) {
        val a = (Math.PI / 2.0) + i * 2 * Math.PI / 5
        val px = cx + (outerR * 1.15f * kotlin.math.cos(a)).toFloat()
        val py = cy + (outerR * 1.15f * kotlin.math.sin(a)).toFloat()
        drawCircle(accent, w * 0.025f, Offset(px, py))
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Round 80 (#0) — 4 weird flying ship enemy shapes. Front faces DOWN (toward
// player). User feedback: "share enemy thêm các shape là tàu bay kì quặc,
// đối địch face to face với ship player".
// ─────────────────────────────────────────────────────────────────────────

/** UFO Saucer — oval disc with dome on top + light row at bottom. */
private fun DrawScope.drawEnemyUFO(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val saucerRx = w * 0.42f
    val saucerRy = h * 0.15f
    // Main saucer disc (wide ellipse)
    drawOval(body,
        topLeft = Offset(cx - saucerRx, cy + h * 0.10f - saucerRy),
        size = Size(saucerRx * 2, saucerRy * 2))
    drawOval(accent,
        topLeft = Offset(cx - saucerRx, cy + h * 0.10f - saucerRy),
        size = Size(saucerRx * 2, saucerRy * 2),
        style = Stroke(width = w * 0.025f))
    // Dome (smaller half-ellipse on top — alien cockpit)
    val domeRx = saucerRx * 0.50f
    val domeRy = h * 0.22f
    drawArc(body, startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(cx - domeRx, cy + h * 0.10f - saucerRy - domeRy),
        size = Size(domeRx * 2, domeRy * 2))
    drawArc(accent, startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - domeRx, cy + h * 0.10f - saucerRy - domeRy),
        size = Size(domeRx * 2, domeRy * 2),
        style = Stroke(width = w * 0.025f))
    // Cockpit glow (alien pilot visible inside)
    drawCircle(Color.White.copy(alpha = 0.6f), domeRx * 0.40f,
        Offset(cx, cy + h * 0.10f - saucerRy - domeRy * 0.40f))
    // 5 underside lights pulsing
    val lightY = cy + h * 0.20f
    for (i in 0 until 5) {
        val lx = cx - saucerRx * 0.65f + i * saucerRx * 0.32f
        drawCircle(accent, w * 0.025f, Offset(lx, lightY))
        drawCircle(Color.White, w * 0.012f, Offset(lx, lightY))
    }
}

/** Drone Quad — 4 rotors around square body (face DOWN with cannon protrusion). */
private fun DrawScope.drawEnemyDrone(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val bodyW = w * 0.35f
    val bodyH = h * 0.30f
    // Central body (rotated square)
    drawRect(body,
        topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f),
        size = Size(bodyW, bodyH))
    drawRect(accent,
        topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f),
        size = Size(bodyW, bodyH),
        style = Stroke(width = w * 0.025f))
    // 4 rotor arms (cross-arm extending to 4 corners)
    val armLen = w * 0.45f
    for (sign in intArrayOf(-1, 1)) for (vsign in intArrayOf(-1, 1)) {
        val ex = cx + sign * armLen * 0.85f
        val ey = cy + vsign * armLen * 0.65f
        drawLine(body,
            Offset(cx + sign * bodyW * 0.35f, cy + vsign * bodyH * 0.30f),
            Offset(ex, ey),
            strokeWidth = w * 0.05f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Rotor disc (small circle at end)
        drawCircle(accent, w * 0.06f, Offset(ex, ey))
        drawCircle(Color.White.copy(alpha = 0.65f), w * 0.03f, Offset(ex, ey))
    }
    // Cannon protrusion (face DOWN toward player)
    val cannonW = w * 0.08f
    drawRect(accent,
        topLeft = Offset(cx - cannonW / 2f, cy + bodyH / 2f),
        size = Size(cannonW, h * 0.18f))
    // Sensor "eye" (red dot) on body — alien aggressive look
    drawCircle(Color(0xFFFF2D55), w * 0.05f, Offset(cx, cy))
}

/** Manta Ray Ship — wide delta with tail (gliding alien ship). */
private fun DrawScope.drawEnemyMantaRay(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    // Wide delta-wing body
    val wingPath = PathPool.acquire().apply {
        moveTo(cx, cy + h * 0.45f)              // tail tip (rear)
        cubicTo(cx + w * 0.20f, cy + h * 0.20f,
            cx + w * 0.45f, cy + h * 0.05f,
            cx + w * 0.48f, cy - h * 0.15f)     // right wingtip
        cubicTo(cx + w * 0.30f, cy - h * 0.30f,
            cx + w * 0.10f, cy - h * 0.30f,
            cx, cy - h * 0.18f)                 // front center (head)
        cubicTo(cx - w * 0.10f, cy - h * 0.30f,
            cx - w * 0.30f, cy - h * 0.30f,
            cx - w * 0.48f, cy - h * 0.15f)     // left wingtip
        cubicTo(cx - w * 0.45f, cy + h * 0.05f,
            cx - w * 0.20f, cy + h * 0.20f,
            cx, cy + h * 0.45f)
        close()
    }
    drawPath(wingPath, body)
    drawPath(wingPath, accent, style = Stroke(width = w * 0.025f))
    PathPool.release(wingPath)
    // 2 dorsal eyes (top of head)
    drawCircle(accent, w * 0.05f, Offset(cx - w * 0.07f, cy - h * 0.20f))
    drawCircle(accent, w * 0.05f, Offset(cx + w * 0.07f, cy - h * 0.20f))
    drawCircle(Color.White, w * 0.02f, Offset(cx - w * 0.07f, cy - h * 0.20f))
    drawCircle(Color.White, w * 0.02f, Offset(cx + w * 0.07f, cy - h * 0.20f))
    // Spine ridge (3 dots running back)
    for (i in 0 until 3) {
        drawCircle(accent.copy(alpha = 0.85f), w * 0.025f,
            Offset(cx, cy - h * 0.05f + i * h * 0.12f))
    }
}

/** Battle Mech — boxy alien ship with twin side cannons + viewport. */
private fun DrawScope.drawEnemyMech(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val bodyW = w * 0.50f
    val bodyH = h * 0.55f
    // Main hull (slightly trapezoidal)
    val hullPath = PathPool.acquire().apply {
        moveTo(cx - bodyW * 0.40f, cy - bodyH / 2f)
        lineTo(cx + bodyW * 0.40f, cy - bodyH / 2f)
        lineTo(cx + bodyW / 2f, cy + bodyH / 2f)
        lineTo(cx - bodyW / 2f, cy + bodyH / 2f)
        close()
    }
    drawPath(hullPath, body)
    drawPath(hullPath, accent, style = Stroke(width = w * 0.03f))
    PathPool.release(hullPath)
    // Viewport (top-front of hull) — face DOWN reading direction
    drawRect(accent,
        topLeft = Offset(cx - bodyW * 0.25f, cy - bodyH * 0.30f),
        size = Size(bodyW * 0.50f, bodyH * 0.15f))
    drawRect(Color.White.copy(alpha = 0.6f),
        topLeft = Offset(cx - bodyW * 0.22f, cy - bodyH * 0.28f),
        size = Size(bodyW * 0.44f, bodyH * 0.10f))
    // 2 side cannons (sticking out from sides at front)
    for (sign in intArrayOf(-1, 1)) {
        val cannonX = cx + sign * bodyW * 0.55f
        drawRect(body,
            topLeft = Offset(cannonX - w * 0.04f, cy - bodyH * 0.20f),
            size = Size(w * 0.08f, bodyH * 0.50f))
        // Cannon tip glow
        drawCircle(accent, w * 0.04f, Offset(cannonX, cy + bodyH * 0.30f))
    }
    // Antenna / sensor on top
    drawLine(accent,
        Offset(cx, cy - bodyH / 2f),
        Offset(cx, cy - bodyH * 0.75f),
        strokeWidth = w * 0.025f)
    drawCircle(Color(0xFFFF2D55), w * 0.03f, Offset(cx, cy - bodyH * 0.78f))
    // Center body marking (cross)
    drawLine(accent.copy(alpha = 0.6f),
        Offset(cx - w * 0.05f, cy), Offset(cx + w * 0.05f, cy),
        strokeWidth = w * 0.02f)
    drawLine(accent.copy(alpha = 0.6f),
        Offset(cx, cy - h * 0.05f), Offset(cx, cy + h * 0.05f),
        strokeWidth = w * 0.02f)
}

// ─────────────────────────────────────────────────────────────────────────
// Round 81 — 12 new boss shape recipes per user roster. Each distinct
// silhouette. NSFW versions are tasteful abstract (no literal anatomy
// render). Trump caricature uses generic "Tycoon" name (no real name).
// ─────────────────────────────────────────────────────────────────────────

/** Hen Mother — chicken silhouette: oval body + small head + beak + comb + 2 legs. */
private fun DrawScope.drawBossHenMother(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val bodyRx = w * 0.30f; val bodyRy = h * 0.25f
    // Egg-shape body
    drawOval(body, topLeft = Offset(cx - bodyRx, cy - bodyRy * 0.5f), size = Size(bodyRx * 2, bodyRy * 2))
    // Head circle
    val headR = w * 0.13f
    drawCircle(body, headR, Offset(cx - bodyRx * 0.85f, cy - bodyRy * 0.85f))
    // Beak (yellow triangle)
    val beakPath = PathPool.acquire().apply {
        moveTo(cx - bodyRx * 1.20f, cy - bodyRy * 0.85f)
        lineTo(cx - bodyRx * 1.50f, cy - bodyRy * 0.95f)
        lineTo(cx - bodyRx * 1.20f, cy - bodyRy * 0.65f)
        close()
    }
    drawPath(beakPath, Color(0xFFFFC020))
    PathPool.release(beakPath)
    // Comb (red 3-prong on top of head)
    for (i in 0 until 3) {
        val cx2 = cx - bodyRx * 0.85f - headR * 0.20f + i * headR * 0.20f
        val tipY = cy - bodyRy * 0.85f - headR - i * headR * 0.10f
        drawCircle(Color(0xFFFF2D55), headR * 0.18f, Offset(cx2, tipY))
    }
    // Eye
    drawCircle(Color.Black, headR * 0.18f, Offset(cx - bodyRx * 0.95f, cy - bodyRy * 0.95f))
    drawCircle(Color.White, headR * 0.07f, Offset(cx - bodyRx * 0.97f, cy - bodyRy * 0.97f))
    // 2 legs
    for (side in intArrayOf(-1, 1)) {
        drawLine(accent,
            Offset(cx + side * bodyRx * 0.30f, cy + bodyRy * 0.85f),
            Offset(cx + side * bodyRx * 0.30f, cy + bodyRy * 1.30f),
            strokeWidth = w * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // Foot
        drawLine(accent,
            Offset(cx + side * bodyRx * 0.30f, cy + bodyRy * 1.30f),
            Offset(cx + side * bodyRx * 0.45f, cy + bodyRy * 1.35f),
            strokeWidth = w * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // Wing (curved)
    drawArc(accent, startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - bodyRx * 0.5f, cy - bodyRy * 0.3f),
        size = Size(bodyRx * 1.0f, bodyRy * 0.8f),
        style = Stroke(width = w * 0.04f))
    // Tail feathers (3 strokes at back)
    for (i in 0 until 3) {
        val a = (30.0 + i * 25) * Math.PI / 180.0
        val sx = cx + bodyRx * 0.95f
        val sy = cy
        val ex = sx + (w * 0.18f * kotlin.math.cos(a)).toFloat()
        val ey = sy + (w * 0.18f * kotlin.math.sin(a)).toFloat() - h * 0.05f
        drawLine(accent, Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

/** Buffalo Rage — bull head silhouette với 2 curved horns + nose ring + ferocious eyes. */
private fun DrawScope.drawBossBuffalo(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val headRx = w * 0.32f; val headRy = h * 0.30f
    // Head (oval)
    drawOval(body, topLeft = Offset(cx - headRx, cy - headRy), size = Size(headRx * 2, headRy * 2))
    // 2 curved horns (large)
    for (side in intArrayOf(-1, 1)) {
        val baseX = cx + side * headRx * 0.85f
        val baseY = cy - headRy * 0.50f
        val tipX = cx + side * headRx * 1.50f
        val tipY = cy - headRy * 1.10f
        val hornPath = PathPool.acquire().apply {
            moveTo(baseX, baseY)
            cubicTo(cx + side * headRx * 1.20f, cy - headRy * 0.85f,
                cx + side * headRx * 1.50f, cy - headRy * 0.95f,
                tipX, tipY)
            cubicTo(cx + side * headRx * 1.30f, cy - headRy * 0.75f,
                cx + side * headRx * 1.05f, cy - headRy * 0.40f,
                baseX, baseY)
            close()
        }
        drawPath(hornPath, body)
        drawPath(hornPath, Color.White.copy(alpha = 0.5f), style = Stroke(width = w * 0.02f))
        PathPool.release(hornPath)
    }
    // Snout (lighter front)
    drawOval(accent.copy(alpha = 0.7f),
        topLeft = Offset(cx - headRx * 0.4f, cy + headRy * 0.25f),
        size = Size(headRx * 0.8f, headRy * 0.55f))
    // Nose ring
    drawCircle(Color(0xFFFFC020), headRx * 0.15f, Offset(cx, cy + headRy * 0.50f),
        style = Stroke(width = w * 0.04f))
    // 2 nostrils
    drawCircle(Color.Black, headRx * 0.06f, Offset(cx - headRx * 0.12f, cy + headRy * 0.35f))
    drawCircle(Color.Black, headRx * 0.06f, Offset(cx + headRx * 0.12f, cy + headRy * 0.35f))
    // 2 angry eyes
    drawCircle(Color(0xFFFF2D55), headRx * 0.12f, Offset(cx - headRx * 0.35f, cy - headRy * 0.15f))
    drawCircle(Color(0xFFFF2D55), headRx * 0.12f, Offset(cx + headRx * 0.35f, cy - headRy * 0.15f))
    drawCircle(Color.White, headRx * 0.04f, Offset(cx - headRx * 0.30f, cy - headRy * 0.20f))
    drawCircle(Color.White, headRx * 0.04f, Offset(cx + headRx * 0.40f, cy - headRy * 0.20f))
}

/** Dumb Rat — large mouse head với 2 round ears + long whiskers + buck teeth. */
private fun DrawScope.drawBossRat(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val headR = w * 0.30f
    // Head circle
    drawCircle(body, headR, Offset(cx, cy))
    // 2 large round ears (mouse style)
    drawCircle(body, headR * 0.55f, Offset(cx - headR * 0.85f, cy - headR * 0.85f))
    drawCircle(body, headR * 0.55f, Offset(cx + headR * 0.85f, cy - headR * 0.85f))
    drawCircle(accent.copy(alpha = 0.7f), headR * 0.35f, Offset(cx - headR * 0.85f, cy - headR * 0.85f))
    drawCircle(accent.copy(alpha = 0.7f), headR * 0.35f, Offset(cx + headR * 0.85f, cy - headR * 0.85f))
    // Snout (smaller circle on bottom)
    drawCircle(body, headR * 0.55f, Offset(cx, cy + headR * 0.55f))
    // Nose
    drawCircle(Color(0xFFFF80B0), headR * 0.10f, Offset(cx, cy + headR * 0.40f))
    // 2 buck teeth (long rectangles below nose)
    drawRect(Color.White,
        topLeft = Offset(cx - headR * 0.10f, cy + headR * 0.55f),
        size = Size(headR * 0.08f, headR * 0.30f))
    drawRect(Color.White,
        topLeft = Offset(cx + headR * 0.02f, cy + headR * 0.55f),
        size = Size(headR * 0.08f, headR * 0.30f))
    // Eyes (dumb confused look)
    drawCircle(Color.White, headR * 0.18f, Offset(cx - headR * 0.30f, cy - headR * 0.10f))
    drawCircle(Color.White, headR * 0.18f, Offset(cx + headR * 0.30f, cy - headR * 0.10f))
    drawCircle(Color.Black, headR * 0.10f, Offset(cx - headR * 0.28f, cy - headR * 0.05f))
    drawCircle(Color.Black, headR * 0.10f, Offset(cx + headR * 0.32f, cy - headR * 0.05f))
    // Whiskers (3 each side)
    for (side in intArrayOf(-1, 1)) for (i in 0 until 3) {
        val ang = (i - 1) * 15.0 * Math.PI / 180.0
        val sx = cx + side * headR * 0.20f
        val sy = cy + headR * 0.40f
        val ex = sx + side * (headR * 0.85f) * kotlin.math.cos(ang).toFloat()
        val ey = sy + (headR * 0.50f) * kotlin.math.sin(ang).toFloat()
        drawLine(accent.copy(alpha = 0.85f), Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.015f)
    }
}

/** Fierce Tiger — fierce tiger head với stripes + fangs + mane fluff. */
private fun DrawScope.drawBossTiger(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val headR = w * 0.34f
    val tigerOrange = body                               // Wave 17q — thân theo bossColorFor (per-kind); chi tiết (sọc/mắt) giữ nguyên
    val stripeBlack = Color(0xFF202020)
    // Mane fluff (8 spikes around)
    for (i in 0 until 8) {
        val a = i * 2.0 * Math.PI / 8
        val sx = cx + (headR * 0.95f * kotlin.math.cos(a)).toFloat()
        val sy = cy + (headR * 0.95f * kotlin.math.sin(a)).toFloat()
        val ex = cx + (headR * 1.25f * kotlin.math.cos(a)).toFloat()
        val ey = cy + (headR * 1.25f * kotlin.math.sin(a)).toFloat()
        drawLine(tigerOrange, Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // Main head (orange)
    drawCircle(tigerOrange, headR, Offset(cx, cy))
    // Stripes (5 black curves across face)
    for (i in 0 until 5) {
        val angOff = (i - 2) * 18.0 * Math.PI / 180.0
        drawArc(stripeBlack,
            startAngle = (40f + i * 25f), sweepAngle = 25f,
            useCenter = false,
            topLeft = Offset(cx - headR * 0.85f, cy - headR * 0.55f),
            size = Size(headR * 1.7f, headR * 1.5f),
            style = Stroke(width = w * 0.03f))
    }
    // 2 ears (triangular)
    for (side in intArrayOf(-1, 1)) {
        val earPath = PathPool.acquire().apply {
            moveTo(cx + side * headR * 0.60f, cy - headR * 0.95f)
            lineTo(cx + side * headR * 0.40f, cy - headR * 0.40f)
            lineTo(cx + side * headR * 0.85f, cy - headR * 0.55f)
            close()
        }
        drawPath(earPath, tigerOrange)
        PathPool.release(earPath)
    }
    // Eyes (fierce yellow)
    drawCircle(Color(0xFFFFE040), headR * 0.18f, Offset(cx - headR * 0.32f, cy - headR * 0.05f))
    drawCircle(Color(0xFFFFE040), headR * 0.18f, Offset(cx + headR * 0.32f, cy - headR * 0.05f))
    drawCircle(Color.Black, headR * 0.06f, Offset(cx - headR * 0.32f, cy - headR * 0.05f))
    drawCircle(Color.Black, headR * 0.06f, Offset(cx + headR * 0.32f, cy - headR * 0.05f))
    // Snout + open mouth with fangs
    drawCircle(Color.White.copy(alpha = 0.8f), headR * 0.18f, Offset(cx, cy + headR * 0.30f))
    drawCircle(Color.Black, headR * 0.08f, Offset(cx, cy + headR * 0.25f))
    // 2 fangs (white triangles going down from mouth)
    for (side in intArrayOf(-1, 1)) {
        val fpath = PathPool.acquire().apply {
            moveTo(cx + side * headR * 0.12f, cy + headR * 0.40f)
            lineTo(cx + side * headR * 0.04f, cy + headR * 0.40f)
            lineTo(cx + side * headR * 0.08f, cy + headR * 0.65f)
            close()
        }
        drawPath(fpath, Color.White)
        PathPool.release(fpath)
    }
}

/** Sexy Diva — feminine silhouette with hourglass shape + hair flow + crown gem. */
private fun DrawScope.drawBossDiva(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val divaPink = Color(0xFFFF60A0)
    // Hair (long flowing on sides)
    for (side in intArrayOf(-1, 1)) {
        val hairPath = PathPool.acquire().apply {
            moveTo(cx + side * w * 0.10f, cy - h * 0.40f)
            cubicTo(cx + side * w * 0.40f, cy - h * 0.30f,
                cx + side * w * 0.45f, cy - h * 0.10f,
                cx + side * w * 0.50f, cy + h * 0.20f)
            cubicTo(cx + side * w * 0.55f, cy + h * 0.30f,
                cx + side * w * 0.50f, cy + h * 0.40f,
                cx + side * w * 0.45f, cy + h * 0.50f)
            lineTo(cx + side * w * 0.20f, cy)
            close()
        }
        drawPath(hairPath, body)
        PathPool.release(hairPath)
    }
    // Head (round)
    drawCircle(divaPink, w * 0.15f, Offset(cx, cy - h * 0.25f))
    // Crown gem (top of head)
    val gemPath = PathPool.acquire().apply {
        moveTo(cx, cy - h * 0.48f)
        lineTo(cx + w * 0.06f, cy - h * 0.38f)
        lineTo(cx, cy - h * 0.30f)
        lineTo(cx - w * 0.06f, cy - h * 0.38f)
        close()
    }
    drawPath(gemPath, Color(0xFFFFD700))
    drawPath(gemPath, Color.White.copy(alpha = 0.7f), style = Stroke(width = w * 0.015f))
    PathPool.release(gemPath)
    // Eyes (closed/sultry — 2 curved lines)
    drawArc(Color.Black, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx - w * 0.10f, cy - h * 0.30f),
        size = Size(w * 0.06f, h * 0.04f),
        style = Stroke(width = w * 0.012f))
    drawArc(Color.Black, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx + w * 0.04f, cy - h * 0.30f),
        size = Size(w * 0.06f, h * 0.04f),
        style = Stroke(width = w * 0.012f))
    // Lips (heart-shape red)
    drawCircle(Color(0xFFCC2030), w * 0.018f, Offset(cx - w * 0.025f, cy - h * 0.20f))
    drawCircle(Color(0xFFCC2030), w * 0.018f, Offset(cx + w * 0.025f, cy - h * 0.20f))
    val lipsTri = PathPool.acquire().apply {
        moveTo(cx - w * 0.04f, cy - h * 0.18f)
        lineTo(cx + w * 0.04f, cy - h * 0.18f)
        lineTo(cx, cy - h * 0.13f)
        close()
    }
    drawPath(lipsTri, Color(0xFFCC2030))
    PathPool.release(lipsTri)
    // Body (hourglass — neck + shoulders + waist + hips)
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.06f, cy - h * 0.12f)
        cubicTo(cx - w * 0.25f, cy - h * 0.05f, cx - w * 0.30f, cy + h * 0.05f, cx - w * 0.10f, cy + h * 0.15f)
        cubicTo(cx - w * 0.25f, cy + h * 0.30f, cx - w * 0.28f, cy + h * 0.40f, cx - w * 0.10f, cy + h * 0.50f)
        lineTo(cx + w * 0.10f, cy + h * 0.50f)
        cubicTo(cx + w * 0.28f, cy + h * 0.40f, cx + w * 0.25f, cy + h * 0.30f, cx + w * 0.10f, cy + h * 0.15f)
        cubicTo(cx + w * 0.30f, cy + h * 0.05f, cx + w * 0.25f, cy - h * 0.05f, cx + w * 0.06f, cy - h * 0.12f)
        close()
    }
    drawPath(bodyPath, body)
    PathPool.release(bodyPath)
}

/** Troll Tower — tall pointed obelisk tower with crown + glowing tip (tasteful phallic). */
private fun DrawScope.drawBossTrollTower(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    // Tall tapered spire (very tall, narrow)
    val spirePath = PathPool.acquire().apply {
        moveTo(cx, cy - h * 0.45f)
        lineTo(cx + w * 0.10f, cy - h * 0.30f)
        lineTo(cx + w * 0.15f, cy + h * 0.30f)
        lineTo(cx + w * 0.22f, cy + h * 0.45f)
        lineTo(cx - w * 0.22f, cy + h * 0.45f)
        lineTo(cx - w * 0.15f, cy + h * 0.30f)
        lineTo(cx - w * 0.10f, cy - h * 0.30f)
        close()
    }
    drawPath(spirePath, body)
    drawPath(spirePath, accent, style = Stroke(width = w * 0.025f))
    PathPool.release(spirePath)
    // Crown of jewels at top
    for (i in 0 until 5) {
        val px = cx - w * 0.10f + i * w * 0.05f
        drawCircle(Color(0xFFFFD700), w * 0.022f, Offset(px, cy - h * 0.42f))
    }
    // Tip glow (bright white)
    drawCircle(Color.White, w * 0.06f, Offset(cx, cy - h * 0.45f))
    drawCircle(Color.White.copy(alpha = 0.5f), w * 0.10f, Offset(cx, cy - h * 0.45f))
    // Energy rings around tip (3 expanding circles)
    for (i in 0 until 3) {
        drawCircle(accent.copy(alpha = 0.4f - i * 0.10f), w * (0.12f + i * 0.06f),
            Offset(cx, cy - h * 0.42f),
            style = Stroke(width = w * 0.012f))
    }
    // Stripes on shaft (3 horizontal)
    for (i in 0 until 3) {
        val y = cy - h * 0.20f + i * h * 0.20f
        drawLine(accent.copy(alpha = 0.6f),
            Offset(cx - w * 0.14f, y), Offset(cx + w * 0.14f, y),
            strokeWidth = w * 0.015f)
    }
}

/** Twin Summits — 2 dome peaks side by side (tasteful twin domes for nhũ hoa). */
private fun DrawScope.drawBossTwinSummits(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val domeRx = w * 0.24f; val domeRy = h * 0.30f
    // 2 large dome curves
    for (side in intArrayOf(-1, 1)) {
        val px = cx + side * w * 0.20f
        drawArc(body, startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(px - domeRx, cy - domeRy * 0.5f),
            size = Size(domeRx * 2, domeRy * 2))
        drawArc(accent, startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(px - domeRx, cy - domeRy * 0.5f),
            size = Size(domeRx * 2, domeRy * 2),
            style = Stroke(width = w * 0.025f))
        // Glowing tip (cannon mouth — milk ray source)
        drawCircle(Color.White.copy(alpha = 0.9f), domeRx * 0.18f, Offset(px, cy - domeRy * 0.30f))
        drawCircle(accent, domeRx * 0.10f, Offset(px, cy - domeRy * 0.30f))
    }
    // Connecting base
    drawRect(body,
        topLeft = Offset(cx - w * 0.40f, cy + domeRy * 0.30f),
        size = Size(w * 0.80f, h * 0.15f))
    drawRect(accent,
        topLeft = Offset(cx - w * 0.40f, cy + domeRy * 0.30f),
        size = Size(w * 0.80f, h * 0.15f),
        style = Stroke(width = w * 0.025f))
    // 2 milk-ray emission dots between domes
    drawCircle(Color.White, w * 0.05f, Offset(cx - w * 0.20f, cy - domeRy * 0.10f))
    drawCircle(Color.White, w * 0.05f, Offset(cx + w * 0.20f, cy - domeRy * 0.10f))
    // Glow halos around tips
    drawCircle(Color.White.copy(alpha = 0.30f), domeRx * 0.35f, Offset(cx - w * 0.20f, cy - domeRy * 0.30f))
    drawCircle(Color.White.copy(alpha = 0.30f), domeRx * 0.35f, Offset(cx + w * 0.20f, cy - domeRy * 0.30f))
}

/** Void Globes — 2 large dark spheres clustered (tasteful for cặp mông). */
private fun DrawScope.drawBossVoidGlobes(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val globeR = w * 0.26f
    val darkBody = Color(0xFF202020)
    // 2 large spheres side by side
    drawCircle(darkBody, globeR, Offset(cx - w * 0.18f, cy + h * 0.05f))
    drawCircle(darkBody, globeR, Offset(cx + w * 0.18f, cy + h * 0.05f))
    // Body color overlay (slight tint)
    drawCircle(body.copy(alpha = 0.55f), globeR, Offset(cx - w * 0.18f, cy + h * 0.05f))
    drawCircle(body.copy(alpha = 0.55f), globeR, Offset(cx + w * 0.18f, cy + h * 0.05f))
    // Outline
    drawCircle(accent, globeR, Offset(cx - w * 0.18f, cy + h * 0.05f),
        style = Stroke(width = w * 0.02f))
    drawCircle(accent, globeR, Offset(cx + w * 0.18f, cy + h * 0.05f),
        style = Stroke(width = w * 0.02f))
    // Glow highlights upper-left
    drawCircle(Color.White.copy(alpha = 0.25f), globeR * 0.3f,
        Offset(cx - w * 0.25f, cy - h * 0.05f))
    drawCircle(Color.White.copy(alpha = 0.25f), globeR * 0.3f,
        Offset(cx + w * 0.11f, cy - h * 0.05f))
    // Center cluster spit (3 brown blobs between)
    for (i in 0 until 3) {
        drawCircle(Color(0xFF6B3010), w * 0.04f - i * w * 0.008f,
            Offset(cx, cy + h * 0.25f + i * h * 0.08f))
    }
    // 2 cleft lines (subtle, accents the rounded form)
    drawLine(Color.Black.copy(alpha = 0.3f),
        Offset(cx - w * 0.18f, cy - h * 0.15f), Offset(cx - w * 0.18f, cy + h * 0.25f),
        strokeWidth = w * 0.015f)
    drawLine(Color.Black.copy(alpha = 0.3f),
        Offset(cx + w * 0.18f, cy - h * 0.15f), Offset(cx + w * 0.18f, cy + h * 0.25f),
        strokeWidth = w * 0.015f)
}

/** White Dragon — long sinuous dragon body với 2 horns + blue eyes + claws + breath flame. */
private fun DrawScope.drawBossWhiteDragon(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val white = Color(0xFFE8E8F0)
    val blueEye = Color(0xFF0080FF)
    val flameColor = Color(0xFFFF6020)
    // Sinuous body (S-shape using cubic)
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx + w * 0.30f, cy + h * 0.45f)              // tail tip
        cubicTo(cx + w * 0.10f, cy + h * 0.30f,
            cx - w * 0.20f, cy + h * 0.10f,
            cx - w * 0.10f, cy - h * 0.10f)
        cubicTo(cx + w * 0.05f, cy - h * 0.25f,
            cx + w * 0.15f, cy - h * 0.30f,
            cx + w * 0.05f, cy - h * 0.40f)                  // head
        cubicTo(cx - w * 0.05f, cy - h * 0.30f,
            cx - w * 0.20f, cy - h * 0.20f,
            cx - w * 0.20f, cy + h * 0.05f)
        cubicTo(cx - w * 0.05f, cy + h * 0.25f,
            cx + w * 0.20f, cy + h * 0.40f,
            cx + w * 0.30f, cy + h * 0.45f)
        close()
    }
    drawPath(bodyPath, white)
    drawPath(bodyPath, accent, style = Stroke(width = w * 0.02f))
    PathPool.release(bodyPath)
    // Head detail at top
    drawCircle(white, w * 0.10f, Offset(cx + w * 0.05f, cy - h * 0.35f))
    // 2 horns
    for (side in intArrayOf(-1, 1)) {
        drawLine(white,
            Offset(cx + w * 0.05f + side * w * 0.05f, cy - h * 0.42f),
            Offset(cx + w * 0.05f + side * w * 0.10f, cy - h * 0.50f),
            strokeWidth = w * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // Blue eyes (2 small)
    drawCircle(blueEye, w * 0.025f, Offset(cx + w * 0.02f, cy - h * 0.36f))
    drawCircle(blueEye, w * 0.025f, Offset(cx + w * 0.10f, cy - h * 0.36f))
    drawCircle(Color.White, w * 0.010f, Offset(cx + w * 0.02f, cy - h * 0.37f))
    drawCircle(Color.White, w * 0.010f, Offset(cx + w * 0.10f, cy - h * 0.37f))
    // Fire breath (3 flame tongues from mouth)
    for (i in 0 until 3) {
        val angDeg = -120f + i * 12f
        val a = angDeg * Math.PI / 180.0
        val sx = cx + w * 0.05f
        val sy = cy - h * 0.30f
        val ex = sx + (w * 0.30f * kotlin.math.cos(a)).toFloat()
        val ey = sy + (w * 0.30f * kotlin.math.sin(a)).toFloat()
        drawLine(flameColor,
            Offset(sx, sy), Offset(ex, ey),
            strokeWidth = w * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // 2 claws (small triangles)
    for ((x, y) in listOf(cx - w * 0.10f to cy + h * 0.15f, cx + w * 0.20f to cy + h * 0.20f)) {
        val cpath = PathPool.acquire().apply {
            moveTo(x, y)
            lineTo(x - w * 0.03f, y + h * 0.05f)
            lineTo(x - w * 0.06f, y - h * 0.02f)
            close()
        }
        drawPath(cpath, accent)
        PathPool.release(cpath)
    }
}

/** Hammer and Sickle — combined symbol Boss (cộng sản). Red star background. */
private fun DrawScope.drawBossHammerSickle(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val red = body                                       // Wave 17q — đĩa nền theo bossColorFor; búa-liềm vàng giữ nguyên
    val gold = Color(0xFFFFD700)
    // Red circular background
    drawCircle(red, w * 0.42f, Offset(cx, cy))
    drawCircle(gold, w * 0.42f, Offset(cx, cy), style = Stroke(width = w * 0.025f))
    // Sickle (curved blade left)
    val sicklePath = PathPool.acquire().apply {
        moveTo(cx - w * 0.25f, cy - h * 0.05f)
        cubicTo(cx - w * 0.30f, cy - h * 0.20f,
            cx - w * 0.10f, cy - h * 0.32f,
            cx + w * 0.10f, cy - h * 0.30f)
        cubicTo(cx + w * 0.05f, cy - h * 0.20f,
            cx - w * 0.10f, cy - h * 0.18f,
            cx - w * 0.18f, cy - h * 0.05f)
        close()
    }
    drawPath(sicklePath, gold)
    PathPool.release(sicklePath)
    // Sickle handle (curved line)
    drawArc(gold,
        startAngle = 90f, sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - w * 0.30f, cy - h * 0.05f),
        size = Size(w * 0.30f, h * 0.30f),
        style = Stroke(width = w * 0.03f))
    // Hammer head (rectangle at right)
    drawRect(gold,
        topLeft = Offset(cx + w * 0.05f, cy - h * 0.05f),
        size = Size(w * 0.25f, h * 0.10f))
    // Hammer handle (diagonal line)
    drawLine(gold,
        Offset(cx + w * 0.10f, cy),
        Offset(cx - w * 0.05f, cy + h * 0.25f),
        strokeWidth = w * 0.04f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // Crossing emphasis
    drawCircle(gold, w * 0.04f, Offset(cx + w * 0.10f, cy))
    // 5-point star at top
    val starOuter = w * 0.10f
    val starInner = starOuter * 0.42f
    val starCx = cx; val starCy = cy - h * 0.32f
    val starPath = PathPool.acquire().apply {
        val rotation = -Math.PI / 2.0
        for (i in 0 until 10) {
            val a = rotation + i * Math.PI / 5
            val r = if (i % 2 == 0) starOuter else starInner
            val x = starCx + (r * kotlin.math.cos(a)).toFloat()
            val y = starCy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(starPath, gold)
    PathPool.release(starPath)
}

/** Money Tycoon — fat boss in suit + money bag + dollar signs (tư bản). */
private fun DrawScope.drawBossMoneyTycoon(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val moneyGreen = Color(0xFF208040)
    val gold = Color(0xFFFFD700)
    // Round body (fat tycoon torso)
    drawCircle(body, w * 0.35f, Offset(cx, cy + h * 0.05f))
    drawCircle(accent, w * 0.35f, Offset(cx, cy + h * 0.05f), style = Stroke(width = w * 0.025f))
    // Head (round)
    drawCircle(body, w * 0.16f, Offset(cx, cy - h * 0.30f))
    // Top hat (rectangle + brim)
    drawRect(Color.Black,
        topLeft = Offset(cx - w * 0.12f, cy - h * 0.52f),
        size = Size(w * 0.24f, h * 0.15f))
    drawRect(Color.Black,
        topLeft = Offset(cx - w * 0.20f, cy - h * 0.40f),
        size = Size(w * 0.40f, h * 0.04f))
    // Hat band (gold)
    drawRect(gold,
        topLeft = Offset(cx - w * 0.12f, cy - h * 0.43f),
        size = Size(w * 0.24f, h * 0.03f))
    // Monocle (right eye)
    drawCircle(Color.White.copy(alpha = 0.7f), w * 0.05f, Offset(cx + w * 0.06f, cy - h * 0.30f))
    drawCircle(Color.Black, w * 0.05f, Offset(cx + w * 0.06f, cy - h * 0.30f), style = Stroke(width = w * 0.015f))
    drawLine(Color.Black,
        Offset(cx + w * 0.10f, cy - h * 0.28f),
        Offset(cx + w * 0.18f, cy - h * 0.20f),
        strokeWidth = w * 0.015f)
    // Left eye (small dot)
    drawCircle(Color.Black, w * 0.018f, Offset(cx - w * 0.06f, cy - h * 0.30f))
    // Mustache (curved)
    drawLine(Color.Black,
        Offset(cx - w * 0.08f, cy - h * 0.22f),
        Offset(cx + w * 0.08f, cy - h * 0.22f),
        strokeWidth = w * 0.025f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // $ sign on belly (big)
    val dollarSize = w * 0.15f
    drawArc(gold, startAngle = 60f, sweepAngle = 240f, useCenter = false,
        topLeft = Offset(cx - dollarSize, cy - dollarSize * 0.10f),
        size = Size(dollarSize * 2, dollarSize * 0.8f),
        style = Stroke(width = w * 0.05f))
    drawArc(gold, startAngle = 240f, sweepAngle = 240f, useCenter = false,
        topLeft = Offset(cx - dollarSize, cy + dollarSize * 0.20f),
        size = Size(dollarSize * 2, dollarSize * 0.8f),
        style = Stroke(width = w * 0.05f))
    drawLine(gold,
        Offset(cx, cy - dollarSize * 0.25f),
        Offset(cx, cy + dollarSize * 0.85f),
        strokeWidth = w * 0.04f)
    // Money bag in left hand (small sack with $ on it)
    val bagX = cx - w * 0.45f
    val bagY = cy + h * 0.20f
    drawCircle(Color(0xFF8B5C2E), w * 0.10f, Offset(bagX, bagY))
    drawCircle(moneyGreen, w * 0.04f, Offset(bagX, bagY))
}

/** Golden Tycoon — orange-hair caricature (Trump-inspired, no real name) + golden tie. */
private fun DrawScope.drawBossGoldenTycoon(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val orangeHair = Color(0xFFFF9520)
    val gold = Color(0xFFFFC020)
    val flesh = Color(0xFFFFB890)
    // Head (round, flesh)
    drawCircle(flesh, w * 0.22f, Offset(cx, cy - h * 0.15f))
    // Distinctive orange hair (swept forward — caricature)
    val hairPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.25f, cy - h * 0.25f)
        cubicTo(cx - w * 0.30f, cy - h * 0.40f,
            cx + w * 0.10f, cy - h * 0.50f,
            cx + w * 0.30f, cy - h * 0.35f)
        cubicTo(cx + w * 0.20f, cy - h * 0.30f,
            cx + w * 0.10f, cy - h * 0.32f,
            cx + w * 0.05f, cy - h * 0.28f)
        cubicTo(cx - w * 0.10f, cy - h * 0.32f,
            cx - w * 0.15f, cy - h * 0.30f,
            cx - w * 0.25f, cy - h * 0.25f)
        close()
    }
    drawPath(hairPath, orangeHair)
    PathPool.release(hairPath)
    // Hair detail strokes (sweep lines)
    for (i in 0 until 4) {
        val sx = cx - w * 0.15f + i * w * 0.10f
        drawLine(Color(0xFFCC6010),
            Offset(sx, cy - h * 0.35f),
            Offset(sx + w * 0.05f, cy - h * 0.30f),
            strokeWidth = w * 0.012f)
    }
    // Eyes (small narrow with bag)
    drawArc(Color.Black, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx - w * 0.13f, cy - h * 0.18f),
        size = Size(w * 0.08f, h * 0.05f),
        style = Stroke(width = w * 0.014f))
    drawArc(Color.Black, startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx + w * 0.05f, cy - h * 0.18f),
        size = Size(w * 0.08f, h * 0.05f),
        style = Stroke(width = w * 0.014f))
    // Mouth (pursed/pouty)
    drawCircle(Color(0xFFCC2020), w * 0.04f, Offset(cx, cy - h * 0.02f))
    // Suit (dark body)
    val suitPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.18f, cy + h * 0.05f)
        lineTo(cx + w * 0.18f, cy + h * 0.05f)
        lineTo(cx + w * 0.35f, cy + h * 0.50f)
        lineTo(cx - w * 0.35f, cy + h * 0.50f)
        close()
    }
    drawPath(suitPath, Color(0xFF202040))
    PathPool.release(suitPath)
    // White shirt collar
    val collarPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.08f, cy + h * 0.05f)
        lineTo(cx + w * 0.08f, cy + h * 0.05f)
        lineTo(cx, cy + h * 0.18f)
        close()
    }
    drawPath(collarPath, Color.White)
    PathPool.release(collarPath)
    // Golden tie (long, distinctive)
    val tiePath = PathPool.acquire().apply {
        moveTo(cx - w * 0.04f, cy + h * 0.08f)
        lineTo(cx + w * 0.04f, cy + h * 0.08f)
        lineTo(cx + w * 0.05f, cy + h * 0.30f)
        lineTo(cx, cy + h * 0.45f)
        lineTo(cx - w * 0.05f, cy + h * 0.30f)
        close()
    }
    drawPath(tiePath, gold)
    drawPath(tiePath, Color(0xFFCC9000), style = Stroke(width = w * 0.012f))
    PathPool.release(tiePath)
    // 3 dollar bills floating (visual hint at attack pattern)
    for (i in 0 until 3) {
        val bx = cx - w * 0.45f + i * w * 0.45f
        val by = cy + h * 0.40f - i * h * 0.05f
        drawRect(Color(0xFF80C080),
            topLeft = Offset(bx, by),
            size = Size(w * 0.12f, h * 0.07f))
        drawRect(Color(0xFF208040),
            topLeft = Offset(bx, by),
            size = Size(w * 0.12f, h * 0.07f),
            style = Stroke(width = w * 0.012f))
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Round 82 (boss + enemy wire) — 10 R81 enemy in-game shape recipes (mirror
// of InfoScreen previews; signature matches drawXXX(cx, cy, wPx, hPx, body, accent)).
// ─────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawEnemySpinningSaw(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val outerR = minOf(w, h) * 0.42f
    val toothCount = 12
    val toothPath = PathPool.acquire().apply {
        for (i in 0 until toothCount * 2) {
            val a = -Math.PI / 2 + i * Math.PI / toothCount
            val r = if (i % 2 == 0) outerR else outerR * 0.78f
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(toothPath, body)
    PathPool.release(toothPath)
    drawCircle(accent, outerR * 0.40f, Offset(cx, cy))
    drawCircle(Color.Black, outerR * 0.15f, Offset(cx, cy))
}

private fun DrawScope.drawEnemyTentacleSquid(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val cxLocal = cx; val cyLocal = cy - h * 0.10f
    val headR = w * 0.28f
    drawCircle(body, headR, Offset(cxLocal, cyLocal))
    for (i in 0 until 6) {
        val baseAngle = -90.0 + (i - 2.5) * 22.0
        val a = baseAngle * Math.PI / 180.0
        val bx = cxLocal + (headR * kotlin.math.cos(a)).toFloat()
        val by = cyLocal + (headR * kotlin.math.sin(a)).toFloat()
        val ex = bx + (headR * 0.85f * kotlin.math.cos(a)).toFloat()
        val ey = by + headR * 1.20f
        val tPath = PathPool.acquire().apply {
            moveTo(bx, by)
            cubicTo(bx + (i - 2.5).toFloat() * w * 0.05f, by + h * 0.10f,
                ex - (i - 2.5).toFloat() * w * 0.03f, ey - h * 0.05f, ex, ey)
        }
        drawPath(tPath, body,
            style = Stroke(width = w * 0.035f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        PathPool.release(tPath)
    }
    drawCircle(accent, headR * 0.18f, Offset(cxLocal - headR * 0.30f, cyLocal))
    drawCircle(accent, headR * 0.18f, Offset(cxLocal + headR * 0.30f, cyLocal))
    drawCircle(Color.Black, headR * 0.08f, Offset(cxLocal - headR * 0.30f, cyLocal))
    drawCircle(Color.Black, headR * 0.08f, Offset(cxLocal + headR * 0.30f, cyLocal))
}

private fun DrawScope.drawEnemyMineLayer(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val bodyW = w * 0.60f; val bodyH = h * 0.35f
    drawRect(body, topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f), size = Size(bodyW, bodyH))
    drawRect(accent, topLeft = Offset(cx - bodyW / 2f, cy - bodyH / 2f), size = Size(bodyW, bodyH),
        style = Stroke(width = w * 0.03f))
    drawCircle(body, w * 0.10f, Offset(cx, cy - bodyH * 0.40f))
    drawRect(Color(0xFF404040),
        topLeft = Offset(cx - bodyW * 0.55f, cy + bodyH * 0.45f),
        size = Size(bodyW * 1.10f, h * 0.10f))
    for (i in 0 until 3) {
        drawCircle(Color(0xFFFF2D55), w * 0.04f, Offset(cx - w * 0.20f + i * w * 0.20f, cy + h * 0.40f))
    }
}

private fun DrawScope.drawEnemyShieldDrone(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val bodyR = w * 0.18f
    val hexPath = PathPool.acquire().apply {
        for (i in 0 until 6) {
            val a = i * Math.PI / 3
            val x = cx + (bodyR * kotlin.math.cos(a)).toFloat()
            val y = cy + (bodyR * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(hexPath, body)
    PathPool.release(hexPath)
    drawArc(accent.copy(alpha = 0.55f),
        startAngle = 30f, sweepAngle = 120f, useCenter = false,
        topLeft = Offset(cx - w * 0.38f, cy - h * 0.05f),
        size = Size(w * 0.76f, h * 0.40f),
        style = Stroke(width = w * 0.04f))
    drawArc(Color.White.copy(alpha = 0.30f),
        startAngle = 30f, sweepAngle = 120f, useCenter = true,
        topLeft = Offset(cx - w * 0.38f, cy - h * 0.05f),
        size = Size(w * 0.76f, h * 0.40f))
    drawCircle(Color(0xFFFF2D55), bodyR * 0.4f, Offset(cx, cy))
}

private fun DrawScope.drawEnemySniper(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    drawRect(body,
        topLeft = Offset(cx - w * 0.05f, cy - h * 0.40f),
        size = Size(w * 0.10f, h * 0.80f))
    drawRect(accent,
        topLeft = Offset(cx - w * 0.10f, cy - h * 0.20f),
        size = Size(w * 0.20f, h * 0.10f))
    drawCircle(Color.Red, w * 0.025f, Offset(cx + w * 0.07f, cy - h * 0.15f))
    drawRect(body,
        topLeft = Offset(cx - w * 0.15f, cy + h * 0.30f),
        size = Size(w * 0.30f, h * 0.10f))
    drawCircle(Color(0xFFFFE040), w * 0.06f, Offset(cx, cy - h * 0.42f))
    drawCircle(Color.White, w * 0.025f, Offset(cx, cy - h * 0.42f))
}

private fun DrawScope.drawEnemyBomberCrawler(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    drawRect(body,
        topLeft = Offset(cx - w * 0.40f, cy - h * 0.20f),
        size = Size(w * 0.80f, h * 0.30f))
    drawRect(accent,
        topLeft = Offset(cx - w * 0.40f, cy - h * 0.20f),
        size = Size(w * 0.80f, h * 0.30f),
        style = Stroke(width = w * 0.025f))
    drawLine(body,
        Offset(cx, cy - h * 0.05f), Offset(cx + w * 0.20f, cy - h * 0.35f),
        strokeWidth = w * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    for (i in 0 until 4) {
        drawCircle(Color(0xFF404040), h * 0.06f, Offset(cx - w * 0.30f + i * w * 0.20f, cy + h * 0.18f))
    }
    drawCircle(Color(0xFFFFE040), w * 0.03f, Offset(cx + w * 0.22f, cy - h * 0.38f))
}

private fun DrawScope.drawEnemyMirrorTwin(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    for (sign in intArrayOf(-1, 1)) {
        val px = cx + sign * w * 0.18f
        val triPath = PathPool.acquire().apply {
            moveTo(px, cy - h * 0.20f)
            lineTo(px + w * 0.10f, cy + h * 0.20f)
            lineTo(px - w * 0.10f, cy + h * 0.20f)
            close()
        }
        drawPath(triPath, body)
        PathPool.release(triPath)
        drawCircle(accent, w * 0.04f, Offset(px, cy))
    }
    drawLine(accent, Offset(cx - w * 0.10f, cy), Offset(cx + w * 0.10f, cy),
        strokeWidth = w * 0.015f)
    drawLine(accent, Offset(cx - w * 0.05f, cy - h * 0.05f), Offset(cx + w * 0.05f, cy - h * 0.05f),
        strokeWidth = w * 0.01f)
    drawLine(accent, Offset(cx - w * 0.05f, cy + h * 0.05f), Offset(cx + w * 0.05f, cy + h * 0.05f),
        strokeWidth = w * 0.01f)
}

private fun DrawScope.drawEnemyPhantom(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val ghostPath = PathPool.acquire().apply {
        moveTo(cx - w * 0.30f, cy - h * 0.20f)
        cubicTo(cx - w * 0.35f, cy - h * 0.45f, cx + w * 0.35f, cy - h * 0.45f, cx + w * 0.30f, cy - h * 0.20f)
        lineTo(cx + w * 0.30f, cy + h * 0.30f)
        val waveCount = 5
        val waveStep = w * 0.60f / waveCount
        for (i in 0..waveCount) {
            val wx = cx + w * 0.30f - i * waveStep
            val wy = if (i % 2 == 0) cy + h * 0.40f else cy + h * 0.30f
            lineTo(wx, wy)
        }
        close()
    }
    drawPath(ghostPath, body.copy(alpha = 0.55f))
    drawPath(ghostPath, accent.copy(alpha = 0.85f), style = Stroke(width = w * 0.025f))
    PathPool.release(ghostPath)
    drawCircle(Color.Black.copy(alpha = 0.8f), w * 0.05f, Offset(cx - w * 0.10f, cy - h * 0.15f))
    drawCircle(Color.Black.copy(alpha = 0.8f), w * 0.05f, Offset(cx + w * 0.10f, cy - h * 0.15f))
    drawCircle(Color.Black.copy(alpha = 0.8f), w * 0.04f, Offset(cx, cy + h * 0.05f),
        style = Stroke(width = w * 0.015f))
}

private fun DrawScope.drawEnemyHealer(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    drawCircle(Color(0xFF60FFAA).copy(alpha = 0.25f), w * 0.42f, Offset(cx, cy))
    drawCircle(Color(0xFF60FFAA), w * 0.42f, Offset(cx, cy),
        style = Stroke(width = w * 0.02f))
    drawCircle(body, w * 0.18f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.8f), w * 0.10f, Offset(cx, cy))
    val armW = w * 0.04f
    val armL = w * 0.20f
    drawRect(Color(0xFF60FFAA),
        topLeft = Offset(cx - armW / 2f, cy - armL / 2f), size = Size(armW, armL))
    drawRect(Color(0xFF60FFAA),
        topLeft = Offset(cx - armL / 2f, cy - armW / 2f), size = Size(armL, armW))
    for (i in 0 until 4) {
        val a = i * Math.PI / 2
        drawCircle(Color(0xFF60FFAA), w * 0.025f,
            Offset(cx + (w * 0.32f * kotlin.math.cos(a)).toFloat(),
                cy + (w * 0.32f * kotlin.math.sin(a)).toFloat()))
    }
}

private fun DrawScope.drawEnemyKamikaze(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val bodyPath = PathPool.acquire().apply {
        moveTo(cx, cy - h * 0.30f)
        lineTo(cx + w * 0.30f, cy + h * 0.30f)
        lineTo(cx - w * 0.30f, cy + h * 0.30f)
        close()
    }
    drawPath(bodyPath, Color(0xFFFFE040))
    drawPath(bodyPath, Color.Black, style = Stroke(width = w * 0.04f))
    PathPool.release(bodyPath)
    drawRect(Color.Black,
        topLeft = Offset(cx - w * 0.02f, cy - h * 0.10f),
        size = Size(w * 0.04f, h * 0.20f))
    drawCircle(Color.Black, w * 0.025f, Offset(cx, cy + h * 0.18f))
    for (i in 0 until 3) {
        val sy = cy + h * 0.35f + i * h * 0.05f
        drawLine(Color(0xFFFF6020),
            Offset(cx - w * 0.20f + i * w * 0.05f, sy),
            Offset(cx + w * 0.20f - i * w * 0.05f, sy),
            strokeWidth = w * 0.02f)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Task 09 (đợt 3) — shape RIÊNG cho 5 địch chủ đề (Wave 9a hoàn tất).
// Dùng PathPool (hot-path) như các recipe khác.
// ─────────────────────────────────────────────────────────────────────────

/** Splitter: 2 nửa tam giác tách khỏi khe giữa ("phân thân"). */
private fun DrawScope.drawEnemySplitter(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val topY = cy - h * 0.35f; val botY = cy + h * 0.35f
    val left = PathPool.acquire().apply {
        moveTo(cx - w * 0.06f, topY); lineTo(cx - w * 0.06f, botY); lineTo(cx - w * 0.42f, cy); close()
    }
    drawPath(left, body); PathPool.release(left)
    val right = PathPool.acquire().apply {
        moveTo(cx + w * 0.06f, topY); lineTo(cx + w * 0.06f, botY); lineTo(cx + w * 0.42f, cy); close()
    }
    drawPath(right, body); PathPool.release(right)
    drawLine(accent, Offset(cx, topY), Offset(cx, botY), strokeWidth = w * 0.05f)
    drawCircle(accent, w * 0.07f, Offset(cx, cy))
}

/** Repulsor: đĩa + 6 gai toả ngoài ("puffer" đẩy lùi). */
private fun DrawScope.drawEnemyRepulsor(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val r = minOf(w, h) * 0.26f
    for (i in 0 until 6) {
        val a = i * 60.0 * Math.PI / 180.0
        drawLine(accent,
            Offset(cx, cy),
            Offset(cx + (r * 1.8f * kotlin.math.cos(a)).toFloat(), cy + (r * 1.8f * kotlin.math.sin(a)).toFloat()),
            strokeWidth = w * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    drawCircle(body, r, Offset(cx, cy))
    drawCircle(accent, r * 0.42f, Offset(cx, cy))
}

/** Jammer: chảo radar (arc) + cột phát + đèn đỏ + cánh. */
private fun DrawScope.drawEnemyJammer(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    drawArc(body, 200f, 140f, true,
        topLeft = Offset(cx - w * 0.28f, cy - h * 0.28f), size = Size(w * 0.56f, h * 0.5f))
    drawLine(body, Offset(cx, cy), Offset(cx, cy - h * 0.42f), strokeWidth = w * 0.06f)
    drawCircle(accent, w * 0.07f, Offset(cx, cy - h * 0.42f))
    drawLine(body, Offset(cx - w * 0.3f, cy + h * 0.22f), Offset(cx + w * 0.3f, cy + h * 0.22f),
        strokeWidth = w * 0.05f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
}

/** Missileer: thân tên lửa + mũi nhọn (hướng xuống) + 2 cánh + đèn. */
private fun DrawScope.drawEnemyMissileer(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    drawRect(body, topLeft = Offset(cx - w * 0.12f, cy - h * 0.22f), size = Size(w * 0.24f, h * 0.5f))
    val nose = PathPool.acquire().apply {
        moveTo(cx - w * 0.12f, cy + h * 0.28f); lineTo(cx + w * 0.12f, cy + h * 0.28f); lineTo(cx, cy + h * 0.48f); close()
    }
    drawPath(nose, accent); PathPool.release(nose)
    val finL = PathPool.acquire().apply {
        moveTo(cx - w * 0.12f, cy - h * 0.22f); lineTo(cx - w * 0.3f, cy - h * 0.38f); lineTo(cx - w * 0.12f, cy); close()
    }
    drawPath(finL, body); PathPool.release(finL)
    val finR = PathPool.acquire().apply {
        moveTo(cx + w * 0.12f, cy - h * 0.22f); lineTo(cx + w * 0.3f, cy - h * 0.38f); lineTo(cx + w * 0.12f, cy); close()
    }
    drawPath(finR, body); PathPool.release(finR)
    drawCircle(accent, w * 0.05f, Offset(cx, cy - h * 0.24f))
}

/** Predator: trăng khuyết sleek (arc dày) + 2 mắt săn teal. */
private fun DrawScope.drawEnemyPredator(cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color) {
    val r = minOf(w, h) * 0.32f
    drawArc(body, startAngle = 35f, sweepAngle = 290f, useCenter = false,
        topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2),
        style = Stroke(width = w * 0.16f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawCircle(accent, w * 0.05f, Offset(cx - w * 0.12f, cy - h * 0.04f))
    drawCircle(accent, w * 0.05f, Offset(cx + w * 0.12f, cy - h * 0.04f))
}
