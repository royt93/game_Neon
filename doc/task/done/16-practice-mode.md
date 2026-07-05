# Task 16 — QoL Practice mode (theo chương, mở theo tiến độ campaign)

**Picked 2026-07-05.** Chọn granularity "Theo chương, mở theo tiến độ" (khuyến nghị)
qua AskUserQuestion: nút "Luyện tập" ở Menu → chọn 1 chương ĐÃ MỞ để chơi lại từ
đầu chương, checkpoint namespace riêng, KHÔNG ảnh hưởng tiến độ campaign.

## Slice 1 — model + helpers (DONE)
- `GameMode.PRACTICE("practice", "Luyện tập")`.
- `stage/Stage.kt`: `stageChapterOf(s)`, `chapterStartIndex(chapterId)` (indexOfFirst chương, coerceAtLeast 0),
  `maxUnlockedChapter(campaignCheckpointIndex)` (walk-back, luôn ≥1).
- Provider: `else -> StaticListProvider()` đã cover PRACTICE → không cần đổi provider.
- GameState đọc `initialStageIndex` từ `checkpointFor("practice")` (đã có sẵn cơ chế).
- Test: `PracticeStagesTest` (4).

## Slice 2 — UI (DONE)
- `ui/dlg/practicepicker/DialogChapterPicker.kt`: NeonBottomSheet cyan liệt kê `Chapter.entries` sort theo id;
  chương `id ≤ maxUnlockedChapter(campaign cp)` mở (tint chương, số + "Vào đầu chương để luyện"),
  còn lại khoá (🔒 xám, "Chưa mở — chơi chiến dịch để mở"). Campaign cp đọc qua
  `(LocalContext.applicationContext as App).runPersistence.checkpointFor(GameMode.CAMPAIGN.key)`.
- `Navigation.PracticePicker` route.
- MenuScreen: param `onOpenPractice` + nút "Luyện tập" (◎ NeonCyan, testTag `menu_practice`) — nhóm "Khác" thành 3 nút (Luyện tập | Bách khoa | Cài đặt).
- MainActivity: wire `onOpenPractice → navigate(PracticePicker.route)` + `dialog(PracticePicker.route)`:
  chọn chương → `saveCheckpoint("practice", chapterStartIndex(ch))` + `setLastMode("practice")` →
  `navigate(Game.route){ popUpTo(Menu.route); launchSingleTop }`. `chapterId ≤ 0` (dismiss) → popBackStack.

## Slice 3 — eyeball (DONE)
- Emulator Pixel 10 Pro XL (emulator-5554). Menu "Luyện tập" hiện đúng → picker render đẹp
  (chương 1 mở, 2–5 khoá) → tap chương 1 → Game vào "Ch.1 · St.1 — Vành Đai Tiểu Hành Tinh".
- Logcat xác nhận: `chose chapter 1` → `saveCheckpoint mode=practice stage=0` → `setLastMode=practice`
  → `rememberGameState runMode=PRACTICE` → `checkpoint for practice = 0` → stage advance lưu checkpoint
  namespace `practice` (campaign không đổi).

## Trạng thái
✅ **DONE (2026-07-05).** Compile 2 flavor + JVM test PASS. Eyeball emulator Pixel 10 Pro XL OK.
Pixel 7 Pro KHÔNG đụng (Gradle installDevDebug lỡ cài cả 2 máy — từ đó chuyển `adb -s emulator-5554`).
Uncommitted — user tự commit.

## ✅ TASK 16 DONE — Practice mode theo chương, gated theo campaign, checkpoint cô lập.
