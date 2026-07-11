# Task 22 — One-handed mode (new, QoL)

**Picked 2026-07-11** qua AskUserQuestion (nhóm "tính năng mới" — chọn "QoL nhỏ",
task 2/2). Hiện control chia 2 bên màn hình (`ButtonsMovement` + `ButtonSettings`
trong `ui/game/controls/`) mặc định 2 tay. Thêm option gom control về 1 bên cho
người chơi 1 tay (vd cầm điện thoại + tay kia bận việc khác).

## Slice 0 — chốt thiết kế (TODO)
- Layout 1 tay: di chuyển bằng joystick ảo 1 góc + nút bắn/skill dồn cùng bên,
  hay giữ nguyên 2 cụm nhưng cho phép chọn "cả 2 cụm dồn về trái/phải"?
- Đề xuất đơn giản nhất (ít risk nhất): thêm setting 3 giá trị `TWO_HANDED` (default)
  / `LEFT_HANDED` / `RIGHT_HANDED` — khi chọn 1 tay, dồn toàn bộ nút (movement +
  fire + smart bomb) về nửa màn hình tương ứng, không đổi cơ chế điều khiển.

## Slice 1 — SettingsRepository (TODO)
- Thêm enum `ControlHandMode` + Flow tương tự các setting enum khác
  (`ShipSkin`/`Difficulty` pattern) + ProGuard `-keep` rule (theo convention hiện
  có trong `proguard-rules.pro` cho các enum DataStore).

## Slice 2 — UI layout (TODO)
- `ButtonsMovement`/`ButtonSettings`: đọc `ControlHandMode` từ `LocalSettings`,
  đổi vị trí/alignment theo giá trị. Đảm bảo không đè lên HUD khác
  (`SmartBombButton`, `PowerUpIndicators`) khi dồn 2 cụm cùng 1 bên.
- `DialogSettings`: thêm picker chọn `ControlHandMode`.

## Slice 3 — i18n + test (TODO)
- String cả `values-vi` + `values-en`.
- Unit test đọc/ghi setting mới (theo pattern các repo test hiện có).

## Slice 4 — eyeball device cả 3 mode (TODO)
- Xác nhận không nút nào bị che/lấn khi dồn 1 bên trên cả màn hình nhỏ lẫn lớn.

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro chủ yếu ở layout chồng lấn khi dồn nút 1 bên,
không đụng game logic.
