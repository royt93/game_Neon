# Task 18 — Status effect chains + roguelike curses (enhance)

**Picked 2026-07-11** qua AskUserQuestion (nhóm "enhance tính năng cũ" — chọn cả 3,
task 2/3). Mở rộng Wave 4 Combat depth: status (`BURN`/`SLOW`/`STUN`, round 34-35) +
9 buff post-boss (`DialogBuffPicker`, round 34) đã có nền, nhưng "chains deferred"
và "curses deferred" chưa từng làm (feature.md dòng round 34 note).

## Slice 0 — chốt thiết kế (TODO)
- **Chain**: khi 1 địch trúng status, có % lan sang địch khác trong bán kính X (chỉ
  BURN/SLOW hợp lý lan; STUN lan dễ quá OP → cân nhắc loại trừ hoặc giảm mạnh %).
- **Curse**: mở rộng `DialogBuffPicker` — pick 1 trong N option thấy cả buff lẫn
  curse đi kèm (vd "+30% damage nhưng -20% HP tối đa"). KHÔNG bắt buộc chọn curse
  (giữ pure-buff option luôn có mặt) — tránh ép user vào lựa chọn xấu.

## Slice 1 — status chain (TODO)
- `StatusEffectController` (hoặc controller tương ứng): thêm bán kính lan +
  cooldown per-chain (tránh lan vô hạn 1 frame → toàn màn hình cùng status).
- Test: 3 địch đứng gần, 1 địch BURN → xác nhận đúng N địch lân cận nhận BURN,
  không lan quá bán kính, không lan 2 lần liên tiếp cùng 1 tick.

## Slice 2 — curse trong buff picker (TODO)
- `RunModifier`/buff data model: thêm field curse optional (áp dụng cùng lúc buff).
- `DialogBuffPicker` UI: hiển thị rõ 2 phần buff (xanh) / curse (đỏ) trên cùng card.

## Slice 3 — cân bằng + test (TODO)
- Playtest tay: xác nhận không có combo buff+curse nào phá game (soft-lock hoặc
  quá OP). Unit test coverage cho chain radius + curse application.
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. **Rủi ro cân bằng cao nhất trong đợt 4** — nên làm
sau khi có kết quả eyeball rõ ràng, có thể cần playtest nhiều vòng hơn task khác.
