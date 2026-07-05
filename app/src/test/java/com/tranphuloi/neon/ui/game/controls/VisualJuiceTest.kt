package com.tranphuloi.neon.ui.game.controls

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Task 14 (đợt 4) — cường độ screen-tear tắt dần theo thời gian sau damage (pure).
 */
class VisualJuiceTest {

    @Test
    fun `alpha = 1 ngay khi trúng đòn`() {
        assertEquals(1f, VisualJuice.damageTearAlpha(0L, 250L), 0.0001f)
    }

    @Test
    fun `alpha giảm tuyến tính giữa cửa sổ`() {
        assertEquals("giữa 250ms → 0.5", 0.5f, VisualJuice.damageTearAlpha(125L, 250L), 0.0001f)
    }

    @Test
    fun `alpha = 0 khi hết duration`() {
        assertEquals(0f, VisualJuice.damageTearAlpha(250L, 250L), 0.0001f)
    }

    @Test
    fun `alpha coerce về 0 khi quá duration`() {
        assertEquals(0f, VisualJuice.damageTearAlpha(400L, 250L), 0.0001f)
    }

    @Test
    fun `duration không hợp lệ trả 0`() {
        assertEquals(0f, VisualJuice.damageTearAlpha(10L, 0L), 0.0001f)
    }
}
