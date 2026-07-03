package com.tranphuloi.neon.ui.game.drone

/** Task 01 — map [Drone] domain → [DroneUI]. Gọi cuối `rememberGameState()`. */
class DroneToDroneUIMapper {
    operator fun invoke(drone: Drone): DroneUI = DroneUI(
        xOffset = drone.xOffset,
        yOffset = drone.yOffset,
        size = drone.size,
        hpRatio = (drone.hp.toFloat() / Drone.MAX_HP.toFloat()).coerceIn(0f, 1f),
    )
}
