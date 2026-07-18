# Task 29 — Weekly/seasonal event (new)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng mới"). Tái dùng
pattern `dailyEntries(dayKey = todayUtcDayKey())` đã có ở `LeaderboardRepository`
và daily challenge modifier (Task 08), mở rộng lên chu kỳ tuần/mùa.

## Slice 0 — chốt thiết kế (TODO)
- Chu kỳ: tuần (7 ngày UTC) hay mùa (đề xuất tuần — đơn giản hơn, tái dùng
  `todayUtcDayKey()` pattern với week bucket).
- Nội dung event: modifier preset đặc biệt (tương tự daily challenge) hay
  achievement/reward riêng theo tuần?
- Reward: mineral bonus, cosmetic riêng, hay chỉ leaderboard riêng theo tuần?

## Slice 1 — key bucket theo tuần (TODO)
- Hàm pure `weekKey(): String` (tương tự `todayUtcDayKey()`), test đơn vị
  boundary qua nửa đêm UTC + qua ranh giới tuần.

## Slice 2 — content rotation (TODO)
- Danh sách preset event (modifier/reward) theo tuần, chọn theo `weekKey()`
  (deterministic, không RNG runtime).
- Test: cùng tuần → cùng preset; khác tuần → khác preset.

## Slice 3 — leaderboard/reward riêng theo tuần (TODO)
- Mở rộng `LeaderboardRepository` bucket "weekly", persistence roundtrip.

## Slice 4 — UI entry + i18n (TODO)
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 5 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro vừa (lịch UTC + content rotation, tái
dùng pattern daily đã có sẵn giảm rủi ro).
