# Task 34 — Procedural run-modifier draft (exclusive/signature)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng độc quyền/khác
biệt"). Mở rộng hệ `RunModifier` (9 modifier preset cố định hiện có) thành cơ
chế roguelike draft: đầu mỗi run, hệ thống random tạo N modifier "procedural"
(kết hợp ngẫu nhiên các multiplier field: hpMul/damageMul/speedMul/...),
người chơi chọn 1 trong số đó trước khi bắt đầu.

## Slice 0 — chốt thiết kế (TODO)
- Phạm vi random mỗi field multiplier (min/max hợp lý — tái dùng cap hiện có
  của `EffectiveStats`, vd hpMul 0.3-3.0 — nhưng RNG per-run cần range hẹp
  hơn để tránh roll ra modifier vừa yếu vừa mạnh cùng lúc, phá cân bằng).
- Số lựa chọn hiển thị mỗi run (đề xuất 3, giống `DialogBuffPicker` pattern).
- RNG seed: có cần deterministic theo ngày (tái dùng pattern daily challenge
  Task 08) hay random thuần mỗi lần vào game?
- Loại trừ tổ hợp phá game: liệt kê rõ combo field nào KHÔNG được cùng roll
  (vd speedMul quá cao + hpMul quá thấp cùng lúc = chết ngay lập tức, không
  vui).

## Slice 1 — logic gen procedural modifier thuần (TODO)
- Hàm pure `generateProceduralModifiers(seed, count): List<RunModifier>`,
  test đơn vị: cùng seed → cùng kết quả (deterministic), giá trị luôn trong
  range đã chốt.

## Slice 2 — UI draft picker (TODO)
- Dialog chọn trước khi vào run (tái dùng pattern `DialogBuffPicker`), hiển
  thị rõ từng multiplier field của mỗi lựa chọn, i18n vi+en.

## Slice 3 — cân bằng + test (TODO)
- Playtest nhiều seed khác nhau, xác nhận không có tổ hợp nào phá game hoặc
  vô dụng hoàn toàn.
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro cao (RNG cân bằng khó kiểm soát hết mọi
tổ hợp, cần range + loại trừ rõ ràng ở Slice 0 để không tạo modifier phá
game). Để cuối cùng nhóm Exclusive theo đề xuất thứ tự.
