package org.starloco.locos.area.map

import org.starloco.locos.common.CryptManager
import org.starloco.locos.game.world.World

import java.security.InvalidParameterException
import java.util.HashMap
import java.util.Optional
import java.util.stream.Collectors
import java.util.stream.Stream

interface CellsDataProvider {
    fun cellCount(): Int

    fun cellData(cellID: Int): Long
    fun overrideMask(cellID: Int): Int

    fun active(cellID: Int): Boolean {
        return activeRaw(cellData(cellID)) != 0
    }

    fun lineOfSight(cellID: Int): Boolean {
        return lineOfSightRaw(cellData(cellID)) != 0
    }

    fun movement(cellID: Int): Int {
        return movementRaw(cellData(cellID))
    }

    fun object2(cellID: Int): Int {
        return object2NumRaw(cellData(cellID))
    }

    fun object2Interactive(cellID: Int): Boolean {
        return object2InteractiveRaw(cellData(cellID)) != 0
    }

    companion object {
        @JvmStatic
        fun activeRaw(cellData: Long): Int {
            return (cellData and 0x1).toInt()
        }

        @JvmStatic
        fun lineOfSightRaw(cellData: Long): Int {
            return (cellData shr 1 and 0x1L).toInt()
        }

        @JvmStatic
        fun movementRaw(cellData: Long): Int {
            return (cellData shr 2 and 0x7).toInt()
        }

        @JvmStatic
        fun groundRotRaw(cellData: Long): Int {
            return (cellData shr 25 and 0x3).toInt()
        }

        @JvmStatic
        fun groundLevelRaw(cellData: Long): Int {
            return (cellData shr 5 and 0xF).toInt()
        }

        @JvmStatic
        fun groundSlopeRaw(cellData: Long): Int {
            return (cellData shr 9 and 0xF).toInt()
        }

        @JvmStatic
        fun groundFlipRaw(cellData: Long): Int {
            return (cellData shr 24 and 0x1).toInt()
        }

        @JvmStatic
        fun groundNumRaw(cellData: Long): Int {
            return (cellData shr 13 and 0xF).toInt()
        }

        @JvmStatic
        fun object1NumRaw(cellData: Long): Int {
            return (cellData shr 27 and 0x3FFF).toInt()
        }

        @JvmStatic
        fun object1RotRaw(cellData: Long): Int {
            return (cellData shr 42 and 0x3).toInt()
        }

        @JvmStatic
        fun object1FlipRaw(cellData: Long): Int {
            return (cellData shr 41 and 1).toInt()
        }

        @JvmStatic
        fun object2NumRaw(cellData: Long): Int {
            return (cellData shr 44 and 0x3FFF).toInt()
        }

        @JvmStatic
        fun object2FlipRaw(cellData: Long): Int {
            return (cellData shr 58 and 1).toInt()
        }

        @JvmStatic
        fun object2InteractiveRaw(cellData: Long): Int {
            return (cellData shr 59 and 0x1).toInt()
        }

        @JvmStatic
        fun b64ToLong(data: ByteArray, offset: Int): Long {
            val active = ((data[offset].toInt() and 0x20) shr 5).toLong()
            val lineOfSight = (data[offset].toInt() and 0x1).toLong()
            val movement = ((data[offset + 2].toInt() and 0x38) shr 3).toLong()
            val groundLevel = (data[offset + 1].toInt() and 0xF).toLong()
            val groundSlope = ((data[offset + 4].toInt() and 0x3C) shr 2).toLong()
            val groundNum = (((data[offset].toInt() and 0x18) shl 6) or ((data[offset + 2].toInt() and 0x7) shl 6) or (data[offset + 3].toInt() and 0x3F)).toLong()
            val groundFlip = ((data[offset + 4].toInt() and 0x2) shr 1).toLong()
            val groundRot = ((data[offset + 1].toInt() and 0x30) shr 4).toLong()
            val obj1Num = (((data[offset].toInt() and 0x4) shl 11) or ((data[offset + 4].toInt() and 0x1) shl 12) or ((data[offset + 5].toInt() and 0x3F) shl 6) or (data[offset + 6].toInt() and 0x3F)).toLong()
            val obj1Flip = ((data[offset + 7].toInt() and 0x8) shr 3).toLong()
            val obj1Rot = ((data[offset + 7].toInt() and 0x30) shr 4).toLong()
            val obj2Num = (((data[offset].toInt() and 0x2) shl 12) or ((data[offset + 7].toInt() and 0x1) shl 12) or ((data[offset + 8].toInt() and 0x3F) shl 6) or (data[offset + 9].toInt() and 0x3F)).toLong()
            val obj2Flip = ((data[offset + 7].toInt() and 0x4) shr 2).toLong()
            val obj2Interactive = ((data[offset + 7].toInt() and 0x2) shr 1).toLong()

            // Obj2Interactive-Obj2Flip-Obj2Num-Obj1Rot-Obj1Flip-Obj1Num-GndRot-GndFlip-GndNum-GndSlope-GndLevel-Movement-LoS-Active
            // Each line make the room it needs first, then OR its value in.
            // Code could be optimized, but consistency and ease of read win.
            var result = 0L
            result = (result shl 1) or obj2Interactive   // Offset=59
            result = (result shl 1) or obj2Flip          // Offset=58
            result = (result shl 14) or obj2Num          // Offset=44
            result = (result shl 2) or obj1Rot           // Offset=42
            result = (result shl 1) or obj1Flip          // Offset=41
            result = (result shl 14) or obj1Num          // Offset=27
            result = (result shl 2) or groundRot         // Offset=25
            result = (result shl 1) or groundFlip        // Offset=24
            result = (result shl 11) or groundNum        // Offset=13
            result = (result shl 4) or groundSlope       // Offset=9
            result = (result shl 4) or groundLevel       // Offset=5
            result = (result shl 3) or movement          // Offset=2
            result = (result shl 1) or lineOfSight       // Offset=1
            result = (result shl 1) or active            // Offset=0

            return result
        }
    }

