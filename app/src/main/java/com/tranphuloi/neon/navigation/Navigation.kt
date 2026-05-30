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
/** Wave 6 (36x) round 45 — pre-game loadout (BulletType + SecondaryWeapon). */
object LoadoutPicker : Navigation(route = "loadout-picker")
/** Round 67.5 — Bách Khoa (info guide) screen accessed from MenuScreen. */
object Info : Navigation(route = "info")
/** Round 73 (Wave 8) — Ship picker: chọn loại tàu (5 ShipShape) ảnh hưởng stat. */
object ShipPicker : Navigation(route = "ship-picker")
/** Wave 11c — Statistics screen reading MetaProgressionRepository aggregate Flows. */
object Stats : Navigation(route = "stats")
/** Wave 12 — Shop / Economy. Spend lifetime minerals on permanent unlocks. */
object Shop : Navigation(route = "shop")
