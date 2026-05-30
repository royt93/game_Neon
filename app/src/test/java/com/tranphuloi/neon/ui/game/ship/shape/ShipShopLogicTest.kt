package com.tranphuloi.neon.ui.game.ship.shape

import com.tranphuloi.neon.ui.game.state.EffectiveStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 13a — pins every branch of [ShipShopLogic] (ship purchase economy).
 * Pure logic, no DataStore. These predicates are the source of truth for ship
 * ownership / affordability / migration, so a regression here would silently
 * let a player buy a ship they own, lose an owned ship, or pay the wrong cost.
 */
class ShipShopLogicTest {

    private val ranksEmpty = emptyMap<String, Int>()

    // ── persistKey ──

    @Test
    fun `persistKey is shop_ship_ prefixed with shape key`() {
        assertEquals("shop_ship_fighter", ShipShopLogic.persistKey(ShipShape.FIGHTER))
        assertEquals("shop_ship_tank", ShipShopLogic.persistKey(ShipShape.TANK))
    }

    @Test
    fun `all persistKeys are unique`() {
        val keys = ShipShape.entries.map { ShipShopLogic.persistKey(it) }
        assertEquals("Duplicate ship persistKey", keys.size, keys.toSet().size)
    }

    @Test
    fun `persistKey collides with neither node_ nor stockpile_ prefixes`() {
        ShipShape.entries.forEach {
            val k = ShipShopLogic.persistKey(it)
            assertFalse(k.startsWith("node_"))
            assertFalse(k.startsWith("stockpile_"))
        }
    }

    // ── isFree ──

    @Test
    fun `FIGHTER is the only free ship`() {
        assertTrue(ShipShopLogic.isFree(ShipShape.FIGHTER))
        ShipShape.entries.filter { it != ShipShape.FIGHTER }.forEach {
            assertFalse("${it.key} should not be free (cost ${it.unlockMinerals})", ShipShopLogic.isFree(it))
        }
    }

    // ── discountFactor ──

    @Test
    fun `discountFactor rank 0 is 1x (no discount)`() {
        assertEquals(1f, ShipShopLogic.discountFactor(0), 0.0001f)
    }

    @Test
    fun `discountFactor scales 10 percent per rank`() {
        assertEquals(0.9f, ShipShopLogic.discountFactor(1), 0.0001f)
        assertEquals(0.7f, ShipShopLogic.discountFactor(3), 0.0001f)
    }

    @Test
    fun `discountFactor caps at 50 percent off`() {
        assertEquals(0.5f, ShipShopLogic.discountFactor(5), 0.0001f)
        assertEquals(0.5f, ShipShopLogic.discountFactor(99), 0.0001f)
    }

    @Test
    fun `discountFactor clamps negative rank to no discount`() {
        assertEquals(1f, ShipShopLogic.discountFactor(-3), 0.0001f)
    }

    @Test
    fun `discountFactor constants stay in sync with EffectiveStats node`() {
        // Drift guard: the skill-tree node and the ship-cost discount must agree.
        assertEquals(
            EffectiveStats.META_SHIP_UNLOCK_DISCOUNT_PER_RANK,
            ShipShopLogic.DISCOUNT_PER_RANK,
            0.0001f,
        )
    }

    // ── effectiveCost ──

    @Test
    fun `effectiveCost free ship is 0 regardless of discount`() {
        assertEquals(0, ShipShopLogic.effectiveCost(ShipShape.FIGHTER, 0))
        assertEquals(0, ShipShopLogic.effectiveCost(ShipShape.FIGHTER, 5))
    }

    @Test
    fun `effectiveCost equals base at rank 0`() {
        assertEquals(ShipShape.TANK.unlockMinerals, ShipShopLogic.effectiveCost(ShipShape.TANK, 0))
    }

    @Test
    fun `effectiveCost applies discount`() {
        // TANK 1000 × 0.7 = 700 at rank 3.
        assertEquals(700, ShipShopLogic.effectiveCost(ShipShape.TANK, 3))
        // capped 50%: 1000 × 0.5 = 500.
        assertEquals(500, ShipShopLogic.effectiveCost(ShipShape.TANK, 5))
    }

    @Test
    fun `effectiveCost never exceeds base cost`() {
        ShipShape.entries.forEach { shape ->
            (0..10).forEach { rank ->
                assertTrue(
                    "${shape.key} cost at rank $rank exceeds base",
                    ShipShopLogic.effectiveCost(shape, rank) <= shape.unlockMinerals,
                )
            }
        }
    }

    // ── isOwned ──

    @Test
    fun `isOwned free ship always true even with empty ranks`() {
        assertTrue(ShipShopLogic.isOwned(ShipShape.FIGHTER, ranksEmpty))
    }

    @Test
    fun `isOwned paid ship false at rank 0, true at rank 1`() {
        assertFalse(ShipShopLogic.isOwned(ShipShape.TANK, ranksEmpty))
        assertTrue(ShipShopLogic.isOwned(ShipShape.TANK, mapOf("shop_ship_tank" to 1)))
    }

    // ── canBuy ──

    @Test
    fun `canBuy false when already owned`() {
        assertFalse(ShipShopLogic.canBuy(99999, ShipShape.TANK, mapOf("shop_ship_tank" to 1), 0))
    }

    @Test
    fun `canBuy false for free ship (it's owned, not purchasable)`() {
        assertFalse(ShipShopLogic.canBuy(99999, ShipShape.FIGHTER, ranksEmpty, 0))
    }

    @Test
    fun `canBuy true with exact balance, false one short`() {
        assertTrue(ShipShopLogic.canBuy(1000, ShipShape.TANK, ranksEmpty, 0))
        assertFalse(ShipShopLogic.canBuy(999, ShipShape.TANK, ranksEmpty, 0))
    }

    @Test
    fun `canBuy honours discount — affordable after discount`() {
        // TANK 1000, rank 5 → 500. balance 500 enough though < base 1000.
        assertTrue(ShipShopLogic.canBuy(500, ShipShape.TANK, ranksEmpty, 5))
        assertFalse(ShipShopLogic.canBuy(499, ShipShape.TANK, ranksEmpty, 5))
    }

    // ── migrationGrantKeys ──

    @Test
    fun `migration grants nothing when selected is free FIGHTER`() {
        assertTrue(ShipShopLogic.migrationGrantKeys(ShipShape.FIGHTER, ranksEmpty).isEmpty())
    }

    @Test
    fun `migration grants the selected paid ship not yet owned`() {
        assertEquals(
            listOf("shop_ship_tank"),
            ShipShopLogic.migrationGrantKeys(ShipShape.TANK, ranksEmpty),
        )
    }

    @Test
    fun `migration grants nothing when selected paid ship already owned (idempotent)`() {
        assertTrue(
            ShipShopLogic.migrationGrantKeys(ShipShape.TANK, mapOf("shop_ship_tank" to 1)).isEmpty(),
        )
    }

    // ── catalog invariants ──

    @Test
    fun `every paid ship has positive unlockMinerals`() {
        ShipShape.entries.filter { it != ShipShape.FIGHTER }.forEach {
            assertTrue("${it.key} must cost > 0", it.unlockMinerals > 0)
        }
    }
}
