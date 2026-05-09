package com.tranphuloi.neon.ui.game.enemy.ship.factory

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.Row
import com.tranphuloi.neon.ui.game.enemy.ship.model.ZigZag
import com.tranphuloi.neon.ui.game.enemy.ship.model.ZigZagInitialPosition

class FormationXOffset(private val screenWidth: Float) {

    fun zigZagXOffset(formation: ZigZag): Float {
        return if (formation.position == ZigZagInitialPosition.LEFT) 0f else screenWidth
    }

    fun rowXOffset(formation: Row, previousEnemy: Enemy?, enemyWidth: Float): Float {
        val divider = formation.rowCount + 1
        val distanceBetween = screenWidth / divider - enemyWidth / divider
        return previousEnemy?.let { it.xOffset + distanceBetween } ?: distanceBetween
    }
}
