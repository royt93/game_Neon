package com.tranphuloi.neon.ui.game.ship.laser

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.utils.UuidUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 14b audit — INTEGRATION test for the real collision path
 * [LasersController.monitorLaserCollision].
 *
 * The pure-logic [UltimateLaserBossCapTest] pins the `hitBossIds` set + the
 * `bossChipDamage` formula in isolation. This file closes the gap the audit
 * flagged: it drives the ACTUAL collision loop with a live [UltimateLaser]
 * beam against fake enemies and asserts the wired behavior —
 *   • a boss is chipped exactly ONCE no matter how many Millis(1) ticks pass,
 *   • a regular mob still takes the full [UltimateLaser.impactPower] and dies,
 *   • a maxed-out damage multiplier still cannot one-shot the weakest boss,
 *   • two beams of one sweep each chip the same boss independently.
 *
 * Runs as a plain JVM test: `LasersController` only touches pure Compose
 * geometry + `Logger` (no-op under `returnDefaultValues=true`).
 */
class UltimateLaserCollisionTest {

    /** Minimal [Enemy] whose rect is its raw offset/size; takes flat damage. */
    private class FakeEnemy(
        override val enemyId: String,
        override val isBoss: Boolean,
        override var hp: Float,
        override val initialHp: Float = hp,
        override var xOffset: Float = 0f,
        override var yOffset: Float = 0f,
        override val width: Float = 200f,
        override val height: Float = 200f,
    ) : Enemy {
        override val impactPower: Float = 0f
        override val drawableId: Int = 0
        override val minerals: Int = 0
        override val destroyed: Boolean get() = hp <= 0f
        override val outOfScreen: Boolean = false
        override var lastImpactMillis: Long = 0L
        override val displayName: String = "fake"
        override fun enemyRect(): Rect =
            Rect(offset = Offset(xOffset, yOffset), size = Size(width, height))
        override fun process() {}
        override fun generateLasers(): List<Laser> = emptyList()
        override fun onObjectImpact(impactPower: Float) { hp -= impactPower }
    }

    // Beam rect = (50,50)..(80,110) — sits well inside a 200×200 enemy at origin.
    private fun beam(id: String = "beam", x: Float = 50f) =
        UltimateLaser(id = id, xOffset = x, yOffset = 50f, yRange = 100f)

    private fun controller(beams: List<UltimateLaser>, dmgMul: Float) =
        LasersController(
            screenWidth = 1000f,
            screenHeight = 2000f,
            uuidUtils = UuidUtils(),
            initialUltimateLasers = beams,
            setShipLasers = {},
            setUltimateLasers = {},
            onLaserHit = { _, _, _, _, _, _ -> },
            damageMultiplier = { dmgMul },
        )

    @Test
    fun `boss is chipped exactly once across many collision ticks`() {
        val boss = FakeEnemy("boss", isBoss = true, hp = 2000f)
        val controller = controller(listOf(beam()), dmgMul = 1f)
        // Simulate 5 Millis(1) ticks of the beam overlapping the boss.
        repeat(5) { controller.monitorLaserCollision(emptyList(), listOf(boss)) }
        val oneChip = UltimateLaser.bossChipDamage(1f, 2000f) // min(250, 160) = 160
        assertEquals(
            "Boss must lose exactly ONE chip, not one per tick",
            2000f - oneChip, boss.hp, 0.01f,
        )
    }

    @Test
    fun `regular mob takes full impact power and dies in a single tick`() {
        val mob = FakeEnemy("mob", isBoss = false, hp = 100f)
        val controller = controller(listOf(beam()), dmgMul = 1f)
        controller.monitorLaserCollision(emptyList(), listOf(mob))
        // Full impactPower (1000) ≫ mob hp → dead. The cap is boss-only.
        assertTrue("Mob must die to the full-power sweep", mob.hp <= 0f)
    }

    @Test
    fun `a 5x damage run still cannot one-shot the weakest boss`() {
        val boss = FakeEnemy("boss", isBoss = true, hp = 1200f)
        val controller = controller(listOf(beam()), dmgMul = 5f)
        repeat(10) { controller.monitorLaserCollision(emptyList(), listOf(boss)) }
        // chip = min(250×5=1250, 0.08×1200=96) = 96, applied once.
        assertEquals(1200f - 96f, boss.hp, 0.01f)
        assertTrue("weakest boss must survive a high-damage ultimate", boss.hp > 0f)
    }

    @Test
    fun `two beams of one sweep each chip the same boss once`() {
        val boss = FakeEnemy("boss", isBoss = true, hp = 2000f)
        // Second beam offset so its rect also overlaps the boss.
        val controller = controller(listOf(beam("b1", x = 50f), beam("b2", x = 90f)), dmgMul = 1f)
        repeat(3) { controller.monitorLaserCollision(emptyList(), listOf(boss)) }
        val oneChip = UltimateLaser.bossChipDamage(1f, 2000f) // 160 each
        assertEquals(
            "Two independent beams chip once each (not shared, not per-tick)",
            2000f - 2 * oneChip, boss.hp, 0.01f,
        )
    }
}
