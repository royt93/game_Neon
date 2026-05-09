package com.tranphuloi.neon.ui.game.explosion.model

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class Explosion(
    val xOffset: Float,
    val yOffset: Float,
    val size: Float,
) : Serializable {

    val startTimeMillis: Long = System.currentTimeMillis()
    private val endTimeMillis = startTimeMillis + 450
    var removed: Boolean = false
        private set

    /** Progress 0..1 used by the burst-lines + ring-shockwave overlay (13c). */
    fun progress(now: Long = System.currentTimeMillis()): Float =
        ((now - startTimeMillis).toFloat() / 450f).coerceIn(0f, 1f)

    fun process() {
        if (System.currentTimeMillis() > endTimeMillis) removed = true
    }
}
