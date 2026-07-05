package com.tranphuloi.neon.ui.game.controls

/**
 * Task 14 (đợt 4) — hàm thuần cho visual juice (không phụ thuộc Compose → test JVM).
 */
object VisualJuice {
    /**
     * Cường độ screen-tear 0f..1f theo thời gian TRÔI QUA kể từ lúc trúng đòn.
     * = 1 ngay khi trúng (elapsed 0), giảm tuyến tính, = 0 sau [durationMs].
     */
    fun damageTearAlpha(elapsedMs: Long, durationMs: Long = 250L): Float {
        if (durationMs <= 0L) return 0f
        return (1f - elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)
    }
}
