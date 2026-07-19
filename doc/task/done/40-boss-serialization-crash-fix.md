# Task 40 — Fix boss model serialization crash (bugfix)

**Nguồn gốc:** phát hiện qua logcat review trên Pixel 7 Pro khi playtest Task
27 (2026-07-19). Log không xuất hiện lúc chơi bình thường — chỉ khi
Activity bị che khuất (background) giữa lúc đang combat với boss (`LevelOneBoss`
/ `FinalBoss` / `MidBoss` — bất kỳ boss nào, không riêng gì Amip Vũ Trụ của
Task 27).

## Slice 0 — xác định root cause (DONE)
- Stacktrace: `Parcel.writeBundle()` → `ObjectOutputStream` ném
  `NotSerializableException` trên field ẩn danh của boss (tên class synthetic
  kiểu `GameStateKt$ExternalSyntheticLambda66`) → framework bắt lại thành
  `BadParcelableException`, làm crash toàn app.
- Nguyên nhân: `Enemy : Serializable` (bắt buộc mọi implementation phải
  serializable để sống sót qua `rememberSaveable`), nhưng `LevelOneBoss`,
  `FinalBoss`, `MidBoss` đều có field constructor
  `private val getShip: () -> Ship` — một lambda capture từ `GameState.kt`,
  compile ra synthetic class **không** implement `Serializable`.
- Trigger cụ thể: khi Android che Activity (app khác hiện dialog permission,
  hoặc user bấm Home) trong lúc `enemies` list (giữ trong
  `rememberSaveable`) đang có 1 boss còn sống, hệ điều hành gọi
  `BaseBundle.dumpStats()` (đường debug/diagnostic của
  `PendingTransactionActions$StopInfo.collectBundleStates()`) → cố serialize
  toàn bộ Bundle instance-state → vỡ ngay tại field lambda này.

## Slice 1 — fix (DONE)
- Thêm `@Transient` lên field `getShip` ở cả 3 file boss model:
  `LevelOneBoss.kt`, `FinalBoss.kt`, `MidBoss.kt`. `@Transient` (Kotlin,
  `kotlin.jvm.Transient`) target đúng backing field của
  primary-constructor `val`/`private val` mà không cần `@field:` use-site
  target (annotation này chỉ có `AnnotationTarget.FIELD`).
- Không cần logic khác — `getShip` chỉ dùng để đọc vị trí ship tại
  runtime (`generateLasers()`), không cần khôi phục lại sau khi restore từ
  Bundle (ship reference luôn được re-inject lại từ `GameState.kt` mỗi lần
  boss model được tạo mới qua factory).

## Slice 2 — verify (DONE)
- Build sạch cả 2 flavor + unit test:
  `compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.
- Verify trực tiếp trên Pixel 7 Pro: resume campaign vào đúng 1 trận boss
  đang chạy (Ch.1 St.30, `LevelOneBossType` "Mặt trời đỏ máu", 2125/3000 HP),
  bấm `KEYCODE_HOME` rồi mở lại app — lặp lại 2 lần liên tiếp. Xác nhận qua
  `adb shell dumpsys activity processes | grep pid` rằng process **không**
  bị kill/restart (trước fix sẽ crash ngay khi Bundle dump chạy), và
  screenshot xác nhận state trận đấu (HP, timer) được giữ nguyên.
- Armed `Monitor` logcat lọc `fatal|exception|error|crash|anr|badparcelable|
  notserializable` xuyên suốt — không có event crash nào bắn ra (1 false
  positive vô hại: chuỗi "UncaughtExceptionHandler" khớp pattern "exception").

## Trạng thái
✅ **DONE (2026-07-19)** — fix + verify device Pixel 7 Pro, committed cùng
Task 27 (`8c1a215`). Bug tiền tồn tại từ trước, không liên quan tới Task 27 —
ảnh hưởng mọi boss type (Level 1 / Level 2-Final / Mid-boss), phát hiện tình
cờ trong lúc playtest Task 27.
