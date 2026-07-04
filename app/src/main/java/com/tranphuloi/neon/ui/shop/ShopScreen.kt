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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
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
import com.tranphuloi.neon.common.NeonBottomSheet
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
// Tab order (user pick): Tàu > Đạn > Skin > Tiêu hao > Nâng cấp > Hiển thị.
// TabBar renders in this declaration order.
private enum class ShopTab(val label: String, val accent: Color) {
    SHIP("Tàu", NeonCyan),
    BULLET("Đạn", NeonViolet),
    SKIN("Skin", NeonMagenta),
    CONSUMABLE("Tiêu hao", NeonGold),
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

    var tab by rememberSaveable { mutableStateOf(ShopTab.SHIP) }
    val grouped = ShopItem.ALL.groupBy { it.category }

    // Wave 15 — purchase-confirm bottom sheet. Every spend routes through here
    // first: a row tap builds a [PurchaseRequest] (display + the actual spend
    // captured as `confirm`) instead of deducting immediately. Khoáng is a
    // scarce, non-refundable currency + the tabs are scroll-then-tap lists, so
    // an accidental tap must not silently drain it. `remember` (not Saveable):
    // it holds a lambda + is a transient confirmation — fine to drop on config
    // change.
    var pending by remember { mutableStateOf<PurchaseRequest?>(null) }
    val requestPurchase: (PurchaseRequest) -> Unit = { pending = it }

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
                    ShopTab.SKIN -> SkinTab(
                        balance = balance, allRanks = allRanks, meta = meta, scope = scope,
                        onRequestPurchase = requestPurchase,
                    )
                    ShopTab.BULLET -> BulletTab(
                        balance = balance, allRanks = allRanks, meta = meta, scope = scope,
                        onRequestPurchase = requestPurchase,
                    )
                    ShopTab.CONSUMABLE -> CategorySection(
                        items = grouped[ShopItem.Category.CONSUMABLE].orEmpty(),
                        accent = NeonGold, balance = balance, allRanks = allRanks,
                        meta = meta, scope = scope,
                        footnote = "Tự áp dụng khi vào run — bom dư giữ lại cho run sau.",
                        onRequestPurchase = requestPurchase,
                    )
                    ShopTab.SHIP -> ShipTab(
                        balance = balance, allRanks = allRanks, meta = meta, scope = scope,
                        onRequestPurchase = requestPurchase,
                    )
                    ShopTab.UPGRADE -> MetaUpgradeNodes(
                        scope = scope, onRequestPurchase = requestPurchase,
                    )
                    ShopTab.DISPLAY -> DisplayTab(scope = scope)
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // Wave 15 — purchase-confirm sheet overlays everything when armed.
        pending?.let { req ->
            PurchaseConfirmSheet(
                request = req,
                onConfirm = {
                    req.confirm()
                    pending = null
                },
                onDismiss = { pending = null },
            )
        }
    }
}

/**
 * Wave 15 — a pending purchase awaiting confirmation. [confirm] captures the
 * actual spend (already wrapped in the caller's `scope.launch { … }`) so the
 * sheet stays decoupled from spendOnNode/spendOnStockpile signatures.
 */
internal class PurchaseRequest(
    val title: String,
    val description: String,
    val cost: Int,
    val balanceAfter: Int,
    val accent: Color,
    val confirm: () -> Unit,
)

@Composable
private fun PurchaseConfirmSheet(
    request: PurchaseRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    NeonBottomSheet(
        title = "XÁC NHẬN MUA",
        accentColor = request.accent,
        onDismiss = onDismiss,
    ) {
        Text(
            text = request.title,
            style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black),
        )
        if (request.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = request.description,
                style = TextStyle(color = Color(0xFFB0C0D0), fontSize = 13.sp),
            )
        }
        Spacer(Modifier.height(14.dp))
        ConfirmInfoLine("Giá", "${request.cost} ◇", request.accent)
        Spacer(Modifier.height(4.dp))
        ConfirmInfoLine("Số dư sau khi mua", "${request.balanceAfter} ◇", Color.White)
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.5.dp, Color(0xFF8090A0)), RoundedCornerShape(10.dp))
                    .clickable { onDismiss() }
                    .padding(vertical = 12.dp),
            ) {
                Text("Huỷ", style = TextStyle(color = Color(0xFFB0C0D0), fontSize = 14.sp, fontWeight = FontWeight.Black))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(request.accent.copy(alpha = 0.22f))
                    .border(BorderStroke(1.5.dp, request.accent), RoundedCornerShape(10.dp))
                    .neonGlow(request.accent, intensity = 0.35f, radiusFactor = 1.2f)
                    .clickable { onConfirm() }
                    .padding(vertical = 12.dp),
            ) {
                Text("Xác nhận", style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black))
            }
        }
    }
}

