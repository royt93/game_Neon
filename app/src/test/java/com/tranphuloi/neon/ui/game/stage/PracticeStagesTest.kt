package com.tranphuloi.neon.ui.game.stage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task: QoL Practice — helper chọn chương luyện tập (thuần, trên script `stages`).
 */
class PracticeStagesTest {

    @Test
    fun `chapterStartIndex chương 1 là 0`() {
        assertEquals("chương 1 mở đầu script", 0, chapterStartIndex(1))
    }

    @Test
    fun `chapterStartIndex tăng dần theo chương`() {
        assertTrue("ch2 sau ch1", chapterStartIndex(2) > chapterStartIndex(1))
        assertTrue("ch3 sau ch2", chapterStartIndex(3) > chapterStartIndex(2))
        assertTrue("ch5 sau ch4", chapterStartIndex(5) > chapterStartIndex(4))
    }

    @Test
    fun `maxUnlockedChapter đầu run luôn 1`() {
        assertEquals(1, maxUnlockedChapter(0))
        assertEquals(1, maxUnlockedChapter(-9))
    }

    @Test
    fun `maxUnlockedChapter suy đúng từ checkpoint`() {
        assertEquals(3, maxUnlockedChapter(chapterStartIndex(3)))
        assertEquals(5, maxUnlockedChapter(chapterStartIndex(5)))
    }
}
