package com.tranphuloi.neon.ui.game.booster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 86 BUG FIX verification — InfoScreen.drawBoosterPreview historically
 * dispatched on `BoosterType.drawableId` (6 base shapes) bypassing all R78-R85
 * BoosterShape redesigns (TWIN_CANNON/KATANA/PRISM/SINE_WAVE/etc.).
 *
 * Symptom: in Bách Khoa Vật phẩm tab, 27 BoosterTypes only showed 6 unique
 * shapes. Players saw "many items trùng shape" while in-game render was OK.
 *
 * R86 fix: extracted shared [com.tranphuloi.neon.ui.game.world.drawBoosterShape]
 * dispatcher used by both BoosterCanvas (in-game) + InfoScreen (preview).
 *
 * These tests ensure the mapper assigns truly unique shape per BoosterType,
 * preventing regression to the 6-shape state.
 */
class BoosterShapeRenderConsistencyTest {

    private val mapper = BoosterToBoosterUIMapper()

    @Test
    fun `27 BoosterTypes map to 27 distinct BoosterShapes (no preview-side dup)`() {
        val shapeOf = BoosterType.values().associateWith { mapper.shapeFor(it) }
        val byShape = shapeOf.entries.groupBy({ it.value }) { it.key }
        val dups = byShape.filter { it.value.size > 1 }
        assertTrue(
            "BoosterTypes share BoosterShape — bug regressed: $dups",
            dups.isEmpty(),
        )
        assertEquals(
            "Expected 27 distinct shapes used (one per BoosterType)",
            27, byShape.size,
        )
    }

    @Test
    fun `user-complained items map to distinct R78-R85 shapes (not base 6)`() {
        // User R86 callout: vũ khí tối thượng / plasma / cuồng nộ / khổng lồ /
        // năng lượng / nguyên tử "still duplicate". These ALL should resolve to
        // unique non-base shapes after R78-R85.
        val complained = mapOf(
            BoosterType.ULTIMATE_WEAPON_BOOSTER to BoosterShape.STAR,
            BoosterType.PLASMA_BOOSTER to BoosterShape.RING_PULSE,       // prism gem
            BoosterType.BERSERK to BoosterShape.RAGE_FANG,
            BoosterType.GIANT_BOOSTER to BoosterShape.BIG_DOT,           // boulder
            BoosterType.KAMEHAMEHA_BOOSTER to BoosterShape.BEAM,         // sine wave
            BoosterType.ATOMIC_BOOSTER to BoosterShape.ATOM,
        )
        for ((type, expected) in complained) {
            assertEquals(
                "User-complained $type should map to $expected, got ${mapper.shapeFor(type)}",
                expected, mapper.shapeFor(type),
            )
        }
        // Verify these 6 user-callouts use 6 DIFFERENT shapes (no internal dup).
        val shapes = complained.values.toSet()
        assertEquals(
            "User-complained items have duplicate shapes",
            complained.size, shapes.size,
        )
    }
}
