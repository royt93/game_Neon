package com.tranphuloi.neon.ui.game.enemy.ship.factory

import com.tranphuloi.neon.ui.game.enemy.ship.model.BossKind
import com.tranphuloi.neon.ui.game.enemy.ship.model.Enemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBoss
import com.tranphuloi.neon.ui.game.enemy.ship.model.FinalBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBoss
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelOneBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBoss
import com.tranphuloi.neon.ui.game.enemy.ship.model.LevelTwoBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBoss
import com.tranphuloi.neon.ui.game.enemy.ship.model.MidBossType
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemy
import com.tranphuloi.neon.ui.game.enemy.ship.model.RegularEnemyType
import com.tranphuloi.neon.ui.game.enemy.ship.model.Row
import com.tranphuloi.neon.ui.game.enemy.ship.model.SineWave
import com.tranphuloi.neon.ui.game.enemy.ship.model.VFormation
import com.tranphuloi.neon.ui.game.enemy.ship.model.ZigZag
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.utils.Logger

class EnemyFactory(
    private val screenWidth: Float,
    private val screenHeight: Float,
    // Round 78 (#4 follow-up) — exposed (internal) so EnemyController.setSpawnXMargin
    // can pipe the same margin into the formation helper.
    internal val formationXOffsetMutable: FormationXOffset = FormationXOffset(screenWidth),
    /**
     * Round 78 (#4 spec fix follow-up) — extend X spawn range into the negative
     * margins and beyond screenWidth so enemies appear at the visual screen
     * edges at FAR/MEDIUM camera zoom. graphicsLayer.scale shrinks the visible
     * world to inner X% of screen — without this extension the outer margin
     * bands at FAR zoom would always be empty of enemies.
     *
     * Only applied to the V/SineWave/Triangle "anchored to centerX" formations
     * + the random fallback. Row/ZigZag formations preserve their original
     * spawn pattern so the formation reads correctly at all zoom levels.
     */
    var spawnXMargin: Float = 0f,
) {

    /**
     * Round 79 (#1) — chapter context để pick bossKind override theo Chapter.
     * Map duplicate slots sang BossKind mới (Ch3End→DEATH_MOON, Ch3Mid→HAUNTED_KID,
     * Ch4Mid→HELL_LORD, Ch4End→SATAN_GLYPH). Set bởi EnemyController khi chapter
     * advance trong GameState. Default 1 = Chapter 1.
     */
    var currentChapterId: Int = 1

    private fun resolveBossKindForChapter(type: EnemyType): BossKind? = when {
        type is LevelOneBossType && currentChapterId == 3 -> BossKind.DEATH_MOON
        type is LevelTwoBossType && currentChapterId == 4 -> BossKind.SATAN_GLYPH
        type == MidBossType.OFFENSIVE && currentChapterId == 4 -> BossKind.HELL_LORD
        type == MidBossType.SWARM -> BossKind.HAUNTED_KID
        else -> null
    }

    operator fun invoke(type: EnemyType, getShip: () -> Ship): List<Enemy> {
        val enemies: MutableList<Enemy> = mutableListOf()
        if (type is RegularEnemyType) {
            when (type.formation) {
                is ZigZag -> {
                    val enemy = RegularEnemy(
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        xOffset = formationXOffsetMutable.zigZagXOffset(type.formation),
                        type = type
                    )
                    enemies += enemy
                    Logger.v { "EnemyFactory: ZigZag spawn drawable=${type.drawableId} hp=${type.hp} formation=${type.formation}" }
                }

                is Row -> {
                    for (i in 1..type.formation.rowCount) {
                        val enemy = RegularEnemy(
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            xOffset = formationXOffsetMutable.rowXOffset(
                                formation = type.formation,
                                previousEnemy = enemies.lastOrNull(),
                                enemyWidth = type.width
                            ),
                            type = type
                        )
                        enemies += enemy
                    }
                    Logger.v { "EnemyFactory: Row spawn count=${type.formation.rowCount} drawable=${type.drawableId} hp=${type.hp}" }
                }

                is VFormation -> {
                    // V shape: center enemy first (highest), then 1 left + 1 right each
                    // layer staggered DOWN (so V points upward — leading ship at top center).
                    val n = type.formation.count.coerceAtLeast(3)
                    val half = (n - 1) / 2
                    val xStep = type.width * 1.4f
                    val yStep = type.height * 0.9f
                    val centerX = screenWidth / 2f - type.width / 2f
                    for (i in 0 until n) {
                        // Slot ordering: -half .. +half (so even N has one extra on the right)
                        val slot = i - half
                        val yLayer = kotlin.math.abs(slot)
                        val sign = if (slot >= 0) 1 else -1
                        val xs = centerX + sign * yLayer * xStep
                        val ys = -yLayer * yStep                    // negative = above screen top, descend in
                        // Round 78 (#4 fix) — clamp extends to negative margin at FAR zoom
                        // so V-formation wing tips reach the visual screen edge.
                        enemies += RegularEnemy(
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            xOffset = xs.coerceIn(-spawnXMargin, screenWidth - type.width + spawnXMargin),
                            type = type,
                            initialYOffset = ys,
                        )
                    }
                    Logger.v { "EnemyFactory: VFormation spawn count=$n drawable=${type.drawableId}" }
                }

                is SineWave -> {
                    val n = type.formation.count.coerceAtLeast(3)
                    val yStep = type.height * 1.4f
                    val centerX = screenWidth / 2f - type.width / 2f
                    for (i in 0 until n) {
                        // All share the same anchor x; stagger vertically so the wave
                        // pattern is visible as a serpent of [n] segments.
                        enemies += RegularEnemy(
                            screenWidth = screenWidth,
                            screenHeight = screenHeight,
                            xOffset = centerX,
                            type = type,
                            initialYOffset = -i * yStep,
                        )
                    }
                    Logger.v { "EnemyFactory: SineWave spawn count=$n drawable=${type.drawableId}" }
                }
            }
        } else if (type is LevelOneBossType && enemies.isEmpty()) {
            val boss = LevelOneBoss(
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                getShip = getShip,
                bossKindOverride = resolveBossKindForChapter(type),
            )
            enemies += boss
            Logger.w("EnemyFactory: LevelOneBoss SPAWNED hp=${boss.hp.toInt()} impactPower=${boss.impactPower} kind=${boss.bossKind}")
        } else if (type is LevelTwoBossType && enemies.isEmpty()) {
            val boss = LevelTwoBoss(
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                bossKindOverride = resolveBossKindForChapter(type),
            )
            enemies += boss
            Logger.w("EnemyFactory: LevelTwoBoss SPAWNED hp=${boss.hp.toInt()} impactPower=${boss.impactPower} kind=${boss.bossKind}")
        } else if (type is MidBossType && enemies.isEmpty()) {
            val mid = MidBoss(
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                variant = type,
                getShip = getShip,
                bossKindOverride = resolveBossKindForChapter(type),
            )
            enemies += mid
            Logger.w("EnemyFactory: MidBoss SPAWNED variant=${type::class.simpleName} hp=${mid.hp.toInt()} kind=${mid.bossKind}")
        } else if (type is FinalBossType && enemies.isEmpty()) {
            val finalBoss = FinalBoss(
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                getShip = getShip,
            )
            enemies += finalBoss
            Logger.w("EnemyFactory: FinalBoss SPAWNED hp=${finalBoss.hp.toInt()} (3-phase, 22500 total)")
        }
        return enemies
    }
}
