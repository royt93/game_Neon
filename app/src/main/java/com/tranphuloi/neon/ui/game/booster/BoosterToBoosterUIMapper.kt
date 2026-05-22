package com.tranphuloi.neon.ui.game.booster

class BoosterToBoosterUIMapper {

    operator fun invoke(booster: Booster): BoosterUI {
        return with(booster) {
            BoosterUI(
                xOffset = xOffset,
                yOffset = yOffset,
                size = size,
                drawableId = type.drawableId,
                rarityRingColorHex = rarity.ringColorHex,
                isEliteRarity = rarity != BoosterRarity.COMMON,
            )
        }
    }
}
