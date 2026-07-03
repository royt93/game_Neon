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

        drawBoosterShape(booster.shape, cx, cy, sizePx, glowColor)
    }
}

/**
 * Shared booster shape dispatcher — used by both [BoosterCanvas] (in-game
 * render) and InfoScreen.drawBoosterPreview (Bách Khoa Vật phẩm tab) so the
 * silhouette stays identical across both surfaces. Exhaustive `when` over
 * [BoosterShape] — compiler flags new values.
 */
internal fun DrawScope.drawBoosterShape(
    shape: BoosterShape,
    cx: Float,
    cy: Float,
    size: Float,
    color: Color,
) {
    when (shape) {
        BoosterShape.CROSS -> drawCross(cx, cy, size, color)
        BoosterShape.OCTAGON -> drawOctagon(cx, cy, size, color)
        BoosterShape.TRIANGLE_UP -> drawTriangleUp(cx, cy, size, color)
        BoosterShape.TRIPLE_BARS -> drawTripleBars(cx, cy, size, color)
        BoosterShape.STAR -> drawStar(cx, cy, size, color)
        BoosterShape.HEART -> drawHeart(cx, cy, size, color)
        BoosterShape.ATOM -> drawAtomShape(cx, cy, size, color)
        BoosterShape.FLAME -> drawFlameShape(cx, cy, size, color)
        BoosterShape.MAGNET -> drawMagnetShape(cx, cy, size, color)
        BoosterShape.LIGHTNING -> drawLightningShape(cx, cy, size, color)
        BoosterShape.CROSSHAIR -> drawCrosshairShape(cx, cy, size, color)
        BoosterShape.BEAM -> drawBeamShape(cx, cy, size, color)
        BoosterShape.SPLIT_FORK -> drawSplitShape(cx, cy, size, color)
        BoosterShape.ARROW_RIGHT -> drawArrowRightShape(cx, cy, size, color)
        BoosterShape.RING_PULSE -> drawRingPulseShape(cx, cy, size, color)
        BoosterShape.DOLLAR -> drawDollarShape(cx, cy, size, color)
        BoosterShape.SHARD -> drawShardShape(cx, cy, size, color)
        BoosterShape.AURA_RING -> drawAuraRingShape(cx, cy, size, color)
        BoosterShape.PHASE_DIAMOND -> drawPhaseDiamondShape(cx, cy, size, color)
        BoosterShape.CLOUD_PUFF -> drawCloudPuffShape(cx, cy, size, color)
        BoosterShape.SPREAD_FAN -> drawSpreadFanShape(cx, cy, size, color)
        BoosterShape.CRYSTAL_SPARK -> drawCrystalSparkShape(cx, cy, size, color)
        BoosterShape.DOUBLE_ARROW -> drawDoubleArrowShape(cx, cy, size, color)
        BoosterShape.ARROW_CYCLE -> drawArrowCycleShape(cx, cy, size, color)
        BoosterShape.BIG_DOT -> drawBigDotShape(cx, cy, size, color)
        BoosterShape.RAGE_FANG -> drawRageFangShape(cx, cy, size, color)
        BoosterShape.HEALING_FLASK -> drawHealingFlaskShape(cx, cy, size, color)
        BoosterShape.REGEN_PULSE -> drawRegenPulseShape(cx, cy, size, color)
        BoosterShape.FREEZE_FLAKE -> drawFreezeFlakeShape(cx, cy, size, color)
        BoosterShape.MINI_RING -> drawMiniRingShape(cx, cy, size, color)
        BoosterShape.VAMPIRE_FANG -> drawVampireFangShape(cx, cy, size, color)
        BoosterShape.GHOST_TRAIL -> drawGhostTrailShape(cx, cy, size, color)
        BoosterShape.GRAVITY_WELL -> drawGravityWellShape(cx, cy, size, color)
        BoosterShape.REFLECT_BUMPER -> drawReflectBumperShape(cx, cy, size, color)
        BoosterShape.CHAIN_BOLT -> drawChainBoltShape(cx, cy, size, color)
        BoosterShape.CLONE_PAIR -> drawClonePairShape(cx, cy, size, color)
        BoosterShape.DRONE_ROTOR -> drawDroneShape(cx, cy, size, color)
    }
}

