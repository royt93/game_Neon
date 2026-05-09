package com.tranphuloi.neon.ui.game.mineral.mapper

import com.tranphuloi.neon.ui.game.mineral.model.Mineral
import com.tranphuloi.neon.ui.game.mineral.model.MineralUI

class MineralToMineralUIMapper {

    operator fun invoke(mineral: Mineral): MineralUI {
        return with(mineral) {
            MineralUI(
                xOffset = xOffset,
                yOffset = yOffset,
                width = width,
                alpha = alpha
            )
        }
    }
}
