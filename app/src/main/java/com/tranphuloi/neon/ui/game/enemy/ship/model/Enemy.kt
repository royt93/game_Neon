package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.compose.ui.geometry.Rect
import com.tranphuloi.neon.ui.game.laser.Laser
import java.io.Serializable

interface Enemy : Serializable {
    val enemyId: String
    val width: Float
    val height: Float
    var xOffset: Float
    var yOffset: Float
    var hp: Float
    val initialHp: Float
    val impactPower: Float
    val drawableId: Int
    val minerals: Int
    val destroyed: Boolean
    val outOfScreen: Boolean
    var lastImpactMillis: Long
    val isBoss: Boolean
    val displayName: String

    /** True while boss is sliding from off-screen to patrol position. */
    val isInEntryPhase: Boolean get() = false

    /** 34d FinalBoss phase id (1..3); 0 for non-multi-phase enemies. */
    val currentPhase: Int get() = 0

    /** 34d Wall-clock when last phase transition fired; 0 if none. Drives cinematic banner. */
    val phaseTransitionMillis: Long get() = 0L

    fun enemyRect(): Rect
    fun process()
    fun generateLasers(): List<Laser>
    fun onObjectImpact(impactPower: Float)
}
