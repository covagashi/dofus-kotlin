package org.starloco.locos.auction

import org.starloco.locos.client.Account
import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.AuctionData
import org.starloco.locos.entity.exchange.NpcExchange
import org.starloco.locos.entity.npc.Npc
import org.starloco.locos.game.scheduler.Updatable
import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.util.TimerWaiter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.LinkedList
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(AuctionManager::class.java)

/**
 * Created by Locos on 31/01/2018.
 */
class AuctionManager : Updatable<Void?>(10000) {

    private val auctions = LinkedList<Auction>()
    private var current: Auction? = null
    private val map: Int = 10111
    private var task: ScheduledFuture<*>? = null
    private var counter: Byte = 0

    fun getAuctions(): LinkedList<Auction> = auctions

    fun talk(key: String, `object`: GameObject?, tradeTalk: Boolean, vararg params: Any?) {
        val map = World.world.getMap(this.map)
        val npc = map.getNpcByTemplateId(9605)!!

        for (player in map.players) {
            var msg = player.lang.trans(key, *params)
            if (`object` != null) msg = getTalkStringObject(`object`, msg)
            player.send("cMK|" + npc.id + "|Commissaire|" + msg + "|")
        }
        if (tradeTalk) {
            for (player in World.world.onlinePlayers) {
                var msg = player.lang.trans(key, *params)
                if (`object` != null) msg = getTalkStringObject(`object`, msg)
                player.send("cMK:|" + npc.id + "|Commissaire|" + msg + "|")
            }
        }
    }

    fun talkNext() {
        val map = World.world.getMap(this.map)
        val npc = map.getNpcByTemplateId(9605)!!
        for (player in map.players) {
            val msg = player.lang.trans("game.auction.auctionmanager.stop.none") + (if (this.auctions.size == 0) "" else player.lang.trans("game.auction.auctionmanager.stop.next"))
            player.send("cMK|" + npc.id + "|Commissaire|" + msg + "|")
        }
        for (player in World.world.onlinePlayers) {
            val msg = player.lang.trans("game.auction.auctionmanager.stop.none") + (if (this.auctions.size == 0) "" else player.lang.trans("game.auction.auctionmanager.stop.next"))
            player.send("cMK:|" + npc.id + "|Commissaire|" + msg + "|")
        }
    }

    fun talk(player: Player, msg: String) {
        val map = World.world.getMap(this.map)
        val npc = map.getNpcByTemplateId(9605)!!
        player.send("cMK|" + npc.id + "|Commissaire|" + msg + "|")
    }

    private fun getTalkStringObject(`object`: GameObject, msg: String): String {
        return "°0" + msg + "|" + `object`.template!!.id + "!" + `object`.encodeStats()
    }

    private fun currentIsAvailable(): Boolean = auctionIsAvailable(current)

    private fun auctionIsAvailable(auction: Auction?): Boolean {
        val available = auction != null && auction.owner != null && auction.`object` != null
        if (!available) log.error(if (auction == null) "AuctionM : current is null" else "AuctionM : " + auction.`object` + " " + auction.owner + " ")
        return available
    }

    override fun update() {
        if (this.verify()) {
            val date = Calendar.getInstance().time
            val hour = (SimpleDateFormat("HH").format(date)).toInt()

            if (hour >= 16 && hour < 23) {
                if (current == null) {
                    this.current = this.auctions.pollFirst()

                    if (current != null && currentIsAvailable()) {
                        this.task = TimerWaiter.addNext({ check(null, -1) }, 0, TimeUnit.MILLISECONDS)
                    } else {
                        this.current = null
                    }
                }
            }
        }
    }

    @Synchronized
    private fun check(player: Player?, kamas: Int) {
        if (current != null && currentIsAvailable()) {
            if (!this.start(player, kamas))
                if (!this.newAuction(player, kamas))
                    if (!this.counter(player, kamas))
                        this.stop()
        }
    }

    private fun start(player: Player?, kamas: Int): Boolean {
        if (kamas == -1 && currentIsAvailable() && player != null) { // Lancement de l'enchère
            val current = this.current!!
            this.talk("game.auction.auctionmanager.start", current.`object`, true, current.`object`!!.quantity, current.price, current.owner!!.name)
            this.counter = 0
            this.task = TimerWaiter.addNext({ check(player, 0) }, 8, TimeUnit.SECONDS)
            return true
        }
        return false
    }

