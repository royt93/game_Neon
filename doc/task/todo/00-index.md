# Task Backlog — todo (Picked 2026-07-03)

4 feature user chốt làm (kết hợp cả 4). Rã chi tiết trong từng file. Thứ tự đề xuất
theo rủi ro tăng dần / giá trị:

| # | Task | Loại | Scope | Rủi ro | Trạng thái |
|---|---|---|---|---|---|
| 01 | [Drone companion](01-drone-companion.md) | Feature mới (entity) | Vừa | Đụng GameState/GameWorld | ✅ Done (7/7 slice, 2026-07-03) |
| 02 | [Lightning chain laser](02-lightning-chain-laser.md) | Enhance đạn | Vừa | Nhiều `when` exhaustive + perf chain | ✅ Done (3/3 slice, 2026-07-04) |
| 04 | [Boss dialogue / narrative](04-boss-dialogue-narrative.md) | Lore/content | Nhỏ | Thấp (che UI + i18n) | ✅ Done (5/5 slice, 2026-07-04) |
| 03 | [Unlockable ships + XP](03-unlockable-ships-xp.md) | Progression | Lớn | Cân bằng + persistence | ✅ Done (5/5 slice, 2026-07-04) |

**Đợt 1 (01–04) ✅ HOÀN TẤT 2026-07-04** — cả 4 verify device S24 Ultra, JVM ~808→869 (100% pass).

## Backlog đợt 2 (Picked 2026-07-04, chưa bắt đầu)
Bám các hệ vừa xây (drone/lightning/ship-XP/dialogue):

| # | Task | Loại | Rủi ro | Trạng thái |
|---|---|---|---|---|
| 05 | [Kỹ năng chủ động theo tàu](05-ship-active-abilities.md) | Gameplay depth | Vừa (cân bằng 22 skill + GameState) | ✅ Done (Slice 1–3, 2026-07-04, verify device) |
| 06 | [Biến thể drone (skill-tree)](06-drone-variants.md) | Enhance Task 01 | Vừa (cân bằng heal/shield) | ✅ Done (Slice 1–3, 2026-07-04, verify device A50s) |
| 07 | [Mở rộng thành tựu](07-achievements-expansion.md) | Content | Thấp (tái dùng repo) | ✅ Done (2026-07-04, verify device) |
| 08 | [Thử thách hằng ngày + modifier](08-daily-challenge-modifiers.md) | Replay/economy | Vừa (RNG seed xác định) | 📋 todo |

**Đề xuất bắt đầu đợt 2:** 07 (an toàn nhất, gắn kết 4 feature) → 05 (synergy cao Task 03) → 06 → 08.

## Quy ước
- Mỗi task có **Slice 0 = chốt số liệu/thiết kế** trước khi code.
- Mỗi feature user-facing: string **cả vi + en**; state class `@Immutable/@Stable`;
  thêm entity thì theo checklist 5-mảnh + tinker (KHÔNG coroutine loop mới).
- Verify sau mỗi slice: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.
- Xong task → chuyển file sang `doc/task/done/`.
