package com.tranphuloi.neon.ui.game.world

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.PathPool
import com.tranphuloi.neon.common.drawSoftHalo
import com.tranphuloi.neon.ui.game.drone.DroneUI

/**
 * Task 01 — batched render cho drone companion (tối đa 2). Vẽ thuần vector neon:
 * thân diamond NeonCyan + lõi trắng + halo mềm + vòng HP (cyan→gold→red theo
 * hpRatio). Mờ dần khi sắp vỡ. Toạ độ theo dp (đổi sang px như các Canvas khác).
 */
@Composable
fun DroneCanvas(
    drones: List<DroneUI>,
    modifier: Modifier = Modifier,
) {
    if (drones.isEmpty()) return
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        for (d in drones) drawDrone(d, density)
    }
}

private fun DrawScope.drawDrone(d: DroneUI, density: Density) {
    with(density) {
        val sizePx = d.size.dp.toPx()
        val cx = d.xOffset.dp.toPx() + sizePx / 2f
        val cy = d.yOffset.dp.toPx() + sizePx / 2f
        val r = sizePx / 2f
        val alpha = (0.55f + 0.45f * d.hpRatio).coerceIn(0f, 1f) // mờ dần khi mất HP

        // Task 06 — màu thân theo biến thể (cyan tấn công / gold khiên / green hồi).
        val bodyColor = when (d.variant) {
            com.tranphuloi.neon.ui.game.drone.DroneVariant.ATTACK -> NeonCyan
            com.tranphuloi.neon.ui.game.drone.DroneVariant.SHIELD -> NeonGold
            com.tranphuloi.neon.ui.game.drone.DroneVariant.HEAL -> Color(0xFF3DFF88)
        }

        drawSoftHalo(bodyColor, 0.7f * alpha, r * 1.8f, Offset(cx, cy))

        // Thân drone: diamond (hình thoi).
        val body = PathPool.acquire().apply {
            moveTo(cx, cy - r * 0.8f)
            lineTo(cx + r * 0.8f, cy)
            lineTo(cx, cy + r * 0.8f)
            lineTo(cx - r * 0.8f, cy)
            close()
        }
        drawPath(body, bodyColor.copy(alpha = alpha))
        drawPath(body, Color.White.copy(alpha = 0.7f * alpha), style = Stroke(width = sizePx * 0.06f))
        PathPool.release(body)

        // Lõi sáng.
        drawCircle(Color.White.copy(alpha = 0.85f * alpha), r * 0.22f, Offset(cx, cy))

        // Vòng HP (bắt đầu từ đỉnh, quét theo hpRatio).
        val hpColor = when {
            d.hpRatio > 0.5f -> NeonCyan
            d.hpRatio > 0.25f -> NeonGold
            else -> NeonRedAlert
        }
        drawArc(
            color = hpColor.copy(alpha = alpha),
            startAngle = -90f,
            sweepAngle = 360f * d.hpRatio,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = sizePx * 0.08f),
        )
    }
}
