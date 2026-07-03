# Task 04 — Boss Dialogue + Stage Narrative + Win Epilogue

**Loại:** Lore/content (enhance) · **Ưu tiên:** thấp-trung bình (rủi ro code thấp) · **Trạng thái:** 📋 todo

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
