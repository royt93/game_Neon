package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.PathPool
import com.tranphuloi.neon.common.drawSoftHalo
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import com.tranphuloi.neon.ui.game.ship.laser.LaserUI

/**
 * Round 66 — Pure-vector laser rendering (pilot for the "no PNG textures"
 * direction). Replaces the round 49 `drawImage(ImageBitmap)` path with three
 * stacked draw primitives per laser:
 *   1. Radial gradient halo (unchanged from round 49 — drives the neon glow).
 *   2. Outer body: drawRoundRect tinted to `glow` color, full alpha.
 *   3. Inner core: drawRoundRect white at alpha 0.85, half the width — gives
 *      the "hot center" look characteristic of neon beams.
 *
 * Why pilot here: lasers are the simplest entities in the game (vertical bars
 * with glow). Migration from bitmap → vector is essentially lossless
 * aesthetically (the original sprites were just colored stripes too) AND
 * eliminates 5 webp drawables from the APK. If user approves the look, the
 * same pattern can expand to boosters/space-rocks/enemies/ship in subsequent
 * rounds (66b/c/d).
 *
 * API change vs round 49: the `LaserSprites` parameter is gone. Callers no
 * longer need `rememberLaserSprites()` either. GameWorld.kt updated in the
 * same commit. The `drawableId` field on `LaserUI` is now unused for
 * rendering (kept for backward compat with persistence + mapper tests).
 */
@Composable
fun LaserCanvas(
    lasers: List<LaserUI>,
    glow: Color,
    intensity: Float,
    radiusFactor: Float,
    modifier: Modifier = Modifier,
) {
    if (lasers.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (laser in lasers) {
            drawLaser(laser, glow, intensity, radiusFactor, density)
        }
    }
}

/**
 * Round 66 — kept as a type alias for backward compatibility. Future cleanup
 * round can remove this + [rememberLaserSprites] once no callers reference
 * them. Marked @Deprecated to surface usage sites.
 */
@Immutable
@Deprecated("Round 66 — pure-vector LaserCanvas no longer needs sprites. Remove call site.")
data class LaserSprites(val byDrawableId: Map<Int, Any> = emptyMap())

@Composable
@Deprecated("Round 66 — pure-vector LaserCanvas no longer needs sprites. Remove call site.")
fun rememberLaserSprites(): LaserSprites = LaserSprites()

private fun DrawScope.drawLaser(
    laser: LaserUI,
    glow: Color,
    intensity: Float,
    radiusFactor: Float,
    density: Density,
) {
    with(density) {
        val xPx = laser.xOffset.dp.toPx()
        val yPx = laser.yOffset.dp.toPx()
        val wPx = laser.width.dp.toPx()
        val hPx = laser.height.dp.toPx()
        val cx = xPx + wPx / 2f
        val cy = yPx + hPx / 2f
        val glowR = (minOf(wPx, hPx) / 2f) * radiusFactor

        // Round 78 (#6 perf) — was Brush.radialGradient per-laser per-frame.
        // Up to 30 ship + 30 enemy + ultimate lasers = ~80 brush allocs/frame.
        // Replaced with drawSoftHalo (3 drawCircles, no Brush/Shader allocation).
        drawSoftHalo(glow, intensity, glowR, Offset(cx, cy))

        // Round 71 (Issue 4a) — dispatch per BulletType. Each laser has unique
        // vector silhouette in-game matching InfoScreen Bullets tab.
        val drawBody: DrawScope.() -> Unit = {
            drawLaserBody(laser.bulletType, xPx, yPx, wPx, hPx, glow)
        }
        if (laser.rotation != 0f) {
            rotate(degrees = laser.rotation, pivot = Offset(cx, cy)) { drawBody() }
        } else {
            drawBody()
        }
    }
}

/** Round 71 (Issue 4a) — dispatch table for in-game laser shape. */
private fun DrawScope.drawLaserBody(
    bulletType: BulletType,
    xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color,
) {
    val bodyCorner = (wPx / 2f).coerceAtMost(hPx / 2f)
    when (bulletType) {
        BulletType.NORMAL -> drawCapsuleBody(xPx, yPx, wPx, hPx, bodyCorner, glow)
        BulletType.PIERCING -> drawNeedleBody(xPx, yPx, wPx, hPx, glow)
        BulletType.PLASMA -> drawOrbBody(xPx, yPx, wPx, hPx, glow)
        BulletType.FIRE -> drawFireBody(xPx, yPx, wPx, hPx, glow)
        BulletType.HOMING -> drawHomingBody(xPx, yPx, wPx, hPx, glow)
        BulletType.BOUNCE -> drawBounceBody(xPx, yPx, wPx, hPx, glow)
        BulletType.GIANT -> drawGiantBody(xPx, yPx, wPx, hPx, bodyCorner, glow)
        BulletType.SMOKE -> drawSmokeBody(xPx, yPx, wPx, hPx, glow)
        BulletType.ZIGZAG -> drawZigzagBody(xPx, yPx, wPx, hPx, glow)
        BulletType.KAMEHAMEHA -> drawBeamBody(xPx, yPx, wPx, hPx, glow)
        BulletType.ATOMIC -> drawAtomicBody(xPx, yPx, wPx, hPx, glow)
        BulletType.SPLIT -> drawSplitBody(xPx, yPx, wPx, hPx, glow)
    }
}

