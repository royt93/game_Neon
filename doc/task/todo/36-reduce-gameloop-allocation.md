# Task 36 — Giảm allocation trong game loop (performance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tối ưu hiệu suất"). Vòng
lặp chính trong `rememberGameState()` chạy trên `Dispatchers.IO` ~125Hz
(`delay(8)`), gọi `tinker(...)` cho ~15+ ID mỗi iteration — audit + giảm
allocation (list rebuild, boxing, lambda capture mới mỗi frame) trong hot
path này để giảm áp lực GC.

## Slice 0 — chốt thiết kế (TODO)
- Công cụ đo baseline: dùng `perf/HotPathPerfTest` hiện có (JVM, iteration-
  budget timing) làm thước đo trước/sau, hay cần thêm Android Studio
  Profiler / memory allocation tracker trên device thật?
- Phạm vi audit: ưu tiên controller nào trước (đề xuất: controller có list
  lớn nhất mỗi tick — enemy/laser/spaceObject, vì đây là nơi nhiều khả năng
  rebuild list mỗi frame thay vì mutate in-place).
- Ngưỡng thành công: giảm bao nhiêu % allocation/tick được coi là đạt (không
  cần con số tuyệt đối nếu không đo được chính xác trên CI, nhưng cần có số
  so sánh trước/sau trên cùng 1 thiết bị).

## Slice 1 — audit từng controller hot path (TODO)
- Rà `*Controller.kt` trong `ui/game/`: tìm chỗ tạo `List`/`Map` mới mỗi lần
  gọi thay vì mutate/reuse buffer, tìm boxing không cần thiết (`Int`→`Integer`
  qua generic collection).
- Ghi lại danh sách điểm nóng tìm được (không sửa vội, review trước).

## Slice 2 — sửa từng điểm nóng (TODO)
- Thay list rebuild bằng mutate in-place khi an toàn (không phá Compose
  `mutableStateOf` snapshot semantics — cẩn thận vì state phải trigger
  recomposition đúng cách, không được mutate list bên trong `mutableStateOf`
  mà không set lại reference nếu Compose cần detect thay đổi).
- Test: `perf/HotPathPerfTest` không regress, unit test hiện có vẫn pass.

## Slice 3 — verify + so sánh perf (TODO)
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.
- Chạy lại `HotPathPerfTest`, ghi số liệu trước/sau.

## Slice 4 — eyeball device (kiểm tra framerate/giật) (TODO)

## Trạng thái
📋 **TODO** — chưa bắt đầu. Rủi ro vừa (đụng hot path nhiều controller cùng
lúc — cần cẩn thận không phá Compose state semantics khi tối ưu mutate
in-place).
