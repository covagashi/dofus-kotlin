package org.starloco.locos.job.maging

import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import java.util.ArrayList

class Rune(val id: Short, val weight: Float, val bonus: Byte) {

    var characteristic: Short = 0
        private set

    init {
        this.characteristic = World.world.getObjTemplate(this.id.toInt())!!.strTemplate.split("#")[0].toShort(16)
        if (this.characteristic.toInt() == 112)
            this.characteristic = Constant.STATS_ADD_DOMA.toShort()
        runes.add(this)
    }

    fun getChance(): ByteArray =
        if (this.weight <= 1) byteArrayOf(66, 34, 0) else byteArrayOf(43, 50, 7)

    companion object {
        @JvmField
        val runes: MutableList<Rune> = ArrayList()

        @JvmStatic
        fun getRuneById(id: Int): Rune? {
            for (rune in runes)
                if (rune.id.toInt() == id)
                    return rune
            return null
        }

        @JvmStatic
        fun getRuneByCharacteristic(stat: Short): Rune? {
            for (rune in runes)
                if (rune.characteristic == stat)
                    return rune
            return null
        }

        @JvmStatic
        fun getRuneByCharacteristicAndByWeight(stat: Short): Rune? {
            var valid: Rune? = null
            var weight = 999f
            for (rune in runes) {
                if (rune.characteristic == stat && weight > rune.weight) {
                    weight = rune.weight
                    valid = rune
                }
            }
            return valid
        }
    }
}
