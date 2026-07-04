package com.tranphuloi.neon.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.core.app.ApplicationProvider
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
 * Task 10 — INTEGRATION (real DataStore) cho Prestige: doPrestige XOÁ skill-tree,
 * GIỮ shop unlock + khoáng bị trừ + prestige_level tăng; thiếu điều kiện → no-op.
 */
@RunWith(RobolectricTestRunner::class)
class MetaProgressionPrestigeIntegrationTest {

    private lateinit var repo: MetaProgressionRepository

    @Before
    fun setup() {
        runBlocking {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            repo = MetaProgressionRepository(ctx)
            ctx.metaDataStore.edit { it.clear() }
        }
    }

    private fun ctx() = ApplicationProvider.getApplicationContext<android.content.Context>()

    /** Gieo 8 rank skill-tree (base_hp 5 + base_damage 3) + 1 shop unlock + khoáng. */
    private fun seed(minerals: Int) = runBlocking {
        ctx().metaDataStore.edit {
            it[intPreferencesKey("node_base_hp")] = 5
            it[intPreferencesKey("node_base_damage")] = 3
            it[intPreferencesKey("node_shop_skin_aura_violet")] = 1   // shop — PHẢI sống sót
        }
        repo.addMinerals(minerals)
    }

    private fun minerals() = runBlocking { repo.lifetimeMinerals.first() }
    private fun level() = runBlocking { repo.prestigeLevel.first() }
    private fun ranks() = runBlocking { repo.totalSkillRanks.first() }
    private fun allRanks() = runBlocking { repo.allRanks.first() }

    @Test
    fun `totalSkillRanks đếm skill-tree KHÔNG tính shop`() {
        seed(5000)
        assertEquals("5 + 3 (bỏ shop) = 8", 8, ranks())
    }

    @Test
    fun `doPrestige đủ điều kiện xoá skill-tree giữ shop trừ khoáng tăng cấp`() {
        seed(5000)
        val ok = runBlocking { repo.doPrestige(cost = 1500, minRanks = 8) }
        assertTrue("prestige thành công", ok)
        assertEquals("khoáng bị trừ 5000-1500", 3500, minerals())
        assertEquals("prestige level +1", 1, level())
        assertEquals("skill-tree bị xoá hết", 0, ranks())
        assertEquals("shop unlock GIỮ nguyên", 1, allRanks()["shop_skin_aura_violet"])
        assertEquals("skill node đã xoá", null, allRanks()["base_hp"])
    }

    @Test
    fun `doPrestige thiếu rank là no-op`() {
        runBlocking {
            ctx().metaDataStore.edit { it[intPreferencesKey("node_base_hp")] = 3 } // chỉ 3 rank
            repo.addMinerals(5000)
        }
        val ok = runBlocking { repo.doPrestige(cost = 1500, minRanks = 8) }
        assertFalse("thiếu rank → false", ok)
        assertEquals("khoáng không đổi", 5000, minerals())
        assertEquals("chưa prestige", 0, level())
        assertEquals("skill-tree còn nguyên", 3, ranks())
    }

    @Test
    fun `doPrestige thiếu khoáng là no-op`() {
        seed(1000) // < cost 1500
        val ok = runBlocking { repo.doPrestige(cost = 1500, minRanks = 8) }
        assertFalse("thiếu tiền → false", ok)
        assertEquals("khoáng không đổi", 1000, minerals())
        assertEquals("skill-tree còn nguyên", 8, ranks())
    }
}
