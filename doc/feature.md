# Feature Tracker — Sky Force U*S*A (Neon)

> **Cách dùng:** mỗi câu là một selector. Đặt `[x]` vào option bạn pick. Option có ⭐ là tối ưu khuyến nghị.
> Sau khi tick, ping AI để triển khai theo wave.

---

## 📊 Status Legend

- ✅ **Implemented** — đã code + build pass + audit
- 🟡 **In progress** — đang làm
- 📋 **Picked** — user đã chọn, chờ triển khai
- 💭 **Idea** — ý tưởng đang thảo luận
- ❌ **Skipped** — user pick `a` skip
- ⏸️ **Deferred** — defer sang session khác (scope quá lớn)

---

# Phần 1 — ✅ Đã triển khai

## 🏗️ Architecture & Foundation

- ✅ DataStore preferences (`SettingsRepository` + `LeaderboardRepository`)
- ✅ `AudioPlayerHolder` activity-scoped (Wb — fix 700ms overlap)
- ✅ `popUpTo(Game.route, inclusive=true)` + `launchSingleTop` (Db)
- ✅ `LeakWatch` source-set switching (debug = LeakCanary, release = no-op) (Yb)
- ✅ `App.UncaughtExceptionHandler` chain
- ✅ `App.onTrimMemory` / `onLowMemory` / `onConfigurationChanged` logging
- ✅ Logger sprinkling toàn flow (~155 calls, 30+ files) (Xa)
- ✅ Game loop trên IO dispatcher + delay(8) pacing
- ✅ Source set: `app/src/{debug,release}/java/...`

## 🎨 Visual / UI

- ✅ Cyberpunk-neon palette (Color.kt: NeonCyan, NeonMagenta, NeonViolet, NeonGold, NeonRedAlert, NeonBgDeep/Mid/Edge)
- ✅ `NeonGlow` modifier (radial gradient drawBehind)
- ✅ Glow effect cho ship/lasers/enemies/boosters/space objects/enemy lasers
- ✅ Splash screen pulse glow + scale animation
- ✅ Splash uses Android 12+ SplashScreen API (Pc)
- ✅ Dialogs revamped: pause/gameover/settings/difficulty với neon border + glow
- ✅ HUD revamped: HP color theo % (cyan/gold/red), mineral icon glow
- ✅ **HP bar visual** (110dp×8dp, fill theo hp/1000, glow scale theo deficit)
- ✅ 5-layer parallax starfield (124 stars) (Ac → 1c)
- ✅ Star spectral colors (white/yellow/blue/red distribution)
- ✅ Star twinkle (near layer 4 only) (Bb)
- ✅ Dust particles (50, foreground filler) (Hb)
- ✅ Nebula blobs (3 drifting) (Eb)
- ✅ Galaxy spiral (top-right corner, slow rotate) (Gb)
- ✅ Comet w/ parabolic trajectory + sparkle trail (6-12s rare) (Dc)
- ✅ Stage tint overlay (Fb)
- ✅ Vignette dark frame
- ✅ Low-HP red pulse warning (softened: alpha 0.10–0.30)
- ✅ Combo popup neon (3-layer Text stack + Canvas halo)
- ✅ Power-up duration indicators (badge + countdown ring + warning flash)

## 🎮 Gameplay

- ✅ Difficulty multiplier (Easy 0.7× / Normal 1.0× / Hard 1.4×)
- ✅ I-frames 600ms after damage (Gb)
- ✅ Mineral magnet với stage-scaled radius (80→100px) (Lc)
- ✅ Combo multiplier với tier popups (Kc)
- ✅ Combo expiry check periodic
- ✅ Tutorial overlay first-time + DataStore flag (Nb)
- ✅ Leaderboard top-10 + show on GameOver (Fc)
- ✅ Settings dialog: volume / vibration / reduce motion / difficulty / skin (Ic)
- ✅ Difficulty picker first-time
- ✅ Rate / Share / Privacy URLs (Qb)
- ✅ Pause dialog: Resume / Restart / Settings buttons (Bb)

## 🎵 Audio

- ✅ ExoPlayer Activity-scoped (3 BGM tracks shuffle)
- ✅ 5 SFX (laser/explosion/damage/pickup/gameover) via SoundPool (Ab)
- ✅ Music + SFX volume wired qua settings flow → AudioPlayerHolder/SfxController

## 🎯 Haptic

- ✅ HapticController (4 patterns: LIGHT_TICK, MEDIUM, HEAVY, LONG)
- ✅ Haptic events: damage / game over / booster pickup / enemy kill / stage advance / combo escalation
- ✅ Vibration toggle in settings

## 🌐 Production / Polish

- ✅ Immersive fullscreen (hide system bars)
- ✅ Keep screen on (FLAG_KEEP_SCREEN_ON)
- ✅ i18n vi + en + locales_config.xml (Tc)
- ✅ ProGuard rules basic (existing)

## 🐛 Bug fixes from audits

