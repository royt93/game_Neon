package com.tranphuloi.neon.ui.dlg.practicepicker

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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.App
import com.tranphuloi.neon.common.NeonBottomSheet
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.ui.game.mode.GameMode
import com.tranphuloi.neon.ui.game.stage.Chapter
import com.tranphuloi.neon.ui.game.stage.maxUnlockedChapter
import com.tranphuloi.neon.utils.Logger

/**
 * Task: QoL Practice — chọn 1 chương ĐÃ MỞ (theo checkpoint campaign) để luyện tập.
 * [onPicked] nhận chapterId; caller ghi checkpoint "practice" + setLastMode + nav Game.
 * Chương chưa tới (id > max mở) hiện khoá, không bấm được.
 */
@Composable
fun DialogChapterPicker(onPicked: (Int) -> Unit) {
    val app = LocalContext.current.applicationContext as App
    val campaignCp by app.runPersistence.checkpointFor(GameMode.CAMPAIGN.key).collectAsState(initial = 0)
    val maxCh = maxUnlockedChapter(campaignCp)

    LaunchedEffect(maxCh) { Logger.d("DialogChapterPicker shown — maxUnlocked=$maxCh (cp=$campaignCp)") }

    NeonBottomSheet(
        title = "Luyện tập — chọn chương",
        accentColor = NeonCyan,
        onDismiss = { Logger.d("DialogChapterPicker: dismissed"); onPicked(0) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Chơi lại chương đã mở · không ảnh hưởng tiến độ chiến dịch",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Chapter.entries.sortedBy { it.id }.forEach { ch ->
                val unlocked = ch.id <= maxCh
                val color = if (unlocked) Color(ch.tintArgb) else Color.White.copy(alpha = 0.25f)
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .let { m -> if (unlocked) m.clickable { Logger.d("Practice: chose chapter ${ch.id}"); onPicked(ch.id) } else m }
                        .background(color.copy(alpha = 0.13f))
                        .border(BorderStroke(1.5.dp, color), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = 0.22f)),
                    ) {
                        Text(
                            text = if (unlocked) "${ch.id}" else "🔒",
                            color = color, fontSize = 20.sp, fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chương ${ch.id} · ${ch.displayName}",
                            color = if (unlocked) Color.White else Color.White.copy(alpha = 0.4f),
                            fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (unlocked) "Vào đầu chương để luyện" else "Chưa mở — chơi chiến dịch để mở",
                            color = Color.White.copy(alpha = if (unlocked) 0.65f else 0.35f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}
