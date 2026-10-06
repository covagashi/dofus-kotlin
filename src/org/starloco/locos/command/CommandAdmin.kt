package org.starloco.locos.command

import org.starloco.locos.entity.npc.NpcMovable
import org.starloco.locos.quest.QuestProgress
import org.starloco.locos.script.DataScriptVM
import org.starloco.locos.util.Pair
import org.starloco.locos.area.map.GameCase
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.client.Account
import org.starloco.locos.client.Player
import org.starloco.locos.command.administration.AdminUser
import org.starloco.locos.command.administration.Command
import org.starloco.locos.command.administration.Group
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.*
import org.starloco.locos.database.data.login.*
import org.starloco.locos.entity.Collector
import org.starloco.locos.entity.monster.Monster
import org.starloco.locos.entity.monster.MonsterGrade
import org.starloco.locos.entity.monster.MonsterGroup
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.entity.npc.Npc
import org.starloco.locos.entity.pet.PetEntry
import org.starloco.locos.event.EventManager
import org.starloco.locos.event.type.Event
import org.starloco.locos.event.type.EventFindMe
import org.starloco.locos.fight.Challenge
import org.starloco.locos.fight.Fight
import org.starloco.locos.game.GameClient
import org.starloco.locos.game.GameServer
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.game.scheduler.entity.WorldSave
import org.starloco.locos.game.world.World
import org.starloco.locos.job.JobAction
import org.starloco.locos.job.JobStat
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.kernel.Main
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.`object`.ObjectSet
import org.starloco.locos.`object`.ObjectTemplate
import org.starloco.locos.util.TimerWaiter

import java.util.*
import java.util.Map.Entry
import java.util.stream.Collectors
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(CommandAdmin::class.java)

class CommandAdmin(player: Player) : AdminUser(player) {

    override fun apply(packet: String) {
        var msg: String = packet.substring(2)
        var infos: List<String> = msg.split(" ")

        if (infos.size == 0) return
        var command: String = infos[0]

        try {
            var groupe: Group? = this.player!!.getGroup()
            if (groupe == null) {
                this.client!!.kick()
                return
            }
            if (!groupe.haveCommand(command)) {
                this.sendMessage("Commande invalide !")
                return
            }

            this.command(command, infos, msg)
        } catch (e: Exception) {
            this.player!!.sendMessage("Error: "+e.message)
            Main.logger.error("Command error: {}", command, e)
        }
    }

