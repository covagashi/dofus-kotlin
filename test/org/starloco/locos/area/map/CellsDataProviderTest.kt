package org.starloco.locos.area.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CellsDataProviderTest {

    // b64ToLong bit layout (offsets within the packed long):
    // [59]=obj2Interactive [58]=obj2Flip [44..57]=obj2Num(14b) [42..43]=obj1Rot(2b)
    // [41]=obj1Flip [27..40]=obj1Num(14b) [25..26]=groundRot(2b) [24]=groundFlip
    // [13..23]=groundNum(11b) [9..12]=groundSlope(4b) [5..8]=groundLevel(4b)
    // [2..4]=movement(3b) [1]=lineOfSight [0]=active
    private fun cell(
        active: Int = 0, lineOfSight: Int = 0, movement: Int = 0,
        groundLevel: Int = 0, groundSlope: Int = 0, groundNum: Int = 0,
        groundFlip: Int = 0, groundRot: Int = 0,
        obj1Num: Int = 0, obj1Flip: Int = 0, obj1Rot: Int = 0,
        obj2Num: Int = 0, obj2Flip: Int = 0, obj2Interactive: Int = 0
    ): ByteArray {
        val b = ByteArray(10)
        b[0] = ((active shl 5) or
                ((groundNum and 0x600) shr 6) or
                ((obj1Num and 0x2000) shr 11) or
                ((obj2Num and 0x2000) shr 12) or
                lineOfSight).toByte()
        b[1] = ((groundRot shl 4) or groundLevel).toByte()
        b[2] = ((movement shl 3) or ((groundNum shr 6) and 0x7)).toByte()
        b[3] = (groundNum and 0x3F).toByte()
        b[4] = ((groundSlope shl 2) or (groundFlip shl 1) or ((obj1Num shr 12) and 0x1)).toByte()
        b[5] = ((obj1Num shr 6) and 0x3F).toByte()
        b[6] = (obj1Num and 0x3F).toByte()
        b[7] = ((obj1Rot shl 4) or (obj1Flip shl 3) or (obj2Flip shl 2) or
                (obj2Interactive shl 1) or ((obj2Num shr 12) and 0x1)).toByte()
        b[8] = ((obj2Num shr 6) and 0x3F).toByte()
        b[9] = (obj2Num and 0x3F).toByte()
        return b
    }

    @Test
    fun decodeActiveAndLineOfSight() {
        val p = CellsDataProvider.RawCellsDataProvider(cell(active = 1, lineOfSight = 1))
        assertTrue(p.active(0))
        assertTrue(p.lineOfSight(0))
        assertEquals(1, p.cellCount())
    }

    @Test
    fun decodeInactive() {
        val p = CellsDataProvider.RawCellsDataProvider(cell(active = 0, lineOfSight = 0))
        assertFalse(p.active(0))
        assertFalse(p.lineOfSight(0))
    }

    @Test
    fun decodeMovement() {
        val p = CellsDataProvider.RawCellsDataProvider(cell(movement = 5))
        assertEquals(5, p.movement(0))
    }

    @Test
    fun decodeObject2Fields() {
        val p = CellsDataProvider.RawCellsDataProvider(cell(obj2Num = 1234, obj2Interactive = 1))
        assertEquals(1234, p.object2(0))
        assertTrue(p.object2Interactive(0))
    }

    @Test
    fun decodeTwoCells() {
        val data = cell(active = 1, movement = 2) + cell(active = 0, movement = 7)
        val p = CellsDataProvider.RawCellsDataProvider(data)
        assertEquals(2, p.cellCount())
        assertTrue(p.active(0))
        assertEquals(2, p.movement(0))
        assertFalse(p.active(1))
        assertEquals(7, p.movement(1))
    }

    @Test
    fun rejectsBadLength() {
        assertFailsWith<Exception> {
            CellsDataProvider.RawCellsDataProvider(ByteArray(11))
        }
    }

    @Test
    fun rawGettersMatchPacking() {
        val data = CellsDataProvider.b64ToLong(
            cell(active = 1, lineOfSight = 1, movement = 3, groundLevel = 9, groundSlope = 6),
            0
        )
        assertEquals(1, CellsDataProvider.activeRaw(data))
        assertEquals(1, CellsDataProvider.lineOfSightRaw(data))
        assertEquals(3, CellsDataProvider.movementRaw(data))
        assertEquals(9, CellsDataProvider.groundLevelRaw(data))
        assertEquals(6, CellsDataProvider.groundSlopeRaw(data))
    }
}
