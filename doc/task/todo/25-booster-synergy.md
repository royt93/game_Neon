# Task 25 — Booster synergy (enhance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "enhance tính năng cũ").
Hệ thống hiện có 26 `BoosterType` implemented rải qua các Wave, nhưng mỗi
booster hoạt động độc lập — không có cơ chế combo/synergy khi nhiều booster
active cùng lúc.

## Slice 0 — chốt thiết kế (TODO)
- Chọn N cặp/nhóm booster có synergy rõ ràng (đề xuất 3-4 cặp, vd 2 booster
  cùng nhóm "damage" active cùng lúc → bonus % thêm; tránh chọn cặp dễ tạo
  combo phá game).
- Cơ chế kích hoạt: tự động phát hiện khi đủ điều kiện (không cần UI riêng),
  hay cần thông báo rõ cho user (banner "Synergy activated!")?
- Giới hạn: synergy có stack nhiều lần không, hay chỉ 1 lần/loại mỗi run?

## Slice 1 — logic phát hiện synergy (TODO)
- `BoosterController` (hoặc controller liên quan): thêm hàm kiểm tra tổ hợp
  booster active hiện tại → trả về bonus tổng hợp.
- Test: các tổ hợp booster đã chọn → xác nhận đúng bonus, tổ hợp không match
  → không có bonus thừa.

## Slice 2 — UI thông báo synergy (TODO)
- Banner/indicator nhỏ khi synergy kích hoạt (tái dùng pattern banner có sẵn
  trong `ui/game/controls/`), i18n vi+en.

## Slice 3 — cân bằng + verify (TODO)
- Playtest: xác nhận không combo nào vượt ngưỡng OP.
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro vừa (26 booster tổ hợp dễ tạo combo OP nếu
không giới hạn kỹ ở Slice 0).
