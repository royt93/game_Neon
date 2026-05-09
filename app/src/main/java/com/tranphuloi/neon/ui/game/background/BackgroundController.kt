package com.tranphuloi.neon.ui.game.background

import com.tranphuloi.neon.ui.game.common.Millis
import com.tranphuloi.neon.utils.Logger
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Controls all space-themed background entities:
 *  - 5-layer parallax starfield (depth illusion as ship moves forward)
 *  - Star twinkle (near layer only — perf-friendly)
 *  - Spectral color variety (white/yellow/blue/red distribution)
 *  - Comet w/ parabolic trajectory + sparkle trail (rare event 6-12s)
 *  - Drifting nebula blobs (3 fixed positions, alpha pulse)
 *  - Galaxy spiral (top-right corner, ultra-slow rotation)
 *  - Dust particles (~50, foreground parallax filler)
 *
 * All entities tick on the same shared cadence. Single Canvas in
 * [com.tranphuloi.neon.ui.game.background.SpaceBackground] renders everything,
 * avoiding 200+ Canvas instances per frame.
 */
class BackgroundController(
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val updateState: (BackgroundState) -> Unit,
) {

    init {
        Logger.d("BackgroundController init: screen=${screenWidth}x${screenHeight}")
    }

    private var stars: List<BgStar> = emptyList()
    private var dust: List<DustParticle> = emptyList()
    private var nebula: List<NebulaBlob> = emptyList()
    private var comet: Comet? = null
    private var galaxy: Galaxy? = null
    private var nextCometSpawnAtMillis: Long = 0L

    /** Spawn all static entities (stars, dust, nebula, galaxy) plus seed comet timer. */
    fun init() {
        Logger.d("BackgroundController.init: building entities")
        stars = buildList {
            // Layer 0 (farthest, dimmest, slowest)
            addAll(generateStars(count = 50, layer = 0, sizeMin = 1f, sizeMax = 2f, speed = 0.15f, alpha = 0.25f, twinkles = false))
            // Layer 1
            addAll(generateStars(count = 30, layer = 1, sizeMin = 2f, sizeMax = 3f, speed = 0.3f, alpha = 0.4f, twinkles = false))
            // Layer 2 (mid)
            addAll(generateStars(count = 22, layer = 2, sizeMin = 3f, sizeMax = 5f, speed = 0.5f, alpha = 0.6f, twinkles = false))
            // Layer 3
            addAll(generateStars(count = 14, layer = 3, sizeMin = 5f, sizeMax = 8f, speed = 0.75f, alpha = 0.8f, twinkles = false))
            // Layer 4 (nearest, brightest, twinkles)
            addAll(generateStars(count = 8, layer = 4, sizeMin = 7f, sizeMax = 12f, speed = 1.1f, alpha = 1.0f, twinkles = true))
        }
        Logger.d("BackgroundController.init: spawned ${stars.size} stars across 5 layers")

        dust = (1..50).map {
            DustParticle(
                xOffset = Random.nextInt(0, screenWidth.toInt()).toFloat(),
                yOffset = Random.nextInt(0, screenHeight.toInt()).toFloat(),
                ySpeed = Random.nextFloat() * 1.5f + 1.5f,    // 1.5..3.0 (faster than near layer)
                maxYOffset = screenHeight,
                alpha = Random.nextFloat() * 0.25f + 0.1f,    // 0.1..0.35
            )
        }
        Logger.d("BackgroundController.init: spawned ${dust.size} dust particles")

        nebula = listOf(
            NebulaBlob(
                xCenter = screenWidth * 0.25f,
                yCenter = screenHeight * 0.3f,
                radius = screenWidth * 0.55f,
                colorArgb = NEBULA_VIOLET_ARGB,
                baseAlpha = 0.10f,
                pulsePhase = 0f,
                pulseSpeed = 0.012f,
            ),
            NebulaBlob(
                xCenter = screenWidth * 0.85f,
                yCenter = screenHeight * 0.55f,
                radius = screenWidth * 0.5f,
                colorArgb = NEBULA_CYAN_ARGB,
                baseAlpha = 0.08f,
                pulsePhase = 1.5f,
                pulseSpeed = 0.009f,
            ),
            NebulaBlob(
                xCenter = screenWidth * 0.45f,
                yCenter = screenHeight * 0.85f,
                radius = screenWidth * 0.6f,
                colorArgb = NEBULA_MAGENTA_ARGB,
                baseAlpha = 0.06f,
                pulsePhase = 3.0f,
                pulseSpeed = 0.011f,
            ),
        )
        Logger.d("BackgroundController.init: spawned ${nebula.size} nebula blobs")

        galaxy = Galaxy(
            xCenter = screenWidth * 0.82f,
            yCenter = screenHeight * 0.18f,
            radius = screenWidth * 0.18f,
            rotation = 0f,
            rotationSpeed = 0.06f,                            // ~1 rotation per minute @ 100Hz tick
            coreColorArgb = GALAXY_CORE_ARGB,
            armColorArgb = GALAXY_ARM_ARGB,
        )
        Logger.d("BackgroundController.init: spawned galaxy at top-right corner")

        nextCometSpawnAtMillis = System.currentTimeMillis() + Random.nextInt(6_000, 12_000)
        Logger.d("BackgroundController.init: first comet scheduled in ${nextCometSpawnAtMillis - System.currentTimeMillis()}ms")

        publish()
    }

    val tickId: String = UUID.randomUUID().toString()
    val tickRepeatTime = Millis(20)            // 50Hz background tick — perf-friendly

    fun tick() {
        // Update all entity states.
        stars.forEach { it.tick() }
        dust.forEach { it.tick() }
        nebula.forEach { it.tick() }
        galaxy?.tick()

        // Comet: advance + sparkle trail + cleanup.
        // Sparkles list is rebuilt as immutable List each tick so the render thread
        // never iterates a list being structurally mutated (fixes ConcurrentModificationException).
        comet?.let { c ->
            val advancing = c.t < 1f
            if (advancing) c.t += c.tStep

            // Build new immutable sparkle list: fade existing (drop dead) + add new at head.
            val updated = ArrayList<Sparkle>(c.sparkles.size + 1)
            c.sparkles.forEach { s ->
                val newAlpha = s.alpha - Sparkle.SPARKLE_FADE
                if (newAlpha > 0f) {
                    updated.add(s.copy(alpha = newAlpha))
                }
            }
            if (advancing) {
                val (x, y) = c.position()
                updated.add(
                    Sparkle(
                        xOffset = x + (Random.nextFloat() - 0.5f) * 8f,
                        yOffset = y + (Random.nextFloat() - 0.5f) * 8f,
                        alpha = 1f,
                        size = Random.nextFloat() * 2f + 1f,
                    )
                )
            }
            // Atomic reassignment — readers either see old or new list, never partial.
            c.sparkles = updated

            if (c.isFinished()) {
                Logger.d("BackgroundController: comet finished, sparkles cleared")
                comet = null
            }
        }

        // Schedule next comet if window elapsed.
        val now = System.currentTimeMillis()
        if (comet == null && now >= nextCometSpawnAtMillis) {
            spawnComet()
            nextCometSpawnAtMillis = now + Random.nextInt(6_000, 12_000)
        }

        publish()
    }

    private fun spawnComet() {
        // Random direction: top-left → bottom-right OR top-right → bottom-left.
        val leftToRight = Random.nextBoolean()
        val startX = if (leftToRight) -20f else screenWidth + 20f
        val endX = if (leftToRight) screenWidth + 20f else -20f
        val startY = Random.nextFloat() * screenHeight * 0.4f
        val endY = startY + screenHeight * (0.5f + Random.nextFloat() * 0.3f)
        // Parabola sags toward bottom — natural arc.
        val controlX = (startX + endX) / 2f
        val controlY = (startY + endY) / 2f - screenHeight * 0.15f
        comet = Comet(
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            controlX = controlX,
            controlY = controlY,
            t = 0f,
            tStep = 0.012f,                                 // ~1.4s travel @ 50Hz tick
            colorArgb = COMET_COLOR_ARGB,
            sparkles = emptyList(),
        )
        Logger.d("BackgroundController.spawnComet: ${if (leftToRight) "L→R" else "R→L"} from (${startX.toInt()},${startY.toInt()}) to (${endX.toInt()},${endY.toInt()})")
    }

    private fun publish() {
        updateState(
            BackgroundState(
                stars = stars,
                dust = dust,
                nebula = nebula,
                comet = comet,
                galaxy = galaxy,
            )
        )
    }

    private fun generateStars(
        count: Int,
        layer: Int,
        sizeMin: Float,
        sizeMax: Float,
        speed: Float,
        alpha: Float,
        twinkles: Boolean,
    ): List<BgStar> {
        return (1..count).map {
            val size = Random.nextFloat() * (sizeMax - sizeMin) + sizeMin
            BgStar(
                xOffset = Random.nextInt(0, screenWidth.toInt()).toFloat(),
                yOffset = Random.nextInt(0, screenHeight.toInt()).toFloat(),
                baseSize = size,
                maxYOffset = screenHeight,
                layer = layer,
                ySpeed = speed,
                baseAlpha = alpha,
                baseColorArgb = pickStarColor(),
                twinkles = twinkles,
                twinklePhase = Random.nextFloat() * (2f * PI.toFloat()),
                twinkleSpeed = 0.05f + Random.nextFloat() * 0.10f,
            )
        }
    }

    /**
     * Spectral distribution roughly matching real stars:
     *  - 70% white-blue (most common O/B/A spectral)
     *  - 15% yellow (G-type, like Sun)
     *  - 10% blue (hot O/B)
     *  - 5% red (M giants / cool dwarfs)
     */
    private fun pickStarColor(): Long {
        val r = Random.nextFloat()
        return when {
            r < 0.70f -> STAR_WHITE_ARGB
            r < 0.85f -> STAR_YELLOW_ARGB
            r < 0.95f -> STAR_BLUE_ARGB
            else -> STAR_RED_ARGB
        }
    }

    companion object {
        // Pre-computed ARGB Long values to avoid Color allocation in draw loops.
        // Format: 0xAARRGGBB
        const val STAR_WHITE_ARGB: Long = 0xFFF5F8FFL          // slight cyan tint
        const val STAR_YELLOW_ARGB: Long = 0xFFFFF0C8L
        const val STAR_BLUE_ARGB: Long = 0xFFB4D0FFL
        const val STAR_RED_ARGB: Long = 0xFFFFB4B4L

        const val NEBULA_VIOLET_ARGB: Long = 0xFFB14CFFL
        const val NEBULA_CYAN_ARGB: Long = 0xFF00F0FFL
        const val NEBULA_MAGENTA_ARGB: Long = 0xFFFF2DE0L

        const val GALAXY_CORE_ARGB: Long = 0xFFFFE8C8L         // warm core
        const val GALAXY_ARM_ARGB: Long = 0xFFB14CFFL          // violet arms

        const val COMET_COLOR_ARGB: Long = 0xFFE0F0FFL         // slight blue-white
    }
}

/** Snapshot pushed to the Composable each tick. */
data class BackgroundState(
    val stars: List<BgStar>,
    val dust: List<DustParticle>,
    val nebula: List<NebulaBlob>,
    val comet: Comet?,
    val galaxy: Galaxy?,
)
