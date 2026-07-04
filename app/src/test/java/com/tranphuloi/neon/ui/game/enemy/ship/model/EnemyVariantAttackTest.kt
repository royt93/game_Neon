package com.tranphuloi.neon.ui.game.enemy.ship.model

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.ui.game.enemy.laser.EnemyLaser
import com.tranphuloi.neon.ui.game.enemy.laser.LaserMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 09 (đợt 3) — đòn RIÊNG cho 5 địch chủ đề. Dispatch qua
 * [EnemyAttackKind] (không phụ thuộc R) nên test dựng [RegularEnemyType]
 * trực tiếp. Pin: số tia + hướng/quỹ đạo đặc trưng từng loại.
 */
class EnemyVariantAttackTest {

    private fun enemyWith(kind: EnemyAttackKind): RegularEnemy {
        val type = RegularEnemyType(
            drawableId = 0,
            width = 40f,
            height = 40f,
            hp = 100f,
            impactPower = 50f,
            formation = Row(rowCount = 1),
            xOffsetSpeed = 1f,
            yOffsetSpeed = 1f,
            enemySpawnRate = Millis(1000),
            attackKind = kind,
        )
        return RegularEnemy(
            screenWidth = 1000f,
            screenHeight = 2000f,
            xOffset = 100f,
            type = type,
        )
    }

    private fun lasers(kind: EnemyAttackKind): List<EnemyLaser> =
        enemyWith(kind).generateLasers().map { it as EnemyLaser }

    @Test
    fun `SINGLE bắn đúng 1 tia thẳng — default giữ nguyên`() {
        val l = lasers(EnemyAttackKind.SINGLE)
        assertEquals("SINGLE should fire exactly 1 laser / đúng 1 tia", 1, l.size)
        assertEquals("SINGLE laser goes straight down / bay thẳng", 0f, l[0].xOffsetMovementSpeed, 0.001f)
    }

    @Test
    fun `SPLIT bắn 2 tia toả ra 2 bên`() {
        val l = lasers(EnemyAttackKind.SPLIT)
        assertEquals("SPLIT fires 2 lasers / 2 tia", 2, l.size)
        assertTrue("SPLIT has one leftward laser / có tia lệch trái", l.any { it.xOffsetMovementSpeed < 0f })
        assertTrue("SPLIT has one rightward laser / có tia lệch phải", l.any { it.xOffsetMovementSpeed > 0f })
    }

    @Test
    fun `CONE3 bắn nón 3 tia (giữa + 2 bên)`() {
        val l = lasers(EnemyAttackKind.CONE3)
        assertEquals("CONE3 fires 3 lasers / 3 tia", 3, l.size)
        assertTrue("CONE3 has a straight center laser / có tia giữa thẳng", l.any { it.xOffsetMovementSpeed == 0f })
        assertTrue("CONE3 has a left laser / có tia trái", l.any { it.xOffsetMovementSpeed < 0f })
        assertTrue("CONE3 has a right laser / có tia phải", l.any { it.xOffsetMovementSpeed > 0f })
    }

    @Test
    fun `CURVE2 bắn 2 tia quỹ đạo CONG`() {
        val l = lasers(EnemyAttackKind.CURVE2)
        assertEquals("CURVE2 fires 2 lasers / 2 tia", 2, l.size)
        assertTrue("all CURVE2 lasers use CURVE motion / đều CONG",
            l.all { it.motion == LaserMotion.CURVE })
    }

    @Test
    fun `VOLLEY3 bắn loạt 3 tia thẳng lệch ngang`() {
        val l = lasers(EnemyAttackKind.VOLLEY3)
        assertEquals("VOLLEY3 fires 3 lasers / 3 tia", 3, l.size)
        assertTrue("VOLLEY3 lasers all linear-down / đều thẳng",
            l.all { it.motion == LaserMotion.LINEAR && it.xOffsetMovementSpeed == 0f })
        assertEquals("VOLLEY3 lasers spread across 3 distinct x / 3 vị trí x khác nhau",
            3, l.map { it.xOffset }.distinct().size)
    }

    @Test
    fun `HOMING1 bắn 1 tia bám tàu`() {
        val l = lasers(EnemyAttackKind.HOMING1)
        assertEquals("HOMING1 fires 1 laser / 1 tia", 1, l.size)
        assertEquals("HOMING1 laser homes / quỹ đạo HOMING", LaserMotion.HOMING, l[0].motion)
    }

    @Test
    fun `mỗi attack kind cho số tia đúng như thiết kế`() {
        assertEquals(1, lasers(EnemyAttackKind.SINGLE).size)
        assertEquals(2, lasers(EnemyAttackKind.SPLIT).size)
        assertEquals(3, lasers(EnemyAttackKind.CONE3).size)
        assertEquals(2, lasers(EnemyAttackKind.CURVE2).size)
        assertEquals(3, lasers(EnemyAttackKind.VOLLEY3).size)
        assertEquals(1, lasers(EnemyAttackKind.HOMING1).size)
    }
}