private fun DrawScope.drawBounceBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f
    val cy = yPx + hPx / 2f
    val r = maxOf(wPx, hPx) * 0.45f
    // Ball + 2 motion arcs behind to indicate ricochet trail.
    val trailR1 = r * 0.7f
    val trailR2 = r * 0.45f
    drawCircle(glow.copy(alpha = 0.35f), trailR1,
        Offset(cx - wPx * 0.4f, cy + hPx * 0.25f))
    drawCircle(glow.copy(alpha = 0.5f), trailR2,
        Offset(cx + wPx * 0.4f, cy + hPx * 0.15f))
    // Main ball
    drawCircle(glow, r, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.4f, Offset(cx, cy))
}

private fun DrawScope.drawGiantBody(
    xPx: Float, yPx: Float, wPx: Float, hPx: Float, bodyCorner: Float, glow: Color,
) {
    // Mega-capsule with INNER segment line — distinguishes from NORMAL capsule
    // (which has just outer body + white core). GIANT looks "engineered" with
    // 3 segment dividers in core.
    drawCapsuleBody(xPx, yPx, wPx, hPx, bodyCorner, glow)
    // 2 horizontal segment dividers across the white core
    val coreW = wPx * 0.5f
    val coreX = xPx + (wPx - coreW) / 2f
    val divY1 = yPx + hPx * 0.35f
    val divY2 = yPx + hPx * 0.65f
    drawLine(glow, Offset(coreX, divY1), Offset(coreX + coreW, divY1),
        strokeWidth = wPx * 0.08f)
    drawLine(glow, Offset(coreX, divY2), Offset(coreX + coreW, divY2),
        strokeWidth = wPx * 0.08f)
}

private fun DrawScope.drawHomingBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val bodyCorner = (wPx / 2f).coerceAtMost(hPx / 2f)
    drawCapsuleBody(xPx, yPx, wPx, hPx, bodyCorner, glow)
    // Targeting ring around the bullet (signals lock-on)
    val cx = xPx + wPx / 2f
    val cy = yPx + hPx / 2f
    val ringR = maxOf(wPx, hPx) * 0.55f
    drawCircle(
        color = glow,
        radius = ringR,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = wPx * 0.35f),
    )
}

private fun DrawScope.drawNeedleBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f
    val path = PathPool.acquire().apply {
        moveTo(cx, yPx)                                  // pointed top
        lineTo(xPx + wPx, yPx + hPx)
        lineTo(xPx, yPx + hPx)
        close()
    }
    drawPath(path, glow)
    val corePath = PathPool.acquire().apply {
        moveTo(cx, yPx + hPx * 0.15f)
        lineTo(cx + wPx * 0.25f, yPx + hPx * 0.95f)
        lineTo(cx - wPx * 0.25f, yPx + hPx * 0.95f)
        close()
    }
    drawPath(corePath, Color.White.copy(alpha = 0.85f))
    PathPool.release(corePath)
}

private fun DrawScope.drawOrbBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f
    val cy = yPx + hPx / 2f
    val r = maxOf(wPx, hPx) * 0.5f
    drawCircle(glow, r, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.85f), r * 0.5f, Offset(cx, cy))
}

private fun DrawScope.drawFireBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    drawCapsuleBody(xPx, yPx, wPx, hPx * 0.75f, wPx / 2f, glow)
    // Flame trail at bottom
    val cx = xPx + wPx / 2f
    val path = PathPool.acquire().apply {
        moveTo(xPx, yPx + hPx * 0.75f)
        lineTo(cx, yPx + hPx)
        lineTo(xPx + wPx, yPx + hPx * 0.75f)
        close()
    }
    drawPath(path, Color(0xFFFFD040).copy(alpha = 0.9f))
    PathPool.release(path)
}

