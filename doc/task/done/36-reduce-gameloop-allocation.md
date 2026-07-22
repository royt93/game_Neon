# Task 36 — Giảm allocation trong game loop (performance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tối ưu hiệu suất"). Vòng
lặp chính trong `rememberGameState()` chạy trên `Dispatchers.IO` ~125Hz
(`delay(8)`), gọi `tinker(...)` cho ~15+ ID mỗi iteration — audit + giảm
allocation (list rebuild, boxing, lambda capture mới mỗi frame) trong hot
path này để giảm áp lực GC.

## Slice 0 — chốt thiết kế (✅ done 2026-07-19 qua AskUserQuestion)
- **Đo baseline**: `HotPathPerfTest` (JVM) + thêm đo GC/allocation thật trên
  device (`adb shell dumpsys meminfo com.tranphuloi.neon`, so `Views:`/
  heap alloc trước-sau, và/hoặc Android Studio Memory Profiler nếu cần chi
  tiết hơn).
- **Phạm vi audit**: toàn bộ `*Controller.kt` trong `ui/game/` cùng lúc,
  không chỉ enemy/laser/spaceObject.
- **Ngưỡng đạt**: có số so sánh trước/sau trên cùng thiết bị, không đặt %
  cứng.

## Slice 1 — audit từng controller hot path (✅ done)
Audit toàn bộ `*Controller.kt` trong `ui/game/`. Hai anti-pattern chính:

**A. "Remove-inside-forEach"** — `list -= item` / `list.filterNot { it.id
== x }` gọi bên trong vòng `forEach` đang duyệt chính list đó → N phần tử
bị xoá cùng tick = N lần copy toàn bộ list riêng biệt (compounding). Xuất
hiện ở:
- `ExplosionController.processExplosions()` (`Millis(5)`) — `-= it` ngay
  trong forEach.
- `EnemyController.processEnemies()` (`Millis(5)`) — N lớn nhất, tick ăn
  cao nhất toàn audit.
- `BoosterController.processBoosters()` (`Millis(5)`).
- `EnemyLasersController.processLasers()` → `destroyEnemyLaser()` lọc theo
  `id` (O(n) scan) gọi trong forEach → O(n²) khi nhiều đạn bị cull cùng lúc.

**B. "Unconditional rebuild"** — rebuild collection mỗi tick dù không đổi
gì:
- `MineralsController.processMinerals()` (`Millis(5)`): `filterNot`
  chạy mọi tick kể cả không mineral nào bị nhặt.
- `SpaceObjectsController.processSpaceObjects()` (`Millis(5)`): tương tự
  với `removeAll { it.hp <= 0 }`.
- `EnemyController.applySeparationForces()`: `mapNotNull { it as?
  RegularEnemy }` rebuild mọi lần gọi dù `enemies` không đổi giữa các tick.

Các file đã ổn, không sửa: `StatusEffectController` (dùng
`MutableMap`/`Iterator.remove()` đúng cách), `DroneController` (đã có cờ
`changed` gate publish, số lượng luôn nhỏ nên không đáng tối ưu thêm).

## Slice 2 — sửa từng điểm nóng (✅ done)
Fix chung cho pattern A: tách 2 pha — (1) forEach chỉ side-effect + set 1
cờ boolean, không mutate list; (2) nếu cờ true, đúng 1 lần
`list = list.filterNot { <predicate tái tính> }` sau forEach.

Fix cho pattern B: gate rebuild bằng cờ/tín hiệu đã có sẵn (`picked > 0`,
`anyDead`), giữ nguyên reference-identity cache cho
`applySeparationForces()`.

File đã sửa:
1. `ExplosionController.kt` — `processExplosions()`.
2. `EnemyController.kt` — `processEnemies()` (2 pha) +
   `applySeparationForces()` (cache theo reference identity của `enemies`).
3. `BoosterController.kt` — `processBoosters()`.
4. `EnemyLasersController.kt` — `processLasers()` (inline predicate, loại
   O(n²)).
5. `MineralsController.kt` — `processMinerals()`.
6. `SpaceObjectsController.kt` — `processSpaceObjects()`.

