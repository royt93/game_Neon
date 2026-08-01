package com.tranphuloi.neon.ui.game.ship.shape

import androidx.compose.runtime.Immutable

/** Task 33 — averaged stat multipliers for a fused ship pair. */
@Immutable
data class FusedShipStats(
    val hpMul: Float,
    val speedMul: Float,
    val damageMul: Float,
)

/** Task 33 — fusion stat formula: simple average of both ships' base multipliers. */
fun fusedStats(a: ShipShape, b: ShipShape): FusedShipStats = FusedShipStats(
    hpMul = (a.hpMul + b.hpMul) / 2f,
    speedMul = (a.speedMul + b.speedMul) / 2f,
    damageMul = (a.damageMul + b.damageMul) / 2f,
)
