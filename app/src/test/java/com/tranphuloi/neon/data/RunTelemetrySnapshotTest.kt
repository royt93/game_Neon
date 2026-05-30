package com.tranphuloi.neon.data

import com.tranphuloi.neon.ui.game.controls.BossRank
import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Wave 11c — pin RunTelemetrySnapshot contract.
 *
 * Snapshot is the atomic point-in-time view shipped from GameState to GameScreen
 * at GAME_OVER. Both consumers (RunStats / recordRunMetrics) read the same
 * instance, so equality + immutability invariants must hold.
 */
class RunTelemetrySnapshotTest {

    @Test
    fun `EMPTY constant has empty maps and zero time`() {
        val e = RunTelemetrySnapshot.EMPTY
        assertEquals(emptyMap<BulletType, Int>(), e.bulletKills)
        assertEquals(emptyMap<BossKind, Int>(), e.bossKills)
        assertEquals(emptyList<BossRank>(), e.ranksAchieved)
        assertEquals(ShipSkin.AURA_CYAN, e.shipSkin)
        assertEquals(0L, e.shipTimeMillis)
    }

    @Test
    fun `EMPTY constant is shared instance (no per-access allocation)`() {
        // Pre-Wave-11c the data-class fields were re-created every recomposition;
        // EMPTY exists specifically to avoid that. Two reads must return SAME ref.
        val a = RunTelemetrySnapshot.EMPTY
        val b = RunTelemetrySnapshot.EMPTY
        assertSame("EMPTY should be a shared singleton", a, b)
    }

    @Test
    fun `data class equality holds for identical content`() {
        val a = RunTelemetrySnapshot(
            bulletKills = mapOf(BulletType.PLASMA to 5),
            bossKills = mapOf(BossKind.STAR to 1),
            ranksAchieved = listOf(BossRank.A),
            shipSkin = ShipSkin.AURA_GOLD,
            shipTimeMillis = 60_000L,
        )
        val b = RunTelemetrySnapshot(
            bulletKills = mapOf(BulletType.PLASMA to 5),
            bossKills = mapOf(BossKind.STAR to 1),
            ranksAchieved = listOf(BossRank.A),
            shipSkin = ShipSkin.AURA_GOLD,
            shipTimeMillis = 60_000L,
        )
        assertEquals(a, b)
    }

    @Test
    fun `inequality fires when any field differs`() {
        val base = RunTelemetrySnapshot(
            bulletKills = mapOf(BulletType.PLASMA to 5),
            bossKills = emptyMap(),
            ranksAchieved = emptyList(),
            shipSkin = ShipSkin.AURA_CYAN,
            shipTimeMillis = 1000L,
        )
        // Different bullet count
        assertNotEquals(base, base.copy(bulletKills = mapOf(BulletType.PLASMA to 6)))
        // Different boss kills
        assertNotEquals(base, base.copy(bossKills = mapOf(BossKind.STAR to 1)))
        // Different ranks
        assertNotEquals(base, base.copy(ranksAchieved = listOf(BossRank.S)))
        // Different skin
        assertNotEquals(base, base.copy(shipSkin = ShipSkin.AURA_GOLD))
        // Different time
        assertNotEquals(base, base.copy(shipTimeMillis = 2000L))
    }

    @Test
    fun `snapshot bulletKills total matches input`() {
        // Sanity invariant for the Statistics-screen consumer: sum reads back.
        val kills = mapOf(
            BulletType.NORMAL to 25,
            BulletType.PLASMA to 10,
            BulletType.HOMING to 5,
        )
        val snap = RunTelemetrySnapshot(
            bulletKills = kills,
            bossKills = emptyMap(),
            ranksAchieved = emptyList(),
            shipSkin = ShipSkin.AURA_CYAN,
            shipTimeMillis = 0L,
        )
        assertEquals(40, snap.bulletKills.values.sum())
    }
}
