# Task 25 — Booster synergy (enhance)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "enhance tính năng cũ").
Hệ thống hiện có 26 `BoosterType` implemented rải qua các Wave, nhưng mỗi
booster hoạt động độc lập — không có cơ chế combo/synergy khi nhiều booster
active cùng lúc.

## Slice 0 — chốt thiết kế (✅ DONE, qua AskUserQuestion)
Chốt **3 cặp synergy** (không phải nhóm lớn, để tránh combo OP):
1. **Khiên + Vũ khí mạnh** (SHIELD + LASER_BOOSTER/TRIPLE_LASER_BOOSTER/PLASMA)
   → +15% damage khi cả hai active.
2. **Kinh tế** (MAGNET_BOOST + MINERAL_SUPERCHARGE) → +25% giá trị khoáng vật.
3. **Hút máu** (VAMPIRE_BOOSTER + bất kỳ booster gây damage, trừ CHAIN_LIGHTNING
   — theo note có sẵn trong `Ship.kt` về phạm vi lifesteal chỉ tính laser hit
   trực tiếp) → +10% lifesteal.

Cơ chế: **banner thông báo** (không chỉ tự động âm thầm), hiện 1 lần/run lúc
synergy lần đầu active. Bonus: **continuous while both active** — không phải
"dùng 1 lần" — cứ cả 2 điều kiện đúng thì bonus áp dụng, kiểm tra tại điểm đọc
tương ứng (damage multiplier / mineral bonus / lifesteal multiplier).

**Vấn đề kỹ thuật phát sinh + cách xử lý:** MINERAL_SUPERCHARGE là hiệu ứng
one-shot tức thời (không có duration/active-window như đa số booster khác),
xung đột với rule "continuous while both active" của cặp 2. Xử lý: kiểm tra
MAGNET_BOOST có active hay không **tại đúng thời điểm** SUPERCHARGE fire (thay
vì theo dõi liên tục 2 điều kiện) — cách diễn giải thực dụng, không hỏi lại
user.

## Slice 1 — logic phát hiện + bonus (✅ DONE)
- Cặp 1: `GameState.kt` — `damageMultiplier` lambda, thêm `shieldSynergyMul`
  (1.15f khi `ship.shieldEnabled && weaponBoosterActive`).
- Cặp 2: `MineralsController.flushAllToShip()` — `isMagnetBoostActive: () ->
  Boolean` lambda param, bonus ×1.25 khi magnet active lúc supercharge fire.
- Cặp 3: `ShipController.applyVampireHeal()` — lifesteal 0.5f → 0.6f khi có
  dmg booster khác active (berserk/critSurge/spreadShot/doubleFire).

## Slice 2 — UI thông báo synergy (✅ DONE)
- `SynergyKind` enum (`ui/game/ship/ship/SynergyKind.kt`).
- `SynergyBanner.kt` (`ui/game/controls/`) — theo pattern `RevivedBanner`
  (stringResource, không hardcode text như `WaveClearBanner`), pop-in/hold/
  fade 1.8s giống `WaveClearBanner`.
- Trigger "1 lần/run": edge-detect gate trong `ShipController.monitorShipCollisions()`
  (cặp 1, 3 — continuous check mỗi ~100ms) + gate riêng trong `GameState.kt`
  tại `mineralSuperchargeRef.run` (cặp 2 — event-triggered).
- 6 string resource mới (title+desc ×3) thêm cả `values/`, `values-vi/`,
  `values-en/`.
- Wire `SynergyBanner(...)` vào `GameScreen.kt` cạnh `AchievementBanner`.

## Slice 3 — cân bằng + verify (✅ DONE)
- `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`
  → BUILD SUCCESSFUL, tests pass.
- Playtest trên device thật (SM-S928B, `R5CX613VZBR`): tạm tăng `weight` của
  8 loại booster liên quan lên 300 trong `BoosterType.kt` (đã revert sau khi
  xong, xem note dưới) để ép rơi thường xuyên hơn, dùng `uiautomator dump`
  lấy tọa độ tap chính xác thay vì đọc trực tiếp trên screenshot đã scale.
  - **SHIELD_WEAPON**: xác nhận đầy đủ qua `adb logcat` —
    `Logger.d("Synergy activated: SHIELD_WEAPON")` bắn ra đúng lúc khiên +
    vũ khí mạnh cùng active. Xác nhận cả pipeline detection → callback →
    state-set hoạt động đúng trên thiết bị thật.
  - **MAGNET_SUPERCHARGE**: xác nhận cơ chế nền `flushAllToShip` chạy đúng
    (log `no minerals on screen` khi không nhặt gì, chạy bình thường khi có),
    nhưng chưa bắt được đúng thời điểm magnet active lúc supercharge fire để
    log riêng nhánh synergy (×1.25) trong các lượt chơi quan sát được.
  - **VAMPIRE_DMG**: chưa quan sát được lần kích hoạt nào trong các lượt chơi
    (dù cả VAMPIRE_BOOSTER và BERSERK đều nằm trong tập 8 loại được tăng
    weight) — cần thêm thời gian chơi hoặc chiến lược nhặt có chủ đích hơn để
    xác nhận nếu muốn full coverage.
  - Đã **revert `BoosterType.kt`** về weight gốc, rebuild
    (`compileDevDebugKotlin compileProductionReleaseKotlin
    testDevDebugUnitTest` → BUILD SUCCESSFUL, `BoosterTypeTest` pass với
    weight gốc) và `installDevDebug` lại lên device.

## Slice 4 — eyeball device (✅ DONE, một phần suy luận)
- `SynergyBanner` chưa chụp được trực tiếp lúc hiện trên màn hình (banner
  chỉ hiện ~1.8s, chưa canh trùng được screenshot/screen-recording với thời
  điểm fire trong các lượt quan sát được).
- Rủi ro đánh giá **thấp**: composable tái dùng nguyên pattern render/wiring
  đã verify của `RevivedBanner`/`WaveClearBanner` trong cùng overlay stack
  (`GameScreen.kt`), không có logic mới về vị trí/animation.
- Không có ad che UI trong bất kỳ screenshot nào chụp được (R4 — dev flavor
  tắt ad).

## Trạng thái
✅ **Done** — code xong, build/test xanh, SHIELD_WEAPON xác nhận log đầy đủ
trên device thật; MAGNET_SUPERCHARGE xác nhận cơ chế nền; VAMPIRE_DMG và
xác nhận trực quan banner còn dựa vào suy luận pattern-reuse (rủi ro thấp,
không cần thêm playtest kéo dài do lợi ích giảm dần).
