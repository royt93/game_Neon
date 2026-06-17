package com.tranphuloi.neon.ui.game.status

import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 18 — chứng minh wiring "đạn → status khi trúng" cho batch 3 (và giữ
 * nguyên batch trước). Đây là SSOT mà GameState.onLaserHit gọi.
 */
class BulletOnHitStatusTest {

    @Test
    fun `FISH_SAUCE applies CORROSION (ăn mòn DoT)`() {
        assertEquals(
            listOf(StatusEffect.CORROSION),
            BulletOnHitStatus.statusEffectsFor(BulletType.FISH_SAUCE),
        )
    }

    @Test
    fun `QR_CODE applies BOTH SLOW and STUN (đơ máy)`() {
        assertEquals(
            setOf(StatusEffect.SLOW, StatusEffect.STUN),
            BulletOnHitStatus.statusEffectsFor(BulletType.QR_CODE).toSet(),
        )
    }

    @Test
    fun `DURIAN slows and HEART stuns (batch 2 giữ nguyên)`() {
        assertEquals(listOf(StatusEffect.SLOW), BulletOnHitStatus.statusEffectsFor(BulletType.DURIAN))
        assertEquals(listOf(StatusEffect.STUN), BulletOnHitStatus.statusEffectsFor(BulletType.HEART))
    }

    @Test
    fun `bullets without an on-hit debuff return empty`() {
        listOf(
            BulletType.NORMAL, BulletType.PLASMA,
            BulletType.BUBBLE_TEA, BulletType.SANDAL,
        ).forEach {
            assertTrue("$it không nên có status đảm bảo", BulletOnHitStatus.statusEffectsFor(it).isEmpty())
        }
    }

    @Test
    fun `FIRE and ATOMIC excluded here to avoid double-apply (BURN áp ở nhánh if riêng)`() {
        assertTrue(BulletOnHitStatus.statusEffectsFor(BulletType.FIRE).isEmpty())
        assertTrue(BulletOnHitStatus.statusEffectsFor(BulletType.ATOMIC).isEmpty())
    }
}
