package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * Task 14 (đợt 4) — glitch / RGB-tear thoáng khi ship trúng đòn: vài dải ngang
 * bị tách màu (cyan lệch trái + magenta lệch phải) → cảm giác "màn hình rách".
 * [intensity] 0f..1f (tắt dần, xem [VisualJuice.damageTearAlpha]); [seed] đổi mỗi
 * hit để vị trí dải khác nhau. Gate `reduceMotion` ở call site (glitch = motion).
 */
@Composable
fun DamageTearOverlay(
    intensity: Float,
    seed: Int,
    modifier: Modifier = Modifier,
) {
    if (intensity <= 0f) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bands = 5
        val shift = w * 0.03f * intensity
        val bandH = h * 0.018f
        val a = 0.35f * intensity   // subtle — chồng lên chromatic-mép sẵn có, không nhiễu
        val cyan = Color(0xFF00E5FF).copy(alpha = a)
        val magenta = Color(0xFFFF3DCB).copy(alpha = a)
        for (i in 0 until bands) {
            val y = (((seed * 131 + i * 613) % 1000).let { if (it < 0) it + 1000 else it } / 1000f) * (h - bandH)
            drawRect(cyan, topLeft = Offset(-shift, y), size = Size(w, bandH))
            drawRect(magenta, topLeft = Offset(shift, y), size = Size(w, bandH))
        }
    }
}
