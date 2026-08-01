package com.tranphuloi.neon.ui.game.ship.fusion

import com.tranphuloi.neon.ui.game.ship.shape.ShipShape

/**
 * Task 33 — tàu dung hợp thứ 2 (partner), chọn trong Shop → tab Tàu. Là
 * singleton tiến-trình (transient, không persist qua DataStore) — giống
 * [com.tranphuloi.neon.ui.game.trial.TrialSession], nhưng KHÔNG bị clear khi
 * bấm Play thường: người chơi chọn 1 lần, giữ nguyên cho tới khi tự đổi/bỏ
 * trong Shop (không phải "thử 1 lần" như trial).
 */
object FusionSession {
    @Volatile
    var partner: ShipShape? = null

    fun clear() {
        partner = null
    }
}
