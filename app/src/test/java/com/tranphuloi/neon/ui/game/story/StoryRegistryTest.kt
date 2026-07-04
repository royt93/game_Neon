package com.tranphuloi.neon.ui.game.story

import com.tranphuloi.neon.R
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 04 — StoryRegistry lookups (@StringRes) cho beat/phase/defeat. Đảm bảo
 * KHÔNG key rỗng (mọi boss có defeat), map đúng theo key, và null đúng chỗ.
 * JUnit4, no-mock. R resolve được trong unit test (generated).
 */
class StoryRegistryTest {

    @Test
    fun `chapter beat exists for chapters 1 through 5 and is unique`() {
        val ids = (1..5).map { StoryRegistry.chapterBeatRes(it) }
        assertTrue("mọi chương 1..5 có beat (khác null, khác 0)", ids.all { it != null && it != 0 })
        assertEquals("5 beat phải phân biệt", 5, ids.toSet().size)
    }

    @Test
    fun `chapter beat is null outside 1 through 5`() {
        assertNull(StoryRegistry.chapterBeatRes(0))
        assertNull(StoryRegistry.chapterBeatRes(6))
        assertNull(StoryRegistry.chapterBeatRes(-1))
    }

    @Test
    fun `final boss phase lines exist only for phase 2 and 3`() {
        assertNull("phase 1 không có thoại", StoryRegistry.finalBossPhaseRes(1))
        assertNotNull("phase 2 có thoại", StoryRegistry.finalBossPhaseRes(2))
        assertNotNull("phase 3 có thoại", StoryRegistry.finalBossPhaseRes(3))
        assertNull("phase 4 (không tồn tại) null", StoryRegistry.finalBossPhaseRes(4))
        assertTrue(
            "phase 2 và 3 khác nhau",
            StoryRegistry.finalBossPhaseRes(2) != StoryRegistry.finalBossPhaseRes(3),
        )
    }

    @Test
    fun `phaseLineOnAdvance fires only when phase increases`() {
        // Không tăng → null (không fire lại cùng phase).
        assertNull("1→1 không fire", StoryRegistry.phaseLineOnAdvance(1, 1))
        assertNull("2→2 không fire", StoryRegistry.phaseLineOnAdvance(2, 2))
        assertNull("3→3 không fire", StoryRegistry.phaseLineOnAdvance(3, 3))
        // Giảm (không xảy ra thực tế nhưng phải an toàn) → null.
        assertNull("3→2 (giảm) không fire", StoryRegistry.phaseLineOnAdvance(3, 2))
        // Tăng → res của phase mới.
        assertEquals("1→2 fire phase2", R.string.story_final_phase2, StoryRegistry.phaseLineOnAdvance(1, 2))
        assertEquals("2→3 fire phase3", R.string.story_final_phase3, StoryRegistry.phaseLineOnAdvance(2, 3))
        // Nhảy cấp 1→3 → lấy thoại phase 3 (không sót/không lặp).
        assertEquals("1→3 nhảy cấp fire phase3", R.string.story_final_phase3, StoryRegistry.phaseLineOnAdvance(1, 3))
    }

    @Test
    fun `every boss kind has a non-empty defeat line`() {
        for (kind in BossKind.entries) {
            assertTrue(
                "boss $kind phải có defeat line (res != 0)",
                StoryRegistry.bossDefeatRes(kind) != 0,
            )
        }
        // null (không rõ kind) cũng phải có fallback.
        assertTrue("kind null → generic fallback", StoryRegistry.bossDefeatRes(null) != 0)
    }

    @Test
    fun `final boss defeat maps to the dedicated line, others may fall back to generic`() {
        assertEquals(
            "SPIDER (Bá Vương) → defeat riêng",
            R.string.story_defeat_final, StoryRegistry.bossDefeatRes(BossKind.SPIDER),
        )
        // Một kind không có câu riêng → generic.
        assertEquals(
            "STAR → generic fallback",
            R.string.story_defeat_generic, StoryRegistry.bossDefeatRes(BossKind.STAR),
        )
        // Kind nổi bật có câu riêng (không phải generic).
        assertTrue(
            "HELL_LORD có defeat riêng (khác generic)",
            StoryRegistry.bossDefeatRes(BossKind.HELL_LORD) != R.string.story_defeat_generic,
        )
    }
}
