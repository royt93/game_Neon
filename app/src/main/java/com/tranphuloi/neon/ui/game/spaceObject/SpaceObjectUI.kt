package com.tranphuloi.neon.ui.game.spaceObject

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import java.io.Serializable

@Immutable
@androidx.annotation.Keep
data class SpaceObjectUI(
    val id: String,
    val size: Float,
    val xOffset: Float,
    val yOffset: Float,
    val rotation: Float,
    @DrawableRes val drawableId: Int,
) : Serializable
