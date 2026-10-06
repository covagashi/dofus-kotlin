package org.starloco.locos.event

import org.starloco.locos.common.SocketManager
import org.starloco.locos.common.Formulas
import org.starloco.locos.client.Player
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.login.EventData
import org.starloco.locos.event.type.Event
import org.starloco.locos.game.scheduler.Updatable
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Config
import org.starloco.locos.util.TimerWaiter
import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * Created by Locos on 02/10/2016.
 */
class EventManager private constructor() : Updatable<Long>(60000) {

    enum class State {
        WAITING, INITIALIZE, PROCESSED, STARTED, FINISHED
    }

    /** EventManager  **/

    val events: Array<Event?> = (DatabaseManager.get(EventData::class.java) as EventData).load()
    var state = State.WAITING
        private set
    var current: Event? = null
        private set
    private var lastest: Event? = null
    private var count: Short = 0
    val participants: MutableList<Player> = ArrayList()

    fun getCurrentEvent(): Event? = current

    fun startNewEvent(temp: Event?) {
        val event = temp ?: this.events[Formulas.random.nextInt(this.events.size)]

        if (event != null) {
            if (this.events.size > 1 && this.lastest != null && event.getEventId() == this.lastest!!.getEventId()) {
                this.startNewEvent(null)
                return
            }

            event.prepare()
            this.lastTime = System.currentTimeMillis()
            this.current = event

            if (this.current!!.getMaxPlayers().toInt() == -1) {
                World.world.sendMessageToAll("event.eventmanager.start.wait", event.getEventName())
                this.state = State.STARTED
                TimerWaiter.addNext({ this.current!!.perform() }, 0, TimeUnit.SECONDS)
            } else {
                this.state = State.PROCESSED
                World.world.sendMessageToAll("event.eventmanager.start.now", event.getEventName())
            }
        } else {
            this.startNewEvent(null)
        }
    }

    @Synchronized
    private fun startCurrentEvent() {
        if (this.state == State.STARTED)
            return
        this.state = State.STARTED

        if (!this.hasEnoughPlayers()) {
            this.count = 0
            this.lastTime = System.currentTimeMillis()
            this.state = State.PROCESSED
        } else if (this.moveAllPlayersToEventMap(true)) {
            this.lastTime = System.currentTimeMillis()
            TimerWaiter.addNext({ this.current!!.perform() }, 0, TimeUnit.SECONDS)
        }
    }

    fun finishCurrentEvent() {
        this.participants.stream().filter { it != null }.forEach { player ->
            player.teleportOldMap()
            player.blockMovement = false
        }

        this.lastest = this.current
        this.current = null
        this.lastTime = System.currentTimeMillis()
        this.count = 0
        this.state = State.WAITING
    }

    @Synchronized
    fun subscribe(player: Player): Byte {
        if (this.current == null || this.state == State.WAITING) {
            return 0
        } else {
            val current = this.current!!
            if (this.state == State.PROCESSED) {
                if (this.participants.size >= current.getMaxPlayers()) {
                    player.sendMessage(player.lang.trans("event.eventmanager.subscribe.full", current.getEventName()))
                } else if (this.participants.contains(player)) {
                    this.participants.remove(player)
                    player.sendMessage(player.lang.trans("event.eventmanager.unsubscribe", current.getEventName()))
                } else if (this.hasSameIP(player)) {
                    player.sendMessage(player.lang.trans("event.eventmanager.subscribe.already.reseau"))
                } else {
                    this.participants.add(player)
                    player.sendMessage(player.lang.trans("event.eventmanager.subscribe.already", current.getEventName()))

                    if (this.participants.size >= current.getMaxPlayers()) {
                        this.startCurrentEvent()
                    } else {
                        this.participants.forEach { target -> target.sendMessage(target.lang.trans("event.eventmanager.wait.player", current.getMaxPlayers() - this.participants.size)) }
                    }
                }
            } else {
                player.sendMessage(player.lang.trans("event.eventmanager.start.already", current.getEventName()))
            }
        }
        return 1
    }

