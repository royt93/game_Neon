package com.tranphuloi.neon.ui.game.ship.shape

/**
 * Single source of truth for [ShipShape] → ARGB accent color. Used by both
 * DialogShipPicker (selection grid) and InfoScreen (Bách Khoa Tàu tab). Was
 * two mirrored `when` tables that drifted independently for `CHET_CHOC` and
 * `TU_THAN` until consolidation here.
 *
 * Base 5 ships use top-level Neon theme palette colors (cyan/gold/violet/
 * orange/magenta). The 16 R79 ships use thematic hex literals named in
 * comments next to each branch.
 *
 * Exhaustive `when` so any new ShipShape must declare an accent color.
 */
object ShipShapeColorMap {

    fun argbFor(shape: ShipShape): Long = when (shape) {
        // Founding 5 ships use the top-level Neon theme palette.
        ShipShape.FIGHTER -> 0xFF00F0FFL                    // NeonCyan
        ShipShape.BOMBER -> 0xFFFFCB47L                     // NeonGold
        ShipShape.STEALTH -> 0xFFB14CFFL                    // NeonViolet
        ShipShape.TANK -> 0xFFFF6020L                       // orange (matches FIRE bullet hue)
        ShipShape.INTERCEPTOR -> 0xFFFF2DE0L                // NeonMagenta
        // Thematic ships — color picked to match the ship's silhouette concept.
        ShipShape.NGOI_SAO -> 0xFFFFD700L                   // gold star
        ShipShape.CAU_VONG -> 0xFFFF80E0L                   // pink rainbow accent
        ShipShape.PHU_THUY -> 0xFFB14CFFL                   // violet witch
        ShipShape.AURA_GLOW -> 0xFF60FFAAL                  // mint aura
        ShipShape.SUNG_3_NONG -> 0xFFFF3030L                // crimson gun
        ShipShape.OBELISK_SPIRE -> 0xFFE0E0E0L              // pale stone
        ShipShape.VIETNAM -> 0xFFDA251DL                    // VN red
        ShipShape.DIVA -> 0xFFFF80B0L                       // diva pink
        ShipShape.CHET_CHOC -> 0xFF606060L                  // gray reaper steel
        ShipShape.TU_THAN -> 0xFF000000L                    // black void
        ShipShape.MANG_NHEN_ACE -> 0xFFCC2030L              // spider red
        ShipShape.AO_GIAP_THIET -> 0xFFCC2020L              // iron red
        ShipShape.TWIN_DOMES -> 0xFFFFA0C0L                 // soft pink
        ShipShape.NHAT_BAN -> 0xFFBC002DL                   // Hinomaru red
        ShipShape.HAN_QUOC -> 0xFF0047A0L                   // Korea blue
        ShipShape.MY -> 0xFF3C3B6EL                         // USA blue
        ShipShape.PHAP -> 0xFF002654L                       // France blue
    }
}
