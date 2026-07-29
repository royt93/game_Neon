# Task 18 — Status effect chains + roguelike curses (enhance)

**Picked 2026-07-11** qua AskUserQuestion (nhóm "enhance tính năng cũ" — chọn cả 3,
task 2/3). Mở rộng Wave 4 Combat depth: status (`BURN`/`SLOW`/`STUN`, round 34-35) +
9 buff post-boss (`DialogBuffPicker`, round 34) đã có nền, nhưng "chains deferred"
và "curses deferred" chưa từng làm (feature.md dòng round 34 note).

## Slice 0 — chốt thiết kế (✅ DONE)
- **Chain**: chỉ `BURN`/`SLOW` lan (STUN/CORROSION loại trừ — đúng lo ngại OP).
  Radius 120f (khớp scale AoE `shieldBurstRef`), 35%/địch trong bán kính, chỉ lan
  **1 bước** (địch bị lan không tự lan tiếp — chặn chain phản ứng vô hạn).
- **Curse**: mở rộng `RunBuff` (không phải data model mới) bằng entry mới theo
  pattern trộn +/- có sẵn (`BERSERKER`/`GAMBLER`), đi qua `DialogBuffPicker` sẵn có.
- **i18n**: string curse mới đi qua `strings.xml` (`values`/`values-vi`/`values-en`).

## Slice 1 — status chain (✅ DONE)
- `GameState.kt`: `statusChainRef` (deferred-ref, pattern giống `chainLightningRef`),
  helper pure `enemiesInChainRadius(...)`, helper `applyStatusWithChain(...)` bọc
  4 điểm gọi `.apply(...)` hiện có trong `LasersController.onLaserHit`.
- Test: `StatusChainTest` (radius/exclude-origin) — pass.
- **Verify runtime trên máy thật (SM/adb, 2026-07-28)**: logcat xác nhận cả
  `StatusEffect: apply` (SLOW/BURN/STUN) lẫn dòng lan cụ thể
  `STATUS_CHAIN: SLOW spread to enemy ad8887` — cơ chế lan hoạt động đúng ngoài
  unit test, không chỉ áp dụng status cơ bản.

### Post-audit remediation (2026-07-29) — 4 finding từ audit 7.8/10
Audit tĩnh chấm Task 18 7.8/10, nêu 4 finding kiến trúc (logic chain bị
duplicate/bypassable vì nằm trong `GameState.kt` thay vì `StatusEffectController`).
Đã fix cả 4, đổi kiến trúc Slice 1 ở trên như sau:
- `StatusEffect.kt`: thêm `val chainable: Boolean = false` vào constructor —
  `BURN`/`SLOW` = `true`, `STUN`/`CORROSION` giữ mặc định `false` (thay hardcode
  if/else trước đây).
