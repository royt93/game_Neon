package com.tranphuloi.neon.ui.game.ship.laser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 18 — Dép Lào boomerang: chứng minh quỹ đạo "bay lên → đỉnh → quay về →
 * tự huỷ" và metadata. Thuần JVM (R.drawable trả 0 nhờ returnDefaultValues).
 */
class BoomerangShipLaserTest {

    private fun make(yStart: Float = 700f, yRange: Float = 800f) =
        BoomerangShipLaser(id = "b", xOffset = 100f, yOffset = yStart, yRange = yRange)

    @Test
    fun `rises first (yOffset decreases) and is not yet returning`() {
        val b = make()
        val y0 = b.yOffset
        b.moveLaser()
        assertTrue("phải bay LÊN trước (yOffset giảm)", b.yOffset < y0)
        assertFalse("chưa quay về", b.returning)
    }

    @Test
    fun `turns around at the apex then travels back down`() {
        val b = make(yStart = 700f, yRange = 800f)   // apex = 700 - APEX_RISE(440) = 260
        var guard = 0
        while (!b.returning && guard++ < 1000) b.moveLaser()
        assertTrue("phải đạt đỉnh và đổi sang quay về", b.returning)
        val yAtTurn = b.yOffset
        assertTrue(
            "đỉnh cách điểm phóng ~APEX_RISE",
            yAtTurn <= 700f - BoomerangShipLaser.APEX_RISE + BoomerangShipLaser.BOOMERANG_SPEED,
        )
        b.moveLaser()
        assertTrue("sau đỉnh phải đi XUỐNG (yOffset tăng)", b.yOffset > yAtTurn)
    }

    @Test
    fun `still rises before returning even when launched high on screen`() {
        // launchY 200 < apex cố định cũ (224) → trước đây quay về tức thì (phí).
        val b = make(yStart = 200f, yRange = 800f)
        val y0 = b.yOffset
        b.moveLaser()
        assertTrue("tàu ở nửa trên vẫn bay LÊN trước", b.yOffset < y0)
        assertFalse("KHÔNG quay về tức thì", b.returning)
    }

    @Test
    fun `re-hit cooldown blocks same enemy but allows a different one`() {
        val b = make()
        assertTrue(b.canHit("e1", 1000L))
        b.registerHit("e1", 1000L)
        assertFalse("cùng địch trong cooldown → chặn", b.canHit("e1", 1000L + 100))
        assertTrue("địch KHÁC vẫn trúng được", b.canHit("e2", 1000L + 100))
        assertTrue(
            "hết cooldown (≥180ms) → trúng lại được",
            b.canHit("e1", 1000L + BoomerangShipLaser.REHIT_COOLDOWN_MS),
        )
    }

    @Test
    fun `self-destroys after returning to launch height`() {
        val b = make(yStart = 700f, yRange = 800f)
        var guard = 0
        while (!b.destroyed && guard++ < 5000) b.moveLaser()
        assertTrue("phải tự huỷ khi quay về mức phóng", b.destroyed)
        assertTrue("huỷ ở quanh mức phóng (≈700)", b.yOffset >= 700f - BoomerangShipLaser.BOOMERANG_SPEED)
    }

    @Test
    fun `spins continuously each tick`() {
        val b = make()
        val r0 = b.rotation
        b.moveLaser()
        assertEquals(r0 + BoomerangShipLaser.BOOMERANG_SPIN, b.rotation, 0.001f)
    }

    @Test
    fun `is a SANDAL bullet that can hit 4 times`() {
        val b = make()
        assertEquals(BulletType.SANDAL, b.bulletType)
        assertEquals("đánh được cả lượt đi lẫn về", 4, b.hitsRemaining)
    }
}
