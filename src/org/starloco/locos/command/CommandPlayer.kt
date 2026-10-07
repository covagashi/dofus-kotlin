package org.starloco.locos.command

import org.starloco.locos.auction.AuctionManager
import org.starloco.locos.client.Player
import org.starloco.locos.client.other.Party
import org.starloco.locos.common.SocketManager
import org.starloco.locos.event.EventManager
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.kernel.Logging
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.util.TimerWaiter
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import org.starloco.locos.common.splitJ

object CommandPlayer {

    @JvmField
    val canal = "Général"
    @JvmField
    var canalMute = false

    @JvmStatic
    fun analyse(player: Player, msg: String): Boolean {
        var msg = msg
        msg = msg.replace("|", "")
        if (msg[0] == '.') {
            if (command(msg, "help")) {
                return commandHelp(player, msg)
            } else if (command(msg, "all") && msg.length > 5) {
                return commandAll(player, msg)
            } else if (command(msg, "noall")) {
                return commandNoAll(player, msg)
            } else if (command(msg, "staff") || command(msg, "admin")) {
                return commandStaff(player, msg)
            } else if (command(msg, "deblo")) {
                return commandDeblo(player, msg)
            } else if (command(msg, "infos")) {
                return commandInfos(player, msg)
            } else if (command(msg, "master") || command(msg, "maitre") || command(msg, "maître") || command(msg, "maestro")) {
                return commandMaster(player, msg)
            } else if (command(msg, "pass")) {
                return commandPass(player, msg)
            } else if (command(msg, "interval")) {
                return commandInterval(player, msg)
            } else if (command(msg, "start") || command(msg, "astrub")) {
                return commandAstrub(player, msg)
            } else if (command(msg, "walkfast")) {
                player.walkFast = !player.walkFast
                return true
            } else if (command(msg, "vip")) {
                player.sendMessage(player.getLang().trans("command.commandplayer.vip"))
                return true
            } else if (command(msg, "savepos")) {
                return commandStart(player, msg)
            } else if (command(msg, "transfert")) {
                return commandTransfert(player, msg)
            } else if (command(msg, "banque")) {
                if (!player.getAccount()!!.isSubscribeWithoutCondition()) {
                    player.sendMessage(player.getLang().trans("command.commandplayer.life.nosubscribe"))
                    return true
                }
                if (player.isInPrison() || player.fight != null || player.exchangeAction != null)
                    return true
                player.openBank()
                return true
            } else if (command(msg, "groupe")) {
                if (player.isInPrison() || player.fight != null)
                    return true
                val count = byteArrayOf(0)
                World.world.onlinePlayers.stream().filter { p -> p != player && p.party == null && p.getAccount()!!.currentIp == player.getAccount()!!.currentIp && p.fight == null && !p.isInPrison() }.forEach { p ->
                    if (count[0].toInt() <= 8) {
                        if (player.party == null) {
                            val party = Party(player, p)
                            SocketManager.GAME_SEND_GROUP_CREATE(player.getGameClient()!!, party)
                            SocketManager.GAME_SEND_PL_PACKET(player.getGameClient()!!, party)
                            SocketManager.GAME_SEND_GROUP_CREATE(p.getGameClient()!!, party)
                            SocketManager.GAME_SEND_PL_PACKET(p.getGameClient()!!, party)
                            player.party = party
                            p.party = party
                            SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(player.getGameClient()!!, party)
                            SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(p.getGameClient()!!, party)
                        } else {
                            SocketManager.GAME_SEND_GROUP_CREATE(p.getGameClient()!!, player.party!!)
                            SocketManager.GAME_SEND_PL_PACKET(p.getGameClient()!!, player.party!!)
                            SocketManager.GAME_SEND_PM_ADD_PACKET_TO_GROUP(player.party!!, p)
                            player.party!!.addPlayer(p)
                            p.party = player.party
                            SocketManager.GAME_SEND_ALL_PM_ADD_PACKET(p.getGameClient()!!, player.party!!)
                            SocketManager.GAME_SEND_PR_PACKET(p)
                        }
                    }
                    count[0] = (count[0] + 1).toByte()
                }

                return true
            } else if (command(msg, "event")) {
                return player.cantTP() || EventManager.instance.subscribe(player).toInt() == 1
            } else if (command(msg, "auction")) {
                if (player.cantTP() || player.dead.toInt() != 0 || player.isGhost || player.away || player.fight != null)
                    return true
                AuctionManager.getInstance().onPlayerCommand(player, msg.splitJ(" ").toTypedArray())
                return true
            } else {
                player.sendMessage(player.getLang().trans("command.commandplayer.default"))
                return true
            }
        }
        return false
    }

