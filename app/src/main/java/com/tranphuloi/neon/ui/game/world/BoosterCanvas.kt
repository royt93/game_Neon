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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.ui.game.booster.BoosterUI

/**
 * Round 66b — Pure-vector booster rendering. Replaces drawImage(webp/xml) with
 * shape-specific Path/Circle primitives per booster drawableId:
 *
 *   booster_health        → red cross (medical / +)
 *   booster_shield        → cyan octagon (defensive shape)
 *   booster_red_lasers    → red triangle pointing up (laser/arrow)
 *   booster_triple_laser  → 3 vertical bars (visual trio)
 *   booster_ultimate_weapon → gold 5-point star (power)
 *   booster_revive        → green heart shape (life)
 *
 * Glyph + rarity ring overlays in GameWorld unchanged. Round 60 boosters that
 * reuse a base drawable (MAGNET_BOOST→shield, CRIT_SURGE→red_lasers, etc.)
 * inherit the base shape; the existing tint + glyph differentiate them.
 *
 * Visual character: clean neon geometric icons. Generic enough that the same
 * pattern can be the "official" look without per-sprite art.
 */
@Composable
fun BoosterCanvas(
    boosters: List<BoosterUI>,
    modifier: Modifier = Modifier,
) {
    if (boosters.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (b in boosters) {
            drawBooster(b, density)
        }
    }
}

/** Round 66b — sprite preload no longer needed; stub kept for back-compat. */
@Immutable
@Deprecated("Round 66b — pure-vector BoosterCanvas no longer needs sprites.")
data class BoosterSprites(val byDrawableId: Map<Int, Any> = emptyMap())

@Composable
@Deprecated("Round 66b — pure-vector BoosterCanvas no longer needs sprites.")
fun rememberBoosterSprites(): BoosterSprites = BoosterSprites()

private fun DrawScope.drawBooster(booster: BoosterUI, density: Density) {
    with(density) {
        val sizePx = booster.size.dp.toPx()
        val xPx = booster.xOffset.dp.toPx()
        val yPx = booster.yOffset.dp.toPx()
        val cx = xPx + sizePx / 2f
        val cy = yPx + sizePx / 2f
        val glowR = (sizePx / 2f) * 1.8f

        val glowColor = if (booster.tintColorHex != 0L) Color(booster.tintColorHex) else NeonGold

        // 1. Halo (radial gradient) — same recipe as Round 59.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.85f),
                    glowColor.copy(alpha = 0.34f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(cx, cy),
        )

        // 2. Shape per drawableId. Color from glowColor (tint already applied
        // for PIERCING/PLASMA). Inner core (white 0.85) added for neon "bright
        // center" feel.
        when (booster.drawableId) {
            R.drawable.booster_health -> drawCross(cx, cy, sizePx, glowColor)
            R.drawable.booster_shield -> drawOctagon(cx, cy, sizePx, glowColor)
            R.drawable.booster_red_lasers -> drawTriangleUp(cx, cy, sizePx, glowColor)
            R.drawable.booster_triple_laser -> drawTripleBars(cx, cy, sizePx, glowColor)
            R.drawable.booster_ultimate_weapon -> drawStar(cx, cy, sizePx, glowColor)
            R.drawable.booster_revive -> drawHeart(cx, cy, sizePx, glowColor)
            else -> drawOctagon(cx, cy, sizePx, glowColor)              // fallback
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Shape recipes — each takes center + size + color and draws a neon icon.
// ──────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawCross(cx: Float, cy: Float, size: Float, color: Color) {
    val armWide = size * 0.30f
    val armLong = size * 0.85f
    val hw = armLong / 2f
    val hh = armWide / 2f
    // Horizontal bar
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - hw, cy - hh),
        size = Size(armLong, armWide),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(armWide / 2f),
    )
    // Vertical bar
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - hh, cy - hw),
        size = Size(armWide, armLong),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(armWide / 2f),
    )
    // Inner core (white) — small center diamond for hot spot
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = armWide * 0.32f,
        center = Offset(cx, cy),
    )
}

