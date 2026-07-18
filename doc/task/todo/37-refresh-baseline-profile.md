# Task 37 — Refresh baseline profile (performance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tối ưu hiệu suất"). Module
`:baselineprofile` đã tồn tại từ Task 11 (đợt 3), generate baseline profile
qua macrobenchmark + UiAutomator. Từ đó tới nay đã thêm nhiều feature mới
(ship XP, prestige, per-ship loadout, one-handed mode, colorblind migration,
...) — profile hiện tại (`app/src/main/baseline-prof.txt`) có thể đã lỗi
thời với code path khởi động/first-frame hiện tại.

## Slice 0 — chốt thiết kế (TODO)
- Không cần chốt số liệu thiết kế mới (task này là regenerate, không phải
  feature mới) — chỉ cần xác nhận: device nào dùng để generate (theo R3,
  hỏi device target trước khi chạy), và có cần review diff giữa profile cũ
  và mới trước khi commit hay không (đề xuất: có review, tránh commit profile
  bị lỗi do chạy nhầm flow).

## Slice 1 — chạy lại generate (TODO)
- `./gradlew :baselineprofile:generateBaselineProfile` trên device đã chọn
  (theo R3).
- So sánh diff `app/src/main/baseline-prof.txt` cũ vs mới.

## Slice 2 — verify build với profile mới (TODO)
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.
- Build release APK, xác nhận `androidx.profileinstaller` vẫn hoạt động bình
  thường trên API 24-30 (không cần test physical device đủ range, chỉ cần
  build không lỗi + profile cài được).

## Slice 3 — eyeball device (đo startup time trước/sau nếu tiện) (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro thấp (đã có module + quy trình từ Task 11,
chỉ regenerate, không code mới).
