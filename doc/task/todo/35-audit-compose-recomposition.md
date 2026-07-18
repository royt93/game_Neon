# Task 35 — Audit Compose recomposition (performance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tối ưu hiệu suất", option
recommended). Audit toàn bộ state data class (domain model + UI projection)
trong `ui/game/` xác nhận độ phủ `@Immutable`/`@Stable` — quy ước đã đặt ra
nhưng chưa từng audit toàn bộ có tuân thủ đầy đủ hay không.

## Slice 0 — chốt thiết kế (TODO)
- Phạm vi audit: toàn bộ `*UI` projection type + domain model (Booster,
  Laser, Enemy, Mineral, SpaceObject, Explosion, DamageNumber, PickupPopup,
  ImpactSpark, PickupBurst, ...) + `GameState` data class chính nó.
- Công cụ đo: Compose Compiler metrics (`-P
  plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=...`) để
  liệt kê class "unstable" — xác nhận flag build cụ thể cần thêm vào
  `app/build.gradle` (tạm thời, không commit vĩnh viễn nếu ảnh hưởng build
  time).
- Ngưỡng chấp nhận: sửa hết unstable class tìm được, hay chỉ ưu tiên các
  class nằm trong hot path recomposition cao (GameWorld, HUD elements)?

## Slice 1 — chạy Compose compiler metrics (TODO)
- Bật report tạm thời, build, thu thập danh sách class unstable + composable
  bị skip=false bất thường.

## Slice 2 — sửa từng class thiếu annotation (TODO)
- Thêm `@Immutable`/`@Stable` đúng chỗ, hoặc đổi `var`→`val`/List→ImmutableList
  nếu compiler không tự suy ra được stability.
- Test: `compileDevDebugKotlin` không lỗi, unit test hiện có không hỏng
  (vì đổi field mutability có thể ảnh hưởng logic).

## Slice 3 — verify + so sánh metrics trước/sau (TODO)
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.
- Ghi lại số class unstable giảm được (trước → sau).

## Slice 4 — eyeball device (kiểm tra không giật/lag hơn) (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro thấp (chỉ thêm annotation/audit, không đổi
logic gameplay) — ưu tiên làm sớm trong đợt 5 theo đề xuất thứ tự.
