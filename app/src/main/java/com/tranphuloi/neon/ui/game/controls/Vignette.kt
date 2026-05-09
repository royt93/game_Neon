package com.tranphuloi.neon.ui.game.controls

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.tranphuloi.neon.common.NeonRedAlert

/**
 * Edge-darkening vignette + low-HP red pulse warning (1c cinematic).
 * - Always-on subtle dark vignette at edges to focus eye on center.
 * - When [hp] < 300, red pulses around edge → "screen edge red pulse" effect.
 */
@Composable
fun Vignette(
    hp: Int,
    modifier: Modifier = Modifier,
) {
    val lowHp = hp in 1..299
    val transition = rememberInfiniteTransition(label = "vignettePulse")
    // Softened — was 0.25..0.8 (too intense), now 0.10..0.30. Slower pulse (1100ms vs 700ms).
    val pulse by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "vignettePulseAlpha"
    )

    Canvas(modifier = modifier) {
        val baseRadius = size.minDimension * 0.55f
        // Static dark vignette.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.45f),
                ),
                center = Offset(size.width / 2, size.height / 2),
                radius = baseRadius * 1.6f,
            )
        )
        // Low-HP red pulse.
        if (lowHp) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        NeonRedAlert.copy(alpha = pulse),
                    ),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = baseRadius * 1.4f,
                )
            )
        }
    }
}
