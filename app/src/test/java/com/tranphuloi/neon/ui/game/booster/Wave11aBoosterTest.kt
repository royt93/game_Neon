package com.tranphuloi.neon.ui.game.booster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Defensive invariant — Wave 11a boosters identity contracts.
 *
 * Pin shape + glyph + color for each Wave 11a booster so future refactors
 * can't silently drift the visual identity. Mirrors pattern from
 * BoosterPreviewColorDistinctTest / BoosterGlyphTest but focused on
 * Wave 11a additions.
 */
class Wave11aBoosterTest {

    private val mapper = BoosterToBoosterUIMapper()

    /** Wave 11a booster identity table — pin shape + glyph for each new entry. */
    private val identity = mapOf(
        BoosterType.REGEN_BOOSTER to Triple(BoosterShape.REGEN_PULSE, "♻", BoosterToBoosterUIMapper.REGEN_TINT_ARGB),
        BoosterType.TIME_FREEZE_BOOSTER to Triple(BoosterShape.FREEZE_FLAKE, "❄", BoosterToBoosterUIMapper.TIME_FREEZE_TINT_ARGB),
        BoosterType.MINI_BOOSTER to Triple(BoosterShape.MINI_RING, "◌", BoosterToBoosterUIMapper.MINI_TINT_ARGB),
        BoosterType.VAMPIRE_BOOSTER to Triple(BoosterShape.VAMPIRE_FANG, "Ѵ", BoosterToBoosterUIMapper.VAMPIRE_TINT_ARGB),
        BoosterType.GHOST_BOOSTER to Triple(BoosterShape.GHOST_TRAIL, "ѻ", BoosterToBoosterUIMapper.GHOST_TINT_ARGB),
        BoosterType.GRAVITY_BOOSTER to Triple(BoosterShape.GRAVITY_WELL, "⊚", BoosterToBoosterUIMapper.GRAVITY_TINT_ARGB),
        BoosterType.REFLECT_BOOSTER to Triple(BoosterShape.REFLECT_BUMPER, "⇋", BoosterToBoosterUIMapper.REFLECT_TINT_ARGB),
        BoosterType.CHAIN_LIGHTNING_BOOSTER to Triple(BoosterShape.CHAIN_BOLT, "⚜", BoosterToBoosterUIMapper.CHAIN_LIGHTNING_TINT_ARGB),
        BoosterType.CLONE_BOOSTER to Triple(BoosterShape.CLONE_PAIR, "ǁ", BoosterToBoosterUIMapper.CLONE_TINT_ARGB),
    )

    @Test
    fun `every Wave 11a booster maps to its declared shape`() {
        for ((type, triple) in identity) {
            val expected = triple.first
            assertEquals(
                "Wave 11a $type drifted from expected shape",
                expected, mapper.shapeFor(type),
            )
        }
    }

    @Test
    fun `every Wave 11a booster maps to its declared glyph`() {
        for ((type, triple) in identity) {
            val expected = triple.second
            assertEquals(
                "Wave 11a $type drifted from expected glyph",
                expected, mapper.glyphFor(type),
            )
        }
    }

    @Test
    fun `every Wave 11a booster maps to its declared preview color`() {
        for ((type, triple) in identity) {
            val expected = triple.third
            assertEquals(
                "Wave 11a $type drifted from expected color",
                expected, mapper.previewColorArgb(type),
            )
        }
    }

    @Test
    fun `Wave 11a glyphs are unique among non-base boosters`() {
        val wave11aGlyphs = identity.values.map { it.second }
        val unique = wave11aGlyphs.toSet()
        assertEquals(
            "Wave 11a boosters share a glyph: $wave11aGlyphs",
            wave11aGlyphs.size, unique.size,
        )
    }

    @Test
    fun `Wave 11a colors are unique among non-base boosters`() {
        val wave11aColors = identity.values.map { it.third }
        val unique = wave11aColors.toSet()
        assertEquals(
            "Wave 11a boosters share a color: $wave11aColors",
            wave11aColors.size, unique.size,
        )
    }