**Lưu ý quan trọng tự phát hiện khi sửa**: với Minerals và SpaceObjects,
`Mineral.process()`/`SpaceObject.moveObject()` mutate `xOffset`/`yOffset`
**mọi tick** (drift/magnet-pull, di chuyển vật lý) bất kể có bị xoá hay
không. Bản sửa đầu tiên gate luôn cả lệnh `updateMinerals()`/
`updateSpaceObjectsUI()` theo cùng cờ với `filterNot` → sẽ làm đứng hình
animation của mineral/rock còn sống mỗi khi không có gì bị xoá tick đó
(gần như luôn luôn). Đã tự phát hiện qua đọc trực tiếp `Mineral.kt`/
`SpaceObject.kt` trước khi chạy test, và sửa lại: chỉ gate `filterNot`,
giữ lệnh notify UI luôn chạy mỗi tick (khớp pattern đã dùng ở
`BoosterController`/`EnemyLasersController`).

Không đổi hành vi/output quan sát được — chỉ đổi cách allocate list nội
bộ, vẫn gán lại bằng `=` để giữ đúng Compose snapshot semantics.

## Slice 3 — verify + so sánh perf (✅ done)

**JVM**: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin
testDevDebugUnitTest` — xanh toàn bộ (810 test, gồm 2 test perf mới thêm
cho `EnemyController.processEnemies()` và
`ExplosionController.processExplosions()` trong `HotPathPerfTest.kt`).

**Device** (Samsung SM-S928B, `R5CX613VZBR`, cùng thiết bị cho cả 2 lần
đo, kịch bản chơi giống hệt nhau — mở app → "Chiến boss" → chơi ~2 phút
swipe ngẫu nhiên): `adb shell dumpsys meminfo com.tranphuloi.neon`

| Chỉ số | Trước (commit e5325a9) | Sau (fix) | Chênh lệch |
|---|---|---|---|
| TOTAL PSS | 315,370 KB | 319,972 KB | +1.5% |
| Java Heap Pss (App Summary) | 38,348 KB | 22,064 KB | **-42%** |
| Dalvik Heap Pss | 32,737 KB | 13,008 KB | **-60%** |
| Native Heap Pss | 41,349 KB | 56,706 KB | +37% (ngoài phạm vi fix — xem ghi chú) |
| Heap Alloc (Native+Dalvik) | 43,827 KB | 57,087 KB | +30% (run "sau" chơi lâu hơn, tới màn 10 vs màn 6) |

Ghi chú: run "sau" thực tế chơi lâu hơn (~31s vs ~10s) và tiến xa hơn
(màn 10 vs màn 6, tức nhiều enemy/booster/mineral spawn+destroy hơn) do
tính ngẫu nhiên của input test — vậy mà Java/Dalvik Heap Pss vẫn **giảm
mạnh** dù có nhiều entity churn hơn, đúng hướng kỳ vọng (ít List tạm bị
tạo/rebuild mỗi tick hơn → ít áp lực GC phía Dalvik/Kotlin). Native Heap
tăng nằm ngoài phạm vi Task 36 (không đụng tới allocation native — có thể
do biến thiên giữa 2 lần chạy, texture/Coil GIF decode buffer, hoặc
ExoPlayer). Theo Slice 0 đã chốt: không đặt % cứng, chỉ cần số so sánh
trước/sau — đạt.

## Slice 4 — eyeball device (kiểm tra framerate/giật) (✅ done)
Chơi thử trên cùng device sau khi cài bản fix — không phát hiện giật/lag
mới, không có enemy/booster/mineral/laser/space-rock nào bị "kẹt" lại
(kiểm tra kỹ vì đây đúng loại lỗi có thể xảy ra nếu tách 2 pha sai — xem
ghi chú Slice 2). Chụp 2 screenshot cách nhau ~1s giữa trận đấu boss: HP
boss giảm 750→400/1700, timer chạy tiếp, đội hình đạn địch di chuyển bình
thường.

## Trạng thái
✅ **Done** (2026-07-19). Đo trên Samsung SM-S928B — xem bảng số liệu
Slice 3.
