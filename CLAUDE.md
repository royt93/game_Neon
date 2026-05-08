# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A 2D vertical-scrolling shoot-'em-up Android game written in Kotlin + Jetpack Compose. The repo is named `neon` (root project + README), but the user-facing app label is set per flavor to **"Sky force 2024"** (production) / **"Sky force 2024 DEV"** (dev). Application namespace is `com.roy93group.neon`. Forked/derived from the original Neon by Mario Dujić.

## Build & run

The project uses **flavor dimension `type`** with two flavors: `dev` and `production`. Every assemble/install task must combine flavor + buildType, e.g.:

```bash
./gradlew assembleDevDebug              # debug APK, dev flavor (default for local work)
./gradlew assembleProductionRelease     # release APK, production flavor (minify + shrink on)
./gradlew installDevDebug               # install dev/debug to a connected device
./gradlew clean
./gradlew test                          # what CI runs (.github/workflows/android-ci.yml). No tests exist yet.
```

Toolchain pinned in code (do not "modernize" without intent):

- Kotlin **1.6.10**, AGP **8.0.0**, Compose Compiler `1.2.0-alpha02`, Compose libs `1.2.0-alpha02`
- `compileSdk` / `targetSdk` 34, `minSdk` 21, Java 1.8
- CI uses JDK 17 (Zulu) — local JDK should match for Gradle compatibility with AGP 8

The release signing key (`app/keystore.jks`) and `app/private_key.pepk` are checked in. `local.properties` is also checked in (only `sdk.dir`).

## Flavor-specific resValues

`app/build.gradle` injects ad/SDK config as Android string resources per flavor (no `BuildConfig` fields). The keys `SDK_KEY`, `BANNER`, `INTER`, `EnableAdInter`, `EnableAdBanner` are read as `R.string.*` at runtime. Production has `EnableAdInter`/`EnableAdBanner` = `"true"`, dev has them = `"false"`. There is no ad SDK actually wired in yet — `App.onCreate()` is empty and the TODO comments at the top of `App.kt` (firebase, applovin, rate, share, policy) reflect what's still planned.

## Architecture

### Navigation
Single `MainActivity` (`ComponentActivity`) hosts a `NavHost` with three routes declared as `sealed class Navigation` in `navigation/Navigation.kt`:

`Splash` → `Game` → `GamePause` (rendered as a `dialog(...)`, not a full screen).

Restart from the pause dialog uses `popUpTo(navController.graph.id)` to wipe and re-enter `Game.route`. Activity orientation is locked to portrait in the manifest.

### Game state (the central pattern)
`ui/game/state/GameState.kt` is the heart of the game. `rememberGameState()` is a Composable that:

1. Reads screen size from `LocalConfiguration` and saves it via `rememberSaveable`.
2. Holds **all** game entity lists as Compose `mutableStateOf` / `rememberSaveable` (stars, ship, lasers, ultimateLasers, spaceObjects, boosters, enemies, enemyLasers, minerals, explosions).
3. Constructs one **Controller per domain**, passing each the relevant getters/setters as lambdas. Controllers own mutation logic but never own the state — `GameState` does.
4. Inside a single `DisposableEffect(lifecycle) { ... launch(IO) { while(loopRunning) { ... } } }` block, runs **one tight unbounded loop on `Dispatchers.IO`** that drives every system on each iteration via `tinker(...)`. The loop only does work when `gameStatus == GameStatus.RUNNING`. Lifecycle observation flips `gameStatus` to `PAUSE` on `ON_PAUSE`/`ON_STOP`.
5. Returns a `GameState` data class containing the UI-projected lists (`*UI` types via per-domain `*Mapper` objects) plus input callbacks.

The Composable's last statement is `refreshHandler` (an unused mutableState mutated each loop iteration). This is **load-bearing** — reading it inside the Composable is what causes recomposition each frame so Compose actually re-renders the `GameWorld`. Don't remove it.

