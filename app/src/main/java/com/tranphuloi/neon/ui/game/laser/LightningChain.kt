package com.tranphuloi.neon.ui.game.laser

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy

/**
 * Task 02 (Slice 1) — logic chain sét THUẦN (không Compose/Android) cho
 * [com.tranphuloi.neon.ui.game.ship.laser.BulletType.LIGHTNING].
 *
 * Từ điểm va chạm, nhảy TUẦN TỰ tối đa [MAX_STEPS] lần: mỗi bước chọn địch còn
 * sống, CHƯA thăm, GẦN NHẤT trong [RADIUS] tính từ vị trí hop trước (visited-set
 * tránh nhảy lặp / dao động). Sát thương giảm dần ×[FALLOFF] mỗi bước.
 *
 * Khác `CHAIN_LIGHTNING_BOOSTER` (2 địch gần điểm gốc, 50%, 1 cấp): đây là chuỗi
 * tuần tự nhiều bước từ nạn nhân này sang nạn nhân kế.
 */
object LightningChain {
    const val MAX_STEPS: Int = 3
    const val RADIUS: Float = 120f
    const val FALLOFF: Float = 0.7f

    /**
     * Trả danh sách địch bị chain, theo THỨ TỰ nhảy (hop 0 = gần điểm bắt đầu nhất).
     *
     * @param excludeIds địch không được chain vào (thường là địch trúng đòn chính).
     */
    fun computeChainTargets(
        startX: Float,
        startY: Float,
        enemies: List<Enemy>,
        excludeIds: Set<String> = emptySet(),
        maxSteps: Int = MAX_STEPS,
        radius: Float = RADIUS,
    ): List<Enemy> {
        if (enemies.isEmpty() || maxSteps <= 0) return emptyList()
        val visited = HashSet(excludeIds)
        val result = ArrayList<Enemy>(maxSteps)
        val r2 = radius * radius
        var px = startX
        var py = startY
        repeat(maxSteps) {
            var best: Enemy? = null
            var bestD2 = Float.MAX_VALUE
            for (e in enemies) {
                if (e.destroyed || e.hp <= 0f) continue
                if (e.enemyId in visited) continue
                val cx = e.xOffset + e.width / 2f
                val cy = e.yOffset + e.height / 2f
                val dx = cx - px
                val dy = cy - py
                val d2 = dx * dx + dy * dy
                if (d2 <= r2 && d2 < bestD2) {
                    bestD2 = d2
                    best = e
                }
            }
            val next = best ?: return result
            result += next
            visited += next.enemyId
            px = next.xOffset + next.width / 2f
            py = next.yOffset + next.height / 2f
        }
        return result
    }

    /**
     * Sát thương cho hop thứ [hopIndex] (0-based): base × falloff^(hopIndex+1).
     * Sàn 1 để không có hop 0-damage.
     */
    fun damageForHop(baseDamage: Int, hopIndex: Int, falloff: Float = FALLOFF): Int {
        var dmg = baseDamage.toDouble()
        repeat(hopIndex + 1) { dmg *= falloff }
        // roundToInt (không truncate) để tránh lệch do 0.7f imprecise (69.99 → 70).
        return Math.round(dmg).toInt().coerceAtLeast(1)
    }
}