- ✅ BUG 1b: DisposableEffect rapid restart (3 lần → 1 lần)
- ✅ BUG: `withFrameNanos` ép Main thread → revert IO dispatcher
- ✅ BUG: music volume slider không có hiệu lực → wired
- ✅ BUG: ConcurrentModificationException sparkles → immutable list pattern
- ✅ BUG: `Color(Long.toULong())` crash → `Color(Long)` proper API
- ✅ BUG: Background không full screen → density multiplier dp→px
- ✅ BUG: Slider spam DataStore.edit → onValueChangeFinished commit
- ✅ BUG: Combo expiry không gọi → periodic check 60 frames
- ✅ PERF: Background lag (user feedback) → reduced star count 124→80, dust 50→25, tick rate 50Hz→25Hz, ~50% giảm draw operations
- ✅ PERF: `LaunchedEffect { while(true) }` chạy mãi dù không có entity (DamageNumbers, PickupPopups, ComboHud, PowerUpIndicators) → short-circuit + auto-stop
- ✅ UX: Boss HP bar overlap player HP → moved to TopEnd + compacted (200dp × 22dp single row, was full-width 60dp tall)
- ✅ BUG: Double haptic on Game Over (LONG + MEDIUM fire cùng lúc) → skip MEDIUM khi gameStatus=GAME_OVER
- ✅ PERF (round 2): Background still lag → galaxy path **cached via remember** (was 72 path ops/frame), nebula brush colors 3→2 stops, layer-4 star halo Brush removed (was 5 allocations/frame), tick 25Hz→15Hz (66ms), galaxy core dùng simple drawCircle thay Brush
- ✅ UX: BossHpBar position 16dp dưới settings icon (top-right) — top=80→108dp
- ✅ UX: BossIntroOverlay redesigned — top banner zone (y=70-160dp) thay full-screen text → không overlap damage numbers/combo popups/pickup popups ở middle/bottom screen
- ✅ PERF (round 3): **Native AndroidView Canvas** thay Compose Canvas cho SpaceBackground. New `SpaceBackgroundView : View` với native `Canvas.drawCircle`/`drawPath`/`Paint` (single Paint reused, native Path cached). Compose entry dùng `AndroidView { factory + update }`. Đạt ~3-5× perf gain (native Skia vs Compose draw scope wrapping)
- ✅ UX: Damage numbers hidden trong 1.5s boss intro → tránh clutter với boss banner ở top
- ✅ PERF (round 4): **Background self-animate ở display rate**. View dùng `postInvalidateOnAnimation()` + delta-time từ `System.nanoTime()` để mutate vị trí entities mỗi frame ~60Hz, decoupled khỏi controller's 30Hz tick. Speeds chuyển từ px/tick sang dp/SECOND (layer 4: 500dp/sec — traverse 891dp screen trong ~2s). Fix complaint "stars nháy vị trí kì quặc không liền mạch, không có cảm giác máy bay đang bay tới". Pause-aware (running flag stops animation loop).
- ✅ PERF (round 4): **Camera zoom smooth via Animatable**. Replace manual `sin((elapsed/300)*PI)` (chỉ sample mỗi recomposition → khựng) bằng `Animatable.animateTo` chain với `tween()` easing → display-refresh-rate interpolation. Cả gameZoom (1.0→1.10→1.0 over 300ms) và killCam (snap→1.5×→hold→settle) dùng cùng pattern.
- ✅ UX (round 5): **Ship spawn cinematic fly-in**. ShipController choreographs 3s entry: fly up (0..800ms cubic ease-out from off-screen-bottom to center), sway 1.5 oscillations (800..2200ms damped sine, ±90px), fly down (2200..3000ms smoothstep to play position). I-frames + spawn check trong `updateHp` absorb damage suốt 3s. Player input ignored during spawn (path drives position).
- ✅ UX (round 5): **Movement buttons trong suốt neon**. Image alpha 0.45 default → 0.95 pressed; always-on outer ring stroke (1.5dp) + subtle ambient halo (alpha 0.18); press boosts ring + halo. User feedback: "trong suốt thấy rõ nhiều view hơn".
- ✅ UX (round 5): **Combo pickup popup simplified**. Was magenta "+1 ×3 = +3" (user confused: "đoạn text màu hồng kì lạ"). Now always gold "+N" for any pickup. Combo info đã có ở ComboHud + ComboPopup, popup pickup chỉ confirm gain count.
- ✅ UX (round 5): **Ship destruction starburst**. onShipDestroyed spawns 5 explosions: large center (1.4× ship size) + 4 diagonal corners (0.7×). "Tan vỡ" feel. Required moving `explosionsController` declaration trước `shipController` trong GameState.kt để callback access được.
- ✅ UX (round 5): **Boss dramatic slide-in**. LevelOneBoss + LevelTwoBoss start `yOffset = -height` (off-screen above), slide down @ 2 px/tick (~400 px/sec) until reaching patrol Y. Combined với BossIntroOverlay's warning banner cho cinematic entrance. Damage check sớm (`if hp<=0 destroyed=true`) trong entry phase tránh chết khi vẫn off-screen.
- ✅ UX (round 6): **Ship spawn smooth + materialize**. Add `spawnAlpha` + `spawnScale` + `spawnRotation` fields to Ship. Phase 1 (fly up): alpha 0→1, scale 0.6→1.0, rotation -8°→0, back-out easing (overshoot ~5%). Phase 2 (sway): banking rotation cos*14° + vertical bob 6px (damped). Phase 3 (fly down): smoothstep + tilt return 0. Single `ship.copy()` per frame. Render via graphicsLayer in GameWorld.
- ✅ UX (round 6): **Pickup popup zoom-in/zoom-out**. Scale curve 0..0.18 punch-in (0→peak), 0.18..0.45 settle (peak→1.0), then hold. peak=1.4 normal, peak=1.6 combo. Combined với existing alpha-fade + float-up.
- ✅ UX (round 6): **Movement buttons pure Compose chevron**. Replaced bitmap drawables (still had purple residue from `button_move_*_purple.png`) with Compose `Path`-drawn chevrons (3-point zigzag, StrokeCap.Round, 7dp width). Fully tinted by `glowColor` (NeonCyan/NeonMagenta). No bitmap deps, no purple bleed-through.
- ✅ UX (round 6): **Camera follow ship (kill-cam)**. graphicsLayer `transformOrigin = TransformOrigin(shipPivotX, shipPivotY)` so zoom pivots on ship's screen-space position. Ship-relative areas don't crop during 1.5× kill-cam zoom. Pivot computed as `(shipX + width/2) / screenWidthDp`.
- ✅ UX (round 6): **Boss entry thrust trail**. Add `isInEntryPhase` to Enemy + EnemyUI. In GameWorld, when boss is in entry, render 4 fading sprite copies above (offset y -30dp each, alpha 0.55→0). Looks like rocket descent motion-blur trail.
- ✅ UX (round 6): **Boss entry lightning bolts**. New `BossEntryLightning` Canvas overlay. 5 zigzag bolts from random screen-edge → boss center, regenerated @ 12fps for crackle. Three-layer stroke: outer NeonCyan glow + mid NeonRedAlert tint + inner white core. Renders only `enemies.filter { isBoss && isInEntryPhase }`.
- ✅ UX (round 7): **Buttons same color (NeonCyan)** — both left/right MovementButton dùng cyan thay vì cyan/magenta khác nhau. Match màu ship.
- ✅ UX (round 7): **Ship banking rotation** — add `bankRotation: Float` to Ship. ShipController lerps toward ±16° (smoothing 0.18) khi `movingLeft`/`movingRight`. Single ship.copy() per frame. GameWorld áp `rotationZ = spawnRotation + bankRotation`.
- ✅ UX (round 7): **Game Over dialog glow revamp**. Replace Card với Box custom: vertical gradient bg, pulsing red border (animated alpha 0.5↔1.0 every 900ms), inner cyan accent border, animated title "GAME OVER" với scale pulse + color shimmer red↔gold, "★ NEW BEST ★" badge khi user beat record, leaderboard panel với current run highlighted gold, dramatic restart pill button (cyan gradient + pulsing halo).
- ✅ UX (round 7): **Magnet visual: remove ring, keep lines** — bỏ pulsing circle quanh ship (user confused "đó là gì?"). Kept connection lines từ minerals đang bị hút (pulse alpha gắn theo same animator).
- ✅ UX (round 7): **Spark particles on hit** — new `ImpactSparkController` + `ImpactSparkOverlay`. Spawn 6 radial sparks at laser hit point, ease-out outward 12-26dp, fade 220ms. Two-layer stroke: outer NeonCyan glow + bright white core. Hooked vào `LasersController.onLaserHit`.
- ✅ UX (round 7): **Enemy knockback** — `Enemy.onObjectImpact` push `yOffset -= 5f` (regular) or `-2.5f` (boss, skip during entry phase). Movement speed naturally returns to trajectory ~150ms.
- ✅ UX (round 7): **Player chromatic aberration on damage** — 240ms full-screen overlay sau lastShipDamageMillis: red gradient từ left edge + cyan gradient từ right edge, both fade by elapsed. Fake RGB channel split (real shader unavailable in Compose).
- ✅ UX (round 7): **Pickup ring shockwave + particle burst** — new `PickupBurstController`. On booster/mineral pickup, spawn 1 burst at pickup point: ring expand 0→60dp ease-out 400ms + 8 radial sparkles travel 28dp. Gold neon. Hooked vào `onBoosterPickedUp(x, y)` (signature changed to include position) + `MineralsController.updateMineralsEarnedTotal` callback.
- ✅ UX (round 7): **Ship glow boost on pickup** — neonGlow intensity 0.5→1.0 over 220ms after latest pickup (booster or mineral). Computed in GameWorld from `lastBoosterPickupMillis`/`lastMineralPickupMillis`.
- ✅ UX (round 7): **HUD stat icon flash** — IndicatorStatus icons scale 1.0→1.3→1.0 + neonGlow flash khi mineralsEarnedTotal/hp changes. Per-stat scale pulse: mineral icon (350ms on lastMineralPickup), HP icon (350ms on lastBoosterPickup). LaunchedEffect chỉ tick khi flash active.
- ✅ UX (round 8): **Camera zoom removed** — bỏ gameZoomScaleAnim + killCamScaleAnim Animatables + finalScale graphicsLayer + TransformOrigin trong GameScreen. Slow-mo (frameCount % 2 skip during kill-cam) vẫn giữ trong GameState.
- ✅ UX (round 8): **Ship engine flame natural** — flame Path rotated theo `ship.bankRotation + spawnRotation` (pivot tại engine attachment point). Reduce length 36→27dp outer, 22→16dp inner. Flame banks theo ship khi user move left/right.
- ✅ UX (round 8): **Center text resize + no-overlap** — ComboPopup 38→24sp, StageBanner 44→30sp regular + 96→64sp countdown, WaveClearBanner 36→24sp. Halo radii also reduced (300→200, 360→240, 280→180dp). Padding offsets: ComboPopup -180dp (above), StageBanner center, WaveClearBanner +180dp (below) → 3 banners không bao giờ overlap.
- ✅ UX (round 8): **Hit ring expand at hit point** — In ImpactSparkOverlay: group sparks by `createdAtMillis`, draw 1 cyan ring per burst (0→40dp ease-out 220ms) + inner white ring 85% radius. Visualize impact zone clearly.
- ✅ UX (round 8): **Spark count 6→16 + travel 25-50dp** — Bigger, brighter spark burst per laser hit. More dramatic feedback.
- ✅ UX (round 8): **Hit-stop micro freeze every hit 80ms** — `HitStopController.freezeForHit()` with cooldown (only fires nếu `now >= unfrozenAtMillis` để rapid hits không compound). Hooked vào `LasersController.onLaserHit` mỗi laser→enemy hit. "Đấm" feel.
- ✅ UX (round 8): **Galaxy spiral → warm nebula blob** — galaxy = null, thêm 4th NebulaBlob ở top-right (gold-orange `0xFFFFB048`, radius 0.45*screenWidth, alpha 0.09, slow pulse). Smooth blob thay vì spiral path lùm xùm.
- ✅ ARCH (round 8): **Common NeonDialog component** — `common/NeonDialog.kt` shared across project. Container: vertical gradient bg, accent border (optional pulse), inner cyan accent border, neon halo, animated title with scale pulse + optional shimmer to secondary color. Slots: title (string), body (Composable), actions (RowScope). Plus `NeonDialogButton` pill button (gradient bg, pulsing border, neon halo, optional leadingGlyph "▶"/"↻"/"⚙"/etc.). Consistent style for all future dialogs.
- ✅ ARCH (round 8): **DialogGameOver + DialogGamePause refactored to NeonDialog**. Both now share visual language. GameOver keeps NEW BEST badge + leaderboard table. GamePause has 3 action buttons: Resume (cyan), Restart (magenta), Settings (cyan). Reduced ~150 lines duplicated styling code total.
- ✅ UX (round 9): **Pause dialog buttons stacked vertical**. NeonDialog.actions slot RowScope→ColumnScope, NeonDialogButton fillMaxWidth + bigger padding. Resume/Restart/Settings stack vertically — fixes button cropping on small screens.
- ✅ UX (round 9): **Ship symmetric L/R bounds**. leftLimit `-width/4` matches rightLimit `screenWidth - width*0.75` (was `width/1.5` asymmetric ~8px diff). Movement feels even.
- ✅ UX (round 9): **Ship engine flame longer + 3-layer**. Lengths 50/35/22dp (was 27/16) outer cyan halo + mid gold + bright white core. Deeper flicker (0.65-1.35 vs 0.7-1.3). Banks with ship rotation. Natural taper.
- ✅ UX (round 9): **Smooth enemy knockback velocity decay**. Replace instant `yOffset -= 5f` với `knockbackVel` field accumulating per hit (capped at -3f), decays 15%/tick (~100ms recovery). Smoother feel. Applied to RegularEnemy + LevelOneBoss + LevelTwoBoss (boss magnitude smaller -0.7f).
- ✅ UX (round 9): **Enemy HP bar smaller + auto-hide at full HP**. Height 5→3dp, width 100%→70% enemy width, centered above. Hidden when `currentHp >= initialHp` (no clutter on undamaged enemies).
- ✅ UX (round 9): **Background reduce + slow**. Stars 80→53 (counts halved per layer), speeds halved (L0:30→15, L4:500→250 dp/sec). Dust 25→15, speed 250-450→125-225. Less visual noise per user feedback.
- ✅ UX (round 9): **Camera zoom removed**. All gameZoom + killCam Animatables + graphicsLayer scale + TransformOrigin removed. Slow-mo (frame skip) trong GameState giữ nguyên cho kill-cam timing.
- ✅ UX (round 9): **Ship destroy implosion → flash → BANG**. 3-phase choreography: phase 1 (0-200ms) ship scale 1→0.3 alpha 1→0.7 (suck-in via graphicsLayer), phase 2 (200-300ms) full-screen white flash (triangular pulse), phase 3 (300ms+) 5 explosions starburst (delayed via coroutineScope.launch). Add `destroyedAtMillis` to Ship, set on hp→0.
- ✅ UX (round 9): **Center text resize + no-overlap**. ComboPopup 38→24sp, StageBanner 44→30/96→64sp, WaveClearBanner 36→24sp. Padding offsets ±180dp (above/below center) prevent overlap.
- ✅ UX (round 9): **Hit ring + spark count + hit-stop**. Hit ring 0→40dp expand at hit point. Spark count 6→16, travel 25-50dp. HitStop micro freeze 80ms per hit (cooldown anti-compound).
- ✅ UX (round 9): **Galaxy spiral → warm nebula**. Top-right gold-orange `0xFFFFB048` blob replaces galaxy spiral path.
- ✅ PERF (round 10): **Hit-stop lag fix**. Round 9 added `freezeForHit()` 80ms per laser hit — with triple-laser firing 3 lasers × 5+ Hz, freeze cumulated → game effectively pause-spammed → user perceived as lag. Reduced to 30ms freeze + rate cap 250ms (max 1 freeze per 250ms window). Snappier feel + still preserves "đấm" feedback on first hit of a burst.
- ✅ BUG (round 10): **Ship rotation pivot drift**. graphicsLayer rotationZ was on outer Box (`size(shieldSize.dp)` = 180dp) but Image at top-left default → rotation center ≠ Image center → bank LEFT/RIGHT made ship visually drift. Fix: rotationZ moved to Image directly (Image-center pivot via default transformOrigin 0.5/0.5). Outer Box keeps alpha + scale only. Bank now rotates in-place correctly.
- ✅ UX (round 10): **Ship flame meteor streak**. Cone lengths 50→100/35→70/22→45dp + add 6-dot ember trail extending past cone tip (12dp spacing, alpha fade, gold halo + white core). Tapered "vệt sao băng" feel.
- ✅ BUG (round 11): **Ship stuck at center after Activity recreate**. Config change (theme/locale switch) recreates Activity. `Ship.yOffset` rememberSaveable preserves mid-spawn-anim position (e.g. y=312 sway phase), but `ShipController.spawnStartMillis` resets fresh (just `remember`, not saveable). When user pauses → comes back after `spawnTotalMillis` (3s) elapses on the new controller's clock, spawn anim is bypassed; legacy `if (newY > maxYOffset) newY -= movementSpeed` only handles upward settle. Ship stuck above play position. Fix: bi-directional settle in `moveShip` — coerce toward `maxYOffset` from either side (`coerceAtLeast`/`coerceAtMost`).

