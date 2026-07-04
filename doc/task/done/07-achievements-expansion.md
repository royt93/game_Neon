# Task 07 — Mở rộng thành tựu

**Loại:** Content (low-risk) · **Ưu tiên:** trung bình (an toàn nhất) · **Trạng thái:** ✅ Implemented (2026-07-04) — verify device

## Thực hiện (2026-07-04)
- **5 enum** `Achievement`: DRONE_DUO/CHAIN_TRIPLE/LIGHTNING_MASTER/SHIP_MAX_LEVEL/SHIP_COLLECTOR (hardcode VN đúng pattern enum). Count test 36→41.
- **Điều kiện thuần** `AchievementUnlocks` (single-source ngưỡng) + `AchievementUnlocksTest` **5 test**.
- **Wiring**: in-game DRONE_DUO (droneSpawnRef) + CHAIN_TRIPLE (lightningChainRef) qua `unlockAchievement` (coroutineScope.launch); lifetime LIGHTNING_MASTER/SHIP_MAX_LEVEL/SHIP_COLLECTOR ở `GameScreen.awardLifetime` (dùng `bulletKills`/`allShipXp`/`allRanks` sẵn có).
- **Verify device S24 Ultra**: inject fighter Lv5 → run → logcat `AchievementsRepository.unlock: NEW ship_max_level`. JVM 869→**874**.

## ✅ Slice 0 — ĐÃ CHỐT (2026-07-04): 5 thành tựu LOW-RISK
Chỉ dùng data/flow SẴN CÓ (không counter persistence mới):
- **DRONE_DUO** (Bronze) — in-game: nuôi 2 drone cùng lúc (`droneController.count()>=2`).
- **CHAIN_TRIPLE** (Silver) — in-game: 1 phát sét lan ≥3 địch (`lightningChainRef` hops≥3).
- **LIGHTNING_MASTER** (Silver) — lifetime: `bulletKills[LIGHTNING]>=100` (mirror PLASMA_MASTER).
- **SHIP_MAX_LEVEL** (Silver) — lifetime: 1 tàu đạt Lv5 (`allShipXp` value ≥1500, qua ShipXpLevels).
- **SHIP_COLLECTOR** (Gold) — lifetime: sở hữu ≥10 tàu (`ShipShopLogic.isOwned` trên allRanks).
- **Defer:** boss "cả 3 độ khó" / "nghe hết beat" — cần counter persistence mới (rủi ro cao hơn), để đợt sau.
- i18n: title/description hardcode VN trong enum (đúng pattern `Achievement` hiện tại).

## Mục tiêu
Thêm thành tựu gắn kết 4 feature vừa làm: drone (Task 01), sét chain (Task 02), ship level/XP (Task 03), boss narrative (Task 04). Rủi ro thấp — tái dùng `AchievementsRepository` (unlock-once-and-persist).

## Ý tưởng thành tựu (chốt danh sách ở Slice 0)
- [ ] Drone: "Nuôi 2 drone cùng lúc", "Drone hạ 50 địch", "Drone vỡ 10 lần".
- [ ] Sét chain: "Chain 3 địch 1 phát", "Hạ 100 địch bằng Sét Chain".
- [ ] Ship XP: "Đưa 1 tàu lên Lv5", "Sở hữu 10 tàu", "Max level 3 tàu".
- [ ] Boss: "Hạ FinalBoss cả 3 độ khó", "Nghe hết beat 5 chương".
- [ ] Chốt: bao nhiêu cái + tier (Bronze/Silver/Gold có sẵn)?

## Điểm tích hợp (code thật)
- `data/AchievementsRepository.kt` + `Achievement` enum — thêm entry mới.
- Counters: cần thêm đếm (drone kills, chain kills, ship max-level) — 1 số đã có (`bossesDefeatedTotal`, `finalBossDefeated`); số khác cần thêm ở `GameState`/telemetry.
- `AchievementBanner` (`ui/game/controls/`) — hiển thị unlock (đã có).
- i18n: tên/mô tả thành tựu → cả `values-vi` + `values-en` (+ default).

## Rủi ro
- Thấp. Chủ yếu là thêm counter + đảm bảo unlock đúng thời điểm (idempotent qua repo).

## Test
- Unit: điều kiện unlock (thuần, nếu tách logic). Integration: unlock persist (Robolectric, mẫu `MetaProgressionShipXpIntegrationTest`).
