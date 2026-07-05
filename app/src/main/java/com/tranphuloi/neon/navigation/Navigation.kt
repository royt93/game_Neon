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
/** QoL Practice — pick an unlocked chapter to practice. */
object PracticePicker : Navigation(route = "practice-picker")
/** Wave 5 (25x) — pick a RunModifier (or skip) for the next run. */
object ModifierPicker : Navigation(route = "modifier-picker")
// Wave 13a (slice C) — MetaUpgrade route removed; skill-tree moved to Shop → Nâng cấp tab.
/** Wave 4 (42x) round 34 — roguelike buff picker (post-boss). */
object BuffPicker : Navigation(route = "buff-picker")
/** Wave 6 (36x) round 45 — pre-game loadout (BulletType + SecondaryWeapon). */
object LoadoutPicker : Navigation(route = "loadout-picker")
/** Round 67.5 — Bách Khoa (info guide) screen accessed from MenuScreen. */
object Info : Navigation(route = "info")
// Wave 13a (slice D) — ShipPicker route removed; ship choose/buy moved to Shop → Tàu tab.
/** Wave 11c — Statistics screen reading MetaProgressionRepository aggregate Flows. */
object Stats : Navigation(route = "stats")
/** Wave 12 — Shop / Economy. Spend lifetime minerals on permanent unlocks. */
object Shop : Navigation(route = "shop")
