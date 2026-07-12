package com.tranphuloi.neon.ui.dlg.difficulty

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.Difficulty
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.launch

@Composable
fun DialogDifficultyPicker(onPicked: () -> Unit) {
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { Logger.d("DialogDifficultyPicker shown") }

    com.tranphuloi.neon.common.NeonBottomSheet(
        title = "Chọn độ khó",
        accentColor = palette.magenta,
        onDismiss = {
            Logger.d("DialogDifficultyPicker: dismissed")
            onPicked()
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Có thể đổi sau ở Cài đặt",
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Difficulty.values().forEach { d ->
                val color = when (d) {
                    Difficulty.EASY -> palette.cyan
                    Difficulty.NORMAL -> palette.gold
                    Difficulty.HARD -> palette.redAlert
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
                            Logger.d("DifficultyPicker: chose ${d.key}")
                            scope.launch {
                                settings.setDifficulty(d)
                                onPicked()
                            }
                        }
                        .padding(vertical = 10.dp),
                ) {
                    Text(
                        text = when (d) {
                            Difficulty.EASY -> "Dễ"
                            Difficulty.NORMAL -> "Vừa"
                            Difficulty.HARD -> "Khó"
                        },
                        color = color,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.h6,
                    )
                }
            }
        }
    }
}
