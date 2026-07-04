package com.tranphuloi.neon.ui.game.ship.shape

/**
 * Task 05 — quản lý cooldown kỹ năng chủ động (THUẦN, inject `now` → test JVM).
 *
 * Giữ mốc kích hoạt cuối + cooldown thực (đã tính theo level). `tryActivate` chỉ
 * thành công khi đã hết cooldown (trả true + cập nhật mốc). `progress` 0..1 cho
 * vòng cooldown trên nút HUD (1 = sẵn sàng).
 */
class ShipAbilityController(
    val ability: ShipAbility,
    val cooldownMs: Long,
) {
    private var lastActivatedMillis: Long = Long.MIN_VALUE / 2

    fun isReady(nowMillis: Long): Boolean =
        nowMillis - lastActivatedMillis >= cooldownMs

    /** Kích hoạt nếu sẵn sàng. Trả true nếu vừa kích hoạt (caller áp hiệu ứng). */
    fun tryActivate(nowMillis: Long): Boolean {
        if (!isReady(nowMillis)) return false
        lastActivatedMillis = nowMillis
        return true
    }

    /** Tiến độ hồi chiêu 0..1 (1 = sẵn sàng). Dùng cho vòng cooldown HUD. */
    fun progress(nowMillis: Long): Float {
        if (cooldownMs <= 0L) return 1f
        val elapsed = nowMillis - lastActivatedMillis
        return (elapsed.toFloat() / cooldownMs).coerceIn(0f, 1f)
    }

    /** Mili-giây còn lại của cooldown (0 nếu sẵn sàng). */
    fun remainingMs(nowMillis: Long): Long =
        (cooldownMs - (nowMillis - lastActivatedMillis)).coerceAtLeast(0L)
}
