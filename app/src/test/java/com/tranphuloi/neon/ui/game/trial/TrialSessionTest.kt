package com.tranphuloi.neon.ui.game.trial

import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 25 (#trial) — pin hành vi TrialSession (set/clear) + TrialSpec.Bullet.
 * Singleton tiến-trình → dọn sau mỗi test để không rò trạng thái.
 */
class TrialSessionTest {

    @After
    fun tearDown() = TrialSession.clear()

    @Test
    fun `default spec is null (run thuong)`() {
        TrialSession.clear()
        assertNull(TrialSession.spec)
    }

    @Test
    fun `set bullet trial holds the type`() {
        TrialSession.spec = TrialSpec.Bullet(BulletType.KAMEHAMEHA)
        val s = TrialSession.spec
        assertTrue(s is TrialSpec.Bullet)
        assertEquals(BulletType.KAMEHAMEHA, (s as TrialSpec.Bullet).type)
    }

    @Test
    fun `clear resets to null`() {
        TrialSession.spec = TrialSpec.Bullet(BulletType.PLASMA)
        TrialSession.clear()
        assertNull(TrialSession.spec)
    }
}
