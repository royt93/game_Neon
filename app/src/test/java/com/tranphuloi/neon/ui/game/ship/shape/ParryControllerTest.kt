package com.tranphuloi.neon.ui.game.ship.shape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 13 (đợt 4) — Parry cooldown/window logic (pure, thời gian truyền vào).
 */
class ParryControllerTest {

    private fun ctl() = ParryController(windowMs = 400L, cooldownMs = 5_000L)

    @Test
    fun `kích hoạt lần đầu thành công và mở cửa sổ 400ms`() {
        val p = ctl()
        assertTrue("lần đầu kích hoạt được", p.tryActivate(1000L))
        assertEquals("windowEnd = now + 400", 1400L, p.windowEndMillis)
        assertTrue("trong cửa sổ", p.isActive(1200L))
        assertFalse("ngoài cửa sổ", p.isActive(1500L))
    }

    @Test
    fun `trong cooldown KHÔNG kích hoạt lại`() {
        val p = ctl()
        p.tryActivate(1000L) // cooldownEnd = 6000
        assertFalse("t=3000 < 6000 cd → false", p.tryActivate(3000L))
        assertFalse("chưa ready", p.isReady(3000L))
    }

    @Test
    fun `hết cooldown kích hoạt lại được`() {
        val p = ctl()
        p.tryActivate(1000L)
        assertTrue("t=6000 ready", p.isReady(6000L))
        assertTrue("kích hoạt lại", p.tryActivate(6000L))
        assertEquals("window mới", 6400L, p.windowEndMillis)
    }

    @Test
    fun `progress tăng 0 → 1 trong cooldown`() {
        val p = ctl()
        p.tryActivate(1000L) // cd 1000..6000
        assertEquals(0f, p.progress(1000L), 0.001f)
        assertEquals("2500/5000", 0.5f, p.progress(3500L), 0.01f)
        assertEquals(1f, p.progress(6000L), 0.001f)
        assertEquals("quá cd vẫn 1", 1f, p.progress(7000L), 0.001f)
    }

    @Test
    fun `mới tạo là sẵn sàng`() {
        val p = ctl()
        assertTrue(p.isReady(0L))
        assertEquals(1f, p.progress(0L), 0.001f)
    }

    @Test
    fun `remainingMs đếm ngược đúng`() {
        val p = ctl()
        p.tryActivate(1000L)
        assertEquals(5000L, p.remainingMs(1000L))
        assertEquals(2000L, p.remainingMs(4000L))
        assertEquals(0L, p.remainingMs(6000L))
    }
}
