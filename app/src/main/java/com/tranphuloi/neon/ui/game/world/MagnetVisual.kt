package com.tranphuloi.neon.ui.game.world

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.ui.game.mineral.model.MineralUI
import com.tranphuloi.neon.ui.game.ship.ship.Ship

/**
 * 16c: Magnet visual layer — pulse circle around ship + connection lines from
 * minerals being attracted to ship.
 *
 *  - Ring uses pulsing alpha so it doesn't compete with HUD attention.
 *  - Lines drawn only for minerals within magnet radius (i.e. actually being pulled).
 *  - Single Canvas — no per-mineral Composable.
 */
@Composable
fun MagnetVisual(
    ship: Ship,
    minerals: List<MineralUI>,
    magnetRadius: Float,
    modifier: Modifier = Modifier,
) {
    if (magnetRadius <= 0f) return
    val density = LocalDensity.current.density
    val pulse = rememberInfiniteTransition(label = "magnetPulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "magnetPulseAlpha",
    )

    Canvas(modifier = modifier) {
        // All inputs are in dp-space → convert to px for Canvas drawScope.
        val shipCx = (ship.xOffset + ship.width / 2f) * density
        val shipCy = (ship.yOffset + ship.height / 2f) * density

        // Per user feedback: pulsing ring around ship was confusing — keep ONLY the
        // connection lines from minerals being attracted to ship. Lines pulse with
        // [pulseAlpha] so the magnet is still visually communicated.
        val rSq = magnetRadius * magnetRadius
        minerals.forEach { m ->
            val mx = (m.xOffset + m.width / 2f) * density
            val my = (m.yOffset + 12f) * density       // mineral icon height ~25dp, center ~12
            val dx = (m.xOffset + m.width / 2f) - (ship.xOffset + ship.width / 2f)
            val dy = (m.yOffset + 12f) - (ship.yOffset + ship.height / 2f)
            if (dx * dx + dy * dy <= rSq) {
                drawLine(
                    color = NeonGold.copy(alpha = m.alpha * 0.5f * (0.6f + pulseAlpha)),
                    start = Offset(shipCx, shipCy),
                    end = Offset(mx, my),
                    strokeWidth = 1f * density,
                )
            }
        }
    }
}
