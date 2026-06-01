package com.tranphuloi.neon.ui.game.ship.laser

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
 * Wave 16 (Bullet Slice 3) — implement the 3 previously-stubbed player bullets:
 *   • ZIGZAG — sine-weaving path ([ZigZagShipLaser])
 *   • SMOKE  — slow fat puff with a small AoE on hit ([SmokeShipLaser])
 *   • SPLIT  — bursts into 3 NORMAL children on hit (collision arm)
 *
 * Before this they all fell back to a plain straight ShipLaser (a bought weapon
 * that behaved identically to NORMAL). These tests pin each real behaviour.
 */
class BulletSlice3Test {

    // ── ZIGZAG ──

    @Test
    fun `ZIGZAG weaves left-right around its spawn x while rising`() {
        val z = ZigZagShipLaser(id = "z", xOffset = 100f, yOffset = 500f, yRange = 800f)
        val startY = z.yOffset
        val xs = (1..60).map { z.moveLaser(); z.xOffset }
        assertTrue("must rise (yOffset decreases)", z.yOffset < startY)
        assertTrue("must actually weave away from centre", xs.any { kotlin.math.abs(it - 100f) > 15f })
        assertTrue("must stay within its amplitude", xs.all { kotlin.math.abs(it - 100f) <= 36f })
        assertTrue("must weave to BOTH sides", xs.any { it > 100f } && xs.any { it < 100f })
        assertEquals(BulletType.ZIGZAG, z.bulletType)
    }

    // ── SMOKE ──

    @Test
    fun `SMOKE is slower and fatter than a normal shot`() {
        val smoke = SmokeShipLaser(id = "s", xOffset = 0f, yOffset = 0f, yRange = 800f)
        val normal = ShipLaser(id = "n", xOffset = 0f, yOffset = 0f, yRange = 800f)
        assertTrue("smoke drifts slowly", smoke.yOffsetMovementSpeed < normal.yOffsetMovementSpeed)
        assertTrue("smoke is fatter", smoke.width > normal.width)
        assertEquals(BulletType.SMOKE, smoke.bulletType)
        // The splash radius the collision arm reads comes from the enum.
        assertTrue("smoke carries an AoE radius", BulletType.SMOKE.aoeRadius > 0f)
    }

    // ── SPLIT (collision integration) ──

    private class FakeEnemy(
        override val enemyId: String = "e",
        override val isBoss: Boolean = false,
        override var hp: Float = 100f,
        override var xOffset: Float = 0f,
        override var yOffset: Float = 0f,
        override val width: Float = 200f,
        override val height: Float = 200f,
    ) : Enemy {
        override val initialHp: Float = hp
        override val impactPower: Float = 0f
        override val drawableId: Int = 0
        override val minerals: Int = 0
        override val destroyed: Boolean get() = hp <= 0f
        override val outOfScreen: Boolean = false
        override var lastImpactMillis: Long = 0L
        override val displayName: String = "fake"
        override fun enemyRect(): Rect = Rect(Offset(xOffset, yOffset), Size(width, height))
        override fun process() {}
        override fun generateLasers(): List<Laser> = emptyList()
        override fun onObjectImpact(impactPower: Float) { hp -= impactPower }
    }

    @Test
    fun `SPLIT bursts into 3 NORMAL children on hit`() {
        var captured: List<Laser> = emptyList()
        val split = ShipLaser(
            id = "parent", xOffset = 50f, yOffset = 50f, yRange = 800f,
            bulletType = BulletType.SPLIT,
        )
        val controller = LasersController(
            screenWidth = 400f,
            screenHeight = 800f,
            uuidUtils = UuidUtils(),
            initialShipLasers = listOf(split),
            setShipLasers = { captured = it },
            setUltimateLasers = {},
            onLaserHit = { _, _, _, _, _, _ -> },
            damageMultiplier = { 1f },
        )
        controller.monitorLaserCollision(emptyList(), listOf(FakeEnemy()))
        // Parent removed, replaced by exactly 3 children.
        assertEquals("split must yield 3 children", 3, captured.size)
        assertTrue("children must be NORMAL (no recursive split)", captured.all { it.bulletType == BulletType.NORMAL })
        // Children fan out around the parent's x (50): left / centre / right.
        val xs = captured.map { it.xOffset }.sorted()
        assertTrue("children spread horizontally", xs.first() < 50f && xs.last() > 50f)
    }

    // ── Wave 16 satirical bullets (LOTTERY / FIREWORK / BRICK) ──

    @Test
    fun `new satirical bullets carry their signature metadata`() {
        assertEquals("firework has a wide splash", 130f, BulletType.FIREWORK.aoeRadius, 0f)
        assertTrue("brick hits heavy", BulletType.BRICK.damageMultiplier >= 2f)
        // Lottery's randomness lives at spawn (impactPower), so its enum damage
        // mul is the neutral base.
        assertEquals(1f, BulletType.LOTTERY.damageMultiplier, 0f)
    }

