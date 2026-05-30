package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.neonGlow

/**
 * 20b: Smart bomb button — Round 67.7 vector. Replaced emoji 💣 với Canvas
 * drawing: dark sphere body + diagonal fuse line + bright spark dot at tip.
 */
@Composable
fun SmartBombButton(
    count: Int,
    onDispatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = count > 0
    val accent = if (enabled) NeonGold else Color.White.copy(alpha = 0.25f)
    // Pixel-3 round 5 — reverted Pixel-3 #5 label addition per user feedback
    // "tôi không cần label sóng nổ + bomb". Column wrapper removed; restored
    // to single Box icon as original.
    Box(
        modifier = modifier
            .size(42.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(onTap = { onDispatch() })
                }
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
                    color = if (enabled) NeonRedAlert else accent,
                    intensity = if (enabled) 0.55f else 0.0f,
                    radiusFactor = 1.5f,
                ),
        ) {
            // Round 67.7 — vector bomb: sphere + fuse + spark.
            Canvas(modifier = Modifier.size(22.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f + size.height * 0.10f
                val bombR = size.width * 0.35f
                // Bomb sphere
                drawCircle(
                    color = if (enabled) NeonRedAlert else accent,
                    radius = bombR,
                    center = Offset(cx, cy),
                )
                // Bomb sphere outline highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = bombR * 0.30f,
                    center = Offset(cx - bombR * 0.30f, cy - bombR * 0.30f),
                )
                // Fuse line (top-right) — diagonal stroke from sphere tip outward
                val fuseStart = Offset(cx + bombR * 0.55f, cy - bombR * 0.85f)
                val fuseEnd = Offset(cx + bombR * 1.05f, cy - bombR * 1.55f)
                drawLine(
                    color = accent,
                    start = fuseStart,
                    end = fuseEnd,
                    strokeWidth = size.width * 0.05f,
                )
                // Spark at fuse tip
                drawCircle(
                    color = NeonGold,
                    radius = size.width * 0.07f,
                    center = fuseEnd,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = size.width * 0.03f,
                    center = fuseEnd,
                )
            }
        }
        // Count badge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.85f))
                .border(BorderStroke(1.dp, accent), CircleShape),
        ) {
            Text(
                text = count.toString(),
                color = accent,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
