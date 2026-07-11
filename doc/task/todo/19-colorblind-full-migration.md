# Task 19 — Color blind mode: migrate toàn bộ UI (enhance)

**Picked 2026-07-11** qua AskUserQuestion (nhóm "enhance tính năng cũ" — chọn cả 3,
task 3/3). Round 39 đã dựng hạ tầng `LocalNeonPalette` + Wong palette + Settings
picker, nhưng chỉ **7 file** trong codebase thực sự đọc `LocalNeonPalette`/
`ColorBlindMode` (đã grep xác nhận) — phần lớn dialog/HUD vẫn hardcode `Color(...)`.

## Slice 0 — audit danh sách file cần migrate (TODO)
- Đã xác nhận CHƯA dùng palette: `DialogDifficultyPicker`, `DialogBuffPicker`,
  `DialogGameOver`, `DialogModePicker`, `DialogChapterPicker` (grep ban đầu, còn
  nhiều hơn — cần `grep -rL "LocalNeonPalette" ui/` đầy đủ để ra danh sách chốt).
- Ưu tiên theo tần suất người dùng thấy: HUD combat trước (đang chơi liên tục) →
  dialog thỉnh thoảng mở sau.

## Slice 1 — migrate nhóm HUD/controls (TODO)
- `ui/game/controls/*` (BossHpBar, IndicatorStatus, PowerUpIndicators, HazardOverlay
  Vignette...) — đây là nhóm hiện trên màn hình nhiều nhất lúc combat.

## Slice 2 — migrate nhóm dialog (TODO)
- Các `ui/dlg/*` còn thiếu (danh sách chốt ở Slice 0).

## Slice 3 — verify không quên case nào (TODO)
- Sau khi xong, `grep -rL "LocalNeonPalette"` trên các dir đã migrate phải rỗng
  (trừ file không liên quan màu — vd data class thuần).
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device với từng ColorBlindMode (TODO)
- Bật lần lượt Protanopia/Deuteranopia/Tritanopia (hoặc tên enum thật trong
  `ColorBlindMode`), xác nhận màu đổi đúng ở mọi màn hình vừa migrate.

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro thấp nhất trong 3 task enhance (không đụng
game logic, chỉ đổi nguồn màu), nhưng tốn công rà nhiều file nhỏ lẻ nhất.
