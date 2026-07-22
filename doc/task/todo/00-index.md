# Task Backlog — ✅ ĐÓNG SỔ (Picked 2026-07-03, hoàn tất 2026-07-04)

**Toàn bộ 8 task (đợt 1 + đợt 2) đã xong, verify device, trên nhánh `dev`, 916 test pass.**
File chi tiết đã chuyển sang [`doc/task/done/`](../done/) theo quy ước.

4 feature user chốt làm (kết hợp cả 4). Rã chi tiết trong từng file. Thứ tự đề xuất
theo rủi ro tăng dần / giá trị:

| # | Task | Loại | Scope | Rủi ro | Trạng thái |
|---|---|---|---|---|---|
| 01 | [Drone companion](../done/01-drone-companion.md) | Feature mới (entity) | Vừa | Đụng GameState/GameWorld | ✅ Done (7/7 slice, 2026-07-03) |
| 02 | [Lightning chain laser](../done/02-lightning-chain-laser.md) | Enhance đạn | Vừa | Nhiều `when` exhaustive + perf chain | ✅ Done (3/3 slice, 2026-07-04) |
| 04 | [Boss dialogue / narrative](../done/04-boss-dialogue-narrative.md) | Lore/content | Nhỏ | Thấp (che UI + i18n) | ✅ Done (5/5 slice, 2026-07-04) |
| 03 | [Unlockable ships + XP](../done/03-unlockable-ships-xp.md) | Progression | Lớn | Cân bằng + persistence | ✅ Done (5/5 slice, 2026-07-04) |

**Đợt 1 (01–04) ✅ HOÀN TẤT 2026-07-04** — cả 4 verify device S24 Ultra, JVM ~808→869 (100% pass).

## Backlog đợt 2 (Picked 2026-07-04, chưa bắt đầu)
Bám các hệ vừa xây (drone/lightning/ship-XP/dialogue):

| # | Task | Loại | Rủi ro | Trạng thái |
|---|---|---|---|---|
| 05 | [Kỹ năng chủ động theo tàu](../done/05-ship-active-abilities.md) | Gameplay depth | Vừa (cân bằng 22 skill + GameState) | ✅ Done (Slice 1–3, 2026-07-04, verify device) |
| 06 | [Biến thể drone (skill-tree)](../done/06-drone-variants.md) | Enhance Task 01 | Vừa (cân bằng heal/shield) | ✅ Done (Slice 1–3, 2026-07-04, verify device A50s) |
| 07 | [Mở rộng thành tựu](../done/07-achievements-expansion.md) | Content | Thấp (tái dùng repo) | ✅ Done (2026-07-04, verify device) |
| 08 | [Thử thách hằng ngày + modifier](../done/08-daily-challenge-modifiers.md) | Replay/economy | Vừa (RNG seed xác định) | ✅ Done (2026-07-04, verify device A50s) |

**Đề xuất bắt đầu đợt 2:** 07 (an toàn nhất, gắn kết 4 feature) → 05 (synergy cao Task 03) → 06 → 08.

## Backlog đợt 3 (Picked 2026-07-04, kết hợp 09+10+11)
Từ backlog còn treo thật sau khi đóng đợt 1+2:

| # | Task | Loại | Rủi ro | Trạng thái |
|---|---|---|---|---|
| 09 | [Enemy variants (5 địch)](../done/09-enemy-variants.md) | Content | Vừa (attack riêng/địch) | ✅ Done (eyeball 2/5 device) |
| 10 | [Mineral sink: Prestige Reset](../done/10-mineral-sink-prestige.md) | Economy/progression | Vừa (persistence + cân bằng) | ✅ Done (eyeball device) |
| 11 | [Baseline Profile](../done/11-baseline-profile.md) | Infra/perf | Cao (module mới + config-cache + device) | ✅ Done — **GENERATED** (baselineprofile 1.5.0-alpha07 hỗ trợ AGP 9.1.1; 25k rule, generate trên S24 Ultra) |

**Thứ tự:** 09 (an toàn, data-driven) → 10 (progression) → 11 (infra nặng, cần device, có fallback/defer nếu build khó).

## Backlog đợt 4 (Picked 2026-07-11, audit source code → AskUserQuestion 3 nhóm)
Enhance / Optimize / New feature, mỗi nhóm user chọn qua AskUserQuestion:

