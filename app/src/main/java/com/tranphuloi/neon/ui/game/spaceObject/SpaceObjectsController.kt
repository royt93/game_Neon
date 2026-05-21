package com.tranphuloi.neon.ui.game.spaceObject

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.utils.Logger
import java.util.UUID
import kotlin.random.Random

class SpaceObjectsController(
    private val screenWidth: Float,
    private val screenHeight: Float,
    initialSpaceObjects: List<SpaceObject>,
    private val setSpaceObjects: (List<SpaceObject>) -> Unit,
) {

    init {
        Logger.d("SpaceObjectsController init: initialObjects=${initialSpaceObjects.size}, screen=${screenWidth}x${screenHeight}")
    }

    var spaceObjects: List<SpaceObject> = initialSpaceObjects
        private set

    private val minRockSize = 20
    private val maxRockSize = 80

    val addSpaceRockId = UUID.randomUUID().toString()
    fun addSpaceRock() {
        val rockSize = Random.nextInt(minRockSize, maxRockSize)
        val rockXOffset = Random.nextInt(rockSize, screenWidth.toInt() - rockSize).toFloat()
        val spaceRock = SpaceRock(
            xOffset = rockXOffset,
            size = rockSize.toFloat(),
            screenHeight = screenHeight
        )
        spaceObjects = spaceObjects.toMutableList().apply { add(spaceRock) }
        Logger.d("SpaceObjectsController.addSpaceRock: size=$rockSize at x=${rockXOffset.toInt()} y=${spaceRock.yOffset.toInt()} (active=${spaceObjects.size})")
        updateSpaceObjectsUI()
    }

    val processSpaceObjectsId = UUID.randomUUID().toString()
    val processSpaceObjectsRepeatTime = Millis(5)
    fun processSpaceObjects() {
        // Round 37 — was logging "removed N" every 5ms tick. Redundant with the
        // per-rock onLaserHit log path. Aggregate stats add noise without signal.
        spaceObjects.forEach { it.moveObject() }
        spaceObjects = spaceObjects.toMutableList().apply { removeAll { it.hp <= 0 } }
        updateSpaceObjectsUI()
    }

    fun hasSpaceObjects() = spaceObjects.isNotEmpty()

    private fun updateSpaceObjectsUI() {
        setSpaceObjects(spaceObjects)
    }
}
