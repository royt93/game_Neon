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

    val pickable = listOf(
        GameMode.CAMPAIGN,
        GameMode.SURVIVAL,
        GameMode.TIME_ATTACK,
        GameMode.BOSS_RUSH,
        GameMode.ENDLESS,
    )

    Card(
        backgroundColor = NeonBgMid,
        border = BorderStroke(2.dp, NeonViolet),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.neonGlow(color = NeonViolet, intensity = 0.4f, radiusFactor = 1.2f)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "CHỌN CHẾ ĐỘ",
                color = NeonViolet,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h5,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Khởi động lại để áp dụng chế độ mới",
                color = Color.White.copy(alpha = 0.7f),
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
                val description = when (mode) {
                    GameMode.CAMPAIGN -> "5 chương · 100 màn"
                    GameMode.SURVIVAL -> "Sóng vô tận · đua điểm"
                    GameMode.TIME_ATTACK -> "60 giây · điểm tối đa"
                    GameMode.BOSS_RUSH -> "Tất cả boss · liên tiếp"
                    GameMode.ENDLESS -> "Thời gian sống · tăng cấp dần"
                    else -> ""
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f))
                        .border(BorderStroke(1.5.dp, color), RoundedCornerShape(12.dp))
                        .clickable {
                            Logger.d("ModePicker: chose ${mode.key}")
                            scope.launch {
                                settings.setLastMode(mode.key)
                                onPicked()
                            }
                        }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = mode.displayName,
                            color = color,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.h6,
                        )
                        Text(
                            text = description,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.caption,
                        )
                    }
                }
            }
        }
    }
}
