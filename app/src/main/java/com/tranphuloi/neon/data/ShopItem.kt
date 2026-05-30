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
                displayName = "Skin Tím",
                description = "Mở khóa ship skin AURA VIOLET",
                cost = 500,
                category = Category.SHIP_SKIN_UNLOCK,
            ),
            ShopItem(
                id = "skin_aura_red",
                displayName = "Skin Đỏ Báo Động",
                description = "Mở khóa ship skin AURA REDALERT",
                cost = 800,
                category = Category.SHIP_SKIN_UNLOCK,
            ),
            // Bullet type unlocks (gated bullets beyond default NORMAL)
            ShopItem(
                id = "bullet_kamehameha",
                displayName = "Đạn Kamehameha",
                description = "Mở khóa BulletType KAMEHAMEHA cho Loadout",
                cost = 1000,
                category = Category.BULLET_TYPE_UNLOCK,
            ),
            ShopItem(
                id = "bullet_atomic",
                displayName = "Đạn Atomic",
                description = "Mở khóa BulletType ATOMIC cho Loadout",
                cost = 1500,
                category = Category.BULLET_TYPE_UNLOCK,
            ),
            // Consumables (stockpile) — round 3 wires gameplay consumers.
            ShopItem(
                id = "smartbomb_pack_3",
                displayName = "Túi bom +3",
                description = "+3 smart bomb stock (consumed on use)",
                cost = 300,
                category = Category.CONSUMABLE,
                stockpileAdd = 3,
            ),
            ShopItem(
                id = "revive_pack_1",
                displayName = "Túi hồi sinh +1",
                description = "+1 token hồi sinh — tự kích hoạt khi vào run kế tiếp bạn chơi",
                cost = 600,
                category = Category.CONSUMABLE,
                stockpileAdd = 1,
            ),
        )
    }
}