## 🆕 Wave 3 implementation (round 12 — gameplay depth)

- ✅ **24b Boss kill rank S/A/B/C/D**. Snapshot `bossSpawnedAtMillis` + `playerHpAtBossSpawn` when StageBoss begins (in `StageController.onStageAdvance` callback). On boss kill compute `BossRank.compute(timeToKillMs, hpRatio)`: S = perfect (no damage + <30s), A = excellent (<45s or no damage), B = good (<60s), C = OK (<90s), D = poor. New `BossRankOverlay` 1.8s composable: 3-layer giant letter + label + radial halo + scale pop-in. Center anchor zIndex 445.
- ✅ **15c Stats screen breakdown**. New `data/RunStats.kt` data class (score, timeSec, enemiesKilled, bossesDefeated, maxCombo, stagesReached) + `LocalRunStats` CompositionLocal provided at MainActivity. Counters tracked in GameState (`enemiesKilledTotal`, `bossesDefeatedTotal`, `maxComboReached` updated in `onEnemyKilled`). GameScreen writes RunStats on GAME_OVER before navigation. DialogGameOver reads via `LocalRunStats.current.value` and renders new `StatsPanel` body section above leaderboard.
- ✅ **20b Smart bomb stack**. `smartBombs: Int` rememberSaveable starting at 2 (+1 per boss kill). New `SmartBombButton` composable — circular badge w/ 💣 emoji + count badge bottom-right, neon gold/red glow when usable. `dispatchSmartBomb` callback in GameState: marks all on-screen enemies hp=0 (process picks up + spawns explosion + minerals each), clears all enemyLasers, triggers screen feedback. Anchored bottom-end above movement buttons.
- ✅ **19b Charge shot (hold both arrows)** — *initial impl, replaced in round 13*. ShipController tracked `bothPressedSinceMillis` via `movingLeft`/`movingRight` setter overrides. Held both ≥ 1500ms fired ultimate.

