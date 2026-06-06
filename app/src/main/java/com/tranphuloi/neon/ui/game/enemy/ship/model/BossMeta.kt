package com.tranphuloi.neon.ui.game.enemy.ship.model

/**
 * Wave 25c (refactor nợ kỹ thuật) — gom 4 bảng `when(BossKind)` song song (màu /
 * nhãn chiêu HUD / lời thoại taunt / pitch sting intro) về MỘT bảng dữ liệu duy
 * nhất. Trước đây 1 boss đụng 4 file (EnemyCanvas.bossColorFor, BossHpBar.bossSkillLabel,
 * StoryRegistry.tauntText, GameScreen pitch-when); nay 1 boss = 1 dòng ở đây.
 *
 * KHÔNG phụ thuộc Compose (màu là ARGB Long) để model-layer sạch; `bossColorFor`
 * (render layer) bọc `Color(...)`. Shape vẫn dispatch riêng (`drawBossShapeByKind`)
 * vì gắn DrawScope + hàm vẽ private. `when` exhaustive → thêm boss thiếu = compile error.
 */
data class BossMeta(
    /** ARGB màu nhận diện (đục, alpha=FF). */
    val colorArgb: Long,
    /** Nhãn chiêu ngắn hiện dưới tên trên thanh HP boss. */
    val skill: String,
    /** Lời thoại khi boss xuất hiện (tiếng Việt, in-character). */
    val taunt: String,
    /** Pitch shift sfx_explosion lúc intro (cao = chói, thấp = trầm). */
    val introPitch: Float,
)

