package com.tranphuloi.neon.ui.game.stage

import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 16 — pin that Boss Rush now includes the FULL boss roster.
 *
 * User report: "boss chưa đầy đủ ở chế độ chiến boss". Root cause: the old
 * roster was `stages.filterIsInstance<StageBoss>()` = only the ~2-3 chapter
 * finals; the 21 mid-boss variants (spawned via Chapter.midBossTypes during
 * campaign, NOT as StageBoss) never appeared. Now the provider builds the
 * roster directly from [MidBossType.ALL] + chapter finals.
 */
class BossRushRosterTest {

    private val provider = BossRushProvider()

    private fun bossEnemyTypes(): List<Any> =
        (0 until provider.size())
            .map { provider.getAt(it) }
            .filterIsInstance<StageBoss>()
            .map { it.enemyType }

    @Test
    fun `every mid-boss variant appears in Boss Rush`() {
        val rosterTypes = bossEnemyTypes().toSet()
        val missing = MidBossType.ALL.filter { it !in rosterTypes }
        assertTrue("mid-boss variants missing from Boss Rush: $missing", missing.isEmpty())
    }

    @Test
    fun `Boss Rush has at least 24 boss stages (was ~3 before)`() {
        val bossCount = bossEnemyTypes().size
        assertTrue("expected the full roster (≥24), got $bossCount", bossCount >= 24)
    }

    @Test
    fun `mode-picker boss count label matches the real roster (no stale ~9)`() {
        // Nhãn DialogModePicker dùng bossRushRosterSize; phải khớp số StageBoss
        // thật và KHÔNG còn là ~9 (hardcode cũ sai mà user bắt được).
        assertEquals(bossEnemyTypes().size, bossRushRosterSize)
        assertTrue("label phải > 9 boss, got $bossRushRosterSize", bossRushRosterSize > 9)
    }
}
