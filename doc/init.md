## 🔥 Quy tắc kỹ thuật ưu tiên cao

> Dự án **Sky Force U*S*A** (`com.tranphuloi.neon`) — game bắn máy bay cuộn dọc,
> **Kotlin + Jetpack Compose**. Các quy tắc dưới là bắt buộc khi thêm code mới.
> (Chi tiết kiến trúc xem `CLAUDE.md` ở gốc repo.)

- **Không memory leak, không bug.** Entity transient (damage numbers, popups,
  sparks) dùng immutable list snapshot; kiểm tra qua `LeakWatch.watch(obj, "...")`
  (debug = LeakCanary, release = no-op) — không import LeakCanary trực tiếp ở `main/`.
- **Một game loop duy nhất** trên `Dispatchers.IO` trong `GameState.kt` (`delay(8)`).
  Thêm hệ thống mới qua `tinker(id, repeatTime, doWork)`, KHÔNG tạo coroutine loop
  riêng. Mỗi `tinker` id phải unique + ổn định (rememberSaveable / constant).
- **State sống ở `GameState.kt`**, không ở controller. Controller chỉ nhận
  getter/setter lambda. Không bỏ `refreshHandler` read cuối `rememberGameState()`.
- **Settings / Leaderboard / Achievements** truy cập qua CompositionLocal
  (`LocalSettings.current` …), không re-instantiate repository.
- **Logger** thay cho `Log.d`: `Logger.d` cho sự kiện thưa (init/lifecycle/stage/
  boss/achievement), `Logger.v { }` cho hot-path (per-frame/collision/spawn).
- **Canvas batching:** entity volume cao phải gộp vào Canvas chung (`LaserCanvas`,
  `EnemyCanvas`, `SpaceObjectCanvas`, `BoosterCanvas`, `MineralCanvas`) — tránh
  Composable `forEach` per-entity.
- **Animation đồng bộ** giữa các màn/dialog (NeonBottomSheet, pulse, banner) — tham
  khảo screen khác trước khi thêm hiệu ứng mới.
- **i18n:** mọi string mới thêm vào CẢ `values-vi/strings.xml` và `values-en/strings.xml`.
- **Compose stability:** data class state mới nên `@Immutable` / `@Stable`.
- **Build verify mỗi wave:**
  `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`.