## 🔧 Wave 3 fixes (round 13 — feedback iteration)

- ✅ **Smart bomb position fix**. Was `align(BottomEnd) padding(end=16dp, bottom=110dp)` overlapping right movement button on small screens. Moved to `align(BottomCenter) padding(bottom=32dp)` — between L/R movement buttons, ergonomic thumb-reach center.
- ✅ **19b Charge shot redesign → auto-charge no-damage**. Old hold-both-arrows mechanic was illogical: (a) ship sat still (movements cancelled), (b) bank rotation only -16° due to `when` order, (c) no UI hint for discovery, (d) input overload. **New mechanic**: ShipController tracks `chargeStartMillis`. Every `CHARGE_FILL_MS` (8s) without taking damage, `consumeChargeShot()` returns true → fire ultimate laser. `resetCharge()` called in `updateHp` on any damage taken. Rewards defensive play. Visual ramp via existing `chargeProgress` field: ship neonGlow intensity + radius scale up as charge fills. No new UI needed — purely time-based + glow feedback.
- ✅ BUG (round 13): **Laser misalignment with ship**. Ship lasers used `align(BottomStart) + absoluteOffset(y = -ship.height/2)` while ship sprite uses default `offset` (TopStart). Two coord systems → laser visually disconnected from ship's gun. Fix: shipLasers + ultimateLasers rendering switched to `.offset` (TopStart) like enemyLasers + ship. `fireLasers` now sets `laser.yOffset = ship.yOffset - laser.height` so laser bottom edge is flush with ship top edge. `processShipLasers` off-screen threshold updated `< -screenHeight` → `< -100f` (consistent with positive-y-decreasing-to-negative scroll).
- ✅ BUG (round 14): **Smart bomb position + count badge invisible**. Was `BottomCenter padding=32dp` overlapping ship sprite. Moved to `TopEnd padding(top=168dp, end=12dp)` (below BossHpBar, free space). Count badge was hidden because outer Box used `.clip(CircleShape)` which clipped corner-positioned badge. Restructured: 60dp wrapper Box (no clip) → 48dp inner clipped circle button + 20dp count badge as sibling at BottomEnd corner.
- ✅ UX (round 14): **Status banner exclusive priority**. Multiple banners at center (StageBanner + BossRankOverlay + ComboPopup + WaveClearBanner) overlapped when firing simultaneously. Added priority gating in GameScreen: `bossRankActive` (1.8s window) suppresses all 4 lower banners; `bossIntroActive` (1.5s window) suppresses StageBanner + ComboPopup. Single visible banner at any time → clean UI.
- ✅ BUG (round 14): **Ship destroy still shows flame + magnet**. ShipEngineFlame + MagnetVisual render independently of the ship Box (ship.alpha=0 trick on Box doesn't reach them). After implosion 200ms, ship sprite is gone but flame + magnet still drew → ghost UI. Fix: gate both composables with `shipAlive = ship.destroyedAtMillis == 0L || (now - ship.destroyedAtMillis) < 200L`.
- ✅ UX (round 14): **Ship flame teardrop shape**. Old triangle path (wide-top, point-bottom) was unnatural — flame shouldn't be widest where it attaches to engine. Reshaped to teardrop 5-point polygon: narrow top (3-8dp depending on layer), bulge at 32% length, taper to point at tail. Per layer top widths: outer 4dp / mid 2.5dp / core 1.5dp; bulge widths 18 / 11 / 5.5dp. Visually emerges from a small thruster nozzle.
- ✅ UX (round 14): **Impact spark on enemy laser → ship**. Previously only ship-laser → enemy spawned sparks (via `onLaserHit` → `impactSparkController.spawnBurst`). Reverse direction had no visual feedback — only chromatic aberration + screen shake. Added: `onShipDamaged` callback in GameState now spawns burst at ship center. Required moving `impactSparkController` declaration above shipController so the callback compiles.
- ✅ UX (round 15): **Smart bomb above right (>) movement button**. Was `TopEnd padding(top=168dp)` (corner area, far from thumb). Now `BottomEnd padding(end=30dp, bottom=156dp)`: smart bomb 60dp wrapper sits 32dp above right movement button (button at bottom 124dp + 32dp gap = wrapper bottom 156dp). End padding 30dp centers the 60dp wrapper visually with the 80dp button (both centers aligned).
- ✅ BUG (round 15): **Ship destroy alpha=0 unreliable**. graphicsLayer alpha=0 on outer Box was supposed to hide ship sprite + shield + glow after implosion, but on some devices/configs the ship UI persisted post-destroy. Fix: wrap entire ship Box in `if (shipAlive)` so it's REMOVED FROM COMPOSITION after 200ms (was relying on alpha=0). Same shipAlive gate already protects flame + magnet (round 14).
- ✅ UX (round 15): **Ship flame natural variation**. Added per-layer flicker phases (outer/mid/core staggered ±90-100°), lateral sway via `cos(flicker)` on bulge x-coords (4dp/2.5dp/1.2dp per layer), per-layer bulge width flicker `1±0.15·sin`. Tip also drifts with sway*0.5 → flame "bends" organically while flickering. Less mechanical, more "real" feel.
- ✅ UX (round 15): **Stronger knockback + bigger hit ring**. Knockback impulse -1.5/-3 → -3/-6 (regular enemy), -0.7/-1.5 → -1.5/-3 (boss). Decay 0.85 → 0.92 → ~200ms recovery (was 100ms, too brief). Hit ring radius 40 → 60dp, duration 220 → 320ms, stroke 2.5 → 4dp + alpha 0.65 → 0.85. Collision feedback now unmistakable.
- 🚨 BUG (round 16): **Round 13 collision regression — laser passes through enemies**. Round 13 switched lasers to TopStart-anchor `.offset` rendering, BUT `monitorLaserCollision` still computed laser rect Y as `laser.yOffset + screenHeight - laser.height` (the OLD BottomStart→TopStart conversion). After the round-13 native-TopStart change, this conversion pushed laserRect off-screen → ship lasers (-25 dmg) never overlapped enemy rects → **all kills in recent logs were -1000 from ultimate** (which got "lucky" hits as its yOffset went very negative). Fix: use raw `laser.yOffset` directly. Bonus: updated `fireUltimateLaser` initial yOffset to `screenHeight` (start at bottom, sweep up — was `-10` which now means "barely above top, immediately off-screen"). Added Logger to `ImpactSparkController.spawnBurst` to verify firing.
- ✅ BUG (round 16): **Ship destroy stale `shipAlive` computation**. Root cause: after `ship.destroyedAtMillis` is set, ship state stops mutating → GameWorld stops recomposing → `shipAlive = (now - destroyedAtMillis) < 200L` reads stale `now` from last composition → forever stays `true` → ship Box never removed. Fix: `LaunchedEffect(ship.destroyedAtMillis)` ticks `destroyTickMillis` state every 33ms for 2s after destroy, forcing recompose so `shipAlive` flips false at 200ms threshold.
- ✅ BUG (round 17): **Ship destroy STILL not hiding after round-16 tick fix**. Round-16's `destroyTickMillis` tick approach was unreliable — Compose can smart-skip GameWorld recomposition if no observable input changes, and reading a local mutated long inside a Composable doesn't always survive smart-skip on every device/build. Logs showed `Ship destroyed` event firing but ship sprite + flame + magnet kept rendering intact. **Definitive fix**: added explicit `shipSpriteHidden: Boolean = false` field to `Ship` data class. `GameState.onShipDestroyed` launches a coroutine that runs `delay(200L)` then `ship = ship.copy(shipSpriteHidden = true)` — mutating the Ship reference itself guarantees Compose recomposes GameWorld with the new value. `shipAlive = !ship.shipSpriteHidden` (was time-based). Kept `destroyTickMillis` tick (now 8 × 25ms = 200ms exactly) but ONLY for implosion scale/alpha interpolation, not visibility gating. Added comprehensive Logger.d at every state mutation + LaunchedEffect entry/exit + recompose for diagnosis.
- ✅ BUG (round 18): **Ship destroy: shipSpriteHidden RESET back to false 16ms after being set**. After round-17 fix, log proved state mutation happened correctly: `Ship sprite hidden via state flag` at +200ms, GameWorld recomposed `shipSpriteHidden=true (recompose triggered)` at +207ms, but **16ms later** GameWorld recomposed AGAIN with `shipSpriteHidden=false`. Root cause: kill-cam keeps the IO loop running (slow-mo), and `ShipController.moveShip()` continues firing — its internal cached `ship` reference is stale (`shipSpriteHidden=false`), and when it calls `setShip(ship.copy(xOffset = newX))`, the controller's stale-state copy overwrites our flag. Same pattern for `monitorShipCollisions` (also calls setShip on damage). **Fix**: gate both `shipController.moveShip` + `shipController.monitorShipCollisions` tinker calls on `ship.destroyedAtMillis == 0L` so they stop firing once destruction begins. Other entity tinkers (enemies, lasers, explosions) continue to run during kill-cam.
- ✅ UX (round 18): **Pháo hoa nổ tung + tia điện** at every laser hit. Existing 16-spark radial burst + cyan ring lacked dramatic punch. Added 3 layers to `ImpactSparkOverlay`: (a) **core flash** — bright filled white→cyan radial gradient circle 14dp that pops in first 30% of life then fades, (b) **6 electric zigzag arcs** — each is a 4-segment polyline radiating outward with perpendicular jitter (deterministic from burst hash for stable shape), drawn with cyan glow halo + white bright core, fades over first 60% of life, (c) **gold outer firework ring** at 1.15× hit-ring radius for warm contrast. Total visual = 16 sparks + ring + white-inner-ring + gold-outer-ring + core-flash + 6 zigzag bolts = unmistakable "fireworks burst" feedback per hit.
- ✅ UX (round 16): **Smart bomb 30% smaller + 8dp from edge**. Wrapper 60→42dp, inner 48→34dp, badge 20→14dp, fonts proportionally scaled. Position: `padding(end=8dp, bottom=156dp)` — sits directly above right movement button, sticks 8dp to right edge per user spec.
- ✅ UX (round 16): **Flame rounded with cubic bezier**. 5-point polygon teardrop had visible corners ("thô, nhiều góc cạnh"). Replaced with two cubic beziers per layer: top-left → S-curve down through bulge → tip → S-curve up through bulge → top-right. Smooth, organic, "mềm mại" shape with same teardrop silhouette + lateral sway.

## 🆕 Wave 1 implemented (this round)

### Files mới

- `data/AchievementsRepository.kt` + `LocalAchievements`
- `ui/game/damage/DamageNumber.kt` (entity + controller)
- `ui/game/pickup/PickupPopup.kt` (entity + controller)
- `ui/game/hitstop/HitStopController.kt`
- `ui/game/controls/ComboHud.kt`
- `ui/game/controls/BossHpBar.kt`
- `ui/game/controls/AchievementBanner.kt`
- `ui/game/world/ExplosionBurstOverlay.kt`
- `ui/game/world/DamageNumbersOverlay.kt`
- `ui/game/world/PickupPopupOverlay.kt`
- `ui/game/world/MagnetVisual.kt`

### Files modified

- `Enemy.kt` + 3 impls (RegularEnemy, LevelOneBoss, LevelTwoBoss): thêm `isBoss` + `displayName`
- `EnemyUI.kt` + mapper: thêm `isBoss`, `displayName`, `currentHp`, `initialHp`
- `GameState.kt`: 5 controllers mới + 7 fields mới trong `GameState` data class
- `LasersController.kt`: `onLaserHit` callback
- `GameWorld.kt`: render 3 overlay layers + magnet visual + pass damage/pickup args
- `GameScreen.kt`: BossHpBar + AchievementBanner + boss kill flash overlay
- `IndicatorStatus.kt`: ComboHud integration
- `App.kt`: AchievementsRepository init
- `MainActivity.kt`: LocalAchievements provider

---

# Phần 2 — 📋 Đã pick, chờ triển khai (Wave 11-17)

## Round 4 picks (11c-17c)

### Round 4 picks (11c-17c)


| Code | Feature | Detail |
| --- | --- | --- |
| 11c | Kill-cam slow-mo replay | Slow-mo zoom 50% × 1.5s + cinematic camera bounce + screen shake trước GameOver |
| 12c | Wave clear bonus | 5 mineral burst + "WAVE CLEAR! +5" banner khi stage clear |
| 13c | Star explosion enemy | Radial burst lines (8-12 tia) + ring shockwave 0→100px |
| 14c | Auto-revive token | Booster `REVIVE` rare 5%, respawn HP=300, 1×/game + "REVIVED!" banner + 1.5s i-frames |
| 15c | Statistics breakdown | Stats by stage (boss reached, mineral by type) trên GameOver |
| 16c | Magnet visual | Vòng tròn pulse around ship + line từ mineral về ship khi đang hút |
| 17c | Daily challenge | Daily seed + leaderboard riêng + daily reward (multiplier/skin unlock) |

### Round 5 picks (Q31-48 batch 1-4 via AskUserQuestion)


| Code | Feature | Detail |
| --- | --- | --- |
| 31d | +100 stages, 5 chapters | Asteroid Belt → Nebula Cloud → Ice Planet → Hostile Station → Galaxy Core |
| 32d | Chapter themes + hazards | Visual palettes + unique enemies + asteroid storm / nebula fog / ice patches |
| 33c+d | Mid-bosses every 5 stages | 3 types (Offensive/Defensive/Swarm) + phase transition khi HP <50% |
| 34d | Final boss multi-phase | 3-phase 5000→7500→10000 HP + cinematic intro/outro + alt endings |
| 35d | +10 player bullet types | Spread/Homing/Piercing/Plasma/Beam/Wave/Lightning/Cluster/Mine/Drone |
| 36d | Loadout + upgrade + dual-wield | Pre-game pick 2 weapons + level up via use + combo effects |
| 37c | Enemy bullet patterns | Spread fan / aimed / sine wave per enemy type |
| 38d | +10 support items | Speed/Magnet+/Score×2/TimeSlow/Invincibility/HOT/Drone/Bomb/Missile/Mine |
| 39c | Item rarity tiers + visual | Common 60%/Rare 30%/Legendary 10% + color border + particle aura |
| 40c | Stack + combo synergies | 2× shield = 20s + shield+laser = "Aegis Mode" |
| 41c | Status effects + chains | Burn/Slow/Stun + frozen+laser = shatter AoE |
| 42c | Roguelike buffs + curses | Boss kill = pick 1/3 buffs + curses for stronger buffs |
| 43d | All game modes | Survival + Time Attack + Boss Rush (+ Daily 17c) |
| 19c | Charge shot 3 tiers | 0.4s/0.8s/1.5s = 2×/5×/15× damage |
| 29c | Secondary weapon + Beam | Long-press swap Laser ↔ Spread ↔ Beam (drain energy) |
| 48c | 15-node skill tree | Branching upgrade tree, spend lifetime minerals |


### Round 6 picks (Q18-30 game feel batch 5-7)


| Code | Feature | Detail |
| --- | --- | --- |
| 18c | Hit pause + boss flash | 60ms freeze enemy kill, 120ms + screen flash boss kill |
| 20c | Smart bomb + shockwave | BOMB booster max 3 stack, double-tap Settings = clear screen + radial expand |
| 21c | Boss intro full | Zoom 1.5s + name banner + warning border pulse + alarm SFX + audio sting |
| 24c | Boss kill rank + multiplier | S/A/B/C theo time-to-kill + score ×3/×2/×1.5 |
| 23c | Endless + leaderboard | Sau End → endless (×1.05/wave) + leaderboard riêng track survival time |
| 25c | Random modifiers (player choice) | Mỗi game start, chọn 1/3 modifiers (Triple speed / Half HP+2× dmg / etc) |
| 26c | Photo mode + filter + share | Pause → ẩn HUD → screenshot + filter neon/muted + share intent |
| 27c | A11y profiles + contrast + text | Color blind palettes + high contrast + larger text option |
| 28c | Procedural patterns + custom | Mix ZigZag/Row/cluster + V-formation / sine wave / custom |
| 30c | Camera zoom + shake + chromatic | Zoom 1.1× × 300ms khi boss/damage/kill + shake + chromatic aberration |
| 45d | Ship full customization | Skin + tint + trail + glow + weapon loadout + module slot |
| 46c | Achievements tiered | +20 achievements với Bronze/Silver/Gold tiers |
| 47d | Story full | Boss dialogue + chapter intros + epilogue cinematic |


### Implementation strategy picks (batch 8 meta)


| Code | Decision |
| --- | --- |
| Wave order | "làm tất cả trong 1 prompt" (user explicit) — but realistic ~50+ features will need multiple sessions, will checkpoint after each wave |
| Build cadence | Build verify (debug + release) + audit memory leak SAU MỖI WAVE |
| Wave 7 deferred | Có, sau khi xong tất cả features (Vb Hilt + Ub ViewModel + Rb Material 3 + Mc per-stage BGM + Zc ProGuard + AAc/CCc benchmark) |

---

# Phần 3 — 📋 Câu hỏi mới chờ pick (31-48)

> **Hướng dẫn pick:** thay `[ ]` thành `[x]` ở option muốn chọn. Mỗi câu pick 1 option.

## 🌌 Section A: STAGES & PROGRESSION

### Câu 31: Tổng số stages (hiện ~30)

- [ ] **a)** Giữ ~30
- [ ] **b)** +20 → 50 stages
- [ ] **c)** +50 → 80 stages
- [ ] **d)** ⭐ +100 stages chia 5 chapters (Asteroid Belt → Nebula Cloud → Ice Planet → Hostile Station → Galaxy Core)

