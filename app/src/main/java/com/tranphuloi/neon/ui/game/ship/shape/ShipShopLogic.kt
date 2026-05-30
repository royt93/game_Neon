package com.tranphuloi.neon.ui.game.ship.shape

/**
 * Wave 13a — pure ship-purchase economy logic, extracted so every branch is
 * unit-testable without DataStore (mirrors `ShopItem`'s `canSpendOnNode` style).
 *
 * Ship unlock model changed from "lifetime-minerals threshold gate" to a real
 * **purchase** (deduct via `spendOnNode`), so once bought a ship is owned
 * forever — the old gate re-locked a ship when the spendable balance dropped
 * below its threshold (a latent bug). Ownership persists as a node rank under
 * key [persistKey] (`shop_ship_<shapeKey>`), same DataStore namespace the Shop
 * skins/bullets already use.
 *
 * FIGHTER (cost 0) is always owned. The `SHIP_UNLOCK_DISCOUNT` skill node still
 * applies — now as a discount on the purchase cost (was: discount on threshold).
 */
object ShipShopLogic {
    const val PERSIST_PREFIX = "shop_ship_"

    /** Per-rank discount from the SHIP_UNLOCK_DISCOUNT node (10%/rank). */
    const val DISCOUNT_PER_RANK = 0.10f

    /** Floor on the discount multiplier — max 50% off no matter the rank. */
    const val MIN_DISCOUNT_FACTOR = 0.5f

    /** Node key (allRanks-stripped form) for a ship's ownership flag. */
    fun persistKey(shape: ShipShape): String = PERSIST_PREFIX + shape.key

    /** FIGHTER is free → always owned without a record. */
    fun isFree(shape: ShipShape): Boolean = shape.unlockMinerals <= 0

    /**
     * Discount multiplier in [MIN_DISCOUNT_FACTOR, 1f] from the unlock-discount
     * node rank. rank 0 → 1.0 (no discount), rank 5 → 0.5 (capped).
     */
    fun discountFactor(discountRank: Int): Float =
        (1f - discountRank.coerceAtLeast(0) * DISCOUNT_PER_RANK).coerceIn(MIN_DISCOUNT_FACTOR, 1f)

    /** Final cost after discount. Free ships stay 0. */
    fun effectiveCost(shape: ShipShape, discountRank: Int): Int {
        if (isFree(shape)) return 0
        return (shape.unlockMinerals * discountFactor(discountRank)).toInt()
    }

    /**
     * Owned = free ship, OR a purchase rank > 0 recorded under [persistKey].
     * [ranks] is `MetaProgressionRepository.allRanks` (keyed by stripped node key).
     */
    fun isOwned(shape: ShipShape, ranks: Map<String, Int>): Boolean =
        isFree(shape) || (ranks[persistKey(shape)] ?: 0) > 0

    /**
     * Purchasable now = not already owned AND balance covers the discounted
     * cost. (Free ships are never "purchasable" — they're owned.)
     */
    fun canBuy(balance: Int, shape: ShipShape, ranks: Map<String, Int>, discountRank: Int): Boolean {
        if (isOwned(shape, ranks)) return false
        return balance >= effectiveCost(shape, discountRank)
    }

    /**
     * Migration set: when switching from threshold-gate to purchase, grant
     * (rank=1, NO charge) the ships the player must not lose — FIGHTER (free)
     * plus the ship they currently have selected (so their active ship keeps
     * working). Returns the persistKeys to grant that aren't already owned.
     *
     * Idempotent by design: callers may invoke on every ship-tab load; a key
     * already at rank>0 is filtered out so nothing is re-granted or double-counted.
     */
    fun migrationGrantKeys(selected: ShipShape, ranks: Map<String, Int>): List<String> {
        val needed = buildList {
            // selected ship must stay usable; FIGHTER is free (no record needed).
            if (!isFree(selected) && !isOwned(selected, ranks)) add(persistKey(selected))
        }
        return needed
    }
}
