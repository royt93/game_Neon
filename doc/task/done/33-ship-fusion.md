# Task 33 — Ship fusion (exclusive/signature)

**Picked 2026-07-18** qua AskUserQuestion (nhóm "tính năng độc quyền/khác
biệt"). Kết hợp 2 tàu đã sở hữu (`ShipShape` enum, đã unlock qua
`ShipShopLogic`) thành 1 tàu lai mới với stats/visual kết hợp — tính năng
signature chưa từng có trong game bắn súng cùng thể loại.

## Slice 0 — chốt thiết kế (✅ DONE, 2026-07-30, qua AskUserQuestion)
- Công thức stats fusion: **trung bình cộng** hpMul/speedMul/damageMul của 2
  tàu gốc (`ShipFusion.kt` → `fusedStats()`).
- Visual: **hybrid path-drawing** — nửa trên vẽ tàu A, nửa dưới vẽ tàu B
  (`ShipVector.kt` → `drawFusionShipVector()`, tái dùng dispatch
  `drawShipVector` sẵn có theo shape, tránh bùng nổ tổ hợp C(22,2)=231 cặp).
- Điều kiện fusion: **chỉ cần sở hữu** (`ShipShopLogic.isOwned`), không gate
  theo `ShipXpLevels`.
- Cổng mở khoá: **chỉ theo run, không persist** — chọn lại mỗi lần trong
  Shop (`FusionSession` singleton tiến-trình, không qua DataStore).

**Quyết định phạm vi bổ sung (không thuộc 4 câu hỏi gốc):** `ShipAbility` /
`ShipPassive` / `ShipXpLevels` vẫn khoá theo tàu chính (`ctx.shipShape`) —
fusion chỉ ghi đè 3 hệ số stat cơ bản + visual, không đụng 3 hệ thống trên
(giữ thay đổi tối thiểu).

## Slice 1 — logic tính stats fusion thuần (✅ DONE)
- `ui/game/ship/shape/ShipFusion.kt` — `fusedStats(a, b)` trung bình cộng
  hpMul/speedMul/damageMul. Test: `ShipFusionTest.kt` (averaging, symmetric,
  fuse-với-chính-nó = không đổi).
- `EffectiveStats.compute()` áp dụng `fusedStats(ctx.shipShape, it)` khi
  `ctx.fusionPartner != null`, giữ nguyên logic cap hiện có. Test: 2 case mới
  trong `EffectiveStatsTest.kt`.

## Slice 2 — UI chọn tổ hợp (✅ DONE)
- Mở rộng `ShopScreen.kt` → `ShipTab` (không tạo màn/dialog mới): sau danh
  sách tàu, hiện khu vực "Dung hợp tàu" khi sở hữu ≥2 tàu — chọn 1 tàu khác
  tàu chính làm partner, preview stats fused (HP/Tốc/DMG ×), nút "Bỏ dung
  hợp". Đổi tàu chính tự động bỏ fusion cũ (tránh partner == chính).
- i18n vi+en+default: `ship_fusion_title`, `ship_fusion_preview`,
  `ship_fusion_clear`.

## Slice 3 — persistence (N/A — theo quyết định Slice 0: không persist)

## Slice 4 — wiring vào GameState (✅ DONE)
- `RunContext.fusionPartner`, `GameState.kt` (`rememberRunSetupBundle`) đọc
  `FusionSession.partner` (bỏ qua nếu đang có Ship trial, hoặc partner không
  còn sở hữu, hoặc trùng tàu chính).
- `GameWorld.kt` — cả 2 điểm vẽ tàu (chính + clone booster) đọc
  `FusionSession.partner` trực tiếp (khớp pattern hiện có: visual luôn theo
  `selectedShipShape` sống, không qua `RunContext` đông cứng), gọi
  `drawFusionShipVector` khi có partner.
- Verify: `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`
  → ✅ BUILD SUCCESSFUL, cả 2 flavor compile + toàn bộ unit test pass
  (2026-07-30).

## Slice 5 — eyeball device (✅ DONE, 2026-07-30, thiết bị thật SM-S928B)
- Mua tàu thứ 2 (Oanh tạc) trong Shop, chọn làm partner dung hợp → preview
  đúng công thức trung bình: "HP x1,13 · Tốc x0,95 · DMG x1,05" khớp
  avg(1.0,1.25)=1.13 / avg(1.0,0.9)=0.95 / avg(1.0,1.1)=1.05.
- Vào 1 run: silhouette tàu lai hiển thị đúng — nửa trên cyan/lam (Tiêm
  kích), nửa dưới vàng gold (Oanh tạc), khớp thiết kế hybrid path-drawing ở
  Slice 0.
- Fusion partner giữ nguyên qua điều hướng màn hình (Shop đóng → Menu →
  Tiếp tục → vào lại run vẫn hiển thị tàu lai).
- Bấm "✕ Bỏ dung hợp" trong Shop → preview/partner biến mất → vào lại run
  mới → tàu về đúng silhouette đơn tông màu cyan bình thường (Tiêm kích),
  không còn nửa vàng.
- Không gặp quảng cáo nào trong suốt quá trình test (R4 N/A).
- *Ghi chú kỹ thuật*: để test cần ≥2 tàu sở hữu nhưng thiết bị test chỉ có
  134 khoáng sản (chưa đủ mua tàu thứ 2 giá 200); dùng 1 debug hook tạm thời
  (long-press vào ô số dư trong Shop → cộng 1000 khoáng sản) để mua đủ tàu
  test, sau đó **đã revert hoàn toàn** — không còn trong code đã commit.

## Trạng thái
✅ **CLOSED** — Slice 0-5 xong, build + unit test pass, verify trực quan trên
thiết bị thật thành công (2026-07-30).
