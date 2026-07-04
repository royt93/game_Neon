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
 * Task 08 — INTEGRATION (real DataStore) cho thưởng THỬ THÁCH HẰNG NGÀY:
 * claim 1 lần/ngày (chống farm), cộng minerals, và mở lại vào ngày mới.
 */
@RunWith(RobolectricTestRunner::class)
class MetaProgressionDailyChallengeIntegrationTest {

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
    private fun available(day: Long) = runBlocking { repo.dailyChallengeAvailable(day).first() }

    @Test
    fun `claim grants reward once and blocks same-day re-claim`() {
        runBlocking {
            assertTrue("đầu ngày còn nhận được", available(20638L))
            assertEquals("lần đầu +100", 100, repo.claimDailyChallenge(20638L, 100))
            assertEquals("claim lại cùng ngày → 0", 0, repo.claimDailyChallenge(20638L, 100))
        }
        assertEquals("chỉ cộng 1 lần", 100, minerals())
        assertFalse("đã claim → không còn available", available(20638L))
    }

    @Test
    fun `new day re-opens the claim`() {
        runBlocking {
            repo.claimDailyChallenge(20638L, 100)
            assertTrue("ngày mới mở lại", available(20639L))
            assertEquals("ngày mới +100", 100, repo.claimDailyChallenge(20639L, 100))
        }
        assertEquals("2 ngày = 200", 200, minerals())
    }
}
