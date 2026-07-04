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
    /** Task 09 — kiểu đòn RIÊNG (5 địch chủ đề). Mặc định SINGLE = 1 tia như cũ. */
    val attackKind: EnemyAttackKind = EnemyAttackKind.SINGLE,
) : EnemyType(spawnRate = enemySpawnRate)

@Keep object LevelOneBossType : EnemyType(spawnRate = Once)
@Keep object LevelTwoBossType : EnemyType(spawnRate = Once)
