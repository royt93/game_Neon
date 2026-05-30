package com.tranphuloi.neon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 12 round 2 — pin the ShopItem catalog invariants.
 *
 * Style follows [MetaProgressionKeysTest]: pure data sanity, no DataStore
 * mocking. Each test pins a single invariant of the static [ShopItem.ALL]
 * list — a violation would either ship a broken shop UI (negative cost,
 * un-clickable item) or silently corrupt persistence (id collision → two
 * items writing to the same `node_shop_*` key).
 */
class ShopItemCatalogTest {

    private val all = ShopItem.ALL

    @Test
    fun `catalog is non-empty`() {
        assertTrue("Catalog must have at least one item", all.isNotEmpty())
    }

    @Test
    fun `all ids are unique`() {
        val ids = all.map { it.id }
        assertEquals(
            "Duplicate id detected — two items would share a persistKey",
            ids.size, ids.toSet().size,
        )
    }

    @Test
    fun `every item has positive cost`() {
        all.forEach {
            assertTrue("Item ${it.id} has cost ${it.cost}, must be > 0", it.cost > 0)
        }
    }

    @Test
    fun `every item has maxRank gte 1`() {
        all.forEach {
            assertTrue(
                "Item ${it.id} has maxRank=${it.maxRank}, must be ≥ 1",
                it.maxRank >= 1,
            )
        }
    }

    @Test
    fun `CONSUMABLE items have stockpileAdd gte 1`() {
        all.filter { it.isConsumable }.forEach {
            assertTrue(
                "Consumable ${it.id} has stockpileAdd=${it.stockpileAdd}, must be ≥ 1",
                it.stockpileAdd >= 1,
            )
        }
    }

    @Test
    fun `every category has at least one item`() {
        // If a category goes empty, the section silently disappears from the
        // UI (CategorySection short-circuits on empty list). Pin so removing
        // the last item of a category is a visible decision.
        ShopItem.Category.entries.forEach { cat ->
            assertNotNull(
                "Category $cat has no items",
                all.firstOrNull { it.category == cat },
            )
        }
    }

    @Test
    fun `persistKey is prefixed with shop_`() {
        all.forEach {
            assertTrue(
                "${it.id}.persistKey=${it.persistKey} must start with 'shop_'",
                it.persistKey.startsWith("shop_"),
            )
        }
    }

    @Test
    fun `persistKey never starts with node_ or stockpile_ directly`() {
        // Defensive — `persistKey` is the SUFFIX appended after NODE_PREFIX /
        // STOCKPILE_PREFIX inside MetaProgressionRepository. If someone ever
        // hard-coded "node_" into the id, the final stored key becomes
        // `node_node_*` which silently breaks Flow reads.
        all.forEach {
            assertFalse(
                "${it.id}.persistKey collides with NODE_PREFIX",
                it.persistKey.startsWith("node_"),
            )
            assertFalse(
                "${it.id}.persistKey collides with STOCKPILE_PREFIX",
                it.persistKey.startsWith("stockpile_"),
            )
        }
    }

    @Test
    fun `cost ordering stays in the calibrated band`() {
        // Round 3: stat buffs removed; cheapest item is now a consumable (300),
        // most expensive a bullet unlock (1500). Pin min + max so a future
        // "discount everything to 10 minerals" doesn't ship without a conscious
        // revisit.
        val minCost = all.minOf { it.cost }
        val maxCost = all.maxOf { it.cost }
        // Tightened to the real current floor (300) so a "discount everything"
        // regression breaks visibly instead of sliding under a loose ≥100 pin.
        assertTrue("min cost $minCost should be ≥ 300", minCost >= 300)
        assertTrue("max cost $maxCost should be ≤ 2000", maxCost <= 2000)
    }

    // ── Stability pin — full id set so an accidental rename breaks ──
    @Test
    fun `id set is stable (rename detection)`() {
        // Renaming an id ORPHANS prior purchase state in DataStore: the old
        // `node_shop_<old_id>` rank key sits unread, the new `node_shop_<new_id>`
        // reads 0. This pin breaks visibly so a rename forces a migration.
        // Round 3 — PERMANENT_BUFF items (buff_hp/damage/magnet) removed; the
        // skill-tree owns the stat-buff economy now.
        val expected = setOf(
            "skin_aura_violet", "skin_aura_red",
            "bullet_kamehameha", "bullet_atomic",
            "smartbomb_pack_3", "revive_pack_1",
        )
        val actual = all.map { it.id }.toSet()
        assertEquals(
            "ShopItem id renamed — DataStore migration required",
            expected, actual,
        )
    }

    @Test
    fun `Category enum has 3 entries`() {
        // Pin so adding a 4th category triggers a visible test break + author
        // remembers to: extend ShopScreen with a CategorySection, decide a
        // persistKey route (node_ or stockpile_), update the test list above.
        // Round 3 dropped PERMANENT_BUFF (skill-tree owns stat buffs) → 3.
        assertEquals(3, ShopItem.Category.entries.size)
    }

