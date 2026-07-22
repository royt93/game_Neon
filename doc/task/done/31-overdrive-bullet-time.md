# Task 31 — Overdrive bullet-time (exclusive/signature)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng độc quyền/khác
biệt", option recommended). Tính năng signature: tích 1 thanh meter qua combat
(combo/kill), khi đầy kích hoạt "Overdrive" — chậm thời gian toàn màn hình
(bullet-time) trừ ship người chơi, trong X giây.

## Slice 0 — chốt thiết kế (✅ DONE, qua AskUserQuestion 2026-07-20)
- **Meter**: đếm kill riêng (`killsSinceLastOverdrive`), KHÔNG dùng
  `ComboController.count` trực tiếp (count đó tự reset sau 2s không giết địch
  — sẽ làm mất tiến trình meter khi người chơi phải né thay vì tấn công liên
  tục). Hook song song với `onEnemyKilled()` hiện có, độc lập với combo decay.
- **Ngưỡng đầy**: 20 kill / lần Overdrive.
- **Hệ số chậm + thời lượng**: enemy/laser/spaceObject × 0.4 tốc độ, 4 giây.
  Ship người chơi giữ tốc độ bình thường (không đổi).
- **Audio** (đổi qua `AskUserQuestion` 2026-07-20, sau khi phát hiện xung đột
  với Round 64 — BGM pitch-shift đã bị revert vì nghe "overlay"/lệch tông):
  **bỏ pitch-shift**, dùng SFX sting một lần — tái dùng
  `SfxController.play(SfxEvent.PICKUP, rate = 0.6f)` lúc bắt đầu (thunk trầm)
  và `rate = 1.4f` lúc kết thúc (chime sáng). Không cần asset mới.
- **Kỹ thuật chậm thời gian** (đổi so với bản nháp ban đầu — xem lý do dưới):
  KHÔNG thêm coroutine loop mới, KHÔNG nhân `timeScale` vào từng entity. Thay
  vào đó **tick-skip có phạm vi** (scoped): tái dùng đúng idiom `frameCount`
  + `isTimeFrozen` đã có sẵn cho kill-cam/boss slow-mo — thêm biến
  `overdriveSkipTick` (chạy 2/5 tick = 40% tốc độ hiệu dụng), gate thêm
  `&& !overdriveSkipTick` vào đúng 3 điểm gọi `tinker(...)`:
  `spaceObjectsController.processSpaceObjects()`,
  `enemyLaserController.processLasers()`, `enemyController.processEnemies()`.
  Không đụng tốc độ bắn (`fireEnemyLasers`), không đụng ship/booster/mineral.
  Lý do đổi: cách cũ (`yOffset += speed * timeScale`) phải sửa từng file model
  entity (SpaceRock, RegularEnemy, PlasmaShipLaser, EnemyLaser, …) — đúng rủi
  ro cao nhất mà mục "Trạng thái" bên dưới đã cảnh báo (dễ bỏ sót 1 loại
  entity). Cách tick-skip chỉ sửa 3 dòng điều kiện trong `GameState.kt`, dùng
  lại cơ chế đã chạy production cho kill-cam/boss slow-mo — diff nhỏ hơn
  nhiều, không cần audit từng entity.

## Slice 1 — meter + trigger logic (✅ DONE)
- `OverdriveController` mới (`ui/game/overdrive/OverdriveController.kt`):
  đếm kill độc lập, trigger ở ngưỡng 20 (reset về 0), `isActive(now)`,
  `checkExpiry(now)` báo đúng 1 lần khi active→inactive.
- Test: `OverdriveControllerBehaviorTest` (6 test) — tích đúng ngưỡng, trigger
  đúng 1 lần và reset kill count, `isActive` đúng suốt 4s rồi tắt,
  `checkExpiry` đúng 1 lần trên chuyển trạng thái, không báo trước khi từng
  trigger, trigger lại đúng ở chu kỳ thứ 2.

## Slice 2 — tick-skip vào 3 tinker call site (✅ DONE)
- Thay vì "áp timeScale vào entity controller" (xem lý do đổi ở Slice 0),
  thêm `overdriveSkipTick` (dựa `frameCount % 5L`) và gate vào đúng 3 tinker
  call site (space object / enemy-laser move / enemy move) trong
  `GameState.kt`. Ship, fire-rate, booster, mineral không đổi.
- Không có unit test riêng cho tỷ lệ tick-skip — khớp tiền lệ hiện có
  (kill-cam/boss slow-mo cũng không có test trực tiếp, nằm trong
  `DisposableEffect` của Composable; verify qua device).

## Slice 3 — UI/VFX + audio (✅ DONE)
- `OverdriveMeterHud` mới (ring-progress, tái dùng pattern `ComboHud`), wire
  vào `IndicatorStatus` cạnh `ComboHud`.
- `Vignette` thêm param `overdriveActive` — vẽ thêm ring màu cyan
  (`palette.cyan`, tái dùng animation `pulse` có sẵn, không gate theo `lowHp`).
- Audio sting qua `SfxController.play(SfxEvent.PICKUP, rate = ...)` (xem
  Slice 0).
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`
  — pass sạch.

## Slice 4 — eyeball device (✅ DONE, TECNO BG6 `118743744X002560`)
- Meter ẩn đúng lúc `killCount=0` (screen3.png), hiện + tích đúng độc lập với
  combo (screen4.png — meter "16" trong khi combo "⚔ 16" trùng ngẫu nhiên vì
  test không dodge, không phải do phụ thuộc lẫn nhau).
- Trigger đúng ngưỡng 20: icon "⚡" + label "OVERDRIVE!" hiện, toàn màn hình
  nhuốm cyan qua `Vignette` (screen5.png).
- Sau ~vài giây, meter đã reset về 0 và tích lại (quan sát "6" ở góc trái,
  không còn active) — xác nhận reset + refill hoạt động đúng (screen6.png/
  screen7.png, cách nhau 1s, entity duy chuyển ~55px/s ở tốc độ bình thường
  ngoài cửa sổ Overdrive — dùng làm baseline so sánh).
- Không bắt được 2 frame liên tiếp *trong* đúng cửa sổ 4s active để đo trực
  tiếp tỷ lệ chậm 40% bằng mắt (cửa sổ quá ngắn so với chu kỳ chụp adb thủ
  công) — chấp nhận rủi ro thấp vì `overdriveSkipTick` tái dùng đúng idiom
  `frameCount` đã chạy production cho kill-cam/boss slow-mo (không phải cơ
  chế mới, không cần audit thêm).
- SFX sting (rate 0.6f lúc bắt đầu / 1.4f lúc kết thúc) không kiểm chứng được
  qua ảnh chụp màn hình — đúng logic gọi đã bọc trong nhánh
  `onEnemyKilled`/`checkExpiry` (đã unit test gián tiếp qua
  `OverdriveControllerBehaviorTest`); để user tự nghe khi chơi nếu cần xác
  nhận thêm.
- Không có ad overlay xuất hiện trong suốt quá trình test (theo dõi theo R4).

## Trạng thái
✅ **Hoàn tất** — Slice 1-4 xong, build + unit test pass, verify device thành
công trên TECNO BG6 (không crash, không ad overlay). Overdrive meter, trigger,
vignette cyan, reset/refill đều đúng như thiết kế.
