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

## Slice 0 — chốt thiết kế (DONE)
- Khảo sát xác nhận: `readyForNextStage` (GameState.kt) chỉ cần
  `!enemyController.hasEnemies() && !spaceObjectsController.hasSpaceObjects()`
  — không có điều kiện đếm kill/count. Stage advancement thuần time-based
  (`StageController.getGameStage`). → tắt hẳn spawn enemy thường **an toàn**,
  không làm treo stage.
- **Quyết định** (AskUserQuestion, user chọn Recommended cho cả 2):
  1. Khi `effectiveStats.bossesOnly == true` → bỏ qua hoàn toàn spawn enemy
     thường (không filter theo type, chặn thẳng ở nguồn gọi
     `enemyController.addEnemy(...)`). Giữ nguyên `scoreMul=2f` đã có sẵn ở
     preset "Chỉ boss" (không đổi).
  2. Giữ nguyên spawn rate boss hiện tại (`stage.enemyType.spawnRate` của
     `StageBoss`) — không tăng tốc xuất hiện boss.
- **Vị trí sửa thật sự** (đã điều chỉnh so với dự kiến ban đầu — xem Slice 1):
  không guard tại `GameState.kt`, mà theo đúng convention sẵn có của
  `BoosterController` (`noShieldDrops`/`noBoosters` truyền vào controller dạng
  lambda getter) — thêm tham số `bossesOnly: () -> Boolean` vào
  `EnemyController`, guard bên trong `addEnemy()`. `GameState.kt` chỉ truyền
  lambda `{ effectiveStats.bossesOnly }` vào constructor.

## Slice 1 — wire filter logic (DONE)
- Thêm tham số `bossesOnly: () -> Boolean = { false }` vào constructor
  `EnemyController` (`ui/game/enemy/ship/controller/EnemyController.kt`), guard
  ngay sau khi tính `isBossSpawn` trong `addEnemy(type: EnemyType)`:
  `if (!isBossSpawn && bossesOnly()) { Logger.v { ... SKIPPED ... }; return }`.
  Boss (`MidBossType`/`LevelOneBossType`/`LevelTwoBossType`/`FinalBossType`)
  luôn bypass guard này.
- `GameState.kt` wire `bossesOnly = { effectiveStats.bossesOnly }` vào lời gọi
  constructor `EnemyController(...)`.
- Test mới: `EnemyControllerBossesOnlyTest.kt` (3 test, capture kết quả qua
  callback `setEnemies` vì `enemies` là property private không có getter công
  khai — theo đúng pattern của `EnemyControllerSlowTest`):
  - `regular enemy spawn skipped when bossesOnly active`
  - `boss spawn still works when bossesOnly active`
  - `regular enemy spawns normally when bossesOnly inactive`

## Slice 2 — cân bằng + verify (DONE)
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`
  → build sạch cả 2 flavor, **953/953 test pass** (0 fail, 0 error), bao gồm
  3 test mới của Slice 1.
- Playtest tay trên device (xem Slice 3): chọn thử thách preset "Chỉ boss"
  (×2.0) từ menu "Thử thách", chơi Ch.1 từ St.4. Xác nhận:
  - Enemy thường **không xuất hiện** trong suốt St.4 → St.9 (HP tàu giữ
    nguyên 1428 suốt — không bị bắn phá bởi enemy thường).
  - Stage vẫn tiến bình thường theo thời gian (`StageController advance`
    log idx 4→5→6→7→8→9→10), **không treo** — xác nhận đúng dự đoán Slice 0
    (`readyForNextStage` không phụ thuộc kill-count).
  - Mid-boss "Lính gác mắt sát thủ" (St.10) spawn và tấn công bình thường,
    hạ được, run tiếp tục sang mid-boss #2 (St.12→13) — chuỗi boss không bị
    ảnh hưởng bởi modifier.
  - Điểm tăng 72 → 282 nhờ `scoreMul=2.0` (preset có sẵn, không đổi).
  - Không crash/ANR liên quan app trong logcat; không quảng cáo che UI (app
    chưa wire ad SDK thật).

## Slice 3 — eyeball device (DONE)
- Device: **Pixel 7 Pro** (`2B051FDH3006MU`, duy nhất kết nối lúc test).
- `installDevDebug` → cài sạch, mở app, chọn thử thách "Chỉ boss", chơi thử
  ~2 phút (chi tiết ở Slice 2). Thoát về menu qua "Về menu" — checkpoint lưu
  ở St.13, không lỗi.
- Đóng dialog "Chọn thử thách" (không chọn preset mới) → dòng "Thử thách:
  Chỉ boss" tự biến mất khỏi màn hình chính, trạng thái quay lại bình thường
  (không cần thao tác khôi phục thêm).

## Trạng thái
✅ **Done** (2026-07-19, verify device Pixel 7 Pro) — bossesOnly modifier wired
vào `EnemyController.addEnemy()` theo đúng convention lambda-injection của
codebase; 953/953 test pass; playtest xác nhận stage không treo, boss vẫn
spawn/hoạt động bình thường khi tắt enemy thường.
