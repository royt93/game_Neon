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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.ui.game.story.StoryLine
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.delay

/**
 * Wave 5 (47x) — bottom-aligned dialogue card. Slide-up + fade-out. Uses
 * the existing banner pattern (AchievementBanner). Speaker color cyan for
 * "CAPTAIN", magenta for boss taunts (anything else).
 */
@Composable
fun StoryOverlay(
    line: StoryLine?,
    shownAtMillis: Long,
    modifier: Modifier = Modifier,
) {
    if (line == null || shownAtMillis == 0L) return

    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(shownAtMillis) {
        Logger.d("StoryOverlay shown: ${line.speaker} \"${line.text}\"")
        val ticks = (line.durationMs / 33).coerceAtLeast(30)
        repeat(ticks) {
            nowMillis = System.currentTimeMillis()
            delay(33L)
        }
    }
    val elapsed = (nowMillis - shownAtMillis).coerceAtLeast(0L)
    if (elapsed > line.durationMs) return

    val t = elapsed.toFloat() / line.durationMs
    // Round 24 — anchor is TopCenter (caller should align top), slide DOWN from
    // above the screen. translationY starts negative (above anchor) and slides
    // to 0 (anchor position). Reverse direction on slide-out.
    val slideOffset = when {
        t < 0.12f -> -80f * (1f - t / 0.12f)
        t < 0.85f -> 0f
        else -> -80f * ((t - 0.85f) / 0.15f)
    }
    val alpha = when {
        t < 0.12f -> t / 0.12f
        t < 0.85f -> 1f
        else -> 1f - (t - 0.85f) / 0.15f
    }

    // Round 24 — Vietnamese speaker label "ĐỘI TRƯỞNG" (Captain) gets cyan;
    // boss taunts (anything else) get magenta.
    val speakerColor =
        if (line.speaker == "Đội trưởng") palette.cyan else palette.magenta

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Round 24 fix — was vertical=80dp bottom-aligned → overlapped ship
            // at screenHeight-140 area. New: top-aligned with top padding 170dp
            // which clears HUD (16+50≈66dp) + BossHpBar (top=92, height ≈30 → ends 122dp)
            // + BossIntroOverlay banner zone (y=70-160). Ship locked at maxYOffset
            // (screenHeight-140) so upper area never has ship.
            .padding(start = 18.dp, end = 18.dp, top = 170.dp, bottom = 0.dp)
            .graphicsLayer {
                translationY = slideOffset
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(10.dp))
            .background(NeonBgMid.copy(alpha = 0.92f))
            .border(BorderStroke(1.5.dp, speakerColor), RoundedCornerShape(10.dp))
            .neonGlow(speakerColor, intensity = 0.35f, radiusFactor = 1.15f)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "● ${line.speaker}",
            color = speakerColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = line.text,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
