package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.tranphuloi.neon.common.drawSoftHalo
import com.tranphuloi.neon.ui.game.booster.BoosterShape
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

        // Round 78 (#6 perf) — drawSoftHalo replaces Brush.radialGradient.
        drawSoftHalo(glowColor, 0.85f, glowR, Offset(cx, cy))

        // Round 78 spec follow-up — single dispatch on the type-safe shape
        // enum. `when` is exhaustive → compiler flags any new BoosterShape that
        // isn't handled here. No more drawableId/shapeKey two-tier lookup.
        when (booster.shape) {
            BoosterShape.CROSS -> drawCross(cx, cy, sizePx, glowColor)
            BoosterShape.OCTAGON -> drawOctagon(cx, cy, sizePx, glowColor)
            BoosterShape.TRIANGLE_UP -> drawTriangleUp(cx, cy, sizePx, glowColor)
            BoosterShape.TRIPLE_BARS -> drawTripleBars(cx, cy, sizePx, glowColor)
            BoosterShape.STAR -> drawStar(cx, cy, sizePx, glowColor)
            BoosterShape.HEART -> drawHeart(cx, cy, sizePx, glowColor)
            BoosterShape.ATOM -> drawAtomShape(cx, cy, sizePx, glowColor)
            BoosterShape.FLAME -> drawFlameShape(cx, cy, sizePx, glowColor)
            BoosterShape.MAGNET -> drawMagnetShape(cx, cy, sizePx, glowColor)
            BoosterShape.LIGHTNING -> drawLightningShape(cx, cy, sizePx, glowColor)
            BoosterShape.CROSSHAIR -> drawCrosshairShape(cx, cy, sizePx, glowColor)
            BoosterShape.BEAM -> drawBeamShape(cx, cy, sizePx, glowColor)
            BoosterShape.SPLIT_FORK -> drawSplitShape(cx, cy, sizePx, glowColor)
            BoosterShape.ARROW_RIGHT -> drawArrowRightShape(cx, cy, sizePx, glowColor)
            BoosterShape.RING_PULSE -> drawRingPulseShape(cx, cy, sizePx, glowColor)
            BoosterShape.DOLLAR -> drawDollarShape(cx, cy, sizePx, glowColor)
            BoosterShape.PLUS_DOUBLE -> drawPlusDoubleShape(cx, cy, sizePx, glowColor)
            BoosterShape.SHARD -> drawShardShape(cx, cy, sizePx, glowColor)
            BoosterShape.AURA_RING -> drawAuraRingShape(cx, cy, sizePx, glowColor)
            BoosterShape.PHASE_DIAMOND -> drawPhaseDiamondShape(cx, cy, sizePx, glowColor)
            BoosterShape.CLOUD_PUFF -> drawCloudPuffShape(cx, cy, sizePx, glowColor)
            BoosterShape.SPREAD_FAN -> drawSpreadFanShape(cx, cy, sizePx, glowColor)
            BoosterShape.CRYSTAL_SPARK -> drawCrystalSparkShape(cx, cy, sizePx, glowColor)
            BoosterShape.DOUBLE_ARROW -> drawDoubleArrowShape(cx, cy, sizePx, glowColor)
            BoosterShape.ARROW_CYCLE -> drawArrowCycleShape(cx, cy, sizePx, glowColor)
            BoosterShape.BIG_DOT -> drawBigDotShape(cx, cy, sizePx, glowColor)
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Round 78 (#3 fix) — 9 new booster shape recipes for type disambiguation.
// ──────────────────────────────────────────────────────────────────────────

/** Atom — central nucleus + 3 elliptical orbits (rotated). */
private fun DrawScope.drawAtomShape(cx: Float, cy: Float, size: Float, color: Color) {
    val nucR = size * 0.10f
    val orbitR = size * 0.40f
    drawCircle(color, nucR, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.85f), nucR * 0.55f, Offset(cx, cy))
    // 3 elliptical orbits at 0°, 60°, 120°
    for (i in 0 until 3) {
        rotate(degrees = i * 60f, pivot = Offset(cx, cy)) {
            drawOval(
                color = color,
                topLeft = Offset(cx - orbitR, cy - orbitR * 0.35f),
                size = Size(orbitR * 2, orbitR * 0.70f),
                style = Stroke(width = size * 0.05f),
            )
        }
    }
}

/** Flame — 3 petal "fire" shape pointing up. */
private fun DrawScope.drawFlameShape(cx: Float, cy: Float, size: Float, color: Color) {
    val h = size * 0.85f
    val w = size * 0.55f
    val outer = Path().apply {
        moveTo(cx, cy - h / 2f)                      // top tip
        cubicTo(
            cx + w / 2f, cy - h * 0.10f,             // right curve out
            cx + w / 4f, cy + h / 4f,
            cx + w / 6f, cy + h / 2f,                 // right base
        )
        lineTo(cx - w / 6f, cy + h / 2f)
        cubicTo(
            cx - w / 4f, cy + h / 4f,
            cx - w / 2f, cy - h * 0.10f,
            cx, cy - h / 2f,
        )
        close()
    }
    drawPath(outer, color)
    // Inner flame (lighter)
    val inner = Path().apply {
        val ih = h * 0.55f
        val iw = w * 0.50f
        moveTo(cx, cy - ih / 2f)
        cubicTo(cx + iw / 2f, cy, cx + iw / 4f, cy + ih / 4f, cx, cy + ih / 2f)
        cubicTo(cx - iw / 4f, cy + ih / 4f, cx - iw / 2f, cy, cx, cy - ih / 2f)
        close()
    }
    drawPath(inner, Color.White.copy(alpha = 0.8f))
}

/** Magnet — U-shape horseshoe with 2 pole tips. */
private fun DrawScope.drawMagnetShape(cx: Float, cy: Float, size: Float, color: Color) {
    val w = size * 0.75f
    val h = size * 0.80f
    val thick = size * 0.18f
    // U-shape path
    val path = Path().apply {
        // Outer left arm down → bottom arc → right arm up → inner right down → inner bottom → inner left up
        moveTo(cx - w / 2f, cy - h / 2f)
        lineTo(cx - w / 2f, cy + h * 0.20f)
        // bottom arc out
        cubicTo(
            cx - w / 2f, cy + h / 2f,
            cx + w / 2f, cy + h / 2f,
            cx + w / 2f, cy + h * 0.20f,
        )
        lineTo(cx + w / 2f, cy - h / 2f)
        lineTo(cx + w / 2f - thick, cy - h / 2f)
        lineTo(cx + w / 2f - thick, cy + h * 0.10f)
        cubicTo(
            cx + w / 2f - thick, cy + h * 0.40f,
            cx - w / 2f + thick, cy + h * 0.40f,
            cx - w / 2f + thick, cy + h * 0.10f,
        )
        lineTo(cx - w / 2f + thick, cy - h / 2f)
        close()
    }
    drawPath(path, color)
    // Pole tips (red top-left & top-right caps)
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(cx - w / 2f, cy - h / 2f),
        size = Size(thick, size * 0.10f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(cx + w / 2f - thick, cy - h / 2f),
        size = Size(thick, size * 0.10f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f),
    )
}

/** Lightning bolt — zigzag from top-right to bottom-left. */
private fun DrawScope.drawLightningShape(cx: Float, cy: Float, size: Float, color: Color) {
    val h = size * 0.85f
    val w = size * 0.55f
    val path = Path().apply {
        moveTo(cx + w * 0.20f, cy - h / 2f)
        lineTo(cx - w * 0.10f, cy - h * 0.10f)
        lineTo(cx + w * 0.05f, cy)
        lineTo(cx - w * 0.20f, cy + h / 2f)
        lineTo(cx + w * 0.10f, cy + h * 0.10f)
        lineTo(cx - w * 0.05f, cy)
        close()
    }
    drawPath(path, color)
    drawPath(path, Color.White.copy(alpha = 0.85f), style = Stroke(width = size * 0.03f))
}

/** Crosshair — concentric circles + 4-direction cross marker. */
private fun DrawScope.drawCrosshairShape(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.40f
    drawCircle(color, r, Offset(cx, cy), style = Stroke(width = size * 0.06f))
    drawCircle(color, r * 0.55f, Offset(cx, cy), style = Stroke(width = size * 0.05f))
    val tickLen = size * 0.12f
    drawLine(color, Offset(cx, cy - r - tickLen), Offset(cx, cy - r * 0.7f),
        strokeWidth = size * 0.06f)
    drawLine(color, Offset(cx, cy + r * 0.7f), Offset(cx, cy + r + tickLen),
        strokeWidth = size * 0.06f)
    drawLine(color, Offset(cx - r - tickLen, cy), Offset(cx - r * 0.7f, cy),
        strokeWidth = size * 0.06f)
    drawLine(color, Offset(cx + r * 0.7f, cy), Offset(cx + r + tickLen, cy),
        strokeWidth = size * 0.06f)
    drawCircle(Color.White.copy(alpha = 0.9f), size * 0.05f, Offset(cx, cy))
}

/** Beam — horizontal rectangle with bright center streak (kamehameha aesthetic). */
private fun DrawScope.drawBeamShape(cx: Float, cy: Float, size: Float, color: Color) {
    val w = size * 0.85f
    val h = size * 0.32f
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - w / 2f, cy - h / 2f),
        size = Size(w, h),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(cx - w * 0.30f, cy - h * 0.18f),
        size = Size(w * 0.60f, h * 0.36f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.18f),
    )
    // Hand-cup tip glow on the left (origin)
    drawCircle(color, size * 0.15f, Offset(cx - w / 2f - size * 0.05f, cy))
}

