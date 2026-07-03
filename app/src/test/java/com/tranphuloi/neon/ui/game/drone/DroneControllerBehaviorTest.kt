package com.tranphuloi.neon.ui.game.drone

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.Laser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Behavior tests for [DroneController] (Task 01, Slice 1) — orbit quanh tàu trong
 * bound, nhắm địch gần nhất, cooldown bắn, và HP/vỡ. Logic thuần nên test JVM.
 */
class DroneControllerBehaviorTest {

    private val screenWidth = 400f
    private val screenHeight = 800f
    private val shipX = 200f
    private val shipY = 400f

    private fun newController(
        initial: List<Drone> = emptyList(),
        maxDrones: Int = 2,
    ): Pair<DroneController, MutableList<List<Drone>>> {
        val captured = mutableListOf<List<Drone>>()
        val ctrl = DroneController(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            initialDrones = initial,
            maxDrones = maxDrones,
            setDrones = { captured.add(it) },
        )
        return ctrl to captured
    }

    // ── addDrone ──

    @Test
    fun `addDrone is a no-op when maxDrones is zero (drone locked)`() {
        // Task 01 (Slice 5) — rank 0 ⇒ maxDrones 0 ⇒ nhặt DRONE_BOOSTER không spawn.
        val (ctrl, _) = newController(maxDrones = 0)
        ctrl.addDrone(shipX, shipY)
        assertEquals("chưa mở khoá (maxDrones=0) thì không spawn drone", 0, ctrl.count())
        assertTrue("không có drone", !ctrl.hasDrones())
    }

    @Test
    fun `addDrone caps at maxDrones`() {
        val (ctrl, _) = newController(maxDrones = 2)
        ctrl.addDrone(shipX, shipY)
        ctrl.addDrone(shipX, shipY)
        ctrl.addDrone(shipX, shipY) // vượt trần → no-op
        assertEquals("chỉ giữ tối đa maxDrones=2", 2, ctrl.count())
    }

    @Test
    fun `addDrone spawns on orbit radius around ship`() {
        val (ctrl, _) = newController()
        ctrl.addDrone(shipX, shipY)
        val d = ctrl.drones.single()
        val dist = hypot(d.xOffset - shipX, d.yOffset - shipY)
        assertTrue("drone spawn cách tàu ~ORBIT_RADIUS, thực tế=$dist", kotlin.math.abs(dist - Drone.ORBIT_RADIUS) < 1f)
    }

    // ── orbitStep ──

    @Test
    fun `orbitStep keeps drones within screen bounds`() {
        val (ctrl, _) = newController()
        ctrl.addDrone(shipX, shipY)
        repeat(200) { ctrl.orbitStep(shipX, shipY) }
        ctrl.drones.forEach { d ->
            assertTrue("x trong bound", d.xOffset in d.size..(screenWidth - d.size))
            assertTrue("y trong bound", d.yOffset in d.size..(screenHeight - d.size))
        }
    }

    @Test
    fun `orbitStep advances orbit angle`() {
        val (ctrl, _) = newController()
        ctrl.addDrone(shipX, shipY)
        val a0 = ctrl.drones.single().orbitAngle
        ctrl.orbitStep(shipX, shipY)
        assertEquals("góc tăng đúng ORBIT_SPEED", a0 + Drone.ORBIT_SPEED, ctrl.drones.single().orbitAngle, 1e-4f)
    }

    // ── nearestEnemy ──

    @Test
    fun `nearestEnemy picks closest alive enemy and skips destroyed`() {
        val (ctrl, _) = newController()
        val far = fakeEnemy("far", 380f, 20f)
        val near = fakeEnemy("near", 210f, 410f)
        val nearestButDead = fakeEnemy("dead", 200f, 400f, destroyed = true)
        val pick = ctrl.nearestEnemy(shipX, shipY, listOf(far, nearestButDead, near))
        assertEquals("phải chọn 'near' (bỏ qua địch đã chết dù gần hơn)", "near", pick?.enemyId)
    }

