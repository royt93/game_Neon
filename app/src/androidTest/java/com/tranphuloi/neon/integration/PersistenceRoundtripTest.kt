package com.tranphuloi.neon.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tranphuloi.neon.App
import com.tranphuloi.neon.data.Achievement
import com.tranphuloi.neon.data.Difficulty
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test — persistence roundtrip THẬT trên DataStore của thiết bị.
 * Dùng lại 5 repository singleton mà [App.onCreate] đã tạo (không new lại để
 * tránh xung đột "multiple DataStores active for the same file").
 *
 * Mỗi test ghi giá trị → đọc lại qua Flow.first() → assert bằng đúng giá trị vừa
 * ghi. Vì DataStore chia sẻ giữa các lần chạy, các test dùng giá trị/ khoá riêng
 * hoặc so sánh delta thay vì giá trị tuyệt đối ban đầu.
 */
@RunWith(AndroidJUnit4::class)
class PersistenceRoundtripTest {

    private val app: App get() = ApplicationProvider.getApplicationContext()

    @Test
    fun settings_difficulty_roundtrips() = runBlocking {
        app.settings.setDifficulty(Difficulty.HARD)
        assertEquals(Difficulty.HARD, app.settings.difficulty.first())

        app.settings.setDifficulty(Difficulty.EASY)
        assertEquals(Difficulty.EASY, app.settings.difficulty.first())
    }

    @Test
    fun settings_volume_roundtrips() = runBlocking {
        app.settings.setMusicVolume(37)
        assertEquals(37, app.settings.musicVolume.first())

        app.settings.setSfxVolume(88)
        assertEquals(88, app.settings.sfxVolume.first())
    }

    @Test
    fun leaderboard_submit_keeps_high_scores_sorted_desc() = runBlocking {
        // Nộp một điểm rất cao → phải nằm trong top và list luôn giảm dần.
        val huge = 9_999_999
        val after = app.leaderboard.submit(huge)
        assertTrue("điểm vừa nộp phải có mặt trong bảng", after.any { it.score == huge })
        val scores = after.map { it.score }
        assertEquals("bảng xếp hạng phải sắp giảm dần", scores.sortedDescending(), scores)
    }

    @Test
    fun achievement_unlock_persists_and_is_idempotent() = runBlocking {
        // unlock() trả true nếu vừa mở khoá, false nếu đã mở trước đó.
        app.achievements.unlock(Achievement.FIRST_BLOOD)
        val unlocked = app.achievements.unlockedFlow.first()
        assertTrue(
            "FIRST_BLOOD phải nằm trong tập đã mở khoá",
            unlocked.contains(Achievement.FIRST_BLOOD.id),
        )
        // Mở khoá lần nữa → không throw, vẫn còn trong tập.
        app.achievements.unlock(Achievement.FIRST_BLOOD)
        assertTrue(app.achievements.unlockedFlow.first().contains(Achievement.FIRST_BLOOD.id))
    }

    @Test
    fun meta_add_minerals_increases_lifetime_balance_by_delta() = runBlocking {
        val before = app.metaProgression.lifetimeMinerals.first()
        app.metaProgression.addMinerals(123)
        val after = app.metaProgression.lifetimeMinerals.first()
        assertEquals("lifetime minerals phải tăng đúng 123", before + 123, after)
    }

    @Test
    fun run_checkpoint_saves_and_clears() = runBlocking {
        val mode = "campaign"
        app.runPersistence.saveCheckpoint(mode, 7)
        assertEquals(7, app.runPersistence.checkpointFor(mode).first())

        app.runPersistence.clearCheckpoint(mode)
        // Sau clear, checkpoint về mặc định (0 = chưa có).
        assertEquals(0, app.runPersistence.checkpointFor(mode).first())
    }
}
