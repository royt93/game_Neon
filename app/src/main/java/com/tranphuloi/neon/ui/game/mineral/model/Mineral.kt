package com.tranphuloi.neon.ui.game.mineral.model

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class Mineral(
    var xOffset: Float,
    var yOffset: Float,
    val width: Float,
) : Serializable {

    var alpha: Float = 1f
    private val alphaAnimationSpeed: Float = 0.009f

    private val animationYOffset: Float = yOffset - 60f
    private val yOffsetMovementSpeed: Float = 1f
    var removed: Boolean = false
        private set

    /**
     * Process one tick. If [magnetTargetX]/[magnetTargetY] supplied AND distance < [magnetRadius],
     * lerp the mineral toward the target (collected when distance < pickupDistance).
     * Otherwise default upward drift + alpha fade.
     */
    fun process(
        magnetTargetX: Float? = null,
        magnetTargetY: Float? = null,
        magnetRadius: Float = 0f,
        pickupDistance: Float = 16f,
    ) {
        if (magnetTargetX != null && magnetTargetY != null && magnetRadius > 0f) {
            val dx = magnetTargetX - xOffset
            val dy = magnetTargetY - yOffset
            val distSq = dx * dx + dy * dy
            val rSq = magnetRadius * magnetRadius
            if (distSq < pickupDistance * pickupDistance) {
                removed = true
                return
            }
            if (distSq < rSq) {
                val pull = MAGNET_PULL_SPEED
                xOffset += dx / kotlin.math.sqrt(distSq) * pull
                yOffset += dy / kotlin.math.sqrt(distSq) * pull
                if (yOffset <= animationYOffset) alpha -= alphaAnimationSpeed
                if (alpha <= 0f) removed = true
                return
            }
        }
        yOffset -= yOffsetMovementSpeed
        if (yOffset <= animationYOffset) {
            alpha -= alphaAnimationSpeed
        }
        if (alpha <= 0f || yOffset + width < 0f) {
            removed = true
        }
    }

    companion object {
        const val MAGNET_PULL_SPEED: Float = 4f
    }
}