    @Test
    fun `Wave 11a shapes are unique enum entries (not aliased)`() {
        val wave11aShapes = identity.values.map { it.first }
        val unique = wave11aShapes.toSet()
        assertEquals(
            "Wave 11a boosters share a shape: $wave11aShapes",
            wave11aShapes.size, unique.size,
        )
    }

    @Test
    fun `REGEN color is distinct from HEALING_AURA (both heal-themed)`() {
        // R86 audit: HEALING_AURA shifted from mint 0xFF60FFAA → sea-green 0xFF20D090.
        // REGEN should not collide — it's a slower heal so picks soft-pastel green.
        assertFalse(
            "REGEN color collides with HEALING_AURA — visually indistinguishable in preview",
            mapper.previewColorArgb(BoosterType.REGEN_BOOSTER) ==
                mapper.previewColorArgb(BoosterType.HEALING_AURA),
        )
    }

    @Test
    fun `GHOST color is distinct from PHASE_SHIELD (both translucent-themed)`() {
        assertFalse(
            "GHOST color collides with PHASE_SHIELD — both translucent boosters",
            mapper.previewColorArgb(BoosterType.GHOST_BOOSTER) ==
                mapper.previewColorArgb(BoosterType.PHASE_SHIELD),
        )
    }

    @Test
    fun `VAMPIRE color is distinct from BERSERK (both red-themed)`() {
        assertFalse(
            "VAMPIRE color collides with BERSERK — both red boosters",
            mapper.previewColorArgb(BoosterType.VAMPIRE_BOOSTER) ==
                mapper.previewColorArgb(BoosterType.BERSERK),
        )
    }

    @Test
    fun `GRAVITY color is distinct from MAGNET_BOOST (both pull-themed)`() {
        assertFalse(
            "GRAVITY color collides with MAGNET_BOOST — both attraction-themed",
            mapper.previewColorArgb(BoosterType.GRAVITY_BOOSTER) ==
                mapper.previewColorArgb(BoosterType.MAGNET_BOOST),
        )
    }

    @Test
    fun `CHAIN_LIGHTNING color is distinct from PLASMA + KAMEHAMEHA (all blue-themed)`() {
        val chainColor = mapper.previewColorArgb(BoosterType.CHAIN_LIGHTNING_BOOSTER)
        assertFalse(
            "CHAIN_LIGHTNING color collides with PLASMA — both blue",
            chainColor == mapper.previewColorArgb(BoosterType.PLASMA_BOOSTER),
        )
        assertFalse(
            "CHAIN_LIGHTNING color collides with KAMEHAMEHA — both blue",
            chainColor == mapper.previewColorArgb(BoosterType.KAMEHAMEHA_BOOSTER),
        )
    }

    @Test
    fun `REFLECT color is distinct from SCORE_X3 + ZIGZAG (all yellow-themed)`() {
        val reflectColor = mapper.previewColorArgb(BoosterType.REFLECT_BOOSTER)
        assertFalse(
            "REFLECT color collides with SCORE_X3 — both gold-yellow",
            reflectColor == mapper.previewColorArgb(BoosterType.SCORE_X3),
        )
        assertFalse(
            "REFLECT color collides with ZIGZAG — both yellow",
            reflectColor == mapper.previewColorArgb(BoosterType.ZIGZAG_BOOSTER),
        )
    }

    @Test
    fun `TIME_FREEZE_BOOSTER weight preserves REVIVE_TOKEN as rarest`() {
        // Wave 11a tried weight=4 for TIME_FREEZE (rare strong effect) but that
        // broke "REVIVE rarest" invariant. Test pins TIME_FREEZE >= REVIVE_TOKEN
        // weight so duration shortness is the rarity lever, not pickup rate.
        assertTrue(
            "TIME_FREEZE_BOOSTER weight ${BoosterType.TIME_FREEZE_BOOSTER.weight} dropped below REVIVE_TOKEN (5) — invariant regressed",
            BoosterType.TIME_FREEZE_BOOSTER.weight >= BoosterType.REVIVE_TOKEN.weight,
        )
    }
}
