package com.tranphuloi.neon.data

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.tranphuloi.neon.ui.game.ship.shape.ShipXpLevels
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Task 03 — INTEGRATION test (real DataStore via Robolectric) cho XP tàu:
 * addShipXp cộng dồn atomic, tách biệt theo tàu, và map allShipXp + level suy ra
 * qua [ShipXpLevels] khớp giá trị persist.
 */
@RunWith(RobolectricTestRunner::class)
class MetaProgressionShipXpIntegrationTest {

    private lateinit var repo: MetaProgressionRepository

    @Before
    fun setup() {
        runBlocking {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            repo = MetaProgressionRepository(ctx)
            ctx.metaDataStore.edit { it.clear() }
        }
    }

    private fun xp(key: String) = runBlocking { repo.shipXp(key).first() }

    @Test
    fun `ship xp starts at zero`() {
        assertEquals(0, xp("fighter"))
    }

    @Test
    fun `addShipXp accumulates for the same ship`() {
        runBlocking {
            repo.addShipXp("fighter", 40)
            repo.addShipXp("fighter", 70)
        }
        assertEquals("40+70 dồn atomic", 110, xp("fighter"))
        assertEquals("110 XP → level 2", 2, ShipXpLevels.levelForXp(xp("fighter")))
    }

    @Test
    fun `xp is tracked separately per ship`() {
        runBlocking {
            repo.addShipXp("fighter", 300)
            repo.addShipXp("bomber", 50)
        }
        assertEquals(300, xp("fighter"))
        assertEquals(50, xp("bomber"))
        assertEquals("chưa mua/chơi → 0", 0, xp("tank"))
    }

    @Test
    fun `addShipXp ignores non-positive amounts`() {
        runBlocking {
            repo.addShipXp("fighter", 100)
            repo.addShipXp("fighter", 0)
            repo.addShipXp("fighter", -50)
        }
        assertEquals("chỉ cộng amount dương", 100, xp("fighter"))
    }

    @Test
    fun `allShipXp maps every ship key`() {
        runBlocking {
            repo.addShipXp("fighter", 700)
            repo.addShipXp("stealth", 120)
        }
        val map = runBlocking { repo.allShipXp.first() }
        assertEquals(700, map["fighter"])
        assertEquals(120, map["stealth"])
        assertEquals("fighter 700 → L4", 4, ShipXpLevels.levelForXp(map["fighter"] ?: 0))
    }
}
