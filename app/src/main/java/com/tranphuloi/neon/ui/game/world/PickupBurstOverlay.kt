package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.ui.game.spark.PickupBurst
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders an expanding ring shockwave + 8 radial sparkles per active
 * [PickupBurst]. Single Canvas over fillMaxSize.
 */
@Composable
fun PickupBurstOverlay(bursts: List<PickupBurst>) {
    if (bursts.isEmpty()) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasBursts = bursts.isNotEmpty()
    LaunchedEffect(hasBursts) {
        if (!hasBursts) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }

    val density = LocalDensity.current.density
    Canvas(modifier = Modifier.fillMaxSize()) {
        bursts.forEach { burst ->
            val t = burst.progress(nowMillis)
            if (t >= 1f) return@forEach
            val cx = burst.xOffset * density
            val cy = burst.yOffset * density
            val alphaFade = 1f - t

            // 1) Expanding ring: radius eases-out from 0 to RING_MAX_RADIUS,
            //    stroke width thins, alpha fades.
            val ringT = 1f - (1f - t) * (1f - t)
            val ringRadius = PickupBurst.RING_MAX_RADIUS * density * ringT
            drawCircle(
                color = NeonGold.copy(alpha = 0.7f * alphaFade),
                radius = ringRadius,
                center = Offset(cx, cy),
                style = Stroke(width = (3.dp.toPx()) * (1f - t * 0.5f)),
            )

            // 2) 8 radial sparkles: each travels outward distance T*SPARKLE_TRAVEL.
            val travel = PickupBurst.SPARKLE_TRAVEL * density * ringT
            for (i in 0 until PickupBurst.SPARKLE_COUNT) {
                val angle = (i.toFloat() / PickupBurst.SPARKLE_COUNT) * 2f * PI.toFloat()
                val sx = cx + cos(angle) * travel
                val sy = cy + sin(angle) * travel
                drawCircle(
                    color = Color.White.copy(alpha = alphaFade),
                    radius = 1.6.dp.toPx(),
                    center = Offset(sx, sy),
                )
                drawCircle(
                    color = NeonGold.copy(alpha = 0.55f * alphaFade),
                    radius = 3.dp.toPx(),
                    center = Offset(sx, sy),
                )
            }
        }
    }
}
