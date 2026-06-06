package com.tranphuloi.neon.ui.game.state

import androidx.compose.runtime.Immutable
import com.tranphuloi.neon.ui.game.buff.BuffMultipliers
import com.tranphuloi.neon.ui.game.buff.RunBuff

/**
 * Wave 5 foundation — pure function computing player stats from RunContext +
 * base values. Single source of truth for stat scaling: difficulty × modifier ×
 * meta upgrades all combine here, with caps to prevent multiplicative blowup.
 *
 * Caps (after applying all multipliers):
 *  - hp:      0.3× .. 3.0× of base
 *  - damage:  0.5× .. 4.0× of base
 *  - speed:   0.5× .. 3.5× of base
 *  - magnet:  0.5× .. 3.0× of base
 *  - score:   0.5× .. 4.0× of base
 *
 * Difficulty.multiplier is INCOMING damage multiplier (player takes more on
 * Hard), so it scales hp INVERSELY (Hard = effectively less hp).
 */
@Immutable
data class EffectiveStats(
    val hpMul: Float,
    val damageMul: Float,
    val speedMul: Float,
    val magnetMul: Float,
    val scoreMul: Float,
    val noShieldDrops: Boolean,
    val bossesOnly: Boolean,
    val noBoosters: Boolean = false,
) {
    /**
     * Round 36 — merge roguelike buffs onto already-computed stats. Same caps as
     * [compute]. Extracted from GameState so the math is unit-testable.
     */
    fun withBuffs(buffs: List<RunBuff>): EffectiveStats {
        val b = BuffMultipliers.from(buffs)
        return copy(
            hpMul = (hpMul * b.hpMul).coerceIn(0.3f, 3.0f),
            damageMul = (damageMul * b.damageMul).coerceIn(0.5f, 4.0f),
            speedMul = (speedMul * b.speedMul).coerceIn(0.5f, 3.5f),
            magnetMul = (magnetMul * b.magnetMul).coerceIn(0.5f, 3.0f),
            scoreMul = (scoreMul * b.scoreMul).coerceIn(0.5f, 4.0f),
        )
    }

    companion object {
        /** Meta upgrade keys → per-rank effect. Each rank adds the listed delta. */
        private const val META_HP_PER_RANK = 0.10f          // +10% hp per rank
        private const val META_DAMAGE_PER_RANK = 0.08f      // +8% damage per rank
        private const val META_SPEED_PER_RANK = 0.06f       // +6% speed per rank
        private const val META_MAGNET_PER_RANK = 0.15f      // +15% magnet radius per rank

        /** Meta upgrade keys. Kept in sync with SkillNode (48x). */
        const val META_KEY_HP = "meta_hp"
        const val META_KEY_DAMAGE = "meta_damage"
        const val META_KEY_SPEED = "meta_speed"
        const val META_KEY_MAGNET = "meta_magnet"
        /** Round 74 (R73f) — wire `meta_lifetime` (LIFETIME_BONUS) cho score. */
        const val META_KEY_LIFETIME = "meta_lifetime"
        /** Round 74 (R73f) — wire `meta_shield` (BASE_SHIELD) cho shield duration. */
        const val META_KEY_SHIELD = "meta_shield"
        /** Round 74 (R73f) — Wave 8 ship-shape unlock discount node key. */
        const val META_KEY_SHIP_UNLOCK_DISCOUNT = "meta_ship_unlock"
        /** Round 74 (R73f) — Wave 10 bullet-buff duration extender. */
        const val META_KEY_BULLET_DURATION = "meta_bullet_duration"

        // Round 75 — add constants cho 9 SkillNode keys (trước hardcode strings).
        // Mỗi key trỏ tới `SkillNode.<X>.key` — single source of truth.
        const val META_KEY_REGEN = "meta_regen"
        const val META_KEY_CRIT = "meta_crit"
        const val META_KEY_SHIELD_BURST = "meta_shield_burst"
        const val META_KEY_DASH = "meta_dash"
        const val META_KEY_EXTRA_BOMB = "meta_extra_bomb"
        const val META_KEY_COMBO_KEEP = "meta_combo_keep"
        const val META_KEY_REVIVE_DROP = "meta_revive_drop"
        const val META_KEY_LEGENDARY_HP = "meta_legendary_hp"
        const val META_KEY_LEGENDARY_DMG = "meta_legendary_dmg"

        /** Round 74 — per-rank score % bonus from LIFETIME_BONUS. */
        const val META_LIFETIME_PER_RANK = 0.05f
        /** Round 74 — per-rank seconds added to base shield duration. */
        const val META_SHIELD_SEC_PER_RANK = 1.5f
        /** Round 74 — per-rank ship unlock discount %. */
        const val META_SHIP_UNLOCK_DISCOUNT_PER_RANK = 0.10f
        /** Round 74 — per-rank bullet active duration % bonus. */
        const val META_BULLET_DURATION_PER_RANK = 0.10f

        fun compute(ctx: RunContext): EffectiveStats {
            // 1) Difficulty (incoming dmg multiplier) → inverse hp scale
            val diffHp = 1f / ctx.difficulty.multiplier

            // 2) Modifier
            val mod = ctx.modifier

            // 3) Meta upgrades
            val metaHpRank = ctx.metaUpgrades[META_KEY_HP] ?: 0
            val metaDmgRank = ctx.metaUpgrades[META_KEY_DAMAGE] ?: 0
            val metaSpdRank = ctx.metaUpgrades[META_KEY_SPEED] ?: 0
            val metaMagRank = ctx.metaUpgrades[META_KEY_MAGNET] ?: 0
            // Round 74 (R73f) — wire LIFETIME_BONUS → scoreMul.
            val metaLifeRank = ctx.metaUpgrades[META_KEY_LIFETIME] ?: 0

            val metaHp = 1f + metaHpRank * META_HP_PER_RANK
            val metaDmg = 1f + metaDmgRank * META_DAMAGE_PER_RANK
            val metaSpd = 1f + metaSpdRank * META_SPEED_PER_RANK
            val metaMag = 1f + metaMagRank * META_MAGNET_PER_RANK
            val metaLife = 1f + metaLifeRank * META_LIFETIME_PER_RANK

            // Round 73 (Wave 8 — ShipShape wiring) — ship shape stat mul
            // applied AFTER mod + meta + before caps.
            val shipHpMul = ctx.shipShape.hpMul
            val shipSpeedMul = ctx.shipShape.speedMul
            val shipDamageMul = ctx.shipShape.damageMul

            return EffectiveStats(
                hpMul = (diffHp * mod.hpMul * metaHp * shipHpMul).coerceIn(0.3f, 3.0f),
                damageMul = (mod.damageMul * metaDmg * shipDamageMul).coerceIn(0.5f, 4.0f),
                speedMul = (mod.speedMul * metaSpd * shipSpeedMul).coerceIn(0.5f, 3.5f),
                magnetMul = (mod.magnetMul * metaMag).coerceIn(0.5f, 3.0f),
                // Round 74 (R73f) — apply metaLife multiplier vào scoreMul.
                scoreMul = (mod.scoreMul * metaLife).coerceIn(0.5f, 4.0f),
                noShieldDrops = mod.noShieldDrops,
                bossesOnly = mod.bossesOnly,
                noBoosters = mod.noBoosters,
            )
        }
    }
}
