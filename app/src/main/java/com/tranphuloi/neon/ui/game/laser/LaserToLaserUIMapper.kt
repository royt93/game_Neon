package com.tranphuloi.neon.ui.game.laser

import com.tranphuloi.neon.ui.game.ship.laser.LaserUI

class LaserToLaserUIMapper {

    operator fun invoke(laser: Laser): LaserUI {
        return with(laser) {
            LaserUI(
                id = id,
                xOffset = xOffset,
                yOffset = yOffset,
                width = width,
                height = height,
                rotation = rotation,
                drawableId = drawableId
            )
        }
    }
}
