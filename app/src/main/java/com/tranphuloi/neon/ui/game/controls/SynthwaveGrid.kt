package com.tranphuloi.neon.ui.game.controls

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
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonMagenta

/**
 * Animated synthwave perspective grid behind the game world (1c cinematic).
 * Horizontal lines drift downward to create motion; vertical lines fan from horizon.
 */
@Composable
fun SynthwaveGrid(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "synthwaveGrid")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
        ),
        label = "synthwaveDrift"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.55f
        val rowSpacing = h * 0.07f
        val rows = 16
        val color = NeonMagenta.copy(alpha = 0.18f)
        val centerX = w / 2f

        // Horizontal lines drifting downward in perspective.
        for (i in 0 until rows) {
            val t = ((i.toFloat() + drift) / rows).coerceIn(0f, 1f)
            val y = horizonY + (h - horizonY) * t * t
            val alpha = (1f - t) * 0.55f
            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.5f
            )
        }

        // Vertical lines fanning out from horizon.
        val cyan = NeonCyan.copy(alpha = 0.22f)
        val cols = 14
        for (i in 0..cols) {
            val tCol = (i.toFloat() / cols)
            val xBottom = w * tCol
            drawLine(
                color = cyan,
                start = Offset(centerX, horizonY),
                end = Offset(xBottom, h),
                strokeWidth = 1f
            )
        }
    }
}
