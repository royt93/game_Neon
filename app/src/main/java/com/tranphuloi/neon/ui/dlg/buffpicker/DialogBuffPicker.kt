package com.tranphuloi.neon.ui.dlg.buffpicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonBottomSheet
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.ui.game.buff.RunBuff
import com.tranphuloi.neon.utils.Logger

/**
 * Wave 4 (42x) round 34 — post-boss roguelike buff picker. Rolls 3 random
 * buffs from RunBuff.pickThree(); user picks 1 (or ✕ to skip).
 *
 * Unlike modifier picker, this is in-game during run — sheet appears after
 * boss kill cinematic. Game gameStatus set to PAUSE while sheet is up.
 */
@Composable
fun DialogBuffPicker(
    onPicked: (RunBuff?) -> Unit,
) {
    val choices = remember { RunBuff.pickThree() }

    LaunchedEffect(Unit) {
        Logger.d("DialogBuffPicker shown: ${choices.joinToString { it.key }}")
    }

    NeonBottomSheet(
        title = "CHỌN BUFF",
        accentColor = NeonMagenta,
        titleSize = 22.sp,
        dismissible = true,
        onDismiss = {
            Logger.d("DialogBuffPicker: ✕ dismissed → no buff picked")
            onPicked(null)
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Phần thưởng sau khi hạ boss · chọn 1",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(14.dp))
            choices.forEachIndexed { idx, buff ->
                val color = when (idx) {
                    0 -> NeonCyan
                    1 -> NeonMagenta
                    else -> NeonRedAlert
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            Logger.d("DialogBuffPicker: chose ${buff.key}")
                            onPicked(buff)
                        }
                        .background(color.copy(alpha = 0.13f))
                        .border(BorderStroke(1.5.dp, color), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    // Icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = 0.25f))
                            .border(BorderStroke(1.dp, color.copy(alpha = 0.7f)), RoundedCornerShape(12.dp)),
                    ) {
                        Text(
                            text = buff.glyph,
                            color = color,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = buff.displayName,
                            color = color,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = buff.description,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            // Hint text
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hoặc tap ✕ để bỏ qua",
                color = NeonGold.copy(alpha = 0.65f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