    // ── Wave 16 fix — ship laser top-cull respects camera-zoom extraYSpan ──

    @Test
    fun `at FAR zoom a ship laser keeps flying past the top instead of vanishing`() {
        var captured: List<Laser> = emptyList()
        val far = controllerWith(
            listOf(ShipLaser(id = "far", xOffset = 50f, yOffset = -200f, yRange = 800f)),
        ) { captured = it }
        far.setExtraYSpan(190f)                       // FAR zoom extends visible top
        far.processShipLasers()
        assertTrue(
            "laser at y=-200 must survive at FAR (cull only past -290), not vanish mid-view",
            captured.any { it.id == "far" },
        )
    }

    @Test
    fun `at default zoom a ship laser is culled once it clears the top`() {
        var captured: List<Laser> = emptyList()
        val near = controllerWith(
            listOf(ShipLaser(id = "near", xOffset = 50f, yOffset = -150f, yRange = 800f)),
        ) { captured = it }
        // extraYSpan defaults to 0 → cull threshold -100.
        near.processShipLasers()
        assertTrue("laser past -100 is culled at default zoom", captured.none { it.id == "near" })
    }

    @Test
    fun `FIREWORK splashes a nearby second enemy (AoE)`() {
        val primary = FakeEnemy("a", hp = 500f, xOffset = 40f, yOffset = 40f, width = 40f, height = 40f)
        val bystander = FakeEnemy("b", hp = 500f, xOffset = 100f, yOffset = 100f, width = 40f, height = 40f)
        val firework = PlasmaShipLaser(
            id = "fw", xOffset = 50f, yOffset = 50f, yRange = 800f,
            bulletType = BulletType.FIREWORK,
        )
        val controller = LasersController(
            screenWidth = 400f, screenHeight = 800f, uuidUtils = UuidUtils(),
            initialShipLasers = listOf(firework),
            setShipLasers = {}, setUltimateLasers = {},
            onLaserHit = { _, _, _, _, _, _ -> }, damageMultiplier = { 1f },
        )
        controller.monitorLaserCollision(emptyList(), listOf(primary, bystander))
        assertTrue("primary takes the direct hit", primary.hp < 500f)
        // bystander is ~85px from primary centre < FIREWORK.aoeRadius(130) → splash.
        assertTrue("bystander takes splash damage", bystander.hp < 500f)
    }

    // ── Wave 16 batch 2 behaviors (BANH_MI / DURIAN / BRICK / HEART) ──

    private fun controllerWith(lasers: List<Laser>, captured: (List<Laser>) -> Unit) =
        LasersController(
            screenWidth = 400f, screenHeight = 800f, uuidUtils = UuidUtils(),
            initialShipLasers = lasers,
            setShipLasers = captured, setUltimateLasers = {},
            onLaserHit = { _, _, _, _, _, _ -> }, damageMultiplier = { 1f },
        )

    @Test
    fun `BANH_MI pierces — survives the first hit, decrementing pierce`() {
        val banhMi = PiercingShipLaser(
            id = "bm", xOffset = 50f, yOffset = 50f, yRange = 800f,
            bulletType = BulletType.BANH_MI,
        )
        assertEquals("starts with BANH_MI pierce count", 3, banhMi.pierceRemaining)
        controllerWith(listOf(banhMi)) {}.monitorLaserCollision(emptyList(), listOf(FakeEnemy()))
        assertEquals("pierced once → 2 left, not destroyed", 2, banhMi.pierceRemaining)
    }

    @Test
    fun `DURIAN splashes a nearby enemy (AoE 110)`() {
        val primary = FakeEnemy("a", hp = 500f, xOffset = 40f, yOffset = 40f, width = 40f, height = 40f)
        val bystander = FakeEnemy("b", hp = 500f, xOffset = 95f, yOffset = 95f, width = 40f, height = 40f)
        val durian = PlasmaShipLaser(
            id = "dr", xOffset = 50f, yOffset = 50f, yRange = 800f, bulletType = BulletType.DURIAN,
        )
        controllerWith(listOf(durian)) {}.monitorLaserCollision(emptyList(), listOf(primary, bystander))
        assertTrue("primary hit", primary.hp < 500f)
        assertTrue("bystander within 110px takes splash", bystander.hp < 500f)
    }