/** Split — Y-fork shape (1 stem, 2 prongs). */
private fun DrawScope.drawSplitShape(cx: Float, cy: Float, size: Float, color: Color) {
    val stemH = size * 0.35f
    val pronguH = size * 0.45f
    val pronguW = size * 0.30f
    val thick = size * 0.12f
    // Stem (vertical from bottom)
    drawRoundRect(
        color = color,
        topLeft = Offset(cx - thick / 2f, cy),
        size = Size(thick, stemH + size * 0.05f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(thick / 2f),
    )
    // Left prong (up-left diag)
    drawLine(color,
        Offset(cx, cy - size * 0.05f),
        Offset(cx - pronguW, cy - pronguH),
        strokeWidth = thick, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // Right prong (up-right diag)
    drawLine(color,
        Offset(cx, cy - size * 0.05f),
        Offset(cx + pronguW, cy - pronguH),
        strokeWidth = thick, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawCircle(Color.White.copy(alpha = 0.85f), thick * 0.55f, Offset(cx, cy))
}

/** Arrow-right — rightward chevron arrow (piercing motion). */
private fun DrawScope.drawArrowRightShape(cx: Float, cy: Float, size: Float, color: Color) {
    val w = size * 0.85f; val h = size * 0.55f
    val path = Path().apply {
        moveTo(cx - w / 2f, cy - h / 4f)
        lineTo(cx + w / 4f, cy - h / 4f)
        lineTo(cx + w / 4f, cy - h / 2f)
        lineTo(cx + w / 2f, cy)
        lineTo(cx + w / 4f, cy + h / 2f)
        lineTo(cx + w / 4f, cy + h / 4f)
        lineTo(cx - w / 2f, cy + h / 4f)
        close()
    }
    drawPath(path, color)
    drawPath(path, Color.White.copy(alpha = 0.8f), style = Stroke(width = size * 0.04f))
}

/** Ring pulse — 3 concentric circles (plasma containment vibe). */
private fun DrawScope.drawRingPulseShape(cx: Float, cy: Float, size: Float, color: Color) {
    drawCircle(color, size * 0.42f, Offset(cx, cy), style = Stroke(width = size * 0.06f))
    drawCircle(color, size * 0.30f, Offset(cx, cy), style = Stroke(width = size * 0.05f))
    drawCircle(color, size * 0.18f, Offset(cx, cy), style = Stroke(width = size * 0.04f))
    drawCircle(Color.White.copy(alpha = 0.85f), size * 0.06f, Offset(cx, cy))
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

// ──────────────────────────────────────────────────────────────────────────
// Round 78 spec follow-up — 10 more booster shape recipes to eliminate
// remaining duplicates across booster_health/shield/red_lasers/triple_laser/
// ultimate_weapon families.
// ──────────────────────────────────────────────────────────────────────────

/** Dollar — $ glyph approximated via S-curve path + 2 vertical bars. */
private fun DrawScope.drawDollarShape(cx: Float, cy: Float, size: Float, color: Color) {
    val h = size * 0.75f
    // 2 vertical bars (the | of the $ sign)
    val barW = size * 0.06f
    drawRoundRect(color,
        topLeft = Offset(cx - barW / 2f, cy - h * 0.55f),
        size = Size(barW, h * 1.10f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f))
    // Upper curve (top arc of S)
    val curveR = size * 0.25f
    drawArc(color = color,
        startAngle = 60f, sweepAngle = 240f,
        useCenter = false,
        topLeft = Offset(cx - curveR, cy - h * 0.45f),
        size = Size(curveR * 2, curveR * 1.2f),
        style = Stroke(width = size * 0.10f))
    // Lower curve (bottom arc of S)
    drawArc(color = color,
        startAngle = 240f, sweepAngle = 240f,
        useCenter = false,
        topLeft = Offset(cx - curveR, cy - h * 0.05f),
        size = Size(curveR * 2, curveR * 1.2f),
        style = Stroke(width = size * 0.10f))
}

/** Plus-double — large + with extra crossbar (quick-heal "double dose"). */
private fun DrawScope.drawPlusDoubleShape(cx: Float, cy: Float, size: Float, color: Color) {
    val armW = size * 0.20f
    val armL = size * 0.75f
    // Main vertical bar
    drawRoundRect(color,
        topLeft = Offset(cx - armW / 2f, cy - armL / 2f),
        size = Size(armW, armL),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(armW / 2f))
    // Main horizontal bar
    drawRoundRect(color,
        topLeft = Offset(cx - armL / 2f, cy - armW / 2f),
        size = Size(armL, armW),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(armW / 2f))
    // Small extra horizontal bar (the "double")
    val extraW = size * 0.50f; val extraH = size * 0.10f
    drawRoundRect(Color.White.copy(alpha = 0.85f),
        topLeft = Offset(cx - extraW / 2f, cy - extraH / 2f),
        size = Size(extraW, extraH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(extraH / 2f))
}

/** Shard — crystal shard (vertical elongated rhombus, faceted). */
private fun DrawScope.drawShardShape(cx: Float, cy: Float, size: Float, color: Color) {
    val halfW = size * 0.22f
    val halfH = size * 0.45f
    val outer = Path().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy - halfH * 0.20f)
        lineTo(cx + halfW * 0.7f, cy + halfH)
        lineTo(cx - halfW * 0.7f, cy + halfH)
        lineTo(cx - halfW, cy - halfH * 0.20f)
        close()
    }
    drawPath(outer, color)
    drawPath(outer, Color.White.copy(alpha = 0.8f), style = Stroke(width = size * 0.04f))
    // Inner facet line
    drawLine(Color.White.copy(alpha = 0.65f),
        Offset(cx, cy - halfH), Offset(cx, cy + halfH),
        strokeWidth = size * 0.03f)
}

/** Aura ring — soft outer ring + ring + inner pulse dot. */
private fun DrawScope.drawAuraRingShape(cx: Float, cy: Float, size: Float, color: Color) {
    drawCircle(color, size * 0.42f, Offset(cx, cy),
        style = Stroke(width = size * 0.08f))
    drawCircle(color, size * 0.30f, Offset(cx, cy),
        style = Stroke(width = size * 0.04f))
    drawCircle(color, size * 0.10f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.85f), size * 0.05f, Offset(cx, cy))
}

/** Phase diamond — diamond outline with ghost stutter (double offset). */
private fun DrawScope.drawPhaseDiamondShape(cx: Float, cy: Float, size: Float, color: Color) {
    val halfW = size * 0.35f
    val halfH = size * 0.42f
    fun makeDiamond(ox: Float, oy: Float, alpha: Float) {
        val p = Path().apply {
            moveTo(cx + ox, cy + oy - halfH)
            lineTo(cx + ox + halfW, cy + oy)
            lineTo(cx + ox, cy + oy + halfH)
            lineTo(cx + ox - halfW, cy + oy)
            close()
        }
        drawPath(p, color.copy(alpha = alpha), style = Stroke(width = size * 0.06f))
    }
    // Ghost copy offset top-right
    makeDiamond(size * 0.08f, -size * 0.05f, 0.40f)
    // Main diamond center
    makeDiamond(0f, 0f, 0.95f)
    drawCircle(Color.White.copy(alpha = 0.8f), size * 0.06f, Offset(cx, cy))
}

/** Cloud puff — 3-lobe cloud silhouette. */
private fun DrawScope.drawCloudPuffShape(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.20f
    drawCircle(color, r, Offset(cx - r * 0.95f, cy + r * 0.20f))
    drawCircle(color, r * 1.20f, Offset(cx, cy))
    drawCircle(color, r, Offset(cx + r * 1.0f, cy + r * 0.10f))
    drawCircle(color, r * 0.85f, Offset(cx + r * 0.45f, cy - r * 0.55f))
    drawCircle(color, r * 0.80f, Offset(cx - r * 0.50f, cy - r * 0.45f))
    // Base bar (cloud bottom flat)
    drawRoundRect(color,
        topLeft = Offset(cx - size * 0.32f, cy + r * 0.25f),
        size = Size(size * 0.64f, size * 0.12f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.06f))
}

/** Spread fan — 3 chevron arrows fanned outward upward. */
private fun DrawScope.drawSpreadFanShape(cx: Float, cy: Float, size: Float, color: Color) {
    val h = size * 0.35f
    val w = size * 0.18f
    val drawArrow: (Float, Float) -> Unit = { dx, angle ->
        val rad = angle * Math.PI / 180.0
        val ax = cx + dx
        // Use simple chevron
        val path = Path().apply {
            val tx = (ax + h * kotlin.math.sin(rad)).toFloat()
            val ty = (cy - h * kotlin.math.cos(rad)).toFloat()
            val bx1 = (ax + w * 0.5f * kotlin.math.cos(rad)).toFloat()
            val by1 = (cy + w * 0.5f * kotlin.math.sin(rad)).toFloat()
            val bx2 = (ax - w * 0.5f * kotlin.math.cos(rad)).toFloat()
            val by2 = (cy - w * 0.5f * kotlin.math.sin(rad)).toFloat()
            moveTo(tx, ty)
            lineTo(bx1, by1)
            lineTo(ax, cy)
            lineTo(bx2, by2)
            close()
        }
        drawPath(path, color)
    }
    drawArrow(0f, 0f)
    drawArrow(-size * 0.25f, -25f)
    drawArrow(size * 0.25f, 25f)
    drawCircle(Color.White.copy(alpha = 0.7f), size * 0.05f, Offset(cx, cy + size * 0.05f))
}

/** Crystal spark — 4-pointed star (sharp diagonal spikes). */
private fun DrawScope.drawCrystalSparkShape(cx: Float, cy: Float, size: Float, color: Color) {
    val outerR = size * 0.45f
    val innerR = outerR * 0.22f
    val path = Path().apply {
        val pts = 4
        for (i in 0 until pts * 2) {
            val a = -Math.PI / 2 + i * Math.PI / pts
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + (r * kotlin.math.cos(a)).toFloat()
            val y = cy + (r * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color)
    drawPath(path, Color.White.copy(alpha = 0.85f), style = Stroke(width = size * 0.03f))
    drawCircle(Color.White, size * 0.07f, Offset(cx, cy))
}

/** Double arrow — 2 right-pointing chevrons stacked (double fire rate). */
private fun DrawScope.drawDoubleArrowShape(cx: Float, cy: Float, size: Float, color: Color) {
    val w = size * 0.30f; val h = size * 0.40f
    val drawOne: (Float) -> Unit = { dx ->
        val p = Path().apply {
            moveTo(cx + dx - w / 2f, cy - h / 2f)
            lineTo(cx + dx + w / 2f, cy)
            lineTo(cx + dx - w / 2f, cy + h / 2f)
            lineTo(cx + dx - w * 0.25f, cy + h / 2f)
            lineTo(cx + dx + w * 0.25f, cy)
            lineTo(cx + dx - w * 0.25f, cy - h / 2f)
            close()
        }
        drawPath(p, color)
    }
    drawOne(-size * 0.20f)
    drawOne(size * 0.20f)
}

/** Arrow cycle — 2 arrows curving (bounce/cyclic). */
private fun DrawScope.drawArrowCycleShape(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.35f
    drawArc(color = color,
        startAngle = 30f, sweepAngle = 240f,
        useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = size * 0.08f))
    // Arrow head at start (top-right)
    val ang0 = 30.0 * Math.PI / 180.0
    val ax = cx + (r * kotlin.math.cos(ang0)).toFloat()
    val ay = cy + (r * kotlin.math.sin(ang0)).toFloat()
    val arr = Path().apply {
        moveTo(ax + size * 0.10f, ay - size * 0.05f)
        lineTo(ax + size * 0.18f, ay + size * 0.08f)
        lineTo(ax - size * 0.05f, ay + size * 0.10f)
        close()
    }
    drawPath(arr, color)
    drawCircle(Color.White.copy(alpha = 0.7f), size * 0.06f, Offset(cx, cy))
}

/** Big dot — large solid disc with thick ring (giant bullet "heavyweight"). */
private fun DrawScope.drawBigDotShape(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.40f
    drawCircle(color, r, Offset(cx, cy), style = Stroke(width = size * 0.10f))
    drawCircle(color, r * 0.65f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.85f), r * 0.25f, Offset(cx, cy))
}
