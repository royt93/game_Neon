package com.tranphuloi.neon.ui.game.mineral.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class MineralUI(
    val xOffset: Float,
    val yOffset: Float,
    val width: Float,
    val alpha: Float,
)
