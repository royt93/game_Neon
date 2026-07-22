# Task 32 — Nhạc nền reactive combat (exclusive/signature)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng độc quyền/khác
biệt"). `AudioPlayer.kt` hiện wrap Media3 ExoPlayer 1.10.0, phát 1 track nhạc
nền cố định theo `gameStatus`. Task này làm nhạc nền phản ứng theo cường độ
combat (combo cao, boss phase).

## Phát hiện 2026-07-20 — đã implement từ trước, doc bị lạc hậu

Khi bắt đầu Slice 0, phát hiện **dynamic music intensity đã được code và
device-verify từ Round 19/8c** (trước khi `doc/task/todo/` tracker này tồn
tại). Không viết code mới — chỉ đóng sổ đúng trạng thái.

Bằng chứng cụ thể (`GameScreen.kt:482-527`):
- **Kỹ thuật đã chọn (khác cả 2 phương án Slice 0 gốc nêu — không phải
  multi-ExoPlayer layer, không phải track-switching):** modulate **volume**
  của track nhạc nền hiện có theo 3 tier cường độ combat, crossfade mượt qua
  `Animatable` (500ms tween):
  - `low` (×0.70): ít địch, không boss, HP > 50%
  - `mid` (×0.85): ≥4 địch trên màn HOẶC HP 30-50%
  - `high` (×1.00): boss đang active HOẶC HP < 30%
  - `effectiveMusicVolume = userMusicVolume × animatedIntensity` — tôn trọng
    volume setting của user, không ghi đè.
  - Restore về `musicVolumePref` khi `GameScreen` dispose (`DisposableEffect`)
    để splash/menu không bị kẹt ở volume giảm.
- **Vì sao né được vấn đề asset nêu ở Slice 0 gốc** ("cần bao nhiêu
  track/stem mới... không tự generate/tải asset nhạc"): giải pháp không cần
  stem/layer riêng — chỉ modulate volume trên **track nhạc nền hiện có**
  (`Song.BKG`/`BKG1`/`BKG2`). Không cần asset mới.
- **Pitch modulation đã thử và revert:** Round 62 thêm pitch shift
  (boss=1.05, low-HP=0.92) qua `AudioPlayerHolder.setPitch()`
  (`PlaybackParameters` + Sonic algorithm). Round 64 revert sau feedback
  runtime "nhạc nền có vẻ như bị overlay" — pitch shift làm track nhạc nhiều
  nhạc cụ nghe lệch tông (mỗi nhạc cụ bị Sonic algorithm dịch không đều).
  `setPitch()` vẫn giữ làm public API trên `AudioPlayerHolder` (không gọi ở
  đâu) phòng khi cần lại, nhưng **không dùng cho production hiện tại** — chỉ
  volume-based intensity (Round 19/8c) là cơ chế đang chạy.
- Đây cũng chính là lý do Task 31 (Overdrive) chọn SFX sting thay vì
  pitch-shift BGM khi thiết kế — tránh lặp lại đúng vấn đề Round 64 này.

## Trạng thái

✅ **Đã Done từ trước (Round 19/8c, trước 2026-07-18)** — phát hiện lại và
đóng sổ chính thức 2026-07-20. Không có unit test riêng cho logic intensity
(nằm inline trong `GameScreen` Composable, không phải trong controller có
thể test độc lập — cùng tiền lệ với combo/overdrive logic trong
`GameState.kt`). Verify qua Logger.v trace + runtime feedback đã ghi trong
comment Round 62/64 ở trên, không cần verify lại.
