# Task 19 — Color blind mode: migrate toàn bộ UI (enhance)

**Picked 2026-07-11** qua AskUserQuestion (nhóm "enhance tính năng cũ" — chọn cả 3,
task 3/3). Round 39 đã dựng hạ tầng `LocalNeonPalette` + Wong palette + Settings
picker, nhưng phần lớn dialog/HUD vẫn hardcode `Color(...)`/`NeonXxx` thay vì đọc
palette — toggle "Mù màu" không đổi được màu ở đa số màn hình.

## Slice 0 — audit danh sách file cần migrate (DONE)
- Sửa 2 giả định sai trong bản draft ban đầu: `NeonPalette` chỉ có **5 field**
  (`cyan/magenta/violet/gold/redAlert`), và `ColorBlindMode` chỉ có **2 giá trị
  thật** (`NORMAL`, `COLORBLIND_SAFE`) — không phải 3 kiểu Protanopia/Deuteranopia/
  Tritanopia như task doc gốc đoán.
- Grep đầy đủ `ui/dlg/*` + `ui/game/controls/*` → chốt danh sách **30 file** cần
  migrate (7 dlg + 23 controls), tất cả chỉ dùng 5 màu palette nêu trên.
- Loại khỏi scope có chủ đích: `DamageTearOverlay.kt`/`HazardOverlay.kt` (màu
  glitch/vignette trang trí, không khớp hex nào trong 5 màu palette) và
  `ui/game/world/GameWorld.kt` (layer render entity — comment gốc trong
  `Color.kt` đã ghi rõ đây là scope loại trừ có chủ đích từ round 39: "game
  entity bitmaps + nhiều bề mặt trang trí không cần đổi màu").

## Slice 1+2 — migrate HUD/controls + dialog (DONE)
- Pattern áp dụng nhất quán: composable nào dùng `NeonCyan/Magenta/Violet/Gold/
  RedAlert` → thêm `val palette = LocalNeonPalette.current` đầu hàm, thay từng
  hằng số bằng `palette.xxx`, xoá import chết.
- Helper không phải `@Composable` (DrawScope extension, hàm thuần, lambda enum)
  → không đọc `.current` trực tiếp được, thêm tham số `color`/`palette` truyền
  từ nơi gọi (`SecondaryWeaponButton.drawWeaponIcon`, `StageBanner.pickColor`).
- Helper `@Composable` riêng tư (StatBarsRow, PowerUpBadge...) → tự đọc
  `LocalNeonPalette.current` của mình, không cần truyền tham số — đơn giản hơn,
  diff nhỏ hơn.
- Case đặc biệt `BossRankOverlay.kt`: enum `BossRank.color` giữ nguyên hardcode
  (vì `StatsScreen.kt`'s `RankBar` cũng dùng, ngoài scope task này) — thay vào
  đó overlay tự map `when(rank) { ... palette.xxx }` cục bộ.
- 30 file gốc migrate qua 4 batch (agent), cộng thêm phát hiện giữa chừng
  `DialogSettings.kt` chỉ migrate DỞ DANG từ task trước (9 chỗ còn sót:
  `accentColor`, các `SectionPanel`/slider color, checkbox `SettingCheck`) —
  sửa trực tiếp, không qua agent.
- `IndicatorStatus.kt`: phát hiện thêm 2 chỗ hardcode `Color(0xFFB14CFF)` (trùng
  hex `NeonViolet` dù không import theo tên) — cũng thay bằng `palette.violet`.

## Slice 3 — verify không quên case nào (DONE)
- Sweep cuối: `grep -rn "NeonCyan\|NeonMagenta\|NeonViolet\|NeonGold\|NeonRedAlert"`
  trên `ui/dlg` + `ui/game/controls` → rỗng (0 residual).
- `./gradlew compileDevDebugKotlin compileProductionReleaseKotlin testDevDebugUnitTest`
  — BUILD SUCCESSFUL, toàn bộ unit test pass, không regression.
- Không cần test mới: cơ chế đọc/ghi `ColorBlindMode` đã có từ round 39, task
  này thuần migrate consumer, không thêm data-layer surface.

## Slice 4 — eyeball device (DONE)
- Device: Pixel 7 Pro (`2B051FDH3006MU`) — máy duy nhất kết nối, dùng trực tiếp
  theo ngoại lệ 1-thiết-bị của R3.
- Phát hiện task doc gốc sai vị trí toggle: "Chế độ màu" nằm ở **Cửa hàng → tab
  Hiển thị**, không phải Dialog Settings (đã dời trong 1 refactor trước đó,
  "Wave 13a slice E").
- Bật "Mù màu" → verify trực quan:
  - `DialogSettings.kt`: header, "Âm thanh", "Chơi", "Độ khó", "Tầm nhìn" đều đổi
    đúng tông `COLORBLIND_SAFE` (cyan→xanh da trời nhạt, magenta→cam,
    gold→vàng thuần).
  - Vào 1 lượt chơi thật (Ch.1 St.4/5): HUD (`IndicatorStatus` — thanh HP, viên
    khoáng, các nút kỹ năng) đổi màu đúng, không còn tông neon rực gốc.
  - `DialogGameOver.kt` (khi ship chết tự nhiên trong lúc verify) cũng đổi đúng
    màu (header/badge/stats/2 nút CTA).
- Gặp lại hiện tượng app tự văng sang app khác (`com.galaxyjoy.pop_star_blast`,
  "Pop Star Blast") **3 lần** trong lúc verify — không phải quảng cáo trong
  Neon nên R4 không áp dụng; mỗi lần recover bằng
  `adb shell am start -n com.tranphuloi.neon/.ui.MainActivity` rồi tiếp tục
  bình thường.
- Tự sửa 1 lỗi thao tác: có lúc dùng nhầm toạ độ screenshot-space (432×936)
  làm toạ độ real-device trực tiếp thay vì nhân hệ số ×3.333 (Pixel 7 Pro thật
  1440×3120) — phát hiện qua `dumpsys activity activities`, sửa lại toạ độ
  đúng và tiếp tục.
- Reset "Chế độ màu" về "Tiêu chuẩn" (NORMAL) sau khi verify xong — không để
  lại state test trên máy.

## Trạng thái
✅ **DONE** (2026-07-12) — 30 file gốc + 2 file phát hiện thêm
(`DialogSettings.kt`, `IndicatorStatus.kt`) đã migrate sang `LocalNeonPalette`;
build + unit test pass không regression; verify trực quan trên Pixel 7 Pro xác
nhận dialog + HUD combat + game-over đều đổi màu đúng khi bật "Mù màu"; đã
reset thiết bị về mặc định. `GameWorld.kt` giữ nguyên hardcode có chủ đích
(ngoài scope, đã ghi chú trong `Color.kt`); `BossRank` enum trong
`StatsScreen.kt`'s `RankBar` là candidate follow-up nếu sau này muốn phủ 100%.
