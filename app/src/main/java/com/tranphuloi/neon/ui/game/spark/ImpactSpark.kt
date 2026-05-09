package com.tranphuloi.neon.ui.game.spark

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.utils.Logger
import java.io.Serializable
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Tiny laser-impact spark — 1 of ~6 spawned at a hit point. Travels along
 * a random radial vector then fades. Pure decoration, no gameplay effect.
 */
@Keep
@Immutable
data class ImpactSpark(
    val id: String,
    val originX: Float,                    // dp — anchor at hit point
    val originY: Float,
    val vx: Float,                         // dp/sec velocity components
    val vy: Float,
    val length: Float,                     // dp travel distance
    val createdAtMillis: Long,
) : Serializable {

    fun progress(now: Long = System.currentTimeMillis()): Float =
        ((now - createdAtMillis).toFloat() / DURATION_MILLIS).coerceIn(0f, 1f)

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        now - createdAtMillis > DURATION_MILLIS

    /** Current x,y position; ease-out so spark slows near end. */
    fun position(now: Long = System.currentTimeMillis()): Pair<Float, Float> {
        val t = progress(now)
        val tEase = 1f - (1f - t) * (1f - t)              // quadratic ease-out
        return originX + vx * length * tEase to originY + vy * length * tEase
    }

    companion object {
        const val DURATION_MILLIS: Long = 220L
    }
}

class ImpactSparkController(
    private val updateState: (List<ImpactSpark>) -> Unit,
) {

    init {
        Logger.d("ImpactSparkController init")
    }

    @Volatile
    private var sparks: List<ImpactSpark> = emptyList()

    /** Spawn a 16-spark radial burst at [xOffset], [yOffset]. */
    fun spawnBurst(xOffset: Float, yOffset: Float) {
        val now = System.currentTimeMillis()
        val count = 16
        val baseAngle = Random.nextFloat() * 2f * PI.toFloat()
        val newSparks = (0 until count).map { i ->
            val angle = baseAngle + (i.toFloat() / count) * 2f * PI.toFloat() +
                (Random.nextFloat() - 0.5f) * 0.4f
            val length = 25f + Random.nextFloat() * 25f       // 25..50 dp (was 12..26)
            ImpactSpark(
                id = UUID.randomUUID().toString(),
                originX = xOffset,
                originY = yOffset,
                vx = cos(angle),
                vy = sin(angle),
                length = length,
                createdAtMillis = now,
            )
        }
        sparks = sparks + newSparks
        updateState(sparks)
    }

    val tickId: String = UUID.randomUUID().toString()
    val tickRepeatTime = Millis(50)

    fun tick() {
        if (sparks.isEmpty()) return
        val now = System.currentTimeMillis()
        val survivors = sparks.filterNot { it.isExpired(now) }
        if (survivors.size != sparks.size) {
            sparks = survivors
            updateState(sparks)
        }
    }
}
