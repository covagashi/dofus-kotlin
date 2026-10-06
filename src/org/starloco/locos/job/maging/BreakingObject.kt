package org.starloco.locos.job.maging

import org.starloco.locos.game.world.World.Couple
import java.util.ArrayList

class BreakingObject {

    var objects: ArrayList<Couple<Int, Int>> = ArrayList()
    var count = 0
    var isStop = false

    @Synchronized
    fun addObject(id: Int, quantity: Int): Int {
        val couple = this.search(id)

        return if (couple == null) {
            this.objects.add(Couple(id, quantity))
            quantity
        } else {
            couple.second += quantity
            couple.second
        }
    }

    @Synchronized
    fun removeObject(id: Int, quantity: Int): Int {
        val couple = this.search(id)

        if (couple != null) {
            return if (quantity > couple.second) {
                this.objects.remove(couple)
                quantity
            } else {
                couple.second -= quantity
                if (couple.second <= 0) {
                    this.objects.remove(couple)
                    return 0
                }
                couple.second
            }
        }
        return 0
    }

    private fun search(id: Int): Couple<Int, Int>? {
        for (couple in this.objects)
            if (couple.first == id)
                return couple
        return null
    }
}
