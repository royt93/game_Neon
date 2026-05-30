# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line numbers + source file for stack-trace debugging in release.
# Tradeoff: ~10KB APK bloat for production crash reports staying actionable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Wave 11d audit-10 P0 fix — entity Serializable keeps ──────────────────
# Project-wide pattern: rememberSaveable + Bundle marshalling needs entity
# field names + constructors intact. @Keep annotations cover most entities;
# below catches anything that missed an explicit @Keep but extends our
# Serializable hierarchy.
#
# Note: enums get a default `valueOf(String)` reflection path in DataStore
# string preference deserialization. Names MUST survive minification.
-keep class com.tranphuloi.neon.ui.game.enemy.ship.model.** { *; }
-keep class com.tranphuloi.neon.ui.game.ship.ship.Ship { *; }
-keep class com.tranphuloi.neon.ui.game.ship.laser.BulletType { *; }
-keep class com.tranphuloi.neon.ui.game.ship.shape.ShipShape { *; }
-keep class com.tranphuloi.neon.ui.game.spaceObject.SpaceObject* { *; }
-keep class com.tranphuloi.neon.ui.game.stage.Stage* { *; }
-keep class com.tranphuloi.neon.ui.game.common.RepeatTime { *; }
-keep class com.tranphuloi.neon.ui.game.common.Millis { *; }
-keep class com.tranphuloi.neon.ui.game.common.Once { *; }
-keep class com.tranphuloi.neon.ui.game.common.Never { *; }
-keep class com.tranphuloi.neon.ui.game.booster.BoosterType { *; }
-keep class com.tranphuloi.neon.ui.game.booster.BoosterShape { *; }
-keep class com.tranphuloi.neon.ui.game.booster.BoosterRarity { *; }
-keep class com.tranphuloi.neon.ui.game.mode.GameMode { *; }
-keep class com.tranphuloi.neon.ui.game.modifier.RunModifier { *; }

# Settings/persistence enums read by DataStore via `entries.firstOrNull { key }`.
-keep class com.tranphuloi.neon.data.ShipSkin { *; }
-keep class com.tranphuloi.neon.data.Difficulty { *; }
-keep class com.tranphuloi.neon.data.CameraZoom { *; }
-keep class com.tranphuloi.neon.data.ColorBlindMode { *; }
-keep class com.tranphuloi.neon.data.Achievement { *; }
-keep class com.tranphuloi.neon.data.AchievementTier { *; }
-keep class com.tranphuloi.neon.data.RunStats { *; }
-keep class com.tranphuloi.neon.data.RunTelemetrySnapshot { *; }

# Boss rank used by RunStats + MetaProgressionRepository persistence.
-keep class com.tranphuloi.neon.ui.game.controls.BossRank { *; }

# ── Media3 ExoPlayer — reflection-based decoder lookup ────────────────────
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.extractor.** { *; }
-dontwarn androidx.media3.**

# ── DataStore-Preferences — Preferences class uses reflection for keys ────
-keep class androidx.datastore.** { *; }

# ── Compose ────────────────────────────────────────────────────────────────
# The Compose Compiler + Compose AAR consumer-rules already preserve the
# code paths needed for state restoration and runtime. We do NOT add a
# blanket `keep @Composable methods` rule — it negates R8 shrinking across
# every library Composable for no real benefit (audit-10 polish:
# the broad rule was responsible for ~1MB of the recent APK bloat).

# Kotlin reflection (rememberSaveable Saver, runtime type checks).
-keep class kotlin.Metadata { *; }

# Coroutines: kotlinx-coroutines ships its own consumer-rules.pro inside the
# AAR which already covers MainDispatcherFactory + CoroutineExceptionHandler,
# so we don't need to repeat them here.
