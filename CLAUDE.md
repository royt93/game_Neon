# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A 2D vertical-scrolling shoot-'em-up Android game written in Kotlin + Jetpack Compose. The repo is named `neon` (root project + README), but the user-facing app label is set per flavor to **"Sky force U*S*A"** (production) / **"Sky force U*S*A DEV"** (dev). Application namespace / `applicationId` is `com.tranphuloi.neon` (recently migrated from `com.roy93group.neon` — assume any lingering `roy93group` reference outside the populated source tree is stale). Forked/derived from the original Neon by Mario Dujić.

## Build & run

The project uses **flavor dimension `type`** with two flavors: `dev` and `production`. Every assemble/install task must combine flavor + buildType, e.g.:

```bash
./gradlew assembleDevDebug              # debug APK, dev flavor (default for local work)
./gradlew assembleProductionRelease     # release APK, production flavor (minify + shrink on)
./gradlew installDevDebug               # install dev/debug to a connected device
./gradlew clean
./gradlew test                          # what CI runs (.github/workflows/android-ci.yml). JVM unit tests live under app/src/test/.

# Fast verify after any code change (compiles both flavors + runs unit tests):
./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest
```

Toolchain pinned in code (versions live in root `build.gradle`'s `ext { ... }` and `gradle/wrapper/gradle-wrapper.properties` — bump there, not in `app/build.gradle`):

- Kotlin **2.3.20**, AGP **9.1.1**, Gradle wrapper **9.3.1**
- Compose: Kotlin 2.x compose plugin (`org.jetbrains.kotlin.plugin.compose`, applied in `app/build.gradle`) + Compose **BOM `2026.04.01`** — individual Compose artifacts have no version, the BOM aligns them. There is no longer a separate `composeOptions { kotlinCompilerExtensionVersion ... }` block.
- `compileSdk` / `targetSdk` **37**, `minSdk` **24** (raised from 23 — `Configuration.getLocales`, `LocaleList.get`, `ConcurrentHashMap.merge` crash on API 23), source/target/jvmTarget all **JDK 17** (`compileOptions` + `kotlin { compilerOptions { jvmTarget = JVM_17 } }`). CI also uses JDK 17 (Zulu).
- `kotlin.compilerOptions.freeCompilerArgs` includes `-Xannotation-default-target=param-property`. Don't strip it — it preserves Kotlin 1.x annotation-targeting semantics under Kotlin 2.x and the codebase has not been audited for the new defaults.
- `org.gradle.configuration-cache=true` is enabled in `gradle.properties`. New Gradle code (plugins, custom tasks) must be configuration-cache-compatible (no `Project` access at execution time, no `Task.project`, etc.).

Release signing reads credentials from root `keystore.properties` (`storeFile`/`storePassword`/`keyAlias`/`keyPassword`), loaded via `Properties()` in `app/build.gradle`; if that file is absent the `release` signing config is left unset and release builds are simply unsigned (CI/dev still build fine). `keystore.properties` **is** committed (private repo, intentional — see `keystore.properties.template` for the shape) alongside `app/keystore.jks` and `app/private_key.pepk`. `local.properties` is also checked in (only `sdk.dir`).

A separate `:baselineprofile` Gradle module (`com.android.test` + `androidx.baselineprofile` plugin) generates the app's startup baseline profile via `baselineprofile/src/main/java/com/tranphuloi/neon/baselineprofile/BaselineProfileGenerator.kt` (macrobenchmark + UiAutomator, `useConnectedDevices = true` — needs a connected device/emulator, same as Tier 3 tests). `app/build.gradle` wires it with `baselineProfile project(":baselineprofile")`; the generated profile lands in `app/src/main/baseline-prof.txt` and is installed at runtime on API 24-30 via `androidx.profileinstaller` (API 31+ handles it natively). Regenerate with `./gradlew :baselineprofile:generateBaselineProfile` after major changes to app startup/first-frame code paths.

## Flavor-specific resValues

`app/build.gradle` injects ad/SDK config as Android string resources per flavor (no `BuildConfig` fields). The keys `SDK_KEY`, `BANNER`, `INTER`, `EnableAdInter`, `EnableAdBanner` are read as `R.string.*` at runtime. Production has `EnableAdInter`/`EnableAdBanner` = `"true"`, dev has them = `"false"`. There is no ad SDK actually wired in yet — the TODO comments at the top of `App.kt` (firebase, applovin, rate, share, policy) reflect what's still planned.

`App.onCreate()` is **not** empty: it installs a chained `Thread.setDefaultUncaughtExceptionHandler` (preserving any previous handler) and instantiates the three repositories (`SettingsRepository`, `LeaderboardRepository`, `AchievementsRepository`) on `applicationContext`. It also overrides `onTrimMemory` / `onLowMemory` / `onConfigurationChanged` for logging via the `utils/Logger` helper. `Logger` is used pervasively across the codebase (~155+ call sites) — prefer it over `Log.d` for new code.

## Source sets and LeakCanary

Both `app/src/debug/java/com/tranphuloi/neon/utils/LeakWatch.kt` and `app/src/release/java/com/tranphuloi/neon/utils/LeakWatch.kt` exist. The debug variant delegates to `leakcanary.AppWatcher.objectWatcher.expectWeaklyReachable(...)`; the release variant is a no-op. The `LeakWatch.watch(obj, description)` API is the only thing main-set code may call — never reference LeakCanary classes from `main/` directly, or release builds will fail to compile.

## Testing (3 tiers, 828 tests)

- **Tier 1 — JVM unit** (`app/src/test/`, 808 tests): controller correctness + `perf/HotPathPerfTest` (iteration-budget timing). Run `./gradlew testDevDebugUnitTest` — no device. Convention: JUnit4 only, **no mocking framework**, class `XxxControllerBehaviorTest`, backtick English test names, `private fun newController(...)` capturing state via lambda setters, bilingual assert messages.
- **Tier 2 — widget** (`app/src/androidTest/widget/`): isolated Compose UI via `createComposeRule()` (NeonTheme wrapper; provide `LocalSettings` etc. from `App` singleton, don't `new` repos → "multiple DataStores" error). `NeonDialogButton` renders `"$glyph $text"` so match with `substring = true`.
- **Tier 3 — integration** (`app/src/androidTest/integration/`): `PersistenceRoundtripTest` (5 real DataStore repos via `ApplicationProvider`), `NavigationFlowTest` + `GameLoopMultiTickTest` (UiAutomator, locate Menu buttons by `By.res("menu_play"/"menu_settings")` — testTags exposed via `testTagsAsResourceId=true` on the root NavHost; `FlowSupport.findByTag` scrolls if a button is below the fold). Run `./gradlew connectedDevDebugAndroidTest` — **needs a device/emulator** (per R3, list `adb devices` + confirm target first). No Test Orchestrator — it OOM's low-RAM devices (e.g. Galaxy A11); tests are process-shared, `@Before` seeds difficulty + clears checkpoints for a clean Menu. Verified 17/17 ×2 on a real SM-A115F (API 31).

**Android 17 / API 37 gotchas (hard-won — don't regress these):**
- Compose UI tests need **espresso 3.7.0 + androidx.test 1.7.0** (`InputManagerGlobal`). espresso ≤3.6.x throws `NoSuchMethodException: InputManager.getInstance` → breaks *every* Compose test (compose-ui-test calls `Espresso.onIdle()`).
- **Full-activity nav flows use UiAutomator, NOT the compose test rule.** `SplashScreen` navigates inside a `LaunchedEffect`; compose-ui-test dispatches effects off-main → `navController` hits `setCurrentState` off-main → `IllegalStateException`. UiAutomator (`By.text`/`Until`) lets the app run effects on the real main thread. `effectContext = Dispatchers.Main` does **not** fix it (frame-deferring interceptor overrides).
- Integration `@Before` seeds `settings.setDifficulty(...)` (marks `difficultyPicked=true` → skip DifficultyPicker) and calls `App.clearAllCheckpoints()` (see `integration/FlowSupport.kt`) — a checkpoint makes MenuScreen (scale-to-fit, no scroll) clip the play button. Play-button lookup relaunches the activity up to 3× to dodge an adaptive-scale layout race.
- Dev flavor `applicationId` = `com.tranphuloi.neon` (unchanged), but the on-device package label differs; `run-as` may be blocked and `/sdcard` writes are EPERM — dump diagnostics via `Log.i` + `adb logcat`, not files.
- Robolectric is retained **only** for `MetaProgressionShipIntegrationTest` (pure DataStore, no Compose) — Robolectric works fine there; it's abandoned only for Compose UI (AGP 9.1.1 doesn't merge `ui-test-manifest` into the JVM unit-test manifest).
- **`NeonDialogButton` now uses `Modifier.clickable`** (semantics OnClick + ripple + Button role for TalkBack) and gates its infinite `rememberInfiniteTransition` pulse on `ANIMATOR_DURATION_SCALE != 0` (respects the OS "Remove animations" a11y setting). Still don't isolate-widget-test `DialogGamePause`: it's wrapped in `NeonBottomSheet` whose `LaunchedEffect { delay(16); visible = true }` reveal is unreliable under `createComposeRule` on some devices (empty tree / no reveal), independent of the button. It's covered on-device by `GameLoopMultiTickTest` (`back_press…` opens it; `pause_dialog_resume_button_works` taps Resume via `By.res("pause_resume")` — the pause buttons carry `testTag`s and the dialog declares `testTagsAsResourceId` itself since a Dialog window doesn't inherit it from the NavHost root; needs a `device.waitForIdle()` after the dialog opens before locating). `ComboHudWidgetTest` shows the `mainClock.autoAdvance = false` trick for a lone infinite animation — but it breaks `delay()`-based reveals, so it won't help NeonBottomSheet content in `createComposeRule`.

## Feature tracker

`doc/feature.md` is the single source of truth for in-flight features, picks, and implementation status (uses the ✅ / 🟡 / 📋 / 💭 / ❌ / ⏸️ legend from the user's global preferences). When the user picks features via `AskUserQuestion`, move them into this doc rather than re-deriving from chat history.

## Architecture

### Navigation
`MainActivity` lives at `ui/MainActivity.kt` (`ComponentActivity`) and hosts a `NavHost`. Routes are declared as top-level `object`s extending `sealed class Navigation` in `navigation/Navigation.kt`:

`Splash` → `Game` plus dialog-style routes `GamePause`, `GameOver`, `Settings`, `DifficultyPicker` (all rendered as `dialog(...)`, not full screens).

Restart wipes the back stack via `popUpTo(Game.route, inclusive=true) { launchSingleTop = true }` and re-enters `Game.route`. Activity orientation is locked to portrait in the manifest.

`MainActivity` provides three Compose CompositionLocals (`LocalSettings`, `LocalLeaderboard`, plus an achievements equivalent) sourced from the `App` singleton — see `data/AppLocals.kt`. Screens read settings/leaderboard via these locals rather than constructor injection.

### Persistence (`data/`)
DataStore-Preferences-backed repositories, instantiated once in `App.onCreate()`:

- `SettingsRepository` — reduce-motion, music/SFX volume, vibration, `Difficulty` enum (with damage `multiplier`), `ShipSkin` enum, tutorial-shown flag. Exposed as `Flow<…>`.
- `LeaderboardRepository` — all-time and **daily** leaderboards (`dailyEntries(dayKey = todayUtcDayKey())`). Daily key buckets by UTC day.
- `AchievementsRepository` — unlock-once-and-persist for the `Achievement` enum.
- `RunStats` — pure data class summarizing a finished run (not persisted by itself).

Settings reads inside `GameState`/screens go through `LocalSettings.current` and `.<flag>Flow.collectAsState(...)`. Writes happen in suspending repo methods called from `rememberCoroutineScope().launch { ... }` inside UI callbacks.

### Game state (the central pattern)
`ui/game/state/GameState.kt` (~1000 lines) is the heart of the game. `rememberGameState()` is a Composable that:

1. Reads screen size from `LocalConfiguration` and saves it via `rememberSaveable`.
2. Holds **all** game entity lists as Compose `mutableStateOf` / `rememberSaveable` (stars/background, ship, lasers, ultimateLasers, spaceObjects, boosters, enemies, enemyLasers, minerals, explosions, damage numbers, pickup popups, sparks).
3. Constructs one **Controller per domain**, passing each the relevant getters/setters as lambdas. Controllers own mutation logic but never own the state — `GameState` does.
4. Inside a single `DisposableEffect(lifecycle) { ... launch(IO) { while(loopRunning) { ... } } }` block, runs **one paced loop on `Dispatchers.IO`** that drives every system on each iteration via `tinker(...)`. The loop calls `delay(8)` at the bottom (~125Hz cap) — this both yields to the Main thread for Compose recomposition and propagates cancellation. The loop only does real work when `gameStatus == GameStatus.RUNNING`. Lifecycle observation flips `gameStatus` to `PAUSE` on `ON_PAUSE`/`ON_STOP`.
5. Returns a `GameState` data class containing the UI-projected lists (`*UI` types via per-domain `*Mapper` objects) plus input callbacks.

`GameStatus` itself now lives at `ui/game/settings/GameStatus.kt` (not under `state/`).

The Composable's last statement is `refreshHandler` (an unused mutableState mutated each loop iteration). This is **load-bearing** — reading it inside the Composable is what causes recomposition each frame so Compose actually re-renders the `GameWorld`. Don't remove it.

There is also a "kill-cam" mechanism: when the ship is destroyed, `GameScreen` delays navigation to `GAME_OVER` by ~1500ms so the explosion can play out. Search `GameState.kt` for `killCamStartedAtMillis` for the wiring.

### `tinker(id, repeatTime, doWork)` — the scheduler
Defined in `core/Tinker.kt`. A process-wide `mutableMapOf<String, Long>` maps each unique work `id` to its last-run timestamp. `RepeatTime` (`ui/game/common/RepeatTime.kt`) is `Millis(timeMillis) | Once | Never`. The game loop calls `tinker(...)` for ~15+ different IDs per iteration; each fires its `doWork` only when its interval has elapsed. Controllers expose their own `*Id` and `*RepeatTime` properties to drive this. **Important**: every `id` must be globally unique and stable across recompositions — most are generated once via `rememberSaveable { UUID.randomUUID().toString() }` or via constants in the controller. Because the map is module-level, IDs survive Composable recomposition (intentional) but also **leak across activities** if not pruned — relevant when adding new top-level screens.

### Per-entity layout (Booster / Enemy / Laser / Mineral / SpaceObject / Explosion)
Each "world" game entity follows the same shape:

- **Domain model** (`Booster`, `Laser`, `Enemy`, …) — pure data, often `Serializable` so it survives `rememberSaveable`.
- **UI projection** (`BoosterUI`, `LaserUI`, …) — what `GameWorld` consumes (drawableId, offsets, size, rotation, alpha).
- **Mapper** (`BoosterToBoosterUIMapper`, …) — invoked at the bottom of `rememberGameState()` to project the domain list into UI list.
- **Controller** (`BoosterController`, `LasersController`, …) — owns spawn/move/collision logic, exposes `*Id` + `*RepeatTime` constants for the game loop's `tinker` calls.

When adding a new entity type, mirror this five-piece structure and add it to: (a) `rememberGameState()` state list + controller construction, (b) the loop's `tinker` calls, (c) the returned `GameState` data class, (d) `GameScreen` plumbing, (e) `GameWorld` rendering.

### Juice / game-feel subsystems (don't mirror the 5-piece pattern)
Several newer `ui/game/` subpackages are *not* full domain entities — they're presentation/feel layers that piggyback on existing events. Treat them as single-controller helpers, not as new domains:

- `combo/ComboController` — counts/decays combo, drives `ComboHud` + `ComboPopup`.
- `damage/DamageNumber` + its controller — floating damage numbers.
- `pickup/PickupPopup` + its controller — "+score / item picked" floaters.
- `spark/ImpactSpark.kt`, `PickupBurst.kt` — particle bursts on hit / pickup.
- `haptic/HapticController` — centralized vibration; reads `vibrationEnabledFlow` from `SettingsRepository`.
- `hitstop/HitStopController` — micro-pause on heavy hits; gates parts of the loop.
- `background/BackgroundController` + `SpaceBackground(View)` — parallax space background.

The `ui/game/controls/` package is the **HUD/overlay layer**: `BossHpBar`, `BossIntroOverlay`, `BossEntryLightning`, `BossRankOverlay`, `PhaseTransitionBanner`, `WaveClearBanner`, `StageBanner`, `AchievementBanner`, `RevivedBanner`, `TutorialOverlay`, `ComboHud`/`ComboPopup`, `HazardOverlay`, `PowerUpIndicators`, `IndicatorStatus`, `Vignette`, `SmartBombButton`, `ButtonSettings`, `ButtonsMovement`. These are leaf Composables consumed by `GameScreen` — wire new HUD elements here, not in `GameWorld`.

Dialogs live under `ui/dlg/`: `gamepause`, `gameover`, `settings`, `difficulty`. They are NavHost `dialog` destinations, not full screens.

### Stages
`ui/game/stage/Stage.kt` defines the static `stages: List<Stage>` script (`StageMessage` / `StageGame` / `StageBoss` / `StageBreak`). `StageController` advances through them based on elapsed time + a `readyForNextStage` flag (true when no enemies and no space objects remain). `StageGame` carries `enemyType.spawnRate` and `spaceRockSpawnRateMillis` which the loop feeds directly into `tinker` repeat times — i.e. stage difficulty is encoded as `RepeatTime` values.

### Rendering
`ui/game/world/GameWorld.kt` is a single `BoxWithConstraints` that draws every entity by absolute offset from state. High-volume entity lists (lasers, enemies, space objects, boosters, minerals) are **not** drawn via a per-item `Image` in a Composable `forEach` — each has a dedicated batched-drawing Composable in `ui/game/world/` (`LaserCanvas`, `EnemyCanvas`, `SpaceObjectCanvas`, `BoosterCanvas`, `MineralCanvas`) that draws the whole list inside one `Canvas`. Follow this pattern for any new high-cardinality entity type; low-cardinality/one-off visuals (ship, explosion) can stay as plain `Image`/Coil composables. The animated explosion uses **Coil 3.x** (`coil3.compose.rememberAsyncImagePainter` + `coil3.request.ImageRequest`) backed by an `ImageLoader` configured in `ui/game/utils/ImageLoader.kt` with `coil3.gif.AnimatedImageDecoder` / `GifDecoder` to play a GIF (`R.drawable.anim_explosion`). Note: the legacy `coil.compose.rememberImagePainter` API does **not** exist in Coil 3 — use the `coil3.*` packages and `rememberAsyncImagePainter`. The starfield uses `Canvas` with a radial gradient. The shield aura uses `Canvas` + `infiniteRepeatable` color animation between `ShipShieldOne` / `ShipShieldTwo`.

### Audio
`ui/game/audio/AudioPlayer.kt` wraps **AndroidX Media3 ExoPlayer `1.10.0`** (`androidx.media3.exoplayer.ExoPlayer`, not the legacy `com.google.android.exoplayer2` package — that migration is already done) and is driven off `gameStatus` (pauses with the game).

## Conventions to preserve

- **Don't remove the unused `refreshHandler` read** at the end of `rememberGameState()` — see above.
- **Don't introduce new coroutine loops** for new entities. Add them as `tinker` entries inside the existing loop in `GameState.kt`. The single-loop design is intentional: it lets `gameStatus == RUNNING` be the only pause gate, and the loop's `delay(8)` is the only place yielding to Main for recomposition.
- **State lives in `GameState.kt`, not in controllers.** Controllers receive setter lambdas. Replicate this when adding new entity domains.
- **Settings/leaderboard/achievements are accessed via CompositionLocals**, not by re-instantiating the repos. Use `LocalSettings.current` etc.; the singletons live on `App` and are provided once in `MainActivity`.
- The empty package directories under `com/tranphuloi/neon/game/...` (mirroring every subpath of the populated `ui/game/...` tree) are leftover from a refactor — ignore them and add code under `ui/game/...`. Don't "fix" by moving files unless asked. (`com/tranphuloi/neon/{common,core,data,navigation,utils}` at the top level *are* populated — those are real and not leftovers.)
- New game-feel additions (sparks, popups, banners, haptic, hitstop) should follow the "single controller + leaf Composable in `controls/` (HUD) or new package" pattern — **don't** force the 5-piece domain shape onto them.
- LeakCanary is on the `debugImplementation` classpath and wired via the `LeakWatch` source-set split; check it when investigating retention bugs and use `LeakWatch.watch(obj, "description")` from main-set code rather than depending on LeakCanary directly.
- **i18n:** every new user-facing string must be added to both `app/src/main/res/values-vi/strings.xml` and `values-en/strings.xml` (there's also a `values/strings.xml` default and `values-night/` for theme-only overrides — don't confuse the two).
- **Compose stability:** new state data classes (entity domain models, UI projections) should be annotated `@Immutable` or `@Stable` so Compose can skip unnecessary recomposition — the existing entity/UI types follow this.
- **Logger granularity:** `Logger.d(...)` for sparse events (init, lifecycle, stage/boss transitions, achievements); `Logger.v { ... }` (lambda form) for hot-path per-frame/collision/spawn logging, so the string isn't built when verbose logging is disabled.

## Store assets tooling (`store-assets/`)

A separate, self-contained Next.js + ShadCN app for producing App Store / Google Play marketing screenshots — scaffolded by the `app-store-screenshots` skill and unrelated to the Android app's build. Run with `bun install && bun dev` (or npm/pnpm/yarn) from inside `store-assets/`; see `store-assets/README.md` for the editor's persistence model (`app-store-screenshots.json` is the git-tracked canonical project state) and customization points.
