# Task 13 — Counter-attack / Parry (đợt 4)

**Picked 2026-07-05.** Cơ chế kỹ năng mới: bấm nút Parry → cửa sổ ngắn PHẢN ĐẠN
địch (reflect + phản đòn địch gần), có cooldown.

## Slice 0 — thiết kế (tái dùng hệ REFLECT sẵn có)

Đã có: `ShipController.isReflectActive()` = `ship.reflectEndMillis > now` gate nhánh
absorb+retaliate trong collision enemy-laser (1065-1069) → `onReflectAbsorb` →
`reflectRetaliateRef` bắn phản đòn địch gần nhất + visual TrailLineOverlay.

**Parry = kích hoạt cửa sổ reflect tạm thời** → tái dùng NGUYÊN hạ tầng đó, không
đụng collision. Absorb path return trước damage ⇒ parry cũng = miễn thương với đạn hấp thụ.

### Số liệu
- `PARRY_WINDOW_MS = 400` (cửa sổ phản đạn).
- `PARRY_COOLDOWN_MS = 5000` (hồi chiêu).

### Model
- `ParryController.kt` (pure, testable — như ShipAbilityController Task 05):
  - `tryActivate(now): Boolean` — nếu `now >= cooldownEnd`: set `windowEnd=now+WINDOW`, `cooldownEnd=now+COOLDOWN`, return true; else false.
  - `isActive(now)`, `progress(now): Float` (0..1, 1=sẵn sàng), `remainingMs(now)`, `windowEndMillis`.
- `ShipController.grantReflectUntil(end: Long)` — `ship = ship.copy(reflectEndMillis = maxOf(ship.reflectEndMillis, end))`.

### Wire
- GameState: `parryController = remember { ParryController() }`.
- ParryButton onClick: `if (parryController.tryActivate(now)) shipController.grantReflectUntil(parryController.windowEndMillis)`.
- ParryButton cooldownProgress = `parryController.progress(now)` (recompute mỗi frame qua refreshHandler).

### UI (Slice 2)
- `ParryButton.kt` (mirror AbilityButton/SmartBombButton) — glyph khiên/⇋, màu cyan, vòng cooldown. Thêm vào GameScreen cạnh ability/bomb.
- i18n: nút không chữ (glyph) — không cần string.

### Test
- `ParryControllerTest` (pure): tryActivate lần đầu true + set window; trong cooldown → false; hết cooldown → true lại; progress 0→1; isActive trong/ngoài window.

### Slice hoá
- **Slice 1:** ParryController + ShipController.grantReflectUntil + test. Compile.
- **Slice 2:** ParryButton HUD + wire GameScreen/GameState.
- **Slice 3:** eyeball emulator Pixel 10 Pro XL (bấm parry → đạn bị phản, cooldown ring).

## Trạng thái
✅ **DONE (2026-07-05, 3/3 slice).**
- **Slice 1:** `ParryController` (pure: tryActivate/isActive/progress/remainingMs/windowEndMillis) + `ShipController.grantReflectUntil` (set reflectEndMillis → tái dùng absorb+retaliate). Test `ParryControllerTest` (6). Compile + JVM PASS.
- **Slice 2:** `ParryButton.kt` (mirror AbilityButton, cyan ⇋, vòng cooldown) + GameState (`parryController` + expose `parryCooldownProgress`/`activateParry`, guard RUNNING) + GameScreen (stack trên AbilityButton, bottom=306dp). Compile 2 flavor + JVM + androidTest compile PASS.
- **Slice 3:** eyeball emulator Pixel 10 Pro XL — ParryButton render đỉnh stack HUD; tap → log "Parry activated: window→…" + cooldown ring; reflect dùng path REFLECT đã verified. Pixel 7 KHÔNG đụng.

## ✅ TASK 13 DONE — parry = cửa sổ reflect 400ms + cooldown 5s, tái dùng hạ tầng REFLECT.
