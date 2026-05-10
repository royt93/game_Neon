package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.delay

/**
 * 34d Wave 4 — cinematic banner for FinalBoss phase 2/3 entry.
 *   Triggers on phaseTransitionMillis change (FinalBoss only).
 *   Duration 1.4s: pop-in (0..0.18) → hold pulse (0.18..0.7) → fade-out (0.7..1.0).
 *   Renders "PHASE N" w/ red halo + 3-layer text stack.
 */
@Composable
fun PhaseTransitionBanner(
    phase: Int,
    phaseTransitionMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (phaseTransitionMillis == 0L || phase < 2) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(phaseTransitionMillis) {
        Logger.d("PhaseTransitionBanner shown: phase=$phase @ $phaseTransitionMillis")
        repeat(45) {
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val elapsed = (nowMillis - phaseTransitionMillis).coerceAtLeast(0L)
    if (elapsed > 1400L) return

    val t = elapsed.toFloat() / 1400f
    val popScale = when {
        t < 0.18f -> {
            val u = t / 0.18f - 1f
            1f + 2.7f * u * u * u + 1.7f * u * u                          // back-out overshoot
        }
        t < 0.7f -> 1f + 0.04f * kotlin.math.sin((t - 0.18f) * 18f)        // gentle pulse
        else -> (1f - (t - 0.7f) / 0.3f).coerceAtLeast(0f)
    }
    val alpha = when {
        t < 0.18f -> (t / 0.18f).coerceAtMost(1f)
        t < 0.7f -> 1f
        else -> (1f - (t - 0.7f) / 0.3f).coerceAtLeast(0f)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha },
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension * 0.35f * (0.6f + 0.4f * popScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonRedAlert.copy(alpha = 0.55f),
                        NeonGold.copy(alpha = 0.25f),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = r,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
        }
        Text(
            text = "PHASE $phase",
            color = NeonRedAlert.copy(alpha = 0.7f * alpha),
            fontSize = 60.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer {
                scaleX = popScale * 1.06f
                scaleY = popScale * 1.06f
            },
        )
        Text(
            text = "PHASE $phase",
            color = NeonGold.copy(alpha = 0.95f * alpha),
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer {
                scaleX = popScale * 1.02f
                scaleY = popScale * 1.02f
            },
        )
        Text(
            text = "PHASE $phase",
            color = Color.White.copy(alpha = alpha),
            fontSize = 52.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer {
                scaleX = popScale
                scaleY = popScale
            },
        )
    }
}