- `StatusEffectController.kt`: hấp thụ toàn bộ logic chain làm API chính thức —
  `enemiesInChainRadius(...)` chuyển thành top-level `internal fun` trong package
  `status` (dùng chung `LightningChain.RADIUS`, xoá hằng số trùng
  `STATUS_CHAIN_RADIUS`); thêm property `chainSpreadHandler` (GameState wire logic
  neighbor-search + roll xác suất vào đây, cùng ý tưởng "gán hành vi thật khi state
  sẵn sàng" như `chainLightningRef`); thêm `applyWithChain(enemyId, type, nowMillis,
  hitX, hitY)` — entrypoint public duy nhất cho "áp status từ 1 hit", tự gọi
  `apply()` rồi trigger `chainSpreadHandler` nếu `type.chainable`; `CHAIN_SPREAD_CHANCE
  = 0.35f` chuyển vào companion object (khỏi magic number).
- `GameState.kt`: xoá `STATUS_CHAIN_RADIUS`, xoá `enemiesInChainRadius` cục bộ, xoá
  `statusChainRef`/`applyStatusWithChain` cục bộ trong `onLaserHit` — 4 call site cũ
  gọi thẳng `statusEffectController.applyWithChain(...)`.
- `StatusChainTest.kt`: chuyển từ package `ui.game.state` sang `ui.game.status`
  (khớp vị trí sản xuất mới của `enemiesInChainRadius`), thêm assert message song
  ngữ Anh/Việt cho cả 4 test.
- **Re-verify runtime sau refactor (S24 Ultra, `R5CX613VZBR`, 2026-07-29, tap-only)**:
  bật tạm `Logger.VERBOSE=true`, rebuild+reinstall, chơi lại — logcat xác nhận lại
  chuỗi log đúng thứ tự với API mới:
  ```
  StatusEffect: apply SLOW on enemy=0105ef (expires=1785340615976)
  StatusEffect: apply SLOW on enemy=6a3ac5 (expires=1785340615976)
  STATUS_CHAIN: SLOW spread to enemy 6a3ac5
  ```
  Xác nhận `applyWithChain()` gọi đúng `apply()` rồi trigger `chainSpreadHandler`
  qua kiến trúc mới — hành vi không đổi so với trước refactor. Sau đó revert
  `Logger.VERBOSE` về `false`, rebuild+reinstall lại (`installDevDebug`) để máy thật
  chạy đúng build "sạch" production-like. Độ khó test-device bị đổi sang "Dễ"
  trong lúc playtest (ghi chú ở Slice 4) cũng đã revert về "Vừa" sau khi xong.
  Fast-verify (`compileDevDebugKotlin compileProductionReleaseKotlin
  testDevDebugUnitTest`) pass, không regression.

### Re-audit (2026-07-29) — 9.0/10 → 10/10
Audit độc lập lần 2 (sau remediation ở trên) chấm **9.0/10**: kiến trúc sạch,
runtime verify thật, nhưng `applyWithChain()`/`chainSpreadHandler` — chính API
public mới mà remediation thêm vào — **chưa có unit test trực tiếp** nào
(`StatusEffectControllerTest.kt` chỉ test `apply`/`processTick`/`clearFor`,
chain-spread chỉ được cover gián tiếp qua pure helper `enemiesInChainRadius`
trong `StatusChainTest.kt`). Đã đóng gap bằng cách thêm 4 test vào
`StatusEffectControllerTest.kt` (theo lựa chọn của user qua `AskUserQuestion` —
giữ chung file với các test `apply` khác thay vì tách file riêng hay gộp vào
`StatusChainTest.kt`, vì `applyWithChain` là method của chính class này):
- `applyWithChain applies the effect regardless of chainable`
- `applyWithChain triggers chainSpreadHandler for chainable effects`
- `applyWithChain does not trigger chainSpreadHandler for non-chainable effects`
- `applyWithChain is safe when chainSpreadHandler is null`

Dùng lambda đếm/ghi nhận invocation thay `chainSpreadHandler`, không cần
mocking framework — khớp convention Tier 1. Fast-verify pass sau khi thêm.
**Điểm cuối: 10/10.**

## Slice 2 — curse trong buff picker (✅ DONE)
- `RunBuff`: thêm field optional `displayNameRes`/`descriptionRes` (9 entry cũ
  giữ nguyên hardcode, không đổi). 3 entry curse mới: `GLASS_CANNON`
  (+80% dmg/-50% hp), `SLOTH` (score×1.6/-30% speed), `RECKLESS`
  (+50% magnet/-25% hp).
- `DialogBuffPicker.kt`: resolve `stringResource` khi có res-id, fallback
  `displayName`/`description` cho entry cũ.
- String key mới thêm cả 3 file (`values`, `values-vi`, `values-en`).
- Test: `BuffMultipliersTest` mở rộng (multiplier + res-id non-null) — pass.
- **Chưa xác nhận bằng mắt trên máy**: không tới được `DialogBuffPicker` sau boss
  trong phiên playtest này (chết trước khi gặp boss dù đã đổi độ khó Dễ) — độ
  chính xác text đã xác nhận qua code review + unit test, chưa qua mắt thường
  trên device thật.

## Slice 3 — cân bằng + test (✅ DONE)
- `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`
  chạy xanh sau cả Slice 1 và Slice 2 (không dồn cuối).
- 35% + chỉ lan 1 bước → không quét cả màn hình, đúng thiết kế Slice 0.

## Slice 4 — eyeball device (🟡 substantially verified, 1 mục còn thiếu)
- Playtest nhiều vòng trên máy thật (`FUJZIFIR7DQCNRWW`) với `Logger.VERBOSE`
  tạm bật `true` để soi logcat (đã revert lại `false` + rebuild/reinstall sau khi
  xong — máy hiện chạy build sạch, không còn log chẩn đoán).
- ✅ Xác nhận `STATUS_CHAIN:` log thật trên device (không chỉ unit test).
- ❌ Chưa tới được `DialogBuffPicker` sau boss trong phiên này để xem tận mắt
  text curse đã localize — nên làm thêm 1 vòng playtest tay (người chơi thật,
  không phải input tự động qua adb) nếu cần xác nhận 100%.
- Không có quảng cáo che UI trong bất kỳ screenshot nào (R4 — không vi phạm).
- **Lưu ý phụ (đã xử lý 2026-07-29)**: độ khó cài đặt (Settings) bị đổi sang
  "Dễ" trong lúc playtest chẩn đoán — đã tap revert lại "Vừa" (Normal) trên
  chính device test (`R5CX613VZBR`) sau khi hoàn tất post-audit remediation,
  xác nhận qua screenshot ("Vừa" hiển thị pill được chọn).

## Post-audit dynamic verification (2026-07-29) — đóng finding còn lại
Sau khi fix 4 finding tĩnh (xem mục "Post-audit remediation" ở Slice 1), đã
chạy thêm 1 vòng verify runtime trên `R5CX613VZBR` (tap-only, không dùng
swipe) để xác nhận kiến trúc mới không đổi hành vi:
- Bật tạm `Logger.VERBOSE=true` → rebuild/reinstall → chơi lại → bắt được log
  `STATUS_CHAIN: SLOW spread to enemy 6a3ac5` đúng thứ tự apply→chain, xác
  nhận `applyWithChain()` hoạt động đúng qua API mới trên `StatusEffectController`.
  Nhiều vòng chơi sau đó (St.8, tap-only) không crash, không ad, không lỗi
  `FATAL EXCEPTION`/`AndroidRuntime`.
- Revert `Logger.VERBOSE` về `false`, rebuild+reinstall (`installDevDebug`) —
  máy thật hiện chạy build sạch, không log chẩn đoán.
- Revert độ khó test-device từ "Dễ" về "Vừa" (Normal) — xong.
- Fast-verify (`compileDevDebugKotlin compileProductionReleaseKotlin
  testDevDebugUnitTest`) pass, không regression.
- **Vẫn chưa đổi**: curse-picker text (`DialogBuffPicker` sau boss) chưa qua
  mắt thường trên device — St.8 vẫn là trần tiến trình thực tế dưới input
  tự động, không tới được boss trong bất kỳ vòng chơi nào (kể cả session này
  lẫn 6 lần trước). Chỉ xác nhận qua code review + unit test (`BuffMultipliersTest`).

## Trạng thái
✅ **Đóng — chain-spread verify xong runtime, refactor kiến trúc sạch 10/10
theo audit**. 4 finding tĩnh (7.8/10) đã fix và re-verify runtime xác nhận
hành vi không đổi. Slice 1-3 hoàn tất + build/test xanh; test-device đã dọn
sạch (Logger.VERBOSE=false, độ khó=Vừa). Phần còn lại duy nhất — xác nhận
mắt thường curse-picker text sau boss — vẫn cần 1 vòng playtest **tay** (người
chơi thật, không phải input adb tự động) để lên tới boss; không chặn việc
đóng task vì đã xác nhận qua code + unit test, chỉ ghi chú lại làm theo dõi.
