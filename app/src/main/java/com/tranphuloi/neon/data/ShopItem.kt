package com.tranphuloi.neon.data

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

/**
 * Wave 12 — Shop / Economy.
 *
 * Permanent unlock items the player can purchase with lifetime minerals
 * (accumulated via MetaProgressionRepository.lifetimeMinerals). Each item
 * stores its purchase state in a separate DataStore key under
 * `MetaProgressionRepository.shopDataStore` (see ShopRepository).
 *
 * Categories:
 *   - **SHIP_SKIN_UNLOCK**: cosmetic ship aura. Purchase gates the skin in
 *     DialogSettings (locked until bought). See [ShipSkin.shopUnlockId].
 *   - **BULLET_TYPE_UNLOCK**: unlock a BulletType for the Loadout picker
 *     (locked in DialogLoadoutPicker until bought). See [BulletType.shopUnlockId].
 *   - **CONSUMABLE**: stockpile consumed at run start (bombs fold into the
 *     run's starting count, a revive token is granted at spawn). Stockpile
 *     lives in MetaProgressionRepository alongside lifetimeMinerals.
 *
 * Round-history:
 *   - Round 1: scaffolding UI (purchase stubbed).
 *   - Round 2: real purchase + persistence (spendOnNode / spendOnStockpile).
 *   - Round 3: unlock-gate consumers wired (skins, bullets, consumables).
 *     **PERMANENT_BUFF dropped** — it duplicated the skill-tree
 *     (GIÁP CỨNG / HỎA LỰC / HỐ HẤP DẪN in DialogMetaUpgrade), which remains
 *     the single stat-buff economy. Shop now sells only cosmetics, bullet
 *     unlocks, and consumables.
 */
