# Task 26 — Achievement round 2 (enhance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "enhance tính năng cũ").
Task 07 (đợt 2) đã mở rộng achievement round 1. Task này thêm round 2, tái
dùng nguyên `AchievementsRepository` (unlock-once-and-persist), không đổi
hạ tầng.

## Slice 0 — chốt thiết kế (TODO)
- Số achievement mới thêm (đề xuất 6-10, bám theo feature mới của đợt 3-5:
  ship XP/level, prestige, per-ship loadout, one-handed mode, v.v. — điều
  kiện chưa từng có achievement).
- Điều kiện unlock cụ thể mỗi achievement (vd "đạt ship level 5", "prestige
  reset lần đầu", "hạ boss bằng chế độ 1 tay").

## Slice 1 — thêm entry vào enum Achievement (TODO)
- Mở rộng enum hiện có, giữ giá trị cũ không đổi (tránh vỡ persistence).
- Test: mỗi achievement mới unlock đúng điều kiện, không unlock nhầm.

## Slice 2 — trigger unlock tại đúng call site (TODO)
- Gắn check unlock vào nơi điều kiện xảy ra thực tế (cuối run, sau prestige
  reset, v.v.), gọi qua `AchievementsRepository` có sẵn.

## Slice 3 — UI + i18n (TODO)
- Hiển thị achievement mới trong màn achievement hiện có, string vi+en.
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro thấp (tái dùng repo có sẵn, không đổi
hạ tầng, chỉ thêm entry + trigger).
