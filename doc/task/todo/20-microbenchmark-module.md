# Task 20 — Macrobenchmark/microbenchmark module (perf, CCc)

**Picked 2026-07-11** qua AskUserQuestion (nhóm "optimize hiệu suất"). Đóng nốt
`CCc` trong feature.md Wave 7 — "should run alongside enemy Canvas (round 50) so
the saving is quantifiable instead of subjective". 7 round perf-chain (44-49 +
57 + 59) đã xong nhưng **chưa có con số đo được**, chỉ có `HotPathPerfTest`
(iteration-budget JVM timing, không đo FPS/frame thật trên device).

## Slice 0 — chốt scope đo (TODO)
- Đo gì: frame time / jank count trong lúc combat peak (nhiều enemy + laser +
  booster cùng lúc — kịch bản gây "subjective lag" theo report cũ).
- Baseline so sánh: trước/sau round 44-49 không còn tái tạo được (đã merge) →
  benchmark này là baseline **từ giờ trở đi**, dùng để bắt regression tương lai,
  không phải để chứng minh lại gain quá khứ.

## Slice 1 — tạo module `:microbenchmark` (TODO)
- Copy cấu trúc `:baselineprofile` (`com.android.test` + companion plugin,
  `targetProjectPath = ":app"`, `useConnectedDevices = true`) — dùng
  `androidx.benchmark:benchmark-macro-junit4` (đã có sẵn dependency trong
  `:baselineprofile/build.gradle`, cùng version).
- Thêm `androidx.benchmark:benchmark-junit4` (microbenchmark, khác macro) nếu cần
  đo micro-level (mapper/controller function), hoặc chỉ dùng macro nếu chỉ cần
  đo end-to-end frame timing khi chơi.

## Slice 2 — viết benchmark case (TODO)
- `FrameTimingMetric` trong lúc chạy 1 stage combat-nặng qua UiAutomator (tương tự
  cách `GameLoopMultiTickTest` lái game qua Menu → Play).
- Output: p50/p90/p99 frame time — so với ngưỡng jank chấp nhận được (vd 16ms/33ms).

## Slice 3 — verify trên device thật (TODO)
- Chạy `./gradlew :microbenchmark:connectedDevDebugAndroidTest` (hoặc tên task
  tương ứng plugin sinh ra) trên máy yếu đã có sẵn trong CLAUDE.md (S24 Ultra +
  A50s/A115F để so sánh máy mạnh/yếu).

## Slice 4 — quyết định có đưa vào CI không (TODO)
- Macrobenchmark cần device thật → khó chạy trên GitHub Actions runner chuẩn
  (không có emulator hardware accel mặc định). Có thể để manual-only trước, CI
  sau nếu cần.

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro cao nhất về mặt hạ tầng (module Gradle mới +
device thật), nhưng không đụng game logic hiện có nên an toàn cho gameplay.
