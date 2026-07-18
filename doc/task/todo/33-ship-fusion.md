# Task 33 — Ship fusion (exclusive/signature)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng độc quyền/khác
biệt"). Kết hợp 2 tàu đã sở hữu (`ShipShape` enum, đã unlock qua
`ShipShopLogic`) thành 1 tàu lai mới với stats/visual kết hợp — tính năng
signature chưa từng có trong game bắn súng cùng thể loại.

## Slice 0 — chốt thiết kế (TODO)
- Công thức stats fusion: trung bình cộng 2 tàu gốc, hay cộng dồn có giảm hệ
  số (tránh OP)? Cụ thể hpMul/damageMul/speedMul của tàu fusion so với 2 tàu
  gốc.
- Visual: cần sprite mới cho mỗi cặp fusion (bùng nổ số lượng combination
  C(n,2)) hay dùng hiệu ứng overlay/tint chung trên 1 sprite base (đề xuất:
  overlay/tint — tránh cần asset mới cho từng cặp).
- Điều kiện fusion: cần cả 2 tàu gốc đạt level tối thiểu nào (tái dùng
  `ShipXpLevels` từ Task 03) hay chỉ cần sở hữu?
- Cổng mở khoá: unlock permanent (như mua tàu) hay chỉ chọn theo run (không
  persist, chọn lại mỗi lần)?

## Slice 1 — logic tính stats fusion thuần (TODO)
- Hàm pure `fusedStats(shipA, shipB): ShipStats`, test đơn vị công thức +
  không vượt cap hpMul/damageMul hiện có.

## Slice 2 — UI chọn tổ hợp (TODO)
- Màn/dialog chọn 2 tàu sở hữu để fusion (mở rộng shop/ship-select hiện có),
  hiển thị preview stats kết quả, i18n vi+en.

## Slice 3 — persistence (nếu unlock permanent) (TODO)
- Mở rộng `MetaProgressionRepository` hoặc tương đương, test roundtrip.

## Slice 4 — wiring vào GameState (chọn ship fusion để chơi) (TODO)
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 5 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro cao (cân bằng stats fusion dễ tạo tàu OP
nếu công thức Slice 0 không giới hạn kỹ; UI chọn tổ hợp là mặt mới, chưa có
pattern sẵn để tái dùng).
