# Task 28 — Endless/Survival mode (new)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng mới"). Chế độ chơi
mới ngoài script `stages: List<Stage>` cố định — sống sót càng lâu càng tốt,
độ khó tăng dần vô hạn.

## Slice 0 — chốt thiết kế (TODO)
- Công thức scaling độ khó theo thời gian (vd mỗi X giây tăng
  spawnRate/hpMul/damageMul thêm Y%, cap ở mức nào để không vô nghĩa/vỡ game
  ở phút thứ N).
- Entry point: menu riêng hay toggle trong màn chọn difficulty hiện có?
- Leaderboard riêng (thời gian sống sót) hay dùng chung bảng leaderboard đã
  có (all-time/daily)?
- Điều kiện kết thúc: chỉ khi chết, hay có cap thời gian tối đa (tránh
  session vô hạn thực sự)?

## Slice 1 — logic scaling độ khó (TODO)
- Controller mới hoặc mở rộng `StageController`: tính hệ số scaling theo
  elapsed time, feed vào `tinker` repeat time (theo đúng pattern stage hiện
  có, không thêm coroutine loop mới).
- Test: hệ số tăng đúng công thức theo mốc thời gian.

## Slice 2 — leaderboard riêng (TODO)
- Mở rộng `LeaderboardRepository` với bucket "endless" (tương tự daily key).
- Test: roundtrip persistence.

## Slice 3 — UI entry point + navigation (TODO)
- Route mới hoặc flag trong `Game` route hiện có, i18n vi+en.
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. **Rủi ro cao nhất trong nhóm New** (scaling độ
khó vô hạn dễ vỡ cân bằng nếu không cap rõ ở Slice 0; leaderboard riêng thêm
scope persistence).
