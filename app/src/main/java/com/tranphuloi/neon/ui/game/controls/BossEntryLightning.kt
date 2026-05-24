package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI
import kotlinx.coroutines.delay

/**
 * Boss entry lightning crackle: draws 5 zigzag neon bolts from random screen-edge
 * positions to the boss center while the boss is descending from off-screen.
 *
 * Bolts are deterministically randomized off `seed` (updated ~12fps for a flickering
 * crackle), each rendered as outer glow + bright inner core (Compose has no real
 * blur, so the glow is approximated with a wider transparent stroke).
 */
@Composable
fun BossEntryLightning(
    boss: EnemyUI,
    /**
     * Round 79 audit fix (gap 6) — FAR zoom margin in dp. graphicsLayer scale
     * shrinks GameWorld to inner X% of screen; allowing bolt origins to extend
     * into negative game coords makes them reach the visual screen edge at any
     * zoom. Default 0 = no extension (NEAR zoom or unset).
     */
    spawnXMargin: Float = 0f,
    modifier: Modifier = Modifier,
) {
    if (!boss.isInEntryPhase) return

    var seed by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(boss.enemyId) {
        while (true) {
            seed = System.currentTimeMillis()
            delay(80L)
        }
    }

    val bossCxDp = boss.xOffset + boss.width / 2f
    val bossCyDp = boss.yOffset + boss.height / 2f

    Canvas(modifier = modifier.fillMaxSize()) {
        val random = kotlin.random.Random(seed)
        val cx = bossCxDp.dp.toPx()
        val cy = bossCyDp.dp.toPx()
        val marginPx = spawnXMargin.dp.toPx()
        // Round 79 fix — extended visual bounds at FAR zoom.
        val visualLeft = -marginPx
        val visualRight = size.width + marginPx
        val visualWidth = visualRight - visualLeft

        repeat(5) {
            // Random origin: 0=top edge, 1=left edge, 2=right edge.
            val edge = random.nextInt(3)
            val (sx, sy) = when (edge) {
                0 -> visualLeft + random.nextFloat() * visualWidth to -marginPx
                1 -> visualLeft to random.nextFloat() * cy
                else -> visualRight to random.nextFloat() * cy
            }

            // 5-segment zigzag: linearly interpolate from origin to boss with
            // perpendicular jitter at each waypoint.
            val path = Path().apply {
                moveTo(sx, sy)
                val segments = 6
                val jitterRange = 28f.dp.toPx()
                for (i in 1 until segments) {
                    val t = i.toFloat() / segments
                    val baseX = sx + (cx - sx) * t
                    val baseY = sy + (cy - sy) * t
                    val jx = baseX + (random.nextFloat() - 0.5f) * jitterRange
                    val jy = baseY + (random.nextFloat() - 0.5f) * jitterRange
                    lineTo(jx, jy)
                }
                lineTo(cx, cy)
            }

            val coreAlpha = 0.7f + random.nextFloat() * 0.3f
            // Outer cyan glow.
            drawPath(
                path = path,
                color = NeonCyan.copy(alpha = coreAlpha * 0.45f),
                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
            )
            // Mid layer: red alert tint for menace.
            drawPath(
                path = path,
                color = NeonRedAlert.copy(alpha = coreAlpha * 0.35f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
            // Inner bright white core.
            drawPath(
                path = path,
                color = Color.White.copy(alpha = coreAlpha),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

