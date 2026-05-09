package com.tranphuloi.neon.ui.game.booster

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import java.io.Serializable

@Keep
@Immutable
data class BoosterUI(
    val xOffset: Float,
    val yOffset: Float,
    val size: Float,
    @DrawableRes val drawableId: Int,
) : Serializable
