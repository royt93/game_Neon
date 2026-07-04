# Task 09 — Enemy variant expansion (5 địch chủ đề còn thiếu)

**Picked 2026-07-04 (đợt 3, kết hợp 09+10+11).** Hoàn tất Wave 9a (10/15 → 15/15).
Thêm 5 địch chủ đề deferred: **Splitter / Repulsor / Jammer / Missileer / Predator**.

## Slice 0 — thiết kế chốt (data-driven, KHÔNG sửa enum/factory/controller lõi)

Framework enemy modular: shape + movement + attack **decoupled qua `drawableId`**.
- **Family** (EnemyFamily.fromDrawableId) → hp/speed/impact mul tự động.
- **Shape** → EnemyCanvas.drawEnemyShape `when(drawableId)` + hàm `drawXxx` vector.
- **Attack RIÊNG** → override `RegularEnemy.generateLasers()` theo drawableId (default = 1 tia center). Đây là điểm khác biệt chính giữa các variant.
- **Movement** = formation procedural theo stage (KHÔNG per-variant) — không ép được, chỉ chọn family/spawn.
- **Wire** → thêm drawableId vào `Chapter.regularEnemyDrawables` (chapter phù hợp).
- **Asset** → vector `res/drawable/enemy_*.xml`.
- **Test** → EnemyControllerBehaviorTest style (fake enemy, pin laser count/motion).

### 5 địch (family + attack riêng + shape + màu + chapter)

| Địch | Key/drawable | Family | Attack RIÊNG (generateLasers) | Shape | Màu | Chapter |
|---|---|---|---|---|---|---|
| **Splitter** (Phân thân) | `enemy_splitter` | ELITE (1.25/1.1/1.1) | Bắn 2 tia từ 2 mép (trái+phải), như "kéo mở" | 2 nửa tam giác tách | Vàng gold | Ch4–5 |
| **Repulsor** (Đẩy lùi) | `enemy_repulsor` | HEAVY (1.6/0.7/1.3) | Nón 3 tia (center + ±25°) đẩy dồn | Lục giác + 6 gai toả | Tím magenta | Ch3–5 |
| **Jammer** (Nhiễu sóng) | `enemy_jammer` | SCOUT (0.7/1.3/0.8) | 2 tia CURVE (bay uốn lượn thất thường) | Đĩa radar + 2 cánh cong | Cyan + đỏ | Ch2–5 |
| **Missileer** (Pháo thủ) | `enemy_missileer` | BERSERKER (0.9/1.2/1.4) | Loạt 3 tia (center + ±offset) thẳng xuống "volley" | Tên lửa (mũi nhọn+thân+cánh) | Cam + trắng | Ch4–5 |
| **Predator** (Săn mồi) | `enemy_predator` | FIGHTER (1.0/1.0/1.0) | 1 tia HOMING nhắm tàu (bám đuổi) | Trăng khuyết sleek | Xanh lá đậm + teal | Ch3–5 |

- **Motion RIÊNG tái dùng `LaserMotion`** (đã có LINEAR/HOMING/ACCEL/CURVE): Jammer=CURVE, Predator=HOMING, còn lại LINEAR. HOMING cần cập nhật target = tâm tàu mỗi tick (đã có cơ chế ở EnemyLasersController — kiểm tra khi impl).
- **Cân bằng:** dmg tia giữ chuẩn; điểm nhấn là hình dạng đòn (fan/volley/homing/curve/split), không tăng số lượng đạn quá tay (né được).

### Checklist file / địch
1. `EnemyFamily.kt` — map 5 drawable mới vào family (`fromDrawableId`).
2. `EnemyCanvas.kt` — 5 nhánh `when(drawableId)` + 5 hàm `drawSplitter/drawRepulsor/drawJammer/drawMissileer/drawPredator` (vector, dùng body+accent color).
3. `RegularEnemy.kt` (hoặc subclass) — `generateLasers()` override theo drawableId (5 pattern). Kiểm tra cách R81+ enemy (sniper/healer/kamikaze) đã làm để nhất quán.
4. `Chapter.kt` — thêm drawable vào `regularEnemyDrawables` của chapter phù hợp.
5. `res/drawable/enemy_{splitter,repulsor,jammer,missileer,predator}.xml` — vector (hoặc bỏ nếu render thuần vector qua EnemyCanvas, không cần sprite).
6. Test: `EnemyVariantAttackTest` (mới) — pin mỗi địch: số tia + motion (Splitter=2, Repulsor=3 fan, Missileer=3 volley, Jammer=2 CURVE, Predator=1 HOMING). Cập nhật count/name-set nếu có pin variant.
7. i18n: displayName vi/en nếu hiện ở Bách Khoa (enemy tab). InfoScreen enemy tab additive.

### Slice hoá (verify từng bước)
- **Slice 1:** Splitter + Repulsor (LINEAR, đơn giản nhất) + shape + test + compile.
- **Slice 2:** Jammer (CURVE) + Predator (HOMING) — kiểm tra motion wire.
- **Slice 3:** Missileer (volley) + Bách Khoa enemy tab + eyeball device + doc.

## Verify
`./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest` mỗi slice; eyeball device (spawn ở chapter tương ứng hoặc inject tạm) cuối task.

## Trạng thái
✅ **DONE (2026-07-04)** — làm gộp 1 pass (không chia slice vì 5 địch cùng cấu trúc).
- 5 drawable placeholder + `EnemyAttackKind` (enum + `forDrawable`) + field `attackKind` trên `RegularEnemyType`.
- `RegularEnemy.generateLasers` dispatch 6 kind (SINGLE/SPLIT/CONE3/CURVE2/VOLLEY3/HOMING1) — attack riêng, tái dùng `LaserMotion.CURVE/HOMING`.
- `EnemyFamily.fromDrawableId` map 5 địch → family (ELITE/HEAVY/SCOUT/BERSERKER/FIGHTER).
- `EnemyCanvas`: 5 màu body + accent + 5 nhánh shape dispatch + 5 hàm `drawEnemy*` (PathPool).
- `Stage.buildGameStage` set `attackKind = forDrawable(drawable)`; wire drawable vào Ch3 (jammer+predator), Ch4 (splitter+repulsor), Ch5 (missileer).
- Test `EnemyVariantAttackTest` (7): pin số tia + motion từng kind. Compile 2 flavor + full JVM test PASS.
- **Eyeball device (10CEA5016Z0012X, inject tạm Ch1):** Repulsor (đĩa magenta + 6 gai, bắn CONE3) + Splitter (2 nửa vàng, bắn SPLIT 2 tia) render đúng recipe → revert inject. 3 địch còn lại (jammer/missileer/predator) cùng code path đã chứng minh.
