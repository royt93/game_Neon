package com.tranphuloi.neon.ui.game.ship.weapon

import androidx.annotation.Keep
import androidx.compose.runtime.Stable
import java.io.Serializable

/**
 * Wave 6 (29x) round 41 — stationary proximity mine fired from the secondary
 * slot. Spawns just behind the ship; explodes when any enemy enters [AOE_RADIUS]
 * or after [LIFETIME_MS] (auto-detonate so abandoned mines don't accumulate).
 *
 * Game state owns the list; rendering uses the [createdAtMillis] timestamp to
 * pulse a warning glow.
 */
@Keep
@Stable
data class Mine(
    val id: String,
    val xOffset: Float,
    val yOffset: Float,
    val createdAtMillis: Long,
    var detonated: Boolean = false,
) : Serializable {
    companion object {
        /** Mine sprite size in dp. */
        const val SIZE: Float = 24f

        /** Damage dealt to each enemy in radius on detonation. */
        const val EXPLOSION_DAMAGE: Float = 80f

        /** Detonation radius (dp from mine center). */
        const val AOE_RADIUS: Float = 110f

        /** Auto-detonate after this long, so abandoned mines clear themselves. */
        const val LIFETIME_MS: Long = 10_000L

        /** Proximity trigger radius (smaller than AoE so contact triggers reliably). */
        const val TRIGGER_RADIUS: Float = 60f
    }
}
