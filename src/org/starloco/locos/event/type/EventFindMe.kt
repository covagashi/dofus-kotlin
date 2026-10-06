package org.starloco.locos.event.`type`

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.client.Player
import org.starloco.locos.common.Formulas
import org.starloco.locos.event.EventManager
import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.util.TimerWaiter
import java.util.ArrayList

/**
 * Created by Locos on 22/10/2016.
 */
class EventFindMe(id: Byte, maxPlayers: Byte, name: String, description: String) : Event(id, maxPlayers, name, description) {

    private var eventMap: GameMap? = null
    private var cell: GameCase? = null
    private var `object`: GameObject? = null
    private var count = 0
    private var time: Long = 0

    override fun getMap(): GameMap? = eventMap

    fun getCell(): GameCase? = cell

    override fun prepare() {
        // Generate an item by the level of the population
        this.eventMap = this.getRandomMap()
        this.cell = this.eventMap!!.getCase(this.eventMap!!.randomFreeCellId)
        this.`object` = World.world.getObjTemplate(26001)!!.createNewItem(1, false)
    }

    private fun getRandomMap(): GameMap {
        val maps = ArrayList<GameMap>()
        val area = World.world.getArea(18)
        for (sub in area!!.getSubAreas())
            if (sub != null && sub.id != 440 && sub.id != 447)
                maps.addAll(sub.getMaps())
        return maps[Formulas.random.nextInt(maps.size)]
    }

    override fun perform() {
        this.cell!!.tryDropItem(this.`object`!!)
        for (player in World.world.onlinePlayers) {
            player.sendTypeMessage("Event", player.lang.trans("event.findme.find", eventMap!!.subArea!!.name))
        }
        this.time = System.currentTimeMillis()
        TimerWaiter.addNext({ this.execute() }, 5000)
    }

    override fun execute() {
        if (this.cell!!.getDroppedItem(false) == null) {
            this.close()
        } else if (count % 30 == 0) {
            val end = System.currentTimeMillis() - this.time > (30 * 60 * 1000)
            val name = if (end) eventMap!!.subArea!!.name + " - [" + eventMap!!.x + ", " + eventMap!!.y + "]" else eventMap!!.subArea!!.name
            for (player in World.world.onlinePlayers)
                player.sendTypeMessage("Event", player.lang.trans("event.findme.find", name))
            TimerWaiter.addNext({ this.execute() }, 5000)
        } else {
            TimerWaiter.addNext({ this.execute() }, 5000)
        }
        count += 1
    }

    override fun close() {
        for (player in World.world.onlinePlayers) {
            player.sendTypeMessage("Event", player.lang.trans("event.findme.win"))
        }
        EventManager.instance.finishCurrentEvent()
    }

    override fun onReceivePacket(manager: EventManager, player: Player, packet: String): Boolean = false

    override fun getEmptyCellForPlayer(player: Player): GameCase? = null

    override fun kickPlayer(player: Player) {}
}
