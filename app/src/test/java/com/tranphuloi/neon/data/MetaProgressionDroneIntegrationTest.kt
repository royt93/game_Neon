package com.tranphuloi.neon.data

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.tranphuloi.neon.ui.game.meta.SkillNode
import com.tranphuloi.neon.ui.game.state.EffectiveStats
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
 * Task 01 (Slice 7) — INTEGRATION test (real DataStore via Robolectric) cho node
 * kỹ năng DRONE_FLEET: mua tăng rank, persist qua `allRanks[META_KEY_DRONE]`, chặn
 * quá maxRank / thiếu minerals. Đây là "hợp đồng" mà GameState đọc để suy ra
 * maxDrones + gate DRONE_BOOSTER (rank 0 = khoá).
 *
 * NOTE: @Before/@Test dùng block body (không `= runBlocking`) để trả Unit — JUnit
 * từ chối test method trả non-void.
 */
@RunWith(RobolectricTestRunner::class)
class MetaProgressionDroneIntegrationTest {

    private lateinit var repo: MetaProgressionRepository
    private val droneKey = EffectiveStats.META_KEY_DRONE
    private val maxRank = SkillNode.DRONE_FLEET.maxRank

    @Before
    fun setup() {
        runBlocking {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            repo = MetaProgressionRepository(ctx)
            ctx.metaDataStore.edit { it.clear() }
        }
    }

    private fun ranks() = runBlocking { repo.allRanks.first() }
    private fun droneRank() = ranks()[droneKey] ?: 0

    @Test
    fun `drone is locked by default (rank 0)`() {
        assertEquals("chưa mua → rank 0 (khoá)", 0, droneRank())
    }

    @Test
    fun `buying DRONE_FLEET raises rank and persists in allRanks`() {
        runBlocking {
            repo.addMinerals(5000)
            val ok = repo.spendOnNode(droneKey, cost = 320, maxRank = maxRank)
            assertTrue("mua cấp 1 thành công", ok)
        }
        assertEquals("rank 1 sau khi mua", 1, droneRank())
    }

    @Test
    fun `DRONE_FLEET can be ranked up to maxRank then refuses`() {
        runBlocking {
            repo.addMinerals(100_000)
            repeat(maxRank) { assertTrue(repo.spendOnNode(droneKey, cost = 100, maxRank = maxRank)) }
            // Mua thêm vượt trần → bị từ chối.
            val over = repo.spendOnNode(droneKey, cost = 100, maxRank = maxRank)
            assertFalse("vượt maxRank phải bị từ chối", over)
        }
        assertEquals("dừng đúng maxRank", maxRank, droneRank())
    }

    @Test
    fun `buying DRONE_FLEET is refused with insufficient minerals`() {
        runBlocking {
            repo.addMinerals(10) // < cost
            val ok = repo.spendOnNode(droneKey, cost = 320, maxRank = maxRank)
            assertFalse("thiếu minerals → từ chối", ok)
        }
        assertEquals("vẫn khoá", 0, droneRank())
    }
}
