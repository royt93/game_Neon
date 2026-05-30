package com.tranphuloi.neon.ui.game.enemy.ship.model

import com.tranphuloi.neon.ui.game.common.Once
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 13c — pins [EnemySize] (size-variety logic) + verifies [RegularEnemy]
 * actually scales hitbox / HP / minerals from `sizeScale`. Pure JVM, no Android.
 */
class EnemySizeTest {

    private fun typeOf(width: Float = 100f, height: Float = 80f, hp: Float = 100f) = RegularEnemyType(
        drawableId = 0,
        width = width,
        height = height,
        hp = hp,
        impactPower = 0f,
        formation = ZigZag(position = ZigZagInitialPosition.LEFT),
        xOffsetSpeed = 4f,
        yOffsetSpeed = 3f,
        enemySpawnRate = Once,
    )

    private fun enemy(scale: Float) = RegularEnemy(
        screenWidth = 400f, screenHeight = 800f, xOffset = 0f, type = typeOf(), sizeScale = scale,
    )

    // ── EnemySize.pick (weighted by roll) ──

    @Test
    fun `pick maps roll bands to small normal large`() {
        assertEquals(EnemySize.SMALL, EnemySize.pick(0.0f))
        assertEquals(EnemySize.SMALL, EnemySize.pick(0.19f))
        assertEquals(EnemySize.NORMAL, EnemySize.pick(0.20f))
        assertEquals(EnemySize.NORMAL, EnemySize.pick(0.84f))
        assertEquals(EnemySize.LARGE, EnemySize.pick(0.85f))
        assertEquals(EnemySize.LARGE, EnemySize.pick(0.999f))
    }

    @Test
    fun `pick only ever returns one of the three scales`() {
        var roll = 0f
        while (roll < 1f) {
            val s = EnemySize.pick(roll)
            assertTrue("scale $s out of set", s == EnemySize.SMALL || s == EnemySize.NORMAL || s == EnemySize.LARGE)
            roll += 0.01f
        }
    }

    // ── factors ──

    @Test
    fun `hpFactor is linear in scale`() {
        assertEquals(0.7f, EnemySize.hpFactor(EnemySize.SMALL), 0.0001f)
        assertEquals(1.0f, EnemySize.hpFactor(EnemySize.NORMAL), 0.0001f)
        assertEquals(1.4f, EnemySize.hpFactor(EnemySize.LARGE), 0.0001f)
    }

    @Test
    fun `speedFactor is inverse of scale (bigger slower) within clamp`() {
        assertEquals(1.0f, EnemySize.speedFactor(EnemySize.NORMAL), 0.0001f)
        assertTrue("small should be faster than normal", EnemySize.speedFactor(EnemySize.SMALL) > 1f)
        assertTrue("large should be slower than normal", EnemySize.speedFactor(EnemySize.LARGE) < 1f)
        // clamp bounds respected for extreme scales.
        assertTrue(EnemySize.speedFactor(0.1f) <= 1.6f)
        assertTrue(EnemySize.speedFactor(10f) >= 0.6f)
    }

    @Test
    fun `mineralReward larger gives more`() {
        assertEquals(1, EnemySize.mineralReward(EnemySize.SMALL))
        assertEquals(1, EnemySize.mineralReward(EnemySize.NORMAL))
        assertEquals(2, EnemySize.mineralReward(EnemySize.LARGE))
    }

    // ── RegularEnemy applies the scale ──

    @Test
    fun `RegularEnemy scales width height and hitbox by sizeScale`() {
        val small = enemy(EnemySize.SMALL)
        val large = enemy(EnemySize.LARGE)
        assertEquals(100f * 0.7f, small.width, 0.001f)
        assertEquals(80f * 0.7f, small.height, 0.001f)
        assertEquals(100f * 1.4f, large.width, 0.001f)
        // hitbox radius follows width → collision honest.
        assertEquals(large.width / 2f, large.enemyRect().width / 2f, 0.001f)
    }

    @Test
    fun `RegularEnemy scales hp and initialHp by sizeScale`() {
        assertEquals(100f * 1.4f, enemy(EnemySize.LARGE).hp, 0.001f)
        assertEquals(100f * 0.7f, enemy(EnemySize.SMALL).hp, 0.001f)
        assertEquals(enemy(EnemySize.LARGE).hp, enemy(EnemySize.LARGE).initialHp, 0.001f)
    }

    @Test
    fun `RegularEnemy large drops more minerals`() {
        assertEquals(2, enemy(EnemySize.LARGE).minerals)
        assertEquals(1, enemy(EnemySize.NORMAL).minerals)
    }

    @Test
    fun `default sizeScale is baseline (no behaviour change for unspecified)`() {
        val e = RegularEnemy(screenWidth = 400f, screenHeight = 800f, xOffset = 0f, type = typeOf())
        assertEquals(100f, e.width, 0.001f)
        assertEquals(100f, e.hp, 0.001f)
        assertEquals(1, e.minerals)
    }
}