    private fun commandPass(player: Player, msg: String): Boolean {
        if (player.party != null && player.party!!.master != null) {
            val option = player.party!!.getOptionByPlayer(player)
            if (option != null) {
                option.togglePass()
                player.sendMessage(player.getLang().trans("command.commandplayer.pass"))
                return true
            }
        } else {
            player.sendMessage(player.getLang().trans("command.commandplayer.pass.error"))
        }
        return false
    }

    private fun commandInterval(player: Player, msg: String): Boolean {
        if (player.party != null && player.party!!.master != null) {
            val option = player.party!!.getOptionByPlayer(player)
            if (option != null) {
                try {
                    val second = (msg.split(" ")[1]).toByte()
                    for (opt in player.party!!.getOptions()) {
                        if (opt != null && opt.second == second) {
                            player.sendMessage(player.getLang().trans("command.commandplayer.interval.error"))
                            return true
                        }
                    }

                    option.second = second
                } catch (ignored: Exception) {
                    player.sendMessage(player.getLang().trans("command.commandplayer.interval.error"))
                }

                if (option.second < 1) option.second = 1.toByte()
                else if (option.second > 29) option.second = 29.toByte()

                player.sendMessage(player.getLang().trans("command.commandplayer.interval"))
                return true
            }
        } else {
            player.sendMessage(player.getLang().trans("command.commandplayer.interval.master"))
        }
        return false
    }

    private val bannedItemJob = listOf(491, 493, 494, 495, 496)

    private fun commandTransfert(player: Player, msg: String): Boolean {
        if (player.isInPrison() || player.fight != null)
            return true

        if (commandTransfertWithMaster(player, msg)) {
            return true
        }

        if (player.exchangeAction == null || player.exchangeAction!!.getType() != ExchangeAction.IN_BANK) {
            player.sendMessage(player.getLang().trans("command.commandplayer.transfer.noinbank"))
            return true
        }
        val info = msg.split(" ")
        val map = player.curMap.id
        SocketManager.GAME_SEND_EV_PACKET(player.getGameClient()!!)
        player.sendTypeMessage("Bank", player.getLang().trans("command.commandplayer.transfer.waitting"))
        var count = 0

        val bank = info.size >= 2 && info[1].equals("bank", ignoreCase = true)

        for (obj in ArrayList(if (bank) player.getAccount()!!.bank else player.items.values)) {
            if (info.size == 2) {
                if (obj == null || obj.template == null || !obj.template!!.strTemplate.isEmpty())
                    continue
                if (obj.template!!.isAnEquipment(true, null))
                    continue
                when (obj.template!!.type) {
                    Constant.ITEM_TYPE_OBJET_VIVANT, Constant.ITEM_TYPE_PRISME,
                    Constant.ITEM_TYPE_FILET_CAPTURE, Constant.ITEM_TYPE_CERTIF_MONTURE,
                    Constant.ITEM_TYPE_OBJET_UTILISABLE, Constant.ITEM_TYPE_OBJET_ELEVAGE,
                    Constant.ITEM_TYPE_CADEAUX, Constant.ITEM_TYPE_PARCHO_RECHERCHE,
                    Constant.ITEM_TYPE_PIERRE_AME, Constant.ITEM_TYPE_BOUCLIER,
                    Constant.ITEM_TYPE_SAC_DOS, Constant.ITEM_TYPE_OBJET_MISSION,
                    Constant.ITEM_TYPE_BOISSON, Constant.ITEM_TYPE_CERTIFICAT_CHANIL,
                    Constant.ITEM_TYPE_FEE_ARTIFICE, Constant.ITEM_TYPE_MAITRISE,
                    Constant.ITEM_TYPE_POTION_SORT, Constant.ITEM_TYPE_POTION_METIER,
                    Constant.ITEM_TYPE_POTION_OUBLIE, Constant.ITEM_TYPE_BONBON,
                    Constant.ITEM_TYPE_PERSO_SUIVEUR, Constant.ITEM_TYPE_RP_BUFF,
                    Constant.ITEM_TYPE_MALEDICTION, Constant.ITEM_TYPE_BENEDICTION,
                    Constant.ITEM_TYPE_TRANSFORM, Constant.ITEM_TYPE_DOCUMENT,
                    Constant.ITEM_TYPE_QUETES -> continue
                }
            }
            if (obj.position != -1)
                continue
            when (obj.template!!.type) {
                Constant.ITEM_TYPE_BONBON, Constant.ITEM_TYPE_PERSO_SUIVEUR,
                Constant.ITEM_TYPE_RP_BUFF, Constant.ITEM_TYPE_MALEDICTION,
                Constant.ITEM_TYPE_BENEDICTION, Constant.ITEM_TYPE_TRANSFORM,
                Constant.ITEM_TYPE_DOCUMENT, Constant.ITEM_TYPE_QUETES -> continue
            }
            if (bannedItemJob.contains(obj.template!!.id))
                continue
            count++
            if (!bank) {
                player.addInBank(obj.guid, obj.quantity, false)
            } else {
                player.removeFromBank(obj.guid, obj.quantity)
            }
        }

        player.sendTypeMessage("Bank", player.getLang().trans("command.commandplayer.transfer.good", count))
        player.exchangeAction = null
        if (player.curMap.id == map)
            player.openBank()
        return true
    }

