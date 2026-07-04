package com.tranphuloi.neon.ui.game.ship.shape

/**
 * Task 03 — hệ level/XP cho tàu (THUẦN, không Compose/Android → test JVM).
 *
 * Mỗi tàu tích XP riêng (persist theo key ở MetaProgressionRepository). XP kiếm
 * mỗi run = số địch thường diệt + 10×boss hạ. Level 1..5 theo ngưỡng luỹ kế; mỗi
 * cấp trên 1 cho +2% hpMul (thuần survivability, khiêm tốn — EffectiveStats vẫn
 * cap hpMul 0.3–3.0). Bonus KHÔNG ảnh hưởng cổng mở khoá (vẫn theo minerals).
 */
object ShipXpLevels {
    const val MAX_LEVEL: Int = 5
    const val HP_BONUS_PER_LEVEL: Float = 0.02f
    const val XP_PER_BOSS: Int = 10

    /** XP luỹ kế cần để ĐẠT mỗi level (index = level-1). L1=0 … L5=1500. */
    private val THRESHOLDS = intArrayOf(0, 100, 300, 700, 1500)

    /** Level hiện tại (1..[MAX_LEVEL]) từ tổng XP. */
    fun levelForXp(xp: Int): Int {
        var lvl = 1
        for (i in THRESHOLDS.indices) if (xp >= THRESHOLDS[i]) lvl = i + 1
        return lvl.coerceIn(1, MAX_LEVEL)
    }

    /** XP luỹ kế cần để ĐẠT [level] (kẹp 1..[MAX_LEVEL]). */
    fun xpForLevel(level: Int): Int = THRESHOLDS[level.coerceIn(1, MAX_LEVEL) - 1]

    /** XP còn thiếu để lên level kế; 0 nếu đã max. */
    fun xpToNextLevel(xp: Int): Int {
        val lvl = levelForXp(xp)
        if (lvl >= MAX_LEVEL) return 0
        return (xpForLevel(lvl + 1) - xp).coerceAtLeast(0)
    }

    /** Tiến độ 0..1 trong level hiện tại (cho thanh XP). Max level → 1. */
    fun progressInLevel(xp: Int): Float {
        val lvl = levelForXp(xp)
        if (lvl >= MAX_LEVEL) return 1f
        val cur = xpForLevel(lvl)
        val next = xpForLevel(lvl + 1)
        if (next <= cur) return 1f
        return ((xp - cur).toFloat() / (next - cur)).coerceIn(0f, 1f)
    }

    /** Hệ số nhân HP theo level: 1.0 + 0.02×(level-1). L1=1.0 … L5=1.08. */
    fun hpBonusMulForLevel(level: Int): Float =
        1f + HP_BONUS_PER_LEVEL * (level.coerceIn(1, MAX_LEVEL) - 1)

    /** Hệ số nhân HP trực tiếp từ tổng XP (tiện cho GameState). */
    fun hpBonusMulForXp(xp: Int): Float = hpBonusMulForLevel(levelForXp(xp))

    /** XP kiếm được của 1 run: địch thường + [XP_PER_BOSS]×boss. */
    fun xpForRun(enemiesKilled: Int, bossesDefeated: Int): Int =
        enemiesKilled.coerceAtLeast(0) + XP_PER_BOSS * bossesDefeated.coerceAtLeast(0)

    // Task 05 — cooldown kỹ năng chủ động giảm theo level tàu.
    const val CD_REDUCTION_PER_LEVEL: Float = 0.10f

    /** Hệ số cooldown theo level: 1.0 − 0.10×(level-1), sàn 0.6 (L5). */
    fun cooldownMulForLevel(level: Int): Float =
        (1f - CD_REDUCTION_PER_LEVEL * (level.coerceIn(1, MAX_LEVEL) - 1)).coerceAtLeast(0.6f)

    /** Hệ số cooldown trực tiếp từ tổng XP. */
    fun cooldownMulForXp(xp: Int): Float = cooldownMulForLevel(levelForXp(xp))
}
