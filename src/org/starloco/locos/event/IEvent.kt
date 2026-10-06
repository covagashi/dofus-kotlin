package org.starloco.locos.event

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.client.Player

/**
 * Created by Locos on 02/10/2016.
 */
interface IEvent {

    fun prepare()
    fun perform()
    fun execute()
    fun close()

    @Throws(Exception::class)
    fun onReceivePacket(manager: EventManager, player: Player, packet: String): Boolean
    fun getEmptyCellForPlayer(player: Player): GameCase?
}
