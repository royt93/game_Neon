# Task 08 — Thử thách hằng ngày + modifier

**Loại:** Feature mới (replay/economy) · **Ưu tiên:** trung bình · **Trạng thái:** 📋 todo

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
