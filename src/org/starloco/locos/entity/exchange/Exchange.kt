package org.starloco.locos.entity.exchange

import org.starloco.locos.client.Player
import org.starloco.locos.game.world.World

abstract class Exchange(
    @JvmField protected val player1: Player,
    @JvmField protected val player2: Player
) {

    @JvmField
    protected var kamas1: Long = 0
    @JvmField
    protected var kamas2: Long = 0
    @JvmField
    protected var items1 = ArrayList<World.Couple<Int, Int>>()
    @JvmField
    protected var items2 = ArrayList<World.Couple<Int, Int>>()
    @JvmField
    protected var ok1: Boolean = false
    @JvmField
    protected var ok2: Boolean = false

    abstract fun toogleOk(id: Int): Boolean

    abstract fun apply()

    abstract fun cancel()

    companion object {
        @JvmStatic
        fun getCoupleInList(
            items: ArrayList<World.Couple<Int, Int>>, id: Int
        ): World.Couple<Int, Int>? {
            for (couple in items)
                if (couple.first == id)
                    return couple
            return null
        }
    }
}
