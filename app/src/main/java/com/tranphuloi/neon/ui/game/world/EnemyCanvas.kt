package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonMagenta
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

        // Glow halo — Magenta as before.
        val glowIntensity = 0.45f + hitFlash * 0.4f
        val glowRadiusFactor = 1.4f + hitFlash * 0.4f
        val glowR = (minOf(wPx, hPx) / 2f) * glowRadiusFactor
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    NeonMagenta.copy(alpha = glowIntensity),
                    NeonMagenta.copy(alpha = glowIntensity * 0.4f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(cx, cy),
        )

        // Body color, blended with hit flash white + status effect tint.
        val baseBody = bodyColorFor(enemy.drawableId)
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
    }
}

/**
 * Map drawableId → primary body color. Preserves original sprite family hue.
 */
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
private fun DrawScope.drawEnemyShape(
    drawableId: Int,
    cx: Float, cy: Float, wPx: Float, hPx: Float,
    body: Color, accent: Color, isBoss: Boolean,
    bossKind: com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind? = null,
) {
    // Round 71 (Issue 4d) — Boss dispatch via bossKind nếu boss, else family
    // dispatch theo drawableId.
    if (isBoss && bossKind != null) {
        when (bossKind) {
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.STAR ->
                drawBossStar(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.CROSS ->
                drawBossCross(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.ORB ->
                drawBossOrb(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.FRACTAL ->
                drawBossFractal(cx, cy, wPx, hPx, body, accent)
            com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind.SPIDER ->
                drawBossSpider(cx, cy, wPx, hPx, body, accent)
        }
        return
    }
    when (drawableId) {
        // Light blue family — dart/triangle attackers (pointing DOWN since
        // they're enemies coming at the player).
        R.drawable.enemy_light_blue_1 -> drawDart(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_light_blue_2 -> drawDart(cx, cy, wPx, hPx, body, accent, variant = 1)
        R.drawable.enemy_light_blue_3 -> drawDart(cx, cy, wPx, hPx, body, accent, variant = 2)
        R.drawable.enemy_light_blue_4 -> drawDart(cx, cy, wPx, hPx, body, accent, variant = 3)
        R.drawable.enemy_light_blue_5 -> drawDart(cx, cy, wPx, hPx, body, accent, variant = 4)
        // Green family — hexagons (balanced medium).
        R.drawable.enemy_green_1 -> drawHexagon(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_green_2 -> drawHexagon(cx, cy, wPx, hPx, body, accent, variant = 1)
        R.drawable.enemy_green_3 -> drawHexagon(cx, cy, wPx, hPx, body, accent, variant = 2)
        R.drawable.enemy_green_4 -> drawHexagon(cx, cy, wPx, hPx, body, accent, variant = 3)
        // Red family — diamonds (heavy hitters).
        R.drawable.enemy_red_1 -> drawDiamond(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_red_2 -> drawDiamond(cx, cy, wPx, hPx, body, accent, variant = 1)
        R.drawable.enemy_red_3 -> drawDiamond(cx, cy, wPx, hPx, body, accent, variant = 2)
        // Round 74 (R73d) — Wave 9a: 8 new shape recipes cho ELITE + BERSERKER family.
        // ELITE: cross + orb (violet).
        R.drawable.enemy_cross_1 -> drawCross(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_cross_2 -> drawCross(cx, cy, wPx, hPx, body, accent, variant = 1)
        R.drawable.enemy_orb_1 -> drawOrb(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_orb_2 -> drawOrb(cx, cy, wPx, hPx, body, accent, variant = 1)
        // BERSERKER: chevron + spike (orange).
        R.drawable.enemy_chevron_1 -> drawChevron(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_chevron_2 -> drawChevron(cx, cy, wPx, hPx, body, accent, variant = 1)
        R.drawable.enemy_spike_1 -> drawSpike(cx, cy, wPx, hPx, body, accent, variant = 0)
        R.drawable.enemy_spike_2 -> drawSpike(cx, cy, wPx, hPx, body, accent, variant = 1)
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
        val path = androidx.compose.ui.graphics.Path().apply {
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
    val path = androidx.compose.ui.graphics.Path().apply {
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
    val outer = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy + r)
        lineTo(cx - r * 0.866f, cy - r * 0.5f)
        lineTo(cx + r * 0.866f, cy - r * 0.5f)
        close()
    }
    drawPath(outer, body)
    drawPath(outer, accent, style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.08f))
    // Inner upward triangle (Sierpinski child)
    val innerR = r * 0.5f
    val inner = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx, cy - innerR)
        lineTo(cx - innerR * 0.866f, cy + innerR * 0.5f)
        lineTo(cx + innerR * 0.866f, cy + innerR * 0.5f)
        close()
    }
    drawPath(inner, accent)
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

// ─────────── Shape recipes ───────────

/** Dart shape — triangle pointing DOWN (enemies attack downward). */
private fun DrawScope.drawDart(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color, variant: Int,
) {
    val halfW = w * 0.42f
    val halfH = h * 0.45f
    val tipY = cy + halfH                            // points down
    val baseY = cy - halfH
    val notch = h * (0.10f + variant * 0.04f)        // variant tweaks back notch depth
    val path = Path().apply {
        moveTo(cx, tipY)                             // tip down
        lineTo(cx + halfW, baseY)                    // top-right
        lineTo(cx, baseY + notch)                    // back notch (chevron)
        lineTo(cx - halfW, baseY)                    // top-left
        close()
    }
    drawPath(path = path, color = body)
    drawPath(path = path, color = accent, style = Stroke(width = w * 0.06f))
    // Cockpit spot
    drawCircle(
        color = accent,
        radius = w * 0.10f,
        center = Offset(cx, cy + h * 0.10f),
    )
}

/** Hexagon — flat-top, slightly elongated vertically. variant rotates 10° each. */
private fun DrawScope.drawHexagon(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color, variant: Int,
) {
    val rx = w * 0.45f
    val ry = h * 0.50f
    val baseAngle = Math.toRadians((variant * 10).toDouble())
    val path = Path().apply {
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
    // Inner pip
    drawCircle(color = accent, radius = w * 0.12f, center = Offset(cx, cy))
}

/** Diamond — vertical, wider than tall, with 4-spike accent. */
private fun DrawScope.drawDiamond(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color, variant: Int,
) {
    val halfW = w * 0.45f
    val halfH = h * 0.50f
    val path = Path().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy)
        close()
    }
    drawPath(path = path, color = body)
    drawPath(path = path, color = accent, style = Stroke(width = w * 0.07f))
    // Variant adds extra spike pips
    val spikeR = w * 0.10f
    drawCircle(color = accent, radius = spikeR, center = Offset(cx, cy))
    if (variant >= 1) {
        drawCircle(color = accent, radius = spikeR * 0.6f, center = Offset(cx, cy - halfH * 0.6f))
    }
    if (variant >= 2) {
        drawCircle(color = accent, radius = spikeR * 0.6f, center = Offset(cx, cy + halfH * 0.6f))
    }
}

/** Boss shape — large 8-pointed star with inner hexagon detail. */
private fun DrawScope.drawBossStar(
    cx: Float, cy: Float, w: Float, h: Float, body: Color, accent: Color,
) {
    val outerR = minOf(w, h) * 0.50f
    val innerR = outerR * 0.55f
    val path = Path().apply {
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
    // Inner hexagon core
    val coreR = outerR * 0.40f
    val core = Path().apply {
        for (i in 0 until 6) {
            val a = 2.0 * Math.PI * i / 6.0
            val x = cx + (coreR * cos(a)).toFloat()
            val y = cy + (coreR * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = core, color = accent)
    drawCircle(color = Color.White.copy(alpha = 0.75f), radius = coreR * 0.30f, center = Offset(cx, cy))
}
