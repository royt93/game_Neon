package com.tranphuloi.neon.ui.game.ship.laser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 17j — chốt MODEL nhận diện đạn: mỗi [BulletType] khai báo shape / size
 * (bodyWidth) / color / special. Các test này BẢO ĐẢM mọi field phân biệt giữa
 * 18 loại → không bao giờ "đụng hàng" (nếu thêm/sửa đạn làm trùng → vỡ test).
 */
class BulletModelTest {

    private val all = BulletType.entries

    @Test
    fun `every bullet has a UNIQUE shape`() {
        val shapes = all.map { it.shape }
        assertEquals("shape phải duy nhất; trùng: ${dups(shapes)}", all.size, shapes.toSet().size)
    }

    @Test
    fun `every bullet has a UNIQUE bodyWidth (size)`() {
        val widths = all.map { it.bodyWidth }
        assertEquals("size phải duy nhất; trùng: ${dups(widths)}", all.size, widths.toSet().size)
    }

    @Test
    fun `every bullet has a UNIQUE color`() {
        val colors = all.map { it.colorArgb }
        assertEquals("color phải duy nhất; trùng: ${dups(colors)}", all.size, colors.toSet().size)
    }

    @Test
    fun `every bullet has a UNIQUE special description`() {
        val specials = all.map { it.special }
        assertEquals("special phải duy nhất; trùng: ${dups(specials)}", all.size, specials.toSet().size)
    }

    @Test
    fun `colorArgb matches BulletTypeColorMap (model + map consistent)`() {
        all.forEach {
            assertEquals("màu model ≠ map cho $it", BulletTypeColorMap.argbFor(it), it.colorArgb)
        }
    }

    @Test
    fun `bodyWidth spans a wide contrast range (needle to giant)`() {
        assertTrue("nhỏ nhất ≤ 3px (kim)", all.minOf { it.bodyWidth } <= 3f)
        assertTrue("lớn nhất ≥ 36px (khổng lồ)", all.maxOf { it.bodyWidth } >= 36f)
    }

    @Test
    fun `every bullet declares a non-empty special`() {
        assertTrue(all.all { it.special.isNotBlank() })
    }

    private fun <T> dups(list: List<T>): List<T> =
        list.groupBy { it }.filter { it.value.size > 1 }.keys.toList()
}
