# Task 26 — Achievement round 2 (enhance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "enhance tính năng cũ").
Task 07 (đợt 2) đã mở rộng achievement round 1. Task này thêm round 2, tái
dùng nguyên `AchievementsRepository` (unlock-once-and-persist), không đổi
hạ tầng.

## Slice 0 — chốt thiết kế (DONE)
Audit wiring có sẵn: 2 điểm trigger unlock —
- **In-run** qua `unlockAchievement(Achievement)` (hoisted trong
  `GameState.kt`, gọi `achievementsRepo.unlock` + reward + banner + TTS) —
  gọi trực tiếp tại call site sự kiện xảy ra (VD enemy chết, drone spawn).
- **Lifetime/cross-run** qua `awardLifetime(Achievement)` (local fun trong
  `GameScreen.kt`'s `LaunchedEffect(GAME_OVER)`, chạy 1 lần sau
  `metaRepo.recordRunMetrics(...)` để tổng mới nhất) — dùng cho điều kiện cần
  đọc `MetaProgressionRepository`/`SettingsRepository` flow.
- Điều kiện phức tạp/cần test độc lập → hàm thuần trong
  `AchievementUnlocks.kt` (Task 07 pattern, "single-source, tested").
- Không có màn achievement browser riêng — chỉ có `AchievementBanner` (toast
  lúc unlock) đọc thẳng `achievement.title`/`.description` (tiếng Việt hard-code
  trong enum, không qua `strings.xml`). Cả 32 achievement hiện có đều theo
  convention này → Task 26 **không cần Slice i18n riêng** (không lệch chuẩn).

**7 achievement mới** (bám feature đợt 3-5 chưa từng có thành tựu):

| Enum | Tier | Điều kiện | Trigger |
|---|---|---|---|
| `PRESTIGE_FIRST` | Bronze | `metaRepo.prestigeLevel >= 1` | lifetime (GAME_OVER) |
| `PRESTIGE_MASTER` | Gold | `metaRepo.prestigeLevel >= 5` | lifetime (GAME_OVER) |
| `LOADOUT_TINKERER` | Silver | tùy biến loadout riêng cho ≥5 ship khác nhau (đếm dòng `SHIP_LOADOUTS` csv) | lifetime (GAME_OVER) |
| `SHIP_ALL_MAX_LEVEL` | Gold | TẤT CẢ ship đang sở hữu đều đạt Lv tối đa (mở rộng `SHIP_MAX_LEVEL` vốn chỉ cần 1 ship) | lifetime (GAME_OVER) |
| `ONE_HAND_BOSS_KILL` | Silver | Hạ boss khi `ControlHandMode != TWO_HANDED` | in-run (boss kill site, `GameState.kt`) |
| `DAILY_STREAK_7` | Silver | `metaRepo.dailyStreak >= 7` (điểm danh 7 ngày liên tiếp) | lifetime (GAME_OVER) |
| `DAILY_STREAK_30` | Gold | `metaRepo.dailyStreak >= 30` | lifetime (GAME_OVER) |

Cần thêm 1 flow mới `SettingsRepository.customizedShipLoadoutCount: Flow<Int>`
(đếm số dòng `SHIP_LOADOUTS` csv — chưa có sẵn, các flow hiện tại chỉ đọc
theo từng ship riêng lẻ).

## Slice 1 — thêm entry vào enum Achievement (DONE)
- Thêm 7 entry cuối enum `Achievement` (`AchievementsRepository.kt`), giữ
  nguyên toàn bộ entry cũ (tránh vỡ persistence — id string là khoá lưu).

## Slice 2 — trigger unlock tại đúng call site (DONE)
- `AchievementUnlocks.kt`: thêm hàm thuần `prestigeFirst`, `prestigeMaster`,
  `loadoutTinkerer`, `shipAllMaxLevel`, `oneHandBossKill`, `dailyStreak7`,
  `dailyStreak30` (test độc lập, không đụng GameState/GameScreen).
- `SettingsRepository.kt`: thêm `customizedShipLoadoutCount: Flow<Int>`.
- `GameState.kt`: đọc `controlHandMode` 1 lần/run (giống pattern
  `selectedDroneVariant`), gọi `unlockAchievement(ONE_HAND_BOSS_KILL)` tại
  đúng chỗ đang unlock `BOSS_3`/`BOSS_5`.
- `GameScreen.kt`: thêm 6 block `awardLifetime(...)` trong
  `LaunchedEffect(GAME_OVER)`, cạnh các achievement lifetime hiện có
  (`PLASMA_MASTER`, `LIGHTNING_MASTER`, ...).
- Test: mỗi achievement mới unlock đúng điều kiện qua `AchievementUnlocks`
  unit test, không unlock nhầm ngưỡng dưới.

## Slice 3 — verify (DONE)
- Không cần i18n riêng (xem Slice 0). Verify:
  `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device (DONE)
- Build: `compileDevDebugKotlin`, `compileProductionReleaseKotlin`,
  `testDevDebugUnitTest` → **BUILD SUCCESSFUL**, 957/957 test pass (950 cũ +
  7 test mới trong `AchievementUnlocksTest`). Phải sửa
  `AchievementsWave11cTest`'s drift-audit (`Achievement.entries.size`:
  41 → 48) vì test này cố tình fail khi có ai thêm achievement mà quên cập
  nhật — đúng như thiết kế, không phải bug.
- Device: Pixel 7 Pro (`2B051FDH3006MU`, thiết bị duy nhất đang kết nối,
  theo R3). Cài `installDevDebug`, force-stop + relaunch `MainActivity`
  sạch.
- Playthrough: Menu → Cài đặt → đổi "Tay thuận" sang "Tay trái" (xác nhận
  nút hành động chuyển sang bên trái màn hình đúng layout 1 tay) → Tiếp tục
  Ch.1 St.4 → di chuyển/bắn vài chục giây → Tạm dừng → Về menu → đổi lại
  "Hai tay" (khôi phục setting gốc). Không có quảng cáo che UI ở bất kỳ bước
  nào (R4 không kích hoạt).
- Logcat filter `FATAL|AndroidRuntime|Exception|ANR` trong suốt phiên
  (launch → gameplay → pause → về menu → đổi lại settings): **sạch**, không
  có dòng nào liên quan `com.tranphuloi.neon`. Chỉ có warning hệ thống vô hại
  (`AppOps: attributionTag not declared`, `MediaProvider FileNotFoundException`)
  — không liên quan code Task 26.
- Không thử nghiệm được on-device các điều kiện cross-run dài hạn
  (`PRESTIGE_FIRST/MASTER`, `DAILY_STREAK_7/30`, `SHIP_ALL_MAX_LEVEL`,
  `LOADOUT_TINKERER`) vì cần state tích luỹ nhiều ngày/run thực tế — đã cover
  đầy đủ bằng unit test biên (`AchievementUnlocksTest`, 7 test case mới).
  `ONE_HAND_BOSS_KILL` xác nhận được layout điều kiện tiên quyết (tay trái)
  hoạt động đúng nhưng không hạ được boss trong phiên test ngắn — logic unlock
  đã unit-test qua `oneHandBossKill(...)`.

## Trạng thái
✅ **DONE** — 7 achievement mới (Task 26 round 2), build sạch + 957 test pass,
verify trên Pixel 7 Pro không crash/ANR, không cần Slice i18n riêng.