    @Test
    fun `nearestEnemy returns null when no live enemy`() {
        val (ctrl, _) = newController()
        assertNull(ctrl.nearestEnemy(shipX, shipY, listOf(fakeEnemy("d", 10f, 10f, destroyed = true))))
    }

    // ── fireStep ──

    @Test
    fun `fireStep produces no shot without enemies`() {
        val (ctrl, _) = newController()
        ctrl.addDrone(shipX, shipY)
        assertTrue("không địch → không bắn", ctrl.fireStep(1_000L, emptyList()).isEmpty())
    }

    @Test
    fun `fireStep fires once then respects cooldown`() {
        val (ctrl, _) = newController(maxDrones = 1)
        ctrl.addDrone(shipX, shipY)
        val enemies = listOf(fakeEnemy("e", 200f, 100f))

        val first = ctrl.fireStep(1_000L, enemies)
        assertEquals("lần đầu (quá cooldown từ 0) phải bắn 1 phát", 1, first.size)

        val tooSoon = ctrl.fireStep(1_000L + Drone.FIRE_INTERVAL_MS - 50, enemies)
        assertTrue("trong cooldown → không bắn", tooSoon.isEmpty())

        val later = ctrl.fireStep(1_000L + Drone.FIRE_INTERVAL_MS + 1, enemies)
        assertEquals("qua cooldown → bắn lại", 1, later.size)
    }

    @Test
    fun `fireStep shot aims at nearest enemy center`() {
        val (ctrl, _) = newController(maxDrones = 1)
        ctrl.addDrone(shipX, shipY)
        val e = fakeEnemy("e", 100f, 100f) // center (130,130) với width/height=60
        val shot = ctrl.fireStep(5_000L, listOf(e)).single()
        assertEquals("targetX = enemy center x", 130f, shot.targetX)
        assertEquals("targetY = enemy center y", 130f, shot.targetY)
    }

    // ── damage / HP ──

    @Test
    fun `damage reduces hp and removes drone when hp drops to zero`() {
        val (ctrl, _) = newController(maxDrones = 1)
        ctrl.addDrone(shipX, shipY)
        val id = ctrl.drones.single().id

        ctrl.damage(id, 10)
        assertEquals("còn sống sau damage nhẹ", Drone.MAX_HP - 10, ctrl.drones.single().hp)

        ctrl.damage(id, Drone.MAX_HP)
        assertTrue("HP≤0 → drone bị loại", ctrl.drones.isEmpty())
    }

    @Test
    fun `processDrones removes destroyed drones`() {
        val dead = Drone(id = "d0", xOffset = 100f, yOffset = 100f, orbitAngle = 0f, hp = 0)
        val (ctrl, captured) = newController(initial = listOf(dead))
        ctrl.processDrones()
        assertTrue("drone hp=0 phải bị dọn", ctrl.drones.isEmpty())
        assertTrue("phải publish list rỗng", captured.last().isEmpty())
    }

    // ── Slice 6 — monitorDroneCollision ──

    @Test
    fun `enemy laser overlapping a drone damages it and consumes the laser`() {
        val drone = Drone(id = "d0", xOffset = 100f, yOffset = 100f, orbitAngle = 0f) // rect [100..144]
        val (ctrl, _) = newController(initial = listOf(drone), maxDrones = 2)
        val laser = fakeLaser(x = 110f, y = 110f, dmg = 15f)
        val hits = ctrl.monitorDroneCollision(listOf(laser))
        assertEquals("1 cú trúng", 1, hits.size)
        assertTrue("chưa vỡ (hp còn)", !hits[0].destroyed)
        assertTrue("đạn địch tan sau khi trúng drone", laser.destroyed)
        assertEquals("drone còn sống", 1, ctrl.count())
        assertEquals("HP trừ đúng impactPower", Drone.MAX_HP - 15, ctrl.drones[0].hp)
    }

