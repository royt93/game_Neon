package com.tranphuloi.neon.ui.game.enemy.ship.factory

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.Row
import com.tranphuloi.neon.ui.game.enemy.ship.model.ZigZag
import com.tranphuloi.neon.ui.game.enemy.ship.model.ZigZagInitialPosition

class FormationXOffset(private val screenWidth: Float) {

    /**
     * Round 78 (#4 spec fix follow-up) — extend the X spawn anchor at FAR zoom
     * so ZigZag bounces between true visual screen edges (not the unscaled
     * playfield edges that render in the inner 70%).
     */
    var spawnXMargin: Float = 0f

    fun zigZagXOffset(formation: ZigZag): Float {
        // Audit-5 P2 fix — apply per-stage anchor shift to ZigZag start X.
        // ZigZag was the LAST formation type without positional variation
        // (50% of chapter 1 stages → user's "repetitive feel" complaint
        // partially survived). Shift moves the bounce wall inward/outward.
        val shift = formation.xAnchorShift
        return if (formation.position == ZigZagInitialPosition.LEFT) {
            -spawnXMargin + shift
        } else {
            screenWidth + spawnXMargin + shift
        }
    }

    fun rowXOffset(formation: Row, previousEnemy: Enemy?, enemyWidth: Float): Float {
        // Effective screen width includes extended margin so a full Row spans
        // visual screen edge to edge at any zoom.
        val effectiveWidth = screenWidth + spawnXMargin * 2f
        val divider = formation.rowCount + 1
        val distanceBetween = effectiveWidth / divider - enemyWidth / divider
        // Pixel-2 #4 — apply per-stage anchor shift to the Row's starting X.
        // Subsequent members chain off `previousEnemy` so the shift propagates.
        return previousEnemy?.let { it.xOffset + distanceBetween }
            ?: (-spawnXMargin + distanceBetween + formation.xAnchorShift)
    }
}
