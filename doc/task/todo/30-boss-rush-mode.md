# Task 30 — Boss Rush mode (new)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng mới"). Chế độ
chơi mới: đấu liên tiếp tất cả boss đã gặp trong game, không có stage
thường/space object xen giữa.

**Audit 2026-07-19:** phát hiện tính năng **đã tồn tại đầy đủ** từ nhiều
Wave trước (`GameMode.BOSS_RUSH`, `BossRushProvider`, wiring
`DialogModePicker`, achievement `BOSS_RUSH_CLEAR`/`BOSS_RUSH_S`, test
`BossRushRosterTest`) — doc này chỉ chưa được cập nhật. Việc còn lại thực
tế chỉ là Slice 0 (3 câu hỏi thiết kế), nhưng implementation có sẵn **lệch
cả 3 điểm** với quyết định vừa chốt qua AskUserQuestion. User xác nhận qua
AskUserQuestion: **"Sửa đủ cả 3 điểm"**.

## Slice 0 — chốt thiết kế (✅ done 2026-07-19)
- Thứ tự boss: **theo thứ tự chương gốc** (Chapter.kt 1→5), không random.
- Giữa 2 boss: **hồi 1 phần HP** (không full, không giữ nguyên).
- Entry point: **menu riêng**, unlock sau khi clear campaign lần đầu.

## Slice 1 — chuỗi transition boss-to-boss (✅ done, Wave trước — nay reconcile)
- `BossRushProvider` (`StageProviders.kt`) build sẵn script boss-to-boss với
  `StageMessage` đệm giữa (tái dùng `BossIntroOverlay` per-boss qua
  `StageBoss` bình thường, không cần controller riêng).
- **Task 30 (2026-07-19) — 3 fix reconcile:**
  1. `allBosses` build lại trực tiếp từ `Chapter.entries` (đúng thứ tự
     campaign 1→5) thay vì `MidBossType.ALL.forEachIndexed` + round-robin
     gán chapter tay + 6 dòng special-case final/biến-thể. `chapterId` đúng
     → `BossKindResolver` tự resolve đúng biến thể hình ảnh, hết special-case.
  2. `GameState.kt`: heal giữa 2 boss đổi từ `setHp(initialShipHp)` (full)
     sang `shipController.healCapped(initialShipHp * 0.4f, maxHp =
     initialShipHp)` (partial +40%), trigger đúng lúc `StageMessage` mang
     sentinel `BossRushProvider.BOSS_RUSH_GAP_MESSAGE`.
  3. `DialogModePicker.kt`: bỏ `GameMode.BOSS_RUSH` khỏi `pickable` (nhánh
     `when` theo mode vẫn giữ case BOSS_RUSH, không đụng — pattern nhất
     quán với SURVIVAL/DAILY/PRACTICE). `MenuScreen.kt` thêm nút riêng
     "Chiến Boss" (glyph ☠) trong row "Tiến trình", gated bởi
     `Achievement.FINAL_BOSS_KILL` (dimmed alpha khi chưa unlock, click
     no-op) — tái dùng convention locked-visual có sẵn ở
     `DialogChapterPicker.kt`. `MainActivity.kt` wire `onOpenBossRush`
     mirror pattern Practice-mode direct-launch (clear checkpoint → set
     mode → navigate, luôn bắt đầu lại từ đầu, không resume dở dang).
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin
  testDevDebugUnitTest` — 2 flavor sạch, toàn bộ unit test pass (gồm
  `BossRushRosterTest` không cần sửa assertion — roster mới vẫn cover đủ
  `MidBossType.ALL` + roster size vẫn ≥24 như test yêu cầu).

## Slice 2 — HUD/banner riêng (📋 TODO — gap có thật, xác nhận qua audit)
- "Boss N/Total" **chưa tồn tại**: grep `StageBanner.kt`/`BossIntroOverlay.kt`
  xác nhận không có tham chiếu `BOSS_RUSH` hay đếm thứ tự boss nào —
  `BossIntroOverlay` hiện chỉ hiện tên boss, không có số thứ tự/tổng số.
  Ngoài phạm vi "sửa đủ cả 3 điểm" đã chốt — để lại làm task riêng nếu
  user muốn.

## Slice 3 — leaderboard/reward riêng (📋 TODO — gap có thật, xác nhận qua audit)
- Boss Rush hiện **dùng chung** bucket leaderboard all-time/daily với mọi
  mode khác (`DialogGameOver.kt`: `leaderboard.submit(parsed)` +
  `submitDaily(...)`, không nhánh riêng theo `runMode == BOSS_RUSH`, khác
  với Endless đã có `submitEndless(...)` riêng). Không có bucket điểm/thời
  gian hoàn thành riêng cho Boss Rush. Ngoài phạm vi đã chốt — để lại làm
  task riêng nếu user muốn.

## Slice 4 — eyeball device (⏳ pending — cần Pixel 7 Pro theo R3)
- Cần xác nhận: nút "Chiến Boss" mờ khi chưa unlock `FINAL_BOSS_KILL`; sau
  unlock — boss đầu tiên đúng thứ tự chương 1 (không random); heal có giới
  hạn (không đầy 100%) qua log filter `BOSS_RUSH`; `DialogModePicker` không
  còn liệt kê Boss Rush.

## Trạng thái
🟡 **In progress** — Slice 0/1 xong (reconcile 3 điểm lệch), build+test xanh.
Slice 2/3 xác nhận là gap có thật nhưng ngoài phạm vi đã chốt (chưa làm).
Slice 4 (device) còn chờ verify.
