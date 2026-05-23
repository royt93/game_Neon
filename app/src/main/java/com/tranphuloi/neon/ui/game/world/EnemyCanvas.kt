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
                )
            }
        }

        // Main body shape per family.
        drawEnemyShape(
            drawableId = enemy.drawableId,
            cx = cx,
            cy = cy,
            wPx = wPx,
            hPx = hPx,
            body = bodyColor,
            accent = accent,
            isBoss = enemy.isBoss,
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
) {
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
        // Bosses — large 8-pointed stars w/ inner detail.
        R.drawable.enemy_green_boss -> drawBossStar(cx, cy, wPx, hPx, body, accent)
        R.drawable.enemy_red_boss -> drawBossStar(cx, cy, wPx, hPx, body, accent)
        else -> drawHexagon(cx, cy, wPx, hPx, body, accent, variant = 0)
    }
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
