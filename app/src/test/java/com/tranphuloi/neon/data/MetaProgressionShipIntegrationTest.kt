package com.tranphuloi.neon.data

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.tranphuloi.neon.ui.game.meta.SkillNode
import com.tranphuloi.neon.ui.game.ship.shape.ShipShape
import com.tranphuloi.neon.ui.game.ship.shape.ShipShopLogic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Wave 13a — INTEGRATION test (real DataStore via Robolectric) for the ship
 * purchase + migration persistence wired in slice A/B. Exercises the actual
 * `MetaProgressionRepository` suspend methods end-to-end against a real
 * preferences DataStore — the layer `ShipShopLogicTest` (pure) can't cover.
 *
 * NOTE: @Before/@Test use block bodies (not `= runBlocking`) so the methods
 * return Unit — JUnit rejects test methods with a non-void return type.
 */
@RunWith(RobolectricTestRunner::class)
class MetaProgressionShipIntegrationTest {

    private lateinit var repo: MetaProgressionRepository

    @Before
    fun setup() {
        runBlocking {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            repo = MetaProgressionRepository(ctx)
            // DataStore is a process singleton keyed by file name → clear between
            // tests so ranks/balance don't bleed across cases.
            ctx.metaDataStore.edit { it.clear() }
        }
    }

    private fun balance() = runBlocking { repo.lifetimeMinerals.first() }
    private fun ranks() = runBlocking { repo.allRanks.first() }

    @Test
    fun `buying a ship deducts balance and records ownership rank`() {
        runBlocking {
            repo.addMinerals(1500)
            val tankKey = ShipShopLogic.persistKey(ShipShape.TANK) // "shop_ship_tank"
            val ok = repo.spendOnNode(tankKey, ShipShape.TANK.unlockMinerals, maxRank = 1)
            assertTrue("purchase should succeed with enough balance", ok)
        }
        assertEquals(1500 - ShipShape.TANK.unlockMinerals, balance())
        assertTrue("TANK should now be owned", ShipShopLogic.isOwned(ShipShape.TANK, ranks()))
    }

    @Test
    fun `buying a ship is refused when balance insufficient`() {
        runBlocking {
            repo.addMinerals(100) // < TANK 1000
            val ok = repo.spendOnNode(ShipShopLogic.persistKey(ShipShape.TANK), ShipShape.TANK.unlockMinerals, 1)
            assertFalse(ok)
        }
        assertEquals(100, balance())
        assertFalse(ShipShopLogic.isOwned(ShipShape.TANK, ranks()))
    }

    @Test
    fun `grantNodeFree grants once without charging and is idempotent`() {
        runBlocking {
            repo.addMinerals(500)
            val key = ShipShopLogic.persistKey(ShipShape.STEALTH)
            assertTrue("first grant should report granted", repo.grantNodeFree(key))
            assertFalse("second grant should be a no-op", repo.grantNodeFree(key))
        }
        assertEquals("grant must NOT deduct minerals", 500, balance())
        assertTrue(ShipShopLogic.isOwned(ShipShape.STEALTH, ranks()))
    }

    @Test
    fun `migration grant preserves the selected ship without charging`() {
        runBlocking {
            // Player selected CAU_VONG under the old threshold model, low balance now.
            repo.addMinerals(50)
            ShipShopLogic.migrationGrantKeys(ShipShape.CAU_VONG, ranks()).forEach { repo.grantNodeFree(it) }
        }
        assertEquals("migration must not charge", 50, balance())
        assertTrue("selected ship kept", ShipShopLogic.isOwned(ShipShape.CAU_VONG, ranks()))
    }

    @Test
    fun `owned ship cannot be re-bought (canBuy false after purchase)`() {
        runBlocking {
            repo.addMinerals(5000)
            repo.spendOnNode(ShipShopLogic.persistKey(ShipShape.TANK), ShipShape.TANK.unlockMinerals, 1)
        }
        assertFalse(ShipShopLogic.canBuy(balance(), ShipShape.TANK, ranks(), discountRank = 0))
    }

    @Test
    fun `ship purchase and skin purchase coexist in allRanks without key collision`() {
        runBlocking {
            repo.addMinerals(3000)
            repo.spendOnNode(ShipShopLogic.persistKey(ShipShape.BOMBER), ShipShape.BOMBER.unlockMinerals, 1)
            repo.spendOnNode("shop_skin_aura_violet", 500, 1)
        }
        val r = ranks()
        assertTrue(ShipShopLogic.isOwned(ShipShape.BOMBER, r))
        assertEquals(1, r["shop_skin_aura_violet"])
        assertTrue(r.containsKey("shop_ship_bomber"))
    }

    // Task 24 — Mở rộng skill-tree: roundtrip persistence cho node mới.

    @Test
    fun `MINERAL_BOOST node spends across multiple ranks and persists`() {
        val node = SkillNode.MINERAL_BOOST
        runBlocking {
            repo.addMinerals(10_000)
            repeat(2) { rank ->
                val ok = repo.spendOnNode(node.key, node.costForNextRank(rank), node.maxRank)
                assertTrue("rank ${rank + 1} purchase should succeed", ok)
            }
        }
        assertEquals(2, ranks()[node.key])
        val expectedSpent = node.costForNextRank(0) + node.costForNextRank(1)
        assertEquals(10_000 - expectedSpent, balance())
    }

    @Test
    fun `SECOND_WIND node (maxRank 1) cannot be bought twice`() {
        val node = SkillNode.SECOND_WIND
        runBlocking {
            repo.addMinerals(2_000)
            val first = repo.spendOnNode(node.key, node.costForNextRank(0), node.maxRank)
            assertTrue("first purchase should succeed", first)
            val second = repo.spendOnNode(node.key, node.costForNextRank(1), node.maxRank)
            assertFalse("second purchase must be refused at maxRank", second)
        }
        assertEquals(1, ranks()[node.key])
    }
}
