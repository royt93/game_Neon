package com.tranphuloi.neon.ui.game.booster

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.utils.Logger
import com.tranphuloi.neon.utils.UuidUtils

class BoosterController(
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val uuidUtils: UuidUtils,
    private val generateBooster: GenerateBooster = GenerateBooster(uuidUtils, screenHeight),
    initialBoosters: List<Booster>,
    private val updateBoosters: (List<Booster>) -> Unit,
    /**
     * Wave 5 (25x) — when true, SHIELD_BOOSTER spawns are skipped. Wired via
     * RunModifier.NO_SHIELDS. Re-roll bounded — if generator returns SHIELD,
     * skip silently and let the next addBooster tick (4s) try again.
     */
    private val noShieldDrops: () -> Boolean = { false },
) {

    init {
        Logger.d("BoosterController init: initialBoosters=${initialBoosters.size}, screen=${screenWidth}x${screenHeight}")
    }

    var boosters: List<Booster> = initialBoosters
        private set

    val addBoosterId = uuidUtils.getUuid()
    val addBoosterRepeatTime = Millis(4000)
    fun addBooster() {
        if (boosters.size >= MAX_BOOSTERS) {
            Logger.d("BoosterController.addBooster: SKIPPED (cap=$MAX_BOOSTERS reached, current=${boosters.size})")
            return
        }
        val booster = generateBooster(width = BOOSTER_SIZE, maxXOffset = screenWidth - BOOSTER_SIZE)
        // 25x NO_SHIELDS modifier — drop SHIELD rolls.
        if (noShieldDrops() && booster.type == BoosterType.SHIELD_BOOSTER) {
            Logger.d("BoosterController.addBooster: SKIPPED SHIELD (NO_SHIELDS modifier active)")
            return
        }
        boosters += booster
        Logger.v { "BoosterController.addBooster: type=${booster.type} at x=${booster.xOffset.toInt()} (active=${boosters.size}/$MAX_BOOSTERS)" }
        updateBoosters()
    }

    val processBoostersId = uuidUtils.getUuid()
    val processBoostersRepeatTime = Millis(5)
    fun processBoosters() {
        // Round 37 — was logging "$before → $after" every 5ms tick whenever boosters
        // were removed. Redundant with GameState's per-event booster pickup log.
        boosters.forEach {
            if (it.collected) boosters -= it
            it.moveObject()
        }
        updateBoosters()
    }

    fun hasBoosters() = boosters.isNotEmpty()

    private fun updateBoosters() {
        updateBoosters(boosters)
    }

    companion object {
        const val BOOSTER_SIZE = 40f
        const val MAX_BOOSTERS = 3
    }
}