    private fun commandTransfertWithMaster(player: Player, msg: String): Boolean {
        val info = msg.split(" ")
        if (info.size == 1 && player.party != null && player.party!!.master != null && player.party!!.master!!.id == player.id) {
            val objects = ArrayList<GameObject>()
            player.party!!.players.stream()
                    .filter { follower -> follower.fight == null && follower.gameClient != null && player.party!!.isWithTheMaster(follower!!, false, false) }
                    .forEach { follower ->
                        follower.gameClient!!.clearAllPanels(null)
                        for (obj in ArrayList(follower.items.values)) {
                            if (obj != null) {
                                if (obj.position != -1 || obj.template!!.isAnEquipment(true, null))
                                    continue
                                when (obj.template!!.type) {
                                    Constant.ITEM_TYPE_OBJET_VIVANT, Constant.ITEM_TYPE_PRISME,
                                    Constant.ITEM_TYPE_FILET_CAPTURE, Constant.ITEM_TYPE_CERTIF_MONTURE,
                                    Constant.ITEM_TYPE_OBJET_UTILISABLE, Constant.ITEM_TYPE_OBJET_ELEVAGE,
                                    Constant.ITEM_TYPE_CADEAUX, Constant.ITEM_TYPE_PARCHO_RECHERCHE, Constant.ITEM_TYPE_PIERRE_AME,
                                    Constant.ITEM_TYPE_BOUCLIER, Constant.ITEM_TYPE_SAC_DOS, Constant.ITEM_TYPE_OBJET_MISSION,
                                    Constant.ITEM_TYPE_BOISSON, Constant.ITEM_TYPE_CERTIFICAT_CHANIL, Constant.ITEM_TYPE_FEE_ARTIFICE,
                                    Constant.ITEM_TYPE_MAITRISE, Constant.ITEM_TYPE_POTION_SORT,
                                    Constant.ITEM_TYPE_POTION_METIER, Constant.ITEM_TYPE_POTION_OUBLIE,
                                    Constant.ITEM_TYPE_BONBON, Constant.ITEM_TYPE_PERSO_SUIVEUR,
                                    Constant.ITEM_TYPE_RP_BUFF, Constant.ITEM_TYPE_MALEDICTION,
                                    Constant.ITEM_TYPE_BENEDICTION, Constant.ITEM_TYPE_TRANSFORM,
                                    Constant.ITEM_TYPE_DOCUMENT, Constant.ITEM_TYPE_QUETES,
                                    Constant.ITEM_TYPE_OUTIL -> continue
                                }
                                follower.removeItem(obj.guid, obj.quantity, true, false)
                                objects.add(obj)
                            }
                        }
                    }
            TimerWaiter.addNext({
                for (obj in objects) {
                    if (!player.addItem(obj, true, false))
                        World.world.removeGameObject(obj.guid)
                }
                player.sendTypeMessage("Transfert", objects.size.toString() + " objets récupérés.")
            }, 1000)
            return true
        }
        return false
    }

