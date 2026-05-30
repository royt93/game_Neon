package com.tranphuloi.neon.ui.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonStarfieldBackground
import com.tranphuloi.neon.common.NeonViolet
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.data.ColorBlindMode
import com.tranphuloi.neon.data.LocalMetaProgression
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.data.MetaProgressionRepository
import com.tranphuloi.neon.data.ShopItem
import com.tranphuloi.neon.ui.game.ship.shape.ShipShape
import com.tranphuloi.neon.ui.game.ship.shape.ShipShapeColorMap
import com.tranphuloi.neon.ui.game.ship.shape.ShipShopLogic
import com.tranphuloi.neon.ui.game.state.EffectiveStats
import com.tranphuloi.neon.ui.game.world.drawShipVector
import com.tranphuloi.neon.utils.Logger
import kotlinx.coroutines.launch

/**
 * Wave 13a — Shop hub. Single place to spend minerals, organised by tabs:
 *   - **Skin / Đạn / Tiêu hao**: the round-12 catalog (skins, bullet unlocks,
 *     consumables) routed via `spendOnNode` / `spendOnStockpile`.
 *   - **Tàu**: ship purchase (was a threshold-gate in Bách Khoa → moved here +
 *     converted to a real purchase). See [ShipShopLogic]. A migration grants
 *     the currently-selected ship for free on first load so nobody loses their
 *     active ship.
 *
 * (Nâng cấp / Hiển thị tabs land in the next 13a slices.)
 */
private enum class ShopTab(val label: String, val accent: Color) {
    SKIN("Skin", NeonMagenta),
    BULLET("Đạn", NeonViolet),
    CONSUMABLE("Tiêu hao", NeonGold),
    SHIP("Tàu", NeonCyan),
    // Wave 13a (slice C) — skill-tree (permanent stat upgrades), moved from menu.
    UPGRADE("Nâng cấp", NeonCyan),
    // Wave 13a (slice E) — display/accessibility (ColorBlindMode), free.
    DISPLAY("Hiển thị", NeonCyan),
}

