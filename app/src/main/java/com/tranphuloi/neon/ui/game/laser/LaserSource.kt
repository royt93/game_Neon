package com.tranphuloi.neon.ui.game.laser

/**
 * Task 01 (Slice 3) — nguồn phát của một [Laser] trong hệ laser chung.
 * Cho phép drone bắn vào cùng list `shipLasers` (tái dùng collision + render)
 * nhưng vẫn phân biệt được nguồn nếu cần render/âm thanh riêng.
 */
enum class LaserSource {
    SHIP,
    DRONE,
}
