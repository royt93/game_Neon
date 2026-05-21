package com.tranphuloi.neon.navigation

sealed class Navigation(val route: String)
object Splash : Navigation(route = "splash")
/** Round 25 — start menu: pick mode / continue / open settings before entering Game. */
object Menu : Navigation(route = "menu")
object Game : Navigation(route = "game")
object GamePause : Navigation(route = "game-pause")
object GameOver : Navigation(route = "game-over")
object Settings : Navigation(route = "settings")
object DifficultyPicker : Navigation(route = "difficulty-picker")
/** Wave 5 (43x) — pick a GameMode for the next run. */
object ModePicker : Navigation(route = "mode-picker")
/** Wave 5 (25x) — pick a RunModifier (or skip) for the next run. */
object ModifierPicker : Navigation(route = "modifier-picker")
/** Wave 5 (48x) — meta progression / skill tree. */
object MetaUpgrade : Navigation(route = "meta-upgrade")
/** Wave 4 (42x) round 34 — roguelike buff picker (post-boss). */
object BuffPicker : Navigation(route = "buff-picker")
