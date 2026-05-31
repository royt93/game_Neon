package com.tranphuloi.neon.ui.info

import com.tranphuloi.neon.ui.game.booster.BoosterToBoosterUIMapper
import com.tranphuloi.neon.ui.game.booster.BoosterType
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import com.tranphuloi.neon.ui.game.ship.laser.BulletTypeColorMap
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Defensive invariant — [BulletTypeColorMap] must stay in lockstep with the
 * booster identity tint constants in [BoosterToBoosterUIMapper]. Historical
 * bug: bullet color was 11 hardcoded hex literals copy-pasted from booster
 * tints; when PLASMA/GIANT tints shifted, two literals drifted because
 * compile-checks don't catch semantic "same identity = same color"
 * invariants.
 *
 * Current implementation references mapper constants from a single object
 * so it can't drift. These tests pin that contract:
 *   1. Every BulletType that has a matching BoosterType (e.g. PLASMA bullet
 *      activated by PLASMA_BOOSTER) shares the same ARGB.
 *   2. NORMAL bullet (no booster origin) uses NeonCyan from the theme.
 *   3. Pair table stays in sync with the BulletType enum.
 */
class BulletColorIdentityTest {

    private val mapper = BoosterToBoosterUIMapper()

    /** Pairs of BulletType ↔ BoosterType that share a visual identity. */
    private val pairs = listOf(
        BulletType.PIERCING to BoosterType.PIERCING_BOOSTER,
        BulletType.PLASMA to BoosterType.PLASMA_BOOSTER,
        BulletType.FIRE to BoosterType.FIRE_BOOSTER,
        BulletType.HOMING to BoosterType.HOMING_BOOSTER,
        BulletType.BOUNCE to BoosterType.BOUNCE_BOOSTER,
        BulletType.GIANT to BoosterType.GIANT_BOOSTER,
        BulletType.SMOKE to BoosterType.SMOKE_BOOSTER,
        BulletType.ZIGZAG to BoosterType.ZIGZAG_BOOSTER,
        BulletType.KAMEHAMEHA to BoosterType.KAMEHAMEHA_BOOSTER,
        BulletType.ATOMIC to BoosterType.ATOMIC_BOOSTER,
        BulletType.SPLIT to BoosterType.SPLIT_BOOSTER,
    )

    @Test
    fun `BulletTypeColorMap matches mapper previewColorArgb for every paired BulletType-BoosterType`() {
        for ((bullet, booster) in pairs) {
            val expected = mapper.previewColorArgb(booster)
            val actual = BulletTypeColorMap.argbFor(bullet)
            assertEquals(
                "Bullet $bullet drifted from booster $booster — hex literal regression?",
                expected, actual,
            )
        }
    }

    @Test
    fun `NORMAL bullet (no booster origin) uses NeonCyan from theme`() {
        // NeonCyan = 0xFF00F0FF (also SHIELD's natural sprite hue, but NORMAL
        // isn't a booster effect — the visual reuse is theme-level, not bug).
        assertEquals(0xFF00F0FFL, BulletTypeColorMap.argbFor(BulletType.NORMAL))
        assertEquals(BulletTypeColorMap.NORMAL_ARGB, BulletTypeColorMap.argbFor(BulletType.NORMAL))
    }

    /**
     * Wave 16 — satirical bullets with NO booster origin (color is a literal in
     * [BulletTypeColorMap], not derived from a BoosterType). Exempt from the
     * booster-pair completeness check below, but still accounted for so a NEW
     * bullet can't slip through uncategorised.
     */
    private val bulletOnlyLiteralColors = setOf(
        BulletType.LOTTERY, BulletType.FIREWORK, BulletType.BRICK,
        // Wave 16 batch 2
        BulletType.BANH_MI, BulletType.DURIAN, BulletType.HEART,
    )

    @Test
    fun `every non-NORMAL BulletType is either booster-paired or a known literal-color bullet`() {
        val coveredBullets = pairs.map { it.first }.toSet() + bulletOnlyLiteralColors
        val allNonNormal = BulletType.values().filter { it != BulletType.NORMAL }.toSet()
        assertEquals(
            "BulletType out of sync — new BulletType added without a color pair OR a " +
                "bulletOnlyLiteralColors entry?",
            allNonNormal, coveredBullets,
        )
    }
}
