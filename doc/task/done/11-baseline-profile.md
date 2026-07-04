# Task 11 — Baseline Profile (giảm jank cold-start)

**Picked 2026-07-04 (đợt 3).** Infra nặng nhất; giảm jank first-composition GameScreen (đã xác nhận là bottleneck, gameplay đã mượt 0 skip). Phải generate trên device thật.

## Slice 0 — thiết kế chốt

Build đủ điều kiện: AGP 9.1.1 + Gradle 9.5.0 + config-cache ON. Chưa có gì về baseline profile (không plugin/dep/module/profile file/ProfileInstaller init).

**Cách tiếp cận ít rủi ro:** dùng **plugin `androidx.baselineprofile`** + module test riêng `:baselineprofile` (`com.android.test`) chạy macrobenchmark generate profile trên device, plugin tự merge profile vào `:app` + tự kéo `profileinstaller`.

### Ràng buộc (hard-won)
- **Config-cache ON** → plugin/task phải config-cache-compatible (androidx.baselineprofile OK).
- **Flavor dimension `type` (dev/production)** → module `:baselineprofile` phải khai flavor khớp; generate cho **production** (build release minified = profile chính xác). Baseline plugin cần biết variant target.
- **Device thật `FUJZIFIR7DQCNRWW`**, KHÔNG Test Orchestrator (OOM low-RAM) — macrobenchmark chạy trực tiếp connected device.
- Release minify+shrink ON → profile phải gen trên build minified.
- Version macrobenchmark/benchmark: dùng bản khớp AGP 9.1.1 (kiểm tra qua context7/BOM lúc impl; tránh hardcode version cũ 1.2.x nếu quá cũ).

### File tạo/sửa
1. `settings.gradle` — `include ':baselineprofile'`.
2. Root `build.gradle` — classpath/plugin `androidx.baselineprofile` (version khớp AGP).
3. `app/build.gradle` — apply `androidx.baselineprofile` (consumer) + `baselineProfile { }` block trỏ producer module; profileinstaller tự thêm (hoặc thêm tay nếu cần).
4. `baselineprofile/build.gradle` — `com.android.test` + `androidx.baselineprofile` (producer) + flavor `type` khớp + deps macrobenchmark/uiautomator/junit; `targetProjectPath = ':app'`.
5. `baselineprofile/src/main/AndroidManifest.xml` — rỗng.
6. `baselineprofile/src/main/java/.../BaselineProfileGenerator.kt` — `BaselineProfileRule`, startup COLD + đợi `By.res("menu_play")` + tap play + chạy vài giây game loop (phủ đường cold-start + GameScreen composition).
7. (Nếu plugin không tự lo) `App.onCreate` — không cần gọi ProfileInstaller thủ công khi dùng plugin (plugin nhúng + profileinstaller tự load). Xác nhận lúc impl.

### Generate + verify
- `./gradlew :app:generateProductionReleaseBaselineProfile` (hoặc task plugin phơi) trên device `FUJZIFIR7DQCNRWW`.
- Profile ra `app/src/production/generated/baselineProfiles/` (hoặc theo plugin) → commit.
- Verify: profile xuất hiện trong APK release; đo jank cold-start trước/sau (macrobenchmark StartupTimingMetric) nếu kịp.

### Rủi ro / thoát hiểm
- Nếu module `:baselineprofile` + flavor + config-cache gây lỗi build khó gỡ → **fallback**: chỉ thêm `androidx.profileinstaller` dep + profile thủ công tối thiểu, hoặc DEFER Task 11 và báo user (không làm hỏng build hiện tại).
- KHÔNG để Task 11 làm vỡ build 2 flavor hiện có — mọi bước verify `compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest` phải xanh.

### Slice hoá
- **Slice 1:** module + gradle wiring, compile xanh 2 flavor (chưa generate).
- **Slice 2:** generator test + generate profile trên device.
- **Slice 3:** verify profile trong APK + đo trước/sau + doc.

## Trạng thái
✅ **DONE — GENERATED profile shipped (2026-07-04, cập nhật).**

