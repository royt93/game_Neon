package com.tranphuloi.neon.ui.game.booster

/**
 * Per-type visual identity for a [BoosterType] — the three signals players
 * use to disambiguate boosters: silhouette, color, and TopEnd badge glyph.
 *
 * Single source of truth so InfoScreen preview (Bách Khoa Vật phẩm) and
 * in-game booster pickup render the same identity for each type. Was
 * 3 separate `when` tables (shapeFor / previewColorArgb / glyphFor) that
 * had to be kept in sync manually — replaced by [BoosterToBoosterUIMapper.previewSpecFor].
 */
data class BoosterPreviewSpec(
    val shape: BoosterShape,
    val colorArgb: Long,
    val glyph: String?,
) {
    /**
     * In-game sprite tint: 0L for base-6 (the raw drawable already has its
     * identity hue baked in — no re-tint needed). Non-base 21 use [colorArgb]
     * so the pickup glow matches the InfoScreen preview color.
     *
     * Base-6 ⇔ no glyph (their iconic shape/sprite self-identifies), so we
     * use glyph nullity as the discriminator.
     */
    val gameTintArgb: Long get() = if (glyph == null) 0L else colorArgb
}

class BoosterToBoosterUIMapper {

    operator fun invoke(booster: Booster): BoosterUI {
        val spec = previewSpecFor(booster.type)
        return with(booster) {
            BoosterUI(
                xOffset = xOffset,
                yOffset = yOffset,
                size = size,
                drawableId = type.drawableId,
                rarityRingColorHex = rarity.ringColorHex,
                isEliteRarity = rarity != BoosterRarity.COMMON,
                tintColorHex = spec.gameTintArgb,
                glyph = spec.glyph,
                shape = spec.shape,
            )
        }
    }

