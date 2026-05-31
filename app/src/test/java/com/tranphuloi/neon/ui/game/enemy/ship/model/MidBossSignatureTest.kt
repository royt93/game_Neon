package com.tranphuloi.neon.ui.game.enemy.ship.model

import com.tranphuloi.neon.ui.game.ship.ship.Ship
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wave 16 batch 1 — pin the per-boss SIGNATURE attack patterns added to
 * [MidBoss.generateLasers]. Before this, 18 mid-boss variants shared just 3
 * generic patterns (aimed / spread / barrage). These tests verify the 6
 * batch-1 signatures each emit their distinct geometry, so a future refactor
 * that collapses them back to the generic trio breaks visibly.
 */
class MidBossSignatureTest {

    private fun boss(v: MidBossType) = MidBoss(
        screenWidth = 400f,
        screenHeight = 800f,
        variant = v,
        getShip = { Ship(xOffset = 200f, yOffset = 700f) },
    )

    @Test
    fun `SKULL fan emits a spread of bullets with varied horizontal speeds`() {
        val lasers = boss(MidBossType.SKULL_CROSSBONES).generateLasers()
        assertEquals("phase-1 fan width", 5, lasers.size)
        val xSpeeds = lasers.map { it.xOffsetMovementSpeed }
        assertTrue("fan must spread (distinct x-speeds)", xSpeeds.toSet().size > 1)
        assertTrue("all bullets travel downward", lasers.all { it.yOffsetMovementSpeed > 0f })
    }

    @Test
    fun `VAMPIRE volley heals (lifesteal) but never above spawn HP`() {
        val b = boss(MidBossType.VAMPIRE)
        b.onObjectImpact(1000f)                  // drop below full so heal is visible
        val hpBefore = b.hp
        b.generateLasers()
        assertTrue("vampire must regain HP on volley", b.hp > hpBefore)

        // Heal is clamped: many volleys at full HP must not exceed initialHp.
        val full = boss(MidBossType.VAMPIRE)
        repeat(20) { full.generateLasers() }
        assertTrue("lifesteal must cap at spawn HP", full.hp <= full.initialHp)
    }

    @Test
    fun `CENTIPEDE poison spray is wide (both sides) and slow`() {
        val lasers = boss(MidBossType.COSMIC_CENTIPEDE).generateLasers()
        assertEquals(6, lasers.size)
        assertTrue("spray must reach the left", lasers.any { it.xOffsetMovementSpeed < -0.1f })
        assertTrue("spray must reach the right", lasers.any { it.xOffsetMovementSpeed > 0.1f })
        assertTrue("poison drifts slowly", lasers.all { it.yOffsetMovementSpeed <= 0.55f })
    }

    @Test
    fun `HEN drops a 4-egg cluster`() {
        assertEquals(4, boss(MidBossType.HEN_MOTHER).generateLasers().size)
    }

    @Test
    fun `BUFFALO horn charge is a heavy fast parallel pair`() {
        val lasers = boss(MidBossType.BUFFALO_RAGE).generateLasers()
        assertEquals("phase-1 = 2 horns", 2, lasers.size)
        assertTrue("horns are heavy (wide)", lasers.all { it.width >= 30f })
        assertTrue("horns charge fast", lasers.all { it.yOffsetMovementSpeed >= 1.2f })
    }

    @Test
    fun `TIGER roar is a near-full-width wall with gaps and no horizontal drift`() {
        val lasers = boss(MidBossType.FIERCE_TIGER).generateLasers()
        // 8 columns minus 1-2 random gaps (phase 1 = up to 2 gaps).
        assertTrue("wall should be 6-7 wide, got ${lasers.size}", lasers.size in 6..7)
        assertTrue("wall bullets fall straight", lasers.all { it.xOffsetMovementSpeed == 0f })
    }

    // ── Batch 2 ──

