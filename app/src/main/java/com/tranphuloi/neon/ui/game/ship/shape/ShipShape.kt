package com.tranphuloi.neon.ui.game.ship.shape

import androidx.compose.runtime.Immutable

/**
 * Round 68 (Wave 8) — Ship shape variants. Each shape có stat profile riêng
 * + vector recipe khác (deferred to Round 69+). Behaviors stub trong Round 68:
 * tất cả render về Fighter shape (drawShipVector hiện tại) but stats múliplier
 * áp dụng đúng qua effectiveStats.
 *
 * Unlocking: lifetime minerals threshold per shape. FIGHTER là default
 * unlocked. Round 69+ implement unlock check + ShipPickerScreen UI.
 */
@Immutable
@androidx.annotation.Keep
enum class ShipShape(
    val key: String,
    val displayName: String,
    val unlockMinerals: Int,
    val hpMul: Float,
    val speedMul: Float,
    val damageMul: Float,
) {
    // Round 79 (#3) — Price rebalance (5× cheaper) + 12 new ships. User
    // feedback: "ship có giá quá mắc". Tier spread: free entry → late-game.
    FIGHTER(key = "fighter", displayName = "Tiêm kích", unlockMinerals = 0,
        hpMul = 1f, speedMul = 1f, damageMul = 1f),
    BOMBER(key = "bomber", displayName = "Oanh tạc", unlockMinerals = 200,
        hpMul = 1.25f, speedMul = 0.90f, damageMul = 1.1f),
    STEALTH(key = "stealth", displayName = "Tàng hình", unlockMinerals = 500,
        hpMul = 0.85f, speedMul = 1.20f, damageMul = 1f),
    TANK(key = "tank", displayName = "Tăng", unlockMinerals = 1000,
        hpMul = 1.50f, speedMul = 0.75f, damageMul = 0.95f),
    INTERCEPTOR(key = "interceptor", displayName = "Đánh chặn", unlockMinerals = 2000,
        hpMul = 0.80f, speedMul = 1.30f, damageMul = 1.10f),

    // ── Round 79 (#3) — 12 new ships, tasteful + inspired-by per user policy ──
    NGOI_SAO(key = "ngoi_sao", displayName = "Ngôi sao", unlockMinerals = 300,
        hpMul = 1.05f, speedMul = 1.05f, damageMul = 1.05f),
    CAU_VONG(key = "cau_vong", displayName = "Cầu vồng", unlockMinerals = 600,
        hpMul = 1.0f, speedMul = 1.10f, damageMul = 1.0f),
    PHU_THUY(key = "phu_thuy", displayName = "Phù thuỷ", unlockMinerals = 800,
        hpMul = 0.95f, speedMul = 1.15f, damageMul = 1.05f),
    AURA_GLOW(key = "aura_glow", displayName = "Aura", unlockMinerals = 1200,
        hpMul = 1.20f, speedMul = 1.0f, damageMul = 1.0f),
    SUNG_3_NONG(key = "sung_3_nong", displayName = "Súng 3 nòng", unlockMinerals = 1500,
        hpMul = 1.15f, speedMul = 0.85f, damageMul = 1.25f),
    OBELISK_SPIRE(key = "obelisk_spire", displayName = "Đỉnh tháp", unlockMinerals = 1500,
        hpMul = 1.30f, speedMul = 0.80f, damageMul = 1.15f),
    VIETNAM(key = "vietnam", displayName = "Việt nam", unlockMinerals = 2000,
        hpMul = 1.10f, speedMul = 1.05f, damageMul = 1.10f),
    DIVA(key = "diva", displayName = "Nữ thần diva", unlockMinerals = 2500,
        hpMul = 0.95f, speedMul = 1.15f, damageMul = 1.10f),
    CHET_CHOC(key = "chet_choc", displayName = "Chết chóc", unlockMinerals = 3000,
        hpMul = 0.90f, speedMul = 1.10f, damageMul = 1.30f),
    TU_THAN(key = "tu_than", displayName = "Tử thần", unlockMinerals = 4500,
        hpMul = 1.10f, speedMul = 1.20f, damageMul = 1.30f),
    MANG_NHEN_ACE(key = "mang_nhen", displayName = "Mạng nhện ace", unlockMinerals = 6000,
        hpMul = 1.0f, speedMul = 1.25f, damageMul = 1.10f),
    AO_GIAP_THIET(key = "ao_giap_thiet", displayName = "Áo giáp thiết", unlockMinerals = 7500,
        hpMul = 1.35f, speedMul = 0.95f, damageMul = 1.20f),

    // ── Round 79 audit follow-up (gap 2+3) ──
    // Gap 2: Twin Domes — tasteful curve-double silhouette (user "vú phụ nữ").
    TWIN_DOMES(key = "twin_domes", displayName = "Đỉnh đôi", unlockMinerals = 1800,
        hpMul = 1.10f, speedMul = 1.05f, damageMul = 1.05f),
    // Gap 3: 4 country variants ("với các quốc gia nổi tiếng").
    NHAT_BAN(key = "nhat_ban", displayName = "Nhật bản", unlockMinerals = 2200,
        hpMul = 1.0f, speedMul = 1.15f, damageMul = 1.15f),
    HAN_QUOC(key = "han_quoc", displayName = "Hàn quốc", unlockMinerals = 2800,
        hpMul = 1.05f, speedMul = 1.20f, damageMul = 1.05f),
    MY(key = "my", displayName = "Mỹ", unlockMinerals = 3500,
        hpMul = 1.20f, speedMul = 1.10f, damageMul = 1.15f),
    PHAP(key = "phap", displayName = "Pháp", unlockMinerals = 4000,
        hpMul = 1.10f, speedMul = 1.25f, damageMul = 1.15f);

    companion object {
        fun fromKey(key: String?): ShipShape =
            entries.firstOrNull { it.key == key } ?: FIGHTER
    }
}
