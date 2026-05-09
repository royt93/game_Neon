package com.tranphuloi.neon.ui.game.ship.ship

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.tranphuloi.neon.R
import java.io.Serializable

@Keep
@Immutable
data class Ship(
    val width: Float = 85f,
    val height: Float = 90f,
    val shieldSize: Float = height * 2,
    val shieldEnabled: Boolean = false,
    val laserBoosterEnabled: Boolean = false,
    val tripleLaserBoosterEnabled: Boolean = false,
    val shieldEndMillis: Long = 0L,
    val laserBoosterEndMillis: Long = 0L,
    val tripleLaserBoosterEndMillis: Long = 0L,
    val xOffset: Float,
    val yOffset: Float,
    val hp: Int = 1000,
    // Spawn cinematic transforms — driven by ShipController during fly-in.
    // Default values (1, 0, 1) are the "live" steady state once spawn finishes.
    val spawnAlpha: Float = 0f,
    val spawnRotation: Float = 0f,
    val spawnScale: Float = 0.6f,
    // Banking rotation — tilts ±16° when player is moving left/right.
    // Lerped each tick by ShipController for smooth feel.
    val bankRotation: Float = 0f,
    // Set when ship is destroyed (hp→0). Drives implosion animation.
    val destroyedAtMillis: Long = 0L,
    @DrawableRes val drawableId: Int = R.drawable.ship_regular_laser,
) : Serializable {
    val shieldRadius: Float get() = shieldSize / 2
}
