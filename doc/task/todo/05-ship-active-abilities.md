# Task 05 — Kỹ năng chủ động theo tàu

**Loại:** Feature mới (gameplay depth) · **Ưu tiên:** cao (synergy Task 03) · **Trạng thái:** 🟡 In progress (Slice 0 chốt 2026-07-04) — EPIC nhiều slice

## ✅ Slice 0 — ĐÃ CHỐT (2026-07-04)
- **22 skill RIÊNG** (map 1-1 với 22 ShipShape).
- **Hiệu ứng MỚI** (không tái dùng booster buff) — 10 primitive `AbilityEffect`: NOVA/OVERDRIVE/BULWARK/PHASE_DASH/REPAIR/TIME_DILATION/LASER_STORM/MAGNET_PULSE/DECOY/CRIT_FRENZY; mỗi tàu 1 ability tên/tham số riêng.
- **Cooldown giảm theo LEVEL tàu** (Task 03): CD thực = base × `ShipXpLevels.cooldownMulForLevel` (L1 1.0 → L5 0.6).
- Nút HUD riêng (mẫu `SecondaryWeaponButton` — có vòng cooldown).

## Rã slice
- [x] **Slice 1 (2026-07-04)** — khung: `AbilityEffect` (10) + `ShipAbility` (22, map 1-1 `forShip`) + cooldown/level + `ShipAbilityController` (pure ready/activate/progress/remaining) + `ShipAbilityTest` **6 test**.
- [x] **Slice 2 (2026-07-04)** — wire GameState (`shipAbility`/`abilityCooldownProgress`/`activateAbility`, `RunContext.shipLevel` cho cd/level) + HUD `AbilityButton` (glyph + vòng cooldown magenta) + GameScreen (stack trên secondary). Activation → dispatch (log; effect Slice 3+). **Verify device**: nút ✸ hiện in-game, tap → logcat `ShipAbility activated: BUNG_NO effect=NOVA cd=16000ms`. JVM 874→880.
- [x] **Slice 3 (2026-07-04) — HIỆN THỰC 10 EFFECT.** Instant (world-level trong `activateAbility`): NOVA (nổ lan toàn màn + explosion), REPAIR (`healCapped` 35% maxHp), MAGNET_PULSE (`flushAllToShip`), LASER_STORM (6 loạt `fireLasers`). Duration (timer MỚI trên Ship, đọc lazy): OVERDRIVE (×1.6 dmg), CRIT_FRENZY (×2.2 dmg), BULWARK/PHASE_DASH/DECOY (invuln — damage path coi như khiên → 0 dmg), TIME_DILATION (đóng băng địch — OR vào freeze gate). `ShipController.activateAbilityTimer` set 4 timer ability trên Ship (khác field booster buff). Test: `ShipAbilityTimerTest` **5**. **Verify device**: FIGHTER NOVA → màn sạch địch + nút vào cooldown. JVM 880→885.

## Trạng thái: ✅ Implemented (Slice 1–3, 2026-07-04) — 22 skill + 10 effect + verify device
- i18n: tên/mô tả/glyph hardcode VN trong enum (đúng pattern ShipShape).
- Ghi chú: duration effect dùng timer RIÊNG trên Ship (không tái dùng field booster buff shieldEnabled/berserk…); một số cơ chế nền (freeze gate, damage-mul, invuln-as-shield) chia sẻ đường dẫn hạ tầng — hợp lý về kỹ thuật, tránh dựng 6 hệ song song.

## Mục tiêu
Mỗi `ShipShape` (21 tàu) có **1 kỹ năng chủ động** bấm-kích-hoạt (cooldown), làm 21 tàu khác biệt thật sự thay vì chỉ khác stat mul. Mở/nâng theo **level tàu** (tái dùng `ShipXpLevels` của Task 03).

## Quyết định cần chốt (Slice 0)
- [ ] Số skill: mỗi tàu 1 skill riêng, hay gom ~5 archetype skill (Tấn công/Phòng thủ/Cơ động/Hồi/Đặc biệt) map theo tàu? (đề xuất: archetype để cân bằng dễ + ít UI).
- [ ] Cổng mở: có sẵn khi sở hữu tàu, hay mở ở level X? (đề xuất: L1 có skill, L3/L5 giảm cooldown).
- [ ] Nút kích hoạt: thêm nút HUD riêng (cạnh SmartBombButton) hay dùng cử chỉ?
- [ ] Cooldown + thời lượng: giá trị mặc định (đề xuất CD 12–20s).

## Điểm tích hợp (code thật)
- `ui/game/ship/shape/ShipShape.kt` — thêm field `ability: ShipAbility` (enum mới).
- `ui/game/controls/SmartBombButton.kt` — mẫu nút HUD cooldown để nhân bản `AbilityButton`.
- `GameState.kt` — state cooldown + tinker kích hoạt (KHÔNG coroutine loop mới); áp hiệu ứng qua ShipController/EffectiveStats tạm thời.
- `ShipXpLevels` (Task 03) — gate/nâng theo level.
- Logic thuần `ShipAbilityController` (cooldown/ready/activate) → unit test.

## Rủi ro
- Cân bằng 21 skill (nếu per-tàu). Đụng GameState (vùng nhạy). Làm từng slice, verify.

## Test
- Unit: cooldown/ready/activate (thuần). Widget: nút skill hiển thị cooldown. Integration: kích hoạt in-game (device).
