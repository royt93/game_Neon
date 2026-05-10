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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.ui.game.spark.ImpactSpark
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
        // 1) Hit ring per burst — group sparks by createdAtMillis (all sparks in the
        //    same burst share timestamp + origin). Draw exactly one ring per burst.
        //    Bigger (60dp, was 40) + longer duration (320ms via DURATION_MILLIS) +
        //    thicker stroke for unmistakable collision feedback.
        val bursts = sparks.distinctBy { it.createdAtMillis }
        bursts.forEach { burst ->
            val t = burst.progress(nowMillis)
            if (t >= 1f) return@forEach
            val tEase = 1f - (1f - t) * (1f - t)
            val ringRadius = 60f * density * tEase
            val alpha = (1f - t)
            val ox = burst.originX * density
            val oy = burst.originY * density
            // Outer cyan ring (bold), mid white ring, both thick early.
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
