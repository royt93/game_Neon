# Task 24 — Mở rộng skill-tree (enhance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "enhance tính năng cũ").
`MetaProgressionRepository` đã có skill-tree + prestige + XP + daily
challenges + telemetry persistence (từ Task 05 "kỹ năng chủ động theo tàu" +
Task 10 "prestige reset"). Task này thêm nhánh/node mới vào cây kỹ năng hiện
có, không tạo hệ thống mới.

## Slice 0 — chốt thiết kế (TODO)
- Số node mới thêm vào skill-tree (đề xuất: 4-6 node, mỗi node 1 hiệu ứng nhỏ
  rõ ràng — tránh trùng lặp 22 skill chủ động đã có ở Task 05).
- Cơ chế unlock: theo rank hiện có (`spendOnNode`) hay theo điều kiện mới
  (prestige level tối thiểu)?
- Chi phí mineral mỗi node mới — cân đối với economy hiện tại (đã có mineral
  sink qua prestige reset ở Task 10, tránh làm mineral dư thừa vô nghĩa).

## Slice 1 — data model node mới (TODO)
- Mở rộng enum/data class node hiện có trong `MetaProgressionRepository`,
  giữ key cũ không đổi (tránh vỡ save cũ).
- Test: roundtrip persistence cho node mới (theo mẫu
  `MetaProgressionShipIntegrationTest`).

## Slice 2 — áp dụng hiệu ứng vào gameplay (TODO)
- Đọc rank node mới ở nơi phù hợp (`EffectiveStats` hoặc controller liên
  quan), áp hiệu ứng khi tính stats/behaviour đầu run.

## Slice 3 — UI skill-tree (TODO)
- Hiển thị node mới trong màn skill-tree hiện có, i18n vi+en.
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro vừa (cân bằng node mới + không phá save
cũ của `MetaProgressionRepository`).
