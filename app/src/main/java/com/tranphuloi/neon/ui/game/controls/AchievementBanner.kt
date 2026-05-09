package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.Achievement
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.delay

/**
 * 9b: Slide-in achievement unlock banner. Auto-dismiss after 3s.
 */
@Composable
fun AchievementBanner(
    achievement: Achievement?,
    shownAtMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (achievement == null || shownAtMillis == 0L) return

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shownAtMillis) {
        Logger.d("AchievementBanner shown: ${achievement.id}")
        repeat(90) {                                                    // ~3s @ 33ms
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val elapsed = (nowMillis - shownAtMillis).coerceAtLeast(0L)
    if (elapsed > 3000L) return

    val t = elapsed.toFloat() / 3000f                                   // 0..1
    // Slide in 0..0.15, hold 0.15..0.85, slide out 0.85..1.0
    val slideOffset = when {
        t < 0.15f -> -200f * (1f - t / 0.15f)
        t < 0.85f -> 0f
        else -> -200f * ((t - 0.85f) / 0.15f)
    }
    val alpha = when {
        t < 0.15f -> t / 0.15f
        t < 0.85f -> 1f
        else -> 1f - (t - 0.85f) / 0.15f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 80.dp)
            .graphicsLayer {
                translationY = slideOffset
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(8.dp))
            .background(NeonBgMid)
            .border(BorderStroke(2.dp, NeonGold), RoundedCornerShape(8.dp))
            .neonGlow(NeonGold, intensity = 0.45f, radiusFactor = 1.2f)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "🏆 ACHIEVEMENT UNLOCKED",
            color = NeonGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = achievement.title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = achievement.description,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
        )
    }
}
