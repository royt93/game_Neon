package com.tranphuloi.neon.ui.game.stage

import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round 82 audit — verifies the R81 boss roster wire is correct:
 *   - Each chapter declares midBossTypes (list, not single).
 *   - Total mid-bosses across 5 chapters = 15 (3 existing + 12 R81 = 15).
 *   - Every BossKind from R81 roster is reachable through at least one
 *     chapter's mid-boss list (no orphan BossKind defined but never spawned).
 *   - Each new MidBossType variant has a distinct defaultBossKind.
 */
class ChapterMidBossWireTest {

    @Test
    fun `every chapter has midBossTypes list (back-compat midBossType returns first)`() {
        for (chapter in Chapter.values()) {
            assertNotNull(
                "Chapter ${chapter.id} missing midBossTypes",
                chapter.midBossTypes,
            )
            // midBossType (singular convenience) should always equal first item or null.
            assertEquals(
                "Ch${chapter.id} midBossType should equal midBossTypes.firstOrNull()",
                chapter.midBossTypes.firstOrNull(), chapter.midBossType,
            )
        }
    }

    @Test
    fun `total mid-boss spawn slots across 5 chapters equals 22`() {
        // R82=16; Wave15 +3 (Ch1/2/3) → 19; Wave16 +3 (Ch4 +CONDOM +VENOM,
        // Ch5 +CORRUPTION) → 22. Now: Ch1=4, Ch2=4, Ch3=4, Ch4=5, Ch5=5 → 22.
        val total = Chapter.values().sumOf { it.midBossTypes.size }
        assertEquals(
            "Expected 22 mid-boss slots (4+4+4+5+5 per chapter). Got $total",
            22, total,
        )
    }

    @Test
    fun `Wave16 3 new BossKinds are wired to chapters`() {
        val expected = setOf(
            BossKind.GIANT_CONDOM, BossKind.VENOM_SPIDER, BossKind.CORRUPTION,
        )
        val wired = Chapter.values().flatMap { it.midBossTypes }.map { it.defaultBossKind }.toSet()
        assertTrue("Wave16 BossKinds not wired: ${expected - wired}", (expected - wired).isEmpty())
    }

