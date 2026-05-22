package com.tranphuloi.neon.ui.game.enemy.ship.mapper

import com.tranphuloi.neon.TestEnemy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Round 48 — verifies the memoization fast-path actually fires when fields are
 * unchanged, falls through when ANY field differs, and the LRU evicts the
 * eldest entry once capacity is exceeded.
 */
class EnemyToEnemyUIMapperTest {

    @Test
    fun `identical enemy invocation returns same EnemyUI reference (cache hit)`() {
        val mapper = EnemyToEnemyUIMapper()
        val e = TestEnemy(enemyId = "e1")
        val first = mapper(e)
        val second = mapper(e)
        // Same EnemyUI instance returned — the field-compare fast path bypassed allocation.
        assertSame("expected cached reference reuse", first, second)
    }

    @Test
    fun `xOffset change triggers cache miss → new EnemyUI`() {
        val mapper = EnemyToEnemyUIMapper()
        val e = TestEnemy(enemyId = "e1")
        val first = mapper(e)
        e.xOffset = 50f                            // moved
        val second = mapper(e)
        assertNotSame("xOffset change must miss cache", first, second)
        assertEquals(50f, second.xOffset, 0f)
    }

    @Test
    fun `yOffset change triggers cache miss`() {
        val mapper = EnemyToEnemyUIMapper()
        val e = TestEnemy(enemyId = "e1")
        val first = mapper(e)
        e.yOffset = 200f
        val second = mapper(e)
        assertNotSame(first, second)
    }

    @Test
    fun `hp change triggers cache miss`() {
        val mapper = EnemyToEnemyUIMapper()
        val e = TestEnemy(enemyId = "e1", hp = 10f)
        val first = mapper(e)
        e.hp = 5f
        val second = mapper(e)
        assertNotSame(first, second)
        assertEquals(5f, second.currentHp, 0f)
    }

    @Test
    fun `lastImpactMillis change triggers cache miss`() {
        val mapper = EnemyToEnemyUIMapper()
        val e = TestEnemy(enemyId = "e1")
        val first = mapper(e)
        e.lastImpactMillis = 1234L
        val second = mapper(e)
        assertNotSame(first, second)
    }

    @Test
    fun `empty tints singleton hits cache across invocations`() {
        val mapper = EnemyToEnemyUIMapper()
        val e = TestEnemy(enemyId = "e1")
        // Both invocations pass `emptyList()` — Kotlin's singleton.
        val first = mapper(e, emptyList())
        val second = mapper(e, emptyList())
        assertSame(first, second)
    }

    @Test
    fun `different tints content triggers cache miss even at same position`() {
        val mapper = EnemyToEnemyUIMapper()
        val e = TestEnemy(enemyId = "e1")
        val first = mapper(e, listOf(0xFFFF6020L))               // BURN tint
        val second = mapper(e, listOf(0xFF00D4FFL))              // SLOW tint
        assertNotSame(first, second)
    }

    @Test
    fun `structurally-equal non-empty tints hit cache (uses ==, not ===)`() {
        // Audit fix: was `===` which forced cache miss for new ArrayLists with
        // same content. `==` (AbstractList.equals) now catches this.
        val mapper = EnemyToEnemyUIMapper()
        val e = TestEnemy(enemyId = "e1")
        val first = mapper(e, listOf(0xFFFF6020L))
        val second = mapper(e, listOf(0xFFFF6020L))              // new List, same content
        assertSame("structurally equal tints should hit cache", first, second)
    }

    @Test
    fun `different enemy ids get independent cache slots`() {
        val mapper = EnemyToEnemyUIMapper()
        val a = TestEnemy(enemyId = "a").also { it.xOffset = 10f }
        val b = TestEnemy(enemyId = "b").also { it.xOffset = 20f }
        val aUi = mapper(a)
        val bUi = mapper(b)
        // Different ids → independent entries.
        assertNotSame(aUi, bUi)
        assertEquals(10f, aUi.xOffset, 0f)
        assertEquals(20f, bUi.xOffset, 0f)
        // Re-invoke each → cache hit per id.
        assertSame(aUi, mapper(a))
        assertSame(bUi, mapper(b))
    }

    @Test
    fun `LRU evicts eldest when CACHE_CAPACITY exceeded`() {
        val mapper = EnemyToEnemyUIMapper()
        // Fill cache to capacity.
        val firstEnemy = TestEnemy(enemyId = "id-0")
        val firstUi = mapper(firstEnemy)
        for (i in 1 until EnemyToEnemyUIMapper.CACHE_CAPACITY) {
            mapper(TestEnemy(enemyId = "id-$i"))
        }
        // Cache now full with id-0 at the head (oldest).
        // Adding one more → id-0 evicted.
        mapper(TestEnemy(enemyId = "id-${EnemyToEnemyUIMapper.CACHE_CAPACITY}"))
        // Re-invoke the original enemy — should miss cache (new UI built).
        val reEvicted = mapper(firstEnemy)
        assertNotSame("id-0 should have been evicted after capacity overflow", firstUi, reEvicted)
    }

    @Test
    fun `accessing a cached entry refreshes its LRU position`() {
        val mapper = EnemyToEnemyUIMapper()
        val oldest = TestEnemy(enemyId = "oldest")
        val oldestUi = mapper(oldest)
        // Fill cache up to but not over capacity.
        for (i in 1 until EnemyToEnemyUIMapper.CACHE_CAPACITY) {
            mapper(TestEnemy(enemyId = "filler-$i"))
        }
        // Touch 'oldest' to move it to tail (most-recent).
        assertSame(oldestUi, mapper(oldest))
        // Overflow by 1 — 'filler-1' (now eldest) should be evicted, NOT 'oldest'.
        mapper(TestEnemy(enemyId = "new-entry"))
        // Re-invoke 'oldest' — still cached (LRU saved it).
        assertSame("oldest should survive after access refresh", oldestUi, mapper(oldest))
    }

    @Test
    fun `trimDead removes entries not in alive set`() {
        val mapper = EnemyToEnemyUIMapper()
        val a = TestEnemy(enemyId = "a")
        val b = TestEnemy(enemyId = "b")
        val aUi = mapper(a)
        val bUi = mapper(b)
        // Mark b dead.
        mapper.trimDead(aliveIds = setOf("a"))
        // 'a' still cached, 'b' evicted.
        assertSame(aUi, mapper(a))
        val newBUi = mapper(b)
        assertNotSame("b should have been trimmed", bUi, newBUi)
    }
}