    @Test
    fun `drone breaks when a hit drops HP to zero`() {
        val drone = Drone(id = "d0", xOffset = 100f, yOffset = 100f, orbitAngle = 0f, hp = 20)
        val (ctrl, _) = newController(initial = listOf(drone), maxDrones = 2)
        val laser = fakeLaser(x = 110f, y = 110f, dmg = 40f)
        val hits = ctrl.monitorDroneCollision(listOf(laser))
        assertEquals(1, hits.size)
        assertTrue("cú này khiến drone vỡ", hits[0].destroyed)
        assertTrue("drone bị loại", ctrl.drones.isEmpty())
    }

    @Test
    fun `non-overlapping enemy laser does not hit the drone`() {
        val drone = Drone(id = "d0", xOffset = 100f, yOffset = 100f, orbitAngle = 0f)
        val (ctrl, _) = newController(initial = listOf(drone), maxDrones = 2)
        val laser = fakeLaser(x = 300f, y = 300f, dmg = 15f)
        val hits = ctrl.monitorDroneCollision(listOf(laser))
        assertTrue("không trúng", hits.isEmpty())
        assertTrue("đạn còn sống", !laser.destroyed)
        assertEquals("HP không đổi", Drone.MAX_HP, ctrl.drones[0].hp)
    }

    @Test
    fun `already-destroyed enemy laser is ignored`() {
        val drone = Drone(id = "d0", xOffset = 100f, yOffset = 100f, orbitAngle = 0f)
        val (ctrl, _) = newController(initial = listOf(drone), maxDrones = 2)
        val laser = fakeLaser(x = 110f, y = 110f, dmg = 15f, destroyed = true)
        val hits = ctrl.monitorDroneCollision(listOf(laser))
        assertTrue("đạn đã destroyed → bỏ qua", hits.isEmpty())
        assertEquals("HP không đổi", Drone.MAX_HP, ctrl.drones[0].hp)
    }

    @Test
    fun `one enemy laser hits at most one drone`() {
        val a = Drone(id = "a", xOffset = 100f, yOffset = 100f, orbitAngle = 0f) // [100..144]
        val b = Drone(id = "b", xOffset = 120f, yOffset = 100f, orbitAngle = 0f) // [120..164]
        val (ctrl, _) = newController(initial = listOf(a, b), maxDrones = 2)
        val laser = fakeLaser(x = 130f, y = 110f, w = 4f, dmg = 15f) // chồng cả a và b
        val hits = ctrl.monitorDroneCollision(listOf(laser))
        assertEquals("1 đạn chỉ trúng 1 drone", 1, hits.size)
        assertEquals("cả 2 drone còn sống", 2, ctrl.count())
    }

    // ── fake Laser tối giản (chỉ cần vị trí/size/impactPower/destroyed) ──
    private fun fakeLaser(
        x: Float, y: Float, w: Float = 12f, h: Float = 24f, dmg: Float = 15f, destroyed: Boolean = false,
    ): Laser = object : Laser {
        override val id = "l-$x-$y"
        override var xOffset = x
        override var yOffset = y
        override var width = w
        override var height = h
        override var rotation = 0f
        override var impactPower = dmg
        override val drawableId = 0
        override val xOffsetMovementSpeed = 0f
        override val yOffsetMovementSpeed = 0f
        override var destroyed = destroyed
        override fun moveLaser() {}
    }

    // ── fake Enemy tối giản (chỉ cần vị trí + destroyed) ──
    private fun fakeEnemy(id: String, x: Float, y: Float, destroyed: Boolean = false): Enemy =
        object : Enemy {
            override val enemyId = id
            override val width = 60f
            override val height = 60f
            override var xOffset = x
            override var yOffset = y
            override var hp = if (destroyed) 0f else 100f
            override val initialHp = 100f
            override val impactPower = 10f
            override val drawableId = 0
            override val minerals = 1
            override val destroyed = destroyed
            override val outOfScreen = false
            override var lastImpactMillis = 0L
            override val isBoss = false
            override val displayName = "fake"
            override fun enemyRect() =
                androidx.compose.ui.geometry.Rect(xOffset, yOffset, xOffset + width, yOffset + height)
            override fun process() {}
            override fun generateLasers(): List<Laser> = emptyList()
            override fun onObjectImpact(impactPower: Float) {}
        }
}
