# Task 35 — Audit Compose recomposition (performance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tối ưu hiệu suất", option
recommended). Audit toàn bộ state data class (domain model + UI projection)
trong `ui/game/` xác nhận độ phủ `@Immutable`/`@Stable` — quy ước đã đặt ra
nhưng chưa từng audit toàn bộ có tuân thủ đầy đủ hay không.

## Slice 0 — chốt thiết kế (DONE)
- Phạm vi audit: toàn bộ `*UI` projection type + domain model (Booster,
  Laser, Enemy, Mineral, SpaceObject, Explosion, DamageNumber, PickupPopup,
  ImpactSpark, PickupBurst, ...) + `GameState`/`BackgroundState`.
- Công cụ đo: bật tạm `composeCompiler { reportsDestination = ...;
  metricsDestination = ... }` trong `app/build.gradle` (Kotlin 2.x compose
  plugin API) → sinh `app-classes.txt` breakdown stability theo field. Gỡ
  ngay sau khi audit xong (không commit vĩnh viễn).
- Ngưỡng chấp nhận: **chỉ sửa class nào compiler chỉ đích danh field cụ thể
  gây unstable VÀ việc sửa không đổi hành vi runtime** — không chase toàn bộ
  217 unstable class app-wide một cách máy móc.

## Slice 1 — chạy Compose compiler metrics (DONE)
- 217 unstable class app-wide (bao gồm cả test source set). Trong
  `ui/game/`: tất cả `*UI` projection type (LaserUI, SpaceObjectUI,
  BoosterUI, DroneUI, EnemyUI, MineralUI, DamageNumber, ImpactSpark,
  TrailLine, PickupBurst, PickupPopup) đã **stable** sẵn — không cần sửa.
- `GameState` unstable **chỉ** vì mọi field `List<T>` (shipLasers,
  enemies, minerals, explosions, mines, ...) — `List` interface luôn bị
  Compose compiler coi unstable bất kể type phần tử, đây là giới hạn biết
  trước của Compose, không phải lỗi thiếu annotation.
- 3 class element-level thực sự thiếu annotation, xác nhận qua field
  breakdown:
  - `Explosion` — unstable do `var removed: Boolean` (flag "đã hết đời").
  - `Mine` — unstable do `var detonated: Boolean` (flag tương tự).
  - `BackgroundState` — unstable do 3 field `List<BgStar>/List<DustParticle>/
    List<NebulaBlob>`.

## Slice 2 — sửa từng class thiếu annotation (DONE — 2/3, 1 cố ý KHÔNG sửa)
- **`Explosion`** (`ui/game/explosion/model/Explosion.kt`): thêm `@Stable`.
  An toàn — chỉ render qua `ExplosionCanvas(explosion: Explosion)` gọi
  per-item trong `GameWorld`'s forEach; mỗi item tự chạy animation qua
  `LaunchedEffect(explosion.startTimeMillis)` độc lập với skip/recompose
  của composable cha, nên đánh dấu stable chỉ giúp Compose skip re-draw
  item không đổi, không ảnh hưởng animation.
- **`Mine`** (`ui/game/ship/weapon/Mine.kt`): thêm `@Stable`. Tương tự —
  render qua `mines.forEach { key(m.id) { ... } }` trực tiếp trong
  `GameWorld`, không có side-effect nào phụ thuộc equality-based skip.
- **`BackgroundState` — CỐ Ý KHÔNG sửa** (phát hiện quan trọng, xem chi
  tiết bên dưới).

### Phát hiện quan trọng: vì sao KHÔNG đánh dấu `BackgroundState` stable/immutable
`BackgroundState` được tiêu thụ bởi `SpaceBackground(state: BackgroundState,
...)` — một `@Composable` bọc `AndroidView` quanh `SpaceBackgroundView` (custom
View thuần, không phải Canvas Compose — lý do: "3-5× faster rendering").
Cơ chế hoạt động:
- `SpaceBackgroundView.state` là property `var` thường (không phải
  `mutableStateOf`); **setter của nó tự gọi `invalidate()`** để yêu cầu vẽ
  lại frame kế tiếp.
