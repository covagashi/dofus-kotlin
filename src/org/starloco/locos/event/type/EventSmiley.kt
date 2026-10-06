package org.starloco.locos.event.`type`

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.client.Player
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.entity.npc.Npc
import org.starloco.locos.event.EventManager
import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.`object`.ObjectTemplate
import org.starloco.locos.util.TimerWaiter
import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * Created by Locos on 02/10/2016.
 */
class EventSmiley(id: Byte, maxPlayers: Byte, name: String, description: String) : Event(id, maxPlayers, name, description) {

    private val emotes = ArrayList<Byte>()
    private val answers = ArrayList<World.Couple<Player, ArrayList<Byte>>>()
    private var state: Byte = 0
    private var count: Byte = 0
    private val cells = shortArrayOf(239, 253, 225, 267, 211, 281, 197, 295, 183, 309, 169)
    private var animator: Npc? = null

    init {
        this.map = World.world.getMap(9862)
    }

    override fun prepare() {
        this.answers.clear()
        this.emotes.clear()
        this.state = 0
        this.count = 0
        this.animator = this.map!!.addNpc(EventManager.NPC, 221, 1)

        if (this.map!!.players.isNotEmpty()) {
            SocketManager.GAME_SEND_ADD_NPC_TO_MAP(this.map!!, this.animator!!)
        }

        TimerWaiter.addNext({
            var ok = true
            while (EventManager.instance.state == EventManager.State.INITIALIZE || EventManager.instance.state == EventManager.State.PROCESSED) {
                moveAnimatorToCellId(if (ok) 137 else 221)
                Event.wait(2500)
                ok = !ok
            }
        }, 0, TimeUnit.SECONDS)
    }

    override fun perform() {
        this.moveAnimatorToCellId(179)
        Event.wait(1500)
        this.animator!!.orientation = 1.toByte()
        SocketManager.GAME_SEND_eD_PACKET_TO_MAP(this.map!!, this.animator!!.id, 1)
        Event.wait(1000)
        SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.welcome")
        Event.wait(3000)
        SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.start.1")
        Event.wait(4000)
        SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.start.2")
        Event.wait(5000)
        SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.start.3")
        Event.wait(1500)
        SocketManager.GAME_SEND_EMOTICONE_TO_MAP(this.map!!, this.animator!!.id, 10)
        Event.wait(2000)
        SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.start.4")
        Event.wait(3500)
        SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.start.5")
        Event.wait(4000)
        SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.start.6")
        Event.wait(5500)
        SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.start.7")
        this.execute()
    }

    override fun execute() {
        this.count = 0

        val participants = ArrayList(EventManager.instance.participants)
        var nbPlayers = participants.size

        for (player in participants)
            if (player != null && player.isOnline)
                this.answers.add(World.Couple<Player, ArrayList<Byte>>(player, ArrayList()))

        //239 cell emote pnj, 179 cell non emote
        while (nbPlayers > 1) {
            this.count++

            this.moveAnimatorToCellId(134)
            Event.wait(2000)

            this.emotes.add((Formulas.random.nextInt(14) + 1).toByte())

            for (e in this.emotes) {
                SocketManager.GAME_SEND_EMOTICONE_TO_MAP(this.map!!, this.animator!!.id, e.toInt())
                Event.wait(1500 - 100)
            }
            Event.wait(1500)


            this.moveAnimatorToCellId(179)
            Event.wait(1500)

            SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.run.1")
            Event.wait(750)

            this.initializeTurn((3000 + 1000 * this.count).toShort())
            Event.wait(1500 + 650 * this.count)
            SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.run.2")
            Event.wait(1500 + 650 * this.count)
            SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.run.3")
            this.state = 0

            for (pair in ArrayList(this.answers)) {
                if (pair.second.size == this.count.toInt()) {
                    var c = 0
                    var kick = false
                    for (b1 in pair.second) {
                        val b2 = this.emotes.getOrNull(c)
                        if (b2 == null) {
                            kick = true
                            break
                        } else if (b1 != b2) {
                            kick = true
                            break
                        }
                        c++
                    }
                    if (kick) {
                        this.kickPlayer(pair.first)
                        nbPlayers--
                    } else {
                        pair.second.clear()
                        pair.first.sendMessage(pair.first.lang.trans("event.type.eventsmiley.congratulation"))
                    }
                } else {
                    this.kickPlayer(pair.first)
                    nbPlayers--
                }
            }

            Event.wait(1000)
            if (EventManager.instance.participants.size > 1) {
                SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.next")
            }
            Event.wait(2000)
        }

        this.close()
    }

    override fun close() {
        if (EventManager.instance.participants.isNotEmpty()) {
            val winner = EventManager.instance.participants[0]

            SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.win")
            winner.sendMessage(winner.lang.trans("event.type.eventsmiley.winner"))
            val template = World.world.getObjTemplate(EventManager.TOKEN)

            if (template != null) {
                val `object` = template.createNewItem(1, false)

                if (`object` != null && winner.addItem(`object`, true, false)) {
                    World.world.addGameObject(`object`)
                }
            }
        } else {
            SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(this.map!!, "", this.animator!!.id, "Event", "event.type.eventsmiley.win.none")
        }

        Event.wait(2000)
        this.moveAnimatorToCellId(344)
        Event.wait(2500)
        this.map!!.removeNpcOrMobGroup(this.animator!!.id)
        this.map!!.send("GM|-" + this.animator!!.id)
        this.map!!.send("GV")
        EventManager.instance.finishCurrentEvent()
    }

    override fun getEmptyCellForPlayer(player: Player): GameCase? =
        map!!.getCase(this.cells[(count++).toInt()].toInt())

    override fun kickPlayer(player: Player) {
        EventManager.instance.participants.remove(player)
        val iterator = this.answers.iterator()

        while (iterator.hasNext()) {
            val pair = iterator.next()

            if (pair.first.id == player.id) {
                this.map!!.send("GA;208;" + player.id + ";" + player.curCell.cellId + ",2916,11,8,1")
                player.sendMessage(player.lang.trans("event.type.eventsmiley.lose"))
                player.teleportOldMap()
                player.blockMovement = false
                iterator.remove()
                break
            }
        }
    }

    override fun onReceivePacket(manager: EventManager, player: Player, packet: String): Boolean {
        if (packet.startsWith("BS") && this.state.toInt() == 1) {
            val emote = packet.substring(2).toByte()
            for (pair in this.answers) {
                if (pair.first.id == player.id) {
                    pair.second.add(emote)
                    if (pair.second.size == this.count.toInt())
                        player.sendMessage(player.lang.trans("event.type.eventsmiley.count.ok"))
                    break
                }
            }

        }
        return false
    }

    private fun initializeTurn(time: Short) {
        this.state = 1
        for (player in EventManager.instance.participants) {
            player.send("GTS" + player.id + "|" + time)
        }
    }

    private fun moveAnimatorToCellId(cellId: Int) {
        val path: String?

        try {
            path = PathFinding.getShortestStringPathBetween(this.map!!, this.animator!!.cellId, cellId, 20)
        } catch (e: Exception) {
            return
        }

        if (path != null) {
            this.animator!!.cellId = cellId
            SocketManager.GAME_SEND_GA_PACKET_TO_MAP(this.map!!, "0", 1, this.animator!!.id, path)
        }
    }
}
