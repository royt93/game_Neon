package com.tranphuloi.neon.ui.game.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 11d — pin the audit-P3 variant-picker contract.
 *
 * Audit-2 Risk #1 fix — these tests drive the PRODUCTION
 * [VoiceAnnouncer.pickNonRepeatingFor] function directly (now `internal` +
 * accepts an injected `Random` for determinism). The earlier version of this
 * file re-implemented the algorithm in a helper, which made the tests
 * **tautological**: a production-side regression to the biased version would
 * have passed all assertions because the test's copy was independent of the
 * code under test. Now, any drift in the picker math is caught.
 *
 * Contract pinned:
 *   1. size <= 1 returns 0 always.
 *   2. last < 0 or last >= size → plain uniform pick over [0, size).
 *   3. Otherwise pick is uniform over [0, size) \ {last}.
 *   4. Pick never equals `last` when 1 < size.
 */
class VoicePickerWave11dTest {

    private fun pick(last: Int, size: Int, rng: kotlin.random.Random): Int =
        VoiceAnnouncer.pickNonRepeatingFor(last = last, size = size, rng = rng)

    @Test
    fun `size 1 returns 0 always`() {
        repeat(10) {
            val p = pick(last = -1, size = 1, rng = kotlin.random.Random(it.toLong()))
            assertEquals(0, p)
        }
    }

    @Test
    fun `negative last gives uniform pick over full range`() {
        val p = pick(last = -1, size = 3, rng = kotlin.random.Random(42L))
        assertTrue("pick $p must be in [0,3)", p in 0..2)
    }

    @Test
    fun `pick never equals last when size 2`() {
        for (seed in 0..999) {
            val p = pick(last = 0, size = 2, rng = kotlin.random.Random(seed.toLong()))
            assertNotEquals("seed=$seed: pick should never equal last=0", 0, p)
            assertEquals(1, p)
        }
    }

    @Test
    fun `pick never equals last when size 3`() {
        for (seed in 0..999) {
            for (last in 0..2) {
                val p = pick(last = last, size = 3, rng = kotlin.random.Random(seed.toLong()))
                assertNotEquals("seed=$seed last=$last: pick $p should never equal last", last, p)
                assertTrue("seed=$seed: pick $p must be in [0,3)", p in 0..2)
            }
        }
    }

    @Test
    fun `pick distribution is uniform over remaining indices for size 3`() {
        // P3 audit complaint — the prior impl had ~2× bias toward (last+1) % size.
        // New impl: for size=3 with last=1, pick should be {0, 2} with equal
        // probability over many trials. Slack accommodates RNG noise.
        val counts = IntArray(3)
        val trials = 30_000
        val rng = kotlin.random.Random(12345L)
        repeat(trials) {
            val p = pick(last = 1, size = 3, rng = rng)
            counts[p]++
        }
        assertEquals("counts[last] must be 0 — last is excluded", 0, counts[1])
        val expected = trials / 2
        val slack = expected / 6   // ~16% tolerance
        assertTrue(
            "counts[0]=${counts[0]} should be ~$expected (±$slack)",
            kotlin.math.abs(counts[0] - expected) < slack,
        )
        assertTrue(
            "counts[2]=${counts[2]} should be ~$expected (±$slack)",
            kotlin.math.abs(counts[2] - expected) < slack,
        )
    }

    @Test
    fun `pick distribution is uniform over remaining indices for size 4`() {
        // With size=4 and last=2, the other 3 indices {0, 1, 3} should each
        // get ~33.3% of picks. Old impl biased index 3 to ~50%.
        val counts = IntArray(4)
        val trials = 30_000
        val rng = kotlin.random.Random(98765L)
        repeat(trials) {
            val p = pick(last = 2, size = 4, rng = rng)
            counts[p]++
        }
        assertEquals(0, counts[2])
        val expected = trials / 3
        val slack = expected / 6
        for (i in listOf(0, 1, 3)) {
            assertTrue(
                "counts[$i]=${counts[i]} should be ~$expected (±$slack)",
                kotlin.math.abs(counts[i] - expected) < slack,
            )
        }
    }

