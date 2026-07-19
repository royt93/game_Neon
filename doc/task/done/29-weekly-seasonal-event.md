# Task 29 — Weekly/seasonal event

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng mới"). Tái dùng 1:1
pattern daily challenge (Task 08) — `dailyEntries(dayKey = todayUtcDayKey())`
ở `LeaderboardRepository` — mở rộng lên chu kỳ tuần (7 ngày UTC).

## Slice 0 — chốt thiết kế (DONE 2026-07-19)
- Chu kỳ: **tuần (7 ngày UTC)** — `todayUtcWeekKey() = epochMillis / (7×MILLIS_PER_DAY)`.
- Nội dung event: **modifier preset đặc biệt theo tuần** (giống daily challenge,
  deterministic qua `Math.floorMod(weekKey, poolSize)`).
- Reward: **mineral bonus (+300, so với daily +100) + leaderboard riêng theo tuần**.

## Slice 1 — key bucket theo tuần (DONE)
- `LeaderboardRepository.todayUtcWeekKey()` (companion object, cạnh
  `todayUtcDayKey()`), `MILLIS_PER_WEEK = MILLIS_PER_DAY * 7`.

## Slice 2 — content rotation (DONE)
- `ui/game/modifier/WeeklyEvent.kt` — mirror `DailyChallenge.kt` 1:1:
  `modifierFor(weekKey)` deterministic, pool = mọi `RunModifier` trừ NONE.
- Test: `WeeklyEventTest.kt` (same-week → same modifier, never NONE, cycles
  through whole pool, wraps around, negative key an toàn).

## Slice 3 — leaderboard/reward riêng theo tuần (DONE)
- `LeaderboardRepository`: `WEEKLY_KEY`, `submitWeekly()`, `weeklyEntries()` —
  mirror bucket `DAILY_KEY`/`submitDaily()`/`dailyEntries()` 1:1 (CSV
  `weekKey|score|timestamp`, cap `MAX_ENTRIES` mỗi tuần).
- `MetaProgressionRepository`: `LAST_WEEKLY_EVENT_KEY`, `claimWeeklyEvent()`,
  `weeklyEventAvailable()` — mirror `claimDailyChallenge()` 1:1 (chống farm,
  1 lần/tuần, độc lập hoàn toàn với key theo ngày của daily).
- Test tích hợp (real DataStore, Robolectric):
  `MetaProgressionWeeklyEventIntegrationTest.kt` — claim 1 lần/tuần, mở lại
  tuần mới, daily/weekly claim độc lập không đụng nhau.

## Slice 4 — UI entry + i18n (DONE)
- `DialogModifierPicker.kt` — card "Sự kiện tuần này" (accent `palette.violet`,
  glyph `◈`) cạnh card "Thử thách hôm nay", tap → `setLastModifier` +
  `onPicked()` (không cần field phân biệt "nguồn chọn" — claim chỉ so khớp
  giá trị modifier đang chơi, không quan tâm chọn qua card nào).
- `GameScreen.kt` — block claim riêng: so `RunModifier.fromKey(lastModifier)`
  với `WeeklyEvent.modifierFor(thisWeek)`, khớp thì `claimWeeklyEvent`.
- `DialogGameOver.kt` — `submitWeekly()` gọi vô điều kiện cạnh `submitDaily`;
  `WeeklyPanel` Composable (mirror `DailyPanel`, accent violet) hiện ở
  `RevealWrap(revealStep >= 8)`, tăng vòng lặp reveal `1..8` → `1..9`.
- i18n: `weekly_event_label`, `weekly_seed_label`, `weekly_best_this_week`
  thêm vào `values/`, `values-vi/`, `values-en/strings.xml`.
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin
  testDevDebugUnitTest` → BUILD SUCCESSFUL, tất cả unit test pass.

## Slice 5 — eyeball device (DONE 2026-07-19, Samsung SM-S928B)
- Cài `installDevDebug`, mở modifier picker → card "Sự kiện tuần này" hiện
  đúng modifier tuần (kiểm tra chéo bằng tính tay `weekKey % poolSize`, khớp
  100% với modifier app hiển thị). Tap card → menu cập nhật
  "Thử thách: Được ăn cả (×2.5)" — xác nhận `setLastModifier` hoạt động
  đúng qua card mới. Không có quảng cáo che UI trong lúc test (R4 n/a).
- Không chơi hết 1 run tới game-over trong lần smoke test này (tốn thời
  gian, không cần thiết — phần claim/submit/panel đã có test tích hợp DataStore
  thật ở Slice 3 và mirror 1:1 logic daily đã verify on-device ở Task 08).

## Trạng thái
✅ **DONE** — toàn bộ 6 slice hoàn thành, build xanh, test xanh, verify
UI on-device (Samsung SM-S928B).
