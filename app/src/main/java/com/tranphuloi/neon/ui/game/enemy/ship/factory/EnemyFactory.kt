package com.tranphuloi.neon.ui.game.enemy.ship.factory

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBoss
import com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBoss
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBoss
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBoss
import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.Row
import com.tranphuloi.neon.ui.game.enemy.ship.model.SineWave
import com.tranphuloi.neon.ui.game.enemy.ship.model.VFormation
import com.tranphuloi.neon.ui.game.enemy.ship.model.ZigZag
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.utils.Logger

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
                    Logger.d("EnemyFactory: ZigZag spawn drawable=${type.drawableId} hp=${type.hp} formation=${type.formation}")
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
                    Logger.d("EnemyFactory: Row spawn count=${type.formation.rowCount} drawable=${type.drawableId} hp=${type.hp}")
                }

                is VFormation -> {
                    // V shape: center enemy first (highest), then 1 left + 1 right each
                    // layer staggered DOWN (so V points upward — leading ship at top center).
                    val n = type.formation.count.coerceAtLeast(3)
                    val half = (n - 1) / 2
                    val xStep = type.width * 1.4f
                    val yStep = type.height * 0.9f
                    val centerX = screenWidth / 2f - type.width / 2f
                    for (i in 0 until n) {
                        // Slot ordering: -half .. +half (so even N has one extra on the right)
                        val slot = i - half
                        val yLayer = kotlin.math.abs(slot)
                        val sign = if (slot >= 0) 1 else -1
                        val xs = centerX + sign * yLayer * xStep
                        val ys = -yLayer * yStep                    // negative = above screen top, descend in
                        enemies += RegularEnemy(
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            xOffset = xs.coerceIn(0f, screenWidth - type.width),
                            type = type,
                            initialYOffset = ys,
                        )
                    }
                    Logger.d("EnemyFactory: VFormation spawn count=$n drawable=${type.drawableId}")
                }

                is SineWave -> {
                    val n = type.formation.count.coerceAtLeast(3)
                    val yStep = type.height * 1.4f
                    val centerX = screenWidth / 2f - type.width / 2f
                    for (i in 0 until n) {
                        // All share the same anchor x; stagger vertically so the wave
                        // pattern is visible as a serpent of [n] segments.
                        enemies += RegularEnemy(
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            xOffset = centerX,
                            type = type,
                            initialYOffset = -i * yStep,
                        )
                    }
                    Logger.d("EnemyFactory: SineWave spawn count=$n drawable=${type.drawableId}")
                }
            }
        } else if (type is LevelOneBossType && enemies.isEmpty()) {
            val boss = LevelOneBoss(
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                getShip = getShip
            )
            enemies += boss
            Logger.w("EnemyFactory: LevelOneBoss SPAWNED hp=${boss.hp.toInt()} impactPower=${boss.impactPower}")
        } else if (type is LevelTwoBossType && enemies.isEmpty()) {
            val boss = LevelTwoBoss(screenWidth = screenWidth, screenHeight = screenHeight)
            enemies += boss
            Logger.w("EnemyFactory: LevelTwoBoss SPAWNED hp=${boss.hp.toInt()} impactPower=${boss.impactPower}")
        } else if (type is MidBossType && enemies.isEmpty()) {
            val mid = MidBoss(
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                variant = type,
                getShip = getShip,
            )
            enemies += mid
            Logger.w("EnemyFactory: MidBoss SPAWNED variant=${type::class.simpleName} hp=${mid.hp.toInt()}")
        } else if (type is FinalBossType && enemies.isEmpty()) {
            val finalBoss = FinalBoss(
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                getShip = getShip,
            )
            enemies += finalBoss
            Logger.w("EnemyFactory: FinalBoss SPAWNED hp=${finalBoss.hp.toInt()} (3-phase, 22500 total)")
        }
        return enemies
    }
}
