package com.tranphuloi.neon.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.tranphuloi.neon.App
import com.tranphuloi.neon.common.NeonTheme
import com.tranphuloi.neon.data.LocalAchievements
import com.tranphuloi.neon.data.LocalLeaderboard
import com.tranphuloi.neon.data.LocalMetaProgression
import com.tranphuloi.neon.data.LocalRunPersistence
import com.tranphuloi.neon.data.LocalSettings
import com.tranphuloi.neon.navigation.DifficultyPicker
import com.tranphuloi.neon.navigation.Game
import com.tranphuloi.neon.navigation.GameOver
import com.tranphuloi.neon.navigation.GamePause
import com.tranphuloi.neon.navigation.Menu
import com.tranphuloi.neon.navigation.BuffPicker
import com.tranphuloi.neon.navigation.ModePicker
import com.tranphuloi.neon.navigation.ModifierPicker
import com.tranphuloi.neon.navigation.Settings as SettingsRoute
import com.tranphuloi.neon.navigation.Splash
import com.tranphuloi.neon.ui.dlg.difficulty.DialogDifficultyPicker
import com.tranphuloi.neon.ui.dlg.modepicker.DialogModePicker
import com.tranphuloi.neon.ui.dlg.modifierpicker.DialogModifierPicker
import com.tranphuloi.neon.ui.menu.MenuScreen
import com.tranphuloi.neon.ui.dlg.settings.DialogSettings
import com.tranphuloi.neon.ui.dlg.gameover.DialogGameOver
import com.tranphuloi.neon.ui.dlg.gamepause.DialogGamePause
import com.tranphuloi.neon.ui.game.GameScreen
import com.tranphuloi.neon.ui.game.audio.AudioPlayerHolder
import com.tranphuloi.neon.ui.game.audio.LocalAudioPlayer
import com.tranphuloi.neon.ui.game.audio.LocalSfx
import com.tranphuloi.neon.ui.game.audio.LocalVoiceAnnouncer
import com.tranphuloi.neon.ui.game.audio.SfxController
import com.tranphuloi.neon.ui.game.audio.VoiceAnnouncer
import com.tranphuloi.neon.ui.game.audio.Song
import com.tranphuloi.neon.ui.game.haptic.HapticController
import com.tranphuloi.neon.ui.game.haptic.LocalHaptic
import com.tranphuloi.neon.ui.splash.SplashScreen
import com.tranphuloi.neon.utils.LeakWatch
import com.tranphuloi.neon.utils.Logger

class MainActivity : ComponentActivity() {

    private lateinit var audioHolder: AudioPlayerHolder
    private lateinit var haptic: HapticController
    private lateinit var sfx: SfxController
    private lateinit var voiceAnnouncer: VoiceAnnouncer

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.d("MainActivity.onCreate")
        // Install system SplashScreen (Android 12+ native, back-port for older).
        // Must be called before super.onCreate(). Returns a controller for fine-grained
        // keepOnScreen() condition; we let it auto-dismiss on first frame.
        val splash = installSplashScreen()
        Logger.d("MainActivity.onCreate: installSplashScreen returned controller=$splash")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        Logger.d("MainActivity.onCreate: enabling FLAG_KEEP_SCREEN_ON")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Logger.d("MainActivity.onCreate: building AudioPlayerHolder")
        audioHolder = AudioPlayerHolder(applicationContext)
        lifecycle.addObserver(audioHolder)
        val playlist = Song.values().toList().shuffled()
        audioHolder.build(playlist)

        Logger.d("MainActivity.onCreate: building HapticController")
        haptic = HapticController(applicationContext)

        Logger.d("MainActivity.onCreate: building SfxController")
        sfx = SfxController(applicationContext)

        Logger.d("MainActivity.onCreate: building VoiceAnnouncer")
        voiceAnnouncer = VoiceAnnouncer(applicationContext)

