package com.tranphuloi.neon.ui.dlg.modifierpicker

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
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.ui.game.modifier.RunModifier
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.launch

/**
 * Wave 5 (25x) — pre-game modifier picker. Rolls 3 random modifiers (excluding
 * NONE) on first composition; user picks one (or skips → applies NONE).
 *
 * Visual: 3 vertical cards, each showing the modifier title + description + score
 * bonus. Picked modifier is persisted to settings.lastModifier and read by
 * GameState at run start.
 */
@Composable
fun DialogModifierPicker(onPicked: () -> Unit) {
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    val choices = remember { RunModifier.pickThree() }

    LaunchedEffect(Unit) {
        Logger.d("DialogModifierPicker shown: ${choices.joinToString { it.key }}")
    }

    Card(
        backgroundColor = NeonBgMid,
        border = BorderStroke(2.dp, NeonGold),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.neonGlow(color = NeonGold, intensity = 0.4f, radiusFactor = 1.2f)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "CHỌN BUFF",
                color = NeonGold,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h5,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mạo hiểm · thưởng — chọn cái khó để nhân điểm",
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            choices.forEachIndexed { idx, mod ->
                val color = when (idx) {
                    0 -> NeonCyan
                    1 -> NeonMagenta
                    else -> NeonRedAlert
                }
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.13f))
                        .border(BorderStroke(1.5.dp, color), RoundedCornerShape(12.dp))
                        .clickable {
                            Logger.d("ModifierPicker: chose ${mod.key} (score×${mod.scoreMul})")
                            scope.launch {
                                settings.setLastModifier(mod.key)
                                onPicked()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = mod.displayName,
                                color = color,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "×${mod.scoreMul}",
                                color = NeonGold,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = mod.description,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.caption,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(onClick = {
                Logger.d("ModifierPicker: skipped — apply NONE")
                scope.launch {
                    settings.setLastModifier(RunModifier.NONE.key)
                    onPicked()
                }
            }) {
                Text("BỎ QUA", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        }
    }
}
