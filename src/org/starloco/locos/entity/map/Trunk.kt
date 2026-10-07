package org.starloco.locos.entity.map

import org.starloco.locos.client.Account
import org.starloco.locos.client.Player
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.BankData
import org.starloco.locos.database.data.game.TrunkData
import org.starloco.locos.database.data.login.PlayerData
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.GameObject
import java.util.Optional
import java.util.stream.Stream
import org.starloco.locos.common.splitJ

class Trunk(
    var id: Int,
    var houseId: Int,
    var mapId: Int,
    var cellId: Int
) {

    var key: String? = null
    var ownerId: Int = 0
    var kamas: Long = 0
    var player: Player? = null
    var `object`: MutableMap<Int, GameObject> = HashMap()

    fun setObjects(`object`: String) {
        for (item in `object`.splitJ("|")) {
            if (item == "")
                continue
            val infos = item.split(":")
            val guid = infos[0].toInt()

            val obj = World.world.getGameObject(guid) ?: continue
            this.`object`[obj.guid] = obj
        }
    }

    fun Lock(P: Player) {
        P.exchangeAction = ExchangeAction(ExchangeAction.LOCK_TRUNK, this)
        SocketManager.GAME_SEND_KODE(P, "CK1|8")
    }

    fun enter(player: Player) {
        if (player.fight != null || player.exchangeAction != null)
            return

        val house = World.world.getHouse(houseId)

        if (house!!.ownerId == player.accID && this.ownerId != player.accID)
            this.ownerId = player.accID
        if (this.ownerId == player.accID || (player.guild != null && player.guild!!.id == house!!.guildId && house!!.canDo(
                Constant.C_GNOCODE
            ))
        ) {
            player.exchangeAction = ExchangeAction(ExchangeAction.IN_TRUNK, this)
            open(player, "-", true)
        } else if (player.guild != null && player.guild!!.id == house!!.guildId && !house!!.canDo(Constant.C_GNOCODE)) {
            player.exchangeAction = ExchangeAction(ExchangeAction.IN_TRUNK, this)
            SocketManager.GAME_SEND_KODE(player, "CK0|8")
        } else if (player.guild == null && house!!.canDo(Constant.C_OCANTOPEN)) {
            SocketManager.GAME_SEND_MESSAGE(player, player.lang.trans("area.map.entity.trunk.enter.guilde.only"))
        } else if (this.ownerId > 0) {
            SocketManager.GAME_SEND_KODE(player, "CK0|8")
        }
    }

    fun isTrunk(P: Player, t: Trunk): Boolean//Savoir si c'est son coffre
    {
        return t.ownerId == P.accID
    }

    fun parseToTrunkPacket(): String {
        val packet = StringBuilder()

        for (obj in this.`object`.values)
            packet.append("O").append(obj.encodeItem()).append(";")
        if (kamas != 0L)
            packet.append("G").append(kamas)
        return packet.toString()
    }

    fun addInTrunk(guid: Int, qua: Int, P: Player) {
        var str = ""
        if (qua <= 0)
            return
        if ((P.exchangeAction!!.getValue() as Trunk).id != id)
            return

        if (this.`object`.size >= 10000) // Le plus grand c'est pour si un admin ajoute des objets via la bdd...
        {
            SocketManager.GAME_SEND_MESSAGE(P, P.lang.trans("area.map.entity.trunk.addintrunk.max"))
            return
        }

        val PersoObj = World.world.getGameObject(guid) ?: return
        if (PersoObj.isAttach) return
        //Si le joueur n'a pas l'item dans son sac ...
        if (P.items[guid] == null)
            return

        //Si c'est un item quip ...
        if (PersoObj.position != Constant.ITEM_POS_NO_EQUIPED)
            return

        var TrunkObj = getSimilarTrunkItem(PersoObj)
        val newQua = PersoObj.quantity - qua
        if (TrunkObj == null)//S'il n'y pas d'item du meme Template
        {
            //S'il ne reste pas d'item dans le sac
            if (newQua <= 0) {
                //On enleve l'objet du sac du joueur
                P.removeItem(PersoObj.guid)
                //On met l'objet du sac dans le coffre, avec la meme quantit
                this.`object`[PersoObj.guid] = PersoObj
                str = ("O+" + PersoObj.guid + "|" + PersoObj.quantity
                        + "|" + PersoObj.template!!.id + "|"
                        + PersoObj.encodeStats())
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(P, guid)
            } else
            //S'il reste des objets au joueur
            {
                //on modifie la quantit d'item du sac
                PersoObj.quantity = newQua
                //On ajoute l'objet au coffre et au monde
                TrunkObj = PersoObj.getClone(qua, true)!!
                World.world.addGameObject(TrunkObj)
                this.`object`[TrunkObj.guid] = TrunkObj
                //Envoie des packets
                str = ("O+" + TrunkObj.guid + "|" + TrunkObj.quantity
                        + "|" + TrunkObj.template!!.id + "|"
                        + TrunkObj.encodeStats())
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, PersoObj)
            }
        } else//S'il y a un item equivalent dans le coffre
        {
            //S'il ne reste pas d'item dans le sac
            if (newQua <= 0) {
                //On enleve l'objet du sac du joueur
                P.removeItem(PersoObj.guid)
                //On enleve l'objet du monde
                World.world.removeGameObject(PersoObj.guid)
                //On ajoute la quantit a l'objet dans le coffre
                TrunkObj.quantity = TrunkObj.quantity + PersoObj.quantity
                //on envoie l'ajout au coffre de l'objet
                str = ("O+" + TrunkObj.guid + "|" + TrunkObj.quantity
                        + "|" + TrunkObj.template!!.id + "|"
                        + TrunkObj.encodeStats())
                //on envoie la supression de l'objet du sac au joueur
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(P, guid)
            } else
            //S'il restait des objets
            {
                //on modifie la quantit d'item du sac
                PersoObj.quantity = newQua
                TrunkObj.quantity = TrunkObj.quantity + qua
                str = ("O+" + TrunkObj.guid + "|" + TrunkObj.quantity
                        + "|" + TrunkObj.template!!.id + "|"
                        + TrunkObj.encodeStats())
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, PersoObj)
            }
        }

        for (perso in P.curMap.players)
            if (perso.exchangeAction != null && perso.exchangeAction!!.getType() == ExchangeAction.IN_TRUNK && id == (perso.exchangeAction!!.getValue() as Trunk).id)
                SocketManager.GAME_SEND_EsK_PACKET(perso, str)

        SocketManager.GAME_SEND_Ow_PACKET(P)
        (DatabaseManager.get(TrunkData::class.java) as TrunkData).update(this)
        (DatabaseManager.get(PlayerData::class.java) as PlayerData).update(P)
    }

    fun removeFromTrunk(guid: Int, qua: Int, P: Player) {
        if (qua <= 0)
            return
        if ((P.exchangeAction!!.getValue() as Trunk).id != id)
            return
        val TrunkObj = World.world.getGameObject(guid) ?: return
        //Si le joueur n'a pas l'item dans son coffre

        if (this.`object`[guid] == null)
            return

        var PersoObj = P.getSimilarItem(TrunkObj)
        var str = ""
        val newQua = TrunkObj.quantity - qua

        if (PersoObj == null)//Si le joueur n'avait aucun item similaire
        {
            //S'il ne reste rien dans le coffre
            if (newQua <= 0) {
                //On retire l'item du coffre

                this.`object`.remove(guid)
                //On l'ajoute au joueur
                P.items[guid] = TrunkObj

                //On envoie les packets
                SocketManager.GAME_SEND_OAKO_PACKET(P, TrunkObj)
                str = "O-$guid"
            } else
            //S'il reste des objets dans le coffre
            {
                //On cre une copy de l'item dans le coffre
                PersoObj = TrunkObj.getClone(qua, true)!!
                //On l'ajoute au monde
                World.world.addGameObject(PersoObj)
                //On retire X objet du coffre
                TrunkObj.quantity = newQua
                //On l'ajoute au joueur
                P.items[PersoObj.guid] = PersoObj

                //On envoie les packets
                SocketManager.GAME_SEND_OAKO_PACKET(P, PersoObj)
                str = ("O+" + TrunkObj.guid + "|" + TrunkObj.quantity
                        + "|" + TrunkObj.template!!.id + "|"
                        + TrunkObj.encodeStats())
            }
        } else {
            //S'il ne reste rien dans le coffre
            if (newQua <= 0) {
                //On retire l'item du coffre

                this.`object`.remove(TrunkObj.guid)

                World.world.removeGameObject(TrunkObj.guid)
                //On Modifie la quantit de l'item du sac du joueur
                PersoObj.quantity = PersoObj.quantity + TrunkObj.quantity
                //On envoie les packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, PersoObj)
                str = "O-$guid"
            } else
            //S'il reste des objets dans le coffre
            {
                //On retire X objet du coffre
                TrunkObj.quantity = newQua
                //On ajoute X objets au joueurs
                PersoObj.quantity = PersoObj.quantity + qua
                //On envoie les packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, PersoObj)
                str = ("O+" + TrunkObj.guid + "|" + TrunkObj.quantity
                        + "|" + TrunkObj.template!!.id + "|"
                        + TrunkObj.encodeStats())
            }
        }

        for (perso in P.curMap.players)
            if (perso.exchangeAction != null && perso.exchangeAction!!.getType() == ExchangeAction.IN_TRUNK && id == (perso.exchangeAction!!.getValue() as Trunk).id)
                SocketManager.GAME_SEND_EsK_PACKET(perso, str)

        SocketManager.GAME_SEND_Ow_PACKET(P)
        (DatabaseManager.get(TrunkData::class.java) as TrunkData).update(this)
        (DatabaseManager.get(PlayerData::class.java) as PlayerData).update(P)
    }

    private fun getSimilarTrunkItem(obj: GameObject): GameObject? {
        for (`object` in this.`object`.values)
            if (World.world.conditionManager.stackIfSimilar(`object`, obj, true))
                return `object`
        return null
    }

    fun parseTrunkObjetsToDB(): String {
        val str = StringBuilder()
        for (entry in this.`object`.entries) {
            val obj = entry.value
            str.append(obj.guid).append("|")
        }
        return str.toString()
    }

    fun moveTrunkToBank(Cbank: Account) {
        for (obj in this.`object`.entries)
            Cbank.bank.add(obj.value)
        this.`object`.clear()
        (DatabaseManager.get(TrunkData::class.java) as TrunkData).update(this)
        (DatabaseManager.get(BankData::class.java) as BankData).update(Cbank)
    }

    companion object {
        @JvmStatic
        fun closeCode(P: Player) {
            SocketManager.GAME_SEND_KODE(P, "V")
        }

        @JvmStatic
        fun getTrunkIdByCoord(map_id: Int, cell_id: Int): Optional<Trunk> {
            for (trunk in World.world.trunks.entries)
                if (trunk.value.mapId == map_id && trunk.value.cellId == cell_id)
                    return Optional.ofNullable(trunk.value)
            return Optional.empty()
        }

        @JvmStatic
        fun lock(P: Player, packet: String) {
            val t = P.exchangeAction!!.getValue() as Trunk?
                ?: return
            if (t.isTrunk(P, t)) {
                (DatabaseManager.get(TrunkData::class.java) as TrunkData).updateCode(P, t, packet) //Change le code
                t.key = packet
                closeCode(P)
            } else {
                closeCode(P)
            }
            P.exchangeAction = null
        }

        @JvmStatic
        fun open(P: Player, packet: String, isTrunk: Boolean) {//Ouvrir un coffre
            val t = P.exchangeAction!!.getValue() as Trunk?
                ?: return
            if (packet.compareTo(t.key!!) == 0 || isTrunk)//Si c'est chez lui ou que le mot de passe est bon
            {
                t.player = P
                SocketManager.GAME_SEND_ECK_PACKET(P.gameClient!!, 5, "")
                SocketManager.GAME_SEND_EL_TRUNK_PACKET(P, t)
                closeCode(P)
                P.exchangeAction = ExchangeAction(ExchangeAction.IN_TRUNK, t)
            } else if (packet.compareTo(t.key!!) != 0)//Mauvais code
            {
                SocketManager.GAME_SEND_KODE(P, "KE")
                closeCode(P)
                P.exchangeAction = null
            }
        }

        @JvmStatic
        fun getTrunksByHouse(h: House): Stream<Trunk> {
            return World.world.trunks.values.stream().filter { it.houseId == h.id }
        }
    }
}
