# Task 28 — Endless/Survival mode (new)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng mới"). Chế độ chơi
mới ngoài script `stages: List<Stage>` cố định — sống sót càng lâu càng tốt,
độ khó tăng dần vô hạn.

## Phát hiện 2026-07-20 — đã implement từ trước, doc bị lạc hậu

Khi bắt đầu Slice 0 (research kiến trúc), phát hiện **cả `ENDLESS` lẫn
`SURVIVAL` đã được code, test, và tune balance trên device thật** ở một
"Wave" cũ (Wave 5 / Wave 13 / Wave 13d — quy ước đặt tên khác với "đợt" hiện
tại, có trước khi `doc/task/todo/` tracker này tồn tại). Task doc này chỉ đơn
giản chưa được cập nhật để phản ánh thực tế. Không viết code mới — chỉ đóng
sổ đúng trạng thái.

Bằng chứng cụ thể (đã verify trực tiếp qua code, không chỉ qua ghi nhớ):
- `ui/game/mode/GameMode.kt` — enum có `ENDLESS("endless", "Vô tận")` và
  `SURVIVAL("survival", "Sinh tồn")`, kèm doc-comment mô tả rõ mỗi mode.
- `ui/game/stage/StageProviders.kt` — `EndlessProvider` (scaling exp theo
  cycle, đã retune ở Wave 13d sau khi log device Pixel 7 Pro cho thấy chết
  trong ~6 hit ở stage ~109): HP ×1.08^c cap 4.0, impact ×1.05^c cap 1.8
  (fix chính cho instant-death), speed ×1.03^c cap 1.6, spawn ×0.96^c floor
  0.55 (hoặc 300ms, tùy giá trị nào lớn hơn). `SurvivalProvider` scaling đơn
  giản hơn (+15%/cycle, chỉ HP).
- `ui/game/state/GameState.kt:305` — `EndlessProvider()` đã wired vào
  `StageController` qua cơ chế `StageProvider` pluggable sẵn có (đúng đúng
  pattern Boss Rush đã dùng — provider abstract, không cần sửa
  `StageController`).
- `data/LeaderboardRepository.kt` — bucket `ENDLESS_KEY` riêng,
  `submitEndless(seconds: Int)` + `endlessEntries: Flow<List<LeaderboardEntry>>`
  sort theo giây sống sót.
- `ui/dlg/gameover/DialogGameOver.kt` — gọi `leaderboard.submitEndless(sec)`
  và render `endlessEntries` khi kết thúc run ở mode Endless.
- `ui/dlg/modepicker/DialogModePicker.kt` — `GameMode.ENDLESS` hiện trong
  danh sách chọn mode (icon "◌", mô tả "Sống sót · tăng độ khó liên tục").
  `SURVIVAL` đã bị **gỡ khỏi picker** ở Wave 13 (#3) vì "gần trùng lặp với
  ENDLESS (cả 2 đều = chapter-1 loop vô hạn + escalation + score theo thời
  gian sống), tên bị lẫn nhau — ENDLESS là bản phong phú hơn (scaling exp +
  theme rotation)". Enum `SURVIVAL` vẫn còn tồn tại trong code (không xoá)
  nhưng không còn entry point UI.
- `app/src/test/java/.../stage/EndlessEscalationTest.kt` — 6 test: cycle 0
  baseline, cycle 5 escalation dưới cap, cycle 30 chạm đúng cap, cycle 100
  không "nổ" thêm (no late-game blowup), impact ramp mềm hơn curve cũ (guard
  chống regression), monotonic (cycle cao hơn không bao giờ yếu hơn).

## Trạng thái

✅ **Đã Done từ trước (không rõ ngày chính xác, trước 2026-07-18)** — phát
hiện lại và đóng sổ chính thức 2026-07-20. Cả `ENDLESS` (entry point chính,
scaling exp đã balance-tune qua device log) và `SURVIVAL` (giữ trong enum,
không còn UI picker) đều đã code + test + verify device. Không cần thêm
Slice nào — 4 câu hỏi Slice 0 gốc (công thức scaling / entry point /
leaderboard riêng / điều kiện kết thúc) đều đã có câu trả lời trong code:
- Scaling: exp theo cycle, cap rõ ràng (xem trên), tránh vỡ game ở stage cao.
- Entry point: `ModePicker` dialog, nút riêng cho ENDLESS.
- Leaderboard: bucket `ENDLESS_KEY` riêng, không lẫn all-time/daily.
- Kết thúc: không cap thời gian — chỉ khi ship chết (`submitEndless` chạy ở
  `GAME_OVER`), provider loop vô hạn không có "victory" tự nhiên.
