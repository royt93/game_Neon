# Task 14 — Visual juice bundle (đợt 4)

**Picked 2026-07-05.** Từ pool 💭 Visual. Rà hiện trạng để tránh trùng:
- **Color grading per chapter** — ✅ ĐÃ CÓ (`GameScreen.stageTintColor` + layer stageTint per chapter).
- **Ship death** — ✅ ĐÃ CÓ kill-cam (zoom 1.5× + slow-mo + 1.5s delay).
- **Screen shake / flash / vignette / camera zoom on damage** — ✅ ĐÃ CÓ.

→ Cái GENUINELY MỚI (chưa có): **screen-tear / RGB-glitch khi trúng đòn** (chromatic
split bands). Đây là trọng tâm task; các mục kia đã tồn tại nên không làm lại (tránh
trùng/regression).

## Slice 0 — thiết kế
- **Pure** `VisualJuice.damageTearAlpha(elapsedMs, durationMs=250): Float` = `(1 - elapsed/dur).coerceIn(0,1)` — cường độ tear tắt dần 250ms sau damage. Testable JVM (không Compose).
- **`DamageTearOverlay.kt`** (controls, leaf): vẽ vài dải ngang RGB-split (cyan/magenta lệch nhau) alpha × intensity, vị trí dải theo seed (đổi mỗi hit). Gate `!reduceMotion` (glitch = motion, tôn trọng a11y).
- **Wire GameScreen**: `damageElapsed` sẵn có → `tearAlpha = if (reduceMotion) 0 else damageTearAlpha(damageElapsed)`; `if (tearAlpha>0) DamageTearOverlay(...)` zIndex cao (cạnh flash@811).

## Test
- `VisualJuiceTest` (pure): alpha=1 ngay khi damage (elapsed 0), giảm tuyến tính, =0 sau duration, coerce.

## Slice hoá (gộp — nhỏ)
- Slice 1: pure + overlay + wire + test → compile.
- Slice 2: eyeball emulator Pixel 10 Pro XL (trúng đòn → glitch tear thoáng).

## Trạng thái
✅ **DONE (2026-07-05).** User chốt "glitch-tear khi trúng đòn".
- **Phát hiện:** color-grade (stageTint), ship death (kill-cam), chromatic-mép + shake + flash + vignette + camera-zoom → ĐỀU ĐÃ CÓ. Bundle chỉ thiếu 1 flavor: glitch dải ngang.
- **Thêm mới:** `VisualJuice.damageTearAlpha` (pure) + `DamageTearOverlay` (5 dải ngang RGB-split cyan/magenta, alpha 0.35×intensity, tắt dần 250ms, gate reduceMotion) + wire GameScreen (seed = lastShipDamageMillis, zIndex 253).
- Test `VisualJuiceTest` (5). Compile 2 flavor + JVM PASS.
- **Eyeball emulator Pixel 10 Pro XL:** trúng đòn → 5 dải glitch RGB cắt ngang màn (chồng flash đỏ) đọc đúng "màn hình rách". (Temp bump duration 1500ms để bắt, đã revert 250ms + verify.) Pixel 7 KHÔNG đụng.

## ✅ TASK 14 DONE — thêm glitch-tear; các juice khác (grade/death/chromatic) đã có sẵn.
