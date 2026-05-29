package com.tranphuloi.neon.ui.game.booster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 86 follow-up — color path BUG FIX verification.
 *
 * Symptom: InfoScreen.boosterColor dispatched on `BoosterType.drawableId`
 * (6 base sprites), so 21 non-base BoosterTypes inherited their
 * sprite-mate's color. After the R86 shape fix, shapes became distinct but
 * 5 ultimate-family boosters (ULTIMATE/PLASMA/BERSERK/GIANT/KAMEHAMEHA/
 * ATOMIC) still rendered the same gold → user perceived "trùng".
 *
 * Fix: previewColorArgb() now exhaustive per-type → 27 distinct colors,
 * with 4 collision overrides where in-game tint shared hex with a base-6
 * sprite hue (PLASMA/QUICK_HEAL/HEALING_AURA/GIANT).
 */
class BoosterPreviewColorDistinctTest {

    private val mapper = BoosterToBoosterUIMapper()

    @Test
    fun `all BoosterTypes map to distinct preview colors (no duplicate)`() {
        val colorOf = BoosterType.values().associateWith { mapper.previewColorArgb(it) }
        val byColor = colorOf.entries.groupBy({ it.value }) { it.key }
        val dups = byColor.filter { it.value.size > 1 }
        assertTrue(
            "BoosterTypes share preview color — bug regressed: $dups",
            dups.isEmpty(),
        )
        assertEquals(
            "Expected one distinct preview color per BoosterType",
            BoosterType.values().size, byColor.size,
        )
    }

    @Test
    fun `ultimate-family 6 boosters get 6 distinct preview colors (user R86 callout)`() {
        // User R86: vũ khí tối thượng / plasma / cuồng nộ / khổng lồ /
        // năng lượng / nguyên tử "still duplicate". All 5 share booster_ultimate_weapon
        // drawable + ULTIMATE itself uses gold sprite → preview color must differ.
        val ultimateFamily = listOf(
            BoosterType.ULTIMATE_WEAPON_BOOSTER,
            BoosterType.PLASMA_BOOSTER,
            BoosterType.BERSERK,
            BoosterType.GIANT_BOOSTER,
            BoosterType.KAMEHAMEHA_BOOSTER,
            BoosterType.ATOMIC_BOOSTER,
        )
        val colors = ultimateFamily.map { mapper.previewColorArgb(it) }.toSet()
        assertEquals(
            "Ultimate-family boosters share preview color — user complaint regressed",
            ultimateFamily.size, colors.size,
        )
    }

    @Test
    fun `health-family 5 boosters get distinct preview colors`() {
        // 5 boosters share booster_health drawable → must get distinct colors.
        val healthFamily = listOf(
            BoosterType.HEALTH_BOOSTER,
            BoosterType.SCORE_X3,
            BoosterType.QUICK_HEAL,
            BoosterType.MINERAL_SUPERCHARGE,
            BoosterType.HEALING_AURA,
        )
        val colors = healthFamily.map { mapper.previewColorArgb(it) }.toSet()
        assertEquals(
            "Health-family boosters share preview color",
            healthFamily.size, colors.size,
        )
    }

    @Test
    fun `laser-family 8 boosters get distinct preview colors`() {
        val laserFamily = listOf(
            BoosterType.LASER_BOOSTER,
            BoosterType.PIERCING_BOOSTER,
            BoosterType.CRIT_SURGE,
            BoosterType.DOUBLE_FIRE,
            BoosterType.FIRE_BOOSTER,
            BoosterType.BOUNCE_BOOSTER,
            BoosterType.ZIGZAG_BOOSTER,
            BoosterType.SPLIT_BOOSTER,
        )
        val colors = laserFamily.map { mapper.previewColorArgb(it) }.toSet()
        assertEquals(
            "Laser-family boosters share preview color",
            laserFamily.size, colors.size,
        )
    }

    @Test
    fun `preview color is fully opaque (alpha = 0xFF)`() {
        // Halo overlay uses alpha = 0.3f; the BASE color must be fully opaque
        // so alpha-blending math doesn't yield a translucent ghost.
        for (type in BoosterType.values()) {
            val argb = mapper.previewColorArgb(type)
            val alpha = (argb ushr 24) and 0xFF
            assertEquals(
                "Preview color for $type must have alpha=0xFF, got 0x${alpha.toString(16)}",
                0xFFL, alpha,
            )
        }
    }
}
