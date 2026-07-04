# Task 06 — Biến thể drone (skill-tree)

**Loại:** Enhance Task 01 · **Ưu tiên:** trung bình · **Trạng thái:** 🟡 In progress (Slice 0 chốt 2026-07-04)

## ✅ Slice 0 — ĐÃ CHỐT (2026-07-04)
- **3 variant:** ATTACK (bắn, hiện tại), SHIELD (không bắn, 2×HP, chặn/đệm đạn địch), HEAL (không bắn, hồi tàu +1 HP mỗi 0.7s ≈ 1.4 HP/s — thấp, an toàn).
- **Chọn ở loadout:** setting `selectedDroneVariant` — mọi drone spawn theo loại đã chọn.
- **Mở khoá:** DRONE_FLEET (Task 01) mở cả 3 (không thêm node).

## Rã slice
- [x] **Slice 1 (2026-07-04)** — `DroneVariant{ATTACK,SHIELD,HEAL}` + `Drone.variant` + `maxHpFor` (SHIELD ×2) + DroneController (addDrone nhận variant, fireStep chỉ ATTACK, `healStep`) + 4 test (SHIELD 2×HP, only-ATTACK-fires, healStep cooldown, no-HEAL→0).
- [x] **Slice 2 (2026-07-04)** — `SettingsRepository.selectedDroneVariant` + setter + `DroneVariant.fromKey`; GameState đọc variant (1 lần/run) → addDrone; HEAL tick trong loop → `shipController.healCapped`; mapper hpRatio theo maxHpFor + DroneCanvas màu (cyan/gold/green) theo variant.
- [x] **Slice 3 (2026-07-04)** — loadout dialog: section "DRONE" (3 card LoadoutCard) chọn variant → `setSelectedDroneVariant`. Compile 2 flavor + JVM **889/889**. **Verify device Galaxy A50s (SM-A507FN)**: section DRONE render đúng 3 variant (màu cyan/gold/green, glyph ◈/⛨/✚, tên/mô tả), tap "Drone Hồi Máu" → logcat `DroneVariant pick=HEAL` + `setSelectedDroneVariant=HEAL`, ✓ chuyển đúng card. (Hành vi in-game heal/shield/color cover bằng unit test + cần DRONE_FLEET unlock + booster drop ngẫu nhiên để thấy live.)

## Trạng thái: ✅ Implemented (Slice 1–3, 2026-07-04) — verify device (loadout UI + chọn/persist)

## Mục tiêu
Mở rộng drone companion (Task 01) thành **3 biến thể**: TẤN CÔNG (bắn — hiện tại), KHIÊN (chặn/hút đạn địch), HỒI MÁU (hồi HP tàu chậm). Chọn biến thể qua skill-tree / node meta.

## Quyết định cần chốt (Slice 0)
- [ ] Cách chọn biến thể: node skill-tree riêng mỗi loại, hay 1 node "loại drone" xoay vòng? (đề xuất: mở khoá dần, chọn ở loadout).
- [ ] Cân bằng: drone HỒI MÁU hồi bao nhiêu/giây (dễ OP → giữ thấp)? KHIÊN chặn mấy đạn?
- [ ] Có cho mix (1 attack + 1 shield) khi maxDrones=2 không?

## Điểm tích hợp (code thật)
- `ui/game/drone/DroneController.kt` — thêm `DroneVariant` enum + rẽ nhánh hành vi trong fire/collision.
- `ui/game/drone/Drone.kt` — field `variant`.
- `ui/game/drone/DroneCanvas.kt` — màu/hình theo biến thể (cyan attack / gold shield / green heal).
- `EffectiveStats`/`SkillNode` — node meta mở khoá (mẫu `DRONE_FLEET` của Task 01 Slice 5).
- `GameState.kt` — drone HỒI MÁU gọi `shipController.updateHp(+n)`; KHIÊN đánh dấu đạn địch destroyed (đã có `monitorDroneCollision`).

## Rủi ro
- Drone hồi máu dễ phá cân bằng survivability → giữ rate rất thấp + cap.

## Test
- Unit: hành vi mỗi biến thể (heal rate, shield block) thuần. Integration: nhặt + hoạt động (device).