### ⭐ Cập nhật: generated ĐÃ CHẠY với `androidx.baselineprofile 1.5.0-alpha07`
- Probe lại maven-metadata: `<latest> = 1.5.0-alpha07` (1.4.1 là `<release>` cũ thời AGP 8). **1.5.0-alpha07 apply THÀNH CÔNG trên AGP 9.1.1** (hết lỗi `TestExtension`).
- Dựng module `:baselineprofile` (`com.android.test` + `androidx.baselineprofile`, KHÔNG cần `kotlin.android` — AGP 9 tích hợp sẵn Kotlin) + `BaselineProfileGenerator` (`BaselineProfileRule.collect`, `includeInStartupProfile=true`): cold-start → menu (relaunch-retry né clip) → vào Game → chạy loop 6s.
- App: apply plugin `androidx.baselineprofile` + `baselineProfile project(":baselineprofile")`.
- **Generate trên device SM-S928B (S24 Ultra):** `./gradlew generateProductionReleaseBaselineProfile` (macrobenchmark 1 test PASS) → `app/src/productionRelease/generated/baselineProfiles/baseline-prof.txt` (**25,034 rule**, 3,553 method-level cho neon với cờ startup SPL) + `startup-prof.txt`.
- **Verify:** release APK nhúng `assets/dexopt/baseline.prof` **12,627 bytes** (giàu hơn curated 7848). Compile 2 flavor + full JVM test PASS.
- Curated `src/main/baseline-prof.txt` **đã gỡ** (generated thay). `baselineprofile/build/` + `.kotlin/` thêm vào .gitignore.

### Regenerate về sau
`./gradlew generateProductionReleaseBaselineProfile` trên device ổn định (rebuild sau khi đổi code startup/game path). Bump version `androidx.baselineprofile` khi có bản stable ≥ 1.5.0.

### (lịch sử) curated stopgap trước khi tìm ra 1.5.0-alpha07 — nay bỏ

### Đã probe & chặn: macrobenchmark-generated (approach chuẩn ban đầu)
- Plugin `androidx.baselineprofile` **1.4.1 là bản mới nhất** trên Google Maven (1.4.2+ = 404).
- Apply plugin trên AGP 9.1.1 **FAIL**: `Extension of type 'TestExtension' does not exist` — AGP 9 đổi `TestExtension` → `TestExtensionImpl`; plugin 1.4.1 (thời AGP 8) chưa hỗ trợ. Không có version tương thích AGP 9.1.1 hiện tại.
- `kotlin.android` plugin cũng fail apply trên `com.android.test` với toolchain này.
- Device serial đổi liên tục trong session → generate on-device không tin cậy.
- → Module `:baselineprofile` + plugin đã revert sạch (build giữ xanh). Theo đúng fallback DEFER của Slice 0.

### Đã ship: curated baseline profile (low-risk, KHÔNG cần plugin/module/device)
- `app/src/main/baseline-prof.txt` — **139 whole-class rule** hot-path cold-start + GameScreen first-composition (App/MainActivity/GameScreenKt/GameState(+Kt)/EffectiveStats/RunContext + toàn bộ Controller/Mapper/world Canvas/AudioPlayer/ImageLoader). Tên class lấy TỪ output compile thật → descriptor chuẩn (gồm cả lambda/inner class hot-path).
- Dep `androidx.profileinstaller:profileinstaller:1.4.1` (runtime AAR, không phụ thuộc AGP → an toàn) — cài profile lúc chạy đầu (API 24-30; API 31+ nền tảng tự dùng).
- **Verify:** `assembleProductionRelease` chạy `mergeArtProfile`/`compileArtProfile` → **release APK nhúng `assets/dexopt/baseline.prof` (7848 bytes) + `baseline.profm`**. Compile 2 flavor + full JVM test PASS.

### TODO khi có plugin hỗ trợ AGP 9
Khi `androidx.baselineprofile` (hoặc benchmark) ra bản hỗ trợ AGP 9: dựng lại module `:baselineprofile` (`com.android.test`) + `BaselineProfileRule` startup+gameplay, generate trên device ổn định → thay curated bằng generated (chính xác hơn). Curated hiện tại vẫn hợp lệ, chỉ coarse hơn.
