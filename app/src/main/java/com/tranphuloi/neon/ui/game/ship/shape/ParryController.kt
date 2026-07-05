package com.tranphuloi.neon.ui.game.ship.shape

/**
 * Task 13 (đợt 4) — Counter-attack / Parry. Cửa sổ ngắn PHẢN ĐẠN + cooldown.
 * Pure logic (không phụ thuộc Android) để test; kích hoạt thành công thì mở cửa
 * sổ reflect ([windowEndMillis]) — GameState set `ship.reflectEndMillis` để tái
 * dùng nhánh absorb+retaliate sẵn có (ShipController). Thời gian truyền vào (now)
 * để test tất định.
 */
class ParryController(
    private val windowMs: Long = 400L,
    private val cooldownMs: Long = 5_000L,
) {
    private var windowEnd: Long = 0L
    private var cooldownEnd: Long = 0L

    /** Cuối cửa sổ phản đạn của lần kích hoạt gần nhất (GameState dùng set reflectEndMillis). */
    val windowEndMillis: Long get() = windowEnd

    /** Kích hoạt parry nếu đã hết cooldown. Trả true nếu kích hoạt (mở cửa sổ). */
    fun tryActivate(now: Long): Boolean {
        if (now < cooldownEnd) return false
        windowEnd = now + windowMs
        cooldownEnd = now + cooldownMs
        return true
    }

    /** Đang trong cửa sổ phản đạn. */
    fun isActive(now: Long): Boolean = now < windowEnd

    /** Sẵn sàng bấm lại (hết cooldown). */
    fun isReady(now: Long): Boolean = now >= cooldownEnd

    /** Tiến trình hồi chiêu 0f..1f (1 = sẵn sàng). */
    fun progress(now: Long): Float {
        if (now >= cooldownEnd) return 1f
        val elapsed = (cooldownMs - (cooldownEnd - now)).toFloat()
        return (elapsed / cooldownMs).coerceIn(0f, 1f)
    }

    /** Mili giây còn lại tới khi sẵn sàng (0 nếu đã sẵn sàng). */
    fun remainingMs(now: Long): Long = (cooldownEnd - now).coerceAtLeast(0L)
}
