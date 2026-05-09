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
        explosions += explosion
        Logger.d("ExplosionController.addExplosion: at (${explosion.xOffset.toInt()},${explosion.yOffset.toInt()}) size=${explosion.size.toInt()} (active=${explosions.size})")
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
        val removed = before - explosions.size
        if (removed > 0) {
            Logger.d("ExplosionController.processExplosions: removed $removed (active=${explosions.size})")
        }
        updateExplosions(explosions)
    }
}
