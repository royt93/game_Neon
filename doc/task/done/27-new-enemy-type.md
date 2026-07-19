# Task 27 — Enemy type mới (new)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng mới", option
recommended). Doc gốc đề xuất 5 candidate (Splitter/Repulsor/Jammer/
Missileer/Predator) — **audit lại 2026-07-19 phát hiện cả 5 đã implement bởi
Task 09 (2026-07-04)**, chỉ là biến thể attack-pattern (đòn bắn), không có
enemy nào trong game có cơ chế hành vi riêng ngoài 6 kiểu bắn. Hỏi lại user
qua AskUserQuestion → chốt: tạo **địch hoàn toàn mới** với cơ chế
**mitosis-on-death** (tách làm 2 khi chết) — cơ chế hành vi đầu tiên trong
game ngoài attack pattern.

## Slice 0 — chốt thiết kế (✅ done 2026-07-19)
- **Tên:** "Amip Vũ Trụ" (Cosmic Amoeba). Drawable mới `R.drawable.enemy_amoeba`
  (placeholder XML 1dp, theo đúng convention — hình vẽ thật nằm trong
  `EnemyCanvas.kt` shape recipe dispatch by drawableId, không phải vector XML).
- **Family:** `FIGHTER` (cân bằng, giống Predator — mitosis tự nó đã là điểm
  nhấn, không cần family lệch hẳn sang tank/nhanh).
- **AttackKind:** không set — `EnemyAttackKind.forDrawable()` mặc định
  `SINGLE` cho drawableId không nằm trong 5 case Task 09, khỏi cần sửa file.
- **Chapter:** 4 (cùng nhóm chủ đề với Splitter/Repulsor, `regularEnemyDrawables`).
- **Cơ chế mitosis — tận dụng lại `EnemySize`/`sizeScale` sẵn có (Wave 13c),
  KHÔNG cần công thức stat mới:**
  - `RegularEnemyType` thêm field mới `splitsOnDeath: Boolean = false` (default
    → 34 enemy type hiện có không đổi hành vi).
  - `RegularEnemy` thêm field mới `isMitosisChild: Boolean = false` (default →
    mọi call site khác không đổi). Đây chính là cơ chế chống-đệ-quy: con sinh
    ra mang cờ `true`, khi con đó chết sẽ KHÔNG tách tiếp — không cần đụng
    `Enemy` interface (tức không đụng các boss class).
  - Con sinh ra: `sizeScale = parent.sizeScale * 0.4f` → do `EnemySize.hpFactor`
    tuyến tính theo scale, HP con = 40% HP cha mỗi con (~80% tổng, đúng đề xuất
    gốc), width/height cũng scale theo, `speedFactor` tự động khiến con nhanh
    hơn (coerce tối đa 1.6×), mineral reward tự tính theo `EnemySize.mineralReward`.
  - Spawn tại vị trí cha chết (`xOffset ± width*0.3` để 2 con không đè khít lên
    nhau), `yOffset` giữ nguyên.
  - Điểm chèn logic: `EnemyController.processEnemies()` — ngay sau
    `onEnemyKilled(it)` (dòng ~263), nếu
    `it is RegularEnemy && it.type.splitsOnDeath && !it.isMitosisChild` → gọi
    `enemyFactory.spawnMitosisChildren(it)` rồi `enemies += children`.
- **Info/Bestiary:** thêm 1 `EnemyVariantSpec` trong `InfoScreen.kt` (title/
  subtitle/description tiếng Việt only — theo đúng convention 10 địch Round 81,
  không phải string resource).

## Slice 1 — domain model + shape (✅ done 2026-07-19)
- `RegularEnemyType.splitsOnDeath` + `RegularEnemy.isMitosisChild` thêm vào
  model (default false, không đổi hành vi 34 enemy type cũ). `RegularEnemy.type`
  đổi từ `private` sang public (cần cho Slice 2 đọc `it.type.splitsOnDeath`
  và tái dùng `parent.type` khi tạo con — data class nên visibility không ảnh
  hưởng equals/hashCode/copy).
