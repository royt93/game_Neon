package com.tranphuloi.neon.ui.game.world

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
// Round 72 — LocalContext + Coil imports removed cùng với GIF explosion migration.
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Round 72 — coil3 imports removed; explosion migrated to ExplosionCanvas.
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.ShipShieldOne
import com.tranphuloi.neon.common.ShipShieldTwo
import com.tranphuloi.neon.common.neonGlow
import com.tranphuloi.neon.ui.game.booster.BoosterUI
import com.tranphuloi.neon.ui.game.controls.BossEntryLightning
import com.tranphuloi.neon.ui.game.damage.DamageNumber
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI
import com.tranphuloi.neon.ui.game.pickup.PickupPopup
import com.tranphuloi.neon.ui.game.explosion.model.Explosion
import com.tranphuloi.neon.ui.game.mineral.model.MineralUI
import com.tranphuloi.neon.ui.game.ship.laser.LaserUI
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObjectUI
// Round 72 — rememberImageLoader removed; no more Coil-loaded assets in GameWorld.
import com.tranphuloi.neon.utils.Logger

@Composable
fun GameWorld(
    ship: Ship,
    shipLasers: List<LaserUI>,
    ultimateLasers: List<LaserUI>,
    spaceObjects: List<SpaceObjectUI>,
    boosters: List<BoosterUI>,
    enemies: List<EnemyUI>,
    enemyLasers: List<LaserUI>,
    minerals: List<MineralUI>,
    explosions: List<Explosion>,
    magnetRadius: Float,
    damageNumbers: List<DamageNumber>,
    impactSparks: List<com.tranphuloi.neon.ui.game.spark.ImpactSpark>,
    pickupBursts: List<com.tranphuloi.neon.ui.game.spark.PickupBurst>,
    pickupPopups: List<PickupPopup>,
    bossIntroShownAtMillis: Long,
    lastBoosterPickupMillis: Long,
    lastMineralPickupMillis: Long,
    chargeProgress: Float,
    /** Round 41 (29x.2) — active mines (rendered as glowing diamonds). */
    mines: List<com.tranphuloi.neon.ui.game.ship.weapon.Mine> = emptyList(),
    /** Round 41 (29x.2) — wall-clock of last BURST sweep; 0 = no active sweep. */
    lastBurstSweepMillis: Long = 0L,
    modifier: Modifier = Modifier,
) {

    // Round 72 — `imageLoader` removed: no Coil-loaded asset in GameWorld nữa.
    // Round 49 (refactored in Round 66) — LaserCanvas now renders pure-vector
    // capsules (drawRoundRect + glow) instead of drawImage. Sprite preload
    // removed. The 5 ic_laser_*.webp drawables can be deleted in a cleanup
    // round once we confirm vector look is keeper.
    // Round 66b — Enemy/SpaceObject/Booster sprite preloads removed; their
    // Canvas now renders pure-vector shapes. Mineral keeps sprite (ic_mineral
    // is a tiny gem icon, vector equivalent would be a single drawCircle
    // which loses character — out of scope for this round).
    // Round 67.6 — mineralSprite preload removed; MineralCanvas now vector.

    // Round 38 — ship aura color from Settings. Defaults to AURA_CYAN's glow so
    // first-run / unset preference renders the original cyan look unchanged.
    val settings = com.tranphuloi.neon.data.LocalSettings.current
    val shipSkin by settings.shipSkin.collectAsState(initial = com.tranphuloi.neon.data.ShipSkin.AURA_CYAN)
    val shipGlowColor = Color(shipSkin.glowColorHex)
    // Round 73 (Issue 2 user audit) — read selectedShipShape để drawShipVector
    // render đúng silhouette theo loại tàu user pick.
    val selectedShipShape by settings.selectedShipShape.collectAsState(
        initial = com.tranphuloi.neon.ui.game.ship.shape.ShipShape.FIGHTER,
    )
    // Round 77 (R77g) — camera zoom level scale toàn entity render.
    val cameraZoom by settings.cameraZoom.collectAsState(
        initial = com.tranphuloi.neon.data.CameraZoom.MEDIUM,
    )
    // Round 41 — BURST sweep uses palette.cyan so it follows Color Blind mode.
    val palette = com.tranphuloi.neon.common.LocalNeonPalette.current

    val infiniteTransition = rememberInfiniteTransition()
    val shipShieldColor by infiniteTransition.animateColor(
        initialValue = ShipShieldOne,
        targetValue = ShipShieldTwo,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Implosion animation tick — drives scale/alpha shrink during the 0-200ms
    // window after destroy. ONLY for animation interpolation; ship visibility is
    // gated by `ship.shipSpriteHidden` (an explicit Boolean on Ship state) which
    // GameState flips via state mutation at +200ms. This avoids relying on the
    // tick alone (Compose smart-skip can ignore reads of a local long var).
    var destroyTickMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(ship.destroyedAtMillis) {
        if (ship.destroyedAtMillis == 0L) return@LaunchedEffect
        Logger.d("GameWorld: implosion LaunchedEffect started (destroyedAtMillis=${ship.destroyedAtMillis})")
        repeat(8) {       // 8 × 25ms = 200ms — exactly covers implosion phase
            destroyTickMillis = System.currentTimeMillis()
            delay(25L)
        }
        Logger.d("GameWorld: implosion LaunchedEffect finished")
    }
    LaunchedEffect(ship.shipSpriteHidden) {
        Logger.d("GameWorld: ship.shipSpriteHidden=${ship.shipSpriteHidden} (recompose triggered)")
    }
    // Round 58 — Compose render-frame counter. `withFrameNanos` fires once
    // per actual Choreographer-paced render frame, so this gives the true
    // on-screen FPS independent of the IO loop's tick rate (which currently
    // logs as PERF frame=N every 1000 iterations). On a 60Hz display the
    // expected ceiling is 60 FPS; if Compose recompose work is the bottleneck
    // this number drops noticeably below 60 at peak enemies. Compare against
    // the IO loop's ~96Hz to confirm whether round 57 enemy Canvas refactor
    // moved the needle. Reports every 1000ms to keep logcat clean.
    var renderFrameCount by remember { mutableLongStateOf(0L) }
    var lastRenderReportMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                renderFrameCount++
                val now = System.currentTimeMillis()
                val elapsed = now - lastRenderReportMillis
                if (elapsed >= 1000L) {
                    val fps = renderFrameCount * 1000.0 / elapsed
                    Logger.d("PERF render FPS=${"%.1f".format(fps)} over ${elapsed}ms (frames=$renderFrameCount)")
                    renderFrameCount = 0
                    lastRenderReportMillis = now
                }
            }
        }
    }
    // Round 77 (R77g) — camera zoom applies graphicsLayer scale to entire world.
    // Pivot at center so zooming doesn't shift origin. Gameplay coordinates
    // (collision/movement) unchanged — purely visual scaling.
    Box(
        modifier = modifier.fillMaxSize().graphicsLayer {
            scaleX = cameraZoom.pixelScale
            scaleY = cameraZoom.pixelScale
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
        },
    ) {
        // Primary visibility gate: explicit state flag on Ship. Set to true via
        // GameState.onShipDestroyed coroutine after delay(200L). Mutating Ship
        // changes the data-class reference → Compose recomposes GameWorld and
        // removes the ship/flame/magnet from the slot table.
        val shipAlive = !ship.shipSpriteHidden
        // 16c: Magnet visual — render below other entities so doesn't obscure ship.
        if (shipAlive) {
            MagnetVisual(
                ship = ship,
                minerals = minerals,
                magnetRadius = magnetRadius,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Round 49 — was 2 forEach blocks (shipLasers + ultimateLasers) of
        // Image+Modifier.neonGlow Composables, one Composable subtree per
        // laser. Replaced with 2 Canvas passes via [LaserCanvas]. Each laser
        // becomes a `drawImage` + `drawCircle` call inside DrawScope instead
        // of a Composable subtree → ~10-20× less recompose work + no Modifier
        // allocation per entity. Sprites pre-loaded once into laserSprites.
        // 3 separate calls preserve the original z-order (ship + ultimate go
        // here, behind enemies; enemyLasers go later, in front).
        com.tranphuloi.neon.ui.game.world.LaserCanvas(
            lasers = shipLasers,
            glow = shipGlowColor,
            intensity = 0.7f,
            radiusFactor = 2.4f,
            modifier = Modifier.fillMaxSize(),
        )
        com.tranphuloi.neon.ui.game.world.LaserCanvas(
            lasers = ultimateLasers,
            glow = NeonGold,
            intensity = 0.85f,
            radiusFactor = 2.0f,
            modifier = Modifier.fillMaxSize(),
        )
        // Round 59 — was forEach { Image + Modifier.size+offset+neonGlow+rotate }
        // → one Canvas pass replicating glow + rotate via DrawScope. Same recipe
        // as round 49/57: visual parity, Compose subtree elimination.
        com.tranphuloi.neon.ui.game.world.SpaceObjectCanvas(
            spaceObjects = spaceObjects,
            modifier = Modifier.fillMaxSize(),
        )
        // Round 59 — booster sprite layer (sprite + glow + PIERCING/PLASMA tint
        // overlay) moved into one Canvas pass via [BoosterCanvas]. Rarity ring
        // (animated Border + neonGlow) + glyph Text stay as Composable overlay
        // below because Border + Text aren't a simple DrawScope recipe (font
        // metrics, RoundedCornerShape stroke) — same call as round 57 where
        // EnemyHpBar remained Composable. At typical N≤3 boosters on-screen,
        // the overlay cost is negligible.
        com.tranphuloi.neon.ui.game.world.BoosterCanvas(
            boosters = boosters,
            modifier = Modifier.fillMaxSize(),
        )
        boosters.forEach {
            if (it.rarityRingColorHex != 0L || it.glyph != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(it.size.dp)
                        .offset(x = it.xOffset.dp, y = it.yOffset.dp),
                ) {
                    if (it.rarityRingColorHex != 0L) {
                        val ringColor = Color(it.rarityRingColorHex)
                        val ringPulse = if (it.isEliteRarity) {
                            0.6f + 0.4f * kotlin.math.abs(
                                kotlin.math.sin(System.currentTimeMillis() / 240.0).toFloat()
                            )
                        } else {
                            0.35f                                  // common: static dim ring
                        }
                        Box(
                            modifier = Modifier
                                .size(it.size.dp)
                                .border(
                                    BorderStroke(2.dp, ringColor.copy(alpha = ringPulse)),
                                    RoundedCornerShape(50),
                                )
                                .neonGlow(
                                    color = ringColor,
                                    intensity = if (it.isEliteRarity) 0.55f * ringPulse else 0f,
                                    radiusFactor = 1.6f,
                                ),
                        )
                    }
                    if (it.glyph != null) {
                        Text(
                            text = it.glyph,
                            color = Color(it.tintColorHex),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp),
                        )
                    }
                }
            }
        }
        // 3b: Ship engine flame trail — drawn before ship sprite so flame appears
        // to emanate from engines (ship Image covers the flame's top edge).
        // Hidden when ship destroyed (after implosion phase).
        if (shipAlive) ShipEngineFlame(
            ship = ship,
            modifier = Modifier.fillMaxSize(),
        )
        // Implosion (ship destroy phase 1, 0-200ms): scale 1.0→0.3, alpha 1.0→0.7.
        // After 200ms ship Box is REMOVED FROM COMPOSITION (`if (shipAlive)`) —
        // graphicsLayer alpha=0 was unreliable on some devices, so this guarantees
        // the ship sprite + shield + glow all disappear cleanly.
        val destroyElapsed = if (ship.destroyedAtMillis > 0L)
            destroyTickMillis - ship.destroyedAtMillis else -1L
        val implosionT = if (destroyElapsed in 0L..200L) destroyElapsed / 200f else -1f
        val shipImplodeScale = if (implosionT >= 0f) 1f - 0.7f * implosionT else 1f
        val shipImplodeAlpha = if (implosionT >= 0f) 1f - 0.3f * implosionT else 1f
        if (shipAlive) Box(
            modifier = Modifier
                .size(ship.shieldSize.dp)
                .offset(x = ship.xOffset.dp, y = ship.yOffset.dp)
                .graphicsLayer {
                    // Spawn cinematic transforms — apply alpha + scale on the outer
                    // Box so shield aura scales together. Rotation is moved to the
                    // Image directly (below) so it pivots around the ship's center.
                    alpha = ship.spawnAlpha * shipImplodeAlpha
                    scaleX = ship.spawnScale * shipImplodeScale
                    scaleY = ship.spawnScale * shipImplodeScale
                }
        ) {
            if (ship.shieldEnabled) {
                Canvas(
                    modifier = Modifier
                        .size(ship.shieldSize.dp)
                        .offset(
                            x = (ship.width / 2 - ship.shieldRadius).dp,
                            y = (ship.height / 2 - ship.shieldRadius).dp
                        ),
                    onDraw = {
                        val colors =
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                shipShieldColor
                            )
                        drawCircle(
                            radius = ship.shieldRadius,
                            brush = Brush.radialGradient(
                                colors = colors,
                                radius = ship.shieldRadius
                            ),
                            blendMode = BlendMode.Hardlight
                        )
                    }
                )
            }
            // Round 61 — PHASE_SHIELD ghost overlay. When ship.phaseShieldEndMillis
            // is in the future, render an expanding/contracting translucent cyan
            // ring (~0.7Hz pulse) around the ship so player knows iframes are
            // active from the buff, not just from taking damage (600ms iframes
            // already show through their own logic).
            val phaseNow = System.currentTimeMillis()
            if (ship.phaseShieldEndMillis > phaseNow) {
                val remaining = (ship.phaseShieldEndMillis - phaseNow).coerceAtLeast(0L)
                val pulse = 0.55f + 0.35f * kotlin.math.abs(
                    kotlin.math.sin(phaseNow / 240.0).toFloat()
                )
                // Fade out in the last 800ms so player sees buff ending.
                val tailFade = if (remaining < 800L) remaining / 800f else 1f
                val ringRadius = ship.shieldRadius * (0.85f + 0.10f * pulse)
                Canvas(
                    modifier = Modifier
                        .size(ship.shieldSize.dp)
                        .offset(
                            x = (ship.width / 2 - ship.shieldRadius).dp,
                            y = (ship.height / 2 - ship.shieldRadius).dp,
                        ),
                    onDraw = {
                        // Translucent cyan ring — distinct from blue shield orb.
                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.45f * pulse * tailFade),
                            radius = ringRadius,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
                        )
                        // Soft inner halo, even more translucent.
                        drawCircle(
                            color = NeonCyan.copy(alpha = 0.12f * pulse * tailFade),
                            radius = ringRadius * 0.85f,
                        )
                    }
                )
            }
            // Ship glow boost on pickup — intensity bump 0.5→1.0 then fade back
            // over 220ms. Compute progress from latest of booster / mineral pickup.
            val nowForGlow = System.currentTimeMillis()
            val sinceBooster = (nowForGlow - lastBoosterPickupMillis).coerceAtLeast(0L)
            val sinceMineral = (nowForGlow - lastMineralPickupMillis).coerceAtLeast(0L)
            val glowElapsed = minOf(sinceBooster, sinceMineral)
            val glowBoost = if (glowElapsed in 0L..220L) {
                (1f - glowElapsed.toFloat() / 220f).coerceIn(0f, 1f) * 0.5f
            } else 0f
            // 19b Charge shot ramp — when player holds both arrows, glow + halo
            // intensity scale up to signal pending mega blast.
            val chargeBoost = chargeProgress * 0.6f
            // Round 66b — Ship pure vector: arrow body + wings + engine
            // glow + cockpit. Replaces Image(ship.drawableId). Rotation +
            // glow boost preserved via graphicsLayer + neonGlow on the
            // Canvas modifier. Color tracks shipSkin (shipGlowColor).
            Canvas(
                modifier = Modifier
                    .width(ship.width.dp)
                    .height(ship.height.dp)
                    .graphicsLayer {
                        rotationZ = ship.spawnRotation + ship.bankRotation
                    }
                    .neonGlow(
                        color = shipGlowColor,
                        intensity = 0.5f + glowBoost + chargeBoost,
                        radiusFactor = 1.5f + glowBoost * 0.4f + chargeBoost * 0.6f,
                    )
            ) {
                drawShipVector(
                    color = shipGlowColor,
                    laserBoosterEnabled = ship.laserBoosterEnabled,
                    shape = selectedShipShape,
                )
            }
        }
        val nowMillis = System.currentTimeMillis()
        // Round 57 — single Canvas pass replaces the prior per-enemy Compose
        // subtree (Column + 1..N Images for sprite + flash + status-effect tints
        // + boss thrust trail). At peak 30 enemies × 4 sub-Images each, that's
        // ~120 Image composables per frame; now it's one Canvas. HP bars stay
        // as a separate Composable overlay (see below) because their 3-layer
        // animation isn't a simple draw recipe.
        EnemyCanvas(
            enemies = enemies,
            nowMillis = nowMillis,
            modifier = Modifier.fillMaxSize(),
        )
        // Round 70 (Issue 8) — Thay EnemyHpBar (3-layer bar nằm trên enemy)
        // bằng EnemyHpNumber (số HP ở center enemy, chỉ show khi damaged +
        // hide tier-1). Boss vẫn dùng BossHpBar full-width.
        // Round 78 (#6 perf) — Filter ở caller để chỉ recompose Box+Text cho
        // enemies damaged thật sự. Trước fix: 30 Composable + 30 neonGlow Brush
        // allocs/frame chỉ để return early. Sau fix: typically 0-3 Composables.
        enemies.forEach {
            val needsHpNumber = !it.isBoss &&
                it.initialHp >= 50f &&
                it.currentHp < it.initialHp &&
                it.currentHp > 0f
            if (needsHpNumber) {
                key(it.enemyId) {
                    Box(modifier = Modifier.offset(x = it.xOffset.dp, y = it.yOffset.dp)) {
                        EnemyHpNumber(
                            enemyId = it.enemyId,
                            currentHp = it.currentHp,
                            initialHp = it.initialHp,
                            lastImpactMillis = it.lastImpactMillis,
                            enemyWidth = it.width,
                        )
                    }
                }
            }
        }
        // Boss entry lightning crackle — drawn after enemies so bolts overlay the
        // boss + thrust trail. Filter for entry-phase boss(es) only.
        enemies.filter { it.isBoss && it.isInEntryPhase }.forEach { boss ->
            BossEntryLightning(boss = boss, modifier = Modifier.fillMaxSize())
        }
        // Laser impact spark burst at every enemy hit point.
        ImpactSparkOverlay(sparks = impactSparks)
        // Pickup burst (ring shockwave + 8 sparkles) on item collected.
        PickupBurstOverlay(bursts = pickupBursts)
        // Round 41 (29x.2) — active mines. Pulse alpha at ~2.5Hz so it reads as "armed".
        mines.forEach { m ->
            key(m.id) {
                val age = (System.currentTimeMillis() - m.createdAtMillis).coerceAtLeast(0L)
                // Symmetric oscillation: |sin| swings 0..1 (every 200ms half-period) — gives a
                // smooth "armed and blinking" feel rather than asymmetric clamp-to-floor.
                val pulse = 0.55f + 0.45f * kotlin.math.abs(kotlin.math.sin(age / 200.0).toFloat())
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(com.tranphuloi.neon.ui.game.ship.weapon.Mine.SIZE.dp)
                        .offset(x = m.xOffset.dp, y = m.yOffset.dp)
                        .neonGlow(color = NeonRedAlert, intensity = 0.7f * pulse, radiusFactor = 2.0f),
                ) {
                    Text(text = "◆", color = NeonRedAlert, fontSize = 22.sp)
                }
            }
        }
        // Round 41 (29x.2) — BURST sweep visual: a fading horizontal cyan band across
        // the upper 2/3 of the screen, lasting 280ms after fire.
        if (lastBurstSweepMillis > 0L) {
            val sweepAge = System.currentTimeMillis() - lastBurstSweepMillis
            if (sweepAge in 0L..280L) {
                val alpha = (1f - sweepAge / 280f).coerceIn(0f, 1f) * 0.55f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.cyan.copy(alpha = alpha)),
                )
            }
        }
        // Round 59 — minerals to a single Canvas pass. Same 25dp sprite, alpha
        // gradient + position preserved exactly.
        com.tranphuloi.neon.ui.game.world.MineralCanvas(
            minerals = minerals,
            modifier = Modifier.fillMaxSize(),
        )
        // Round 72 (Issue 4 user audit) — PURE-CANVAS explosion thay GIF.
        // Trước fix: `anim_explosion.gif` loaded qua Coil 3 + ExplosionBurstOverlay
        // sparks/ring overlay. Sau fix: ExplosionCanvas chứa fireball + sparks +
        // shockwave ring trong 1 lớp Canvas. GIF asset không còn được render
        // (sẵn sàng xoá file ở cleanup phase).
        explosions.forEach {
            ExplosionCanvas(explosion = it)
        }
        // Round 49 — enemy lasers also via LaserCanvas. Placed here (after
        // enemies + explosions) so they render in front of enemies just like
        // before; same z-order as the prior forEach block.
        com.tranphuloi.neon.ui.game.world.LaserCanvas(
            lasers = enemyLasers,
            glow = NeonRedAlert,
            intensity = 0.55f,
            radiusFactor = 1.8f,
            modifier = Modifier.fillMaxSize(),
        )
        // 2c+10b: Damage numbers overlay (top-most game-world layer).
        // Hidden during boss intro to avoid clutter with the boss name banner.
        DamageNumbersOverlay(
            numbers = damageNumbers,
            bossIntroShownAtMillis = bossIntroShownAtMillis,
        )
        // 4c: Mineral pickup popups.
        PickupPopupOverlay(popups = pickupPopups)
    }
}
