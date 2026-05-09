package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.Keep
import java.io.Serializable

sealed interface EnemyFormation : Serializable

@Keep
data class ZigZag(val position: ZigZagInitialPosition) : EnemyFormation

@Keep
data class Row(val rowCount: Int) : EnemyFormation

enum class ZigZagInitialPosition {
    LEFT,
    RIGHT
}