        val app = application as App
        Logger.d("MainActivity.onCreate: App cast OK, entering setContent")
        setContent {
            Logger.d("MainActivity.setContent: composing NavHost root")
            // Round 51 (26x Photo mode) — shared flag between GamePause dialog
            // (sets true on "CHỤP ẢNH" tap + pops back) and GameScreen (reads
            // via param + clears after the capture LaunchedEffect runs).
            // mutableStateOf at the Activity-Compose root so it survives the
            // pop-back-to-Game navigation.
            var photoCaptureRequest by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(false)
            }
            // Round 39 — resolve active palette from colorBlindMode setting once at root
            // so every consumer of LocalNeonPalette sees the same value.
            val colorBlindMode by app.settings.colorBlindMode.collectAsState(
                initial = com.tranphuloi.neon.data.ColorBlindMode.NORMAL,
            )
            val palette = when (colorBlindMode) {
                com.tranphuloi.neon.data.ColorBlindMode.NORMAL ->
                    com.tranphuloi.neon.common.NeonPalette.NORMAL
                com.tranphuloi.neon.data.ColorBlindMode.COLORBLIND_SAFE ->
                    com.tranphuloi.neon.common.NeonPalette.COLORBLIND_SAFE
            }
            CompositionLocalProvider(
                LocalAudioPlayer provides audioHolder,
                LocalHaptic provides haptic,
                LocalSfx provides sfx,
                LocalVoiceAnnouncer provides voiceAnnouncer,
                LocalSettings provides app.settings,
                LocalLeaderboard provides app.leaderboard,
                LocalAchievements provides app.achievements,
                LocalMetaProgression provides app.metaProgression,
                LocalRunPersistence provides app.runPersistence,
                com.tranphuloi.neon.common.LocalNeonPalette provides palette,
                com.tranphuloi.neon.data.LocalRunStats provides remember {
                    androidx.compose.runtime.mutableStateOf<com.tranphuloi.neon.data.RunStats?>(null)
                },
                com.tranphuloi.neon.ui.game.buff.LocalActiveBuffs provides remember {
                    androidx.compose.runtime.mutableStateOf<List<com.tranphuloi.neon.ui.game.buff.RunBuff>>(emptyList())
                },
            ) {
                NeonTheme {
                    val navController = rememberNavController()

                    // Wire settings volumes to the actual audio engines.
                    val musicVolume by app.settings.musicVolume.collectAsState(initial = 80)
                    val sfxVolume by app.settings.sfxVolume.collectAsState(initial = 90)
                    val vibrationEnabled by app.settings.vibrationEnabled.collectAsState(initial = true)
                    LaunchedEffect(musicVolume) {
                        Logger.d("MainActivity: musicVolume changed → $musicVolume")
                        audioHolder.setVolume(musicVolume)
                    }
                    LaunchedEffect(sfxVolume) {
                        Logger.d("MainActivity: sfxVolume changed → $sfxVolume")
                        sfx.setVolume(sfxVolume)
                        // Round 62 — TTS volume tracks SFX slider (same family of
                        // gameplay-related sound). No separate user control.
                        voiceAnnouncer.setVolume(sfxVolume)
                    }
                    LaunchedEffect(vibrationEnabled) {
                        Logger.d("MainActivity: vibrationEnabled changed → $vibrationEnabled")
                        haptic.setEnabled(vibrationEnabled)
                    }
                    // Round 62 — voice announcer toggle. Observed at Activity scope
                    // so the setting applies across game/menu/dialog screens.
                    val voiceAnnouncerEnabled by app.settings.voiceAnnouncerEnabled
                        .collectAsState(initial = true)
                    LaunchedEffect(voiceAnnouncerEnabled) {
                        Logger.d("MainActivity: voiceAnnouncerEnabled changed → $voiceAnnouncerEnabled")
                        voiceAnnouncer.setEnabled(voiceAnnouncerEnabled)
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Splash.route
                    ) {
                        composable(route = Splash.route) {
                            SplashScreen(
                                onPickDifficulty = {
                                    Logger.d("Nav: Splash → DifficultyPicker (first run)")
                                    navController.navigate(DifficultyPicker.route)
                                },
                                onStartGame = {
                                    Logger.d("Nav: Splash → Menu (round 25 — was Game)")
                                    with(navController) {
                                        popBackStack()
                                        navigate(Menu.route)
                                    }
                                },
                            )
                        }
                        composable(route = Menu.route) {
                            // Round 68 — auto-skip LoadoutPicker if Settings flag enabled
                            // (default true). Player only sees picker when they explicitly
                            // tap "TRANG BỊ" or turn the setting off.
                            val autoSkipLoadout by app.settings.autoSkipLoadout
                                .collectAsState(initial = true)
                            MenuScreen(
                                onPlay = {
                                    if (autoSkipLoadout) {
                                        Logger.d("Nav: Menu → Game (auto-skip LoadoutPicker)")
                                        navController.navigate(Game.route)
                                    } else {
                                        Logger.d("Nav: Menu → LoadoutPicker")
                                        navController.navigate(com.tranphuloi.neon.navigation.LoadoutPicker.route)
                                    }
                                },
                                onOpenLoadout = {
                                    Logger.d("Nav: Menu → LoadoutPicker (manual)")
                                    navController.navigate(com.tranphuloi.neon.navigation.LoadoutPicker.route)
                                },
                                onOpenModePicker = {
                                    Logger.d("Nav: Menu → ModePicker")
                                    navController.navigate(ModePicker.route)
                                },
                                onOpenModifierPicker = {
                                    Logger.d("Nav: Menu → ModifierPicker")
                                    navController.navigate(ModifierPicker.route)
                                },
                                onOpenSettings = {
                                    Logger.d("Nav: Menu → Settings")
                                    navController.navigate(SettingsRoute.route)
                                },
                                onOpenInfo = {
                                    Logger.d("Nav: Menu → Info (Bách Khoa)")
                                    navController.navigate(com.tranphuloi.neon.navigation.Info.route)
                                },
                                onOpenStats = {
                                    Logger.d("Nav: Menu → Stats")
                                    navController.navigate(com.tranphuloi.neon.navigation.Stats.route)
                                },
                                onOpenShop = {
                                    Logger.d("Nav: Menu → Shop")
                                    navController.navigate(com.tranphuloi.neon.navigation.Shop.route)
                                },
                            )
                        }
                        composable(route = com.tranphuloi.neon.navigation.Info.route) {
                            com.tranphuloi.neon.ui.info.InfoScreen(
                                onBack = {
                                    Logger.d("Nav: Info → back")
                                    navController.popBackStack()
                                },
                            )
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
                                onOpenBuffPicker = {
                                    Logger.d("Nav: Game → BuffPicker (post-boss reward)")
                                    navController.navigate(BuffPicker.route)
                                },
                                photoCaptureRequested = photoCaptureRequest,
                                onPhotoCaptureConsumed = { photoCaptureRequest = false },
                            )
                        }
                        dialog(
                            route = GamePause.route,
                            // Round 28 — bottom sheet dimensions. dismissOnBackPress=true
                            // so back press from pause resumes game (sheet onDismiss=Resume).
                            dialogProperties = com.tranphuloi.neon.common.bottomSheetDialogProperties(
                                dismissOnBackPress = true,
                            ),
                        ) {
                            DialogGamePause(
                                onResumeGame = {
                                    Logger.d("Nav: GamePause → Game (resume via popBackStack)")
                                    navController.popBackStack()
                                },
                                onRestartGame = {
                                    Logger.d("Nav: GamePause → Game (restart, popUpTo Game inclusive)")
                                    navController.navigate(Game.route) {
                                        popUpTo(Game.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onSettings = {
                                    Logger.d("Nav: GamePause → Settings")
                                    navController.navigate(SettingsRoute.route)
                                },
                                onBackToMenu = {
                                    Logger.d("Nav: GamePause → Menu (checkpoint preserved)")
                                    navController.navigate(Menu.route) {
                                        popUpTo(Menu.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onCapturePhoto = {
                                    // Round 51 — pop pause sheet first so it
                                    // doesn't end up in the screenshot, then
                                    // signal GameScreen to run the capture
                                    // flow via the shared flag.
                                    Logger.d("Nav: GamePause → Game (photo capture requested)")
                                    photoCaptureRequest = true
                                    navController.popBackStack()
                                },
                            )
                        }
                        dialog(
                            route = SettingsRoute.route,
                            dialogProperties = com.tranphuloi.neon.common.bottomSheetDialogProperties(),
                        ) {
                            DialogSettings(
                                onDismiss = {
                                    Logger.d("Nav: Settings → back")
                                    navController.popBackStack()
                                },
                            )
                        }
                        dialog(
                            route = DifficultyPicker.route,
                            dialogProperties = com.tranphuloi.neon.common.bottomSheetDialogProperties(),
                        ) {
                            DialogDifficultyPicker(onPicked = {
                                Logger.d("Nav: DifficultyPicker → Menu (first-time onboarding)")
                                navController.navigate(Menu.route) {
                                    popUpTo(Splash.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            })
                        }
                        dialog(
                            route = ModePicker.route,
                            dialogProperties = com.tranphuloi.neon.common.bottomSheetDialogProperties(),
                        ) {
                            DialogModePicker(onPicked = {
                                // Round 25 — was Game restart; now just dismiss.
                                // User picks Play from Menu to apply the new mode.
                                Logger.d("Nav: ModePicker → back (mode saved)")
                                navController.popBackStack()
                            })
                        }
                        dialog(
                            route = ModifierPicker.route,
                            dialogProperties = com.tranphuloi.neon.common.bottomSheetDialogProperties(),
                        ) {
                            DialogModifierPicker(onPicked = {
                                Logger.d("Nav: ModifierPicker → back (modifier saved)")
                                navController.popBackStack()
                            })
                        }
                        // Wave 13a (slice C) — MetaUpgrade route gỡ bỏ; skill-tree
                        // đã chuyển sang Cửa hàng (Shop → tab Nâng cấp).
                        dialog(
                            route = BuffPicker.route,
                            dialogProperties = com.tranphuloi.neon.common.bottomSheetDialogProperties(),
                        ) {
                            val activeBuffs = com.tranphuloi.neon.ui.game.buff.LocalActiveBuffs.current
                            com.tranphuloi.neon.ui.dlg.buffpicker.DialogBuffPicker(
                                onPicked = { picked ->
                                    if (picked != null) {
                                        Logger.d("Nav: BuffPicker → applied ${picked.key} (total buffs: ${activeBuffs.value.size + 1})")
                                        activeBuffs.value = activeBuffs.value + picked
                                    } else {
                                        Logger.d("Nav: BuffPicker → skipped (no buff applied)")
                                    }
                                    navController.popBackStack()
                                },
                            )
                        }
                        // Round 45 (36x) — Loadout picker. PLAY tap → here → Game.
                        // After confirm we replace LoadoutPicker on the back stack so a
                        // back-press from Game returns to Menu, not the picker.
                        dialog(
                            route = com.tranphuloi.neon.navigation.LoadoutPicker.route,
                            dialogProperties = com.tranphuloi.neon.common.bottomSheetDialogProperties(),
                        ) {
                            com.tranphuloi.neon.ui.dlg.loadoutpicker.DialogLoadoutPicker(
                                onConfirm = {
                                    Logger.d("Nav: LoadoutPicker → Game")
                                    navController.navigate(Game.route) {
                                        popUpTo(com.tranphuloi.neon.navigation.LoadoutPicker.route) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                },
                                // Round 72 fix — dismiss chỉ pop back stack về
                                // Menu, KHÔNG navigate Game.
                                onDismiss = {
                                    Logger.d("Nav: LoadoutPicker dismissed → back to Menu")
                                    navController.popBackStack()
                                },
                            )
                        }
                        // Wave 13a (slice D) — ShipPicker route gỡ bỏ; chọn/mua tàu
                        // đã chuyển sang Cửa hàng (Shop → tab Tàu).
                        // Wave 11c — Statistics screen (THỐNG KÊ).
                        composable(route = com.tranphuloi.neon.navigation.Stats.route) {
                            com.tranphuloi.neon.ui.stats.StatsScreen(
                                onBack = {
                                    Logger.d("Nav: Stats → back")
                                    navController.popBackStack()
                                },
                            )
                        }
                        // Wave 12 round 1 — Shop / Economy scaffolding.
                        composable(route = com.tranphuloi.neon.navigation.Shop.route) {
                            com.tranphuloi.neon.ui.shop.ShopScreen(
                                onBack = {
                                    Logger.d("Nav: Shop → back")
                                    navController.popBackStack()
                                },
                            )
                        }
                        dialog(
                            route = "${GameOver.route}/{score}",
                            // Round 28 — bottom sheet sized, but modal-final: back-press +
                            // scrim-tap disabled. User MUST tap CHƠI LẠI / VỀ MENU / ✕ explicitly.
                            // (NeonBottomSheet's `dismissible = false` also gates swipe-down + scrim.)
                            dialogProperties = com.tranphuloi.neon.common.bottomSheetDialogProperties(
                                dismissOnBackPress = false,
                            ),
                        ) { backStackEntry ->
                            val score = backStackEntry.arguments?.getString("score").orEmpty()
                            DialogGameOver(
                                score = score,
                                onRestartGame = {
                                    Logger.d("Nav: GameOver → Game (restart, popUpTo Game inclusive)")
                                    navController.navigate(Game.route) {
                                        popUpTo(Game.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onBackToMenu = {
                                    Logger.d("Nav: GameOver → Menu (checkpoint preserved)")
                                    navController.navigate(Menu.route) {
                                        popUpTo(Menu.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Logger.d("MainActivity.onStart")
    }

    override fun onResume() {
        super.onResume()
        Logger.d("MainActivity.onResume")
    }

    override fun onPause() {
        Logger.d("MainActivity.onPause")
        super.onPause()
    }

    override fun onStop() {
        Logger.d("MainActivity.onStop")
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Logger.d("MainActivity.onWindowFocusChanged hasFocus=$hasFocus")
    }

    override fun onDestroy() {
        Logger.d("MainActivity.onDestroy: releasing SfxController + VoiceAnnouncer + watching for leaks")
        sfx.release()
        // Round 62 — TextToSpeech is a notorious leak source on some OEM TTS
        // engines (holds Context refs internally). Always shutdown() in
        // onDestroy + use LeakWatch to catch regressions. Use applicationContext
        // inside VoiceAnnouncer (not Activity) so the engine can't pin the
        // Activity even if shutdown is racy.
        voiceAnnouncer.release()
        LeakWatch.watch(
            audioHolder,
            "MainActivity.onDestroy → AudioPlayerHolder must be GC'd"
        )
        LeakWatch.watch(
            sfx,
            "MainActivity.onDestroy → SfxController must be GC'd"
        )
        LeakWatch.watch(
            haptic,
            "MainActivity.onDestroy → HapticController must be GC'd"
        )
        LeakWatch.watch(
            voiceAnnouncer,
            "MainActivity.onDestroy → VoiceAnnouncer must be GC'd"
        )
        super.onDestroy()
    }
}
