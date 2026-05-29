package com.tranphuloi.neon.ui.game.world

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import kotlin.math.cos
import kotlin.math.sin
import com.tranphuloi.neon.common.PathPool

/**
 * 3b: Ship engine flame trail — flickering cone of cyan/orange fire below the ship.
 *
 * Design:
 *   - Outer cone (large, cyan, low alpha)
 *   - Inner cone (smaller, gold, higher alpha)
 *   - Both flicker by oscillating length with sin(time)
 *   - Drawn beneath ship (so ship sprite covers the cone's top edge)
 */
@Composable
fun ShipEngineFlame(
    ship: Ship,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val transition = rememberInfiniteTransition(label = "shipFlame")
    val flicker by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 280, easing = LinearEasing),
        ),
        label = "shipFlameFlicker",
    )

    Canvas(modifier = modifier) {
        // Ship center bottom in pixels.
        val cxPx = (ship.xOffset + ship.width / 2f) * density
        // Place flame START just above ship's bottom edge (so it appears to come from engines).
        val topY = (ship.yOffset + ship.height * 0.85f) * density

        // Per-layer flicker phases — staggered so the 3 layers don't pulse in sync.
        val outerLenFactor = 1f + 0.35f * sin(flicker)
        val midLenFactor = 1f + 0.30f * sin(flicker + 1.7f)         // ~98° phase shift
        val coreLenFactor = 1f + 0.25f * sin(flicker * 1.4f + 0.8f) // faster + offset
        val outerLen = 100f * density * outerLenFactor
        val midLen = 70f * density * midLenFactor
        val coreLen = 45f * density * coreLenFactor

        // Lateral sway — the bulge drifts left/right with cosine of flicker so the
        // flame "bends" organically while flickering. Different per layer.
        val outerSway = cos(flicker * 0.7f) * 4f * density
        val midSway = cos(flicker * 0.7f + 0.5f) * 2.5f * density
        val coreSway = cos(flicker * 0.9f) * 1.2f * density

        // Teardrop widths — slight per-layer flicker on bulge size too.
        val outerBulgeFlicker = 1f + 0.15f * sin(flicker + 2.1f)
        val outerTopHalf = 4f * density
        val outerBulgeHalf = 18f * density * outerBulgeFlicker
        val midTopHalf = 2.5f * density
        val midBulgeHalf = 11f * density * outerBulgeFlicker
        val coreTopHalf = 1.5f * density
        val coreBulgeHalf = 5.5f * density

        val totalRotation = ship.bankRotation + ship.spawnRotation
        rotate(degrees = totalRotation, pivot = androidx.compose.ui.geometry.Offset(cxPx, topY)) {
            // Layer 1: outer cyan halo — teardrop with lateral sway.
            // Cubic bezier curves give smooth rounded sides (was sharp 5-point polygon
            // per user "thô, nhiều góc cạnh"). Two cubics: top-left → bulge-left →
            // tip → bulge-right → top-right. Each cubic uses two control points to
            // shape an S-curve through the bulge.
            val outerPath = PathPool.acquire().apply {
                moveTo(cxPx - outerTopHalf, topY)
                cubicTo(
                    cxPx - outerBulgeHalf - 2f, topY + outerLen * 0.18f,
                    cxPx - outerBulgeHalf + outerSway, topY + outerLen * 0.55f,
                    cxPx + outerSway * 0.5f, topY + outerLen,
                )
                cubicTo(
                    cxPx + outerBulgeHalf + outerSway, topY + outerLen * 0.55f,
                    cxPx + outerBulgeHalf + 2f, topY + outerLen * 0.18f,
                    cxPx + outerTopHalf, topY,
                )
                close()
            }
            drawPath(
                path = outerPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.5f),
                        NeonCyan.copy(alpha = 0.22f),
                        NeonCyan.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    startY = topY,
                    endY = topY + outerLen,
                ),
            )
            PathPool.release(outerPath)

            // Layer 2: mid gold cone — bezier teardrop.
            val midPath = PathPool.acquire().apply {
                moveTo(cxPx - midTopHalf, topY)
                cubicTo(
                    cxPx - midBulgeHalf - 1f, topY + midLen * 0.18f,
                    cxPx - midBulgeHalf + midSway, topY + midLen * 0.55f,
                    cxPx + midSway * 0.5f, topY + midLen,
                )
                cubicTo(
                    cxPx + midBulgeHalf + midSway, topY + midLen * 0.55f,
                    cxPx + midBulgeHalf + 1f, topY + midLen * 0.18f,
                    cxPx + midTopHalf, topY,
                )
                close()
            }
            drawPath(
                path = midPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NeonGold.copy(alpha = 0.85f),
                        NeonGold.copy(alpha = 0.45f),
                        NeonGold.copy(alpha = 0.15f),
                        Color.Transparent,
                    ),
                    startY = topY,
                    endY = topY + midLen,
                ),
            )
            PathPool.release(midPath)

            // Layer 3: bright white core — bezier teardrop.
            val corePath = PathPool.acquire().apply {
                moveTo(cxPx - coreTopHalf, topY)
                cubicTo(
                    cxPx - coreBulgeHalf, topY + coreLen * 0.18f,
                    cxPx - coreBulgeHalf + coreSway, topY + coreLen * 0.55f,
                    cxPx + coreSway * 0.5f, topY + coreLen,
                )
                cubicTo(
                    cxPx + coreBulgeHalf + coreSway, topY + coreLen * 0.55f,
                    cxPx + coreBulgeHalf, topY + coreLen * 0.18f,
                    cxPx + coreTopHalf, topY,
                )
                close()
            }
            drawPath(
                path = corePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.45f),
                        Color.Transparent,
                    ),
                    startY = topY,
                    endY = topY + coreLen,
                ),
            )
            PathPool.release(corePath)

            // Ember trail — 6 fading sparkle dots extending past the cone tip,
            // mimicking a shooting-star/meteor tail. Sizes shrink, alpha fades.
            val coneTipY = topY + outerLen
            val emberSpacing = 12f * density
            for (i in 0 until 6) {
                val ey = coneTipY + i * emberSpacing
                val emberAlpha = (1f - i * 0.16f) * 0.55f * outerLenFactor
                val emberRadius = (3f - i * 0.4f) * density
                if (emberAlpha > 0f && emberRadius > 0f) {
                    drawCircle(
                        color = NeonGold.copy(alpha = emberAlpha * 0.5f),
                        radius = emberRadius * 1.8f,
                        center = androidx.compose.ui.geometry.Offset(cxPx, ey),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = emberAlpha),
                        radius = emberRadius,
                        center = androidx.compose.ui.geometry.Offset(cxPx, ey),
                    )
                }
            }
        }
    }
}

/** Compose helper: full-screen modifier suitable for ShipEngineFlame's Canvas. */
fun engineFlameModifier(): Modifier = Modifier
