package com.tranphuloi.neon.ui.game.laser

import com.tranphuloi.neon.TestLaser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class LaserToLaserUIMapperTest {

    @Test
    fun `identical laser hits cache`() {
        val mapper = LaserToLaserUIMapper()
        val l = TestLaser(id = "l1")
        assertSame(mapper(l), mapper(l))
    }

    @Test
    fun `xOffset change misses cache`() {
        val mapper = LaserToLaserUIMapper()
        val l = TestLaser(id = "l1")
        val first = mapper(l)
        l.xOffset = 100f
        val second = mapper(l)
        assertNotSame(first, second)
        assertEquals(100f, second.xOffset, 0f)
    }

    @Test
    fun `yOffset change misses cache`() {
        val mapper = LaserToLaserUIMapper()
        val l = TestLaser(id = "l1")
        val first = mapper(l)
        l.yOffset = 500f
        val second = mapper(l)
        assertNotSame(first, second)
    }

    @Test
    fun `rotation change misses cache (homing missile case)`() {
        val mapper = LaserToLaserUIMapper()
        val l = TestLaser(id = "missile-1")
        val first = mapper(l)
        l.rotation = 15f                              // missile homing tilt
        val second = mapper(l)
        assertNotSame(first, second)
        assertEquals(15f, second.rotation, 0f)
    }

    @Test
    fun `LRU evicts eldest at CACHE_CAPACITY overflow`() {
        val mapper = LaserToLaserUIMapper()
        val firstLaser = TestLaser(id = "id-0")
        val firstUi = mapper(firstLaser)
        for (i in 1 until LaserToLaserUIMapper.CACHE_CAPACITY) {
            mapper(TestLaser(id = "id-$i"))
        }
        mapper(TestLaser(id = "id-${LaserToLaserUIMapper.CACHE_CAPACITY}"))
        // id-0 (eldest, never re-accessed) should have been evicted.
        val rebuilt = mapper(firstLaser)
        assertNotSame(firstUi, rebuilt)
    }

    @Test
    fun `cache shared across ship + ultimate + enemy lasers is safe via unique uuids`() {
        // Production behavior: one mapper instance is reused for 3 laser lists.
        // Different ids → independent slots; same id (which would never happen
        // since uuids are unique) would alias — verified isolated here.
        val mapper = LaserToLaserUIMapper()
        val ship = TestLaser(id = "ship-aaa")
        val ult = TestLaser(id = "ult-bbb")
        val enemy = TestLaser(id = "enemy-ccc")
        val s = mapper(ship)
        val u = mapper(ult)
        val e = mapper(enemy)
        // All distinct.
        assertNotSame(s, u)
        assertNotSame(u, e)
        // Each still hits its own cache entry on re-invocation.
        assertSame(s, mapper(ship))
        assertSame(u, mapper(ult))
        assertSame(e, mapper(enemy))
    }

    @Test
    fun `trimDead removes inactive lasers`() {
        val mapper = LaserToLaserUIMapper()
        val a = TestLaser(id = "a")
        val b = TestLaser(id = "b")
        val aUi = mapper(a)
        val bUi = mapper(b)
        mapper.trimDead(aliveIds = setOf("a"))
        assertSame(aUi, mapper(a))
        assertNotSame(bUi, mapper(b))
    }
}
