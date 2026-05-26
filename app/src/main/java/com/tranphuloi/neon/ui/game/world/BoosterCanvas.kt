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
            BoosterShape.SHARD -> drawShardShape(cx, cy, sizePx, glowColor)
            BoosterShape.AURA_RING -> drawAuraRingShape(cx, cy, sizePx, glowColor)
            BoosterShape.PHASE_DIAMOND -> drawPhaseDiamondShape(cx, cy, sizePx, glowColor)
            BoosterShape.CLOUD_PUFF -> drawCloudPuffShape(cx, cy, sizePx, glowColor)
            BoosterShape.SPREAD_FAN -> drawSpreadFanShape(cx, cy, sizePx, glowColor)
            BoosterShape.CRYSTAL_SPARK -> drawCrystalSparkShape(cx, cy, sizePx, glowColor)
            BoosterShape.DOUBLE_ARROW -> drawDoubleArrowShape(cx, cy, sizePx, glowColor)
            BoosterShape.ARROW_CYCLE -> drawArrowCycleShape(cx, cy, sizePx, glowColor)
            BoosterShape.BIG_DOT -> drawBigDotShape(cx, cy, sizePx, glowColor)
            BoosterShape.RAGE_FANG -> drawRageFangShape(cx, cy, sizePx, glowColor)
            BoosterShape.HEALING_FLASK -> drawHealingFlaskShape(cx, cy, sizePx, glowColor)
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
/** Energy Wave — audit redesign: oscilloscope sine wave (replaces horizontal bar). */
private fun DrawScope.drawBeamShape(cx: Float, cy: Float, size: Float, color: Color) {
    val w = size * 0.85f
    val amp = size * 0.20f
    val path = Path().apply {
        val steps = 24
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = cx - w / 2f + w * t
            val y = cy + (amp * kotlin.math.sin(t * 4 * Math.PI).toFloat())
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    // Draw wave (3-layer: outer glow + mid stroke + bright core)
    drawPath(path, color.copy(alpha = 0.4f),
        style = Stroke(width = size * 0.10f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawPath(path, color,
        style = Stroke(width = size * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    drawPath(path, Color.White,
        style = Stroke(width = size * 0.025f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    // Origin emitter (left dot)
    drawCircle(color, size * 0.07f, Offset(cx - w / 2f, cy))
    drawCircle(Color.White, size * 0.03f, Offset(cx - w / 2f, cy))
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

/**
 * Arrow-right — Round 80 audit fix: redesigned thành LANCE (long needle/spike)
 * để KHÔNG visually dup với DOUBLE_ARROW/SPREAD_FAN. Thin elongated diamond
 * with motion-streak tail. Reads as "piercing/penetrate" — match PIERCING_BOOSTER
 * function.
 */
private fun DrawScope.drawArrowRightShape(cx: Float, cy: Float, size: Float, color: Color) {
    val h = size * 0.85f
    val halfH = h / 2f
    val w = size * 0.18f
    // Long thin diamond pointing UP (lance tip)
    val lance = Path().apply {
        moveTo(cx, cy - halfH)              // tip top
        lineTo(cx + w / 2f, cy - halfH * 0.20f)
        lineTo(cx + w * 0.30f, cy + halfH * 0.85f)  // base right
        lineTo(cx - w * 0.30f, cy + halfH * 0.85f)  // base left
        lineTo(cx - w / 2f, cy - halfH * 0.20f)
        close()
    }
    drawPath(lance, color)
    drawPath(lance, Color.White.copy(alpha = 0.65f), style = Stroke(width = size * 0.025f))
    // Motion streak — 3 horizontal lines behind lance (tail) suggesting speed
    for (i in 0 until 3) {
        val ty = cy + halfH * 0.50f + i * size * 0.10f
        drawLine(color.copy(alpha = 0.45f - i * 0.12f),
            Offset(cx - w * 0.40f - i * size * 0.04f, ty),
            Offset(cx + w * 0.40f + i * size * 0.04f, ty),
            strokeWidth = size * 0.025f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // Tip glint (white)
    drawCircle(Color.White, size * 0.035f, Offset(cx, cy - halfH * 0.85f))
}

/** Plasma Prism — faceted gem/prism (replaces 3-ring concentric, distinct từ ATOM/CROSSHAIR). */
private fun DrawScope.drawRingPulseShape(cx: Float, cy: Float, size: Float, color: Color) {
    val halfW = size * 0.32f
    val halfH = size * 0.42f
    // Hexagonal gem outline
    val gemPath = Path().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy - halfH * 0.45f)
        lineTo(cx + halfW, cy + halfH * 0.45f)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy + halfH * 0.45f)
        lineTo(cx - halfW, cy - halfH * 0.45f)
        close()
    }
    drawPath(gemPath, color)
    drawPath(gemPath, Color.White.copy(alpha = 0.6f), style = Stroke(width = size * 0.025f))
    // Internal facet lines (4 lines splitting top half + bottom into facets)
    drawLine(Color.White.copy(alpha = 0.55f),
        Offset(cx, cy - halfH), Offset(cx + halfW, cy - halfH * 0.45f),
        strokeWidth = size * 0.015f)
    drawLine(Color.White.copy(alpha = 0.55f),
        Offset(cx, cy - halfH), Offset(cx - halfW, cy - halfH * 0.45f),
        strokeWidth = size * 0.015f)
    drawLine(Color.White.copy(alpha = 0.55f),
        Offset(cx, cy - halfH), Offset(cx, cy + halfH),
        strokeWidth = size * 0.015f)
    drawLine(Color.White.copy(alpha = 0.55f),
        Offset(cx, cy + halfH), Offset(cx + halfW, cy + halfH * 0.45f),
        strokeWidth = size * 0.015f)
    drawLine(Color.White.copy(alpha = 0.55f),
        Offset(cx, cy + halfH), Offset(cx - halfW, cy + halfH * 0.45f),
        strokeWidth = size * 0.015f)
    // Bright highlight upper facet
    val hlPath = Path().apply {
        moveTo(cx, cy - halfH + size * 0.04f)
        lineTo(cx + halfW * 0.65f, cy - halfH * 0.40f)
        lineTo(cx + halfW * 0.30f, cy - halfH * 0.18f)
        close()
    }
    drawPath(hlPath, Color.White.copy(alpha = 0.45f))
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

/**
 * Round 81 audit fix — was filled triangle (dup motif với ARROW_RIGHT lance,
 * SPREAD_FAN, etc.). Redesigned thành LASER_BEAM: thin elongated bar pointing UP
 * with start dot + 2 small parallel side beams. Reads as "laser beam emission"
 * distinct from any triangle/arrow.
 */
private fun DrawScope.drawTriangleUp(cx: Float, cy: Float, size: Float, color: Color) {
    // Main central beam (long thin bar pointing up)
    val beamW = size * 0.10f
    val beamH = size * 0.75f
    drawRoundRect(color,
        topLeft = Offset(cx - beamW / 2f, cy - beamH / 2f),
        size = Size(beamW, beamH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(beamW / 2f))
    // 2 side beams (shorter, parallel)
    for (sign in intArrayOf(-1, 1)) {
        val sx = cx + sign * size * 0.18f
        drawRoundRect(color.copy(alpha = 0.75f),
            topLeft = Offset(sx - beamW / 3f, cy - beamH * 0.30f),
            size = Size(beamW * 0.65f, beamH * 0.60f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(beamW / 4f))
    }
    // Emission disc at bottom (laser origin)
    drawCircle(color, size * 0.10f, Offset(cx, cy + beamH / 2f - size * 0.05f))
    drawCircle(Color.White.copy(alpha = 0.85f), size * 0.04f, Offset(cx, cy + beamH / 2f - size * 0.05f))
    // Tip glow at top
    drawCircle(Color.White, size * 0.04f, Offset(cx, cy - beamH / 2f + size * 0.03f))
}

private fun DrawScope.drawTripleBars(cx: Float, cy: Float, size: Float, color: Color) {
    val barW = size * 0.18f
    val barH = size * 0.80f
    val gap = size * 0.12f
    // Audit redesign — was 3 vertical bars. Now: TRIDENT silhouette (3-prong
    // tip pointing UP với handle bottom) để distinct từ DOUBLE_ARROW twin
    // cannons hoặc TWIN_DOMES vertical bars.
    val tipY = cy - barH * 0.45f
    val baseY = cy + barH * 0.05f
    val handleY = cy + barH * 0.45f
    // 3 prong spikes
    for (i in -1..1) {
        val tipX = cx + i * (barW + gap * 0.8f)
        val prong = Path().apply {
            moveTo(tipX, tipY)
            lineTo(tipX + barW * 0.55f, baseY)
            lineTo(tipX - barW * 0.55f, baseY)
            close()
        }
        drawPath(prong, color)
    }
    // Cross-bar connecting prongs
    drawRect(color,
        topLeft = Offset(cx - barW * 1.5f - gap, baseY),
        size = Size((barW * 1.5f + gap) * 2f, barH * 0.08f))
    // Handle vertical
    drawRoundRect(color,
        topLeft = Offset(cx - barW * 0.18f, baseY + barH * 0.08f),
        size = Size(barW * 0.36f, handleY - baseY - barH * 0.08f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW * 0.18f))
    // Pommel
    drawCircle(color, barW * 0.30f, Offset(cx, handleY))
    drawCircle(Color.White.copy(alpha = 0.7f), barW * 0.12f, Offset(cx, handleY))
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

// drawPlusDoubleShape removed Round 79 audit — PLUS_DOUBLE BoosterShape value
// no longer referenced (QUICK_HEAL maps to HEALING_FLASK instead).

/**
 * Mineral Supercharge — audit redesign: GEM_CLUSTER (3 small clustered gems)
 * thay single vertical shard (dup vertical-pointed cluster). Reads như "ore
 * vein" / mineral deposit — match MINERAL_SUPERCHARGE function.
 */
private fun DrawScope.drawShardShape(cx: Float, cy: Float, size: Float, color: Color) {
    // 3 small diamonds clustered: 1 large center + 2 smaller flanking
    val gems = listOf(
        Triple(cx, cy - size * 0.05f, size * 0.18f),                  // center large
        Triple(cx - size * 0.22f, cy + size * 0.10f, size * 0.13f),   // left small
        Triple(cx + size * 0.22f, cy + size * 0.10f, size * 0.13f),   // right small
    )
    for ((gx, gy, gr) in gems) {
        val gem = Path().apply {
            moveTo(gx, gy - gr)
            lineTo(gx + gr * 0.7f, gy)
            lineTo(gx, gy + gr)
            lineTo(gx - gr * 0.7f, gy)
            close()
        }
        drawPath(gem, color)
        drawPath(gem, Color.White.copy(alpha = 0.7f), style = Stroke(width = size * 0.015f))
        // Diagonal facet highlight
        drawLine(Color.White.copy(alpha = 0.6f),
            Offset(gx - gr * 0.3f, gy - gr * 0.4f),
            Offset(gx + gr * 0.3f, gy + gr * 0.4f),
            strokeWidth = size * 0.012f)
        // Center sparkle
        drawCircle(Color.White, gr * 0.18f, Offset(gx - gr * 0.15f, gy - gr * 0.2f))
    }
}

/**
 * Aura — Round 80 audit fix: redesigned thành "CLOVER LEAF" (3 lobes + stem)
 * thay heart-with-rays (vẫn dup motif với HEART/REVIVE_TOKEN). Reads as
 * "healing herbal aura" — match HEALING_AURA function.
 */
private fun DrawScope.drawAuraRingShape(cx: Float, cy: Float, size: Float, color: Color) {
    val lobeR = size * 0.18f
    val centerY = cy - size * 0.05f
    // 3 clover lobes: top + bottom-left + bottom-right
    val offsetD = lobeR * 1.0f
    drawCircle(color, lobeR, Offset(cx, centerY - offsetD))
    drawCircle(color, lobeR, Offset(cx - offsetD * 0.95f, centerY + offsetD * 0.65f))
    drawCircle(color, lobeR, Offset(cx + offsetD * 0.95f, centerY + offsetD * 0.65f))
    // White spots inside each lobe (organic detail)
    drawCircle(Color.White.copy(alpha = 0.70f), lobeR * 0.30f,
        Offset(cx - lobeR * 0.25f, centerY - offsetD - lobeR * 0.20f))
    drawCircle(Color.White.copy(alpha = 0.70f), lobeR * 0.30f,
        Offset(cx - offsetD * 0.95f - lobeR * 0.25f, centerY + offsetD * 0.65f - lobeR * 0.20f))
    drawCircle(Color.White.copy(alpha = 0.70f), lobeR * 0.30f,
        Offset(cx + offsetD * 0.95f - lobeR * 0.25f, centerY + offsetD * 0.65f - lobeR * 0.20f))
    // Stem curving down
    val stemPath = Path().apply {
        moveTo(cx, centerY + offsetD * 0.50f)
        cubicTo(cx + size * 0.05f, cy + size * 0.30f,
            cx + size * 0.08f, cy + size * 0.40f,
            cx + size * 0.10f, cy + size * 0.45f)
    }
    drawPath(stemPath, color, style = Stroke(width = size * 0.05f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round))
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

/**
 * Crit Surge — audit redesign: KATANA (curved blade + handle + tsuba guard)
 * thay dagger (dup vertical-pointed cluster với SHARD/LANCE/TRIDENT). Reads
 * như "critical strike samurai-style" — match CRIT_SURGE function.
 */
private fun DrawScope.drawCrystalSparkShape(cx: Float, cy: Float, size: Float, color: Color) {
    // Curved katana blade (top-right diagonal arc)
    val bladeStartX = cx - size * 0.30f
    val bladeStartY = cy + size * 0.25f
    val bladeEndX = cx + size * 0.35f
    val bladeEndY = cy - size * 0.30f
    // Spine path (curved)
    val spinePath = Path().apply {
        moveTo(bladeStartX, bladeStartY)
        cubicTo(bladeStartX + size * 0.20f, bladeStartY - size * 0.15f,
            bladeEndX - size * 0.20f, bladeEndY + size * 0.05f,
            bladeEndX, bladeEndY)
    }
    // Edge path (parallel curve below spine)
    val edgePath = Path().apply {
        moveTo(bladeStartX + size * 0.05f, bladeStartY + size * 0.08f)
        cubicTo(bladeStartX + size * 0.25f, bladeStartY - size * 0.05f,
            bladeEndX - size * 0.15f, bladeEndY + size * 0.13f,
            bladeEndX + size * 0.08f, bladeEndY + size * 0.05f)
    }
    // Blade fill (filled region between spine and edge)
    val bladePath = Path().apply {
        moveTo(bladeStartX, bladeStartY)
        cubicTo(bladeStartX + size * 0.20f, bladeStartY - size * 0.15f,
            bladeEndX - size * 0.20f, bladeEndY + size * 0.05f,
            bladeEndX, bladeEndY)
        lineTo(bladeEndX + size * 0.08f, bladeEndY + size * 0.05f)
        cubicTo(bladeEndX - size * 0.15f, bladeEndY + size * 0.13f,
            bladeStartX + size * 0.25f, bladeStartY - size * 0.05f,
            bladeStartX + size * 0.05f, bladeStartY + size * 0.08f)
        close()
    }
    drawPath(bladePath, color)
    drawPath(spinePath, Color.White.copy(alpha = 0.75f),
        style = Stroke(width = size * 0.015f))
    // Tsuba (round guard at handle base)
    drawCircle(color, size * 0.06f, Offset(bladeStartX - size * 0.04f, bladeStartY + size * 0.05f))
    // Handle (short rect bottom-left, wrapped grip pattern)
    val handleW = size * 0.05f
    val handleLen = size * 0.18f
    val handleAngle = kotlin.math.atan2((bladeStartY + size * 0.05f) - (cy + size * 0.42f),
        (bladeStartX - size * 0.04f) - (cx - size * 0.38f))
    val hex = cx - size * 0.38f
    val hey = cy + size * 0.42f
    drawLine(color,
        Offset(hex, hey),
        Offset(bladeStartX - size * 0.04f, bladeStartY + size * 0.05f),
        strokeWidth = handleW,
        cap = androidx.compose.ui.graphics.StrokeCap.Round)
    // Grip wraps (3 white perpendicular ticks on handle)
    for (i in 0 until 3) {
        val t = (i + 1) / 4f
        val px = hex + ((bladeStartX - size * 0.04f) - hex) * t
        val py = hey + ((bladeStartY + size * 0.05f) - hey) * t
        drawCircle(Color.White.copy(alpha = 0.65f), size * 0.012f, Offset(px, py))
    }
}

/**
 * Double arrow — Round 80 audit fix: redesigned thành "TWIN CANNON" (2 vertical
 * barrels with muzzle bursts) thay 2 chevrons (dup motif với ARROW_RIGHT).
 * Reads as "double-fire" — match DOUBLE_FIRE function.
 */
private fun DrawScope.drawDoubleArrowShape(cx: Float, cy: Float, size: Float, color: Color) {
    val barrelW = size * 0.16f
    val barrelH = size * 0.60f
    val gap = size * 0.30f
    // 2 vertical barrels
    for (side in intArrayOf(-1, 1)) {
        val bx = cx + side * gap / 2f
        drawRoundRect(color,
            topLeft = Offset(bx - barrelW / 2f, cy - barrelH / 2f),
            size = Size(barrelW, barrelH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barrelW * 0.30f))
        // Muzzle flash at top
        drawCircle(Color.White.copy(alpha = 0.85f), barrelW * 0.45f,
            Offset(bx, cy - barrelH / 2f - barrelW * 0.15f))
        drawCircle(color, barrelW * 0.25f,
            Offset(bx, cy - barrelH / 2f - barrelW * 0.15f))
    }
    // Connecting plate at bottom
    drawRoundRect(color,
        topLeft = Offset(cx - size * 0.32f, cy + barrelH / 2f - size * 0.05f),
        size = Size(size * 0.64f, size * 0.16f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.04f))
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

/** Giant Boulder — rocky asteroid silhouette với craters (replaces simple disc). */
private fun DrawScope.drawBigDotShape(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.42f
    // Irregular rock outline (8-vertex with noise)
    val noise = floatArrayOf(1.0f, 0.92f, 1.05f, 0.88f, 1.0f, 0.95f, 1.02f, 0.9f)
    val path = Path().apply {
        for (i in 0 until 8) {
            val a = i * Math.PI / 4.0
            val rr = r * noise[i]
            val x = cx + (rr * kotlin.math.cos(a)).toFloat()
            val y = cy + (rr * kotlin.math.sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color)
    drawPath(path, Color.Black.copy(alpha = 0.45f), style = Stroke(width = size * 0.03f))
    // 3 craters (small darker circles)
    drawCircle(Color.Black.copy(alpha = 0.40f), size * 0.07f, Offset(cx - r * 0.30f, cy - r * 0.20f))
    drawCircle(Color.Black.copy(alpha = 0.40f), size * 0.05f, Offset(cx + r * 0.35f, cy + r * 0.05f))
    drawCircle(Color.Black.copy(alpha = 0.40f), size * 0.04f, Offset(cx, cy + r * 0.35f))
    // Crack line
    drawLine(Color.Black.copy(alpha = 0.55f),
        Offset(cx - r * 0.30f, cy + r * 0.10f), Offset(cx + r * 0.20f, cy - r * 0.10f),
        strokeWidth = size * 0.02f)
}

// ──────────────────────────────────────────────────────────────────────────
// Round 79 — 2 more booster shapes to eliminate remaining visual dups.
// ──────────────────────────────────────────────────────────────────────────

/** Rage fang — jagged downward fang/teeth row (BERSERK rage). */
private fun DrawScope.drawRageFangShape(cx: Float, cy: Float, size: Float, color: Color) {
    val w = size * 0.80f
    val h = size * 0.65f
    // 5 fang teeth pointing down
    val toothCount = 5
    val toothW = w / toothCount
    val path = Path().apply {
        moveTo(cx - w / 2f, cy - h / 2f)
        // Top edge (lip)
        for (i in 0..toothCount) {
            val px = cx - w / 2f + i * toothW
            lineTo(px, cy - h / 2f)
        }
        // Right side down
        lineTo(cx + w / 2f, cy - h * 0.15f)
        // Bottom edge — alternating fang tips
        for (i in toothCount - 1 downTo 0) {
            val tipX = cx - w / 2f + i * toothW + toothW / 2f
            val tipY = if (i % 2 == 0) cy + h / 2f else cy + h * 0.25f
            val nextValleyX = cx - w / 2f + i * toothW
            val nextValleyY = cy - h * 0.15f
            lineTo(tipX, tipY)
            lineTo(nextValleyX, nextValleyY)
        }
        close()
    }
    drawPath(path, color)
    // Inner highlight: white drip down center
    drawCircle(Color.White.copy(alpha = 0.7f), size * 0.05f,
        Offset(cx, cy - h * 0.10f))
    // Blood drip at center fang tip
    drawCircle(Color.White.copy(alpha = 0.5f), size * 0.03f,
        Offset(cx, cy + h * 0.55f))
}

/** Healing flask — potion bottle silhouette with liquid level (QUICK_HEAL). */
private fun DrawScope.drawHealingFlaskShape(cx: Float, cy: Float, size: Float, color: Color) {
    val bottleW = size * 0.50f
    val bottleH = size * 0.65f
    val neckW = bottleW * 0.40f
    val neckH = bottleH * 0.25f
    // Bottle neck
    drawRoundRect(color,
        topLeft = Offset(cx - neckW / 2f, cy - bottleH / 2f),
        size = Size(neckW, neckH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size * 0.04f))
    // Bottle body (rounded rect)
    drawRoundRect(color,
        topLeft = Offset(cx - bottleW / 2f, cy - bottleH / 2f + neckH * 0.65f),
        size = Size(bottleW, bottleH * 0.85f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(bottleW * 0.25f))
    // Liquid level (inner brighter color, partial fill)
    val liquidTopY = cy - bottleH * 0.08f
    drawRoundRect(Color.White.copy(alpha = 0.85f),
        topLeft = Offset(cx - bottleW * 0.40f, liquidTopY),
        size = Size(bottleW * 0.80f, bottleH * 0.40f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(bottleW * 0.20f))
    // Round 81 audit fix — removed cross marking (user complained vẫn dup CROSS).
    // Thay bằng "❤" (heart symbol minimal) trên flask body để indicate "healing".
    val heartLobeR = size * 0.04f
    drawCircle(color, heartLobeR, Offset(cx - heartLobeR * 0.85f, cy + bottleH * 0.05f))
    drawCircle(color, heartLobeR, Offset(cx + heartLobeR * 0.85f, cy + bottleH * 0.05f))
    val heartTri = Path().apply {
        moveTo(cx - heartLobeR * 1.85f, cy + bottleH * 0.08f)
        lineTo(cx + heartLobeR * 1.85f, cy + bottleH * 0.08f)
        lineTo(cx, cy + bottleH * 0.20f)
        close()
    }
    drawPath(heartTri, color)
    // Bubble dots inside liquid (3 small)
    for (i in 0 until 3) {
        drawCircle(Color.White.copy(alpha = 0.5f), size * 0.02f,
            Offset(cx - bottleW * 0.20f + i * bottleW * 0.20f, cy + bottleH * 0.18f - i * size * 0.04f))
    }
    // Cap (top of neck)
    drawRect(color,
        topLeft = Offset(cx - neckW * 0.65f, cy - bottleH / 2f - size * 0.04f),
        size = Size(neckW * 1.30f, size * 0.05f))
}
