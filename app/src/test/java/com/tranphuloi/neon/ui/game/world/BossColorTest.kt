package com.tranphuloi.neon.ui.game.world

import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 17h/17o — chốt MÀU NHẬN DIỆN boss: mỗi BossKind 1 màu RIÊNG (halo + thân)
 * để 27 boss không còn chung tông đỏ. Test bảo đảm không trùng + đều đục (opaque).
 */
class BossColorTest {

    @Test
    fun `every BossKind has a UNIQUE color`() {
        val colors = BossKind.entries.map { bossColorFor(it) }
        val dups = colors.groupBy { it }.filter { it.value.size > 1 }
        assertEquals("màu boss phải duy nhất; trùng nhóm: ${dups.size}", BossKind.entries.size, colors.toSet().size)
    }

    @Test
    fun `every boss color is fully opaque`() {
        assertTrue("màu boss phải đục (alpha=1)", BossKind.entries.all { bossColorFor(it).alpha == 1f })
    }
}