/**
 * Wave 16 — vector mini-glyph badge, replacing the `Text(glyph)` overlay in
 * [com.tranphuloi.neon.ui.game.world.GameWorld].
 *
 * Bug fixed: many booster glyphs were exotic Unicode (Ѱ Ѵ ѻ ǁ ⊛ ⌇ ❍ ⇋ ⚜ ⊚ ◌
 * …) the device font has no glyph for → Android rendered the missing-glyph box
 * ("?"/tofu). Drawing each as a tiny vector removes the font dependency
 * entirely. Dispatch on the existing glyph String so the data model +
 * `BoosterGlyphTest` stay untouched. Unknown glyph → small dot (never tofu).
 *
 * Drawn into a small square badge centered at (cx, cy); [sz] is the badge edge.
 */
/**
 * Every glyph string [drawBoosterGlyphVector] draws a dedicated vector for.
 * Pinned against the booster catalog by `BoosterGlyphVectorTest`: any booster
 * glyph NOT in this set would fall through to the generic-dot `else` branch
 * (losing its distinct badge). Keep in sync with the `when` arms below.
 */
internal val BOOSTER_GLYPH_VECTORS: Set<String> = setOf(
    "→", "◯", "♨", "◎", "⇄", "⬤", "❍", "⌇", "⊛", "⊙",
    "Ѱ", "⊕", "✱", "☆", "⚡", "◇", "$", "✚", "✦", "+",
    "⚯", "♻", "❄", "◌", "Ѵ", "ѻ", "⊚", "⇋", "⚜", "ǁ",
    "◈",
)

