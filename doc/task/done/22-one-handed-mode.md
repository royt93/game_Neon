# Task 22 — One-handed mode (new, accessibility/QoL)

**Picked 2026-07-11** qua AskUserQuestion, ngay sau khi Task 21 xong. Mục tiêu:
cho phép người chơi thuận tay trái vẫn dùng game bằng 1 tay thoải mái.

## Slice 0 — chốt thiết kế (DONE)
- Phát hiện khi bắt tay vào code: đề bài gốc nhắc tới `ButtonsMovement` (nút
  trái/phải rời rạc), nhưng comment "Round 77 (R77h)" trong `GameScreen.kt`
  cho thấy 2 nút này **đã bị gỡ khỏi compose tree từ trước** — di chuyển giờ
  là kéo-thả bất kỳ đâu trên màn hình (`detectDragGestures`), không phụ thuộc
  tay thuận. Thu hẹp phạm vi thật của task: chỉ cần mirror cụm nút hành động
  cố định `SmartBombButton`/`SecondaryWeaponButton`/`AbilityButton`/`ParryButton`
  (đang neo `BottomEnd`) sang `BottomStart` khi chọn tay trái.
- Quyết định: `ButtonSettings` (nút pause, `TopEnd`) **không mirror** — `TopStart`
  đã bị `IndicatorStatus` + ActiveBuffsHud chiếm chỗ, dồn pause qua đó sẽ đè lên
  nhau. Lý do này ghi lại trong doc-comment của `ControlHandMode.kt`.
- Thiết kế đơn giản nhất: enum 3 giá trị `ControlHandMode`
  (`TWO_HANDED` mặc định / `LEFT_HANDED` / `RIGHT_HANDED`), không đổi cơ chế
  điều khiển. `RIGHT_HANDED` dùng chung nhánh layout với `TWO_HANDED` (đều là
  `BottomEnd`/`TopEnd`) vì layout gốc vốn đã thiên về tay phải.

## Slice 1 — data layer (DONE)
- `data/ControlHandMode.kt` mới: enum `TWO_HANDED("two_handed")` /
  `LEFT_HANDED("left_handed")` / `RIGHT_HANDED("right_handed")`, theo đúng
  pattern `key`/`displayName`/`fromKey()` của `Difficulty`/`CameraZoom`.
- `SettingsRepository.kt`: thêm `SettingsKeys.CONTROL_HAND_MODE`
  (`stringPreferencesKey`), Flow `controlHandMode` (default `TWO_HANDED`),
  `suspend fun setControlHandMode(value: ControlHandMode)`.
- `app/proguard-rules.pro`: thêm `-keep class com.tranphuloi.neon.data.ControlHandMode { *; }`
  vào nhóm "Settings/persistence enums".

## Slice 2 — UI layout + picker (DONE)
- `GameScreen.kt`: đọc `controlHandMode` qua `LocalSettings.current` (cùng
  chỗ với `reduceMotion`/`vibrationEnabled`). Thêm 2 helper cục bộ
  (`actionButtonsAlign`, `actionButtonEdgePadding(bottom)`) để tránh lặp
  if/else 4 lần trên 4 nút; không tạo abstraction lớn hơn mức cần.
  `SmartBombButton`/`SecondaryWeaponButton`/`AbilityButton`/`ParryButton` đổi
  từ hardcode `Alignment.BottomEnd` + `padding(end=8.dp,...)` sang dùng 2
  helper trên. `ButtonSettings` (pause, `TopEnd`) giữ nguyên không đổi.
- `DialogSettings.kt`: thêm `ControlGroup` "Tay thuận" trong `SectionPanel`
  "Chơi", ngay sau block "Tầm nhìn" — 3 `Pill` (Hai tay/Tay trái/Tay phải),
  màu `palette.cyan`, đúng khuôn `LabelledPillRow` như các picker khác.

## Slice 3 — i18n + test (DONE)
- Không cần string mới trong `strings.xml` — theo đúng lệ đã có của repo,
  `displayName` các enum settings (Difficulty/ShipSkin/CameraZoom/...) đều
  hardcode tiếng Việt trực tiếp trong enum, không qua `R.string`.
- Test: thêm `settings_controlHandMode_roundtrips` vào
  `PersistenceRoundtripTest.kt` (Tier 3, DataStore thật trên thiết bị) — set
  `LEFT_HANDED` → assert đọc lại đúng → set lại `TWO_HANDED` → assert đúng.

## Slice 4 — eyeball device (DONE)
- Device: Pixel 7 Pro (`2B051FDH3006MU`) — máy duy nhất kết nối, dùng trực
  tiếp theo ngoại lệ 1-thiết-bị của R3.
- `./gradlew :app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.tranphuloi.neon.integration.PersistenceRoundtripTest,com.tranphuloi.neon.integration.GameLoopMultiTickTest`
  — **13/13 test pass** (8 PersistenceRoundtripTest gồm test mới + 5
  GameLoopMultiTickTest).
- `mcp__android__get_uilayout` và `adb shell uiautomator dump` đều lỗi
  (`null root node returned by UiTestAutomationBridge`) ngoài context
  instrumentation test → fallback sang tap theo tọa độ scale từ screenshot
  (như Task 21), kèm check `dumpsys window | grep mCurrentFocus` sau mỗi tap.
- Xác nhận trực quan cả 2 layout:
  - Chọn "Tay trái" trong Settings → vào game: 4 nút hành động
    (SecondaryWeapon/Ability/Parry/SmartBomb) chuyển đúng sang góc dưới-trái,
    không đè lên `IndicatorStatus` (HP/score/chapter) ở góc trên-trái; nút
    pause vẫn ở trên-phải như cũ.
  - Reset lại "Hai tay" trong Settings → vào game: 4 nút hành động trở về
    đúng góc dưới-phải như layout gốc trước Task 22 (`RIGHT_HANDED` dùng
    chung nhánh này nên không cần verify riêng).
  - Trong lúc thao tác, thiết bị có vài lần tự văng sang app khác không liên
    quan (launcher, 1 app traffic-lookup cài sẵn máy) sau một số tap thủ
    công — đúng hiện tượng bất thường đã ghi nhận từ Task 21, không phải do
    code thay đổi (relaunch lại `MainActivity` là quay về đúng trạng thái,
    checkpoint game vẫn còn nguyên). Không có ad nào che UI (R4 — không áp
    dụng, dev flavor tắt ad).
  - Đã reset `ControlHandMode` về `TWO_HANDED` (mặc định) trên thiết bị của
    user sau khi verify xong, không để lại state đã đổi.

## Trạng thái
✅ **DONE** (2026-07-11) — build + unit test pass, on-device integration test
13/13 pass (Pixel 7 Pro), verify trực quan cả 2 layout (Tay trái / Hai tay),
đã reset thiết bị về setting mặc định sau khi test.
