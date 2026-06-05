package com.tranphuloi.neon.ui.game.enemy.laser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 17 — pin the non-linear [EnemyLaser] trajectories (HOMING / ACCEL /
 * CURVE) added so bosses can have genuinely distinct attack feels (vs every
 * bullet flying in a straight line). LINEAR must stay byte-for-byte unchanged.
 */
class EnemyLaserMotionTest {

    private fun laser(
        motion: LaserMotion,
        xs: Float = 0f,
        ys: Float = 1f,
        x: Float = 100f,
        y: Float = 100f,
    ) = EnemyLaser(
        xOffset = x, yOffset = y, yRange = 800f, width = 20f,
        xOffsetMovementSpeed = xs, yOffsetMovementSpeed = ys, motion = motion,
    )

    @Test
    fun `LINEAR is unchanged — moves by exactly its spawn velocity`() {
        val l = laser(LaserMotion.LINEAR, xs = 2f, ys = 3f)
        l.moveLaser()
        assertEquals(102f, l.xOffset, 0.001f)
        assertEquals(103f, l.yOffset, 0.001f)
    }

    @Test
    fun `HOMING steers toward the target over time`() {
        val l = laser(LaserMotion.HOMING, xs = 0f, ys = 1f, x = 100f, y = 100f)
        l.targetX = 320f                 // target far to the right + below
        l.targetY = 500f
        val x0 = l.xOffset
        repeat(80) { l.moveLaser() }
        assertTrue("homing bullet must curve toward target x (was $x0, now ${l.xOffset})", l.xOffset > x0 + 30f)
        assertTrue("and descend toward target y", l.yOffset > 100f)
        assertTrue("must close horizontal gap to target", kotlin.math.abs(l.xOffset - 320f) < kotlin.math.abs(x0 - 320f))
    }

    @Test
    fun `ACCEL speeds up — later steps cover more distance than early ones`() {
        val l = laser(LaserMotion.ACCEL, xs = 0f, ys = 0.5f)
        val yStart = l.yOffset
        l.moveLaser()
        val firstStep = l.yOffset - yStart
        repeat(25) { l.moveLaser() }
        val before = l.yOffset
        l.moveLaser()
        val laterStep = l.yOffset - before
        assertTrue("later step ($laterStep) must exceed first step ($firstStep)", laterStep > firstStep * 1.5f)
    }

    @Test
    fun `ACCEL is capped (never becomes un-dodgeable)`() {
        val l = laser(LaserMotion.ACCEL, xs = 0f, ys = 1f)
        repeat(300) { l.moveLaser() }
        val before = l.yOffset
        l.moveLaser()
        assertTrue("per-tick speed must stay capped", (l.yOffset - before) <= 4.01f)
    }

    @Test
    fun `HOMING bullet flying off the TOP is culled (no leak)`() {
        // Người chơi né qua → đạn homing quay đầu bay ngược lên. Phải bị cull ở
        // đỉnh, nếu không sẽ kẹt ngoài màn tới khi đầy MAX_ENEMY_LASERS.
        val homing = EnemyLaser(
            xOffset = 100f, yOffset = 40f, yRange = 800f, width = 20f,
            xOffsetMovementSpeed = 0f, yOffsetMovementSpeed = -2f,
            motion = LaserMotion.HOMING,
        )
        val c = EnemyLasersController(
            screenHeight = 800f,
            initialEnemyLasers = listOf(homing),
            setEnemyLasers = {},
            shipPosition = { 100f to -800f },          // "tàu" ở trên → bullet lái lên
        )
        repeat(300) { c.processLasers() }
        assertTrue("đạn homing bay khỏi đỉnh phải bị cull", !c.hasEnemyLasers())
    }

    @Test
    fun `destroying one of two identical-param lasers removes ONLY that instance (id-based)`() {
        // Hai đạn cùng MỌI tham số ctor (data-equals coi bằng nhau) nhưng KHÁC id.
        // Trước fix: `list - laser` value-equals xoá nhầm con còn sống. Sau fix:
        // lọc theo id → diệt đúng con đã destroyed.
        val a = EnemyLaser(
            xOffset = 100f, yOffset = 100f, yRange = 800f, width = 20f,
            xOffsetMovementSpeed = 0f, yOffsetMovementSpeed = 0f,
        )
        val b = EnemyLaser(
            xOffset = 100f, yOffset = 100f, yRange = 800f, width = 20f,
            xOffsetMovementSpeed = 0f, yOffsetMovementSpeed = 0f,
        )
        b.destroyed = true
        val c = EnemyLasersController(
            screenHeight = 800f, initialEnemyLasers = listOf(a, b), setEnemyLasers = {},
        )
        c.processLasers()
        assertEquals("chỉ còn 1 đạn", 1, c.enemyLasers.size)
        assertEquals("đạn SỐNG (a) phải còn, không bị xoá nhầm", a.id, c.enemyLasers.first().id)
    }

    @Test
    fun `bullet drifting off the SIDE is culled (no horizontal leak)`() {
        val drift = EnemyLaser(
            xOffset = 380f, yOffset = 100f, yRange = 800f, width = 20f,
            xOffsetMovementSpeed = 5f, yOffsetMovementSpeed = 0.1f,   // bay sang phải, gần như không xuống
        )
        val c = EnemyLasersController(
            screenHeight = 800f, screenWidth = 400f,
            initialEnemyLasers = listOf(drift), setEnemyLasers = {},
        )
        repeat(200) { c.processLasers() }
        assertTrue("đạn lệch ngang khỏi màn phải bị cull", !c.hasEnemyLasers())
    }

    @Test
    fun `CURVE weaves to BOTH sides while still descending`() {
        val l = laser(LaserMotion.CURVE, xs = 0f, ys = 1f, x = 100f)
        val xs = (1..80).map { l.moveLaser(); l.xOffset }
        assertTrue("curves right of spawn", xs.any { it > 101f })
        assertTrue("curves left of spawn", xs.any { it < 99f })
        assertTrue("still falls downward", l.yOffset > 100f)
    }
}