### Câu 32: Chapter themes

- [ ] **a)** Skip
- [ ] **b)** Visual theme: mỗi chapter đổi background palette
- [ ] **c)** (b) + unique enemy types per chapter
- [ ] **d)** ⭐ (c) + stage hazards (asteroid storm / nebula fog / ice trượt)

### Câu 33: Mid-bosses

- [ ] **a)** Skip
- [ ] **b)** Mid-boss mỗi 10 stages, HP 1500
- [ ] **c)** ⭐ Mỗi 5 stages, 3 types (Offensive/Defensive/Swarm)
- [ ] **d)** (c) + phase transitions khi HP <50%

### Câu 34: Final boss multi-phase

- [ ] **a)** Skip
- [ ] **b)** Final Boss 1-phase HP 5000
- [ ] **c)** ⭐ 3-phase (5000→7500→10000 HP)
- [ ] **d)** (c) + cinematic intro/outro + alternative endings theo difficulty

## ⚡ Section B: BULLETS / WEAPONS

### Câu 35: Player bullet types (hiện 4)

- [ ] **a)** Skip
- [ ] **b)** +3 (Spread / Homing / Piercing)
- [ ] **c)** +6 (b) + (Plasma orb / Beam / Ricochet)
- [ ] **d)** ⭐ +10 đầy đủ (Spread / Homing / Piercing / Plasma / Beam / Wave / Lightning chain / Cluster bomb / Mine / Drone orbit)

