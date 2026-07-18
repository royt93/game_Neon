# Task 31 — Overdrive bullet-time (exclusive/signature)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng độc quyền/khác
biệt", option recommended). Tính năng signature: tích 1 thanh meter qua combat
(combo/kill), khi đầy kích hoạt "Overdrive" — chậm thời gian toàn màn hình
(bullet-time) trừ ship người chơi, trong X giây.

## Slice 0 — chốt thiết kế (TODO)
- Cơ chế tích meter: theo combo (`ComboController`) hay theo damage gây ra?
  Ngưỡng đầy meter (thời gian trung bình để đầy 1 lần/run).
- Hệ số chậm thời gian (đề xuất 0.3-0.5x tốc độ enemy/laser/spaceObject,
  giữ ship người chơi ở tốc độ bình thường) + thời lượng hiệu lực (X giây,
  đề xuất 3-5s).
- **Kỹ thuật chậm thời gian**: KHÔNG thêm coroutine loop mới — phải áp hệ số
  scale vào delta di chuyển/animation trong loop `tinker` hiện có (nhân
  velocity/displacement của enemy/laser theo hệ số timescale khi Overdrive
  active). Xác nhận đúng nơi áp dụng khi bắt đầu code (ứng viên: từng
  controller entity đọc 1 giá trị `timeScale` chung từ `GameState`).
- Có làm chậm nhạc nền/SFX theo (pitch-shift) hay giữ nguyên audio bình
  thường trong lúc Overdrive?

## Slice 1 — meter + trigger logic (TODO)
- Controller mới `OverdriveController`: tích meter, trigger khi đầy, đếm
  ngược thời lượng hiệu lực.
- Test: tích đúng theo combo/damage, trigger đúng ngưỡng, hết hiệu lực đúng
  thời gian.

## Slice 2 — áp `timeScale` vào entity controller (TODO)
- Mỗi controller entity (enemy/laser/spaceObject di chuyển) nhân displacement
  theo `timeScale` khi Overdrive active — audit toàn bộ nơi tính vị trí mỗi
  tick để không bỏ sót entity nào (bug hiển nhiên nếu 1 loại địch "quên" áp
  timeScale sẽ không chậm theo, phá hiệu ứng).
- Test: vị trí entity sau N tick với timeScale=0.4 đúng 40% so với bình
  thường.

## Slice 3 — UI/VFX + audio (TODO)
- Meter HUD (tái dùng pattern `ComboHud`), overlay màu/vignette khi active
  (tái dùng `Vignette`).
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. **Rủi ro cao nhất toàn đợt 5** — đụng timing của
toàn bộ entity controller trong game loop, dễ bỏ sót 1 loại entity không áp
timeScale đúng. Nên làm sau khi các task khác ổn định, để cuối theo đề xuất
thứ tự ở `00-index.md`.
