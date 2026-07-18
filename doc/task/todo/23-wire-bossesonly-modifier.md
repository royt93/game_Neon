# Task 23 — Wire `bossesOnly` modifier vào gameplay thực (enhance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "enhance tính năng cũ", option
recommended). Audit source xác nhận: `bossesOnly` được khai báo trong
`RunModifier.kt`, set `true` ở 1 preset ("Chỉ boss"), propagate vào
`EffectiveStats` khi merge, và log ra ở `GameState.kt` — nhưng **không có bất
kỳ consumer nào đọc `merged.bossesOnly` để filter enemy spawn hoặc áp dụng
hiệu ứng gameplay**. Đây là dead field thật sự (grep hẹp trên toàn bộ
`ui/game/`: chỉ 3 file tham chiếu, không file nào là spawn/score controller).

## Hiện trạng (đã có)
- `RunModifier.kt` — field `bossesOnly: Boolean = false`, preset "Chỉ boss" set `true`.
- `EffectiveStats.kt` — merge field vào stats tổng hợp của run.
- `GameState.kt` — chỉ log giá trị field ra debug, không dùng để rẽ nhánh logic.

## Slice 0 — chốt thiết kế (TODO)
- Xác định hành vi thực khi `bossesOnly == true`: filter enemy spawn controller
  chỉ cho phép spawn `EnemyType` thuộc nhóm boss (bỏ qua enemy thường/space
  object), hay giữ enemy thường nhưng nhân điểm số khi hạ boss?
- Chốt: có giữ nguyên spawn rate boss hiện tại hay tăng tốc xuất hiện boss khi
  modifier này bật (vì không còn enemy thường để "câu giờ")?
- Xác định file spawn controller cụ thể sẽ đọc field này (tên chính xác cần
  xác nhận khi bắt đầu code — có thể là `EnemyController`/`StageController`).

## Slice 1 — wire filter logic (TODO)
- Đọc `effectiveStats.bossesOnly` (hoặc field tương đương) trong spawn
  controller, bỏ qua spawn enemy thường khi `true`.
- Test: unit test controller — khi bật modifier, danh sách enemy spawn ra
  chỉ chứa boss type.

## Slice 2 — cân bằng + verify (TODO)
- Playtest tay: xác nhận stage vẫn kết thúc được (không bị treo vì thiếu
  enemy thường để trigger `readyForNextStage`).
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.

## Slice 3 — eyeball device (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro thấp (field cô lập, sửa 1 controller, không
đụng entity khác), nhưng cần chốt rõ Slice 0 vì đụng điều kiện
`readyForNextStage` của `StageController`.
