package com.tranphuloi.neon.ui.game.booster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 56 — validate that the weighted-random `Booster.type` roll actually
 * produces the distribution implied by [BoosterType.weight].
 *
 * Triggered by 3 consecutive runtime logs showing 0 PIERCING/PLASMA drops
 * after round 55 weight bump (8 → 12, combined 13.8% → 19.4%). Possibilities:
 *   (a) APK on device didn't pick up the round 55 change (build cache);
 *   (b) Weighted-pick algorithm in Booster.kt has a subtle bias;
 *   (c) RNG variance — possible but increasingly unlikely cumulative.
 *
 * This test exercises (b) directly. If 1000 simulated rolls match weights
 * within 30% tolerance, the algorithm is sound and the runtime gap is (a)
 * or (c) — not a code bug.
 *
 * NOTE: [kotlin.random.Random.Default] is not seeded here. With N=1000 and
 * each type's binomial std dev ~9 (PIERCING: σ ≈ sqrt(1000 × 0.097 × 0.903)
 * = 9.4), the 30% tolerance window is ~30/9.4 ≈ 3σ — flake probability ~0.3%
 * per type across each test run. Acceptable for a sanity smoke test.
 */
class BoosterTypeDistributionTest {

    private fun rollBoosters(count: Int): Map<BoosterType, Int> {
        val tally = mutableMapOf<BoosterType, Int>()
        BoosterType.entries.forEach { tally[it] = 0 }
        repeat(count) {
            val b = Booster(id = "t$it", xOffset = 0f, size = 40f, screenHeight = 800f)
            tally[b.type] = tally.getValue(b.type) + 1
        }
        return tally
    }

    @Test
    fun `every booster type is reachable in 1000 rolls`() {
        // Smoke test: weighted-pick algorithm must produce every enum entry at
        // least once. If any type is unreachable the algorithm has a structural
        // bug (e.g., loop termination misses the last bucket).
        val tally = rollBoosters(1000)
        BoosterType.entries.forEach { type ->
            assertTrue(
                "$type produced 0 rolls in N=1000 — algorithm cannot reach this type",
                tally.getValue(type) > 0,
            )
        }
    }

    @Test
    fun `distribution matches weights within 30 percent tolerance`() {
        val n = 1000
        val totalWeight = BoosterType.entries.sumOf { it.weight }.toDouble()
        val tally = rollBoosters(n)

        BoosterType.entries.forEach { type ->
            val expected = n * type.weight / totalWeight
            val actual = tally.getValue(type)
            val tolerance = expected * 0.30
            val low = expected - tolerance
            val high = expected + tolerance
            assertTrue(
                "$type weight=${type.weight}: expected ${"%.1f".format(expected)} " +
                    "(±${"%.1f".format(tolerance)}), got $actual — distribution skewed",
                actual.toDouble() in low..high,
            )
        }
    }

    @Test
    fun `PIERCING reaches at least 30 in N=1000 (round 60 recalibrated)`() {
        // Round 55 floor was 50 (~5σ at 9.7% expected). Round 60 (38x) added 10
        // new boosters at weight 6 each → total weight 124 → 184, diluting
        // PIERCING share to 12/184 = 6.5%. New floor 30 = ~5σ below 65 expected.
        val tally = rollBoosters(1000)
        val piercing = tally.getValue(BoosterType.PIERCING_BOOSTER)
        assertTrue(
            "PIERCING produced $piercing/1000 = ${piercing / 10.0}% — should be ~6.5%. " +
                "If this fails, weight=12 isn't taking effect at runtime.",
            piercing >= 30,
        )
    }

    @Test
    fun `PLASMA reaches at least 30 in N=1000 (round 60 recalibrated)`() {
        val tally = rollBoosters(1000)
        val plasma = tally.getValue(BoosterType.PLASMA_BOOSTER)
        assertTrue(
            "PLASMA produced $plasma/1000 = ${plasma / 10.0}% — should be ~6.5%. " +
                "If this fails, weight=12 isn't taking effect at runtime.",
            plasma >= 30,
        )
    }

    @Test
    fun `combined PIERCING+PLASMA reaches at least 90 in N=1000 (round 60)`() {
        val tally = rollBoosters(1000)
        val combined = tally.getValue(BoosterType.PIERCING_BOOSTER) +
            tally.getValue(BoosterType.PLASMA_BOOSTER)
        // Round 55: expected 19.4% combined (24/124), floor 150.
        // Round 60: expected 13.0% combined (24/184), floor 90 = ~5σ below 130.
        assertTrue(
            "PIERCING+PLASMA combined $combined/1000 = ${combined / 10.0}% — should be ~13.0%. " +
                "If this fails, round 55 weight bump (8 → 12) isn't active.",
            combined >= 90,
        )
    }

    @Test
    fun `weight ordering is preserved in observed counts (LASER greater than PIERCING greater than REVIVE)`() {
        // Sanity check that higher-weight types appear more often. Uses N=2000
        // so the relative ordering is stable even at the tails (REVIVE weight=5
        // could otherwise be noisy at N=1000).
        val tally = rollBoosters(2000)
        val laserCount = tally.getValue(BoosterType.LASER_BOOSTER)         // weight 19
        val piercingCount = tally.getValue(BoosterType.PIERCING_BOOSTER)   // weight 12
        val reviveCount = tally.getValue(BoosterType.REVIVE_TOKEN)         // weight 5
        assertTrue(
            "LASER ($laserCount) should be more common than PIERCING ($piercingCount)",
            laserCount > piercingCount,
        )
        assertTrue(
            "PIERCING ($piercingCount) should be more common than REVIVE ($reviveCount)",
            piercingCount > reviveCount,
        )
    }

    @Test
    fun `total tally equals N (no dropped or double-counted rolls)`() {
        val n = 1000
        val tally = rollBoosters(n)
        assertEquals(n, tally.values.sum())
    }
}
