package com.tranphuloi.neon.ui.game.damage

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.utils.Logger
import java.io.Serializable
import java.util.UUID

/**
 * 2c+10b: Floating damage number entity.
 *  - Crit (damage ≥ 100) renders larger + red.
 *  - Boss damage gets aggregated 200ms by [DamageNumberController] to prevent UI spam.
 */
@Keep
@Immutable
data class DamageNumber(
    val id: String,
    val damage: Int,
    val isCrit: Boolean,
    val xOffset: Float,                          // dp
    val yOffset: Float,                          // dp, decreases over lifetime
    val initialY: Float,
    val createdAtMillis: Long,
) : Serializable {

    /** Progress 0..1 over the entity's 600ms lifetime. */
    fun progress(now: Long = System.currentTimeMillis()): Float =
        ((now - createdAtMillis).toFloat() / DURATION_MILLIS).coerceIn(0f, 1f)

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        now - createdAtMillis > DURATION_MILLIS

    companion object {
        const val DURATION_MILLIS: Long = 600L
        const val FLOAT_DISTANCE: Float = 40f                // dp
        const val CRIT_THRESHOLD: Int = 100
    }
}

/**
 * Aggregates damage spawn events. Critical for bosses where many laser hits/sec
 * would spam dozens of -25 popups. Aggregation window 200ms.
 */
class DamageNumberController(
    private val updateState: (List<DamageNumber>) -> Unit,
) {

    init {
        Logger.d("DamageNumberController init")
    }

    // Immutable list — mutated atomically per tick to avoid concurrent iteration.
    @Volatile
    private var numbers: List<DamageNumber> = emptyList()

    // Pending aggregation per target id.
    private data class Pending(
        val targetId: String,
        val xOffset: Float,
        val yOffset: Float,
        var totalDamage: Int,
        val firstSeenMillis: Long,
        val isBoss: Boolean,
    )

    private val pending = mutableMapOf<String, Pending>()

    fun report(targetId: String, damage: Int, xOffset: Float, yOffset: Float, isBoss: Boolean) {
        val now = System.currentTimeMillis()
        if (isBoss) {
            // Aggregate by target.
            val existing = pending[targetId]
            if (existing != null && now - existing.firstSeenMillis < AGGREGATE_WINDOW_MILLIS) {
                existing.totalDamage += damage
            } else {
                pending[targetId] = Pending(targetId, xOffset, yOffset, damage, now, true)
            }
        } else {
            // Spawn immediately.
            spawn(damage, xOffset, yOffset, now)
        }
    }

    val tickId: String = UUID.randomUUID().toString()
    val tickRepeatTime = Millis(50)

    fun tick() {
        val now = System.currentTimeMillis()

        // Flush pending aggregations whose window has closed.
        val toFlush = pending.values.filter { now - it.firstSeenMillis >= AGGREGATE_WINDOW_MILLIS }
        toFlush.forEach { p ->
            spawn(p.totalDamage, p.xOffset, p.yOffset, now)
            pending.remove(p.targetId)
        }

        // Drop expired + republish.
        val before = numbers.size
        val survivors = numbers.filterNot { it.isExpired(now) }
        if (survivors.size != before) {
            numbers = survivors
            updateState(numbers)
        }
    }

    private fun spawn(damage: Int, xOffset: Float, yOffset: Float, now: Long) {
        val isCrit = damage >= DamageNumber.CRIT_THRESHOLD
        val newNumber = DamageNumber(
            id = UUID.randomUUID().toString(),
            damage = damage,
            isCrit = isCrit,
            xOffset = xOffset,
            yOffset = yOffset,
            initialY = yOffset,
            createdAtMillis = now,
        )
        numbers = numbers + newNumber                      // immutable copy
        Logger.d("DamageNumberController.spawn: -$damage at (${xOffset.toInt()},${yOffset.toInt()}) crit=$isCrit (active=${numbers.size})")
        updateState(numbers)
    }

    companion object {
        const val AGGREGATE_WINDOW_MILLIS: Long = 200L
    }
}