    private fun newAuction(player: Player?, kamas: Int): Boolean {
        val current = this.current!!
        if (kamas > 0 && kamas != current.price) {
            if (current.customer != null) {
                current.customer!!.addKamas(current.price.toLong())
                SocketManager.GAME_SEND_STATS_PACKET(current.customer!!)
            }
            counter = 0
            current.price = kamas
            current.customer = player

            val newPrice = (current.price * 0.05).toInt() + current.price

            this.talk("game.auction.auctionmanager.newAuction", current.`object`, true, current.`object`!!.quantity, current.price, current.customer!!.name, newPrice)
            this.task = TimerWaiter.addNext({ check(player, 0) }, 8, TimeUnit.SECONDS)
            return true
        }
        return false
    }

    private fun counter(player: Player?, kamas: Int): Boolean {
        if (counter.toInt() == 0 || counter.toInt() == 1) {
            if (this.currentIsAvailable()) {
                val current = this.current!!
                if (current.customer != null)
                    this.talk("game.auction.auctionmanager.counter.1", current.`object`, false, current.`object`!!.quantity, current.price, current.customer!!.name, counter + 1)
                else
                    this.talk("game.auction.auctionmanager.counter.2", current.`object`, false, current.`object`!!.quantity, current.price, counter + 1)
                this.counter++
                this.task = TimerWaiter.addNext({ check(player, kamas) }, 8, TimeUnit.SECONDS)
                return true
            }
        }
        counter = 0
        return false
    }

    private fun stop() {
        if (this.currentIsAvailable()) {
            var target = this.current!!.customer
            val `object` = this.current!!.`object`!!

            if (target != null) {
                target.addInBank(`object`.guid, `object`.quantity, true)
                target.send("M121")

                val owner = this.current!!.owner!!.account
                owner.setBankKamas(owner.getBankKamas() + this.current!!.price)
                if (owner.currentPlayer != null)
                    owner.currentPlayer!!.send("Im065;" + this.current!!.price + "~" + `object`.template!!.id)
                (DatabaseManager.get(AuctionData::class.java) as AuctionData).delete(this.current!!)
                this.talk("game.auction.auctionmanager.stop.felicitation", null, false, target.name)
            } else {
                if (this.current!!.retry.toInt() >= 3) {
                    target = this.current!!.owner
                    if (target!!.isOnline)
                        target.send("Im067")
                    target.addInBank(`object`.guid, `object`.quantity, true)
                    (DatabaseManager.get(AuctionData::class.java) as AuctionData).delete(this.current!!)
                } else {
                    this.current!!.incRetry()
                    this.auctions.addLast(this.current!!)
                    (DatabaseManager.get(AuctionData::class.java) as AuctionData).update(this.current!!)
                }
                if (target != null)
                    this.talkNext()
            }
        }
        this.current = null
    }

    override fun get(): Void? = null

    fun onPlayerLoadMap(player: Player) {
        if (!this.isValid(player)) return

        if (this.current != null && this.currentIsAvailable()) {
            val current = this.current!!
            val date = Calendar.getInstance().time
            val hour = (SimpleDateFormat("HH").format(date)).toInt()
            if (hour >= 16 && hour < 23) {
                player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.encherie.infos"))
            }
            if (current.customer == null) {
                val msg = player.lang.trans("game.auction.auctionmanager.start", current.`object`!!.quantity, current.price, current.owner!!.name)
                this.talk(player, getTalkStringObject(current.`object`!!, msg))
            } else {
                val msg = player.lang.trans("game.auction.auctionmanager.newAuction", current.`object`!!.quantity, current.price, current.customer!!.name, "")
                this.talk(player, getTalkStringObject(current.`object`!!, msg))
            }
        }
    }

