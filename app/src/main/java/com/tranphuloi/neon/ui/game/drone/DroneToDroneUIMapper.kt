package com.tranphuloi.neon.ui.game.drone

/** Task 01 — map [Drone] domain → [DroneUI]. Gọi cuối `rememberGameState()`. */
class DroneToDroneUIMapper {
    operator fun invoke(drone: Drone): DroneUI = DroneUI(
        xOffset = drone.xOffset,
        yOffset = drone.yOffset,
        size = drone.size,
        // Task 06 — chia theo maxHp của variant (SHIELD ×2) để vòng HP không >1.
        hpRatio = (drone.hp.toFloat() / Drone.maxHpFor(drone.variant).toFloat()).coerceIn(0f, 1f),
        variant = drone.variant,
    )
}
