# Task Backlog — todo (Picked 2026-07-03)

4 feature user chốt làm (kết hợp cả 4). Rã chi tiết trong từng file. Thứ tự đề xuất
theo rủi ro tăng dần / giá trị:

| # | Task | Loại | Scope | Rủi ro | Trạng thái |
|---|---|---|---|---|---|
| 01 | [Drone companion](01-drone-companion.md) | Feature mới (entity) | Vừa | Đụng GameState/GameWorld | ✅ Done (7/7 slice, 2026-07-03) |
| 02 | [Lightning chain laser](02-lightning-chain-laser.md) | Enhance đạn | Vừa | Nhiều `when` exhaustive + perf chain | 📋 todo |
| 04 | [Boss dialogue / narrative](04-boss-dialogue-narrative.md) | Lore/content | Nhỏ | Thấp (che UI + i18n) | 📋 todo |
| 03 | [Unlockable ships + XP](03-unlockable-ships-xp.md) | Progression | Lớn | Cân bằng + persistence | 📋 todo |

**Đề xuất bắt đầu:** 04 (rủi ro thấp, khởi động nhẹ) hoặc 01 (giá trị gameplay cao,
khớp kiến trúc + tận dụng bộ test controller vừa dựng). 03 để cuối (scope lớn nhất).

## Quy ước
- Mỗi task có **Slice 0 = chốt số liệu/thiết kế** trước khi code.
- Mỗi feature user-facing: string **cả vi + en**; state class `@Immutable/@Stable`;
  thêm entity thì theo checklist 5-mảnh + tinker (KHÔNG coroutine loop mới).
- Verify sau mỗi slice: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.
- Xong task → chuyển file sang `doc/task/done/`.
