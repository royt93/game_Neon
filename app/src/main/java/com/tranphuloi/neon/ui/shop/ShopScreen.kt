package com.tranphuloi.neon.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tranphuloi.neon.common.NeonActionBar
import com.tranphuloi.neon.common.NeonBgDeep
import com.tranphuloi.neon.common.NeonBgEdge
import com.tranphuloi.neon.common.NeonBgMid
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonStarfieldBackground
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.data.LocalMetaProgression
import com.tranphuloi.neon.data.MetaProgressionRepository
import com.tranphuloi.neon.data.ShopItem
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.launch

/**
 * Wave 12 round 3 — Shop / Economy screen. Purchase + persistence + the
 * unlock-gate consumers it feeds.
 *
 * Persistence routes per category:
 *   - SHIP_SKIN_UNLOCK / BULLET_TYPE_UNLOCK → `spendOnNode` with key
 *     `shop_<id>`. Stores rank 0..maxRank (1). Atomic deduction. Consumed by
 *     DialogSettings (skins) + DialogLoadoutPicker (bullets) via
 *     `ShopItem.isShopUnlocked`.
 *   - CONSUMABLE → `spendOnStockpile` with key `shop_<id>` under
 *     STOCKPILE_PREFIX. Cumulative stock count; consumed at run start in
 *     `rememberGameState` (bombs fold into starting count, revive granted at
 *     spawn) via `consumeStockpile`.
 *
 * PERMANENT_BUFF was dropped in round 3 — the skill-tree (DialogMetaUpgrade)
 * owns the stat-buff economy.
 */
@Composable
fun ShopScreen(onBack: () -> Unit) {
    val meta = LocalMetaProgression.current
    val balance by meta.lifetimeMinerals.collectAsState(initial = 0)
    val allRanks by meta.allRanks.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()

    val grouped = ShopItem.ALL.groupBy { it.category }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NeonBgEdge, NeonBgMid, NeonBgDeep))
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        NeonStarfieldBackground(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {
            NeonActionBar(
                title = "CỬA HÀNG",
                titleColor = NeonGold,
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BalanceCard(balance = balance)

                CategorySection(
                    title = "Skin tàu",
                    accent = NeonMagenta,
                    items = grouped[ShopItem.Category.SHIP_SKIN_UNLOCK].orEmpty(),
                    balance = balance,
                    allRanks = allRanks,
                    meta = meta,
                    scope = scope,
                )
                CategorySection(
                    title = "Mở khóa đạn",
                    accent = NeonViolet,
                    items = grouped[ShopItem.Category.BULLET_TYPE_UNLOCK].orEmpty(),
                    balance = balance,
                    allRanks = allRanks,
                    meta = meta,
                    scope = scope,
                )
                CategorySection(
                    title = "Đồ tiêu hao",
                    accent = NeonGold,
                    items = grouped[ShopItem.Category.CONSUMABLE].orEmpty(),
                    balance = balance,
                    allRanks = allRanks,
                    meta = meta,
                    scope = scope,
                )

                Text(
                    text = "Ghi chú: Mua skin để mở khóa trong Cài đặt, mua đạn để mở khóa trong Trang bị. Đồ tiêu hao (bom / hồi sinh) tự áp dụng khi vào run — bom dư giữ lại cho run sau.",
                    style = TextStyle(color = Color(0xFF8090A0), fontSize = 11.sp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E1320))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Số dư khoáng sản",
            style = TextStyle(color = Color(0xFFB0C0D0), fontSize = 14.sp),
        )
        Text(
            text = balance.toString(),
            style = TextStyle(
                color = NeonGold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            ),
        )
    }
}

@Composable
private fun CategorySection(
    title: String,
    accent: Color,
    items: List<ShopItem>,
    balance: Int,
    allRanks: Map<String, Int>,
    meta: MetaProgressionRepository,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    if (items.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E1320))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        items.forEach { item ->
            ShopItemRow(
                item = item,
                balance = balance,
                allRanks = allRanks,
                meta = meta,
                scope = scope,
                accent = accent,
            )
        }
    }
}

@Composable
private fun ShopItemRow(
    item: ShopItem,
    balance: Int,
    allRanks: Map<String, Int>,
    meta: MetaProgressionRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    accent: Color,
) {
    // Always collect stockpile flow — Compose state slots must have stable
    // structure across recomposition (a branched `collectAsState` would leak
    // subscriptions when isConsumable flips). Rank items just ignore the
    // value (always 0 for their key).
    val stockpile by meta.stockpileCount(item.persistKey).collectAsState(initial = 0)
    val rank = allRanks[item.persistKey] ?: 0

    val isMaxed = !item.isConsumable && rank >= item.maxRank
    val affordable = balance >= item.cost && !isMaxed
    val rowAlpha = if (affordable || isMaxed) 1f else 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF161C28))
            .clickable(enabled = affordable) {
                scope.launch {
                    val ok = if (item.isConsumable) {
                        meta.spendOnStockpile(item.persistKey, item.cost, item.stockpileAdd)
                    } else {
                        meta.spendOnNode(item.persistKey, item.cost, item.maxRank)
                    }
                    Logger.d("Shop: purchase ${item.id} → $ok")
                }
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.padding(end = 8.dp)) {
            Text(
                text = item.displayName,
                style = TextStyle(
                    color = Color.White.copy(alpha = rowAlpha),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = item.description,
                style = TextStyle(
                    color = Color(0xFFB0C0D0).copy(alpha = rowAlpha),
                    fontSize = 11.sp,
                ),
            )
            // Inline state line — empty if untouched, otherwise shows
            // current purchase state per category.
            val stateLine = inlineStateLine(item, rank, stockpile)
            if (stateLine.isNotEmpty()) {
                Text(
                    text = stateLine,
                    style = TextStyle(color = accent.copy(alpha = rowAlpha * 0.85f), fontSize = 10.sp),
                )
            }
        }
        Text(
            text = if (isMaxed) "MAX" else "${item.cost} ◇",
            style = TextStyle(
                color = when {
                    isMaxed -> accent
                    affordable -> accent
                    else -> Color(0xFF606878)
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            ),
        )
    }
}

private fun inlineStateLine(item: ShopItem, rank: Int, stockpile: Int): String = when {
    item.isConsumable -> if (stockpile > 0) "Đang có: $stockpile" else ""
    item.maxRank > 1 -> if (rank > 0) "Rank $rank/${item.maxRank}" else ""
    else -> if (rank > 0) "✓ Đã mua" else ""
}
