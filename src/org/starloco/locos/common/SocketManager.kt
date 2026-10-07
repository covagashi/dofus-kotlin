package org.starloco.locos.common

import org.starloco.locos.anims.KeyFrame
import org.starloco.locos.area.map.CellsDataProvider
import org.starloco.locos.area.map.GameCase
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.area.map.MapData
import org.starloco.locos.entity.map.MountPark
import org.starloco.locos.entity.map.Trunk
import org.starloco.locos.client.Player
import org.starloco.locos.client.other.Party
import org.starloco.locos.database.data.game.ExperienceTables
import org.starloco.locos.database.data.game.SaleOffer
import org.starloco.locos.entity.Collector
import org.starloco.locos.entity.Prism
import org.starloco.locos.entity.monster.MonsterGroup
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.entity.npc.Npc
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.game.GameClient
import org.starloco.locos.game.world.World
import org.starloco.locos.hdv.BigStore
import org.starloco.locos.hdv.BigStoreListing
import org.starloco.locos.job.JobStat
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.`object`.ObjectSet
import org.starloco.locos.`object`.ObjectTemplate
import org.starloco.locos.guild.Guild
import org.starloco.locos.guild.GuildMember
import org.starloco.locos.util.Pair

import java.util.*
import java.util.Map.Entry
import java.util.stream.Collectors
import java.util.stream.Stream
import org.starloco.locos.util.Predicates
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(SocketManager::class.java)

object SocketManager {

    @JvmStatic fun send(player: Player, packet: String) {
        if (player != null && player.getAccount() != null)
            SocketManager.send(player.getGameClient(), packet)
    }

    @JvmStatic fun send(client: GameClient?, packet: String) {
        if (client != null && client.getSession() != null && !client.getSession().isClosing() && client.getSession().isConnected()) {
            client.send(packet)
        }
    }

    @JvmStatic fun GAME_SEND_UPDATE_ITEM(P: Player, obj: GameObject) // Utilis� pour tours bonbon
    {
        var packet: String = "OC|" + obj.encodeItem()
        send(P, packet)
    }

    @JvmStatic fun MULTI_SEND_Af_PACKET(out: GameClient, position: Int, totalAbo: Int, totalNonAbo: Int, button: Int) {
        send(out, "Af" + position + "|" + totalAbo + "|" + totalNonAbo + "|" + button + "|" + Config.gameServerId)
    }

