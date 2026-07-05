# Task 15 — Đạn Airburst (đợt 4, thay Cluster/Mine đã có)

**Picked 2026-07-05.** Cluster (=SPLIT) + Mine (=SecondaryWeapon.MINE) đã tồn tại →
thêm đạn MỚI thực sự khác: **AIRBURST** — trúng địch nổ tung **8 đạn con toả 360°**
(khác SPLIT 3 con thẳng, FIREWORK 5 con thẳng). 5-piece pattern như Task 02.

## Slice 0
- `ShipLaser.moveLaser` thêm `xOffset += xOffsetMovementSpeed` (đạn khác =0 → backward-compat). Cho phép đạn con bay ngang.
- Collision arm AIRBURST: 8 con NORMAL, góc i×45°, `xOffsetMovementSpeed=cos·S`, `yOffsetMovementSpeed=sin·S` (toả 360°).
- Pure helper `airburstVelocities(count, speed): List<Pair<Float,Float>>` (vx,vy) — testable.
- BulletType.AIRBURST + BulletShape + color + LaserCanvas draw + InfoScreen/LoadoutPicker preview + ShopItem unlock + i18n.
- Cập nhật test count/uniqueness/name-set (như Task 02 khi thêm LIGHTNING).

## Slice hoá
- Slice 1: enum + moveLaser + collision + velocities helper + all exhaustive `when` (compile-drive) + shop + test. Compile.
- Slice 2: preview + eyeball emulator.

## Trạng thái
✅ **DONE (2026-07-05, 5-piece đầy đủ).**
- `BulletType.AIRBURST` ("Nổ chùm", ST ×0.55, shopUnlockId=bullet_airburst) + `BulletShape.AIRBURST_STAR` + màu cam 0xFFFF8C42.
- `Airburst.velocities()` (pure): 8 con quạt 25°–155°, tất cả vy>0 (cull off-top an toàn). `ShipLaser` đưa vx/vy vào ctor + `moveLaser` thêm `xOffset += xSpeed` (backward-compat).
- LasersController: spawn arm + collision arm (đẻ 8 con NORMAL toả quạt).
- Exhaustive `when` (9 site): LaserCanvas/InfoScreen×3/LoadoutPicker×4/ShopScreen — reuse firework draw (burst star).
- ShopItem `bullet_airburst` (1050◇). Test: `AirburstTest` (4) + fix count 23→24/name-set/bodyWidth(19f)/color-identity/catalog.
- Compile 2 flavor + **940+ JVM test PASS**. **Eyeball emulator Pixel 10 Pro XL:** shop "Nổ chùm" icon burst cam + "8 mảnh toả quạt · ST ×0.55" · 1050◇. Pixel 7 KHÔNG đụng.

## ✅ TASK 15 DONE — đạn Airburst mới (Cluster=SPLIT, Mine=SecondaryWeapon.MINE đã có sẵn).