@Composable
private fun ConfirmInfoLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = TextStyle(color = Color(0xFF8090A0), fontSize = 13.sp))
        Text(value, style = TextStyle(color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Black))
    }
}

@Composable
private fun TabBar(current: ShopTab, onSelect: (ShopTab) -> Unit) {
    // Wave 18 — giảm padding/spacing/font để 6 tab (Tàu…Hiển thị) vừa khít màn,
    // không bị cắt tab cuối ở vị trí nghỉ; vẫn giữ horizontalScroll cho máy hẹp.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    .padding(horizontal = 10.dp, vertical = 9.dp),
            ) {
                Text(
                    text = t.label,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (selected) Color.White else t.accent.copy(alpha = 0.8f),
                        fontSize = 12.sp,
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
    onRequestPurchase: (PurchaseRequest) -> Unit,
) {
    if (items.isEmpty()) return
    items.forEach { item ->
        ShopItemRow(
            item = item, balance = balance, allRanks = allRanks,
            meta = meta, scope = scope, accent = accent,
            onRequestPurchase = onRequestPurchase,
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
    onRequestPurchase: (PurchaseRequest) -> Unit,
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
                onRequestPurchase(
                    PurchaseRequest(
                        title = item.displayName,
                        description = item.description,
                        cost = item.cost,
                        balanceAfter = balance - item.cost,
                        accent = accent,
                        confirm = {
                            scope.launch {
                                val ok = if (item.isConsumable) {
                                    meta.spendOnStockpile(item.persistKey, item.cost, item.stockpileAdd)
                                } else {
                                    meta.spendOnNode(item.persistKey, item.cost, item.maxRank)
                                }
                                Logger.d("Shop: purchase ${item.id} → $ok")
                            }
                        },
                    )
                )
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

// ─────────────────────────── bullet tab (full picker: buy + select, Wave 14a) ───────────────────────────

@Composable
private fun BulletTab(
    balance: Int,
    allRanks: Map<String, Int>,
    meta: MetaProgressionRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onRequestPurchase: (PurchaseRequest) -> Unit,
) {
    val settings = LocalSettings.current
    val preferred by settings.preferredBulletType
        .collectAsState(initial = com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL)

    // Migration (Wave 14a) — if the player's selected bullet just became
    // shop-gated (GIANT/PLASMA were free before), grant it for free so it stays
    // usable. Idempotent (grantNodeFree no-ops if owned).
    LaunchedEffect(preferred) {
        if (!ShopItem.isShopUnlocked(allRanks, preferred.shopUnlockId)) {
            val item = preferred.shopUnlockId?.let { id -> ShopItem.ALL.firstOrNull { it.id == id } }
            if (item != null) meta.grantNodeFree(item.persistKey)
        }
    }

    Text(
        text = "Chọn vũ khí chính cho run kế. Đạn 🔒 mua 1 lần rồi dùng mãi.",
        style = TextStyle(color = Color(0xFF8090A0), fontSize = 11.sp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
    com.tranphuloi.neon.ui.game.ship.laser.BulletType.entries.forEach { bullet ->
        val shopItem = bullet.shopUnlockId?.let { id -> ShopItem.ALL.firstOrNull { it.id == id } }
        val owned = ShopItem.isShopUnlocked(allRanks, bullet.shopUnlockId)
        val cost = shopItem?.cost ?: 0
        BulletRow(
            color = Color(com.tranphuloi.neon.ui.game.ship.laser.BulletTypeColorMap.argbFor(bullet)),
            glyph = bullet.glyph,
            name = bullet.displayName,
            stats = bulletDesc(bullet) + " · ST ×${bullet.damageMultiplier}",
            owned = owned,
            selected = bullet == preferred,
            cost = cost,
            canBuy = !owned && balance >= cost,
            onBuy = {
                val item = shopItem ?: return@BulletRow
                onRequestPurchase(
                    PurchaseRequest(
                        title = bullet.displayName,
                        description = bulletDesc(bullet) + " · Sát thương ×${bullet.damageMultiplier}. Mua 1 lần, dùng mãi.",
                        cost = item.cost,
                        balanceAfter = balance - item.cost,
                        accent = Color(com.tranphuloi.neon.ui.game.ship.laser.BulletTypeColorMap.argbFor(bullet)),
                        confirm = {
                            scope.launch {
                                val ok = meta.spendOnNode(item.persistKey, item.cost, item.maxRank)
                                Logger.d("Shop bullet: buy ${bullet.name} → $ok")
                            }
                        },
                    )
                )
            },
            onSelect = {
                Logger.d("Shop bullet: select ${bullet.name}")
                scope.launch { settings.setPreferredBulletType(bullet) }
            },
        )
    }
}

@Composable
private fun BulletRow(
    color: Color,
    glyph: String,
    name: String,
    stats: String,
    owned: Boolean,
    selected: Boolean,
    cost: Int,
    canBuy: Boolean,
    onBuy: () -> Unit,
    onSelect: () -> Unit,
) {
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
            .clickable(enabled = owned || canBuy) { if (owned) onSelect() else onBuy() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.22f * rowAlpha))
                    .border(BorderStroke(1.dp, color.copy(alpha = 0.6f * rowAlpha)), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = if (!owned) "🔒" else glyph, style = TextStyle(color = color.copy(alpha = rowAlpha), fontSize = 18.sp, fontWeight = FontWeight.Black))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = TextStyle(color = color.copy(alpha = rowAlpha), fontSize = 14.sp, fontWeight = FontWeight.Black))
                Text(text = stats, style = TextStyle(color = Color(0xFFB0C0D0).copy(alpha = rowAlpha), fontSize = 10.sp))
            }
        }
        when {
            selected -> Text("✓ ĐANG DÙNG", style = TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.Black))
            owned -> Text("Có sẵn", style = TextStyle(color = color.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold))
            else -> Text("$cost ◇", style = TextStyle(color = if (canBuy) color else Color(0xFF606878), fontSize = 14.sp, fontWeight = FontWeight.Black))
        }
    }
}

