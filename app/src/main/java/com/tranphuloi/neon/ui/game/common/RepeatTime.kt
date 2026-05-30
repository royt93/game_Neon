package com.tranphuloi.neon.ui.game.common

import androidx.annotation.Keep
import java.io.Serializable

@Keep
sealed interface RepeatTime : Serializable

@Keep class Millis(val timeMillis: Int) : RepeatTime
@Keep object Once : RepeatTime
@Keep object Never : RepeatTime