| # | Task | Nhóm | Rủi ro | Trạng thái |
|---|---|---|---|---|
| 17 | [Per-ship loadout persistence](../done/17-per-ship-loadout-persistence.md) | Enhance (8c) | Vừa (audit call site global setting) | ✅ Done (4/4 slice, 2026-07-11, verify device Pixel 7 Pro) |
| 18 | [Status chains + curses](18-status-chains-curses.md) | Enhance (Wave 4 combat) | Cao (cân bằng buff/curse) | 📋 TODO |
| 19 | [Color blind full migration](../done/19-colorblind-full-migration.md) | Enhance (accessibility) | Thấp (nhiều file nhỏ lẻ) | ✅ Done (4/4 slice, 2026-07-12, verify device Pixel 7 Pro) |
| 20 | [Microbenchmark module](../done/20-microbenchmark-module.md) | Perf (CCc) | Cao (module Gradle mới + device) | ✅ Done (macro+micro baseline đo trên S24 Ultra, 2026-07-21) |
| 21 | [Quick restart hotkey](../done/21-quick-restart-hotkey.md) | New (QoL) | Thấp (tái dùng logic có sẵn) | ✅ Done (4/4 slice, 2026-07-11, verify device Pixel 7 Pro) |
| 22 | [One-handed mode](../done/22-one-handed-mode.md) | New (QoL) | Thấp (layout chồng lấn) | ✅ Done (4/4 slice, 2026-07-11, verify device Pixel 7 Pro) |

**Đề xuất thứ tự bắt đầu:** 21 (✅ xong) → 22 (✅ xong) → 17 (✅ xong) → 19 (✅ xong)
→ 20 (hạ tầng perf, không đụng gameplay) → 18 (rủi ro cân bằng cao nhất, để cuối
khi đã quen nhịp).

## Backlog đợt 5 (Picked 2026-07-18, audit source code → AskUserQuestion 4 nhóm)
Enhance / New / Exclusive (tính năng độc quyền) / Performance, mỗi nhóm user chọn cả 4 candidate qua AskUserQuestion (scope lớn nhất từ trước tới nay — 16 task):

