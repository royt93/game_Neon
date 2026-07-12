package com.tranphuloi.neon.data

/**
 * Task 22 — tay thuận điều khiển. Di chuyển vẫn là kéo-thả bất kỳ đâu trên màn
 * hình (Round 77/R77h, `ButtonsMovement` đã bị gỡ khỏi compose tree) — mode này
 * chỉ dồn cụm nút hành động (SmartBomb/SecondaryWeapon/Ability/Parry, hiện đóng
 * ở BottomEnd) sang BottomStart khi LEFT_HANDED. `ButtonSettings` (pause) giữ
 * nguyên TopEnd ở mọi mode: TopStart đã kín chỗ bởi IndicatorStatus +
 * ActiveBuffsHud, dồn nút pause qua đó sẽ đè lên nhau.
 */
enum class ControlHandMode(val key: String, val displayName: String) {
    TWO_HANDED(key = "two_handed", displayName = "Hai tay"),
    LEFT_HANDED(key = "left_handed", displayName = "Tay trái"),
    RIGHT_HANDED(key = "right_handed", displayName = "Tay phải");

    companion object {
        fun fromKey(key: String?): ControlHandMode =
            entries.firstOrNull { it.key == key } ?: TWO_HANDED
    }
}