    @Test
    fun `last == 0 still picks uniformly from rest`() {
        val counts = IntArray(3)
        val trials = 30_000
        val rng = kotlin.random.Random(7L)
        repeat(trials) {
            val p = pick(last = 0, size = 3, rng = rng)
            counts[p]++
        }
        assertEquals(0, counts[0])
        val expected = trials / 2
        val slack = expected / 6
        assertTrue(kotlin.math.abs(counts[1] - expected) < slack)
        assertTrue(kotlin.math.abs(counts[2] - expected) < slack)
    }

    @Test
    fun `last == size minus 1 still picks uniformly from rest`() {
        val counts = IntArray(3)
        val trials = 30_000
        val rng = kotlin.random.Random(11L)
        repeat(trials) {
            val p = pick(last = 2, size = 3, rng = rng)
            counts[p]++
        }
        assertEquals(0, counts[2])
        val expected = trials / 2
        val slack = expected / 6
        assertTrue(kotlin.math.abs(counts[0] - expected) < slack)
        assertTrue(kotlin.math.abs(counts[1] - expected) < slack)
    }

    @Test
    fun `out-of-range last falls back to uniform full pick`() {
        val counts = IntArray(3)
        val trials = 30_000
        val rng = kotlin.random.Random(99L)
        repeat(trials) {
            val p = pick(last = 3, size = 3, rng = rng)
            counts[p]++
        }
        val expected = trials / 3
        val slack = expected / 4
        for (i in 0..2) {
            assertTrue(
                "counts[$i]=${counts[i]} should be ~$expected (±$slack)",
                kotlin.math.abs(counts[i] - expected) < slack,
            )
        }
    }

    @Test
    fun `over many calls picker never repeats same index back-to-back`() {
        // Walk sequential picks; ensure pick[i] != pick[i-1] for all i.
        // Simulates the announcer's stateful flow.
        val rng = kotlin.random.Random(2026L)
        val size = 3
        var last = -1
        for (i in 0 until 5000) {
            val p = pick(last = last, size = size, rng = rng)
            if (last >= 0) {
                assertFalse("iter=$i: pick $p should differ from last $last", p == last)
            }
            last = p
        }
    }

    // ── Audit-2 Risk #1 — regression guards specific to the BIASED previous impl ──

    @Test
    fun `regression — biased impl would have failed this uniformity test for size 3`() {
        // The OLD biased algorithm produced (last+1) % size with ~2× probability.
        // For size=3, last=1: index 2 would have appeared ~66.6% of trials.
        // We assert UNIFORM, so any "optimization" that reintroduces the bias
        // will overshoot the upper bound here.
        val counts = IntArray(3)
        val trials = 30_000
        val rng = kotlin.random.Random(424242L)
        repeat(trials) {
            val p = pick(last = 1, size = 3, rng = rng)
            counts[p]++
        }
        // If biased toward index 2: counts[2] ≈ 20000 (66.6%). Uniform bound
        // is 15000 ± 2500. Anything above 17500 (58%) is a regression flag.
        val biasThreshold = (trials * 0.58).toInt()
        assertTrue(
            "counts[2]=${counts[2]} crossed biased threshold $biasThreshold — picker may be back to (last+1)%size bias",
            counts[2] < biasThreshold,
        )
    }

    @Test
    fun `regression — biased impl would have failed for size 4 too`() {
        // For size=4, last=2: biased index 3 would be ~50%. Uniform expectation
        // is 33%. Anything > 42% flags regression.
        val counts = IntArray(4)
        val trials = 30_000
        val rng = kotlin.random.Random(7777L)
        repeat(trials) {
            val p = pick(last = 2, size = 4, rng = rng)
            counts[p]++
        }
        val biasThreshold = (trials * 0.42).toInt()
        assertTrue(
            "counts[3]=${counts[3]} crossed biased threshold $biasThreshold",
            counts[3] < biasThreshold,
        )
    }
}
