package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.ui.game.explosion.model.Explosion
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 13c: Per-explosion overlay rendering radial burst lines + ring shockwave.
 * Lifecycle: ticks at 30fps for the explosion's 450ms lifespan, auto-stops via remember(key).
 *
 * Burst: 10 lines radiating from center, length grows 0 → max, alpha fades 1 → 0.
 * Ring: circle expanding 0 → 100px (in dp), stroke width tapers, alpha fades.
 */
@Composable
fun ExplosionBurstOverlay(explosion: Explosion) {
    val density = LocalDensity.current.density
    var nowMillis by remember(explosion.startTimeMillis) {
        mutableLongStateOf(System.currentTimeMillis())
    }
    LaunchedEffect(explosion.startTimeMillis) {
        // Tick at 30fps for the explosion's lifespan only.
        repeat(15) {                                                    // 15 × 33ms ≈ 450ms
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val t = explosion.progress(nowMillis)
    if (t >= 1f) return

    // Center the overlay over the GIF. The Image is at (xOffset, yOffset) sized [size].
    val sizeDp = (explosion.size + 60f).dp                              // extra room for shockwave ring
    val offsetX = (explosion.xOffset - 30f).dp
    val offsetY = (explosion.yOffset - 30f).dp

    Canvas(
        modifier = Modifier
            .offset(offsetX, offsetY)
            .size(sizeDp),
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Burst lines: 10 spokes, length scales with t, alpha fades inverse t.
        val burstAlpha = (1f - t).coerceAtLeast(0f)
        val maxLen = (explosion.size * 0.9f) * density
        val burstLen = maxLen * (0.3f + t * 0.7f)                       // grows out
        val innerLen = burstLen * 0.4f                                  // hollow start
        val burstColor = NeonGold.copy(alpha = burstAlpha * 0.8f)
        val spokes = 10
        for (i in 0 until spokes) {
            val angle = (i.toFloat() / spokes) * 2f * PI.toFloat()
            val sx = cx + cos(angle) * innerLen
            val sy = cy + sin(angle) * innerLen
            val ex = cx + cos(angle) * burstLen
            val ey = cy + sin(angle) * burstLen
            drawLine(
                color = burstColor,
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = (3f - t * 2f) * density,
            )
        }

        // Ring shockwave: 0 → 100dp, stroke tapers, alpha fades.
        val ringR = (t * 100f) * density
        val ringAlpha = ((1f - t) * 0.8f).coerceAtLeast(0f)
        if (ringR > 0f) {
            drawCircle(
                color = NeonCyan.copy(alpha = ringAlpha),
                radius = ringR,
                center = Offset(cx, cy),
                style = Stroke(width = (4f - t * 3f) * density),
            )
        }
    }
}
