# Task 10 — Mineral sink: Prestige Reset

**Picked 2026-07-04 (đợt 3).** User chọn **Prestige Reset** — sink VÔ HẠN hút khoáng dư + progression sâu.

## Slice 0 — thiết kế chốt

**Ý tưởng:** Reset toàn bộ skill-tree (mọi `node_*` về 0) để đổi lấy **1 cấp Prestige** = buff vĩnh viễn cộng dồn (+X% tất cả stats). Giá reset tăng theo số lần đã prestige (exponential) → luôn có chỗ tiêu.

### Số liệu (chốt)
- **Buff/level:** +4% tất cả stat chính (HP, damage, magnet, speed) mỗi cấp prestige. Cộng dồn (level 3 = +12%). Không trần cứng nhưng EffectiveStats vẫn `coerceIn` an toàn.
- **Cost:** `prestigeCost(level) = 1500 × 2^level` (level 0→1: 1500◇, 1→2: 3000, 2→3: 6000…). Exponential = sink vô hạn.
- **Điều kiện:** phải có tổng rank skill-tree ≥ ngưỡng (vd ≥ 10 rank đã mua) để reset có ý nghĩa (tránh prestige lúc cây trống). Chốt: **yêu cầu ≥ 8 rank tổng**.
- Reset **CHỈ** xoá `node_*` (skill-tree), KHÔNG đụng shop unlock (skin/bullet), stockpile, lifetime_minerals, achievements.

### Data (MetaProgressionRepository)
- Key mới: `prestige_level` (intPreferencesKey).
- `prestigeLevel: Flow<Int>`.
- `prestigeMultiplier(): Float` thuần = `1f + 0.04f × level` (hoặc hàm pure trong module-layer để test).
- `prestigeCost(level): Int` thuần (pure, tested).
- `suspend fun doPrestige(currentLevel, cost, totalRanks): Boolean` — atomic edit: check `lifetime_minerals ≥ cost` && `totalRanks ≥ 8`, trừ cost, xoá mọi key prefix `node_`, `prestige_level += 1`. Trả false nếu không đủ.
- `totalSkillRanks: Flow<Int>` (sum allRanks của key `node_*`) — cho gate UI.

### Áp buff vào gameplay
- `EffectiveStats` / `RunContext`: nhân thêm `prestigeMul` vào hpMul/damageMul/magnetMul/speedMul (đọc prestigeLevel lúc build run context, như shipLevel Task 03). Giữ `coerceIn` hiện có.
- Đọc prestige ở nơi build RunContext (GameScreen/GameState) giống shipLevelHpMul.

### UI (DialogMetaUpgrade — skill-tree screen)
- Header/nút "⭐ Prestige (Lv N · +M%)" + phụ đề cost + điều kiện.
- Bấm → `PurchaseConfirmSheet` (tái dùng) cảnh báo RÕ "sẽ XOÁ toàn bộ skill-tree, đổi +4% vĩnh viễn". Chỉ Xác nhận mới reset.
- Disable/nhả xám khi `totalRanks < 8` hoặc `lifetime_minerals < cost`.
- i18n vi + en.

### Test
- `PrestigeLogicTest` (JVM pure): prestigeCost exponential đúng (1500/3000/6000…), prestigeMultiplier (+4%/lvl), gate (ranks<8 → false).
- `MetaProgressionPrestigeIntegrationTest` (Robolectric, như ShipXp): doPrestige trừ đúng + xoá node_* + tăng level + không đụng shop/stockpile/lifetime; thiếu tiền/ranks → false, state nguyên.
- `EffectiveStatsTest`: prestigeMul nhân đúng + coerce.
- Cập nhật MetaProgressionKeysTest (key mới).

### Slice hoá
- **Slice 1:** Repo (key + flows + doPrestige + pure funcs) + test JVM/Robolectric.
- **Slice 2:** EffectiveStats/RunContext wire + test; verify buff áp dụng.
- **Slice 3:** UI DialogMetaUpgrade + confirm sheet + i18n + eyeball device.

## Audit → 9.5 (2026-07-04)
- **(a) Prestige buff thoát cap** — `EffectiveStats.compute`: base stat coerce như cũ RỒI nhân `pMul` NGOÀI, cap cuối rộng (hp/spd/mag 6.0, dmg/score 8.0). Trước pMul nằm trong coerce → player maxed chạm trần, prestige vô nghĩa; nay reward không bão hoà. `pMul=1` ⇒ giá trị hệt cũ.
- **(b) Gồm scoreMul** — prestige nay nhân cả score → card "+4% mọi chỉ số" đúng nghĩa.
- **Test bổ sung:** +4 unit (`EffectiveStatsTest`: escape-cap hp, gồm score, backward-compat pMul=1, cap cuối 6.0 — JVM PASS) · +4 widget (`PrestigeCardWidgetTest`: render theo state + click enabled/disabled; `PrestigeCard`→`internal`) · +1 integration (`PersistenceRoundtripTest.prestige_wipes_skilltree_keeps_shop...` — DataStore thật). **Verify device: `connectedDevDebugAndroidTest` 25/25 PASS ×2 (SM-S928B)** — gồm widget + integration prestige mới.
- Nit sửa: androidTest method name dùng underscore (D8 không cho space trong method name khi dex; JVM tier mới được backtick-space).

## Trạng thái
✅ **DONE (2026-07-04)**.
- **Slice 1 (repo):** `prestige_level` key + `SHOP_NODE_INFIX` (loại `node_shop_*` khỏi reset) + pure funcs `prestigeCost`(1500×2^lvl)/`prestigeMultiplier`(+4%/lvl)/`canPrestige`/`PRESTIGE_MIN_RANKS`=8 + flows `prestigeLevel`/`totalSkillRanks` + `doPrestige(cost)` atomic (xoá skill-tree, GIỮ shop/stockpile/XP, +1 level). Test: `PrestigeLogicTest` (5) + `MetaProgressionPrestigeIntegrationTest` (4 — xoá skill giữ shop, thiếu rank/tiền no-op).
- **Slice 2 (wire):** `RunContext.prestigeMul` + `EffectiveStats.compute` nhân pMul vào hp/damage/speed/magnet (coerce giữ nguyên); GameState đọc `prestigeLevel` 1 lần → `prestigeMultiplier`.
- **Slice 3 (UI):** `PrestigeCard` đầu tab Nâng cấp (ShopUpgradeContent) — cấp + buff + cost + gate; bấm → `PurchaseConfirmSheet` (confirm = doPrestige). Nhả xám khi chưa đủ.
- Compile 2 flavor + full JVM test PASS. **Eyeball device (10CEA5016Z0012X):** card hiện đúng "⭐ Prestige · cấp 0 · +0% mọi chỉ số", gate đỏ "Cần ≥ 8 rank (đang 0)", 1500◇ — disabled chính xác.