### `tinker(id, repeatTime, doWork)` — the scheduler
Defined in `core/Tinker.kt`. A process-wide `mutableMapOf<String, Long>` maps each unique work `id` to its last-run timestamp. `RepeatTime` (`ui/game/common/RepeatTime.kt`) is `Millis(timeMillis) | Once | Never`. The game loop calls `tinker(...)` for ~15+ different IDs per iteration; each fires its `doWork` only when its interval has elapsed. Controllers expose their own `*Id` and `*RepeatTime` properties to drive this. **Important**: every `id` must be globally unique and stable across recompositions — most are generated once via `rememberSaveable { UUID.randomUUID().toString() }` or via constants in the controller. Because the map is module-level, IDs survive Composable recomposition (intentional) but also **leak across activities** if not pruned — relevant when adding new top-level screens.

### Per-entity layout (Booster / Enemy / Laser / Mineral / SpaceObject / Explosion)
Each game entity follows the same shape:

- **Domain model** (`Booster`, `Laser`, `Enemy`, …) — pure data, often `Serializable` so it survives `rememberSaveable`.
- **UI projection** (`BoosterUI`, `LaserUI`, …) — what `GameWorld` consumes (drawableId, offsets, size, rotation, alpha).
- **Mapper** (`BoosterToBoosterUIMapper`, …) — invoked at the bottom of `rememberGameState()` to project the domain list into UI list.
- **Controller** (`BoosterController`, `LasersController`, …) — owns spawn/move/collision logic, exposes `*Id` + `*RepeatTime` constants for the game loop's `tinker` calls.

When adding a new entity type, mirror this five-piece structure and add it to: (a) `rememberGameState()` state list + controller construction, (b) the loop's `tinker` calls, (c) the returned `GameState` data class, (d) `GameScreen` plumbing, (e) `GameWorld` rendering.

### Stages
`ui/game/stage/Stage.kt` defines the static `stages: List<Stage>` script (`StageMessage` / `StageGame` / `StageBoss` / `StageBreak`). `StageController` advances through them based on elapsed time + a `readyForNextStage` flag (true when no enemies and no space objects remain). `StageGame` carries `enemyType.spawnRate` and `spaceRockSpawnRateMillis` which the loop feeds directly into `tinker` repeat times — i.e. stage difficulty is encoded as `RepeatTime` values.

### Rendering
`ui/game/world/GameWorld.kt` is a single `BoxWithConstraints` that draws every entity by absolute offset from state. Lasers/ship/enemies/space-objects use `Image(painterResource(...))`. The animated explosion uses **Coil** (`rememberImagePainter` + an `ImageLoader` from `utils/ImageLoader.kt`) to play a GIF (`R.drawable.anim_explosion`). The starfield uses `Canvas` with a radial gradient. The shield aura uses `Canvas` + `infiniteRepeatable` color animation between `ShipShieldOne` / `ShipShieldTwo`.

### Audio
`ui/game/audio/AudioPlayer.kt` wraps **ExoPlayer 2.16.1** and is driven off `gameStatus` (pauses with the game).

## Conventions to preserve

- **Don't remove the unused `refreshHandler` read** at the end of `rememberGameState()` — see above.
- **Don't introduce new coroutine loops** for new entities. Add them as `tinker` entries inside the existing loop in `GameState.kt`. The single-loop design is intentional: it lets `gameStatus == RUNNING` be the only pause gate.
- **State lives in `GameState.kt`, not in controllers.** Controllers receive setter lambdas. Replicate this when adding new entity domains.
- The empty package directories `com/roy93group/neon/{game,common,utils,navigation}/...` parallel to the populated `ui/game/...` paths are leftover from a refactor — ignore them and add code under `ui/game/...`. Don't "fix" by moving files unless asked.
- App.kt's TODO list (firebase, applovin, rate, share, policy) is a roadmap, not a checklist already done — `App.onCreate()` is genuinely empty.
- LeakCanary is on the `debugImplementation` classpath; check it when investigating retention bugs.
