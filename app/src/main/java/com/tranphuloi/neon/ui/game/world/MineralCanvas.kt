package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.tranphuloi.neon.common.PathPool
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.ui.game.mineral.model.MineralUI

/**
 * Round 67.6 — Pure-vector mineral rendering. Round 66b kept this as bitmap
 * (`ic_mineral.webp`), this round closes the gap. Each mineral = gold diamond
 * (rhombus) with cyan stroke + bright inner sparkle line. Vector recipe
 * matches the new HUD mineral indicator in IndicatorStatus.kt so a mineral
 * in flight looks identical to the count icon.
 */
@Composable
fun MineralCanvas(
    minerals: List<MineralUI>,
    modifier: Modifier = Modifier,
) {
    if (minerals.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (m in minerals) {
            drawMineral(m, density)
        }
    }
}

@Immutable
@Deprecated("Round 67.6 — pure-vector MineralCanvas no longer needs sprite.")
data class MineralSprite(val bitmap: Any? = null)

@Composable
@Deprecated("Round 67.6 — pure-vector MineralCanvas no longer needs sprite.")
fun rememberMineralSprite(): MineralSprite = MineralSprite()

private fun DrawScope.drawMineral(
    mineral: MineralUI,
    density: Density,
) {
    with(density) {
        val sizePx = 25.dp.toPx()                                       // matches Round 59 layout
        val xPx = mineral.xOffset.dp.toPx()
        val yPx = mineral.yOffset.dp.toPx()
        val cx = xPx + sizePx / 2f
        val cy = yPx + sizePx / 2f
        val alpha = mineral.alpha.coerceIn(0f, 1f)

        // Diamond/rhombus path
        val halfW = sizePx * 0.42f
        val halfH = sizePx * 0.46f
        val path = PathPool.acquire().apply {
            moveTo(cx, cy - halfH)
            lineTo(cx + halfW, cy)
            lineTo(cx, cy + halfH)
            lineTo(cx - halfW, cy)
            close()
        }
        drawPath(path, NeonGold.copy(alpha = alpha))
        PathPool.release(path)
        drawPath(
            path,
            NeonCyan.copy(alpha = alpha),
            style = Stroke(width = sizePx * 0.08f),
        )
        // Inner sparkle line — diagonal hot-streak
        drawLine(
            color = Color.White.copy(alpha = alpha * 0.85f),
            start = Offset(cx - halfW * 0.3f, cy - halfH * 0.3f),
            end = Offset(cx + halfW * 0.15f, cy + halfH * 0.15f),
            strokeWidth = sizePx * 0.10f,
        )
    }
}
