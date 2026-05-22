package com.tranphuloi.neon.ui.game.booster

import androidx.annotation.Keep
import com.tranphuloi.neon.utils.Logger
import java.io.Serializable
import kotlin.random.Random

@Keep
data class Booster(
    val id: String,
    var xOffset: Float,
    var size: Float,
    private val screenHeight: Float,
    var collected: Boolean = false,
) : Serializable {

    var yOffset = 1f
    // Weighted random pick — REVIVE_TOKEN has weight 5 (rare ~5%), others 19 each.
    val type: BoosterType = run {
        val all = BoosterType.values()
        val totalWeight = all.sumOf { it.weight }
        var roll = Random.nextInt(totalWeight)
        var pick = all.first()
        for (t in all) {
            if (roll < t.weight) {
                pick = t
                break
            }
            roll -= t.weight
        }
        pick
    }
    // Round 43 (39x) — rarity rolled independently of type. REVIVE_TOKEN is always
    // COMMON because it's a binary "has it / doesn't" effect — multiplier wouldn't
    // do anything meaningful for a single-charge revive.
    val rarity: BoosterRarity = if (type == BoosterType.REVIVE_TOKEN) {
        BoosterRarity.COMMON
    } else run {
        val all = BoosterRarity.entries
        val totalWeight = all.sumOf { it.weight }
        var roll = Random.nextInt(totalWeight)
        var pick = all.first()
        for (r in all) {
            if (roll < r.weight) {
                pick = r
                break
            }
            roll -= r.weight
        }
        pick
    }

    private val createdAtMillis: Long = System.currentTimeMillis()

    fun moveObject() {
        if (System.currentTimeMillis() - createdAtMillis > BOOSTER_TIMEOUT_MILLIS) {
            collected = true
            return
        }
        if (yOffset < screenHeight + 100) {
            yOffset += 1
        } else {
            collected = true
        }
    }

    companion object {
        const val BOOSTER_TIMEOUT_MILLIS: Long = 15_000L
    }

    fun collect() {
        Logger.d("Booster.collect: id=${id.take(6)} type=$type")
        collected = true
    }
}