    @Synchronized
    fun onPlayerChat(player: Player, msg: String) {
        var msg = msg
        if (!this.isValid(player)) return
        if (this.current != null && this.currentIsAvailable() && (msg.startsWith("moi") || msg.startsWith("me"))) {
            try {
                if (player.id == this.current!!.owner!!.id) return
                msg = msg.replace("|", "")

                val percent = (this.current!!.price * 0.05).toInt()
                var price = (if (percent <= 1) 1 else percent) + this.current!!.price

                if (msg.length > 4 && msg.contains(" ")) {
                    val split = msg.split(" ")
                    if (split.size == 2) {
                        try {
                            if ((split[1]).toInt() > price)
                                price = (split[1]).toInt()
                        } catch (ignored: Exception) {
                        }
                    }
                }

                if (player.kamas < price) {
                    player.send("Im063")
                    return
                }

                player.addKamas(-price.toLong())
                player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.encherie", price))
                SocketManager.GAME_SEND_STATS_PACKET(player)
                this.task?.cancel(true)
                this.task = TimerWaiter.addNext({ this.check(player, price) }, 100, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                log.error("unexpected error", e)
                }
        }
    }

    fun onPlayerCommand(player: Player, info: Array<String>) {
        if (!this.isValid(player)) return
        if (player.exchangeAction != null) {
            val exchange = player.exchangeAction!!.getValue() as NpcExchange

            if (info.size > 1) {
                if (info[1].equals("show", ignoreCase = true)) {
                    var count = 0
                    for (auction in this.auctions) {
                        if (!this.auctionIsAvailable(auction)) continue
                        if (count == 100) break
                        count++

                        val `object` = auction.`object`
                        val str = `object`!!.guid.toString() + "|" + `object`.quantity + "|" + `object`.template!!.id + "|" +
                            auction.`object`!!.encodeStats() + ",3db#0#0#0#" + auction.owner!!.name + ",c2#" + auction.price.toString(16) + "#0#0#0"

                        val strFinal = str
                        TimerWaiter.addNext({ SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(player.gameClient!!, 'O', "+", strFinal) }, (count * 100).toLong())
                    }
                    player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.waitingqueue", auctions.size))
                } else {
                    try {
                        if (exchange != null) {
                            val price = (info[1]).toInt()
                            if (price < 10 || price > 1_000_000) throw Exception()

                            if (this.auctions.stream().filter { it.owner != null && it.owner!!.id == player.id }.count() >= 3) {
                                player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.too.much.items"))
                            }

                            val auction = Auction(price, player, null, 0.toByte())
                            exchange.auction = auction
                            if (exchange.isPlayerOk())
                                exchange.toogleOK(true)
                            player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.confirm", price))
                        }
                    } catch (e: Exception) {
                        player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.price"))
                    }
                }
            }
        }
    }

    fun onPlayerOpenExchange(player: Player, exchange: NpcExchange) {
        if (!this.isValid(player)) return

        player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.infos.1"))
        player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.infos.2"))
        player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.infos.3"))
    }

    fun onPlayerChangeItemInNpcExchange(player: Player, `object`: GameObject?): Boolean {
        if (!this.isValid(player)) return false
        if (`object` != null && `object`.template != null && `object`.template!!.level <= 50) return true
        if (player.exchangeAction != null) {
            val exchange = player.exchangeAction!!.getValue() as NpcExchange
            if (exchange != null) {
                if (exchange.items1.size >= 1) return true
                else player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.enterprice"))
            }
        }
        return false
    }

    fun onPlayerAccept(player: Player?, exchange: NpcExchange?): Boolean {
        if (player != null) if (!this.isValid(player)) return false
        if (exchange != null) {
            val auction = exchange.auction

            if (auction != null && exchange.items1.size == 1) {
                if (player == null) {
                    exchange.toogleOK(true)
                    (DatabaseManager.get(AuctionData::class.java) as AuctionData).insert(auction)
                    return true
                } else {
                    this.auctions.addLast(auction)
                    player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.deposit"))
                    return false
                }
            }
        }
        return false
    }

    private fun isValid(player: Player): Boolean {
        val map = World.world.getMap(this.map)
        if (!map.players.contains(player)) return false
        if (player.fight != null || player.dead.toInt() == 1 || player.isGhost
            || (player.exchangeAction != null && player.exchangeAction!!.getValue() !is NpcExchange)) {
            player.sendTypeMessage("Auction", player.lang.trans("game.auction.auctionmanager.isvalid"))
            return false
        }
        return true
    }

    companion object {
        private var instance: AuctionManager? = null

        @JvmStatic
        fun getInstance(): AuctionManager = instance ?: AuctionManager().also { instance = it }
    }
}
