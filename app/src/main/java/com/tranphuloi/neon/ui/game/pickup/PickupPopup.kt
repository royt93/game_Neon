package com.tranphuloi.neon.ui.game.pickup

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.utils.Logger
import java.io.Serializable
import java.util.UUID

/**
 * 4c: Mineral / item pickup floating popup. Reuses same lifecycle pattern
 * as DamageNumber (immutable list snapshot, atomic reassign per tick).
 */
@Keep
@Immutable
data class PickupPopup(
    val id: String,
    val text: String,                            // e.g. "+1", "+1 ×3 = +3"
    val xOffset: Float,
    val yOffset: Float,
    val initialY: Float,
    val createdAtMillis: Long,
    val isComboBonus: Boolean,
    /**
     * Round 58 — ARGB color override for bullet-type activation hints
     * (magenta for PIERCING, cyan for PLASMA). 0 = default NeonGold.
     */
    val colorHex: Long = 0L,
    /**
     * Round 58 — bigger 18sp font for bullet-type activation, vs 12-14sp
     * for the mineral pickup popups. Makes the buff hint impossible to miss.
     */
    val isLargeFont: Boolean = false,
) : Serializable {

    fun progress(now: Long = System.currentTimeMillis()): Float =
        ((now - createdAtMillis).toFloat() / DURATION_MILLIS).coerceIn(0f, 1f)

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        now - createdAtMillis > DURATION_MILLIS

    companion object {
        const val DURATION_MILLIS: Long = 500L
        const val FLOAT_DISTANCE: Float = 30f
    }
}

class PickupPopupController(
    private val updateState: (List<PickupPopup>) -> Unit,
) {

    init {
        Logger.d("PickupPopupController init")
    }

    @Volatile
    private var popups: List<PickupPopup> = emptyList()

    /**
     * Round 58 — bullet-type activation hint. Caller supplies pre-formatted
     * text (e.g. "→ PIERCING ×4" / "◯ PLASMA 110px") + the discriminator color.
     * Renders at 18sp instead of the 12sp mineral popup so it's clearly a
     * different signal class. 500ms lifetime same as mineral popups.
     */
    fun spawnBulletTypeActivation(
        text: String,
        colorHex: Long,
        xOffset: Float,
        yOffset: Float,
    ) {
        val popup = PickupPopup(
            id = UUID.randomUUID().toString(),
            text = text,
            xOffset = xOffset,
            yOffset = yOffset,
            initialY = yOffset,
            createdAtMillis = System.currentTimeMillis(),
            isComboBonus = false,
            colorHex = colorHex,
            isLargeFont = true,
        )
        popups = popups + popup
        Logger.d("PickupPopupController.spawnBulletTypeActivation '$text' at (${xOffset.toInt()},${yOffset.toInt()})")
        updateState(popups)
    }

    fun spawnMineralPickup(xOffset: Float, yOffset: Float, comboCount: Int, multiplier: Int) {
        val now = System.currentTimeMillis()
        // Always show simple "+N" gold (was "+1 ×3 = +3" magenta which confused users
        // into thinking it was an enemy effect). Combo info is already in ComboHud +
        // ComboPopup — pickup popup just confirms the gain count.
        val isCombo = comboCount > 1
        val text = "+$multiplier"
        val popup = PickupPopup(
            id = UUID.randomUUID().toString(),
            text = text,
            xOffset = xOffset,
            yOffset = yOffset,
            initialY = yOffset,
            createdAtMillis = now,
            isComboBonus = isCombo,
        )
        popups = popups + popup
        Logger.v { "PickupPopupController.spawnMineralPickup '$text' at (${xOffset.toInt()},${yOffset.toInt()}) (active=${popups.size})" }
        updateState(popups)
    }

    val tickId: String = UUID.randomUUID().toString()
    val tickRepeatTime = Millis(50)

    fun tick() {
        val now = System.currentTimeMillis()
        val before = popups.size
        val survivors = popups.filterNot { it.isExpired(now) }
        if (survivors.size != before) {
            popups = survivors
            updateState(popups)
        }
    }
}
