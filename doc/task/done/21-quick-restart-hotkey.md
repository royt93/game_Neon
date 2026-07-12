# Task 21 — Quick restart hotkey (new, QoL)

**Picked 2026-07-11** qua AskUserQuestion (nhóm "tính năng mới" — chọn "QoL nhỏ",
task 1/2). Hiện restart chỉ có ở `DialogGameOver` (sau khi chết) — muốn restart
giữa chừng phải: Pause → thoát về Menu → Play lại. QoL: thêm nút restart nhanh
ngay trong `DialogGamePause`.

## Slice 0 — chốt hành vi (DONE)
- Phát hiện khi bắt tay vào code: nút "Chơi lại" (testTag `pause_restart`) và
  wiring restart (`popUpTo(Game.route, inclusive=true) { launchSingleTop = true }`
  trong `MainActivity`) **đã tồn tại sẵn** trong `DialogGamePause` — nhưng bấm
  là restart NGAY, không có bước xác nhận. Phần việc thật của task 21 thu hẹp
  lại đúng 1 việc: thêm confirm bước 2 trước khi gọi `onRestartGame()`.
- Không tìm thấy confirm pattern có sẵn để tái dùng (`DialogSettings` không có
  reset flow) → tự làm state cục bộ đơn giản nhất (`confirmingRestart` boolean)
  ngay trong `DialogGamePause`, không tạo component/dialog mới.

## Slice 1 — UI (DONE)
- `DialogGamePause.kt`: thêm `var confirmingRestart by remember { mutableStateOf(false) }`.
  Bấm "Chơi lại" lần đầu → set `true`, thay 5 nút thường bằng: message cảnh báo
  mất tiến trình + nút "Đồng ý" (testTag `pause_restart_confirm_yes`, màu
  `NeonRedAlert`) + nút "Hủy" (testTag `pause_restart_cancel`).

## Slice 2 — wiring (DONE)
- Không đụng `MainActivity`/navigation — tái dùng nguyên `onRestartGame()` đã
  có, chỉ gọi nó khi user bấm "Đồng ý" ở bước confirm.

## Slice 3 — i18n + test (DONE)
- String mới cả `values/`, `values-vi/`, `values-en/` `strings.xml`:
  `restart_confirm_message`, `restart_confirm_yes_button`, `restart_confirm_cancel_button`.
- Test: `DialogGamePause` xác nhận không isolate-widget-test được (đúng lưu ý
  CLAUDE.md) → thêm 2 test on-device UiAutomator vào
  `GameLoopMultiTickTest.kt`:
  - `pause_dialog_restart_asks_confirm_then_cancel_returns`
  - `pause_dialog_restart_confirm_restarts_game`
  Cả 2 dùng `By.res("pause_restart_confirm_yes"/"pause_restart_cancel")`.

## Slice 4 — eyeball device (DONE)
- Device: Pixel 7 Pro (`cheetah`, API 17) — máy duy nhất kết nối (`adb devices`).
- `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`
  pass cả 2 flavor + toàn bộ JVM unit test.
- `./gradlew :app:connectedDevDebugAndroidTest` (class `GameLoopMultiTickTest`)
  — **5/5 test pass** trên thiết bị thật, gồm 2 test restart-confirm mới.
- Chụp màn hình xác nhận trực quan: dialog "Tạm dừng" hiển thị đúng nút
  "CHƠI LẠI" (màu magenta, cạnh Resume/Settings/Capture/Menu) như thiết kế.
  Không dừng thao tác tap tay thêm nữa sau khi phát hiện vài lần tap tọa độ
  trên máy thật (nhiều app khác cài sẵn) vô tình văng sang app khác không
  liên quan — rủi ro không đáng để tiếp tục tap mù khi automated UiAutomator
  test đã phủ đúng luồng tương tác cần kiểm.

## Trạng thái
✅ **DONE** (2026-07-11) — build 2 flavor + unit test pass, on-device
integration test 5/5 pass (Pixel 7 Pro), verify trực quan dialog Tạm dừng.
Không có ad che UI trong lúc test (R4 — không áp dụng, dev flavor tắt ad).
