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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.delay

/**
 * 14c: Big center banner shown when ship is revived via auto-revive token.
 * 1.6s total: scale-up pop-in (0..0.18s) + hold w/ pulse (0.18..1.2s) + fade-out (1.2..1.6s).
 * Behind text: radial gold-cyan halo expanding outward.
 */
@Composable
fun RevivedBanner(
    shownAtMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (shownAtMillis == 0L) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shownAtMillis) {
        Logger.d("RevivedBanner shown @ $shownAtMillis")
        repeat(50) {                                                       // ~1.65s @ 33ms
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val elapsed = (nowMillis - shownAtMillis).coerceAtLeast(0L)
    if (elapsed > 1600L) return

    val t = elapsed.toFloat() / 1600f                                      // 0..1
    val popScale = when {
        t < 0.15f -> {
            val tt = t / 0.15f
            // back-out overshoot to ~1.15 then settle
            val u = tt - 1f
            1f + 2.7f * u * u * u + 1.7f * u * u
        }
        t < 0.75f -> 1f + 0.05f * kotlin.math.sin((t - 0.15f) * 18f)        // pulse hold
        else -> (1f - (t - 0.75f) / 0.25f).coerceAtLeast(0f) * 1f          // shrink-out
    }
    val alpha = when {
        t < 0.15f -> (t / 0.15f).coerceAtMost(1f)
        t < 0.75f -> 1f
        else -> (1f - (t - 0.75f) / 0.25f).coerceAtLeast(0f)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Halo backdrop — gold→cyan→transparent radial, scales with pop.
        Canvas(modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val haloRadius = size.minDimension * 0.45f * (0.6f + 0.4f * popScale)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonGold.copy(alpha = 0.45f),
                        NeonCyan.copy(alpha = 0.20f),
                        Color.Transparent,
                    ),
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    radius = haloRadius,
                ),
                radius = haloRadius,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )
        }
        // Three-layer Text stack — outer cyan glow, mid gold, inner white.
        Text(
            text = stringResource(id = R.string.revived_banner),
            color = NeonCyan.copy(alpha = 0.65f * alpha),
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer {
                scaleX = popScale * 1.06f
                scaleY = popScale * 1.06f
            },
        )
        Text(
            text = stringResource(id = R.string.revived_banner),
            color = NeonGold.copy(alpha = 0.95f * alpha),
            fontSize = 60.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer {
                scaleX = popScale * 1.02f
                scaleY = popScale * 1.02f
            },
        )
        Text(
            text = stringResource(id = R.string.revived_banner),
            color = Color.White.copy(alpha = alpha),
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.graphicsLayer {
                scaleX = popScale
                scaleY = popScale
            },
        )
    }
}