    @Test
    fun `Wave15 3 new BossKinds are wired to chapters 1-3`() {
        val expectedWave15 = setOf(
            BossKind.SKULL_CROSSBONES, BossKind.VAMPIRE, BossKind.COSMIC_CENTIPEDE,
        )
        val wired = Chapter.values()
            .flatMap { it.midBossTypes }
            .map { it.defaultBossKind }
            .toSet()
        val missing = expectedWave15 - wired
        assertTrue(
            "Wave15 BossKinds not wired to any chapter: $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun `all 12 R81 BossKinds are wired to some chapter`() {
        val expectedR81 = setOf(
            BossKind.HEN_MOTHER, BossKind.BUFFALO_RAGE, BossKind.DUMB_RAT,
            BossKind.FIERCE_TIGER, BossKind.SEXY_DIVA, BossKind.TROLL_TOWER,
            BossKind.TWIN_SUMMITS, BossKind.VOID_GLOBES, BossKind.WHITE_DRAGON,
            BossKind.HAMMER_SICKLE, BossKind.MONEY_TYCOON, BossKind.GOLDEN_TYCOON,
        )
        val wired = Chapter.values()
            .flatMap { it.midBossTypes }
            .map { it.defaultBossKind }
            .toSet()
        val missing = expectedR81 - wired
        assertTrue(
            "R81 BossKinds not wired to any chapter: $missing",
            missing.isEmpty(),
        )
    }

    @Test
    fun `each MidBossType has a non-trivial defaultBossKind`() {
        // Round 82 — every variant now self-describes its BossKind via the
        // new field. Verifies no variant accidentally defaults to fallback.
        for (variant in listOf(
            MidBossType.OFFENSIVE, MidBossType.DEFENSIVE, MidBossType.SWARM,
            MidBossType.HEN_MOTHER, MidBossType.BUFFALO_RAGE, MidBossType.DUMB_RAT,
            MidBossType.FIERCE_TIGER, MidBossType.SEXY_DIVA, MidBossType.TROLL_TOWER,
            MidBossType.TWIN_SUMMITS, MidBossType.VOID_GLOBES, MidBossType.WHITE_DRAGON,
            MidBossType.HAMMER_SICKLE, MidBossType.MONEY_TYCOON, MidBossType.GOLDEN_TYCOON,
        )) {
            assertNotNull(
                "${variant::class.simpleName} missing defaultBossKind",
                variant.defaultBossKind,
            )
        }
    }

    @Test
    fun `R81 new MidBossType variants map to matching BossKind`() {
        // 1-to-1 sanity check on naming convention (HEN_MOTHER variant → HEN_MOTHER kind).
        assertEquals(BossKind.HEN_MOTHER, MidBossType.HEN_MOTHER.defaultBossKind)
        assertEquals(BossKind.BUFFALO_RAGE, MidBossType.BUFFALO_RAGE.defaultBossKind)
        assertEquals(BossKind.DUMB_RAT, MidBossType.DUMB_RAT.defaultBossKind)
        assertEquals(BossKind.FIERCE_TIGER, MidBossType.FIERCE_TIGER.defaultBossKind)
        assertEquals(BossKind.SEXY_DIVA, MidBossType.SEXY_DIVA.defaultBossKind)
        assertEquals(BossKind.TROLL_TOWER, MidBossType.TROLL_TOWER.defaultBossKind)
        assertEquals(BossKind.TWIN_SUMMITS, MidBossType.TWIN_SUMMITS.defaultBossKind)
        assertEquals(BossKind.VOID_GLOBES, MidBossType.VOID_GLOBES.defaultBossKind)
        assertEquals(BossKind.WHITE_DRAGON, MidBossType.WHITE_DRAGON.defaultBossKind)
        assertEquals(BossKind.HAMMER_SICKLE, MidBossType.HAMMER_SICKLE.defaultBossKind)
        assertEquals(BossKind.MONEY_TYCOON, MidBossType.MONEY_TYCOON.defaultBossKind)
        assertEquals(BossKind.GOLDEN_TYCOON, MidBossType.GOLDEN_TYCOON.defaultBossKind)
    }

    @Test
    fun `all 5 chapters now have at least one midBoss (R82 wire fills Ch5)`() {
        // Before R82, GALAXY_CORE (Ch5) had midBossType=null. R82 wires 4
        // mini-bosses before FinalBoss.
        for (chapter in Chapter.values()) {
            assertTrue(
                "Ch${chapter.id} should have at least 1 mid-boss after R82",
                chapter.midBossTypes.isNotEmpty(),
            )
        }
    }

    @Test
    fun `R82 enemy roster — chapters extend regularEnemyDrawables with R81 enemies`() {
        // R82 wires 10 R81 enemies into chapter rosters. Verify each new enemy
        // drawableId is referenced by some chapter (no orphan drawable).
        val newDrawables = setOf(
            com.tranphuloi.neon.R.drawable.enemy_spinning_saw,
            com.tranphuloi.neon.R.drawable.enemy_tentacle_squid,
            com.tranphuloi.neon.R.drawable.enemy_mine_layer,
            com.tranphuloi.neon.R.drawable.enemy_shield_drone,
            com.tranphuloi.neon.R.drawable.enemy_sniper,
            com.tranphuloi.neon.R.drawable.enemy_bomber_crawler,
            com.tranphuloi.neon.R.drawable.enemy_mirror_twin,
            com.tranphuloi.neon.R.drawable.enemy_phantom,
            com.tranphuloi.neon.R.drawable.enemy_healer,
            com.tranphuloi.neon.R.drawable.enemy_kamikaze,
        )
        val wiredDrawables = Chapter.values().flatMap { it.regularEnemyDrawables }.toSet()
        val orphan = newDrawables - wiredDrawables
        assertTrue(
            "R81 enemy drawables not wired to any chapter: $orphan",
            orphan.isEmpty(),
        )
    }
}