    @Test
    fun `BRICK is a heavy single-hit shot (destroyed on impact, wide body)`() {
        var captured: List<Laser> = listOf(ShipLaser(id = "x", xOffset = 0f, yOffset = 0f, yRange = 800f))
        val brick = ShipLaser(
            id = "br", xOffset = 50f, yOffset = 50f, yRange = 800f, width = 14f,
            bulletType = BulletType.BRICK,
        )
        assertTrue("brick body is wide", brick.width >= 14f)
        controllerWith(listOf(brick)) { captured = it }
            .monitorLaserCollision(emptyList(), listOf(FakeEnemy()))
        assertTrue("brick destroyed on hit (no children)", captured.none { it.id == "br" })
    }

    @Test
    fun `HEART homes toward its target x`() {
        val heart = MissileLaser(
            id = "ht", xOffset = 100f, yOffset = 500f, yRange = 800f, bulletType = BulletType.HEART,
        )
        assertEquals(BulletType.HEART, heart.bulletType)
        heart.targetX = 200f                          // enemy to the right
        val x0 = heart.xOffset
        heart.moveLaser()
        assertTrue("heart nudges toward target", heart.xOffset > x0)
        assertTrue("heart also rises", heart.yOffset < 500f)
    }

    // ── Wave 17 — de-dup đạn: mỗi loại 1 cơ chế riêng ──

    @Test
    fun `GIANT now PLOWS through enemies (pierces, not single-hit)`() {
        // Trước GIANT là đạn to ×2 dmg single-hit (stat thuần). Nay xuyên 4.
        assertEquals("GIANT carries a pierce count", 4, BulletType.GIANT.pierceCount)
        val giant = GiantShipLaser(id = "g", xOffset = 50f, yOffset = 50f, yRange = 800f)
        assertEquals("GiantShipLaser seeds pierceRemaining from enum", 4, giant.pierceRemaining)
        var captured: List<Laser> = listOf(giant)
        controllerWith(listOf(giant)) { captured = it }
            .monitorLaserCollision(emptyList(), listOf(FakeEnemy()))
        assertTrue("GIANT survives the first hit (plows on)", captured.any { it.id == "g" })
        assertEquals("pierce decremented to 3", 3, giant.pierceRemaining)
    }

    @Test
    fun `FIREWORK both splashes AND bursts into child bullets (vs PLASMA splash-only, SPLIT children-only)`() {
        val primary = FakeEnemy("a", hp = 500f, xOffset = 40f, yOffset = 40f, width = 40f, height = 40f)
        val bystander = FakeEnemy("b", hp = 500f, xOffset = 100f, yOffset = 100f, width = 40f, height = 40f)
        var captured: List<Laser> = emptyList()
        val firework = PlasmaShipLaser(
            id = "fw", xOffset = 50f, yOffset = 50f, yRange = 800f, bulletType = BulletType.FIREWORK,
        )
        controllerWith(listOf(firework)) { captured = it }
            .monitorLaserCollision(emptyList(), listOf(primary, bystander))
        assertTrue("splash hits the bystander", bystander.hp < 500f)        // AoE leg
        val children = captured.filter { it.id != "fw" }
        assertEquals("bursts into 5 children", 5, children.size)            // re-fire leg
        assertTrue("children are NORMAL (no recursion)", children.all { it.bulletType == BulletType.NORMAL })
        assertTrue("parent consumed", captured.none { it.id == "fw" })
    }

    @Test
    fun `all 18 bullet types spawn with a DISTINCT width (size riêng)`() {
        val base = Ship(xOffset = 200f, yOffset = 700f)
        val widthByType = BulletType.entries.associateWith { type ->
            var captured: List<Laser> = emptyList()
            LasersController(
                screenWidth = 400f, screenHeight = 800f, uuidUtils = UuidUtils(),
                initialShipLasers = emptyList(),
                setShipLasers = { captured = it }, setUltimateLasers = {},
                onLaserHit = { _, _, _, _, _, _ -> }, damageMultiplier = { 1f },
            ).fireLasers(base.copy(activeBulletType = type))
            captured.first().width
        }
        assertEquals(
            "mỗi loại đạn phải có width riêng; trùng: ${widthByType.entries.groupBy { it.value }.filter { it.value.size > 1 }}",
            widthByType.size, widthByType.values.toSet().size,
        )
    }

    @Test
    fun `KAMEHAMEHA is a WIDE beam, PIERCING is a thin needle (distinct bodies)`() {
        // Cả hai cùng xuyên, nhưng KAMEHAMEHA = beam bản rộng + xuyên-tất + ×3,
        // PIERCING = kim mảnh xuyên 3 ×1 → khác hẳn cảm giác.
        assertTrue("kamehameha pierces far more", BulletType.KAMEHAMEHA.pierceCount > BulletType.PIERCING.pierceCount + 50)
        assertTrue("kamehameha hits much harder", BulletType.KAMEHAMEHA.damageMultiplier >= BulletType.PIERCING.damageMultiplier * 2f)
    }
}
