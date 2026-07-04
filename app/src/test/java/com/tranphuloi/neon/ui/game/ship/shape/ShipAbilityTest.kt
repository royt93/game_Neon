package com.tranphuloi.neon.ui.game.ship.shape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 05 — khung kỹ năng chủ động: mapping 1-1 ship→ability, cooldown giảm theo
 * level, và cooldown controller (ready/activate/progress). JUnit4, no-mock.
 */
class ShipAbilityTest {

    @Test
    fun `every ship maps to an ability and all abilities are used`() {
        // Map 1-1: mỗi tàu 1 ability, và mỗi ability được đúng 1 tàu dùng (22↔22).
        val mapped = ShipShape.entries.map { ShipAbility.forShip(it) }
        assertEquals("mỗi tàu 1 ability", ShipShape.entries.size, mapped.size)
        assertEquals("không ability nào trùng (1-1)", ShipShape.entries.size, mapped.toSet().size)
        assertEquals("dùng hết mọi ShipAbility", ShipAbility.entries.size, mapped.toSet().size)
    }

    @Test
    fun `every ability has positive cooldown and non-empty labels`() {
        for (a in ShipAbility.entries) {
            assertTrue("$a cooldown > 0", a.baseCooldownMs > 0)
            assertTrue("$a có tên", a.displayName.isNotBlank())
            assertTrue("$a có mô tả", a.description.isNotBlank())
            assertTrue("$a có glyph", a.glyph.isNotBlank())
        }
    }

    @Test
    fun `effective cooldown drops with ship level`() {
        val a = ShipAbility.BUNG_NO // base 16000
        val l1 = a.effectiveCooldownMs(1)
        val l5 = a.effectiveCooldownMs(5)
        assertEquals("L1 = base", 16_000L, l1)
        assertEquals("L5 = base × 0.6", (16_000L * 0.6f).toLong(), l5)
        assertTrue("L5 nhanh hơn L1", l5 < l1)
    }

    @Test
    fun `controller not ready until cooldown elapses`() {
        val c = ShipAbilityController(ShipAbility.BUNG_NO, cooldownMs = 10_000L)
        assertTrue("ban đầu sẵn sàng", c.isReady(0L))
        assertTrue("kích hoạt lần đầu OK", c.tryActivate(1_000L))
        assertFalse("ngay sau đó chưa hồi", c.isReady(2_000L))
        assertFalse("kích hoạt lại thất bại", c.tryActivate(5_000L))
        assertTrue("đủ cooldown → sẵn sàng", c.isReady(11_000L))
        assertTrue("kích hoạt lại OK sau cooldown", c.tryActivate(11_000L))
    }

    @Test
    fun `controller progress goes 0 to 1 over cooldown`() {
        val c = ShipAbilityController(ShipAbility.BUNG_NO, cooldownMs = 10_000L)
        c.tryActivate(0L)
        assertEquals("vừa kích hoạt → 0", 0f, c.progress(0L), 0.001f)
        assertEquals("giữa chừng → 0.5", 0.5f, c.progress(5_000L), 0.001f)
        assertEquals("hết cooldown → 1", 1f, c.progress(10_000L), 0.001f)
        assertEquals("quá cooldown vẫn kẹp 1", 1f, c.progress(99_000L), 0.001f)
    }

    @Test
    fun `controller remainingMs counts down to zero`() {
        val c = ShipAbilityController(ShipAbility.BUNG_NO, cooldownMs = 10_000L)
        c.tryActivate(0L)
        assertEquals(10_000L, c.remainingMs(0L))
        assertEquals(4_000L, c.remainingMs(6_000L))
        assertEquals(0L, c.remainingMs(10_000L))
        assertEquals("không âm", 0L, c.remainingMs(20_000L))
    }
}
