package com.tranphuloi.neon.data

import com.tranphuloi.neon.ui.game.controls.BossRank
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 11b — pin RunStats breakdown-map contract.
 *
 * RunStats was extended with bulletKills/bossKills/ranksAchieved/shipSkin/
 * shipTimeMillis fields. Existing call sites use the default-empty values to
 * preserve back-compat. New call sites (GameScreen GAME_OVER) populate them.
 */
class RunStatsWave11bTest {

    @Test
    fun `RunStats minimal construction defaults Wave 11b fields to empty`() {
        // Back-compat: any pre-Wave-11b caller (only positional params for
        // score/timeSec/etc) still constructs a valid RunStats with empty
        // breakdown maps. Persistence flush sees empty → no-op.
        val rs = RunStats(
            score = 1000,
            timeSec = 60,
            enemiesKilled = 30,
            bossesDefeated = 1,
            maxCombo = 12,
            stagesReached = 5,
        )
        assertEquals(emptyMap<BulletType, Int>(), rs.bulletKills)
        assertEquals(emptyMap<BossKind, Int>(), rs.bossKills)
        assertEquals(emptyList<BossRank>(), rs.ranksAchieved)
        assertEquals(ShipSkin.AURA_CYAN, rs.shipSkin)
        assertEquals(0L, rs.shipTimeMillis)
    }

    @Test
    fun `RunStats with populated breakdown carries all telemetry`() {
        val bulletKills = mapOf(
            BulletType.NORMAL to 25,
            BulletType.PIERCING to 5,
            BulletType.PLASMA to 3,
        )
        val bossKills = mapOf(
            BossKind.STAR to 1,
            BossKind.SPIDER to 1,
        )
        val ranks = listOf(BossRank.S, BossRank.A)
        val rs = RunStats(
            score = 5000,
            timeSec = 300,
            enemiesKilled = 33,
            bossesDefeated = 2,
            maxCombo = 25,
            stagesReached = 12,
            victoryAchieved = true,
            bulletKills = bulletKills,
            bossKills = bossKills,
            ranksAchieved = ranks,
            shipSkin = ShipSkin.AURA_GOLD,
            shipTimeMillis = 300_000L,
        )
        assertEquals(bulletKills, rs.bulletKills)
        assertEquals(bossKills, rs.bossKills)
        assertEquals(ranks, rs.ranksAchieved)
        assertEquals(ShipSkin.AURA_GOLD, rs.shipSkin)
        assertEquals(300_000L, rs.shipTimeMillis)
    }

    @Test
    fun `RunStats bossKills sum should equal bossesDefeated in well-formed run`() {
        // Contract: bossesDefeated is the rollup of bossKills. If they
        // diverge, either the boss had no bossKind (regular-boss?) or the
        // wire-up missed a path. This test pins the invariant for well-
        // formed runs (boss with known kind).
        val bossKills = mapOf(
            BossKind.STAR to 1,
            BossKind.CROSS to 1,
            BossKind.SPIDER to 1,
        )
        val rs = RunStats(
            score = 0, timeSec = 0,
            enemiesKilled = 0, bossesDefeated = 3,
            maxCombo = 0, stagesReached = 0,
            bossKills = bossKills,
        )
        val rollup = rs.bossKills.values.sum()
        assertEquals(rs.bossesDefeated, rollup)
    }

    @Test
    fun `RunStats ranksAchieved size never exceeds bossesDefeated`() {
        // Rank is computed only for bosses with non-zero TTK + non-zero HP
        // ratio (see GameState onEnemyKilled). So ranksAchieved ≤ bossesDefeated.
        val rs = RunStats(
            score = 0, timeSec = 0,
            enemiesKilled = 0, bossesDefeated = 2,
            maxCombo = 0, stagesReached = 0,
            ranksAchieved = listOf(BossRank.A, BossRank.S),
        )
        assertTrue("ranks ≤ bossesDefeated",
            rs.ranksAchieved.size <= rs.bossesDefeated)
    }

    @Test
    fun `RunStats bulletKills sum should equal enemiesKilled + bossesDefeated`() {
        // Every kill (regular OR boss) is attributed to whatever bullet
        // type was active. So bulletKills.sum == enemiesKilled + bossesDefeated.
        // This contract is pinned by GameState's onEnemyKilled wiring
        // (single `bulletKillsThisRun[bt] += 1` increment per kill, BEFORE
        // the if (enemy.isBoss) split).
        val bulletKills = mapOf(
            BulletType.NORMAL to 30,
            BulletType.HOMING to 2,
        )
        val rs = RunStats(
            score = 0, timeSec = 0,
            enemiesKilled = 30, bossesDefeated = 2,
            maxCombo = 0, stagesReached = 0,
            bulletKills = bulletKills,
        )
        val total = rs.bulletKills.values.sum()
        assertEquals(rs.enemiesKilled + rs.bossesDefeated, total)
    }

    @Test
    fun `RunStats data class equality includes Wave 11b fields`() {
        // Two RunStats with same base fields but different bulletKills
        // should NOT be equal — necessary for Compose state diff to
        // recompose statistics consumers when only breakdown changes.
        val base = RunStats(
            score = 100, timeSec = 10,
            enemiesKilled = 5, bossesDefeated = 0,
            maxCombo = 1, stagesReached = 1,
        )
        val withBullets = base.copy(
            bulletKills = mapOf(BulletType.PLASMA to 5)
        )
        assertEquals(false, base == withBullets)
    }
}
