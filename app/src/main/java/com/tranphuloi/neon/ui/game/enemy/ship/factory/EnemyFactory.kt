package com.tranphuloi.neon.ui.game.enemy.ship.factory

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBoss
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBoss
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.Row
import com.tranphuloi.neon.ui.game.enemy.ship.model.ZigZag
import com.tranphuloi.neon.ui.game.ship.ship.Ship

class EnemyFactory(
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val formationXOffset: FormationXOffset = FormationXOffset(screenWidth),
) {

    operator fun invoke(type: EnemyType, getShip: () -> Ship): List<Enemy> {
        val enemies: MutableList<Enemy> = mutableListOf()
        if (type is RegularEnemyType) {
            when (type.formation) {
                is ZigZag -> {
                    val enemy = RegularEnemy(
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        xOffset = formationXOffset.zigZagXOffset(type.formation),
                        type = type
                    )
                    enemies += enemy
                }

                is Row -> {
                    for (i in 1..type.formation.rowCount) {
                        val enemy = RegularEnemy(
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            xOffset = formationXOffset.rowXOffset(
                                formation = type.formation,
                                previousEnemy = enemies.lastOrNull(),
                                enemyWidth = type.width
                            ),
                            type = type
                        )
                        enemies += enemy
                    }
                }
            }
        } else if (type is LevelOneBossType && enemies.isEmpty()) {
            val boss = LevelOneBoss(
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                getShip = getShip
            )
            enemies += boss
        } else if (type is LevelTwoBossType && enemies.isEmpty()) {
            val boss = LevelTwoBoss(screenWidth = screenWidth, screenHeight = screenHeight)
            enemies += boss
        }
        return enemies
    }
}
