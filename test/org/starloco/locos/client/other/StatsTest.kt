package org.starloco.locos.client.other

import org.starloco.locos.kernel.Constant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatsTest {

    private fun statsOf(vararg pairs: Pair<Int, Int>): Stats =
        Stats(pairs.toMap().toMutableMap())

    // region addOneStat

    @Test
    fun `addOneStat inserts on empty map`() {
        val s = Stats(mutableMapOf())
        assertEquals(5, s.addOneStat(119, 5))
        assertEquals(5, s.get(119))
    }

    @Test
    fun `addOneStat accumulates existing value`() {
        val s = statsOf(119 to 10)
        assertEquals(13, s.addOneStat(119, 3))
    }

    @Test
    fun `addOneStat removes entry when result is zero or below`() {
        val s = statsOf(119 to 5)
        assertEquals(0, s.addOneStat(119, -5))
        assertFalse(s.effects.containsKey(119))
    }

    @Test
    fun `addOneStat on missing stat with nonpositive value does nothing`() {
        val s = Stats(mutableMapOf())
        assertEquals(0, s.addOneStat(119, 0))
        assertFalse(s.effects.containsKey(119))
    }

    @Test
    fun `addOneStat remaps id 112 to STATS_ADD_DOMA`() {
        val s = Stats(mutableMapOf())
        s.addOneStat(112, 7)
        assertEquals(7, s.get(Constant.STATS_ADD_DOMA))
        assertFalse(s.effects.containsKey(112))
    }

    // endregion

    // region getEffect rem interactions

    @Test
    fun `getEffect applies PA removal stats`() {
        val s = statsOf(
            Constant.STATS_ADD_PA to 6,
            Constant.STATS_REM_PA to 2,
            Constant.STATS_REM_PA2 to 1
        )
        assertEquals(3, s.getEffect(Constant.STATS_ADD_PA))
    }

    @Test
    fun `getEffect adodge gets quarter of sagessse bonus`() {
        val s = statsOf(
            Constant.STATS_ADD_ADODGE to 10,
            Constant.STATS_ADD_SAGE to 8,
            Constant.STATS_REM_AFLEE to 3
        )
        // 10 - 3 + 8/4 = 9
        assertEquals(9, s.getEffect(Constant.STATS_ADD_ADODGE))
    }

    @Test
    fun `getEffect on missing stat returns 0`() {
        assertEquals(0, statsOf().getEffect(Constant.STATS_ADD_FORC))
    }

    // endregion

    // region comparison & cumul

    @Test
    fun `isSameStats is symmetric`() {
        val a = statsOf(119 to 5, 124 to 3)
        val b = statsOf(119 to 5, 124 to 3)
        val c = statsOf(119 to 5)
        assertTrue(a.isSameStats(b))
        assertFalse(a.isSameStats(c))
        assertFalse(c.isSameStats(a))
    }

    @Test
    fun `cumulStat sums both maps across effect range`() {
        val a = statsOf(119 to 5)
        val b = statsOf(119 to 7, 124 to 2)
        val c = Stats.cumulStat(a, b)
        assertEquals(12, c.get(119))
        assertEquals(2, c.get(124))
    }

    @Test
    fun `cumulStatFight skips zero and absent stats`() {
        val a = statsOf(119 to 0)
        val b = statsOf(124 to 4)
        val c = Stats.cumulStatFight(a, b)
        assertFalse(c.effects.containsKey(119))
        assertEquals(4, c.get(124))
    }

    // endregion

    // region encoding

    @Test
    fun `encodeItemSetStats produces hex stat string`() {
        val s = statsOf(0x6f to 0x5)
        assertEquals("6f#5#0#0", s.encodeItemSetStats())
    }

    @Test
    fun `encodeItemSetStats joins multiple entries with comma`() {
        val s = statsOf(0x6f to 0x5, 0x7c to 0xa)
        val parts = s.encodeItemSetStats().split(",")
        assertEquals(setOf("6f#5#0#0", "7c#a#0#0"), parts.toSet())
    }

    @Test
    fun `encodeItemSetStats on empty returns empty`() {
        assertEquals("", statsOf().encodeItemSetStats())
    }

    // endregion
}