private fun DrawScope.drawOctagon(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.42f
    val path = Path().apply {
        // 8-vertex polygon, regular, rotated 22.5° so flat edges point cardinal.
        val angleStep = 2.0 * Math.PI / 8.0
        val startAngle = -Math.PI / 8.0                               // -22.5°
        for (i in 0 until 8) {
            val a = startAngle + i * angleStep
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = path, color = color)
    // Inner ring (white stroke) for shield-look detail
    drawCircle(
        color = Color.White.copy(alpha = 0.75f),
        radius = r * 0.55f,
        center = Offset(cx, cy),
        style = Stroke(width = size * 0.08f),
    )
}

private fun DrawScope.drawTriangleUp(cx: Float, cy: Float, size: Float, color: Color) {
    val h = size * 0.85f
    val w = size * 0.80f
    val top = Offset(cx, cy - h / 2f)
    val left = Offset(cx - w / 2f, cy + h / 2f)
    val right = Offset(cx + w / 2f, cy + h / 2f)
    val path = Path().apply {
        moveTo(top.x, top.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path = path, color = color)
    // Inner highlight — smaller triangle white-tinted
    val innerScale = 0.45f
    val innerH = h * innerScale
    val innerW = w * innerScale
    val innerTop = Offset(cx, cy - innerH / 2f)
    val innerLeft = Offset(cx - innerW / 2f, cy + innerH / 2f)
    val innerRight = Offset(cx + innerW / 2f, cy + innerH / 2f)
    val innerPath = Path().apply {
        moveTo(innerTop.x, innerTop.y)
        lineTo(innerLeft.x, innerLeft.y)
        lineTo(innerRight.x, innerRight.y)
        close()
    }
    drawPath(path = innerPath, color = Color.White.copy(alpha = 0.75f))
}

private fun DrawScope.drawTripleBars(cx: Float, cy: Float, size: Float, color: Color) {
    val barW = size * 0.18f
    val barH = size * 0.80f
    val gap = size * 0.12f
    // 3 vertical bars, centered on cx
    val xs = listOf(cx - barW - gap, cx, cx + barW + gap)
    xs.forEach { centerX ->
        drawRoundRect(
            color = color,
            topLeft = Offset(centerX - barW / 2f, cy - barH / 2f),
            size = Size(barW, barH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f),
        )
    }
    // Inner white spine on center bar
    drawRoundRect(
        color = Color.White.copy(alpha = 0.80f),
        topLeft = Offset(cx - barW * 0.25f, cy - barH * 0.40f),
        size = Size(barW * 0.5f, barH * 0.80f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW * 0.25f),
    )
}

private fun DrawScope.drawStar(cx: Float, cy: Float, size: Float, color: Color) {
    val outerR = size * 0.45f
    val innerR = outerR * 0.45f
    val path = Path().apply {
        // 10-vertex alternating outer/inner radii, 5-point star, rotated so
        // one point faces up (-90°).
        val startAngle = -Math.PI / 2.0
        val step = Math.PI / 5.0
        for (i in 0 until 10) {
            val a = startAngle + i * step
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = path, color = color)
    // Center dot — bright white core
    drawCircle(
        color = Color.White.copy(alpha = 0.80f),
        radius = innerR * 0.5f,
        center = Offset(cx, cy),
    )
}

private fun DrawScope.drawHeart(cx: Float, cy: Float, size: Float, color: Color) {
    // Heart shape using 2 circles (lobes) + downward triangle (point).
    val lobeR = size * 0.22f
    val lobeY = cy - size * 0.10f
    val leftLobeX = cx - lobeR * 0.85f
    val rightLobeX = cx + lobeR * 0.85f
    val tip = Offset(cx, cy + size * 0.42f)
    // Triangle connecting lobes' bottoms to tip
    val path = Path().apply {
        // Start at left lobe outer edge
        val leftEdge = Offset(leftLobeX - lobeR, lobeY)
        moveTo(leftEdge.x, leftEdge.y)
        // Arc-style up-and-over: approximate via simple polygon (good enough at small size)
        lineTo(cx - lobeR * 1.0f, lobeY - lobeR * 0.6f)
        lineTo(cx, lobeY - lobeR * 0.2f)
        lineTo(cx + lobeR * 1.0f, lobeY - lobeR * 0.6f)
        lineTo(rightLobeX + lobeR, lobeY)
        lineTo(tip.x, tip.y)
        close()
    }
    drawPath(path = path, color = color)
    // Two filled lobes (circles) to ROUND the top edges of the polygon.
    drawCircle(color = color, radius = lobeR, center = Offset(leftLobeX, lobeY))
    drawCircle(color = color, radius = lobeR, center = Offset(rightLobeX, lobeY))
    // Inner highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.50f),
        radius = lobeR * 0.40f,
        center = Offset(leftLobeX - lobeR * 0.25f, lobeY - lobeR * 0.25f),
    )
}
