package com.tranphuloi.neon.ui.dlg.modepicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.ui.game.mode.GameMode
import com.tranphuloi.neon.utils.Logger
import com.tranphuloi.neon.common.neonGlow
import kotlinx.coroutines.launch

/**
 * Wave 5 (43x) — pick a game mode before next run. Saves to settings; the
 * next entry to GameScreen reads it via SettingsRepository.lastMode.
 *
 * DAILY is omitted from the picker (it's reached via the GameOver daily
 * leaderboard CTA in round 19). ENDLESS is also omitted — it's auto-triggered
 * after CAMPAIGN ends.
 */
@Composable
fun DialogModePicker(onPicked: () -> Unit) {
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { Logger.d("DialogModePicker shown") }

    // Wave 13 (#3) — SURVIVAL removed from the picker: it was a near-duplicate of
    // ENDLESS (both = chapter-1 cycled forever + escalation + score-by-survival),
    // with cross-wired names. ENDLESS is the richer twin (exp scaling + theme
    // rotation + endless leaderboard) so it stays; SurvivalProvider is kept only
    // for back-compat of any saved lastMode=survival (GameMode.fromKey fallback).
    val pickable = listOf(
        GameMode.CAMPAIGN,
        GameMode.TIME_ATTACK,
        GameMode.BOSS_RUSH,
        GameMode.ENDLESS,
    )

    com.tranphuloi.neon.common.NeonBottomSheet(
        title = "CHỌN CHẾ ĐỘ",
        accentColor = NeonViolet,
        onDismiss = {
            Logger.d("DialogModePicker: dismissed")
            onPicked()
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Khởi động lại để áp dụng chế độ mới",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            pickable.forEach { mode ->
                val color = when (mode) {
                    GameMode.CAMPAIGN -> NeonCyan
                    GameMode.SURVIVAL -> NeonGold
                    GameMode.TIME_ATTACK -> NeonMagenta
                    GameMode.BOSS_RUSH -> NeonRedAlert
                    GameMode.ENDLESS -> NeonViolet
                    else -> NeonCyan
                }
                val glyph = when (mode) {
                    GameMode.CAMPAIGN -> "⊕"
                    GameMode.SURVIVAL -> "∞"
                    GameMode.TIME_ATTACK -> "⏱"
                    GameMode.BOSS_RUSH -> "☠"
                    GameMode.ENDLESS -> "◌"
                    else -> "?"
                }
                val description = when (mode) {
                    GameMode.CAMPAIGN -> "Cốt truyện chính"
                    GameMode.SURVIVAL -> "Sóng vô tận · đua điểm"
                    GameMode.TIME_ATTACK -> "Đua điểm trong thời gian giới hạn"
                    GameMode.BOSS_RUSH -> "Đánh tất cả boss liên tiếp"
                    GameMode.ENDLESS -> "Sống sót · tăng độ khó liên tục"
                    else -> ""
                }
                val metaInfo = when (mode) {
                    GameMode.CAMPAIGN -> "5 chương · 100 màn"
                    GameMode.SURVIVAL -> "Vô tận · scaling +15%/wave"
                    GameMode.TIME_ATTACK -> "60 giây"
                    GameMode.BOSS_RUSH -> "~9 boss"
                    GameMode.ENDLESS -> "Vô tận · scaling exp"
                    else -> ""
                }
                // Round 30 revamp — card layout: icon left + title/description/meta right
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            Logger.d("ModePicker: chose ${mode.key}")
                            scope.launch {
                                settings.setLastMode(mode.key)
                                onPicked()
                            }
                        }
                        .background(color.copy(alpha = 0.13f))
                        .border(BorderStroke(1.5.dp, color), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    // Icon box: 56dp circular accent
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = 0.22f))
                            .border(BorderStroke(1.dp, color.copy(alpha = 0.7f)), RoundedCornerShape(12.dp)),
                    ) {
                        Text(
                            text = glyph,
                            color = color,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.displayName,
                            color = color,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = description,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Meta info chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = 0.18f))
                                .border(BorderStroke(0.5.dp, color.copy(alpha = 0.45f)), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = metaInfo,
                                color = color.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "▶",
                        color = color.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