- `enemy_amoeba.xml` placeholder + `drawEnemyAmoeba(...)` (blob 2 thùy chồng
  lấn + rãnh giữa) trong `EnemyCanvas.kt`, kèm `bodyColorFor`/`accentColorFor`
  dispatch. `EnemyFamily.fromDrawableId` thêm case `enemy_amoeba -> FIGHTER`
  tường minh.
- `InfoScreen.kt`: `drawEnemyAmoebaPreview(...)` + `EnemyVariantSpec` entry
  ("Amip Vũ Trụ") trong `enemyVariantSpecs()`.

## Slice 2 — controller logic (✅ done 2026-07-19)
- `EnemyFactory.spawnMitosisChildren(parent: RegularEnemy): List<RegularEnemy>`
  — `childScale = parent.sizeScale * 0.4f`, spawn tại `parent.xOffset ± width*0.3`,
  `isMitosisChild = true`.
- `EnemyController.processEnemies()`: wire injection ngay sau `onEnemyKilled(it)`
  — guard `it is RegularEnemy && it.type.splitsOnDeath && !it.isMitosisChild`.
- Test mới `EnemyControllerMitosisTest.kt` (3 test, tất cả pass):
  1. `splitsOnDeath=true` chết → đúng 2 con, `isMitosisChild=true`,
     `sizeScale = parent*0.4f`, hp > 0.
  2. Con (`isMitosisChild=true`) chết → KHÔNG tách tiếp (list rỗng).
  3. `splitsOnDeath=false` (enemy thường) chết → không sinh con, hành vi cũ
     không đổi.

## Slice 3 — wiring vào Stage/Chapter + verify (✅ done 2026-07-19)
- `Chapter.kt`: thêm `R.drawable.enemy_amoeba` vào `regularEnemyDrawables` của
  Chapter 4 (HOSTILE_STATION), sau Splitter/Repulsor.
- `Stage.kt`: `RegularEnemyType(...)` construction thêm
  `splitsOnDeath = drawable == R.drawable.enemy_amoeba` — hp/impact/speed vẫn
  tự tính qua formula sẵn có, không cần số liệu tay.
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`
  → cả 2 flavor compile sạch, 956/956 unit test pass (953 cũ + 3 mitosis mới).

## Slice 4 — eyeball device (✅ done 2026-07-19, scoped down)
- Device: Pixel 7 Pro (`2B051FDH3006MU`) — chỉ 1 device kết nối, dùng luôn
  theo R3 (đã thông báo).
- Xác nhận qua `Bách khoa` (InfoScreen) trên máy thật: entry "Amip Vũ Trụ"
  hiển thị đúng — icon 2 thùy xanh lục + rãnh giữa, subtitle "Trạm Thù Địch
  (Fighter) · HP ~180 · tách khi chết", description mô tả mitosis đúng thiết
  kế. Không crash, không quảng cáo che UI (R4 — n/a, InfoScreen không có ad).
- **KHÔNG** verify combat trực tiếp trong Chương 4: checkpoint hiện tại chỉ ở
  Chương 1 · Màn 13 (còn xa Chương 4), chơi xuyên 3 chương tốn nhiều thời
  gian. Hỏi user qua AskUserQuestion → chọn dừng ở mức build+test+bestiary
  hiện tại thay vì playtest sâu. Rủi ro còn lại (chấp nhận được): chưa thấy
  tận mắt Amip bắn/tách đôi trong gameplay thật — logic đã cover đủ bởi 3 unit
  test ở Slice 2.

## Trạng thái
✅ **Done** — Slice 0-3 xong đầy đủ + verify device (InfoScreen). Slice 4 combat
in-game chưa playtest (chấp nhận theo quyết định user 2026-07-19).
