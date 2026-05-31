package com.tranphuloi.neon.data

import com.tranphuloi.neon.ui.game.controls.BossRank
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 11b — pin the key-naming contract for MetaProgressionRepository.
 *
 * The repository derives DataStore preference keys from enum identifiers:
 *   - BulletType: "bullet_kill_${name}"
 *   - BossKind:   "boss_kill_${name}"
 *   - ShipSkin:   "ship_time_${key}"     (uses lower_snake key, not enum name)
 *   - BossRank:   "rank_dist_${letter}"  (uses S/A/B/C/D letter)
 *
 * If anyone renames an enum value, the migration would silently zero out
 * historical telemetry (old key still in DataStore, new key reads 0). These
 * tests don't prevent that — but they pin the SHAPE of every key so a rename
 * triggers a visible test break, forcing a conscious migration choice.
 */
class MetaProgressionKeysTest {

    // ── Enum sizes — pin so accidental delete/add is caught ──

    @Test
    fun `BulletType has 12 entries`() {
        assertEquals(18, BulletType.entries.size)
    }

    @Test
    fun `BossKind has 27 entries`() {
        assertEquals(27, BossKind.entries.size)
    }

    @Test
    fun `BossRank has 5 entries (S A B C D)`() {
        assertEquals(5, BossRank.entries.size)
        val letters = BossRank.entries.map { it.letter }.toSet()
        assertEquals(setOf("S", "A", "B", "C", "D"), letters)
    }

    @Test
    fun `ShipSkin has 8 entries`() {
        assertEquals(8, ShipSkin.entries.size)
    }

    // ── Key uniqueness — overlap would collide in DataStore ──

