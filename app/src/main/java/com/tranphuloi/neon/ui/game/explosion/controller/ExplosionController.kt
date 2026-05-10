package com.tranphuloi.neon.ui.game.explosion.controller

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.explosion.model.Explosion
import com.tranphuloi.neon.utils.Logger
import java.util.*

class ExplosionController(
    initialExplosions: List<Explosion>,
    private val updateExplosions: (List<Explosion>) -> Unit,
) {

    init {
        Logger.d("ExplosionController init: initialExplosions=${initialExplosions.size}")
    }

    private var explosions: List<Explosion> = initialExplosions

    fun addExplosion(xOffset: Float, yOffset: Float, width: Float, height: Float) {
        val size: Float = maxOf(a = width, b = height)
        val explosion = Explosion(
            xOffset = xOffset - size,
            yOffset = yOffset - size,
            size = size * 2
        )
        // Cap to MAX_ACTIVE — drop oldest. 16-18 simultaneous Coil GIF
        // decoders during burst combat were a major heap/CPU pressure source.
        val combined = explosions + explosion
        explosions = if (combined.size > MAX_ACTIVE) combined.takeLast(MAX_ACTIVE) else combined
        updateExplosions(explosions)
    }

    val processExplosionsId = UUID.randomUUID().toString()
    val processExplosionsRepeatTime = Millis(5)
    fun processExplosions() {
        val before = explosions.size
        explosions.forEach {
            it.process()
            if (it.removed) explosions -= it
        }
        if (before != explosions.size) {
            updateExplosions(explosions)
        }
    }

    companion object {
        const val MAX_ACTIVE: Int = 8
    }
}
