package org.starloco.locos.fight.ia

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter

/**
 * Created by Locos on 18/09/2015.
 */
interface IA {

    fun getFight(): Fight
    fun getFighter(): Fighter
    fun isStop(): Boolean
    fun setStop(stop: Boolean)
    fun addNext(runnable: Runnable, time: Int)

    fun apply()
    fun endTurn()
}
