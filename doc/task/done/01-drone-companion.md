# Task 01 — Drone Companion (bạn đồng hành tự bắn)

**Loại:** Feature mới (entity domain) · **Ưu tiên:** cao · **Trạng thái:** ✅ Implemented (2026-07-03, 7/7 slice)

## Mục tiêu
Một drone bay lượn quanh tàu người chơi, **tự bắn** đạn yếu vào địch gần nhất. Nhặt được qua booster/power-up, tồn tại có thời hạn (hoặc vĩnh viễn theo thiết kế chốt ở Slice 0).

## ✅ Slice 0 — ĐÃ CHỐT (2026-07-03)
- **Nguồn spawn = KẾT HỢP CẢ 3:**
  - **Skill-tree unlock (vĩnh viễn):** node meta bằng lifetime minerals (`MetaProgressionRepository`) mở tính năng drone; rank có thể tăng maxDrones/HP/dmg.
  - **Booster pickup (trong run):** `BoosterType.DRONE` rơi trong màn → thêm 1 drone (tới `maxDrones`). Chỉ ý nghĩa khi đã unlock (nếu chưa unlock: không rơi / rơi nhưng thành mineral).
  - **Hiển thị power-up:** dùng `PowerUpIndicators` để show số drone đang hoạt động (không countdown vì hết hạn theo HP).
- **maxDrones = 2** → BẮT BUỘC `DroneCanvas` batched.
- **Thời hạn = CÓ HP, chết thì mất:** drone có HP; trúng enemy laser / va chạm địch → mất HP; HP≤0 → vỡ (nổ nhỏ) + remove.
- **Đạn drone = DÙNG CHUNG hệ laser** + thêm field nguồn (vd `LaserSource.SHIP | DRONE`) để phân biệt render/âm thanh nếu cần; tái dùng collision hiện có.
- **Test: đủ 3 tầng** — unit (controller/logic thuần) + widget (indicator/số drone) + integration (nhặt booster → drone xuất hiện; drone chết khi trúng đạn) trên thiết bị thật.

## Tiến độ
- [x] **Slice 1 (2026-07-03) — domain + controller + unit test.** `Drone.kt` (+ `DroneShot`), `DroneController.kt` (orbit/nearestEnemy/fireStep/damage/processDrones, lambda setter, tinker ids). `DroneControllerBehaviorTest` **11/11 pass**; main+production compile OK.
- [x] **Slice 2 (2026-07-03) — UI + render + wiring.** `DroneUI` + `DroneToDroneUIMapper` + `DroneCanvas` (neon body + halo + vòng HP). Wire GameState (state `drones`, `droneController`, orbit tinker, data class field, return map, `droneMapper`) + GameWorld (param + `DroneCanvas`) + GameScreen. **TEMP:** spawn 1 drone lúc vào trận để verify (Slice 4 sẽ thay bằng booster). Compile 2 flavor OK, JVM 819/819.
- [x] **Slice 3 (2026-07-03) — laser source flag + drone bắn hệ laser chung.** `LaserSource{SHIP,DRONE}` + field `source` (default SHIP) trên interface `Laser`. `DroneLaser` (directional, factory `aimedAt` chuẩn hoá tốc độ, tự huỷ khi ra màn) bơm vào `shipLasers` qua `LasersController.addDroneLasers()` (tái dùng move/cleanup/collision). GameState thêm fire tinker (`fireStep`→`DroneLaser`). `DroneLaserBehaviorTest` **7/7 pass**. Tổng JVM 826/826, 2 flavor compile OK.
- [x] **Slice 4 (2026-07-03) — `BoosterType.DRONE_BOOSTER` spawn + pickup.** Thêm enum `DRONE_BOOSTER` (weight 6, total 292→298) + shape `DRONE_ROTOR` + tint teal + glyph "◈" (mapper + BoosterCanvas shape/glyph-vector arm). `ShipController` thêm callback `onDroneBoosterPickedUp`; GameState wire qua `droneSpawnRef` (@Volatile deferred ref) → `droneController.addDrone(...)`, **gỡ TEMP spawn** của Slice 2. InfoScreen: 4 nhánh label (title/desc/tip/duration). Cập nhật test count (37 types, weight 298) + glyph-vector set. Tổng JVM 826/826, 2 flavor OK.
- [x] **Slice 5 (2026-07-03) — skill-tree unlock + maxDrones theo rank.** Thêm `SkillNode.DRONE_FLEET` (key `meta_drone`, tier-1, parent HỎA LỰC rank≥2, maxRank 2, cost 320) + `EffectiveStats.META_KEY_DRONE`. GameState đọc `droneRank` (1 lần/run từ `runContext.metaUpgrades`, coerce 0..2) → `DroneController.maxDrones = droneRank` (rank 0 = khoá, không spawn). `BoosterController` thêm gate `droneUnlocked` → chưa mở khoá thì **không rơi** DRONE_BOOSTER (skip roll như NO_SHIELDS). Node tự xuất hiện ở skill-tree (ShopUpgradeContent iterate `SkillNode.values()`) + tự persist (`allRanks` generic theo key). Test: +2 (drone maxDrones=0 no-op; booster gate khi locked). Tổng JVM 828/828, 2 flavor OK.
- [x] **Slice 6 (2026-07-03) — indicator số drone + collision drone↔đạn địch.** `PowerUpIndicators` thêm `droneCount` → badge tĩnh cyan "◈N" (không countdown vì hết theo HP); GameScreen truyền `gameState.drones.size`. `DroneController.monitorDroneCollision(enemyLasers)` (AABB thuần) → đạn địch chồng drone: đạn tan + drone trừ HP (impactPower), HP≤0 → vỡ; trả `DroneHit(x,y,destroyed)`. GameState tinker `collisionId` (Millis 16): spark mỗi cú + explosion khi vỡ. Test: +5 (trúng/vỡ/không-chồng/đạn-đã-destroyed/1-đạn-1-drone) → DroneController 17 tests. Tổng JVM 833/833, 2 flavor OK.
- [x] **Slice 7 (2026-07-03) — widget + integration test + i18n. ✅ TASK HOÀN TẤT.** Widget: `PowerUpIndicatorsDroneWidgetTest` (badge "◈N" hiện/ẩn theo droneCount) **2/2 pass trên SM-S928B**. Integration (Robolectric JVM): `MetaProgressionDroneIntegrationTest` (DRONE_FLEET locked-by-default / mua tăng rank / trần maxRank / thiếu minerals) **4/4 pass**. i18n: không phát sinh string resource mới — badge là glyph; nhãn InfoScreen theo pattern hardcoded-VN của file (bách khoa VN-only). Tổng JVM **837/837**, 2 flavor compile OK.

