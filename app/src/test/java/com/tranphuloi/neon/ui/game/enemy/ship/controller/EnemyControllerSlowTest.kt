package com.tranphuloi.neon.ui.game.enemy.ship.controller

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.utils.UuidUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 16 (Slice 4b) — verify the SLOW status now actually slows enemy
 * movement (DURIAN bullet → StatusEffect.SLOW → EnemyController skips that
 * enemy's `process()` on alternate ticks). Before this it was a no-op:
 * `StatusEffectController.isSlowed` existed but nothing read it.
 */
class EnemyControllerSlowTest {

    /** Enemy that counts how many times it was actually advanced (process()). */
    private class CountingEnemy : Enemy {
        var processCount = 0
        override val enemyId: String = "counter"
        override val width: Float = 10f
        override val height: Float = 10f
        override var xOffset: Float = 100f
        override var yOffset: Float = 10f
        override var hp: Float = 100f
        override val initialHp: Float = 100f
        override val impactPower: Float = 0f
        override val drawableId: Int = 0
        override val minerals: Int = 0
        override val destroyed: Boolean = false
        override val outOfScreen: Boolean = false
        override var lastImpactMillis: Long = 0L
        override val isBoss: Boolean = false
        override val displayName: String = "fake"
        override fun enemyRect(): Rect = Rect(Offset(xOffset, yOffset), Size(width, height))
        override fun process() { processCount++ }
        override fun generateLasers(): List<Laser> = emptyList()
        override fun onObjectImpact(impactPower: Float) {}
    }

    private fun controller(enemy: Enemy, slowed: Boolean) = EnemyController(
        screenWidth = 400f,
        screenHeight = 800f,
        uuidUtils = UuidUtils(),
        getShip = { Ship(xOffset = 200f, yOffset = 700f) },
        initialEnemies = listOf(enemy),
        setEnemies = {},
        addMinerals = { _, _, _, _ -> },
        addExplosion = { _, _, _, _ -> },
        isSlowed = { slowed },
    )

    @Test
    fun `slowed enemy advances on roughly half the ticks`() {
        val slow = CountingEnemy()
        val ctl = controller(slow, slowed = true)
        repeat(6) { ctl.processEnemies() }
        // ticks 1,3,5 advance; 2,4,6 skipped → 3 of 6.
        assertEquals("slowed enemy should move ~half the ticks", 3, slow.processCount)
    }

    @Test
    fun `un-slowed enemy advances every tick`() {
        val normal = CountingEnemy()
        val ctl = controller(normal, slowed = false)
        repeat(6) { ctl.processEnemies() }
        assertEquals("normal enemy moves every tick", 6, normal.processCount)
    }

    @Test
    fun `SLOW genuinely reduces movement vs normal`() {
        val s = CountingEnemy()
        val n = CountingEnemy()
        val sc = controller(s, slowed = true)
        val nc = controller(n, slowed = false)
        repeat(8) { sc.processEnemies(); nc.processEnemies() }
        assertTrue(
            "slowed (${s.processCount}) must move less than normal (${n.processCount})",
            s.processCount < n.processCount,
        )
    }
}