    @Test
    fun `WHITE_DRAGON fire breath is a narrow fast stream`() {
        val lasers = boss(MidBossType.WHITE_DRAGON).generateLasers()
        assertEquals(3, lasers.size)
        assertTrue("breath is fast", lasers.all { it.yOffsetMovementSpeed >= 1.0f })
        assertTrue("breath is narrow", lasers.all { kotlin.math.abs(it.xOffsetMovementSpeed) < 0.35f })
    }

    @Test
    fun `HAMMER_SICKLE throws two heavy projectiles to both sides`() {
        val lasers = boss(MidBossType.HAMMER_SICKLE).generateLasers()
        assertEquals(2, lasers.size)
        assertTrue("one flies left", lasers.any { it.xOffsetMovementSpeed < 0f })
        assertTrue("one flies right", lasers.any { it.xOffsetMovementSpeed > 0f })
        assertTrue("projectiles are heavy", lasers.all { it.width >= 30f })
    }

    @Test
    fun `MONEY_TYCOON rain spawns light bullets spread across the screen width`() {
        val lasers = boss(MidBossType.MONEY_TYCOON).generateLasers()
        assertEquals(5, lasers.size)
        assertTrue("bullets are light", lasers.all { it.width <= 20f })
        // Rain falls near-vertically across a wide x-span (not from a single point).
        assertTrue("rain falls nearly straight", lasers.all { kotlin.math.abs(it.xOffsetMovementSpeed) < 0.2f })
        assertTrue("rain spread wide", lasers.map { it.xOffset }.toSet().size > 1)
    }

    @Test
    fun `GOLDEN_TYCOON spiral rotates between volleys`() {
        val b = boss(MidBossType.GOLDEN_TYCOON)
        val first = b.generateLasers().first().xOffsetMovementSpeed
        val second = b.generateLasers().first().xOffsetMovementSpeed
        assertTrue("spiral angle must advance each volley", first != second)
    }

    @Test
    fun `SEXY_DIVA hair whip alternates side each volley`() {
        val b = boss(MidBossType.SEXY_DIVA)
        val v1 = b.generateLasers().map { it.xOffsetMovementSpeed }
        val v2 = b.generateLasers().map { it.xOffsetMovementSpeed }
        // All of one volley lean one way, the next volley the other way.
        assertTrue("volley 1 leans one side", v1.all { it > 0f } || v1.all { it < 0f })
        assertTrue("volley 2 leans the opposite side", v2.all { it > 0f } || v2.all { it < 0f })
        assertTrue("sides must differ", (v1.first() > 0f) != (v2.first() > 0f))
    }

    @Test
    fun `TROLL_TOWER beam has a straight-down core column plus angled edges`() {
        val lasers = boss(MidBossType.TROLL_TOWER).generateLasers()
        assertEquals("phase-1 = 1 core + 2 edges", 3, lasers.size)
        assertTrue("has a straight-down core", lasers.any { it.xOffsetMovementSpeed == 0f && it.yOffsetMovementSpeed >= 1.1f })
        assertTrue("has angled edges", lasers.any { it.xOffsetMovementSpeed < 0f } && lasers.any { it.xOffsetMovementSpeed > 0f })
    }

    // ── Batch 3 (final) ──

    @Test
    fun `OFFENSIVE eye beam is a fast tight aimed burst`() {
        val lasers = boss(MidBossType.OFFENSIVE).generateLasers()
        assertEquals(3, lasers.size)
        assertTrue("eye beam is fast", lasers.all { it.yOffsetMovementSpeed >= 1.0f })
    }

    @Test
    fun `DEFENSIVE atom orbit is a rotating ring that still drifts downward`() {
        val lasers = boss(MidBossType.DEFENSIVE).generateLasers()
        assertEquals(6, lasers.size)
        assertTrue("orbit ring has varied x-speeds", lasers.map { it.xOffsetMovementSpeed }.toSet().size > 2)
        assertTrue("every orbit bullet still reaches downward", lasers.all { it.yOffsetMovementSpeed > 0f })
    }

    @Test
    fun `SWARM haunt scatter is a 5-bullet erratic spray`() {
        assertEquals(5, boss(MidBossType.SWARM).generateLasers().size)
    }

