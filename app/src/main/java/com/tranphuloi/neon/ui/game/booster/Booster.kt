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
    private val boosters: Array<BoosterType> = BoosterType.values()
    private val index: Int = Random.nextInt(0, boosters.size)
    val type: BoosterType = boosters[index]

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
