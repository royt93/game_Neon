package com.tranphuloi.neon.ui.game.mineral.controller

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.mineral.model.Mineral
import com.tranphuloi.neon.utils.Logger
import java.util.*

class MineralsController(
    initialMinerals: List<Mineral>,
    private val updateMinerals: (List<Mineral>) -> Unit,
    private val updateMineralsEarnedTotal: (Int) -> Unit,
    private val getShipCenter: () -> Pair<Float, Float>,
    private val getMagnetRadius: () -> Float,
) {

    init {
        Logger.d("MineralsController init: initialMinerals=${initialMinerals.size}")
    }

    private var minerals: List<Mineral> = initialMinerals
    private var pickedThisTick: Int = 0

    fun addMinerals(xOffset: Float, yOffset: Float, width: Float, mineralAmount: Int) {
        val mineral = Mineral(
            xOffset = xOffset,
            yOffset = yOffset,
            width = width
        )
        minerals += mineral
        Logger.v { "MineralsController.addMinerals: at (${xOffset.toInt()},${yOffset.toInt()}) +$mineralAmount → total active=${minerals.size}" }
        updateMinerals(minerals)
        updateMineralsEarnedTotal(mineralAmount)
    }

    val processMineralsId = UUID.randomUUID().toString()
    val processMineralsRepeatTime = Millis(5)

    fun processMinerals() {
        val (cx, cy) = getShipCenter()
        val r = getMagnetRadius()
        var picked = 0
        minerals.forEach { mineral ->
            val wasRemoved = mineral.removed
            mineral.process(
                magnetTargetX = cx,
                magnetTargetY = cy,
                magnetRadius = r
            )
            if (mineral.removed && !wasRemoved) {
                // Magnet pickup → award the same +1 like death-pickup. The original pickup
                // happened at addMinerals time; magnet doesn't double-count by design,
                // it just removes the mineral from the world.
                picked++
            }
        }
        minerals = minerals.filterNot { it.removed }
        // Round 37 — was logging picked count per 5ms tick. During a magnet sweep
        // through a mineral cluster this fired up to 200×/sec. GameState already
        // emits a per-event log when minerals are awarded via consumePickedThisTick.
        pickedThisTick = picked
        updateMinerals(minerals)
    }

    /**
     * Number of minerals magnet-picked in the most recent processMinerals() call.
     * Consumed by GameState to fire haptic / SFX on pickup.
     */
    fun consumePickedThisTick(): Int {
        val v = pickedThisTick
        pickedThisTick = 0
        return v
    }
}