private fun DrawScope.drawSmokeBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    drawCapsuleBody(xPx, yPx, wPx, hPx * 0.7f, wPx / 2f, glow)
    // Cloud puff at bottom
    val cx = xPx + wPx / 2f
    drawCircle(glow.copy(alpha = 0.5f), wPx * 0.55f, Offset(cx, yPx + hPx * 0.85f))
}

private fun DrawScope.drawZigzagBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    // Use wider stroke + zigzag path. wPx scaled wider for visibility.
    val widerW = wPx * 1.8f
    val cx = xPx + wPx / 2f
    val path = PathPool.acquire().apply {
        val step = hPx / 4f
        moveTo(cx - widerW / 2, yPx)
        lineTo(cx + widerW / 2, yPx + step)
        lineTo(cx - widerW / 2, yPx + step * 2)
        lineTo(cx + widerW / 2, yPx + step * 3)
        lineTo(cx - widerW / 2, yPx + hPx)
    }
    drawPath(path, glow, style = androidx.compose.ui.graphics.drawscope.Stroke(
        width = wPx * 0.7f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round,
        join = androidx.compose.ui.graphics.StrokeJoin.Round,
    ))
    PathPool.release(path)
    PathPool.release(path)
}

private fun DrawScope.drawBeamBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    // Wide horizontal-style beam (use full available width as height for "beam" look)
    val beamW = wPx * 1.6f
    val cx = xPx + wPx / 2f
    val cy = yPx + hPx / 2f
    drawRoundRect(
        color = glow,
        topLeft = Offset(cx - beamW / 2, cy - hPx * 0.25f),
        size = Size(beamW, hPx * 0.5f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(hPx * 0.25f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.9f),
        topLeft = Offset(cx - beamW / 2 + beamW * 0.05f, cy - hPx * 0.12f),
        size = Size(beamW * 0.9f, hPx * 0.24f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(hPx * 0.12f),
    )
}

private fun DrawScope.drawAtomicBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    val cx = xPx + wPx / 2f
    val cy = yPx + hPx / 2f
    val r = maxOf(wPx, hPx) * 0.5f
    // Nucleus
    drawCircle(glow, r * 0.45f, Offset(cx, cy))
    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.2f, Offset(cx, cy))
    // 3 orbiting electrons
    for (i in 0 until 3) {
        val angle = i * 120.0 * Math.PI / 180.0
        val ex = cx + (r * 0.9f * kotlin.math.cos(angle)).toFloat()
        val ey = cy + (r * 0.9f * kotlin.math.sin(angle)).toFloat()
        drawCircle(glow.copy(alpha = 0.85f), r * 0.15f, Offset(ex, ey))
    }
}

private fun DrawScope.drawSplitBody(xPx: Float, yPx: Float, wPx: Float, hPx: Float, glow: Color) {
    drawCapsuleBody(xPx, yPx + hPx * 0.3f, wPx, hPx * 0.65f, wPx / 2f, glow)
    // 3 branches at top
    val cx = xPx + wPx / 2f
    val branchTop = yPx + hPx * 0.3f
    val branchLen = hPx * 0.35f
    for (i in -1..1) {
        val angle = i * 30.0 * Math.PI / 180.0
        val tipX = cx + (branchLen * kotlin.math.sin(angle)).toFloat()
        val tipY = branchTop - (branchLen * kotlin.math.cos(angle)).toFloat()
        drawLine(
            color = glow,
            start = Offset(cx, branchTop),
            end = Offset(tipX, tipY),
            strokeWidth = wPx * 0.5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

/**
 * Round 66 — the per-laser body draw. Extracted so the rotate() block can
 * reuse it cleanly. Two stacked rounded rectangles: outer tinted to `glow`,
 * inner white-hot core at ~50% the width with high alpha for the neon "bright
 * center" feel.
 */
private fun DrawScope.drawCapsuleBody(
    xPx: Float,
    yPx: Float,
    wPx: Float,
    hPx: Float,
    cornerRadius: Float,
    glow: Color,
) {
    // Outer body — full tinted color.
    drawRoundRect(
        color = glow,
        topLeft = Offset(xPx, yPx),
        size = Size(wPx, hPx),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
    )
    // Inner hot core — white, 50% width centered, slightly inset vertically
    // so the rounded ends stay tinted.
    val coreW = wPx * 0.5f
    val coreInsetY = hPx * 0.10f
    val coreH = (hPx - coreInsetY * 2f).coerceAtLeast(1f)
    val coreX = xPx + (wPx - coreW) / 2f
    val coreY = yPx + coreInsetY
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(coreX, coreY),
        size = Size(coreW, coreH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(coreW / 2f),
    )
}
