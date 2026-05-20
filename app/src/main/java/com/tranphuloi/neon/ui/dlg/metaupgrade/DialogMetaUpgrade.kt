package com.tranphuloi.neon.ui.dlg.metaupgrade

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.LocalMetaProgression
import com.tranphuloi.neon.ui.game.meta.SkillNode
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.launch

/**
 * Wave 5 (48x) — meta progression / skill tree screen. Displays lifetime
 * mineral balance + all 15 skill nodes. Each node shows: name, description,
 * current rank / max, next-rank cost, and either an "UNLOCK" or "RANK UP"
 * button (or LOCKED indicator if prerequisite unmet).
 *
 * Layout: vertical LazyColumn grouped by tier. Tier 0 root nodes appear
 * first (always unlockable). Tier 1 nodes show prerequisite hint when locked.
 * Tier 2 endgame nodes shown last.
 */
@Composable
fun DialogMetaUpgrade(onDismiss: () -> Unit) {
    val meta = LocalMetaProgression.current
    val scope = rememberCoroutineScope()
    val balance by meta.lifetimeMinerals.collectAsState(initial = 0)
    val ranks by meta.allRanks.collectAsState(initial = emptyMap())

    LaunchedEffect(Unit) { Logger.d("DialogMetaUpgrade shown (balance=$balance, nodes=${ranks.size})") }

    com.tranphuloi.neon.common.NeonBottomSheet(
        title = "CÂY NÂNG CẤP",
        accentColor = NeonGold,
        onDismiss = {
            Logger.d("DialogMetaUpgrade: dismissed")
            onDismiss()
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Balance row (under header, right-aligned)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "♦ $balance",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Dùng khoáng vật tích lũy mua nâng cấp vĩnh viễn.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                // Round 29 — wrap content height: heightIn(max) cap so very tall
                // screens don't stretch sheet unnecessarily; short content shrinks.
                modifier = Modifier.heightIn(max = 480.dp),
            ) {
                items(items = SkillNode.values()) { node ->
                    NodeRow(node = node, ranks = ranks, balance = balance, onBuy = {
                        scope.launch {
                            meta.spendOnNode(
                                nodeKey = node.key,
                                cost = node.costForNextRank(ranks[node.key] ?: 0),
                                maxRank = node.maxRank,
                            )
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun NodeRow(
    node: SkillNode,
    ranks: Map<String, Int>,
    balance: Int,
    onBuy: () -> Unit,
) {
    val currentRank = ranks[node.key] ?: 0
    val parentRank = node.parentKey?.let { ranks[it] ?: 0 } ?: Int.MAX_VALUE
    val prereqMet = parentRank >= node.minRequiredParentRank
    val atMax = currentRank >= node.maxRank
    val nextCost = node.costForNextRank(currentRank)
    val canAfford = balance >= nextCost
    val color = when (node.tierIndex) {
        0 -> NeonCyan
        1 -> NeonMagenta
        else -> NeonGold
    }
    val rowAlpha = if (!prereqMet) 0.4f else 1f

    val glyph = nodeGlyph(node.key)
    val tierLabel = when (node.tierIndex) {
        0 -> "TIER I"
        1 -> "TIER II"
        else -> "TIER III"
    }

    // Round 30 revamp — icon + tier badge + progress bar + buy button
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f * rowAlpha))
            .border(BorderStroke(1.dp, color.copy(alpha = rowAlpha)), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        // Icon box 48dp
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.20f * rowAlpha))
                .border(BorderStroke(1.dp, color.copy(alpha = 0.65f * rowAlpha)), RoundedCornerShape(10.dp)),
        ) {
            Text(
                text = if (!prereqMet) "🔒" else glyph,
                color = color.copy(alpha = rowAlpha),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Tier badge + node name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.25f * rowAlpha))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = tierLabel,
                        color = color.copy(alpha = rowAlpha),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = node.displayName,
                    color = color.copy(alpha = rowAlpha),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Progress bar
            ProgressBarSegments(
                currentRank = currentRank,
                maxRank = node.maxRank,
                color = color.copy(alpha = rowAlpha),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = node.description,
                color = Color.White.copy(alpha = 0.80f * rowAlpha),
                fontSize = 12.sp,
            )
            if (!prereqMet) {
                Text(
                    text = "Khóa — cần cấp ${node.minRequiredParentRank} của nốt cha",
                    color = NeonViolet.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Right side: buy button or status.
        when {
            atMax -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonGold.copy(alpha = 0.25f))
                    .border(BorderStroke(1.dp, NeonGold), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("TỐI ĐA", color = NeonGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            !prereqMet -> Text("🔒", color = NeonViolet, fontSize = 18.sp)
            else -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (canAfford) color.copy(alpha = 0.30f) else Color.Transparent
                        )
                        .border(
                            BorderStroke(1.dp, color.copy(alpha = if (canAfford) 0.9f else 0.35f)),
                            RoundedCornerShape(10.dp),
                        )
                        .clickable(enabled = canAfford) { onBuy() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "♦",
                            color = if (canAfford) NeonGold else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                        )
                        Text(
                            text = "$nextCost",
                            color = if (canAfford) color else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

/** Map node key → unicode glyph icon. */
private fun nodeGlyph(key: String): String = when (key) {
    com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_HP -> "♥"
    com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_DAMAGE -> "⚔"
    com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_MAGNET -> "◉"
    "meta_shield" -> "⊞"
    com.tranphuloi.neon.ui.game.state.EffectiveStats.META_KEY_SPEED -> "⚡"
    "meta_regen" -> "✚"
    "meta_crit" -> "✦"
    "meta_lifetime" -> "★"
    "meta_shield_burst" -> "❂"
    "meta_dash" -> "⚝"
    "meta_extra_bomb" -> "◐"
    "meta_combo_keep" -> "∞"
    "meta_revive_drop" -> "♡"
    "meta_legendary_hp" -> "☆"
    "meta_legendary_dmg" -> "✪"
    else -> "?"
}

/**
 * Round 30 — visual progress bar: maxRank segments, currentRank filled.
 * Each segment is a rounded rect with a small gap between.
 */
@Composable
private fun ProgressBarSegments(currentRank: Int, maxRank: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        for (i in 0 until maxRank) {
            val filled = i < currentRank
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (filled) color else Color.White.copy(alpha = 0.12f)),
            )
        }
    }
}
