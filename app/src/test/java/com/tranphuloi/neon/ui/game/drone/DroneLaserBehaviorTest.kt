package com.tranphuloi.neon.ui.game.drone

import com.tranphuloi.neon.ui.game.laser.LaserSource
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * Task 01 (Slice 3) — hành vi [DroneLaser]: bay theo hướng tới địch, chuẩn hoá
 * tốc độ, tự huỷ khi ra màn, và cờ nguồn = DRONE. JUnit4, no-mock (đúng convention).
 */
class DroneLaserBehaviorTest {

    private val screenW = 1000f
    private val screenH = 2000f

    @Test
    fun `aimedAt normalizes velocity to SPEED`() {
        val l = DroneLaser.aimedAt(
            id = "d", fromX = 500f, fromY = 1000f,
            targetX = 800f, targetY = 400f,
            screenWidth = screenW, screenHeight = screenH,
        )
        val speed = sqrt(l.xOffsetMovementSpeed * l.xOffsetMovementSpeed + l.yOffsetMovementSpeed * l.yOffsetMovementSpeed)
        assertEquals("tốc độ phải chuẩn hoá về SPEED / speed must normalize to SPEED", DroneLaser.SPEED, speed, 0.01f)
    }

    @Test
    fun `aimedAt points toward a target above and to the right`() {
        val l = DroneLaser.aimedAt(
            id = "d", fromX = 500f, fromY = 1000f,
            targetX = 800f, targetY = 400f, // phải + trên
            screenWidth = screenW, screenHeight = screenH,
        )
        assertTrue("địch bên phải → vx>0 / target right → vx>0", l.xOffsetMovementSpeed > 0f)
        assertTrue("địch phía trên → vy<0 / target above → vy<0", l.yOffsetMovementSpeed < 0f)
    }

    @Test
    fun `aimedAt fires straight up when target coincides with source`() {
        val l = DroneLaser.aimedAt(
            id = "d", fromX = 500f, fromY = 1000f,
            targetX = 500f, targetY = 1000f, // trùng điểm
            screenWidth = screenW, screenHeight = screenH,
        )
        assertEquals("trùng điểm → vx=0 / coincident → vx=0", 0f, l.xOffsetMovementSpeed, 0.001f)
        assertEquals("trùng điểm → bắn thẳng lên / coincident → straight up", -DroneLaser.SPEED, l.yOffsetMovementSpeed, 0.001f)
    }

    @Test
    fun `moveLaser advances by the velocity vector`() {
        val l = DroneLaser.aimedAt(
            id = "d", fromX = 500f, fromY = 1000f,
            targetX = 500f, targetY = 0f, // thẳng lên
            screenWidth = screenW, screenHeight = screenH,
        )
        val y0 = l.yOffset
        l.moveLaser()
        assertEquals("y giảm đúng SPEED mỗi tick / y drops by SPEED per tick", y0 - DroneLaser.SPEED, l.yOffset, 0.001f)
    }

    @Test
    fun `moveLaser stays alive while on screen`() {
        val l = DroneLaser.aimedAt(
            id = "d", fromX = 500f, fromY = 1000f,
            targetX = 500f, targetY = 0f,
            screenWidth = screenW, screenHeight = screenH,
        )
        l.moveLaser()
        assertFalse("còn trong màn → chưa huỷ / on-screen → not destroyed", l.destroyed)
    }

    @Test
    fun `moveLaser self-destroys after leaving the top of the screen`() {
        val l = DroneLaser.aimedAt(
            id = "d", fromX = 500f, fromY = 50f,
            targetX = 500f, targetY = 0f, // bay thẳng lên, gần mép trên
            screenWidth = screenW, screenHeight = screenH,
        )
        // Đủ tick để vượt -OFF_SCREEN_MARGIN.
        repeat(40) { l.moveLaser() }
        assertTrue("bay khỏi mép trên → tự huỷ / off top → self-destroy", l.destroyed)
    }

    @Test
    fun `carries drone source, normal bullet type and drone damage`() {
        val l = DroneLaser.aimedAt(
            id = "d", fromX = 0f, fromY = 0f,
            targetX = 10f, targetY = 10f,
            screenWidth = screenW, screenHeight = screenH,
        )
        assertEquals("nguồn = DRONE / source = DRONE", LaserSource.DRONE, l.source)
        assertEquals("bullet type = NORMAL", BulletType.NORMAL, l.bulletType)
        assertEquals("damage = Drone.SHOT_DAMAGE", Drone.SHOT_DAMAGE, l.impactPower, 0.001f)
    }
}