    @Test
    fun `BulletType names are unique`() {
        val names = BulletType.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `BossKind names are unique`() {
        val names = BossKind.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `ShipSkin keys are unique`() {
        val keys = ShipSkin.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `BossRank letters are unique`() {
        val letters = BossRank.entries.map { it.letter }
        assertEquals(letters.size, letters.toSet().size)
    }

    // ── Key prefix invariants — keys MUST differ across categories ──

    @Test
    fun `key prefixes don't collide across categories`() {
        // If "bullet_kill_X" and "boss_kill_X" prefixes ever overlapped, a
        // single DataStore key would back two semantic values. Pin both
        // prefixes literally so renames break this test.
        val bulletKey = "bullet_kill_" + BulletType.NORMAL.name
        val bossKey = "boss_kill_" + BossKind.STAR.name
        val skinKey = "ship_time_" + ShipSkin.AURA_CYAN.key
        val rankKey = "rank_dist_" + BossRank.S.letter
        val all = setOf(bulletKey, bossKey, skinKey, rankKey)
        assertEquals("All 4 derived keys distinct", 4, all.size)
    }

    // ── Key string content sanity ──

    @Test
    fun `BulletType names use only A-Z and underscore`() {
        BulletType.entries.forEach {
            val name = it.name
            assertTrue("$name has invalid chars",
                name.matches(Regex("[A-Z_]+")))
        }
    }

    @Test
    fun `BossKind names use only A-Z and underscore`() {
        BossKind.entries.forEach {
            val name = it.name
            assertTrue("$name has invalid chars",
                name.matches(Regex("[A-Z_]+")))
        }
    }

    @Test
    fun `ShipSkin keys are lowercase snake_case`() {
        ShipSkin.entries.forEach {
            val key = it.key
            assertTrue("$key not lowercase snake",
                key.matches(Regex("[a-z_]+")))
            assertFalse("$key is empty", key.isEmpty())
        }
    }

    @Test
    fun `BossRank letters are single uppercase char`() {
        BossRank.entries.forEach {
            val letter = it.letter
            assertEquals("$letter not single char", 1, letter.length)
            assertTrue("$letter not uppercase",
                letter.matches(Regex("[A-Z]")))
        }
    }

    // ── Stability pins — explicit name expectations ──
    // These break if anyone renames a heavily-trafficked enum value. The
    // break is the point: forces an intentional migration decision.

    @Test
    fun `BulletType NORMAL stays named NORMAL (DataStore key stability)`() {
        // NORMAL is the default bullet — its kill counter is the highest-
        // traffic key. Rename = old data orphaned + new data starts at 0.
        assertNotEquals("BulletType.NORMAL missing — key migration needed",
            null, BulletType.entries.firstOrNull { it.name == "NORMAL" })
    }

    @Test
    fun `BossKind STAR stays named STAR (first boss, key stability)`() {
        // STAR is Level 1 Boss, encountered by every campaign player.
        assertNotEquals("BossKind.STAR missing — key migration needed",
            null, BossKind.entries.firstOrNull { it.name == "STAR" })
    }

    @Test
    fun `ShipSkin AURA_CYAN stays keyed aura_cyan (default skin)`() {
        // AURA_CYAN is the default skin returned by fromKey fallback. Its
        // time-played counter accumulates for every fresh install.
        val cyan = ShipSkin.AURA_CYAN
        assertEquals("aura_cyan", cyan.key)
    }

    // ── P2-2 audit fix — full name-set pins ──
    // Size + single-name pins above won't catch renames that swap two entries
    // (e.g., NORMAL → FIRE, FIRE → NORMAL). The set-based pin below captures
    // EVERY name; any rename triggers a visible break.

    @Test
    fun `BulletType full name set is stable (rename detection)`() {
        val expected = setOf(
            "NORMAL", "PIERCING", "PLASMA", "FIRE", "HOMING",
            "BOUNCE", "GIANT", "SMOKE", "ZIGZAG", "KAMEHAMEHA",
            "ATOMIC", "SPLIT",
            // Wave 16 — đạn trào phúng batch 1
            "LOTTERY", "FIREWORK", "BRICK",
            // Wave 16 — đạn trào phúng batch 2
            "BANH_MI", "DURIAN", "HEART",
        )
        val actual = BulletType.entries.map { it.name }.toSet()
        assertEquals(
            "Rename detected — DataStore key migration required for renamed entries",
            expected, actual,
        )
    }

    @Test
    fun `BossKind full name set is stable (rename detection)`() {
        val expected = setOf(
            // R71 original
            "STAR", "CROSS", "ORB", "FRACTAL", "SPIDER",
            // R79 expansion
            "DEATH_MOON", "HAUNTED_KID", "HELL_LORD", "SATAN_GLYPH",
            // R81 roster
            "HEN_MOTHER", "BUFFALO_RAGE", "DUMB_RAT", "FIERCE_TIGER",
            "SEXY_DIVA", "TROLL_TOWER", "TWIN_SUMMITS", "VOID_GLOBES",
            "WHITE_DRAGON", "HAMMER_SICKLE", "MONEY_TYCOON", "GOLDEN_TYCOON",
            // Wave 15 batch 1
            "SKULL_CROSSBONES", "VAMPIRE", "COSMIC_CENTIPEDE",
            // Wave 16 batch 2
            "GIANT_CONDOM", "VENOM_SPIDER", "CORRUPTION",
        )
        val actual = BossKind.entries.map { it.name }.toSet()
        assertEquals(
            "Rename detected — DataStore key migration required for renamed entries",
            expected, actual,
        )
    }

    @Test
    fun `ShipSkin full key set is stable (rename detection)`() {
        val expected = setOf(
            "aura_cyan", "aura_gold", "aura_magenta", "aura_violet", "aura_red",
            // Wave 16
            "aura_emerald", "aura_amber", "aura_ice",
        )
        val actual = ShipSkin.entries.map { it.key }.toSet()
        assertEquals(
            "Rename detected — DataStore key migration required for renamed entries",
            expected, actual,
        )
    }

    @Test
    fun `BossRank letter set is stable`() {
        val expected = setOf("S", "A", "B", "C", "D")
        val actual = BossRank.entries.map { it.letter }.toSet()
        assertEquals(expected, actual)
    }
}
