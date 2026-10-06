package org.starloco.locos.game.world

import org.starloco.locos.client.Account
import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.HouseData
import org.starloco.locos.database.data.game.TrunkData
import org.starloco.locos.database.data.login.PlayerData
import org.starloco.locos.database.data.login.AccountData
import org.starloco.locos.entity.map.House
import org.starloco.locos.entity.map.Trunk
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.kernel.Constant

/**
 * Created by Locos on 30/10/2016.
 */
class HouseManager {

    fun getHouseIdByCoord(map_id: Int, cell_id: Int): House? {
        for (house in World.world.houses.entries)
            if (house.value.mapId == map_id
                    && house.value.cellId == cell_id)
                return house.value
        return null
    }

    fun load(player: Player, newMapID: Int) {
        World.world.houses.entries.stream().filter { house -> house.value.mapId == newMapID }.forEach { house ->
            val packet = StringBuilder()
            packet.append("P").append(house.value.id).append("|")
            if (house.value.ownerId > 0) {
                val C = World.world.ensureAccountLoaded(house.value.ownerId)
                if (C == null)//Ne devrait pas arriver
                    packet.append("undefined;")
                else
                    packet.append(World.world.ensureAccountLoaded(house.value.ownerId)!!.pseudo).append(";")
            } else {
                packet.append(";")
            }

            if (house.value.sale > 0)//Si prix > 0
                packet.append("1")//Achetable
            else
                packet.append("0")//Non achetable

            if (house.value.guildId > 0) //Maison de guilde
            {
                val G = World.world.getGuild(house.value.guildId)
                if (G != null) {
                    val Gname = G.name
                    val Gemblem = G.emblem
                    if (G.getMembers().size < 10 && G.id > 2)//Ce n'est plus une maison de guilde
                    {
                        DatabaseManager.get(HouseData::class.java).updateGuild(house.value, 0, 0)
                    } else {
                        //Affiche le blason pour les membre de guilde OU Affiche le blason pour les non membre de guilde
                        if (player.getGuild() != null
                                && player.getGuild()!!.id == house.value.guildId
                                && house.value.canDo(Constant.H_GBLASON))//meme guilde
                        {
                            packet.append(";").append(Gname).append(";").append(Gemblem)
                        } else if (house.value.canDo(Constant.H_OBLASON))//Pas de guilde/guilde-différente
                        {
                            packet.append(";").append(Gname).append(";").append(Gemblem)
                        }
                    }
                }
            }
            SocketManager.GAME_SEND_hOUSE(player, packet.toString())

            if (house.value.ownerId == player.accID) {
                val packet1 = StringBuilder()
                packet1.append("L+|").append(house.value.id).append(";").append(house.value.access).append(";")

                if (house.value.sale <= 0) {
                    packet1.append("0;").append(house.value.sale)
                } else if (house.value.sale > 0) {
                    packet1.append("1;").append(house.value.sale)
                }
                SocketManager.GAME_SEND_hOUSE(player, packet1.toString())
            }
        }
    }

    fun buy(player: Player)//Acheter une maison
    {
        val house = player.inHouse

        if (World.world.houseManager.alreadyHaveHouse(player)) {
            SocketManager.GAME_SEND_Im_PACKET(player, "132;1")
            return
        }

        if (player.kamas < house!!.sale)
            return

        player.kamas = player.kamas - house!!.sale

        val kamas = Trunk.getTrunksByHouse(house!!).mapToLong { trunk ->
            if (house!!.ownerId > 0)
                trunk.moveTrunkToBank(World.world.ensureAccountLoaded(house!!.ownerId)!!)//Déplacement des items vers la banque

            val trunkKamas = trunk.kamas
            trunk.kamas = 0//Retrait kamas
            trunk.key = "-"//ResetPass
            trunk.ownerId = player.accID//ResetOwner
            DatabaseManager.get(TrunkData::class.java).update(trunk)
            trunkKamas
        }.sum()

        //Ajoute des kamas dans la banque du vendeur
        if (house!!.ownerId > 0) {
            val seller = World.world.ensureAccountLoaded(house!!.ownerId)
            seller!!.setBankKamas(seller!!.getBankKamas() + house!!.sale + kamas)

            val sellerPlayer = seller!!.currentPlayer
            if (sellerPlayer != null)//FIXME: change the packet (Im)
                SocketManager.GAME_SEND_MESSAGE(sellerPlayer, sellerPlayer.getLang().trans("game.game.world.housemanager.sell"))
            DatabaseManager.get(AccountData::class.java).update(seller)
        }

        closeBuy(player)
        SocketManager.GAME_SEND_STATS_PACKET(player)
        DatabaseManager.get(HouseData::class.java).buy(player, house!!)

        for (viewer in player.curMap.players)
            World.world.houseManager.load(viewer, viewer.curMap.id)

        DatabaseManager.get(PlayerData::class.java).update(player)
    }

    fun sell(P: Player, packet: String)//Vendre une maison
    {
        val h = P.inHouse
        val price = Integer.parseInt(packet)
        if (h!!.isHouse(P, h!!)) {
            SocketManager.GAME_SEND_hOUSE(P, "V")
            SocketManager.GAME_SEND_hOUSE(P, "SK" + h!!.id + "|" + price)
            //Vente de la maison
            DatabaseManager.get(HouseData::class.java).sell(h!!, price)
            //Rafraichir la map après la mise en vente
            for (z in P.curMap.players)
                load(z, z.curMap.id)
        }
    }

