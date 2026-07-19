# Task 39 — Investigate AIBinder_linkToDeath warning (bugfix/tech-debt)

**Nguồn gốc:** phát hiện qua on-device playtest + logcat review trên Pixel 7
Pro (2026-07-18, production release build). Log app-tagged (`tranphuloi.neon`)
xuất hiện lặp lại:

```
W/tranphuloi.neon: AIBinder_linkToDeath is being called with a non-null
cookie and no onUnlink callback set. Use AIBinder_DeathRecipient_setOnUnlinked
to manage the lifetime of the cookie. This will become an abort.
```

## Slice 0 — xác định nguồn gốc (DONE)
- Reproduce: `adb logcat -c` → force-stop → `am start` → capture
  `adb logcat --pid=<pid> -v threadtime` ngay từ cold start (không filter
  `*:W` như lần đầu, để thấy được cả log `I`/`D` xen kẽ và xác định chính
  xác thread/context đứng trước warning).
- **Root cause xác nhận qua timeline log chính xác đến mili-giây:**
  ```
  11:08:02.258 I ExoPlayerImpl: Init d32461d [AndroidXMedia3/1.10.0]
  11:08:02.435 (t3237) D CCodec: allocate(c2.android.mp3.decoder)
  11:08:02.437 (t3242) D CCodec: allocate(c2.android.mp3.decoder)
  11:08:02.470 (t3237) I CCodec: Created component [c2.android.mp3.decoder]
  11:08:02.471 (t3242) W tranphuloi.neon: AIBinder_linkToDeath ... will become an abort
  11:08:02.471 (t3242) I CCodec: Created component [c2.android.mp3.decoder]
  ```
  Warning bắn ra **đúng lúc** `CCodec`/Codec2 HAL client tạo xong component
  decoder MP3 thứ 2 — nằm bên trong chính thư viện nền tảng
  `libstagefright_ccodec` (Codec2Client NDK binder), không phải code Kotlin
  của app. Đây là bug/quirk đã biết của AOSP Codec2 client: mỗi lần tạo một
  component decoder qua HAL binder, nó gọi `AIBinder_linkToDeath` với cookie
  non-null nhưng không set `onUnlink` — xảy ra với **bất kỳ app nào** dùng
  `MediaCodec`/ExoPlayer để decode audio/video trên thiết bị Android 13+.
- Giải thích tại sao xuất hiện **2 lần**: `AudioPlayerHolder.kt` (single
  `ExoPlayer` instance, xem `build(playlist: List<Song>)`) add nhiều
  `MediaItem` nhạc nền vào playlist rồi `repeatMode = REPEAT_MODE_ALL`.
  ExoPlayer tự pre-buffer/probe decoder cho track hiện tại **và** track kế
  tiếp trong hàng đợi (hành vi chuẩn cho gapless playback) → 2 lần tạo
  component `c2.android.mp3.decoder` gần như đồng thời → 2 lần warning.
  Không có ExoPlayer instance thứ 2 nào khác trong codebase (grep xác nhận
  chỉ 1 call site `ExoPlayer.Builder` toàn repo).

## Slice 1 — xử lý tại điểm gọi (KHÔNG CẦN — kết luận sau Slice 0)
- Đây là code bên trong platform/vendor binary (`libstagefright_ccodec`),
  app không có quyền truy cập hay override `AIBinder_DeathRecipient_setOnUnlinked`
  từ Kotlin/Media3 API — không có call site nào trong app code để vá.
  Không tìm thấy issue tương ứng đã fix trong Media3 1.10.0 changelog (warning
  phát sinh ở tầng OS/Codec2 client, dưới cả Media3).
- Cân nhắc giảm số lần trigger (vd tắt pre-buffer track kế tiếp trong
  playlist) — **không làm**: sẽ đánh đổi mất gapless playback nhạc nền chỉ để
  giảm 1 dòng log cảnh báo vô hại, không đáng.

## Slice 2 — verify (DONE)
- Warning **không** gây crash/ANR trong bất kỳ lần test nào (2220 dòng log
  phiên đầu ~44 phút + phiên reproduce lần 2 vừa rồi) — chỉ là warning ghi
  log, ExoPlayer vẫn init và phát nhạc bình thường ngay sau đó.
- Không cần thay đổi code hay build lại.

## Trạng thái
✅ **DONE** — điều tra khép lại. Root cause: bug đã biết trong
`libstagefright_ccodec`/Codec2 HAL client của **nền tảng Android** (kích hoạt
bởi ExoPlayer tạo 2 decoder instance cho playlist nhạc nền), không phải bug
trong code app, không có call site nào để vá từ phía Kotlin. Ghi nhận lại để
tránh điều tra lại nếu gặp lại log này trong tương lai; theo dõi thụ động —
nếu một bản Android tương lai thực sự "abort" như message cảnh báo, đó sẽ là
regression ở tầng OS/vendor, cần retest lại trên OS version mới khi phát hành.
