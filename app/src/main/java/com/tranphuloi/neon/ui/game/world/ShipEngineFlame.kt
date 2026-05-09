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

        // Flicker length factor 0.7..1.3
        val lenFactor = 1f + 0.3f * sin(flicker)
        val outerLen = 36f * density * lenFactor
        val innerLen = 22f * density * lenFactor
        val outerHalfWidth = 18f * density
        val innerHalfWidth = 10f * density

        // Outer cone (cyan).
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
                    NeonCyan.copy(alpha = 0.55f),
                    NeonCyan.copy(alpha = 0.0f),
                ),
                startY = topY,
                endY = topY + outerLen,
            ),
        )

        // Inner cone (gold) — hotter core.
        val innerPath = Path().apply {
            moveTo(cxPx - innerHalfWidth, topY)
            lineTo(cxPx + innerHalfWidth, topY)
            lineTo(cxPx, topY + innerLen)
            close()
        }
        drawPath(
            path = innerPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    NeonGold.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.35f),
                    Color.Transparent,
                ),
                startY = topY,
                endY = topY + innerLen,
            ),
        )
    }
}

/** Compose helper: full-screen modifier suitable for ShipEngineFlame's Canvas. */
fun engineFlameModifier(): Modifier = Modifier
