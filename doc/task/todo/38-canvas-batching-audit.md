# Task 38 — Canvas batching audit (performance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tối ưu hiệu suất"). Các
entity mật độ cao (lasers, enemies, spaceObjects, boosters, minerals) đã có
Canvas batched-drawing riêng (`LaserCanvas`, `EnemyCanvas`,
`SpaceObjectCanvas`, `BoosterCanvas`, `MineralCanvas`) theo đúng convention —
task này audit lại xem có entity/draw call nào lọt lưới quy ước (vẽ per-item
qua `Image`/Composable `forEach` thay vì batched Canvas) sau các đợt feature
gần đây, và tinh chỉnh hiệu năng vẽ trong các Canvas hiện có.

## Slice 0 — chốt thiết kế (TODO)
- Phạm vi audit: rà toàn bộ `ui/game/world/` + `ui/game/{combo,damage,pickup,
  spark}/` — xác nhận entity nào (nếu có) đang vẽ per-item ngoài batched
  Canvas mà lẽ ra nên gộp (theo ngưỡng nào coi là "cardinality cao" — đề
  xuất: bất kỳ list nào thường xuyên > 10-15 item cùng lúc).
- Công cụ đo: Android Studio Layout Inspector / GPU rendering profile trên
  device, hay chỉ review code theo convention (không đo runtime)?

## Slice 1 — audit code theo convention (TODO)
- Liệt kê từng entity list hiện có, xác nhận đã batched Canvas hay chưa;
  đặc biệt chú ý các "juice/game-feel" package mới (spark, pickup, combo) —
  vì chúng KHÔNG bắt buộc theo 5-mảnh domain nhưng vẫn nên batched nếu
  cardinality cao.

## Slice 2 — tinh chỉnh Canvas hiện có (nếu cần) (TODO)
- Nếu tìm thấy điểm chưa tối ưu trong Canvas hiện có (vẽ lại toàn bộ path
  mỗi frame thay vì cache, v.v.), sửa tại đúng file đó.
- Test: unit test hiện có (nếu có) cho mapper/controller liên quan không
  hỏng.

## Slice 3 — verify + eyeball (TODO)
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.
- Eyeball device: stress test màn có nhiều entity cùng lúc (boss stage +
  nhiều enemy/laser), xác nhận framerate ổn định.

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro thấp (audit + tinh chỉnh Canvas đã có sẵn,
không thêm entity/domain mới).
