package com.tranphuloi.neon.ui.game.state

import com.tranphuloi.neon.ui.game.ship.shape.ShipXpLevels

/**
 * Task 07 — điều kiện mở thành tựu THUẦN (testable, single-source cho ngưỡng).
 * Tách khỏi wiring GameState/GameScreen để unit-test + tránh drift ngưỡng.
 */
object AchievementUnlocks {
    const val LIGHTNING_KILLS_TARGET: Int = 100
    const val SHIP_COLLECTOR_TARGET: Int = 10
    const val DRONE_DUO_COUNT: Int = 2
    const val CHAIN_TRIPLE_HOPS: Int = 3

    /** DRONE_DUO — nuôi ≥2 drone cùng lúc. */
    fun droneDuo(droneCount: Int): Boolean = droneCount >= DRONE_DUO_COUNT

    /** CHAIN_TRIPLE — 1 phát sét lan trúng ≥3 địch. */
    fun chainTriple(chainHops: Int): Boolean = chainHops >= CHAIN_TRIPLE_HOPS

    /** LIGHTNING_MASTER — tích luỹ ≥100 kill bằng Sét Chain (lifetime). */
    fun lightningMaster(lightningKills: Int): Boolean = lightningKills >= LIGHTNING_KILLS_TARGET

    /** SHIP_MAX_LEVEL — có ít nhất 1 tàu đạt cấp tối đa. */
    fun shipMaxLevel(allShipXp: Map<String, Int>): Boolean =
        allShipXp.values.any { ShipXpLevels.levelForXp(it) >= ShipXpLevels.MAX_LEVEL }

    /** SHIP_COLLECTOR — sở hữu ≥10 tàu. */
    fun shipCollector(ownedShipCount: Int): Boolean = ownedShipCount >= SHIP_COLLECTOR_TARGET
}