    private fun commandStart(player: Player, msg: String): Boolean {
        val mapId = player.curMap.id
        if (player.isInPrison() || player.cantTP() || player.fight != null)
            return true
        player.warpToSavePos()

        val party = player.party
        if (party != null && party.master != null && party.master!!.name == player.name) {
            for (slave in player.party!!.players) {
                if (slave.curMap.id == mapId) {
                    if (!player.isInPrison())
                        if (!player.cantTP())
                            if (player.fight == null)
                                slave.warpToSavePos()
                }
            }
        }
        return true
    }

    private fun commandAstrub(player: Player, msg: String): Boolean {
        val mapId = player.curMap.id
        if (player.isInPrison() || player.cantTP() || player.fight != null || Config.gameServerId == 22)
            return true
        player.teleport(952, 250)

        val party = player.party
        if (party != null && party.master != null && party.master!!.name == player.name) {
            player.party!!.players.stream().filter { p -> party.isWithTheMaster(p, false, true) }.forEach { slave ->
                if (slave.curMap.id == mapId) {
                    if (!player.isInPrison() && !player.cantTP())
                        if (player.fight == null)
                            slave.teleport(952, 250)
                }
            }
        }
        return true
    }

    private fun commandMaster(player: Player, msg: String): Boolean {
        val split = msg.split(" ")

        if (split.size == 2) {
            val name = split[1]
            val target = World.world.getPlayerByName(name)

            if (target != null && target !== player) {
                if (target.party === player.party) {
                    val party = target.party
                    party!!.chief = target
                    party!!.master = target

                    for (member in party!!.players)
                        member.send("PL" + target.id)
                } else {
                    player.sendMessage(player.getLang().trans("command.commandplayer.master.nogroup.name", name))
                }
            }
            return true
        } else {
            if (player.cantTP()) return true

            val party = player.party

            if (party == null) {
                player.sendMessage(player.getLang().trans("command.commandplayer.master.nogroup"))
                return true
            }

            val players = player.party!!.players

            if (party.chief.name != player.name) {
                player.sendMessage(player.getLang().trans("command.commandplayer.master.noking"))
                return true
            }

            if (msg.length <= 8 && party.master != null) {
                player.sendMessage(player.getLang().trans("command.commandplayer.master.disabled"))
                players.stream().filter { follower -> follower !== party.master }
                        .forEach { follower -> SocketManager.GAME_SEND_MESSAGE(follower, follower.lang.trans("command.commandplayer.master.nofollow", party.master!!.name)) }
                party.master = null
                return true
            }

            var target: Player? = player

            if (msg.length > 8) {
                val name = msg.substring(8, msg.length - 1)
                target = World.world.getPlayerByName(name)
            }

            if (target == null) {
                player.sendMessage(player.getLang().trans("command.commandplayer.master.noavailable"))
                return true
            }
            if (target.party == null || !target.party!!.players.contains(player)) {
                player.sendMessage(player.getLang().trans("command.commandplayer.master.nogroup.atuser"))
                return true
            }

            party.master = target

            val message = player.getLang().trans("command.commandplayer.master.follow", target.name)
            for (follower in players)
                if (follower !== target)
                    SocketManager.GAME_SEND_MESSAGE(follower, message)

            party.moveAllPlayersToMaster(null, false)
            SocketManager.GAME_SEND_MESSAGE(target, target.getLang().trans("command.commandplayer.master.master"))
            return true
        }
    }

    private fun commandHelp(player: Player, msg: String): Boolean {
        player.sendMessage(player.getLang().trans("command.commandplayer.default"))
        return true
    }

    //region Commands

