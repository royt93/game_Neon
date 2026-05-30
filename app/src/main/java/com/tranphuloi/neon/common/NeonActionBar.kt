package com.tranphuloi.neon.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Wave 11d (Pixel 7 Pro feedback round 2) #1 fix — shared action bar for
 * full-screen routes (Info, Stats, etc).
 *
 * Layout: title text on the left with infinite glow pulse, (✕) close icon
 * on the right inside a rounded circle. Caller passes a `titleColor` so
 * each screen can keep its accent (Stats uses NeonGold, Info uses NeonCyan).
 *
 * Why a shared composable: prior code duplicated this pattern with subtle
 * drift (Info used `← QUAY LẠI` text button at the right, Stats used a `✕`
 * icon — inconsistent). Single source of truth here so future screens
 * inherit the same affordance + glow + sizing rules.
 */
@Composable
fun NeonActionBar(
    title: String,
    titleColor: Color,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    closeColor: Color = titleColor,
) {
    // Subtle title pulse — same period as StatsScreen / MenuScreen glow conventions.
    val transition = rememberInfiniteTransition(label = "actionbar-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "title-pulse",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Pixel-3 #1 — structure flipped: (✕) icon FIRST (left), title SECOND
        // (right). Common iOS-style "close in top-left" affordance. Title
        // still gets maxLines=1 + ellipsis + weight(1f) so long names truncate.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A2030))
                .semantics {
                    contentDescription = "Đóng"
                    role = Role.Button
                }
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✕",
                style = TextStyle(
                    color = closeColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = TextStyle(
                color = titleColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .neonGlow(
                    color = titleColor,
                    intensity = pulse,
                    radiusFactor = 1.5f,
                ),
        )
    }
}
