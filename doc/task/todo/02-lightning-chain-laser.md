# Task 02 — Lightning Chain Laser (đạn sét nhảy giữa địch)

**Loại:** Enhance hệ đạn · **Ưu tiên:** trung bình · **Trạng thái:** 📋 todo

## Mục tiêu
Thêm loại đạn **LIGHTNING**: khi trúng 1 địch, sét **nhảy** sang N địch gần kế tiếp (chain), mỗi bước giảm damage. Đây là phần "chain" từng bị defer (xem feature.md Wave 4).

## Quyết định cần chốt (Slice 0)
- [ ] Số bước chain tối đa (đề xuất 3) + bán kính nhảy (đề xuất ~120px) + hệ số giảm dmg mỗi bước (đề xuất ×0.7).
- [ ] Chain chọn địch **gần nhất chưa bị nhảy tới** (tránh lặp vô hạn).

## Kiến trúc — bám hệ `ui/game/ship/laser/`
- [ ] `BulletType.kt`: thêm `LIGHTNING(displayName="Sét", ...)`. **Cảnh báo:** đụng ~8 nhánh `when` exhaustive: `buildOneLaser`, collision, `BulletTypeColorMap`, `LaserCanvas`, `DialogLoadoutPicker` (×4), `InfoScreen` (×2), `ShopScreen` — sửa hết để compile + hiển thị đúng.
- [ ] Logic chain: đặt ở nơi xử lý va chạm laser (trong `GameState.kt` hoặc `LasersController`). Khi laser LIGHTNING trúng địch → tính danh sách địch chain (gần nhất, chưa thăm, trong bán kính) → gây dmg giảm dần → phát tia.
- [ ] Render tia sét: `LaserCanvas.kt` (hoặc overlay riêng) vẽ các đoạn zigzag nối các địch trong chain (alpha fade nhanh ~150ms).

## Assets & i18n
- [ ] Màu tia (đề xuất NeonCyan/trắng) trong `BulletTypeColorMap`.
- [ ] `displayName` "Sét" + mô tả → **cả** `values-vi` + `values-en` (nếu dùng string resource) hoặc hằng như các bullet khác (theo pattern hiện tại của BulletType).
- [ ] Preview trong InfoScreen/loadout/shop.

## Test
- [ ] `LightningChainBehaviorTest` (JVM): hàm thuần chọn mục tiêu chain — đúng thứ tự gần→xa, không lặp địch, dừng ở max bước, tôn trọng bán kính; dmg giảm đúng hệ số mỗi bước. (Tách logic chain ra hàm pure để test được, không cần Compose.)
- [ ] (tuỳ) `HotPathPerfTest`: chain tính trên nhiều địch mỗi hit — chốt budget để không O(n²) nặng.
- [ ] Verify build 2 flavor + unit test.

## Acceptance
- Đạn LIGHTNING trúng → sét nhảy tối đa N địch, dmg giảm dần, có tia hiển thị; mọi `when` compile exhaustive.
- Không lag khi nhiều địch (perf test xanh).

## Rủi ro
- Nhiều điểm `when` rải rác dễ sót → grep `BulletType.` để rà đủ.
- Chain trên đám đông có thể tốn CPU → giới hạn bước + bán kính + dùng bình phương khoảng cách (tránh sqrt).
