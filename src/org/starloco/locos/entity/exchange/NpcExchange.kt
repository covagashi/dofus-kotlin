package org.starloco.locos.entity.exchange

import org.starloco.locos.auction.Auction
import org.starloco.locos.auction.AuctionManager
import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.login.PlayerData
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.entity.npc.NpcTemplate
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.ObjectTemplate
import java.util.stream.Collectors

class NpcExchange(var player: Player, n: NpcTemplate) {
    private var kamas1: Long = 0
    private var kamas2: Long = 0
    val items1 = ArrayList<World.Couple<Int, Int>>()
    private val items2 = ArrayList<World.Couple<Int, Int>>()
    private var ok1 = false
    private var ok2 = false
    var auction: Auction? = null
    var npc: NpcTemplate? = n

    init {
        AuctionManager.getInstance().onPlayerOpenExchange(player, this)
    }

    @Synchronized
    fun getKamas(b: Boolean): Long {
        if (b) return this.kamas2
        return this.kamas1
    }

    fun isPlayerOk(): Boolean {
        return ok1
    }

    @Synchronized
    fun toogleOK(paramBoolean: Boolean) {
        if (paramBoolean) {
            this.ok2 = !this.ok2
            SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
            AuctionManager.getInstance().onPlayerAccept(player, this)
        } else {
            this.ok1 = !this.ok1
            SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok1, this.player.id)
            if (AuctionManager.getInstance().onPlayerAccept(null, this))
                return
        }
        if (this.ok2 && this.ok1)
            apply()
    }

    @Synchronized
    fun setKamas(ok: Boolean, kamas: Long) {
        var kamas = kamas
        if (kamas < 0L)
            return
        this.ok1 = false
        this.ok2 = false
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok1, this.player.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
        if (ok) {
            this.kamas2 = kamas
            SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player.gameClient!!, 'G', "", kamas.toString())
            putAllGiveItem()
            return
        }
        if (kamas > this.player.kamas)
            return
        this.kamas1 = kamas
        SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player, 'G', "", kamas.toString())
        putAllGiveItem()
    }

    @Synchronized
    fun cancel() {
        if (this.player.account != null && this.player.gameClient != null)
            SocketManager.GAME_SEND_EV_PACKET(this.player.gameClient!!)
        this.player.exchangeAction = null
    }

    @Synchronized
    fun apply() {
        for (couple in items1) {
            if (couple.second == 0) continue
            if (World.world.getGameObject(couple.first)!!.position != Constant.ITEM_POS_NO_EQUIPED) continue
            if (!this.player.hasItemGuid(couple.first)) {
                couple.second = 0 //On met la quantit a 0 pour viter les problemes
                continue
            }
            val obj = World.world.getGameObject(couple.first)
            if (obj!!.quantity - couple.second < 1) {
                this.player.removeItem(couple.first)
                if (this.auction != null) {
                    this.auction!!.`object` = obj
                } else {
                    World.world.removeGameObject(obj!!.guid)
                }
                couple.second = obj!!.quantity
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(this.player, couple.first)
            } else {
                if (this.auction != null) {
                    this.auction!!.`object` = obj!!.getClone(couple.second, true)
                }
                obj!!.quantity = obj!!.quantity - couple.second
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this.player, obj)
            }
        }

        for (couple1 in items2) {
            if (couple1.second == 0) continue
            if (World.world.getObjTemplate(couple1.first) == null) continue
            val t = World.world.getObjTemplate(couple1.first)

            val obj1 = t!!.createNewItem(couple1.second, false)
            if (this.player.addItem(obj1!!, true, false))
                World.world.addGameObject(obj1)

            if (t!!.type == Constant.ITEM_TYPE_CERTIF_MONTURE) {
                //obj.setMountStats(this.player, null);
                val mount =
                    Mount(Constant.getMountColorByParchoTemplate(obj1!!.template!!.id), this.player.id, false)
                obj1.clearStats()
                obj1.stats.addOneStat(995, mount.id)
                obj1.txtStat[996] = this.player.name
                obj1.txtStat[997] = mount.name!!
                mount.setToMax()
            }

            SocketManager.GAME_SEND_Im_PACKET(this.player, "021;" + couple1.second + "~" + couple1.first)
        }
        this.player.exchangeAction = null
        SocketManager.GAME_SEND_EXCHANGE_VALID(this.player.gameClient!!, 'a')
        (DatabaseManager.get(PlayerData::class.java) as PlayerData).update(this.player)
    }

    @Synchronized
    fun addItem(obj: Int, qua: Int) {
        if (qua <= 0) return
        if (World.world.getGameObject(obj) == null) return
        this.ok2 = false
        this.ok1 = this.ok2
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok1, this.player.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
        val str = "$obj|$qua"
        val couple = getCoupleInList(items1, obj)
        if (couple != null) {
            couple.second += qua
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player, 'O', "+", "" + obj + "|" + couple.second)
            putAllGiveItem()
            return
        }
        SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player, 'O', "+", str)
        items1.add(World.Couple(obj, qua))
        putAllGiveItem()
    }

    @Synchronized
    fun removeItem(guid: Int, qua: Int) {
        if (qua < 0) return
        this.ok2 = false
        this.ok1 = this.ok2
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok1, this.player.id)
        SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
        if (World.world.getGameObject(guid) == null) return
        val couple = getCoupleInList(items1, guid)
        val newQua = couple!!.second - qua
        if (newQua < 1) {
            items1.remove(couple)
            putAllGiveItem()
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player, 'O', "-", "" + guid)
        } else {
            couple.second = newQua
            SocketManager.GAME_SEND_EXCHANGE_MOVE_OK(this.player, 'O', "+", "" + guid + "|" + newQua)
            putAllGiveItem()
        }
    }

    @Synchronized
    fun getQuaItem(obj: Int, b: Boolean): Int {
        val list: ArrayList<World.Couple<Int, Int>> = if (b) this.items2 else this.items1
        for (item in list)
            if (item.first == obj)
                return item.second
        return 0
    }

    @Synchronized
    fun clearItems() {
        if (this.items2.isEmpty()) return
        for (i in items2)
            SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player.gameClient!!, 'O', "-", i.first.toString() + "")
        this.kamas2 = 0
        this.items2.clear()
        if (this.ok2) {
            this.ok2 = false
            SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
        }
    }

    @Synchronized
    private fun getCoupleInList(items: ArrayList<World.Couple<Int, Int>>, guid: Int): World.Couple<Int, Int>? {
        for (couple in items)
            if (couple.first == guid)
                return couple
        return null
    }

    @Synchronized
    fun putAllGiveItem() {
        val itemsTemplates = items1.stream()
            // Get template ID for each object
            .map { World.Couple(World.world.getGameObject(it.first)!!.template!!.id, it.second) }
            .collect(Collectors.toList())

        if (kamas1 != 0L) {
            itemsTemplates.add(World.Couple(0, kamas1.toInt()))
        }
        val outcome = this.npc!!.barterOutcome(this.player, itemsTemplates)

        this.clearItems()
        if (outcome == null) {
            if (this.ok2) {
                this.ok2 = false
                SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
            }
            return
        }

        val str =
            outcome.first.toString() + "|" + outcome.second + "|" + outcome.first + "|" + World.world.getObjTemplate(
                outcome.first
            )!!.strTemplate
        if (outcome.first == 0) {
            this.kamas2 = outcome.second.toLong()
        } else {
            this.items2.add(World.Couple(outcome.first, outcome.second))
        }
        SocketManager.GAME_SEND_EXCHANGE_OTHER_MOVE_OK(this.player.gameClient!!, 'O', "+", str)

        if (!this.ok2) {
            this.ok2 = true
            SocketManager.GAME_SEND_EXCHANGE_OK(this.player.gameClient!!, this.ok2)
        }
    }
}