fun bossMetaFor(kind: BossKind): BossMeta = when (kind) {
    BossKind.STAR -> BossMeta(0xFFFFD23F, "Vòng tia 8 hướng", "Tiểu tốt vô danh. Không trụ nổi 1 phút đâu.", 1.4f)
    BossKind.CROSS -> BossMeta(0xFFE6E6FF, "Quét trục luân phiên", "Đã nghiền nát phi công mạnh hơn ngươi nhiều.", 1.15f)
    BossKind.ORB -> BossMeta(0xFF00E5FF, "Tia mắt săn đuổi", "Mắt ta dõi theo mọi cử động của ngươi.", 1.0f)
    BossKind.FRACTAL -> BossMeta(0xFF5CFF7A, "Quỹ đạo nguyên tử", "Phá khiên ta đi. Thách đó.", 0.85f)
    BossKind.SPIDER -> BossMeta(0xFF9B5CFF, "Tơ nhện đa hướng", "Ngươi không nên đi xa đến vậy. Kết thúc ở đây.", 0.65f)
    BossKind.DEATH_MOON -> BossMeta(0xFFB0C4DE, "Lực hấp dẫn kéo", "Mặt trăng tử thần đã đến. Hết đường rồi.", 0.75f)
    BossKind.HAUNTED_KID -> BossMeta(0xFFAEEFC0, "Ám khắp đỉnh màn", "Hi hi... chơi với em không?", 1.55f)
    BossKind.HELL_LORD -> BossMeta(0xFFFF5A1E, "Tia mắt địa ngục", "Địa ngục chào đón linh hồn ngươi.", 0.55f)
    BossKind.SATAN_GLYPH -> BossMeta(0xFFB3002D, "Ấn quỷ tỏa", "Ngũ giác đã được vẽ. Linh hồn ngươi là vật tế.", 1.25f)
    BossKind.HEN_MOTHER -> BossMeta(0xFFFFD740, "Ổ trứng rơi chậm", "Cục tác! Ta đẻ trứng cho ngươi đó!", 1.60f)
    BossKind.BUFFALO_RAGE -> BossMeta(0xFF8B4A2F, "Húc sừng gia tốc", "Sừng ta sẽ xuyên qua tàu ngươi!", 0.60f)
    BossKind.DUMB_RAT -> BossMeta(0xFFA0A0A8, "Gặm nhấm thất thường", "Phô-mai... à không, laser! Ta bắn đây!", 1.40f)
    BossKind.FIERCE_TIGER -> BossMeta(0xFFFF8A1E, "Gầm: tường có khe", "Gầm! Hú vang khắp dải Ngân Hà!", 0.70f)
    BossKind.SEXY_DIVA -> BossMeta(0xFFFF4FA3, "Quất tóc bay cong", "Tóc ta lấp lánh, đẹp lắm. Cẩn thận nhé.", 1.20f)
    BossKind.TROLL_TOWER -> BossMeta(0xFF2E8B57, "Tia dọc từ đỉnh", "Cao chót vót, ánh sáng từ đỉnh thần thánh.", 0.85f)
    BossKind.TWIN_SUMMITS -> BossMeta(0xFF1FC8C8, "Tia kép song song", "Hai đỉnh kép, hai tia sữa song hành.", 1.10f)
    BossKind.VOID_GLOBES -> BossMeta(0xFF6A5ACD, "Xé hư vô hình X", "Vô tận hư vô, hứng đòn của ta đi!", 0.65f)
    BossKind.WHITE_DRAGON -> BossMeta(0xFFCFF0FF, "Thét luồng lửa", "Bạch Long Mắt Lam — thét ra lửa!", 0.50f)
    BossKind.HAMMER_SICKLE -> BossMeta(0xFFE02020, "Quăng búa & liềm", "Búa liềm bịp bợm — vinh quang giai cấp!", 0.90f)
    BossKind.MONEY_TYCOON -> BossMeta(0xFF35C759, "Mưa tiền khắp màn", "Tiền là sức mạnh, tàu trẻ con.", 1.05f)
    BossKind.GOLDEN_TYCOON -> BossMeta(0xFFFFC400, "Đô la xoáy ốc", "Tin ta đi, tàu ngươi sẽ chết tuyệt vời nhất.", 1.15f)
    BossKind.SKULL_CROSSBONES -> BossMeta(0xFFE8E2D0, "Quạt xương xoay", "Khặc khặc... xương ngươi sẽ chéo cùng ta!", 0.80f)
    BossKind.VAMPIRE -> BossMeta(0xFFC4123B, "Bầy dơi hút máu", "Ta khát... chỉ một giọt máu tàu ngươi thôi!", 0.55f)
    BossKind.COSMIC_CENTIPEDE -> BossMeta(0xFFB6FF3A, "Phun độc cung rộng", "Trăm chân ta bò khắp Ngân Hà — không thoát đâu!", 1.30f)
    BossKind.GIANT_CONDOM -> BossMeta(0xFFFFB6D5, "Sóng xung kích", "Ta bảo vệ tất cả... rồi nuốt chửng ngươi!", 1.10f)
    BossKind.VENOM_SPIDER -> BossMeta(0xFF7CFF2A, "Lưới tơ độc 8 hướng", "Tơ độc đã giăng. Ngươi chỉ là con mồi.", 0.70f)
    BossKind.CORRUPTION -> BossMeta(0xFFD23FFF, "Tường tiền đè, khe quét", "Có tiền mua tiên cũng được — kể cả mạng ngươi!", 0.85f)
    BossKind.TRAFFIC_JAM -> BossMeta(0xFFFFA000, "Lấp làn trái/phải", "Kẹt xe giờ cao điểm — đừng hòng nhúc nhích!", 0.95f)
    BossKind.KPI_BOSS -> BossMeta(0xFF2D7DFF, "Cột chỉ tiêu + deadline", "Chưa đạt chỉ tiêu quý này — tăng ca tới chết đi!", 1.08f)
    BossKind.TIKTOKER -> BossMeta(0xFFFF2E63, "Spam tim bay cong", "Nhớ like share nha — à nhầm, ăn đạn nè!", 1.45f)
    BossKind.ATM_BANKRUPT -> BossMeta(0xFF2BD4A8, "Nhả tiền dồn rồi kẹt", "Số dư: 0đ. Giao dịch bị từ chối — và cả ngươi nữa!", 1.02f)
    BossKind.NOKIA_BRICK -> BossMeta(0xFF3A5BA0, "Ném gạch nặng chậm", "Pin ta trâu cả tuần. Ăn cục gạch này!", 0.62f)
    BossKind.INFLATION_STORM -> BossMeta(0xFFFF6F3D, "Đạn nhiều dần + tăng tốc", "Hôm nay giá lại tăng! Đỡ nổi không?", 1.28f)
    BossKind.SOCIAL_DRAMA -> BossMeta(0xFFFF1493, "Ném đá nhiều hướng", "Phốt ngươi đây! Cả cõi mạng ném đá!", 1.50f)
    BossKind.PYRAMID_SCHEME -> BossMeta(0xFFE8B923, "Spread hình tháp", "Tham gia tuyến dưới đi — à thôi, ăn đạn trước!", 0.90f)
    BossKind.FORTUNE_TELLER -> BossMeta(0xFF9D4EDD, "Quạt + tia tiên tri", "Ta đã thấy trước cái chết của ngươi rồi.", 1.18f)
    BossKind.KITCHEN_GOD -> BossMeta(0xFFE63A2B, "Cá nhảy cong + lửa", "Ta cưỡi cá về trời — tiễn ngươi xuống đất!", 0.78f)
    BossKind.CRYPTO_BRO -> BossMeta(0xFFF7931A, "Pump & dump", "All-in đi cháu! À mà thôi, cháu cháy tài khoản rồi.", 1.03f)
    BossKind.TOXIC_KID -> BossMeta(0xFF7FFF00, "Spam hướng loạn", "Gà! Báo! Đỡ được loạt này không hả ông cháu?", 1.60f)
    BossKind.KARAOKE_BOSS -> BossMeta(0xFFFF44CC, "Sóng âm vòng cung", "Nghe hết bài này đã rồi hẵng chết nhé!", 1.35f)
    BossKind.FLASHY_TYCOON -> BossMeta(0xFFFFE14D, "Flex loạt rộng", "Nhìn cho rõ — toàn hàng hiệu đấy! Lác mắt chưa?", 0.92f)
    BossKind.DR_GOOGLE -> BossMeta(0xFF18B8A0, "Chẩn đoán bừa hình X", "Triệu chứng của ngươi à? Để ta tra... ung thư giai đoạn cuối!", 1.12f)
    BossKind.CAT_EMPEROR -> BossMeta(0xFFB089FF, "Vuốt mèo 2 cụm", "Meo~ Quỳ xuống tung hô Trẫm đi, hạ dân!", 1.22f)
    BossKind.SALE_FANATIC -> BossMeta(0xFFFF5252, "Flash sale dồn dập", "Sale 90%! Chốt đơn ngay kẻo hết — à nhầm, chốt MẠNG ngươi!", 1.40f)
    BossKind.GHOST_MONTH -> BossMeta(0xFFAFE9E0, "Hồn cong vật vờ", "Tháng cô hồn... ta đói lắm rồi. Cúng mạng ngươi nhé?", 0.70f)
}