    @JvmStatic fun GAME_SEND_ATTRIBUTE_FAILED(out: GameClient) {
        var packet: String = "ATE"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_AV0(out: GameClient) {
        var packet: String = "AV0"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_PERSO_LIST(out: GameClient, persos: Map<Int,Player>, subscriber: Long) {
        var packet: StringBuilder = StringBuilder()

        packet.append("ALK")
        if (Config.subscription)
            packet.append(subscriber)
        else
            packet.append("86400000")
        packet.append("|").append(persos.size)
        for (entry in  persos.entries)
            packet.append(entry.value.parseALK())
        send(out, packet.toString())
    }

    @JvmStatic fun GAME_SEND_NAME_ALREADY_EXIST(out: GameClient) {
        var packet: String = "AAEa"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_CREATE_PERSO_FULL(out: GameClient) {
        var packet: String = "AAEf"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_CREATE_OK(out: GameClient) {
        var packet: String = "AAK"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_DELETE_PERSO_FAILED(out: GameClient) {
        var packet: String = "ADE"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_CREATE_FAILED(out: GameClient) {
        var packet: String = "AAEF"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_PERSO_SELECTION_FAILED(out: GameClient) {
        var packet: String = "ASE"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_STATS_PACKET(perso: Player) {
        var packet: String = perso.getAsPacket()
        SocketManager.GAME_SEND_Ow_PACKET(perso)
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_Rx_PACKET(out: Player) {
        var packet: String = "Rx" + out.mountXpGive
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_Rn_PACKET(out: Player, name: String) {
        var packet: String = "Rn" + name
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(perso: Player, item: GameObject) {
        send(perso, "OCO" + item.encodeItem())
    }

    @JvmStatic fun GAME_SEND_Re_PACKET(out: Player, sign: String, DD: Mount?) {
        var packet: String = "Re" + sign
        if (sign.equals("+"))
            packet += DD!!.parse()

        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ASK(out: GameClient, perso: Player) {
        try {
            var packet: StringBuilder = StringBuilder()
            var color1: Int = perso.color1
        var color2: Int = perso.color2
        var color3: Int = perso.color3
            if (perso.getObjetByPos(Constant.ITEM_POS_MALEDICTION) != null) {
                if (perso.getObjetByPos(Constant.ITEM_POS_MALEDICTION)!!.template!!.id == 10838) {
                    color1 = 16342021
                    color2 = 16342021
                    color3 = 16342021
                }
            }
            packet.append("ASK|").append(perso.id).append("|").append(perso.name).append("|")
            packet.append(perso.level).append("|").append(if (perso.morphMode) -1 else perso.classe).append("|").append(perso.sexe)
            packet.append("|").append(perso.gfxId).append("|").append((if (color1 == -1) "-1" else java.lang.Integer.toHexString(color1)))
            packet.append("|").append((if (color2 == -1) "-1" else java.lang.Integer.toHexString(color2))).append("|")
            packet.append((if (color3 == -1) "-1" else java.lang.Integer.toHexString(color3))).append("|")
            packet.append(perso.encodeItemASK())
            send(out, packet.toString())
        } catch (e: Exception) {
            log.error("unexpected error", e)
            log.error("Error occured : " + e.message)
        }
    }

    @JvmStatic fun GAME_SEND_ALIGNEMENT(out: GameClient, alliID: Int) {
        var packet: String = "ZS" + alliID
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ADD_CANAL(out: GameClient, chans: String) {
        var packet: String = "cC+" + chans
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ZONE_ALLIGN_STATUT(out: GameClient) {
        var packet: String = "al|" + World.world.sousZoneStateString
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_RESTRICTIONS(out: GameClient) {
        var packet: String = "AR6bk"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_Ow_PACKET(perso: Player) {
        var packet: String = "Ow" + perso.getPodUsed() + "|" + perso.getMaxPod()
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_OT_PACKET(out: GameClient, id: Int) {
        var packet: String = "OT"
        if (id > 0)
            packet += id
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_SEE_FRIEND_CONNEXION(out: GameClient, see: Boolean) {
        var packet: String = "FO" + (if (see) "+" else "-")
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GAME_CREATE(out: GameClient, _name: String) {
        var packet: String = "GCK|1|" + _name
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_MAPDATA(out: GameClient, id: Int, date: String, key: String) {
        var packet: String = "GDM|" + id + "|" + date + "|" + key
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GDK_PACKET(out: GameClient) {
        var packet: String = "GDK"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_MAP_MOBS_GMS_PACKETS(out: GameClient, Map: GameMap) {
        var packet: String = Map.getMobGroupGMsPackets()
        if (packet.equals(""))
            return
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_MAP_OBJECTS_GDS_PACKETS(out: GameClient, Map: GameMap) {
        var packet: String = Map.getObjectsGDsPackets()
        if (packet.equals(""))
            return
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_MAP_NPCS_GMS_PACKETS(out: GameClient, Map: GameMap) {
        if (out == null || out.player == null || Map == null)
            return
        var packet: String = Map.getNpcsGMsPackets(out.player)
        if (packet.isEmpty())
            return
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_MAP_PERCO_GMS_PACKETS(out: GameClient, Map: GameMap) {
        var packet: String = Collector.parseGM(Map)
        if (packet.length < 5)
            return
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ERASE_ON_MAP_TO_MAP(map: GameMap, guid: Int) {
        if (map == null)
            return
        var packet: String = "GM|-" + guid
        for (z in  map.players) {
            if (z == null || z.getGameClient() == null)
                continue

            send(z.getGameClient()!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_ON_FIGHTER_KICK(f: Fight, guid: Int, team: Int) {
        var packet: String = "GM|-" + guid
        for (F in  f.getFighters(team)) {
            if (F.player == null
                    || F.player!!.getGameClient() == null
                    || F.player!!.id == guid)
                continue
            send(F.player!!.getGameClient()!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_ALTER_FIGHTER_MOUNT(fight: Fight, fighter: Fighter, guid: Int, team: Int, otherteam: Int) {
        var packet: StringBuilder = StringBuilder()
        packet.append("GM|-").append(guid).append((0x00 as Char)).append(fighter.getGmPacket('+', true))
        for (F in  fight.getFighters(team)) {
            if (F.player == null
                    || F.player!!.getGameClient() == null
                    || !F.player!!.isOnline)
                continue
            send(F.player!!.getGameClient()!!, packet.toString())
        }
        if (otherteam > -1) {
            for (F in  fight.getFighters(otherteam)) {
                if (F.player == null
                        || F.player!!.getGameClient() == null
                        || !F.player!!.isOnline)
                    continue
                send(F.player!!.getGameClient()!!, packet.toString())
            }
        }
    }

    @JvmStatic fun GAME_SEND_ADD_PLAYER_TO_MAP(map: GameMap, perso: Player) {
        var packet: String = "GM|+" + perso.parseToGM()
        for (z in  map.players) {
            if (perso.size > 0)
                send(z, packet)
            else if (z.getGroup() != null)
                send(z, packet)
        }
    }

    @JvmStatic fun GAME_SEND_DUEL_Y_AWAY(out: GameClient, guid: Int) {
        var packet: String = "GA;903;" + guid + ";o"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_DUEL_E_AWAY(out: GameClient, guid: Int) {
        var packet: String = "GA;903;" + guid + ";z"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_MAP_NEW_DUEL_TO_MAP(map: GameMap, guid: Int, guid2: Int) {
        var packet: String = "GA;900;" + guid + ";" + guid2
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_CANCEL_DUEL_TO_MAP(map: GameMap, guid: Int, guid2: Int) {
        var packet: String = "GA;902;" + guid + ";" + guid2
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_MAP_START_DUEL_TO_MAP(map: GameMap, guid: Int, guid2: Int) {
        var packet: String = "GA;901;" + guid + ";" + guid2
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_MAP_FIGHT_COUNT(out: GameClient, map: GameMap) {
        var packet: String = "fC" + map.getNbrFight()
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(fight: Fight, teams: Int, state: Int, cancelBtn: Int, duel: Int, spec: Int, time: Int, type: Int) {
        var packet: StringBuilder = StringBuilder()
        packet.append("GJK").append(state).append("|").append(cancelBtn).append("|").append(duel).append("|").append(spec).append("|").append(time).append("|").append(type)
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            send(f.player!!, packet.toString())
        }
    }

    @JvmStatic fun GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(fight: Fight, teams: Int, positions: List<List<Int>>, team: Int) {
        if(positions.size != 2) throw IllegalStateException("attempted to send invalid number of fight positions")

        var packet: String = "GP" + MapData.encodePositions(positions) + "|" + team
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(map: GameMap) {
        var packet: String = "fC" + map.getNbrFight()
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(map: GameMap, arg1: Int, guid1: Int, guid2: Int, cell1: Int, str1: String, cell2: Int, str2: String) {
        var packet: StringBuilder = StringBuilder()
        packet.append("Gc+").append(guid1).append(";").append(arg1).append("|").append(guid1).append(";").append(cell1).append(";").append(str1).append("|").append(guid2).append(";").append(cell2).append(";").append(str2)
        for (z in  map.players)
            send(z, packet.toString())
    }

    @JvmStatic fun GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(p: Player, arg1: Int, guid1: Int, guid2: Int, cell1: Int, str1: String, cell2: Int, str2: String) {
        send(p, "Gc+" + guid1 + ";" + arg1 + "|" + guid1 + ";" + cell1 + ";" + str1 + "|" + guid2 + ";" + cell2 + ";" + str2)
    }

    @JvmStatic fun GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(map: GameMap, guid: Int) {
        var packet: String = "Gc-" + guid
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(map: GameMap, teamID: Int, perso: Fighter) {
        var packet: StringBuilder = StringBuilder()
        packet.append("Gt").append(teamID).append("|+").append(perso.id).append(";").append(perso.getPacketsName()).append(";").append(perso.getLvl())
        for (z in  map.players) {
            send(z, packet.toString())
        }
    }

    @JvmStatic fun GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(p: Player, teamID: Int, perso: Fighter) {
        send(p, "Gt" + teamID + "|+" + perso.id + ";" + perso.getPacketsName() + ";" + perso.getLvl())
    }

    @JvmStatic fun GAME_SEND_REFRESH_TEAM_PACKET_TO_MAP(map: GameMap, team: Int, fighters: Collection<Fighter>) {
        var builder: StringBuilder = StringBuilder("Gt")
        builder.append(team)
        for (fighter in  fighters)
            builder.append("|+").append(fighter.id).append(";").append(fighter.getPacketsName()).append(";").append(fighter.getLvl())
        sendPacketToMap(map, builder.toString())
    }

    @JvmStatic fun GAME_SEND_REMOVE_IN_TEAM_PACKET_TO_MAP(map: GameMap, teamID: Int, perso: Fighter) {
        if (map == null || perso == null) return
        var packet: StringBuilder = StringBuilder()
        packet.append("Gt").append(teamID).append("|-").append(perso.id).append(";").append(perso.getPacketsName()).append(";").append(perso.getLvl())
        for (z in  map.players)
            send(z, packet.toString())
    }

    @JvmStatic fun GAME_SEND_MAP_MOBS_GMS_PACKETS_TO_MAP(map: GameMap, perso: Player) {
        var packet: String = map.getMobGroupGMsPackets() // Un par un comme sa lors du respawn :)
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_MAP_MOBS_GM_PACKET(map: GameMap, current_Mobs: MonsterGroup) {
        var packet: String = "GM|"
        packet += current_Mobs.encodeGM(); // Un par un comme sa lors du respawn :)
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_MAP_GMS_PACKETS(map: GameMap, _perso: Player) {
        var packet: String = (if (_perso
                .fight != null)
                map
                        .getFightersGMsPackets(_perso.fight!!) else
                map
                        .getPlayersGMsPackets())
        send(_perso, packet)
    }

    @JvmStatic fun GAME_SEND_ON_EQUIP_ITEM(map: GameMap, _perso: Player) {
        var packet: String = _perso.parseToOa()
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_ON_EQUIP_ITEM_FIGHT(_perso: Player, f: Fighter, F: Fight) {
        var packet: String = _perso.parseToOa()
        for (z in  F.getFighters(f.getTeam2())) {
            if (z.player == null)
                continue
            send(z.player!!, packet)
        }
        for (z in  F.getFighters(f.getOtherTeam())) {
            if (z.player == null)
                continue
            send(z.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_FIGHT_CHANGE_PLACE_PACKET_TO_FIGHT(fight: Fight, teams: Int, guid: Int, cell: Int) {
        var packet: String = "GIC|" + guid + ";" + cell + ";1"
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_Ew_PACKET(perso: Player, pods: Int, podsMax: Int) { //Pods de la dinde
        var packet: String = "Ew" + pods + ";" + podsMax + ""
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_EL_MOUNT_PACKET(out: Player, drago: Mount) { // Inventaire dinde : Liste des objets
        var packet: String = "EL" + drago.parseToMountObjects()
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GM_MOUNT_TO_MAP(map: GameMap, dd: Mount) {
        var packet: String = dd.parseToGM()
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_GDO_OBJECT_TO_MAP(out: GameClient, map: GameMap) {// Actualisation d'une cellule
        var packet: String = map.getObjects()
        if (packet.equals(""))
            return
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GM_MOUNT(out: GameClient, map: GameMap, ok: Boolean) {
        var packet: String = map.getGMOfMount(ok)
        if (Objects.equals(packet, ""))
            return
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_Ef_MOUNT_TO_ETABLE(perso: Player, c: Char, s: String) {
        var packet: String = "Ef" + c + s
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_GA_ACTION_TO_MAP(mapa: GameMap, idUnique: String, idAction: Int, s1: String, s2: String) {
        var packet: String = "GA" + idUnique + ";" + idAction + ";" + s1
        if (!s2.equals(""))
            packet += ";" + s2
        for (z in  mapa.players)
            send(z, packet)
    }

    @JvmStatic fun SEND_GDO_PUT_OBJECT_MOUNT(map: GameMap, str: String) {
        var packet: String = "GDO+" + str
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun SEND_GDE_FRAME_OBJECT_EXTERNAL(map: GameMap, str: String) {
        var packet: String = "GDE|" + str
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(map: GameMap, s: Char, option: Char, guid: Int) {
        var packet: String = "Go" + s + option + guid
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(fight: Fight, teams: Int, guid: Int, b: Boolean) {
        var packet: String = "GR" + (if (b) "1" else "0") + guid
        if (fight.state != 2)
            return
        for (f in  fight.getFighters(teams)) {
            if (f.player == null || !f.player!!.isOnline)
                continue
            if (f.hasLeft())
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_GJK_PACKET(out: Player, state: Int, cancelBtn: Int, duel: Int, spec: Int, time: Int, unknown: Int) {

        send(out, "GJK" + state + "|" + cancelBtn + "|" + duel + "|" + spec + "|" + time + "|" + unknown)
    }

    @JvmStatic fun GAME_SEND_FIGHT_PLACES_PACKET(out: GameClient, positions: List<List<Int>>, team: Int) {
        if(positions.size != 2) throw IllegalStateException("attempted to send invalid number of fight positions")

        var packet: String = "GP" + MapData.encodePositions(positions) + "|" + team

        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_Im_PACKET_TO_ALL(str: String) {
        var packet: String = "Im" + str
        for (perso in  World.world.onlinePlayers)
            send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_Im_PACKET(out: Player, str: String) {
        var packet: String = "Im" + str
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_TUTORIAL_CREATE(out: Player, id: Int, date: String) {
        SocketManager.send(out, "TC" + id + "|" + date)
    }

    @JvmStatic fun GAME_SEND_Im_PACKET_TO_MAP(map: GameMap, id: String) {
        var packet: String = "Im" + id
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_eUK_PACKET_TO_MAP(map: GameMap, guid: Int, emote: Int) {
        var packet: String = "eUK" + guid + "|" + emote
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_Im_PACKET_TO_FIGHT(fight: Fight, teams: Int, id: String) {
        var packet: String = "Im" + id
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_MESSAGE(out: Player, mess: String, color: String) {
        var packet: String = "cs<font color='#" + color + "'>" + mess + "</font>"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_MESSAGE(out: Player, mess: String) {
        var packet: String = "cs<font color='#" + "B9121B" +
                "'>" + mess + "</font>"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_MESSAGE_TO_MAP(map: GameMap, mess: String, color: String) {
        var packet: String = "cs<font color='#" + color + "'>" + mess + "</font>"
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_GA903_ERROR_PACKET(out: GameClient, c: Char, guid: Int) {
        var packet: String = "GA;903;" + guid + ";" + c
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GIC_PACKETS_TO_FIGHT(fight: Fight, teams: Int) {
        val packet: StringBuilder = StringBuilder()
        packet.append("GIC|")
        for (p in  fight.getFighters(3)) {
            if (p.cell == null)
                continue
            packet.append(p.id).append(";").append(p.cell!!.getId()).append(";1|")
        }
        fight.getFighters(teams).stream()
            .filter(Predicates.not(Fighter::hasLeft))
            .forEach({ f -> f.send(packet.toString()) })
    }

    @JvmStatic fun GAME_SEND_GIC_PACKET_TO_FIGHT(fight: Fight, teams: Int, fighter: Fighter) {
        fight.getFighters(teams).stream()
            .filter(Predicates.not(Fighter::hasLeft))
            .forEach({ f -> f.send("GIC|" + fighter.id + ";" + fighter.cell!!.getId() + ";1|") })
    }

    @JvmStatic fun GAME_SEND_GS_PACKET_TO_FIGHT(fight: Fight, teams: Int) {
        var packet: String = "GS"
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            f.initFightBuffs()

            f.send(packet)
        }
    }

    @JvmStatic fun GAME_SEND_GS_PACKET(out: Player) {
        var packet: String = "GS"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GTL_PACKET_TO_FIGHT(fight: Fight, teams: Int) {
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, fight.getGTL())
        }
    }

    @JvmStatic fun GAME_SEND_GTL_PACKET(out: Player, fight: Fight) {
        var packet: String = fight.getGTL()
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GTM_PACKET_TO_FIGHT(fight: Fight, teams: Int) {
        var packet: StringBuilder = StringBuilder()
        packet.append("GTM")
        for (f in  fight.getFighters(3)) {
            packet.append("|").append(f.id).append(";")
            if (f.isDead || f.cell == null) {
                if (f.cell == null)
                    f.setIsDead(true)
                packet.append("1")
                continue
            }
            packet.append("0;").append(f.getPdv()).append(";").append(f.getPa()).append(";").append(f.getPm()).append(";")
            packet.append((if (f.isHidden()) "-1" else f.cell!!.getId())).append(";");//On envoie pas la cell d'un invisible :p
            packet.append(";");//??
            packet.append(f.getPdvMax())
        }
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet.toString())
        }
    }

    @JvmStatic fun GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(fight: Fight, teams: Int, guid: Int, time: Int, turns: Int) {
        var packet: String = "GTS" + guid + "|" + time + "|" + turns
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue

            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_GAMETURNSTART_PACKET(P: Player, guid: Int, time: Int) {
        var packet: String = "GTS" + guid + "|" + time
        send(P, packet)
    }

    @JvmStatic fun GAME_SEND_GV_PACKET(P: Player) {
        var packet: String = "GV"
        send(P, packet)
    }

    @JvmStatic fun GAME_SEND_GAS_PACKET_TO_FIGHT(fight: Fight, teams: Int, guid: Int) {
        var packet: String = "GAS" + guid
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_GA_PACKET_TO_FIGHT(fight: Fight, teams: Int, actionID: Int, s1: String, s2: String) {
        var packet: String = "GA;" + actionID + ";" + s1

        if (!s2.equals(""))
            packet += ";" + s2
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_GA_PACKET(perso: Player, actionID: Int, s1: String, s2: String) {
        var packet: String = "GA;" + actionID + ";" + s1
        if (!s2.equals(""))
            packet += ";" + s2
        send(perso, packet)
    }

    @JvmStatic fun SEND_SB_SPELL_BOOST(perso: Player, modif: String) {
        var packet: String = "SB" + modif
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_GA_PACKET(out: GameClient, actionID: String, s0: String, s1: String, s2: String) {
        var packet: String = "GA" + actionID + ";" + s0
        if (!s1.equals(""))
            packet += ";" + s1
        if (!s2.equals(""))
            packet += ";" + s2

        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GA_PACKET_TO_FIGHT(fight: Fight, teams: Int, gameActionID: Int, s1: String, s2: String, s3: String) {
        var packet: String = "GA" + gameActionID + ";" + s1 + ";" + s2 + ";" + s3
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_GAMEACTION_TO_FIGHT(fight: Fight, teams: Int, packet: String) {
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_GAF_PACKET_TO_FIGHT(fight: Fight, teams: Int, i1: Int, guid: Int) {
        var packet: String = "GAF" + i1 + "|" + guid
        for (f in  fight.getFighters(teams)) {
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_BN(out: Player) {
        var packet: String = "BN"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_BN(out: GameClient) {
        var packet: String = "BN"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(fight: Fight, teams: Int, guid: Int) {
        var packet: String = "GTF" + guid
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue

            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_GTR_PACKET_TO_FIGHT(fight: Fight, teams: Int, guid: Int) {
        var packet: String = "GTR" + guid
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_EMOTICONE_TO_MAP(map: GameMap, guid: Int, id: Int) {
        var packet: String = "cS" + guid + "|" + id
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_SPELL_UPGRADE_FAILED(_out: GameClient) {
        var packet: String = "SUE"
        send(_out, packet)
    }

    @JvmStatic fun GAME_SEND_SPELL_UPGRADE_SUCCESS(_out: GameClient, spellID: Int, level: Int) {
        var packet: String = "SUK" + spellID + "~" + level
        send(_out, packet)
    }

    @JvmStatic fun GAME_SEND_SPELL_LIST(perso: Player) {
        var packet: String = "SL" + perso.encodeSpellListForSL()
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(fight: Fight, teams: Int, guid: Int) {
        var packet: String = "GA;103;" + guid + ";" + guid
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft() || f.player == null)
                continue
            if (f.player!!.isOnline)
                send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(fight: Fight, teams: Int, map: GameMap) {
        var packet: String = map.getFightersGMsPackets(fight)
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_MAP_FIGHT_GMS_PACKETS(fight: Fight, map: GameMap, _perso: Player) {
        var packet: String = map.getFightersGMsPackets(fight)
        send(_perso, packet)
    }

    @JvmStatic fun GAME_SEND_FIGHT_PLAYER_JOIN(fight: Fight, teams: Int, _fighter: Fighter) {
        var packet: String = _fighter.getGmPacket('+', true)

        for (f in  fight.getFighters(teams)) {

            if (f != _fighter) {
                if (f.player == null || !f.player!!.isOnline)
                    continue
                if (f.player != null
                        && f.player!!.getGameClient() != null) {
                    send(f.player!!, packet)
                }
            }
        }
    }

    @JvmStatic fun GAME_SEND_cMK_PACKET(perso: Player, suffix: String, guid: Int, name: String, msg: String) {
        var packet: String = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_FIGHT_LIST_PACKET(out: GameClient, map: GameMap) {
        var packet: StringBuilder = StringBuilder()
        packet.append("fL")
        for (entry in  map.fights) {
            if (packet.length > 2)
                packet.append("|")
            packet.append(entry.parseFightInfos())
        }
        send(out, packet.toString())
    }

    @JvmStatic fun GAME_SEND_cMK_PACKET_TO_MAP(map: GameMap, suffix: String, guid: Int, name: String, key: String) {
        var packet: String = "cMK" + suffix + "|" + guid + "|" + name + "|"
        for (target in  map.players) {
            if (target != null && target.getLang() != null)
                send(target, packet + target.getLang().trans(key))
        }
    }

    @JvmStatic fun GAME_SEND_cMK_PACKET_TO_GUILD(g: Guild, suffix: String, guid: Int, name: String, msg: String) {
        var packet: String = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg
        for (perso in  g.getPlayers()) {
            if (perso == null || !perso.isOnline)
                continue
            send(perso, packet)
        }
    }

    @JvmStatic fun GAME_SEND_cMK_PACKET_TO_ALL(player: Player, suffix: String, guid: Int, name: String, msg: String) {
        var packet: String = "cMK" + suffix + "|" + guid + "|" + name + "|"
        if (player.level < 6) {
            SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("common.socketmanager.canal.all.send"))
            GAME_SEND_BN(player)
            return
        }
        for (target in  World.world.onlinePlayers) {
            send(target, packet + msg)
        }
    }

    @JvmStatic fun GAME_SEND_cMK_PACKET_TO_ALIGN(suffix: String, guid: Int, name: String, msg: String, _perso: Player) {
        var packet: String = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg
        for (perso in  World.world.onlinePlayers) {
            if (perso.alignment == _perso.alignment) {
                send(perso, packet)
            }
        }
    }

    @JvmStatic fun GAME_SEND_cMK_PACKET_TO_ADMIN(suffix: String, guid: Int, name: String, msg: String) {
        var packet: String = "cMK" + suffix + "|" + guid + "|" + name + "|" + msg
        for (perso in  World.world.onlinePlayers)
            if (perso.isOnline)
                if (perso.getAccount() != null)
                    if (perso.getGroup() != null)
                        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_cMK_PACKET_TO_FIGHT(fight: Fight, teams: Int, suffix: String, guid: Int, name: String, msg: String) {
        var packet: String = "cMK" + suffix + "|" + guid + "|" + name + "|"
        for (fighter in  fight.getFighters(teams)) {
            var target: Player? = fighter.player
            if (fighter.hasLeft() || target == null || !target.isOnline)
                continue
            send(target, packet + msg)
        }
    }

    @JvmStatic fun GAME_SEND_GDZ_PACKET_TO_FIGHT(fight: Fight, teams: Int, suffix: String, cell: Int, size: Int, unk: Int) {
        var packet: String = "GDZ" + suffix + cell + ";" + size + ";" + unk

        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_GDC_PACKET_TO_FIGHT(fight: Fight, teams: Int, cell: Int) {
        var packet: String = "GDC" + cell

        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_GA2_PACKET(out: GameClient, guid: Int) {
        var packet: String = "GA;2;" + guid + ";"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_CHAT_ERROR_PACKET(out: GameClient, name: String) {
        var packet: String = "cMEf" + name
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_eD_PACKET_TO_MAP(map: GameMap, guid: Int, dir: Int) {
        var packet: String = "eD" + guid + "|" + dir
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_ECK_PACKET(out: Player, type: Int, str: String) {
        var packet: String = "ECK" + type
        if (!str.equals(""))
            packet += "|" + str
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ECK_PACKET(out: GameClient, type: Int, str: String) {
        var packet: String = "ECK" + type
        if (!str.equals(""))
            packet += "|" + str
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ITEM_VENDOR_LIST_PACKET(out: GameClient, offers: List<SaleOffer>) {
        var packet: String = offers.stream()
            .map(SaleOffer::encode)
            .collect(Collectors.joining("|", "EL", ""))
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ITEM_LIST_PACKET_PERCEPTEUR(out: GameClient, perco: Collector) {
        var packet: String = "EL" + perco.getItemCollectorList()
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ITEM_LIST_PACKET_SELLER(p: Player, out: Player) {
        var packet: String = "EL" + p.parseStoreItemsList()
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EV_PACKET(out: GameClient) {
        var packet: String = "EV"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_DIALOG_CREATE_PACKET(out: GameClient, id: Int) {
        send(out, "DCK" + id)
    }

    @JvmStatic fun GAME_SEND_DOCUMENT_CREATE_PACKET(out: GameClient, id: Int, date: String) {
        send(out, "dCK" + id + "_" + date)
    }

    @JvmStatic fun GAME_SEND_DOCUMENT_CLOSE_PACKET(out: GameClient) {
        send(out, "dV")
    }

    @JvmStatic fun GAME_SEND_QUESTION_PACKET(out: GameClient, id: Int, answers: List<Int>, param: String) {
        var pck: StringBuilder = StringBuilder("DQ")
        pck.append(id)
        if (param != null && !param.isEmpty()) {
            pck.append(";")
            pck.append(param)
        }
        if (answers != null && !answers.isEmpty()) {
            pck.append("|")
            pck.append(answers.stream().map { it.toString() }.collect(Collectors.joining(";")))
        }
        send(out, pck.toString())
    }

    @JvmStatic fun GAME_SEND_QUESTION_PACKET(out: GameClient, str: String) {
        var packet: String = "DQ" + str
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_END_DIALOG_PACKET(out: GameClient) {
        var packet: String = "DV"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_PAUSE_DIALOG_PACKET(out: GameClient) {
        var packet: String = "DP"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_BUY_ERROR_PACKET(out: GameClient) {
        var packet: String = "EBE"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_SELL_ERROR_PACKET(out: GameClient) {
        var packet: String = "ESE"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_BUY_OK_PACKET(out: GameClient) {
        var packet: String = "EBK"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_OBJECT_QUANTITY_PACKET(out: Player, obj: GameObject) {

        var packet: String = "OQ" + obj.guid + "|" + obj.quantity
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_OAKO_PACKET(out: Player, obj: GameObject) {
        var packet: String = "OAKO" + obj.encodeItem()
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ESK_PACKEt(out: Player) {
        var packet: String = "ESK"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_REMOVE_ITEM_PACKET(out: Player, guid: Int) {
        var packet: String = "OR" + guid
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_DELETE_OBJECT_FAILED_PACKET(out: GameClient) {
        var packet: String = "OdE"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_OBJET_MOVE_PACKET(out: Player, obj: GameObject) {
        var packet: String = "OM" + obj.guid + "|"
        if (obj.position != Constant.ITEM_POS_NO_EQUIPED)
            packet += obj.position

        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_DELETE_STATS_ITEM_FM(perso: Player, id: Int) {
        var packet: String = "OR" + id
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_EMOTICONE_TO_FIGHT(fight: Fight, teams: Int, guid: Int, id: Int) {
        var packet: String = "cS" + guid + "|" + id
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            if (f.player == null || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun GAME_SEND_OAEL_PACKET(out: GameClient) {
        var packet: String = "OAEL"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_NEW_LVL_PACKET(out: GameClient, lvl: Int) {
        var packet: String = "AN" + lvl
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_MESSAGE_TO_ALL(msg: String, color: String) {
        var packet: String = "cs<font color='#" + color + "'>" + msg + "</font>"
        for (P in  World.world.onlinePlayers)
            send(P, packet)
    }

    @JvmStatic fun GAME_SEND_EXCHANGE_REQUEST_OK(out: GameClient, guid: Int, guidT: Int, msgID: Int) {
        var packet: String = "ERK" + guid + "|" + guidT + "|" + msgID
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EXCHANGE_REQUEST_ERROR(out: GameClient, c: Char) {
        var packet: String = "ERE" + c
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EXCHANGE_CONFIRM_OK(out: GameClient, type: Int) {
        var packet: String = "ECK" + type
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EXCHANGE_MOVE_OK(out: Player, type: Char, signe: String, s1: String) {
        var packet: String = "EMK" + type + signe
        if (!s1.equals(""))
            packet += s1
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EXCHANGE_MOVE_OK_FM(out: Player, type: Char, signe: String, s1: String) {
        var packet: String = "EmK" + type + signe
        if (!s1.equals(""))
            packet += s1
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EXCHANGE_OTHER_MOVE_OK(out: GameClient, type: Char, signe: String, s1: String) {
        var packet: String = "EmK" + type + signe
        if (!s1.equals(""))
            packet += s1
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EXCHANGE_OTHER_MOVE_OK_FM(out: GameClient, type: Char, signe: String, s1: String) {
        var packet: String = "EMK" + type + signe
        if (!s1.equals(""))
            packet += s1
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EXCHANGE_OK(out: GameClient, ok: Boolean, guid: Int) {
        var packet: String = "EK" + (if (ok) "1" else "0") + guid
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EXCHANGE_OK(out: GameClient, ok: Boolean) {
        var str: String = "EK" + (if (ok) "1" else "0")
        send(out, str)
    }

    @JvmStatic fun GAME_SEND_EXCHANGE_VALID(out: GameClient, c: Char) {
        var packet: String = "EV" + c
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GROUP_INVITATION_ERROR(out: GameClient, s: String) {
        var packet: String = "PIE" + s
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GROUP_INVITATION(out: GameClient, n1: String, n2: String) {
        var packet: String = "PIK" + n1 + "|" + n2
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_GROUP_CREATE(out: GameClient, g: Party) {
        var packet: String = "PCK" + g.chief.name
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_PL_PACKET(out: GameClient, g: Party) {
        var packet: String = "PL" + g.chief.id
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_PR_PACKET(out: Player) {
        var packet: String = "PR"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_PV_PACKET(out: GameClient, s: String) {
        var packet: String = "PV" + s
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ALL_PM_ADD_PACKET(out: GameClient, g: Party) {
        var packet: StringBuilder = StringBuilder()
        packet.append("PM+")
        var first: Boolean = true
        for (p in  g.players) {
            if (!first)
                packet.append("|")
            packet.append(p.parseToPM())
            first = false
        }
        send(out, packet.toString())
    }

    @JvmStatic fun GAME_SEND_PM_ADD_PACKET_TO_GROUP(g: Party, p: Player) {
        var packet: String = "PM+" + p.parseToPM()
        for (P in  g.players)
            send(P, packet)
    }

    @JvmStatic fun GAME_SEND_PM_MOD_PACKET_TO_GROUP(g: Party, p: Player) {
        var packet: String = "PM~" + p.parseToPM()
        for (P in  g.players)
            send(P, packet)
    }

    @JvmStatic fun GAME_SEND_PM_DEL_PACKET_TO_GROUP(party: Party, guid: Int) {
        var packet: String = "PM-" + guid
        for (P in  ArrayList(party.players)) send(P, packet)
    }

    @JvmStatic fun GAME_SEND_cMK_PACKET_TO_GROUP(g: Party, s: String, guid: Int, name: String, msg: String) {
        var packet: String = "cMK" + s + "|" + guid + "|" + name + "|" + msg + "|"

        for (target in  g.players)
            send(target, packet)
    }

    @JvmStatic fun GAME_SEND_FIGHT_DETAILS(out: GameClient, fight: Fight) {
        if (fight == null)
            return
        var packet: StringBuilder = StringBuilder()
        packet.append("fD").append(fight.id).append("|")
        fight.getFighters(1).stream().filter({ f -> !f.isInvocation() }).forEach({ f -> packet.append(f.getPacketsName()).append("~").append(f.getLvl()).append(";") })
        packet.append("|")
        fight.getFighters(2).stream().filter({ f -> !f.isInvocation() }).forEach({ f -> packet.append(f.getPacketsName()).append("~").append(f.getLvl()).append(";") })
        send(out, packet.toString())
    }

    @JvmStatic fun GAME_SEND_IQ_PACKET(perso: Player, actorID: Int, qua: Int) {
        var packet: String = "IQ" + actorID + "|" + qua
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_JN_PACKET(perso: Player, jobID: Int, lvl: Int) {
        var packet: String = "JN" + jobID + "|" + lvl
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_GDC_PACKET_TO_MAP(map: GameMap, cellID: Int, permanentLevel: Boolean) {
        var cellData: String = map.cellsData.encodeCellData(cellID)
        var hexMask: String = java.lang.Integer.toHexString(map.cellsData.overrideMask(cellID))

        var packet: String = "GDC"+cellID+";"+cellData+hexMask+";"+(if (permanentLevel) "1" else "0")
        sendPacketToMap(map, packet)
    }


    @JvmStatic fun GAME_SEND_GDC_PACKET(player: Player, map: GameMap, permanentLevel: Boolean) {
        var level: String =if (permanentLevel) "1" else "0"

        var ov: CellsDataProvider.CellsDataOverride = map.cellsData

        var packet: String = "GDC" + ov.getOverrides().map { cellID ->
            val cellData: String = ov.encodeCellData(cellID)
            val hexMask: String = java.lang.Integer.toHexString(ov.overrideMask(cellID))
            cellID.toString()+";"+cellData+hexMask+";"+level
        }.collect(Collectors.joining("|"))
        send(player, packet)
    }

    @JvmStatic fun GAME_SEND_GDF_PACKET(player: Player, cellStates: Stream<Pair<Int,KeyFrame?>>) {
        var packet: String = "GDF" + cellStates.map({ p -> "|"+p.first+";"+p.second!!.frame+";"+(if (p.second!!.isObjectInteractive()) "1" else "0") }).collect(Collectors.joining())
        send(player, packet)
    }

    @JvmStatic fun GAME_SEND_GDF_PACKET_TO_MAP(map: GameMap, cellID: Int, state: Int, isInteractive: Boolean) {
        var packet: String = "GDF|" + cellID + ";" + state + ";" +
                (if (isInteractive) "1" else "0")
        map.players.forEach({ p -> send(p, packet) })
    }

    @JvmStatic fun GAME_SEND_GDF_PACKET_TO_FIGHT(player: Player, collection: Collection<GameCase>) {
        var packet: StringBuilder = StringBuilder("GDF|")
        for (cell in  collection) {
            if (cell.`object` == null)
                continue
//            if (cell.`object`.template == null)
//                continue;
//
//            switch (cell.`object`.template!!.id) {
//                case 7515:
//                case 7511:
//                case 7517:
//                case 7512:
//                case 7513:
//                case 7516:
//                case 7550:
//                case 7518:
//                case 7534:
//                case 7535:
//                case 7533:
//                case 7551:
//                case 7500:
//                case 7536:
//                case 7501:
//                case 7502:
//                case 7503:
//                case 7542:
//                case 7541:
//                case 7504:
//                case 7553:
//                case 7505:
//                case 7506:
//                case 7507:
//                case 7557:
//                case 7554:
//                case 7508:
//                case 7509:
//                case 7552:
//                    packet.append(cell.getId()).append(";1;0|");
//                    break;
//            }
        }
        send(player, packet.toString())
    }

    @JvmStatic fun GAME_SEND_GA_PACKET_TO_MAP(map: GameMap, gameActionID: String, actionID: Int, actorID: Int, s2: String) {
        var packet: String = "GA" + gameActionID + ";" + actionID + ";" + actorID
        if (!s2.equals(""))
            packet += ";" + s2

        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_EL_BANK_PACKET(perso: Player) {
        var packet: String = "EL" + perso.parseBankPacket()
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_EL_TRUNK_PACKET(perso: Player, t: Trunk) {
        var packet: String = "EL" + t.parseToTrunkPacket()
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_JX_PACKET(perso: Player, SMs: List<JobStat>) {
        var packet: StringBuilder = StringBuilder()
        packet.append("JX")
        for (sm in  SMs)
            packet.append("|").append(sm.template.id).append(";").append(sm.get_lvl()).append(";").append(sm.getXpString(";")).append(";")
        send(perso, packet.toString())
    }

    @JvmStatic fun GAME_SEND_JO_PACKET(perso: Player, JobStats: List<JobStat>) {
        for (SM in  JobStats) {
            var packet: String = "JO" + SM.position + "|" + SM.getOptBinValue() +
                    "|" + SM.slotsPublic
            send(perso, packet)
        }
    }

    @JvmStatic fun GAME_SEND_JO_PACKET(perso: Player, SM: JobStat) {
        var packet: String = "JO" + SM.position + "|" + SM.getOptBinValue() +
                "|" + SM.slotsPublic
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_JS_PACKET(perso: Player, SMs: List<JobStat>) {
        var packet: StringBuilder = StringBuilder("JS")
        for (sm in  SMs) {
            packet.append(sm.parseJS())
        }
        send(perso, packet.toString())
    }

    @JvmStatic fun GAME_SEND_EsK_PACKET(perso: Player, str: String) {
        var packet: String = "EsK" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_FIGHT_SHOW_CASE(PWs: ArrayList<GameClient>, guid: Int, cellID: Int) {
        var packet: String = "Gf" + guid + "|" + cellID
        for (PW in  PWs) {
            send(PW, packet)
        }
    }

    @JvmStatic fun GAME_SEND_Ea_PACKET(perso: Player, str: String) {
        var packet: String = "Ea" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_EA_PACKET(perso: Player, str: String) {
        var packet: String = "EA" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_Ec_PACKET(perso: Player, str: String) {
        var packet: String = "Ec" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_Em_PACKET(perso: Player, str: String) {
        var packet: String = "Em" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_IO_PACKET_TO_MAP(map: GameMap, guid: Int, str: String) {
        var packet: String = "IO" + guid + "|" + str
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_FRIENDLIST_PACKET(perso: Player) {
        var packet: String = "FL" + perso.getAccount()!!.parseFriendList()
        send(perso, packet)
        if (perso.wife != 0) {
            var packet2: String = "FS" + perso.get_wife_friendlist()
            send(perso, packet2)
        }
    }

    @JvmStatic fun GAME_SEND_FRIEND_ONLINE(friend: Player, perso: Player) {
        var packet: String = "Im0143;" + friend.getAccount()!!.pseudo +
                " (<b><a href='asfunction:onHref,ShowPlayerPopupMenu," +
                friend.name + "'>" + friend.name + "</a></b>)"
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_FA_PACKET(perso: Player, str: String) {
        var packet: String = "FA" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_FD_PACKET(perso: Player, str: String) {
        var packet: String = "FD" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_Rp_PACKET(perso: Player, MP: MountPark?) {
        var packet: StringBuilder = StringBuilder()
        if (MP == null)
            return

        packet.append("Rp").append(MP.owner).append(";").append(MP.price).append(";").append(MP.size).append(";").append(MP.maxObject).append(";")

        var G: Guild? = MP.guild
        //Si une guilde est definie
        if (G != null) {
            packet.append(G.name).append(";").append(G.emblem)
        } else {
            packet.append(";")
        }

        send(perso, packet.toString())
    }

    @JvmStatic fun GAME_SEND_OS_PACKET(perso: Player, pano: Int) {
        var packet: StringBuilder = StringBuilder()
        packet.append("OS")
        var num: Int = perso.getNumbEquipedItemOfPanoplie(pano)
        if (num <= 0)
            packet.append("-").append(pano)
        else {
            packet.append("+").append(pano).append("|")
            var IS: ObjectSet? = World.world.getItemSet(pano)
            if (IS != null) {
                var items: StringBuilder = StringBuilder()
                //Pour chaque objet de la pano
                for (OT in  IS.itemTemplates) {
                    //Si le joueur l'a �quip�
                    if (perso.hasEquiped(OT.id)) {
                        //On l'ajoute au packet
                        if (items.length > 0)
                            items.append(";")
                        items.append(OT.id)
                    }
                }
                packet.append(items).append("|").append(IS.getBonusStatByItemNumb(num).encodeItemSetStats())
            }
        }
        send(perso, packet.toString())
    }

    @JvmStatic fun GAME_SEND_MOUNT_DESCRIPTION_PACKET(perso: Player, DD: Mount) {
        var packet: String = "Rd" + DD.parse()
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_Rr_PACKET(perso: Player, str: String) {
        var packet: String = "Rr" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_ALTER_GM_PACKET(map: GameMap, perso: Player) {
        var packet: String = "GM|~" + perso.parseToGM()
        for (z in  map.players) {
            if (perso.size > 0)
                send(z, packet)
            else if (z.getGroup() != null)
                send(z, packet)
        }
    }

    @JvmStatic fun GAME_SEND_ALTER_GM_PACKET(perso: Player, npc: Npc) {
        var packet: String = "GM|" + npc.encodeGM(true, perso)
        perso.send(packet)
    }

    @JvmStatic fun GAME_SEND_GX_PACKET(perso: Player, npc: Npc) {
        var extra: Int = npc.template.getExtraClip(perso)
        var sExtra: String = if (extra == -1) "-" else (extra).toString()
        var packet: String = "GX" + sExtra + "|" + npc.id
        perso.send(packet)
    }

    @JvmStatic fun GAME_SEND_ALTER_GM_PACKET(perso: Player) {
        var packet: String = "GM|~" + perso.parseToGM()
        perso.send(packet)
    }

    @JvmStatic fun GAME_SEND_Ee_PACKET(perso: Player, c: Char, s: String) {
        var packet: String = "Ee" + c + s
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_Ee_PACKET_WAIT(perso: Player, c: Char, s: String) {
        var packet: String = "Ee" + c + s
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_cC_PACKET(perso: Player, c: Char, s: String) {
        var packet: String = "cC" + c + s
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_ADD_NPC_TO_MAP(map: GameMap, npc: Npc) {
        for (z in  map.players)
            send(z, "GM|" + npc.encodeGM(false, z))
    }

    @JvmStatic fun GAME_SEND_ADD_NPC(player: Player, npc: Npc) {
        send(player, "GM|" + npc.encodeGM(false, player))
    }

    @JvmStatic fun GAME_SEND_ADD_PERCO_TO_MAP(map: GameMap) {
        var packet: String = "GM|" + Collector.parseGM(map)
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_GDO_PACKET_TO_MAP(map: GameMap, c: Char, cell: Int, itm: Int, i: Int) {
        var packet: String = "GDO" + c + cell + ";" + itm + ";" + i
        for (z in  map.players)
            send(z, packet)
    }

    @JvmStatic fun GAME_SEND_ZC_PACKET(p: Player, a: Int) {
        var packet: String = "ZC" + a
        send(p, packet)
    }

    @JvmStatic fun GAME_SEND_GIP_PACKET(p: Player, a: Int) {
        var packet: String = "GIP" + a
        send(p, packet)
    }

    @JvmStatic fun GAME_SEND_gn_PACKET(p: Player) {
        var packet: String = "gn"
        send(p, packet)
    }

    @JvmStatic fun GAME_SEND_gC_PACKET(p: Player, s: String) {
        var packet: String = "gC" + s
        send(p, packet)
    }

    @JvmStatic fun GAME_SEND_gV_PACKET(p: Player) {
        var packet: String = "gV"
        send(p, packet)
    }

    @JvmStatic fun GAME_SEND_gIM_PACKET(p: Player, g: Guild, c: Char) {
        var packet: String = "gIM" + c
        if (c == '+') {
            packet += g.parseMembersToGM()
        }
        send(p, packet)
    }

    @JvmStatic fun GAME_SEND_gIB_PACKET(p: Player, infos: String) {
        var packet: String = "gIB" + infos
        send(p, packet)
    }

    @JvmStatic fun GAME_SEND_gIH_PACKET(p: Player, infos: String) {
        var packet: String = "gIH" + infos
        send(p, packet)
    }

    @JvmStatic fun GAME_SEND_gS_PACKET(p: Player, gm: GuildMember) {
        send(p, "gS" + gm.guild!!.name + "|" + gm.guild!!.emblem.replace(',', '|') + "|" + gm.parseRights())
    }

    @JvmStatic fun GAME_SEND_gJ_PACKET(p: Player, str: String) {
        var packet: String = "gJ" + str
        send(p, packet)
    }

    @JvmStatic fun GAME_SEND_gK_PACKET(p: Player, str: String) {
        var packet: String = "gK" + str
        send(p, packet)
    }

    @JvmStatic fun GAME_SEND_gIG_PACKET(p: Player, g: Guild) {
        if (g == null) {
            send(p, "gIG")
        } else {
            var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.guilds

            var xpMin: Long = xpTable.minXpAt(g.lvl)
            var xpMax: Long = xpTable.maxXpAt(g.lvl)
            send(p, "gIG" + (if (g.haveTenMembers()) 1 else 0) + "|" + g.lvl + "|" + xpMin + "|" + g.xp + "|" + xpMax)
        }
    }

    @JvmStatic fun GAME_SEND_WC_PACKET(perso: Player) {
        var packet: String = "WC" + perso.parseZaapList()
        send(perso.getGameClient()!!, packet)
    }

    @JvmStatic fun GAME_SEND_WV_PACKET(out: Player) {
        var packet: String = "WV"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ZAAPI_PACKET(perso: Player, list: String) {
        var packet: String = "Wc" + perso.curMap!!.id + "|" + list
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_CLOSE_ZAAPI_PACKET(out: Player) {
        var packet: String = "Wv"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_WUE_PACKET(out: Player) {
        var packet: String = "WUE"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EMOTE_LIST(perso: Player, s: String) {
        send(perso, "eL" + s)
    }

    @JvmStatic fun GAME_SEND_ADD_ENEMY(out: Player, pr: Player) {
        var packet: String = "iAK" + pr.getAccount()!!.name + ";2;" + pr.name + ";36;10;0;100.FL."
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_iAEA_PACKET(out: Player) {
        var packet: String = "iAEA."
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_ENEMY_LIST(perso: Player) {
        var packet: String = "iL" + perso.getAccount()!!.parseEnemyList()
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_iD_COMMANDE(perso: Player, str: String) {
        var packet: String = "iD" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_BWK(perso: Player, str: String) {
        var packet: String = "BWK" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_KODE(perso: Player, str: String) {
        var packet: String = "K" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_hOUSE(perso: Player, str: String) {
        var packet: String = "h" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_FORGETSPELL_INTERFACE(sign: Char, perso: Player) {
        var packet: String = "SF" + sign
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_R_PACKET(perso: Player, str: String) {
        var packet: String = "R" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_gIF_PACKET(perso: Player, str: String) {
        var packet: String = "gIF" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_gITM_PACKET(perso: Player, str: String) {
        var packet: String = "gITM" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_gITp_PACKET(perso: Player, str: String) {
        var packet: String = "gITp" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_gITP_PACKET(perso: Player, str: String) {
        var packet: String = "gITP" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_IH_PACKET(perso: Player, str: String) {
        var packet: String = "IH" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_FLAG_PACKET(perso: Player, cible: Player) {
        var packet: String = "IC" + cible.curMap!!.x + "|" + cible.curMap!!.y
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_FLAG_PACKET(perso: Player, CurMap: GameMap) {
        var packet: String = "IC" + CurMap.x + "|" + CurMap.y
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_DELETE_FLAG_PACKET(perso: Player) {
        var packet: String = "IC|"
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_gT_PACKET(perso: Player, str: String) {
        var packet: String = "gT" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_GUILDHOUSE_PACKET(perso: Player) {
        var packet: String = "gUT"
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_GUILDENCLO_PACKET(perso: Player) {
        var packet: String = "gUF"
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_EHm_DEL_PACKET(out: Player, id: Int) {
        var packet: String = "EHm-" + id
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EHm_ADD_PACKET(out: Player, c: BigStore.CheapestListings) {
        var packet: String = "EHm+" + java.lang.String.join("|",
                java.lang.String.valueOf(c.lineId),
                java.lang.String.valueOf(c.itemTemplateId),
                java.lang.String.valueOf(c.stats),
                if (c.minPrices[0] == 0) "" else java.lang.String.valueOf(c.minPrices[0]),
                if (c.minPrices[1] == 0) "" else java.lang.String.valueOf(c.minPrices[1]),
                if (c.minPrices[2] == 0) "" else java.lang.String.valueOf(c.minPrices[2])
        )
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EHP_PACKET(out: Player, id: Int) //Packet d'envoie du prix moyen du template (En r�ponse a un packet EHP)
    {
        var template: ObjectTemplate? = World.world.getObjTemplate(id)
        if (template != null)
            send(out, "EHP" + id + "|" + template.avgPrice)
    }

    @JvmStatic fun GAME_SEND_EHl(out: Player, bigStore: BigStore, category: Int, templateId: Int) {
        var packet: String = "EHl" + templateId + "|" + bigStore.linesForTemplate(category, templateId).stream()
                .map({ line ->
                    // Encode lines
                    java.lang.String.join(
                            ";",
                            java.lang.String.valueOf(line.lineId),
                            line.stats,
                            if (line.minPrices[0] == 0) "" else java.lang.String.valueOf(line.minPrices[0]),
                            if (line.minPrices[1] == 0) "" else java.lang.String.valueOf(line.minPrices[1]),
                            if (line.minPrices[2] == 0) "" else java.lang.String.valueOf(line.minPrices[2]),
                            java.lang.String.valueOf(line.itemTemplateId)
                    )
                })
                .collect(Collectors.joining("|"))

        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_EHL_PACKET(out: Player, categ: Int, templates: List<Int>) {
        var packet: String = "EHL" + categ + "|" + templates.stream().map { it.toString() }.collect(Collectors.joining(";"))
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_HDVITEM_SELLING(perso: Player, hdvId: Int) {
        var packet: String = "EL" + perso.getAccount()!!.getHdvEntries(hdvId).stream()
                .filter(Objects::nonNull)
                .map(BigStoreListing::parseToEL)
                .collect(Collectors.joining("|"))
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_WEDDING(action: Int, homme: Int, femme: Int, parlant: Int) {
        var packet: String = "GA;" + action + ";" + homme + ";" + homme + "," + femme + "," + parlant
        var Homme: Player? = World.world.getPlayer(homme)
        send(Homme!!, packet)
    }

    @JvmStatic fun GAME_SEND_PF(perso: Player, str: String) {
        var packet: String = "PF" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_MERCHANT_LIST(P: Player) {
        var packet: StringBuilder = StringBuilder()
        packet.append("GM|")
        if (World.world.getSeller(P.curMap!!.id) == null)
            return
        for (pID in  World.world.getSeller(P.curMap!!.id)!!) {
            if (!World.world.getPlayer(pID)!!.isOnline
                    && World.world.getPlayer(pID)!!.seeSeller) {
                packet.append("~").append(World.world.getPlayer(pID)!!.parseToMerchant()).append("|")
            }
        }
        if (packet.length < 5)
            return
        send(P, packet.toString())
    }

    @JvmStatic fun GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(fight: Fight, teams: Int, state: Int, cancelBtn: Int, duel: Int, spec: Int, time: Long, type: Int) {
        var packet: StringBuilder = StringBuilder()
        packet.append("GJK").append(state).append("|")
        packet.append(cancelBtn).append("|").append(duel).append("|")
        packet.append(spec).append("|").append(time).append("|").append(type)
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft())
                continue
            send(f.player!!, packet.toString())
        }
    }

    @JvmStatic fun GAME_SEND_GJK_PACKET(out: Player, state: Int, cancelBtn: Int, duel: Int, spec: Int, time: Long, unknown: Int) {
        send(out, "GJK" + state + "|" + cancelBtn + "|" + duel + "|" + spec + "|" + time + "|" + unknown)
    }

    @JvmStatic fun GAME_SEND_cMK_PACKET_INCARNAM_CHAT(suffix: String, guid: Int, name: String, msg: String) {
        var packet: String = "cMK" + suffix + "|" + guid + "|" + name + "|"

        for (target in  World.world.onlinePlayers) {
            if (target.curMap != null && target.curMap!!.subArea != null && target.curMap!!.subArea!!.area != null && target.curMap!!.subArea!!.area!!.id == 45) {
                send(target, packet + msg)
            }
        }
    }

    @JvmStatic fun GAME_SEND_Ag_PACKET(out: GameClient, idObjet: Int, codObjet: String) {
        var packet: String = "Ag1|" +
                idObjet +
                "|Cadeau Dofus| Voilà un joli cadeau pour vous ! " +
                "Un jeune aventurier comme vous sera sans servir de la meilleur façon ! " +
                "Bonne continuation avec ceci ! |DOFUS|" + codObjet
        send(out, packet)
    }

    @JvmStatic fun SEND_Ej_LIVRE(pj: Player, str: String) {
        var packet: String = "Ej" + str
        send(pj, packet)
    }

    @JvmStatic fun SEND_EW_METIER_PUBLIC(pj: Player, str: String) {
        var packet: String = "EW" + str
        send(pj, packet)
    }

    @JvmStatic fun SEND_EJ_LIVRE(pj: Player, str: String) {
        var packet: String = "EJ" + str
        send(pj, packet)
    }

    @JvmStatic fun SEND_GDF_PERSO(perso: Player, celda: Int, frame: Int, esInteractivo: Int) {
        var packet: String = "GDF|" + celda + ";" + frame + ";" + esInteractivo
        send(perso, packet)
    }

    @JvmStatic fun SEND_EMK_MOVE_ITEM(out: GameClient, tipoOG: Char, signo: String, s1: String) {
        var packet: String = "EMK" + tipoOG + signo
        if (!s1.equals(""))
            packet += s1
        send(out, packet)
    }

    @JvmStatic fun SEND_OR_DELETE_ITEM(out: GameClient, id: Int) {
        var packet: String = "OR" + id
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_CHALLENGE_FIGHT(fight: Fight, team: Int, str: String) {
        var packet: StringBuilder = StringBuilder()
        packet.append("Gd").append(str)

        for (fighter in  fight.getFighters(team)) {
            if (fighter.hasLeft())
                continue
            if (fighter.player == null
                    || !fighter.player!!.isOnline)
                continue
            send(fighter.player!!, packet.toString())
        }
    }

    @JvmStatic fun GAME_SEND_CHALLENGE_PERSO(p: Player, str: String) {
        send(p, "Gd" + str)
    }

    @JvmStatic fun GAME_SEND_Im_PACKET_TO_CHALLENGE(fight: Fight, challenge: Int, str: String) {
        var packet: StringBuilder = StringBuilder()
        packet.append("Im").append(str)
        for (fighter in  fight.getFighters(challenge)) {
            if (fighter.hasLeft())
                continue
            if (fighter.player == null
                    || !fighter.player!!.isOnline)
                continue
            send(fighter.player!!, packet.toString())
        }
    }

    @JvmStatic fun GAME_SEND_Im_PACKET_TO_CHALLENGE_PERSO(player: Player, str: String) {
        send(player, "Im" + str)
    }

    @JvmStatic fun GAME_SEND_MESSAGE_SERVER(out: Player, args: String) {
        var packet: String = "M1" + args
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_WELCOME(perso: Player) {
        send(perso, "TB")
    }

    @JvmStatic fun GAME_SEND_Eq_PACKET(Personnage: Player, Prix: Long) {
        send(Personnage, "Eq1|1|" + Prix)
    }

    @JvmStatic fun GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(fight: Fight, teams: Int) {
        var packet: String = "GA;0"
        for (f in  fight.getFighters(teams)) {
            if (f.hasLeft() || f.player == null
                    || !f.player!!.isOnline)
                continue
            send(f.player!!, packet)
        }
    }

    @JvmStatic fun SEND_gA_PERCEPTEUR(perso: Player, str: String) {
        var packet: String = "gA" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_PERCO_INFOS_PACKET(perso: Player, perco: Collector, car: String) {
        send(perso, "gA" + car + perco.getFullName() + "|" + "-1" + "|" + World.world.getMap(perco.map)!!.x + "|" + World.world.getMap(perco.map)!!.y)
    }

    @JvmStatic fun SEND_Wp_MENU_Prisme(perso: Player) {
        var packet: String = "Wp" + perso.parsePrismesList()
        send(perso.getGameClient()!!, packet)
    }

    @JvmStatic fun SEND_Ww_CLOSE_Prisme(out: Player) {
        var packet: String = "Ww"
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(p: Player, str: String) {
        var packet: String = "am" + str
        //Evite les putins de flood en console.
        if (p == null || p.getAccount() == null)
            return
        send(p, packet)
    }

    @JvmStatic fun SEND_CB_BONUS_CONQUETE(pj: Player, str: String) {
        var packet: String = "CB" + str
        send(pj, packet)
    }

    @JvmStatic fun SEND_Cb_BALANCE_CONQUETE(pj: Player, str: String) {
        var packet: String = "Cb" + str
        send(pj, packet)
    }

    @JvmStatic fun SEND_GM_PRISME_TO_MAP(out: GameClient, Map: GameMap) {// envia informacion de todos mercantes en 1 Map
        var packet: String = Map.getPrismeGMPacket()
        if (Objects.equals(packet, "") || packet.isEmpty())
            return
        send(out, packet)
    }

    @JvmStatic fun GAME_SEND_PRISME_TO_MAP(Map: GameMap, Prisme: Prism) {
        var packet: String = Prisme.parseToGM()
        for (z in  Map.players)
            send(z, packet)
    }

    @JvmStatic fun SEND_CP_INFO_DEFENSEURS_PRISME(perso: Player, str: String) {
        var packet: String = "CP" + str
        send(perso, packet)
    }

    @JvmStatic fun SEND_Cp_INFO_ATTAQUANT_PRISME(perso: Player, str: String) {
        var packet: String = "Cp" + str
        send(perso, packet)
    }

    @JvmStatic fun SEND_CW_INFO_WORLD_CONQUETE(pj: Player, str: String) {
        var packet: String = "CW" + str
        send(pj, packet)
    }

    @JvmStatic fun SEND_CIJ_INFO_JOIN_PRISME(pj: Player, str: String) {
        var packet: String = "CIJ" + str
        send(pj, packet)
    }

    @JvmStatic fun GAME_SEND_aM_ALIGN_PACKET_TO_AREA(perso: Player, str: String) {
        var packet: String = "aM" + str
        send(perso, packet)
    }

    @JvmStatic fun SEND_GA_ACTION_TO_Map(Map: GameMap, gameActionID: String, actionID: Int, s1: String, s2: String) {
        var packet: String = "GA" + gameActionID + ";" + actionID + ";" + s1
        if (!s2.equals(""))
            packet += ";" + s2
        for (z in  Map.players)
            send(z, packet)
    }

    @JvmStatic fun SEND_CS_SURVIVRE_MESSAGE_PRISME(perso: Player, str: String) {
        var packet: String = "CS" + str
        send(perso, packet)
    }

    @JvmStatic fun SEND_CD_MORT_MESSAGE_PRISME(perso: Player, str: String) {
        var packet: String = "CD" + str
        send(perso, packet)
    }

    @JvmStatic fun SEND_CA_ATTAQUE_MESSAGE_PRISME(perso: Player, str: String) {
        var packet: String = "CA" + str
        send(perso, packet)
    }

    @JvmStatic fun GAME_SEND_ACTION_TO_DOOR(map: GameMap, args: Int, open: Boolean) {
        for (z in  map.players)
            GAME_SEND_ACTION_TO_DOOR(z, args, open)
    }

    @JvmStatic fun GAME_SEND_ACTION_TO_DOOR(p: Player, args: Int, open: Boolean) {
        send(p, "GDF|" + args + ";" + (if (open) "2" else "4"))
    }

    @JvmStatic fun sendPacketToMap(map: GameMap, packet: String) {
        for (perso in  map.players)
            send(perso, packet)
    }
}