/** Wave 14 — mô tả tiếng Việt dễ hiểu cho từng loại đạn (hiện ở row tab Đạn). */
private fun bulletDesc(b: com.tranphuloi.neon.ui.game.ship.laser.BulletType): String =
    when (b) {
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL -> "Đạn cơ bản"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.PIERCING -> "Xuyên qua nhiều địch"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.PLASMA -> "Nổ lan vùng nhỏ khi trúng"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.FIRE -> "Gây cháy, mất máu dần"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.HOMING -> "Tự đuổi theo địch"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.BOUNCE -> "Nảy khỏi mép màn hình"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.GIANT -> "To gấp đôi, mạnh hơn"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.SMOKE -> "Toả khói làm chậm địch"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.ZIGZAG -> "Bay zigzag né dễ"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.KAMEHAMEHA -> "Tia lớn xuyên thấu tất cả"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.ATOMIC -> "Nổ AoE rộng khắp"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.SPLIT -> "Tách thành 3 mảnh"
        // Wave 16 — đạn trào phúng.
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.LOTTERY -> "Vé số — sát thương hên xui"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.FIREWORK -> "Pháo hoa — nổ chùm rộng"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.BRICK -> "Cục gạch — to, nặng, mạnh"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.BANH_MI -> "Bánh mì — xuyên nhiều địch"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.DURIAN -> "Sầu riêng — nổ mùi AoE"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.HEART -> "Tim — tự đuổi theo địch"
        // Wave 18 — batch 3.
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.BUBBLE_TEA -> "Trà sữa — nổ vùng + văng trân châu"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.FISH_SAUCE -> "Nước mắm — ăn mòn mất máu dần"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.SANDAL -> "Dép lào — boomerang đánh 2 chiều"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.QR_CODE -> "Mã QR — quét địch: chậm + choáng"
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.LIGHTNING -> "Sét chain — lan qua 3 địch gần nhau"
    }

