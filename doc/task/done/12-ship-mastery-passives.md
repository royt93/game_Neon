# Task 12 — Ship mastery passives (đợt 4)

**Picked 2026-07-05.** Mỗi tàu (22) mở 1 **PASSIVE độc nhất** khi đạt **max level L5** (1500 XP,
`ShipXpLevels.MAX_LEVEL`). Xây trên Task 03 (XP/level) + mirror pattern Task 05 (ShipAbility).
User chốt **Hybrid**: 5 stat primitive (fold EffectiveStats) + hook runtime đơn giản.

## Slice 0 — thiết kế

### Primitives (`PassiveEffect`)
**Stat (fold vào EffectiveStats — pure testable):**
- `DAMAGE_UP` +% damageMul · `HP_UP` +% hpMul · `SPEED_UP` +% speedMul · `MAGNET_UP` +% magnetMul · `SCORE_UP` +% scoreMul

**Hook runtime (Slice 2):**
- `START_SHIELD` vào trận +N khiên (tái dùng cơ chế buff_start_shield) · `REGEN` +N hp/giây (tái dùng REGEN node) · `LIFESTEAL` diệt địch hồi N hp · `COMBO_KEEP` cửa sổ combo ×2 (tái dùng buff_combo_keep) · `PIERCE_UP` +1 xuyên

### Model (`ShipPassive.kt`, mirror `ShipAbility`)
- `enum PassiveEffect { DAMAGE_UP, HP_UP, SPEED_UP, MAGNET_UP, SCORE_UP, START_SHIELD, REGEN, LIFESTEAL, COMBO_KEEP, PIERCE_UP }`
- `enum ShipPassive(displayName, description, effect, magnitude: Float, glyph)` — 22 entry, map 1-1 ShipShape qua `forShip(shape)` exhaustive.
- `companion.activeFor(shape, shipLevel): ShipPassive?` = `forShip(shape)` nếu `shipLevel >= ShipXpLevels.MAX_LEVEL`, else null (khoá).

### Wire
- **Stat (Slice 1):** `EffectiveStats.compute` resolve `activeFor(ctx.shipShape, ctx.shipLevel)`; nếu effect là stat → nhân bonus vào mul tương ứng (ngoài cap gốc? không — trong cap thường, passive là buff vừa phải). Áp trước prestige cap cuối.
- **Hook (Slice 2):** GameState đọc passive → START_SHIELD (spawn), REGEN (tick), LIFESTEAL (onEnemyKilled), COMBO_KEEP (ComboController window), PIERCE_UP (laser pierce).

### 22 map (dự kiến, tinh chỉnh khi code)
Fighter→dmg, Bomber→dmg, Stealth→speed, Tank→hp, Interceptor→speed, Ngôi sao→score,
Cầu vồng→magnet, Phù thuỷ→lifesteal, Aura glow→regen, Súng 3 nòng→pierce, Obelisk→hp,
Vietnam→score, Diva→magnet, Chết chóc→dmg, Tử thần→lifesteal, Mạng nhện→combo_keep,
Áo giáp thiết→start_shield, Twin domes→pierce, Nhật→speed, Hàn→dmg, Mỹ→hp, Pháp→magnet.

### Test
- `ShipPassiveTest` (pure): 22 map đủ + unique + `activeFor` khoá <L5 / mở =L5; magnitude>0.
- `EffectiveStatsTest`: stat passive áp đúng khi L5, không áp khi <L5.
- Widget: hiển thị passive ở shop ship row (Slice 3).

### Slice hoá
- **Slice 1:** ShipPassive.kt + EffectiveStats stat wire + test (pure + EffectiveStats). Compile.
- **Slice 2:** 5 hook runtime GameState + test.
- **Slice 3:** UI (shop/loadout hiện passive khi L5) + eyeball emulator Pixel 10 Pro XL.

## Trạng thái
🟡 **Slice 1 DONE (2026-07-05)** — enum + stat wire + test.
- `ShipPassive.kt`: `PassiveEffect` (10) + `ShipPassive` (22, map 1-1 `forShip`, exhaustive) + `activeFor(shape, level)` (khoá <L5).
- `EffectiveStats.compute`: 5 stat passive (DAMAGE/HP/SPEED/MAGNET/SCORE) nhân trong cap gốc khi L5. **15/22 tàu passive hoạt động** (stat); 7 tàu còn lại chờ hook Slice 2.
- Test: `ShipPassiveTest` (6: map đủ/độc nhất/khoá/mở/magnitude/có cả stat+hook) + `EffectiveStatsTest` (+3: DAMAGE_UP áp L5, không áp <L5, HP_UP TANK trong cap). Compile 2 flavor + JVM PASS.
- Điểm hook Slice 2 đã định vị: START_SHIELD (GameState:344), COMBO_KEEP (326/529), REGEN (1810), LIFESTEAL (onEnemyKilled 1258), PIERCE_UP (pierceCountForRarity 721).

✅ **Slice 2 DONE (2026-07-05)** — hook runtime trong GameState (đọc `masteryPassive` 1 lần):
- START_SHIELD (GIÁP THÉP) → shield khởi đầu 8s lúc spawn.
- LIFESTEAL (Hút linh hồn/Lưỡi hái) → `healCapped` trong onEnemyKilled.
- REGEN (Hào quang hồi) → `healCapped` trong tick 1s (cạnh REGEN node).
- COMBO_KEEP (Tơ nhện) → cửa sổ combo ×2 (OR với buff giữ combo).
- PIERCE_UP: pierce nằm sâu ở LasersController → **remap 2 tàu (Xuyên phá/Song pháo) sang DAMAGE_UP** (+15%); giữ primitive PIERCE_UP dự phòng.
- **Cả 22 tàu passive hoạt động.** Compile 2 flavor + JVM test PASS.

✅ **Slice 3 DONE (2026-07-05)** — UI: `ShopScreen` ship-row hiện dòng passive magenta khi `maxed` (L5): "`{glyph} {tên}: {mô tả}`". Compile 2 flavor + JVM test PASS.
- **Eyeball emulator Pixel 10 Pro XL:** row Tiêm kích hiện "Lv5 (MAX) · +8% HP" + "Thiện xạ: +12% sát thương vĩnh viễn" (magenta) đúng. (Inject XP bị chặn — production image no root + run-as write denied → dùng temp-hack THRESHOLDS=0 để eyeball, đã REVERT + verify.)

## ✅ TASK 12 DONE (3/3 slice) — 22 mastery passive, unlock L5, verified emulator.
