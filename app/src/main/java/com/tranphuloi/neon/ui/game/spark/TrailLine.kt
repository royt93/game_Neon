package com.tranphuloi.neon.ui.game.spark

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import java.io.Serializable
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Animated line segment connecting two world points. Used for VFX where a
 * "thing" visibly travels between locations — e.g. REFLECT bounce arc from
 * ship to retaliated enemy, or CHAIN_LIGHTNING bolt between chain targets.
 *
 * Spec for Wave 11a audit follow-up: REFLECT + CHAIN_LIGHTNING earlier shipped
 * with 3-point impactSpark clusters that read as "3 explosions" not "line
 * traveled". This entity gives a true connected line render.
 *
 * `jagged = true` injects deterministic perpendicular offsets along the line
 * → reads as electric arc (CHAIN_LIGHTNING). `jagged = false` = smooth
 * curve (REFLECT bounce).
 */
@Keep
@Immutable
data class TrailLine(
    val id: String,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val colorArgb: Long,
    val createdAtMillis: Long,
    val durationMillis: Long = 280L,
    val jagged: Boolean = false,
    val widthDp: Float = 2.5f,
) : Serializable {

    fun progress(now: Long = System.currentTimeMillis()): Float =
        ((now - createdAtMillis).toFloat() / durationMillis).coerceIn(0f, 1f)

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        now - createdAtMillis > durationMillis

    /**
     * For jagged variant — returns N intermediate points with deterministic
     * perpendicular offset (seed by id hash so shape stays stable across
     * recomposition).
     */
    fun jaggedPoints(segments: Int = 6): List<Pair<Float, Float>> {
        if (!jagged) return listOf(x1 to y1, x2 to y2)
        val dx = x2 - x1
        val dy = y2 - y1
        val len = hypot(dx, dy)
        if (len < 1f) return listOf(x1 to y1, x2 to y2)
        // Perpendicular unit vector
        val px = -dy / len
        val py = dx / len
        // Max jitter scales with line length (longer line = bigger zigzag)
        val maxJitter = (len * 0.10f).coerceAtMost(40f)
        val rng = Random(id.hashCode())
        val pts = mutableListOf<Pair<Float, Float>>()
        pts.add(x1 to y1)
        for (i in 1 until segments) {
            val t = i.toFloat() / segments
            val baseX = x1 + dx * t
            val baseY = y1 + dy * t
            // Sin envelope so jitter is 0 at endpoints, max at midpoint
            val envelope = kotlin.math.sin(t * kotlin.math.PI).toFloat().absoluteValue
            val jitter = (rng.nextFloat() - 0.5f) * 2f * maxJitter * envelope
            pts.add(baseX + px * jitter to baseY + py * jitter)
        }
        pts.add(x2 to y2)
        return pts
    }
}

/**
 * Owns the active [TrailLine] list, expires lines past duration. GameState
 * binds add* helpers from REFLECT + CHAIN_LIGHTNING dispatch sites.
 */
class TrailLineController(
    private val updateState: (List<TrailLine>) -> Unit,
) {
    val tickId: String = UUID.randomUUID().toString()
    val tickRepeatTime = com.tranphuloi.neon.ui.game.common.Millis(40)

    /**
     * P0 audit fix — cap line list size. CHAIN_LIGHTNING can drop 2 bolts per
     * laser hit + REFLECT adds arcs. Without a cap, IO-paused player who spammed
     * REFLECT could accumulate hundreds of TrailLine instances before tickExpiry
     * drains. 32 is well above peak normal load (FPS 120 × duration 300ms ×
     * worst-case 2 chains × 5 lasers = ~36 transient lines).
     *
     * Thread safety: add* + tickExpiry both run on the IO game loop (tinker is
     * single-threaded via DisposableEffect+launch). No concurrent mutation
     * possible. Read-side (TrailLineOverlay) reads from Compose state which
     * snapshots safely.
     */
    private var lines: List<TrailLine> = emptyList()

    private fun appendCapped(line: TrailLine) {
        val next = if (lines.size >= MAX_LINES) {
            // Drop oldest (first) to free slot
            lines.drop(1) + line
        } else {
            lines + line
        }
        lines = next
        updateState(lines)
    }

    fun addBounceArc(x1: Float, y1: Float, x2: Float, y2: Float) {
        appendCapped(TrailLine(
            id = UUID.randomUUID().toString(),
            x1 = x1, y1 = y1, x2 = x2, y2 = y2,
            colorArgb = 0xFFE0E060L,                  // REFLECT pale yellow bumper
            createdAtMillis = System.currentTimeMillis(),
            durationMillis = 300L,
            jagged = false,
            widthDp = 3f,
        ))
    }

    fun addChainBolt(x1: Float, y1: Float, x2: Float, y2: Float) {
        appendCapped(TrailLine(
            id = UUID.randomUUID().toString(),
            x1 = x1, y1 = y1, x2 = x2, y2 = y2,
            colorArgb = 0xFF80B0FFL,                  // CHAIN_LIGHTNING electric arc blue
            createdAtMillis = System.currentTimeMillis(),
            durationMillis = 220L,
            jagged = true,
            widthDp = 2.5f,
        ))
    }

    fun tickExpiry() {
        val now = System.currentTimeMillis()
        val remaining = lines.filterNot { it.isExpired(now) }
        if (remaining.size != lines.size) {
            lines = remaining
            updateState(lines)
        }
    }

    fun hasLines(): Boolean = lines.isNotEmpty()

    companion object {
        const val MAX_LINES = 32
    }
}
