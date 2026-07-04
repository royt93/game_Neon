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

## Quy ước
- Mỗi task có **Slice 0 = chốt số liệu/thiết kế** trước khi code.
- Mỗi feature user-facing: string **cả vi + en**; state class `@Immutable/@Stable`;
  thêm entity thì theo checklist 5-mảnh + tinker (KHÔNG coroutine loop mới).
- Verify sau mỗi slice: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.
- Xong task → chuyển file sang `doc/task/done/`.
