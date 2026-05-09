package com.tranphuloi.neon.ui.game.ship.laser

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import java.io.Serializable

@Keep
@Immutable
data class LaserUI(
    val id: String,
    val xOffset: Float,
    val yOffset: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    @DrawableRes val drawableId: Int,
) : Serializable