    private fun commandAll(player: Player, msg: String): Boolean {
        if (player.isInPrison())
            return true
        if (canalMute && player.getGroup() == null) {
            player.sendMessage(player.getLang().trans("command.commandplayer.commandall.unvailable"))
            return true
        }
        if (player.noall) {
            player.sendMessage(player.getLang().trans("command.commandplayer.noall"))
            return true
        }
        if (player.getGroup() == null && System.currentTimeMillis() - player.getGameClient()!!.timeLastTaverne < 10000) {
            player.sendMessage(player.getLang().trans("command.commandplayer.allwait").replace("#1", (10 - ((System.currentTimeMillis() - player.getGameClient()!!.timeLastTaverne) / 1000)).toString()))
            return true
        }

        player.getGameClient()!!.timeLastTaverne = System.currentTimeMillis()

        val prefix = "<font color='#C35617'>[" + (SimpleDateFormat("HH:mm").format(Date(System.currentTimeMillis()))) + "] (" + canal + ") (" + (if (Config.gameServerKey.isNullOrEmpty()) getNameServerById(Config.gameServerId) else Config.gameServerKey) + ") <b><a href='asfunction:onHref,ShowPlayerPopupMenu," + player.name + "'>" + player.name + "</a></b>"

        Logging.getInstance().write("AllMessage", "[" + (SimpleDateFormat("HH:mm").format(Date(System.currentTimeMillis()))) + "] : " + player.name + " : " + msg.substring(5, msg.length - 1))

        val message = "Im116;" + prefix + "~" + msg.substring(5, msg.length).replace(";", ":").replace("~", "").replace("|", "").replace("<", "").replace(">", "") + "</font>"

        World.world.onlinePlayers.stream().filter { p -> !p.noall }.forEach { p -> p.send(message) }
        Config.exchangeClient!!.send("DM" + player.name + ";" + getNameServerById(Config.gameServerId) + ";" + msg.substring(5, msg.length).replace("\n", "").replace("\r", "").replace(";", ":").replace("~", "").replace("|", "").replace("<", "").replace(">", ""))
        return true
    }

    private fun commandNoAll(player: Player, msg: String): Boolean {
        if (player.noall) {
            player.noall = false
            player.sendMessage(player.getLang().trans("command.commandplayer.all.on"))
        } else {
            player.noall = true
            player.sendMessage(player.getLang().trans("command.commandplayer.all.off"))
        }
        return true
    }

    private fun commandDeblo(player: Player, msg: String): Boolean {
        if (player.cantTP())
            return true
        if (player.fight != null)
            return true
        if (player.curCell.isWalkable(false)) {
            player.sendMessage(player.getLang().trans("command.commandplayer.deblo.no"))
            return true
        }
        player.teleport(player.curMap.id, player.curMap.randomFreeCellId)
        return true
    }

    private fun commandInfos(player: Player, msg: String): Boolean {
        var uptime = System.currentTimeMillis() - Config.startTime
        val jour = (uptime / (1000 * 3600 * 24)).toInt()
        uptime %= (1000 * 3600 * 24)
        val hour = (uptime / (1000 * 3600)).toInt()
        uptime %= (1000 * 3600)
        val min = (uptime / (1000 * 60)).toInt()
        uptime %= (1000 * 60)
        val sec = (uptime / 1000).toInt()
        val nbPlayer = Config.gameServer!!.getClients().stream().filter { gc -> gc != null && gc.player != null }.count().toInt()

        var mess = player.getLang().trans("command.commandplayer.info.uptime", jour.toString(), hour.toString(), min.toString(), sec.toString())
        if (nbPlayer > 0)
            mess += player.getLang().trans("command.commandplayer.info.online", nbPlayer.toString())
        player.sendMessage(mess)
        return true
    }

    private fun commandStaff(player: Player, msg: String): Boolean {
        var message = player.getLang().trans("command.commandplayer.staff")
        var vide = true
        for (target in World.world.onlinePlayers) {
            if (target == null)
                continue
            if (target.getGroup() == null || target.isInvisible)
                continue

            message += "\n- <b><a href='asfunction:onHref,ShowPlayerPopupMenu," + target.name + "'>[" + target.getGroup()!!.name + "] " + target.name + "</a></b>"
            vide = false
        }
        if (vide)
            message = player.getLang().trans("command.commandplayer.staff.none")
        player.sendMessage(message)
        return true
    }

    private fun command(msg: String, command: String): Boolean {
        return msg.length > command.length && msg.substring(1, command.length + 1).equals(command, ignoreCase = true)
    }
    //endregion

    private fun getNameServerById(id: Int): String {
        when (id) {
            13 -> return "Silouate"
            19 -> return "Allister"
            22 -> return "Oto Mustam"
            1 -> return "Jiva"
            2 -> return "Zeus"
            37 -> return "Nostalgy"
            4001 -> return "Alma"
            4002 -> return "Aguabrial"
            4005 -> return "Bolgrot"
        }
        return "Unknown"
    }
}
