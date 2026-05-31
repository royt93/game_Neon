package com.tranphuloi.neon.ui.shop

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.data.LocalMetaProgression
import com.tranphuloi.neon.ui.game.meta.SkillNode
import com.tranphuloi.neon.ui.game.state.EffectiveStats
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.launch

/**
 * Wave 13a (slice C) — skill-tree node list, extracted from the old
 * `DialogMetaUpgrade` so it can live inside the Shop "Nâng cấp" tab.
 *
 * Renders via a plain `Column` forEach (NOT a LazyColumn) because the Shop tab
 * host is already inside a `verticalScroll` — nesting a same-axis lazy list
 * there crashes ("infinite max height"). Node count (~17) is small, so eager
 * layout is fine. The balance row is omitted (the Shop screen shows balance).
 */
@Composable
internal fun MetaUpgradeNodes(
    scope: kotlinx.coroutines.CoroutineScope,
    onRequestPurchase: (PurchaseRequest) -> Unit,
) {
    val meta = LocalMetaProgression.current
    val balance by meta.lifetimeMinerals.collectAsState(initial = 0)
    val ranks by meta.allRanks.collectAsState(initial = emptyMap())

    Text(
        text = "Nâng cấp vĩnh viễn — áp dụng cho mọi lần chơi sau.",
        color = Color.White.copy(alpha = 0.75f),
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Text(
        text = "NỀN TẢNG = mở khóa sẵn · NHÁNH = cần cấp ≥2 nốt cha · TỐI THƯỢNG = endgame",
        color = Color.White.copy(alpha = 0.55f),
        fontSize = 10.sp,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(modifier = Modifier.height(4.dp))
    SkillNode.values().forEach { node ->
        NodeRow(node = node, ranks = ranks, balance = balance, onBuy = {
            val currentRank = ranks[node.key] ?: 0
            val price = node.costForNextRank(currentRank)
            onRequestPurchase(
                PurchaseRequest(
                    title = node.displayName,
                    description = node.description,
                    cost = price,
                    balanceAfter = balance - price,
                    accent = NeonCyan,
                    confirm = {
                        scope.launch {
                            meta.spendOnNode(
                                nodeKey = node.key,
                                cost = price,
                                maxRank = node.maxRank,
                            )
                            Logger.d("Shop upgrade: buy ${node.key}")
                        }
                    },
                )
            )
        })
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
        0 -> "NỀN TẢNG"
        1 -> "NHÁNH"
        else -> "TỐI THƯỢNG"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f * rowAlpha))
            .border(BorderStroke(1.dp, color.copy(alpha = rowAlpha)), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.20f * rowAlpha))
                .border(BorderStroke(1.dp, color.copy(alpha = 0.65f * rowAlpha)), RoundedCornerShape(10.dp)),
        ) {
            Text(
                text = if (!prereqMet) "🔒" else glyph,
                color = color.copy(alpha = rowAlpha),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.25f * rowAlpha))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(text = tierLabel, color = color.copy(alpha = rowAlpha), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(text = node.displayName, color = color.copy(alpha = rowAlpha), fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Cấp $currentRank/${node.maxRank}",
                    color = color.copy(alpha = 0.85f * rowAlpha),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                ProgressBarSegments(currentRank = currentRank, maxRank = node.maxRank, color = color.copy(alpha = rowAlpha))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = node.description, color = Color.White.copy(alpha = 0.85f * rowAlpha), fontSize = 12.sp)
            if (!prereqMet) {
                Text(
                    text = "🔒 Khoá — cần cấp ${node.minRequiredParentRank} của nốt nền",
                    color = NeonViolet.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                )
            } else if (!atMax) {
                Text(
                    text = "Mua tiếp → cấp ${currentRank + 1} · giá $nextCost khoáng",
                    color = if (canAfford) color.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        when {
            atMax -> Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonGold.copy(alpha = 0.25f))
                    .border(BorderStroke(1.dp, NeonGold), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("TỐI ĐA", color = NeonGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            !prereqMet -> Text("🔒", color = NeonViolet, fontSize = 18.sp)
            else -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (canAfford) color.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.05f))
                    .border(
                        BorderStroke(1.5.dp, color.copy(alpha = if (canAfford) 0.95f else 0.30f)),
                        RoundedCornerShape(10.dp),
                    )
                    .clickable(enabled = canAfford) { onBuy() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (canAfford) "MUA" else "Thiếu",
                        color = if (canAfford) color else NeonRedAlert.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "♦ $nextCost",
                        color = if (canAfford) NeonGold else Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

private fun nodeGlyph(key: String): String = when (key) {
    EffectiveStats.META_KEY_HP -> "♥"
    EffectiveStats.META_KEY_DAMAGE -> "⚔"
    EffectiveStats.META_KEY_MAGNET -> "◉"
    EffectiveStats.META_KEY_SHIELD -> "⊞"
    EffectiveStats.META_KEY_SPEED -> "⚡"
    EffectiveStats.META_KEY_REGEN -> "✚"
    EffectiveStats.META_KEY_CRIT -> "✦"
    EffectiveStats.META_KEY_LIFETIME -> "★"
    EffectiveStats.META_KEY_SHIELD_BURST -> "❂"
    EffectiveStats.META_KEY_DASH -> "⚝"
    EffectiveStats.META_KEY_EXTRA_BOMB -> "◐"
    EffectiveStats.META_KEY_COMBO_KEEP -> "∞"
    EffectiveStats.META_KEY_REVIVE_DROP -> "♡"
    EffectiveStats.META_KEY_LEGENDARY_HP -> "☆"
    EffectiveStats.META_KEY_LEGENDARY_DMG -> "✪"
    EffectiveStats.META_KEY_SHIP_UNLOCK_DISCOUNT -> "◈"
    EffectiveStats.META_KEY_BULLET_DURATION -> "⏲"
    else -> "?"
}

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