- `onDraw()` của View mới thực sự **mutate in-place** `xOffset`/`yOffset`/
  `twinklePhase` của từng `BgStar`/`DustParticle`/`NebulaBlob` qua
  `.advance(dt)` — đây là animation loop thật, chạy hoàn toàn ngoài snapshot
  system của Compose.
- `BackgroundController.tick()` gọi `publish()` mỗi tick, tạo **instance
  `BackgroundState` mới** nhưng tái dùng **cùng list reference** cho
  `stars`/`dust`/`nebula` (chỉ list mới hoàn toàn khi respawn) — nghĩa là 2
  `BackgroundState` liên tiếp gần như luôn `equals()` nhau khi không có sao
  chổi đang bay (vì `List.equals()` short-circuit khi cùng reference).

Nếu đánh dấu `BackgroundState` `@Immutable`/`@Stable`, Compose compiler có
thể coi `SpaceBackground(...)` là **skippable dựa trên equality** — khi 2
state kế tiếp equals-bằng-nhau (trường hợp phổ biến nhất, không sao chổi),
Compose **bỏ qua gọi lại** composable đó, khiến `update` lambda của
`AndroidView` (chỗ gán `view.state = state`) không chạy → `invalidate()`
không được gọi → animation nền (`onDraw`) **dừng hẳn** cho tới khi có state
thực sự khác (VD sao chổi xuất hiện). Đây là bug tiềm ẩn nghiêm trọng nếu áp
dụng annotation một cách máy móc theo quy ước — trạng thái "unstable" hiện
tại của `BackgroundState` **vô tình đúng lại là cần thiết** để buộc Compose
luôn recompose lại `SpaceBackground` mỗi tick, giữ animation nền chạy liên
tục. **Kết luận: giữ nguyên, không thêm annotation.**

- Verify sau khi sửa: `compileDevDebugKotlin` xanh; re-run metrics xác nhận
  `Explosion`/`Mine` chuyển từ `unstable` → `stable class`; `BackgroundState`
  vẫn `unstable class` (giữ nguyên có chủ đích).

## Slice 3 — verify + so sánh metrics trước/sau (DONE)
- `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`
  → BUILD SUCCESSFUL, 943 unit test pass (không hỏng test nào).
- Metrics trước: `Explosion`/`Mine` unstable. Sau: cả 2 `stable class`,
  field breakdown không còn liệt kê field nào unstable.
- Gỡ block `composeCompiler { reportsDestination/metricsDestination }` khỏi
  `app/build.gradle` sau khi thu thập xong số liệu — đúng cam kết "tạm thời"
  ở Slice 0.

## Slice 4 — eyeball device (DONE)
- Device: Pixel 7 Pro (`2B051FDH3006MU`), máy duy nhất kết nối (ngoại lệ R3).
- Máy đang cài bản production release (từ Task 37) — hỏi user qua
  AskUserQuestion, được xác nhận uninstall + cài devDebug (chấp nhận mất
  save/progress cũ trên máy) để test riêng cho phiên này.
- Chơi thử Ch.1 St.4: timer/HP/spawn tiến triển bình thường, background
  starfield tiếp tục di chuyển mượt giữa các frame (xác nhận animation nền
  KHÔNG bị đứng — đúng như dự đoán khi giữ `BackgroundState` unstable).
  Logcat: không FATAL/AndroidRuntime/ANR nào xuất hiện.

## Trạng thái
✅ **DONE** (2026-07-19) — 2/3 class được thêm `@Stable` (`Explosion`,
`Mine`); `BackgroundState` cố ý giữ unstable (phát hiện kiến trúc quan
trọng: đánh dấu stable sẽ có nguy cơ làm đứng animation nền qua cơ chế
AndroidView interop). `GameState`'s List-driven instability không đáng để
chase (kiến trúc `refreshHandler` load-bearing đã ép recompose toàn bộ mỗi
tick ~125Hz bất kể stability của tham số). Build + 943 unit test pass, eyeball
device Pixel 7 Pro không phát hiện regression.