/** Wave 14 — short human descriptor cho tàu, suy từ stat profile (hợp lý cho 23 tàu). */
private fun shipDesc(s: ShipShape): String = when {
    s.hpMul >= 1.3f -> "Trâu bò — chịu đòn cực tốt, hơi chậm"
    s.damageMul >= 1.25f -> "Sát thủ — sát thương cao"
    s.speedMul >= 1.2f -> "Nhanh nhẹn — né tốt, luồn lách"
    s.hpMul >= 1.15f -> "Cứng cáp — nhiều máu hơn"
    s.speedMul >= 1.1f -> "Linh hoạt — cơ động khá"
    s.damageMul >= 1.1f -> "Hơi mạnh — sát thương khá"
    else -> "Cân bằng — dễ chơi cho người mới"
}

// ─────────────────────────── skin tab (buy + select, Wave 13 #1) ───────────────────────────

@Composable
private fun SkinTab(
    balance: Int,
    allRanks: Map<String, Int>,
    meta: MetaProgressionRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onRequestPurchase: (PurchaseRequest) -> Unit,
) {
    val settings = LocalSettings.current
    val selectedSkin by settings.shipSkin.collectAsState(initial = com.tranphuloi.neon.data.ShipSkin.AURA_CYAN)
    Text(
        text = "Chọn hào quang tàu. Skin trả phí mua 1 lần, dùng mãi.",
        style = TextStyle(color = Color(0xFF8090A0), fontSize = 11.sp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
    com.tranphuloi.neon.data.ShipSkin.entries.forEach { skin ->
        val shopItem = skin.shopUnlockId?.let { id -> ShopItem.ALL.firstOrNull { it.id == id } }
        val owned = ShopItem.isShopUnlocked(allRanks, skin.shopUnlockId)
        val cost = shopItem?.cost ?: 0
        SkinRow(
            color = Color(skin.glowColorHex),
            name = skin.displayName,
            owned = owned,
            selected = skin == selectedSkin,
            cost = cost,
            canBuy = !owned && balance >= cost,
            onBuy = {
                val item = shopItem ?: return@SkinRow
                onRequestPurchase(
                    PurchaseRequest(
                        title = "Hào quang ${skin.displayName}",
                        description = "Đổi màu hào quang tàu — chỉ làm đẹp, không đổi chỉ số.",
                        cost = item.cost,
                        balanceAfter = balance - item.cost,
                        accent = Color(skin.glowColorHex),
                        confirm = {
                            scope.launch {
                                val ok = meta.spendOnNode(item.persistKey, item.cost, item.maxRank)
                                Logger.d("Shop skin: buy ${skin.name} → $ok")
                            }
                        },
                    )
                )
            },
            onSelect = {
                Logger.d("Shop skin: select ${skin.name}")
                scope.launch { settings.setShipSkin(skin) }
            },
        )
    }
}

@Composable
private fun SkinRow(
    color: Color,
    name: String,
    owned: Boolean,
    selected: Boolean,
    cost: Int,
    canBuy: Boolean,
    onBuy: () -> Unit,
    onSelect: () -> Unit,
) {
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
            .clickable(enabled = owned || canBuy) { if (owned) onSelect() else onBuy() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Aura swatch — a filled glow circle in the skin colour.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(color.copy(alpha = rowAlpha))
                    .neonGlow(color, intensity = 0.5f, radiusFactor = 1.4f),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Hào quang $name",
                    style = TextStyle(color = color.copy(alpha = rowAlpha), fontSize = 14.sp, fontWeight = FontWeight.Black),
                )
                Text(
                    text = "Đổi màu hào quang tàu — chỉ làm đẹp",
                    style = TextStyle(color = Color(0xFFB0C0D0).copy(alpha = rowAlpha), fontSize = 10.sp),
                )
            }
        }
        when {
            selected -> Text("✓ ĐANG DÙNG", style = TextStyle(color = color, fontSize = 11.sp, fontWeight = FontWeight.Black))
            owned -> Text("Đã sở hữu", style = TextStyle(color = color.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold))
            else -> Text(
                text = "$cost ◇",
                style = TextStyle(color = if (canBuy) color else Color(0xFF606878), fontSize = 14.sp, fontWeight = FontWeight.Black),
            )
        }
    }
}

