package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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

        // 1. Glow halo (radial gradient, opaque center → transparent edge).
        //    Same recipe as round 49 to preserve the neon ambient.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glow.copy(alpha = intensity),
                    glow.copy(alpha = intensity * 0.4f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(cx, cy),
        )

        // 2-3. Body + core. Use cornerRadius = width/2 → pill/capsule shape
        // for vertical beams. PLASMA (square ~aspect 1:1) becomes nearly
        // circular which fits its "energy blob" character.
        val bodyCorner = (wPx / 2f).coerceAtMost(hPx / 2f)
        if (laser.rotation != 0f) {
            rotate(degrees = laser.rotation, pivot = Offset(cx, cy)) {
                drawCapsuleBody(xPx, yPx, wPx, hPx, bodyCorner, glow)
            }
        } else {
            drawCapsuleBody(xPx, yPx, wPx, hPx, bodyCorner, glow)
        }
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
