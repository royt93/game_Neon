package com.tranphuloi.neon.ui.game.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.ui.game.buff.LocalActiveBuffs

/**
 * Round 35 — small HUD strip showing player's currently-picked roguelike buffs.
 * Each buff renders as a 28dp circular chip with its glyph icon. Same icon for
 * duplicate picks shows a count badge.
 *
 * Renders nothing when no buffs active (most of early game).
 */
@Composable
fun ActiveBuffsHud(modifier: Modifier = Modifier) {
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current
    val activeBuffs by LocalActiveBuffs.current
    if (activeBuffs.isEmpty()) return

    // Group same buffs to show count badge if duplicate
    val grouped = activeBuffs.groupBy { it }.mapValues { it.value.size }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 12.dp),
    ) {
        grouped.forEach { (buff, count) ->
            // Color cycle: cyan, gold, magenta, violet based on key hash
            val color = when (buff.key.hashCode() % 4) {
                0 -> palette.cyan
                1 -> palette.gold
                2 -> palette.magenta
                else -> palette.violet
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(color.copy(alpha = 0.20f))
                    .border(BorderStroke(1.dp, color.copy(alpha = 0.7f)), RoundedCornerShape(15.dp)),
            ) {
                Text(
                    text = buff.glyph,
                    color = color,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
                if (count > 1) {
                    Text(
                        text = "$count",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 1.dp, bottom = 1.dp),
                    )
                }
            }
        }
    }
}
