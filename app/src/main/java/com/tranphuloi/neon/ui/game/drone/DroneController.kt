package com.tranphuloi.neon.ui.game.drone

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.common.RepeatTime
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.utils.Logger

/**
 * Task 01 — điều khiển drone companion. Logic THUẦN (không Compose/Android):
 *  - orbit: bay vòng quanh tàu, giãn đều góc theo số drone.
 *  - fire: mỗi drone quá cooldown + có địch → sinh [DroneShot] nhắm địch gần nhất.
 *  - damage/HP: trúng thì trừ HP, HP≤0 → vỡ + loại.
 *
 * State ở GameState; controller nhận [setDrones] để publish (đúng convention repo).
 */
class DroneController(
    private val screenWidth: Float,
    private val screenHeight: Float,
    initialDrones: List<Drone> = emptyList(),
    private val maxDrones: Int = 2,
    private val setDrones: (List<Drone>) -> Unit,
) {
    var drones: List<Drone> = initialDrones
        private set

    // tinker ids/repeat-times cho game loop (unique, stable trong 1 controller).
    val orbitId: String = "drone-orbit-${System.identityHashCode(this)}"
    val orbitRepeatTime: RepeatTime = Millis(16)
    val fireId: String = "drone-fire-${System.identityHashCode(this)}"
    val fireRepeatTime: RepeatTime = Millis(120)
    val collisionId: String = "drone-collision-${System.identityHashCode(this)}"
    val collisionRepeatTime: RepeatTime = Millis(16)

    private var idCounter: Int = initialDrones.size

    fun hasDrones(): Boolean = drones.isNotEmpty()
    fun count(): Int = drones.size

    /** Thêm 1 drone (booster pickup) theo [variant]. No-op nếu đã đạt [maxDrones]. */
    fun addDrone(shipX: Float, shipY: Float, variant: DroneVariant = DroneVariant.ATTACK) {
        if (drones.size >= maxDrones) return
        val slot = drones.size
        // Giãn đều góc khởi tạo theo maxDrones để 2 drone không chồng nhau.
        val angle = (2.0 * Math.PI * slot / maxDrones).toFloat()
        val d = Drone(
            id = "drone-${idCounter++}",
            xOffset = shipX + Drone.ORBIT_RADIUS * Math.cos(angle.toDouble()).toFloat(),
            yOffset = shipY + Drone.ORBIT_RADIUS * Math.sin(angle.toDouble()).toFloat(),
            orbitAngle = angle,
            hp = Drone.maxHpFor(variant),
            variant = variant,
        )
        drones = drones + d
        Logger.d("DroneController.addDrone variant=$variant → count=${drones.size}")
        publish()
    }

    /** Tiến quỹ đạo 1 bước quanh tàu; kẹp trong màn hình. */
    fun orbitStep(shipX: Float, shipY: Float) {
        if (drones.isEmpty()) return
        drones = drones.map { d ->
            val a = d.orbitAngle + Drone.ORBIT_SPEED
            val nx = (shipX + Drone.ORBIT_RADIUS * Math.cos(a.toDouble()).toFloat())
                .coerceIn(d.size, screenWidth - d.size)
            val ny = (shipY + Drone.ORBIT_RADIUS * Math.sin(a.toDouble()).toFloat())
                .coerceIn(d.size, screenHeight - d.size)
            d.copy(orbitAngle = a, xOffset = nx, yOffset = ny)
        }
        publish()
    }

    /** Địch còn sống gần [fromX,fromY] nhất (theo bình phương khoảng cách). */
    fun nearestEnemy(fromX: Float, fromY: Float, enemies: List<Enemy>): Enemy? {
        var best: Enemy? = null
        var bestD2 = Float.MAX_VALUE
        for (e in enemies) {
            if (e.destroyed) continue
            val cx = e.xOffset + e.width / 2f
            val cy = e.yOffset + e.height / 2f
            val dx = cx - fromX
            val dy = cy - fromY
            val d2 = dx * dx + dy * dy
            if (d2 < bestD2) {
                bestD2 = d2
                best = e
            }
        }
        return best
    }

    /**
     * Mỗi drone quá cooldown + có địch → 1 [DroneShot] nhắm địch gần nhất; cập nhật
     * lastFireMillis. Trả danh sách shot để GameState dựng laser (dùng chung hệ laser).
     */
    fun fireStep(nowMillis: Long, enemies: List<Enemy>): List<DroneShot> {
        if (drones.isEmpty() || enemies.isEmpty()) return emptyList()
        val shots = mutableListOf<DroneShot>()
        var changed = false
        drones = drones.map { d ->
            if (d.variant != DroneVariant.ATTACK) return@map d // chỉ ATTACK bắn
            if (nowMillis - d.lastFireMillis < Drone.FIRE_INTERVAL_MS) return@map d
            val target = nearestEnemy(d.xOffset, d.yOffset, enemies) ?: return@map d
            shots += DroneShot(
                fromX = d.xOffset,
                fromY = d.yOffset,
                targetX = target.xOffset + target.width / 2f,
                targetY = target.yOffset + target.height / 2f,
            )
            changed = true
            d.copy(lastFireMillis = nowMillis)
        }
        if (changed) publish()
        return shots
    }

    /**
     * Task 01 (Slice 6) — va chạm drone ↔ đạn địch. Mỗi đạn địch (chưa destroyed)
     * chồng lên 1 drone → đạn tan (destroyed=true) + drone trừ HP bằng impactPower.
     * HP≤0 → drone vỡ + loại. Trả [DroneHit] để GameState nổ/spark/haptic.
     *
     * AABB thuần (không phụ thuộc Compose) để test JVM. Drone rect = [xOffset..
     * xOffset+size] × [yOffset..yOffset+size], khớp cách [DroneCanvas] vẽ (tâm =
     * xOffset+size/2). 1 đạn chỉ trúng 1 drone (drone sau bỏ qua đạn đã destroyed).
     */
    fun monitorDroneCollision(enemyLasers: List<Laser>): List<DroneHit> {
        if (drones.isEmpty() || enemyLasers.isEmpty()) return emptyList()
        val hits = mutableListOf<DroneHit>()
        var changed = false
        drones = drones.mapNotNull { d ->
            var hp = d.hp
            val dx1 = d.xOffset
            val dy1 = d.yOffset
            val dx2 = d.xOffset + d.size
            val dy2 = d.yOffset + d.size
            for (l in enemyLasers) {
                if (l.destroyed) continue
                val lx1 = l.xOffset
                val ly1 = l.yOffset
                val lx2 = l.xOffset + l.width
                val ly2 = l.yOffset + l.height
                val overlaps = dx1 < lx2 && dx2 > lx1 && dy1 < ly2 && dy2 > ly1
                if (!overlaps) continue
                l.destroyed = true
                hp -= l.impactPower.toInt()
                val cx = d.xOffset + d.size / 2f
                val cy = d.yOffset + d.size / 2f
                if (hp <= 0) {
                    hits += DroneHit(cx, cy, destroyed = true)
                    break // drone vỡ — không ăn thêm đạn
                } else {
                    hits += DroneHit(cx, cy, destroyed = false)
                }
            }
            when {
                hp <= 0 -> { changed = true; null }
                hp != d.hp -> { changed = true; d.copy(hp = hp) }
                else -> d
            }
        }
        if (changed) publish()
        return hits
    }

    /**
     * Task 06 — HEAL drone hồi máu tàu: mỗi HEAL drone quá cooldown → +HEAL_AMOUNT.
     * Trả tổng HP cần hồi tick này (GameState áp qua shipController.healCapped) +
     * cập nhật lastFireMillis (tái dùng làm mốc hồi). 0 nếu không có HEAL drone sẵn.
     */
    fun healStep(nowMillis: Long): Int {
        if (drones.isEmpty()) return 0
        var total = 0
        var changed = false
        drones = drones.map { d ->
            if (d.variant != DroneVariant.HEAL) return@map d
            if (nowMillis - d.lastFireMillis < Drone.HEAL_INTERVAL_MS) return@map d
            total += Drone.HEAL_AMOUNT
            changed = true
            d.copy(lastFireMillis = nowMillis)
        }
        if (changed) publish()
        return total
    }

    /** Trừ HP drone; loại nếu vỡ. */
    fun damage(droneId: String, dmg: Int) {
        var changed = false
        drones = drones.mapNotNull { d ->
            if (d.id != droneId) return@mapNotNull d
            changed = true
            val hp = d.hp - dmg
            if (hp <= 0) {
                Logger.d("DroneController: drone $droneId vỡ")
                null
            } else {
                d.copy(hp = hp)
            }
        }
        if (changed) publish()
    }

    /** Dọn drone đã vỡ (HP≤0) nếu có. */
    fun processDrones() {
        val alive = drones.filter { !it.destroyed }
        if (alive.size != drones.size) {
            drones = alive
            publish()
        }
    }

    private fun publish() = setDrones(drones)
}
