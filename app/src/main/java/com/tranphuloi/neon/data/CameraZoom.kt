package com.tranphuloi.neon.data

/**
 * Round 77 (R77g) — Camera zoom levels. User picked 3 levels với default TRUNG_BINH.
 *
 * `pixelScale` áp lên entity render size (ship/laser/enemy/boss/item/space rock/
 * booster/mineral). Collision math (gameplay coordinates) KHÔNG scale —
 * vẫn tính như TRUNG_BINH baseline. Effect: zoom OUT → entities nhỏ hơn nhìn
 * thấy nhiều hơn ở screen, gameplay tốc độ vẫn như cũ.
 */
enum class CameraZoom(val key: String, val displayName: String, val pixelScale: Float) {
    NEAR(key = "near", displayName = "Gần", pixelScale = 1.0f),
    MEDIUM(key = "medium", displayName = "Trung bình", pixelScale = 0.85f),
    FAR(key = "far", displayName = "Xa", pixelScale = 0.7f);

    companion object {
        fun fromKey(key: String?): CameraZoom =
            entries.firstOrNull { it.key == key } ?: MEDIUM
    }
}