    class RawCellsDataProvider(data: ByteArray) : CellsDataProvider {
        private val data: LongArray

        init {
            if (data.size % 10 != 0) throw InvalidParameterException("raw mapdata length is not a multiple of 10")
            this.data = LongArray(data.size / 10)

            var i = 0
            while (i < data.size) {
                this.data[i / 10] = CellsDataProvider.b64ToLong(data, i)
                i += 10
            }
        }

        override fun cellCount(): Int {
            return data.size
        }

        override fun cellData(cellID: Int): Long {
            return data[cellID]
        }

        override fun overrideMask(cellID: Int): Int {
            return 0
        }
    }

    class CellsDataOverride(private val base: RawCellsDataProvider) : CellsDataProvider {

        // K: cellID, V: [overrides, modMask]
        private val overrides = HashMap<Int, LongArray>()

        override fun cellCount(): Int {
            return base.cellCount()
        }

        override fun cellData(cellID: Int): Long {
            val override = overrides[cellID]
            var data = base.cellData(cellID)
            // We have an override
            if (override != null && override[1] != 0L) {
                data = data and override[1].inv()
                data = data or override[0]
            }
            return data
        }

        override fun overrideMask(cellID: Int): Int {
            val override = overrides[cellID] ?: return 0

            val ovMask = override[1]

            var clientMask = 0
            if (ovMask and 0x0000000000000001L != 0L) clientMask = clientMask or 0x2000   // active
            if (ovMask and 0x0000000000000002L != 0L) clientMask = clientMask or 0x1000   // lineOfSight
            if (ovMask and 0x000000000000001CL != 0L) clientMask = clientMask or 0x0800   // movement
            if (ovMask and 0x00000000000001E0L != 0L) clientMask = clientMask or 0x0400   // groundLevel
            if (ovMask and 0x0000000000001E00L != 0L) clientMask = clientMask or 0x0200   // groundSlope
            if (ovMask and 0x0000000000FFE000L != 0L) clientMask = clientMask or 0x0100   // groundNum
            if (ovMask and 0x0000000001000000L != 0L) clientMask = clientMask or 0x0080   // groundFlip
            if (ovMask and 0x0000000006000000L != 0L) clientMask = clientMask or 0x0040   // groundRot
            if (ovMask and 0x000001FFF8000000L != 0L) clientMask = clientMask or 0x0020   // obj1Num
            if (ovMask and 0x0000020000000000L != 0L) clientMask = clientMask or 0x0010   // obj1Flip
            if (ovMask and 0x00000C0000000000L != 0L) clientMask = clientMask or 0x0008   // obj1Rot
            if (ovMask and 0x03FFF00000000000L != 0L) clientMask = clientMask or 0x0004   // obj2Num
            if (ovMask and 0x0400000000000000L != 0L) clientMask = clientMask or 0x0002   // obj2Flip
            if (ovMask and 0x0800000000000000L != 0L) clientMask = clientMask or 0x0001   // obj2Interactive

            return clientMask
        }

        fun encodeCellData(cellID: Int): String {
            val data = Optional.ofNullable(overrides[cellID])
                .map { a -> a[0] }
                .orElse(base.cellData(cellID))

            val groundNum = groundNumRaw(data)
            val l1Num = object1NumRaw(data)
            val l2Num = object2NumRaw(data)

            return Stream.of(
                (activeRaw(data) shl 5) or (groundNum and 0x600 shr 6) or (l1Num and 0x2000 shr 11) or (l2Num and 0x2000 shr 12) or lineOfSightRaw(data),
                (groundRotRaw(data) shl 4) or groundLevelRaw(data),
                (movementRaw(data) shl 3) or (groundNum shr 6 and 0x7),
                groundNum and 0x3F,
                (groundSlopeRaw(data) shl 2) or (groundFlipRaw(data) shl 1) or (l1Num shr 12 and 0x1),
                l1Num shr 6 and 0x3F,
                l1Num and 0x3F,
                (object1RotRaw(data) shl 4) or (object1FlipRaw(data) shl 3) or (object2FlipRaw(data) shl 2) or (object2InteractiveRaw(data) shl 1) or (l2Num shr 12 and 0x1),
                l2Num shr 6 and 0x3F,
                l2Num and 0x3F
            )
                .map { CryptManager.getHashedValueByInt(it) }
                .map { it.toString() }
                .collect(Collectors.joining())
        }