    /**
     * Single source of truth — exhaustive `when` over [BoosterType] returns
     * the full visual identity (shape + color + glyph). Compiler flags any
     * new BoosterType that doesn't declare its preview spec.
     *
     * Color invariant: all 27 ARGB values must be distinct (verified by
     * [com.tranphuloi.neon.ui.game.booster.BoosterPreviewColorDistinctTest]).
     * Shape invariant: all 27 BoosterShapes must be distinct (verified by
     * [com.tranphuloi.neon.ui.game.booster.BoosterShapeUniquenessTest]).
     * Glyph invariant: base-6 null, 21 non-base distinct (verified by
     * [com.tranphuloi.neon.ui.game.booster.BoosterGlyphTest]).
     */
    internal fun previewSpecFor(type: BoosterType): BoosterPreviewSpec = when (type) {
        // Base 6 — iconic shape + drawable natural hue, no badge (sprite self-identifies)
        BoosterType.HEALTH_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.CROSS, 0xFFA8FF60L, null)
        BoosterType.SHIELD_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.OCTAGON, 0xFF00F0FFL, null)
        BoosterType.LASER_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.TRIANGLE_UP, 0xFFFF5555L, null)
        BoosterType.TRIPLE_LASER_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.TRIPLE_BARS, 0xFFFFA040L, null)
        BoosterType.ULTIMATE_WEAPON_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.STAR, 0xFFFFD040L, null)
        BoosterType.REVIVE_TOKEN ->
            BoosterPreviewSpec(BoosterShape.HEART, 0xFF60FFAAL, null)
        // Bullet-family non-base
        BoosterType.PIERCING_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.ARROW_RIGHT, PIERCING_TINT_ARGB, "→")
        BoosterType.PLASMA_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.RING_PULSE, PLASMA_TINT_ARGB, "◯")
        BoosterType.FIRE_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.FLAME, FIRE_TINT_ARGB, "♨")
        BoosterType.HOMING_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.CROSSHAIR, HOMING_TINT_ARGB, "◎")
        BoosterType.BOUNCE_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.ARROW_CYCLE, BOUNCE_TINT_ARGB, "⇄")
        BoosterType.GIANT_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.BIG_DOT, GIANT_TINT_ARGB, "⬤")
        BoosterType.SMOKE_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.CLOUD_PUFF, SMOKE_TINT_ARGB, "❍")
        BoosterType.ZIGZAG_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.LIGHTNING, ZIGZAG_TINT_ARGB, "⌇")
        BoosterType.KAMEHAMEHA_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.BEAM, KAMEHAMEHA_TINT_ARGB, "⊛")
        BoosterType.ATOMIC_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.ATOM, ATOMIC_TINT_ARGB, "⊙")
        BoosterType.SPLIT_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.SPLIT_FORK, SPLIT_TINT_ARGB, "Ѱ")
        // Support-family non-base
        BoosterType.MAGNET_BOOST ->
            BoosterPreviewSpec(BoosterShape.MAGNET, MAGNET_BOOST_TINT_ARGB, "⊕")
        BoosterType.CRIT_SURGE ->
            BoosterPreviewSpec(BoosterShape.CRYSTAL_SPARK, CRIT_SURGE_TINT_ARGB, "✱")
        BoosterType.SPREAD_SHOT ->
            BoosterPreviewSpec(BoosterShape.SPREAD_FAN, SPREAD_SHOT_TINT_ARGB, "☆")
        BoosterType.BERSERK ->
            BoosterPreviewSpec(BoosterShape.RAGE_FANG, BERSERK_TINT_ARGB, "⚡")
        BoosterType.PHASE_SHIELD ->
            BoosterPreviewSpec(BoosterShape.PHASE_DIAMOND, PHASE_SHIELD_TINT_ARGB, "◇")
        BoosterType.SCORE_X3 ->
            BoosterPreviewSpec(BoosterShape.DOLLAR, SCORE_X3_TINT_ARGB, "$")
        BoosterType.QUICK_HEAL ->
            BoosterPreviewSpec(BoosterShape.HEALING_FLASK, QUICK_HEAL_TINT_ARGB, "✚")
        BoosterType.MINERAL_SUPERCHARGE ->
            BoosterPreviewSpec(BoosterShape.SHARD, MINERAL_SUPERCHARGE_TINT_ARGB, "✦")
        BoosterType.HEALING_AURA ->
            BoosterPreviewSpec(BoosterShape.AURA_RING, HEALING_AURA_TINT_ARGB, "+")
        BoosterType.DOUBLE_FIRE ->
            BoosterPreviewSpec(BoosterShape.DOUBLE_ARROW, DOUBLE_FIRE_TINT_ARGB, "⚯")
        // Wave 11a — 3 new boosters
        BoosterType.REGEN_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.REGEN_PULSE, REGEN_TINT_ARGB, "♻")
        BoosterType.TIME_FREEZE_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.FREEZE_FLAKE, TIME_FREEZE_TINT_ARGB, "❄")
        BoosterType.MINI_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.MINI_RING, MINI_TINT_ARGB, "◌")
        BoosterType.VAMPIRE_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.VAMPIRE_FANG, VAMPIRE_TINT_ARGB, "Ѵ")
        BoosterType.GHOST_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.GHOST_TRAIL, GHOST_TINT_ARGB, "ѻ")
        BoosterType.GRAVITY_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.GRAVITY_WELL, GRAVITY_TINT_ARGB, "⊚")
        BoosterType.REFLECT_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.REFLECT_BUMPER, REFLECT_TINT_ARGB, "⇋")
        BoosterType.CHAIN_LIGHTNING_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.CHAIN_BOLT, CHAIN_LIGHTNING_TINT_ARGB, "⚜")
        BoosterType.CLONE_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.CLONE_PAIR, CLONE_TINT_ARGB, "ǁ")
        // Task 01 (Slice 4) — drone companion
        BoosterType.DRONE_BOOSTER ->
            BoosterPreviewSpec(BoosterShape.DRONE_ROTOR, DRONE_TINT_ARGB, "◈")
    }

    // ─── 1-line delegates (kept for back-compat with existing call sites + tests) ───

    internal fun shapeFor(type: BoosterType): BoosterShape = previewSpecFor(type).shape
    internal fun previewColorArgb(type: BoosterType): Long = previewSpecFor(type).colorArgb
    internal fun glyphFor(type: BoosterType): String? = previewSpecFor(type).glyph

    companion object {
        // ARGB tints — used both for in-game booster glow (BoosterCanvas) and
        // pickup-popup activation text (GameState.onBulletTypeActivated). Each
        // is fully opaque (alpha=FF). Hues picked to stay distinct from base-6
        // sprite hues so InfoScreen preview can render all 35 BoosterTypes with
        // unique color identity (the four labeled "non-base disambig" sit in
        // hue ranges that would otherwise collide with HEALTH/SHIELD/REVIVE/
        // ULTIMATE sprite hues).
        const val PIERCING_TINT_ARGB: Long = 0xFFFF2DE0L                  // magenta (piercing arrow)
        const val PLASMA_TINT_ARGB: Long = 0xFF2050FFL                    // deep electric blue — non-base disambig (avoids SHIELD cyan and enemy_shield_drone sky blue)

        const val MAGNET_BOOST_TINT_ARGB: Long = 0xFF8A2BE2L              // violet (magnet field)
        const val CRIT_SURGE_TINT_ARGB: Long = 0xFFFFC020L                // amber (critical strike)
        const val SPREAD_SHOT_TINT_ARGB: Long = 0xFF00E5A0L               // teal-green (fan spread)
        const val BERSERK_TINT_ARGB: Long = 0xFFFF3030L                   // blood red (rage)
        const val PHASE_SHIELD_TINT_ARGB: Long = 0xFFB0E8FFL              // pale-cyan (phase ghost)
        const val SCORE_X3_TINT_ARGB: Long = 0xFFFFD700L                  // gold (score)
        const val QUICK_HEAL_TINT_ARGB: Long = 0xFFCCFF40L                // lime-yellow urgency — non-base disambig (avoids HEALTH bright green)
        const val MINERAL_SUPERCHARGE_TINT_ARGB: Long = 0xFFFF9050L       // orange (energy flash)
        const val HEALING_AURA_TINT_ARGB: Long = 0xFF20D090L              // sea-green continuous heal — non-base disambig (avoids REVIVE mint)
        const val DOUBLE_FIRE_TINT_ARGB: Long = 0xFFFF80E0L               // pink (double rate)

        const val FIRE_TINT_ARGB: Long = 0xFFFF6020L                      // bright orange (fire)
        const val HOMING_TINT_ARGB: Long = 0xFFFF40A0L                    // hot pink (lock-on)
        const val BOUNCE_TINT_ARGB: Long = 0xFF40FFD0L                    // mint (rubber bounce)
        const val GIANT_TINT_ARGB: Long = 0xFFE8A040L                     // amber-bronze heavyweight — non-base disambig (avoids ULTIMATE gold)
        const val SMOKE_TINT_ARGB: Long = 0xFFA0A0B0L                     // gray-blue smoke
        const val ZIGZAG_TINT_ARGB: Long = 0xFFFFE040L                    // electric yellow
        const val KAMEHAMEHA_TINT_ARGB: Long = 0xFF60E0FFL                // sky cyan beam
        const val ATOMIC_TINT_ARGB: Long = 0xFF80FF80L                    // radioactive green
        const val SPLIT_TINT_ARGB: Long = 0xFFB060FFL                     // purple multi-shard

        // Wave 11a — 3 new booster tints
        const val REGEN_TINT_ARGB: Long = 0xFF60E090L                     // soft pastel green (slow passive heal — distinct from HEALING_AURA sea-green 0x20D090)
        const val TIME_FREEZE_TINT_ARGB: Long = 0xFFA0F0FFL               // ice white-cyan (frozen world)
        const val MINI_TINT_ARGB: Long = 0xFFD0A0FFL                      // pale lavender (small + quick)
        const val VAMPIRE_TINT_ARGB: Long = 0xFF8B0028L                   // dark blood crimson (distinct from BERSERK 0xFF3030 lighter red)
        const val GHOST_TINT_ARGB: Long = 0xFFA0FFE0L                     // pale ghost-mint (intangible essence — distinct from PHASE_SHIELD pale-cyan + BOUNCE mint)
        const val GRAVITY_TINT_ARGB: Long = 0xFF8060A0L                   // deep cosmic purple (gravity well — distinct from MAGNET violet which is more red-leaning)
        const val REFLECT_TINT_ARGB: Long = 0xFFE0E060L                   // pale yellow bumper (distinct from SCORE_X3 gold 0xFFD700 + ZIGZAG yellow 0xFFE040)
        const val CHAIN_LIGHTNING_TINT_ARGB: Long = 0xFF80B0FFL           // electric arc blue (chain bolt — distinct from PLASMA 0xFF2050FF deeper blue + KAMEHAMEHA 0xFF60E0FF sky cyan)
        const val CLONE_TINT_ARGB: Long = 0xFFD8C870L                     // pale champagne gold (phantom-twin — distinct from SCORE_X3 0xFFFFD700 + ULTIMATE 0xFFFFD040)

        // Task 01 (Slice 4) — drone companion tint
        const val DRONE_TINT_ARGB: Long = 0xFF10C8D8L                     // bright teal-cyan (drone rotor — distinct from SHIELD 0xFF00F0FF, KAMEHAMEHA 0xFF60E0FF, PLASMA 0xFF2050FF, CHAIN 0xFF80B0FF)
    }
}