### Câu 36: Weapon activation

- [ ] **a)** Booster pickup random
- [ ] **b)** ⭐ Loadout pre-game: chọn 2 weapons, swap bằng long-press Settings
- [ ] **c)** (b) + weapon upgrade system (level up via use)
- [ ] **d)** (c) + dual-wield combination effects

### Câu 37: Enemy bullet patterns

- [ ] **a)** Skip — single shot
- [ ] **b)** Aimed shot
- [ ] **c)** ⭐ Mix theo enemy type: spread fan / aimed / sine wave
- [ ] **d)** Full bullet hell cho boss (spiral / ring / dense walls)

## 💎 Section C: SUPPORT ITEMS

### Câu 38: Số lượng items (hiện 5)

- [ ] **a)** Skip
- [ ] **b)** +3 (Speed / Magnet+ / Score×2)
- [ ] **c)** +6 (b) + (TimeSlow / Invincibility / HealOverTime)
- [ ] **d)** ⭐ +10 (b) + (TimeSlow / Invincibility / HOT / Drone companion / Bomb stack / Missile pack / Mine pack)

### Câu 39: Item rarity tiers

- [ ] **a)** Flat random
- [ ] **b)** 3 tiers (Common 60% / Rare 30% / Legendary 10%)
- [ ] **c)** ⭐ (b) + visual indicators (color border + particle aura)
- [ ] **d)** (c) + chest drop system (boss kill = guaranteed Legendary)

