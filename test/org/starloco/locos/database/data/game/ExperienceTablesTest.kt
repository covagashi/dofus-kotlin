package org.starloco.locos.database.data.game

import kotlin.test.Test
import kotlin.test.assertEquals

class ExperienceTablesTest {

    private val table = ExperienceTables.ExperienceTable(longArrayOf(0, 100, 250, 500, 900))

    @Test
    fun `minXpAt returns threshold of given level`() {
        assertEquals(0, table.minXpAt(1))
        assertEquals(250, table.minXpAt(3))
        assertEquals(900, table.minXpAt(5))
    }

    @Test
    fun `maxXpAt returns next level threshold`() {
        assertEquals(100, table.maxXpAt(1))
        assertEquals(500, table.maxXpAt(3))
    }

    @Test
    fun `maxXpAt clamps beyond max level`() {
        assertEquals(900, table.maxXpAt(5))
        assertEquals(900, table.maxXpAt(99))
    }

    @Test
    fun `levelForXp exact threshold means next level`() {
        // binarySearch hits index 1 for 100 -> level 2
        assertEquals(2, table.levelForXp(100))
        assertEquals(4, table.levelForXp(500))
    }

    @Test
    fun `levelForXp between thresholds returns insertion level`() {
        assertEquals(1, table.levelForXp(50))
        assertEquals(2, table.levelForXp(200))
        assertEquals(4, table.levelForXp(800))
    }

    @Test
    fun `levelForXp at zero is level one`() {
        assertEquals(1, table.levelForXp(0))
    }

    @Test
    fun `maxLevel is table size`() {
        assertEquals(5, table.maxLevel())
    }
}
