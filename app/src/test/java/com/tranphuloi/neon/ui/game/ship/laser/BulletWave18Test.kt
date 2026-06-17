package com.tranphuloi.neon.ui.game.ship.laser

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.laser.Laser
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.utils.UuidUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 18 — integration test cho 4 đạn trào phúng batch 3, drive
 * [LasersController.monitorLaserCollision] / [LasersController.fireLasers] thật
 * với enemy giả (giống BulletSlice3Test). Chứng minh collision + spawn đúng:
 *
 *   • Trà Sữa  — nổ AoE splash + đẻ 3 "trân châu", parent biến mất.
 *   • Dép Lào  — boomerang sống qua nhiều hit (hitsRemaining), huỷ sau 4 hit.
 *   • Nước Mắm — đạn thẳng 1 hit, huỷ ngay, mang đúng bulletType lên onLaserHit.
 *   • Mã QR    — đạn thẳng 1 hit, huỷ ngay, mang đúng bulletType.
 *   • fireLasers — trang bị đạn nào ra đúng loại + đúng width model.
 */
class BulletWave18Test {

    private class FakeEnemy(
        override val enemyId: String = "e",
        override val isBoss: Boolean = false,
        override var hp: Float = 500f,
        override var xOffset: Float = 0f,
        override var yOffset: Float = 0f,
        override val width: Float = 60f,
        override val height: Float = 60f,
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

    private fun controllerWith(
        lasers: List<Laser>,
        captured: (List<Laser>) -> Unit = {},
        onHit: (BulletType) -> Unit = {},
    ) = LasersController(
        screenWidth = 400f,
        screenHeight = 800f,
        uuidUtils = UuidUtils(),
        initialShipLasers = lasers,
        setShipLasers = captured,
        setUltimateLasers = {},
        onLaserHit = { _, _, _, _, _, bt -> onHit(bt) },
        damageMultiplier = { 1f },
    )

    // ── Trà Sữa ──

    @Test
    fun `BUBBLE_TEA splashes a nearby enemy AND bursts into 3 pearls`() {
        val primary = FakeEnemy("a", hp = 500f, xOffset = 40f, yOffset = 40f, width = 40f, height = 40f)
        val bystander = FakeEnemy("b", hp = 500f, xOffset = 95f, yOffset = 95f, width = 40f, height = 40f)
        var captured: List<Laser> = emptyList()
        val tea = PlasmaShipLaser(
            id = "tea", xOffset = 50f, yOffset = 50f, yRange = 800f,
            width = 20f, bulletType = BulletType.BUBBLE_TEA,
        )
        controllerWith(listOf(tea), captured = { captured = it })
            .monitorLaserCollision(emptyList(), listOf(primary, bystander))

        assertTrue("địch trúng trực tiếp mất máu", primary.hp < 500f)
        assertTrue("bystander trong bán kính 100 ăn splash", bystander.hp < 500f)
        val pearls = captured.filter { it.id != "tea" }
        assertEquals("văng ra đúng 3 trân châu", 3, pearls.size)
        assertTrue("trân châu là NORMAL (không đệ quy)", pearls.all { it.bulletType == BulletType.NORMAL })
        assertTrue("parent (ly trà sữa) đã biến mất", captured.none { it.id == "tea" })
    }

    // ── Dép Lào ──

    @Test
    fun `SANDAL survives the first hit (decrements hitsRemaining, not destroyed)`() {
        val boomerang = BoomerangShipLaser(id = "dl", xOffset = 50f, yOffset = 50f, yRange = 800f)
        var captured: List<Laser> = listOf(boomerang)
        controllerWith(listOf(boomerang), captured = { captured = it })
            .monitorLaserCollision(emptyList(), listOf(FakeEnemy()))
        assertEquals("trúng 1 → còn 3 lượt", 3, boomerang.hitsRemaining)
        assertTrue("vẫn bay tiếp (chưa huỷ)", captured.any { it.id == "dl" })
    }

    @Test
    fun `SANDAL is destroyed after exhausting all 4 hits (on DISTINCT enemies)`() {
        // Mỗi địch khác id → không dính cooldown → trừ đủ 4.
        val boomerang = BoomerangShipLaser(id = "dl", xOffset = 50f, yOffset = 50f, yRange = 800f)
        var captured: List<Laser> = listOf(boomerang)
        val controller = controllerWith(listOf(boomerang), captured = { captured = it })
        repeat(4) { i ->
            controller.monitorLaserCollision(
                emptyList(), listOf(FakeEnemy("e$i")), nowMillis = 1000L + i * 10L,
            )
        }
        assertEquals("hết lượt", 0, boomerang.hitsRemaining)
        assertTrue("đã huỷ sau 4 hit", captured.none { it.id == "dl" })
    }

    @Test
    fun `SANDAL counts only ONE hit per enemy within cooldown — proves no point-blank drain (đánh 2 chiều)`() {
        // Bịt blind-spot audit: collision chạy Millis(1) nên đạn chồng lên 1 địch
        // nhiều tick. Cooldown đảm bảo mỗi địch chỉ ăn 1 hit/lượt → còn hit cho
        // lượt VỀ thay vì cắm hết 4 hit vào 1 địch trong vài ms.
        val boomerang = BoomerangShipLaser(id = "dl", xOffset = 50f, yOffset = 50f, yRange = 800f)
        val enemy = FakeEnemy("solo")
        val controller = controllerWith(listOf(boomerang))

        // Lượt "lên": trúng 1 lần.
        controller.monitorLaserCollision(emptyList(), listOf(enemy), nowMillis = 1000L)
        assertEquals("hit lượt lên", 3, boomerang.hitsRemaining)

        // Vẫn chồng lên địch ở nhiều tick kế (trong cooldown 180ms) → KHÔNG trừ.
        repeat(8) { controller.monitorLaserCollision(emptyList(), listOf(enemy), nowMillis = 1000L + it * 15L) }
        assertEquals("trong cooldown không cắm thêm hit", 3, boomerang.hitsRemaining)

        // Lượt "về" (sau cooldown, mô phỏng đã lên đỉnh & quay lại) → trúng lại.
        controller.monitorLaserCollision(emptyList(), listOf(enemy), nowMillis = 1000L + 300L)
        assertEquals("hit lượt về", 2, boomerang.hitsRemaining)
    }

    // ── Nước Mắm + Mã QR (đạn thẳng 1 hit, mang bulletType lên onLaserHit) ──

    @Test
    fun `FISH_SAUCE is a single-hit shot carrying its bulletType to onLaserHit`() {
        val hits = mutableListOf<BulletType>()
        var captured: List<Laser> = emptyList()
        val sauce = ShipLaser(
            id = "ns", xOffset = 50f, yOffset = 50f, yRange = 800f,
            width = 10f, bulletType = BulletType.FISH_SAUCE,
        )
        controllerWith(listOf(sauce), captured = { captured = it }, onHit = { hits.add(it) })
            .monitorLaserCollision(emptyList(), listOf(FakeEnemy()))
        assertEquals("onLaserHit nhận đúng loại Nước Mắm", listOf(BulletType.FISH_SAUCE), hits)
        assertTrue("huỷ ngay, không đẻ con", captured.none { it.id == "ns" } && captured.isEmpty())
    }

    @Test
    fun `QR_CODE is a single-hit shot carrying its bulletType to onLaserHit`() {
        val hits = mutableListOf<BulletType>()
        var captured: List<Laser> = emptyList()
        val qr = ShipLaser(
            id = "qr", xOffset = 50f, yOffset = 50f, yRange = 800f,
            width = 17f, bulletType = BulletType.QR_CODE,
        )
        controllerWith(listOf(qr), captured = { captured = it }, onHit = { hits.add(it) })
            .monitorLaserCollision(emptyList(), listOf(FakeEnemy()))
        assertEquals("onLaserHit nhận đúng loại Mã QR", listOf(BulletType.QR_CODE), hits)
        assertTrue("huỷ ngay", captured.none { it.id == "qr" })
    }

    // ── fireLasers — trang bị đạn nào ra đúng loại + đúng width model ──

    @Test
    fun `fireLasers spawns each batch-3 bullet with its model width`() {
        val base = Ship(xOffset = 200f, yOffset = 700f)
        val expectedWidth = mapOf(
            BulletType.BUBBLE_TEA to 20f,
            BulletType.FISH_SAUCE to 10f,
            BulletType.SANDAL to 16f,
            BulletType.QR_CODE to 17f,
        )
        expectedWidth.forEach { (type, w) ->
            var captured: List<Laser> = emptyList()
            controllerWith(emptyList(), captured = { captured = it })
                .fireLasers(base.copy(activeBulletType = type))
            val l = captured.firstOrNull { it.bulletType == type }
            assertTrue("phải spawn đạn $type", l != null)
            assertEquals("$type đúng width model", w, l!!.width, 0.5f)
        }
    }

    // ── Wave 18b — nhịp bắn + số viên theo item đạn ──

    @Test
    fun `fireLasers sets the next cadence from the active bullet type (mạnh = thưa)`() {
        val c = controllerWith(emptyList())
        c.fireLasers(Ship(xOffset = 200f, yOffset = 700f, activeBulletType = BulletType.NORMAL))
        assertEquals(200, (c.fireLaserRepeatTime as Millis).timeMillis)
        c.fireLasers(Ship(xOffset = 200f, yOffset = 700f, activeBulletType = BulletType.KAMEHAMEHA))
        assertEquals("Kamehameha bắn thưa nhất", 700, (c.fireLaserRepeatTime as Millis).timeMillis)
    }

    @Test
    fun `rapidFireMultiplier shortens the per-type cadence`() {
        val c = controllerWith(emptyList())
        c.rapidFireMultiplier = 0.5f
        c.fireLasers(Ship(xOffset = 200f, yOffset = 700f, activeBulletType = BulletType.NORMAL))
        assertEquals("200ms × 0.5 = 100ms", 100, (c.fireLaserRepeatTime as Millis).timeMillis)
    }

    @Test
    fun `salvoCount 1 yields a single bullet per shot when no buffs active`() {
        var captured: List<Laser> = emptyList()
        controllerWith(emptyList(), captured = { captured = it })
            .fireLasers(Ship(xOffset = 200f, yOffset = 700f, activeBulletType = BulletType.NORMAL))
        assertEquals("1 viên/loạt khi không buff", 1, captured.size)
    }

    @Test
    fun `fireLasers SANDAL spawns a BoomerangShipLaser`() {
        var captured: List<Laser> = emptyList()
        controllerWith(emptyList(), captured = { captured = it })
            .fireLasers(Ship(xOffset = 200f, yOffset = 700f, activeBulletType = BulletType.SANDAL))
        assertTrue(
            "Dép Lào phải là BoomerangShipLaser (có quỹ đạo quay về)",
            captured.any { it is BoomerangShipLaser },
        )
    }
}
