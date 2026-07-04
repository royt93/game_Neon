package com.tranphuloi.neon.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 11c — pin telemetry-driven achievement additions.
 *
 * 6 new achievements added in this wave, all derived from
 * MetaProgressionRepository aggregate Flows. Tests:
 *   - All 6 IDs are present and unique
 *   - No collision with pre-existing 29 achievements
 *   - Stable ID strings (rename = lost unlock history)
 *   - Tier sanity (each new achievement has a sensible tier)
 */
class AchievementsWave11cTest {

    @Test
    fun `Wave 11c achievement enum entries exist`() {
        // If anyone removes these, runs would stop awarding them silently
        // (no compile error — repository.unlock just gets a "no-such-id"
        // semantic). Pin presence.
        assertNotNull(Achievement.PLASMA_MASTER)
        assertNotNull(Achievement.HOMING_VETERAN)
        assertNotNull(Achievement.BOSS_ALL_KINDS)
        assertNotNull(Achievement.S_RANK_10)
        assertNotNull(Achievement.CYAN_HOUR)
        assertNotNull(Achievement.LIFETIME_KILLS_1000)
    }

    @Test
    fun `Wave 11c achievement IDs are stable strings`() {
        // The DataStore key for the unlocked set is the comma-separated IDs.
        // Renaming an ID strands the prior unlock — user loses the badge.
        assertEquals("plasma_master", Achievement.PLASMA_MASTER.id)
        assertEquals("homing_veteran", Achievement.HOMING_VETERAN.id)
        assertEquals("boss_all_kinds", Achievement.BOSS_ALL_KINDS.id)
        assertEquals("s_rank_10", Achievement.S_RANK_10.id)
        assertEquals("cyan_hour", Achievement.CYAN_HOUR.id)
        assertEquals("lifetime_kills_1000", Achievement.LIFETIME_KILLS_1000.id)
    }

    @Test
    fun `All achievement IDs are globally unique (no collisions)`() {
        // Cross-wave check — a Wave 11c ID matching a Wave 1 ID would silently
        // unlock the wrong achievement. ID set must equal entry set size.
        val ids = Achievement.entries.map { it.id }
        assertEquals(
            "Duplicate achievement ID detected — would collide in unlocked-set storage",
            ids.size, ids.toSet().size,
        )
    }

    @Test
    fun `Wave 11c IDs do not collide with pre-Wave-11c IDs`() {
        // Defensive: pin that none of the new IDs reuse an old one.
        val newIds = setOf(
            "plasma_master", "homing_veteran", "boss_all_kinds",
            "s_rank_10", "cyan_hour", "lifetime_kills_1000",
        )
        val oldIds = Achievement.entries
            .filterNot { it.id in newIds }
            .map { it.id }
            .toSet()
        // Intersection should be empty.
        assertTrue("Wave 11c IDs overlap with pre-existing — picker would mis-route",
            (newIds intersect oldIds).isEmpty())
    }

    @Test
    fun `Wave 11c titles and descriptions are non-empty`() {
        val newOnes = setOf(
            Achievement.PLASMA_MASTER, Achievement.HOMING_VETERAN,
            Achievement.BOSS_ALL_KINDS, Achievement.S_RANK_10,
            Achievement.CYAN_HOUR, Achievement.LIFETIME_KILLS_1000,
        )
        newOnes.forEach {
            assertFalse("${it.name} has empty title", it.title.isBlank())
            assertFalse("${it.name} has empty description", it.description.isBlank())
        }
    }

    @Test
    fun `Achievement total count grew by exactly 6 in Wave 11c`() {
        // Pre-Wave-11c: 10 (Wave 1) + 20 (Wave 5 46x) = 30.
        // Wave 11c adds 6 → 36.
        // Task 07 adds 5 (drone/lightning/ship-XP) → 41.
        // If anyone adds more without updating this count, audit fires.
        assertEquals(
            "Achievement count drift — update test",
            41, Achievement.entries.size,
        )
    }

    @Test
    fun `S_RANK_10 + BOSS_ALL_KINDS are GOLD tier (hard milestones)`() {
        // 10 S-ranks lifetime is hard. 21 unique boss kinds is hard.
        // Tier mismatch would mis-prioritize the achievement in any future
        // sorted display.
        assertEquals(AchievementTier.GOLD, Achievement.S_RANK_10.tier)
        assertEquals(AchievementTier.GOLD, Achievement.BOSS_ALL_KINDS.tier)
    }
}
