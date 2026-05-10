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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.ui.game.spark.ImpactSpark
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Renders all active [ImpactSpark]s as fading lines from origin to current
 * position. Single Canvas — no per-spark Composable.
 */
@Composable
fun ImpactSparkOverlay(sparks: List<ImpactSpark>) {
    if (sparks.isEmpty()) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasSparks = sparks.isNotEmpty()
    LaunchedEffect(hasSparks) {
        if (!hasSparks) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(33L)            // ~30 fps is plenty for 220ms-life sparks
        }
    }

    val density = LocalDensity.current.density
    Canvas(modifier = Modifier.fillMaxSize()) {
        // 1) Hit ring + core flash + electric arcs per burst — group sparks by
        //    createdAtMillis (all sparks in the same burst share timestamp + origin).
        val bursts = sparks.distinctBy { it.createdAtMillis }
        bursts.forEach { burst ->
            val t = burst.progress(nowMillis)
            if (t >= 1f) return@forEach
            val tEase = 1f - (1f - t) * (1f - t)
            val alpha = (1f - t)
            val ox = burst.originX * density
            val oy = burst.originY * density

            // 1a) Core flash — bright filled circle that pops at impact and fades
            //     fast (first 30% of life). Gives "pháo hoa nổ tung" punch.
            val coreT = (t / 0.3f).coerceIn(0f, 1f)
            val coreFade = 1f - coreT
            if (coreFade > 0f) {
                val coreRadius = 14f * density * (0.4f + 0.6f * coreT)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = coreFade),
                            NeonCyan.copy(alpha = coreFade * 0.7f),
                            Color.Transparent,
                        ),
                        center = Offset(ox, oy),
                        radius = coreRadius,
                    ),
                    radius = coreRadius,
                    center = Offset(ox, oy),
                )
            }

            // 1b) Electric zigzag arcs — 6 jagged lightning bolts radiating outward.
            //     Each is 4 segments with perpendicular jitter, length grows with t.
            //     Drawn first 60% of life so they fade before sparks complete.
            val arcT = (t / 0.6f).coerceIn(0f, 1f)
            val arcFade = 1f - arcT
            if (arcFade > 0f) {
                val arcLength = 35f * density * (0.3f + 0.7f * arcT)
                val arcCount = 6
                val seedAngle = (burst.createdAtMillis % 360).toFloat()
                for (i in 0 until arcCount) {
                    val baseAngle = (seedAngle + i * (360f / arcCount)) * (Math.PI / 180.0).toFloat()
                    val ux = cos(baseAngle)
                    val uy = sin(baseAngle)
                    // Perpendicular for zigzag jitter.
                    val pxn = -uy
                    val pyn = ux
                    var prevX = ox
                    var prevY = oy
                    val segments = 4
                    for (s in 1..segments) {
                        val sf = s.toFloat() / segments
                        // Pseudo-random jitter from burst hash — stable per arc.
                        val jitterSeed = ((burst.createdAtMillis xor (i * 1009L)) + s * 31L)
                        val jitter = (((jitterSeed % 200L).toFloat() / 200f) - 0.5f) *
                            8f * density * (1f - sf * 0.5f)
                        val nx = ox + ux * arcLength * sf + pxn * jitter
                        val ny = oy + uy * arcLength * sf + pyn * jitter
                        // Glow halo (cyan, thick) + bright core (white, thin).
                        drawLine(
                            color = NeonCyan.copy(alpha = 0.7f * arcFade),
                            start = Offset(prevX, prevY),
                            end = Offset(nx, ny),
                            strokeWidth = 3.5.dp.toPx() * arcFade,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.95f * arcFade),
                            start = Offset(prevX, prevY),
                            end = Offset(nx, ny),
                            strokeWidth = 1.4.dp.toPx() * arcFade,
                            cap = StrokeCap.Round,
                        )
                        prevX = nx
                        prevY = ny
                    }
                }
            }

            // 1c) Sonar-pulse hit rings — 3 concentric rings expanding at offset
            //     phases (0, +0.25, +0.5 of life) so they read as a shockwave radiating
            //     outward. Plus the original cyan/white/gold core+outer rings.
            //
            //     Pulse rings — each spawns at a delayed phase so their expansion
            //     overlaps in time but at different radii (visual "ripple").
            for (pulseIndex in 0..2) {
                val pulseDelay = pulseIndex * 0.22f          // phase offset 0, 0.22, 0.44
                val pulseT = ((t - pulseDelay) / (1f - pulseDelay)).coerceIn(0f, 1f)
                if (pulseT <= 0f || pulseT >= 1f) continue
                val pulseEase = 1f - (1f - pulseT) * (1f - pulseT)
                val pulseRadius = 90f * density * pulseEase
                val pulseAlpha = (1f - pulseT) * 0.55f
                val pulseColor = when (pulseIndex) {
                    0 -> NeonCyan
                    1 -> Color.White
                    else -> NeonGold
                }
                drawCircle(
                    color = pulseColor.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = Offset(ox, oy),
                    style = Stroke(width = 2.5.dp.toPx() * (1f - pulseT * 0.6f)),
                )
            }

            // Original core hit rings — 60dp expanding cyan + smaller white inner.
            val ringRadius = 60f * density * tEase
            drawCircle(
                color = NeonCyan.copy(alpha = 0.85f * alpha),
                radius = ringRadius,
                center = Offset(ox, oy),
                style = Stroke(width = 4.dp.toPx() * (1f - t * 0.5f)),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.55f * alpha),
                radius = ringRadius * 0.85f,
                center = Offset(ox, oy),
                style = Stroke(width = 1.5.dp.toPx() * (1f - t * 0.5f)),
            )
            // Gold "firework" outer ring — slightly larger, slimmer, warm bias.
            drawCircle(
                color = NeonGold.copy(alpha = 0.6f * alpha),
                radius = ringRadius * 1.15f,
                center = Offset(ox, oy),
                style = Stroke(width = 2.dp.toPx() * (1f - t * 0.5f)),
            )
        }

        // 2) Individual sparks — radial particles streaking outward.
        sparks.forEach { spark ->
            val t = spark.progress(nowMillis)
            if (t >= 1f) return@forEach
            val (px, py) = spark.position(nowMillis)
            val ox = spark.originX * density
            val oy = spark.originY * density
            val cx = px * density
            val cy = py * density
            val alpha = (1f - t)
            drawLine(
                color = NeonCyan.copy(alpha = 0.55f * alpha),
                start = Offset(ox, oy),
                end = Offset(cx, cy),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(ox, oy),
                end = Offset(cx, cy),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
