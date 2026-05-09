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
import kotlin.math.sin

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

        // Flicker length factor 0.65..1.35 (deeper flicker for organic feel)
        val lenFactor = 1f + 0.35f * sin(flicker)
        // Longer, 3-layer cone per "dài hơn, tự nhiên hơn" feedback.
        // Outer cyan halo / mid gold / bright white core — staggered lengths for taper.
        val outerLen = 50f * density * lenFactor
        val midLen = 35f * density * lenFactor
        val coreLen = 22f * density * lenFactor
        val outerHalfWidth = 18f * density
        val midHalfWidth = 11f * density
        val coreHalfWidth = 5.5f * density

        val totalRotation = ship.bankRotation + ship.spawnRotation
        rotate(degrees = totalRotation, pivot = androidx.compose.ui.geometry.Offset(cxPx, topY)) {
            // Layer 1: outer cyan halo (largest, softest).
            val outerPath = Path().apply {
                moveTo(cxPx - outerHalfWidth, topY)
                lineTo(cxPx + outerHalfWidth, topY)
                lineTo(cxPx, topY + outerLen)
                close()
            }
            drawPath(
                path = outerPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.5f),
                        NeonCyan.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    startY = topY,
                    endY = topY + outerLen,
                ),
            )

            // Layer 2: mid gold cone — main flame body.
            val midPath = Path().apply {
                moveTo(cxPx - midHalfWidth, topY)
                lineTo(cxPx + midHalfWidth, topY)
                lineTo(cxPx, topY + midLen)
                close()
            }
            drawPath(
                path = midPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NeonGold.copy(alpha = 0.85f),
                        NeonGold.copy(alpha = 0.4f),
                        Color.Transparent,
                    ),
                    startY = topY,
                    endY = topY + midLen,
                ),
            )

            // Layer 3: bright white core — hottest center, sharpest.
            val corePath = Path().apply {
                moveTo(cxPx - coreHalfWidth, topY)
                lineTo(cxPx + coreHalfWidth, topY)
                lineTo(cxPx, topY + coreLen)
                close()
            }
            drawPath(
                path = corePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.4f),
                        Color.Transparent,
                    ),
                    startY = topY,
                    endY = topY + coreLen,
                ),
            )
        }
    }
}

/** Compose helper: full-screen modifier suitable for ShipEngineFlame's Canvas. */
fun engineFlameModifier(): Modifier = Modifier