@Immutable
@Keep
data class ShopItem(
    val id: String,
    val displayName: String,
    val description: String,
    val cost: Int,                 // lifetime minerals
    val category: Category,
    /** Optional: max purchase count for rank-up items (e.g., 5 ranks of +HP). */
    val maxRank: Int = 1,
    /**
     * Wave 12 round 2 — for CONSUMABLE items, how many units one purchase
     * adds to the stockpile (e.g., "smartbomb_pack_3" → addAmount=3). Ignored
     * for non-consumable categories.
     */
    val stockpileAdd: Int = 1,
) {
    /**
     * DataStore namespace key. Rank-based items live under `node_shop_*`
     * (reusing skill-tree node infrastructure); CONSUMABLE under
     * `stockpile_shop_*`. The `shop_` infix prevents collision with future
     * skill-tree node ids that might pick a clashing string.
     */
    val persistKey: String get() = "shop_$id"

    val isConsumable: Boolean get() = category == Category.CONSUMABLE

    @Keep
    enum class Category {
        SHIP_SKIN_UNLOCK,
        BULLET_TYPE_UNLOCK,
        CONSUMABLE,
    }

    companion object {
        /**
         * Round 3 (Wave 12) — stockpile persistKeys consumed by gameplay at run
         * start. These MUST equal the matching item's [persistKey]; pinned by
         * `ShopItemCatalogTest` so a rename can't silently break consumption.
         */
        const val SMARTBOMB_STOCKPILE_KEY = "shop_smartbomb_pack_3"
        const val REVIVE_STOCKPILE_KEY = "shop_revive_pack_1"

        // Wave 14a Round 2 — 6 new "buff 1 run" consumable stockpile keys
        // (consumed at run start by rememberGameState).
        const val X2_MINERALS_KEY = "shop_buff_x2_minerals"
        const val START_SHIELD_KEY = "shop_buff_start_shield"
        const val X2_SCORE_KEY = "shop_buff_x2_score"
        const val COMBO_KEEP_KEY = "shop_buff_combo_keep"
        const val MAGNET_XL_KEY = "shop_buff_magnet_xl"
        const val RAPID_FIRE_KEY = "shop_buff_rapid_fire"

        /**
         * Round 3 (Wave 12) — unlock gate for shop-gated content (skins,
         * bullets). [shopId] is a ShopItem.id, or null for free content.
         * Returns true if not gated (null id, or id absent from catalog) or if
         * the item was purchased (node rank > 0). [allRanks] is
         * MetaProgressionRepository.allRanks, keyed by [persistKey].
         */
        fun isShopUnlocked(allRanks: Map<String, Int>, shopId: String?): Boolean {
            if (shopId == null) return true
            val item = ALL.firstOrNull { it.id == shopId } ?: return true
            return (allRanks[item.persistKey] ?: 0) > 0
        }

        /**
         * Catalog. Costs calibrated assuming the average player accumulates
         * ~500 minerals per 10 endless runs. Cheapest item 300 minerals
         * (~6 runs), most expensive 1500 (~30 runs). Stat buffs deliberately
         * absent — the skill-tree (DialogMetaUpgrade) owns that economy.
         */
        val ALL: List<ShopItem> = listOf(
            // Ship skin unlocks
            ShopItem(
                id = "skin_aura_violet",
                displayName = "Skin tím",
                description = "Đổi hào quang tàu sang màu tím (chỉ làm đẹp, không đổi sức mạnh).",
                cost = 500,
                category = Category.SHIP_SKIN_UNLOCK,
            ),
            ShopItem(
                id = "skin_aura_red",
                displayName = "Skin đỏ báo động",
                description = "Đổi hào quang tàu sang màu đỏ rực (chỉ làm đẹp).",
                cost = 800,
                category = Category.SHIP_SKIN_UNLOCK,
            ),
            // Wave 16 — 3 hào quang mới.
            ShopItem(
                id = "skin_aura_emerald",
                displayName = "Skin lục bảo",
                description = "Đổi hào quang tàu sang xanh lục bảo (chỉ làm đẹp).",
                cost = 600,
                category = Category.SHIP_SKIN_UNLOCK,
            ),
            ShopItem(
                id = "skin_aura_amber",
                displayName = "Skin hổ phách",
                description = "Đổi hào quang tàu sang cam hổ phách (chỉ làm đẹp).",
                cost = 700,
                category = Category.SHIP_SKIN_UNLOCK,
            ),
            ShopItem(
                id = "skin_aura_ice",
                displayName = "Skin băng giá",
                description = "Đổi hào quang tàu sang xanh băng giá (chỉ làm đẹp).",
                cost = 900,
                category = Category.SHIP_SKIN_UNLOCK,
            ),
            // Bullet type unlocks (gated bullets beyond default NORMAL)
            ShopItem(
                id = "bullet_kamehameha",
                displayName = "Đạn kamehameha",
                description = "Tia năng lượng to, xuyên thấu nhiều địch một lúc (sát thương ×3).",
                cost = 1000,
                category = Category.BULLET_TYPE_UNLOCK,
            ),
            ShopItem(
                id = "bullet_atomic",
                displayName = "Đạn nguyên tử",
                description = "Khi trúng sẽ nổ lan ra vùng rộng, sát thương cả cụm địch xung quanh.",
                cost = 1500,
                category = Category.BULLET_TYPE_UNLOCK,
            ),
            // Wave 14a — gate 2 more strong bullets (were free). Migration in the
            // Đạn tab grants the currently-selected one so nobody loses it.
            ShopItem(
                id = "bullet_giant",
                displayName = "Đạn khổng lồ",
                description = "Viên đạn to gấp đôi, sát thương ×2 — dễ trúng, mạnh hơn.",
                cost = 700,
                category = Category.BULLET_TYPE_UNLOCK,
            ),
            ShopItem(
                id = "bullet_plasma",
                displayName = "Đạn plasma",
                description = "Khi trúng sẽ nổ lan một vùng nhỏ, dính cả địch đứng gần.",
                cost = 900,
                category = Category.BULLET_TYPE_UNLOCK,
            ),
            // Task 02 — Sét Chain (premium điều khiển đám đông).
            ShopItem(
                id = "bullet_lightning",
                displayName = "Đạn sét chain",
                description = "Trúng địch sẽ phóng sét lan sang tối đa 3 địch gần nhau, sát thương giảm dần.",
                cost = 1100,
                category = Category.BULLET_TYPE_UNLOCK,
            ),
            // Task 15 (đợt 4) — Nổ Chùm (airburst 8 mảnh 360°).
            ShopItem(
                id = "bullet_airburst",
                displayName = "Đạn nổ chùm",
                description = "Trúng địch nổ tung 8 đạn con toả quạt hướng lên — dọn cụm enemy dày.",
                cost = 1050,
                category = Category.BULLET_TYPE_UNLOCK,
            ),
            // Consumables (stockpile) — round 3 wires gameplay consumers.
            ShopItem(
                id = "smartbomb_pack_3",
                displayName = "Túi bom +3",
                description = "Vào run kế có thêm 3 quả bom dọn sạch màn hình (bấm nút bom để dùng).",
                cost = 300,
                category = Category.CONSUMABLE,
                stockpileAdd = 3,
            ),
            ShopItem(
                id = "revive_pack_1",
                displayName = "Túi hồi sinh +1",
                description = "Vào run kế, khi tàu nổ sẽ tự hồi sinh 1 lần (dùng là hết).",
                cost = 600,
                category = Category.CONSUMABLE,
                stockpileAdd = 1,
            ),
            // Wave 14a Round 2 — 6 "buff 1 run" packs. Consumed at run start in
            // rememberGameState; effect lasts the whole next run, then gone.
            ShopItem(
                id = "buff_x2_minerals",
                displayName = "Gói x2 Khoáng",
                description = "Run kế: mọi khoáng nhặt được nhân đôi (cày shop nhanh hơn).",
                cost = 400,
                category = Category.CONSUMABLE,
            ),
            ShopItem(
                id = "buff_start_shield",
                displayName = "Gói khiên khởi đầu",
                description = "Run kế: vào trận có sẵn 1 lớp khiên đỡ đòn (8 giây đầu).",
                cost = 250,
                category = Category.CONSUMABLE,
            ),
            ShopItem(
                id = "buff_x2_score",
                displayName = "Gói x2 Điểm",
                description = "Run kế: điểm gửi bảng xếp hạng nhân đôi (không đổi khoáng kiếm được).",
                cost = 400,
                category = Category.CONSUMABLE,
            ),
            ShopItem(
                id = "buff_combo_keep",
                displayName = "Gói giữ combo",
                description = "Run kế: combo lâu hết hơn (cửa sổ giữ combo dài gấp đôi).",
                cost = 350,
                category = Category.CONSUMABLE,
            ),
            ShopItem(
                id = "buff_magnet_xl",
                displayName = "Gói nam châm xl",
                description = "Run kế: bán kính hút khoáng to gấp đôi cả run.",
                cost = 300,
                category = Category.CONSUMABLE,
            ),
            ShopItem(
                id = "buff_rapid_fire",
                displayName = "Gói bắn nhanh",
                description = "Run kế: tốc độ bắn nhanh hơn (~1.5×) cả run.",
                cost = 450,
                category = Category.CONSUMABLE,
            ),
        )
    }
}
