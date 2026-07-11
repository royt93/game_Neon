# Task 21 — Quick restart hotkey (new, QoL)

**Picked 2026-07-11** qua AskUserQuestion (nhóm "tính năng mới" — chọn "QoL nhỏ",
task 1/2). Hiện restart chỉ có ở `DialogGameOver` (sau khi chết) — muốn restart
giữa chừng phải: Pause → thoát về Menu → Play lại. QoL: thêm nút restart nhanh
ngay trong `DialogGamePause`.

## Slice 0 — chốt hành vi (TODO)
- Restart giữa chừng có nên giữ checkpoint hiện tại hay luôn về đầu stage/chapter?
  Đề xuất: dùng lại đúng logic restart của `DialogGameOver` (đã có sẵn — xem
  `popUpTo(Game.route, inclusive=true) { launchSingleTop = true }` trong
  `MainActivity`) để nhất quán hành vi, không tạo logic restart thứ 2.
- Cần xác nhận (confirm dialog) trước khi restart để tránh bấm nhầm mất tiến trình
  đang chơi dở — khác `GameOver` (đã chết, không mất gì thêm).

## Slice 1 — UI (TODO)
- `DialogGamePause` thêm nút "Chơi lại" cạnh Resume/Menu hiện có, kèm confirm
  bước 2 (NeonBottomSheet nhỏ hoặc AlertDialog đơn giản) — tái dùng pattern confirm
  đã có nếu tồn tại trong dialog khác (`DialogSettings` reset, v.v.).

## Slice 2 — wiring (TODO)
- Tái dùng handler restart hiện có trong `MainActivity`/`GameOver` navigation thay
  vì viết lại — chỉ thêm entry point gọi từ Pause.

## Slice 3 — i18n + test (TODO)
- String mới cả `values-vi` + `values-en`.
- Widget test (nếu `DialogGamePause` isolate-test được — theo lưu ý CLAUDE.md,
  dialog này có thể cần test qua UiAutomator integration thay vì `createComposeRule`,
  giống `pause_dialog_resume_button_works`).

## Slice 4 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro thấp — tái dùng logic đã có, chỉ thêm entry point mới.
