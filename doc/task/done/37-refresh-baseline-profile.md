# Task 37 — Refresh baseline profile (performance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tối ưu hiệu suất"). Module
`:baselineprofile` đã tồn tại từ Task 11 (đợt 3), generate baseline profile
qua macrobenchmark + UiAutomator. Từ đó tới nay đã thêm nhiều feature mới
(ship XP, prestige, per-ship loadout, one-handed mode, colorblind migration,
...) — profile hiện tại (`app/src/main/baseline-prof.txt`) có thể đã lỗi
thời với code path khởi động/first-frame hiện tại.

## Slice 0 — chốt thiết kế (DONE)
- Device: Pixel 7 Pro (`2B051FDH3006MU`), máy duy nhất kết nối, dùng trực
  tiếp theo ngoại lệ 1-thiết-bị của R3. Có review diff trước khi commit.

## Slice 1 — chạy lại generate (DONE)
- `./gradlew :baselineprofile:generateBaselineProfile` chạy thành công trên
  device đã chọn.
- Diff `baseline-prof.txt`/`startup-prof.txt`: 2320 dòng thêm / 1650 dòng
  bớt — phản ánh đúng các feature mới thêm từ Task 11 tới nay (ship XP,
  prestige, per-ship loadout, one-handed mode, colorblind migration...).

## Slice 2 — verify build với profile mới (DONE)
- `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`
  pass. Build release APK thành công, `androidx.profileinstaller` không lỗi.

## Slice 3 — eyeball device (DONE)
- Cold start production release đo được `TotalTime: 292ms` trên Pixel 7 Pro
  sau khi cài profile mới (verify qua session build+test+logcat ngày
  2026-07-18/19).

## Trạng thái
✅ **DONE** (2026-07-18) — committed `150c1f2 chore: regenerate baseline and
startup profiles`.
