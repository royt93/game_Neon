package com.tranphuloi.neon.data

import com.tranphuloi.neon.ui.game.controls.BossRank
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.ship.laser.BulletType

/**
 * Wave 11c — immutable point-in-time snapshot of one run's telemetry.
 *
 * Produced by `GameState.snapshotRunTelemetry()` under thread-safe lock at
 * GAME_OVER. Consumed by GameScreen to build RunStats (for DialogGameOver
 * display) and to call MetaProgressionRepository.recordRunMetrics (for
 * persistence). Both consumers see the same atomic view.
 *
 * Replaces per-frame `.toMap()` / `.toList()` allocation inside the
 * GameState data class (audit P2-1).
 */
data class RunTelemetrySnapshot(
    val bulletKills: Map<BulletType, Int>,
    val bossKills: Map<BossKind, Int>,
    val ranksAchieved: List<BossRank>,
    val shipSkin: ShipSkin,
    val shipTimeMillis: Long,
) {
    companion object {
        val EMPTY = RunTelemetrySnapshot(
            bulletKills = emptyMap(),
            bossKills = emptyMap(),
            ranksAchieved = emptyList(),
            shipSkin = ShipSkin.AURA_CYAN,
            shipTimeMillis = 0L,
        )
    }
}
