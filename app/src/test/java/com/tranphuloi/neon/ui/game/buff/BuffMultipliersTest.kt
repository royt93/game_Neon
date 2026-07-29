package com.tranphuloi.neon.ui.game.buff

import org.junit.Assert.assertEquals
import org.junit.Test

class BuffMultipliersTest {

    private val EPS = 0.0001f

    @Test
    fun `empty list returns identity multipliers`() {
        val m = BuffMultipliers.from(emptyList())
        assertEquals(1f, m.hpMul, EPS)
        assertEquals(1f, m.damageMul, EPS)
        assertEquals(1f, m.speedMul, EPS)
        assertEquals(1f, m.magnetMul, EPS)
        assertEquals(1f, m.scoreMul, EPS)
    }

    @Test
    fun `single HP_BOOST yields the buff's hp multiplier`() {
        val m = BuffMultipliers.from(listOf(RunBuff.HP_BOOST))
        assertEquals(RunBuff.HP_BOOST.hpMul, m.hpMul, EPS)
        assertEquals(1f, m.damageMul, EPS)
    }

    @Test
    fun `two HP_BOOST stacks multiplicatively`() {
        val m = BuffMultipliers.from(listOf(RunBuff.HP_BOOST, RunBuff.HP_BOOST))
        val expected = RunBuff.HP_BOOST.hpMul * RunBuff.HP_BOOST.hpMul
        assertEquals(expected, m.hpMul, EPS)
    }

    @Test
    fun `BERSERKER trades hp for damage`() {
        val m = BuffMultipliers.from(listOf(RunBuff.BERSERKER))
        assertEquals(0.90f, m.hpMul, EPS)
        assertEquals(1.50f, m.damageMul, EPS)
    }

    @Test
    fun `BALANCED applies plus-10pct to every axis`() {
        val m = BuffMultipliers.from(listOf(RunBuff.BALANCED))
        assertEquals(1.10f, m.hpMul, EPS)
        assertEquals(1.10f, m.damageMul, EPS)
        assertEquals(1.10f, m.speedMul, EPS)
        assertEquals(1.10f, m.magnetMul, EPS)
        assertEquals(1.10f, m.scoreMul, EPS)
    }

    @Test
    fun `mixed buffs aggregate per axis`() {
        val m = BuffMultipliers.from(listOf(RunBuff.HP_BOOST, RunBuff.DAMAGE_BOOST, RunBuff.GAMBLER))
        // hp: 1.25 * 1.0 * 0.70
        assertEquals(1.25f * 0.70f, m.hpMul, EPS)
        // dmg: 1.0 * 1.30 * 1.0
        assertEquals(1.30f, m.damageMul, EPS)
        // score: 1.0 * 1.0 * 2.0
        assertEquals(2.0f, m.scoreMul, EPS)
    }

    @Test
    fun `GLASS_CANNON trades hp for damage harder than BERSERKER`() {
        val m = BuffMultipliers.from(listOf(RunBuff.GLASS_CANNON))
        assertEquals(0.50f, m.hpMul, EPS)
        assertEquals(1.80f, m.damageMul, EPS)
    }

    @Test
    fun `SLOTH trades speed for score`() {
        val m = BuffMultipliers.from(listOf(RunBuff.SLOTH))
        assertEquals(1.60f, m.scoreMul, EPS)
        assertEquals(0.70f, m.speedMul, EPS)
    }

    @Test
    fun `RECKLESS trades hp for magnet radius`() {
        val m = BuffMultipliers.from(listOf(RunBuff.RECKLESS))
        assertEquals(0.75f, m.hpMul, EPS)
        assertEquals(1.50f, m.magnetMul, EPS)
    }

    @Test
    fun `curse buffs carry a string-resource id for i18n`() {
        assertEquals(true, RunBuff.GLASS_CANNON.displayNameRes != null)
        assertEquals(true, RunBuff.SLOTH.descriptionRes != null)
        assertEquals(true, RunBuff.RECKLESS.displayNameRes != null)
    }

    @Test
    fun `pickThree returns three distinct buffs`() {
        val picks = RunBuff.pickThree()
        assertEquals(3, picks.size)
        assertEquals(3, picks.toSet().size)
    }

    @Test
    fun `fromKey roundtrips for every buff`() {
        RunBuff.entries.forEach {
            assertEquals(it, RunBuff.fromKey(it.key))
        }
    }

    @Test
    fun `fromKey returns null for unknown key`() {
        assertEquals(null, RunBuff.fromKey("not_a_real_key"))
    }
}
