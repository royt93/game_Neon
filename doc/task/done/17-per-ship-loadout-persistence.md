# Task 17 — Per-ship loadout persistence (8c, enhance)

**Picked 2026-07-11** qua AskUserQuestion (nhóm "enhance tính năng cũ" — chọn cả 3,
đây là task 1/3). Đóng nốt Wave 8 8c (đã ⏸️ deferred trong feature.md): trước đây
`SettingsRepository.preferredBulletType` + `secondaryWeapon` là **1 setting global**
áp dụng cho mọi ship, dù đã có 22 `ShipShape` (Task 03) + hệ Loadout (Wave 6).

## Slice 0 — chốt data shape (DONE)
- Chọn hướng (a) trong đề xuất: 1 key CSV `SettingsKeys.SHIP_LOADOUTS`, mỗi dòng
  `"<shipShape.key>|<bulletType.name>|<secondaryWeapon.name>"` — tái dùng đúng pattern
  CSV của `LeaderboardRepository` (parse/replace/`joinToString` bằng `\n`).
- Audit toàn bộ call site (`rg` cả `app/src`): xác nhận đúng 4 chỗ đọc + 3 chỗ ghi,
  không có usage ẩn nào khác (kể cả test — `ShopItemCatalogTest` chỉ nhắc tên biến
  trong comment, không gọi property/setter thật):
  - Đọc: `DialogLoadoutPicker.kt` (bullet + secondary), `ShopScreen.kt` `BulletTab`,
    `GameState.kt` (secondary reactive + bullet run-init one-shot).
  - Ghi: `DialogLoadoutPicker.kt` (2 onClick), `ShopScreen.kt` `BulletTab` onSelect.

## Slice 1 — SettingsRepository (DONE)
- `data/SettingsRepository.kt`: thêm `SettingsKeys.SHIP_LOADOUTS` (CSV key) +
  data class `ShipLoadout(bulletType, secondaryWeapon)`.
- `fun loadoutForShip(shipShape): Flow<ShipLoadout>` — parse CSV tìm dòng khớp
  `shipShape.key`; không có dòng → fallback đọc thẳng key cũ
  `PREFERRED_BULLET_TYPE`/`SECONDARY_WEAPON` (migration êm, không mất pick cũ).
- `suspend fun setLoadoutForShip(shipShape, bulletType, secondaryWeapon)` — thay
  dòng của ship đó trong CSV (giữ nguyên dòng ship khác), viết lại cả block.
- Xoá hẳn `secondaryWeapon`/`preferredBulletType` Flow property công khai và
  `setSecondaryWeapon`/`setPreferredBulletType` — sau khi cả 3 call site migrate,
  2 setter này không còn ai gọi (dead code, xoá theo nguyên tắc lazy). 2 key DataStore
  cũ (`PREFERRED_BULLET_TYPE`/`SECONDARY_WEAPON`) vẫn giữ, chỉ dùng nội bộ làm
  fallback source trong `loadoutForShip`.

## Slice 2 — audit call site (DONE)
- `GameState.kt`: `activeSecondaryWeapon` đổi từ đọc `settingsRepo.secondaryWeapon`
  global sang `remember(runContext.shipShape) { settingsRepo.loadoutForShip(...) }`
  (dùng `runContext.shipShape` — đã tôn trọng cả override Trial-mode, không đọc
  thẳng `selectedShipShape` như trước). Bullet run-init `LaunchedEffect(Unit)` đổi
  `settingsRepo.preferredBulletType.first()` → `settingsRepo.loadoutForShip(runContext.shipShape).first().bulletType`,
  giữ nguyên logic shop-lock-fallback-NORMAL phía sau.
- `ShopScreen.kt` `BulletTab`: thêm đọc `selectedShipShape` (đã có sẵn key
  `settings.selectedShipShape` — cùng cơ chế ship-select tab khác trong cùng
  screen), loadout theo `remember(selectedShape) { settings.loadoutForShip(...) }`;
  `onSelect` gọi `setLoadoutForShip(selectedShape, bullet, loadout.secondaryWeapon)`
  (giữ nguyên secondary hiện có của ship đó).
- `DialogLoadoutPicker.kt`: thêm `produceState` đọc `selectedShipShape` (cùng kiểu
  nullable-chờ-emission-thật như "Round 45 fix 1" đã làm cho bullet/secondary, tránh
  flash sai), rồi `produceState` loadout theo shape đó. 2 onClick (bullet + secondary)
  gọi `setLoadoutForShip(shape, bullet, secondary)` — mỗi click giữ nguyên field còn
  lại (fallback `NORMAL`/`MISSILE` nếu state chưa resolve xong).

## Slice 3 — migration + test (DONE)
- `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`
  — build cả 2 flavor + toàn bộ JVM unit test pass, không regression.
- Test mới (Tier 3, `PersistenceRoundtripTest.kt`) —
  `settings_loadoutForShip_isPerShip_and_falls_back_to_global`: set loadout riêng
  cho `INTERCEPTOR` (PIERCING/MINE) → assert đọc lại đúng; `TANK` chưa từng set
  riêng → assert fallback đúng `NORMAL`/`MISSILE` (không bị `INTERCEPTOR` đè); dọn
  lại `INTERCEPTOR` về mặc định cuối test.

## Slice 4 — eyeball device (DONE)
- Device: Pixel 7 Pro (`2B051FDH3006MU`) — máy duy nhất kết nối, dùng trực tiếp
  theo ngoại lệ 1-thiết-bị của R3. App chưa cài trên máy → `./gradlew installDevDebug`
  trước khi test.
- `./gradlew :app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.tranphuloi.neon.integration.PersistenceRoundtripTest`
  — **9/9 test pass** (8 cũ + 1 mới của Task 17).
- Verify trực quan trên ship mặc định (Tiêm kích/FIGHTER — ship duy nhất free ở
  save mới, không đủ khoáng (0) để đổi sang ship khác thật sự trong UI): vào
  Cửa hàng → tab Đạn → chọn "Xuyên" (PIERCING) cho Tiêm kích → đóng Shop → force-stop
  + relaunch app → mở "Trang bị" → xác nhận "Xuyên" vẫn được đánh dấu ✓ đã chọn
  (persist đúng qua restart, đọc đúng theo ship). Reset lại "Đạn thường" (NORMAL)
  sau khi verify xong, không để lại state test.
- Tính "ship A đổi không ảnh hưởng ship B" được verify qua Tier 3 DataStore test
  thật trên chính thiết bị này (không verify qua UI đổi-ship-thật vì save mới có
  0 khoáng, mọi ship khác đều khoá tiền) — coi là đủ bằng chứng vì test chạy trên
  DataStore thật của máy, không phải mock.
- Không gặp lại hiện tượng app tự văng app khác lần này (khác Task 21/22).

## Trạng thái
✅ **DONE** (2026-07-11) — build + unit test pass, on-device integration test 9/9
pass (Pixel 7 Pro) bao gồm test mới cho Task 17, verify trực quan persist loadout
qua restart app, reset thiết bị về mặc định sau khi test.
