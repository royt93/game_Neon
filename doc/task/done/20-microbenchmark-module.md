# Task 20 — Macrobenchmark/microbenchmark module (perf, CCc)

**Picked 2026-07-11** qua AskUserQuestion (nhóm "optimize hiệu suất"). Đóng nốt
`CCc` trong feature.md Wave 7 — "should run alongside enemy Canvas (round 50) so
the saving is quantifiable instead of subjective". 7 round perf-chain (44-49 +
57 + 59) đã xong nhưng **chưa có con số đo được**, chỉ có `HotPathPerfTest`
(iteration-budget JVM timing, không đo FPS/frame thật trên device).

## Slice 0 — chốt scope đo ✅ DONE

Chốt qua `AskUserQuestion` (2026-07-20):
1. **Scope: Macro + Micro** (cả hai), không chỉ macro.
2. **Kịch bản: Campaign boss stage** (tái dùng flow Menu→Play→độ khó NORMAL có
   sẵn, đo trong lúc gặp boss — nhiều enemy+laser cùng lúc, khớp report
   "subjective lag" gốc).
3. **CI: manual-only** — không wire vào `.github/workflows/android-ci.yml`.
   AndroidX Benchmark docs cảnh báo rõ không tin số đo macrobenchmark từ
   emulator; job `connected-test` hiện tại chạy trên KVM emulator trong CI,
   wire benchmark vào đó sẽ ra số nhiễu nhưng trình bày như baseline thật.
   Chạy thủ công trên máy thật (theo R3) mỗi khi cần baseline/regression
   check mới.

Baseline so sánh: trước/sau round 44-49 không còn tái tạo được (đã merge) →
benchmark này là baseline **từ giờ trở đi**, dùng để bắt regression tương lai,
không phải để chứng minh lại gain quá khứ.

## Slice 1 — tạo module `:microbenchmark` ✅ DONE
- `settings.gradle`: thêm `include ':microbenchmark'`.
- `microbenchmark/build.gradle` — copy cấu trúc `:baselineprofile`
  (`com.android.test`, `targetProjectPath = ":app"`, `flavorDimensions +=
  "type"` với `dev`/`production`), dùng
  `androidx.benchmark:benchmark-macro-junit4:1.5.0-alpha07`.
- `microbenchmark/src/main/AndroidManifest.xml` — copy từ `:baselineprofile`.

## Slice 2 — viết benchmark case ✅ DONE
- `microbenchmark/src/main/java/.../CombatPeakBenchmark.kt` — 1 `@Test`
  dùng `MacrobenchmarkRule`, `StartupMode.WARM`, `metrics =
  listOf(FrameTimingMetric())`, `iterations = 5`. Lái UI qua `UiDevice` giống
  `GameLoopMultiTickTest`/`FlowSupport.kt` (helper `findByTag` copy riêng vì
  macrobenchmark module không depend được vào `:app`'s androidTest source
  set), chờ tới boss stage rồi đo cửa sổ frame timing ~15s.
- `app/src/androidTest/java/com/tranphuloi/neon/perf/HotPathMicrobenchmark.kt`
  — mirror 1:1 danh sách test của `HotPathPerfTest.kt` (5 hàm hot-path):
  `pathPoolAcquireRelease`, `statusEffectControllerApply`,
  `lightningChainComputeChainTargets`, `enemyControllerProcessEnemies`,
  `explosionControllerProcessExplosions`. Mỗi test dùng
  `@get:Rule val benchmarkRule = BenchmarkRule()` +
  `benchmarkRule.measureRepeated { ... }`.

## Slice 3 — verify trên device thật ✅ DONE

**Gradle config, 2 lỗi phải fix trước khi chạy được**:
- Macrobenchmark (`:microbenchmark`, `com.android.test` module đo `:app` riêng
  biệt) cần `experimentalProperties["android.experimental.self-instrumenting"]
  = true` trong `android {}` — thiếu sẽ gặp lỗi `NOT-SELF-INSTRUMENTING`
  (manifest instrument nhầm tiến trình app đang đo thay vì chạy tách biệt).
- `:app` là `com.android.application`, plugin `androidx.benchmark` (tự tạo
  build type non-debuggable + wire instrumentation args) **chỉ support
  `com.android.library`** → không apply được. Fix bằng cách tự thêm
  `testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"]`
  vào `defaultConfig` của cả `app/build.gradle` và `microbenchmark/build.gradle`
  thay vì qua plugin:
  - `app/build.gradle`: `"DEBUGGABLE,EMULATOR,UNLOCKED,ACTIVITY-MISSING,NOT-AOT-COMPILED"`
    (ACTIVITY-MISSING vì microbenchmark in-process không launch activity —
    không đổi sang `AndroidBenchmarkRunner` vì runner đó dùng chung cho mọi
    androidTest của `:app`, đổi sẽ ảnh hưởng widget/integration test khác).
  - `microbenchmark/build.gradle`: `"DEBUGGABLE"`.

  ⚠️ **Giới hạn đã biết**: mọi số đo dưới đây đến từ build **debuggable=true,
  compilationMode="run-from-apk"** (không AOT-compiled) vì `:app` không có
  build type benchmark riêng. AndroidX Benchmark tự cảnh báo: debuggable
  "drastically reduces runtime performance" và số có thể không phản ánh đúng
  hiệu năng bản release thật. Baseline này dùng để bắt **regression tương
  đối** (so sánh lần đo sau với lần đo này), không dùng làm con số hiệu năng
  tuyệt đối của bản production.

