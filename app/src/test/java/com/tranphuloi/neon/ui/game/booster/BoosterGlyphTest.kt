package com.tranphuloi.neon.ui.game.booster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R86 follow-up #2 — glyph badge identity tests.
 *
 * Invariant: glyphFor() is the SINGLE source of truth for booster badges —
 * BoosterToBoosterUIMapper.invoke() delegates to it, so both in-game render
 * (GameWorld.kt:282) and InfoScreen preview (BoosterCard) render the same
 * glyph for each BoosterType.
 *
 * These tests guard the contract so a future refactor that re-inlines the
 * mapping (regressing R86) fails CI.
 */
class BoosterGlyphTest {

    private val mapper = BoosterToBoosterUIMapper()

    /** Base 6 boosters have iconic shape+drawable → no glyph needed. */
    private val baseSix = setOf(
        BoosterType.HEALTH_BOOSTER,
        BoosterType.SHIELD_BOOSTER,
        BoosterType.LASER_BOOSTER,
        BoosterType.TRIPLE_LASER_BOOSTER,
        BoosterType.ULTIMATE_WEAPON_BOOSTER,
        BoosterType.REVIVE_TOKEN,
    )

    @Test
    fun `glyphFor returns null exactly for base 6 BoosterTypes`() {
        for (type in BoosterType.values()) {
            val glyph = mapper.glyphFor(type)
            if (type in baseSix) {
                assertNull("Base-6 $type must NOT have a glyph (sprite self-identifies)", glyph)
            } else {
                assertNotNull("Non-base $type must have a glyph", glyph)
                assertFalse("Non-base $type glyph must not be blank", glyph!!.isBlank())
            }
        }
    }

    @Test
    fun `21 non-base BoosterTypes have 21 distinct glyphs`() {
        val nonBase = BoosterType.values().filter { it !in baseSix }
        val glyphs = nonBase.mapNotNull { mapper.glyphFor(it) }
        assertEquals(
            "Every non-base type should produce a glyph",
            nonBase.size, glyphs.size,
        )
        val unique = glyphs.toSet()
        val dups = glyphs.groupBy { it }.filter { it.value.size > 1 }
        assertTrue(
            "Non-base glyphs share visual badge — user can't disambiguate: $dups",
            dups.isEmpty(),
        )
        assertEquals(
            "Expected 21 unique non-base glyphs",
            nonBase.size, unique.size,
        )
    }

    @Test
    fun `glyphs are short (max 2 chars) for TopEnd badge fit`() {
        // BoosterCard offsets the Text by (4dp, -4dp) at TopEnd — long glyphs
        // would clip the icon box border. fontSize=12sp leaves room for ≤2 chars.
        for (type in BoosterType.values()) {
            val glyph = mapper.glyphFor(type) ?: continue
            assertTrue(
                "Glyph '$glyph' for $type too long (${glyph.length}) — TopEnd 4dp offset can't fit",
                glyph.length <= 2,
            )
        }
    }

    @Test
    fun `glyphFor uses no emoji (Unicode-only neon aesthetic)`() {
        // R67 (Wave 10a) spec — booster glyphs are pure Unicode symbols, never
        // emoji (which break the neon vector aesthetic + render inconsistently
        // across Android API levels). Guards against accidental emoji adds.
        for (type in BoosterType.values()) {
            val glyph = mapper.glyphFor(type) ?: continue
            for (codepoint in glyph.codePoints().toArray()) {
                // Emoji ranges: U+1F300..U+1FAFF (misc symbols + pictographs) and
                // U+2600..U+27BF (dingbats — but some neon glyphs like ✚ ✦ ✱ ☆
                // ⚡ live here, so we allow this range). Strict-block only the
                // 4-byte emoji range.
                assertFalse(
                    "Glyph '$glyph' for $type uses emoji codepoint U+${codepoint.toString(16).uppercase()}",
                    codepoint in 0x1F300..0x1FAFF,
                )
            }
        }
    }

    @Test
    fun `R86 user-complained boosters render expected glyphs (verify in-game vs preview parity)`() {
        // User R86 callout boosters — verify exact glyph each renders, since
        // these were the ones perceived as duplicate. Pin the visual contract.
        val expected = mapOf(
            BoosterType.ULTIMATE_WEAPON_BOOSTER to null,    // base — STAR shape carries identity
            BoosterType.PLASMA_BOOSTER to "◯",
            BoosterType.BERSERK to "⚡",
            BoosterType.GIANT_BOOSTER to "⬤",
            BoosterType.KAMEHAMEHA_BOOSTER to "⊛",
            BoosterType.ATOMIC_BOOSTER to "⊙",
        )
        for ((type, glyph) in expected) {
            assertEquals(
                "R86-callout $type glyph regressed",
                glyph, mapper.glyphFor(type),
            )
        }
    }
}
