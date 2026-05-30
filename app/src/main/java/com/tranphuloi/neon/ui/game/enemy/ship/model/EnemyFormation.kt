package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.Keep
import java.io.Serializable

sealed interface EnemyFormation : Serializable

@Keep
data class ZigZag(
    val position: ZigZagInitialPosition,
    /** Audit-5 P2 fix — per-stage entry-x shift so ZigZag formations don't
     *  always start at the same edge. Range ~[-60, +60] dp; clamped by
     *  FormationXOffset.zigZagXOffset against spawnXMargin. */
    val xAnchorShift: Float = 0f,
) : EnemyFormation

@Keep
data class Row(
    val rowCount: Int,
    /** Pixel-2 #4 — per-stage X-anchor shift (dp). Lets identical Row formations
     *  spawn at different starting X positions across stages, so the spawn
     *  pattern doesn't feel repetitive. Stage.kt picks a deterministic seed
     *  per gameStage. Range: roughly [-60, +60]; clamped by FormationXOffset. */
    val xAnchorShift: Float = 0f,
) : EnemyFormation

/**
 * Wave 5 (28x) — V formation. Spawns [count] enemies in a V shape (point at
 * top-center). Each enemy moves straight down at the regular yOffsetSpeed;
 * staggered spawn positions form the V silhouette.
 *
 * Pixel-2 #4 — [xAnchorShift] varies the V tip position per stage.
 */
@Keep
data class VFormation(
    val count: Int,
    val xAnchorShift: Float = 0f,
) : EnemyFormation

/**
 * Wave 5 (28x) — sine wave. Spawns [count] enemies in a vertical column;
 * each enemy's x oscillates sin(yOffset / period) * amplitude as it descends.
 * Wave pattern feels like a serpent slithering toward the player.
 *
 * Pixel-2 #4 — [xAnchorShift] varies the spawn-x anchor per stage.
 */
@Keep
data class SineWave(
    val count: Int,
    val amplitude: Float = 80f,
    val period: Float = 110f,
    val xAnchorShift: Float = 0f,
) : EnemyFormation

enum class ZigZagInitialPosition {
    LEFT,
    RIGHT
}
