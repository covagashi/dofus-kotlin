package org.starloco.locos.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContentEquals

class FormulasTest {

    // region dice jets

    @Test
    fun `getMinJet returns dice count plus flat bonus`() {
        assertEquals(7, Formulas.getMinJet("1d5+6"))
        assertEquals(4, Formulas.getMinJet("3d10+1"))
    }

    @Test
    fun `getMaxJet returns dice count times faces plus flat bonus`() {
        assertEquals(11, Formulas.getMaxJet("1d5+6"))
        assertEquals(31, Formulas.getMaxJet("3d10+1"))
    }

    @Test
    fun `getMiddleJet returns midpoint of jet`() {
        // ((1 + 5) / 2) * 1 + 6 = 9
        assertEquals(9, Formulas.getMiddleJet("1d5+6"))
    }

    @Test
    fun `getRandomJet stays inside min-max`() {
        repeat(200) {
            val v = Formulas.getRandomJet(null, null, "2d6+3")
            assertTrue(v in 5..15, "jet $v out of bounds")
        }
    }

    @Test
    fun `getRandomJet with zero faces and bonus falls back to plain range`() {
        repeat(200) {
            val v = Formulas.getRandomJet(null, null, "4d0+0")
            assertTrue(v in 0..4, "jet $v out of bounds")
        }
    }

    @Test
    fun `invalid jets return -1`() {
        assertEquals(-1, Formulas.getMinJet("nope"))
        assertEquals(-1, Formulas.getMaxJet("nope"))
        assertEquals(-1, Formulas.getRandomJet(null, null, "nope"))
    }

    // endregion

    // region random value

    @Test
    fun `getRandomValue stays inside inclusive bounds`() {
        repeat(500) {
            val v = Formulas.getRandomValue(1, 10)
            assertTrue(v in 1..10, "value $v out of bounds")
        }
    }

    @Test
    fun `getRandomValue with inverted bounds returns 0`() {
        assertEquals(0, Formulas.getRandomValue(10, 1))
    }

    @Test
    fun `getRandomValue with equal bounds returns the bound`() {
        assertEquals(5, Formulas.getRandomValue(5, 5))
    }

    // endregion

    // region misc

    @Test
    fun `countCell is ring area capped at 64`() {
        assertEquals(0, Formulas.countCell(0))
        assertEquals(4, Formulas.countCell(1))
        assertEquals(2 * 64 * 65, Formulas.countCell(64))
        assertEquals(2 * 64 * 65, Formulas.countCell(100)) // capped
    }

    @Test
    fun `shuffleCharArray preserves character multiset`() {
        val original = "abcdefg0123456789".toCharArray()
        val shuffled = Formulas.shuffleCharArray(original.copyOf())
        assertContentEquals(original.sortedArray(), shuffled.sortedArray())
    }

    // endregion
}