    fun command(command: String, infos: List<String>, msg: String) {
        var infos = infos
        if (command.equals("LOG", ignoreCase = true)) {
            Config.debug = !Config.debug
            this.sendMessage("Les logs console sont : " + (if (Config.debug) "active" else "disable"))
            return
        } else if (command.equals("CHALL", ignoreCase = true)) {
            var challenge: Challenge = Challenge(this.player!!.fight!!,infos[1].toInt(), 0, 0)
            this.player!!.fight!!.allChallenges.put(infos[1].toInt(), challenge)
            challenge.fightStart()
            SocketManager.GAME_SEND_CHALLENGE_FIGHT(this.player!!.fight!!, 1, challenge.parseToPacket())
            return
        } else if (command.equals("HELP", ignoreCase = true)) {
            var cmd: String = if (infos.size == 2) infos[1] else ""

            if (cmd.equals("", ignoreCase = true)) {
                this.sendMessage("\nVous avez actuellement le groupe GM " + this.player!!.getGroup()!!.name.toString() + ".\nCommandes disponibles :\n")
                for (commande in  this.player!!.getGroup().getCommands()) {
                    var args: String = if ((commande.args != null && !commande.args.equals("", ignoreCase = true))) (" + " + commande.args) else ("")
                    var desc: String = if ((commande.desc != null && !commande.desc.equals("", ignoreCase = true))) (commande.desc) else ("")
                    this.sendMessage("<u>" + commande.name + args.toString() + "</u> - " + desc)
                }
            } else {
                this.sendMessage("\nVous avez actuellement le groupe GM " + this.player!!.getGroup()!!.name.toString() + ".\nCommandes recherches :\n")
                for (commande in  this.player!!.getGroup().getCommands()) {
                    if (commande.name.contains(cmd.uppercase())) {
                        var args: String = if ((commande.args != null && !commande.args.equals("", ignoreCase = true))) (" + " + commande.args) else ("")
                        var desc: String = if ((commande.desc != null && !commande.desc.equals("", ignoreCase = true))) (commande.desc) else ("")
                        this.sendMessage("<u>" + commande.name + args.toString() + "</u> - " + desc)
                    }
                }
            }
            return
        }
        else if (command.equals("STARTBOUFBOWL", ignoreCase = true)) {
            if(this.player!!.curMap.id != 9862)
                return

var init1: Player? = null
            var init2: Player? = null
            try {
                init1 = World.world.getPlayerByName(infos[1])
                init2 = World.world.getPlayerByName(infos[2])
            } catch (e: Exception) {
                // ok
            }
            if (init1 == null || init2 == null) {
                sendMessage("Le nom du personnage n'est pas bon.")
                return
            }
            if(init1.curMap.id != 9862 || init2.curMap.id != 9862) {
                sendMessage("un perso n'est pas sur la map. (9862)")
                return
            }
            SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(this.player!!.curMap, init2.id, init1.id)
            var fight: Fight? = null
            fight = this.player!!.curMap.newFightbouf(init1, init2, Constant.FIGHT_TYPE_CHALLENGE)
            init1.fight = fight!!
            init2.fight = fight!!
            return
        } else if (command.equals("ONLINE", ignoreCase = true)) {
            var perso: Player? = this.player!!
            if (infos.size > 1) {//Si un nom de perso est specifie
                try {
                    perso = World.world.getPlayerByName(infos[1])
                } catch (e: Exception) {
                    // ok
                }
                if (perso == null) {
                    this.sendMessage("Le personnage n'a pas ete trouve")
                    return
                }
            }
            if (perso!!.getGameClient() != null)
                perso!!.getGameClient()!!.kick()
            perso!!.online = false
            perso!!.resetVars()
            ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(perso)
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso!!.curMap, perso!!.id)
            World.world.unloadPerso(perso)
            ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).load(perso!!.id)
            var str: String = "Le joueur " + perso!!.name.toString() + " a ete reinitialise de ces variables."
            this.sendMessage(str)
            return
        } else if (command.equals("ANAME", ignoreCase = true)) {
            infos = msg.split(" ", limit = 2)
            var prefix: String = "<b><a href='asfunction:onHref,ShowPlayerPopupMenu," + this.player!!.name.toString() + "'>[" + this.player!!.getGroup()!!.name.toString() + "] " + this.player!!.name.toString() + "</a></b>"
            if(infos.size > 1) {
                var suffix: String = infos[1]
                if (suffix.contains("<") && (!suffix.contains(">") || !suffix.contains("</"))) // S'il n'y a pas de balise fermante
                    suffix = suffix.replace("<", "").replace(">", "")
                if (suffix.contains("<") && suffix.contains(">") && !suffix.contains("</")) // S'il n'y a pas de balise fermante
                    suffix = suffix.replace("<", "").replace(">", "")
                SocketManager.GAME_SEND_Im_PACKET_TO_ALL("116;" + prefix.toString() + "~" + suffix)
            }
            return
        } else if (command.equals("GONAME", ignoreCase = true)
                || command.equals("JOIN", ignoreCase = true)
                || command.equals("GON", ignoreCase = true)) {
            var P: Player? = World.world.getPlayerByName(infos[1])
            if (P == null) {
                var str: String = "Le personnage de destination n'existe pas."
                this.sendMessage(str)
                return
            }
            var mapID: Int = P!!.curMap.id
            var cellID: Int = P!!.curCell.getId()

            var perso: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[2])
                if (perso == null) {
                    var str: String = "Le personnage e teleporter n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
                if (perso!!.fight != null) {
                    var str: String = "La cible e teleporter est en combat."
                    this.sendMessage(str)
                    return
                }
            }
            perso!!.teleport(mapID, cellID)
            var str: String = "Le joueur " + perso!!.name.toString() + " a ete teleporte vers " + P!!.name.toString() + "."
            this.sendMessage(str)
            return
        } else if (command.equals("KICKFIGHT", ignoreCase = true)) {
            var P: Player? = World.world.getPlayerByName(infos[1])
            if (P == null || P.fight == null) {
                this.sendMessage("Le personnage n'a pas ete trouve ou il n'est pas en combat.")
                return
            }
            SocketManager.GAME_SEND_GV_PACKET(P)
            if (P.fight != null) {
                P.fight!!.leftFight(P, null)
                P.fight = null
            }
            SocketManager.GAME_SEND_GV_PACKET(P)
            this.sendMessage("Le personnage " + P!!.name.toString() + " a ete expulse de son combat.")
            return
        } else if (command.equals("DEBUG", ignoreCase = true)) {
            var perso: Player? = this.player!!
            if (infos.size > 1)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[1])
                if (perso == null) {
                    var str: String = "Le personnage n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
                if (perso!!.fight != null) {
                    var str: String = "La cible est en combat."
                    this.sendMessage(str)
                    return
                }
            } else {
                return
            }
            perso!!.warpToSavePos()
            var str: String = "Le joueur " + perso!!.name.toString() + " a ete teleporte e son point de sauvegarde."
            this.sendMessage(str)
            return
        } else if (command.equals("JOBLEFT", ignoreCase = true)) {
            var perso: Player? = this.player!!
            try {
                perso = World.world.getPlayerByName(infos[1])
            } catch (e: Exception) {
                // ok
            }
            if (perso == null)
                perso = this.player!!
            perso!!.doAction = false
            perso!!.exchangeAction = null
            this.sendMessage("L'action de metier e ete annule.")
            return
        } else if (command.equals("WHO", ignoreCase = true)) {
            var mess: String = "\n<u>Liste des joueurs en ligne :</u>"
            this.sendMessage(mess)
            var i: Int = 0

            for (player in  World.world.onlinePlayers) {
                if (i == 30)
                    break
                if (player == null)
                    continue
                i++
                mess = player!!.name.toString() + " (" + player.id.toString() + ") "
                mess += returnClasse(player!!.classe)
                mess += " "
                mess += if (player!!.sexe.toInt() == 0) "M" else "F" + " "
                mess += player!!.level.toString() + " "
                mess += player!!.curMap.id.toString() + "(" + player!!.curMap.x.toString() + "/" + player!!.curMap.y.toString() + ") "
                mess += if (player!!.fight == null) "" else "Combat "
                mess += player!!.getAccount().currentIp

                this.sendMessage(mess)
            }

            if (Config.gameServer!!.getClients().size - 30 > 0) {
                mess = "Et " + (Config.gameServer!!.getClients().size - 30) + " autres personnages"
                this.sendMessage(mess)
            }
            mess = "\n"
            this.sendMessage(mess)
            return
        } else if (command.equals("WHOALL", ignoreCase = true)) {
            var mess: String = "\n<u>Liste des joueurs en ligne :</u>"
            this.sendMessage(mess)
            for (client in  Config.gameServer!!.getClients()) {
                var player: Player? = client.player

                if (player == null)
                    continue

                mess = player!!.name.toString() + " (" + player.id.toString() + ") "
                mess += returnClasse(player!!.classe)
                mess += " "
                mess += if (player!!.sexe.toInt() == 0) "M" else "F" + " "
                mess += player!!.level.toString() + " "
                mess += player!!.curMap.id.toString() + "(" + player!!.curMap.x.toString() + "/" + player!!.curMap.y.toString() + ") "
                mess += if (player!!.fight == null) "" else "Combat "
                mess += player!!.getAccount().currentIp

                this.sendMessage(mess)
            }
            mess = "\n"
            this.sendMessage(mess)
            return
        } else if (command.equals("WHOFIGHT", ignoreCase = true)) {
            var mess: String = ""
            this.sendMessage("\n<u>Liste des joueurs en ligne et en combat :</u>")
            for (client in  Config.gameServer!!.getClients()) {
                var player: Player? = client.player

                if (player == null)
                    continue

                if (player!!.fight == null)
                    continue

                mess = player!!.name.toString() + " (" + player.id.toString() + ") "
                mess += returnClasse(player!!.classe)
                mess += " "
                mess += if (player!!.sexe.toInt() == 0) "M" else "F" + " "
                mess += player!!.level.toString() + " "
                mess += player!!.curMap.id.toString() + "(" + player!!.curMap.x.toString() + "/" + player!!.curMap.y.toString() + ") "
                mess += if (player!!.fight == null) "" else "Combat "
                mess += player!!.getAccount().currentIp

                this.sendMessage(mess)
            }
            if (mess.equals("", ignoreCase = true)) {
                this.sendMessage("Aucun joueur en combat.")
            } else {
                mess = "\n"
                this.sendMessage(mess)
            }
            return
        } else if (command.equals("NAMEGO", ignoreCase = true)
                || command.equals("NGO", ignoreCase = true)) {
            var perso: Player? = World.world.getPlayerByName(infos[1])
            if (perso == null) {
                var str: String = "Le personnage e teleporter n'existe pas."
                this.sendMessage(str)
                return
            }
            if (perso!!.fight != null) {
                var str: String = "Le personnage e teleporter est en combat."
                this.sendMessage(str)
                return
            }
            var P: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                P = World.world.getPlayerByName(infos[2])
                if (P == null) {
                    var str: String = "Le personnage de destination n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
            }
            if (P!!.isOnline) {
                var mapID: Int = P!!.curMap.id
                var cellID: Int = P!!.curCell.getId()
                perso!!.teleport(mapID,cellID)
                var str: String = "Le joueur " + perso!!.name.toString() + " a ete teleporte vers " + P!!.name.toString() + "."
                this.sendMessage(str)
            } else {
                var str: String = "Le joueur " + P!!.name.toString() + " n'est pas en ligne."
                this.sendMessage(str)
            }
            return
        } else if (command.equals("TP", ignoreCase = true)) {
            var mapID: Int = -1
            var cellID: Int = -1
            try {
                mapID = infos[1].toInt()
                cellID = infos[2].toInt()
            } catch (e: Exception) {
                // ok
            }

            if (mapID == -1 || cellID == -1 || World.world.getMap(mapID) == null) {
                var str: String = ""
                if (mapID == -1 || World.world.getMap(mapID) == null)
                    str = "MapID invalide."
                else
                    str = "cellID invalide."
                this.sendMessage(str)
                return
            }
            if (World.world.getMap(mapID).getCase(cellID) == null) {
                var str: String = "cellID invalide."
                this.sendMessage(str)
                return
            }
            var perso: Player? = this.player!!
            if (infos.size > 3)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[3])
                if (perso == null || perso!!.fight != null) {
                    var str: String = "Le personnage n'a pas ete trouve ou est en combat"
                    this.sendMessage(str)
                    return
                }
                if(!perso.isOnline) {
                    perso!!.curMap = World.world.getMap(mapID)
                    perso!!.curCell = World.world.getMap(mapID).getCase(cellID)!!
                }
            }
            perso!!.teleport(mapID, cellID)
            var str: String = "Le joueur " + perso!!.name.toString() + " a ete teleporte."
            this.sendMessage(str)
            return
        } else if (command.equals("SIZE", ignoreCase = true)) {
            var size: Int = -1
            try {
                size = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }
            if (size == -1) {
                var str: String = "Taille invalide."
                this.sendMessage(str)
                return
            }
            var perso: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[2])
                if (perso == null) {
                    var str: String = "Le personnage n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
            }
            perso!!.size = size
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso!!.curMap, perso!!.id)
            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso!!.curMap, perso)
            var str: String = "La taille du joueur " + perso!!.name.toString() + " a ete modifiee."
            this.sendMessage(str)
            return
        } else if (command.equals("FREEZE", ignoreCase = true)) {
            var perso: Player? = this.player!!
            if (infos.size > 1) {
                perso = World.world.getPlayerByName(infos[1])
            }
            if (perso == null) {
                var mess: String = "Le personnage n'existe pas."
                this.sendMessage(mess)
                return
            }
            if (perso!!.blockMovement)
                this.sendMessage("Le joueur n'est plus bloque.")
            else
                this.sendMessage("Le joueur est bloque.")
            perso!!.blockMovement = !perso.blockMovement
            return
        } else if (command.equals("BLOCKMAP", ignoreCase = true)) {
            var i: Int = -1
            try {
                i = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }
            if (i == 0) {
                Main.mapAsBlocked = false
                this.sendMessage("Map deblocke.")
            } else if (i == 1) {
                Main.mapAsBlocked = true
                this.sendMessage("Map blocke.")
            } else {
                this.sendMessage("Aucune information.")
            }
            return
        } else if (command.equals("BLOCKFIGHT", ignoreCase = true)) {
            var i: Int = -1
            try {
                i = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }
            if (i == 0) {
                Main.fightAsBlocked = false
                for(player in  World.world.onlinePlayers)
                    player.sendServerMessage(player.getLang().trans("command.commandadmin.fightblock0"))
                this.sendMessage("Les combats ont etes debloques.")
            } else if (i == 1) {
                for(player in  World.world.onlinePlayers)
                    player.sendServerMessage(player.getLang().trans("command.commandadmin.fightblock1"))
                this.sendMessage("Les combats ont etes bloques.")
                Main.fightAsBlocked = true
            } else {
                this.sendMessage("Aucune information.")
            }
            return
        } else if (command.equals("MUTE", ignoreCase = true)) {
            var player: Player? = null
            lateinit var name: String
            var time: Short

            try {
                name = infos[1]

                if(name.equals("*")) {
                    CommandPlayer.canalMute = !CommandPlayer.canalMute
                    this.sendSuccessMessage("The main channel has been " + (if (CommandPlayer.canalMute) "closed." else "opened."))
                    return
                }

                time = infos[2].toShort()
            } catch (e: Exception) {
                this.sendErrorMessage("The name/time you've enter is/are invalid ! (max time : " + Short.MAX_VALUE.toString() + " in minutes)")
                return
            }

            player = World.world.getPlayerByName(name ?: "")

            if (player == null || time <= 0) {
                this.sendErrorMessage("The player wasn't found or the time is negative. (max time : " + Short.MAX_VALUE.toString() + " in minutes)")
                return
            }

            if (player!!.getAccount() == null) {
                this.sendErrorMessage("The account of the player wasn't found, please check the name.")
                return
            }

            player!!.getAccount().mute(time, this.player!!.name)
            this.sendSuccessMessage("You've mute the player " + player!!.name.toString() + " for " + time.toString() + "minute(s) effective for all players of this account !")

            if (!player.isOnline)
                this.sendErrorMessage("The player is not online, are you sure it is the correct player ?")
            return
        } else if (command.equals("MUTEIP", ignoreCase = true)) {
            var player: Player? = null
            lateinit var name: String
            var time: Short

            try {
                name = infos[1]
                time = infos[2].toShort()
            } catch (e: Exception) {
                this.sendErrorMessage("The name/time you've enter is/are invalid ! (max time : " + Short.MAX_VALUE.toString() + " in minutes)")
                return
            }

            player = World.world.getPlayerByName(name ?: "")

            if (player == null || time <= 0) {
                this.sendErrorMessage("The player wasn't found or the time is negative. (max time : " + Short.MAX_VALUE.toString() + " in minutes)")
                return
            }

            if (player!!.getAccount() == null) {
                this.sendErrorMessage("The account of the player wasn't found, please check the name.")
                return
            }

            var ip: String = player!!.getAccount().lastIP

            if (ip.equals("",ignoreCase = true)) {
                this.sendErrorMessage("Sorry but the server don't have any IP of this account, check another account please.")
                return
            }

            World.world.getAccountsByIp(ip).forEach({ account -> {
                account.mute(time, this.player!!.name)
                if (account.currentPlayer != null)
                    this.sendMessage("You've mute the account " + account.name.toString() + ".")
            } })

            this.sendSuccessMessage("All the accounts of the IP (" + ip.toString() + ") have been mute for " + time.toString() + " minute(s) successfully !")
            return
        } else if (command.equals("UNMUTEIP", ignoreCase = true)) {
            var player: Player? = null
            lateinit var name: String

            try {
                name = infos[1]
            } catch (e: Exception) {
                this.sendErrorMessage("The name/time you've enter is/are invalid ! (max time : " + Short.MAX_VALUE.toString() + " in minutes)")
                return
            }

            player = World.world.getPlayerByName(name ?: "")

            if (player == null) {
                this.sendErrorMessage("The player wasn't found or the time is negative. (max time : " + Short.MAX_VALUE.toString() + " in minutes)")
                return
            }

            if (player!!.getAccount() == null) {
                this.sendErrorMessage("The account of the player wasn't found, please check the name.")
                return
            }

            var ip: String = player!!.getAccount().lastIP

            if (ip.equals("",ignoreCase = true)) {
                this.sendErrorMessage("Sorry but the server don't have any IP of this account, check another account please.")
                return
            }

            World.world.getAccountsByIp(ip).forEach({ account -> {
                account.unMute()
                if (account.currentPlayer != null)
                    this.sendMessage("The account " + account.name.toString() + " is free to talk.")
            } })

            this.sendSuccessMessage("All the accounts of the IP (" + ip.toString() + ") are free to talk successfully !")
            return
        } else if (command.equals("UNMUTE", ignoreCase = true)) {
            var player: Player? = null
            lateinit var name: String

            try {
                name = infos[1]
            } catch (e: Exception) {
                this.sendErrorMessage("The name/time you've enter is/are invalid ! (max time : " + Short.MAX_VALUE.toString() + " in minutes)")
                return
            }

            player = World.world.getPlayerByName(name ?: "")

            if (player == null) {
                this.sendErrorMessage("The player wasn't found or the time is negative. (max time : " + Short.MAX_VALUE.toString() + " in minutes)")
                return
            }

            if (player!!.getAccount() == null) {
                this.sendErrorMessage("The account of the player wasn't found, please check the name.")
                return
            }

            player!!.getAccount().unMute()
            this.sendSuccessMessage("You've unmute the player " + player!!.name.toString() + " effective for all players of this account !")

            if (!player.isOnline)
                this.sendErrorMessage("The player is not online, are you sure it is the correct player ?")
            return
        } else if (command.equals("MUTEMAP", ignoreCase = true)) {
            if (this.player!!.curMap == null)
                return
            this.player!!.curMap.mute()
            var mess: String = ""
            if (this.player!!.curMap.isMute)
                mess = "Vous venez de muter la MAP."
            else
                mess = "Vous venez de demuter la MAP."
            this.sendMessage(mess)
            return
        } else if (command.equals("KICK", ignoreCase = true)) {
            /*
            1 : Tu es resté trop longtemps inactif.
            2 : Ton personnage a atteint le niveau maximum autorisé
            3 : Pour des raisons de maintenance, le serveur va être coupé d'ici quelques minutes.
            4 : Votre connexion a été coupée pour des raisons de maintenance.
            5 : Retry connection (Oui ou Non)
            6 : Le nombre d'objets pour cet inventaire est déjà atteint.
            7 : Cette opération n'est pas autorisée ici.
            8 : Cet objetn 'est plus disponible.
             */

            var player: Player? = null
            var name: String? = null
            var reason = ""

            try {
                name = infos[1]
            } catch (ignored: Exception) {
                this.sendErrorMessage("You need to give the name of the player !")
                return
            }

            try {
                reason = msg.substring(infos[0].length + infos[1].length + 1)
            } catch (ignored: Exception) {}

            player = World.world.getPlayerByName(name ?: "")

            if (player == null) {
                this.sendErrorMessage("The name of the player is invalid or non-existent !")
                return
            }

            if (player!!.isOnline) {
                if (reason.isEmpty()) {
                    player.send("M018|" + this.player!!.name.toString() + ";")
                } else {
                    player.send("M018|" + this.player!!.name.toString() + ";<br>" + reason)
                }
                player!!.getGameClient()!!.kick()
                this.sendSuccessMessage("The player have been kicked successfully.")
            } else {
                this.sendErrorMessage("The player isn't connected, check the name please.")
            }
            return
        } else if (command.equals("JAIL", ignoreCase = true)) {
            var mapID: Int = 666
            var cellID: Int = getCellJail()
            if (cellID == -1 || World.world.getMap(mapID) == null) {
                var str: String = "MapID ou cellID invalide."
                if (cellID == -1)
                    str = "cellID invalide."
                else
                    str = "MapID invalide."
                this.sendMessage(str)
                return
            }
            if (World.world.getMap(mapID).getCase(cellID) == null) {
                var str: String = "cellID invalide."
                this.sendMessage(str)
                return
            }
            try {
                if (infos.size > 1)//Si un nom de perso est specifie
                {
                    var perso: Player? = World.world.getPlayerByName(infos[1])
                    if (perso!!.getGroup() != null) {
                        var str: String = "Il est interdit d'emprisonner un personnage ayant des droits."
                        this.sendMessage(str)
                        return
                    }
                    if (perso == null || perso!!.fight != null) {
                        var str: String = "Le personnage n'a pas ete trouve ou est en combat."
                        this.sendMessage(str)
                        return
                    }
                    if (perso!!.isOnline)
                        perso!!.teleport(mapID, cellID)
                    else
                        perso!!.teleportD(mapID, cellID)
                    var str: String = "Le joueur " + perso!!.name.toString() + " a ete teleporte emprisonne."
                    this.sendMessage(str)
                }
            } catch (e: Exception) {
                this.sendMessage("Introuvable.")
                // ok
                return
            }
            return
        } else if (command.equals("UNJAIL", ignoreCase = true)) {
            var perso: Player? = World.world.getPlayerByName(infos[1])
            if (perso == null || perso!!.fight != null) {
                var str: String = "Le personnage n'a pas ete trouve ou est en combat."
                this.sendMessage(str)
                return
            }
            if (infos.size > 1 && perso!!.isInPrison())//Si un nom de perso est specifie
            {
                perso!!.warpToSavePos()
                var str: String = "Le joueur " + perso!!.name.toString() + " a ete teleporte e son point de sauvegarde."
                this.sendMessage(str)
                return
            }
            return
        } else if (command.equals("BAN", ignoreCase = true)) {
            var player: Player? = World.world.getPlayerByName(infos[1])
            var days: Short = 0

            try {
                days = infos[2].toShort()
            } catch (ignored: Exception) {
                this.sendMessage("You've not enter a day value (the time while the account is banned), the default value is unlimited.")
            }

            if (player == null) {
                this.sendErrorMessage("The player was not found, check the name please.")
                return
            }
            if (player!!.getAccount() == null)
                ((DatabaseManager.get(AccountData::class.java) as AccountData)).load(player.accID)
            if (player!!.getAccount() == null) {
                this.sendErrorMessage("The account of the player was not found, contact a supervisor.")
                return
            }

            player!!.getAccount().isBanned = true
            ((DatabaseManager.get(AccountData::class.java) as AccountData)).updateBannedTime(player!!.getAccount(), System.currentTimeMillis() + 86400000 * if (days.toInt() == 0) 999 else days.toInt())

            if (player!!.fight == null) {
                if (player!!.getGameClient() != null)
                    player!!.getGameClient()!!.kick()
            } else {
                SocketManager.send(player!!, "Im1201;" + this.player!!.name)
            }
            this.sendSuccessMessage("You've kick and ban the player " + player!!.name.toString() + "(Acc: " + player!!.getAccount().name.toString() + ") for " + if (days.toInt() == 0) "unlimited" else days.toString() + " day(s).")
            return
        } else if (command.equals("BANACCOUNT", ignoreCase = true)) {
            var mess: String = "Le compte est introuvable"
            var A: String = ""
            try {
                A = infos[1]
            } catch (e: Exception) {
                // ok
            }
            if (A.equals("", ignoreCase = true)) {
                this.sendMessage("Il faut le nom de compte.")
                return
            }
            for (account in  World.world.accounts) {
                if (account == null)
                    continue
                if (!account.name.equals(A, ignoreCase = true))
                    continue
                account.isBanned = true
                ((DatabaseManager.get(AccountData::class.java) as AccountData)).update(account)
                mess = "Vous avez banni le compte " + A
                var p: Player? = account.currentPlayer
                if (p != null) {
                    if (p.isOnline) {
                        mess += " dont le joueur est " + p.name
                        if (p.fight == null) {
                            if (p.getGameClient() != null)
                                p.getGameClient()!!.kick()
                        } else {
                            SocketManager.send(p, "Im1201;" + this.player!!.name)
                        }
                    }
                }
            }
            this.sendMessage(mess.toString() + ".")
            return
        } else if (command.equals("BANBYID", ignoreCase = true)) {
            var ID: Int = -1
            var mess: String = "Aucun personnage n'a ete trouve."
            try {
                ID = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            if (ID <= 0) {
                this.sendMessage("Une IP est necessaire.")
                return
            }
            for (player in  World.world.players) {
                if (player == null)
                    continue
                if (player.id == ID) {
                    if (player!!.getAccount() == null)
                        ((DatabaseManager.get(AccountData::class.java) as AccountData)).load(player.accID)
                    if (player!!.getAccount() == null) {
                        this.sendMessage("Le personnage n'a pas de compte.")
                        if (player!!.getGameClient() != null)
                            player!!.getGameClient()!!.kick()
                        return
                    }
                    player!!.getAccount().isBanned = true
                    ((DatabaseManager.get(AccountData::class.java) as AccountData)).update(player!!.getAccount())
                    if (player!!.fight == null) {
                        if (player!!.getGameClient() != null)
                            player!!.getGameClient()!!.kick()
                    } else {
                        SocketManager.send(player!!, "Im1201;" + this.player!!.name)
                    }
                    mess = "Vous avez banni " + player!!.name.toString() + "."
                }
            }
            this.sendMessage(mess)
            return
        } else if (command.equals("BANBYIP", ignoreCase = true)) {
            var IP: String = ""
            try {
                IP = infos[1]
            } catch (e: Exception) {
                // ok
            }

            if (IP.equals("", ignoreCase = true)) {
                this.sendMessage("Une IP est necessaire.")
                return
            }
            for (a in  World.world.getAccountsByIp(IP)) {
                if (a == null)
                    continue
                if (!a.lastIP.equals(IP, ignoreCase = true))
                    continue

                a.isBanned = true
                ((DatabaseManager.get(AccountData::class.java) as AccountData)).update(a)
                this.sendMessage("Le compte " + a.name.toString() + " a ete banni.")
                if (a.isOnline()) {
                    var gc: GameClient? = a.gameClient
                    if (gc == null)
                        continue
                    this.sendMessage("Le joueur " + gc.player.name.toString() + " a ete kick.")
                    gc.kick()
                }
            }
            Config.exchangeClient!!.send("SB" + IP)
            ((DatabaseManager.get(BanIpData::class.java) as BanIpData)).insert(IP)
            this.sendMessage("L'IP " + IP.toString() + " a ete banni.")
            return
        } else if (command.equals("BANIP", ignoreCase = true)) {
var P: Player? = null

            try {
                P = World.world.getPlayerByName(infos[1])
            } catch (e: Exception) {
                // ok
            }

            if (P == null) {
                this.sendMessage("Le personnage n'a pas ete trouve.")
                return
            }
            var IP: String = P!!.getAccount().lastIP
            if (IP.equals("",ignoreCase = true)) {
                this.sendMessage("L'IP est invalide.")
                return
            }
            ((DatabaseManager.get(BanIpData::class.java) as BanIpData)).delete(IP)
            ((DatabaseManager.get(BanIpData::class.java) as BanIpData)).insert(IP)
            for (a in  World.world.getAccountsByIp(IP)) {
                if (!a.lastIP.equals(IP, ignoreCase = true))
                    continue

                a.isBanned = true
                ((DatabaseManager.get(AccountData::class.java) as AccountData)).update(a)
                this.sendMessage("Le compte " + a.name.toString() + " a ete banni.")
                if (a.isOnline()) {
                    var gc: GameClient? = a.gameClient
                    if (gc == null)
                        continue
                    this.sendMessage("Le joueur " + gc.player.name.toString() + " a ete kick.")
                    gc.kick()
                }
            }
            Config.exchangeClient!!.send("SB" + IP)
            ((DatabaseManager.get(BanIpData::class.java) as BanIpData)).insert(IP)
            this.sendMessage("L'IP " + IP.toString() + " a ete banni.")
            return
        } else if (command.equals("SHOWITEM", ignoreCase = true)) {
            var perso: Player? = this.player!!
            var name: String? = null
            try {
                name = infos[1]
            } catch (e: Exception) {
                // ok
            }

            perso = World.world.getPlayerByName(name ?: "")
            if (perso == null) {
                var mess: String = "Le personnage n'a pas ete trouve."
                this.sendMessage(mess)
                return
            }
            var mess: String = "==========\n" + "Liste d'items sur le personnage :\n"
            this.sendMessage(mess)
            for (entry in  perso!!.items.entries) {
                mess = entry.value.guid.toString() + " || " + entry.value.template!!.name.toString() + " || " + entry.value.quantity
                this.sendMessage(mess)
            }

            this.sendMessage("Le personnage possede : " + perso!!.kamas.toString() + " Kamas.\n")
            mess = "=========="
            this.sendMessage(mess)
            return
        } else if (command.equals("SHOWBANK", ignoreCase = true)) {
            var perso: Player? = this.player!!
            var name: String? = null
            try {
                name = infos[1]
            } catch (e: Exception) {
                // ok
            }

            perso = World.world.getPlayerByName(name ?: "")
            if (perso == null) {
                var mess: String = "Le personnage n'a pas ete trouve."
                this.sendMessage(mess)
                return
            }
            var cBank: Account = perso!!.getAccount()
            var mess: String = "==========\n" + "Liste d'items dans la banque :"
            this.sendMessage(mess)
            for (entry in  cBank.bank) {
                mess = entry.guid.toString() + " || " + entry.template!!.name.toString() + " || " + entry.quantity
                this.sendMessage(mess)
            }
            this.sendMessage("Le personnage possede : " + cBank.getBankKamas().toString() + " Kamas en banque.")
            mess = "=========="
            this.sendMessage(mess)
            return
        } else if (command.equals("SHOWSTORE", ignoreCase = true)) {
            var perso: Player? = this.player!!
            var name: String? = null
            try {
                name = infos[1]
            } catch (e: Exception) {
                // ok
            }

            perso = World.world.getPlayerByName(name ?: "")
            if (perso == null) {
                var mess: String = "Le personnage n'a pas ete trouve."
                this.sendMessage(mess)
                return
            }
            var mess: String = "==========\n" + "Liste d'items dans le Store :"
            this.sendMessage(mess)
            for (obj in  perso!!.storeItems.entries) {
                var entry: GameObject? = World.world.getGameObject(obj.key)
                mess = entry!!.guid.toString() + " || " + entry.template!!.name.toString() + " || " + entry.quantity
                this.sendMessage(mess)
            }

            mess = "=========="
            this.sendMessage(mess)
            return
        } else if (command.equals("SHOWMOUNT", ignoreCase = true)) {
            var perso: Player? = this.player!!
            var name: String? = null
            try {
                name = infos[1]
            } catch (e: Exception) {
                // ok
            }

            perso = World.world.getPlayerByName(name ?: "")
            if (perso == null) {
                var mess: String = "Le personnage n'a pas ete trouve."
                this.sendMessage(mess)
                return
            }
            var mess: String = "==========\n" + "Liste d'items dans la banque :"
            this.sendMessage(mess)
            if(perso!!.mount != null) {
                for (entry in  perso!!.mount!!.objects.entries) {
                    mess = entry.value.guid.toString() + " || " + entry.value.template!!.name.toString() + " || " + entry.value.quantity
                    this.sendMessage(mess)
                }
            }
            mess = "=========="
            this.sendMessage(mess)
            return
        } else if (command.equals("BLOCKTRADE", ignoreCase = true)) {
            var i: Int = -1
            try {
                i = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            if (i == 0) {
                Main.tradeAsBlocked = false
                this.sendMessage("Les échanges ont été débloqués.")
            } else if (i == 1) {
                Main.tradeAsBlocked = true
                this.sendMessage("Tous les échanges sont bloqués.")
            } else {
                this.sendMessage("Aucune information.")
            }
            return
        } else if (command.equals("ERASEALLMAP", ignoreCase = true)) {
            for (map in  World.world.maps)
                map.delAllDropItem()
            this.sendMessage("Tous les objets sur toutes les maps ont été supprimés.")
            return
        } else if (command.equals("ERASEMAP", ignoreCase = true)) {
            this.player!!.curMap.delAllDropItem()
            this.sendMessage("Les objets de la map ont été supprimés.")
            return
        } else if (command.equals("MORPH", ignoreCase = true)) {
            var morphID: Int = -9
            try {
                morphID = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            if (morphID == -9) {
                var str: String = "MorphID invalide."
                this.sendMessage(str)
                return
            }
            var target: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                target = World.world.getPlayerByName(infos[2])
                if (target == null) {
                    var str: String = "Le personnage n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
            }
            if (morphID == -1) {
                morphID = target!!.classe * 10 + target!!.sexe.toInt()
                target!!.gfxId = morphID
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target!!.curMap, target!!.id)
                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target!!.curMap, target)
                var str: String = "Le joueur " + target!!.name.toString() + " a son apparence originale."
                this.sendMessage(str)
                return
            } else {
                target!!.gfxId = morphID
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(target!!.curMap, target!!.id)
                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(target!!.curMap, target)
                var str: String = "Le joueur " + target!!.name.toString() + " a ete transforme."
                this.sendMessage(str)
                return
            }
        } else if (command.equals("DEMORPHALL", ignoreCase = true)) {
            for (player in  World.world.onlinePlayers) {
                player!!.gfxId = (player!!.classe.toString() + player!!.sexe.toString()).toInt()
            }
            this.sendMessage("Tous les joueurs connectes ont leur apparence originale.")
            return
        } else if (command.equals("ADDHONOR", ignoreCase = true)) {
            var honor: Int = 0
            try {
                honor = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            var perso: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[2])
                if (perso == null) {
                    var str: String = "Le personnage n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
            }
            var str: String = "Vous avez ajoute " + honor.toString() + " points d'honneur e " + perso!!.name.toString() + "."
            if (perso!!.alignment != Constant.ALIGNEMENT_MERCENAIRE) {
                str = "Le joueur n'est pas mercenaire ... l'action a ete annulee."
                this.sendMessage(str)
                return
            }
            perso!!.addHonor(honor)
            this.sendMessage(str)
            return
        } else if (command.equals("HONOR", ignoreCase = true)) {
            var honor: Int = 0
            try {
                honor = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            var perso: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[2])
                if (perso == null) {
                    var str: String = "Le personnage n'a pas ete trouve"
                    this.sendMessage(str)
                    return
                }
            }
            var str: String = "Vous avez ajoute " + honor.toString() + " points d'honneur e " + perso!!.name.toString() + "."
            if (perso!!.alignment == Constant.ALIGNEMENT_NEUTRE) {
                str = "Le joueur est neutre ... l'action a ete annulee."
                this.sendMessage(str)
                return
            }
            perso!!.addHonor(honor)
            this.sendMessage(str)
            return
        } else if (command.equals("NOAGRO", ignoreCase = true)) {
            var perso: Player? = this.player!!
            var name: String? = null
            try {
                name = infos[1]
            } catch (e: Exception) {
                // ok
            }

            perso = World.world.getPlayerByName(name ?: "")
            if (perso == null) {
                var mess: String = "Le personnage n'existe pas."
                this.sendMessage(mess)
                return
            }
            perso!!.canAggro = !perso.canAggro()
            var mess: String = perso!!.name
            if (perso!!.canAggro())
                mess += " peut maintenant etre aggresse."
            else
                mess += " ne peut plus etre agresse."
            this.sendMessage(mess)
            if (!perso.isOnline) {
                mess = "Le personnage " + perso!!.name.toString() + " n'etait pas connecte."
                this.sendMessage(mess)
            }
            return
        } else if (command.equals("WHOIS", ignoreCase = true)) {
            var name: String = ""
            var perso: Player? = null
            try {
                name = infos[1]
            } catch (e: Exception) {
                // ok
            }

            if (name == "")
                return

            perso = World.world.getPlayerByName(name ?: "")
            if (perso == null) {
                var mess: String = "Le personnage n'existe pas."
                this.sendMessage(mess)
                return
            } else if (perso!!.getAccount().lastIP.equals("", ignoreCase = true)) {
                var mess: String = "Aucune IP."
                this.sendMessage(mess)
                return
            }
            var accounts: List<Account> = World.world.getAccountsByIp(perso!!.getAccount().lastIP)
            var mess: String = "Whois sur le joueur : " + name.toString() + "\n"
            mess += "Derniere IP : " + perso!!.getAccount().lastIP.toString() + "\n"
            var i: Int = 1
            for (a in  accounts) {
                var persos: String = ""
                for (entry2 in  a.getPlayers().entries) {
                    perso = entry2.value
                    if (perso != null) {
                        if (persos.equals("", ignoreCase = true))
                            persos += perso!!.name + (if ((perso!!.getGroup() != null)) ":" + perso!!.getGroup()!!.name else "")
                        else
                            persos += ", " + perso!!.name + (if ((perso!!.getGroup() != null)) ":" + perso!!.getGroup()!!.name else "")
                    }
                }
                if (!persos.equals("", ignoreCase = true)) {
                    mess += "[" + i.toString() + "] " + a.name.toString() + " - " + persos + (if ((a.isBanned)) " : banni" else "") + "\n"
                    i++
                }
            }
            this.sendMessage(mess)
            return
        } else if (command.equals("CLEANFIGHT", ignoreCase = true)) {
            this.player!!.curMap.fights.clear()
            this.sendMessage("Tous les combats de la map ont etes supprimes.")
            return
        } else if (command.equals("ETATSERVER", ignoreCase = true)) {
            var etat: Int = 1
            try {
                etat = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }
            GameServer.setState(etat)
            this.sendMessage("Vous avez change l'etat du serveur en " + etat.toString() + ".")
            return
        } else if (command.equals("MPTOTP", ignoreCase = true)) {
            this.player!!.mpToTp = !this.player!!.mpToTp
            var mess: String = ""
            if (this.player!!.mpToTp)
                mess = "Vous venez d'activer le MP to TP."
            else
                mess = "Vous venez de desactiver le MP to TP."
            this.sendMessage(mess)
            return
        } else if (command.equals("RETURNTP", ignoreCase = true)) {
            for (perso in  World.world.onlinePlayers) {
                if (perso!!.thatMap == -1 || perso!!.fight != null)
                    continue
                perso!!.teleport(perso!!.thatMap, perso!!.thatCell)
                perso!!.thatMap = -1
                perso!!.thatCell = -1
            }
            this.sendMessage("Vous venez de renvoyer tous les joueurs e leur ancienne position.")
            return
        } else if (command.equals("GETCASES", ignoreCase = true)) {
            if (this.player!!.getCases) {
                this.sendMessage("Le getCases viens d'etre disable :")
                var i: String = ""
                for (c in  this.player!!.thisCases)
                    i += ";" + c
                this.sendMessage(i.substring(1))
                this.player!!.thisCases.clear()
            } else
                this.sendMessage("Le getCases viens d'etre active. Deplacez-vous sur la map pour capturer les cellules.")
            this.player!!.getCases = !this.player!!.getCases
            return
        } else if (command.equals("WALKFAST", ignoreCase = true)) {
            if (this.player!!.walkFast)
                this.sendMessage("La marche instantanne viens d'etre disable.")
            else
                this.sendMessage("La marche instantanne viens d'etre active.")
            this.player!!.walkFast = !this.player!!.walkFast
            return
        } else if (command.equals("LISTMAP", ignoreCase = true)) {
            var data: String = ""
            var i: ArrayList<GameMap> = World.world.getMapByPosInArray(this.player!!.curMap.x,this.player!!.curMap.y)
            for (map in  i)
                data += map.id.toString() + " | "
            this.sendMessage(data)
            return
        } else if (command.equals("DELINVENTORY", ignoreCase = true)) {
            infos = msg.split(" ", limit = 3)
            var perso: Player? = World.world.getPlayerByName(infos[1])

            if (perso == null) {
                this.sendMessage("Le nom du personnage est incorrect.")
                return
            }

            var count: Long = 0
            for (obj in perso!!.items.values) {
                val guid = obj!!.guid
                SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(perso, guid)
                perso!!.deleteItem(guid)
                count++
            }

            this.sendMessage("Vous venez de supprimer " + count.toString() + " objets au joueur " + perso!!.name.toString() + ".")
            SocketManager.GAME_SEND_STATS_PACKET(perso)
        } else if (command.equals("RMOBS", ignoreCase = true)) {
            this.player!!.curMap.refreshSpawns()
            var mess: String = "Les spawns de monstres sur la map ont etes rafraichit."
            this.sendMessage(mess)
            return
        } else if (command.equals("DELJOB", ignoreCase = true)) {
            var perso: Player? = this.player!!
            infos = msg.split(" ", limit = 3)
            var job: Int = -1
            try {
                job = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            try {
                perso = World.world.getPlayerByName(infos[2])
            } catch (e: Exception) {
                // ok
            }

            if (perso == null) {
                this.sendMessage("Le nom du personnage est incorrect.")
                return
            }
            if (job < 1)
                return
            var jobStats: JobStat = perso!!.getMetierByID(job)!!
            if (jobStats == null)
                return
            perso!!.unlearnJob(jobStats.id)
            SocketManager.GAME_SEND_MESSAGE(perso, perso!!.getLang().trans("command.commandadmin.unlearn.spell"))
            this.sendMessage("Vous avez supprimé le métier " + job.toString() + " sur le personnage " + perso!!.name.toString() + ".")
            return
        } else if (command.equals("STRIGGER", ignoreCase = true)) {
            this.player!!.thatMap = this.player!!.curMap.id
            this.player!!.thatCell = this.player!!.curCell.getId()
            this.sendMessage("Vous avez sauvegarde la map " + this.player!!.thatMap.toString() + " et la cellule " + this.player!!.thatCell.toString() + ".")
            return
        } else if (command.equals("INFOS", ignoreCase = true)) {
            var uptime: Long = System.currentTimeMillis() - Config.startTime
            var day: Int = ((uptime / (1000 * 3600 * 24)) as Int)
            uptime %= (1000 * 3600 * 24)
            var hour: Int = ((uptime / (1000 * 3600)) as Int)
            uptime %= (1000 * 3600)
            var min: Int = ((uptime / (1000 * 60)) as Int)
            uptime %= (1000 * 60)
            var sec: Int = ((uptime / (1000)) as Int)

            var message: String = "\n<u><b>Global informations system of the emulator :</b></u>\n\n<u>Uptime :</u> " + day.toString() + "j " + hour.toString() + "h " + min.toString() + "m " + sec.toString() + "s.\n"
            message += "Online players         : " + Config.gameServer!!.getClients().size.toString() + "\n"
            message += "Unique online players  : " + Config.gameServer!!.getPlayersNumberByIp().toString() + "\n"
            message += "Online clients         : " + Config.gameServer!!.getClients().size.toString() + "\n"


            var mb: Int = 1024 * 1024
            var instance: Runtime = Runtime.getRuntime()

            message += "\n<u>Heap utilization statistics :</u>"
            message += "\nTotal Memory : " + instance.totalMemory() / mb + " Mo."
            message += "\nFree Memory  : " + instance.freeMemory() / mb + " Mo."
            message += "\nUsed Memory  : " + (instance.totalMemory() - instance.freeMemory()) / mb + " Mo."
            message += "\nMax Memory   : " + instance.maxMemory() / mb + " Mo."
            message += "\n\n<u>Available processor :</u> " + instance.availableProcessors()
            var list: Set<Thread> = Thread.getAllStackTraces().keys
            var news: Int = 0
            var running: Int = 0
            var blocked: Int = 0
            var waiting: Int = 0
            var sleeping: Int = 0
            var terminated: Int = 0
            for(thread in  list) {
                when (thread.state) {
Thread.State.NEW -> {news++ 
}
Thread.State.RUNNABLE -> {running++ 
}
Thread.State.BLOCKED -> {blocked++ 
}
Thread.State.WAITING -> {waiting++ 
}
Thread.State.TIMED_WAITING -> {sleeping++ 
}
Thread.State.TERMINATED -> {news++ 
}
}
            }

            message +="\n\n<u>Informations of " + list.size.toString() + " threads :</u> "
            message += "\nNEW           : " + news
            message += "\nRUNNABLE      : " + running
            message += "\nBLOCKED       : " + blocked
            message += "\nWAITING       : " + waiting
            message += "\nTIMED_WAITING : " + sleeping
            message += "\nTERMINATED    : " + terminated

            this.sendMessage(message.toString() + "\n")

            if(infos.size > 1) {
                message = "List of all threads :\n"
                for(thread in  list)
                    message += "- " + thread.id.toString() + " -> " + thread.getName().toString() + " -> " + thread.state.name.uppercase().toString() + "" + (if (thread.isDaemon()) " (Daemon)" else "") + ".\n"
                this.sendMessage(message)
            }
            return
        } else if (command.equals("STARTFIGHT", ignoreCase = true)) {
            if (this.player!!.fight == null) {
                this.sendMessage("Vous devez etre dans un combat.")
                return
            }
            this.player!!.fight!!.startFight()
            this.sendMessage("Le combat a ete demarre.")
            return
        } else if (command.equals("ENDFIGHTNULL", ignoreCase = true)) {
            var name: String = if (infos.size > 2) infos[2] else ""
            var target: Player? = if (!name.isEmpty()) World.world.getPlayerByName(name ?: "") else this.player!!

            //team 1 = red, team 0 = blue
            if(target != null && target.fight != null) {
                target.fight!!.endFight(0.toByte())
                this.sendSuccessMessage("You've ended the fight successfully.")
            } else {
                if(target == null)
                    this.sendErrorMessage("The player name you've indicate is invalid")
                else
                    this.sendErrorMessage("The player you've indicate is not in a fight (if he's bugued, try KICKFIGHT command).")
            }
            return
        } else if (command.equals("ENDFIGHT", ignoreCase = true)) {
            var i: Byte
            var name: String = if (infos.size > 2) infos[2] else ""
            var target: Player? = if (!name.isEmpty()) World.world.getPlayerByName(name ?: "") else this.player!!

            try {
                i = infos[1].toByte()
            } catch (e: Exception) {
                this.sendErrorMessage("Please, specify a type (0: no-winners/loosers, 1: red team win, 2: blue team win).")
                return
            }

            //team 1 = red, team 0 = blue
            if(target != null && target.fight != null) {
                target.fight!!.endFight(i)
                this.sendSuccessMessage("You've ended the fight successfully.")
            } else {
                if(target == null)
                    this.sendErrorMessage("The player name you've indicate is invalid")
                else
                    this.sendErrorMessage("The player you've indicate is not in a fight (if he's bugued, try KICKFIGHT command).")
            }
            return
        } else if (command.equals("ENDFIGHTALL", ignoreCase = true)) {
            try {
                for (client in  Config.gameServer!!.getClients()) {
                    var player: Player? = client.player
                    if (player == null)
                        continue
                    var f: Fight = player!!.fight!!
                    if (f == null)
                        continue
                    try {
                        if (f.launchTime > 1)
                            continue
                        f.endFight(1.toByte())
                        this.sendMessage("Le combat de " + player!!.name.toString() + " a ete termine.")
                    } catch (e: Exception) {
                        // ok
                        this.sendMessage("Le combat de " + player!!.name.toString() + " a deje ete termine.")
                    }
                }
            } catch (e: Exception) {
                log.error("unexpected error", e)
                this.sendMessage("Erreur lors de la commande endfightall : " + e.message.toString() + ".")
            } finally {
                this.sendMessage("Tous les combats ont ete termines.")
            }
            return
        } else if (command.equals("MAPINFO", ignoreCase = true)) {
            var mess: String = "==========\n" + "Liste des PNJs de la Map :"
            this.sendMessage(mess)
            var map: GameMap = this.player!!.curMap
            for (entry in  map.npcs.entries) {
                mess = entry.key.toString() + " | " + entry.value.template.id.toString() + " | " + entry.value.cellId
                val leg = entry.value.template.legacy
                if(leg != null) {
                        mess += " | " + leg.getInitQuestionId(this.player!!.curMap.id)
                }
                this.sendMessage(mess)
            }
            mess = "Liste des groupes de monstres :"
            this.sendMessage(mess)
            for (entry in  map.mobGroups.entries) {
                mess = entry.key.toString() + " | " + entry.value.cellId.toString() + " | " + entry.value.alignment.toString() + " | " + entry.value.mobs.size
                this.sendMessage(mess)
            }
            mess = "=========="
            this.sendMessage(mess)
            return
        } else if (command.equals("UNBANIP", ignoreCase = true)) {
            var perso: Player? = null
            try {
                perso = World.world.getPlayerByName(infos[1])
            } catch (e: Exception) {
                // ok
            }
            if (perso == null) {
                this.sendMessage("Le nom du personnage n'est pas bon.")
                return
            }
            ((DatabaseManager.get(BanIpData::class.java) as BanIpData)).delete(perso!!.getAccount().currentIp)
            this.sendMessage("L'IP a ete debanni.")
            return
        } else if (command.equals("UNBAN", ignoreCase = true)) {
            var P: Player? = World.world.getPlayerByName(infos[1])
            if (P == null) {
                this.sendMessage("Personnage non trouve.")
                return
            }
            if (P!!.getAccount() == null)
                ((DatabaseManager.get(AccountData::class.java) as AccountData)).load(P.accID)
            if (P!!.getAccount() == null) {
                this.sendMessage("Le personnage n'a pas de compte.")
                return
            }
            P!!.getAccount().isBanned = false
            ((DatabaseManager.get(AccountData::class.java) as AccountData)).update(P!!.getAccount())
            this.sendMessage("Vous avez debanni " + P!!.name.toString() + ".")
            return
        } else if (command.equals("EXIT", ignoreCase = true)) {
            this.sendMessage("Lancement du reboot.")
            Main.runnables.add({  -> Main.stop("Exit by administrator") })
            return
        } else  if (command.equals("SETMAX", ignoreCase = true)) {
            var i: Short = infos[1].toShort()
            this.sendMessage("Le maximum de joueur a été fixer à : " + i)
            GameServer.MAX_PLAYERS = i
            return
        } else if (command.equals("SAVE", ignoreCase = true) && !Config.isSaving) {
            TimerWaiter.addNext({  -> WorldSave.cast(1) }, 1000)
            var mess: String = "Sauvegarde lancee!"
            this.sendMessage(mess)
            return
        } else if (command.equals("LEVEL", ignoreCase = true)) {
            var count: Int
            try {
                count = infos[1].toInt()
                if (count < 1) count = 1
                var xpTable: ExperienceTables.ExperienceTable = World.world.experiences!!.players
                if (count > xpTable.maxLevel()) count = xpTable.maxLevel()

                var player: Player? = this.player!!

                if (infos.size == 3) {
                    player = World.world.getPlayerByName(infos[2])
                    if (player == null) player = this.player!!
                }

                if (player!!.level < count) {
                    while (player!!.level < count)
                        player!!.levelUp(false, true)
                    SocketManager.GAME_SEND_NEW_LVL_PACKET(player!!.getGameClient()!!, player!!.level)
                }

                this.sendSuccessMessage("You've fix the level of '" + player!!.name.toString() + "' to " + count.toString() + ".")
                SocketManager.GAME_SEND_STATS_PACKET(player!!)
            } catch (e: Exception) {
                this.sendMessage("You've send an invalid level.")
            }
            return
        } else if (command.equals("KAMAS", ignoreCase = true)) {
            var count: Int = 0
            try {
                count = infos[1].toInt()
            } catch (e: Exception) {
                // ok
                this.sendMessage("Valeur incorecte.")
                return
            }
            if (count == 0) {
                this.sendMessage("Valeur inutile.")
                return
            }
            var perso: Player? = this.player!!
            if (infos.size == 3)//Si le nom du perso est specifie
            {
                var name: String = infos[2]
                perso = World.world.getPlayerByName(name ?: "")
                if (perso == null)
                    perso = this.player!!
            }
            var curKamas: Long = perso!!.kamas
            var newKamas: Long = curKamas + count
            if (newKamas < 0)
                newKamas = 0
            if (newKamas > 1000000000)
                newKamas = 1000000000
            perso!!.kamas = newKamas
            if (perso!!.isOnline)
                SocketManager.GAME_SEND_STATS_PACKET(perso)
            var mess: String = "Vous avez "
            mess += (if (count < 0) "retire" else "ajoute") + " "
            mess += Math.abs(count).toString() + " kamas e " + perso!!.name.toString() + "."
            this.sendMessage(mess)
            return
        } else if (command.equals("ITEMSET", ignoreCase = true)) {
            var tID: Int = 0
            try {
                tID = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            var IS: ObjectSet? = World.world.getItemSet(tID)
            if (tID == 0 || IS == null) {
                var mess: String = "La panoplie " + tID.toString() + " n'existe pas."
                this.sendMessage(mess)
                return
            }
            var useMax: Boolean = false
            if (infos.size == 3)
                useMax = infos[2].equals("MAX");//Si un jet est specifie

            for (t in  IS.itemTemplates) {
                var obj: GameObject? = t.createNewItem(1,useMax)
                if (this.player!!.addItem(obj!!, true, false))//Si le joueur n'avait pas d'item similaire
                    World.world.addGameObject(obj)
            }
            var str: String = "Creation de la panoplie " + tID.toString() + " reussie"
            if (useMax)
                str += " avec des stats maximums"
            str += "."
            this.sendMessage(str)
            return
        } else if (command.equals("ITEM", ignoreCase = true) || command.equals("!getitem", ignoreCase = true) || command.equals("getitem", ignoreCase = true)) {
            var tID: Int = 0
            try {
                tID = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            if (tID == 0) {
                var mess: String = "Le template " + tID.toString() + " n'existe pas."
                this.sendMessage(mess)
                return
            }
            var qua: Int = 1
            if (infos.size == 3)//Si une quantite est specifiee
            {
                try {
                    qua = infos[2].toInt()
                } catch (e: Exception) {
                    // ok
                }
            }
            var lier: Boolean = infos.size == 5
            var useMax: Boolean = false
            if (infos.size == 4)//Si un jet est specifie
            {
                if (infos[3].equals("MAX", ignoreCase = true))
                    useMax = true
            }
            var t: ObjectTemplate? = World.world.getObjTemplate(tID)
            if (t == null) {
                var mess: String = "Le template " + tID.toString() + " n'existe pas."
                this.sendMessage(mess)
                return
            }
            if (t.type == Constant.ITEM_TYPE_OBJET_ELEVAGE
                    && (t.strTemplate.isEmpty() || t.strTemplate.equals("", ignoreCase = true))) {
                this.sendMessage("Impossible de creer l'item d'elevage. Le StrTemplate (" + tID.toString() + ") est vide.")
                return
            }
            if (qua < 1)
                qua = 1
            var obj: GameObject? = t.createNewItem(qua,useMax)


            if(t.type == Constant.ITEM_TYPE_CERTIF_MONTURE) {
                //obj.setMountStats(this.player!!, null);
                var mount: Mount = Mount(Constant.getMountColorByParchoTemplate(obj!!.template!!.id),this.player!!.id, false)
                obj!!.clearStats()
                obj!!.stats.addOneStat(995, (mount.id))
                obj!!.txtStat.put(996, this.player!!.name)
                obj!!.txtStat.put(997, mount.name ?: "")
                mount.setToMax()
            }
            if(lier) {
                var player: Player? = World.world.getPlayerByName(infos[4])
                obj!!.attachToPlayer(player!!)
                if (player!!.addItem(obj, true, false))//Si le joueur n'avait pas d'item similaire
                    World.world.addGameObject(obj)
            } else {
                if (this.player!!.addItem(obj!!, true, false))//Si le joueur n'avait pas d'item similaire
                    World.world.addGameObject(obj)
            }
            var str: String = "Creation de l'item " + tID.toString() + " reussie"
            if (useMax)
                str += " avec des stats maximums"
            str += "."
            this.sendMessage(str)
            SocketManager.GAME_SEND_Ow_PACKET(this.player!!)
            return
        } else if (command.equals("SPELLPOINT", ignoreCase = true)) {
            var pts: Int = -1
            try {
                pts = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            if (pts == -1) {
                var str: String = "Valeur invalide."
                this.sendMessage(str)
                return
            }
            var perso: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[2])
                if (perso == null) {
                    var str: String = "Le personnage n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
            }
            perso!!.addSpellPoint(pts)
            SocketManager.GAME_SEND_STATS_PACKET(perso)
            var str: String = "Vous avez ajoute " + pts.toString() + " points de sorts e " + perso!!.name.toString() + "."
            this.sendMessage(str)
            return
        } else if(command.equals("RELOAD", ignoreCase = true)) {
            when (if (infos.size > 0) infos[1].uppercase() else "") {
"HOUSES" -> {(World.world.houses as MutableMap).clear()
                    DatabaseManager.get(BaseHouseData::class.java).loadFully()
                    DatabaseManager.get(HouseData::class.java).loadFully()
                    
}
"DROPS" -> {DatabaseManager.get(DropData::class.java).loadFully()
                    
}
"ITEMS" -> {DatabaseManager.get(ObjectTemplateData::class.java).loadFully()
                    DatabaseManager.get(ObjectActionData::class.java).loadFully()
                    
}
"SCRIPTS" -> {World.world.scheduler.execute({  -> {
                        DataScriptVM.getInstance()!!.safeLoadData()
                        this.sendSuccessMessage("You've successfully reload the data type 'scripts' !")
                    } })
                    return
}
"ADMIN" -> {Config.gameServer!!.getClients().stream().filter({ client -> client != null && client.player != null }).forEach({ client -> ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).reloadGroup(client.player) })
                    
}
"MONSTERS" -> {DatabaseManager.get(MonsterData::class.java).loadFully()
                    
}
"SPELLS" -> {DatabaseManager.get(SpellData::class.java).loadFully()
                    
}
else -> {this.sendErrorMessage("Please enter a valid reload data :\nhouses, endfightactions, quests, drops, items, npcs, admin, monsters, spells, battle-type, battle-area")
                    return
            
}
}
            this.sendSuccessMessage("You've successfully reload the data type '" + infos[1].toString() + "' !")
        } else if(command.equals("UTILITY", ignoreCase = true)) {
            when (infos[1].uppercase()) {
"CHECK-PA" -> {var pa: Int = infos[2].toInt()
                    var players: StringBuilder = StringBuilder()
                    for(player in  World.world.players) {
                        if(player!!.getStuffStats().getEffect(Constant.STATS_ADD_PA) > pa || player!!.getStuffStats().getEffect(Constant.STATS_ADD_PA2) > pa)
                            players.append(player!!.name).append("(").append(player!!.getStuffStats().getEffect(Constant.STATS_ADD_PA)).append("-").append(player!!.getStuffStats().getEffect(Constant.STATS_ADD_PA2)).append("), ")
                    }
                    this.sendSuccessMessage(players.toString())
                    
}
"SPEC" -> {var player1: Player? = World.world.getPlayerByName(infos[2])
                    if(player1 != null) {
                        SocketManager.GAME_SEND_ASK(this.client!!, player1)
                    } else {
                        this.sendErrorMessage("Invalid player name")
                    }
                    
}
"UP-STARS" -> {for (map in World.world.maps) {
                            if (map == null) continue
                            map.mobGroups.values.filterNotNull().forEach { it.setStarBonus(150.toShort()) }
                            map.fixMobGroups.values.filterNotNull().forEach { it.setStarBonus(150.toShort()) }
                        }
                    }
                    
"COMPENSATIONS" -> {var ok: Boolean = infos[2].equals("true",ignoreCase = true)
                    var level: Int = infos[3].toInt()
                    var ips: MutableList<String> = ArrayList()
                    var affected: MutableList<Account> = ArrayList()

                    for(account in  World.world.accounts) {
                        if(account == null || account.lastConnectionDate == null || account.lastConnectionDate.isEmpty() || account.getPlayers().isEmpty()) continue

                        var date: List<String> = account.lastConnectionDate.split("~")//2018~05~31~18~16
                        var okk: Boolean = false
                        for(player in  account.getPlayers().values)
                            if(player!!.level > level)
                                okk = true

                        if(okk && (date[1].equals("05") || date[1].equals("06")) && date[2].toInt() >= 1 && !account.lastIP.isEmpty() && !ips.contains(account.lastIP)) {
                            if(ok)
                                account.addGift(26012, 10.toShort(), 1.toByte())
                            affected.add(account)
                            ips.add(account.lastIP)
                        }
                    }
                    this.sendMessage(affected.size.toString() + " accounts affected")
                    var accounts: StringBuilder = StringBuilder()
                    for(account in  affected)
                        accounts.append(account.name).append(", ")
                    this.sendMessage(accounts.toString())
                    
}
"EVENT-POS" -> {var eee: Event? = EventManager.instance.getCurrentEvent()
                    if(eee is EventFindMe) {
                        var ee: EventFindMe = (eee as EventFindMe)
                        this.sendSuccessMessage(ee.getMap()!!.id.toString() + "," + ee.getCell()!!.getId())
                    }
                    
}
"EVENT" -> {var name: String = infos[2]
                    var event: Event? = null
                    for(e in  EventManager.instance.events)
                        if(e!!.getEventName().equals(name, ignoreCase = true))
                            event = e
                    if(event != null) {
                        EventManager.instance.startNewEvent(event)
                        this.sendSuccessMessage("The event has started successfully.")
                    } else this.sendErrorMessage("The event with the name '" + name.toString() + "' was not found.")
                    
}
"ENCRYPTION" -> {Config.encryption = !Config.encryption
                    this.sendSuccessMessage("The encryption is : " + Config.encryption)
                    
}
"MVM" -> {var group1: String = infos[2]
            var group2: String = infos[3]
                    var cell: GameCase = this.player!!.curCell
                    var map: GameMap = this.player!!.curMap
                    var mg1: MonsterGroup = map.spawnGroupOnCommand(cell.getId() - map.w,group1, true)
                    var mg2: MonsterGroup = map.spawnGroupOnCommand(cell.getId() - map.w + 1,group2, true)
                    var hashMap: MutableMap<Int,MonsterGrade> = HashMap()
                    var guid: Int = -20
                    for(f in  ArrayList(mg2.mobs.values)) {
                        hashMap[guid] = f
                        guid--
                    }
                    mg2.mobs = hashMap
                    map.startFightMonsterVersusMonster(mg1, mg2)

                    
}
"FM" -> {var coef: Float = infos[2].toFloat()
                    JobAction.coefExo = coef
                    this.sendSuccessMessage("Exotic coefficient set to " + coef)
                    
}
"LISTMAP" -> {var map: GameMap = this.player!!.curMap
                    val list: StringBuilder = StringBuilder()
                    when (infos[2].uppercase()) {
"AREA" -> {this.sendSuccessMessage("List maps of area " + map.area!!.id.toString() + " (total: " + map.area!!.getMaps().size.toString() + ") :")
                            if(map.area != null) {
                                for(tmp in  map.area!!.getMaps())
                                    list.append(tmp.id).append(",")
                                this.sendMessage(list.toString())
                            }
                            
}
"SUBAREA" -> {this.sendSuccessMessage("List maps of sub-area " + map.subArea!!.id.toString() + " (total: " + map.subArea!!.getMaps().size.toString() + ") :")
                            if(map.subArea != null) {
                                for(tmp in  map.subArea!!.getMaps())
                                    list.append(tmp.id).append(",")
                                this.sendMessage(list.toString())
                            }
                            
}
else -> {this.sendErrorMessage("Invalid command, please give arguments [area|subarea].")
                            
}
}
                    
}
"COLLECTOR" -> {when (infos[2].uppercase()) {
"GET" -> {var collector: Collector? = World.world.getCollector(infos[3].toInt())
                            if (collector == null || collector.inFight > 0 || collector.exchange || collector.map != this.player!!.curMap.id)
                                return
                            collector.exchange = true
                            SocketManager.GAME_SEND_ECK_PACKET(this.client!!, 8, collector.id.toString() + "")
                            SocketManager.GAME_SEND_ITEM_LIST_PACKET_PERCEPTEUR(this.client!!, collector)
                            this.player!!.exchangeAction = ExchangeAction(ExchangeAction.TRADING_WITH_COLLECTOR, collector.id)
                            this.player!!.DialogTimer()
                            
}
else -> {
                            val message = StringBuilder("All id of collectors present on the map " + this.player!!.curMap.id.toString() + " :<br>")
                            World.world.collectors.values.filter { it.map == this.player!!.curMap.id }.forEach { collector1 ->
                                message.append("> ").append(collector1.id).append(" | ").append(collector1.date).append(" | ")
                                        .append(if (World.world.getGuild(collector1.guildId) != null) World.world.getGuild(collector1.guildId)!!.name else "Unknow").append("<br>")
                            }
                            this.sendMessage(message.toString())
                            
}
}
                    
}
"RECEIVE" -> {try {
                        this.player!!.getGameClient()!!.parsePacket(infos[2])
                    } catch (e: InterruptedException) {
                        log.error("unexpected error", e)
                        this.sendErrorMessage("You've fail the structure of the command. Please retry.")
                    }
                    this.sendSuccessMessage("You send to server this packet : " + infos[2])
                    
}
"DEBUG" -> {var position: Int = infos[2].toInt()
                    var `object`: GameObject? = this.player!!.getObjetByPos(position)
                    this.sendSuccessMessage("The id of position " + position.toString() + " is " + `object`!!.guid)
                    
}
}
        } else if (command.equals("LSPELL", ignoreCase = true)) {
            var spell: Int = -1
            try {
                spell = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            if (spell == -1) {
                var str: String = "Valeur invalide."
                this.sendMessage(str)
                return
            }
            var perso: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[2])
                if (perso == null) {
                    var str: String = "Le personnage n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
            }
            perso!!.learnSpell(spell, 1, true, true, true)
            var str: String = "Le sort " + spell.toString() + " a ete appris e " + perso!!.name.toString() + "."
            this.sendMessage(str)
            return
        } else if (command.equals("CAPITAL", ignoreCase = true)) {
            var pts: Int = -1
            try {
                pts = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            if (pts == -1) {
                var str: String = "Valeur invalide."
                this.sendMessage(str)
                return
            }
            var perso: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[2])
                if (perso == null) {
                    var str: String = "Le personnage n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
            }
            perso!!.addCapital(pts)
            SocketManager.GAME_SEND_STATS_PACKET(perso)
            var str: String = "Vous avez ajoute " + pts.toString() + " points de capital e " + perso!!.name.toString() + "."
            this.sendMessage(str)
            return
        } else if (command.equals("ALIGN", ignoreCase = true)) {
            var align: Byte = -1
            try {
                align = infos[1].toByte()
            } catch (e: Exception) {
                // ok
            }

            if (align.toInt() < Constant.ALIGNEMENT_NEUTRE
                    || align.toInt() > Constant.ALIGNEMENT_MERCENAIRE) {
                var str: String = "Valeur invalide."
                this.sendMessage(str)
                return
            }
            var perso: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[2])
                if (perso == null) {
                    var str: String = "Le personnage n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
            }
            perso!!.modifAlignement(align.toInt())
            var a: String = ""
            if (align.toInt() == 0)
                a = "neutre"
            else if (align.toInt() == 1)
                a = "bontarien"
            else if (align.toInt() == 2)
                a = "brakmarien"
            else if (align.toInt() == 3)
                a = "serianne"
            var str: String = "L'alignement du joueur a ete modifie en " + a.toString() + "."
            this.sendMessage(str)
            return
        } else if (command.equals("LIFE", ignoreCase = true)) {
            var count: Int = 0
            try {
                count = infos[1].toInt()
                if (count < 0)
                    count = 0
                if (count > 100)
                    count = 100
                var perso: Player? = this.player!!
                if (infos.size == 3)//Si le nom du perso est specifie
                {
                    var name: String = infos[2]
                    perso = World.world.getPlayerByName(name ?: "")
                    if (perso == null)
                        perso = this.player!!
                }
                var newPDV: Int = perso!!.maxPdv * count / 100
                perso!!.setPdv(newPDV)
                if (perso!!.isOnline)
                    SocketManager.GAME_SEND_STATS_PACKET(perso)
                var mess: String = "Vous avez fixe le pourcentage de vitalite de " + perso!!.name.toString() + " e " + count.toString() + "%."
                this.sendMessage(mess)
            } catch (e: Exception) {
                // ok
                this.sendMessage("Valeur incorecte.")
                return
            }
            return
        } else if (command.equals("XPJOB", ignoreCase = true)) {
            var job: Int = -1
            var xp: Long = -1
            try {
                job = infos[1].toInt()
                xp = infos[2].toLong()
            } catch (e: Exception) {
                // ok
            }

            if (job == -1 || xp < 0) {
                var str: String = "Valeurs invalides."
                this.sendMessage(str)
                return
            }
            var perso: Player? = this.player!!
            if (infos.size > 3)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[3])
                if (perso == null) {
                    var str: String = "Le personnage n'a pas ete trouv.e"
                    this.sendMessage(str)
                    return
                }
            }
            var SM: JobStat = perso!!.getMetierByID(job)!!
            if (SM == null) {
                var str: String = "Le joueur ne possede pas le metier demande."
                this.sendMessage(str)
                return
            }
            SM.addXp(perso!!, xp)
            var SMs: ArrayList<JobStat> = ArrayList<JobStat>()
            SMs.add(SM)
            SocketManager.GAME_SEND_JX_PACKET(perso, SMs)
            var str: String = "Vous avez ajoute " + xp.toString() + " points d'experience au metier " + job.toString() + " de " + perso!!.name.toString() + "."
            this.sendMessage(str)
            return
        } else if (command.equals("LJOB", ignoreCase = true)) {
            var job: Int = -1
            try {
                job = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            if (job == -1 || World.world.getMetier(job) == null) {
                var str: String = "Valeur invalide."
                this.sendMessage(str)
                return
            }
            var perso: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[2])
                if (perso == null) {
                    var str: String = "Le personnage n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
            }
            perso!!.learnJob(World.world.getMetier(job)!!)
            var str: String = "Le metier " + job.toString() + " a ete appris e " + perso!!.name.toString() + "."
            this.sendMessage(str)
            return
        } else if (command.equals("UNLSPELL", ignoreCase = true)) {
            var perso: Player? = this.player!!
            if (infos.size > 2)//Si un nom de perso est specifie
            {
                perso = World.world.getPlayerByName(infos[2])
                if (perso == null) {
                    var str: String = "Le personnage n'a pas ete trouve."
                    this.sendMessage(str)
                    return
                }
            }
            perso!!.exchangeAction = ExchangeAction(ExchangeAction.FORGETTING_SPELL, 0)
            SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', perso)
            return
        } else if (command.equals("SPAWN", ignoreCase = true)) {
            var Mob: String? = null
            try {
                Mob = infos[1]
            } catch (e: Exception) {
                // ok
            }

            if (Mob == null) {
                this.sendMessage("Les parametres sont invalides.")
                return
            }
            this.player!!.curMap.spawnGroupOnCommand(this.player!!.curCell.getId(), Mob, true)
            this.sendMessage("Vous avez ajoute un groupe de monstres.")
            return
        } else if (command.equals("SHUTDOWN", ignoreCase = true)) {
            var time: Int = 30
            var OffOn: Int = 0
            try {
                OffOn = infos[1].toInt()
                time = infos[2].toInt()
            } catch (e: Exception) {
                // ok
            }

            if (OffOn == 1 && this.isTimerStart)// demande de demarer le reboot
            {
                this.sendMessage("Un reboot est déjà programmé.")
            } else if (OffOn == 1 && !this.isTimerStart) {
                if (time <= 15) {
                    for(player in  World.world.onlinePlayers) {
                        player.sendServerMessage(player.getLang().trans("command.commandadmin.fightblock1"))
                        player.send("M13")
                    }
                    Main.fightAsBlocked = true
                }
                this.timer = createTimer(time)
                this.timer!!.start()
                this.isTimerStart = true
                var timeMSG: String = "minutes"
                if (time <= 1)
                    timeMSG = "minute"
                SocketManager.GAME_SEND_Im_PACKET_TO_ALL("115;" + time.toString() + " " + timeMSG)
                this.sendMessage("Reboot programmé.")
            } else if (OffOn == 0 && this.isTimerStart) {
                this.timer!!.stop()
                this.isTimerStart = false
                for(player in  World.world.onlinePlayers)
                    player.sendServerMessage(player.getLang().trans("command.commandadmin.fightblock0"))
                Main.fightAsBlocked = true
                this.sendMessage("Reboot arrêté.")
            } else if (OffOn == 0 && !this.isTimerStart) {
                this.sendMessage("Aucun reboot n'est lancé.")
            }
            return
        } else if (command.equals("LINEM", ignoreCase = true)) {
            var line: String = "|"
            for(split in  infos[1].split(",")) {
                var id: Int = split.toInt()
                var monster: Monster? = World.world.getMonstre(id)


                for (monsterGrade in  monster!!.grades.values)
                    line += monster!!.id.toString() + "," + monsterGrade.level.toString() + "|"
            }
            this.sendMessage(line)
            return
        } else if (command.equals("ENERGIE", ignoreCase = true)) {
            try {
                var perso: Player? = this.player!!
                var name: String? = null
                name = infos[2]
                perso = World.world.getPlayerByName(name ?: "")
                var jet: Int = infos[1].toInt()
                var EnergyTotal: Int = perso!!.energy + jet
                if (EnergyTotal > 10000)
                    EnergyTotal = 10000
                perso!!.energy = EnergyTotal
                SocketManager.GAME_SEND_STATS_PACKET(perso)
                this.sendMessage("Vous avez fixe l'energie de " + perso!!.name.toString() + " e " + EnergyTotal.toString() + ".")
                return
            } catch (e: Exception) {

            }
            return
        } else if (command.equals("RES", ignoreCase = true)) {
            var player: Player? = this.player!!

            if(infos.size > 1) {
                player = World.world.getPlayerByName(infos[1])
            }
            if (player == null) {
                this.sendErrorMessage("The player you've specified was not found.")
                return
            }
            if (player!!.fight != null) {
                this.sendErrorMessage("The player's currently in fight.")
                return
            }

            if (player!!.isOnline) {
                if(player!!.dead.toInt() == 1) {
                    player!!.isGhost = false
                    player!!.dead = 0.toByte()
                    player!!.energy = 1000
                    player!!.gfxId = (player!!.classe.toString() + player!!.sexe.toString()).toInt()
                    player!!.canAggro = true
                    player!!.away = false
                    player.speed = 0
                    SocketManager.GAME_SEND_ALTER_GM_PACKET(player!!.curMap, player)
                } else if(player.isGhost) {
                    player.setAlive()
                    player.setPdv(player!!.maxPdv)
                }
                this.sendMessage("The player '" + player!!.name.toString() + "' has been revived successfully.")
            } else {
                this.sendMessage("The player's not connected.")
            }
            return
        } else if (command.equals("KICKALL", ignoreCase = true)) {
            this.sendMessage("Tout le monde va etre kicke.")
            Config.gameServer!!.kickAll(true)
            return
        } else if (command.equals("RESET", ignoreCase = true)) {
            var perso: Player? = this.player!!
            if (infos.size > 1) {
                perso = World.world.getPlayerByName(infos[1])
            }
            if (perso == null) {
                var mess: String = "Le personnage n'existe pas."
                this.sendMessage(mess)
                return
            }
            perso!!.resetStats(true)
            this.sendMessage("Vous avez restat " + perso!!.name.toString() + ".")
            return
        }else if (command.equals("RESETALL", ignoreCase = true)) {
            for (perso in  World.world.players) {
                perso!!.resetStats(true)
            }
            this.sendMessage("Vous avez restat tout le monde")
            return
        } else if (command.equals("RENAMEPERSO", ignoreCase = true)) {
            var perso: Player? = this.player!!
            if (infos.size > 1)
                perso = World.world.getPlayerByName(infos[1])
            if (perso == null) {
                var mess: String = "Le personnage n'existe pas."
                this.sendMessage(mess)
                return
            }
            if(World.world.getPlayerByName(infos[2]) != null) {
                var mess: String = "Le personnage " + infos[2].toString() + " existe déjà."
                this.sendMessage(mess)
                return
            }
            var name: String = perso!!.name
            perso!!.name = infos[2]
            ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(perso)
            SocketManager.GAME_SEND_STATS_PACKET(perso)
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso!!.curMap, perso!!.id)
            SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso!!.curMap, perso)
            this.sendMessage("Vous avez renomme " + name.toString() + " en " + perso!!.name.toString() + ".")
            return
        } else if (command.equals("RENAMEGUILDE", ignoreCase = true)) {
            var ancName: String = ""
            var newName: String = ""
            var idGuild: Int = -1
            if (infos.size > 1)
                ancName = infos[1]
            newName = infos[2]
            idGuild = World.world.getGuildByName(ancName)
            if (idGuild == -1) {
                var mess: String = "La guilde n'existe pas."
                this.sendMessage(mess)
                return
            }
            World.world.getGuild(idGuild)!!.name = newName
            this.sendMessage("Vous avez renomme la guilde en " + newName.toString() + ".")
            return
        } else if (command.equals("A", ignoreCase = true)) {
            infos = msg.split(" ", limit = 2)
            var prefix: String = "<b>Server</b>"
            SocketManager.GAME_SEND_Im_PACKET_TO_ALL("116;" + prefix.toString() + "~" + infos[1])
            this.sendMessage("Vous avez envoye un message e tout le serveur.")
            return
        } else if (command.equals("MOVEMOB", ignoreCase = true)) {
            this.player!!.curMap.onMapMonsterDeplacement()
            NpcMovable.moveAll()
            this.sendMessage("Vous avez deplace un groupe de monstres.")
            return
        } else if (command.equals("ALLGIFTS", ignoreCase = true)) {
            var template: Int = -1
            var quantity: Int = 0
            var jp: Int = 0

            try {
                template = infos[1].toInt()
                quantity = infos[2].toInt()
                jp = infos[3].toInt()
            } catch (e: Exception) {
                // ok
                this.sendMessage("Parametre incorrect : ALLGIFTS [templateid] [quantity] [jp= 1 ou 0]")
                return
            }

            var gift: String = template.toString() + "," + quantity.toString() + "," + jp

            for (account in  World.world.accounts) {
                var gifts: String? = ((DatabaseManager.get(GiftData::class.java) as GiftData)).load(account.id)?.getSecond()
                if (gifts.isNullOrEmpty()) {
                    ((DatabaseManager.get(GiftData::class.java) as GiftData)).update(Pair<Account?, String?>(account, gift))
                } else {
                    ((DatabaseManager.get(GiftData::class.java) as GiftData)).update(Pair<Account?, String?>(account, gifts + ";" + gift))
                }
            }
            this.sendMessage(World.world.accounts.size.toString() + " ont reeu le cadeau : " + gift.toString() + ".")
            return
        } else if (command.equals("GIFTS", ignoreCase = true)) {
            var name: String = ""
            var template: Int = -1
            var quantity: Int = 0
            var jp: Int = 0

            try {
                name = infos[1]
                template = infos[2].toInt()
                quantity = infos[3].toInt()
                jp = infos[4].toInt()
            } catch (e: Exception) {
                // ok
                this.sendMessage("Parametre incorrect : GIFTS [account] [templateid] [quantity] [jp= 1 ou 0]")
                return
            }

            var player: Player? = World.world.getPlayerByName(name ?: "")

            if (player == null) {
                this.sendMessage("Personnage inexistant.")
                return
            }

            player!!.getAccount().addGift(template, quantity.toShort(), jp.toByte())
            this.sendMessage(name.toString() + " a reeu le cadeau : " + template.toString() + ".")
            return
        } else if (command.equals("SHOWPOINTS", ignoreCase = true)) {
            var perso: Player? = this.player!!
            if (infos.size > 1)
                perso = World.world.getPlayerByName(infos[1])
            if (perso == null) {
                var mess: String = "Le personnage n'existe pas."
                this.sendMessage(mess)
                return
            }
            this.sendMessage(perso!!.name.toString() + " possede " + perso!!.getAccount().points.toString() + " points boutique.")
            return
        } else if (command.equals("ADDNPC", ignoreCase = true)) {
            var id: Int = 0
            try {
                id = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            if (id == 0 || World.world.getNPCTemplate(id) == null) {
                var str: String = "NpcID invalide."
                this.sendMessage(str)
                return
            }
            var npc: Npc? = this.player!!.curMap.addNpc(id,this.player!!.curCell.getId(), this.player!!.orientation)
            SocketManager.GAME_SEND_ADD_NPC_TO_MAP(this.player!!.curMap, npc!!)
            var str: String = "Le PNJ a ete ajoute"
            if (this.player!!.orientation == 0
                    || this.player!!.orientation == 2
                    || this.player!!.orientation == 4
                    || this.player!!.orientation == 6)
                str += " mais est invisible (orientation diagonale invalide)"
            str += "."
            if (((DatabaseManager.get(NpcData::class.java) as NpcData)).insert(Pair(npc!!, (this.player!!.curMap.id as Int))))
                this.sendMessage(str)
            else
                this.sendMessage("Erreur lors de la sauvegarde de la position.")
            return
        } else if (command.equals("DELNPC", ignoreCase = true)) {
            var id: Int = 0
            try {
                id = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            var npc: Npc? = this.player!!.curMap.getNpc(id)
            if (id == 0 || npc == null) {
                var str: String = "Npc GUID invalide."
                this.sendMessage(str)
                return
            }
            var exC: Int = npc.cellId
            //on l'efface de la map
            SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(this.player!!.curMap,id)
            this.player!!.curMap.removeNpcOrMobGroup(id)

            var str: String = "Le PNJ a ete supprime."
            ((DatabaseManager.get(NpcData::class.java) as NpcData)).delete(Pair(npc!!, (this.player!!.curMap.id as Int)))
            this.sendMessage(str)
            return
        } else if (command.equals("SETSTATS", ignoreCase = true)) {
            var obj: Int = -1
            var stats: String = ""
            try {
                obj = infos[1].toInt()
                stats = infos[2]
            } catch (e: Exception) {
                // ok
            }
            if (obj == -1 || stats.equals("")) {
                this.sendMessage("Les parametres sont invalides.")
                return
            }
            var `object`: GameObject? = World.world.getGameObject(obj)
            if (`object` == null) {
                this.sendMessage("L'objet n'existe pas.")
                return
            }
            if (stats.equals("-1")) {
                `object`.clearStats()
                SocketManager.GAME_SEND_UPDATE_ITEM(this.player!!, `object`)
            } else {
                `object`.refreshStatsObjet(stats)
                SocketManager.GAME_SEND_UPDATE_ITEM(this.player!!, `object`)
            }
            this.sendMessage("L'objet a ete modifie avec succes.")
            return
        } else if (command.equals("ADDCELLPARK", ignoreCase = true)) {
            if (this.player!!.curMap.mountPark!! == null) {
                this.sendMessage("Pas d'enclos sur votre map.")
                return
            }
            this.player!!.curMap.mountPark!!.addCellObject(this.player!!.curCell.getId())
            ((DatabaseManager.get(BaseMountParkData::class.java) as BaseMountParkData)).update(this.player!!.curMap.mountPark!!)
            this.sendMessage("Vous avez ajoute la cellule e l'enclos.")
            return
        } else if (command.equals("CONVERT", ignoreCase = true)) {
            try {
                this.sendMessage(java.lang.Long.toHexString(infos[1].toLong()))
                this.sendMessage(infos[1].toLongOrNull()?.toString(16) ?: "")
            } catch (e: Exception) {
                this.sendMessage(infos[1].toLongOrNull()?.toString(16) ?: "")
            }
            return
        } else if (command.equals("LISTTYPE", ignoreCase = true)) {
            var s: String = ""
            for (obj in  World.world.objTemplates)
                if (obj.type == infos[1].toInt())
                    s += obj.id.toString() + ","
            this.sendMessage(s)
            return
        } else if (command.equals("EMOTE", ignoreCase = true)) {
            var perso: Player? = this.player!!
            var emoteId: Int = 0
            try {
                emoteId = infos[1].toInt()
                perso = World.world.getPlayerByName(infos[2])
            } catch (e: Exception) {
                // ok
            }
            if (perso == null)
                perso = this.player!!
            this.player!!.addStaticEmote(emoteId)
            this.sendMessage("L'emote " + emoteId.toString() + " a ete ajoute au joueur " + perso!!.name.toString() + ".")
            return
        } else if (command.equals("LISTEXTRA", ignoreCase = true)) {
            var mess: String = "Liste des Extra Monstres :"
            for (i in  World.world.extraMonsterOnMap.entries)
                mess += "\n- " + i.key.toString() + " est sur la map : " + i.value.id
            if (World.world.extraMonsterOnMap.size <= 0)
                mess = "Aucun Extra Monstres existe."
            this.sendMessage(mess)
            return
        } else if (command.equals("CREATEGUILD", ignoreCase = true)) {
            var perso: Player? = this.player!!
            if (infos.size > 1) {
                perso = World.world.getPlayerByName(infos[1])
            }
            if (perso == null) {
                var mess: String = "Le personnage n'existe pas."
                this.sendMessage(mess)
                return
            }

            if (!perso.isOnline) {
                var mess: String = "Le personnage " + perso!!.name.toString() + " n'est pas connecte."
                this.sendMessage(mess)
                return
            }
            if (perso!!.getGuild() != null || perso!!.guildMember != null) {
                var mess: String = "Le personnage " + perso!!.name.toString() + " possede deje une guilde."
                this.sendMessage(mess)
                return
            }
            SocketManager.GAME_SEND_gn_PACKET(perso)
            var mess: String = perso!!.name.toString() + ": Panneau de creation de guilde ouvert."
            this.sendMessage(mess)
            return
        } else if (command.equals("SEND", ignoreCase = true)) {
            SocketManager.send(this.client!!, msg.substring(5))
            this.sendMessage("Le paquet a ete envoye : " + msg.substring(5))
            return
        } else if (command.equals("SENDTOMAP", ignoreCase = true)) {
            SocketManager.sendPacketToMap(this.player!!.curMap, infos[1])
            this.sendMessage("Le paquet a ete envoye : " + msg.substring(10))
            return
        } else if (command.equals("SENDTO", ignoreCase = true)) {
            var perso: Player? = null
            try {
                perso = World.world.getPlayerByName(infos[1])
            } catch (e: Exception) {
                // ok
            }
            if (perso == null) {
                this.sendMessage("Le nom du personnage est incorrect.")
                return
            }
            SocketManager.send(World.world.getPlayerByName(infos[1])!!, msg.substring(8 + infos[1].length))
            this.sendMessage("Le paquet a ete envoye : " + msg.substring(8 + infos[1].length).toString() + " e " + infos[1].toString() + ".")
            return
        } else if (command.equals("TITRE", ignoreCase = true)) {
            var perso: Player? = this.player!!
            var TitleID: Byte = 0
            try {
                TitleID = infos[1].toByte()
                perso = World.world.getPlayerByName(infos[2])
            } catch (e: Exception) {
                // ok
            }

            if (perso == null) {
                perso = this.player!!
            }

            perso!!.setCurrentTitle(TitleID.toInt())
            this.sendMessage("Vous avez modifie le titre de " + perso!!.name.toString() + ".")
            ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(perso)
            if (perso!!.fight == null)
                SocketManager.GAME_SEND_ALTER_GM_PACKET(perso!!.curMap, perso)
            return
        } else if (command.equals("POINTS", ignoreCase = true)) {
            var count: Int = 0
            try {
                count = infos[1].toInt()
            } catch (e: Exception) {
                // ok
                this.sendMessage("Valeur incorrecte.")
                return
            }
            if (count == 0) {
                this.sendMessage("Valeur inutile.")
                return
            }
            var perso: Player? = this.player!!
            if (infos.size == 3)//Si le nom du perso est specifie
            {
                var name: String = infos[2]
                perso = World.world.getPlayerByName(name ?: "")
                if (perso == null)
                    perso = this.player!!
            }

            perso!!.getAccount().modPoints(count.toLong())
            if (perso!!.isOnline)
                SocketManager.GAME_SEND_STATS_PACKET(perso)
            var mess: String = "Vous venez de donner " + count.toString() + " points boutique e " + perso!!.name.toString() + "."
            this.sendMessage(mess)
            return
        } else if (command.equals("ITEMTYPE", ignoreCase = true)) {
            var type: String = ""
            try {
                type = infos[1]
            } catch (e: Exception) {
                // ok
            }
            var data: String = ""
            var count: Int = 0
            for (obj in  World.world.objTemplates) {
                if (type.contains(obj.type.toString() + ",") && obj.level >= 2 && obj.level <= 50 && obj.isAnEquipment(false, listOf(Constant.ITEM_TYPE_DOFUS, Constant.ITEM_TYPE_FAMILIER))) {
                    /*GameObject addObj = obj.createNewItem(1, true);
                    if (this.player!!.addObjet(addObj, true))//Si le joueur n'avait pas d'item similaire
                        World.world.addGameObject(addObj);*/
                    data += "," + obj.id
                    count++
                }
            }
            this.sendSuccessMessage(data)
            this.sendMessage("Vous avez tous les objets de type " + type.toString() + " dans votre inventaire." + count)
            return
        } else if (command.equals("FULLMORPH", ignoreCase = true)) {
            this.player!!.setFullMorph(infos[1].toInt(), false, false)
            this.sendMessage("Vous avez ete transforme en crocoburio.")
            return
        } else if (command.equals("UNFULLMORPH", ignoreCase = true)) {
            var pseudo: String = ""
            try {
                pseudo = infos[1]
            } catch (e: Exception) {
                // ok
            }
            var p: Player? = World.world.getPlayerByName(pseudo)
            if (p == null)
                p = this.player!!
            p.unsetFullMorph()
            this.sendMessage("Vous avez transforme dans la forme originale " + p.name.toString() + ".")
            return
        } else if (command.equals("PETSRES", ignoreCase = true)) {
            var objID: Int = 1
            try {
                objID = infos[1].toInt()
            } catch (e: Exception) {
                // ok
            }

            var p: PetEntry? = World.world.getPetsEntry(objID)
            if (p == null) {
                this.sendMessage("Le familier n'existe pas.")
                return
            }
            p!!.resurrection()
            this.sendMessage("Vous avez ressuscite le familier.")
            SocketManager.GAME_SEND_UPDATE_OBJECT_DISPLAY_PACKET(this.player!!, World.world.getGameObject(objID)!!)
            return
        } else if (command.equals("SETGROUPE", ignoreCase = true)) {
            var id: Int
            try {
                id = infos[1].toInt()
            } catch (e: Exception) {
                this.sendErrorMessage("The group you've specified is invalid (it's a number).")
                return
            }

            var group: Group? = Group.byId(id)

            if(id == -1) {
                if (infos.size > 2) {
                    var player: Player? = World.world.getPlayerByName(infos[2])
                    if (player != null) {
                        player.setGroupe(-1, true)
                        player.send("BAIC")
                        ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).updateGroupe(id, infos[2])
                        this.sendSuccessMessage("The player " + infos[2].toString() + " has been remove to his group admin successfully.")
                    }
                } else {
                    this.sendErrorMessage("No player specified, can't change anything.")
                }
            } else
            if(group == null) {
                this.sendErrorMessage("The group you've specified is invalid :")
                for(gp in  Group.getGroups()) {
                    this.sendMessage("-> " + gp.id.toString() + " - " + gp.name)
                }
            } else {
                if (infos.size > 2) {
                    var player: Player? = World.world.getPlayerByName(infos[2])
                    if (player != null) {
                        player.setGroupe(group!!.id, true)
                        player.send("BAIO")
                        ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).updateGroupe(id, infos[2])
                        this.sendSuccessMessage("The player " + infos[2].toString() + " has been assigned to group " + group!!.name.toString() + " successfully.")
                    }
                } else {
                    this.sendErrorMessage("No player specified, can't change anything.")
                }
            }
            return
        } else  if (command.equals("SETFREEPLACE", ignoreCase = true)) {
            //GameServer.freePlace = infos[1].toInt();
            this.sendMessage("")
            return
        } else if (command.equals("SHOWRIGHTGROUPE", ignoreCase = true)) {
            var groupe: Int = -1
            var cmd: String = ""
            try {
                groupe = infos[1].toInt()
                cmd = infos[2]
            } catch (e: Exception) {
                // ok
            }

            var g: Group? = null
            if (groupe > 0)
                g = Group.byId(groupe)

            if (g == null) {
                var str: String = "Le groupe est invalide."
                this.sendMessage(str)
                return
            }

            var c: List<Command> = g.getCommands()

            if (cmd.equals("",ignoreCase = true)) {
                this.sendMessage("\nCommandes disponibles pour le groupe " + g.name.toString() + " :\n")
                for (co in  c) {
                    var args: String = if ((co.args != null && !co.args.equals("", ignoreCase = true))) (" + " + co.args) else ("")
                    var desc: String = if ((co.desc != null && !co.desc.equals("", ignoreCase = true))) (co.desc) else ("")
                    this.sendMessage("<u>" + co.name + args.toString() + "</u> - " + desc)
                }
            } else {
                this.sendMessage("\nCommandes recherches pour le groupe " + g.name.toString() + " :\n")
                for (co in  c) {
                    if (co.name.contains(cmd.uppercase())) {
                        var args: String = if ((co.args != null && !co.args.equals("", ignoreCase = true))) (" + " + co.args) else ("")
                        var desc: String = if ((co.desc != null && !co.desc.equals("", ignoreCase = true))) (co.desc) else ("")
                        this.sendMessage("<u>" + co.name + args.toString() + "</u> - " + desc)
                    }
                }
            }
            return
        } else if (command.equals("INV", ignoreCase = true)) {
            var size: Int = this.player!!.size
            var perso: Player? = this.player!!
            if (size == 0) {
                if (perso!!.gfxId == 8008)
                    perso!!.size = 150
                else
                    perso!!.size = 100
                perso!!.isInvisible = false
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso!!.curMap, perso!!.id)
                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso!!.curMap, perso)
                this.sendMessage("Vous etes visible.")
            } else {
                perso!!.isInvisible = true
                perso!!.size = 0
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso!!.curMap, perso!!.id)
                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(perso!!.curMap, perso)
                this.sendMessage("Vous etes invisible.")
            }
            return
        } else if (command.equals("INCARNAM", ignoreCase = true)) {
            var perso: Player? = this.player!!
            perso!!.teleport(10292,284)
            this.sendMessage("Vous avez ete teleporte a Incarnam.")
            return
        } else if (command.equals("ASTRUB", ignoreCase = true)) {
            var perso: Player? = this.player!!
            perso!!.teleport(7411,311)
            this.sendMessage("Vous avez ete teleporte a Astrub.")
            return
        } else if (command.equals("DELQUEST", ignoreCase = true)) {
            var id: Int = -1
            var perso: String = ""
            try {
                id = infos[1].toInt()
                perso = infos[2]
            } catch (e: Exception) {
                // ok
            }

            if (id == -1 || perso!!.equals("", ignoreCase = true)) {
                this.sendMessage("Un des parametres est invalide.")
                return
            }
            var p: Player? = World.world.getPlayerByName(perso)
            if (p == null) {
                this.sendMessage("Le joueur est introuvable.")
                return
            }

            var qp: QuestProgress = p.getQuestProgress(id)!!
            if (qp == null) {
                this.sendMessage("Le personnage n'a pas la quete.")
                return
            }
            p.delQuestProgress(qp)
            this.sendMessage("La quete a ete supprime sur le personnage " + perso.toString() + ".")
            return
        } /* else if (command.equals("ADDQUEST", ignoreCase = true)) {
            int id = -1;
            String perso = "";
            try {
                id = infos[1].toInt();
                perso = infos[2];
            } catch (Exception e) {
                // ok
            }

            if (id == -1 || perso!!.equals("", ignoreCase = true)) {
                this.sendMessage("Un des parametres est invalide.");
                return;
            }
            Player p = World.world.getPlayerByName(perso);
            Quest q = Quest.quests.get(id);
            if (p == null || q == null) {
                this.sendMessage("La quete ou le joueur est introuvable.");
                return;
            }
            QuestPlayer qp = p.getQuestProgress(q);
            if (qp != null) {
                this.sendMessage("Le personnage a deje la quete.");
                return;
            }
            q.apply(p);
            qp = p.getQuestProgress(q);
            if (qp == null) {
                this.sendMessage("Une erreur est survenue.");
                return;
            }
            this.sendMessage("La quete a ete ajoute sur le personnage " + perso.toString() + ".");
            return;
        } else if (command.equals("FINISHQUEST", ignoreCase = true)) {
            int id = -1;
            String perso = "";
            try {
                id = infos[1].toInt();
                perso = infos[2];
            } catch (Exception e) {
                // ok
            }

            if (id == -1 || perso!!.equals("", ignoreCase = true)) {
                this.sendMessage("Un des parametres est invalide.");
                return;
            }
            Player p = World.world.getPlayerByName(perso);
            Quest q = Quest.quests.get(id);
            if (p == null || q == null) {
                this.sendMessage("La quete ou le joueur est introuvable.");
                return;
            }
            QuestPlayer qp = p.getQuestProgress(q);
            if (qp == null) {
                this.sendMessage("Le personnage n'a pas la quete.");
                return;
            }
            for (QuestObjective e : q.getObjectives()) {
                q.update(p, true, e.getValidationType());
            }
            ((PlayerData) DatabaseManager.get(PlayerData::class.java)).update(p);
            this.sendMessage("La quete a ete termine sur le personnage " + perso.toString() + ".");
            return;
        } else if (command.equals("SKIPQUEST", ignoreCase = true)) {
            int id = -1;
            String perso = "";
            try {
                id = infos[1].toInt();
                perso = infos[2];
            } catch (Exception e) {
                // ok
            }

            if (id == -1 || perso!!.equals("", ignoreCase = true)) {
                this.sendMessage("Un des parametres est invalide.");
                return;
            }
            Player p = World.world.getPlayerByName(perso);
            Quest q = Quest.quests.get(id);
            if (p == null || q == null) {
                this.sendMessage("La quete ou le joueur est introuvable.");
                return;
            }
            QuestPlayer qp = p.getQuestProgress(q);
            if (qp == null) {
                this.sendMessage("Le personnage n'a pas la quete.");
                return;
            }
            for (QuestObjective e : q.getObjectives()) {
                if (qp.isQuestObjectiveIsValidate(e))
                    continue;

                q.update(p, true, e.getValidationType());
                break;
            }
            ((PlayerData) DatabaseManager.get(PlayerData::class.java)).update(p);
            this.sendMessage("La quete est passe e l'etape suivante sur le personnage " + perso.toString() + ".");
            return;
        } else if (command.equals("ITEMQUEST", ignoreCase = true)) {
            int id = -1;
            try {
                id = infos[1].toInt();
            } catch (Exception e) {
                // ok
            }

            if (id == -1) {
                this.sendMessage("Le parametre est invalide.");
                return;
            }
            Quest q = Quest.quests.get(id);
            if (q == null) {
                this.sendMessage("La quete est introuvable.");
                return;
            }

            for (QuestObjective e : q.getObjectives()) {
                for (Entry<Integer, Integer> entry : e.getItemsNeeded().entries) {
                    ObjectTemplate objT = World.world.getObjTemplate(entry.key);
                    int qua = entry.value;
                    GameObject obj = objT.createNewItem(qua, false);
                    if (this.player!!.addItem(obj, true, false))
                        World.world.addGameObject(obj);
                    SocketManager.GAME_SEND_Im_PACKET(this.player!!, "021;" + qua.toString() + "~" + objT.getId());
                    if (objT.getType() == 32) // Si le drop est une mascotte, on l'ajoute ! :)
                    {
                        this.player!!.setMascotte(entry.key);
                    }
                }
            }
            this.sendMessage("Vous avez reeu tous les items necessaire e la quete.");
            return;
        } */ else if (command.equals("SHOWFIGHTPOS", ignoreCase = true)) {
            var mess: String = "Liste des StartCell [teamID][cellID]:"
            this.sendMessage(mess)
            var places: List<List<Int>> = this.player!!.curMap.places
            if (places.isEmpty()) {
                mess = "Les places n'ont pas ete definies"
                this.sendMessage(mess)
                return
            }

            for(i in 0 until places.size) {
                mess = "Team " + i.toString() + " : " + places.get(i).joinToString(",")
                this.sendMessage(mess)
            }
        } else if (command.equals("FINDEXTRAMONSTER", ignoreCase = true)) {
            var extras: Map<Int,Map<String,Map<String,Int>>> = World.world.extraMonsters

            for (entry in  extras.entries) {
                var idMob: Int = entry.key
                for (map in  World.world.maps)
                    map.mobPossibles.stream().filter({ mob -> mob.template.id == idMob }).forEach({ mob -> this.sendMessage("Map avec extraMonster : " + map.id.toString() + " -> " + idMob.toString() + ".") })
            }
            this.sendMessage("Recherche termine et affiche en console.")
        } else if (command.equals("GETAREA", ignoreCase = true)) {
            var subArea: Int = -1
            var area: Int = -1

            var superArea: Int = -1
            try {
                subArea = this.player!!.curMap.subArea!!.id
                area = this.player!!.curMap.subArea!!.area!!.id
                superArea = this.player!!.curMap.subArea!!.area!!.superArea
            } catch (e: Exception) {
                // ok
            }
            this.sendMessage("subArea : " + subArea.toString() + "\nArea : " + area.toString() + "\nsuperArea : " + superArea)
        }  else if (command.equals("DLUA", ignoreCase = true)) {
            var code: String = infos.subList(1, infos.size).joinToString(" ")
            var ret: Array<Any> = DataScriptVM.getInstance()!!.runAdminCommand(this.player!!,code, this::sendMessage)

            this.sendMessage(ret.joinToString(" "))
        } else {
            this.sendMessage("Commande invalide !")
        }
    }

    private fun getCellJail(): Int {
        when (Formulas.random.nextInt(4) + 1) {
1 -> {return 148
}
2 -> {return 156
}
3 -> {return 380
}
4 -> {return 388
}
else -> {return 148
        
}
}
    }

    private fun returnClasse(id: Int): String {
        when (id) {
Constant.CLASS_FECA -> {return "Fec"
}
Constant.CLASS_OSAMODAS -> {return "Osa"
}
Constant.CLASS_ENUTROF -> {return "Enu"
}
Constant.CLASS_SRAM -> {return "Sra"
}
Constant.CLASS_XELOR -> {return "Xel"
}
Constant.CLASS_ECAFLIP -> {return "Eca"
}
Constant.CLASS_ENIRIPSA -> {return "Eni"
}
Constant.CLASS_IOP -> {return "Iop"
}
Constant.CLASS_CRA -> {return "Cra"
}
Constant.CLASS_SADIDA -> {return "Sad"
}
Constant.CLASS_SACRIEUR -> {return "Sac"
}
Constant.CLASS_PANDAWA -> {return "Pan"
}
else -> {return "Unk"
        
}
}
    }
}
