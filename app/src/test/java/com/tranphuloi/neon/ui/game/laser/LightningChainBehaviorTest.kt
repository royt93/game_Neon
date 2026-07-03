package com.tranphuloi.neon.ui.game.laser

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 02 (Slice 1) — hành vi [LightningChain.computeChainTargets]: nhảy tuần tự
 * gần-nhất-chưa-thăm trong bán kính, visited-set tránh lặp, cap maxSteps, bỏ địch
 * chết; + falloff sát thương. JUnit4, no-mock (đúng convention).
 */
class LightningChainBehaviorTest {

    @Test
    fun `empty enemy list yields no chain`() {
        val hops = LightningChain.computeChainTargets(0f, 0f, emptyList())
        assertTrue("không địch → không chain", hops.isEmpty())
    }

    @Test
    fun `chains sequentially to the nearest unvisited enemy each hop`() {
        // Bố trí thẳng hàng, mỗi con cách 30px (tâm), đều trong radius 120 giữa các bước.
        val a = fakeEnemy("a", 100f, 0f)
        val b = fakeEnemy("b", 130f, 0f)
        val c = fakeEnemy("c", 160f, 0f)
        val hops = LightningChain.computeChainTargets(
            startX = 100f + 30f, startY = 30f, // gần a nhất (tâm a = 130,30)
            enemies = listOf(c, b, a), // xáo thứ tự để chắc chắn sort theo khoảng cách
        )
        assertEquals("chain đúng thứ tự a→b→c", listOf("a", "b", "c"), hops.map { it.enemyId })
    }

    @Test
    fun `respects maxSteps cap`() {
        val list = (0 until 6).map { fakeEnemy("e$it", 100f + it * 25f, 0f) }
        val hops = LightningChain.computeChainTargets(100f, 30f, list, maxSteps = 3)
        assertEquals("tối đa 3 hop", 3, hops.size)
    }

    @Test
    fun `does not revisit an enemy (no loop)`() {
        val a = fakeEnemy("a", 100f, 0f)
        val b = fakeEnemy("b", 130f, 0f)
        val hops = LightningChain.computeChainTargets(115f, 30f, listOf(a, b), maxSteps = 5)
        assertEquals("2 địch → tối đa 2 hop dù maxSteps=5", 2, hops.size)
        assertEquals("mỗi địch 1 lần", hops.size, hops.map { it.enemyId }.toSet().size)
    }

    @Test
    fun `excludeIds are never chained`() {
        val a = fakeEnemy("a", 100f, 0f)
        val b = fakeEnemy("b", 130f, 0f)
        val hops = LightningChain.computeChainTargets(
            115f, 30f, listOf(a, b), excludeIds = setOf("a"),
        )
        assertEquals("bỏ 'a' (đòn chính) → chỉ chain 'b'", listOf("b"), hops.map { it.enemyId })
    }

    @Test
    fun `enemies outside the radius are not chained`() {
        val near = fakeEnemy("near", 100f, 0f)  // tâm (130,0)
        val far = fakeEnemy("far", 1000f, 0f)   // ngoài 120px
        val hops = LightningChain.computeChainTargets(130f, 0f, listOf(near, far))
        assertEquals("chỉ chain địch trong bán kính", listOf("near"), hops.map { it.enemyId })
    }

    @Test
    fun `dead enemies are skipped`() {
        val dead = fakeEnemy("dead", 100f, 0f, destroyed = true)
        val alive = fakeEnemy("alive", 130f, 0f)
        val hops = LightningChain.computeChainTargets(130f, 0f, listOf(dead, alive))
        assertEquals("bỏ địch chết", listOf("alive"), hops.map { it.enemyId })
    }

    @Test
    fun `damageForHop applies compounding falloff`() {
        // base 100, falloff 0.7 → hop0 70, hop1 49, hop2 34.
        assertEquals(70, LightningChain.damageForHop(100, 0))
        assertEquals(49, LightningChain.damageForHop(100, 1))
        assertEquals(34, LightningChain.damageForHop(100, 2))
    }

    @Test
    fun `damageForHop floors at one`() {
        assertEquals("không hop 0-damage", 1, LightningChain.damageForHop(1, 5))
    }

    // ── fake Enemy tối giản ──
    private fun fakeEnemy(id: String, x: Float, y: Float, destroyed: Boolean = false): Enemy =
        object : Enemy {
            override val enemyId = id
            override val width = 60f
            override val height = 60f
            override var xOffset = x
            override var yOffset = y
            override var hp = if (destroyed) 0f else 100f
            override val initialHp = 100f
            override val impactPower = 10f
            override val drawableId = 0
            override val minerals = 1
            override val destroyed = destroyed
            override val outOfScreen = false
            override var lastImpactMillis = 0L
            override val isBoss = false
            override val displayName = "fake"
            override fun enemyRect() =
                androidx.compose.ui.geometry.Rect(xOffset, yOffset, xOffset + width, yOffset + height)
            override fun process() {}
            override fun generateLasers(): List<com.tranphuloi.neon.ui.game.laser.Laser> = emptyList()
            override fun onObjectImpact(impactPower: Float) {}
        }
}
