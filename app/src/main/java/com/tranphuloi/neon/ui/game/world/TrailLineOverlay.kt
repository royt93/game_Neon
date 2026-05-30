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
import com.tranphuloi.neon.common.PathPool
import com.tranphuloi.neon.ui.game.spark.TrailLine
import kotlinx.coroutines.delay

/**
 * Renders active [TrailLine]s. Wave 11a audit follow-up adds real connected
 * line visuals (vs prior 3-point impactSpark clusters) for REFLECT bounce
 * arcs + CHAIN_LIGHTNING bolts.
 *
 * Smooth lines (`jagged = false`) → REFLECT: 3-layer stroke (outer halo +
 * mid + bright white core). Jagged lines → CHAIN_LIGHTNING: zigzag path via
 * Path with deterministic per-segment offsets.
 *
 * Fades out over the line's duration (fast-decay alpha curve).
 */
@Composable
fun TrailLineOverlay(lines: List<TrailLine>) {
    if (lines.isEmpty()) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val hasLines = lines.isNotEmpty()
    LaunchedEffect(hasLines) {
        if (!hasLines) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(16L)            // ~60 fps so lines look smooth
        }
    }

    val density = LocalDensity.current.density
    Canvas(modifier = Modifier.fillMaxSize()) {
        lines.forEach { line ->
            val t = line.progress(nowMillis)
            if (t >= 1f) return@forEach
            val alpha = (1f - t) * (1f - t)       // ease-out fade
            val baseColor = Color(line.colorArgb)
            val widthPx = line.widthDp * density
            if (line.jagged) {
                drawJaggedBolt(line, baseColor, alpha, widthPx)
            } else {
                drawSmoothArc(line, baseColor, alpha, widthPx)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothArc(
    line: TrailLine,
    color: Color,
    alpha: Float,
    widthPx: Float,
) {
    val start = Offset(line.x1, line.y1)
    val end = Offset(line.x2, line.y2)
    // Outer halo (wider, low alpha)
    drawLine(
        color = color.copy(alpha = alpha * 0.30f),
        start = start, end = end,
        strokeWidth = widthPx * 3.5f,
        cap = StrokeCap.Round,
    )
    // Mid layer
    drawLine(
        color = color.copy(alpha = alpha * 0.70f),
        start = start, end = end,
        strokeWidth = widthPx * 1.8f,
        cap = StrokeCap.Round,
    )
    // Bright white core
    drawLine(
        color = Color.White.copy(alpha = alpha * 0.85f),
        start = start, end = end,
        strokeWidth = widthPx * 0.6f,
        cap = StrokeCap.Round,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawJaggedBolt(
    line: TrailLine,
    color: Color,
    alpha: Float,
    widthPx: Float,
) {
    val pts = line.jaggedPoints(segments = 6)
    if (pts.size < 2) return
    // Audit-10 P2 fix — was `val path = Path()` per-tick per-trail allocation.
    // At peak combat (multiple chain-lightning bolts + reflect arcs) this
    // produced per-frame Path GC pressure. Migrate to PathPool: acquire +
    // draw 3 layers + release once.
    val path = PathPool.acquire().apply {
        moveTo(pts[0].first, pts[0].second)
        for (i in 1 until pts.size) {
            lineTo(pts[i].first, pts[i].second)
        }
    }
    // Outer halo
    drawPath(
        path = path,
        color = color.copy(alpha = alpha * 0.35f),
        style = Stroke(width = widthPx * 3.5f, cap = StrokeCap.Round),
    )
    // Mid layer
    drawPath(
        path = path,
        color = color.copy(alpha = alpha * 0.75f),
        style = Stroke(width = widthPx * 1.8f, cap = StrokeCap.Round),
    )
    // Bright white core
    drawPath(
        path = path,
        color = Color.White.copy(alpha = alpha * 0.90f),
        style = Stroke(width = widthPx * 0.6f, cap = StrokeCap.Round),
    )
    PathPool.release(path)
}
