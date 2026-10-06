package org.starloco.locos.fight.ia.util.newia.action

/**
 * Created by Locos on 28/04/2018.
 */
interface IAAction {

    fun getType(): Byte
    fun getWaitingTime(): Short
    fun execute(): Boolean

    companion object {
        const val MOVE: Byte = 0
        const val ATTACK: Byte = 1
    }
}
