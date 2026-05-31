package com.tranphuloi.neon.ui.game.story

import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 85 audit — verifies story speaker name matches the chapter-resolved
 * BossKind.displayName (eliminate the Ch4 OFFENSIVE → "TIỂU BOSS TẤN CÔNG"
 * mismatch where banner shows "Chúa Tể Địa Ngục" instead).
 */
class StoryRegistryBossTauntTest {

    @Test
    fun `Ch3 LevelOneBoss taunt speaker matches Death Moon`() {
        val taunt = StoryRegistry.bossTaunt(LevelOneBossType, chapterId = 3)
        assertNotNull(taunt)
        assertEquals(BossKind.DEATH_MOON.displayName, taunt!!.speaker)
    }

    @Test
    fun `Ch1 LevelOneBoss taunt speaker matches Crimson Sun (STAR)`() {
        val taunt = StoryRegistry.bossTaunt(LevelOneBossType, chapterId = 1)
        assertNotNull(taunt)
        assertEquals(BossKind.STAR.displayName, taunt!!.speaker)
    }

    @Test
    fun `Ch4 LevelTwoBoss taunt speaker matches Satan Glyph`() {
        val taunt = StoryRegistry.bossTaunt(LevelTwoBossType, chapterId = 4)
        assertNotNull(taunt)
        assertEquals(BossKind.SATAN_GLYPH.displayName, taunt!!.speaker)
    }

    @Test
    fun `Ch2 LevelTwoBoss taunt speaker matches Cross`() {
        val taunt = StoryRegistry.bossTaunt(LevelTwoBossType, chapterId = 2)
        assertNotNull(taunt)
        assertEquals(BossKind.CROSS.displayName, taunt!!.speaker)
    }

    @Test
    fun `Ch4 MidBoss OFFENSIVE taunt speaker matches Hell Lord (not generic)`() {
        val taunt = StoryRegistry.bossTaunt(MidBossType.OFFENSIVE, chapterId = 4)
        assertNotNull(taunt)
        // Was "TIỂU BOSS TẤN CÔNG" generic before. R85 → "Chúa Tể Địa Ngục".
        assertEquals(BossKind.HELL_LORD.displayName, taunt!!.speaker)
    }

    @Test
    fun `Ch1 MidBoss OFFENSIVE taunt speaker matches Eye (ORB default)`() {
        val taunt = StoryRegistry.bossTaunt(MidBossType.OFFENSIVE, chapterId = 1)
        assertNotNull(taunt)
        assertEquals(BossKind.ORB.displayName, taunt!!.speaker)
    }

    @Test
    fun `Ch3 MidBoss SWARM taunt speaker matches Haunted Kid`() {
        val taunt = StoryRegistry.bossTaunt(MidBossType.SWARM, chapterId = 3)
        assertNotNull(taunt)
        assertEquals(BossKind.HAUNTED_KID.displayName, taunt!!.speaker)
    }

    @Test
    fun `R81 MidBoss variants taunt speakers match their default BossKind`() {
        // 12 R81 variants self-describe via variant.defaultBossKind → straightforward.
        val pairs = listOf(
            MidBossType.HEN_MOTHER to BossKind.HEN_MOTHER,
            MidBossType.BUFFALO_RAGE to BossKind.BUFFALO_RAGE,
            MidBossType.DUMB_RAT to BossKind.DUMB_RAT,
            MidBossType.FIERCE_TIGER to BossKind.FIERCE_TIGER,
            MidBossType.SEXY_DIVA to BossKind.SEXY_DIVA,
            MidBossType.TROLL_TOWER to BossKind.TROLL_TOWER,
            MidBossType.TWIN_SUMMITS to BossKind.TWIN_SUMMITS,
            MidBossType.VOID_GLOBES to BossKind.VOID_GLOBES,
            MidBossType.WHITE_DRAGON to BossKind.WHITE_DRAGON,
            MidBossType.HAMMER_SICKLE to BossKind.HAMMER_SICKLE,
            MidBossType.MONEY_TYCOON to BossKind.MONEY_TYCOON,
            MidBossType.GOLDEN_TYCOON to BossKind.GOLDEN_TYCOON,
            // Wave 15 batch 1
            MidBossType.SKULL_CROSSBONES to BossKind.SKULL_CROSSBONES,
            MidBossType.VAMPIRE to BossKind.VAMPIRE,
            MidBossType.COSMIC_CENTIPEDE to BossKind.COSMIC_CENTIPEDE,
            // Wave 16 batch 2
            MidBossType.GIANT_CONDOM to BossKind.GIANT_CONDOM,
            MidBossType.VENOM_SPIDER to BossKind.VENOM_SPIDER,
            MidBossType.CORRUPTION to BossKind.CORRUPTION,
        )
        for ((variant, expectedKind) in pairs) {
            val taunt = StoryRegistry.bossTaunt(variant, chapterId = 5)
            assertEquals(
                "Variant $variant should map to ${expectedKind.displayName}",
                expectedKind.displayName, taunt!!.speaker,
            )
        }
    }

    @Test
    fun `FinalBoss taunt speaker matches Galaxy Overlord`() {
        val taunt = StoryRegistry.bossTaunt(FinalBossType, chapterId = 5)
        assertNotNull(taunt)
        assertEquals(BossKind.SPIDER.displayName, taunt!!.speaker)
    }

    @Test
    fun `all boss taunts have non-blank text`() {
        for (chapterId in 1..5) {
            for (type in listOf(LevelOneBossType, LevelTwoBossType, FinalBossType,
                MidBossType.OFFENSIVE, MidBossType.DEFENSIVE, MidBossType.SWARM,
                MidBossType.HEN_MOTHER, MidBossType.BUFFALO_RAGE)) {
                val taunt = StoryRegistry.bossTaunt(type, chapterId)
                if (taunt != null) {
                    assertTrue(
                        "taunt text blank for $type ch=$chapterId",
                        taunt.text.isNotBlank(),
                    )
                }
            }
        }
    }

    @Test
    fun `no taunt has dev-jargon speaker (LEVEL or TIỂU BOSS)`() {
        for (chapterId in 1..5) {
            for (type in listOf(LevelOneBossType, LevelTwoBossType, FinalBossType,
                MidBossType.OFFENSIVE, MidBossType.DEFENSIVE, MidBossType.SWARM)) {
                val taunt = StoryRegistry.bossTaunt(type, chapterId) ?: continue
                assertTrue(
                    "Taunt speaker has dev-jargon: '${taunt.speaker}' for $type ch=$chapterId",
                    !taunt.speaker.contains("BOSS CẤP") &&
                        !taunt.speaker.contains("TIỂU BOSS") &&
                        !taunt.speaker.contains("LEVEL"),
                )
            }
        }
    }
}
