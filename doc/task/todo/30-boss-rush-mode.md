# Task 30 — Boss Rush mode (new)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng mới"). Chế độ
chơi mới: đấu liên tiếp tất cả boss đã gặp trong game, không có stage
thường/space object xen giữa. Tái dùng boss AI hiện có (`StageBoss` +
`BossIntroOverlay`/`BossHpBar`/`BossRankOverlay`), cần chuỗi transition mới
giữa các boss.

## Slice 0 — chốt thiết kế (TODO)
- Thứ tự boss (theo thứ tự stage gốc hay random mỗi run — đề xuất theo thứ
  tự gốc, dễ test + không phá spoiler độ khó).
- Giữa 2 boss: có heal/hồi phục 1 phần HP không, hay giữ nguyên HP/trạng thái
  từ boss trước sang boss sau (permadeath trong run)?
- Entry point: menu riêng hay unlock sau khi clear campaign lần đầu?

## Slice 1 — chuỗi transition boss-to-boss (TODO)
- Controller mới hoặc mở rộng `StageController`: sau khi hạ 1 boss, load boss
  tiếp theo ngay (tái dùng `BossIntroOverlay`), không qua `StageGame`/`StageBreak`.
- Test: thứ tự boss đúng, transition không bị treo khi hạ boss cuối.

## Slice 2 — HUD/banner riêng (TODO)
- Hiển thị "Boss N/Total" (tái dùng `StageBanner` pattern), i18n vi+en.

## Slice 3 — leaderboard/reward riêng (TODO)
- Bucket riêng cho Boss Rush (thời gian hoàn thành hoặc số boss hạ được).
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 4 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro vừa (tái dùng boss AI có sẵn giảm rủi ro,
nhưng chuỗi transition mới giữa các boss cần test kỹ tránh treo/crash).
