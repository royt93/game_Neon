package com.tranphuloi.neon.ui.game.drone

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import java.io.Serializable

/**
 * Task 01 — UI projection của [Drone] cho [com.tranphuloi.neon.ui.game.world.DroneCanvas].
 * Vẽ thuần Canvas (neon), không cần drawable → chỉ cần vị trí, size, và tỉ lệ HP
 * để tô vòng máu / mờ dần khi sắp vỡ.
 */
@Keep
@Immutable
data class DroneUI(
    val xOffset: Float,
    val yOffset: Float,
    val size: Float,
    /** hp/MAX_HP trong [0,1] — drive vòng HP + độ mờ. */
    val hpRatio: Float,
) : Serializable
