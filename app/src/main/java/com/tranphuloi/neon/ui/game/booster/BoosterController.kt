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
    /** Wave 21 (#3) — TAY KHÔNG modifier: bỏ HẾT buff rơi (thuần kỹ năng). */
    private val noBoosters: () -> Boolean = { false },
    /**
     * Round 75 (R75c) — REVIVE_DROP meta upgrade rank lookup. +2% mỗi rank to
     * override booster type về REVIVE_TOKEN. Max rank 2 = +4% chance bonus
     * trên top của weighted random pick (REVIVE base ~2%).
     */
    private val reviveDropRank: () -> Int = { 0 },
    /**
     * Task 01 (Slice 5) — DRONE_BOOSTER chỉ rơi khi đã mở khoá DRONE_FLEET.
     * Chưa mở khoá → skip roll (thử lại tick sau), tránh pickup vô tác dụng.
     */
    private val droneUnlocked: () -> Boolean = { true },
) {

    init {
        Logger.d("BoosterController init: initialBoosters=${initialBoosters.size}, screen=${screenWidth}x${screenHeight}")
    }

    var boosters: List<Booster> = initialBoosters
        private set

    val addBoosterId = uuidUtils.getUuid()
    val addBoosterRepeatTime = Millis(4000)
    fun addBooster() {
        // Wave 21 (#3) — TAY KHÔNG: không spawn buff nào cả.
        if (noBoosters()) {
            Logger.v { "BoosterController.addBooster: SKIPPED ALL (TAY KHÔNG modifier active)" }
            return
        }
        if (boosters.size >= MAX_BOOSTERS) {
            // Round 47 audit — was Logger.d but addBooster fires every 4s,
            // and when SHIELD modifier is active this skip-path can pump
            // logcat. Verbose-only for consistency with other skip logs.
            Logger.v { "BoosterController.addBooster: SKIPPED (cap=$MAX_BOOSTERS reached, current=${boosters.size})" }
            return
        }
        // Round 75 (R75c) — REVIVE_DROP roll: extra dice trước khi pick type.
        // Nếu hit, force REVIVE_TOKEN, skip generateBooster.
        val reviveBonus = reviveDropRank() * 0.02f
        val booster = if (reviveBonus > 0f && kotlin.random.Random.nextFloat() < reviveBonus) {
            Logger.d("BoosterController.addBooster: REVIVE_DROP triggered (rank=${reviveDropRank()}, chance=${reviveBonus * 100}%)")
            Booster(
                id = uuidUtils.getUuid(),
                xOffset = kotlin.random.Random.nextInt(BOOSTER_SIZE.toInt(), (screenWidth - BOOSTER_SIZE).toInt()).toFloat(),
                size = BOOSTER_SIZE,
                screenHeight = screenHeight,
                forceType = BoosterType.REVIVE_TOKEN,
            )
        } else generateBooster(width = BOOSTER_SIZE, maxXOffset = screenWidth - BOOSTER_SIZE)
        // 25x NO_SHIELDS modifier — drop SHIELD rolls.
        if (noShieldDrops() && booster.type == BoosterType.SHIELD_BOOSTER) {
            Logger.v { "BoosterController.addBooster: SKIPPED SHIELD (NO_SHIELDS modifier active)" }
            return
        }
        // Task 01 (Slice 5) — chưa mở khoá DRONE_FLEET → không rơi DRONE_BOOSTER.
        if (!droneUnlocked() && booster.type == BoosterType.DRONE_BOOSTER) {
            Logger.v { "BoosterController.addBooster: SKIPPED DRONE (chưa mở khoá DRONE_FLEET)" }
            return
        }
        boosters += booster
        // Round 56 — was Logger.v; promoted to Logger.d so booster distribution is
        // observable in default-build logcat. Spawn ticks every 4s (≤0.25 lines/s
        // sustained), well under the spam threshold. Use this log to verify weight
        // distribution at runtime (e.g., PIERCING/PLASMA showing up at 9.7% each
        // per round 55 bump from 8 → 12).
        Logger.d("BoosterController.addBooster: type=${booster.type} rarity=${booster.rarity} at x=${booster.xOffset.toInt()} (active=${boosters.size}/$MAX_BOOSTERS)")
        updateBoosters()
    }

    val processBoostersId = uuidUtils.getUuid()
    val processBoostersRepeatTime = Millis(5)
    fun processBoosters() {
        // Round 37 — was logging "$before → $after" every 5ms tick whenever boosters
        // were removed. Redundant with GameState's per-event booster pickup log.
        // Task 36 — was `boosters -= it` inside this forEach (1 full-list copy
        // per collected booster, same tick). Flag then filter once instead.
        var anyCollected = false
        boosters.forEach {
            if (it.collected) anyCollected = true
            it.moveObject()
        }
        if (anyCollected) {
            boosters = boosters.filterNot { it.collected }
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