**Thiết bị đo**: Samsung Galaxy S24 Ultra (`SM-S928B`, serial `R5CX613VZBR`,
Android 16/API 36, snapdragon 8 core @ 3.4GHz, 11.6GB RAM).

### Kết quả Macrobenchmark — `CombatPeakBenchmark.combatPeakFrameTiming` (5 iterations)

| Metric | P50 | P90 | P95 | P99 |
|---|---|---|---|---|
| `frameDurationCpuMs` | 9.72 | 12.10 | 12.61 | 14.73 |
| `frameOverrunMs` | 8.20 | 13.85 | 15.10 | 17.27 |

`frameCount` per run: min 17996 / max 20101 / median 19893 (CoV 4.5%).

### Kết quả Microbenchmark — `HotPathMicrobenchmark` (per-function `timeNs`, median)

| Function | median (ns) | min | max | allocationCount |
|---|---|---|---|---|
| `explosionControllerProcessExplosions` | 56.06 | 45.75 | 700.31 | 1.00 |
| `enemyControllerProcessEnemies` | 107.50 | 83.41 | 234.77 | 1.00 |
| `statusEffectControllerApply` | 283.58 | 251.49 | 340.70 | 2.00 |
| `pathPoolAcquireRelease` | 479.57 | 474.59 | 492.16 | 0.00 |
| `lightningChainComputeChainTargets` | 11488.53 | 10350.89 | 13641.20 | 11.00 |

`lightningChainComputeChainTargets` là hot path đắt nhất trong nhóm (~11.5µs/op)
— hợp lý vì nó tính toán chain-target search qua danh sách enemy, không phải
O(1) như các hàm còn lại. Không có regression nào để so sánh (baseline mới),
ghi lại làm mốc cho lần đo tiếp theo.

⚠️ **Giới hạn methodology (phát hiện qua audit sau khi "done")**:
`explosionControllerProcessExplosions` và `enemyControllerProcessEnemies` khởi
tạo entity ở trạng thái "đang chết" (explosion sắp kết thúc animation / enemy
`hp = 0f`), nhưng `measureRepeated` dùng lại **cùng một controller instance**
qua mọi iteration — không reset state. `ExplosionController.processExplosions()`
và `EnemyController.processEnemies()` prune entity đã destroyed/removed ngay
sau lần gọi đầu, nên workload co lại (8→0 explosion, 30→18 enemy) rồi các
iteration sau đo chi phí gần-zero của danh sách đã rỗng/nhỏ hơn. Đây là lý do
CoV của `explosionControllerProcessExplosions` cao bất thường (1.00 tính theo
allocationCount, nhưng min/max 45.75ns↔700.31ns chênh ~15×) — median không đại
diện "chi phí xử lý N entity còn sống" mà là hỗn hợp 1 lần đo thật + nhiều lần
đo no-op. Pattern này **kế thừa từ `HotPathPerfTest.kt`** gốc (JVM proxy,
`repeat(10_000) { controller.processExplosions() }` cùng lỗi), không phải bug
mới của task này — nhưng cần đọc 2 con số này với sự dè dặt, không dùng làm
ngưỡng regression tuyệt đối cho tới khi benchmark được viết lại có reset state
mỗi iteration.

## Slice 4 — quyết định CI ✅ DONE
**Manual-only** (xem Slice 0, quyết định #3).

⚠️ **Sự cố đã sửa (phát hiện qua audit)**: ban đầu tưởng "không đụng CI" là đủ
để tuân thủ manual-only, nhưng `.github/workflows/android-ci.yml`'s
`connected-test` job gọi `./gradlew connectedDevDebugAndroidTest` **không
qualify module** — trong multi-module Gradle, task không qualify chạy trên
MỌI subproject khai báo nó. Cả `:app` (chứa `HotPathMicrobenchmark` mới thêm
vào source set androidTest chung) lẫn `:microbenchmark` (macrobenchmark,
`com.android.test` module cũng tự khai báo `connectedDevDebugAndroidTest`)
đều bị CI quét và chạy tự động trên x86_64 emulator API 34 mỗi lần push —
vi phạm trực tiếp quyết định #3. Đã sửa bằng cách qualify script CI thành
`./gradlew :app:connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notPackage=com.tranphuloi.neon.perf`
— loại hẳn `:microbenchmark` khỏi lệnh (không còn task nào gọi nó) và loại
package `perf/` (chứa `HotPathMicrobenchmark`) khỏi phạm vi `:app`.

## Trạng thái
✅ **DONE** — cả macro và micro benchmark đã chạy thành công trên thiết bị
thật (S24 Ultra), số liệu đã ghi lại làm baseline forward-looking. Không wire
CI theo quyết định đã chốt (sau khi sửa sự cố sweep ở Slice 4).

⚠️ **Sự cố đã sửa (audit)**: `microbenchmark/` thiếu `.gitignore` riêng (khác
`baselineprofile/.gitignore` có sẵn) → `microbenchmark/build/` (507 file,
3.6GB) bị stage vào git index. Đã thêm `microbenchmark/.gitignore` (`/build`)
và `git reset -- microbenchmark/build/` để unstage.
