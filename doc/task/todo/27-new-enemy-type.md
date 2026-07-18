# Task 27 — Enemy type mới (new)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng mới", option
recommended). 5 loại địch đang chờ implement từ audit trước:
Splitter, Repulsor, Jammer, Missileer, Predator. Task này implement **1**
trong số đó (chọn ở Slice 0), theo đúng pattern 5-mảnh entity.

## Slice 0 — chốt thiết kế (TODO)
- Chọn 1 enemy type trong 5 candidate (đề xuất: **Splitter** — tách làm 2 địch
  nhỏ hơn khi bị hạ, cơ chế rõ ràng nhất để implement + test).
- Chốt số liệu: HP, damage, tốc độ di chuyển, spawn rate (dùng `RepeatTime`
  hợp lý so với enemy hiện có cùng stage), và với Splitter cụ thể: HP/damage
  của 2 địch con so với địch mẹ (đề xuất 40% HP mỗi con, tổng ~80% địch mẹ).

## Slice 1 — domain model + UI projection (TODO)
- Thêm case mới vào `EnemyType` enum (domain), `EnemyUI` mapper tương ứng.
- Test: mapper chuyển đổi domain → UI đúng field.

## Slice 2 — controller logic (TODO)
- `EnemyController`: spawn logic + attack pattern riêng + (nếu Splitter) logic
  tách đôi khi HP về 0.
- Test: spawn đúng rate, attack pattern đúng, tách đôi đúng số lượng/HP.

## Slice 3 — wiring vào GameState + loop (TODO)
- Thêm vào `rememberGameState()` state list, `tinker` calls, `GameScreen`,
  `EnemyCanvas` rendering (batched, không phải Image riêng lẻ).
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro vừa (attack pattern riêng + spawn table,
theo đúng checklist 5-mảnh).
