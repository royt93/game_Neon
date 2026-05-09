package com.tranphuloi.neon.ui.game.enemy.ship.mapper

import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI

class EnemyToEnemyUIMapper {

    operator fun invoke(enemy: Enemy): EnemyUI {
        return with(enemy) {
            EnemyUI(
                enemyId = enemyId,
                width = width,
                height = height,
                xOffset = xOffset,
                yOffset = yOffset,
                hpBarWidth = width / initialHp * hp,
                drawableId = drawableId,
                lastImpactMillis = lastImpactMillis,
                isBoss = isBoss,
                displayName = displayName,
                currentHp = hp,
                initialHp = initialHp,
                isInEntryPhase = isInEntryPhase,
            )
        }
    }
}