// ─────────────────────────── ship tab ───────────────────────────

@Composable
private fun ShipTab(
    balance: Int,
    allRanks: Map<String, Int>,
    meta: MetaProgressionRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onRequestPurchase: (PurchaseRequest) -> Unit,
) {
    val settings = LocalSettings.current
    val selectedShape by settings.selectedShipShape.collectAsState(initial = ShipShape.FIGHTER)
    val discountRank = allRanks[EffectiveStats.META_KEY_SHIP_UNLOCK_DISCOUNT] ?: 0
    // Task 03 — XP mỗi tàu để hiển thị level + bonus (chỉ ảnh hưởng survivability).
    val allShipXp by meta.allShipXp.collectAsState(initial = emptyMap())

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
            shipXp = allShipXp[shape.key] ?: 0,
            cost = ShipShopLogic.effectiveCost(shape, discountRank),
            discounted = discountRank > 0 && !ShipShopLogic.isFree(shape),
            canBuy = ShipShopLogic.canBuy(balance, shape, allRanks, discountRank),
            onBuy = {
                val price = ShipShopLogic.effectiveCost(shape, discountRank)
                onRequestPurchase(
                    PurchaseRequest(
                        title = shape.displayName,
                        description = shipDesc(shape) +
                            " · Máu ×${shape.hpMul} · Tốc ×${shape.speedMul} · ST ×${shape.damageMul}",
                        cost = price,
                        balanceAfter = balance - price,
                        accent = Color(ShipShapeColorMap.argbFor(shape)),
                        confirm = {
                            scope.launch {
                                val ok = meta.spendOnNode(
                                    ShipShopLogic.persistKey(shape), price, maxRank = 1,
                                )
                                Logger.d("Shop ship: buy ${shape.key} → $ok")
                            }
                        },
                    )
                )
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
    shipXp: Int = 0,
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
                // Wave 14 — short human descriptor from stat profile.
                Text(
                    text = shipDesc(shape),
                    style = TextStyle(color = color.copy(alpha = rowAlpha * 0.85f), fontSize = 10.sp),
                )
                Text(
                    text = "Máu ×${shape.hpMul} · Tốc ×${shape.speedMul} · ST ×${shape.damageMul}",
                    style = TextStyle(color = Color(0xFFB0C0D0).copy(alpha = rowAlpha), fontSize = 10.sp),
                )
                // Task 03 — level + XP (chỉ tàu sở hữu; bonus +HP theo cấp).
                if (owned) {
                    val lvl = com.tranphuloi.neon.ui.game.ship.shape.ShipXpLevels.levelForXp(shipXp)
                    val bonusPct = ((com.tranphuloi.neon.ui.game.ship.shape.ShipXpLevels.hpBonusMulForLevel(lvl) - 1f) * 100f).toInt()
                    val toNext = com.tranphuloi.neon.ui.game.ship.shape.ShipXpLevels.xpToNextLevel(shipXp)
                    val progress = com.tranphuloi.neon.ui.game.ship.shape.ShipXpLevels.progressInLevel(shipXp)
                    val maxed = lvl >= com.tranphuloi.neon.ui.game.ship.shape.ShipXpLevels.MAX_LEVEL
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (maxed) {
                            "Lv$lvl (MAX) · +$bonusPct% HP"
                        } else {
                            "Lv$lvl · +$bonusPct% HP · còn ${toNext} XP"
                        },
                        style = TextStyle(color = NeonGold.copy(alpha = rowAlpha), fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    )
                    // Thanh tiến độ XP mảnh.
                    Box(
                        Modifier
                            .padding(top = 2.dp)
                            .fillMaxWidth(0.6f)
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.15f * rowAlpha)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(if (maxed) 1f else progress)
                                .fillMaxHeight()
                                .background(NeonGold.copy(alpha = rowAlpha)),
                        )
                    }
                }
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
