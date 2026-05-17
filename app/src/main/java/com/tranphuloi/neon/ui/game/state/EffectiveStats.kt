package com.tranphuloi.neon.ui.game.state

import androidx.compose.runtime.Immutable

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
) {
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

            val metaHp = 1f + metaHpRank * META_HP_PER_RANK
            val metaDmg = 1f + metaDmgRank * META_DAMAGE_PER_RANK
            val metaSpd = 1f + metaSpdRank * META_SPEED_PER_RANK
            val metaMag = 1f + metaMagRank * META_MAGNET_PER_RANK

            return EffectiveStats(
                hpMul = (diffHp * mod.hpMul * metaHp).coerceIn(0.3f, 3.0f),
                damageMul = (mod.damageMul * metaDmg).coerceIn(0.5f, 4.0f),
                speedMul = (mod.speedMul * metaSpd).coerceIn(0.5f, 3.5f),
                magnetMul = (mod.magnetMul * metaMag).coerceIn(0.5f, 3.0f),
                scoreMul = mod.scoreMul.coerceIn(0.5f, 4.0f),
                noShieldDrops = mod.noShieldDrops,
                bossesOnly = mod.bossesOnly,
            )
        }
    }
}
