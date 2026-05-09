package com.tranphuloi.neon.navigation

sealed class Navigation(val route: String)
object Splash : Navigation(route = "splash")
object Game : Navigation(route = "game")
object GamePause : Navigation(route = "game-pause")
object GameOver : Navigation(route = "game-over")
object Settings : Navigation(route = "settings")
object DifficultyPicker : Navigation(route = "difficulty-picker")
