package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.tranphuloi.neon.ui.game.common.Once
import com.tranphuloi.neon.ui.game.common.RepeatTime
import java.io.Serializable

@Keep
sealed class EnemyType(val spawnRate: RepeatTime) : Serializable

@Keep
data class RegularEnemyType(
    @DrawableRes val drawableId: Int,
    val width: Float,
    val height: Float,
    val hp: Float,
    val impactPower: Float,
    val formation: EnemyFormation,
    val xOffsetSpeed: Float,
    val yOffsetSpeed: Float,
    val enemySpawnRate: RepeatTime,
) : EnemyType(spawnRate = enemySpawnRate)

@Keep object LevelOneBossType : EnemyType(spawnRate = Once)
@Keep object LevelTwoBossType : EnemyType(spawnRate = Once)
