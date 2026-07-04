package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonGold
import kotlinx.coroutines.delay

/**
 * 12c: Wave clear banner with "+5 minerals" subtext. Auto-dismiss 1.8s.
 */
@Composable
fun WaveClearBanner(
    shownAtMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (shownAtMillis == 0L) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shownAtMillis) {
        repeat(55) {                                                    // ~1.8s
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val elapsed = (nowMillis - shownAtMillis).coerceAtLeast(0L)
    if (elapsed > 1800L) return
    val t = elapsed.toFloat() / 1800f
    val scale = when {
        t < 0.15f -> 0.6f + (t / 0.15f) * 0.7f
        t < 0.30f -> 1.3f - ((t - 0.15f) / 0.15f) * 0.3f
        else -> 1.0f
    }
    val alpha = if (t < 0.85f) 1f else 1f - ((t - 0.85f) / 0.15f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val r = size.minDimension / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonGold.copy(alpha = 0.55f),
                        NeonGold.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = r,
                ),
                radius = r,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Hoàn thành đợt!",
                color = NeonGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(letterSpacing = 3.sp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "+5 khoáng vật",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
