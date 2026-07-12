package com.tranphuloi.neon.ui.dlg.modifierpicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonBgMid
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
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    val choices = remember { RunModifier.pickThree() }

    LaunchedEffect(Unit) {
        Logger.d("DialogModifierPicker shown: ${choices.joinToString { it.key }}")
    }

    com.tranphuloi.neon.common.NeonBottomSheet(
        title = "Chọn thử thách",
        accentColor = palette.gold,
        onDismiss = {
            // Round 29 — ✕ tap = skip picker = apply NONE modifier.
            Logger.d("DialogModifierPicker: ✕ dismissed → skipping = apply NONE")
            scope.launch {
                settings.setLastModifier(RunModifier.NONE.key)
                onPicked()
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Mạo hiểm · thưởng — chọn cái khó để nhân điểm",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Task 08 — THỬ THÁCH HẰNG NGÀY: modifier cố định theo ngày (mọi người
            // cùng chơi + đua daily leaderboard), thưởng +minerals 1 lần/ngày.
            val today = remember { com.tranphuloi.neon.data.LeaderboardRepository.todayUtcDayKey() }
            val dailyMod = remember(today) { com.tranphuloi.neon.ui.game.modifier.DailyChallenge.modifierFor(today) }
            Column(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.gold.copy(alpha = 0.16f))
                    .border(BorderStroke(2.dp, palette.gold), RoundedCornerShape(14.dp))
                    .clickable {
                        Logger.d("ModifierPicker: DAILY CHALLENGE chose ${dailyMod.key} (day=$today)")
                        scope.launch {
                            settings.setLastModifier(dailyMod.key)
                            onPicked()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "⚡", color = palette.gold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Thử thách hôm nay",
                        color = palette.gold,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = "+${com.tranphuloi.neon.ui.game.modifier.DailyChallenge.REWARD_MINERALS}◇", color = palette.gold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dailyMod.displayName} · thưởng 1 lần/ngày · đua BXH ngày",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                )
            }

            choices.forEachIndexed { idx, mod ->
                val color = when (idx) {
                    0 -> palette.cyan
                    1 -> palette.magenta
                    else -> palette.redAlert
                }
                val glyph = when (mod.key) {
                    "triple_speed" -> "⚡"
                    "glass_cannon" -> "◊"
                    "no_shields" -> "⊘"
                    "super_magnet" -> "◉"
                    "berserker" -> "⚔"
                    "tank" -> "⊞"
                    "bosses_only" -> "☠"
                    "double_or_nothing" -> "✦"
                    else -> "?"
                }
                // Round 30 revamp — icon + name + score badge + stat bars + description
                Column(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(color.copy(alpha = 0.12f))
                        .border(BorderStroke(1.5.dp, color), RoundedCornerShape(14.dp))
                        .clickable {
                            Logger.d("ModifierPicker: chose ${mod.key} (score×${mod.scoreMul})")
                            scope.launch {
                                settings.setLastModifier(mod.key)
                                onPicked()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    // Top row: icon + name + score badge
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Icon box
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color.copy(alpha = 0.25f))
                                .border(BorderStroke(1.dp, color.copy(alpha = 0.65f)), RoundedCornerShape(10.dp)),
                        ) {
                            Text(
                                text = glyph,
                                color = color,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = mod.displayName,
                            color = color,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            modifier = Modifier.weight(1f),
                        )
                        // Score multiplier badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.gold.copy(alpha = 0.25f))
                                .border(BorderStroke(1.dp, palette.gold), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "×${mod.scoreMul}",
                                color = palette.gold,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Stat bars row — only show stats that differ from 1.0
                    StatBarsRow(mod = mod)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = mod.description,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                    )
                }
            }
            // Round 29 — BỎ QUA button removed. The sheet's ✕ button already
            // dismisses; user wants ✕ to mean "skip = apply NONE".
        }
    }
}

/**
 * Round 30 — visual stat bar row. Shows HP / DMG / SPD / MAG as horizontal
 * bars centered around 1.0× (50% width). Positive = green-cyan, negative = red.
 */
@Composable
private fun StatBarsRow(mod: RunModifier) {
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    val items = listOf(
        Triple("HP", mod.hpMul, palette.redAlert),
        Triple("DMG", mod.damageMul, palette.gold),
        Triple("SPD", mod.speedMul, palette.cyan),
        Triple("MAG", mod.magnetMul, palette.violet),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEach { (label, mul, accent) ->
            StatBar(label = label, mul = mul, accent = accent, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatBar(label: String, mul: Float, accent: Color, modifier: Modifier = Modifier) {
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    val isNeutral = kotlin.math.abs(mul - 1f) < 0.01f
    val fillFrac = (mul / 3f).coerceIn(0.1f, 1f)              // 1.0× = ~0.33 fill, 3.0× = 1.0
    val barColor = when {
        isNeutral -> Color.White.copy(alpha = 0.35f)
        mul > 1f -> accent
        else -> palette.redAlert
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = barColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        // Bar background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            // Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillFrac)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (isNeutral) "—" else "×$mul",
            color = barColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