    @Test
    fun `DUMB_RAT nibble fires a small jittery aimed pair`() {
        assertEquals(2, boss(MidBossType.DUMB_RAT).generateLasers().size)
    }

    @Test
    fun `TWIN_SUMMITS fires two straight far-apart columns`() {
        val lasers = boss(MidBossType.TWIN_SUMMITS).generateLasers()
        assertEquals(2, lasers.size)
        assertTrue("columns fall straight", lasers.all { it.xOffsetMovementSpeed == 0f })
        val xs = lasers.map { it.xOffset }.sorted()
        assertTrue("columns are far apart", xs[1] - xs[0] > 100f)
    }

    @Test
    fun `VOID_GLOBES fires crossing streams from two orbs`() {
        val lasers = boss(MidBossType.VOID_GLOBES).generateLasers()
        assertEquals(4, lasers.size)                  // n=2 → 2 orbs × 2
        assertTrue("one stream goes right", lasers.any { it.xOffsetMovementSpeed > 0f })
        assertTrue("one stream goes left", lasers.any { it.xOffsetMovementSpeed < 0f })
    }

    // ── Batch 2 (Wave 16 — 3 user-named bosses) ──

    @Test
    fun `GIANT_CONDOM burst is a downward ring`() {
        val lasers = boss(MidBossType.GIANT_CONDOM).generateLasers()
        assertTrue("ring should be sizeable", lasers.size >= 7)
        assertTrue("every burst bullet drifts downward", lasers.all { it.yOffsetMovementSpeed > 0f })
        assertTrue("ring fans both sides", lasers.any { it.xOffsetMovementSpeed > 0.1f } && lasers.any { it.xOffsetMovementSpeed < -0.1f })
    }

    @Test
    fun `VENOM_SPIDER fires 8 radial spokes (phase 1)`() {
        val lasers = boss(MidBossType.VENOM_SPIDER).generateLasers()
        assertEquals(8, lasers.size)
        assertTrue("spokes go both sides", lasers.any { it.xOffsetMovementSpeed > 0.1f } && lasers.any { it.xOffsetMovementSpeed < -0.1f })
    }

    @Test
    fun `CORRUPTION wall is wide slow with a single gap and no drift`() {
        val lasers = boss(MidBossType.CORRUPTION).generateLasers()
        assertEquals("7 columns minus 1 gap", 6, lasers.size)
        assertTrue("wall falls straight", lasers.all { it.xOffsetMovementSpeed == 0f })
        assertTrue("wall is slow (oppressive)", lasers.all { it.yOffsetMovementSpeed <= 0.55f })
    }

    @Test
    fun `every mid-boss variant emits a non-empty signature volley`() {
        // Exhaustiveness sanity — all 18 variants must produce bullets (no
        // variant left without a signature → no silent empty volley).
        val all = listOf(
            MidBossType.OFFENSIVE, MidBossType.DEFENSIVE, MidBossType.SWARM,
            MidBossType.HEN_MOTHER, MidBossType.BUFFALO_RAGE, MidBossType.DUMB_RAT,
            MidBossType.FIERCE_TIGER, MidBossType.SEXY_DIVA, MidBossType.TROLL_TOWER,
            MidBossType.TWIN_SUMMITS, MidBossType.VOID_GLOBES, MidBossType.WHITE_DRAGON,
            MidBossType.HAMMER_SICKLE, MidBossType.MONEY_TYCOON, MidBossType.GOLDEN_TYCOON,
            MidBossType.SKULL_CROSSBONES, MidBossType.VAMPIRE, MidBossType.COSMIC_CENTIPEDE,
            MidBossType.GIANT_CONDOM, MidBossType.VENOM_SPIDER, MidBossType.CORRUPTION,
        )
        assertEquals("expected all 21 mid-boss variants listed", 21, all.size)
        all.forEach { v ->
            assertTrue("$v emitted no bullets", boss(v).generateLasers().isNotEmpty())
        }
    }

