# Task 17 — Per-ship loadout persistence (8c, enhance)

**Picked 2026-07-11** qua AskUserQuestion (nhóm "enhance tính năng cũ" — chọn cả 3,
đây là task 1/3). Đóng nốt Wave 8 8c (đã ⏸️ deferred trong feature.md): hiện
`SettingsRepository.preferredBulletType` + `secondaryWeapon` là **1 setting global**
áp dụng cho mọi ship, dù đã có 22 `ShipShape` (Task 03) + hệ Loadout (Wave 6).

## Slice 0 — chốt data shape (TODO)
- DataStore Preferences không hỗ trợ Map trực tiếp → 2 hướng:
  a) 1 key CSV `"<shipShape>|<bulletType>|<secondaryWeapon>"` mỗi dòng (giống pattern
     `LeaderboardEntry` CSV trong `LeaderboardRepository.kt`), hoặc
  b) 1 cặp key riêng theo tên ship: `stringPreferencesKey("loadout_bullet_$shipShape")`.
- Đề xuất (a) — nhất quán với pattern CSV đã có, dễ mở rộng field sau này.

## Slice 1 — SettingsRepository (TODO)
- Thêm `loadoutForShip(shipShape: ShipShape): Flow<Pair<BulletType, SecondaryWeapon>>`
  + `suspend fun setLoadoutForShip(shipShape, bulletType, secondaryWeapon)`.
- Giữ `preferredBulletType`/`secondaryWeapon` cũ làm **fallback default** cho ship
  chưa từng set riêng (migration êm, không mất tiến trình user cũ).

## Slice 2 — audit call site (TODO)
- Rà mọi nơi đọc `preferredBulletType`/`secondaryWeapon` global: loadout picker
  dialog, `GameState` init loadout. Chuyển sang đọc theo `selectedShipShape` hiện tại.

## Slice 3 — migration + test (TODO)
- Unit test: ship A đổi loadout không ảnh hưởng ship B; ship chưa từng chọn → dùng
  default global cũ.
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device (TODO)
- Đổi loadout 2 ship khác nhau, restart app, xác nhận mỗi ship nhớ đúng loadout riêng.

## Trạng thái
📋 **TODO** — chưa bắt đầu.