        private fun setActive(cellId: Int, `val`: Int): Boolean {
            val ov = overrides.getOrPut(cellId) { LongArray(2) }

            if (activeRaw(ov[0]) == `val`) return false

            apply(ov, (`val` and 0x1).toLong(), 0x00000000000000001)

            // TODO Optimize: Detect when changing value to base value

            overrides[cellId] = ov
            return true
        }

        private fun setInteractive(cellId: Int, `val`: Int): Boolean {
            val ov = overrides.getOrPut(cellId) { LongArray(2) }

            if (object2InteractiveRaw(ov[0]) == `val`) return false

            apply(ov, (`val` and 0x1).toLong() shl 59, 0x00800000000000000L)

            // TODO Optimize: Detect when changing value to base value

            overrides[cellId] = ov
            return true
        }

        private fun setMovement(cellId: Int, `val`: Int): Boolean {
            val ov = overrides.getOrPut(cellId) { LongArray(2) }

            if (movementRaw(ov[0]) == `val`) return false

            apply(ov, (`val` and 0x7 shl 2).toLong(), 0x000000000000001CL)

            // TODO Optimize: Detect when changing value to base value

            overrides[cellId] = ov
            return true
        }

        private fun setGroundNum(cellId: Int, `val`: Int): Boolean {
            val ov = overrides.getOrPut(cellId) { LongArray(2) }

            if (groundNumRaw(ov[0]) == `val`) return false

            apply(ov, (`val` and 0xF shl 13).toLong(), 0x000000000003E000L)

            overrides[cellId] = ov
            return true
        }

        private fun setObject1Num(cellId: Int, `val`: Int): Boolean {
            val ov = overrides.getOrPut(cellId) { LongArray(2) }

            if (object1NumRaw(ov[0]) == `val`) return false

            apply(ov, (`val` and 0x3FFF).toLong() shl 27, 0x00003FFF80000000L)

            overrides[cellId] = ov
            return true
        }

        private fun setObject2Num(cellId: Int, `val`: Int): Boolean {
            val ov = overrides.getOrPut(cellId) { LongArray(2) }

            if (object2NumRaw(ov[0]) == `val`) return false

            apply(ov, (`val` and 0x3FFF).toLong() shl 44, 0x3FFF800000000000L)

            overrides[cellId] = ov
            return true
        }

        // Return true if it changed anything
        fun applyOverrides(cellId: Int, overrides: Map<String, Int>): Boolean {
            if (overrides.isEmpty()) return false

            val it = overrides.entries.iterator()
            var changed = false
            while (it.hasNext()) {
                val e = it.next()
                when (e.key.lowercase()) {
                    "active" -> changed = setActive(cellId, e.value)
                    "interactive" -> changed = setInteractive(cellId, e.value)
                    "movement" -> changed = setMovement(cellId, e.value)
                    "groundnum" -> changed = setGroundNum(cellId, e.value)
                    "o1num" -> changed = setObject1Num(cellId, e.value)
                    "o2num" -> changed = setObject2Num(cellId, e.value)
                    else -> World.world.logger.warn("ignoring unknown cell override '{}'", e.key)
                }
            }
            return changed
        }

        // Return true if it changed anything
        fun removeOverrides(cellId: Int, overrides: Map<String, Int>): Boolean {
            if (overrides.isEmpty()) return false

            val it = overrides.keys.iterator()
            var changed = false
            while (it.hasNext()) {
                val key = it.next()
                when (key.lowercase()) {
                    "active" -> changed = setActive(cellId, 0)
                    "movement" -> changed = setMovement(cellId, 0)
                    "interactive" -> changed = setInteractive(cellId, 0)
                    "groundnum" -> changed = setGroundNum(cellId, 0)
                    "o1num" -> changed = setObject1Num(cellId, 0)
                    "o2num" -> changed = setObject2Num(cellId, 0)
                    else -> World.world.logger.warn("ignoring unknown cell override '{}'", key)
                }
            }
            return changed
        }

        fun getOverrides(): Stream<Int> {
            return overrides.keys.stream()
        }

        fun isEmpty(): Boolean {
            return overrides.isEmpty()
        }

        companion object {
            private fun apply(ov: LongArray, `val`: Long, mask: Long) {
                // clear bits
                ov[0] = ov[0] and mask.inv()
                // Set bits
                ov[0] = ov[0] or `val`
                // Set mask
                ov[1] = ov[1] or mask
            }
        }
    }
}