internal fun DrawScope.drawBoosterGlyphVector(
    glyph: String, cx: Float, cy: Float, sz: Float, color: Color,
) {
    val r = sz * 0.40f
    val sw = (sz * 0.11f).coerceAtLeast(1.5f)
    val capR = androidx.compose.ui.graphics.StrokeCap.Round
    fun ring(rr: Float, w: Float = sw) =
        drawCircle(color, rr, Offset(cx, cy), style = Stroke(width = w))
    fun dot(rr: Float, ox: Float = 0f, oy: Float = 0f) =
        drawCircle(color, rr, Offset(cx + ox, cy + oy))
    fun ln(x1: Float, y1: Float, x2: Float, y2: Float, w: Float = sw) =
        drawLine(color, Offset(cx + x1, cy + y1), Offset(cx + x2, cy + y2), strokeWidth = w, cap = capR)
    // n evenly-spaced spokes from center (asterisk/snowflake/sparkle base).
    fun spokes(n: Int, len: Float, startDeg: Double = -90.0) {
        for (i in 0 until n) {
            val a = Math.toRadians(startDeg + i * 360.0 / n)
            ln(0f, 0f, (len * kotlin.math.cos(a)).toFloat(), (len * kotlin.math.sin(a)).toFloat())
        }
    }
    fun arrow(dx: Float, dy: Float, tx: Float, ty: Float) {
        ln(dx, dy, tx, ty)
        val ang = kotlin.math.atan2((ty - dy).toDouble(), (tx - dx).toDouble())
        val h = sz * 0.16f
        for (s in listOf(2.5, -2.5)) {
            ln(tx, ty, tx + (h * kotlin.math.cos(ang + s)).toFloat(), ty + (h * kotlin.math.sin(ang + s)).toFloat())
        }
    }
    fun star(points: Int, outer: Float, inner: Float, fill: Boolean) {
        val p = PathPool.acquire().apply {
            val step = Math.PI / points
            for (i in 0 until points * 2) {
                val a = -Math.PI / 2.0 + i * step
                val rr = if (i % 2 == 0) outer else inner
                val x = cx + (rr * kotlin.math.cos(a)).toFloat()
                val y = cy + (rr * kotlin.math.sin(a)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        if (fill) drawPath(p, color) else drawPath(p, color, style = Stroke(width = sw * 0.8f))
        PathPool.release(p)
    }
    fun diamond(half: Float, fill: Boolean) {
        val p = PathPool.acquire().apply {
            moveTo(cx, cy - half); lineTo(cx + half * 0.8f, cy)
            lineTo(cx, cy + half); lineTo(cx - half * 0.8f, cy); close()
        }
        if (fill) drawPath(p, color) else drawPath(p, color, style = Stroke(width = sw * 0.8f))
        PathPool.release(p)
    }
    fun plus(arm: Float) { ln(-arm, 0f, arm, 0f); ln(0f, -arm, 0f, arm) }

    when (glyph) {
        "→" -> arrow(-r, 0f, r, 0f)
        "◯" -> ring(r)
        "♨" -> { ln(-r * 0.5f, r * 0.4f, -r * 0.5f, -r * 0.4f, sw * 0.8f); ln(r * 0.5f, r * 0.4f, r * 0.5f, -r * 0.4f, sw * 0.8f); ln(0f, r * 0.5f, 0f, -r * 0.3f, sw * 0.8f) }
        "◎" -> { ring(r); dot(r * 0.42f) }
        "⇄" -> { arrow(-r, -r * 0.4f, r, -r * 0.4f); arrow(r, r * 0.4f, -r, r * 0.4f) }
        "⬤" -> dot(r)
        "❍" -> { ring(r * 0.85f); dot(r * 0.28f, r * 0.4f, -r * 0.4f) }
        "⌇" -> { ln(0f, -r, r * 0.5f, -r * 0.4f); ln(r * 0.5f, -r * 0.4f, -r * 0.5f, r * 0.1f); ln(-r * 0.5f, r * 0.1f, r * 0.3f, r) }
        "⊛" -> { ring(r); spokes(4, r * 0.55f, -45.0) }
        "⊙" -> { ring(r); dot(r * 0.22f) }
        "Ѱ" -> { ln(0f, -r, 0f, r); ln(0f, r * 0.1f, -r * 0.7f, -r * 0.7f); ln(0f, r * 0.1f, r * 0.7f, -r * 0.7f); ln(-r * 0.5f, r, r * 0.5f, r) }
        "⊕" -> { ring(r); plus(r * 0.55f) }
        "✱" -> spokes(6, r)
        "☆" -> star(5, r, r * 0.45f, fill = false)
        "⚡" -> { ln(r * 0.3f, -r, -r * 0.2f, 0f); ln(-r * 0.2f, 0f, r * 0.15f, 0f); ln(r * 0.15f, 0f, -r * 0.3f, r) }
        "◇" -> diamond(r, fill = false)
        "$" -> { ln(0f, -r * 1.05f, 0f, r * 1.05f, sw * 0.8f); ln(-r * 0.4f, -r * 0.5f, r * 0.4f, -r * 0.5f); ln(-r * 0.4f, 0f, r * 0.4f, 0f); ln(-r * 0.4f, r * 0.5f, r * 0.4f, r * 0.5f) }
        "✚" -> { ln(-r, 0f, r, 0f, sw * 1.4f); ln(0f, -r, 0f, r, sw * 1.4f) }
        "✦" -> { spokes(4, r); spokes(4, r * 0.5f, -45.0) }
        "+" -> plus(r * 0.8f)
        "⚯" -> { drawCircle(color, r * 0.45f, Offset(cx - r * 0.4f, cy), style = Stroke(width = sw)); drawCircle(color, r * 0.45f, Offset(cx + r * 0.4f, cy), style = Stroke(width = sw)) }
        "♻" -> { ring(r * 0.9f); arrow(r * 0.9f, -r * 0.2f, r * 0.5f, -r * 0.7f) }
        "❄" -> { spokes(6, r); for (i in 0 until 6) { val a = Math.toRadians(-90.0 + i * 60.0); val bx = (r * 0.6f * kotlin.math.cos(a)).toFloat(); val by = (r * 0.6f * kotlin.math.sin(a)).toFloat(); ln(bx, by, bx + sz * 0.1f, by - sz * 0.1f, sw * 0.7f); ln(bx, by, bx - sz * 0.1f, by - sz * 0.1f, sw * 0.7f) } }
        "◌" -> for (i in 0 until 8) { val a = Math.toRadians(i * 45.0); dot(sw * 0.6f, (r * kotlin.math.cos(a)).toFloat(), (r * kotlin.math.sin(a)).toFloat()) }
        "Ѵ" -> { ln(-r * 0.7f, -r, 0f, r); ln(r * 0.7f, -r, 0f, r) }
        "ѻ" -> { ring(r * 0.7f); ln(r * 0.45f, r * 0.45f, r, r) }
        "⊚" -> { ring(r); ring(r * 0.5f) }
        "⇋" -> { arrow(-r * 0.4f, -r, -r * 0.4f, r); arrow(r * 0.4f, r, r * 0.4f, -r) }
        "⚜" -> { dot(sw * 0.7f, -r * 0.6f, -r * 0.6f); dot(sw * 0.7f, r * 0.5f, -r * 0.1f); dot(sw * 0.7f, -r * 0.3f, r * 0.6f); ln(-r * 0.6f, -r * 0.6f, r * 0.5f, -r * 0.1f); ln(r * 0.5f, -r * 0.1f, -r * 0.3f, r * 0.6f) }
        "ǁ" -> { ln(-r * 0.35f, -r, -r * 0.35f, r); ln(r * 0.35f, -r, r * 0.35f, r) }
        "◈" -> { diamond(r, fill = false); dot(r * 0.3f) } // drone: hình thoi viền + lõi
        else -> dot(sz * 0.14f)
    }
}

/** Two small ship silhouettes side-by-side — CLONE_BOOSTER (phantom twin firing alongside). */
private fun DrawScope.drawClonePairShape(cx: Float, cy: Float, size: Float, color: Color) {
    val shipW = size * 0.22f
    val shipH = size * 0.32f
    val gap = size * 0.08f
    // Left ship (solid)
    val leftCx = cx - shipW / 2f - gap / 2f
    drawShipSilhouette(leftCx, cy, shipW, shipH, color, alpha = 1f)
    // Right ship (translucent phantom)
    val rightCx = cx + shipW / 2f + gap / 2f
    drawShipSilhouette(rightCx, cy, shipW, shipH, color, alpha = 0.55f)
    // Linking arrow between them (suggests "twin firing")
    drawLine(color.copy(alpha = 0.4f),
        Offset(leftCx + shipW * 0.5f, cy + shipH * 0.6f),
        Offset(rightCx - shipW * 0.5f, cy + shipH * 0.6f),
        strokeWidth = size * 0.03f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round,
    )
}

/** Pentagon ship silhouette (arrow up). Used by drawClonePairShape. */
private fun DrawScope.drawShipSilhouette(
    cx: Float, cy: Float, w: Float, h: Float, color: Color, alpha: Float = 1f,
) {
    // R80 perf — Path pooled to avoid GC churn (this recipe is in CLONE_PAIR
    // preview which renders per-frame for every CLONE_BOOSTER on screen).
    val path = PathPool.acquire().apply {
        moveTo(cx, cy - h / 2f)                       // nose
        lineTo(cx + w / 2f, cy + h / 4f)              // right wing
        lineTo(cx + w / 4f, cy + h / 2f)              // right tail
        lineTo(cx - w / 4f, cy + h / 2f)              // left tail
        lineTo(cx - w / 2f, cy + h / 4f)              // left wing
        close()
    }
    drawPath(path, color.copy(alpha = alpha))
    PathPool.release(path)
}

// ──────────────────────────────────────────────────────────────────────────
// Wave 11a — 3 new shape recipes for new boosters
// ──────────────────────────────────────────────────────────────────────────

/** Pulsing pill (3 nested rings) — REGEN_BOOSTER, hints "slow continuous heal". */
private fun DrawScope.drawRegenPulseShape(cx: Float, cy: Float, size: Float, color: Color) {
    val rOuter = size * 0.45f
    val rMid = size * 0.30f
    val rInner = size * 0.15f
    drawCircle(color.copy(alpha = 0.30f), rOuter, Offset(cx, cy),
        style = Stroke(width = size * 0.06f))
    drawCircle(color.copy(alpha = 0.55f), rMid, Offset(cx, cy),
        style = Stroke(width = size * 0.05f))
    drawCircle(color, rInner, Offset(cx, cy))
}

/** 6-arm snowflake — TIME_FREEZE_BOOSTER. Each arm has 1 short side-branch. */
private fun DrawScope.drawFreezeFlakeShape(cx: Float, cy: Float, size: Float, color: Color) {
    val armLen = size * 0.45f
    val sideLen = size * 0.15f
    val stroke = size * 0.06f
    for (i in 0 until 6) {
        val angle = i * (Math.PI / 3.0)
        val ax = (kotlin.math.cos(angle) * armLen).toFloat()
        val ay = (kotlin.math.sin(angle) * armLen).toFloat()
        drawLine(color, Offset(cx, cy), Offset(cx + ax, cy + ay),
            strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // 2 side-branches at 60% along the arm
        val bx = cx + ax * 0.6f
        val by = cy + ay * 0.6f
        val sideAngle1 = angle + Math.PI / 3.0
        val sideAngle2 = angle - Math.PI / 3.0
        drawLine(color,
            Offset(bx, by),
            Offset(bx + (kotlin.math.cos(sideAngle1) * sideLen).toFloat(),
                   by + (kotlin.math.sin(sideAngle1) * sideLen).toFloat()),
            strokeWidth = stroke * 0.7f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color,
            Offset(bx, by),
            Offset(bx + (kotlin.math.cos(sideAngle2) * sideLen).toFloat(),
                   by + (kotlin.math.sin(sideAngle2) * sideLen).toFloat()),
            strokeWidth = stroke * 0.7f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // Center dot
    drawCircle(color, size * 0.06f, Offset(cx, cy))
}

/** Small ring with right-arrow inside — MINI_BOOSTER (shrink + speed up). */
private fun DrawScope.drawMiniRingShape(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.30f
    drawCircle(color, r, Offset(cx, cy),
        style = Stroke(width = size * 0.08f))
    // Right-pointing arrow inside (speed indicator)
    val arrowSize = size * 0.18f
    val path = PathPool.acquire().apply {
        moveTo(cx - arrowSize * 0.5f, cy - arrowSize * 0.5f)
        lineTo(cx + arrowSize * 0.5f, cy)
        lineTo(cx - arrowSize * 0.5f, cy + arrowSize * 0.5f)
        close()
    }
    drawPath(path, color)
    PathPool.release(path)
}

/** Two downward fangs + blood drop — VAMPIRE_BOOSTER (lifesteal). */
private fun DrawScope.drawVampireFangShape(cx: Float, cy: Float, size: Float, color: Color) {
    val fangW = size * 0.10f
    val fangH = size * 0.35f
    val gap = size * 0.10f
    // Left fang (triangle pointing down)
    val leftFang = PathPool.acquire().apply {
        moveTo(cx - gap - fangW, cy - fangH * 0.4f)
        lineTo(cx - gap, cy - fangH * 0.4f)
        lineTo(cx - gap - fangW * 0.5f, cy + fangH * 0.6f)
        close()
    }
    val rightFang = PathPool.acquire().apply {
        moveTo(cx + gap, cy - fangH * 0.4f)
        lineTo(cx + gap + fangW, cy - fangH * 0.4f)
        lineTo(cx + gap + fangW * 0.5f, cy + fangH * 0.6f)
        close()
    }
    drawPath(leftFang, color)
    drawPath(rightFang, color)
    PathPool.release(leftFang)
    PathPool.release(rightFang)
    // Blood drop below center
    drawCircle(color, size * 0.07f, Offset(cx, cy + fangH * 0.85f))
    // Mouth arc above fangs (suggests vampire grin)
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - size * 0.25f, cy - size * 0.42f),
        size = androidx.compose.ui.geometry.Size(size * 0.50f, size * 0.20f),
        style = Stroke(width = size * 0.05f),
    )
}

/** 3 concentric rings + center dot pulled inward — GRAVITY_BOOSTER (mineral auto-collect). */
private fun DrawScope.drawGravityWellShape(cx: Float, cy: Float, size: Float, color: Color) {
    val rOuter = size * 0.45f
    val rMid = size * 0.30f
    val rInner = size * 0.15f
    drawCircle(color.copy(alpha = 0.30f), rOuter, Offset(cx, cy),
        style = Stroke(width = size * 0.04f))
    drawCircle(color.copy(alpha = 0.60f), rMid, Offset(cx, cy),
        style = Stroke(width = size * 0.05f))
    drawCircle(color, rInner, Offset(cx, cy))
    // 4 inward arrows at cardinal directions (suggests "pulling in")
    val arrowLen = size * 0.10f
    val arrowGap = size * 0.06f
    for (i in 0 until 4) {
        val angle = i * (Math.PI / 2.0)
        val ax = (kotlin.math.cos(angle) * (rOuter + arrowGap)).toFloat()
        val ay = (kotlin.math.sin(angle) * (rOuter + arrowGap)).toFloat()
        val tx = (kotlin.math.cos(angle) * (rOuter + arrowGap + arrowLen)).toFloat()
        val ty = (kotlin.math.sin(angle) * (rOuter + arrowGap + arrowLen)).toFloat()
        drawLine(color, Offset(cx + tx, cy + ty), Offset(cx + ax, cy + ay),
            strokeWidth = size * 0.04f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

/** Shield arc + outward bounce arrow — REFLECT_BOOSTER (deflect enemy lasers). */
private fun DrawScope.drawReflectBumperShape(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.38f
    // Upper shield arc
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
        style = Stroke(width = size * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
    )
    // Two outward chevrons (bounce indicators) — up-left + up-right
    val chevLen = size * 0.12f
    for (sign in listOf(-1, 1)) {
        val baseX = cx + sign * r * 0.55f
        val baseY = cy - r * 0.10f
        val tipX = baseX + sign * chevLen * 0.7f
        val tipY = baseY - chevLen
        drawLine(color, Offset(baseX, baseY), Offset(tipX, tipY),
            strokeWidth = size * 0.05f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, Offset(tipX, tipY), Offset(tipX + sign * chevLen * 0.4f, tipY + chevLen * 0.5f),
            strokeWidth = size * 0.05f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // Center dot
    drawCircle(color, size * 0.06f, Offset(cx, cy))
}

/** 3-node zigzag bolt — CHAIN_LIGHTNING_BOOSTER (laser chains to 2 more enemies). */
private fun DrawScope.drawChainBoltShape(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.10f
    // 3 nodes arranged in lightning Z pattern
    val nodes = listOf(
        Offset(cx - size * 0.30f, cy - size * 0.30f),
        Offset(cx + size * 0.15f, cy - size * 0.05f),
        Offset(cx - size * 0.10f, cy + size * 0.30f),
    )
    // Connecting bolts (zigzag stroke)
    for (i in 0 until nodes.size - 1) {
        drawLine(color, nodes[i], nodes[i + 1],
            strokeWidth = size * 0.05f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
    // Node circles (chain targets)
    for (node in nodes) {
        drawCircle(color, r, node)
        drawCircle(color.copy(alpha = 0.5f), r * 1.5f, node,
            style = Stroke(width = size * 0.02f))
    }
}

/** Dashed circle + 3 trailing dots — GHOST_BOOSTER (intangible/pass-through). */
private fun DrawScope.drawGhostTrailShape(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.32f
    val dashCount = 10
    val sweepPerDash = 360f / dashCount
    val dashSweep = sweepPerDash * 0.55f
    // Dashed circle outline (translucent body)
    for (i in 0 until dashCount) {
        drawArc(
            color = color,
            startAngle = i * sweepPerDash,
            sweepAngle = dashSweep,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = Stroke(width = size * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
    // 3 trailing dots fading right (motion trail / phantom afterimage)
    drawCircle(color.copy(alpha = 0.85f), size * 0.05f, Offset(cx + r + size * 0.10f, cy))
    drawCircle(color.copy(alpha = 0.55f), size * 0.04f, Offset(cx + r + size * 0.22f, cy))
    drawCircle(color.copy(alpha = 0.30f), size * 0.03f, Offset(cx + r + size * 0.32f, cy))
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

/** Flame — 3 petal "fire" shape pointing up. R80 — Path pooled. */
private fun DrawScope.drawFlameShape(cx: Float, cy: Float, size: Float, color: Color) {
    val h = size * 0.85f
    val w = size * 0.55f
    val outer = PathPool.acquire().apply {
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
    PathPool.release(outer)
    // Inner flame (lighter)
    val inner = PathPool.acquire().apply {
        val ih = h * 0.55f
        val iw = w * 0.50f
        moveTo(cx, cy - ih / 2f)
        cubicTo(cx + iw / 2f, cy, cx + iw / 4f, cy + ih / 4f, cx, cy + ih / 2f)
        cubicTo(cx - iw / 4f, cy + ih / 4f, cx - iw / 2f, cy, cx, cy - ih / 2f)
        close()
    }
    drawPath(inner, Color.White.copy(alpha = 0.8f))
    PathPool.release(inner)
}

/** Magnet — U-shape horseshoe with 2 pole tips. R80 — Path pooled. */
private fun DrawScope.drawMagnetShape(cx: Float, cy: Float, size: Float, color: Color) {
    val w = size * 0.75f
    val h = size * 0.80f
    val thick = size * 0.18f
    // U-shape path
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
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
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
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
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
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
    val lance = PathPool.acquire().apply {
        moveTo(cx, cy - halfH)              // tip top
        lineTo(cx + w / 2f, cy - halfH * 0.20f)
        lineTo(cx + w * 0.30f, cy + halfH * 0.85f)  // base right
        lineTo(cx - w * 0.30f, cy + halfH * 0.85f)  // base left
        lineTo(cx - w / 2f, cy - halfH * 0.20f)
        close()
    }
    drawPath(lance, color)
    drawPath(lance, Color.White.copy(alpha = 0.65f), style = Stroke(width = size * 0.025f))
    PathPool.release(lance)
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
    val gemPath = PathPool.acquire().apply {
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
    PathPool.release(gemPath)
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
    val hlPath = PathPool.acquire().apply {
        moveTo(cx, cy - halfH + size * 0.04f)
        lineTo(cx + halfW * 0.65f, cy - halfH * 0.40f)
        lineTo(cx + halfW * 0.30f, cy - halfH * 0.18f)
        close()
    }
    drawPath(hlPath, Color.White.copy(alpha = 0.45f))
    PathPool.release(hlPath)
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
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
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
        val prong = PathPool.acquire().apply {
            moveTo(tipX, tipY)
            lineTo(tipX + barW * 0.55f, baseY)
            lineTo(tipX - barW * 0.55f, baseY)
            close()
        }
        drawPath(prong, color)
        PathPool.release(prong)
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
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
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
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
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
        val gem = PathPool.acquire().apply {
            moveTo(gx, gy - gr)
            lineTo(gx + gr * 0.7f, gy)
            lineTo(gx, gy + gr)
            lineTo(gx - gr * 0.7f, gy)
            close()
        }
        drawPath(gem, color)
        drawPath(gem, Color.White.copy(alpha = 0.7f), style = Stroke(width = size * 0.015f))
        PathPool.release(gem)
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
    val stemPath = PathPool.acquire().apply {
        moveTo(cx, centerY + offsetD * 0.50f)
        cubicTo(cx + size * 0.05f, cy + size * 0.30f,
            cx + size * 0.08f, cy + size * 0.40f,
            cx + size * 0.10f, cy + size * 0.45f)
    }
    drawPath(stemPath, color, style = Stroke(width = size * 0.05f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round))
    PathPool.release(stemPath)
}

/** Phase diamond — diamond outline with ghost stutter (double offset). */
private fun DrawScope.drawPhaseDiamondShape(cx: Float, cy: Float, size: Float, color: Color) {
    val halfW = size * 0.35f
    val halfH = size * 0.42f
    fun makeDiamond(ox: Float, oy: Float, alpha: Float) {
        val p = PathPool.acquire().apply {
            moveTo(cx + ox, cy + oy - halfH)
            lineTo(cx + ox + halfW, cy + oy)
            lineTo(cx + ox, cy + oy + halfH)
            lineTo(cx + ox - halfW, cy + oy)
            close()
        }
        drawPath(p, color.copy(alpha = alpha), style = Stroke(width = size * 0.06f))
        PathPool.release(p)
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
        val path = PathPool.acquire().apply {
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
        PathPool.release(path)
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
    val spinePath = PathPool.acquire().apply {
        moveTo(bladeStartX, bladeStartY)
        cubicTo(bladeStartX + size * 0.20f, bladeStartY - size * 0.15f,
            bladeEndX - size * 0.20f, bladeEndY + size * 0.05f,
            bladeEndX, bladeEndY)
    }
    // Edge path (parallel curve below spine)
    val edgePath = PathPool.acquire().apply {
        moveTo(bladeStartX + size * 0.05f, bladeStartY + size * 0.08f)
        cubicTo(bladeStartX + size * 0.25f, bladeStartY - size * 0.05f,
            bladeEndX - size * 0.15f, bladeEndY + size * 0.13f,
            bladeEndX + size * 0.08f, bladeEndY + size * 0.05f)
    }
    // Blade fill (filled region between spine and edge)
    val bladePath = PathPool.acquire().apply {
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
    PathPool.release(bladePath)
    PathPool.release(spinePath)
    PathPool.release(edgePath)
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
    val arr = PathPool.acquire().apply {
        moveTo(ax + size * 0.10f, ay - size * 0.05f)
        lineTo(ax + size * 0.18f, ay + size * 0.08f)
        lineTo(ax - size * 0.05f, ay + size * 0.10f)
        close()
    }
    drawPath(arr, color)
    PathPool.release(arr)
    drawCircle(Color.White.copy(alpha = 0.7f), size * 0.06f, Offset(cx, cy))
}

/** Giant Boulder — rocky asteroid silhouette với craters (replaces simple disc). */
private fun DrawScope.drawBigDotShape(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.42f
    // Irregular rock outline (8-vertex with noise)
    val noise = floatArrayOf(1.0f, 0.92f, 1.05f, 0.88f, 1.0f, 0.95f, 1.02f, 0.9f)
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
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
    val path = PathPool.acquire().apply {
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
    PathPool.release(path)
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
    val heartTri = PathPool.acquire().apply {
        moveTo(cx - heartLobeR * 1.85f, cy + bottleH * 0.08f)
        lineTo(cx + heartLobeR * 1.85f, cy + bottleH * 0.08f)
        lineTo(cx, cy + bottleH * 0.20f)
        close()
    }
    drawPath(heartTri, color)
    PathPool.release(heartTri)
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

/**
 * Task 01 (Slice 4) — biểu tượng DRONE_BOOSTER: hình thoi (thân drone) + vòng
 * quỹ đạo mảnh quanh nó, khớp motif drone companion trong [DroneCanvas].
 */
private fun DrawScope.drawDroneShape(cx: Float, cy: Float, size: Float, color: Color) {
    val r = size * 0.32f
    // Vòng quỹ đạo.
    drawCircle(
        color = color.copy(alpha = 0.55f),
        radius = size * 0.46f,
        center = Offset(cx, cy),
        style = Stroke(width = size * 0.05f),
    )
    // Thân drone: hình thoi.
    val body = PathPool.acquire().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r, cy)
        lineTo(cx, cy + r)
        lineTo(cx - r, cy)
        close()
    }
    drawPath(body, color)
    PathPool.release(body)
    // Lõi sáng.
    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = size * 0.09f, center = Offset(cx, cy))
}
