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
import androidx.compose.material.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.NeonCyan
import com.tranphuloi.neon.common.NeonGold
import com.tranphuloi.neon.common.NeonMagenta
import com.tranphuloi.neon.common.NeonRedAlert
import com.tranphuloi.neon.common.NeonViolet
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
import com.tranphuloi.neon.ui.game.utils.rememberImageLoader
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

    val imageLoader = rememberImageLoader()
    // Round 49 — pre-load all 5 laser sprite drawables as ImageBitmaps so the
    // Canvas-based [LaserCanvas] doesn't go through resource resolution per
    // frame. Loaded once at GameWorld level + shared across the 3 LaserCanvas
    // calls below.
    val laserSprites = com.tranphuloi.neon.ui.game.world.rememberLaserSprites()

    // Round 38 — ship aura color from Settings. Defaults to AURA_CYAN's glow so
    // first-run / unset preference renders the original cyan look unchanged.
    val settings = com.tranphuloi.neon.data.LocalSettings.current
    val shipSkin by settings.shipSkin.collectAsState(initial = com.tranphuloi.neon.data.ShipSkin.AURA_CYAN)
    val shipGlowColor = Color(shipSkin.glowColorHex)
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
    Box(modifier = modifier.fillMaxSize()) {
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
            sprites = laserSprites,
            glow = shipGlowColor,
            intensity = 0.7f,
            radiusFactor = 2.4f,
            modifier = Modifier.fillMaxSize(),
        )
        com.tranphuloi.neon.ui.game.world.LaserCanvas(
            lasers = ultimateLasers,
            sprites = laserSprites,
            glow = NeonGold,
            intensity = 0.85f,
            radiusFactor = 2.0f,
            modifier = Modifier.fillMaxSize(),
        )
        spaceObjects.forEach {
            Image(
                painterResource(id = it.drawableId),
                contentDescription = stringResource(id = R.string.space_object),
                modifier = Modifier
                    .size(it.size.dp)
                    .offset(x = it.xOffset.dp, y = it.yOffset.dp)
                    .neonGlow(color = NeonViolet, intensity = 0.35f, radiusFactor = 1.4f)
                    .rotate(degrees = it.rotation)
            )
        }
        boosters.forEach {
            // Round 43 (39x) — rarity ring overlay drawn behind the sprite (Box order:
            // ring first, sprite on top). Common is barely visible; Rare/Epic pulse.
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
                // Round 54 — PIERCING/PLASMA boosters reuse existing drawables
                // (booster_red_lasers / booster_ultimate_weapon) and were visually
                // indistinguishable from LASER/ULTIMATE. Swap the glow color
                // (NeonGold default → tint) and overlay a glyph badge so the
                // booster type is identifiable without new art assets.
                val glowColor = if (it.tintColorHex != 0L) Color(it.tintColorHex) else NeonGold
                Image(
                    painterResource(id = it.drawableId),
                    contentDescription = stringResource(id = R.string.booster),
                    modifier = Modifier
                        .size(it.size.dp)
                        .neonGlow(color = glowColor, intensity = 0.85f, radiusFactor = 1.8f)
                )
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
            Image(
                painterResource(id = ship.drawableId),
                contentDescription = stringResource(id = R.string.ship),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .width(ship.width.dp)
                    .height(ship.height.dp)
                    .graphicsLayer {
                        // Rotation pivots on Image center (default transformOrigin
                        // 0.5/0.5) — fixes bank-tilt drift caused by rotating outer
                        // Box (whose center was offset from Image center).
                        rotationZ = ship.spawnRotation + ship.bankRotation
                    }
                    .neonGlow(
                        color = shipGlowColor,
                        intensity = 0.5f + glowBoost + chargeBoost,
                        radiusFactor = 1.5f + glowBoost * 0.4f + chargeBoost * 0.6f,
                    )
            )
        }
        val nowMillis = System.currentTimeMillis()
        // Round 46 — `key(it.enemyId)` is the biggest single perf win here: at peak
        // wave the enemies list churns 30-50 items per second. Without stable keys
        // Compose treats every list shift as a node teardown + recreate (HP bar,
        // sprite, status-effect overlay), which compounds with the per-frame
        // recompose driven by refreshHandler. Stable id → reuse the node.
        enemies.forEach {
            key(it.enemyId) {
                val sinceHit = nowMillis - it.lastImpactMillis
            val hitFlash = if (it.lastImpactMillis > 0L && sinceHit in 0..120) {
                (1f - sinceHit / 120f).coerceIn(0f, 1f)
            } else 0f
            Column(modifier = Modifier.offset(x = it.xOffset.dp, y = it.yOffset.dp)) {
                // Skip mini-HP-bar over boss heads — boss has dedicated top-screen
                // BossHpBar already (avoid duplicate visualization).
                // 3-layer HP bar with damage trail — see EnemyHpBar for layer design.
                if (!it.isBoss) {
                    EnemyHpBar(
                        enemyId = it.enemyId,
                        currentHp = it.currentHp,
                        initialHp = it.initialHp,
                        enemyWidth = it.width,
                    )
                }
                Box {
                    // Boss thrust trail — render fading copies stacked upward when boss
                    // is sliding down from off-screen. Looks like rocket motion blur.
                    if (it.isBoss && it.isInEntryPhase) {
                        for (i in 1..4) {
                            val trailAlpha = (1f - i * 0.22f).coerceIn(0f, 1f) * 0.55f
                            Image(
                                painter = painterResource(id = it.drawableId),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .size(width = it.width.dp, height = it.height.dp)
                                    .offset(y = -(i * 30).dp)
                                    .alpha(trailAlpha)
                                    .neonGlow(
                                        color = NeonMagenta,
                                        intensity = 0.4f * trailAlpha,
                                        radiusFactor = 1.4f,
                                    )
                            )
                        }
                    }
                    Image(
                        painterResource(id = it.drawableId),
                        contentDescription = stringResource(id = R.string.enemy),
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .size(width = it.width.dp, height = it.height.dp)
                            .neonGlow(
                                color = NeonMagenta,
                                intensity = 0.45f + hitFlash * 0.4f,
                                radiusFactor = 1.4f + hitFlash * 0.4f
                            )
                    )
                    if (hitFlash > 0f) {
                        Image(
                            painterResource(id = it.drawableId),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            colorFilter = ColorFilter.tint(
                                Color.White.copy(alpha = hitFlash)
                            ),
                            modifier = Modifier.size(width = it.width.dp, height = it.height.dp)
                        )
                    }
                    // Round 35 (42x) — status effect tint overlay. One Image per active
                    // effect; the ARGB tint encodes its identity (orange=BURN, cyan=SLOW,
                    // yellow=STUN). Pulse alpha at ~3Hz so the overlay is visibly "alive".
                    if (it.activeStatusEffectTints.isNotEmpty()) {
                        val pulse = 0.65f + 0.35f * kotlin.math.sin(nowMillis / 160.0).toFloat()
                        it.activeStatusEffectTints.forEach { argb ->
                            Image(
                                painterResource(id = it.drawableId),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                colorFilter = ColorFilter.tint(Color(argb.toInt())),
                                alpha = pulse,
                                modifier = Modifier.size(width = it.width.dp, height = it.height.dp),
                            )
                        }
                    }
                }
            }
            }   // close key(it.enemyId)
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
        minerals.forEach {
            Box(
                modifier = Modifier
                    .width(it.width.dp)
                    .offset(x = it.xOffset.dp, y = it.yOffset.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mineral),
                    contentDescription = stringResource(id = R.string.mineral_content_description),
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(25.dp)
                        .alpha(alpha = it.alpha)
                )
            }
        }
        explosions.forEach {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(R.drawable.anim_explosion)
                        .build(),
                    imageLoader = imageLoader
                ),
                contentDescription = stringResource(id = R.string.explosion_content_description),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset(it.xOffset.dp, it.yOffset.dp)
                    .size(it.size.dp)
            )
            // 13c: 8-12 burst lines + ring shockwave overlay on top of GIF.
            ExplosionBurstOverlay(explosion = it)
        }
        // Round 49 — enemy lasers also via LaserCanvas. Placed here (after
        // enemies + explosions) so they render in front of enemies just like
        // before; same z-order as the prior forEach block.
        com.tranphuloi.neon.ui.game.world.LaserCanvas(
            lasers = enemyLasers,
            sprites = laserSprites,
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
