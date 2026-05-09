package com.tranphuloi.neon.ui.game.enemy.ship.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import java.io.Serializable

@Keep
@Immutable
data class EnemyUI(
    val enemyId: String,
    val width: Float,
    val height: Float,
    val xOffset: Float,
    val yOffset: Float,
    val hpBarWidth: Float,
    @DrawableRes val drawableId: Int,
) : Serializable
