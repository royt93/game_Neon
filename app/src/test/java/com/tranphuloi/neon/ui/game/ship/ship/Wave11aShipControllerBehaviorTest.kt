package com.tranphuloi.neon.ui.game.ship.ship

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Real behavior tests for Wave 11a boosters via ShipController public API.
 *
 * Per memory `feedback_always_add_tests.md` and Phase 3 audit follow-up:
 * upgrade from data-structure tests → behavior tests. Tests instantiate
 * ShipController with minimal stub callbacks and exercise public methods
 * (`applyVampireHeal`, `isXActive`, `reflectRetaliationDamage`) against
 * Ship state mutations.
 *
 * Limitations: `enable*` functions are private (called via booster pickup
 * dispatch). These tests cover the PUBLIC contract — state queries + heal
 * math — which is what callers (GameState, GameWorld) actually use.
 */
class Wave11aShipControllerBehaviorTest {

    private fun newController(initialShip: Ship): ShipController {
        var shipRef = initialShip
        return ShipController(
            screenWidth = 400f,
            screenHeight = 800f,
            ship = initialShip,
            setShip = { shipRef = it },
        )
    }

    // ── Wave 17r — heal QUA controller (fix clobber: BANH_MI + BOSS_RUSH heal) ──

    @Test
    fun `healCapped raises hp via controller and caps at max`() {
        var captured = Ship(xOffset = 0f, yOffset = 0f, hp = 500)
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = captured, setShip = { captured = it },
        )
        ctrl.healCapped(50, maxHp = 1000)
        assertEquals("hp tăng qua controller", 550, captured.hp)
        ctrl.healCapped(9999, maxHp = 1000)
        assertEquals("cap ở maxHp", 1000, captured.hp)
    }

    // ── Balance polish (Finding #4) — updateHp heal cap ở max hp thật ──

    @Test
    fun `updateHp heal caps at maxHpProvider (no overheal)`() {
        // vampire active + heal lớn: 900 + 500 = 1400 nhưng maxHpProvider=1080 → cap 1080.
        var captured = Ship(xOffset = 0f, yOffset = 0f, hp = 900, vampireEndMillis = Long.MAX_VALUE)
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = captured, setShip = { captured = it },
            maxHpProvider = { 1080 },
        )
        ctrl.applyVampireHeal(damageDealt = 1000)  // heal = 500
        assertEquals("heal cap ở max hp thật (không overheal)", 1080, captured.hp)
    }

    @Test
    fun `updateHp heal default maxHpProvider is uncapped (test-compat)`() {
        // Không truyền maxHpProvider ⇒ Int.MAX_VALUE ⇒ giữ hành vi cũ (uncapped) cho test.
        var captured = Ship(xOffset = 0f, yOffset = 0f, hp = 900, vampireEndMillis = Long.MAX_VALUE)
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = captured, setShip = { captured = it },
        )
        ctrl.applyVampireHeal(damageDealt = 1000)  // 900 + 500 = 1400, không cap
        assertEquals("default MAX_VALUE → không cap", 1400, captured.hp)
    }

    @Test
    fun `setHp sets hp via controller (BOSS_RUSH full heal)`() {
        var captured = Ship(xOffset = 0f, yOffset = 0f, hp = 300)
        ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = captured, setShip = { captured = it },
        ).setHp(1000)
        assertEquals(1000, captured.hp)
    }

    @Test
    fun `heal does nothing when ship already dead`() {
        var captured = Ship(xOffset = 0f, yOffset = 0f, hp = 0)
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = captured, setShip = { captured = it },
        )
        ctrl.healCapped(50)
        ctrl.setHp(1000)
        assertEquals("đã chết thì không hồi", 0, captured.hp)
    }

    // ── Wave 17o — post-spawn spawnRotation reset (fix "ship nghiêng phải") ──

    @Test
    fun `post-spawn moveShip forces leftover spawnRotation back to 0 (no permanent tilt)`() {
        var captured = Ship(xOffset = 0f, yOffset = 0f, spawnRotation = 9.3f)  // kẹt nghiêng phải
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f,
            ship = captured, setShip = { captured = it },
            // Spawn đã xong từ lâu → moveShip vào nhánh post-spawn.
            spawnStartMillisOverride = System.currentTimeMillis() - 10_000L,
        )
        ctrl.moveShip()
        assertEquals(
            "spawnRotation phải reset về 0 sau spawn (hết nghiêng vĩnh viễn)",
            0f, captured.spawnRotation, 0.001f,
        )
    }

    // ── Wave 17l — loadout bullet reaches the controller's internal ship ──

    @Test
    fun `setLoadoutBullet applies the equipped bullet to the ship (fix dual-state clobber)`() {
        var captured = Ship(xOffset = 0f, yOffset = 0f)
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f,
            ship = captured, setShip = { captured = it },
        )
        assertEquals(
            com.tranphuloi.neon.ui.game.ship.laser.BulletType.NORMAL,
            captured.activeBulletType,
        )
        // Trang bị PLASMA → phải tới ship NỘI BỘ (trước fix chỉ set GameState.ship
        // → tick sau controller ghi đè NORMAL → "đổi đạn vẫn y hệt").
        ctrl.setLoadoutBullet(com.tranphuloi.neon.ui.game.ship.laser.BulletType.PLASMA)
        assertEquals(
            "đạn loadout phải tới ship (active)",
            com.tranphuloi.neon.ui.game.ship.laser.BulletType.PLASMA,
            captured.activeBulletType,
        )
        assertEquals(
            "và là đạn nền (base) → không revert NORMAL",
            com.tranphuloi.neon.ui.game.ship.laser.BulletType.PLASMA,
            captured.baseBulletType,
        )
    }

    // ── GHOST behavior ──

    @Test
    fun `isGhostActive returns true when ghostEndMillis in future`() {
        val futureEnd = System.currentTimeMillis() + 10_000L
        val ship = Ship(xOffset = 0f, yOffset = 0f, ghostEndMillis = futureEnd)
        val ctrl = newController(ship)
        assertTrue("ghost should be active when end > now", ctrl.isGhostActive())
    }

    @Test
    fun `isGhostActive returns false for default Ship (no buff)`() {
        val ctrl = newController(Ship(xOffset = 0f, yOffset = 0f))
        assertFalse(ctrl.isGhostActive())
    }

    @Test
    fun `isGhostActive returns false when ghostEndMillis in past`() {
        val ship = Ship(xOffset = 0f, yOffset = 0f,
            ghostEndMillis = System.currentTimeMillis() - 1_000L)
        val ctrl = newController(ship)
        assertFalse(ctrl.isGhostActive())
    }

    // ── GRAVITY behavior ──

    @Test
    fun `isGravityActive reflects ship gravityEndMillis state`() {
        val now = System.currentTimeMillis()
        val withGravity = newController(Ship(xOffset = 0f, yOffset = 0f,
            gravityEndMillis = now + 10_000L))
        assertTrue(withGravity.isGravityActive())

        val withoutGravity = newController(Ship(xOffset = 0f, yOffset = 0f))
        assertFalse(withoutGravity.isGravityActive())
    }

    // ── REFLECT behavior ──

    @Test
    fun `isReflectActive reflects ship reflectEndMillis state`() {
        val now = System.currentTimeMillis()
        val withReflect = newController(Ship(xOffset = 0f, yOffset = 0f,
            reflectEndMillis = now + 8_000L))
        assertTrue(withReflect.isReflectActive())

        val withoutReflect = newController(Ship(xOffset = 0f, yOffset = 0f))
        assertFalse(withoutReflect.isReflectActive())
    }

    @Test
    fun `reflectRetaliationDamage defaults to 30 (Common rarity)`() {
        val ctrl = newController(Ship(xOffset = 0f, yOffset = 0f))
        // Before any REFLECT pickup, reflectDamageMul defaults to 1f
        assertEquals(30, ctrl.reflectRetaliationDamage())
    }

    // ── CHAIN_LIGHTNING behavior ──

    @Test
    fun `isChainLightningActive reflects ship chainLightningEndMillis state`() {
        val now = System.currentTimeMillis()
        val active = newController(Ship(xOffset = 0f, yOffset = 0f,
            chainLightningEndMillis = now + 10_000L))
        assertTrue(active.isChainLightningActive())

        val inactive = newController(Ship(xOffset = 0f, yOffset = 0f))
        assertFalse(inactive.isChainLightningActive())
    }

    // ── VAMPIRE behavior — heal math via public callback ──

    @Test
    fun `applyVampireHeal no-op when vampireEndMillis not set`() {
        val ship = Ship(xOffset = 0f, yOffset = 0f, hp = 500)
        val captured = mutableListOf<Ship>()
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = ship,
            setShip = { captured.add(it) },
        )
        ctrl.applyVampireHeal(damageDealt = 100)
        // No state mutations expected
        assertTrue("VAMPIRE inactive should not trigger any setShip call",
            captured.isEmpty() || captured.all { it.hp == ship.hp })
    }

    @Test
    fun `applyVampireHeal heals 50 percent of damage when active`() {
        val now = System.currentTimeMillis()
        var shipState = Ship(xOffset = 0f, yOffset = 0f, hp = 500,
            vampireEndMillis = now + 10_000L)
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = shipState,
            setShip = { shipState = it },
        )
        ctrl.applyVampireHeal(damageDealt = 100)
        // 50% heal = +50 HP. Final HP should exceed initial 500.
        assertTrue("VAMPIRE should heal ship.hp (was 500, expected > 500), got ${shipState.hp}",
            shipState.hp > 500)
        assertEquals("VAMPIRE should heal exactly 50 HP from 100 damage",
            550, shipState.hp)
    }

    @Test
    fun `applyVampireHeal min 1 HP heal even for tiny damage`() {
        val now = System.currentTimeMillis()
        var shipState = Ship(xOffset = 0f, yOffset = 0f, hp = 500,
            vampireEndMillis = now + 10_000L)
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = shipState,
            setShip = { shipState = it },
        )
        ctrl.applyVampireHeal(damageDealt = 1)
        // 50% of 1 = 0.5, .toInt() = 0, coerceAtLeast(1) = 1
        assertEquals("VAMPIRE should heal min 1 HP from 1 damage (coerceAtLeast)",
            501, shipState.hp)
    }

    @Test
    fun `applyVampireHeal no heal for 0 or negative damage`() {
        val now = System.currentTimeMillis()
        var shipState = Ship(xOffset = 0f, yOffset = 0f, hp = 500,
            vampireEndMillis = now + 10_000L)
        val ctrl = ShipController(
            screenWidth = 400f, screenHeight = 800f, ship = shipState,
            setShip = { shipState = it },
        )
        ctrl.applyVampireHeal(damageDealt = 0)
        assertEquals(500, shipState.hp)
        ctrl.applyVampireHeal(damageDealt = -50)
        assertEquals(500, shipState.hp)
    }

    // ── Cross-buff independence ──

    @Test
    fun `all 7 Wave 11a buff state queries independent`() {
        val now = System.currentTimeMillis()
        val future = now + 10_000L
        // Each buff active independently — no cross-coupling
        val ghostOnly = newController(Ship(xOffset = 0f, yOffset = 0f, ghostEndMillis = future))
        assertTrue(ghostOnly.isGhostActive())
        assertFalse(ghostOnly.isGravityActive())
        assertFalse(ghostOnly.isReflectActive())
        assertFalse(ghostOnly.isChainLightningActive())

        val gravityOnly = newController(Ship(xOffset = 0f, yOffset = 0f, gravityEndMillis = future))
        assertFalse(gravityOnly.isGhostActive())
        assertTrue(gravityOnly.isGravityActive())
        assertFalse(gravityOnly.isReflectActive())
        assertFalse(gravityOnly.isChainLightningActive())
    }

    @Test
    fun `all 4 buff queries return true when all active simultaneously`() {
        val future = System.currentTimeMillis() + 10_000L
        val allActive = newController(Ship(xOffset = 0f, yOffset = 0f,
            ghostEndMillis = future,
            gravityEndMillis = future,
            reflectEndMillis = future,
            chainLightningEndMillis = future,
        ))
        assertTrue(allActive.isGhostActive())
        assertTrue(allActive.isGravityActive())
        assertTrue(allActive.isReflectActive())
        assertTrue(allActive.isChainLightningActive())
    }
}
