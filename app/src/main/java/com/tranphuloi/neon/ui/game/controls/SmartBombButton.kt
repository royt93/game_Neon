package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.neonGlow

/**
 * 20b: Smart bomb button — circular badge showing remaining bomb count.
 *
 * Layout: outer wrapper Box (NOT clipped — so the count badge can sit at the
 * corner outside the inscribed circle), inner clipped circle as the button face,
 * badge as a sibling positioned at the bottom-end corner.
 *
 * Tap to dispatch (clears all enemies + enemy lasers via callback). Disabled when
 * count = 0.
 */
@Composable
fun SmartBombButton(
    count: Int,
    onDispatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = count > 0
    val accent = if (enabled) NeonGold else Color.White.copy(alpha = 0.25f)
    // 30% smaller per user feedback: wrapper 60→42dp, inner 48→34dp, badge 20→14dp.
    Box(
        modifier = modifier
            .size(42.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(onTap = { onDispatch() })
                }
            },
    ) {
        // Inner button face (circle, clipped, glow).
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
            Text(text = "💣", fontSize = 16.sp)
        }
        // Count badge — sibling, NOT clipped by parent. Sits at bottom-end corner.
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
