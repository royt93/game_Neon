package com.tranphuloi.neon.ui.game.laser

interface Laser {
    val id: String
    var xOffset: Float
    var yOffset: Float
    var width: Float
    var height: Float
    var rotation: Float
    var impactPower: Float
    val drawableId: Int
    val xOffsetMovementSpeed: Float
    val yOffsetMovementSpeed: Float
    var destroyed: Boolean

    /**
     * Round 35 (35x) — bullet type for collision branching.
     *   - NORMAL: destroy on first hit (existing behavior).
     *   - PIERCING: decrement pierceRemaining on hit; destroy at 0.
     *   - PLASMA: destroy on first hit + spawn AoE damage radius.
     */
    val bulletType: com.tranphuloi.neon.ui.game.ship.laser.BulletType
        get() = com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL

    /**
     * Round 35 (35x) — PIERCING tracking. Default 0 = single-hit destroy.
     * Mutable so collision handler can decrement.
     */
    var pierceRemaining: Int
        get() = 0
        set(@Suppress("UNUSED_PARAMETER") value) { /* no-op for non-piercing */ }

    /**
     * Task 01 (Slice 3) — nguồn phát. Default SHIP để mọi laser hiện có không đổi
     * hành vi; drone laser override thành [LaserSource.DRONE].
     */
    val source: LaserSource
        get() = LaserSource.SHIP

    fun moveLaser()
}