### Câu 40: Item stacking / combo

- [ ] **a)** Skip
- [ ] **b)** Stack same item (2× shield = 20s)
- [ ] **c)** ⭐ (b) + combo synergies (shield+laser = "Aegis Mode")
- [ ] **d)** (c) + trio combos (3 items đặc biệt)

## 🎮 Section D: GAMEPLAY DEPTH

### Câu 41: Status effects on enemies

- [ ] **a)** Skip
- [ ] **b)** 3 effects (Burn DoT / Slow / Stun)
- [ ] **c)** ⭐ (b) + chain reactions (frozen + laser = shatter AoE)
- [ ] **d)** (c) + element matchup (Fire>Ice / Ice>Plasma...)

### Câu 42: Roguelike buffs

- [ ] **a)** Skip
- [ ] **b)** Sau mỗi boss kill, chọn 1 trong 3 buffs
- [ ] **c)** ⭐ (b) + curses for stronger buffs (risk-reward)
- [ ] **d)** (c) + relic items (permanent passive bonus)

### Câu 43: Alternative game modes

- [ ] **a)** Story only
- [ ] **b)** +Survival (infinite waves)
- [ ] **c)** (b) + Time Attack
- [ ] **d)** ⭐ (c) + Boss Rush + Daily Challenge (đã pick 17c)

### Câu 44: Environmental hazards

- [ ] **a)** Skip
- [ ] **b)** Asteroid showers
- [ ] **c)** ⭐ (b) + Nebula clouds (visibility -50%)
- [ ] **d)** (c) + Solar flares (moving damage zones)

### Câu 45: Ship customization

- [ ] **a)** Skip
- [ ] **b)** Skin + color tint
- [ ] **c)** ⭐ (b) + Trail color + glow color
- [ ] **d)** (c) + Weapon loadout slot + module slot

## 🎁 Section E: META / POLISH

### Câu 46: Achievements expansion

- [ ] **a)** Skip
- [ ] **b)** +20 achievements (kill / time / combo milestones)
- [ ] **c)** ⭐ (b) + tiered (Bronze/Silver/Gold)
- [ ] **d)** (c) + Google Play Games sync

### Câu 47: Story / lore

- [ ] **a)** Skip
- [ ] **b)** Boss dialogue popups
- [ ] **c)** ⭐ (b) + chapter intro narrative screens
- [ ] **d)** (c) + epilogue cinematic

### Câu 48: Permanent meta progression

- [ ] **a)** Skip
- [ ] **b)** 5 upgrade nodes (HP/Damage/Magnet/Shield/Speed)
- [ ] **c)** ⭐ (b) + 15-node skill tree branching
- [ ] **d)** (c) + prestige system (reset for permanent multipliers)

## 💥 Section F: GAME FEEL EXTRA

### Câu 18: Hit pause / hit stop

- [ ] **a)** Skip
- [ ] **b)** ⭐ Pause game tick 60ms khi kill enemy
- [ ] **c)** (b) + 120ms freeze + screen flash trắng khi kill boss

### Câu 19: Charge shot

- [ ] **a)** Skip
- [ ] **b)** ⭐ Giữ LEFT+RIGHT 800ms = mega laser (5× damage)
- [ ] **c)** (b) + 3 mức charge (0.4s/0.8s/1.5s = 2×/5×/15× damage)

### Câu 20: Smart bomb

- [ ] **a)** Skip
- [ ] **b)** ⭐ Booster `BOMB` rare drop, max 3 stack, double-tap Settings = nổ tất cả
- [ ] **c)** (b) + visual radial shockwave

### Câu 21: Boss intro cinematic

- [ ] **a)** Skip
- [ ] **b)** ⭐ Zoom-in 1.5s + tên boss banner + alarm SFX
- [ ] **c)** (b) + warning red border pulse + audio sting

### Câu 22: Permanent meta upgrades (mở rộng)

> Đã có Câu 48 mở rộng version. Skip nếu pick 48.

### Câu 23: Endless mode

- [ ] **a)** Skip
- [ ] **b)** ⭐ Sau "End", endless mode (spawn rate ×1.05/wave, hp ×1.1)
- [ ] **c)** (b) + endless leaderboard riêng (track survival time)

### Câu 24: Boss kill rank

- [ ] **a)** Skip
- [ ] **b)** ⭐ S/A/B/C theo time-to-kill (<20s = S, <40s = A...)
- [ ] **c)** (b) + score multiplier (S=×3, A=×2, B=×1.5)

### Câu 25: Random run modifiers

- [ ] **a)** Skip
- [ ] **b)** ⭐ Mỗi game roll 1 modifier random
- [ ] **c)** (b) + chọn 1 trong 3 modifier mỗi game

### Câu 26: Photo mode

- [ ] **a)** Skip
- [ ] **b)** ⭐ Pause → ẩn HUD → screenshot lưu Pictures/
- [ ] **c)** (b) + filter chọn (neon vs muted) + share intent

### Câu 27: Color blind mode

- [ ] **a)** Skip
- [ ] **b)** ⭐ Profile dropdown: Default / Protanopia / Deuteranopia / Tritanopia
- [ ] **c)** (b) + high contrast + larger text

### Câu 28: Procedural enemy patterns

- [ ] **a)** Skip
- [ ] **b)** ⭐ Stage 10+ random mix ZigZag + Row + cluster
- [ ] **c)** (b) + custom patterns (V-formation, sine wave)

