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
    // Task 10 (polish) — hệ số prestige đã nhân vào các *Mul ở [compute]. Lưu lại để
    // [withBuffs] tách ra, tránh re-clamp về cap gốc làm MẤT thưởng prestige.
    val prestigeMul: Float = 1f,
) {
    /**
     * Round 36 — merge roguelike buffs onto already-computed stats.
     *
     * Balance polish (Task 10): buff phải nhân lên phần BASE (đã bỏ prestige) rồi
     * coerce về CAP GỐC (giữ trần buff như cũ), sau đó nhân LẠI prestige + coerce
     * cap RỘNG — mirror đúng thứ tự của [compute]. Nếu clamp thẳng giá trị đã gồm
     * prestige về cap gốc thì player đã prestige nhặt buff sẽ bị tụt về trần thường
     * (mất toàn bộ reward). prestigeMul=1 ⇒ kết quả HỆT hành vi cũ (cap rộng no-op).
     */
    fun withBuffs(buffs: List<RunBuff>): EffectiveStats {
        val b = BuffMultipliers.from(buffs)
        val p = prestigeMul.coerceAtLeast(1e-4f)
        fun merge(mul: Float, bMul: Float, lo: Float, baseCap: Float, wideCap: Float): Float {
            val base = mul / p                                   // khôi phục base đã clamp (bỏ prestige)
            val buffed = (base * bMul).coerceIn(lo, baseCap)     // buff trên base, trần buff gốc
            return (buffed * p).coerceAtMost(wideCap)            // nhân lại prestige, cap rộng
        }
        return copy(
            hpMul = merge(hpMul, b.hpMul, 0.3f, 3.0f, 6.0f),
            damageMul = merge(damageMul, b.damageMul, 0.5f, 4.0f, 8.0f),
            speedMul = merge(speedMul, b.speedMul, 0.5f, 3.5f, 6.0f),
            magnetMul = merge(magnetMul, b.magnetMul, 0.5f, 3.0f, 6.0f),
            scoreMul = merge(scoreMul, b.scoreMul, 0.5f, 4.0f, 8.0f),
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

        /**
         * Task 01 (Slice 5) — DRONE_FLEET skill node. Rank = số drone tối đa
         * (rank 0 = chưa mở khoá → DRONE_BOOSTER không rơi; rank 1/2 = maxDrones 1/2).
         */
        const val META_KEY_DRONE = "meta_drone"

        /** Round 74 — per-rank score % bonus from LIFETIME_BONUS. */
        const val META_LIFETIME_PER_RANK = 0.05f
        /** Round 74 — per-rank seconds added to base shield duration. */
        const val META_SHIELD_SEC_PER_RANK = 1.5f
        /** Round 74 — per-rank ship unlock discount %. */
        const val META_SHIP_UNLOCK_DISCOUNT_PER_RANK = 0.10f
        /** Round 74 — per-rank bullet active duration % bonus. */
        const val META_BULLET_DURATION_PER_RANK = 0.10f

        // Task 24 (Wave 5) — 6 skill-tree node mới. Đọc trực tiếp tại nơi dùng
        // (GameState/controller), không aggregate trong compute() — giống pattern
        // DASH/CRIT/COMBO_KEEP/REVIVE_DROP/BULLET_DURATION ở trên.
        const val META_KEY_MINERAL_BOOST = "meta_mineral_boost"
        const val META_KEY_FIRE_RATE = "meta_fire_rate"
        const val META_KEY_BOOSTER_DURATION = "meta_booster_duration"
        const val META_KEY_MAGNET_PULL_SPEED = "meta_magnet_pull_speed"
        const val META_KEY_PIERCE_CHANCE = "meta_pierce_chance"
        const val META_KEY_SECOND_WIND = "meta_second_wind"

        /** Task 24 — per-rank mineral amount % bonus. */
        const val META_MINERAL_BOOST_PER_RANK = 0.10f
        /** Task 24 — per-rank fire-interval % reduction (faster firing). */
        const val META_FIRE_RATE_PER_RANK = 0.05f
        /** Task 24 — per-rank % bonus applied on top of every timed booster duration. */
        const val META_BOOSTER_DURATION_PER_RANK = 0.08f
        /** Task 24 — per-rank magnet pull-speed % bonus (distinct from radius). */
        const val META_MAGNET_PULL_SPEED_PER_RANK = 0.20f
        /** Task 24 — per-rank chance (0..1) a NORMAL bullet also gets 1 pierce. */
        const val META_PIERCE_CHANCE_PER_RANK = 0.10f

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
            // applied AFTER mod + meta + before caps. Task 33 — nếu có tàu
            // dung hợp, dùng trung bình cộng 2 tàu thay vì tàu chính riêng lẻ.
            val shipStats = ctx.fusionPartner?.let {
                com.tranphuloi.neon.ui.game.ship.shape.fusedStats(ctx.shipShape, it)
            }
            val shipHpMul = shipStats?.hpMul ?: ctx.shipShape.hpMul
            val shipSpeedMul = shipStats?.speedMul ?: ctx.shipShape.speedMul
            val shipDamageMul = shipStats?.damageMul ?: ctx.shipShape.damageMul

            // Task 10 (audit→9.5) — buff vĩnh viễn PRESTIGE (+4%/cấp).
            // Áp NGOÀI cap gốc: base stat coerce như cũ (giữ cân bằng gốc) rồi nhân
            // pMul lên trên với cap cuối RỘNG → reward KHÔNG bão hoà ở late-game
            // (trước pMul nằm TRONG coerceIn nên player maxed chạm trần → prestige vô
            // nghĩa). pMul=1 (chưa prestige) ⇒ giá trị hệt cũ (cap cuối không đụng).
            // Gồm cả scoreMul để "+% mọi chỉ số" đúng nghĩa.
            val pMul = ctx.prestigeMul
            // Task 12 — mastery passive dạng STAT (chỉ khi tàu đạt max level). Nhân
            // TRONG cap gốc (buff vừa phải, cùng nhóm meta/ship). Hook non-stat
            // (shield/regen/lifesteal/combo/pierce) wire ở GameState, không ở đây.
            val passive = com.tranphuloi.neon.ui.game.ship.shape.ShipPassive
                .activeFor(ctx.shipShape, ctx.shipLevel)
            fun pas(e: com.tranphuloi.neon.ui.game.ship.shape.PassiveEffect): Float =
                if (passive?.effect == e) 1f + passive.magnitude else 1f
            val pasDmg = pas(com.tranphuloi.neon.ui.game.ship.shape.PassiveEffect.DAMAGE_UP)
            val pasHp = pas(com.tranphuloi.neon.ui.game.ship.shape.PassiveEffect.HP_UP)
            val pasSpd = pas(com.tranphuloi.neon.ui.game.ship.shape.PassiveEffect.SPEED_UP)
            val pasMag = pas(com.tranphuloi.neon.ui.game.ship.shape.PassiveEffect.MAGNET_UP)
            val pasScore = pas(com.tranphuloi.neon.ui.game.ship.shape.PassiveEffect.SCORE_UP)
            return EffectiveStats(
                // Task 03 — nhân thêm hp bonus theo LEVEL tàu (shipLevelHpMul); Task 12 passive.
                hpMul = ((diffHp * mod.hpMul * metaHp * shipHpMul * ctx.shipLevelHpMul * pasHp).coerceIn(0.3f, 3.0f) * pMul).coerceAtMost(6.0f),
                damageMul = ((mod.damageMul * metaDmg * shipDamageMul * pasDmg).coerceIn(0.5f, 4.0f) * pMul).coerceAtMost(8.0f),
                speedMul = ((mod.speedMul * metaSpd * shipSpeedMul * pasSpd).coerceIn(0.5f, 3.5f) * pMul).coerceAtMost(6.0f),
                magnetMul = ((mod.magnetMul * metaMag * pasMag).coerceIn(0.5f, 3.0f) * pMul).coerceAtMost(6.0f),
                // Round 74 (R73f) — metaLife → scoreMul; +Task 10 prestige (ngoài cap gốc) + Task 12 passive.
                scoreMul = ((mod.scoreMul * metaLife * pasScore).coerceIn(0.5f, 4.0f) * pMul).coerceAtMost(8.0f),
                noShieldDrops = mod.noShieldDrops,
                bossesOnly = mod.bossesOnly,
                noBoosters = mod.noBoosters,
                prestigeMul = pMul,   // Task 10 (polish) — để withBuffs giữ headroom prestige
            )
        }
    }
}
