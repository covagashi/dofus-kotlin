package org.starloco.locos.event.`type`

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.client.Player
import org.starloco.locos.event.IEvent
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Event::class.java)

/**
 * Created by Locos on 02/10/2016.
 */
abstract class Event(
    @JvmField protected val id: Byte,
    @JvmField protected val maxPlayers: Byte,
    @JvmField protected val name: String,
    @JvmField protected val description: String
) : IEvent {

    @JvmField
    protected var map: GameMap? = null

    open fun getEventId(): Byte = id

    open fun getMaxPlayers(): Byte = maxPlayers

    open fun getEventName(): String = name

    open fun getDescription(): String = description

    open fun getMap(): GameMap? = map

    abstract fun kickPlayer(player: Player)

    companion object {
        @JvmStatic
        fun wait(time: Int) {
            val newTime = System.currentTimeMillis() + time

            while (System.currentTimeMillis() < newTime) {
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    log.error("unexpected error", e)
                }
            }
        }
    }
}
