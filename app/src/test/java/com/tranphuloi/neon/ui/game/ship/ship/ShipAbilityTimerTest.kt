package com.tranphuloi.neon.ui.game.ship.ship

import com.tranphuloi.neon.ui.game.ship.shape.AbilityEffect
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Task 05 — [ShipController.activateAbilityTimer] set đúng timer ability trên Ship
 * theo từng effect duration (BULWARK/PHASE_DASH/DECOY→invuln, OVERDRIVE→overdrive,
 * CRIT_FRENZY→crit, TIME_DILATION→freeze). Instant effect không set timer nào.
 */
class ShipAbilityTimerTest {

    private fun controllerCapturing(): Pair<ShipController, () -> Ship> {
        var captured = Ship(xOffset = 0f, yOffset = 0f)
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = captured, setShip = { captured = it },
        )
        return ctrl to { captured }
    }

    @Test
    fun `bulwark phase-dash decoy set invuln timer`() {
        for (e in listOf(AbilityEffect.BULWARK, AbilityEffect.PHASE_DASH, AbilityEffect.DECOY)) {
            val (ctrl, ship) = controllerCapturing()
            ctrl.activateAbilityTimer(e, nowMillis = 1_000L, durationMs = 4_000L)
            assertEquals("$e → invuln end", 5_000L, ship().abilityInvulnEndMillis)
        }
    }

    @Test
    fun `overdrive sets overdrive timer only`() {
        val (ctrl, ship) = controllerCapturing()
        ctrl.activateAbilityTimer(AbilityEffect.OVERDRIVE, 1_000L, 5_000L)
        assertEquals(6_000L, ship().abilityOverdriveEndMillis)
        assertEquals("không set invuln", 0L, ship().abilityInvulnEndMillis)
    }

    @Test
    fun `crit frenzy sets crit timer`() {
        val (ctrl, ship) = controllerCapturing()
        ctrl.activateAbilityTimer(AbilityEffect.CRIT_FRENZY, 2_000L, 5_000L)
        assertEquals(7_000L, ship().abilityCritEndMillis)
    }

    @Test
    fun `time dilation sets freeze timer`() {
        val (ctrl, ship) = controllerCapturing()
        ctrl.activateAbilityTimer(AbilityEffect.TIME_DILATION, 0L, 4_000L)
        assertEquals(4_000L, ship().abilityFreezeEndMillis)
    }

    @Test
    fun `instant effects and non-positive duration set no timer`() {
        val (ctrl, ship) = controllerCapturing()
        ctrl.activateAbilityTimer(AbilityEffect.NOVA, 1_000L, 0L) // instant + dur 0
        ctrl.activateAbilityTimer(AbilityEffect.BULWARK, 1_000L, 0L) // dur 0 → no-op
        assertEquals(0L, ship().abilityInvulnEndMillis)
        assertEquals(0L, ship().abilityOverdriveEndMillis)
        assertEquals(0L, ship().abilityFreezeEndMillis)
    }
}
