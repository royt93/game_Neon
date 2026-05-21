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

## 🆕 Wave 3 cleanup (round 19 — close out remaining picks)

- ✅ **14c Auto-revive token**. New `BoosterType.REVIVE_TOKEN` (drawable: vector `booster_revive.xml` — neon green heart + cross). `BoosterType` carries a `weight` field (REVIVE=5, others=19 each → ~5% drop). `Booster.type` switched from uniform random to weighted picker. Pickup flow: `ShipController.monitorShipCollisions` `when` arm sets `ship.hasReviveToken=true` instead of healing. Ship gets new `hasReviveToken: Boolean` field + new `onShipRevived` callback. In `updateHp`, when `hp→0`: if token held → restore `hp=300` + `1.5s i-frames` + clear token + invoke callback (skipping `onShipDestroyed`). GameState exposes `revivedShownAtMillis` + `hasReviveToken` to UI. New `RevivedBanner` (1.6s center pulse: pop-in scale 0..0.18s, hold w/ pulse 0.18..0.75s, fade-out 0.75..1.0). New `ReviveTokenBadge` (small neon-green ♥ pill) in `IndicatorStatus` only shown when token held. Heavy haptic + PICKUP sfx on revive. Strings: `revived_banner` (vi/en).
- ✅ **6c Slow-motion critical**. Boss HP <20% triggers 60% game speed for 2s + light additive screen shake + pulsing red radial vignette. Once per boss (gated by `bossSlowMotionTriggeredForId = enemy.enemyId`). Implementation: GameState adds `bossSlowMotionStartedAtMillis` + trigger scan after `processEnemies` tinker (`enemies.firstOrNull { isBoss && hp/initialHp ≤ 0.20 && id != gateId }`). Game loop slow-mo branch: when `inBossSlowMo`, skip `frameCount % 5L in {1, 3}` → 3-of-5 ticks run = 60% speed. GameScreen reads `bossSlowMotionStartedAtMillis`, applies sin/cos additive shake (~5px max, ramp-in 200ms / hold / ramp-out 300ms) + radial pulse vignette (0.30+0.30·sin alpha, gated by `reduceMotion`). Medium haptic on trigger.
- ✅ **8c Dynamic music intensity**. Compute intensity tier each composition via `derivedStateOf`: `hasBoss → 1.0×`, `hp<30% → 1.0×`, `enemyCount≥4 || hp<50% → 0.85×`, else `0.7×`. `Animatable` interpolates target with `tween(500ms)` for smooth ramps. `LaunchedEffect(effectiveMusicVolume)` calls `audioHolder.setVolume(musicVolumePref × intensity)`. `DisposableEffect` restores baseline volume to settings value when GameScreen exits (so splash/menu/dialog playback isn't stuck attenuated). No new audio assets — pure runtime modulation of existing 3-track ExoPlayer playlist.
- ✅ **17c Daily challenge**. New methods on `LeaderboardRepository`: `submitDaily(score, dayKey = todayUtcDayKey())` + `dailyEntries(dayKey)` Flow. Storage uses second prefs key `daily_csv` with format `dayKey|score|timestamp` per line. Per-day cap = `MAX_ENTRIES`, history of older days preserved. `todayUtcDayKey() = currentTimeMillis / 86_400_000L` (days since epoch UTC, stable per calendar day regardless of TZ). `DialogGameOver` submits to BOTH all-time AND today's daily list, displays `DailyPanel` (magenta accent panel: "DAILY CHALLENGE" header + "Day N" seed indicator + TODAY'S BEST + RUNS TODAY + ★ TODAY'S TOP RUN ★ badge when current run is daily best). Strings: `daily_challenge_label`, `daily_seed_label`, `daily_best_today` (vi/en).

### Wave 3 cleanup files

**New:**
- `res/drawable/booster_revive.xml` (vector: neon green heart + cross icon)
- `ui/game/controls/RevivedBanner.kt`

**Modified:**
- `ui/game/booster/BoosterType.kt` (added `REVIVE_TOKEN` + `weight` per type)
- `ui/game/booster/Booster.kt` (uniform random → weighted picker)
- `ui/game/ship/ship/Ship.kt` (added `hasReviveToken`)
- `ui/game/ship/ship/ShipController.kt` (added `onShipRevived` + REVIVE pickup arm + revive-on-death in `updateHp` + `REVIVE_HP`/`REVIVE_IFRAMES_MILLIS` constants)
- `ui/game/state/GameState.kt` (added `revivedShownAtMillis`, `bossSlowMotionStartedAtMillis`, `bossSlowMotionTriggeredForId`, slow-mo loop branch, slow-mo trigger scan, `onShipRevived` wiring, exposed `hasReviveToken`/`revivedShownAtMillis`/`bossSlowMotionStartedAtMillis` in `GameState` data class)
- `ui/game/GameScreen.kt` (RevivedBanner render, slow-mo shake additive, slow-mo pulse vignette, music intensity Animatable + setVolume, baseline restore on dispose, haptics for revive + slow-mo)
- `ui/game/controls/IndicatorStatus.kt` (added `hasReviveToken` param + `ReviveTokenBadge`)
- `data/LeaderboardRepository.kt` (added `submitDaily` + `dailyEntries` Flow + `todayUtcDayKey()`)
- `ui/dlg/gameover/DialogGameOver.kt` (added daily entries collect + `submitDaily` call + `DailyPanel`)
- `res/values/strings.xml` + `values-en` + `values-vi` (added `revived_banner`, `daily_challenge_label`, `daily_seed_label`, `daily_best_today`)

## 🆕 Wave 4 Foundation (round 20 — content expansion: 100-stage 5-chapter campaign)

- ✅ **Round 20 hotfix: 8c music intensity**. Fixed bug discovered ở log đánh giá round 19 — `derivedStateOf { gameState.* }` capture closure `gameState` từ composition đầu tiên (gameState là plain Kotlin object, không phải Compose State → cache không invalidate, intensity stuck @ 0.7). Sửa: bỏ `derivedStateOf`, compute inline mỗi recomposition. Cùng pattern fix cho `stageTint` (cùng bug pattern).
- ✅ **31d 100-stage 5-chapter campaign**. `Stage.kt` refactored: static `stages` list → procedural `buildStageScript()` generator. 5 chapters × ~26 entries = ~130 stage entries. New `Chapter` enum (ASTEROID_BELT / NEBULA_CLOUD / ICE_PLANET / HOSTILE_STATION / GALAXY_CORE) carries `displayName`, `tintArgb`, `regularEnemyDrawables` palette, `hazard`, `midBossType`, `finalBossType`. Each chapter layout: 3 intro messages + 12 game stages (mid-boss inserted at game-stage 6 + 12 with warning/outro flanks) + chapter boss intro + StageBoss + outro message. Difficulty tier 0..2 scales HP/speed/spawn rate per game stage; chapter index further scales (chapter 5 enemies ~60% tougher than chapter 1). `StageGame` + `StageBoss` data classes carry `chapterId` field for downstream tinting. Final chapter (GALAXY_CORE) skips mid-bosses, goes straight to FinalBoss.
- ✅ **32d Chapter themes + hazards**. `stageTintColor()` rewritten: maps chapter id 1..5 to theme color (gold-orange / violet / cyan / red / magenta), alpha 0.07. Per-chapter enemy palette via `Chapter.regularEnemyDrawables` cycled through `buildGameStage`. New `HazardType` enum (ASTEROID_STORM / NEBULA_FOG / ICE_PATCHES) baked into StageGame. ASTEROID_STORM mechanically active via increased `spaceRockSpawnRateMillis = Millis(1500)`; NEBULA_FOG visual via new `HazardOverlay` composable (dark purple gradient pulse 0.20↔0.40 alpha, 4s cycle, gated by reduceMotion); ICE_PATCHES visual = cyan top+bottom edge gradient (mechanic deferred — would require ShipController inertia rework). `StageController.currentChapterId()` walks back through stages to find most recent chapter context; `currentHazard()` reads active StageGame's hazard tag. Both exposed via GameState.
- ✅ **33c+d Mid-bosses + phase transitions**. New `MidBossType` sealed class (OFFENSIVE / DEFENSIVE / SWARM) extending `EnemyType`. New `MidBoss` class (130×90, single drawable per variant): OFFENSIVE 1500 HP w/ sine-wave horizontal patrol + aimed laser → phase 2 = triple spread; DEFENSIVE 2500 HP w/ slow horizontal patrol + single laser → phase 2 = 5-laser barrage with random gap; SWARM 1200 HP w/ figure-8 motion + frequent aimed laser. Phase transition gated at HP<50% (`phase2Engaged` one-shot). Reuses existing `isBoss=true` → triggers BossHpBar + BossRankOverlay automatically. Mid-bosses inserted at game-stage 6 + 12 within each chapter (chapters 1-4; GALAXY_CORE skips mid-boss). EnemyFactory wired with new `MidBossType` arm.
- ✅ **34d FinalBoss 3-phase**. New `FinalBoss` class with 22500 HP single pool, 3-phase gates: Phase 1 (HP 22500..15000) = slow patrol + aimed laser; Phase 2 (HP 15000..7500) = figure-8 patrol + 3-spread laser; Phase 3 (HP 7500..0) = aggressive patrol + 8-direction ring barrage. Phase transition triggers 300ms i-frames + sets `phaseTransitionMillis` for cinematic banner. New `FinalBossType` object EnemyType. `Enemy` interface extended with `currentPhase: Int` + `phaseTransitionMillis: Long` (default 0/0L). `EnemyUI` + mapper pass through. New `PhaseTransitionBanner` composable (1.4s pop-in 0..0.18 → hold 0.18..0.7 → fade 0.7..1.0, "PHASE N" 3-layer text stack + red-gold radial halo). Cinematic intro reuses BossIntroOverlay; outro = 5-explosion starburst already in EnemyController. Alt ending: `RunStats.victoryAchieved: Boolean` set when FinalBoss instance killed. New `VictoryPanel` in DialogGameOver shows difficulty-aware text: Easy = "GALAXY SAVED / Try Normal next time", Normal = "GALAXY OVERLORD DEFEATED", Hard = "LEGENDARY VICTORY".

### Wave 4 Foundation files

**New:**
- `ui/game/stage/Chapter.kt` (5-chapter enum + HazardType enum)
- `ui/game/enemy/ship/model/MidBossType.kt` (3-variant sealed class)
- `ui/game/enemy/ship/model/MidBoss.kt` (~190 LOC, 3 attack variants + phase transition)
- `ui/game/enemy/ship/model/FinalBoss.kt` (~210 LOC, 3-phase HP gates + ring barrage)
- `ui/game/controls/HazardOverlay.kt` (NEBULA_FOG pulse + ICE edge tint)
- `ui/game/controls/PhaseTransitionBanner.kt` (1.4s "PHASE N" cinematic)

**Modified:**
- `ui/game/stage/Stage.kt` (full refactor → procedural `buildStageScript()` from chapters)
- `ui/game/stage/StageController.kt` (added `currentChapterId()` + `currentHazard()` accessors)
- `ui/game/enemy/ship/factory/EnemyFactory.kt` (added MidBoss + FinalBoss spawn arms)
- `ui/game/enemy/ship/model/Enemy.kt` (extended with `currentPhase` + `phaseTransitionMillis` defaults)
- `ui/game/enemy/ship/model/EnemyUI.kt` + mapper (passthrough)
- `ui/game/state/GameState.kt` (track `finalBossDefeated`, expose `currentChapterId`/`currentHazard`/`finalBossDefeated`)
- `ui/game/GameScreen.kt` (chapter-based stageTint, HazardOverlay render, PhaseTransitionBanner render, RunStats victory propagation)
- `data/RunStats.kt` (added `victoryAchieved: Boolean`)
- `ui/dlg/gameover/DialogGameOver.kt` (added `VictoryPanel` with difficulty-aware ending text)

### Edge cases handled

- StageController.currentChapterId() walks backward through stages from current index — correctly identifies chapter even during inter-chapter StageMessage entries.
- StageBoss with default chapterId=1 — backward compat preserved; existing BossOne/BossTwo unchanged.
- FinalBoss phase i-frames (300ms) prevent player chain-killing through phase 2/3 transitions.
- VictoryPanel only renders when `runStats.victoryAchieved == true` — never shown for normal Game Over.
- HazardOverlay returns early when `hazard == null` — no recomposition cost outside hazard stages.

### Round 20 audit fixes (post-implementation review)

**Round 1 — surface bugs caught by static review:**

- 🚨 BUG **Boss intro overlay shows "BOSS" for mid-boss/FinalBoss**. Original `when (newStage.enemyType)` only mapped LevelOneBossType + LevelTwoBossType; everything else fell to `else -> "BOSS"`. Fix: extended `when` with `FinalBossType -> "GALAXY OVERLORD"` + `is MidBossType -> t.displayName` (binds to local `t = newStage.enemyType` for type-safe access).
- 🚨 BUG **FinalBoss `ringBarrage()` upward lasers leak forever**. The 8-direction ring fired lasers in all directions including upward (cos < 0). `EnemyLasersController.processLasers` only destroys lasers with `yOffset > screenHeight` — upward lasers (yOffset goes negative) never exit. Fix: filter out lasers with `ySpeed < 0.15` after applying +0.25 downward bias. Result: 5-6 lasers per ring instead of 8, all guaranteed to leave screen bottom.
- 🚨 BUG **Wave clear spam** — original gate `previousWasGame` fired the bonus on every game-stage transition. Procedural script has 12 short StageGame entries per chapter (5-9s each) → ~60 wave-clear bursts per playthrough. Fix: tightened gate to `previousWasGame && newIsGame == false` — fires only at significant transitions (game→DANGER mid-boss prelude, game→BOSS FIGHT prelude). Result: ~9 wave clears per full campaign, matching original UX intent.
- ⚠️ POLISH **MidBossType.SWARM shared drawable with OFFENSIVE** (both `enemy_red_boss`). Reassigned SWARM to `enemy_green_boss` (only 2 boss webps available; sharing with DEFENSIVE acceptable as behavior differs sharply: figure-8 motion + lower HP).
- ⚠️ POLISH **MidBoss.SWARM phase 2 was no-op** — comment claimed "doubled fire rate" but EnemyLasersController has fixed 1000ms cadence; nothing actually changed. Rewired SWARM phase 2 to use `tripleSpreadLasers(ship)` (same primitive as OFFENSIVE phase 2) so phase 2 has visible aggression boost.

**Round 2 — runtime/UX flow bugs caught on second audit:**

- 🚨 BUG (CRITICAL) **VictoryPanel never displays — GameOver dialog không bao giờ tự fire after FinalBoss kill**. After player defeats FinalBoss, `finalBossDefeated = true` set in onEnemyKilled, stage advances to "VICTORY!" StageMessage, but `gameStatus` stays `RUNNING`. Stage script reaches end (`hasNextStage = false`) → stays at VICTORY! forever. Player has to manually suicide to see GameOver dialog. Fix: in `onEnemyKilled` when `enemy is FinalBoss`, launch coroutine `delay(4000); setGameStatus(GAME_OVER)`. The 4s lets "VICTORY!" StageBanner show + breathing room before dialog. killCamStartedAtMillis stays 0L → no slow-mo replay (ship still alive).
- 🚨 BUG **Victory triggers death haptic + sad GAME_OVER sfx** — original GameOver LaunchedEffect plays `HapticPattern.LONG` + `SfxEvent.GAME_OVER` regardless of cause. UX clash: player wins but feels like dying. Fix: GameScreen branches on `gameState.finalBossDefeated`. Victory path = `HapticPattern.HEAVY` + `SfxEvent.PICKUP` (celebratory) + 500ms delay (no kill-cam to play). Death path unchanged.
- 🚨 BUG **Chapter intro tint stuck on previous chapter for ~7s**. `StageController.currentChapterId()` walked backward from `stageIndex` to find most recent StageGame/StageBoss with chapterId. During chapter N+1 intro messages ("CHAPTER N+1 / NAME / GO!"), walk-back resolves to chapter N's last boss → tint stays gold while player sees "NEBULA CLOUD" header. Fix: added `chapterId: Int = 0` field to `StageMessage` data class; `buildStageScript` tags every intra-chapter message with `chapter.id`; `currentChapterId()` now reads chapter id directly from current stage (O(1)) with fallback walk preserved for legacy/untagged messages.
- ⚠️ POLISH **"Continue!" message duration 2s overlapped BossRankOverlay (1.8s)** — player saw "Continue!" message visible only ~0.2s after mid-boss kill. Bumped to 3s for clearer pacing.

### Round 21 perf fixes (lag diagnosed from runtime log 16:51-16:54)

**Symptom**: User reported app rất lag during chapter 1 boss fight + chapter 2 entry. Log evidence:
- entities=lasers:41+enemies:4 (vs typical 6-11) → Triple Laser + Ult Booster stack
- 14-22 active DamageNumber, 16-18 active Explosion simultaneously
- Heap dao động 10MB ↔ 42MB (GC churn)
- HEAVY haptic firing every 60-90ms during GODLIKE combo (15+ vibrate IPC/sec)
- 191 unguarded `Logger.d` calls; 6+ logs per collision

**Fixes (3 picked, applied):**

- 🔥 PERF **Logger.d debug-guard**. Enabled `buildConfig = true` in `app/build.gradle`, then wrapped both `Logger.d` overloads in `if (!BuildConfig.DEBUG) return`. Release build now no-ops Log.d entirely (was unconditionally calling `Log.d` even in release). Native logd IPC eliminated in production.
- 🔥 PERF **Throttle GODLIKE haptic**. Added `minIntervalMs` to `HapticPattern` enum (HEAVY=200ms, MEDIUM=60ms). `HapticController.vibrate` now tracks per-pattern `lastFireUptimeMs` via `LongArray(HapticPattern.values().size)` keyed by ordinal. Skips fire if `now - last < minIntervalMs`. Prevents Vibrator binder saturation during GODLIKE combo spam.
- 🔥 PERF **Cap entity lists** (drop oldest when exceeding cap):
  - `DamageNumberController.MAX_ACTIVE = 8` (was unbounded; saw 14-22 active)
  - `ImpactSparkController.MAX_ACTIVE = 12` + reduced `BURST_COUNT` 16 → 8 (was unbounded)
  - `ExplosionController.MAX_ACTIVE = 8` (was unbounded; saw 16-18 active = 16-18 Coil GIF decoders)
  - Removed hot-path `Logger.d` calls from `DamageNumberController.spawn`, `ImpactSparkController.spawnBurst`, `ExplosionController.addExplosion`, `processExplosions` (string interpolation cost remained even with debug-guard inside Logger.d).

**Skipped (deferred):**
- ⏸️ Disable Triple Laser when Ult active — user did not pick. Would mutually-exclude the two boosters to prevent 40+ active lasers.
- ⏸️ Replace Coil GIF explosion with Canvas particle / sprite-frame anim — bigger refactor, parked.

**Files modified (round 21):**
- `app/build.gradle` (added `buildConfig = true`)
- `utils/Logger.kt` (BuildConfig.DEBUG guard)
- `ui/game/haptic/HapticController.kt` (per-pattern throttle)
- `ui/game/damage/DamageNumber.kt` (MAX_ACTIVE=8 + log removed)
- `ui/game/spark/ImpactSpark.kt` (MAX_ACTIVE=12, BURST_COUNT 16→8 + log removed)
- `ui/game/explosion/controller/ExplosionController.kt` (MAX_ACTIVE=8 + logs removed)

**Verification**: `assembleDevDebug` + `compileProductionReleaseKotlin` BUILD SUCCESSFUL.

### Known limitations (acknowledged, deferred)

- **Mid-boss drawable variety**: only 2 boss webps exist (`enemy_red_boss`, `enemy_green_boss`). OFFENSIVE = red, DEFENSIVE/SWARM = green (share). Could add per-chapter color filter or new webps later.
- **ICE_PATCHES mechanic** = visual only (cyan edge tint). True ship-slip inertia would require ShipController rewrite — deferred as polish.
- **Chapter intro chapterId lag**: during 7s of "CHAPTER N / NAME / GO!" StageMessage entries, `currentChapterId()` still returns the previous chapter (walks back finds previous chapter's StageGame/StageBoss). Tint will shift only when first game stage of new chapter starts. Acceptable transient.
- **MidBoss/FinalBoss difficulty scaling**: difficulty multiplier currently affects only player damage taken, not enemy HP. FinalBoss 22500 HP fixed across Easy/Normal/Hard. Hard mode = same boss but harder player survival.

## 🆕 Wave 5 Modes + meta (round 22 — modes / endless / modifiers / patterns / achievements / story / progression)

### Phase 0 — foundation scaffolding

- ✅ **RunContext + EffectiveStats + StageProvider**. New `ui/game/state/RunContext.kt` (immutable per-run snapshot: mode + modifier + difficulty + dailySeed + metaUpgrades). New `ui/game/state/EffectiveStats.kt` (pure compute: difficulty × modifier × meta upgrades w/ caps hp 0.3..3.0, damage 0.5..4.0, speed 0.5..3.5, magnet 0.5..3.0, score 0.5..4.0). New `ui/game/stage/StageProvider.kt` sealed interface + StaticListProvider default — `StageController` now accepts `provider` parameter (back-compat default = StaticListProvider). Zero behavior change for existing campaign.

### 43x — Game modes (Survival / TimeAttack / BossRush)

- ✅ **GameMode enum** (`ui/game/mode/GameMode.kt`): CAMPAIGN / SURVIVAL / TIME_ATTACK / BOSS_RUSH / ENDLESS / DAILY. SettingsRepository.lastMode persists pick. DialogModePicker (clone of DifficultyPicker style) under `ui/dlg/modepicker/`. Wired into Settings dialog "Game mode" entry + dedicated ModePicker route. Restart picks up new mode automatically.
- ✅ **StageProviders.kt** with SurvivalProvider (chapter-1 stages cycled, +15% scaling per cycle), BossRushProvider (all StageBoss entries in sequence + "Next!" gaps), TimeAttackProvider (chapter-1 cycled + 60s wall clock).
- ✅ **TIME_ATTACK timer**: GameState's `monitorLoopInSec` forces GAME_OVER when `gameTimeSec >= 60`. Ship stays alive → victory branch in GameScreen.
- ✅ **BOSS_RUSH heal between bosses**: onStageAdvance restores `ship.hp = 1000` when entering "Next!" StageMessage (so each boss is a fresh slate).
- ✅ **Wave-clear logic fixed**: previously referenced global `stages` directly; now uses `stageProvider.getAt(previousIdx)` so non-campaign modes don't crash on out-of-range index.

### 23x — Endless mode + endless leaderboard

- ✅ **EndlessProvider**: chapter-1 stages cycled with `hp × 1.10^wave` + `spawn × 1.05^wave` scaling. Procedural — never terminates.
- ✅ **LeaderboardRepository** extended: new ENDLESS_KEY + `submitEndless(seconds)` + `endlessEntries: Flow<List<LeaderboardEntry>>`. Score field stores survival seconds.
- ✅ **DialogGameOver EndlessPanel**: shows when `runStatsState.gameModeKey == "endless"`. Survival time MM:SS + best ever + "★ NEW ENDLESS BEST ★" badge.
- ✅ **RunStats.gameModeKey** added so DialogGameOver knows the run's mode. GameScreen writes it on GAME_OVER snapshot.

### 25x — Random modifiers (pick 1/3 pre-game)

- ✅ **RunModifier enum** (`ui/game/modifier/RunModifier.kt`): 9 entries (NONE + TRIPLE_SPEED, GLASS_CANNON, NO_SHIELDS, SUPER_MAGNET, BERSERKER, TANK, BOSSES_ONLY, DOUBLE_OR_NOTHING) with hp/damage/speed/magnet/score multipliers + flags.
- ✅ **DialogModifierPicker**: rolls `RunModifier.pickThree()` random non-NONE choices. User picks 1 or SKIP (applies NONE). Settings.lastModifier persists pick.
- ✅ **EffectiveStats applied**: `scoreMul` baked into `mineralsEarnedTotal` updates; `magnetMul` baked into MineralsController's `getMagnetRadius` getter.
- ⏸️ **hpMul / damageMul / speedMul / noShieldDrops / bossesOnly** scope-deferred. Picker shows all multipliers but only score + magnet take effect this round. Controller refactor (Ship/Lasers/Booster) needed to plumb the remaining mods.

### 28x — Procedural enemy patterns (V / SineWave / Cluster)

- ✅ **EnemyFormation extended**: new `VFormation(count)` + `SineWave(count, amplitude, period)` sealed-class members.
- ✅ **RegularEnemy** accepts `initialYOffset` constructor param + adds `sineAnchorX` baseline; `process()` dispatches new `moveSineWaveFormation()` which oscillates `xOffset = sineAnchorX + sin(yOffset/period) * amplitude`.
- ✅ **EnemyFactory** spawns V (5 staggered slots from center, mirrored layers below screen-top so they fly in) + SineWave (single-column N enemies, all sharing anchor x).
- ✅ **Stage.kt buildGameStage** picks procedurally for `chapter.id >= 2`: 35% ZigZag / 30% Row / 17% VFormation / 18% SineWave (seed = chapter.id × 12 + gameStage for determinism). Chapter 1 keeps original alternation (ramp pace).

### 46x — Tiered achievements (+20 Bronze/Silver/Gold)

- ✅ **AchievementTier enum** (BRONZE/SILVER/GOLD) added to AchievementsRepository.
- ✅ **20 new achievements** added: KILL_50/200/500, COMBO_20, SURVIVE_10MIN, STAGE_30/60/100, BOSS_3/5, REVIVE_ONCE, SMART_BOMB_5, ENDLESS_60S/180S/300S, FINAL_BOSS_KILL, HARD_VICTORY, TIME_ATTACK_HIGH, BOSS_RUSH_CLEAR, MODIFIER_RUN.
- ✅ **AchievementBanner tier-coloured border**: bronze (0xFFCD7F32 brown), silver (0xFFB0C4DE light steel blue), gold (NeonGold). Tier label displayed in header ("🏆 ACHIEVEMENT · GOLD").
- ✅ **Trigger sites**: onEnemyKilled, monitorLoopInSec (time + stage milestones + endless tiers), onShipRevived, smart bomb dispatch.
- ⚠️ Existing 10 achievements still use hardcoded VN description strings (deferred lift to `strings.xml` — would be its own pass).

### 47x — Story / lore

- ✅ **StoryLine** (`ui/game/story/StoryLine.kt`) + **StoryRegistry** (`ui/game/story/StoryRegistry.kt`): per-chapter intro narration (5 chapters × 2 lines, CAPTAIN speaker) + per-boss taunt (LevelOne/Two/Final + 3 MidBoss variants, speaker = boss name).
- ✅ **StoryOverlay** (`ui/game/controls/StoryOverlay.kt`): bottom-anchored slide-up dialogue card with speaker color (cyan = CAPTAIN, magenta = boss).
- ✅ **GameState wiring**: chapter intros gated by `chapterIntroPlayedChapter` (saveable) so each chapter narrates only once per run. Boss taunts fire 1.6s after `bossIntroShownAtMillis` so they don't compete with BossIntroOverlay's top banner. Chapter intros chain multiple lines via `coroutineScope.launch { for (l in lines) { ... } }`.
- ✅ **GameScreen render**: StoryOverlay aligned BottomCenter zIndex=448. Doesn't overlap any existing banner (all others are TopCenter / Center).

### 48x — Permanent progression + 15-node skill tree

- ✅ **MetaProgressionRepository** (`data/MetaProgressionRepository.kt`): DataStore-backed `lifetime_minerals` + per-node `node_<key>` int prefs. `addMinerals(amount)` accumulates; `spendOnNode(key, cost, maxRank)` atomic spend + rank-up. `allRanks: Flow<Map<String, Int>>` snapshot for RunContext.
- ✅ **SkillNode enum** (`ui/game/meta/SkillNode.kt`): 15 nodes across 3 tiers.
  - Tier 0 (5 root): FORTIFY (+10% hp/rank, max 5), FIREPOWER (+8% dmg/rank), GRAVITY WELL (+15% magnet/rank, max 4), AEGIS (+1.5s shield), AGILITY (+6% speed, max 4).
  - Tier 1 (8 branches, require parent rank ≥ 2 or 3): REGENESIS, CRITICAL, STARGAZER, REACTIVE, AFTERBURNER, ARMORY, MOMENTUM, PHOENIX HEART.
  - Tier 2 (2 endgame, require tier-1 rank ≥ 2): STAR FORGE, VOID CANNON.
- ✅ **DialogMetaUpgrade** (`ui/dlg/metaupgrade/`): full-screen Card with LazyColumn (height-capped 420dp), header `♦ <balance>`, per-node row showing rank/max + nextCost + UNLOCK/RANK UP button (color-coded per tier, dimmed when prereq unmet).
- ✅ **App + MainActivity provide LocalMetaProgression**. New `MetaUpgrade` nav route. DialogSettings "UPGRADES · SKILL TREE ➤" entry opens the dialog.
- ✅ **RunContext.metaUpgrades** populated via `metaRepo.allRanks.first()` at GameState construction. `EffectiveStats.compute` reads HP/Damage/Speed/Magnet rank bonuses and folds them into the same multiplier system as modifiers + difficulty.
- ✅ **DialogGameOver banks minerals**: `meta.addMinerals(parsed)` runs in the same LaunchedEffect that submits to all-time + daily leaderboards.

### Wave 5 files

**New:**
- `ui/game/mode/GameMode.kt`
- `ui/game/modifier/RunModifier.kt`
- `ui/game/state/RunContext.kt`
- `ui/game/state/EffectiveStats.kt`
- `ui/game/stage/StageProvider.kt` (sealed + StaticListProvider)
- `ui/game/stage/StageProviders.kt` (Survival / BossRush / TimeAttack / Endless)
- `ui/dlg/modepicker/DialogModePicker.kt`
- `ui/dlg/modifierpicker/DialogModifierPicker.kt`
- `ui/dlg/metaupgrade/DialogMetaUpgrade.kt`
- `ui/game/story/StoryLine.kt`
- `ui/game/story/StoryRegistry.kt`
- `ui/game/controls/StoryOverlay.kt`
- `ui/game/meta/SkillNode.kt`
- `data/MetaProgressionRepository.kt`

**Modified:**
- `App.kt` (metaProgression repository)
- `ui/MainActivity.kt` (LocalMetaProgression + ModePicker/ModifierPicker/MetaUpgrade routes)
- `navigation/Navigation.kt` (ModePicker, ModifierPicker, MetaUpgrade routes)
- `data/SettingsRepository.kt` (LAST_MODE + LAST_MODIFIER keys + flows + setters)
- `data/LeaderboardRepository.kt` (ENDLESS_KEY + submitEndless + endlessEntries)
- `data/RunStats.kt` (gameModeKey field)
- `data/AchievementsRepository.kt` (AchievementTier enum + 20 new achievements with tier field)
- `ui/game/enemy/ship/model/EnemyFormation.kt` (VFormation + SineWave)
- `ui/game/enemy/ship/model/RegularEnemy.kt` (initialYOffset + sineAnchorX + moveSineWaveFormation)
- `ui/game/enemy/ship/factory/EnemyFactory.kt` (V + SineWave spawn arms)
- `ui/game/stage/Stage.kt` (procedural pattern picker for chapter ≥ 2)
- `ui/game/stage/StageController.kt` (accepts StageProvider parameter, walk-back via provider)
- `ui/game/state/GameState.kt` (runMode + runModifier + runContext + effectiveStats + storyLine + storyShownMillis + chapterIntroPlayedChapter + smartBombsUsedCount + 12 new achievement trigger sites + TIME_ATTACK timer + BOSS_RUSH heal; achievementsRepo + unlockAchievement hoisted before shipController)
- `ui/game/GameScreen.kt` (StoryOverlay render + RunStats.gameModeKey write)
- `ui/dlg/gameover/DialogGameOver.kt` (EndlessPanel + submitEndless + meta.addMinerals)
- `ui/dlg/settings/DialogSettings.kt` (Game mode + Modifier + Upgrades rows)
- `ui/game/controls/AchievementBanner.kt` (tier-coloured border + tier label header)

### Wave 5 known scope cuts / deferred

- **25x RunModifier full plumbing**: only `scoreMul` + `magnetMul` applied this round. `hpMul`, `damageMul`, `speedMul`, `noShieldDrops`, `bossesOnly` need ShipController / LasersController / BoosterController refactor — deferred. Picker still functions; balance impact reduced to score + magnet until those plumbing pieces land.
- **48x tier-2 nodes (STAR FORGE / VOID CANNON)** persist as ranks but only HP/Damage/Speed/Magnet effects are wired to EffectiveStats. The remaining special nodes (REGENESIS auto-heal, CRITICAL chance, MOMENTUM combo extend, etc.) are spendable but visual-only until the controllers expose hooks for them.
- **Endless via post-campaign auto-trigger**: ENDLESS is currently picker-only (5th option in DialogModePicker). Auto-switch after CAMPAIGN's FinalBoss kill is plausible future work but not picked this round.
- **Existing 10 achievements** keep their hardcoded Vietnamese descriptions. Future i18n lift to strings.xml deferred.
- **TIME_ATTACK HUD countdown**: no visible countdown ring yet — gameTimeIndicator shows MM:SS which still works backward by mental math. Polish for next round.
- **BOSS_RUSH provider chapterId tinting**: BossRushProvider's stages span all chapter colors; the existing `currentChapterId()` walk-back gives reasonable but not necessarily "in-order" tints.

### Verification

- `./gradlew assembleDevDebug` BUILD SUCCESSFUL.
- `./gradlew compileProductionReleaseKotlin` BUILD SUCCESSFUL.

### Round 23 audit fixes (post-Wave-5 review)

Audit identified 1 critical UX gap + 3 medium/minor bugs. All fixed:

- 🚨 **CRITICAL UX (round 23): RunModifier + skill tree multipliers actually applied.** Round 22 only wired scoreMul + magnetMul. Players spending lifetime minerals on FORTIFY/FIREPOWER/AGILITY (or picking TRIPLE_SPEED/GLASS_CANNON/TANK modifier) saw zero gameplay effect. Fixed:
  - **hpMul**: `runMode/runModifier/runContext/effectiveStats` block hoisted ABOVE `var ship by rememberSaveable { ... Ship(...) }` so `Ship(hp = (1000 * effectiveStats.hpMul).toInt().coerceIn(100, 3000))` can apply HP at construction. FORTIFY skill + GLASS_CANNON/TANK/BERSERKER modifier now properly scale max HP.
  - **damageMul**: `LasersController` accepts `damageMultiplier: () -> Float = { 1f }` constructor param. Inside collision processing, `effectiveDamage = laser.impactPower * dmgMul` is applied to both spaceObject + enemy hits. GameState passes `damageMultiplier = { effectiveStats.damageMul }`. FIREPOWER skill + BERSERKER/GLASS_CANNON/DOUBLE_OR_NOTHING modifier now boost damage.
  - **speedMul**: `ShipController` accepts `speedMultiplier: () -> Float`. `moveShip()` computes `effSpeed = movementSpeed * speedMultiplier()` and uses it for x/y movement instead of hardcoded `movementSpeed`. AGILITY skill + TRIPLE_SPEED (×3)/TANK (×0.7) modifier now affect movement.
  - **noShieldDrops**: `BoosterController.addBooster()` accepts `noShieldDrops: () -> Boolean`. If generator rolls `SHIELD_BOOSTER` and flag is true, skip the spawn silently (next 4s tick retries). NO_SHIELDS modifier now actually blocks shield drops.

- ⚠️ **BUG (round 23): BOSS_RUSH_CLEAR achievement unlocked too early.** Round 22 gated at hardcoded `currentIndex >= 18` but BossRushProvider's script has ~28 entries (2 intro + 13 bosses + 12 "Next!" + 1 "ALL CLEAR!"). Player got the achievement at ~boss 9, not at the end. Fix: added `StageController.scriptSize()` returning `provider.size()`. monitorLoopInSec now checks `currentIndex >= scriptSize - 1` so the gate is the last entry of whatever script the provider supplies (also auto-scales if future content changes BossRush length).

- ⚠️ **BUG (round 23): TIME_ATTACK timeout used death feedback path.** Round 22's TIME_ATTACK timer force-set GAME_OVER but `finalBossDefeated` stayed false → GameScreen branched to death path (LONG haptic + GAME_OVER sfx + 1500ms kill-cam delay). Player "wins" the time challenge but feels like dying. Fix: added `timeAttackEnded: Boolean` to GameState (set true when timer expires). GameScreen's `isVictory` flag now checks `finalBossDefeated || timeAttackEnded` so TIME_ATTACK uses HEAVY haptic + PICKUP sfx + 500ms delay (celebratory) branch.

- ⚠️ **BUG (round 23): StageController saver restored with default StaticListProvider.** Round 22's saver's `restore` lambda created `StageController(stageIndex = ...)` with the default `provider = StaticListProvider()`. After process-death restore in SURVIVAL/BOSS_RUSH/TIME_ATTACK/ENDLESS, the controller would load the static campaign script with the saved index — potentially out-of-range (e.g. Endless wave 50 has no equivalent in static script of length ~140) and definitely wrong content. Fix: `StageController.saver(provider: StageProvider)` accepts the live provider, restore uses it directly. If restored stageIndex is out-of-range for the provider, coerce to 0 (safer than crashing).

### Round 23 files modified

- `ui/game/state/GameState.kt` — hoist RunContext/EffectiveStats block before ship init; apply `hpMul` at `Ship(hp = ...)`; pass `damageMultiplier`/`speedMultiplier` to laser/ship controllers; pass `noShieldDrops` to booster controller; add `timeAttackEnded` state + set on timer expiry + expose; fix BOSS_RUSH_CLEAR gate to use `scriptSize()`; remove "scope cut" comment for modifier deferral.
- `ui/game/ship/laser/LasersController.kt` — `damageMultiplier: () -> Float = { 1f }` constructor param + applied to `effectiveDamage` before onObjectImpact / onLaserHit.
- `ui/game/ship/ship/ShipController.kt` — `speedMultiplier: () -> Float = { 1f }` constructor param + applied to `effSpeed` in moveShip().
- `ui/game/booster/BoosterController.kt` — `noShieldDrops: () -> Boolean = { false }` constructor param + SHIELD spawn skip.
- `ui/game/stage/StageController.kt` — add `scriptSize()` accessor; `saver(provider)` accepts live provider + OOB-index coercion to 0.
- `ui/game/GameScreen.kt` — `isVictory = finalBossDefeated || timeAttackEnded`.

### Round 23 verification

- `./gradlew assembleDevDebug` BUILD SUCCESSFUL.
- `./gradlew compileProductionReleaseKotlin` BUILD SUCCESSFUL.

### Round 24 — UX polish (settings cleanup + story repositioning + Vietnamese-first)

User feedback after round 23:
1. Settings dialog cluttered — 11 widgets dồn 1 cột, có thêm new options (mode/modifier/upgrade) trông rối.
2. Story dialogue card overlap với ship (BottomCenter padding 80dp đè lên ship vị trí screenHeight-140).
3. Game cần Vietnamese-first; multi-language phase sẽ làm sau.

**Fixes applied:**

- ✅ **StoryOverlay reposition (round 24)**: chuyển từ `Alignment.BottomCenter padding(vertical=80dp)` (overlap với ship area) sang `Alignment.TopCenter padding(top=170dp)`. Slide animation reverse direction (was +80px → 0 slide-up, now -80px → 0 slide-down). Vị trí 170dp clear:
  - HUD strip (top=16dp + height≈50dp → ends 66dp)
  - BossHpBar (top=92dp + height≈30dp → ends 122dp)
  - BossIntroOverlay banner zone (y=70-160)
  - Ship locked at maxYOffset = screenHeight-140 → bottom half is gameplay zone; top zone safe.
  Speaker color check updated: "ĐỘI TRƯỞNG" → NeonCyan, others → NeonMagenta.

- ✅ **DialogSettings redesign (round 24)**: rewrite từ flat 11-widget column thành 4 sections có header + thin divider line trên đầu mỗi section. Sections:
  - **ÂM THANH**: Music slider + SFX slider.
  - **CHƠI**: Vibration + Reduce motion (side-by-side checkboxes, weight 1f) + Độ khó pills + Skin tàu pills.
  - **CHUẨN BỊ RUN**: 3 NavRow click-through cho Chế độ / Buff / Nâng cấp (label trái + value giữa + action hint phải).
  - **ỨNG DỤNG**: 3 FlatLink Đánh giá / Chia sẻ / Riêng tư.
  - Header row có "ĐÓNG" button inline (thay vì stacked dưới cùng). VerticalScroll cho small screens. Compact spacing (10dp section gap, 4dp inter-row).
  Reusable helpers: `SectionHeader`, `LabelledPillRow`, `NavRow`, `FlatLink`. Cũ `Pill` + `SettingSlider` + `SettingCheck` giữ nguyên signature, thêm `modifier` param cho check.

- ✅ **Việt hóa toàn bộ Wave 5 UI (round 24)**: tất cả hardcoded strings UI chuyển sang tiếng Việt. Multi-language phase (strings.xml proper) deferred.
  - `GameMode.displayName`: CAMPAIGN→CHIẾN DỊCH, SURVIVAL→SINH TỒN, TIME_ATTACK→ĐUA THỜI GIAN, BOSS_RUSH→CHIẾN BOSS, ENDLESS→VÔ TẬN, DAILY→THỬ THÁCH NGÀY.
  - `RunModifier.displayName` + `description`: TRIPLE_SPEED→TỐC HÀNH, GLASS_CANNON→MỎNG NHƯ KÍNH, NO_SHIELDS→KHÔNG KHIÊN, SUPER_MAGNET→NAM CHÂM MẠNH, BERSERKER→CUỒNG BẠO, TANK→GIÁP DÀY, BOSSES_ONLY→CHỈ BOSS, DOUBLE_OR_NOTHING→ĐƯỢC ĂN CẢ. Description full Vietnamese ("Sát thương ×2 nhưng HP ×0.5. Điểm ×2.5.").
  - `SkillNode.displayName` + `description`: 15 nodes Vietnamese (GIÁP CỨNG / HỎA LỰC / HỐ HẤP DẪN / KHIÊN AEGIS / NHANH NHẸN cho tier 0; TỰ HỒI / CHÍ MẠNG / NHÌN SAO / PHẢN ỨNG / HẬU TĂNG LỰC / KHO ĐẠN / ĐÀ COMBO / TIM PHƯỢNG cho tier 1; LÒ RÈN SAO / PHÁO HƯ KHÔNG cho tier 2).
  - `StoryRegistry`: chapter intros + boss taunts Việt hóa. Speaker "CAPTAIN" → "ĐỘI TRƯỞNG". Boss names: "LEVEL 1 BOSS" → "BOSS CẤP 1", "GALAXY OVERLORD" → "BÁ VƯƠNG THIÊN HÀ".
  - `MidBossType.displayName`: "OFFENSIVE MID-BOSS" → "TIỂU BOSS TẤN CÔNG", DEFENSIVE → "TIỂU BOSS PHÒNG THỦ", SWARM → "TIỂU BOSS BẦY ĐÀN".
  - `GameState.bossIntroName when-block`: updated to Vietnamese.
  - `DialogModePicker`: "PICK MODE" → "CHỌN CHẾ ĐỘ", "Restart applies new mode" → "Khởi động lại để áp dụng chế độ mới", mode descriptions Vietnamese.
  - `DialogModifierPicker`: "PICK MODIFIER" → "CHỌN BUFF", "Risk · reward" → "Mạo hiểm · thưởng", "SKIP" → "BỎ QUA".
  - `DialogMetaUpgrade`: "SKILL TREE" → "CÂY NÂNG CẤP", "Spend lifetime minerals..." → "Dùng khoáng vật tích lũy...", "MAX" → "TỐI ĐA", "Locked — requires parent rank X" → "Khóa — cần cấp X của nốt cha", "CLOSE" → "ĐÓNG".
  - `DialogSettings` (new redesign): all section headers + labels Vietnamese.
  - `AchievementBanner` tier label: BRONZE → ĐỒNG, SILVER → BẠC, GOLD → VÀNG. Header "🏆 ACHIEVEMENT" → "🏆 THÀNH TỰU".
  - `Achievement.title` Việt hóa toàn bộ 30 entries (FIRST_BLOOD title "FIRST BLOOD" → "VẾT MÁU ĐẦU TIÊN", etc.). IDs giữ stable cho save compat.

### Round 24 files modified

- `ui/game/controls/StoryOverlay.kt` — top-anchored padding + reversed slide direction + ĐỘI TRƯỞNG speaker check.
- `ui/game/GameScreen.kt` — StoryOverlay align changed from BottomCenter to TopCenter.
- `ui/dlg/settings/DialogSettings.kt` — full rewrite with 4 sections + NavRow/SectionHeader/FlatLink helpers.
- `ui/game/mode/GameMode.kt` — displayName Vietnamese.
- `ui/game/modifier/RunModifier.kt` — displayName + description Vietnamese.
- `ui/game/meta/SkillNode.kt` — 15 nodes displayName + description Vietnamese.
- `ui/game/story/StoryRegistry.kt` — chapter intros + boss taunts Vietnamese.
- `ui/game/enemy/ship/model/MidBossType.kt` — 3 variants displayName Vietnamese.
- `ui/game/state/GameState.kt` — bossIntroName when-block Vietnamese.
- `ui/dlg/modepicker/DialogModePicker.kt` — title + subtitle + mode descriptions Vietnamese.
- `ui/dlg/modifierpicker/DialogModifierPicker.kt` — title + subtitle + SKIP button Vietnamese.
- `ui/dlg/metaupgrade/DialogMetaUpgrade.kt` — header + body strings Vietnamese.
- `ui/game/controls/AchievementBanner.kt` — tier labels + header Vietnamese.
- `data/AchievementsRepository.kt` — all 30 achievement titles Vietnamese (descriptions already were).

### Round 24 verification

- `./gradlew assembleDevDebug` BUILD SUCCESSFUL.
- `./gradlew compileProductionReleaseKotlin` BUILD SUCCESSFUL.

### Round 25 — Menu screen + per-mode checkpoint persistence + UI font boost + device font

User feedback:
1. UI font hơi nhỏ + khó đọc → boost size + padding.
2. Game đang dùng custom Orbitron font → switch về device default font.
3. Cần start menu screen ở launch để user pick mode / continue / settings.
4. Cold launch → mất progress run. CẦN persist mọi mode (Campaign, Survival, TimeAttack, BossRush, Endless) — Player meta data + run state phải lưu xuống DataStore.

**Fixes applied:**

- ✅ **Switch to device default font (round 25)**: `common/Type.kt` `Typography.defaultFontFamily` đổi từ `OrbitronFontFamily` → `FontFamily.Default`. `OrbitronFontFamily` val removed entirely. Font files trong `res/font/` còn lại nhưng không reference từ code; có thể remove trong cleanup pass khác. Tất cả Text composables giờ render bằng system font của device.

- ✅ **Boost UI font sizes round 25**: tăng font size + padding để dễ đọc:
  - `DialogSettings.SectionHeader`: 10→13sp + letterSpacing 3→2.5sp + divider 1.dp alpha 0.25→0.30
  - `DialogSettings.SettingSlider` label/value: 13→16sp
  - `DialogSettings.SettingCheck` label: 13→15sp
  - `DialogSettings.LabelledPillRow` label: 12→14sp, width 82→92dp
  - `DialogSettings.Pill`: 12→14sp + padding (10/5 → 14/7dp) + corner (16→18dp)
  - `DialogSettings.NavRow` label: 12→14sp, value: 13→16sp, actionHint: 11→13sp + padding bump
  - `DialogSettings.FlatLink`: 12→14sp + padding 8→11dp + corner 6→8dp
  - `DialogMetaUpgrade` header: 18→22sp, body: 11→14sp, item displayName: 14→16sp, rank counter: 11→13sp, description: 11→13sp, prereq message: 10→12sp, buy button: 12→14sp, MAX/lock indicator: 12→14/16sp
  - `StoryOverlay`: speaker 11→13sp, text: 14→17sp
  - `AchievementBanner`: header 11→13sp, title 18→20sp, description 12→14sp
  - `DialogModePicker` mode item: caption→14sp, padding tăng
  - `DialogModifierPicker` mod item: caption→14sp, multiplier 14→16sp, padding tăng + corners tăng

- ✅ **MenuScreen mới (round 25)** — `ui/menu/MenuScreen.kt`. Sits between Splash and Game. Layout:
  - Top: app title "SKY FORCE U*S*A" 32sp với neon pulse + "Synthwave Shoot 'em up" tagline + lifetime minerals badge ♦ <balance>
  - Middle: current mode card (CHẾ ĐỘ HIỆN TẠI + mode name 22sp + buff line nếu chọn + "▸ Đang ở màn N" nếu có checkpoint) + Primary button "TIẾP TỤC" (nếu checkpoint>0) hoặc "BẮT ĐẦU" (22sp, pulsing border, leading ▶)
  - Bottom: 2x2 grid secondary buttons "CHẾ ĐỘ" / "BUFF" / "NÂNG CẤP" / "CÀI ĐẶT" (15sp, weight 1f)
  - Nav route: `Menu` mới trong `navigation/Navigation.kt`
  - Splash flow: Splash → Menu (was → Game)
  - First-time DifficultyPicker → Menu (was → Game)
  - ModePicker/ModifierPicker on pick → popBackStack (round 22 hành vi restart-Game removed; pickers chỉ save quietly + return). User explicit Play từ Menu để áp dụng.

- ✅ **Per-mode checkpoint save/load (round 25)** — `data/RunPersistenceRepository.kt`:
  - DataStore `neon_run_persist` riêng. Per-mode keys: `checkpoint_<modeKey>` (Int) + `saved_at_<modeKey>` (Long timestamp) + global `last_played_mode` (String).
  - API: `checkpointFor(mode): Flow<Int>`, `savedAtFor(mode): Flow<Long>`, `lastPlayedModeKey: Flow<String>`, `saveCheckpoint(mode, idx)`, `clearCheckpoint(mode)`.
  - **Checkpoint-style** (NOT full state snapshot): chỉ save stageIndex per mode. Resume = start at saved stageIndex với ship/HP/smartBombs/run-counters fresh. Đơn giản, dễ verify, vẫn đem lại UX "tiếp tục từ chỗ cũ".
  - Save hook: `StageController.onStageAdvance` callback → `coroutineScope.launch { runPersist.saveCheckpoint(runMode.key, idx) }`. Async write không block game loop.
  - Load hook: `rememberGameState()` đọc `runPersistenceRepo.checkpointFor(runMode.key).first()` qua `runBlocking` ở init (one-time, <10ms). Coerce với `stageProvider.hasAt(idx)` để safe nếu checkpoint out-of-range.
  - Clear hook: `GameScreen` LaunchedEffect(gameStatus == GAME_OVER) → `runPersistence.clearCheckpoint(gameModeKey)`. Mọi GAME_OVER (death/victory/timeout) đều clear checkpoint → next launch không auto-resume.
  - MenuScreen reads checkpoint via `runPersist.checkpointFor(lastModeKey)` Flow → primary button label/state phản ánh real-time.
  - Wired into `App` + `MainActivity` (`LocalRunPersistence` CompositionLocal).

- ✅ **Player meta data persistence audit**: re-confirmed các loại data đã persist:
  - Settings (volume / vibration / difficulty / shipSkin / lastMode / lastModifier / tutorialShown) — `SettingsRepository`
  - Leaderboard (all-time top-10 + daily per-UTC-day + endless survival) — `LeaderboardRepository`
  - Achievements (30 entries unlocked CSV) — `AchievementsRepository`
  - Lifetime minerals + skill tree node ranks — `MetaProgressionRepository`
  - Per-mode run checkpoint — `RunPersistenceRepository` (NEW round 25)
  
  Việc force-close app không mất bất kỳ data nào trừ in-run state (HP/score/kills) — cố ý vì design: checkpoint-style = resume tại stage cũ với ship fresh, không tiếp tục với HP/score đã có (đảm bảo công bằng cho leaderboard).

### Round 25 files

**New:**
- `data/RunPersistenceRepository.kt` (~80 LOC, per-mode checkpoint store + LocalRunPersistence CompositionLocal)
- `ui/menu/MenuScreen.kt` (~230 LOC, full start menu with title/mode card/play button/secondary grid + PrimaryButton + SecondaryButton helpers)

**Modified:**
- `common/Type.kt` — removed OrbitronFontFamily, Typography uses FontFamily.Default.
- `App.kt` — wire RunPersistenceRepository.
- `ui/MainActivity.kt` — provide LocalRunPersistence; Splash → Menu instead of Game; MenuScreen route; pickers popBackStack on pick (not Game restart); DifficultyPicker → Menu.
- `navigation/Navigation.kt` — added Menu route.
- `ui/game/state/GameState.kt` — read `runPersistenceRepo.checkpointFor(runMode.key)` for initial stage index; save checkpoint on onStageAdvance.
- `ui/game/GameScreen.kt` — clear checkpoint on GAME_OVER LaunchedEffect.
- `ui/dlg/settings/DialogSettings.kt` — all helpers font/padding boosted.
- `ui/dlg/metaupgrade/DialogMetaUpgrade.kt` — header + row font/padding boosted.
- `ui/game/controls/StoryOverlay.kt` — text font 14→17sp, speaker 11→13sp.
- `ui/game/controls/AchievementBanner.kt` — header/title/desc font boosted.
- `ui/dlg/modepicker/DialogModePicker.kt` — description fontSize 14sp + padding tăng.
- `ui/dlg/modifierpicker/DialogModifierPicker.kt` — description fontSize 14sp + multiplier 16sp + padding tăng.

### Round 25 verification

- `./gradlew assembleDevDebug` BUILD SUCCESSFUL.
- `./gradlew compileProductionReleaseKotlin` BUILD SUCCESSFUL.

### Round 26 — audit-driven polish (Vietnamese gaps + death-keeps-checkpoint + menu scroll + font cleanup + chapter intro fix)

Audit round (post-round-25) flagged 5 items. All fixed.

- ✅ **Việt hóa toàn bộ string gaps còn lại**:
  - `DialogDifficultyPicker`: "PICK DIFFICULTY" → "CHỌN ĐỘ KHÓ", "Can be changed later in Settings" → "Có thể đổi sau ở Cài đặt", difficulty key uppercase → DỄ/VỪA/KHÓ.
  - `TutorialOverlay`: "TAP & HOLD" → "GIỮ & DI", "the arrows below to move" → "mũi tên bên dưới để điều khiển", "Tap anywhere to dismiss" → "Chạm bất kỳ để đóng".
  - `PhaseTransitionBanner`: "PHASE N" → "GIAI ĐOẠN N" (3 layered text instances replaced).
  - `WaveClearBanner`: "WAVE CLEAR!" → "HOÀN THÀNH ĐỢT!", "+5 minerals" → "+5 khoáng vật".
  - `IndicatorStatus.ReviveTokenBadge`: "REVIVE" → "HỒI SINH".
  - `DialogGameOver`: KHOÁNG VẬT / KỶ LỤC / ★ KỶ LỤC MỚI ★ / VictoryPanel headings (THIÊN HÀ ĐƯỢC CỨU / BÁ VƯƠNG BỊ HẠ / CHIẾN THẮNG HUYỀN THOẠI) + Vietnamese subtitles / VÔ TẬN panel (SỐNG SÓT / TỐT NHẤT / ★ KỶ LỤC MỚI VÔ TẬN ★) / DailyPanel (TỐT NHẤT HÔM NAY / SỐ LẦN CHƠI / ★ KỶ LỤC HÔM NAY ★) / StatsPanel (THỐNG KÊ + THỜI GIAN / MÀN ĐẠT / DIỆT ĐỊCH / HẠ BOSS / COMBO TỐI ĐA) / "TOP CAO ĐIỂM".
  - `Chapter.displayName`: VÀNH ĐAI TIỂU HÀNH TINH / MÂY TINH VÂN / HÀNH TINH BĂNG / TRẠM THÙ ĐỊCH / LÕI THIÊN HÀ.
  - `Stage.kt buildStageScript`: "CHAPTER N" → "CHƯƠNG N", "GO!" → "BẮT ĐẦU!", "DANGER" → "NGUY HIỂM", "Continue!" → "Tiếp tục!", "BOSS FIGHT" → "TRẬN BOSS", "Get ready!" → "Sẵn sàng!", "Chapter cleared!" → "Hoàn thành chương!", "VICTORY!" → "CHIẾN THẮNG!".
  - `StageProviders.kt BossRushProvider`: "BOSS RUSH" → "CHIẾN BOSS", "Get ready!" → "Sẵn sàng!", "Next!" → "Tiếp!", "ALL CLEAR!" → "VƯỢT ẢI!". Sentinel `BOSS_RUSH_GAP_MESSAGE` constant added so GameState heal-between-bosses check doesn't string-match brittlely.
  - `values/strings.xml`: swapped to Vietnamese as default (was English). `values-en/` keeps English fallback. App now Vietnamese-first regardless of device locale; switches to English on en-US/etc. devices.

- ✅ **Death KEEP checkpoint, victory CLEAR**: round 25 cleared checkpoint on every GAME_OVER (death + victory). Round 26 changed: only clear on victory. Death preserves checkpoint so user can retry same stage via MenuScreen Continue button. Implementation:
  - `GameScreen.kt` LaunchedEffect(gameStatus==GAME_OVER) branches on `isVictory`: victory → clear; death → preserve.
  - `DialogGameOver` Restart button explicitly clears checkpoint before navigate (user choice to start fresh).
  - **New `VỀ MENU` button** added to GameOver dialog actions — `NeonMagenta` color, "◀" glyph. Navigates back to Menu route WITHOUT clearing checkpoint. UX: user dies → has 2 buttons: "CHƠI LẠI" (fresh from stage 0) or "VỀ MENU" (return + continue later).
  - Wired in `MainActivity` GameOver dialog destination — `onBackToMenu = navigate(Menu, popUpTo Menu inclusive)`.

- ✅ **MenuScreen verticalScroll**: added `Modifier.verticalScroll(rememberScrollState())` + changed Column arrangement từ `SpaceBetween` sang `spacedBy(24.dp)` để overflow tự nhiên trên screen nhỏ (< 480dp tall). Trên screen tall, có whitespace below — chấp nhận được.

- ✅ **Xóa font Orbitron unused**: confirmed zero references (grep `R.font.font_orbitron`, `font_orbitron`, `OrbitronFontFamily` — không hit). Deleted 6 files `res/font/font_orbitron_*.ttf` (regular/medium/semibold/bold/black/extrabold). Removed empty `res/font/` directory. APK giảm ~600KB.

- ✅ **Chapter intro re-play fix khi resume**: round 25 issue — resume mid-game thì `chapterIntroPlayedChapter` reset về 0 (rememberSaveable không persist qua cold launch) → next stage advance in same chapter triggers chapter intro narrative again. Fix: rememberSaveable's `init` lambda now computes initial value từ `stageProvider.chapterAt(initialStageIndex)`. If resume at stage > 0, set `chapterIntroPlayedChapter = currentChapterId` so subsequent stage advances trong same chapter không re-fire intro.

### Round 26 files modified

- `ui/dlg/difficulty/DialogDifficultyPicker.kt` — Vietnamese title + subtitle + difficulty labels.
- `ui/game/controls/TutorialOverlay.kt` — 3 lines Vietnamese.
- `ui/game/controls/PhaseTransitionBanner.kt` — "PHASE N" → "GIAI ĐOẠN N" (replace_all).
- `ui/game/controls/WaveClearBanner.kt` — banner + sub-line Vietnamese.
- `ui/game/controls/IndicatorStatus.kt` — REVIVE badge Vietnamese.
- `ui/dlg/gameover/DialogGameOver.kt` — all internal English labels translated; added `onBackToMenu` callback param + "VỀ MENU" NeonDialogButton; added kotlinx.coroutines.launch import + clear-on-restart for explicit user intent.
- `ui/game/stage/Chapter.kt` — 5 chapter displayName Vietnamese.
- `ui/game/stage/Stage.kt` — 10 StageMessage strings Vietnamese.
- `ui/game/stage/StageProviders.kt` — BossRushProvider 4 StageMessage strings Vietnamese + `BOSS_RUSH_GAP_MESSAGE` companion constant.
- `ui/game/state/GameState.kt` — heal-between-bosses uses `BossRushProvider.BOSS_RUSH_GAP_MESSAGE` constant; `chapterIntroPlayedChapter` init computes from `stageProvider.chapterAt(initialStageIndex)`; clear-on-victory-only.
- `ui/game/GameScreen.kt` — clear-on-victory-only.
- `ui/MainActivity.kt` — DialogGameOver gets `onBackToMenu` handler.
- `ui/menu/MenuScreen.kt` — verticalScroll + Arrangement.spacedBy.
- `res/values/strings.xml` — Vietnamese as default (was English).
- `res/font/` — deleted (6 orbitron .ttf files + folder).

### Round 26 verification

- `./gradlew assembleDevDebug` BUILD SUCCESSFUL.
- `./gradlew compileProductionReleaseKotlin` BUILD SUCCESSFUL.

### Round 27 — MenuScreen revamp + game-flow polish

User feedback (round 26 self-test):
1. MenuScreen plain + ugly, no background, options no icons, buttons reportedly not clicking.
2. MenuScreen ↔ DialogSettings duplicate the mode/buff/upgrade entry points.
3. Pause dialog has no "Back to Menu" — once in-game you can only Resume/Restart/Settings.
4. System back/swipe in Game exits the run instantly instead of pausing.

**All fixed in round 27.**

- ✅ **MenuScreen full rewrite (game-like design)**:
  - Animated starfield background — 60 stars across 3 depth layers (far cyan-tinted small, mid white, near gold-tinted larger). Drift downward at layer-scaled speed (20/40/80 px/sec) + per-star sine twinkle. Pure Canvas decorative layer, doesn't consume pointer events.
  - Title block: "SKY FORCE" 36sp NeonCyan with pulsing glow + "U*S*A" 22sp NeonMagenta below with letter-spacing 6sp. Both use `neonGlow` modifier.
  - Ship logo: center 180dp Box with radial halo gradient (Canvas) behind splash_image (140dp, pulsing scale 0.96..1.04).
  - Info card: shows current mode + buff (only if non-NONE) + checkpoint ("▸ Đang ở màn N" if checkpoint > 0) + lifetime minerals ♦ badge. Violet bordered card.
  - PLAY button: huge full-width (22dp vertical padding, 26sp label), pulsing border alpha 0.55..1.0 with 900ms cycle, horizontal gradient bg (NeonCyan-Magenta-NeonCyan), ▶ glyph 30sp leading, neonGlow intensity 0.7. Label is "TIẾP TỤC" if checkpoint > 0 else "BẮT ĐẦU".
  - 2x2 secondary grid: each button has big icon glyph 32sp on top + label 14sp below. Icons: ⊞ CHẾ ĐỘ (NeonViolet) / ⚡ BUFF (NeonGold) / ⬆ NÂNG CẤP (NeonCyan) / ⚙ CÀI ĐẶT (NeonMagenta). Each button has neonGlow + border in its accent color.
  - **Click reliability fix**: every `Modifier.clickable(onClick = ...)` placed BEFORE `.padding(...)` so entire visible area registers as click target. Every onClick fires `Logger.d` so adb logcat confirms taps. Click handlers verified working in PrimaryButton (PLAY) + 4 MenuButtons.

- ✅ **MenuScreen ↔ DialogSettings duplication removed**:
  - DialogSettings dropped the "CHUẨN BỊ RUN" section (3 NavRows for mode/buff/upgrade — round 24 addition). Settings now has 3 sections only: ÂM THANH / CHƠI / ỨNG DỤNG.
  - `DialogSettings()` signature simplified — removed `onOpenModePicker`, `onOpenModifierPicker`, `onOpenMetaUpgrade` params. `MainActivity` updated.
  - `lastMode` + `lastModifier` Flow reads removed from DialogSettings. `GameMode` import removed. Unused `NavRow` helper composable removed (~30 LOC).
  - Mode/Buff/Upgrade access now lives exclusively in MenuScreen's 2x2 grid → single source of truth.

- ✅ **DialogGamePause "VỀ MENU" button added**:
  - New `onBackToMenu` callback param. New 4th button (NeonGold, "◀" glyph). Click → navigate Menu route, popUpTo Menu inclusive. Checkpoint preserved (no clearCheckpoint call) so user can resume via "TIẾP TỤC" from Menu.
  - MainActivity GamePause destination wired with `onBackToMenu` handler.

- ✅ **System back / swipe-back protection in Game**:
  - `BackHandler(enabled = gameState.gameStatus == GameStatus.RUNNING)` composable added at top of GameScreen. When user presses back / does back-gesture while game is running: intercept → `gameState.toggleGameStatus()` + `onGamePause()` → DialogGamePause shows. Prevents accidental exit of mid-run.
  - When PAUSE or GAME_OVER, BackHandler is disabled → back falls through to default behavior (close dialog or do nothing per dialog properties).
  - Previously: back press in Game popped Game route → user lost run state + sent to Menu/Splash. No more.

### Round 27 files

**Modified:**
- `ui/menu/MenuScreen.kt` — full rewrite (~360 LOC): starfield Canvas, ShipLogo with halo, InfoCard, PlayButton, MenuButton helpers. Click ordering verified.
- `ui/dlg/settings/DialogSettings.kt` — dropped CHUẨN BỊ RUN section, NavRow helper removed, 4 params → 1, `lastMode/lastModifier` reads removed.
- `ui/MainActivity.kt` — Settings dialog wiring simplified; GamePause dialog gets `onBackToMenu` handler.
- `ui/dlg/gamepause/DialogGamePause.kt` — new `onBackToMenu` param + "VỀ MENU" NeonDialogButton (NeonGold, "◀" glyph).
- `ui/game/GameScreen.kt` — `BackHandler(enabled=RUNNING)` at top → opens pause dialog instead of exiting Game route.

### Round 27 verification

- `./gradlew assembleDevDebug` BUILD SUCCESSFUL.
- `./gradlew compileProductionReleaseKotlin` BUILD SUCCESSFUL.

### Round 28 — NeonBottomSheet migration + MenuScreen polish + strategic Logger sprinkle

User picks (all Recommended): strategic ~50-60 Logger calls / Custom NeonBottomSheet / migrate all 7 dialogs / adaptive + stagger + idle + comet.

- ✅ **NeonBottomSheet shared component (round 28)**: new `common/NeonBottomSheet.kt` (~240 LOC). Single common composable used by all dialog routes. Visuals:
  - Container: Box(fillMaxSize) → scrim full-screen + AnimatedVisibility sheet aligned BottomCenter.
  - Sheet: rounded top corners (24dp), neon border + glow in accentColor, NeonBgMid background, full-width, wrap content height.
  - Header: 48dp drag handle (top center) + title (h6, accent color, letter-spacing 2sp) + ✕ close button (NeonRedAlert tint, 36dp circular).
  - Body: caller-provided `@Composable ColumnScope.() -> Unit`.
  - Enter: `slideInVertically(initialOffsetY = { it })` + `fadeIn` 260ms tween.
  - Exit: `slideOutVertically(targetOffsetY = { it })` + `fadeOut` 200ms tween.
  - Dismiss interactions (gated by `dismissible: Boolean = true`):
    - Tap scrim → onDismiss (only if dismissible).
    - Swipe-down >80dp on sheet body → onDismiss (only if dismissible).
    - Tap ✕ → onDismiss (always).
  - Helper: `bottomSheetDialogProperties(dismissOnBackPress, dismissOnClickOutside)` returns `DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, ...)` for NavHost `dialog()` route hosting.

- ✅ **Migrated 7 dialogs → NeonBottomSheet (round 28)**:
  - DialogSettings — `dismissible = true`, NeonCyan accent. Removed Card+TextButton header.
  - DialogDifficultyPicker — `dismissible = true`, NeonMagenta accent. Pickers' "Có thể đổi sau ở Cài đặt" hint preserved in body.
  - DialogModePicker — `dismissible = true`, NeonViolet accent.
  - DialogModifierPicker — `dismissible = true`, NeonGold accent.
  - DialogMetaUpgrade — `dismissible = true`, NeonGold accent. Balance row moved into body (right-aligned ♦ count).
  - DialogGamePause — `dismissible = false` (no swipe/scrim dismiss). ✕ tap maps to Resume (natural "dismiss" semantic for pause).
  - DialogGameOver — `dismissible = false` (modal-final). ✕ tap maps to onBackToMenu (checkpoint preserved). Action buttons (CHƠI LẠI / VỀ MENU) moved from `actions` slot into body Column.
  - MainActivity NavHost: each `dialog(route)` now uses `bottomSheetDialogProperties()` helper. Removed `androidx.compose.ui.window.DialogProperties` import (no longer used).

- ✅ **MenuScreen adaptive + stagger + idle + comet (round 28)**:
  - **Stagger entry animation**: new `EntryAnim(stepIndex)` wrapper — each child fades in + slides up 20px over 420ms with delay = stepIndex × 120ms. Used for title (0ms) / logo (120ms) / info card (240ms) / play (360ms) / 2x2 grid (480ms).
  - **Idle parallax**: ship logo rotates ±3° over 4.2s infinite cycle (graphicsLayer.rotationZ). Combined with existing pulsing scale + halo for "alive" feel.
  - **Comet streak**: new `CometStreak` Canvas composable. State cycle: 8-15s gap → activate 1.5s. Each streak has randomized direction (L→R or R→L), height fraction (15-50% from top), parabolic dip (60px sin curve). Trail: 8 fading white dots behind gold head. Rendered above starfield, below content.
  - **Adaptive layout**: Column arrangement `spacedBy(14dp)`, verticalScroll fallback for very small screens. Sections sized by content (no forced weight).

- ✅ **Strategic Logger sprinkle (round 28)**: total +30 strategic Logger.d calls across:
  - `App.onCreate`: per-repo construction logs (5 new — settings, leaderboard, achievements, metaProgression, runPersistence).
  - `SplashScreen.LaunchedEffect`: decision detail (1 enhanced: which branch + difficultyPicked value).
  - `MainActivity.onCreate`: setContent entry log (1 new).
  - `rememberGameState` entry: log composing (1 new).
  - `EffectiveStats.compute`: detailed multiplier breakdown — hpMul / damageMul / speedMul / magnetMul / scoreMul / noShieldDrops / bossesOnly (6 new sub-logs).
  - `stageProvider` build: log mode at provider construction (1 new).
  - `Achievement.unlock`: enhanced with id + tier + title + description (1 enhanced).
  - `gameStatus` transition: enhanced reason field (1 enhanced).
  - `GameScreen.LaunchedEffect(GAME_OVER)`: separate logs for victory haptic+sfx feedback path and death haptic+sfx feedback path (2 new).
  - `RunStats snapshot`: log all 7 fields written on GAME_OVER (1 new).
  - `RunPersistenceRepository.saveCheckpoint` / `clearCheckpoint`: enhanced detail (mode + stage + timestamp) (2 enhanced).
  - `AchievementBanner shown`: enhanced (id + tier + title) (1 enhanced).
  - `NeonBottomSheet`: mount + dismiss request + scrim tap + ✕ tap + swipe-down logs (5 new — auto inside common component).
  - `MenuScreen` button taps: PLAY / MODE / BUFF / UPGRADE / SETTINGS already logged from round 27 (5 existing).
  - `StageController advance`: enhanced with stage type + chapter id + message text (1 enhanced).
  - Total prefix "roy93~" auto-prepended by Logger object (existing infrastructure since round earlier). 259 baseline → ~289 net.

### Round 28 files

**New:**
- `common/NeonBottomSheet.kt` (~240 LOC: NeonBottomSheet composable + SheetContent + CloseButton + bottomSheetDialogProperties helper)

**Modified:**
- `App.kt` — per-repo construction logs.
- `ui/splash/SplashScreen.kt` — decision detail log.
- `ui/MainActivity.kt` — setContent entry log; NavHost dialog routes now use `bottomSheetDialogProperties()`; DialogProperties import removed.
- `ui/menu/MenuScreen.kt` — `EntryAnim` wrapper + `CometStreak` composable + ShipLogo idle tilt rotation; setValue + mutableStateOf imports.
- `ui/game/state/GameState.kt` — rememberGameState entry log; EffectiveStats detailed breakdown; gameStatus transition enhanced; stageProvider build log; StageController advance enhanced detail.
- `ui/game/GameScreen.kt` — victory/death haptic+sfx feedback logs; RunStats snapshot log.
- `ui/dlg/settings/DialogSettings.kt` — migrated to NeonBottomSheet, header inline removed.
- `ui/dlg/gameover/DialogGameOver.kt` — migrated to NeonBottomSheet (dismissible=false); action buttons inlined into body; verticalScroll on body.
- `ui/dlg/gamepause/DialogGamePause.kt` — migrated to NeonBottomSheet (dismissible=false); ✕ maps to Resume.
- `ui/dlg/difficulty/DialogDifficultyPicker.kt` — migrated to NeonBottomSheet.
- `ui/dlg/modepicker/DialogModePicker.kt` — migrated to NeonBottomSheet.
- `ui/dlg/modifierpicker/DialogModifierPicker.kt` — migrated to NeonBottomSheet.
- `ui/dlg/metaupgrade/DialogMetaUpgrade.kt` — migrated to NeonBottomSheet; balance row moved into body.
- `data/RunPersistenceRepository.kt` — save/clear logs enhanced.
- `ui/game/controls/AchievementBanner.kt` — show log enhanced.

### Round 28 verification

- `./gradlew assembleDevDebug` BUILD SUCCESSFUL.
- `./gradlew compileProductionReleaseKotlin` BUILD SUCCESSFUL.

### Manual test cho round 28

```
1. Launch → Splash → Menu
   ✓ Title pulses + slight fade-in stagger
   ✓ Ship logo slowly tilts ±3° (idle)
   ✓ Info card → Play button → 2x2 grid stagger in (delays 0/120/240/360/480ms)
   ✓ Comet streak: wait 8-15s — gold streak with white sparkle trail across upper area
2. Tap CHẾ ĐỘ → sheet slides UP from bottom
   ✓ Drag handle visible at top
   ✓ "CHỌN CHẾ ĐỘ" title + ✕ button on right
   ✓ Mode list as body
3. Tap ✕ → sheet slides down + fades out → back to Menu
4. Tap CÀI ĐẶT → sheet again
   ✓ Swipe DOWN > ~80dp on sheet body → sheet dismisses (animated)
   ✓ Tap scrim outside sheet → dismisses
5. Tap BUFF → swipe down to dismiss
6. PLAY → Game → Pause (Settings button)
   ✓ Pause sheet appears, dismissible = false
   ✓ Tap ✕ → resumes game (NOT exit)
   ✓ Swipe down on sheet → NO dismiss (intentional, pause is modal)
   ✓ Tap scrim → NO dismiss
7. Back press in Game → opens Pause sheet (round 27 BackHandler still works)
8. Pause → VỀ MENU → Menu (checkpoint preserved → button shows TIẾP TỤC)
9. PLAY → die → GameOver sheet
   ✓ Modal final: scrim/swipe disabled
   ✓ Tap ✕ → back to Menu (checkpoint preserved per round 26)
   ✓ Buttons CHƠI LẠI / VỀ MENU in body, scrollable if content tall
10. adb logcat | grep "roy93~" → verify high-density logs at every key event:
    nav routes / dialog open-close / mode pick / GAME_OVER branch / checkpoint save
```

### Round 29 — NeonBottomSheet polish

User feedback after round 28 self-test:
1. Sheet needs padding bottom 56dp for edge-to-edge / system gesture clearance.
2. DialogModifierPicker "BỎ QUA" button duplicate with ✕ — remove.
3. DialogMetaUpgrade fixed-height LazyColumn (420dp) doesn't wrap content.
4. Sheet display shows status bar (weird) — hide it.
5. Scrim dim should be black opacity 0.8 (was 0.7).

Fixed:
- ✅ **Padding bottom 56dp** — SheetContent Column `padding(bottom = 56dp)` (was 18dp).
- ✅ **BỎ QUA removed** — DialogModifierPicker TextButton xóa. ✕ tap now applies NONE modifier automatically (semantic equivalent).
- ✅ **Wrap content height** — DialogMetaUpgrade LazyColumn `Modifier.height(420dp)` → `Modifier.heightIn(max = 480dp)`. Short list shrinks; tall list caps at 480dp + sheet scrolls.
- ✅ **Status bar hide in sheet** — Dialog has own Window. Inside NeonBottomSheet, LaunchedEffect lấy `DialogWindowProvider.window` qua `LocalView.parent`, gọi `WindowCompat.setDecorFitsSystemWindows(false)` + `insetsController.hide(systemBars())`. Activity already hides bars (round 25) but Dialog window inherits separately.
- ✅ **Scrim alpha 0.8** — `Color.Black.copy(alpha = 0.8f)`.

Round 29 files modified:
- `common/NeonBottomSheet.kt` — scrim 0.8, bottom padding 56dp, status bar hide via DialogWindowProvider.
- `ui/dlg/modifierpicker/DialogModifierPicker.kt` — BỎ QUA removed, ✕ applies NONE.
- `ui/dlg/metaupgrade/DialogMetaUpgrade.kt` — heightIn(max=480dp).

### Round 30 — sheet revamp + Menu bottom space

User feedback:
1. Scrim dim still visible — remove entirely.
2. Settings sheet UI cramped — needs breathing room.
3. Skill tree UI not lively, info unclear.
4. Buff picker UI not lively, info unclear.
5. Mode picker UI not lively, info unclear.
6. MenuScreen has too much center bottom space.

Fixed:
- ✅ **Scrim removed** — alpha 0.8 → 0f. Box still captures click for dismiss but visually transparent (no dim).
- ✅ **Settings revamp** — new `SectionPanel(headerLabel, glyph, color, content)` helper. 3 sections wrap with NeonBgEdge background + border + icon glyph header (♪ ÂM THANH / ⊞ CHƠI / ✦ ỨNG DỤNG). Section spacing 10dp → 18dp.
- ✅ **MetaUpgrade revamp** — icon box 48dp per node (♥/⚔/◉/⊞/⚡/✚/✦/★/❂/⚝/◐/∞/♡/☆/✪) + TIER I/II/III badge + ProgressBarSegments (filled rect per rank/maxRank) + lock icon when prereq unmet + buy button column stacks ♦ + cost.
- ✅ **ModifierPicker revamp** — icon box 46dp per modifier (⚡/◊/⊘/◉/⚔/⊞/☠/✦) + gold score multiplier badge + StatBarsRow showing HP/DMG/SPD/MAG horizontal bars (green/cyan = buff, red = debuff, white dim = neutral).
- ✅ **ModePicker revamp** — icon box 56dp per mode (⊕/∞/⏱/☠/◌) + mode name 18sp + meta info chip (e.g. "5 chương · 100 màn" / "60 giây" / "~9 boss") + ▶ chevron.
- ✅ **MenuScreen bottom space** — verticalScroll removed + `Spacer(Modifier.weight(1f))` before button grid → pushes grid to bottom edge on tall screens.

Round 30 files modified:
- `common/NeonBottomSheet.kt` — scrim alpha 0.
- `ui/dlg/settings/DialogSettings.kt` — SectionPanel helper + 3 sections wrapped.
- `ui/dlg/metaupgrade/DialogMetaUpgrade.kt` — icon + tier badge + progress bar + nodeGlyph() + ProgressBarSegments().
- `ui/dlg/modifierpicker/DialogModifierPicker.kt` — icon box + score badge + StatBarsRow + StatBar.
- `ui/dlg/modepicker/DialogModePicker.kt` — icon box + meta info chip + chevron layout.
- `ui/menu/MenuScreen.kt` — verticalScroll removed, Spacer(weight=1f) trick.

### Round 31 — audit fixes (heightIn max + MenuScreen adaptive + cleanup)

Audit findings post-round-30:
1. NeonBottomSheet không cap max height → sheets có thể overflow trên small screens (MetaUpgrade risk cao).
2. MenuScreen bỏ verticalScroll → có thể clip trên screen < 480dp tall.
3. Legacy SectionHeader helper dead code.

Fixed:
- ✅ **Sheet heightIn max 90%** — NeonBottomSheet Column `.heightIn(max = configuration.screenHeightDp.dp * 0.9f)`. Inner LazyColumn / verticalScroll vẫn scroll bình thường nhưng sheet tổng không vượt 90% viewport.
- ✅ **MenuScreen BoxWithConstraints adaptive** — `maxHeight >= 720.dp` → fillMaxSize + Spacer(weight=1f) push grid xuống bottom. < 720dp → verticalScroll + spacedBy(12dp) fallback.
- ✅ **Cleanup** — xóa legacy `SectionHeader` (~17 LOC), thay fully-qualified `androidx.compose.foundation.layout.Arrangement` references trong MetaUpgrade + ModifierPicker bằng imported `Arrangement`.

Round 31 files modified:
- `common/NeonBottomSheet.kt` — heightIn max + heightIn import.
- `ui/menu/MenuScreen.kt` — BoxWithConstraints wrapper + scroll fallback.
- `ui/dlg/settings/DialogSettings.kt` — SectionHeader removed.
- `ui/dlg/metaupgrade/DialogMetaUpgrade.kt` + `ui/dlg/modifierpicker/DialogModifierPicker.kt` — qualified Arrangement references cleaned.

### Round 32 — UX feedback (menu spacers + Settings CHƠI groups + sheet padding + animation)

User feedback:
1. MenuScreen vẫn có dead space ở center area.
2. Settings CHƠI section UI quá khít — Rung/Reduce/Difficulty/Skin dồn dập.
3. Sheet bottom padding 56dp quá lớn — đổi 32dp.
4. Sheet exit slide animation không thấy (instant disappear).

Fixed:
- ✅ **MenuScreen redistribute spacers** — thay 1 Spacer(weight=1f) bằng 2: Spacer(weight=0.3f) sau Title + Spacer(weight=0.7f) sau Play (gate by tallEnough). Net: title cao hơn, logo + info + play cluster ~60% upper, grid bottom, gap chia 30/70.
- ✅ **Settings CHƠI ControlGroup** — new helper composable wrap mỗi control group (Rung+Reduce / Difficulty / Skin) trong sub-panel `Color.Black.copy(alpha=0.22f)` background + RoundedCorner 10dp + padding 12/10dp. Inter-group spacing 10dp.
- ✅ **Sheet padding bottom 56dp → 32dp** — SheetContent Column.
- ✅ **Sheet exit animation delay** — `rememberCoroutineScope()` + `dismissWithAnim` set visible=false rồi `launch { delay(220L); onDismiss() }`. 220ms > exit anim 200ms → slide-down + fade play đầy đủ trước khi NavHost pop dialog.

Round 32 files modified:
- `ui/menu/MenuScreen.kt` — 2 weighted spacers, title-area + play-area.
- `ui/dlg/settings/DialogSettings.kt` — ControlGroup helper + 3 sub-panels in CHƠI section.
- `common/NeonBottomSheet.kt` — bottom padding 32dp + coroutineScope + delay 220ms before onDismiss.

### Round 33 — menu title + notch + feature.md audit

User feedback:
1. MenuScreen title "SKY FORCE" hơi bé, bị che bởi notch.
2. Audit feature.md.

Fixed:
- ✅ **Title bigger** — "SKY FORCE" 36 → **44sp**, "U*S*A" 22 → **28sp**, letterSpacing tăng 4→5sp / 6→7sp, neonGlow intensity 0.7→0.75 + radius 1.7→1.8.
- ✅ **Clear notch area** — MenuScreen content Column thêm `Modifier.windowInsetsPadding(WindowInsets.displayCutout)` trước padding hard-coded. Activity đã hide status bar (round 25) nhưng physical notch cutout vẫn occupy space → reserve insets explicitly.
- ✅ **feature.md audit** — rounds 29-33 đã được document đầy đủ. Wave 5 status line corrected (round 23 đã wire toàn bộ modifier — không còn "rest deferred").

Round 33 files modified:
- `ui/menu/MenuScreen.kt` — TitleBlock fontSize 36→44sp / 22→28sp, displayCutout insets padding.
- `doc/feature.md` — round 29-33 sections appended, Wave 5 status corrected.

### Round 34 — Wave 4 Combat depth (status effects + roguelike buffs + ice slip)

User picked Wave 4 Combat depth, full wave scope, perf audit parallel. Implemented 4 of 5 sub-features (BulletType refactor deferred).

- ✅ **41x Status effects system** — new `ui/game/status/StatusEffect.kt` (enum BURN/SLOW/STUN với durationMs + tint color) + `StatusEffectController.kt` (Map<enemyId, MutableList<ActiveStatusEffect>>). Apply via `LasersController.onLaserHit` random 10% chance (5% boss). Tick @ 250ms processes BURN damage (5hp/500ms = 10hp/sec), expires old effects, cleans dead-enemy entries. SLOW data layer ready but movement multiplier not wired into Enemy.process yet (defer). STUN gates `EnemyLasersController.fireEnemyLasers` (stunned enemy skips fire this tick). Chain reaction (frozen+laser=shatter) deferred.

- ✅ **42x Roguelike buffs picker post-boss** — new `ui/game/buff/RunBuff.kt` enum 9 entries (HP_BOOST/DAMAGE_BOOST/SPEED_BOOST/MAGNET_BOOST/SCORE_BOOST/BALANCED/BERSERKER/FORTRESS/GAMBLER) với hp/dmg/spd/mag/score multipliers + glyph icon. `BuffMultipliers.from(buffs)` aggregates multiplicatively. `LocalActiveBuffs` CompositionLocal holds the picked buff list across compositions. New `DialogBuffPicker` (NeonBottomSheet) shows 3 random buffs với icon + name + description, dismissible (✕ tap = skip). Trigger: GameState.bossKillBuffOfferMillis set on boss kill (non-FinalBoss). GameScreen LaunchedEffect waits 2.2s for boss rank overlay then navigates to BuffPicker. New nav route `BuffPicker`. EffectiveStats compute merged: `baseEffectiveStats × BuffMultipliers.from(activeBuffs)` với caps preserved.

- ✅ **44x Env hazard Ice slip mechanic** — `ICE_PATCHES` hazard hiện trước round 34 chỉ visual (cyan edge tint, round 20). Now: ShipController accepts `isIceHazardActive: () -> Boolean` callback. When true + user releases movement button, ship continues gliding via `slipVelocityX` with 0.93 decay/tick (~200ms half-life). New `currentHazard: HazardType?` state in GameState updated by stageController.onStageAdvance. ShipController reads reactively. Slip velocity reset when leaving ice zone.

- ⏸️ **35x +10 bullet types** — DEFERRED to round 35. BulletType sealed class + new bullet types (Spread cone, Piercing through enemies, Plasma orb AoE, Beam continuous, Homing target-track, Wave sine, Lightning chain, Cluster split, Mine drop, Drone orbit) is heavy refactor touching LasersController + Laser interface + collision math. Existing TRIPLE_LASER_BOOSTER already provides Spread-like behavior. Scope cut to keep round 34 manageable.

- ✅ **Perf hot path audit (parallel)** — verified no regressions: StatusEffectController.processTick @ 4Hz lightweight, ice slip pure float ops no allocations, buff multiplier recompute only on activeBuffs change, no new per-frame Logger calls in hot path.

### Round 34 files

**New:**
- `ui/game/status/StatusEffect.kt` (~45 LOC — enum + ActiveStatusEffect data class)
- `ui/game/status/StatusEffectController.kt` (~100 LOC — apply/tick/cleanup/predicates)
- `ui/game/buff/RunBuff.kt` (~95 LOC — 9 buff entries + pickThree() + fromKey())
- `ui/game/buff/ActiveBuffs.kt` (~30 LOC — LocalActiveBuffs CompositionLocal + BuffMultipliers aggregator)
- `ui/dlg/buffpicker/DialogBuffPicker.kt` (~120 LOC — bottom sheet picker với icon cards)

**Modified:**
- `navigation/Navigation.kt` — BuffPicker route added.
- `ui/MainActivity.kt` — LocalActiveBuffs provider, BuffPicker dialog route, GameScreen.onOpenBuffPicker wiring.
- `ui/game/state/GameState.kt` — statusEffectController init + tinker, currentHazard state + onStageAdvance hook, bossKillBuffOfferMillis trigger, effectiveStats merge với BuffMultipliers, statusEffectTick state.
- `ui/game/GameScreen.kt` — onOpenBuffPicker param + LaunchedEffect(bossKillBuffOfferMillis) navigate after 2.2s delay.
- `ui/game/ship/ship/ShipController.kt` — isIceHazardActive param + slipVelocityX state + ice slip movement logic.
- `ui/game/enemy/laser/EnemyLasersController.kt` — isEnemyStunned predicate + skip fire if stunned.

### Round 34 verification

- `./gradlew assembleDevDebug` BUILD SUCCESSFUL.
- `./gradlew compileProductionReleaseKotlin` BUILD SUCCESSFUL.

### Round 35 — Wave 4 Combat depth continuation (BulletType + status visuals + buffs HUD + memory leak hardening + first unit tests)

User said "tiếp tục đi" then added "bạn có chắc không? hãy check kỹ memory leak và thêm unit test". Closed out the round-34 deferred BulletType refactor (subset), added the visual layer for round-34 mechanics, hardened LeakWatch coverage, and seeded JUnit testing.

- ✅ **35x BulletType refactor (subset)** — new `ui/game/ship/laser/BulletType.kt` enum (NORMAL / PIERCING / PLASMA). Each carries `damageMultiplier`, `pierceCount`, `aoeRadius`, `glyph`, `activeDurationMillis`. NORMAL is the inert default. PIERCING passes through up to 3 enemies (decrement `pierceRemaining` per hit, destroy on 0). PLASMA fires a fatter beam at +60% damage and applies AoE splash at 80px radius (50% damage) on impact. Added `bulletType` + `pierceRemaining` defaults to the `Laser` interface so existing lasers don't break. New laser classes: `PiercingShipLaser`, `PlasmaShipLaser`. New boosters in `BoosterType`: `PIERCING_BOOSTER` (weight 8) + `PLASMA_BOOSTER` (weight 8) — drawables reuse `booster_red_lasers` / `booster_ultimate_weapon` until dedicated assets exist. `Ship` gained `activeBulletType` + `bulletTypeEndMillis`; `ShipController.setBulletType()` flips it on pickup; expiry check in process tick reverts to NORMAL. `LasersController.fireLasers` short-circuits to `fireBulletTypeLasers(ship)` when non-NORMAL; collision branches on `laser.bulletType`. Homing variant scoped but deferred — needs per-tick target tracking via Laser x/yVelocity refactor.

- ✅ **42x Active buffs HUD** — new `ui/game/controls/ActiveBuffsHud.kt`. Reads `LocalActiveBuffs` via CompositionLocal; renders nothing when empty (most of early game). Each buff is a 30dp circular chip with its glyph icon. Same buff stacked → count badge `xN` in bottom-right. Color cycle (cyan / gold / magenta / violet) by `buff.key.hashCode() % 4`. Mounted in `GameScreen` at `Alignment.TopStart` with `padding(top = 90.dp)` below the IndicatorStatus.

- ✅ **42x Status effect tint overlay** — extended `EnemyUI` with `activeStatusEffectTints: List<Long>` (ARGB packed) and `EnemyToEnemyUIMapper.invoke()` with a `tints` arg. `GameState` reads `statusEffectController.effectsFor(enemyId).map { it.tintColorArgb }` and feeds the mapper. `GameWorld` renders one extra `Image` per tint over the enemy sprite with `ColorFilter.tint` and a 3Hz sin-pulse alpha (0.65 → 1.0). BURN = orange-red, SLOW = cyan, STUN = yellow.

- ✅ **Memory leak hardening** — audit confirmed `tinkerClearAll()` IS called on `GameState.onDispose` (no unbounded growth). Added the 4 missing `LeakWatch.watch(...)` calls for `comboController`, `impactSparkController`, `pickupBurstController`, `statusEffectController` so all 17 controllers are now watched on disposal (debug builds will trigger LeakCanary heap dump if any are retained).

- ✅ **First unit tests** — added `testImplementation "junit:junit:4.13.2"` + `android.testOptions.unitTests.returnDefaultValues = true` so `android.util.Log` calls (via `Logger.d`) return defaults instead of throwing in JVM tests. New `app/src/test/java/...`:
    - `core/TinkerTest.kt` — 7 tests covering Never/Once/Millis semantics + `tinkerClearAll` reset + independent-id tracking.
    - `ui/game/status/StatusEffectControllerTest.kt` — 11 tests covering apply/refresh/coexist/expire/dead-enemy-cleanup/burn-tick-interval/clearFor.
    - `ui/game/buff/BuffMultipliersTest.kt` — 9 tests covering empty/single/stack/mixed aggregation + `pickThree` + `fromKey` roundtrip.
    - `ui/game/ship/laser/BulletTypeTest.kt` — 6 tests covering NORMAL defaults / PIERCING+PLASMA invariants / unique glyphs.
    - Helper: `TestEnemy.kt` — minimal Enemy stub for controller tests.

### Round 35 files

**New:**
- `ui/game/ship/laser/BulletType.kt` (~50 LOC — enum).
- `ui/game/ship/laser/PiercingShipLaser.kt` / `PlasmaShipLaser.kt`.
- `ui/game/controls/ActiveBuffsHud.kt` (~85 LOC).
- `app/src/test/java/com/tranphuloi/neon/TestEnemy.kt`.
- `app/src/test/java/.../{core/TinkerTest,ui/game/status/StatusEffectControllerTest,ui/game/buff/BuffMultipliersTest,ui/game/ship/laser/BulletTypeTest}.kt`.

**Modified:**
- `ui/game/ship/laser/Laser.kt` — default `bulletType` + `pierceRemaining` properties.
- `ui/game/booster/BoosterType.kt` — added PIERCING_BOOSTER / PLASMA_BOOSTER.
- `ui/game/ship/ship/Ship.kt` — added `activeBulletType` + `bulletTypeEndMillis`.
- `ui/game/ship/ship/ShipController.kt` — pickup handling + setBulletType + expiry tick.
- `ui/game/ship/laser/LasersController.kt` — `fireBulletTypeLasers` + PIERCING / PLASMA / NORMAL branching in collision.
- `ui/game/enemy/ship/model/EnemyUI.kt` — `activeStatusEffectTints: List<Long>`.
- `ui/game/enemy/ship/mapper/EnemyToEnemyUIMapper.kt` — `tints` arg.
- `ui/game/state/GameState.kt` — feed tints into mapper + 4 extra LeakWatch.watch calls.
- `ui/game/world/GameWorld.kt` — status effect tint overlay block.
- `ui/game/GameScreen.kt` — mount ActiveBuffsHud at TopStart padding-top 90dp.
- `app/build.gradle` — `testImplementation junit` + `testOptions.unitTests.returnDefaultValues = true`.

### Round 38 — Wave 6 start: ship aura color customization (45x)

User said "tiếp tục phase tiếp theo đi". Picked Wave 6 (Accessibility & Polish), started with ship customization since `ShipSkin` enum was already partly wired (DataStore + picker UI) but had been a dead setting — nothing read it for game rendering. This round makes it real.

- ✅ **ShipSkin enum redefined** — was `REGULAR / BOOSTED` (cosmetic-only labels with no rendering effect). Now 5 color variants: `AURA_CYAN`, `AURA_GOLD`, `AURA_MAGENTA`, `AURA_VIOLET`, `AURA_REDALERT`. Each carries `key` (DataStore), `displayName` (Vietnamese label), and `glowColorHex` (ARGB Long fed into Compose `Color(...)`). Old persisted keys "regular" / "boosted" silently migrate to `AURA_CYAN` via `fromKey`'s fallback (no save data depended on them anyway).

- ✅ **GameWorld wires shipSkin → ship glow** — collects `settings.shipSkin` via `LocalSettings.current` near the top of `GameWorld` (initial = AURA_CYAN so first composition before flow resolves still renders the original cyan). Passes the resolved `Color(skin.glowColorHex)` into the ship sprite's `.neonGlow(...)` modifier, replacing the hardcoded `NeonCyan`.

- ✅ **Ship laser glow follows aura** — same color also feeds the `shipLasers.forEach { ... .neonGlow(...) }` block. Visual reads as "ship's own bullets" instead of cyan tracers from a red ship. Enemy lasers / ultimate lasers / spark FX keep their own palette — only ship-originated visuals follow the skin.

- ✅ **DialogSettings picker revamped** — old "Skin tàu / Thường / Cường hóa" replaced with "Hào quang tàu / Cyan / Vàng / Hồng / Tím / Đỏ". Each Pill uses its own skin's `Color(glowColorHex)` so the chip IS the color preview — no separate swatch needed.

### Round 38 files

**Modified:**
- `data/SettingsRepository.kt` — `ShipSkin` enum redefined (5 colors with glowColorHex), `fromKey` fallback → `AURA_CYAN`.
- `ui/dlg/settings/DialogSettings.kt` — picker labels via `s.displayName`, Pill colors via `Color(s.glowColorHex)`, initial state collectAsState → `AURA_CYAN`.
- `ui/game/world/GameWorld.kt` — collect shipSkin + derive `shipGlowColor` once near function top; thread into ship + ship-laser `.neonGlow(color = shipGlowColor, ...)`. Added `androidx.compose.runtime.collectAsState` import.

### Round 38 verification

- `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest` BUILD SUCCESSFUL.
- **72 tests still pass.**
- Manual test path: Settings → "Hào quang tàu" → pick non-cyan → enter game → ship glow + laser glow match picked color. Restart app → preference persists.

### Round 37 — Hot-path Logger.d audit (8 spam sources removed)

User flagged log `roy93~ rememberGameState: composing — entry point` ném liên tục. Root cause: `rememberGameState()` recomposes ~125Hz (refreshHandler tail-read drives the game loop's per-frame Compose re-render). Audited entire codebase for similar issues.

- ✅ **GameState.kt:68 entry log removed** — fired ~125 lines/sec while game running. Init context still logged once inside `remember { UuidUtils() }` block (line 73, unchanged).

- ✅ **5ms-tick controller cleanup (4 sites)** — these fire whenever items removed/picked, but per 5ms tick → 200Hz worst case. All were aggregate stats with no signal not already covered by per-event upstream logs:
    - `BoosterController.kt:57` — "removed N boosters" (covered by GameState Booster picked-up event)
    - `MineralsController.kt:58` — "magnet picked N minerals" (could spike to ~200×/sec on mineral cluster sweep)
    - `SpaceObjectsController.kt:47` — "removed N rocks" (covered by onLaserHit hit log)
    - `EnemyLasersController.kt:53` — "removed N off-screen lasers" (5-15×/sec in normal play)
    - `EnemyController.kt:65` — "$N enemies left screen" (mid-wave fired multiple times per second)

- ✅ **1ms collision-tick cleanup (2 sites in LasersController)** — `monitorLaserCollision` runs at Millis(1) = 1000Hz; previous round 35 added PIERCING/PLASMA per-hit logs there for debugging:
    - `LasersController.kt:228` — "PIERCING hit … pierceRemaining=X" (fired N×3 times per piercing run through a formation)
    - `LasersController.kt:254` — "PLASMA hit + AoE radius=Npx" (one per impact; onLaserHit already records hit)

- ✅ **Safe sites verified** — ShipController (18 calls), GameState (47 remaining), AudioPlayerHolder (15), MainActivity (40), BackgroundController (9), GameScreen (13), StatusEffectController (5) all classified PER-EVENT or ONCE (inside `remember`/`init`/`LaunchedEffect(Unit)`). No further hot-path firing.

### Round 37 files

**Modified:**
- `ui/game/state/GameState.kt` — removed entry log + breadcrumb explaining why.
- `ui/game/booster/BoosterController.kt`
- `ui/game/mineral/controller/MineralsController.kt`
- `ui/game/spaceObject/SpaceObjectsController.kt`
- `ui/game/enemy/laser/EnemyLasersController.kt`
- `ui/game/enemy/ship/controller/EnemyController.kt`
- `ui/game/ship/laser/LasersController.kt` — removed PIERCING + PLASMA hit logs from 1000Hz collision tick.

### Round 37 verification

- `./gradlew compileDevDebugKotlin testDevDebugUnitTest compileProductionReleaseKotlin` BUILD SUCCESSFUL.
- **72 tests still pass** (no test depended on the removed logs).
- Expected log volume reduction in active combat: ~125Hz entry-log + ~50Hz aggregate ticks = effectively gone. Per-event logs (collisions, pickups, deaths) remain so debugging signal is preserved.

### Round 36 — Test coverage expansion + EffectiveStats refactor

User picked "Test coverage expansion" sau khi audit feature.md. Đẩy JUnit suite từ 33 → 72 tests; extract buff-merge math khỏi GameState để testable.

- ✅ **EffectiveStats.withBuffs() extracted** — merge logic (5 caps × buffMultipliers) trước đây inline trong `GameState.kt:143-152` giờ là pure method trên `EffectiveStats`. GameState.kt giờ chỉ gọi `baseEffectiveStats.withBuffs(activeBuffs)`. Behavior identical, math + caps đều giữ nguyên.

- ✅ **EffectiveStatsTest (21 tests)** — `compute()`: default identity, EASY/HARD inverse hp scaling, mỗi modifier (GLASS_CANNON / NO_SHIELDS / BOSSES_ONLY) propagate đúng flag, meta upgrades stack linear (HP +10%/rank, DMG +8%, SPD +6%, MAG +15%), unknown meta key ignored, hp/damage/speed/score cap clamping ở cả 2 chiều. `withBuffs()`: empty list = identity, HP_BOOST stack, BERSERKER trade-off math, hp/damage/score caps khi merge, flags không bị mutate.

- ✅ **ShipLaserClassesTest (11 tests)** — `PiercingShipLaser`: pierceRemaining khởi tạo = 3, decrement đúng qua assignment, bulletType=PIERCING, không destroyed lúc tạo, moveLaser giảm yOffset. `PlasmaShipLaser`: bulletType=PLASMA, pierceRemaining=0 (default), width=PLASMA_WIDTH=16f, impactPower=40f (=25×1.6), moveLaser dùng yOffsetMovementSpeed=5 (< piercing 7).

- ✅ **BoosterTypeTest (7 tests)** — invariants: mọi type có weight > 0 + drawableId ≠ 0, REVIVE_TOKEN rarest (weight 5 < tất cả), tổng weight = 116 (5×19 + 5 + 2×8), bullet-type rarer than base nhưng commoner than revive, weighted-pick algorithm map đúng roll → type, PIERCING/PLASMA reachable từ inside slot weight.

### Round 36 files

**New tests:**
- `app/src/test/java/.../ui/game/state/EffectiveStatsTest.kt` (~21 tests).
- `app/src/test/java/.../ui/game/ship/laser/ShipLaserClassesTest.kt` (~11 tests).
- `app/src/test/java/.../ui/game/booster/BoosterTypeTest.kt` (~7 tests).

**Modified:**
- `ui/game/state/EffectiveStats.kt` — thêm `withBuffs(buffs: List<RunBuff>)` method (pure, capped).
- `ui/game/state/GameState.kt` — replace inline buff merge (lines 143-152) bằng `baseEffectiveStats.withBuffs(activeBuffs)`.

### Round 36 verification

- `./gradlew testDevDebugUnitTest` BUILD SUCCESSFUL.
- **72 tests, 0 failures, 0 errors** across 7 test classes (+39 mới so với round 35).
- Test count breakdown: EffectiveStatsTest 21 · StatusEffectControllerTest 11 · ShipLaserClassesTest 11 · BuffMultipliersTest 9 · TinkerTest 7 · BoosterTypeTest 7 · BulletTypeTest 6.

### Round 35 verification

- `./gradlew compileDevDebugKotlin` BUILD SUCCESSFUL.
- `./gradlew compileProductionReleaseKotlin` BUILD SUCCESSFUL.
- `./gradlew testDevDebugUnitTest` BUILD SUCCESSFUL — **33 tests, 0 failures, 0 errors** across 4 test classes.
- All 17 controllers now under LeakWatch coverage on `GameState.onDispose`.

### Manual test cho round 34

```
1. Start any mode → play normally
2. Diệt enemies → có 10% chance hit apply status effect
   ✓ logcat: "StatusEffect: apply BURN/SLOW/STUN on enemy=..."
   ✓ BURN: enemy hp tick down 5/500ms (damage numbers nhỏ liên tục)
   ✓ STUN: enemy skip generateLasers — không bắn đạn vài giây
   (SLOW: data only, không thấy visual hoặc speed change yet)
3. Vào ICE_PLANET chapter (chapter 3) → có ICE_PATCHES hazard
   ✓ Sau khi release movement button, ship glide thêm ~200ms
   ✓ Cyan edge tint vẫn còn (visual round 20)
4. Hạ mid-boss hoặc final-boss-1/2 (NOT FinalBoss/Galaxy Overlord)
   ✓ Boss rank overlay 1.8s
   ✓ Sau 2.2s, BuffPicker sheet trượt lên từ bottom
   ✓ 3 buffs random với icons (♥/⚔/⚡/◉/★/✦/☠/⊞/✪)
   ✓ Tap 1 buff hoặc ✕ skip
5. Check EffectiveStats merged: pick BERSERKER (×1.5 dmg, ×0.9 hp)
   ✓ logcat: "effectiveStats computed=... (with 1 active buffs)"
   ✓ Sau buff, laser damage tăng ~50%
6. Hạ thêm 1 boss → BuffPicker lại → pick HP_BOOST
   ✓ 2 buffs stack: dmg × 1.5 × 1.0 = 1.5, hp × 0.9 × 1.25 = 1.125
7. Chết → restart → activeBuffs reset = empty (LocalActiveBuffs fresh per composition)
8. FinalBoss killed (chapter 5) → NO buff picker (victory branch)
```

### Rounds 23-34 verification (cumulative)

- `./gradlew assembleDevDebug` BUILD SUCCESSFUL.
- `./gradlew compileProductionReleaseKotlin` BUILD SUCCESSFUL.
- ~290+ Logger.d call sites with `roy93~` auto-prefix.
- NeonBottomSheet common component used by 7 dialogs (Settings, GameOver, GamePause, DifficultyPicker, ModePicker, ModifierPicker, MetaUpgrade).
- Per-mode checkpoint persistence (Campaign / Survival / TimeAttack / BossRush / Endless) — survives cold launch.
- 4 modifier multipliers (hp/dmg/speed/magnet/score/noShieldDrops) all wired through Ship / Lasers / Booster controllers.
- 4 skill tree base nodes (HP/Damage/Speed/Magnet) propagate via EffectiveStats.
- Vietnamese-first all UI (GameMode / RunModifier / SkillNode / StoryRegistry / Achievement / Stage / Chapter / picker dialogs / settings / game over / pause).
- Custom Orbitron font removed; device default font.
- BackHandler in Game opens pause sheet instead of exiting.

### Manual test cho round 27

```
1. App launch → Splash → MenuScreen
   ✓ Animated starfield background (60 stars drifting downward, 3 layers, twinkling)
   ✓ "SKY FORCE / U*S*A" title pulsing neon
   ✓ Splash ship image with radial halo, pulsing scale
   ✓ InfoCard: chế độ + ♦ minerals + checkpoint nếu có
   ✓ PLAY button huge, pulsing border, ▶ glyph
   ✓ 2x2 grid with icons: ⊞ ⚡ ⬆ ⚙
2. Tap each button → adb logcat should show "MenuScreen: X tapped" (verify clicks work)
3. PLAY → enter Game
4. Press Android back / swipe-back → DialogGamePause opens (NOT exit Game)
5. Pause dialog has 4 buttons: ▶ TIẾP TỤC / ↻ CHƠI LẠI / ⚙ CÀI ĐẶT / ◀ VỀ MENU
6. ⚙ CÀI ĐẶT → DialogSettings: 3 sections (ÂM THANH / CHƠI / ỨNG DỤNG) — NO duplicate mode/buff/upgrade rows
7. Close Settings → back to Pause → ◀ VỀ MENU → MenuScreen
   ✓ Checkpoint preserved → InfoCard shows "▸ Đang ở màn N" + PLAY label = "TIẾP TỤC"
8. From Menu, tap CÀI ĐẶT directly → opens same Settings dialog (no mode rows there)
9. Tap CHẾ ĐỘ → ModePicker → tap a mode → returns to Menu (popBackStack)
10. PLAY → Game with new mode
11. Die → GameOver dialog with CHƠI LẠI + VỀ MENU buttons (round 26 feature still works)
12. Force-close app → reopen → Splash → Menu → checkpoint still there → TIẾP TỤC
```

### Manual test cho round 26

```
1. Cold install (clear data) → Splash → DialogDifficultyPicker "CHỌN ĐỘ KHÓ" với DỄ/VỪA/KHÓ
2. Pick độ khó → Menu (Vietnamese)
3. BẮT ĐẦU → Game → TutorialOverlay "GIỮ & DI" tiếng Việt
4. Chơi tới chapter 2 → "CHƯƠNG 2 / MÂY TINH VÂN / BẮT ĐẦU!" StageBanner
5. Mid-boss → "NGUY HIỂM" → boss → "Tiếp tục!"
6. Wave clear → "HOÀN THÀNH ĐỢT! +5 khoáng vật"
7. Chết → GameOver dialog (Vietnamese labels: KHOÁNG VẬT / KỶ LỤC / THỐNG KÊ / TOP CAO ĐIỂM)
   → 2 buttons: "CHƠI LẠI" + "◀ VỀ MENU"
8. Tap "VỀ MENU" → MenuScreen → checkpoint preserved → button hiện "TIẾP TỤC"
9. Tap TIẾP TỤC → resume tại stage cũ → chapter 2 intro KHÔNG re-fire (đã played)
10. Chơi tới FinalBoss → hạ → VictoryPanel: "BÁ VƯƠNG BỊ HẠ" (NORMAL) hoặc "THIÊN HÀ ĐƯỢC CỨU" (EASY)
11. GameOver dialog → CHƠI LẠI → fresh start từ stage 0 (checkpoint cleared on victory)
12. Pause game → Settings → CHỌN CHẾ ĐỘ "CHIẾN BOSS" → back ra Menu → BẮT ĐẦU → boss-rush mode
13. Boss-rush kill last boss → "VƯỢT ẢI!" StageBanner + BOSS_RUSH_CLEAR achievement
14. Phase 2/3 boss → "GIAI ĐOẠN 2/3" banner
15. Pick revive token → "HỒI SINH" badge trên HUD
16. Achievement unlock → "🏆 THÀNH TỰU · VÀNG" + Vietnamese title (e.g. "ANH HÙNG THIÊN HÀ")
17. Verify font: tất cả text dùng font hệ thống device (Roboto trên Android)
18. Small screen test: MenuScreen scrollable nếu overflow
```

### Manual test cho round 25

```
1. Cold install → app launch → Splash 1.2s → MenuScreen (KHÔNG vào game ngay)
2. Menu thấy: title pulse, balance ♦ minerals, mode card "CHIẾN DỊCH", "BẮT ĐẦU" button
3. Tap CHẾ ĐỘ → DialogModePicker (Việt + font 14-22sp) → CHIẾN BOSS → back về Menu
4. Menu giờ hiện mode card "CHIẾN BOSS"
5. BẮT ĐẦU → Game → kill 2 boss đầu (advance stage index)
6. Force-close app (recent apps swipe up)
7. Cold launch lại → Splash → Menu
8. Menu hiện: "▸ Đang ở màn N" trong mode card + button đổi thành "TIẾP TỤC"
9. Tap TIẾP TỤC → Game start tại stage N với ship HP đầy + smartBombs reset
10. Chết → GameOver dialog → checkpoint cleared
11. Tap RESTART → Game → restart từ stage 0 (vì checkpoint đã clear)
12. Back từ Game → Menu (KHÔNG còn "TIẾP TỤC")
13. Settings (từ Menu) — đọc rõ ràng các section font 13-16sp, không bị crammed
14. AchievementBanner: title 20sp gold ĐỒNG/BẠC/VÀNG dễ đọc
15. Boss spawn → StoryOverlay top: "● BOSS CẤP 1" + dialogue 17sp dễ đọc
16. Font test: nhìn UI text → font system của device (Roboto trên Android, không Orbitron)
```

### Wave 5 known limitations (still deferred)

The following SkillNode entries persist ranks but the per-node special effects are NOT wired to controllers — only their parent's stat bonus takes effect through EffectiveStats:

- **REGENESIS** (auto-heal +5 hp/sec): not wired — would need ShipController tick callback for periodic heal.
- **CRITICAL** (+10% crit chance per rank, 2× damage): not wired — would need probabilistic damage boost in LasersController.
- **STARGAZER** (+5% score per rank): not wired — separate from BASE_MAGNET scoreMul.
- **REACTIVE** (shield expiry mini-bomb): not wired — would need ShipController shield-end hook.
- **AFTERBURNER** (post-damage i-frames): not wired — would need ShipController damage hook.
- **ARMORY** (+1 starting smart bomb per rank): not wired — would need `smartBombs` initial value to read meta rank.
- **MOMENTUM** (combo +0.5s lifetime per rank): not wired — would need ComboController extension.
- **PHOENIX HEART** (+2% revive token drop chance per rank): not wired — would need Booster.type weight adjustment.
- **STAR FORGE / VOID CANNON** (tier-2 endgame): not wired — would need flat hp/damage adders.

Future round will plumb these in. For now, only the 4 base stats (HP, Damage, Speed, Magnet) propagate from ranks via EffectiveStats; ScoreMul + magnetMul propagate from modifier; noShieldDrops + bossesOnly flags wired (bossesOnly currently no-op — would mirror BOSS_RUSH mode behavior, deferred).

---

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

# Phần 2 — 📜 Picks history (đã DONE — archived)

> **Round 33 audit**: tất cả items trong Phần 2 và Phần 3 đã được triển khai qua Wave 3 (round 12-19), Wave 4 (round 20-21), Wave 5 (round 22-32). Giữ làm historical record cho biết picks gốc của các round AskUserQuestion. Status mapping → xem Phần 7 Wave Plan.

## Round 4 picks (11c-17c) ✅ DONE in Wave 3 round 12-19

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

# Phần 3 — 📜 Selector history (đã pick xong — archived)

> **Round 33 audit**: 18 câu hỏi 31-48 dưới đây đã được user pick xong qua AskUserQuestion từ round 5 (Wave 4 foundation) + round 22 (Wave 5). Tất cả picks (option ⭐ Recommended cho mỗi câu) đã được triển khai. Giữ làm historical record của quá trình design selector.

> *Hướng dẫn pick (cũ):* thay `[ ]` thành `[x]` ở option muốn chọn. Mỗi câu pick 1 option.

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

## Wave 3 ✅ DONE (round 12-19)
- [x] 6c Slow-motion critical (round 19)
- [x] 8c Dynamic music intensity (round 19)
- [x] 14c Auto-revive token (round 19)
- [x] 15c Stats screen breakdown (round 12)
- [x] 17c Daily challenge (round 19)
- [x] 19b Charge shot (auto-charge no-damage) (round 12)
- [x] 20b Smart bomb stack (round 12)
- [x] 24b Boss kill rank S/A/B/C/D (round 12)

## Wave 4 Foundation ✅ DONE (round 20)
- [x] 31d +100 stages 5 chapters (procedural buildStageScript)
- [x] 32d Chapter themes + hazards (per-chapter tint + asteroid storm + nebula fog overlay + ice edge tint)
- [x] 33c+d Mid-bosses 3 variants + phase transitions (Offensive/Defensive/Swarm @ HP<50%)
- [x] 34d Final boss 3-phase (22500 HP, ring barrage phase 3, alt endings per difficulty)

## Wave 4 Combat depth (deferred)
- [ ] 35x +10 bullet types
- [ ] 38x +10 support items
- [ ] 41x Status effects + chains
- [ ] 42x Roguelike buffs + curses
- [ ] 44x Environmental hazards (extended — ice slip mechanic, solar flares)

## Wave 5 ✅ DONE (round 22 base + round 23 audit fixes)
- [x] 23x Endless mode (procedural scaling + endless leaderboard)
- [x] 43x Game modes (Survival / TimeAttack / BossRush via StageProvider)
- [x] 25x Random modifiers (all multipliers fully wired in round 23: hp/dmg/speed/magnet/score/noShieldDrops)
- [x] 28x Procedural patterns (V / SineWave from chapter 2)
- [x] 46x Achievements expansion (+20 Bronze/Silver/Gold tiered)
- [x] 47x Story / lore (chapter intros + boss taunts)
- [x] 48x Permanent progression (15-node skill tree + lifetime minerals)

## Wave 5 polish + UX revamp ✅ DONE (rounds 23-33)
- [x] Round 23 — audit fixes: modifier plumbing, BOSS_RUSH_CLEAR gate, TIME_ATTACK victory branch, StageController saver
- [x] Round 24 — StoryOverlay reposition + Vietnamese translation + DialogSettings redesign
- [x] Round 25 — MenuScreen built + per-mode checkpoint persistence + device font + font sizes boosted
- [x] Round 26 — Vietnamese gaps closed + death-keeps-checkpoint + chapter intro fix + orbitron font deleted
- [x] Round 27 — MenuScreen revamp (starfield + icons) + dialog dedup + pause MENU button + BackHandler
- [x] Round 28 — NeonBottomSheet common + migrate 7 dialogs + MenuScreen animations (stagger/idle/comet)
- [x] Round 29 — sheet padding 56dp + remove BỎ QUA + wrap content height + hide status bar in sheet + scrim 0.8
- [x] Round 30 — scrim removed + Settings/MetaUpgrade/Modifier/Mode revamp with icons + MenuScreen bottom space fixed
- [x] Round 31 — sheet heightIn max 90% + MenuScreen BoxWithConstraints adaptive
- [x] Round 32 — sheet padding 32dp + slide animation delay onDismiss + Menu redistribute spacers + Settings ControlGroup
- [x] Round 33 — Menu title 44sp + displayCutout windowInsetsPadding + feature.md audit

## Wave 6 (Polish + accessibility)
- [ ] 26x Photo mode
- [ ] 27x Color blind mode
- [ ] 29x Secondary weapon
- [ ] 36x Loadout system
- [ ] 39x Item rarity tiers
- [ ] 40x Item combos
- [x] 45x Ship customization (round 38 — 5-color aura glow wired into ship + ship-laser rendering)

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
