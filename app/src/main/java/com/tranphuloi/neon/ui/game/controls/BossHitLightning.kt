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
import com.tranphuloi.neon.common.NeonGold
import kotlinx.coroutines.delay
import com.tranphuloi.neon.common.PathPool

/**
 * Round 79 (#4 fix) — Full-screen lightning when player bullet hits boss.
 *
 * User feedback: "ở boss, khi tôi bắn trúng, tia sét tại sao không full width screen?"
 * Existing [com.tranphuloi.neon.ui.game.world.ImpactSparkOverlay] has electric
 * arc bursts but only 35dp long. This component fires 5 zigzag bolts from 4
 * screen-edge anchor points to the boss center, lasting [LIFE_MILLIS]ms then
 * fading. Pattern mirrors [BossEntryLightning] but triggered by hit (not
 * entry phase).
 *
 * @param bossCxDp boss center X in game-coord dp (will be drawn inside
 *   GameWorld's scaled Box, so the bolt origin extends to `-spawnXMargin..(W +
 *   spawnXMargin)` to reach the visual screen edge at FAR zoom).
 * @param triggerMillis wall-clock of last boss hit. Component re-fires whenever
 *   this value changes (each frame's `tinker` from GameState may rewrite it).
 * @param spawnXMargin extension factor passed from GameState so bolt origins
 *   span the actual visual screen at FAR zoom (not just the inner 70%).
 */
@Composable
fun BossHitLightning(
    bossCxDp: Float,
    bossCyDp: Float,
    triggerMillis: Long,
    spawnXMargin: Float,
    modifier: Modifier = Modifier,
) {
    if (triggerMillis <= 0L) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(triggerMillis) {
        while (true) {
            val now = System.currentTimeMillis()
            if (now - triggerMillis > LIFE_MILLIS) {
                nowMillis = now
                break
            }
            nowMillis = now
            delay(33L)
        }
    }

    val elapsed = nowMillis - triggerMillis
    if (elapsed > LIFE_MILLIS) return
    val lifeProgress = elapsed.toFloat() / LIFE_MILLIS.toFloat()
    val alpha = (1f - lifeProgress).coerceIn(0f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val random = kotlin.random.Random(triggerMillis)
        val cx = bossCxDp.dp.toPx()
        val cy = bossCyDp.dp.toPx()
        val marginPx = spawnXMargin.dp.toPx()
        // Logical visual width spans [-margin..(size.width + margin)] when the
        // outer GameWorld Box uses graphicsLayer.scale<1. Compute bolt origins
        // across this extended range so the bolts reach the visual screen edge.
        val visualLeft = -marginPx
        val visualRight = size.width + marginPx
        val visualWidth = visualRight - visualLeft

        repeat(BOLT_COUNT) { i ->
            val edge = i % 4
            val (sx, sy) = when (edge) {
                0 -> visualLeft + random.nextFloat() * visualWidth to -marginPx
                1 -> visualLeft to (random.nextFloat() * cy)
                2 -> visualRight to (random.nextFloat() * cy)
                else -> visualLeft + random.nextFloat() * visualWidth to size.height + marginPx
            }

            // 6-segment zigzag with perpendicular jitter.
            val path = PathPool.acquire().apply {
                moveTo(sx, sy)
                val segments = 7
                val jitterRange = 36f.dp.toPx()
                for (s in 1 until segments) {
                    val t = s.toFloat() / segments
                    val baseX = sx + (cx - sx) * t
                    val baseY = sy + (cy - sy) * t
                    val jx = baseX + (random.nextFloat() - 0.5f) * jitterRange
                    val jy = baseY + (random.nextFloat() - 0.5f) * jitterRange
                    lineTo(jx, jy)
                }
                lineTo(cx, cy)
            }

            val boltAlpha = alpha * (0.6f + random.nextFloat() * 0.4f)
            // Outer cyan glow (wide, dim).
            drawPath(
                path = path,
                color = NeonCyan.copy(alpha = boltAlpha * 0.35f),
                style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round),
            )
            // Mid gold flash (medium).
            drawPath(
                path = path,
                color = NeonGold.copy(alpha = boltAlpha * 0.55f),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
            // Inner white core (thin, bright).
            drawPath(
                path = path,
                color = Color.White.copy(alpha = boltAlpha),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
            )
            PathPool.release(path)
        }
    }
}

private const val LIFE_MILLIS = 280L
private const val BOLT_COUNT = 5
