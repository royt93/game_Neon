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
import androidx.compose.foundation.layout.padding
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

    Card(
        backgroundColor = NeonBgMid,
        border = BorderStroke(2.dp, NeonGold),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.neonGlow(color = NeonGold, intensity = 0.45f, radiusFactor = 1.25f)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CÂY NÂNG CẤP",
                    color = NeonGold,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.weight(1f),
                )
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
                modifier = Modifier.height(420.dp),
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
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    Logger.d("DialogMetaUpgrade: Close pressed")
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("ĐÓNG", color = NeonCyan, fontWeight = FontWeight.Bold)
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f * rowAlpha))
            .border(BorderStroke(1.dp, color.copy(alpha = rowAlpha)), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = node.displayName,
                    color = color.copy(alpha = rowAlpha),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "[$currentRank/${node.maxRank}]",
                    color = Color.White.copy(alpha = 0.75f * rowAlpha),
                    fontSize = 13.sp,
                )
            }
            Text(
                text = node.description,
                color = Color.White.copy(alpha = 0.75f * rowAlpha),
                fontSize = 13.sp,
            )
            if (!prereqMet) {
                Text(
                    text = "Khóa — cần cấp ${node.minRequiredParentRank} của nốt cha",
                    color = NeonViolet.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                )
            }
        }
        // Right side: buy button or status.
        when {
            atMax -> Text("TỐI ĐA", color = NeonGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            !prereqMet -> Text("●", color = NeonViolet, fontSize = 16.sp)
            else -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (canAfford) color.copy(alpha = 0.25f) else Color.Transparent
                        )
                        .border(
                            BorderStroke(1.dp, color.copy(alpha = if (canAfford) 0.9f else 0.35f)),
                            RoundedCornerShape(10.dp),
                        )
                        .clickable(enabled = canAfford) { onBuy() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "♦ $nextCost",
                        color = if (canAfford) color else Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
