package org.starloco.locos.`object`

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.client.Player
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.game.PrismData
import org.starloco.locos.database.data.game.SubAreaData
import org.starloco.locos.database.data.login.GuildData
import org.starloco.locos.database.data.login.PlayerData
import org.starloco.locos.dynamic.Noel
import org.starloco.locos.entity.Prism
import org.starloco.locos.entity.map.MountPark
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.entity.pet.PetEntry
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.spells.SpellEffect
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.game.world.World
import org.starloco.locos.job.JobStat
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.entity.Fragment
import org.starloco.locos.`object`.entity.SoulStone
import org.starloco.locos.other.Action
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import org.slf4j.LoggerFactory
import org.starloco.locos.common.splitJ

private val log = LoggerFactory.getLogger(ObjectAction::class.java)

class ObjectAction(private val type: String, private val args: String, private val cond: String) {
    private var send = true

    fun apply(player0: Player?, target: Player?, objet: Int, cellid: Int) {
        val `object` = World.world.getGameObject(objet)

        if (`object` == null) {
            SocketManager.GAME_SEND_MESSAGE(player0!!, "Error object null. Merci de prévenir un administrateur est d'indiquer le message.")
            return
        }
        if (player0 == null || !player0.isOnline || player0.doAction || player0.gameClient == null)
            return

        if (`object`.template!!.type != 116) {// EPO Fami, this is fucked but, condition is dead
            if (!this.cond.equals("", ignoreCase = true) && !this.cond.equals("-1", ignoreCase = true) && !World.world.conditionManager.validConditions(player0, this.cond)) {
                SocketManager.GAME_SEND_Im_PACKET(player0, "119")
                return
            }
        }
        if (player0.level < World.world.getGameObject(objet)!!.template!!.level) {
            SocketManager.GAME_SEND_Im_PACKET(player0, "119")
            return
        }

        val player = target ?: player0
        val fight = player.fight

        if (fight != null && fight.state != Constant.FIGHT_STATE_PLACE)
            return

        var sureIsOk = false
        var isOk = true
        var turn = 0
        var arg = ""
        var mapId = 0
        var cellId = 0
        var id0 = 0
        var job = 0
        var obj: GameObject?
        var object0: GameObject?
        var map0: GameMap?
        var template: ObjectTemplate? = null
        var templates: MutableList<ObjectTemplate?> = ArrayList()
        try {
            for (type in this.type.splitJ(";")) {
                val split = args.split("|".toRegex(), limit = 2).toTypedArray()
                if (this.args.isNotEmpty() && split.size > turn)
                    arg = split[turn]

                when (type.toInt()) {
                    -1 -> {
                        if (player0.fight != null) return
                        isOk = true
                        send = false
                    }

                    0 -> {//Teleportation.
                        if (player0.fight != null) return
                        mapId = arg.split(",".toRegex(), limit = 2).toTypedArray()[0].toShort().toInt()
                        cellId = arg.split(",".toRegex(), limit = 2).toTypedArray()[1].toInt()
                        if (mapId == 8978) {
                            isOk = false
                            send = false
                            return
                        }
                        if (!player.cantTP())
                            player.teleport(mapId, cellId)
                        else if (player.curCell.cellId == 268)
                            player.teleport(mapId, cellId)
                    }

                    1 -> {//Teleportation au point de sauvegarde.
                        if (player0.fight != null) return
                        if (!player.cantTP())
                            player.warpToSavePos()
                    }

                    2 -> {//Don de Kamas.
                        if (player0.fight != null) return
                        val count = arg.toInt()
                        val curKamas = player.kamas
                        var newKamas = curKamas + count
                        if (newKamas < 0)
                            newKamas = 0
                        player.kamas = newKamas
                        if (player.isOnline)
                            SocketManager.GAME_SEND_STATS_PACKET(player)
                    }

                    3 -> {//Don de vie.
                        if (this.type.splitJ(";").size > 1 && player.fight != null) return
                        var isOk1 = true
                        var isOk2 = true
                        for (arg0 in arg.splitJ(",")) {
                            var `val`: Int
                            val statId1: Int
                            if (arg.contains(";")) {
                                statId1 = arg.split(";")[0].toInt()
                                `val` = World.world.getGameObject(objet)!!.getRandomValue(World.world.getGameObject(objet)!!.encodeStats(), arg.split(";")[0].toInt())
                            } else {
                                statId1 = arg0.toInt()
                                `val` = World.world.getGameObject(objet)!!.getRandomValue(World.world.getGameObject(objet)!!.encodeStats(), arg0.toInt())
                            }
                            when (statId1) {
                                110 -> {//Vie.
                                    if (player.curPdv == player.maxPdv) {
                                        isOk1 = false
                                        continue
                                    }
                                    if (player.curPdv + `val` > player.maxPdv)
                                        `val` = player.maxPdv - player.curPdv
                                    player.setPdv(player.curPdv + `val`)
                                    if (player.fight != null)
                                        player.fight!!.getFighterByPerso(player)!!.setPdv(player.curPdv)
                                    SocketManager.GAME_SEND_STATS_PACKET(player)
                                    SocketManager.GAME_SEND_Im_PACKET(player, "01;$`val`")
                                    sureIsOk = true
                                }
                                139 -> {//Energie.
                                    if (player.energy == Player.maxEnergy.toInt()) {
                                        isOk2 = false
                                        continue
                                    }
                                    player.energy = player.energy + `val`
                                    SocketManager.GAME_SEND_STATS_PACKET(player)
                                    SocketManager.GAME_SEND_Im_PACKET(player, "07;$`val`")
                                    sureIsOk = true
                                }
                                605 -> {//Experience.
                                    player.addXp((`val` * Config.rateXp).toLong())
                                    SocketManager.GAME_SEND_STATS_PACKET(player)
                                    SocketManager.GAME_SEND_Im_PACKET(player, "08;$`val`")
                                    sureIsOk = true
                                }
                                614 -> {//Experience metier.
                                    val jobStat = player.getMetierByID(arg0.split(";")[1].toInt())
                                    if (jobStat == null) {
                                        isOk1 = false
                                        isOk2 = false
                                        continue
                                    }

                                    val can = `object`.template!!.id in 10382..10407
                                    `val` = `val` * (if (can) Config.rateJob else 1)
                                    jobStat.addXp(player, `val`.toLong())
                                    SocketManager.GAME_SEND_JX_PACKET(player, ArrayList(listOf<JobStat>(jobStat)))
                                    SocketManager.GAME_SEND_Im_PACKET(player, "017;" + `val` + "~" + arg0.split(";")[1].toInt())
                                    sureIsOk = true
                                }
                            }
                        }
                        if (arg.splitJ(",").size <= 2)
                            if (!isOk1 && !isOk2)
                                isOk = false
                            else if (isOk1 || isOk2)
                                isOk = false
                        send = false
                    }

                    4 -> {//Don de Stats.
                        if (player0.fight != null) return
                        for (arg0 in arg.splitJ(",")) {
                            val statId = arg0.split(";")[0].toInt()
                            val `val` = arg0.split(";")[1].toInt()
                            when (statId) {
                                1 -> {//Vitalite.
                                    for (i in 0 until `val`) {
                                        player.boostStat(11, false)
                                        player.statsParcho.addOneStat(Constant.STATS_ADD_VITA, 1)
                                    }
                                }
                                2 -> {//Sagesse.
                                    for (i in 0 until `val`) {
                                        player.statsParcho.addOneStat(Constant.STATS_ADD_SAGE, 1)
                                        player.boostStat(12, false)
                                    }
                                }
                                3 -> {//Force.
                                    for (i in 0 until `val`) {
                                        player.boostStat(10, false)
                                        player.statsParcho.addOneStat(Constant.STATS_ADD_FORC, 1)
                                    }
                                }
                                4 -> {//Intelligence.
                                    for (i in 0 until `val`) {
                                        player.boostStat(15, false)
                                        player.statsParcho.addOneStat(Constant.STATS_ADD_INTE, 1)
                                    }
                                }
                                5 -> {//Chance.
                                    for (i in 0 until `val`) {
                                        player.boostStat(13, false)
                                        player.statsParcho.addOneStat(Constant.STATS_ADD_CHAN, 1)
                                    }
                                }
                                6 -> {//Agilite.
                                    for (i in 0 until `val`) {
                                        player.boostStat(14, false)
                                        player.statsParcho.addOneStat(Constant.STATS_ADD_AGIL, 1)
                                    }
                                }
                                7 -> {//Point de Sort.
                                    player.setSpellPoints(player.get_spellPts() + `val`)
                                }
                            }
                        }
                        sureIsOk = true
                        SocketManager.GAME_SEND_STATS_PACKET(player)
                    }

                    6 -> {//Apprendre un sort.
                        if (player0.fight != null) return
                        id0 = arg.toInt()
                        if (World.world.getSort(id0) == null) {
                            isOk = false
                            return
                        }
                        if (!player.learnSpell(id0, if (player.curMap.id == 10129) 6 else 1, true, true, true)) {
                            isOk = false
                            return
                        }
                        send = false
                    }

                    7 -> {//Desapprendre un sort.
                        if (player0.fight != null) {
                            isOk = false
                            return
                        }
                        id0 = arg.toInt()
                        val oldLevel = player.getSortStatBySortIfHas(id0)!!.level
                        if (player.getSortStatBySortIfHas(id0) == null) {
                            isOk = false
                            return
                        }
                        if (oldLevel <= 1) {
                            isOk = false
                            return
                        }
                        player.unlearnSpell(player, id0, 1, oldLevel, true, true)
                    }

                    8 -> {//Desapprendre un sort a un percepteur.
                        val guild = player0.guild
                        if (player0.fight != null || guild == null || player0.guildMember == null) {
                            isOk = false
                            return
                        }

                        obj = World.world.getGameObject(objet)

                        if (obj != null) {
                            val spell = obj.stats[Constant.STATS_FORGET_ONE_LEVEL_SPELL]

                            if (spell != 0) {
                                if (spell <= 4) {
                                    var quantity = -1
                                    when (spell) {
                                        1 -> // Pods
                                            quantity = guild.resetStats(158)
                                        2 -> { // Nb collectors
                                            quantity = guild.nbCollectors
                                            guild.nbCollectors = 0
                                            guild.capital = guild.capital + quantity * 10
                                            quantity = -1
                                        }
                                        3 -> // Prospection
                                            quantity = guild.resetStats(176)
                                        4 -> // Sagesse
                                            quantity = guild.resetStats(124)
                                    }
                                    if (quantity != -1) {
                                        guild.capital = guild.capital + quantity
                                    }
                                } else {
                                    guild.unBoostSpell(spell)
                                }
                                isOk = true
                                send = true
                                (DatabaseManager.get(GuildData::class.java) as GuildData).update(guild)
                                SocketManager.GAME_SEND_gIB_PACKET(player0, guild.parseCollectorToGuild())
                            }
                        }
                        isOk = false
                        send = false
                    }

                    9 -> {//Oublie un metier.
                        if (player0.fight != null) {
                            isOk = false
                            return
                        }
                        job = arg.toInt()
                        val jobStats = player.getMetierByID(job)

                        if (jobStats == null) {
                            player.send("Im149$job")
                            return
                        }

                        player.unlearnJob(jobStats.id)
                    }

                    10 -> {//EPO.
                        if (player0.fight != null)
                            return

                        obj = World.world.getGameObject(objet)
                        if (obj == null)
                            return
                        object0 = player.getObjetByPos(Constant.ITEM_POS_FAMILIER)
                        if (object0 == null)
                            return
                        val pets = World.world.getPetsEntry(object0.guid)
                        if (pets == null)
                            return
                        if (obj.template!!.conditions!!.contains(object0.template!!.id.toString() + ""))
                            pets.giveEpo(player)
                        else
                            isOk = false
                    }

                    11 -> {//Change de Sexe.
                        if (player0.fight != null) return
                        if (player.sexe == 0)
                            player.sexe = 1
                        else
                            player.sexe = 0

                        SocketManager.GAME_SEND_ALTER_GM_PACKET(player.curMap, player)
                        (DatabaseManager.get(PlayerData::class.java) as PlayerData).updateInfos(player)
                    }

                    12 -> {//Change de nom.
                        if (player0.fight != null) return
                        player.changeName = true
                        isOk = false
                        send = false
                    }

                    13 -> {//Apprendre une emote.
                        if (player0.fight != null) return
                        val emote = arg.toInt()

                        if (player.emotes.contains(emote)) {
                            isOk = false
                            return
                        }

                        player.addStaticEmote(emote)
                    }

                    14 -> {//Apprendre un metier.
                        if (player0.fight != null) return
                        job = arg.toInt()
                        if (World.world.getMetier(job) == null)
                            return
                        if (player.getMetierByID(job) != null)//Metier deja appris
                        {
                            SocketManager.GAME_SEND_Im_PACKET(player, "111")
                            return
                        }
                        if (player.getMetierByID(2) != null
                            && player.getMetierByID(2)!!.get_lvl() < 30
                            || player.getMetierByID(11) != null
                            && player.getMetierByID(11)!!.get_lvl() < 30
                            || player.getMetierByID(13) != null
                            && player.getMetierByID(13)!!.get_lvl() < 30
                            || player.getMetierByID(14) != null
                            && player.getMetierByID(14)!!.get_lvl() < 30
                            || player.getMetierByID(15) != null
                            && player.getMetierByID(15)!!.get_lvl() < 30
                            || player.getMetierByID(16) != null
                            && player.getMetierByID(16)!!.get_lvl() < 30
                            || player.getMetierByID(17) != null
                            && player.getMetierByID(17)!!.get_lvl() < 30
                            || player.getMetierByID(18) != null
                            && player.getMetierByID(18)!!.get_lvl() < 30
                            || player.getMetierByID(19) != null
                            && player.getMetierByID(19)!!.get_lvl() < 30
                            || player.getMetierByID(20) != null
                            && player.getMetierByID(20)!!.get_lvl() < 30
                            || player.getMetierByID(24) != null
                            && player.getMetierByID(24)!!.get_lvl() < 30
                            || player.getMetierByID(25) != null
                            && player.getMetierByID(25)!!.get_lvl() < 30
                            || player.getMetierByID(26) != null
                            && player.getMetierByID(26)!!.get_lvl() < 30
                            || player.getMetierByID(27) != null
                            && player.getMetierByID(27)!!.get_lvl() < 30
                            || player.getMetierByID(28) != null
                            && player.getMetierByID(28)!!.get_lvl() < 30
                            || player.getMetierByID(31) != null
                            && player.getMetierByID(31)!!.get_lvl() < 30
                            || player.getMetierByID(36) != null
                            && player.getMetierByID(36)!!.get_lvl() < 30
                            || player.getMetierByID(41) != null
                            && player.getMetierByID(41)!!.get_lvl() < 30
                            || player.getMetierByID(56) != null
                            && player.getMetierByID(56)!!.get_lvl() < 30
                            || player.getMetierByID(58) != null
                            && player.getMetierByID(58)!!.get_lvl() < 30
                            || player.getMetierByID(60) != null
                            && player.getMetierByID(60)!!.get_lvl() < 30
                            || player.getMetierByID(65) != null
                            && player.getMetierByID(65)!!.get_lvl() < 30) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "18;30")
                            return
                        }
                        if (player.totalJobBasic() > 2) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "19")
                            return
                        } else {
                            if (job == 27) {
                                if (!player.hasItemTemplate(966, 1, false))
                                    return
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;"
                                        + 966 + "~" + 1)
                                player.learnJob(World.world.getMetier(job)!!)
                            } else {
                                player.learnJob(World.world.getMetier(job)!!)
                            }
                        }
                    }

                    15 -> {//TP au foyer.
                        if (player0.fight != null) return
                        var tp = false
                        for (i in World.world.houses.values) {
                            if (i.ownerId == player0.account.id) {
                                player.teleport(i.houseMapId.toShort().toInt(), i.houseCellId)
                                tp = true
                                break
                            }
                        }
                        if (!tp) {
                            player.send("Im161")
                            return
                        }
                    }

                    16 -> {//Pnj Follower.
                        if (player0.fight != null) return
                        // Petite larve doree = 7425
                        player.setMascotte(this.args.toInt())
                    }

                    17 -> {//Benediction.
                        if (player0.fight != null) return
                        player.setBenediction(World.world.getGameObject(objet)!!.template!!.id)
                    }

                    18 -> {//Malediction.
                        if (player0.fight != null) return
                        player.setMalediction(World.world.getGameObject(objet)!!.template!!.id)
                    }

                    19 -> {//RolePlay Buff.
                        if (player0.fight != null) return
                        player.setRoleplayBuff(World.world.getGameObject(objet)!!.template!!.id)
                    }

                    20 -> {//Bonbon.
                        if (player0.fight != null) return
                        player.setCandy(World.world.getGameObject(objet)!!.template!!.id)
                    }

                    21 -> {//Poser un objet d'elevage.
                        if (player0.fight != null) return
                        map0 = player.curMap
                        object0 = World.world.getGameObject(objet)
                        id0 = object0!!.template!!.id

                        val resist = object0.getResistance(object0.encodeStats())
                        val resistMax = object0.getResistanceMax(object0.template!!.strTemplate)
                        if (map0!!.mountPark == null)
                            return
                        val MP = map0.mountPark
                        if (player.guild == null) {
                            SocketManager.GAME_SEND_BN(player)
                            return
                        }
                        if (!player.guildMember!!.canDo(Constant.G_AMENCLOS)) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "193")
                            return
                        }
                        if (MP!!.cellOfObject.size == 0 || !MP.cellOfObject.contains(cellid) || MP.getCellAndObject().containsKey(cellid)) {
                            SocketManager.GAME_SEND_BN(player)
                            return
                        }
                        if (MP.getObject().size < MP.maxObject) {
                            MP.addObject(cellid, id0, player.id, resistMax, resist)
                            SocketManager.SEND_GDO_PUT_OBJECT_MOUNT(map0, "$cellid;$id0;1;$resist;$resistMax")
                        } else {
                            SocketManager.GAME_SEND_Im_PACKET(player, "1107")
                            return
                        }
                    }

                    22 -> {//Poser un prisme.
                        val cellId1 = player.curCell.cellId
                        if (player0.fight != null || cellId1 <= 0)
                            return
                        map0 = player.curMap
                        val subArea = map0!!.subArea!!
                        val area = subArea.area!!
                        val alignement = player.alignment

                        if (player.level < 10 || alignement == 0 || alignement == 3) {
                            player.send("Im1155")
                            return
                        }
                        if (!player.showWings) {
                            player.send("Im1148")
                            return
                        }
                        if (!subArea.ownNearestSubArea(player)) {
                            player.send("Im1147")
                            return
                        }
                        if (map0.places.size < 2 || map0.data.noPrisms
                            || area.id == 42 || (subArea.id == 9
                                    || subArea.id == 95) || map0.haveMobFix()) {
                            player.send("Im1146")
                            return
                        }
                        if (subArea.alignment != 0 || !subArea.conquerable) {
                            player.send("Im1149")
                            return
                        }
                        if (subArea.isMoreThanEnemies(player)) {
                            player.send("Im1153")
                            return
                        }

                        val prism = Prism(World.world.getNextIDPrisme(), alignement.toByte(), 1, map0.id, cellId1, player.honor, -1)
                        subArea.alignment = alignement
                        subArea.prism = prism

                        for (z in World.world.onlinePlayers) {
                            if (z == null)
                                continue
                            if (z.alignment == 0) {
                                SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(z, subArea.id.toString() + "|" + alignement + "|1")
                                if (area.alignement == 0)
                                    SocketManager.GAME_SEND_aM_ALIGN_PACKET_TO_AREA(z, area.id.toString() + "|" + alignement)
                                continue
                            }
                            SocketManager.GAME_SEND_am_ALIGN_PACKET_TO_SUBAREA(z, subArea.id.toString() + "|" + alignement + "|0")
                            if (area.alignement == 0)
                                SocketManager.GAME_SEND_aM_ALIGN_PACKET_TO_AREA(z, area.id.toString() + "|" + alignement)
                        }
                        if (area.alignement == 0) {
                            area.prismId = prism.id
                            area.alignement = alignement
                            prism.conquestArea = area.id
                        }
                        World.world.addPrisme(prism)
                        (DatabaseManager.get(PrismData::class.java) as PrismData).insert(prism)
                        player.curMap.subArea!!.alignment = player.alignment
                        (DatabaseManager.get(SubAreaData::class.java) as SubAreaData).update(player.curMap.subArea!!)
                        SocketManager.GAME_SEND_PRISME_TO_MAP(map0, prism)
                    }

                    23 -> {//Rappel Prismatique.
                        if (player0.fight != null) return
                        var dist = 99999
                        var alea: Int
                        mapId = 0
                        cellId = 0
                        for (i in World.world.AllPrisme()!!) {
                            if (i.alignment != player.alignment)
                                continue
                            alea = (World.world.getMap(i.map).x - player.curMap.x) *
                                    (World.world.getMap(i.map).x - player.curMap.x) +
                                    (World.world.getMap(i.map).y - player.curMap.y) *
                                    (World.world.getMap(i.map).y - player.curMap.y)
                            if (alea < dist) {
                                dist = alea
                                mapId = i.map
                                cellId = i.cell
                            }
                        }
                        if (mapId != 0)
                            player.teleport(mapId, cellId)
                    }

                    24 -> {//TP Village aligne.
                        if (player0.fight != null || player0.alignment == 0 || player0.alignment == 3) {
                            isOk = false
                            send = false
                            return
                        }
                        mapId = arg.split(",")[0].toInt().toShort().toInt()
                        cellId = arg.split(",")[1].toInt()
                        if (World.world.getMap(mapId).subArea!!.alignment.toInt() == player.alignment)
                            player.teleport(mapId, cellId)
                    }

                    25 -> {//Spawn groupe.
                        if (player0.fight != null || player0.curMap.haveMobFix()) return
                        val inArena = arg.split(";")[0] == "true"
                        val groupData: String
                        if (inArena && !SoulStone.isInArenaMap(player.curMap.id))
                            return
                        if (arg.split(";")[1][0] == '1') {
                            groupData = arg.split("@")[1]
                        } else {
                            val soulStone = World.world.getGameObject(objet) as SoulStone
                            groupData = soulStone.parseGroupData()
                        }
                        val condition = "MiS = " + player.id
                        player.curMap.spawnNewGroup(true, player.curCell.cellId, groupData, condition)
                    }

                    26 -> {//Ajout d'objet.
                        if (player0.fight != null) return
                        for (i in arg.splitJ(";")) {
                            obj = World.world.getObjTemplate(i.split(",")[0].toInt())!!.createNewItem(i.split(",")[1].toInt(), false)
                            if (player.addItem(obj!!, true, false))
                                World.world.addGameObject(obj)
                        }
                        SocketManager.GAME_SEND_Ow_PACKET(player)
                    }

                    27 -> {//Ajout de titre.
                        if (player0.fight != null) return
                        player.allTitle = arg
                    }

                    28 -> {//Ajout de zaap.
                        if (player0.fight != null) return
                        player.verifAndAddZaap(arg.toInt())
                    }

                    29 -> {//Panel d'oubli de sort.
                        if (player0.fight != null) return
                        player.exchangeAction = ExchangeAction<Any?>(ExchangeAction.FORGETTING_SPELL, 0)
                        SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', player)
                    }

                    31 -> {//Cadeau bworker.
                        if (player0.fight != null) return
                        Action(511, "", "").apply(player, null, objet, -1, null)
                    }

                    32 -> {//Geoposition traque.
                        if (player0.fight != null) return
                        val traque = World.world.getGameObject(objet)!!.traquedName

                        if (traque == null)
                            Unit

                        val cible = World.world.getPlayerByName(traque!!)

                        if (cible == null || cible.alignment == 0 || (cible.alignment == player0.alignment)) {
                            isOk = true
                            send = true
                        } else {
                            if (!cible.isOnline) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "1198")
                                Unit
                            }

                            SocketManager.GAME_SEND_FLAG_PACKET(player, cible)
                        }
                    }

                    33 -> {//Ajout de points boutique.
                        if (player0.fight != null) return
                        player0.account.modPoints(arg.toInt().toLong())
                        player.sendTypeMessage("Shop", "Points : " + player0.account.points)
                    }

                    34 -> {//Fm cac
                        val gameObject = player.getObjetByPos(Constant.ITEM_POS_ARME)

                        if (gameObject == null) {
                            player.sendMessage(player.lang.trans("objet.objectaction.fmcac.noequip"))
                            isOk = false
                            send = false
                            return
                        }

                        var containNeutre = false

                        for (effect in gameObject.effects)
                            if (effect.effectID == 100 || effect.effectID == 95)
                                containNeutre = true

                        if (containNeutre) {
                            for (i in 0 until gameObject.effects.size) {
                                if (gameObject.effects[i].effectID == 100) {
                                    when (this.args.uppercase()) {
                                        "EAU" -> gameObject.effects[i].effectID = 96
                                        "TERRE" -> gameObject.effects[i].effectID = 97
                                        "AIR" -> gameObject.effects[i].effectID = 98
                                        "FEU" -> gameObject.effects[i].effectID = 99
                                    }
                                }
                                if (gameObject.effects[i].effectID == 95) {
                                    when (this.args.uppercase()) {
                                        "EAU" -> gameObject.effects[i].effectID = 91
                                        "TERRE" -> gameObject.effects[i].effectID = 92
                                        "AIR" -> gameObject.effects[i].effectID = 93
                                        "FEU" -> gameObject.effects[i].effectID = 94
                                    }
                                }
                            }

                            SocketManager.GAME_SEND_STATS_PACKET(player)
                            SocketManager.GAME_SEND_UPDATE_ITEM(player, gameObject)
                            player.sendMessage(player.lang.trans("objet.objectaction.fmcac.succes"))
                        } else {
                            player.sendMessage(player.lang.trans("objet.objectaction.fmcac.noneutre"))
                            isOk = false
                            send = false
                        }
                    }

                    35 -> { // Mount cameleon
                        if (player.mount != null) {
                            player.mount!!.capacitys.add(9)
                            player.mount!!.setCastrated()
                            if (player.onMount) {
                                SocketManager.GAME_SEND_ALTER_GM_PACKET(player.curMap, player)
                            }
                            SocketManager.GAME_SEND_MOUNT_DESCRIPTION_PACKET(player, player.mount!!)
                            player.sendMessage(player.lang.trans("objet.objectaction.mountcameleon"))
                            sureIsOk = true
                            send = true
                        } else {
                            player.sendMessage(player.lang.trans("objet.objectaction.mountcameleon.none"))
                            return
                        }
                    }
                    36 -> {//Coffre
                        if (player0.fight != null || player0.level == 1) return
                        var tour = 0
                        val objects = ArrayList<ObjectTemplate>()
                        var nbrMaxItem = 0
                        for (i in arg.splitJ(";")) {
                            tour++
                            when (tour) {
                                1 -> {
                                    val templates0 = ArrayList<ObjectTemplate>()
                                    val maxLvl = if (player.level > 150) 150 else player.level
                                    val minLvl = if (player.level > 150) 120 else if (player.level - 30 <= 0) 1 else player.level - 30

                                    for (j in 0 until i.toInt()) {
                                        do {
                                            templates0.clear()
                                            World.world.objTemplates.stream().filter { t -> t.isAnEquipment(false, listOf(Constant.ITEM_TYPE_FAMILIER, Constant.ITEM_TYPE_CERTIF_MONTURE)) && t.level == Formulas.getRandomValue(minLvl, maxLvl) }.forEach { templates0.add(it) }
                                        } while (templates0.size == 0)
                                        objects.add(templates0[Formulas.getRandomValue(0, templates0.size - 1)])
                                    }
                                }
                                2 -> {
                                    val size = i.split("-")
                                    player.addKamas(Formulas.getRandomValue(size[0].toInt(), size[1].toInt()).toLong())
                                }
                                3 -> nbrMaxItem = i.toInt()
                            }
                        }

                        for (template0 in objects) {
                            if (nbrMaxItem > 0) {
                                obj = template0.createNewItem(1, true)
                                nbrMaxItem--
                            } else {
                                obj = template0.createNewItem(1, false)
                            }
                            if (player.addItem(obj!!, true, false))
                                World.world.addGameObject(obj)
                            SocketManager.GAME_SEND_Im_PACKET(player, "021;1~" + template0.id)
                        }
                        SocketManager.GAME_SEND_Ow_PACKET(player)
                        SocketManager.GAME_SEND_STATS_PACKET(player)
                    }
                    37 -> { // Coffre divers
                        if (player0.fight != null) return
                        template = null
                        templates = ArrayList()
                        var max = false
                        when (arg.toInt()) {
                            2 -> { //Sort
                                World.world.objTemplates.stream().filter { t -> t.type == Constant.ITEM_TYPE_PARCHEMIN_SORT }.forEach { templates.add(it) }
                                template = templates[Formulas.random.nextInt(templates.size)]
                            }
                            3 -> { //Maitrise
                                World.world.objTemplates.stream().filter { t -> t.type == Constant.ITEM_TYPE_MAITRISE }.forEach { templates.add(it) }
                                template = templates[Formulas.random.nextInt(templates.size)]
                            }
                            4 -> { //Obji
                                World.world.objTemplates.stream().filter { t -> t.type == Constant.ITEM_TYPE_OBJET_VIVANT }.forEach { templates.add(it) }
                                template = templates[Formulas.random.nextInt(templates.size)]
                            }
                            5 -> { //Fami
                                World.world.objTemplates.stream().filter { t -> t.type == Constant.ITEM_TYPE_FAMILIER }.forEach { templates.add(it) }
                                template = templates[Formulas.random.nextInt(templates.size)]
                                max = true
                            }
                            6 -> {
                                val item = IntArray(3)
                                item[0] = 7493; item[1] = 7494; item[2] = 7495
                                template = World.world.getObjTemplate(item[Formulas.getRandomValue(0, 2)])
                            }
                        }
                        obj = template!!.createNewItem(1, max)
                        if (player.addItem(obj!!, true, false))
                            World.world.addGameObject(obj)
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;1~" + template.id)
                    }
                    38 -> { // Coffre dragondinde
                        if (player0.fight != null) return
                        templates = ArrayList()
                        val acceptedMount = listOf(7808, 7810, 7811, 7812, 7813, 7814, 7815, 7816, 7817, 7818, 7819, 7820, 7821, 7822)
                        World.world.objTemplates.stream().filter { t -> t.type == Constant.ITEM_TYPE_CERTIF_MONTURE && acceptedMount.contains(t.id) }.forEach { templates.add(it) }
                        template = templates[Formulas.random.nextInt(templates.size)]

                        obj = template!!.createNewItem(1, false)
                        val mount = Mount(Constant.getMountColorByParchoTemplate(template.id), player.id, false)
                        obj!!.clearStats()
                        obj.stats.addOneStat(995, mount.id)
                        obj.txtStat[996] = player.name
                        obj.txtStat[997] = mount.name!!
                        mount.setCastrated()
                        mount.setToMax()

                        if (player.addItem(obj, true, false))
                            World.world.addGameObject(obj)
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;1~" + template.id)
                    }
                    39 -> { // Changer de couleur
                        player.send("bC")
                        send = false
                        isOk = false
                    }
                }
                turn++
            }
        } catch (e: Exception) {
            log.error("unexpected error", e)
                }

        var effect = this.haveEffect(World.world.getGameObject(objet)!!.template!!.id, World.world.getGameObject(objet)!!, player)
        if (effect)
            isOk = true
        if (isOk)
            effect = true
        if (this.type.splitJ(";").size > 1)
            isOk = true
        if (objet != -1) {
            if (send)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + World.world.getGameObject(objet)!!.template!!.id)
            if (sureIsOk || (isOk && effect && World.world.getGameObject(objet)!!.template!!.id != 7799)) {
                if (World.world.getGameObject(objet) != null) {
                    player0.removeItem(objet, 1, true, true)
                }
            }
        }
    }

    private fun haveEffect(id: Int, gameObject: GameObject, player: Player): Boolean {
        if (player.fight != null) return true
        when (id) {
            8378 -> {//Fragment magique.
                for (couple in (gameObject as Fragment).runes) {
                    val objectTemplate = World.world.getObjTemplate(couple.first)

                    if (objectTemplate == null)
                        continue

                    val newGameObject = objectTemplate.createNewItem(couple.second, true)

                    if (newGameObject == null)
                        continue

                    if (!player.addObjetSimiler(newGameObject, true, -1)) {
                        World.world.addGameObject(newGameObject)
                        player.addItem(newGameObject, true)
                    }
                }
                send = true
                return true
            }
            7799 -> {//Le Saut Sifflard
                player.toogleOnMount()
                send = false
                return false
            }

            10832 -> {//Craqueloroche
                if (player.fight != null || player.curMap.haveMobFix()) return false
                player.curMap.spawnNewGroup(true, player.curCell.cellId, "483,1,1000", "MiS=" + player.id)
                return true
            }

            10664 -> {//Abragland
                if (player.fight != null || player.curMap.haveMobFix()) return false
                player.curMap.spawnNewGroup(true, player.curCell.cellId, "47,1,1000", "MiS=" + player.id)
                return true
            }

            10665 -> {//Coffre de Jorbak
                player.setCandy(10688)
                return true
            }

            10670 -> {//Parchemin de persimol
                player.setBenediction(10682)
                return true
            }

            8435 -> {//Ballon Rouge Magique
                SocketManager.sendPacketToMap(player.curMap, ("GA;208;"
                        + player.id + ";" + player.curCell.cellId
                        + ",2906,11,8,1"))
                return true
            }

            8624 -> {//Ballon Bleu Magique
                SocketManager.sendPacketToMap(player.curMap, ("GA;208;"
                        + player.id + ";" + player.curCell.cellId
                        + ",2907,11,8,1"))
                return true
            }

            8625 -> {//Ballon Vert Magique
                SocketManager.sendPacketToMap(player.curMap, ("GA;208;"
                        + player.id + ";" + player.curCell.cellId
                        + ",2908,11,8,1"))
                return true
            }

            8430 -> {//Ballon Jaune Magique
                SocketManager.sendPacketToMap(player.curMap, ("GA;208;"
                        + player.id + ";" + player.curCell.cellId
                        + ",2909,11,8,1"))
                return true
            }

            8621 -> {//Cawotte Maudite
                player.gfxId = 1109
                player.orientation = 1
                SocketManager.GAME_SEND_ALTER_GM_PACKET(player.curMap, player)
                SocketManager.GAME_SEND_eD_PACKET_TO_MAP(player.curMap, player.id, 1)
                return true
            }

            8626 -> {//Nisitik Miditik
                player.gfxId = 1046
                player.orientation = 1
                SocketManager.GAME_SEND_ALTER_GM_PACKET(player.curMap, player)
                SocketManager.GAME_SEND_eD_PACKET_TO_MAP(player.curMap, player.id, 1)
                return true
            }

            10833 -> {//Chapain
                player.gfxId = 9001
                player.orientation = 1
                SocketManager.GAME_SEND_ALTER_GM_PACKET(player.curMap, player)
                SocketManager.GAME_SEND_eD_PACKET_TO_MAP(player.curMap, player.id, 1)
                return true
            }

            10839 -> {//Monstre Pain
                if (player.fight != null || player.curMap.haveMobFix()) return false
                player.curMap.spawnNewGroup(true, player.curCell.cellId, "2787,1,1000", "MiS=" + player.id)
                return true
            }

            8335 -> {//Cadeau 1
                Noel.getRandomObjectOne(player)
                return true
            }
            8336 -> {//Cadeau 2
                Noel.getRandomObjectTwo(player)
                return true
            }
            8337 -> {//Cadeau 3
                Noel.getRandomObjectTree(player)
                return true
            }
            8339 -> {//Cadeau 4
                Noel.getRandomObjectFour(player)
                return true
            }
            8340 -> {//Cadeau 5
                Noel.getRandomObjectFive(player)
                return true
            }
            10912 -> return false //Cadeau nowel 1
            10913 -> return false //Cadeau nowel 2
            10914 -> return false //Cadeau nowel 3
        }
        return false
    }
}
