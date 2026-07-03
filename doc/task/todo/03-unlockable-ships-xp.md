# Task 03 — Unlockable Ships + XP/Level mỗi tàu

**Loại:** Progression sâu (enhance meta) · **Ưu tiên:** trung bình · **Trạng thái:** 📋 todo
**Scope lớn — chia slice rõ, làm dần.**

## Hiện trạng (đã có)
- `ui/game/ship/shape/ShipShape.kt` (enum tàu), `ShipShopLogic.kt` (mua/mở khoá theo minerals), `ShipShapeColorMap.kt`.
- `MetaProgressionRepository` (lifetime minerals, node ranks, `spendOnNode`) + `MetaProgressionShipIntegrationTest` (roundtrip DataStore).
→ Đã có nền "mua tàu". Task này **thêm XP/level mỗi tàu + bonus theo level + cổng mở khoá theo tiến trình**.

## Quyết định cần chốt (Slice 0)
- [ ] Nguồn XP: theo điểm/khoáng/địch diệt mỗi run? (đề xuất: XP = enemiesKilled + bonus boss, cộng cho tàu đang dùng).
- [ ] Bảng level (đề xuất 5 level, ngưỡng luỹ tiến) + bonus mỗi level (đề xuất: +hp hoặc +dmg nhỏ, tránh phá cân bằng).
- [ ] Cổng mở khoá tàu: chỉ theo minerals (đã có) hay thêm điều kiện (đạt level X tàu trước / thành tựu)?

## Slices
1. **Persistence XP** — thêm vào `MetaProgressionRepository`: `shipXp(shipKey): Flow<Int>`, `addShipXp(shipKey, amount)`. Key riêng, atomic edit. Test roundtrip (mở rộng `MetaProgressionShipIntegrationTest` hoặc file mới).
2. **Logic level thuần** — hàm pure `shipLevel(xp): Int` + `xpForNextLevel(level)` + `levelBonus(shipShape, level)`. Test `ShipXpLogicTest` (JVM, ngưỡng + đơn điệu).
3. **Trao XP cuối run** — ở `GameScreen`/`DialogGameOver` (nơi bank minerals) cộng `addShipXp` cho tàu đang dùng.
4. **Áp bonus vào gameplay** — đọc level tàu hiện tại → cộng bonus (hp/dmg) khi khởi tạo Ship trong `GameState`. Giữ nhỏ + gated để không phá độ khó.
5. **UI** — màn/khối chọn tàu (mở rộng shop hiện có): thanh XP + level + bonus hiển thị; cổng mở khoá. i18n vi+en.

## Test
- [ ] `ShipXpLogicTest` (JVM): level theo xp, ngưỡng, bonus đơn điệu tăng.
- [ ] Persistence roundtrip cho shipXp (androidTest, ApplicationProvider — theo mẫu `PersistenceRoundtripTest`).
- [ ] Verify build 2 flavor + unit test sau mỗi slice.

## Acceptance
- Mỗi tàu tích XP qua các run, lên level, nhận bonus nhỏ; UI hiển thị đúng; mở khoá hoạt động.
- Không phá cân bằng độ khó (bonus khiêm tốn, gated).

## Rủi ro
- Scope lớn + chạm cân bằng → làm từng slice, chốt số liệu ở Slice 0.
- Đụng DataStore meta (đã có test) → giữ key mới tách bạch, không vỡ save cũ.
