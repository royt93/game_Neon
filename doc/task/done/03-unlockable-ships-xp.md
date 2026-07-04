# Task 03 — Unlockable Ships + XP/Level mỗi tàu

**Loại:** Progression sâu (enhance meta) · **Ưu tiên:** trung bình · **Trạng thái:** 📋 todo
**Scope lớn — chia slice rõ, làm dần.**

## Hiện trạng (đã có)
- `ui/game/ship/shape/ShipShape.kt` (enum tàu), `ShipShopLogic.kt` (mua/mở khoá theo minerals), `ShipShapeColorMap.kt`.
- `MetaProgressionRepository` (lifetime minerals, node ranks, `spendOnNode`) + `MetaProgressionShipIntegrationTest` (roundtrip DataStore).
→ Đã có nền "mua tàu". Task này **thêm XP/level mỗi tàu + bonus theo level + cổng mở khoá theo tiến trình**.

## ✅ Slice 0 — ĐÃ CHỐT (2026-07-04)
- **Nguồn XP:** `xpForRun = enemiesKilled + 10 × bossesDefeated`, cộng cho **tàu đang dùng** (`gameState.shipShape`).
- **Bảng level:** 5 cấp, ngưỡng XP luỹ kế **0 / 100 / 300 / 700 / 1500** (L1..L5). Bonus = **+2% hpMul mỗi cấp** (L1=+0% … L5=+8%), thuần survivability, khiêm tốn (EffectiveStats cap hpMul 0.3–3.0 vẫn an toàn).
- **Cổng mở khoá:** GIỮ NGUYÊN — chỉ theo minerals (`ShipShopLogic` hiện tại), không thêm điều kiện.

## Rã slice
- [x] **Slice 1 (2026-07-04)** — pure `ShipXpLevels` (levelForXp/xpForLevel/xpToNextLevel/progressInLevel/hpBonusMulForLevel/hpBonusMulForXp/xpForRun) + `ShipXpLogicTest` **7 test**.
- [x] **Slice 2 (2026-07-04)** — `MetaProgressionRepository`: `shipXp(key): Flow<Int>` + `allShipXp: Flow<Map>` + `addShipXp(key, amount)` (atomic, key `shipxp_<key>`) + `MetaProgressionShipXpIntegrationTest` **5 test** (Robolectric).
- [x] **Slice 3 (2026-07-04)** — trao XP cuối run ở `GameScreen` (`addShipXp(gameState.shipShape.key, xpForRun(...))`).
- [x] **Slice 4 (2026-07-04)** — `RunContext.shipLevelHpMul` (đọc XP 1 lần/run → `hpBonusMulForXp`) nhân vào `hpMul` ở `EffectiveStats.compute` (vẫn cap 0.3–3.0).
- [x] **Slice 5 (2026-07-04)** — UI shop ShipRow: dòng "Lv N · +X% HP · còn Y XP" + thanh XP vàng (chỉ tàu sở hữu). **Verify device S24 Ultra**: logcat `addShipXp[fighter] +1 → 1` sau run; shop hiện "Lv1 · +0% HP · còn 99 XP" + bar. i18n: nhãn theo pattern hardcoded-VN của ShopScreen (file 100% VN, không lẫn resource).

## Trạng thái: ✅ Implemented (5/5 slice, 2026-07-04) — verify device đầy đủ (grant + persist + UI). JVM 854→866.

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