### Câu 29: Ship secondary weapon

- [ ] **a)** Skip
- [ ] **b)** ⭐ Long-press Settings = swap weapon (Laser↔Spread fan)
- [ ] **c)** (b) + 3rd mode (Beam continuous, drain energy bar)

### Câu 30: Cinematic camera zoom

- [ ] **a)** Skip
- [ ] **b)** ⭐ Zoom 1.1× × 300ms khi: boss spawn / damage / kill boss
- [ ] **c)** (b) + camera shake + chromatic aberration

---

# Phần 4 — ⏸️ Deferred (cần focused session riêng)

Các architectural refactors quá lớn để gộp chung:

- [ ] **Vb** Hilt DI — kapt setup + refactor mọi controller (~1-2h)
- [ ] **Ub + BBc** ViewModel + StateFlow + SavedStateHandle — touch toàn bộ Composable (~1-2h)
- [ ] **Rb** Material 3 migration — touch every screen (~1-2h)
- [ ] **Mc** Per-stage BGM crossfade — cần thêm 3-5 BGM tracks
- [ ] **Zc** ProGuard + R8 baseline profile — cần test minified release
- [ ] **AAc** Recomposition perf audit — Layout Inspector instrumentation
- [ ] **CCc** Macrobenchmark / microbenchmark — cần benchmark module riêng

---

# Phần 5 — ❌ Đã skip

- ❌ **Ja** Firebase Crashlytics
- ❌ **Sa** Debug overlay (FPS counter)
- ❌ **Xa** Logger refactor (giữ API simple, chỉ thêm calls)

---

# Phần 6 — 💭 Ideas chưa đưa vào selector

> Brainstorm pool — nếu có cảm hứng thì pick lên selector đề xuất.

## Combat
- Beam laser drain energy bar
- Lightning chain laser (jump between enemies)
- Mine drop behind ship (passive defense)
- Drone companion (orbit + auto-fire)
- Cluster bomb (split into smaller bullets)
- Counter-attack (parry enemy laser)

## Visual
- Ship death animation (spiral + explode + zoom)
- Cinematic boss intro với name reveal
- Camera zoom dynamic theo action
- Lens flare on bright objects
- Motion blur on fast moves
- Color grading per chapter
- Screen tear effect on damage

## Audio
- Voice announcer (combo callouts)
- Reactive music intensity
- Spatial audio (3D positioning)
- Audio ducking on big events

## Progression
- XP / level per ship
- Unlockable ships
- Skill tree với prestige
- Currency system (mineral → upgrade)
- Loadout với module slots

## Social
- Cloud save (Google Play Games)
- Online leaderboard
- Replay sharing
- Photo mode export

## QoL
- Difficulty per stage (mid-game adjust)
- Practice mode (specific stage)
- Quick restart hotkey
- One-handed mode

## Monetization (production)
- Rewarded ads for revive
- IAP cho skins / weapons
- Battle pass
- No-ad purchase

## Lore / story
- Intro cinematic
- Boss dialogue
- Stage narrative bits
- Win screen epilogue

---

# Phần 7 — 🛠️ Implementation Wave Plan

## Wave 1 ✅ DONE
- [x] 1c Boss HP bar
- [x] 2c+10b Floating damage numbers + crit zoom + boss aggregation 200ms
- [x] 4c Mineral popup + combo bonus
- [x] 7c Combo HUD always-visible + multiplier
- [x] 9b Achievements basic (DataStore + 10 achievements)
- [x] 13c Star explosion radial burst + ring shockwave
- [x] 16c Magnet visual circle + line
- [x] 18c Hit pause 60ms enemy / 120ms boss + flash

## Wave 2 ✅ DONE
- [x] 3b Ship engine flame trail (Canvas: outer cyan cone + inner gold core, sin flicker)
- [x] 5c StageBanner neon-glow (3-layer Text stack + halo backdrop, "GO!"/boss/countdown variants)
- [x] 11c Kill-cam slow-mo (game tick halved + 1.5× zoom + 1.5s GameOver delay)
- [x] 12c Wave clear bonus (5 mineral burst + WaveClearBanner gold neon)
- [x] 18c done in Wave 1
- [x] 21c BossIntroOverlay (pulsing red border + boss name banner + heavy haptic + alarm SFX)
- [x] 30c Cinematic camera zoom (1.10× × 300ms sine pulse on damage/boss spawn/boss kill, gated by reduceMotion)
- [x] Fix: layer 3-4 stars sizes reduced (3-5px / 3.5-5.5px) + halo radius 1.6→1.2 — không còn nhầm với đạn enemy
- [x] Fix: Boss HP bar duplicate — skip mini bar trên đầu boss trong GameWorld
- [x] Fix: Boss HP bar overlap player HP — moved padding(top=190dp) below player HUD

## Wave 3 (Gameplay depth, ~3h)
- [ ] 6c Slow-motion critical
- [ ] 8c Dynamic music intensity
- [ ] 14c Auto-revive token
- [x] 15c Stats screen breakdown (round 12)
- [ ] 17c Daily challenge
- [x] 19b Charge shot (hold both = mega) (round 12)
- [x] 20b Smart bomb stack (round 12)
- [x] 24b Boss kill rank S/A/B/C (round 12)

## Wave 4 (Content expansion, ~5h+)
- [ ] 31x +stages (theo pick)
- [ ] 32x Chapter themes
- [ ] 33x Mid-bosses
- [ ] 34x Final boss multi-phase
- [ ] 35x +bullets
- [ ] 38x +items
- [ ] 41x Status effects
- [ ] 42x Roguelike buffs
- [ ] 44x Hazards

## Wave 5 (Modes + meta, ~3h)
- [ ] 23x Endless mode
- [ ] 43x Game modes
- [ ] 25x Random modifiers
- [ ] 28x Procedural patterns
- [ ] 46x Achievements expansion
- [ ] 47x Story / lore
- [ ] 48x Permanent progression

## Wave 6 (Polish + accessibility)
- [ ] 26x Photo mode
- [ ] 27x Color blind mode
- [ ] 29x Secondary weapon
- [ ] 36x Loadout system
- [ ] 39x Item rarity tiers
- [ ] 40x Item combos
- [ ] 45x Ship customization

## Wave 7 (Architecture deferred)
- [ ] Vb Hilt
- [ ] Ub ViewModel
- [ ] Rb Material 3
- [ ] Mc Per-stage BGM
- [ ] Zc ProGuard + baseline
- [ ] AAc Recomposition audit
- [ ] CCc Benchmark

---

# Notes

- **Memory leak guarding:** mọi entity transient (damage numbers, popups, sparkles) phải dùng immutable list snapshot pattern (xem bug fix sparkles)
- **Performance:** mọi background/effect mới phải merge vào existing Canvas khi có thể; tránh tạo Canvas riêng cho từng entity
- **Logger:** tiếp tục sprinkle theo style hiện tại (`Logger.d` với category prefix tự nhiên trong message)
- **Build verify:** sau mỗi wave, chạy `./gradlew assembleDevDebug compileProductionReleaseKotlin`
- **i18n:** strings mới phải thêm vào cả `values-vi/strings.xml` và `values-en/strings.xml`
- **Compose stability:** data class state mới nên dùng `@Immutable`/`@Stable` annotation
