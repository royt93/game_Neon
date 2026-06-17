package com.tranphuloi.neon.ui.game.ship.laser

import com.tranphuloi.neon.ui.game.booster.BoosterRarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BulletTypeTest {

    @Test
    fun `NORMAL is the inert default`() {
        val n = BulletType.NORMAL
        assertEquals(0L, n.activeDurationMillis)
        assertEquals(1f, n.damageMultiplier, 0f)
        assertEquals(0, n.pierceCount)
        assertEquals(0f, n.aoeRadius, 0f)
    }

    @Test
    fun `PIERCING pierces up to 3 enemies at base damage`() {
        val p = BulletType.PIERCING
        assertEquals(3, p.pierceCount)
        assertEquals(1f, p.damageMultiplier, 0f)
        assertEquals(0f, p.aoeRadius, 0f)
        assertTrue("timed booster must have positive duration", p.activeDurationMillis > 0L)
    }

    @Test
    fun `PLASMA has AoE radius and damage multiplier`() {
        val p = BulletType.PLASMA
        assertEquals(0, p.pierceCount)
        assertTrue("plasma should be >100% damage", p.damageMultiplier > 1f)
        assertTrue("plasma should have AoE", p.aoeRadius > 0f)
        assertTrue("timed booster must have positive duration", p.activeDurationMillis > 0L)
    }

    @Test
    fun `each type has a unique glyph`() {
        val glyphs = BulletType.entries.map { it.glyph }
        assertEquals(glyphs.size, glyphs.toSet().size)
    }

    @Test
    fun `non-NORMAL types differ from NORMAL on at least one combat axis`() {
        BulletType.entries
            .filter { it != BulletType.NORMAL }
            // Wave 16 — LOTTERY's enum stats equal NORMAL on purpose: its
            // differentiator is RANDOM per-shot damage set on impactPower at
            // spawn (LasersController.buildOneLaser), not an enum combat axis.
            .filter { it != BulletType.LOTTERY }
            .forEach { t ->
                val diff = t.damageMultiplier != BulletType.NORMAL.damageMultiplier ||
                        t.pierceCount != BulletType.NORMAL.pierceCount ||
                        t.aoeRadius != BulletType.NORMAL.aoeRadius
                assertTrue("$t is indistinguishable from NORMAL on combat axes", diff)
            }
    }

    // -- Wave 18 batch 3 (đạn trào phúng mới) --------------------------------

    @Test
    fun `BUBBLE_TEA is an AoE bullet`() {
        val b = BulletType.BUBBLE_TEA
        assertTrue("trà sữa phải có AoE (nổ trân châu)", b.aoeRadius > 0f)
        assertTrue("timed buff", b.activeDurationMillis > 0L)
    }

    @Test
    fun `FISH_SAUCE and QR_CODE rely on status not raw stats`() {
        // Cơ chế (CORROSION / SLOW+STUN) áp ở onLaserHit, không phải AoE/pierce.
        listOf(BulletType.FISH_SAUCE, BulletType.QR_CODE).forEach {
            assertEquals("$it không nên có AoE", 0f, it.aoeRadius, 0f)
            assertEquals("$it không nên xuyên", 0, it.pierceCount)
        }
    }

    @Test
    fun `SANDAL differs from NORMAL on a combat axis (boomerang gets a damage cut)`() {
        // Đánh 2 chiều nên dmg/hit < 1.0 để cân bằng; cũng giúp khác NORMAL.
        assertNotEquals(
            BulletType.NORMAL.damageMultiplier,
            BulletType.SANDAL.damageMultiplier,
        )
    }

    @Test
    fun `batch3 satirical bullets all present`() {
        listOf(
            BulletType.BUBBLE_TEA, BulletType.FISH_SAUCE,
            BulletType.SANDAL, BulletType.QR_CODE,
        ).forEach {
            assertNotEquals("$it phải có tên", "", it.displayName)
            assertTrue("$it phải có thời hạn buff", it.activeDurationMillis > 0L)
        }
    }

    // -- Wave 18b: nhịp bắn + số viên theo item đạn -------------------------

    @Test
    fun `every type has a positive fire interval`() {
        BulletType.entries.forEach {
            assertTrue("$it phải có nhịp bắn > 0", it.fireIntervalMillis > 0L)
        }
    }

    @Test
    fun `strong bullets fire slower than NORMAL (giảm mật độ đạn)`() {
        val n = BulletType.NORMAL.fireIntervalMillis
        listOf(
            BulletType.KAMEHAMEHA, BulletType.GIANT, BulletType.ATOMIC,
            BulletType.PLASMA, BulletType.FIREWORK, BulletType.BUBBLE_TEA,
        ).forEach {
            assertTrue("$it nên bắn THƯA hơn NORMAL", it.fireIntervalMillis > n)
        }
    }

    @Test
    fun `salvoCount is at least 1 for every type`() {
        BulletType.entries.forEach {
            assertTrue("$it salvo >= 1", it.salvoCount >= 1)
        }
    }

    @Test
    fun `display names are non-empty`() {
        BulletType.entries.forEach {
            assertNotEquals("$it has empty displayName", "", it.displayName)
        }
    }

    // -- fromName parse / fallback (round 45) ------------------------------

    @Test
    fun `fromName roundtrips for every type`() {
        BulletType.entries.forEach {
            assertEquals(it, BulletType.fromName(it.name))
        }
    }

    @Test
    fun `fromName returns NORMAL for null`() {
        assertEquals(BulletType.NORMAL, BulletType.fromName(null))
    }

    @Test
    fun `fromName returns NORMAL for unknown name`() {
        assertEquals(BulletType.NORMAL, BulletType.fromName("WHATEVER"))
    }

    @Test
    fun `fromName is case sensitive (enum names are uppercase)`() {
        // Persisting via `enum.name` writes "PIERCING" — a lowercase "piercing"
        // should NOT match. This guarantees DataStore corruption to lowercase
        // doesn't silently match.
        assertEquals(BulletType.NORMAL, BulletType.fromName("piercing"))
    }

    // -- rarity scaling (round 52, 40x Item combos) ------------------------

    @Test
    fun `pierceCountForRarity matches Common-Rare-Epic 3-4-5`() {
        assertEquals(3, BulletType.pierceCountForRarity(BoosterRarity.COMMON))
        assertEquals(4, BulletType.pierceCountForRarity(BoosterRarity.RARE))
        assertEquals(5, BulletType.pierceCountForRarity(BoosterRarity.EPIC))
    }

    @Test
    fun `pierceCountForRarity is monotonically increasing`() {
        val c = BulletType.pierceCountForRarity(BoosterRarity.COMMON)
        val r = BulletType.pierceCountForRarity(BoosterRarity.RARE)
        val e = BulletType.pierceCountForRarity(BoosterRarity.EPIC)
        assertTrue("Rare ($r) must exceed Common ($c)", r > c)
        assertTrue("Epic ($e) must exceed Rare ($r)", e > r)
    }

    @Test
    fun `pierceCountForRarity Common matches base PIERCING`() {
        assertEquals(
            BulletType.PIERCING.pierceCount,
            BulletType.pierceCountForRarity(BoosterRarity.COMMON),
        )
    }

    @Test
    fun `plasmaAoeMultiplierForRarity matches Common-Rare-Epic 1_0-1_375-1_75`() {
        assertEquals(1.0f, BulletType.plasmaAoeMultiplierForRarity(BoosterRarity.COMMON), 0f)
        assertEquals(1.375f, BulletType.plasmaAoeMultiplierForRarity(BoosterRarity.RARE), 0f)
        assertEquals(1.75f, BulletType.plasmaAoeMultiplierForRarity(BoosterRarity.EPIC), 0f)
    }

    @Test
    fun `plasmaAoeMultiplierForRarity yields effective radii 80-110-140`() {
        val base = BulletType.PLASMA.aoeRadius
        assertEquals(80f, base * BulletType.plasmaAoeMultiplierForRarity(BoosterRarity.COMMON), 0f)
        assertEquals(110f, base * BulletType.plasmaAoeMultiplierForRarity(BoosterRarity.RARE), 0f)
        assertEquals(140f, base * BulletType.plasmaAoeMultiplierForRarity(BoosterRarity.EPIC), 0f)
    }

    @Test
    fun `plasmaAoeMultiplierForRarity is monotonically increasing`() {
        val c = BulletType.plasmaAoeMultiplierForRarity(BoosterRarity.COMMON)
        val r = BulletType.plasmaAoeMultiplierForRarity(BoosterRarity.RARE)
        val e = BulletType.plasmaAoeMultiplierForRarity(BoosterRarity.EPIC)
        assertTrue("Rare ($r) must exceed Common ($c)", r > c)
        assertTrue("Epic ($e) must exceed Rare ($r)", e > r)
    }
}