    fun closeCode(player: Player) {
        SocketManager.GAME_SEND_KODE(player, "V")
        player.exchangeAction = null
    }

    fun closeBuy(P: Player) {
        SocketManager.GAME_SEND_hOUSE(P, "V")
    }

    fun lockIt(player: Player, packet: String) {
        val action = player.exchangeAction

        if (action != null && action.getType() == ExchangeAction.LOCK_HOUSE) {
            val house = player.exchangeAction!!.getValue() as House
            if (house != null && house.isHouse(player, house)) {
                DatabaseManager.get(HouseData::class.java).updateCode(player, house, packet)
            }
            closeCode(player)
        }
    }

    fun parseHouseToGuild(P: Player): String {
        var isFirst = true
        var packet = "+"
        for (house in World.world.houses.entries) {
            if (house.value.guildId == P.getGuild()!!.id
                    && house.value.guildRights > 0) {
                var name = ""
                val id = house.value.ownerId
                if (id != -1) {
                    val a = World.world.ensureAccountLoaded(id)
                    if (a != null) {
                        name = a.pseudo
                    }
                }
                if (isFirst) {
                    packet += "${house.key};"
                    if (World.world.getPlayer(house.value.ownerId) == null)
                        packet += "$name;"
                    else
                        packet += World.world.getPlayer(house.value.ownerId)!!.getAccount().pseudo + ";"
                    packet += (World.world.getMap(house.value.houseMapId).x.toString() + ","
                            + World.world.getMap(house.value.houseMapId).y
                            + ";")
                    packet += "0;"
                    packet += house.value.guildRights
                    isFirst = false
                } else {
                    packet += "|"
                    packet += "${house.key};"
                    if (World.world.getPlayer(house.value.ownerId) == null)
                        packet += "$name;"
                    else
                        packet += World.world.getPlayer(house.value.ownerId)!!.getAccount().pseudo + ";"
                    packet += (World.world.getMap(house.value.houseMapId).x.toString() + ","
                            + World.world.getMap(house.value.houseMapId).y
                            + ";")
                    packet += "0;"
                    packet += house.value.guildRights
                }
            }
        }
        return packet
    }

    fun alreadyHaveHouse(P: Player): Boolean {
        for (house in World.world.houses.entries)
            if (house.value.ownerId == P.accID)
                return true
        return false
    }

    fun parseHG(P: Player, packet: String?) {
        val h = P.inHouse
        if (P.getGuild() == null)
            return
        if (packet != null) {
            if (packet[0] == '+') {
                //Ajoute en guilde
                val HouseMaxOnGuild = Math.floor((P.getGuild()!!.lvl / 10).toDouble()).toInt().toByte()
                if (houseOnGuild(P.getGuild()!!.id) >= HouseMaxOnGuild && P.getGuild()!!.id > 2) {
                    P.send("Im1151")
                    return
                }
                if (P.getGuild()!!.getMembers().size < 10 && P.getGuild()!!.id > 2) {
                    return
                }
                DatabaseManager.get(HouseData::class.java).updateGuild(h!!, P.getGuild()!!.id, 0)
                parseHG(P, null)
            } else if (packet[0] == '-') {
                //Retire de la guilde
                DatabaseManager.get(HouseData::class.java).updateGuild(h!!, 0, 0)
                parseHG(P, null)
            } else {
                DatabaseManager.get(HouseData::class.java).updateGuild(h!!, h!!.guildId, Integer.parseInt(packet))
                h!!.parseIntToRight(Integer.parseInt(packet))
            }
        } else {
            if (h!!.guildId <= 0) {
                SocketManager.GAME_SEND_hOUSE(P, "G" + h!!.id)
            } else if (h!!.guildId > 0) {
                SocketManager.GAME_SEND_hOUSE(P, ("G" + h!!.id + ";"
                        + P.getGuild()!!.name + ";"
                        + P.getGuild()!!.emblem + ";" + h!!.guildRights))
            }
        }
    }

    fun houseOnGuild(GuildID: Int): Byte {
        var i: Byte = 0
        for (house in World.world.houses.entries)
            if (house.value.guildId == GuildID)
                i++
        return i
    }

    fun leave(player: Player, packet: String) {
        val h = player.inHouse
        if (!h!!.isHouse(player, h!!))
            return
        val Pguid = Integer.parseInt(packet)
        val Target = World.world.getPlayer(Pguid)
        if (Target == null || !Target.isOnline || Target.fight != null
                || Target.curMap.id != player.curMap.id)
            return
        Target.teleport(h!!.mapId, h!!.cellId)
        SocketManager.GAME_SEND_Im_PACKET(Target, "018;" + player.name)
    }

    fun getHouseByPerso(player: Player): House? {
        for (house in World.world.houses.entries)
            if (house.value.ownerId == player.accID)
                return house.value
        return null
    }

    fun removeHouseGuild(guildId: Int) {
        World.world.houses.entries.stream().filter { h -> h.value.guildId == guildId }.forEach { h ->
            h.value.guildRights = 0
            h.value.guildId = 0
        }
        DatabaseManager.get(HouseData::class.java).removeGuild(guildId) //Supprime les maisons de guilde
    }
}