    private fun hasSameIP(player: Player?): Boolean {
        if (player?.account != null) {
            val ip = player.account.currentIp

            if (ip == "127.0.0.1")
                return false
            for (target in this.participants) {
                if (target?.account != null) {
                    return ip == target.account.currentIp
                }
            }
        }
        return false
    }

    private fun hasEnoughPlayers(): Boolean {
        if (this.current == null)
            return false
        val percent = (100 * this.participants.size) / this.current!!.getMaxPlayers()
        return percent >= 30
    }

    override fun update() {
        if (Config.modeEvent && this.verify()) {
            if (this.state == State.WAITING) {
                val result = (Config.timeBetweenEvent - (++count).toInt()).toShort()
                if (result.toInt() == 0) {
                    this.count = 0
                    this.lastTime = System.currentTimeMillis()
                    this.state = State.INITIALIZE
                    TimerWaiter.addNext({ startNewEvent(null) }, 0, TimeUnit.SECONDS)
                } else if (result.toInt() == 60 || result.toInt() == 30 || result.toInt() == 15 || result.toInt() == 5) {
                    World.world.sendMessageToAll("event.eventmanager.start", result.toString())
                }
            } else if (this.state == State.PROCESSED) {
                val result = ((if (this.hasEnoughPlayers()) 5 else 10) - (++count).toInt()).toShort()
                this.moveAllPlayersToEventMap(false)

                if (result <= 0) {
                    this.startCurrentEvent()
                } else if (result.toInt() == 1 && this.hasEnoughPlayers()) {
                    for (player in this.participants) {
                        player.sendMessage(player.lang.trans("event.eventmanager.start", "1"))
                    }
                }
            }
        }
    }

    override fun get(): Long = lastTime

    private fun moveAllPlayersToEventMap(teleport: Boolean): Boolean {
        var ok = true
        val afk = if (teleport) StringBuilder() else null

        val iterator1 = this.participants.iterator()

        while (iterator1.hasNext()) {
            val player = iterator1.next()
            if (player.fight != null || !player.isOnline || player.isGhost || player.doAction) {
                ok = false
                iterator1.remove()
                player.sendMessage(player.lang.trans("event.eventmanager.player.unavailable"))
                player.sendMessage(player.lang.trans("event.eventmanager.player.eject"))

                if (teleport) {
                    afk!!.append(if (afk.isEmpty()) "<b>${player.name}</b>" else ", <b>${player.name}</b>")
                }
            }
        }

        if (!ok || !teleport) {
            if (teleport) {
                this.participants.forEach { player -> player.sendMessage(player.lang.trans("event.eventmanager.player.afk", afk.toString())) }
                World.world.onlinePlayers.stream().filter { target -> !afk.toString().contains(target.name) }
                    .forEach { target -> target.sendMessage(target.lang.trans("event.eventmanager.subscribe.last.secondes", this.current!!.getEventName())) }
            }
            return false
        }

        val iterator2 = this.participants.iterator()

        while (iterator2.hasNext()) {
            val player = iterator2.next()

            if (player.fight == null && player.isOnline && !player.isGhost && !player.doAction) {
                player.setOldPosition()
                player.blockMovement = true
                var cell = this.current!!.getEmptyCellForPlayer(player)
                if (cell == null)
                    cell = this.current!!.getMap()!!.getCase(this.current!!.getMap()!!.randomFreeCellId)!!
                player.teleport(this.current!!.getMap()!!.id, cell.cellId)
                SocketManager.GAME_SEND_eD_PACKET_TO_MAP(this.current!!.getMap()!!, player.id, 4)
            } else {
                ok = false
                iterator2.remove()
                player.sendMessage(player.lang.trans("event.eventmanager.player.unavailable"))
                player.sendMessage(player.lang.trans("event.eventmanager.player.eject"))
            }
        }

        return ok
    }

    companion object {
        const val TOKEN = 50007
        const val NPC = 16000

        @JvmStatic
        val instance: EventManager = EventManager()

        @JvmStatic
        fun isInEvent(player: Player): Boolean {
            if (Config.modeEvent && instance.state == State.STARTED)
                for (target in instance.participants)
                    if (target.id == player.id)
                        return true
            return false
        }
    }
}
