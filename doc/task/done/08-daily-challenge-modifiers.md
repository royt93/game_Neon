# Task 08 — Thử thách hằng ngày + modifier

**Loại:** Feature mới (replay/economy) · **Ưu tiên:** trung bình · **Trạng thái:** 🟡 In progress (Slice 0 chốt 2026-07-04)

## ✅ Slice 0 — ĐÃ CHỐT (2026-07-04)
- **Modifier deterministic theo ngày:** `DailyChallenge.modifierFor(dayKey)` = pick từ 9 `RunModifier` (trừ NONE) theo `dayKey % 9` (mọi người cùng modifier trong ngày). *Deterministic-spawn (cùng địch) DEFER — rủi ro seed toàn RNG.*
- **UI:** thêm mục "⚡ THỬ THÁCH HÔM NAY" đầu dialog modifier picker (hiện modifier ngày + chọn 1 phát).
- **Thưởng:** +minerals **1 lần/ngày** khi hoàn thành (chống farm qua claim-day, mẫu ĐIỂM DANH).
- **Replay:** tự do; daily leaderboard (`submitDaily` đã gọi mọi GAME_OVER) tự giữ điểm cao nhất.

## Rã slice
- [x] **Slice 1 (2026-07-04)** — `DailyChallenge.modifierFor(dayKey)` (floorMod pool 9) + `REWARD_MINERALS=100` + `DailyChallengeTest` **5**.
- [x] **Slice 2 (2026-07-04)** — `MetaProgressionRepository.claimDailyChallenge/dailyChallengeAvailable` (atomic 1 lần/ngày, key `last_daily_challenge_day`) + integration test **2**.
- [x] **Slice 3 (2026-07-04)** — modifier picker: card "⚡ THỬ THÁCH HÔM NAY +100◇" (set lastModifier=daily) + thưởng ở GAME_OVER (GameScreen: nếu run modifier==daily → claim). JVM **896**.
- [x] **Slice 4 (2026-07-04)** — **verify device A50s**: card render (GLASS_CANNON hôm nay), tap → `setLastModifier=glass_cannon`; chơi → GAME_OVER → `claimDailyChallenge day=20638 +100◇` + `Daily challenge complete`.

## Trạng thái: ✅ Implemented (2026-07-04) — verify device đầy đủ (chọn + chơi + thưởng)
Ghi chú: deterministic-SPAWN (cùng địch) chưa làm — chỉ modifier cố định theo ngày (defer, rủi ro seed toàn RNG).

## Mục tiêu
Chế độ **Thử thách hằng ngày**: seed cố định theo ngày (UTC) → cùng modifier cho mọi người trong ngày, đua trên **daily leaderboard** (đã có). Tăng lý do quay lại mỗi ngày.

## Quyết định cần chốt (Slice 0)
- [ ] Seed: theo `todayUtcDayKey()` (đã có ở LeaderboardRepository) → chọn modifier + tham số RNG xác định.
- [ ] Modifier: tái dùng `RunModifier` (GLASS_CANNON/BERSERKER/…) xoay theo ngày, hay bộ mới?
- [ ] Phần thưởng: minerals bonus 1 lần/ngày khi hoàn thành? Chống farm?
- [ ] 1 lượt/ngày hay chơi lại tự do (chỉ tính điểm đầu)?

## Điểm tích hợp (code thật)
- `data/LeaderboardRepository.kt` — `dailyEntries(dayKey = todayUtcDayKey())` đã có → dùng luôn cho bảng ngày.
- `RunModifier` + `EffectiveStats` — modifier áp vào run (mẫu 25x random modifier).
- `navigation/Navigation.kt` + Menu — thêm route/entry "Thử thách hằng ngày" (mẫu THỬ THÁCH có sẵn ở MenuScreen).
- RNG xác định: **KHÔNG** dùng `Random.nextInt()` trực tiếp (seed theo dayKey) để mọi người cùng màn.
- Điểm danh (`ĐIỂM DANH HÔM NAY`) đã có — tham chiếu cơ chế daily reward.

## Rủi ro
- RNG seed xác định phải phủ mọi spawn (enemy/booster) mới "cùng màn" thật. Chống farm phần thưởng.

## Test
- Unit: seed→modifier xác định (cùng dayKey → cùng kết quả). Integration: leaderboard ngày ghi đúng (Robolectric).
