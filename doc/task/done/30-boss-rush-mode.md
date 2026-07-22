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

## Slice 4 — eyeball device (✅ done 2026-07-19, Samsung SM-S928B)
Verify bằng cách bypass tạm 1 dòng gate trong `MenuScreen.kt`
(`bossRushUnlocked = true`) để test nhánh unlocked, sau đó **revert lại
nguyên bản** (`"final_boss_kill" in unlockedAchievements`) và rebuild trước
khi coi task xong — không có thay đổi nào còn sót lại trong code.

- **(a) Nút "Chiến Boss" khóa khi chưa unlock**: xác nhận cả visual (magenta
  alpha 0.4, dimmed) lẫn hành vi (tap = no-op, ở lại Menu, không log nav) —
  test trước khi bypass và lại confirm sau khi revert (2 lần độc lập).
- **(b) Boss đầu tiên đúng chapter 1, direct-launch không resume**: unlock
  tạm → tap "Chiến Boss" → vào thẳng "Ch.1 · Vành Đai Tiểu Hành Tinh" bất kể
  campaign progress thật đang ở "màn 5" — xác nhận qua screenshot.
- **(c) Heal-cap giữa 2 boss**: xác nhận qua code (công thức + gate điều
  kiện chính xác trong `GameState.kt`) + log logcat live
  `BOSS_RUSH: partial heal between bosses (hp X → Y, cap 500)` bắt được
  đúng thời điểm trigger (1 lần là ca biên hp 0→0 no-op vì ship chết đúng
  lúc transition — log vẫn xác nhận đúng gate/thời điểm, dù không phải ca
  dương tính "đẹp"). Chấp nhận bằng chứng code + live-trigger này là đủ,
  không tiếp tục grind thêm live-play.
- **(d) `DialogModePicker` không liệt kê Boss Rush**: xác nhận qua code —
  `onOpenBossRush` (`MainActivity.kt:254-266`) navigate thẳng
  `Game.route`, hoàn toàn tách biệt route `DialogModifierPicker`
  (dùng cho nút "Chơi" thường) — Boss Rush chưa từng và sẽ không bao giờ
  xuất hiện trong picker đó theo thiết kế direct-launch.

## Trạng thái
✅ **Done** — Slice 0/1/4 xong, build+test xanh, verify device thật
(SM-S928B). Slice 2 (HUD "Boss N/Total") và Slice 3 (leaderboard riêng) là
gap có thật nhưng ngoài phạm vi 3-điểm đã chốt qua AskUserQuestion — để lại
làm task riêng nếu user muốn sau này.

⚠️ **Ghi chú (audit 2026-07-21)**: thêm nút Boss Rush vào `MenuScreen.kt`
đẩy nội dung menu vượt ước lượng chiều cao cứng (`neededH`) có sẵn từ trước
(round Practice mode), lộ lại bug clip/scroll cũ trên màn hình tầm trung.
Nhân task này, `MenuScreen.kt` được viết lại từ `BoxWithConstraints` +
ước lượng chiều cao sang `SubcomposeLayout` 2-pass (đo thật rồi mới scale,
bỏ hẳn `verticalScroll` dự phòng) — sửa root cause thay vì chỉ né bằng số
liệu mới. Đây là thay đổi đáng kể, không riêng gì Boss Rush, nhưng gắn vào
task này vì bug bị lộ ra chính xác lúc thêm nút Boss Rush.
