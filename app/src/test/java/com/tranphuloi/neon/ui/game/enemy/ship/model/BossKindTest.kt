package com.tranphuloi.neon.ui.game.enemy.ship.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 81 audit — verifies [BossKind] enum integrity.
 *
 * Roster: 5 base (R71) + 4 R79 + 12 R81 + 3 Wave15 = 24 total.
 * Each name must be unique (Kotlin enforces this) — test documents the count
 * + verifies no value is accidentally renamed without test update.
 */
class BossKindTest {

    @Test
    fun `BossKind has expected 27 values across R71 R79 R81 Wave15 Wave16`() {
        assertEquals(27, BossKind.values().size)
    }

    @Test
    fun `R71 base 5 BossKinds exist`() {
        val expected = setOf(
            BossKind.STAR, BossKind.CROSS, BossKind.ORB,
            BossKind.FRACTAL, BossKind.SPIDER,
        )
        assertTrue(
            "R71 base BossKinds missing",
            expected.all { it in BossKind.values() },
        )
    }

    @Test
    fun `R79 audit fix 4 BossKinds exist`() {
        val expected = setOf(
            BossKind.DEATH_MOON, BossKind.HAUNTED_KID,
            BossKind.HELL_LORD, BossKind.SATAN_GLYPH,
        )
        assertTrue(
            "R79 audit BossKinds missing",
            expected.all { it in BossKind.values() },
        )
    }

    @Test
    fun `R81 12 new BossKinds exist`() {
        val expected = setOf(
            BossKind.HEN_MOTHER, BossKind.BUFFALO_RAGE, BossKind.DUMB_RAT,
            BossKind.FIERCE_TIGER, BossKind.SEXY_DIVA, BossKind.TROLL_TOWER,
            BossKind.TWIN_SUMMITS, BossKind.VOID_GLOBES, BossKind.WHITE_DRAGON,
            BossKind.HAMMER_SICKLE, BossKind.MONEY_TYCOON, BossKind.GOLDEN_TYCOON,
        )
        assertTrue(
            "R81 BossKinds missing",
            expected.all { it in BossKind.values() },
        )
    }

    @Test
    fun `Wave15 3 new BossKinds exist`() {
        val expected = setOf(
            BossKind.SKULL_CROSSBONES, BossKind.VAMPIRE, BossKind.COSMIC_CENTIPEDE,
        )
        assertTrue(
            "Wave15 BossKinds missing",
            expected.all { it in BossKind.values() },
        )
    }

    @Test
    fun `Wave16 3 new BossKinds exist`() {
        val expected = setOf(
            BossKind.GIANT_CONDOM, BossKind.VENOM_SPIDER, BossKind.CORRUPTION,
        )
        assertTrue(
            "Wave16 BossKinds missing",
            expected.all { it in BossKind.values() },
        )
    }

    @Test
    fun `BossKind names are unique`() {
        val names = BossKind.values().map { it.name }
        assertEquals(
            "BossKind enum has duplicate names",
            names.size, names.toSet().size,
        )
    }

    @Test
    fun `every BossKind has a non-blank Vietnamese displayName`() {
        for (kind in BossKind.values()) {
            assertTrue(
                "BossKind.$kind has blank displayName",
                kind.displayName.isNotBlank(),
            )
            // No dev-jargon leakage (LEVEL 1 BOSS / TIỂU BOSS / OFFENSIVE / etc.).
            assertFalse(
                "BossKind.$kind has dev-jargon displayName='${kind.displayName}'",
                kind.displayName.contains("LEVEL") ||
                    kind.displayName.contains("TIỂU BOSS") ||
                    kind.displayName.contains("OFFENSIVE") ||
                    kind.displayName.contains("DEFENSIVE") ||
                    kind.displayName.contains("SWARM"),
            )
        }
    }

    @Test
    fun `all BossKind displayNames are unique`() {
        val names = BossKind.values().map { it.displayName }
        assertEquals(
            "BossKind displayNames have duplicates",
            names.size, names.toSet().size,
        )
    }
}
