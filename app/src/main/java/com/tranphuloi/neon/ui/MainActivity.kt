package com.tranphuloi.neon.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.tranphuloi.neon.common.NeonTheme
import com.tranphuloi.neon.navigation.Game
import com.tranphuloi.neon.navigation.GameOver
import com.tranphuloi.neon.navigation.GamePause
import com.tranphuloi.neon.navigation.Splash
import com.tranphuloi.neon.ui.dlg.gameover.DialogGameOver
import com.tranphuloi.neon.ui.dlg.gamepause.DialogGamePause
import com.tranphuloi.neon.ui.game.GameScreen
import com.tranphuloi.neon.ui.splash.SplashScreen
import com.tranphuloi.neon.utils.Logger

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.d("MainActivity.onCreate")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            NeonTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Splash.route
                ) {
                    composable(route = Splash.route) {
                        SplashScreen {
                            Logger.d("Nav: Splash → Game")
                            with(navController) {
                                popBackStack()
                                navigate(Game.route)
                            }
                        }
                    }
                    composable(route = Game.route) {
                        GameScreen(
                            onGamePause = {
                                Logger.d("Nav: Game → GamePause")
                                navController.navigate(GamePause.route)
                            },
                            onGameOver = { score ->
                                Logger.d("Nav: Game → GameOver (score=$score)")
                                navController.navigate("${GameOver.route}/$score")
                            },
                        )
                    }
                    dialog(route = GamePause.route) {
                        DialogGamePause(onRestartGame = {
                            Logger.d("Nav: GamePause → Game (restart, popUpTo graph)")
                            navController.navigate(Game.route) { popUpTo(navController.graph.id) }
                        })
                    }
                    dialog(
                        route = "${GameOver.route}/{score}",
                        // Force the user to press Restart — back-press / tap-outside must not
                        // strand them on a frozen game screen with no way out.
                        dialogProperties = DialogProperties(
                            dismissOnBackPress = false,
                            dismissOnClickOutside = false,
                        ),
                    ) { backStackEntry ->
                        val score = backStackEntry.arguments?.getString("score").orEmpty()
                        DialogGameOver(
                            score = score,
                            onRestartGame = {
                                Logger.d("Nav: GameOver → Game (restart, popUpTo graph)")
                                navController.navigate(Game.route) { popUpTo(navController.graph.id) }
                            },
                        )
                    }
                }
            }
        }
    }
}
