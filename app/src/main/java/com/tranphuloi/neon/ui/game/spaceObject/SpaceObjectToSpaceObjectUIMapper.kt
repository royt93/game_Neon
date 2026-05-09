package com.tranphuloi.neon.ui.game.spaceObject

class SpaceObjectToSpaceObjectUIMapper {

    operator fun invoke(spaceObject: SpaceObject): SpaceObjectUI {
        return with(spaceObject) {
            SpaceObjectUI(
                id = id,
                size = size,
                xOffset = xOffset,
                yOffset = yOffset,
                rotation = rotation,
                drawableId = drawableId
            )
        }
    }
}
