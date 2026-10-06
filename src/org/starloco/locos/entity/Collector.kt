package org.starloco.locos.entity

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.client.Player
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.CollectorData
import org.starloco.locos.database.data.login.PlayerData
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.guild.Guild
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Collector::class.java)

class Collector(
    var id: Int,
    val map: Int,
    var cell: Int,
    private val orientation: Byte,
    val guildId: Int,
    val n1: Short,
    val n2: Short,
    var poseur: Player?,
    val date: Long,
    items: String,
    var kamas: Long,
    var xp: Long
) {

    var inFight: Byte = 0
    var _inFightID: Int = -1
    var exchange: Boolean = false
    //Les logs
    private val logObjects = HashMap<Int, GameObject>()
    private val objects = HashMap<Int, GameObject>()
    //La dfense
    val defenseFight = HashMap<Int, Player>()

    init {
        for (item in items.split("|")) {
            if (item == "")
                continue
            val infos = item.split(":")
            val itemId = infos[0].toInt()
            val obj = World.world.getGameObject(itemId) ?: continue
            this.objects[obj.guid] = obj
        }
    }

    fun getFullName(): String {
        return Integer.toString(this.n1.toInt(), 36) + "," + Integer.toString(this.n2.toInt(), 36)
    }

    fun moveOnMap() {
        if (this.inFight > 0) return

        val map = World.world.getMap(this.map)
        val cell = map.getRandomNearFreeCellId(this.cell)
        val path: String?

        try {
            path = PathFinding.getShortestStringPathBetween(map, this.cell, cell, 0)
        } catch (e: Exception) {
            log.error("unexpected error", e)
                return
        }

        if (path != null) {
            this.cell = cell
            for (player in map.players) {
                SocketManager.GAME_SEND_GA_PACKET(player.gameClient!!, "0", "1", this.id.toString() + "", path)
            }
        }
    }

    fun reloadTimer() {
        if (World.world.getGuild(guildId) == null)
            return
        val time = World.world.delayCollectors[this.map]
        if (time != null)
            return
        World.world.delayCollectors[this.map] = this.date
    }

    fun getGuild(): Guild? = World.world.getGuild(guildId)

    fun addLogObjects(id: Int, obj: GameObject) {
        this.logObjects[id] = obj
    }

    fun getLogObjects(): String {
        if (this.logObjects.isEmpty())
            return ""
        val str = StringBuilder()

        for (obj in this.logObjects.values)
            str.append(";").append(obj.template!!.id).append(",").append(obj.quantity)

        return str.toString()
    }

    fun getOjects(): Map<Int, GameObject> {
        return this.objects
    }

    fun haveObjects(id: Int): Boolean {
        return this.objects[id] != null
    }

    fun getPodsTotal(): Int {
        var pod = 0
        for (`object` in this.objects.values)
            pod += `object`.template!!.pod * `object`.quantity
        return pod
    }

    fun getMaxPod(): Int {
        return World.world.getGuild(this.guildId)!!.getStats(Constant.STATS_ADD_PODS)
    }

    fun addObjet(newObj: GameObject): Boolean {
        for (entry in this.objects.entries) {
            val obj = entry.value
            if (World.world.conditionManager.stackIfSimilar(obj, newObj, true)) {
                obj.quantity = obj.quantity + newObj.quantity //On ajoute QUA item a la quantit de l'objet existant
                return false
            }
        }
        this.objects[newObj.guid] = newObj
        return true
    }

    fun removeObjet(id: Int) {
        this.objects.remove(id)
    }

    fun delCollector(id: Int) {
        for (obj in this.objects.values)
            World.world.removeGameObject(obj.guid)
        World.world.collectors.remove(id)
    }

    fun getItemCollectorList(): String {
        val items = StringBuilder()
        if (this.objects.isNotEmpty())
            for (obj in this.objects.values)
                items.append("O").append(obj.encodeItem()).append(";")
        if (this.kamas != 0L)
            items.append("G").append(this.kamas)
        return items.toString()
    }

    fun parseItemCollector(): String {
        var items = ""
        for (obj in this.objects.values)
            items += obj.guid.toString() + "|"
        return items
    }

    fun removeFromCollector(P: Player, id: Int, qua: Int) {
        if (qua <= 0)
            return
        val CollectorObj = World.world.getGameObject(id)
        var PersoObj = P.getSimilarItem(CollectorObj!!)
        val newQua = CollectorObj!!.quantity - qua
        if (PersoObj == null)//Si le joueur n'avait aucun item similaire
        {
            //S'il ne reste rien
            if (newQua <= 0) {
                //On retire l'item
                removeObjet(id)
                //On l'ajoute au joueur
                P.addItem(CollectorObj, true)
                //On envoie les packets
                val str = "O-$id"
                SocketManager.GAME_SEND_EsK_PACKET(P, str)
            } else
            //S'il reste des this.objects
            {
                //On cre une copy de l'item
                PersoObj = CollectorObj!!.getClone(qua, true)
                //On l'ajoute au monde
                World.world.addGameObject(PersoObj)
                //On retire X objet
                CollectorObj!!.quantity = newQua
                //On l'ajoute au joueur
                P.addItem(PersoObj!!, true)

                //On envoie les packets
                val str = ("O+" + CollectorObj!!.guid + "|"
                        + CollectorObj!!.quantity + "|"
                        + CollectorObj!!.template!!.id + "|"
                        + CollectorObj!!.encodeStats())
                SocketManager.GAME_SEND_EsK_PACKET(P, str)
            }
        } else {
            //S'il ne reste rien
            if (newQua <= 0) {
                //On retire l'item
                this.removeObjet(id)
                World.world.removeGameObject(CollectorObj!!.guid)
                //On Modifie la quantit de l'item du sac du joueur
                PersoObj.quantity = PersoObj.quantity + CollectorObj!!.quantity

                //On envoie les packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, PersoObj)
                val str = "O-$id"
                SocketManager.GAME_SEND_EsK_PACKET(P, str)
            } else
            //S'il reste des this.objects
            {
                //On retire X objet
                CollectorObj!!.quantity = newQua
                //On ajoute X this.objects
                PersoObj.quantity = PersoObj.quantity + qua

                //On envoie les packets
                SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(P, PersoObj)
                val str = ("O+" + CollectorObj!!.guid + "|"
                        + CollectorObj!!.quantity + "|"
                        + CollectorObj!!.template!!.id + "|"
                        + CollectorObj!!.encodeStats())
                SocketManager.GAME_SEND_EsK_PACKET(P, str)
            }
        }
        SocketManager.GAME_SEND_Ow_PACKET(P)
        (DatabaseManager.get(PlayerData::class.java) as PlayerData).update(P)
    }

    @Synchronized
    fun addDefenseFight(player: Player): Boolean {
        if (!(player.fight == null && !player.away && !player.isInPrison() && player.exchangeAction == null))
            return false

        for (p in this.defenseFight.values) {
            if (player.account != null && p != null && p.account != null) {
                if (player.account.currentIp.compareTo(p.account.currentIp) == 0) {
                    SocketManager.GAME_SEND_MESSAGE(player, player.lang.trans("fight.join.with.sameip"))
                    return false
                }
            }
        }

        if (this.defenseFight.size >= World.world.getMap(map).maxTeam) {
            return false
        } else {
            this.defenseFight[player.id] = player
            return true
        }
    }

    @Synchronized
    fun delDefenseFight(P: Player): Boolean {
        if (this.defenseFight.containsKey(P.id)) {
            this.defenseFight.remove(P.id)
            return true
        }
        return false
    }

    fun clearDefenseFight() {
        this.defenseFight.clear()
    }

    fun getDrops(): Collection<GameObject> {
        return this.objects.values
    }

    companion object {
        @JvmStatic
        fun parseGM(map: GameMap): String {
            val sock = StringBuilder()
            sock.append("GM|")
            var isFirst = true
            for (entry in World.world.collectors.entries) {
                val c = entry.value ?: continue
                if (c.inFight > 0)
                    continue //On affiche pas le Collector si il est en combat
                if (c.map == map.id) {
                    val G = World.world.getGuild(c.guildId)
                    if (G == null) {
                        c.reloadTimer()
                        (DatabaseManager.get(CollectorData::class.java) as CollectorData).delete(c)
                        World.world.collectors.remove(c.id)
                        continue
                    }
                    if (!isFirst)
                        sock.append("|")
                    sock.append("+")
                    sock.append(c.cell).append(";")
                    sock.append(c.orientation).append(";")
                    sock.append("0").append(";")
                    sock.append(c.id).append(";")
                    sock.append(Integer.toString(c.n1.toInt(), 36)).append(",")
                        .append(Integer.toString(c.n2.toInt(), 36)).append(";")
                    sock.append("-6").append(";")
                    sock.append("6000^100;")
                    sock.append(G.lvl).append(";")
                    sock.append(G.name).append(";").append(G.emblem)
                    isFirst = false
                }
            }
            return sock.toString()
        }

        @JvmStatic
        fun parseToGuild(GuildID: Int): String {
            /*
             * 44705000000000
             * 1486217399798
             * gITM +
             * id; -10000
             * N1, 14
             * N2, 26
             * Owner, Lcoos
             * startDate, date collector poser
             * lastHName, dernier rcolteur
             * lastHD, date a laquel le perco a t rcolt
             * nextHD; date a laquel le perco pourra tre rcolt
             * mapid;
             * state; 0 rcolte, 1 attaque, 2 combat,  la fin du timer, passe en combat automatiquement
             * time; temps en ms quand le perco a t lanc
             * maxTimer;temps en ms quand le combat se lance
             * numbPlayer: 1-7
             *
             * les dates au dessus aucune conversion, juste un timestamp  mettre ?
             * ouaip impec, grand merci :) !
             *
             * TEST : gITM+-10000;14,26,poney,0,tagada,55,6400000000000;5q6;1;
             * 1000000000000;2400000000000;7
             */

            // id du poseur
            // date quand on pose
            var packet = StringBuilder()
            var isFirst = true
            for (entry in World.world.collectors.entries) {
                if (entry.value.guildId == GuildID) {
                    val map = World.world.getMap(entry.value.map)
                    if (isFirst)
                        packet.append("+")
                    if (!isFirst)
                        packet.append("|")

                    val perco = entry.value
                    val inFight = entry.value.inFight
                    var name = ""
                    if (entry.value.poseur != null) {
                        name = entry.value.poseur!!.name
                    }

                    packet.append(perco.id) // id
                    packet.append(";")
                    packet.append(Integer.toString(perco.n1.toInt(), 36)) // nameId1
                    packet.append(",")
                    packet.append(Integer.toString(perco.n2.toInt(), 36)) // nameId2

                    packet.append(",")
                    packet.append(name) // callerName
                    packet.append(",")
                    packet.append(java.lang.Long.toString(perco.date)) // startDate
                    packet.append(",")
                    packet.append("") // lastHName
                    packet.append(",")
                    packet.append("-1") // lastHD
                    packet.append(",")
                    packet.append(
                        java.lang.Long.toString(
                            perco.date + World.world.getGuild(GuildID)!!.lvl * 600000
                        )
                    ) // nextHD
                    packet.append(";")

                    packet.append(Integer.toString(map.id, 36))
                    packet.append(",")
                    packet.append(map.x)
                    packet.append(",")
                    packet.append(map.y)
                    packet.append(";")

                    packet.append(inFight)
                    packet.append(";")

                    if (inFight.toInt() == 1) {
                        if (map.getFight(entry.value._inFightID) == null) {
                            packet.append("45000") //TimerActuel
                            packet.append(";")
                        } else {
                            val fight = map.getFight(entry.value._inFightID)
                            var start = System.currentTimeMillis() - fight!!.launchTime
                            if (start > 45000) start = 45000
                            packet.append(45000 - start) //TimerActuel si combat
                            packet.append(";")
                        }

                        packet.append("45000") //TimerInit
                        packet.append(";")

                        var numcase = World.world.getMap(entry.value.map).maxTeam - 1
                        if (numcase > 7)
                            numcase = 7
                        packet.append(numcase) //Nombre de place maximum : En fonction de la map moins celle du Collector
                        packet.append(";")
                    } else {
                        packet.append("0;")
                        packet.append("45000;")
                        packet.append("7;")
                    }
                    isFirst = false
                }
            }
            if (packet.length == 0)
                packet = StringBuilder("null")

            return packet.toString()
        }

        @JvmStatic
        fun getCollectorByGuildId(id: Int): Int {
            for (entry in World.world.collectors.entries)
                if (entry.value.map == id)
                    return entry.value.guildId
            return 0
        }

        @JvmStatic
        fun getCollectorByMapId(id: Int): Collector? {
            for (entry in World.world.collectors.entries)
                if (entry.value.map == id)
                    return World.world.collectors[entry.value.id]
            return null
        }

        @JvmStatic
        fun countCollectorGuild(GuildID: Int): Int {
            var i = 0
            for (entry in World.world.collectors.entries)
                if (entry.value.guildId == GuildID)
                    i++
            return i
        }

        @JvmStatic
        fun parseAttaque(perso: Player, guildID: Int) {
            for (entry in World.world.collectors.entries)
                if (entry.value.inFight > 0 && entry.value.guildId == guildID)
                    SocketManager.GAME_SEND_gITp_PACKET(
                        perso,
                        parseAttaqueToGuild(entry.value.id, entry.value.map, entry.value._inFightID)
                    )
        }

        @JvmStatic
        fun parseDefense(perso: Player, guildID: Int) {
            for (entry in World.world.collectors.entries)
                if (entry.value.inFight > 0 && entry.value.guildId == guildID)
                    SocketManager.GAME_SEND_gITP_PACKET(perso, parseDefenseToGuild(entry.value))
        }

        @JvmStatic
        fun parseAttaqueToGuild(id: Int, map: Int, fightId: Int): String {
            val str = StringBuilder()
            str.append("+").append(id)
            val gameMap = World.world.getMap(map)

            gameMap?.fights?.stream()?.filter { it.id == fightId }?.forEach { fight ->
                fight.getFighters(1).stream().filter { it.getPlayer() != null }.forEach { f ->
                    str.append("|")
                    str.append(Integer.toString(f.getPlayer()!!.id, 36)).append(";")
                    str.append(f.getPlayer()!!.name).append(";")
                    str.append(f.getPlayer()!!.level).append(";")
                    str.append("0;")
                }
            }
            return str.toString()
        }

        @JvmStatic
        fun parseDefenseToGuild(collector: Collector): String {
            val str = StringBuilder()
            str.append("+").append(collector.id)

            for (player in collector.defenseFight.values) {
                if (player == null)
                    continue
                str.append("|")
                str.append(Integer.toString(player.id, 36)).append(";")
                str.append(player.name).append(";")
                str.append(player.gfxId).append(";")
                str.append(player.level).append(";")
                str.append(Integer.toString(player.color1, 36)).append(";")
                str.append(Integer.toString(player.color2, 36)).append(";")
                str.append(Integer.toString(player.color3, 36)).append(";")
            }
            return str.toString()
        }

        @JvmStatic
        fun removeCollector(GuildID: Int) {
            for (collector in World.world.collectors.values) {
                if (collector.guildId == GuildID) {
                    World.world.collectors.remove(collector.id)
                    for (p in World.world.getMap(collector.map).players) {
                        SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(p.curMap, collector.id) //Suppression visuelle
                    }
                    collector.reloadTimer()
                    (DatabaseManager.get(CollectorData::class.java) as CollectorData).delete(collector)
                }
            }
        }
    }
}
