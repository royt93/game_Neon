package com.tranphuloi.neon.ui.game.mineral.controller

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.mineral.model.Mineral
import com.tranphuloi.neon.ui.game.state.EffectiveStats
import com.tranphuloi.neon.utils.Logger
import java.util.*

class MineralsController(
    initialMinerals: List<Mineral>,
    private val updateMinerals: (List<Mineral>) -> Unit,
    private val updateMineralsEarnedTotal: (Int) -> Unit,
    private val getShipCenter: () -> Pair<Float, Float>,
    private val getMagnetRadius: () -> Float,
    /** Task 24 — MAGNET_PULL_SPEED skill node rank; scales pull *speed*, not radius. */
    private val getMagnetPullSpeedRank: () -> Int = { 0 },
    /**
     * Task 24 — MINERAL_BOOST skill node rank; +10%/rank on every mineral award.
     * Single choke point: addMinerals() has 3 call sites in GameState.kt (enemy
     * death, wave-clear bonus, bomb detonation) — applying here covers all of them.
     */
    private val getMineralBoostRank: () -> Int = { 0 },
    /** Task 25 — synergy: MAGNET_BOOST active when SUPERCHARGE fires → +25% bonus. */
    private val isMagnetBoostActive: () -> Boolean = { false },
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
        val boostedAmount = (mineralAmount * (1f + getMineralBoostRank() * EffectiveStats.META_MINERAL_BOOST_PER_RANK))
            .toInt().coerceAtLeast(mineralAmount)
        Logger.v { "MineralsController.addMinerals: at (${xOffset.toInt()},${yOffset.toInt()}) +$boostedAmount (base=$mineralAmount) → total active=${minerals.size}" }
        updateMinerals(minerals)
        updateMineralsEarnedTotal(boostedAmount)
    }

    val processMineralsId = UUID.randomUUID().toString()
    val processMineralsRepeatTime = Millis(5)

    fun processMinerals() {
        val (cx, cy) = getShipCenter()
        val r = getMagnetRadius()
        val pullSpeed = Mineral.MAGNET_PULL_SPEED * (1f + getMagnetPullSpeedRank() * EffectiveStats.META_MAGNET_PULL_SPEED_PER_RANK)
        var picked = 0
        minerals.forEach { mineral ->
            val wasRemoved = mineral.removed
            mineral.process(
                magnetTargetX = cx,
                magnetTargetY = cy,
                magnetRadius = r,
                pullSpeed = pullSpeed,
            )
            if (mineral.removed && !wasRemoved) {
                // Magnet pickup → award the same +1 like death-pickup. The original pickup
                // happened at addMinerals time; magnet doesn't double-count by design,
                // it just removes the mineral from the world.
                picked++
            }
        }
        // Round 37 — was logging picked count per 5ms tick. During a magnet sweep
        // through a mineral cluster this fired up to 200×/sec. GameState already
        // emits a per-event log when minerals are awarded via consumePickedThisTick.
        pickedThisTick = picked
        // Task 36 — `picked > 0` is already the exact signal that something was
        // removed this tick; gate only the list rebuild (rare event). Positions
        // mutate every tick via drift/magnet pull in Mineral.process(), so
        // updateMinerals() must still fire unconditionally to animate movement.
        if (picked > 0) {
            minerals = minerals.filterNot { it.removed }
        }
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

    /**
     * Round 60 (38x) — MINERAL_SUPERCHARGE one-shot effect. Instant-collects
     * every on-screen mineral and awards a bonus score (+5 per mineral) since
     * the per-mineral spawn-time score was already booked at `addMinerals`.
     * The bonus simulates "wow, free score" without changing the spawn flow.
     */
    fun flushAllToShip() {
        val count = minerals.size
        if (count == 0) {
            Logger.d("MineralsController.flushAllToShip: no minerals on screen — no-op")
            return
        }
        // Task 25 — synergy pair "Kinh tế": MAGNET_BOOST is duration-tracked but
        // SUPERCHARGE is instant, so "both active" is checked at the moment this
        // one-shot fires rather than continuously.
        val bonus = (count * 5 * (if (isMagnetBoostActive()) 1.25f else 1f)).toInt()
        Logger.d("MineralsController.flushAllToShip: instant-collect $count minerals → bonus +$bonus")
        minerals = emptyList()
        updateMinerals(minerals)
        updateMineralsEarnedTotal(bonus)
        pickedThisTick += count                  // surface to consumePickedThisTick for SFX/haptic
    }
}
