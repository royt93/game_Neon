package com.tranphuloi.neon

import androidx.compose.ui.geometry.Rect
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.Laser

/**
 * Minimal Enemy stub used by JVM unit tests. Only [enemyId] and a couple of
 * scalar fields are exercised by the controllers under test — everything else
 * has a sensible default.
 */
class TestEnemy(
    override val enemyId: String,
    override var hp: Float = 10f,
) : Enemy {
    override val width: Float = 50f
    override val height: Float = 50f
    override var xOffset: Float = 0f
    override var yOffset: Float = 0f
    override val initialHp: Float = 10f
    override val impactPower: Float = 1f
    override val drawableId: Int = 0
    override val minerals: Int = 0
    override val destroyed: Boolean = false
    override val outOfScreen: Boolean = false
    override var lastImpactMillis: Long = 0L
    override val isBoss: Boolean = false
    override val displayName: String = "Test"

    override fun enemyRect(): Rect = Rect(0f, 0f, width, height)
    override fun process() {}
    override fun generateLasers(): List<Laser> = emptyList()
    override fun onObjectImpact(impactPower: Float) {
        hp -= impactPower
    }
}