| # | Task | Nhóm | Rủi ro | Trạng thái |
|---|---|---|---|---|
| 23 | [Wire bossesOnly modifier](../done/23-wire-bossesonly-modifier.md) | Enhance | Thấp (field cô lập, không đụng entity khác) | ✅ Done (2026-07-19, verify device Pixel 7 Pro — 953 test pass) |
| 24 | [Mở rộng skill-tree](../done/24-expand-skill-tree.md) | Enhance | Vừa (cân bằng node mới + persistence) | ✅ Done (2026-07-19, verify device Samsung SM-S928B — 6 node mới, mua+unlock đúng thứ tự, chạy Boss Rush nhiều màn không crash) |
| 25 | [Booster synergy](../done/25-booster-synergy.md) | Enhance | Vừa (combo 26 booster, dễ OP) | ✅ Done (2026-07-20, verify device Samsung SM-S928B — SHIELD_WEAPON xác nhận log đầy đủ, MAGNET_SUPERCHARGE xác nhận cơ chế nền, VAMPIRE_DMG/banner UI suy luận qua pattern-reuse) |
| 26 | [Achievement round 2](../done/26-achievement-round-2.md) | Enhance | Thấp (tái dùng repo có sẵn) | ✅ Done (2026-07-19, verify device Pixel 7 Pro — 7 achievement mới, 957 test pass) |
| 27 | [Enemy type mới](../done/27-new-enemy-type.md) | New | Vừa (attack pattern riêng + spawn table) | ✅ Done (2026-07-19, verify device Pixel 7 Pro — Amip Vũ Trụ mitosis-on-death, 956 test pass, đã playtest trực tiếp qua Luyện tập Ch1 (swap tạm toàn bộ slot địch → enemy_amoeba, revert sau test): xác nhận trực quan tách 2 con ~40% size, không tự tách tiếp, không crash) |
| 28 | [Endless/Survival mode](../done/28-endless-survival-mode.md) | New | Cao (scaling độ khó vô hạn + leaderboard riêng) | ✅ Done (phát hiện lại 2026-07-20 — đã code+test+tune balance device từ trước qua "Wave" cũ, chỉ đóng sổ doc) |
| 29 | [Weekly/seasonal event](../done/29-weekly-seasonal-event.md) | New | Vừa (lịch UTC + content rotation) | ✅ Done (2026-07-19, verify device Samsung SM-S928B) |
| 30 | [Boss Rush mode](../done/30-boss-rush-mode.md) | New | Vừa (tái dùng boss AI, cần chuỗi transition mới) | ✅ Done (2026-07-19, verify device Samsung SM-S928B — 4/4 check Slice 4: locked-state, chapter-1 direct-launch, heal-cap, không lọt vào modifier picker) |
| 31 | [Overdrive bullet-time](../done/31-overdrive-bullet-time.md) | Exclusive | Cao (đụng timing toàn bộ game loop) | ✅ Done (2026-07-20, verify device TECNO BG6 — meter tích độc lập, trigger đúng ngưỡng 20, vignette cyan, tick-skip 40% qua idiom frameCount có sẵn, reset/refill đúng) |
| 32 | [Nhạc nền reactive combat](../done/32-reactive-combat-music.md) | Exclusive | Vừa (đụng AudioPlayer/ExoPlayer, cần asset nhạc lớp) | ✅ Done (phát hiện lại 2026-07-20 — volume-based intensity đã code+verify từ Round 19/8c, chỉ đóng sổ doc) |
| 33 | [Ship fusion](33-ship-fusion.md) | Exclusive | Cao (cân bằng + UI chọn tổ hợp mới) | 📋 TODO |
| 34 | [Procedural run-modifier draft] (34-procedural-run-modifier-draft.md) | Exclusive | Cao (RNG cân bằng, dễ tạo modifier phá game) | 📋 TODO |
| 35 | [Audit Compose recomposition](../done/35-audit-compose-recomposition.md) | Performance | Thấp (chỉ thêm annotation/audit, không đổi logic) | ✅ Done (2026-07-19, verify device Pixel 7 Pro) |
| 36 | [Giảm allocation game loop](../done/36-reduce-gameloop-allocation.md) | Performance | Vừa (đụng hot path nhiều controller) | ✅ Done (2026-07-19, verify device Samsung SM-S928B — Java/Dalvik Heap Pss giảm 42%/60% dù run "sau" chơi lâu/xa hơn, TOTAL PSS +1.5%) |
| 37 | [Refresh baseline profile](../done/37-refresh-baseline-profile.md) | Performance | Thấp (đã có module, chỉ regenerate) | ✅ Done (2026-07-18, verify device Pixel 7 Pro) |
| 38 | [Canvas batching audit](../done/38-canvas-batching-audit.md) | Performance | Thấp (audit + tinh chỉnh Canvas hiện có) | ✅ Done (2026-07-19, verify device Pixel 7 Pro — tìm+sửa 3 bug double-release PathPool) |

**Đề xuất thứ tự bắt đầu:** 37 (✅ xong) → 35 (✅ xong) → 38 (✅ xong) → 26 (✅ xong)
→ 23 (✅ xong) → 27 (✅ xong) → 30, 29 (new, tái dùng hệ có sẵn)
→ 24, 25 (enhance cân bằng vừa) → 28 (new rủi ro cao)
→ 36 (perf đụng hot path) → 32, 33, 31, 34 (exclusive, rủi ro cao nhất, để cuối).

## Ngoài backlog (phát hiện qua playtest/audit, không thuộc 16 task đợt 5)

| # | Task | Loại | Trạng thái |
|---|---|---|---|
| 39 | [Investigate AIBinder_linkToDeath warning](../done/39-investigate-aibinder-linktodeath-warning.md) | Bugfix/tech-debt | ✅ Done (2026-07-19) — root cause: Codec2 HAL platform bug, không phải code app |
| 40 | [Fix boss model serialization crash](../done/40-boss-serialization-crash-fix.md) | Bugfix | ✅ Done (2026-07-19) — phát hiện qua playtest Task 27, fix `@Transient` trên `getShip` lambda field ở LevelOneBoss/FinalBoss/MidBoss, verify device |

## Quy ước
- Mỗi task có **Slice 0 = chốt số liệu/thiết kế** trước khi code.
- Mỗi feature user-facing: string **cả vi + en**; state class `@Immutable/@Stable`;
  thêm entity thì theo checklist 5-mảnh + tinker (KHÔNG coroutine loop mới).
- Verify sau mỗi slice: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.
- Xong task → chuyển file sang `doc/task/done/`.