    // ── Wave 16 Wave B — marquee mechanics (SHIELD / TELEPORT) ──

    @Test
    fun `WHITE_DRAGON shields at phase 2 — takes no damage during the window`() {
        val dragon = boss(MidBossType.WHITE_DRAGON)
        dragon.yOffset = 100f                          // exit entry phase
        dragon.onObjectImpact(2000f)                   // 3200 → 1200 (< 50%)
        dragon.process()                               // engages phase 2 → shield on
        assertTrue("shield should be active right after phase-2 entry", dragon.isShielded())
        val hpAtShield = dragon.hp
        dragon.onObjectImpact(500f)                    // blocked by shield
        assertEquals("shielded boss takes NO damage", hpAtShield, dragon.hp, 0.01f)
    }

    @Test
    fun `a non-shield boss DOES take damage at phase 2`() {
        val hen = boss(MidBossType.HEN_MOTHER)         // no shield
        hen.yOffset = 100f
        hen.onObjectImpact(1000f)                      // 1800 → 800 (< 50%)
        hen.process()                                  // phase 2, no shield
        assertFalse("HEN_MOTHER must not be shielded", hen.isShielded())
        val before = hen.hp
        hen.onObjectImpact(200f)
        assertTrue("non-shield boss keeps taking damage", hen.hp < before)
    }

    @Test
    fun `shield state propagates to EnemyUI (drives the shield ring render)`() {
        val mapper = com.tranphuloi.neon.ui.game.enemy.ship.mapper.EnemyToEnemyUIMapper()
        val dragon = boss(MidBossType.WHITE_DRAGON)
        dragon.yOffset = 100f
        dragon.onObjectImpact(2000f)
        dragon.process()                               // shield on
        assertTrue("shielded boss → EnemyUI.isShielded", mapper(dragon).isShielded)

        val hen = boss(MidBossType.HEN_MOTHER)
        hen.yOffset = 100f
        hen.onObjectImpact(1000f)
        hen.process()
        assertFalse("non-shield boss → EnemyUI.isShielded false", mapper(hen).isShielded)
    }

    @Test
    fun `VENOM_SPIDER teleports to a side on its first eligible tick`() {
        val venom = boss(MidBossType.VENOM_SPIDER)
        venom.yOffset = 100f                           // exit entry
        val x0 = venom.xOffset                         // centered ≈ 135
        venom.process()                                // lastTeleport=0 → teleports now
        // side 0 = 0.18 → 400*0.18 - 130/2 = 72 - 65 = 7, held (movement suppressed).
        assertEquals("teleported to left side", 7f, venom.xOffset, 0.5f)
        assertTrue("position actually changed", venom.xOffset != x0)
    }

    @Test
    fun `a non-teleport boss follows movement (no jump to teleport sides)`() {
        val hen = boss(MidBossType.HEN_MOTHER)         // no teleport
        hen.yOffset = 100f
        hen.process()
        // erratic movement keeps it near centre, NOT at the teleport thirds (7 / 328).
        assertTrue("stays near centre, didn't teleport", hen.xOffset > 80f)
    }

    @Test
    fun `the six signatures are not all identical (distinct fingerprints)`() {
        fun fingerprint(v: MidBossType): String {
            val l = boss(v).generateLasers()
            return "${l.size}/${l.firstOrNull()?.width}/${l.map { it.xOffsetMovementSpeed }.toSet().size}"
        }
        val prints = listOf(
            MidBossType.SKULL_CROSSBONES, MidBossType.VAMPIRE, MidBossType.COSMIC_CENTIPEDE,
            MidBossType.HEN_MOTHER, MidBossType.BUFFALO_RAGE, MidBossType.FIERCE_TIGER,
        ).map { fingerprint(it) }
        // Not requiring all 6 unique (egg/skull could coincide on a metric), but
        // the set must show real variety — at least 4 distinct fingerprints.
        assertTrue("signatures too similar: $prints", prints.toSet().size >= 4)
    }
}
