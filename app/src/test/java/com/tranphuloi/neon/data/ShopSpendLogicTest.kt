package com.tranphuloi.neon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 12 round 2 — pin the pure spend-decision logic extracted from
 * [MetaProgressionRepository.spendOnNode] / [spendOnStockpile].
 *
 * These predicates ARE the source of truth for purchase legality. Standing
 * up a real DataStore for unit testing would require Robolectric (project
 * doesn't use it yet); instead the suspend methods delegate to
 * `canSpendOnNode` / `canSpendOnStockpile` and we pin those here.
 *
 * Each test corresponds to one branch of the decision tree.
 */
class ShopSpendLogicTest {

    // ── canSpendOnNode ──

    @Test
    fun `node spend succeeds with exact balance and rank 0`() {
        assertTrue(canSpendOnNode(balance = 100, cost = 100, currentRank = 0, maxRank = 3))
    }

    @Test
    fun `node spend succeeds with excess balance`() {
        assertTrue(canSpendOnNode(balance = 500, cost = 100, currentRank = 1, maxRank = 3))
    }

    @Test
    fun `node spend fails on insufficient balance`() {
        assertFalse(canSpendOnNode(balance = 99, cost = 100, currentRank = 0, maxRank = 3))
    }

    @Test
    fun `node spend fails on zero balance`() {
        assertFalse(canSpendOnNode(balance = 0, cost = 100, currentRank = 0, maxRank = 3))
    }

    @Test
    fun `node spend fails when at max rank`() {
        assertFalse(canSpendOnNode(balance = 1000, cost = 100, currentRank = 3, maxRank = 3))
    }

    @Test
    fun `node spend fails when one beyond max rank (defensive)`() {
        // Shouldn't happen, but if DataStore got corrupted higher than maxRank,
        // we must still refuse — don't let purchase deduct minerals for a
        // no-op rank-up.
        assertFalse(canSpendOnNode(balance = 1000, cost = 100, currentRank = 5, maxRank = 3))
    }

    @Test
    fun `node spend rejects negative cost (defensive)`() {
        // Defensive — a negative cost would otherwise CREDIT minerals on purchase.
        assertFalse(canSpendOnNode(balance = 100, cost = -50, currentRank = 0, maxRank = 3))
    }

    @Test
    fun `node spend allows zero cost for free unlocks`() {
        // Free unlock (cost=0) should succeed if rank < maxRank — used if we
        // ever add a tutorial freebie. balance=0 is fine because 0 >= 0.
        assertTrue(canSpendOnNode(balance = 0, cost = 0, currentRank = 0, maxRank = 1))
    }

    @Test
    fun `node spend rejects zero maxRank (defensive)`() {
        // maxRank=0 is nonsensical (item with no purchase slot). Refuse so a
        // malformed catalog entry doesn't silently deduct minerals.
        assertFalse(canSpendOnNode(balance = 100, cost = 50, currentRank = 0, maxRank = 0))
    }

    // ── canSpendOnStockpile ──

    @Test
    fun `stockpile spend succeeds with sufficient balance and positive add`() {
        assertTrue(canSpendOnStockpile(balance = 300, cost = 300, addAmount = 3))
    }

    @Test
    fun `stockpile spend fails on insufficient balance`() {
        assertFalse(canSpendOnStockpile(balance = 299, cost = 300, addAmount = 3))
    }

    @Test
    fun `stockpile spend rejects zero or negative addAmount`() {
        // Refuse — purchase that deducts minerals but adds nothing is a bug.
        assertFalse(canSpendOnStockpile(balance = 1000, cost = 100, addAmount = 0))
        assertFalse(canSpendOnStockpile(balance = 1000, cost = 100, addAmount = -1))
    }

    @Test
    fun `stockpile spend rejects negative cost`() {
        assertFalse(canSpendOnStockpile(balance = 100, cost = -1, addAmount = 1))
    }

    // ── Integration with catalog — every real ShopItem can succeed for a rich player ──

    @Test
    fun `every catalog item is purchasable for a player with 9999 minerals at rank 0`() {
        ShopItem.ALL.forEach { item ->
            val canBuy = if (item.isConsumable) {
                canSpendOnStockpile(balance = 9999, cost = item.cost, addAmount = item.stockpileAdd)
            } else {
                canSpendOnNode(balance = 9999, cost = item.cost, currentRank = 0, maxRank = item.maxRank)
            }
            assertTrue("Item ${item.id} unreachable for rich player at rank 0", canBuy)
        }
    }

    @Test
    fun `every rank-based item refuses purchase once maxed out`() {
        ShopItem.ALL.filter { !it.isConsumable }.forEach { item ->
            val canBuy = canSpendOnNode(
                balance = 9999,
                cost = item.cost,
                currentRank = item.maxRank,
                maxRank = item.maxRank,
            )
            assertFalse("Maxed item ${item.id} should refuse purchase", canBuy)
        }
    }

    // ── Affordability gate mirrors the UI predicate ──
    // ShopScreen disables the row when `balance < item.cost` OR item is at
    // max rank. Pin that the UI predicate matches the repo predicate, so a
    // user who CAN see "tap-enabled" will not have the repo silently refuse.

    @Test
    fun `UI affordability gate aligns with canSpendOnNode for rank items`() {
        ShopItem.ALL.filter { !it.isConsumable }.forEach { item ->
            val balance = item.cost  // exactly enough
            val uiAffordable = balance >= item.cost
            val repoWillAccept = canSpendOnNode(balance, item.cost, currentRank = 0, item.maxRank)
            assertEquals(
                "UI gate vs repo gate mismatch for ${item.id}",
                uiAffordable, repoWillAccept,
            )
        }
    }

    @Test
    fun `UI affordability gate aligns with canSpendOnStockpile for consumables`() {
        ShopItem.ALL.filter { it.isConsumable }.forEach { item ->
            val balance = item.cost
            val uiAffordable = balance >= item.cost
            val repoWillAccept = canSpendOnStockpile(balance, item.cost, item.stockpileAdd)
            assertEquals(
                "UI gate vs repo gate mismatch for ${item.id}",
                uiAffordable, repoWillAccept,
            )
        }
    }
}