@Composable
fun ShopScreen(onBack: () -> Unit) {
    val meta = LocalMetaProgression.current
    val balance by meta.lifetimeMinerals.collectAsState(initial = 0)
    val allRanks by meta.allRanks.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable { mutableStateOf(ShopTab.SKIN) }
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
            BalanceCard(
                balance = balance,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            TabBar(current = tab, onSelect = { tab = it })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (tab) {
                    ShopTab.SKIN -> CategorySection(
                        items = grouped[ShopItem.Category.SHIP_SKIN_UNLOCK].orEmpty(),
                        accent = NeonMagenta, balance = balance, allRanks = allRanks,
                        meta = meta, scope = scope,
                        footnote = "Skin đã mua sẽ mở khoá để chọn trong Cài đặt.",
                    )
                    ShopTab.BULLET -> CategorySection(
                        items = grouped[ShopItem.Category.BULLET_TYPE_UNLOCK].orEmpty(),
                        accent = NeonViolet, balance = balance, allRanks = allRanks,
                        meta = meta, scope = scope,
                        footnote = "Đạn đã mua sẽ mở khoá để chọn trong Trang bị.",
                    )
                    ShopTab.CONSUMABLE -> CategorySection(
                        items = grouped[ShopItem.Category.CONSUMABLE].orEmpty(),
                        accent = NeonGold, balance = balance, allRanks = allRanks,
                        meta = meta, scope = scope,
                        footnote = "Tự áp dụng khi vào run — bom dư giữ lại cho run sau.",
                    )
                    ShopTab.SHIP -> ShipTab(
                        balance = balance, allRanks = allRanks, meta = meta, scope = scope,
                    )
                    ShopTab.UPGRADE -> MetaUpgradeNodes(scope = scope)
                    ShopTab.DISPLAY -> DisplayTab(scope = scope)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TabBar(current: ShopTab, onSelect: (ShopTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ShopTab.entries.forEach { t ->
            val selected = t == current
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) t.accent.copy(alpha = 0.25f) else Color.Transparent)
                    .border(
                        BorderStroke(1.5.dp, if (selected) t.accent else t.accent.copy(alpha = 0.4f)),
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(t) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    text = t.label,
                    style = TextStyle(
                        color = if (selected) Color.White else t.accent.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    ),
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
            style = TextStyle(color = NeonGold, fontSize = 22.sp, fontWeight = FontWeight.Black),
        )
    }
}

// ─────────────────────────── catalog tabs (skin / bullet / consumable) ───────────────────────────

@Composable
private fun CategorySection(
    items: List<ShopItem>,
    accent: Color,
    balance: Int,
    allRanks: Map<String, Int>,
    meta: MetaProgressionRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    footnote: String,
) {
    if (items.isEmpty()) return
    items.forEach { item ->
        ShopItemRow(
            item = item, balance = balance, allRanks = allRanks,
            meta = meta, scope = scope, accent = accent,
        )
    }
    Text(
        text = "Ghi chú: $footnote",
        style = TextStyle(color = Color(0xFF8090A0), fontSize = 11.sp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
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
    // subscriptions when isConsumable flips). Rank items just ignore the value.
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
        Column(modifier = Modifier.padding(end = 8.dp).width(0.dp).weight(1f)) {
            Text(
                text = item.displayName,
                style = TextStyle(color = Color.White.copy(alpha = rowAlpha), fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = item.description,
                style = TextStyle(color = Color(0xFFB0C0D0).copy(alpha = rowAlpha), fontSize = 11.sp),
            )
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
                color = if (isMaxed || affordable) accent else Color(0xFF606878),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
            ),
        )
    }
}

// ─────────────────────────── display / accessibility tab ───────────────────────────

@Composable
private fun DisplayTab(scope: kotlinx.coroutines.CoroutineScope) {
    val settings = LocalSettings.current
    val mode by settings.colorBlindMode.collectAsState(initial = ColorBlindMode.NORMAL)
    Text(
        text = "Chế độ màu (trợ năng — miễn phí)",
        style = TextStyle(color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Black),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
    ColorBlindMode.entries.forEach { m ->
        val selected = m == mode
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) NeonCyan.copy(alpha = 0.18f) else Color(0xFF161C28))
                .border(
                    BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) NeonCyan else NeonCyan.copy(alpha = 0.3f)),
                    RoundedCornerShape(8.dp),
                )
                .clickable {
                    Logger.d("Shop display: colorBlindMode=${m.name}")
                    scope.launch { settings.setColorBlindMode(m) }
                }
                .padding(12.dp),
        ) {
            Text(
                text = m.displayName,
                style = TextStyle(color = if (selected) Color.White else Color(0xFFB0C0D0), fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
            if (selected) {
                Text("✓", style = TextStyle(color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Black))
            }
        }
    }
    Text(
        text = "Ghi chú: Tuỳ chỉnh hiển thị cho người khó phân biệt màu. Không tốn khoáng.",
        style = TextStyle(color = Color(0xFF8090A0), fontSize = 11.sp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

private fun inlineStateLine(item: ShopItem, rank: Int, stockpile: Int): String = when {
    item.isConsumable -> if (stockpile > 0) "Đang có: $stockpile" else ""
    item.maxRank > 1 -> if (rank > 0) "Rank $rank/${item.maxRank}" else ""
    else -> if (rank > 0) "✓ Đã mua" else ""
}

// ─────────────────────────── ship tab ───────────────────────────

@Composable
private fun ShipTab(
    balance: Int,
    allRanks: Map<String, Int>,
    meta: MetaProgressionRepository,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val settings = LocalSettings.current
    val selectedShape by settings.selectedShipShape.collectAsState(initial = ShipShape.FIGHTER)
    val discountRank = allRanks[EffectiveStats.META_KEY_SHIP_UNLOCK_DISCOUNT] ?: 0

    // Migration (threshold-gate → purchase): grant the currently-selected ship
    // for free so it stays usable. Idempotent (grantNodeFree no-ops if owned),
    // safe on every (re)entry. FIGHTER is free → no record needed.
    LaunchedEffect(selectedShape) {
        ShipShopLogic.migrationGrantKeys(selectedShape, allRanks).forEach { key ->
            // key already has the `shop_ship_` form (= node key sans prefix).
            meta.grantNodeFree(key)
        }
    }

    if (discountRank > 0) {
        Text(
            text = "✦ Giảm giá tàu -${discountRank * 10}% (TỔ HỢP HÀNG KHÔNG cấp $discountRank)",
            style = TextStyle(color = NeonViolet, fontSize = 11.sp, fontWeight = FontWeight.Black),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }

    ShipShape.entries.forEach { shape ->
        ShipRow(
            shape = shape,
            owned = ShipShopLogic.isOwned(shape, allRanks),
            selected = shape == selectedShape,
            cost = ShipShopLogic.effectiveCost(shape, discountRank),
            discounted = discountRank > 0 && !ShipShopLogic.isFree(shape),
            canBuy = ShipShopLogic.canBuy(balance, shape, allRanks, discountRank),
            onBuy = {
                scope.launch {
                    val ok = meta.spendOnNode(
                        ShipShopLogic.persistKey(shape),
                        ShipShopLogic.effectiveCost(shape, discountRank),
                        maxRank = 1,
                    )
                    Logger.d("Shop ship: buy ${shape.key} → $ok")
                }
            },
            onSelect = {
                Logger.d("Shop ship: select ${shape.key}")
                scope.launch { settings.setSelectedShipShape(shape) }
            },
        )
    }
}

@Composable
private fun ShipRow(
    shape: ShipShape,
    owned: Boolean,
    selected: Boolean,
    cost: Int,
    discounted: Boolean,
    canBuy: Boolean,
    onBuy: () -> Unit,
    onSelect: () -> Unit,
) {
    val color = Color(ShipShapeColorMap.argbFor(shape))
    val rowAlpha = if (owned || canBuy) 1f else 0.5f
    val borderColor = when {
        selected -> color
        owned -> color.copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.18f)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161C28))
            .border(BorderStroke(if (selected) 2.dp else 1.dp, borderColor), RoundedCornerShape(10.dp))
            .let { if (selected) it.neonGlow(color, intensity = 0.35f, radiusFactor = 1.3f) else it }
            // Owned → tap selects; not owned → tap buys (if affordable).
            .clickable(enabled = owned || canBuy) { if (owned) onSelect() else onBuy() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(BorderStroke(1.dp, color.copy(alpha = rowAlpha)), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(36.dp)) {
                    drawShipVector(color = color.copy(alpha = rowAlpha), laserBoosterEnabled = false, shape = shape)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shape.displayName,
                    style = TextStyle(color = color.copy(alpha = rowAlpha), fontSize = 14.sp, fontWeight = FontWeight.Black),
                )
                Text(
                    text = "Máu ×${shape.hpMul} · Tốc ×${shape.speedMul} · ST ×${shape.damageMul}",
                    style = TextStyle(color = Color(0xFFB0C0D0).copy(alpha = rowAlpha), fontSize = 10.sp),
                )
            }
        }
        // Right-side state: selected / owned / price.
        when {
            selected -> Text("✓ ĐANG DÙNG", style = TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.Black))
            owned -> Text("Đã sở hữu", style = TextStyle(color = color.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold))
            else -> Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$cost ◇",
                    style = TextStyle(color = if (canBuy) color else Color(0xFF606878), fontSize = 14.sp, fontWeight = FontWeight.Black),
                )
                if (discounted) {
                    Text(
                        text = "gốc ${shape.unlockMinerals}",
                        style = TextStyle(color = Color(0xFF606878), fontSize = 9.sp),
                    )
                }
            }
        }
    }
}
