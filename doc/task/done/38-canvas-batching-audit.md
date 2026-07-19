# Task 38 — Canvas batching audit (performance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tối ưu hiệu suất"). Các
entity mật độ cao (lasers, enemies, spaceObjects, boosters, minerals) đã có
Canvas batched-drawing riêng (`LaserCanvas`, `EnemyCanvas`,
`SpaceObjectCanvas`, `BoosterCanvas`, `MineralCanvas`) theo đúng convention —
task này audit lại xem có entity/draw call nào lọt lưới quy ước (vẽ per-item
qua `Image`/Composable `forEach` thay vì batched Canvas) sau các đợt feature
gần đây, và tinh chỉnh hiệu năng vẽ trong các Canvas hiện có.

## Slice 0 — chốt thiết kế (DONE)
- Phạm vi audit: toàn bộ `ui/game/world/` + `ui/game/{combo,damage,pickup,
  spark}/`.
- Công cụ đo: review code theo convention (không có device profiler gắn sẵn
  trong phiên này) — đối chiếu từng draw call với ngưỡng đề xuất "list nào
  thường xuyên > 10-15 item cùng lúc".

## Slice 1 — audit theo convention (DONE)
- **Đã batched đúng convention** (1 `Canvas` bọc `forEach`): `LaserCanvas`,
  `EnemyCanvas`, `SpaceObjectCanvas`, `BoosterCanvas`, `MineralCanvas`,
  `ImpactSparkOverlay`, `PickupBurstOverlay`, `TrailLineOverlay`,
  `MagnetVisual`, `ShipEngineFlame`. Không cần sửa.
- **3 candidate per-item (`Text`/`Box` trong `forEach`, KHÔNG batched
  Canvas)** — audit riêng từng cái, quyết định giữ nguyên (không mechanical
  áp quy ước, theo đúng kỷ luật đã lập ở Task 35):
  - `DamageNumbersOverlay` — `DamageNumberController` đã **cap cứng
    `MAX_ACTIVE = 8`** (constant có sẵn, comment ghi rõ "Keeps recomposition
    cost bounded during GODLIKE-tier combos that previously left 14-22
    popups alive"). 8 < ngưỡng 10-15 → không cần batch.
  - `PickupPopupOverlay` — không có cap cứng, nhưng chỉ 2 call site
    (`spawnMineralPickup`, `spawnBulletTypeActivation` trong
    `GameState.kt`), mỗi lần 1 hành động rời rạc của người chơi (nhặt
    khoáng sản / kích hoạt loại đạn), lifetime 500ms → cardinality thực tế
    luôn thấp hơn nhiều ngưỡng 10-15 (không giống DamageNumber vốn có thể
    dồn dập theo tốc độ bắn).
  - `EnemyHpNumber` — render qua `enemies.forEach` trong `GameWorld` lọc
    theo điều kiện "đã bị đánh, còn sống, không phải tier-1 (<50hp)".
    `MAX_REGULAR_ENEMIES = 30` là trần cho TOÀN BỘ enemy trên màn (không
    phải riêng số bị dính "HP number"); tier-1 (đa số mob thường) bị loại
    hoàn toàn, và cửa sổ hiển thị chỉ từ lúc trúng đòn đầu tới lúc chết. Vẽ
    chỉ 1 `Text` (không path/stroke phức tạp như body enemy), rẻ hơn nhiều
    so với các shape trong `EnemyCanvas`. Không có công cụ đo runtime trong
    phiên này để chứng minh đây là bottleneck thật, và convert sang Canvas
    (`drawText`/`TextMeasurer`) sẽ phải làm lại hiệu ứng `neonGlow` bằng
    tay → rủi ro regression thật cho lợi ích chưa đo được. **Quyết định:
    giữ nguyên**, không mechanical áp quy ước (cùng kỷ luật với quyết định
    `BackgroundState` ở Task 35).

## Slice 2 — tinh chỉnh Canvas hiện có (DONE — tìm thấy bug thật, không phải chỉ optimization)
Audit từng `PathPool.acquire()/release()` call site trong 5 Canvas file phát
hiện:
- **3 bug double-release** (release cùng 1 `Path` instance 2 lần liên tiếp —
  làm ArrayDeque free-list của `PathPool` chứa 2 reference tới cùng object;
  2 lần `acquire()` không liên quan sau đó có thể alias cùng Path, và vì
  `acquire()` gọi `.reset()`, bên acquire sau sẽ xoá geometry ngay giữa lúc
  bên acquire trước đang dùng — lỗi vẽ hình tiềm ẩn (shape biến mất/nhấp
  nháy) tuỳ tình huống tranh chấp pool):
  - `LaserCanvas.kt` `drawZigzagBody()` — `PathPool.release(path)` lặp 2 lần.
  - `EnemyCanvas.kt` `drawSpike()` (BERSERKER spike shape) — release lặp 2
    lần, dòng thứ 2 thụt lề sai (dấu vết copy-paste).
  - `EnemyCanvas.kt` (3-lobe shape ~dòng 1924) — `baseTri` release lặp 2 lần.
  - Sửa: xoá dòng release trùng ở cả 3 chỗ.
- **1 chỗ chưa dùng `PathPool`**: `LaserCanvas.kt` `drawHeartBody()` (Like/Tim
  shape) vẫn allocate `Path()` thô mỗi frame, không theo quy ước
  `PathPool.acquire()/release()` mà toàn bộ file còn lại đã áp dụng (từ R80
  perf initiative, xem comment trong `PathPool.kt`). Sửa: đổi sang
  `PathPool.acquire()` + thêm `PathPool.release(tri)`.
- Các Canvas còn lại (`SpaceObjectCanvas`, `BoosterCanvas`, `MineralCanvas`,
  `ExplosionCanvas`) — không phát hiện double-release hay Path thô nào.

## Slice 3 — verify + eyeball (DONE)
- `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin
  testDevDebugUnitTest` → BUILD SUCCESSFUL, 943/943 test pass (không đổi so
  với Task 35 — không thêm/hỏng test nào, đúng vì đây chỉ là bugfix
  Path-lifecycle, không đổi API/logic ngoài).
- Device: Pixel 7 Pro (`2B051FDH3006MU`), máy duy nhất kết nối (ngoại lệ
  R3). Đã cài sẵn devDebug từ Task 35 nên không gặp lại xung đột chữ ký.
- Chơi Ch.1 St.4 (đúng màn có nhiều enemy hình trái tim — chính là shape bị
  sửa `drawHeartBody`, và enemy BERSERKER dùng `drawSpike`): nhiều heart
  enemy + HP number hiển thị đúng, không nhấp nháy/biến mất hình, không
  crash. Logcat lọc `FATAL|AndroidRuntime|Exception|ANR` trong phiên chơi:
  không có dòng nào.

## Trạng thái
✅ **DONE** (2026-07-19) — Slice 1: 3 candidate per-item-render (DamageNumber
capped @8, PickupPopup naturally low, EnemyHpNumber — giữ nguyên có cân
nhắc, không mechanical batch). Slice 2: tìm và sửa **3 bug double-release
PathPool thật** (`LaserCanvas.drawZigzagBody`, `EnemyCanvas.drawSpike`,
`EnemyCanvas` 3-lobe shape) + 1 chỗ un-pooled `Path()` (`drawHeartBody`) —
giá trị lớn nhất của task này không phải batching mới mà là dọn nợ kỹ thuật
Path-lifecycle còn sót từ R80. Build + 943 unit test pass, eyeball device
Pixel 7 Pro đúng màn có shape bị sửa, không regression.
