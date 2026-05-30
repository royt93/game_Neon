package com.tranphuloi.neon.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * Pixel-3 #3 — shared starfield background. Extracted from MenuScreen so
 * Stats + Info screens get the same parallax decorative starfield without
 * re-implementing.
 *
 * 60 stars in 3 layers. Drift downward at layer-scaled speed via an infinite
 * transition. Each star also twinkles via sin(phase + time). Pure decorative,
 * doesn't consume pointer events.
 *
 * Layers:
 *  - far  (layer=0): cyan tint, size 0.8–2.0px, alpha 0.4, drift 20 px/sec
 *  - mid  (layer=1): white, size 1.4–3.2px, alpha 0.6, drift 40 px/sec
 *  - near (layer=2): gold tint, size 2.0–4.4px, alpha 0.85, drift 80 px/sec
 *
 * Caller wraps with `.fillMaxSize()` modifier + a gradient background Box
 * underneath. See [com.tranphuloi.neon.ui.stats.StatsScreen] /
 * [com.tranphuloi.neon.ui.info.InfoScreen] for the integration pattern.
 */
private data class NeonStar(
    val xFrac: Float,
    val baseY: Float,
    val sizePx: Float,
    val baseAlpha: Float,
    val twinklePhase: Float,
    val layer: Int,
)

@Composable
fun NeonStarfieldBackground(modifier: Modifier = Modifier) {
    val stars = remember {
        val rng = Random(424242L)
        List(60) {
            val layer = it % 3
            NeonStar(
                xFrac = rng.nextFloat(),
                baseY = rng.nextFloat() * 2000f,
                sizePx = when (layer) {
                    0 -> rng.nextFloat() * 1.2f + 0.8f
                    1 -> rng.nextFloat() * 1.8f + 1.4f
                    else -> rng.nextFloat() * 2.4f + 2.0f
                },
                baseAlpha = when (layer) {
                    0 -> 0.4f
                    1 -> 0.6f
                    else -> 0.85f
                },
                twinklePhase = rng.nextFloat() * 6.283f,
                layer = layer,
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "starfield")
    val timeMs by transition.animateFloat(
        initialValue = 0f,
        targetValue = 60000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
        ),
        label = "starTime",
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        stars.forEach { star ->
            val speed = when (star.layer) {
                0 -> 20f
                1 -> 40f
                else -> 80f
            }
            val cycleSec = (h + 100f) / speed
            val phase = (timeMs / 1000f) % cycleSec
            val y = (star.baseY + phase * speed) % (h + 100f) - 50f
            val twinkle = kotlin.math.sin(timeMs / 600f + star.twinklePhase)
            val alpha = (star.baseAlpha * (0.6f + 0.4f * twinkle)).coerceIn(0f, 1f)
            val color = when (star.layer) {
                0 -> NeonCyan.copy(alpha = alpha * 0.5f)
                1 -> Color.White.copy(alpha = alpha)
                else -> NeonGold.copy(alpha = alpha)
            }
            drawCircle(
                color = color,
                radius = star.sizePx,
                center = Offset(star.xFrac * w, y),
            )
        }
    }
}
