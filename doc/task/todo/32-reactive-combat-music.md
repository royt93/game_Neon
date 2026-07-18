# Task 32 — Nhạc nền reactive combat (exclusive/signature)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng độc quyền/khác
biệt"). `AudioPlayer.kt` hiện wrap Media3 ExoPlayer 1.10.0, phát 1 track nhạc
nền cố định theo `gameStatus`. Task này làm nhạc nền phản ứng theo cường độ
combat (combo cao, boss phase) bằng cách layer thêm track/stem.

## Slice 0 — chốt thiết kế (TODO)
- Kỹ thuật layering: nhiều `ExoPlayer` instance đồng bộ (base + intensity
  layer, crossfade volume) hay 1 track duy nhất chuyển đổi giữa các state
  (base → combat → boss)?
- Điều kiện chuyển cường độ: combo threshold nào, boss phase nào kích hoạt
  layer riêng?
- Asset nhạc cần: cần bao nhiêu track/stem mới (base loop + 1-2 layer combat/
  boss) — xác nhận nguồn asset trước khi code (không tự generate/tải asset
  nhạc, cần user cung cấp hoặc placeholder rõ ràng).

## Slice 1 — mở rộng AudioPlayer cho multi-layer (TODO)
- API mới trên `AudioPlayer`: `setIntensity(level)` hoặc tương tự, quản lý
  crossfade giữa các layer.
- Test: chuyển intensity đúng track/volume theo state.

## Slice 2 — trigger từ ComboController/BossController (TODO)
- Gọi `AudioPlayer.setIntensity(...)` tại đúng điểm combo tăng/giảm, boss
  phase transition.

## Slice 3 — verify + eyeball (TODO)
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.
- Eyeball device: nghe thử chuyển layer mượt, không giật/lag audio.

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro vừa (đụng AudioPlayer/ExoPlayer, phụ thuộc
asset nhạc mới chưa xác nhận nguồn — cần hỏi user asset trước khi code Slice 1).
