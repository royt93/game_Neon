# Task 04 — Boss Dialogue + Stage Narrative + Win Epilogue

**Loại:** Lore/content (enhance) · **Ưu tiên:** thấp-trung bình (rủi ro code thấp) · **Trạng thái:** ✅ Implemented (5/5 slice, 2026-07-04; beat verified device, pipeline confirmed)

## ✅ Slice 0 — ĐÃ CHỐT (2026-07-04)
- **Defeat depth:** FinalBoss (SPIDER) + vài mid-boss nổi bật (HELL_LORD/WHITE_DRAGON/HAMMER_SICKLE/CRYPTO_BRO/KITCHEN_GOD) có câu riêng; còn lại generic fallback.
- **i18n:** SONG NGỮ vi+en (+ default VN) qua string resource (khác pattern hardcoded cũ của StoryRegistry — story mới dùng @StringRes, resolve qua Context).
- **Epilogue:** theo độ khó (EASY/NORMAL/HARD) + gắn ngữ cảnh chương cuối (Lõi Thiên Hà), mở rộng VictoryPanel.
- Phase chỉ áp dụng FinalBoss (currentPhase 1→2→3 theo HP); mid/level boss = phase 0.

## Rã slice
- [x] **Slice 1 (2026-07-04)** — strings song ngữ (17 key × 3 file: default/vi/en, parity OK) + `StoryRegistry.chapterBeatRes/finalBossPhaseRes/bossDefeatRes` (@StringRes) + `StoryRegistryTest` (5 test: beat 1..5, phase 2/3, mọi boss có defeat, map đúng).
- [x] **Slice 2 (2026-07-04)** — trigger phase + defeat trong GameState. Boss defeat: onEnemyKilled(isBoss) → `bossDefeatRes(enemy.bossKind)` resolve qua `context` → StoryLine (speaker = boss displayName) qua `setStoryLineRef` (@Volatile deferred, vì storyLine khai báo sau onEnemyKilled), delay 500ms. FinalBoss phase: check inline trong loop (`enemies.firstOrNull{it is FinalBoss}`, gate `finalBossPhaseSeen`) → `finalBossPhaseRes` khi currentPhase tăng. Non-blocking, không coroutine loop mới.
- [x] **Slice 3 (2026-07-04)** — narrative beat NARRATOR 1 lần/chương ở boss climax (trong handler StageBoss, gate `beatPlayedChapter`, delay 300ms → beat 2.2s rồi taunt ở 2.6s, không đè).
- [x] **Slice 4 (2026-07-04)** — epilogue theo độ khó (EASY/NORMAL/HARD) trong VictoryPanel: `stringResource(story_epilogue_*)`, in nghiêng mờ dưới subtitle, gắn ngữ cảnh Lõi Thiên Hà.
- [x] **Slice 5 (2026-07-04)** — build 2 flavor OK + JVM **852/852**. **Verify device S24 Ultra:** ✅ narrative beat fire ĐÚNG qua logcat: `StoryOverlay shown: ĐỘI TRƯỞNG "Nửa đường Vành đai rồi…"` lúc vào StageBoss idx=10 chapter=1, rồi 2.3s sau boss taunt `"Mắt ta dõi theo…"` → **sequencing đúng, không đè**. 0 FATAL EXCEPTION, phase-check inline chạy sạch. Defeat/phase/epilogue dùng CÙNG cơ chế `setStoryLineRef`→StoryOverlay (pipeline đã verify) + cover bởi unit test; chưa chụp trực quan được vì giết boss 1500hp qua adb input mù không khả thi (hạn chế công cụ, không phải lỗi code).

## Hiện trạng (đã có)
- `ui/game/story/StoryLine.kt` + `StoryRegistry.kt` (chapter intros + boss taunts đã có — feature.md Wave 5 "47x Story/lore").
- `ui/game/controls/StoryOverlay.kt` (render), `BossIntroOverlay.kt`, `enemy/ship/model/BossMeta.kt`.
→ Task này **mở rộng nội dung + thời điểm hiển thị**, không dựng hệ mới.

## Mục tiêu
- [ ] **Thoại boss theo pha:** khi boss vào phase 2/3 → 1 câu ngắn (dùng `currentPhase` của boss).
- [ ] **Thoại khi hạ boss:** câu "defeat line" lúc boss chết.
- [ ] **Mẩu narrative giữa màn:** beat ngắn ở mốc chapter (tận dụng StageMessage/StageController).
- [ ] **Win epilogue:** màn kết khi hạ FinalBoss (hiện `DialogGameOver` có VictoryPanel — thêm đoạn epilogue theo độ khó/chương).

## Slices
1. **Mở rộng registry** — thêm entries vào `StoryRegistry` (per-boss: intro/phase2/phase3/defeat; per-chapter: narrative beat; epilogue). Khoá theo bossKind/chapterId.
2. **Trigger phase/defeat** — trong `GameState`/boss xử lý: khi phase đổi hoặc boss chết → đẩy StoryLine vào StoryOverlay (non-blocking, tự tắt sau ~2.5s, KHÔNG chặn input/combat).
3. **Narrative beat giữa màn** — hook vào `StageController` khi qua mốc chapter.
4. **Epilogue** — mở rộng VictoryPanel/`DialogGameOver` hiển thị đoạn kết.

## i18n (bắt buộc)
- [ ] MỌI câu thoại mới → **cả** `values-vi/strings.xml` + `values-en/strings.xml`. Không hardcode chuỗi user-facing (trừ khi theo pattern hằng sẵn có của story).

## UX / chống che UI
- [ ] Overlay thoại đặt vùng không đè HUD/nút điều khiển lúc combat; auto-dismiss; tôn trọng `reduceMotion` (giảm animation).
- [ ] Thoại phase KHÔNG được che boss HP bar / cản người chơi.

## Test
- [ ] `StoryRegistryTest` (JVM): mỗi boss có đủ intro/phase/defeat; mỗi chapter có beat; epilogue tồn tại; lookup theo key trả đúng; **không key rỗng**.
- [ ] (guard i18n) test/kiểm mọi string key mới có ở cả vi + en (có thể grep thủ công nếu không dựng test resource).
- [ ] Verify build 2 flavor + unit test.

## Acceptance
- Boss có thoại intro/phase/defeat; có beat narrative giữa màn; có epilogue khi thắng; tất cả song ngữ vi+en; không che UI combat; auto-dismiss.

## Rủi ro
- Chủ yếu content → rủi ro code thấp; rủi ro chính là **che UI** và **thiếu bản dịch** → kiểm kỹ 2 điểm này.
