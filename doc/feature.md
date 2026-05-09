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
- [ ] 15c Stats screen breakdown
- [ ] 17c Daily challenge
- [ ] 19b Charge shot (hold both = mega)
- [ ] 20b Smart bomb stack
- [ ] 24b Boss kill rank S/A/B/C

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
