package com.tranphuloi.neon.ui.game.world

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.tranphuloi.neon.R
import com.tranphuloi.neon.common.ShipShieldOne
import com.tranphuloi.neon.common.ShipShieldTwo
import com.tranphuloi.neon.ui.game.booster.BoosterUI
import com.tranphuloi.neon.ui.game.constellation.Star
import com.tranphuloi.neon.ui.game.enemy.ship.model.EnemyUI
import com.tranphuloi.neon.ui.game.explosion.model.Explosion
import com.tranphuloi.neon.ui.game.mineral.model.MineralUI
import com.tranphuloi.neon.ui.game.ship.laser.LaserUI
import com.tranphuloi.neon.ui.game.ship.ship.Ship
import com.tranphuloi.neon.ui.game.spaceObject.SpaceObjectUI
import com.tranphuloi.neon.ui.game.utils.rememberImageLoader

@Composable
fun GameWorld(
    ship: Ship,
    shipLasers: List<LaserUI>,
    ultimateLasers: List<LaserUI>,
    stars: List<Star>,
    spaceObjects: List<SpaceObjectUI>,
    boosters: List<BoosterUI>,
    enemies: List<EnemyUI>,
    enemyLasers: List<LaserUI>,
    minerals: List<MineralUI>,
    explosions: List<Explosion>,
    modifier: Modifier = Modifier,
) {

    val imageLoader = rememberImageLoader()

    val infiniteTransition = rememberInfiniteTransition()
    val shipShieldColor by infiniteTransition.animateColor(
        initialValue = ShipShieldOne,
        targetValue = ShipShieldTwo,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = modifier.fillMaxSize()) {
        shipLasers.forEach {
            Image(
                painterResource(id = it.drawableId),
                contentDescription = stringResource(id = R.string.laser),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .absoluteOffset(x = it.xOffset.dp, y = it.yOffset.dp)
                    .size(width = it.width.dp, height = it.height.dp)
                    .align(Alignment.BottomStart)
            )
        }
        ultimateLasers.forEach {
            Image(
                painterResource(id = it.drawableId),
                contentDescription = stringResource(id = R.string.laser),
                modifier = Modifier
                    .absoluteOffset(x = it.xOffset.dp, y = it.yOffset.dp)
                    .size(width = it.width.dp, height = it.height.dp)
                    .align(Alignment.BottomStart)
                    .rotate(degrees = it.rotation)
            )
        }
        stars.forEach {
            Canvas(
                modifier = Modifier
                    .size(size = it.size.dp)
                    .offset(x = it.xOffset.dp, y = it.yOffset.dp),
                onDraw = {
                    val colors = listOf(Color.White, Color.Transparent)
                    drawCircle(
                        radius = it.size,
                        brush = Brush.radialGradient(colors),
                        blendMode = BlendMode.Luminosity
                    )
                }
            )
        }
        spaceObjects.forEach {
            Image(
                painterResource(id = it.drawableId),
                contentDescription = stringResource(id = R.string.space_object),
                modifier = Modifier
                    .size(it.size.dp)
                    .offset(x = it.xOffset.dp, y = it.yOffset.dp)
                    .rotate(degrees = it.rotation)
            )
        }
        boosters.forEach {
            Image(
                painterResource(id = it.drawableId),
                contentDescription = stringResource(id = R.string.booster),
                modifier = Modifier
                    .size(it.size.dp)
                    .offset(x = it.xOffset.dp, y = it.yOffset.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(ship.shieldSize.dp)
                .offset(x = ship.xOffset.dp, y = ship.yOffset.dp)
        ) {
            if (ship.shieldEnabled) {
                Canvas(
                    modifier = Modifier
                        .size(size = ship.width.dp)
                        .offset(y = (ship.height - ship.width).dp / 2),
                    onDraw = {
                        val colors =
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                shipShieldColor
                            )
                        drawCircle(
                            radius = ship.shieldSize,
                            brush = Brush.radialGradient(
                                colors = colors,
                                radius = ship.height * 2.5f
                            ),
                            blendMode = BlendMode.Hardlight
                        )
                    }
                )
            }
            Image(
                painterResource(id = ship.drawableId),
                contentDescription = stringResource(id = R.string.ship),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .width(ship.width.dp)
                    .height(ship.height.dp)
            )
        }
        enemies.forEach {
            Column(modifier = Modifier.offset(x = it.xOffset.dp, y = it.yOffset.dp)) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .size(width = it.width.dp, height = 5.dp)
                        .background(Color.White.copy(alpha = 0.7f))
                ) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .size(width = it.hpBarWidth.dp, height = 5.dp)
                            .background(Color.Red)
                    )
                }
                Image(
                    painterResource(id = it.drawableId),
                    contentDescription = stringResource(id = R.string.enemy),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.size(width = it.width.dp, height = it.height.dp)
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
        }
        enemyLasers.forEach {
            Image(
                painterResource(id = it.drawableId),
                contentDescription = stringResource(id = R.string.enemy_laser),
                modifier = Modifier
                    .size(width = it.width.dp, height = it.height.dp)
                    .offset(x = it.xOffset.dp, y = it.yOffset.dp)
            )
        }
    }
}
