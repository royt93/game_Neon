package com.tranphuloi.neon.data

import androidx.datastore.preferences.core.edit
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
 * Task 29 — INTEGRATION (real DataStore) cho thưởng SỰ KIỆN TUẦN:
 * claim 1 lần/tuần (chống farm), cộng minerals, và mở lại vào tuần mới.
 */
@RunWith(RobolectricTestRunner::class)
class MetaProgressionWeeklyEventIntegrationTest {

    private lateinit var repo: MetaProgressionRepository

    @Before
    fun setup() {
        runBlocking {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            repo = MetaProgressionRepository(ctx)
            ctx.metaDataStore.edit { it.clear() }
        }
    }

    private fun minerals() = runBlocking { repo.lifetimeMinerals.first() }
    private fun available(week: Long) = runBlocking { repo.weeklyEventAvailable(week).first() }

    @Test
    fun `claim grants reward once and blocks same-week re-claim`() {
        runBlocking {
            assertTrue("đầu tuần còn nhận được", available(2948L))
            assertEquals("lần đầu +300", 300, repo.claimWeeklyEvent(2948L, 300))
            assertEquals("claim lại cùng tuần → 0", 0, repo.claimWeeklyEvent(2948L, 300))
        }
        assertEquals("chỉ cộng 1 lần", 300, minerals())
        assertFalse("đã claim → không còn available", available(2948L))
    }

    @Test
    fun `new week re-opens the claim`() {
        runBlocking {
            repo.claimWeeklyEvent(2948L, 300)
            assertTrue("tuần mới mở lại", available(2949L))
            assertEquals("tuần mới +300", 300, repo.claimWeeklyEvent(2949L, 300))
        }
        assertEquals("2 tuần = 600", 600, minerals())
    }

    @Test
    fun `daily and weekly claims are independent`() {
        runBlocking {
            assertEquals("daily +100", 100, repo.claimDailyChallenge(20638L, 100))
            assertEquals("weekly cùng kỳ vẫn +300 (key riêng)", 300, repo.claimWeeklyEvent(2948L, 300))
        }
        assertEquals("cả 2 cộng dồn", 400, minerals())
    }
}
