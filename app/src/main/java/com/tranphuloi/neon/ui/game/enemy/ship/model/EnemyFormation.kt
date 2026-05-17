package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.Keep
import java.io.Serializable

sealed interface EnemyFormation : Serializable

@Keep
data class ZigZag(val position: ZigZagInitialPosition) : EnemyFormation

@Keep
data class Row(val rowCount: Int) : EnemyFormation

/**
 * Wave 5 (28x) — V formation. Spawns [count] enemies in a V shape (point at
 * top-center). Each enemy moves straight down at the regular yOffsetSpeed;
 * staggered spawn positions form the V silhouette.
 */
@Keep
data class VFormation(val count: Int) : EnemyFormation

/**
 * Wave 5 (28x) — sine wave. Spawns [count] enemies in a vertical column;
 * each enemy's x oscillates sin(yOffset / period) * amplitude as it descends.
 * Wave pattern feels like a serpent slithering toward the player.
 */
@Keep
data class SineWave(
    val count: Int,
    val amplitude: Float = 80f,
    val period: Float = 110f,
) : EnemyFormation

enum class ZigZagInitialPosition {
    LEFT,
    RIGHT
}
