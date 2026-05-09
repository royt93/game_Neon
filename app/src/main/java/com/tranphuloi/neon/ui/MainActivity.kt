package com.tranphuloi.neon.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.tranphuloi.neon.common.NeonTheme
import com.tranphuloi.neon.ui.dlg.gamepause.DialogGamePause
import com.tranphuloi.neon.navigation.Game
import com.tranphuloi.neon.navigation.GamePause
import com.tranphuloi.neon.navigation.Splash
import com.tranphuloi.neon.ui.game.GameScreen
import com.tranphuloi.neon.ui.splash.SplashScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            NeonTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Splash.route
                ) {
                    composable(route = Splash.route) {
                        SplashScreen {
                            with(navController) {
                                popBackStack()
                                navigate(Game.route)
                            }
                        }
                    }
                    composable(route = Game.route) {
                        GameScreen(onGamePause = { navController.navigate(GamePause.route) })
                    }
                    dialog(route = GamePause.route) {
                        DialogGamePause(onRestartGame = {
                            navController.navigate(Game.route) { popUpTo(navController.graph.id) }
                        })
                    }
                }
            }
        }
    }
}
