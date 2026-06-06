package com.tranphuloi.neon.ui.game.modifier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 21 (#3) — thử thách TAY KHÔNG (`BARE_HANDED`): không buff power-up nào rơi.
 */
class BareHandedModifierTest {

    @Test
    fun `BARE_HANDED sets noBoosters and rewards score`() {
        val m = RunModifier.BARE_HANDED
        assertTrue("phải bật noBoosters", m.noBoosters)
        assertEquals("key ổn định cho DataStore", "bare_handed", m.key)
        assertTrue("điểm thưởng > 1", m.scoreMul > 1f)
    }

    @Test
    fun `only BARE_HANDED has noBoosters true`() {
        val withFlag = RunModifier.values().filter { it.noBoosters }
        assertEquals(listOf(RunModifier.BARE_HANDED), withFlag)
    }

    @Test
    fun `default modifiers do not drop boosters`() {
        assertFalse(RunModifier.NONE.noBoosters)
        assertFalse(RunModifier.GLASS_CANNON.noBoosters)
    }

    @Test
    fun `fromKey round-trips bare_handed`() {
        assertEquals(RunModifier.BARE_HANDED, RunModifier.fromKey("bare_handed"))
    }
}
