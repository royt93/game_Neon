# Task 24 — Mở rộng skill-tree (enhance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "enhance tính năng cũ").
`MetaProgressionRepository` đã có skill-tree + prestige + XP + daily
challenges + telemetry persistence (từ Task 05 "kỹ năng chủ động theo tàu" +
Task 10 "prestige reset"). Task này thêm nhánh/node mới vào cây kỹ năng hiện
có, không tạo hệ thống mới.

## Slice 0 — chốt thiết kế (DONE)
- 6 node mới thêm vào `SkillNode` enum: `MINERAL_BOOST` ("Máy hút khoáng"),
  `FIRE_RATE` ("Nòng súng nóng"), `BOOSTER_DURATION` ("Lõi năng lượng"),
  `MAGNET_PULL_SPEED` ("Từ trường mạnh"), `PIERCE_CHANCE` ("Đầu đạn xuyên"),
  `SECOND_WIND` ("Hơi thở thứ hai").
- Cơ chế unlock: theo rank nốt nền hiện có (`minRequiredParentRank = 2` trên
  `META_KEY_MAGNET`/`META_KEY_DAMAGE`/`META_KEY_SPEED`/`META_KEY_CRIT`/
  `META_KEY_REGEN`), dùng lại `spendOnNode` sẵn có — không thêm điều kiện mới.
- Chi phí: 120-250 mineral/node cho nhánh thường (maxRank 3-4 —
  `MAGNET_PULL_SPEED` 120, `MINERAL_BOOST`/`BOOSTER_DURATION` 150,
  `FIRE_RATE` 250), 700-800 cho 2 node "Tối thượng" (`PIERCE_CHANCE`,
  `SECOND_WIND` maxRank 1-3) — cân đối với economy hiện tại.
  ⚠️ **Sửa (audit 2026-07-21)**: bản gốc ghi nhầm "120-150", `FIRE_RATE`
  thực tế cost 250, nằm ngoài khoảng đã công bố ban đầu.

## Slice 1 — data model node mới (DONE)
- Mở rộng `SkillNode` enum, giữ key cũ không đổi.
- Test roundtrip persistence: `MetaProgressionShipIntegrationTest` — thêm
  case cho `MINERAL_BOOST` (multi-rank) và `SECOND_WIND` (maxRank 1, no
  rebuy).

## Slice 2 — áp dụng hiệu ứng vào gameplay (DONE)
- `MineralsController`: `getMagnetPullSpeedRank` + `getMineralBoostRank` wired
  vào `addMinerals()`/`processMinerals()`.
- `LasersController`: `fireRateRank` + `pierceChanceRank` wired vào
  `fireLasers()`/`buildOneLaser()`; `ShipLaser`/`ShipBoostedLaser` thêm
  `pierceRemaining` field.
- `ShipController`: `durationMul` (BOOSTER_DURATION) áp cho mọi buff booster +
  phase-shield duration; `SECOND_WIND` → revive 1 lần/run qua
  `secondWindUsed` field trên `Ship`.

## Slice 3 — UI skill-tree (DONE)
- 6 node hiển thị đúng trong `ShopUpgradeContent.kt`, glyph riêng cho mỗi
  node, i18n vi+en.
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest` — xanh.

## Slice 4 — eyeball device (DONE)
- Mua "Nhanh nhẹn" (BASE_SPEED) lên Cấp 2/4 → unlock đúng "Lõi năng lượng"
  (BOOSTER_DURATION, nốt Nhánh của SPEED).
- Mua "Lõi năng lượng" Cấp 1/4 (giá 150) → số dư trừ đúng (612→462), UI cập
  nhật "Mua tiếp → cấp 2 · giá 300".
- Vào Boss Rush chạy nhiều màn liên tiếp (St.14→22, nhiều boss encounter) với
  rank mới active — không crash, không lỗi, không quảng cáo chặn (R4 OK).

## Trạng thái
✅ **Hoàn thành** 2026-07-19 — 6 node mới verify xong cả unit test lẫn
on-device (mua + không crash qua nhiều stage).

⚠️ **Làm rõ phạm vi test (audit 2026-07-21)**: `MetaProgressionShipIntegrationTest`
chỉ cover roundtrip *persistence* (spend/rank/maxRank guard) cho
`MINERAL_BOOST` và `SECOND_WIND`. Không có unit test riêng verify logic áp
dụng thực tế của rank (fire-rate clamp 0.5x sàn, pierce-chance roll,
mineral-boost amount math, duration multiplier) — phần đó chỉ được xác nhận
qua chơi thử on-device, không có bằng chứng test tự động.
