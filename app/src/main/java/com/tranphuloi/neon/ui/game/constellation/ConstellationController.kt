package com.tranphuloi.neon.ui.game.constellation

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.utils.Logger
import java.util.*
import kotlin.random.Random

class ConstellationController(
    private val stars: () -> List<Star>,
    private val setStars: (List<Star>) -> Unit,
) {

    fun createStars(screenWidth: Float, screenHeight: Float) {
        Logger.d("ConstellationController.createStars: screen=${screenWidth}x${screenHeight}")
        val starList = mutableListOf<Star>()
        for (i in 0..30) {
            val starXOffset = Random.nextInt(0, screenWidth.toInt())
            val starYOffset = Random.nextInt(0, screenHeight.toInt())
            val starSize = Random.nextInt(1, 12)
            val star = Star(
                xOffset = starXOffset.toFloat(),
                yOffset = starYOffset.toFloat(),
                maxYOffset = screenHeight,
                size = starSize.toFloat()
            )
            starList.add(star)
        }
        Logger.d("ConstellationController.createStars: spawned ${starList.size} stars")
        setStars(starList.toList())
    }

    val animateStarsId = UUID.randomUUID().toString()
    val animateStarsRepeatTime = Millis(50)
    fun animateStars() {
        stars().forEach { it.animateStar() }
    }
}
