package com.tranphuloi.neon.ui.game.spark

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.utils.Logger
import java.io.Serializable
import java.util.UUID

/**
 * Pickup feedback — combo of (a) ring shockwave expanding from pickup point and
 * (b) 8 radial sparkles. Both share the same lifetime so a single
 * [PickupBurstOverlay] iteration covers them.
 */
@Keep
@Immutable
data class PickupBurst(
    val id: String,
    val xOffset: Float,                   // dp anchor at pickup point
    val yOffset: Float,
    val createdAtMillis: Long,
) : Serializable {

    fun progress(now: Long = System.currentTimeMillis()): Float =
        ((now - createdAtMillis).toFloat() / DURATION_MILLIS).coerceIn(0f, 1f)

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        now - createdAtMillis > DURATION_MILLIS

    companion object {
        const val DURATION_MILLIS: Long = 400L
        const val RING_MAX_RADIUS: Float = 60f       // dp
        const val SPARKLE_COUNT: Int = 8
        const val SPARKLE_TRAVEL: Float = 28f         // dp
    }
}

class PickupBurstController(
    private val updateState: (List<PickupBurst>) -> Unit,
) {

    init {
        Logger.d("PickupBurstController init")
    }

    @Volatile
    private var bursts: List<PickupBurst> = emptyList()

    fun spawn(xOffset: Float, yOffset: Float) {
        val now = System.currentTimeMillis()
        bursts = bursts + PickupBurst(
            id = UUID.randomUUID().toString(),
            xOffset = xOffset,
            yOffset = yOffset,
            createdAtMillis = now,
        )
        updateState(bursts)
    }

    val tickId: String = UUID.randomUUID().toString()
    val tickRepeatTime = Millis(50)

    fun tick() {
        if (bursts.isEmpty()) return
        val now = System.currentTimeMillis()
        val survivors = bursts.filterNot { it.isExpired(now) }
        if (survivors.size != bursts.size) {
            bursts = survivors
            updateState(bursts)
        }
    }
}
