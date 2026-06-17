package com.tranphuloi.neon.ui.game.status

import com.tranphuloi.neon.TestEnemy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusEffectControllerTest {

    private val now = 1_000_000L

    @Test
    fun `apply adds a new effect to a fresh enemy`() {
        val c = StatusEffectController()
        c.apply("e1", StatusEffect.BURN, now)
        assertEquals(setOf(StatusEffect.BURN), c.effectsFor("e1"))
        assertEquals(1, c.totalActive())
    }

    @Test
    fun `apply same type refreshes expiry without stacking`() {
        val c = StatusEffectController()
        c.apply("e1", StatusEffect.BURN, now)
        c.apply("e1", StatusEffect.BURN, now + 100)
        assertEquals(setOf(StatusEffect.BURN), c.effectsFor("e1"))
        assertEquals(1, c.totalActive())
    }

    @Test
    fun `apply different types coexist on same enemy`() {
        val c = StatusEffectController()
        c.apply("e1", StatusEffect.BURN, now)
        c.apply("e1", StatusEffect.SLOW, now)
        c.apply("e1", StatusEffect.STUN, now)
        assertEquals(
            setOf(StatusEffect.BURN, StatusEffect.SLOW, StatusEffect.STUN),
            c.effectsFor("e1"),
        )
        assertTrue(c.isSlowed("e1"))
        assertTrue(c.isStunned("e1"))
    }

    @Test
    fun `isSlowed and isStunned false when not applied`() {
        val c = StatusEffectController()
        assertFalse(c.isSlowed("missing"))
        assertFalse(c.isStunned("missing"))
        c.apply("e1", StatusEffect.BURN, now)
        assertFalse(c.isSlowed("e1"))
        assertFalse(c.isStunned("e1"))
    }

    @Test
    fun `effectsFor empty when enemy unknown`() {
        val c = StatusEffectController()
        assertEquals(emptySet<StatusEffect>(), c.effectsFor("nope"))
    }

    @Test
    fun `processTick drops expired effects`() {
        val c = StatusEffectController()
        val e = TestEnemy("e1")
        c.apply("e1", StatusEffect.STUN, now)               // STUN dur = 1200ms
        c.processTick(listOf(e), now + StatusEffect.STUN.durationMs + 1)
        assertEquals(emptySet<StatusEffect>(), c.effectsFor("e1"))
        assertEquals(0, c.totalActive())
    }

    @Test
    fun `processTick removes entries for dead enemies`() {
        val c = StatusEffectController()
        c.apply("ghost", StatusEffect.BURN, now)
        assertEquals(1, c.totalActive())
        // "ghost" not in alive list → cleaned even though effect not expired.
        c.processTick(enemies = emptyList(), nowMillis = now + 100)
        assertEquals(0, c.totalActive())
    }

    @Test
    fun `processTick returns burn damage at tick interval`() {
        val c = StatusEffectController()
        val e = TestEnemy("e1")
        c.apply("e1", StatusEffect.BURN, now)
        // First tick at apply time — sinceLastTick = 0 → no damage.
        val firstTick = c.processTick(listOf(e), now)
        assertEquals(0f, firstTick["e1"] ?: 0f, 0.001f)
        // Tick after BURN_TICK_INTERVAL_MS → BURN_TICK_DAMAGE.
        val secondTick = c.processTick(listOf(e), now + StatusEffect.BURN_TICK_INTERVAL_MS + 10)
        assertEquals(StatusEffect.BURN_TICK_DAMAGE, secondTick["e1"]!!, 0.001f)
    }

    @Test
    fun `processTick burn does not tick faster than interval`() {
        val c = StatusEffectController()
        val e = TestEnemy("e1")
        c.apply("e1", StatusEffect.BURN, now)
        // Tick well under the interval — no damage emitted.
        val tick = c.processTick(listOf(e), now + (StatusEffect.BURN_TICK_INTERVAL_MS / 2))
        assertEquals(0f, tick["e1"] ?: 0f, 0.001f)
    }

    @Test
    fun `processTick returns corrosion damage at tick interval (stronger than burn)`() {
        val c = StatusEffectController()
        val e = TestEnemy("e1")
        c.apply("e1", StatusEffect.CORROSION, now)
        // First tick at apply time — sinceLastTick = 0 → no damage.
        assertEquals(0f, c.processTick(listOf(e), now)["e1"] ?: 0f, 0.001f)
        // Tick after interval → CORROSION_TICK_DAMAGE (7, > BURN's 5).
        val tick = c.processTick(listOf(e), now + StatusEffect.BURN_TICK_INTERVAL_MS + 10)
        assertEquals(StatusEffect.CORROSION_TICK_DAMAGE, tick["e1"]!!, 0.001f)
        assertTrue(
            "ăn mòn phải mạnh hơn cháy mỗi tick",
            StatusEffect.CORROSION_TICK_DAMAGE > StatusEffect.BURN_TICK_DAMAGE,
        )
    }

    @Test
    fun `corrosion lasts longer than burn`() {
        assertTrue(StatusEffect.CORROSION.durationMs > StatusEffect.BURN.durationMs)
    }

    @Test
    fun `clearFor drops all effects for an enemy`() {
        val c = StatusEffectController()
        c.apply("e1", StatusEffect.BURN, now)
        c.apply("e1", StatusEffect.SLOW, now)
        c.apply("e2", StatusEffect.STUN, now)
        c.clearFor("e1")
        assertEquals(emptySet<StatusEffect>(), c.effectsFor("e1"))
        assertEquals(setOf(StatusEffect.STUN), c.effectsFor("e2"))
        assertEquals(1, c.totalActive())
    }

    @Test
    fun `processTick handles empty controller as no-op`() {
        val c = StatusEffectController()
        val result = c.processTick(listOf(TestEnemy("e1")), now)
        assertTrue(result.isEmpty())
    }
}