    @Test
    fun `smartbomb_pack_3 adds exactly 3 to stockpile`() {
        // Naming-implied amount must match data. If they drift, the UI shows
        // "+3 stock" but persistence adds a different number.
        val item = all.first { it.id == "smartbomb_pack_3" }
        assertEquals(3, item.stockpileAdd)
    }

    @Test
    fun `revive_pack_1 adds exactly 1 to stockpile`() {
        val item = all.first { it.id == "revive_pack_1" }
        assertEquals(1, item.stockpileAdd)
    }

    @Test
    fun `Vietnamese display names are non-empty`() {
        // Empty displayName would render a clickable row with no label.
        all.forEach {
            assertNotEquals("Item ${it.id} has empty displayName", "", it.displayName)
        }
    }

    // ── Round 3 (Wave 12) — consumer wiring invariants ──

    @Test
    fun `consumable stockpile key constants equal the matching item persistKey`() {
        // GameState reads stockpiles via these const keys. If they drift from
        // the item's persistKey, run-start consumption silently no-ops (reads a
        // key nothing was ever written under).
        val bomb = all.first { it.id == "smartbomb_pack_3" }
        val revive = all.first { it.id == "revive_pack_1" }
        assertEquals(bomb.persistKey, ShopItem.SMARTBOMB_STOCKPILE_KEY)
        assertEquals(revive.persistKey, ShopItem.REVIVE_STOCKPILE_KEY)
    }

    @Test
    fun `isShopUnlocked treats null id and absent id as free`() {
        assertTrue("null shopId = free content", ShopItem.isShopUnlocked(emptyMap(), null))
        assertTrue(
            "id not in catalog = free (not a shop gate)",
            ShopItem.isShopUnlocked(emptyMap(), "not_a_real_item"),
        )
    }

    @Test
    fun `isShopUnlocked requires rank gt 0 for catalog items`() {
        val violet = all.first { it.id == "skin_aura_violet" }
        assertFalse(
            "rank 0 (or absent) = locked",
            ShopItem.isShopUnlocked(emptyMap(), violet.id),
        )
        assertTrue(
            "rank 1 = unlocked",
            ShopItem.isShopUnlocked(mapOf(violet.persistKey to 1), violet.id),
        )
    }

    @Test
    fun `every ShipSkin shopUnlockId resolves to a SHIP_SKIN_UNLOCK catalog item`() {
        ShipSkin.entries.mapNotNull { it.shopUnlockId }.forEach { id ->
            val item = all.firstOrNull { it.id == id }
            assertNotNull("ShipSkin shopUnlockId '$id' has no catalog item", item)
            assertEquals(
                "ShipSkin shopUnlockId '$id' must be a SHIP_SKIN_UNLOCK item",
                ShopItem.Category.SHIP_SKIN_UNLOCK, item!!.category,
            )
        }
    }

    @Test
    fun `every BulletType shopUnlockId resolves to a BULLET_TYPE_UNLOCK catalog item`() {
        com.tranphuloi.neon.ui.game.ship.laser.BulletType.entries
            .mapNotNull { it.shopUnlockId }.forEach { id ->
                val item = all.firstOrNull { it.id == id }
                assertNotNull("BulletType shopUnlockId '$id' has no catalog item", item)
                assertEquals(
                    "BulletType shopUnlockId '$id' must be a BULLET_TYPE_UNLOCK item",
                    ShopItem.Category.BULLET_TYPE_UNLOCK, item!!.category,
                )
            }
    }

    @Test
    fun `gated bullets are locked under empty ranks but NORMAL is always free`() {
        // Pins the run-start fallback trigger: GameState falls back to NORMAL
        // when the saved preferredBulletType is shop-locked. A player with no
        // purchases (empty ranks) must see KAMEHAMEHA/ATOMIC locked, NORMAL free.
        assertFalse(
            "KAMEHAMEHA must be locked with no purchases (→ run-start NORMAL fallback)",
            ShopItem.isShopUnlocked(
                emptyMap(),
                com.tranphuloi.neon.ui.game.ship.laser.BulletType.KAMEHAMEHA.shopUnlockId,
            ),
        )
        assertFalse(
            "ATOMIC must be locked with no purchases (→ run-start NORMAL fallback)",
            ShopItem.isShopUnlocked(
                emptyMap(),
                com.tranphuloi.neon.ui.game.ship.laser.BulletType.ATOMIC.shopUnlockId,
            ),
        )
        assertTrue(
            "NORMAL has no shopUnlockId → always free",
            ShopItem.isShopUnlocked(
                emptyMap(),
                com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL.shopUnlockId,
            ),
        )
    }
}
