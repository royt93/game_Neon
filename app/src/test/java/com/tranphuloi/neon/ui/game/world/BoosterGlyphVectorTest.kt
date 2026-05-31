package com.tranphuloi.neon.ui.game.world

import com.tranphuloi.neon.ui.game.booster.BoosterToBoosterUIMapper
import com.tranphuloi.neon.ui.game.booster.BoosterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 16 — pin the booster glyph → vector mapping.
 *
 * The in-game glyph badge used to be a `Text(glyph)` overlay. Many glyphs were
 * exotic Unicode (Ѱ Ѵ ѻ ǁ ⊛ ⌇ ❍ ⇋ ⚜ ⊚ ◌ …) with no glyph in the device font,
 * so they rendered as "?"/tofu. [drawBoosterGlyphVector] now draws each as a
 * font-free vector, dispatching on the glyph String with a generic-dot `else`
 * fallback.
 *
 * These tests guarantee no booster silently falls to that fallback: every
 * glyph the catalog can produce must have a dedicated vector
 * ([BOOSTER_GLYPH_VECTORS]).
 */
class BoosterGlyphVectorTest {

    private val mapper = BoosterToBoosterUIMapper()
    private val catalogGlyphs: Set<String> =
        BoosterType.entries.mapNotNull { mapper.glyphFor(it) }.toSet()

    @Test
    fun `every booster glyph has a dedicated vector (no tofu fallback)`() {
        val missing = catalogGlyphs - BOOSTER_GLYPH_VECTORS
        assertTrue(
            "Booster glyphs without a vector arm (would render generic dot): $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun `no dead vector entries (every drawn glyph is used by some booster)`() {
        val dead = BOOSTER_GLYPH_VECTORS - catalogGlyphs
        assertTrue(
            "Vector glyphs not produced by any booster (dead entries): $dead",
            dead.isEmpty(),
        )
    }

    @Test
    fun `vector glyph set exactly matches the booster catalog glyph set`() {
        assertEquals(catalogGlyphs, BOOSTER_GLYPH_VECTORS)
    }

    @Test
    fun `no booster glyph is empty or blank`() {
        // A blank glyph would draw nothing yet still allocate a badge Box.
        catalogGlyphs.forEach {
            assertTrue("Booster glyph must be non-blank", it.isNotBlank())
        }
    }
}
