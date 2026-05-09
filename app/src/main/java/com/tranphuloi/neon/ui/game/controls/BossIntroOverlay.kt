package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonBgDeep
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.neonGlow
import kotlinx.coroutines.delay

private const val DURATION_MILLIS: Long = 1500L

/**
 * 21c Boss intro cinematic — REDESIGNED to a TOP BANNER zone (was full-screen
 * centered text that overlapped damage numbers / combo popups / pickup popups).
 *
 * Layout:
 *   - Pulsing red border around entire screen (visual alarm, doesn't block content)
 *   - Boss name banner pinned to TOP-CENTER (y=70dp..160dp), only consumes the
 *     top strip, so player effects in the middle / bottom of the screen remain visible.
 */
@Composable
fun BossIntroOverlay(
    bossName: String,
    shownAtMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (shownAtMillis == 0L || bossName.isBlank()) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shownAtMillis) {
        repeat(50) {
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val elapsed = (nowMillis - shownAtMillis).coerceAtLeast(0L)
    if (elapsed > DURATION_MILLIS) return
    val t = elapsed.toFloat() / DURATION_MILLIS

    val pulseAlpha = (kotlin.math.sin(t * Math.PI.toFloat() * 4f) * 0.5f + 0.5f) * (1f - t * 0.6f)
    // Slide-down from top + scale pop.
    val slideY = when {
        t < 0.18f -> -120f * (1f - t / 0.18f)
        t < 0.85f -> 0f
        else -> -120f * ((t - 0.85f) / 0.15f)
    }
    val bannerAlpha = if (t < 0.85f) 1f else 1f - ((t - 0.85f) / 0.15f)

    Box(modifier = modifier.fillMaxSize()) {
        // Pulsing red warning border around the entire screen — visual alarm,
        // doesn't block player content because it's only a 1-pixel-wide stroke at edges.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    BorderStroke(8.dp, NeonRedAlert.copy(alpha = pulseAlpha)),
                    RectangleShape,
                ),
        )
        // Boss name banner — pinned to TOP. Backed by darkened plate so damage numbers
        // / combo popups in the body of the screen don't show through, but middle and
        // bottom of the screen remain unblocked for gameplay events.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 70.dp, start = 32.dp, end = 32.dp)
                .graphicsLayer {
                    translationY = slideY
                    alpha = bannerAlpha
                },
        ) {
            Text(
                text = "⚠ WARNING",
                color = NeonRedAlert.copy(alpha = pulseAlpha),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = TextStyle(letterSpacing = 4.sp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(NeonBgDeep.copy(alpha = 0.85f))
                    .border(
                        BorderStroke(2.dp, NeonRedAlert),
                        RoundedCornerShape(6.dp),
                    )
                    .neonGlow(NeonRedAlert, intensity = 0.4f, radiusFactor = 1.1f)
                    .padding(vertical = 10.dp, horizontal = 8.dp),
            ) {
                Text(
                    text = bossName,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    style = TextStyle(letterSpacing = 4.sp),
                )
            }
        }
    }
}
