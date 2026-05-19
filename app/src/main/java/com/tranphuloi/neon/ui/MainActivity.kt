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
import com.tranphuloi.neon.navigation.MetaUpgrade
import com.tranphuloi.neon.navigation.ModePicker
import com.tranphuloi.neon.navigation.ModifierPicker
import com.tranphuloi.neon.navigation.Settings as SettingsRoute
import com.tranphuloi.neon.navigation.Splash
import com.tranphuloi.neon.ui.dlg.difficulty.DialogDifficultyPicker
import com.tranphuloi.neon.ui.dlg.metaupgrade.DialogMetaUpgrade
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
import com.tranphuloi.neon.ui.game.audio.SfxController
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

        val app = application as App
        Logger.d("MainActivity.onCreate: App cast OK, entering setContent")
        setContent {
            Logger.d("MainActivity.setContent: composing NavHost root")
            CompositionLocalProvider(
                LocalAudioPlayer provides audioHolder,
                LocalHaptic provides haptic,
                LocalSfx provides sfx,
                LocalSettings provides app.settings,
                LocalLeaderboard provides app.leaderboard,
                LocalAchievements provides app.achievements,
                LocalMetaProgression provides app.metaProgression,
                LocalRunPersistence provides app.runPersistence,
                com.tranphuloi.neon.data.LocalRunStats provides remember {
                    androidx.compose.runtime.mutableStateOf<com.tranphuloi.neon.data.RunStats?>(null)
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
                    }
                    LaunchedEffect(vibrationEnabled) {
                        Logger.d("MainActivity: vibrationEnabled changed → $vibrationEnabled")
                        haptic.setEnabled(vibrationEnabled)
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
                            MenuScreen(
                                onPlay = {
                                    Logger.d("Nav: Menu → Game")
                                    navController.navigate(Game.route)
                                },
                                onOpenModePicker = {
                                    Logger.d("Nav: Menu → ModePicker")
                                    navController.navigate(ModePicker.route)
                                },
                                onOpenModifierPicker = {
                                    Logger.d("Nav: Menu → ModifierPicker")
                                    navController.navigate(ModifierPicker.route)
                                },
                                onOpenMetaUpgrade = {
                                    Logger.d("Nav: Menu → MetaUpgrade")
                                    navController.navigate(MetaUpgrade.route)
                                },
                                onOpenSettings = {
                                    Logger.d("Nav: Menu → Settings")
                                    navController.navigate(SettingsRoute.route)
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
                        dialog(
                            route = MetaUpgrade.route,
                            dialogProperties = com.tranphuloi.neon.common.bottomSheetDialogProperties(),
                        ) {
                            DialogMetaUpgrade(onDismiss = {
                                Logger.d("Nav: MetaUpgrade → back")
                                navController.popBackStack()
                            })
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
        Logger.d("MainActivity.onDestroy: releasing SfxController + watching for leaks")
        sfx.release()
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
        super.onDestroy()
    }
}