## Kiến trúc — bám "5-mảnh" (mẫu: `ui/game/booster/`)
Package `ui/game/drone/`:
- [x] `Drone.kt` — domain `@Immutable Serializable` (xOffset/yOffset, orbitAngle, hp, lastFireMillis, size) + `DroneShot`.
- [ ] `DroneUI.kt` — UI projection (drawableId, offset, size, rotation, alpha, glow).
- [ ] `DroneToDroneUIMapper.kt` — map domain→UI (gọi cuối `rememberGameState()`).
- [x] `DroneController.kt` — orbit/nhắm địch gần nhất/bắn (cooldown)/HP-vỡ; expose `*Id` + `*RepeatTime`. State ở GameState, nhận lambda setter.
- [ ] Render: `ui/game/world/DroneCanvas.kt` (batched — maxDrones=2).

## Wiring (đúng checklist "thêm entity mới" trong CLAUDE.md)
- [ ] `GameState.kt`: (a) list drone `mutableStateOf/rememberSaveable`; (b) construct `DroneController` truyền getters/setters; (c) thêm `tinker(...)` cho move + fire trong loop hiện có (KHÔNG tạo coroutine mới); (d) thêm vào `GameState` data class trả về; (e) map UI ở cuối.
- [ ] `GameScreen.kt`: plumbing drone UI list.
- [ ] `GameWorld.kt`: render drone + đạn drone.
- [ ] Đạn drone: tái dùng `LasersController`/`Laser` hay list riêng nhẹ? Đề xuất: dùng chung hệ laser với cờ nguồn = drone (tránh nhân đôi collision).
- [ ] Booster (nếu chọn 0a): thêm `BoosterType.DRONE` + `GenerateBooster` + `BoosterToBoosterUIMapper` + drawable + màu (đụng các `when` của booster).

## Assets & i18n
- [ ] Drawable drone neon (vector, hợp palette NeonCyan/Magenta).
- [ ] Chuỗi mới (tên power-up, popup "DRONE!") → thêm **cả** `values-vi` và `values-en`.

## Test
- [ ] `DroneControllerBehaviorTest` (JVM, `app/src/test/`): quỹ đạo quanh tàu trong bound; chọn địch gần nhất đúng; cooldown bắn; hết hạn thì remove. Theo convention `XxxControllerBehaviorTest` (JUnit4, no-mock, lambda setter).
- [ ] (tuỳ) integration: nhặt booster drone → drone xuất hiện (androidTest, UiAutomator).
- [ ] Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Acceptance
- Drone bám tàu mượt, tự bắn địch gần nhất, không tụt FPS (đo qua HotPathPerfTest nếu thêm hot path).
- Không tạo coroutine loop mới; chỉ thêm tinker id (unique, stable).
- Build 2 flavor pass + unit test xanh.

## Rủi ro
- Đụng `GameState.kt` (~1000 dòng) + `GameWorld.kt` — vùng nhạy cảm. Làm từng slice, verify sau mỗi slice.
- Cân bằng: drone quá mạnh phá độ khó → giữ damage thấp + cooldown.
