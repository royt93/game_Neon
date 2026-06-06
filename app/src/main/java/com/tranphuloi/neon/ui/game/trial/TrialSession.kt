package com.tranphuloi.neon.ui.game.trial

import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType
import com.tranphuloi.neon.ui.game.ship.laser.BulletType
import com.tranphuloi.neon.ui.game.ship.shape.ShipShape

/**
 * Wave 25 (#trial) — "chơi thử" 1 item từ Bách Khoa. Bấm card → đặt [TrialSession.spec]
 * → vào Game; GameState đọc spec để cấu hình sẵn (đạn/tàu/boss) MÀ KHÔNG ghi đè
 * loadout đã lưu. Là singleton tiến-trình (transient, không persist) — giống map
 * module-level của `tinker`; set khi bấm card, xoá khi bắt đầu 1 run thường (PLAY).
 *
 * KHUNG: 3 biến thể; vòng này wire ĐẠN, Ship/Boss bổ sung sau (tái dùng khung).
 */
sealed interface TrialSpec {
    /** Thử 1 loại đạn (bỏ qua khoá shop — mục đích là dùng thử). */
    data class Bullet(val type: BulletType) : TrialSpec

    /** Thử 1 tàu (shape). */
    data class Ship(val shape: ShipShape) : TrialSpec

    /** Thử 1 boss trong đấu trường luyện (spawn lặp boss đó). */
    data class Boss(val variant: MidBossType) : TrialSpec
}

object TrialSession {
    /** Item đang thử (null = run thường). Đọc 1 lần lúc GameState khởi tạo. */
    @Volatile
    var spec: TrialSpec? = null

    fun clear() {
        spec = null
    }
}
