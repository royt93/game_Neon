package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.neonGlow

/**
 * Task 13 (đợt 4) — nút PARRY (phản đạn). Mirror [AbilityButton] nhưng màu cyan +
 * glyph "⇋" (phản chiếu) để phân biệt với ability (magenta) / bom (gold) / vũ khí
 * phụ (cyan-khác). Bấm khi sẵn sàng → mở cửa sổ phản đạn ngắn.
 */
@Composable
fun ParryButton(
    cooldownProgress: Float,                  // 0f..1f (1 = sẵn sàng)
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    val ready = cooldownProgress >= 1f
    val accent = if (ready) palette.cyan else Color.White.copy(alpha = 0.25f)
    Box(
        modifier = modifier
            .size(42.dp)
            .pointerInput(ready) {
                if (ready) detectTapGestures(onTap = { onActivate() })
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(BorderStroke(1.5.dp, accent), CircleShape)
                .neonGlow(
                    color = accent,
                    intensity = if (ready) 0.55f else 0.0f,
                    radiusFactor = 1.5f,
                ),
        ) {
            Text(
                text = "⇋",
                color = accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
        if (!ready) {
            Canvas(
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.Center),
            ) {
                val sweep = (1f - cooldownProgress).coerceIn(0f, 1f) * 360f
                drawArc(
                    color = palette.cyan.copy(alpha = 0.85f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    size = Size(size.width, size.height),
                    style = Stroke(width = 3f),
                )
            }
        }
    }
}
