package org.starloco.locos.other

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.entity.map.House
import org.starloco.locos.entity.map.MountPark
import org.starloco.locos.client.Player
import org.starloco.locos.client.other.Stalk
import org.starloco.locos.client.other.Stats
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.SocketManager
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.login.PlayerData
import org.starloco.locos.entity.monster.MonsterGroup
import org.starloco.locos.entity.npc.Npc
import org.starloco.locos.entity.pet.PetEntry
import org.starloco.locos.game.GameClient
import org.starloco.locos.game.GameServer
import org.starloco.locos.game.action.ExchangeAction
import org.starloco.locos.game.world.World
import org.starloco.locos.game.world.World.Couple
import org.starloco.locos.job.Job
import org.starloco.locos.job.JobStat
import org.starloco.locos.kernel.Config
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.`object`.ObjectTemplate
import org.starloco.locos.`object`.entity.SoulStone

import java.util.*
import java.util.Map.Entry
import org.slf4j.LoggerFactory
import org.starloco.locos.common.splitJ

private val log = LoggerFactory.getLogger(Action::class.java)

open class Action {

    var id: Int = 0
    private lateinit var args: String
    private lateinit var cond: String

    constructor(id: Int, args: String, cond: String) {
        this.id = id
        this.setArgs(args)
        this.setCond(cond)
    }

    companion object {
    @JvmStatic fun getDopeul(): Map<Int, Couple<Int, Int>> {
        var changeDopeul: MutableMap<Int, Couple<Int, Int>> = HashMap<Int,Couple<Int,Int>>()
        changeDopeul.put(1549, Couple(167, 460)); // Dopeul iop
        changeDopeul.put(1466, Couple(169, 465)); // Dopeul sadida
        changeDopeul.put(1558, Couple(168, 458)); // Dopeul cra
        changeDopeul.put(1470, Couple(162, 464)); // Dopeul enu
        changeDopeul.put(1469, Couple(164, 468)); // Dopeul xelor
        changeDopeul.put(1546, Couple(161, 461)); // Dopeul osa
        changeDopeul.put(1554, Couple(160, 469)); // Dopeul feca
        changeDopeul.put(6928, Couple(166, 462)); // Dopeul eni
        changeDopeul.put(8490, Couple(2691, 466)); // Dopeul panda
        changeDopeul.put(6926, Couple(163, 467)); // Dopeul sram
        changeDopeul.put(1544, Couple(165, 459)); // Dopeul eca
        changeDopeul.put(6949, Couple(455, 463)); // Dopeul sacri
        return changeDopeul
    }    }


    private fun Couple(i: Int, j: Int): Couple<Int,Int> {
        return Couple<Int,Int>(i, j)
    }

    fun getArgs(): String {
        return args
    }

    fun setArgs(args: String) {
        this.args = args
    }

    fun setCond(cond: String) {
        this.cond = cond
    }

    fun apply(player: Player?, target: Player?, itemID: Int, cellid: Int, map: GameMap?): Boolean {

        if (player == null)
            return true
        val player = player
        if (player.fight != null) {
            SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("ther.action.apply.impossible"), "000000")
            return true
        }
        if (!cond.equals("", ignoreCase = true) && !cond.equals("-1", ignoreCase = true) && !World.world.conditionManager.validConditions(player, cond)) {
            SocketManager.GAME_SEND_Im_PACKET(player, "119")
            return true
        }

        var client: GameClient = player.getGameClient()!!
        var mapId: Int = 0; var mapSecu: Short = 0; var cellId: Int = 0; var obj1: Int = 0; var cell: Int = 0; var stats: String = ""; var newStats: String = ""; var obj: GameObject? = null; var `object`: GameObject? = null; var obj1G: GameObject? = null
        when (id) {
-22 -> {if (player.getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR) != null) {
                    var skinFollower: Int = player.getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR)!!.template!!.id
                    var questId: Int = Constant.getQuestByMobSkin(skinFollower)
                    if (questId != -1) {
                        //perso.upgradeQuest(questId);
                        player.setMascotte(1)
                        var itemFollow: Int = Constant.getItemByMobSkin(skinFollower)
                        player.removeItemByTemplateId(itemFollow, 1, false)
                    }
                }
                
}
-11 -> {player.teleport(Constant.getStartMap(player.classe).toInt(), Constant.getStartCell(player.classe))
                SocketManager.GAME_SEND_WELCOME(player)
                
}
-10 -> {if (player.alignment == 1 || player.alignment == 2
                        || player.alignment == 3)
                    return true
                var ange: Int = 0
                var demon: Int = 0
                var total: Int = 0
                for (i in  World.world.players) {
                    if (i == null)
                        continue
                    if (i.alignment == 1)
                        ange++
                    if (i.alignment == 2)
                        demon++
                    total++
                }
                ange = ange / total
                demon = demon / total
                if (ange > demon)
                    player.modifAlignement(2)
                else if (demon > ange)
                    player.modifAlignement(1)
                else if (demon == ange)
                    player.modifAlignement(Formulas.getRandomValue(1, 2))
                
}
-9 -> {player.allTitle = args
                
}
-8 -> {player.verifAndAddZaap(args.toInt())
                
}
-6 -> {var mapActuel: GameMap = player.curMap
                var dopeuls: Map<Int, Couple<Int, Int>> = Action.getDopeul()
                var IDmob: Int? = null
                if (mapActuel.id in dopeuls) {
                    IDmob = dopeuls[mapActuel.id]!!.first
                } else {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.error.dopeul"))
                    return true
                }

                var LVLmob: Int = Formulas.getLvlDopeuls(player.level)
                if (player.level < 11) {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.error.dopeul.join.lvl"))
                    return true
                }
                var certificat: Int = Constant.getCertificatByDopeuls(IDmob)
                if (certificat == -1)
                    return true
                if (player.hasItemTemplate(certificat, 1, false)) {
                    var date: String? = player.getItemTemplate(certificat, 1)!!.txtStat[Constant.STATS_DATE]
                    try {
                        var timeStamp: Long = date!!.toLong()
                        if (System.currentTimeMillis() - timeStamp <= 86400000) {
                            SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.error.dopeul.join.day"))
                            return true
                        } else
                            player.removeItemByTemplateId(certificat, 1, false)
                    } catch (ignored: Exception) {
                        player.removeItemByTemplateId(certificat, 1, false)
                    }
                }
//                boolean b = true;
//                if (player.getQuestPerso() != null
//                        && !player.getQuestPerso().isEmpty()) {
//                    for (Entry<Integer, QuestPlayer> entry : new HashMap(player.getQuestPerso()).entries) {
//                        QuestPlayer qa = entry.value;
//                        if (qa.getQuest().getId() == dopeuls[(int] mapActuel.id).second) {
//                            b = false;
//                            if (qa.isFinished()) {
//                                player.delQuestProgress(entry.key);
//                                if (qa.removeQuestPlayer()) {
//                                    Quest q = Quest.quests[dopeuls[(int] mapActuel.id).second);
//                                    q.apply(player);
//                                }
//                            }
//                        }
//                    }
//                }
//                if (b) {
//                    Quest q = Quest.quests[dopeuls[(int] mapActuel.id).second);
//                    q.apply(player);
//                }
                var grp: String = IDmob.toString() + "," + LVLmob.toString() + "," + LVLmob.toString() + ";"
                var MG: MonsterGroup = MonsterGroup(player.curMap.nextObjectId, map, player.curCell.getId(), grp)
                player.curMap.startFigthVersusDopeuls(player, MG)
                
}
-5 -> {try {
                    var sID: Int = args.toInt()
                    if (World.world.getSort(sID) == null)
                        return true
                    player.learnSpell(sID, 1, true, true, true)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                
}
-4 -> {when (args.toInt()) {
1 -> {player.leaveEnnemyFactionAndPay(player)
                        
}
2 -> {player.leaveEnnemyFaction()
                        
}
}
                
}
-3 -> {var idMascotte: Int = args.toInt()

                if (player.hasItemTemplate(itemID, 1, false)) {
                    player.removeItemByTemplateId(itemID, 1, false)
                    player.setMascotte(idMascotte)
                    ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(player)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + itemID)
                    SocketManager.GAME_SEND_ALTER_GM_PACKET(player.curMap, player)
                }
                
}
-2 -> {if (player.away)
                    return true
                if (player.getGuild() != null || player.guildMember != null) {
                    SocketManager.GAME_SEND_gC_PACKET(player, "Ea")
                    return true
                }
                if (player.hasItemTemplate(1575, 1, false)) {
                    SocketManager.GAME_SEND_gn_PACKET(player)
                    player.removeItemByTemplateId(1575, -1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + -1 + "~" + 1575)
                } else {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.error.guild.create.noguildalogemme"))
                }
                
}
-1 -> {var npc: Npc? = player.curMap.getNpcByTemplateId(9048) // What if it's a different kind of bank teller
                if(npc != null) player.openBank()
                
}
0 -> {try {
                    var newMapID: Short = (args.split(",", limit = 2)[0]).toShort()
                    var newCellID: Int = (args.split(",", limit = 2)[1]).toInt()
                    if (!player.isInPrison()) {
                        player.teleport(newMapID.toInt(), newCellID)
                    } else {
                        if (player.curCell.getId() == 268) {
                            player.teleport(newMapID.toInt(), newCellID)
                        }
                    }
                } catch (e: Exception) {
                    // Pas ok, mais il y a trop de dialogue de PNJ bugg� pour laisser cette erreur flood.
                    // log.error("unexpected error", e)
                    return true
                }
                
}
2 -> {try {
                    var newMapID: Short = (args.split(",")[0]).toShort()
                    var newCellID: Int = (args.split(",")[1]).toInt()
                    var verifMapID: Int = (args.split(",")[2]).toInt()
                    if (player.curMap.id == verifMapID)
                        player.teleport(newMapID.toInt(), newCellID)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    return true
                }
                
}
4 -> {try {
                    var count: Int = args.toInt()
                    var curKamas: Long = player.kamas
                    var newKamas: Long = curKamas + count
                    if (newKamas < 0) {
                        SocketManager.GAME_SEND_Im_PACKET(player, "182")
                        return true
                    } else {
                        player.kamas = newKamas
                        if(count < 0) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "046;" + (-count))
                        } else {
                            SocketManager.GAME_SEND_Im_PACKET(player, "045;" + count)
                        }
                        if (player.isOnline)
                            SocketManager.GAME_SEND_STATS_PACKET(player)
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
5 -> {try {
                    var tID: Int = (args.split(",")[0]).toInt()
                    var count: Int = (args.split(",")[1]).toInt()
                    var send: Boolean = true
                    if (args.splitJ(",").size > 2)
                        send = args.split(",")[2].equals("1")

                    //Si on ajoute
                    if (count > 0) {
                        var T: ObjectTemplate? = World.world.getObjTemplate(tID)
                        if (T == null)
                            return true
                        var O: GameObject? = T!!.createNewItem(count, false)
                        //Si retourne true, on l'ajoute au monde
                        if (player.addItem(O!!, true, false))
                            World.world.addGameObject(O)
                    } else {
                        player.removeItemByTemplateId(tID, -count, false)
                    }
                    //Si en ligne (normalement oui)
                    if (player.isOnline)//on envoie le packet qui indique l'ajout//retrait d'un item
                    {
                        SocketManager.GAME_SEND_Ow_PACKET(player)
                        if (send) {
                            if (count >= 0) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "021;" + count.toString() + "~" + tID)
                            } else if (count < 0) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + (-count).toString() + "~" + tID)
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
6 -> {try {
                    if(client == null) return true
                    var mID: Int = (args.split(",")[0]).toInt()
                    var mapId: Int = (args.split(",")[1]).toInt()
                    var sucess: Int = (args.split(",")[2]).toInt()
                    var fail: Int = (args.split(",")[3]).toInt()
                    if (World.world.getMetier(mID) == null)
                        return true
                    // Si c'est un m�tier 'basic' :
                    if (mID == 2 || mID == 11 || mID == 13 || mID == 14
                            || mID == 15 || mID == 16 || mID == 17 || mID == 18
                            || mID == 19 || mID == 20 || mID == 24 || mID == 25
                            || mID == 26 || mID == 27 || mID == 28 || mID == 31
                            || mID == 36 || mID == 41 || mID == 56 || mID == 58
                            || mID == 60 || mID == 65 || mID == 47 ) { // Ajouter métiers qui bug pnj ici !
                        if (player.getMetierByID(mID) != null)//M�tier d�j� appris
                        {
                            SocketManager.GAME_SEND_Im_PACKET(player, "111")
                            SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                            player.exchangeAction = null
                            return true
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
                            if (sucess == -1 || fail == -1) {
                                SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                                player.exchangeAction = null
                                SocketManager.GAME_SEND_Im_PACKET(player, "18;30")
                            } else
                                SocketManager.send(client, "DQ" + fail.toString() + "|4840")
                            return true
                        }
                        if (player.totalJobBasic() > 2)//On compte les m�tiers d�ja acquis si c'est sup�rieur a 2 on ignore
                        {
                            SocketManager.GAME_SEND_Im_PACKET(player, "19")
                            SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                            player.exchangeAction = null
                            return true
                        } else
                        //Si c'est < ou = � 2 on apprend
                        {
                            if (mID == 27) {
                                if (!player.hasItemTemplate(966, 1, false))
                                    return true
                                player.removeItemByTemplateId(966, 1, false)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 966 + "~" + 1)
                                player.learnJob(World.world.getMetier(mID)!!)
                            } else {
                                if (player.curMap.id != mapId)
                                    return true
                                player.learnJob(World.world.getMetier(mID)!!)
                                if (sucess == -1 || fail == -1) {
                                    SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                                    player.exchangeAction = null
                                } else
                                    SocketManager.send(client, "DQ" + sucess.toString() + "|4840")
                            }
                        }
                    }
                    // Si c'est une specialisations 'FM' :
                    /*
					 * if(mID == 43 || mID == 44 || mID == 45 || mID == 46 ||
					 * mID == 47 || mID == 48 || mID == 49 || mID == 50 || mID
					 * == 62 || mID == 63 || mID == 64) { //Si necessaire lvl
					 * 65, enlev� les hide si ankalike
					 * if(perso.getMetierByID(17) != null &&
					 * perso.getMetierByID(17).get_lvl() < 65 && mID == 43 ||
					 * perso.getMetierByID(11) != null &&
					 * perso.getMetierByID(11).get_lvl() < 65 && mID == 44 ||
					 * perso.getMetierByID(14) != null &&
					 * perso.getMetierByID(14).get_lvl() < 65 && mID == 45 ||
					 * perso.getMetierByID(20) != null &&
					 * perso.getMetierByID(20).get_lvl() < 65 && mID == 46 ||
					 * perso.getMetierByID(31) != null &&
					 * perso.getMetierByID(31).get_lvl() < 65 && mID == 47 ||
					 * perso.getMetierByID(13) != null &&
					 * perso.getMetierByID(13).get_lvl() < 65 && mID == 48 ||
					 * perso.getMetierByID(19) != null &&
					 * perso.getMetierByID(19).get_lvl() < 65 && mID == 49 ||
					 * perso.getMetierByID(18) != null &&
					 * perso.getMetierByID(18).get_lvl() < 65 && mID == 50 ||
					 * perso.getMetierByID(15) != null &&
					 * perso.getMetierByID(15).get_lvl() < 65 && mID == 62 ||
					 * perso.getMetierByID(16) != null &&
					 * perso.getMetierByID(16).get_lvl() < 65 && mID == 63 ||
					 * perso.getMetierByID(27) != null &&
					 * perso.getMetierByID(27).get_lvl() < 65 && mID == 64) {
					 * //On compte les specialisations d�ja acquis si c'est
					 * sup�rieur a 2 on ignore if(perso.getMetierByID(mID) !=
					 * null)//M�tier d�j� appris
					 * SocketManager.GAME_SEND_Im_PACKET(perso, "111");
					 * if(perso.totalJobFM() > 2)//On compte les m�tiers d�ja
					 * acquis si c'est sup�rieur a 2 on ignore
					 * SocketManager.GAME_SEND_Im_PACKET(perso, "19"); else//Si
					 * c'est < ou = � 2 on apprend
					 * perso.learnJob(World.world.getMetier(mID)); }else {
					 * SocketManager.GAME_SEND_Im_PACKET(perso, "12"); } }
					 */
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
7 -> {if (!player.isInPrison())
                    player.warpToSavePos()
                
}
8 -> {try {
                    var statID: Int = (args.split(",", limit = 2)[0]).toInt()
                    var number: Int = (args.split(",", limit = 2)[1]).toInt()
                    player.stats.addOneStat(statID, number)
                    SocketManager.GAME_SEND_STATS_PACKET(player)
                    var messID: Int = 0
                    when (statID) {
Constant.STATS_ADD_INTE -> {messID = 14
                            
}
}
                    if (messID > 0)
                        SocketManager.GAME_SEND_Im_PACKET(player, "0" + messID.toString() + ";" + number)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    return true
                }
                
}
9 -> {try {
                    var sID: Int = (args.split(",", limit = 2)[0]).toInt()
                    var mapId: Int = (args.split(",", limit = 2)[1]).toInt()
                    if (World.world.getSort(sID) == null)
                        return true
                    if (player.curMap.id != mapId)
                        return true
                    player.learnSpell(sID, 1, true, true, true)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
10 -> {try {
                    var min: Int = (args.split(",", limit = 2)[0]).toInt()
                    var max: Int = (args.split(",", limit = 2)[1]).toInt()
                    if (max == 0)
                        max = min
                    var vale: Int = Formulas.getRandomValue(min, max)
                    if (target != null) {
                        if (target.curPdv + vale > target.maxPdv)
                            vale = target.maxPdv - target.curPdv
                        target.setPdv(target.curPdv + vale)
                        SocketManager.GAME_SEND_STATS_PACKET(target)
                    } else {
                        if (player.curPdv + vale > player.maxPdv)
                            vale = player.maxPdv - player.curPdv
                        player.setPdv(player.curPdv + vale)
                        SocketManager.GAME_SEND_STATS_PACKET(player)
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
11 -> {try {
                    var newAlign: Byte = (args.split(",", limit = 2)[0]).toByte()
                    var replace: Boolean = (args.split(",", limit = 2)[1]).toInt() == 1
                    if (player.alignment != Constant.ALIGNEMENT_NEUTRE && !replace)
                        return true
                    player.modifAlignement(newAlign.toInt())
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
12 -> {try {
                    var delObj: Boolean = args.split(",")[0].equals("true")
                    var inArena: Boolean = args.split(",")[1].equals("true")

                    if (inArena && !SoulStone.isInArenaMap(player.curMap.id))
                        return true

                    var pierrePleine: SoulStone = (World.world.getGameObject(itemID) as SoulStone)

                    var groupData: String = pierrePleine.parseGroupData()
                    var condition: String = "MiS = " + player.id //Condition pour que le groupe ne soit lan�able que par le personnage qui � utiliser l'objet
                    player.curMap.spawnNewGroup(true, player.curCell.getId(), groupData, condition)

                    if (delObj)
                        player.removeItem(itemID, 1, true, true)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
13 -> {if (player.level <= 30 || Config.resetLimit) {
                    try {
                        
                        if (player.statsParcho.getEffect(125) != 0 || player.statsParcho.getEffect(124) != 0 || player.statsParcho.getEffect(118) != 0
                                || player.statsParcho.getEffect(119) != 0 || player.statsParcho.getEffect(126) != 0 || player.statsParcho.getEffect(123) != 0) {
                            player.stats.addOneStat(125, -player.stats.getEffect(125) + player.statsParcho.getEffect(125))
                            player.stats.addOneStat(124, -player.stats.getEffect(124) + player.statsParcho.getEffect(124))
                            player.stats.addOneStat(118, -player.stats.getEffect(118) + player.statsParcho.getEffect(118))
                            player.stats.addOneStat(123, -player.stats.getEffect(123) + player.statsParcho.getEffect(123))
                            player.stats.addOneStat(119, -player.stats.getEffect(119) + player.statsParcho.getEffect(119))
                            player.stats.addOneStat(126, -player.stats.getEffect(126) + player.statsParcho.getEffect(126))
                            player.addCapital((player.level - 1) * 5 - player.capital)
                            SocketManager.GAME_SEND_STATS_PACKET(player)
                            return true
                        } else if (player.stats.getEffect(125) == 101 && player.stats.getEffect(124) == 101 && player.stats.getEffect(118) == 101
                                && player.stats.getEffect(123) == 101 && player.stats.getEffect(119) == 101 && player.stats.getEffect(126) == 101) {
                            player.stats.addOneStat(125, -player.stats.getEffect(125) + 101)
                            player.stats.addOneStat(124, -player.stats.getEffect(124) + 101)
                            player.stats.addOneStat(118, -player.stats.getEffect(118) + 101)
                            player.stats.addOneStat(123, -player.stats.getEffect(123) + 101)
                            player.stats.addOneStat(119, -player.stats.getEffect(119) + 101)
                            player.stats.addOneStat(126, -player.stats.getEffect(126) + 101)
                            player.statsParcho.effects.clear()
                            player.statsParcho.addOneStat(125, 101)
                            player.statsParcho.addOneStat(124, 101)
                            player.statsParcho.addOneStat(118, 101)
                            player.statsParcho.addOneStat(123, 101)
                            player.statsParcho.addOneStat(119, 101)
                            player.statsParcho.addOneStat(126, 101)

                            player.addCapital((player.level - 1) * 5 - player.capital)
                            SocketManager.GAME_SEND_STATS_PACKET(player)
                            return true
                        }
                    player.stats.addOneStat(125, -player.stats.getEffect(125))
                    player.stats.addOneStat(124, -player.stats.getEffect(124))
                    player.stats.addOneStat(118, -player.stats.getEffect(118))
                    player.stats.addOneStat(123, -player.stats.getEffect(123))
                    player.stats.addOneStat(119, -player.stats.getEffect(119))
                    player.stats.addOneStat(126, -player.stats.getEffect(126))
                    player.addCapital((player.level - 1) * 5 - player.capital)
                    player.statsParcho.effects.clear()
                    SocketManager.GAME_SEND_STATS_PACKET(player)
                } catch (e: Exception) {
                        log.error("unexpected error", e)
                        GameServer.a()
                    }
                } else {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.error.reset.lvl.30"))
                }
                
}
14 -> {if (player.level <= 30 || Config.resetLimit) {
                    player.exchangeAction = ExchangeAction(ExchangeAction.FORGETTING_SPELL, 0)
                    SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', player)
                } else {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.error.reset.lvl.30"))
                }
                
}
15 -> {try {
                    var newMapID: Short = (args.split(",")[0]).toShort()
                    var newCellID: Int = (args.split(",")[1]).toInt()
                    var ObjetNeed: Int = (args.split(",")[2]).toInt()
                    var MapNeed: Int = (args.split(",")[3]).toInt()
                    if (ObjetNeed == 0) {
                        //T�l�portation sans objets
                        player.teleport(newMapID.toInt(), newCellID)
                    } else if (ObjetNeed > 0) {
                        if (MapNeed == 0) {
                            //T�l�portation sans map
                            player.teleport(newMapID.toInt(), newCellID)
                        } else if (MapNeed > 0) {
                            if (player.hasItemTemplate(ObjetNeed, 1, false)
                                    && player.curMap.id == MapNeed) {
                                //Le perso a l'item
                                //Le perso est sur la bonne map
                                //On t�l�porte, on supprime apr�s
                                player.teleport(newMapID.toInt(), newCellID)
                                player.removeItemByTemplateId(ObjetNeed, 1, false)
                                SocketManager.GAME_SEND_Ow_PACKET(player)
                            } else if (player.curMap.id != MapNeed) {
                                //Le perso n'est pas sur la bonne map
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.map"), "009900")
                            } else {
                                //Le perso ne poss�de pas l'item
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.nokey"), "009900")
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
16 -> {try {
                    var newMapID: Short = (args.split(",")[0]).toShort()
                    var newCellID: Int = (args.split(",")[1]).toInt()
                    var ObjetNeed: Int = (args.split(",")[2]).toInt()
                    var MapNeed: Int = (args.split(",")[3]).toInt()
                    if (ObjetNeed == 0) {
                        //T�l�portation sans objets
                        player.teleport(newMapID.toInt(), newCellID)
                    } else if (ObjetNeed > 0) {
                        if (MapNeed == 0) {
                            //T�l�portation sans map
                            player.teleport(newMapID.toInt(), newCellID)
                        } else if (MapNeed > 0) {
                            if (player.hasItemTemplate(ObjetNeed, 1, false)
                                    && player.curMap.id == MapNeed) {
                                //Le perso a l'item
                                //Le perso est sur la bonne map
                                //On t�l�porte
                                player.teleport(newMapID.toInt(), newCellID)
                                SocketManager.GAME_SEND_Ow_PACKET(player)
                            } else if (player.curMap.id != MapNeed) {
                                //Le perso n'est pas sur la bonne map
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.map"), "009900")
                            } else {
                                //Le perso ne poss�de pas l'item
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.nokey"), "009900")
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
17 -> {try {
                    var JobID: Int = (args.split(",")[0]).toInt()
                    var XpValue: Long = (args.split(",")[1]).toLong()
                    if (player.getMetierByID(JobID) != null) {
                        player.getMetierByID(JobID)!!.addXp(player, XpValue)
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
18 -> {if (World.world.houseManager.alreadyHaveHouse(player))//Si il a une maison
                {
                    var obj2: GameObject? = World.world.getGameObject(itemID)
                    if (player.hasItemTemplate(obj2!!.template!!.id, 1, false)) {
                        player.removeItemByTemplateId(obj2!!.template!!.id, 1, false)
                        var h: House? = World.world.houseManager.getHouseByPerso(player)
                        if (h == null)
                            return true
                        player.teleport(h.mapId, h.cellId)
                    }
                }
                
}
19 -> {SocketManager.GAME_SEND_GUILDHOUSE_PACKET(player)
                
}
20 -> {try {
                    var pts: Int = args.toInt()
                    if (pts < 1)
                        return true
                    player.addSpellPoint(pts)
                    SocketManager.GAME_SEND_STATS_PACKET(player)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
21 -> {try {
                    var energyMin: Int = (args.split(",", limit = 2)[0]).toInt()
                    var energyMax: Int = (args.split(",", limit = 2)[1]).toInt()
                    if (energyMax == 0)
                        energyMax = energyMin
                    var vale: Int = Formulas.getRandomValue(energyMin, energyMax)
                    var EnergyTotal: Int = player.energy + vale
                    if (EnergyTotal > 10000)
                        EnergyTotal = 10000
                    player.energy = EnergyTotal
                    SocketManager.GAME_SEND_STATS_PACKET(player)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
22 -> {try {
                    var XpAdd: Long = args.toLong()
                    if (XpAdd < 1)
                        return true

                    var TotalXp: Long = player.exp + XpAdd
                    player.exp = TotalXp
                    SocketManager.GAME_SEND_STATS_PACKET(player)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
23 -> {var Job: Int = (args.split(",", limit = 2)[0]).toInt()
                var mapId: Int = (args.split(",", limit = 2)[1]).toInt()
                if (player.curMap.id != mapId)
                    return true
                if (Job < 1)
                    return true
                var m2: JobStat = player.getMetierByID(Job)!!
                if (m2 == null)
                    return true
                player.unlearnJob(m2!!.id)
                
}
24 -> {try {
                    var morphID: Int = args.toInt()
                    if (morphID < 0)
                        return true
                    player.gfxId = morphID
                    SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.curMap, player.id)
                    SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(player.curMap, player)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
25 -> {var UnMorphID: Int = player.classe * 10 + player.sexe
                player.gfxId = UnMorphID
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.curMap, player.id)
                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(player.curMap, player)
                
}
26 -> {SocketManager.GAME_SEND_GUILDENCLO_PACKET(player)
                
}
27 -> {var ValidMobGroup: String = ""
                if (player.fight != null)
                    return true
                try {
                    var mapId1: Int = (args.split(":", limit = 2)[1]).toInt()
                    if (player.curMap.id != mapId1)
                        return true
                    for (MobAndLevel in  args.split(":", limit = 2)[0].splitJ("|")) {
                        var monsterID: Int = -1
                        var monsterLevel: Int = -1
                        var MobOrLevel: List<String> = MobAndLevel.split(",")
                        monsterID = MobOrLevel[0].toInt()
                        monsterLevel = MobOrLevel[1].toInt()

                        if (World.world.getMonstre(monsterID) == null
                                || World.world.getMonstre(monsterID)!!.getGradeByLevel(monsterLevel) == null) {
                            continue
                        }
                        ValidMobGroup += monsterID.toString() + "," + monsterLevel.toString() + "," + monsterLevel.toString() + ";"
                    }
                    if (ValidMobGroup.isEmpty())
                        return true
                    var group: MonsterGroup = MonsterGroup(player.curMap.nextObjectId, map, player.curCell.getId(), ValidMobGroup)
                    player.curMap.startFightVersusMonstres(player, group); // Si bug startfight, voir "//Respawn d'un groupe fix" dans fight.java
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
28 -> {try {
                    var sID: Int = args.toInt()
                    var AncLevel: Int = player.getSortStatBySortIfHas(sID)!!.level
                    if (player.getSortStatBySortIfHas(sID) == null)
                        return true
                    if (AncLevel <= 1)
                        return true
                    player.unlearnSpell(player, sID, 1, AncLevel, true, true)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
29 -> {var pKamas3: Long = player.kamas
                var payKamas: Int = player.level * player.level * 25

                if (pKamas3 >= payKamas) {
                    var pNewKamas3: Long = pKamas3 - payKamas
                    if (pNewKamas3 < 0)
                        pNewKamas3 = 0
                    var sID: Int = args.toInt()
                    var AncLevel: Int = player.getSortStatBySortIfHas(sID)!!.level
                    if (player.getSortStatBySortIfHas(sID) == null)
                        return true
                    if (AncLevel <= 1)
                        return true
                    player.unlearnSpell(player, sID, 1, AncLevel, true, true)
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.error.unlearnspell", payKamas))
                } else {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.error.nokamas"))
                    return true
                }
                
}
30 -> {var size: Int = args.toInt()
                player.size = size
                SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(player.curMap, player.id)
                SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(player.curMap, player)
                
}
31 -> {
}
33 -> {var posItem: Int = args.toInt()
                var itemPos: GameObject? = player.getObjetByPos(posItem)
                if (itemPos != null) {
                    itemPos.clearStats()
                    var maxStats: Stats = itemPos.generateNewStatsFromTemplate(itemPos.template!!.strTemplate, true)
                    itemPos.stats = maxStats
                    var idObjPos: Int = itemPos.guid
                    player.removeItem(itemID, 1, true, true)
                    SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(player, idObjPos)
                    SocketManager.GAME_SEND_OAKO_PACKET(player, itemPos)
                    SocketManager.GAME_SEND_STATS_PACKET(player)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, player.getLang().trans("other.action.apply.JP.noitem"))
                }
                
}
35 -> {try {
                    if (player.curMap.id != 741
                            || !player.hasItemTemplate(10563, 1, false))
                        return true
                    player.removeItemByTemplateId(10563, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 10563 + "~" + 1)
                    player.stats.addOneStat(125, -player.stats.getEffect(125))
                    player.stats.addOneStat(124, -player.stats.getEffect(124))
                    player.stats.addOneStat(118, -player.stats.getEffect(118))
                    player.stats.addOneStat(123, -player.stats.getEffect(123))
                    player.stats.addOneStat(119, -player.stats.getEffect(119))
                    player.stats.addOneStat(126, -player.stats.getEffect(126))
                    player.addCapital((player.level - 1) * 5
                            - player.capital)
                    SocketManager.GAME_SEND_STATS_PACKET(player)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
38 -> {player.addStaticEmote(args.toInt())
                
}
50 -> {if (player.alignment == 0 || player.alignment == 3)
                    return true

                if (player.stalk != null && player.stalk!!.time == -2L) {
                    var xp: Long = Formulas.getXpStalk(player.level).toLong()
                    player.addXp(xp)
                    player.stalk = null;//On supprime la traque
                    SocketManager.GAME_SEND_STATS_PACKET(player)
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.traque.exp", xp), "000000")
                    return true
                } else if(player.stalk != null && player.stalk!!.target != null && player.stalk!!.target!!.online) {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.traque.nkill"), "000000")
                    return true
                } else {
                    player.stalk = null
                }

                if (player.stalk == null) {
                    var t: Stalk = Stalk(0, null)
                    player.stalk = t
                }
                if (player.stalk!!.time < System.currentTimeMillis() - 600000 || player.stalk!!.time == 0L) {
var tempP: Player? = null
                    var victimes: ArrayList<Player> = ArrayList<Player>()
                    for (victime in  World.world.onlinePlayers) {
                        if (victime == null || victime == player)
                            continue
						if (victime.getAccount()!!.currentIp.compareTo(player.getAccount()!!.currentIp) == 0)
                            continue
                        if (victime.alignment == player.alignment || victime.alignment == 0 || victime.alignment == 3 || !victime.showWings)
                            continue
                        if (((player.level + 20) >= victime.level) && ((player.level - 20) <= victime.level))
                            victimes.add(victime)
                    }
                    if (victimes.size == 0) {
                        SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.traque.notarget"), "000000")
                        player.stalk = null
                        return true
                    }
                    if (victimes.size == 1)
                        tempP = victimes[0]
                    else
                        tempP = victimes.get(Formulas.getRandomValue(0, victimes.size - 1))
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.traque.target", tempP.name), "000000")
                    player.stalk!!.target = (tempP)
                    player.stalk!!.time = (System.currentTimeMillis())
                    var `object`: GameObject? = player.getItemTemplate(10085)
                    if(`object` != null)
                        player!!.removeItem(`object`!!.guid, player.getNbItemTemplate(10085), true, true)
                    var T: ObjectTemplate? = World.world.getObjTemplate(10085)
                    var newObj: GameObject? = T!!.createNewItem(20, false)
                    newObj!!.addTxtStat(Constant.STATS_NAME_TRAQUE, tempP.name)
                    newObj!!.addTxtStat(Constant.STATS_ALIGNEMENT_TRAQUE, java.lang.Integer.toHexString(tempP.alignment).toString() + "")
                    newObj!!.addTxtStat(Constant.STATS_GRADE_TRAQUE, java.lang.Integer.toHexString(tempP.getGrade()).toString() + "")
                    newObj!!.addTxtStat(Constant.STATS_NIVEAU_TRAQUE, java.lang.Integer.toHexString(tempP.level).toString() + "")

                    if (player.addItem(newObj!!, true, false))
                        World.world.addGameObject(newObj)
                } else {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.traque.sleep"), "000000")
                }
                
}
53 -> {if (args == null)
                    return true
                if (World.world.getMap(args.toInt()) == null)
                    return true
                var CurMap: GameMap = World.world.getMap(args.toInt())
                if (player.fight == null) {
                    SocketManager.GAME_SEND_FLAG_PACKET(player, CurMap)
                }
                
}
60 -> {var ValidMobGroup1: String = ""
                if (player.fight != null)
                    return true
                try {
                    for (MobAndLevel in  args.splitJ("|")) {
                        var monsterID: Int = -1
                        var lvlMin: Int = -1
                        var lvlMax: Int = -1
                        var MobOrLevel: List<String> = MobAndLevel.split(",")
                        monsterID = MobOrLevel[0].toInt()
                        lvlMin = MobOrLevel[1].toInt()
                        lvlMax = MobOrLevel[2].toInt()

                        if (World.world.getMonstre(monsterID) == null
                                || World.world.getMonstre(monsterID)!!.getGradeByLevel(lvlMin) == null
                                || World.world.getMonstre(monsterID)!!.getGradeByLevel(lvlMax) == null) {
                            continue
                        }
                        ValidMobGroup1 += monsterID.toString() + "," + lvlMin.toString() + "," + lvlMax.toString() + ";"
                    }
                    if (ValidMobGroup1.isEmpty())
                        return true
                    var group: MonsterGroup = MonsterGroup(player.curMap.nextObjectId, map, player.curCell.getId(), ValidMobGroup1)
                    player.curMap.startFightVersusProtectors(player, group)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
70 -> {val park = (if (player.curMap.id != 10332) World.world.getMap(8743) else World.world.getMap(8848)).mountPark

                if (park != null) {
                    try {
                        park.getEtable().stream().filter(Objects::nonNull).forEach({ mount -> mount.checkBaby(player, park) })
                        park.getListOfRaising().stream().filter({ integer -> World.world.getMountById(integer) != null }).forEach({ integer -> World.world.getMountById(integer)!!.checkBaby(player, park) })
                    } catch (e: Exception) { log.error("unexpected error", e)
                 }
                    player.openMountPark(park)
                }
                
}
100 -> {if (player.hasItemTemplate(361, 100, false)) {
                    player.removeItemByTemplateId(361, 100, false)
                    var newObjAdded: GameObject? = World.world.getObjTemplate(9201)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                        World.world.addGameObject(newObjAdded)
                        player.addItem(newObjAdded!!, true)
                    }
                }
                
}
102 -> {var map0: GameMap = player.curMap
                if(map0.getCase(297)!!.players.isNotEmpty() && map0.getCase(282)!!.players.isNotEmpty()) {
                    if (map0.getCase(297)!!.players.size == 1 && map0.getCase(282)!!.players.size == 1) {
                        var boy: Player = map0.getCase(282)!!.players[0]
                        var girl: Player = map0.getCase(297)!!.players[0]
                        boy.blockMovement = true
                        girl.blockMovement = true
                        World.world.priestRequest(boy, girl, player)
                    }
                }
                
}
103 -> {if (player.kamas < 50000) {
                    return true
                } else {
                    player.kamas = player.kamas - 50000
                    if (player.isOnline)
                        SocketManager.GAME_SEND_STATS_PACKET(player)
                    var wife: Player? = World.world.getPlayer(player.wife)
                    wife!!.Divorce()
                    player.Divorce()
                }
                
}
104 -> {if (player.curMap.id != 10257)
                    return true

                var arrays: ArrayList<Couple<Short,Int>> = ArrayList()
                for (i in  args.splitJ(";"))
                    arrays.add(Couple((i.split(",")[0]).toShort(), (i.split(",")[1]).toInt()))

                var couple: Couple<Short,Int> = arrays[Formulas.random.nextInt(arrays.size)]
                SocketManager.GAME_SEND_GA_PACKET(player.getGameClient()!!, "", "2", player.id.toString() + "", "6")
                player.teleport(couple.first.toInt(), couple.second)
                
}
105 -> {if (!player.hasItemTemplate(10563, 1, false)) {
                    player.sendMessage(player.getLang().trans("other.action.restat.carac"))
                    return true
                } else {
                    player.removeItemByTemplateId(10563, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 10563 + "~" + 1)
                }

                if (player.statsParcho.getEffect(125) != 0
                        || player.statsParcho.getEffect(124) != 0
                        || player.statsParcho.getEffect(118) != 0
                        || player.statsParcho.getEffect(119) != 0
                        || player.statsParcho.getEffect(126) != 0
                        || player.stats.getEffect(123) != 0) {
                    player.stats.addOneStat(125, -player.stats.getEffect(125) + player.statsParcho.getEffect(125))
                    player.stats.addOneStat(124, -player.stats.getEffect(124) + player.statsParcho.getEffect(124))
                    player.stats.addOneStat(118, -player.stats.getEffect(118) + player.statsParcho.getEffect(118))
                    player.stats.addOneStat(123, -player.stats.getEffect(123) + player.statsParcho.getEffect(123))
                    player.stats.addOneStat(119, -player.stats.getEffect(119) + player.statsParcho.getEffect(119))
                    player.stats.addOneStat(126, -player.stats.getEffect(126) + player.statsParcho.getEffect(126))
                    player.addCapital((player.level - 1) * 5
                            - player.capital)
                } else if (player.stats.getEffect(125) == 101
                        && player.stats.getEffect(124) == 101
                        && player.stats.getEffect(118) == 101
                        && player.stats.getEffect(123) == 101
                        && player.stats.getEffect(119) == 101
                        && player.stats.getEffect(126) == 101) {
                    player.stats.addOneStat(125, -player.stats.getEffect(125) + 101)
                    player.stats.addOneStat(124, -player.stats.getEffect(124) + 101)
                    player.stats.addOneStat(118, -player.stats.getEffect(118) + 101)
                    player.stats.addOneStat(123, -player.stats.getEffect(123) + 101)
                    player.stats.addOneStat(119, -player.stats.getEffect(119) + 101)
                    player.stats.addOneStat(126, -player.stats.getEffect(126) + 101)

                    player.statsParcho.addOneStat(125, 101)
                    player.statsParcho.addOneStat(124, 101)
                    player.statsParcho.addOneStat(118, 101)
                    player.statsParcho.addOneStat(123, 101)
                    player.statsParcho.addOneStat(119, 101)
                    player.statsParcho.addOneStat(126, 101)

                    player.addCapital((player.level - 1) * 5
                            - player.capital)
                } else {
                    player.sendMessage(player.getLang().trans("other.action.restat.carac.secondskill"))
                    return true
                }

                SocketManager.GAME_SEND_STATS_PACKET(player)
                player.sendMessage(player.getLang().trans("other.action.restat.ok"))
                
}
106 -> {when (this.args) {
"1" -> {if(player.hasItemTemplate(15004, 1, false)) {
                            player.removeItemByTemplateId(15004, 1, false)
                            player.exchangeAction = ExchangeAction(ExchangeAction.FORGETTING_SPELL, 0)
                            SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', player)
                        }
                        
}
"2" -> {if(player.hasItemTemplate(15006, 1, false)) {
                            player.removeItemByTemplateId(15006, 1, false)
                            player.stats.addOneStat(125, -player.stats.getEffect(125))
                            player.stats.addOneStat(124, -player.stats.getEffect(124))
                            player.stats.addOneStat(118, -player.stats.getEffect(118))
                            player.stats.addOneStat(123, -player.stats.getEffect(123))
                            player.stats.addOneStat(119, -player.stats.getEffect(119))
                            player.stats.addOneStat(126, -player.stats.getEffect(126))
                            player.addCapital((player.level - 1) * 5 - player.capital)
                            player.statsParcho.effects.clear()
                            SocketManager.GAME_SEND_STATS_PACKET(player)
                        }
                        
}
"3" -> {if(player.hasItemTemplate(15005, 1, false)) {
                            player.removeItemByTemplateId(15005, 1, false)
                            if (player.statsParcho.getEffect(125) != 0 || player.statsParcho.getEffect(124) != 0 || player.statsParcho.getEffect(118) != 0
                                    || player.statsParcho.getEffect(119) != 0 || player.statsParcho.getEffect(126) != 0 || player.statsParcho.getEffect(123) != 0) {
                                player.stats.addOneStat(125, -player.stats.getEffect(125) + player.statsParcho.getEffect(125))
                                player.stats.addOneStat(124, -player.stats.getEffect(124) + player.statsParcho.getEffect(124))
                                player.stats.addOneStat(118, -player.stats.getEffect(118) + player.statsParcho.getEffect(118))
                                player.stats.addOneStat(123, -player.stats.getEffect(123) + player.statsParcho.getEffect(123))
                                player.stats.addOneStat(119, -player.stats.getEffect(119) + player.statsParcho.getEffect(119))
                                player.stats.addOneStat(126, -player.stats.getEffect(126) + player.statsParcho.getEffect(126))
                                player.addCapital((player.level - 1) * 5 - player.capital)
                                SocketManager.GAME_SEND_STATS_PACKET(player)
                            } else if (player.stats.getEffect(125) == 101 && player.stats.getEffect(124) == 101 && player.stats.getEffect(118) == 101
                                    && player.stats.getEffect(123) == 101 && player.stats.getEffect(119) == 101 && player.stats.getEffect(126) == 101) {
                                player.stats.addOneStat(125, -player.stats.getEffect(125) + 101)
                                player.stats.addOneStat(124, -player.stats.getEffect(124) + 101)
                                player.stats.addOneStat(118, -player.stats.getEffect(118) + 101)
                                player.stats.addOneStat(123, -player.stats.getEffect(123) + 101)
                                player.stats.addOneStat(119, -player.stats.getEffect(119) + 101)
                                player.stats.addOneStat(126, -player.stats.getEffect(126) + 101)
                                player.statsParcho.effects.clear()
                                player.statsParcho.addOneStat(125, 101)
                                player.statsParcho.addOneStat(124, 101)
                                player.statsParcho.addOneStat(118, 101)
                                player.statsParcho.addOneStat(123, 101)
                                player.statsParcho.addOneStat(119, 101)
                                player.statsParcho.addOneStat(126, 101)
                                player.addCapital((player.level - 1) * 5 - player.capital)
                                SocketManager.GAME_SEND_STATS_PACKET(player)
                            } else {
                                player.removeItemByTemplateId(15000, 500, false)
                                player.stats.addOneStat(125, -player.stats.getEffect(125))
                                player.stats.addOneStat(124, -player.stats.getEffect(124))
                                player.stats.addOneStat(118, -player.stats.getEffect(118))
                                player.stats.addOneStat(123, -player.stats.getEffect(123))
                                player.stats.addOneStat(119, -player.stats.getEffect(119))
                                player.stats.addOneStat(126, -player.stats.getEffect(126))
                                player.addCapital((player.level - 1) * 5 - player.capital)
                                player.statsParcho.effects.clear()
                                SocketManager.GAME_SEND_STATS_PACKET(player)
                            }
                        }
                        
}
}
                
}
116 -> {var EPO: GameObject? = World.world.getGameObject(itemID)
                if (EPO == null)
                    return true
                var pets: GameObject? = player.getObjetByPos(Constant.ITEM_POS_FAMILIER)
                if (pets == null)
                    return true
                var MyPets: PetEntry? = World.world.getPetsEntry(pets!!.guid)
                if (MyPets == null)
                    return true
                if (EPO.template!!.conditions!!.contains(pets.template!!.id.toString() + ""))
                    MyPets!!.giveEpo(player)
                
}
170 -> {try {
                    var title1: Byte = args.toByte()
                    var target: Player? = World.world.getPlayerByName(player.name)
                    target!!.setCurrentTitle(title1.toInt())
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.title"))
                    SocketManager.GAME_SEND_STATS_PACKET(player)
                    ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(player)
                    if (target!!.fight == null)
                        SocketManager.GAME_SEND_ALTER_GM_PACKET(player.curMap, player)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
171 -> {var type2: Short = ((args.split(",")[0]).toInt().toShort())
                var mapId2: Int = (args.split(",")[1]).toInt()
                if (player.alignment > 0)
                    return true
                if (type2.toInt() == 1 && (player.curMap.id == mapId2)) {
                    //if (player.hasItemTemplate(42, 10, false)) {
                    //    player.removeByTemplateID(42, 10);
                    //    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 10 + "~" + 42);
                        player.modifAlignement(1)
                    //}
                }
                if (type2.toInt() == 2 && (player.curMap.id == mapId2)) {
                    //if (player.hasItemTemplate(95, 10, false)) {
                    //    player.removeByTemplateID(95, 10);
                    //    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 10 + "~" + 95);
                        player.modifAlignement(2)
                    //}
                }
                
}
172 -> {var mapId4: Int = args.toInt()
                if (player.curMap.id != mapId4)
                    return true
                if (player.totalJobBasic() > 2)//On compte les m�tiers d�ja acquis si c'est sup�rieur a 2 on ignore
                {
                    SocketManager.GAME_SEND_Im_PACKET(player, "19")
                    return true
                }
                if (player.hasItemTemplate(459, 20, false)
                        && player.hasItemTemplate(7657, 15, false)) {
                    player.removeItemByTemplateId(459, 20, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 459 + "~" + 20)
                    player.removeItemByTemplateId(7657, 15, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 7657 + "~" + 15)
                    player.learnJob(World.world.getMetier(65)!!)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                    return true
                }
                
}
200 -> {var k: Long = player.kamas
                var cost: Int = if (player.curMap.id == 9520) 100 else 200
                if(k > cost) {
                    player.addKamas(-cost.toLong())
                    SocketManager.GAME_SEND_STATS_PACKET(player)
                    SocketManager.GAME_SEND_Im_PACKET(player, "046;" + 100)
                    if(cost == 100) player.teleport(9541, 407)
                    else player.teleport(9520, 282)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "182")
                }
                
}
219 -> {if (player.curMap.id != 1780)
                    return true
                var type11: Int = args.toInt()
                if (type11 == 1) {
                    var newObjAdded: GameObject? = World.world.getObjTemplate(970)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                        World.world.addGameObject(newObjAdded)
                        player.addItem(newObjAdded!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 970)
                    player.teleport(844, 212)
                } else if (type11 == 2) {
                    var newObjAdded: GameObject? = World.world.getObjTemplate(969)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                        World.world.addGameObject(newObjAdded)
                        player.addItem(newObjAdded!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 969)
                    player.teleport(844, 212)
                } else if (type11 == 3) {
                    var newObjAdded: GameObject? = World.world.getObjTemplate(971)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                        World.world.addGameObject(newObjAdded)
                        player.addItem(newObjAdded!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 971)
                    player.teleport(844, 212)
                }
                
}
220 -> {try {
                    var remove0: String = args.split(";")[0]
                    var add0: String = args.split(";")[1]
                    var add1: String = args.split(";")[4]
                    var obj0: Int = (remove0.split(",")[0]).toInt()
                    var qua0: Int = (remove0.split(",")[1]).toInt()
                    var newObj1: Int = (add0.split(",")[0]).toInt()
                    var newQua1: Int = (add0.split(",")[1]).toInt()
                    var newObj2: Int = (add1.split(",")[0]).toInt()
                    var newQua2: Int = (add1.split(",")[1]).toInt()
                    if (player.hasItemTemplate(obj0, qua0, false)) {
                        player.removeItemByTemplateId(obj0, qua0, false)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua0.toString() + "~" + obj0)
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + newQua1.toString() + "~" + newObj1)
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + newQua2.toString() + "~" + newObj2)
                        var newObjAdded: GameObject? = World.world.getObjTemplate(newObj1)!!.createNewItem(newQua1, false)
                        if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                            World.world.addGameObject(newObjAdded)
                            player.addItem(newObjAdded!!, true)
                        }
                        var newObjAdded1: GameObject? = World.world.getObjTemplate(newObj2)!!.createNewItem(newQua2, false)
                        if (!player.addObjetSimiler(newObjAdded1!!, true, -1)) {
                            World.world.addGameObject(newObjAdded1)
                            player.addItem(newObjAdded1!!, true)
                        }
                    } else {
                        SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                return true
}
221 -> {try {
                    var remove0: String = args.split(";")[0]
                    var remove1: String = args.split(";")[1]
                    var add: String = args.split(";")[4]
                    var obj0: Int = (remove0.split(",")[0]).toInt()
                    var qua0: Int = (remove0.split(",")[1]).toInt()
                    var obj1: Int = (remove1.split(",")[0]).toInt()
                    var qua1: Int = (remove1.split(",")[1]).toInt()
                    var newObj1: Int = (add.split(",")[0]).toInt()
                    var newQua1: Int = (add.split(",")[1]).toInt()
                    if (player.hasItemTemplate(obj0, qua0, false)
                            && player.hasItemTemplate(obj1, qua1, false)) {
                        player.removeItemByTemplateId(obj0, qua0, false)
                        player.removeItemByTemplateId(obj1, qua1, false)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua0.toString() + "~" + obj0)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua1.toString() + "~" + obj1)
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + newQua1.toString() + "~" + newObj1)
                        var newObjAdded: GameObject? = World.world.getObjTemplate(newObj1)!!.createNewItem(newQua1, false)
                        if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                            World.world.addGameObject(newObjAdded)
                            player.addItem(newObjAdded!!, true)
                        }
                    } else {
                        SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                return true
}
222 -> {try {
                    var remove0: String = args.split(";")[0]
                    var remove1: String = args.split(";")[1]
                    var remove2: String = args.split(";")[2]
                    var remove3: String = args.split(";")[3]
                    var add: String = args.split(";")[4]
                    var verifMapId: Int = (args.split(";")[5]).toInt()
                    var obj0: Int = (remove0.split(",")[0]).toInt()
                    var qua0: Int = (remove0.split(",")[1]).toInt()
                    var obj1: Int = (remove1.split(",")[0]).toInt()
                    var qua1: Int = (remove1.split(",")[1]).toInt()
                    var obj2: Int = (remove2.split(",")[0]).toInt()
                    var qua2: Int = (remove2.split(",")[1]).toInt()
                    var obj3: Int = (remove3.split(",")[0]).toInt()
                    var qua3: Int = (remove3.split(",")[1]).toInt()
                    var mapID: Int = (add.split(",")[0]).toInt()
                    var cellID: Int = (add.split(",")[1]).toInt()

                    if (player.hasItemTemplate(obj0, qua0, false)
                            && player.hasItemTemplate(obj1, qua1, false)
                            && player.hasItemTemplate(obj2, qua2, false)
                            && player.hasItemTemplate(obj3, qua3, false)) {
                        player.removeItemByTemplateId(obj0, qua0, false)
                        player.removeItemByTemplateId(obj1, qua1, false)
                        player.removeItemByTemplateId(obj2, qua2, false)
                        player.removeItemByTemplateId(obj3, qua3, false)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua0.toString() + "~" + obj0)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua1.toString() + "~" + obj1)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua2.toString() + "~" + obj2)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua3.toString() + "~" + obj3)
                        if (player.fight != null
                                || player.curMap.id != verifMapId)
                            return true
                        player.teleport(mapID, cellID)
                    } else {
                        SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                return true
}
223 -> {try {
                    var remove0: String = args.split(";")[0]
                    var remove1: String = args.split(";")[1]
                    var remove2: String = args.split(";")[2]
                    var remove3: String = args.split(";")[3]
                    var remove4: String = args.split(";")[4]
                    var add: String = args.split(";")[5]
                    var obj0: Int = (remove0.split(",")[0]).toInt()
                    var qua0: Int = (remove0.split(",")[1]).toInt()
                    var obj1: Int = (remove1.split(",")[0]).toInt()
                    var qua1: Int = (remove1.split(",")[1]).toInt()
                    var obj2: Int = (remove2.split(",")[0]).toInt()
                    var qua2: Int = (remove2.split(",")[1]).toInt()
                    var obj3: Int = (remove3.split(",")[0]).toInt()
                    var qua3: Int = (remove3.split(",")[1]).toInt()
                    var obj4: Int = (remove4.split(",")[0]).toInt()
                    var qua4: Int = (remove4.split(",")[1]).toInt()
                    var newItem: Int = (add.split(",")[0]).toInt()
                    var quaNewItem: Int = (add.split(",")[1]).toInt()
                    if (player.hasItemTemplate(obj0, qua0, false)
                            && player.hasItemTemplate(obj1, qua1, false)
                            && player.hasItemTemplate(obj2, qua2, false)
                            && player.hasItemTemplate(obj3, qua3, false)
                            && player.hasItemTemplate(obj4, qua4, false)) {
                        player.removeItemByTemplateId(obj0, qua0, false)
                        player.removeItemByTemplateId(obj1, qua1, false)
                        player.removeItemByTemplateId(obj2, qua2, false)
                        player.removeItemByTemplateId(obj3, qua3, false)
                        player.removeItemByTemplateId(obj4, qua4, false)
                        var newObjAdded: GameObject? = World.world.getObjTemplate(newItem)!!.createNewItem(quaNewItem, false)
                        if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                            World.world.addGameObject(newObjAdded)
                            player.addItem(newObjAdded!!, true)
                        }
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua0.toString() + "~" + obj0)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua1.toString() + "~" + obj1)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua2.toString() + "~" + obj2)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua3.toString() + "~" + obj3)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua4.toString() + "~" + obj4)
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + quaNewItem.toString() + "~" + newItem)
                    } else {
                        SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                return true
}
224 -> {try {
                    var remove0: String = args.split(";")[0]
                    var remove1: String = args.split(";")[1]
                    var remove2: String = args.split(";")[2]
                    var remove3: String = args.split(";")[3]
                    var add: String = args.split(";")[4]
                    var obj0: Int = (remove0.split(",")[0]).toInt()
                    var qua0: Int = (remove0.split(",")[1]).toInt()
                    var obj1: Int = (remove1.split(",")[0]).toInt()
                    var qua1: Int = (remove1.split(",")[1]).toInt()
                    var obj2: Int = (remove2.split(",")[0]).toInt()
                    var qua2: Int = (remove2.split(",")[1]).toInt()
                    var obj3: Int = (remove3.split(",")[0]).toInt()
                    var qua3: Int = (remove3.split(",")[1]).toInt()
                    var newItem: Int = (add.split(",")[0]).toInt()
                    var quaNewItem: Int = (add.split(",")[1]).toInt()
                    if (player.hasItemTemplate(obj0, qua0, false)
                            && player.hasItemTemplate(obj1, qua1, false)
                            && player.hasItemTemplate(obj2, qua2, false)
                            && player.hasItemTemplate(obj3, qua3, false)) {
                        player.removeItemByTemplateId(obj0, qua0, false)
                        player.removeItemByTemplateId(obj1, qua1, false)
                        player.removeItemByTemplateId(obj2, qua2, false)
                        player.removeItemByTemplateId(obj3, qua3, false)
                        var newObjAdded: GameObject? = World.world.getObjTemplate(newItem)!!.createNewItem(quaNewItem, false)
                        if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                            World.world.addGameObject(newObjAdded)
                            player.addItem(newObjAdded!!, true)
                        }
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua0.toString() + "~" + obj0)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua1.toString() + "~" + obj1)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua2.toString() + "~" + obj2)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua3.toString() + "~" + obj3)
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + quaNewItem.toString() + "~" + newItem)
                    } else {
                        SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                return true
}
225 -> {try {
                    var remove0: String = args.split(";")[0]
                    var remove1: String = args.split(";")[1]
                    var remove2: String = args.split(";")[2]
                    var add: String = args.split(";")[3]
                    var obj0: Int = (remove0.split(",")[0]).toInt()
                    var qua0: Int = (remove0.split(",")[1]).toInt()
                    var obj1: Int = (remove1.split(",")[0]).toInt()
                    var qua1: Int = (remove1.split(",")[1]).toInt()
                    var obj2: Int = (remove2.split(",")[0]).toInt()
                    var qua2: Int = (remove2.split(",")[1]).toInt()
                    var newItem: Int = (add.split(",")[0]).toInt()
                    var quaNewItem: Int = (add.split(",")[1]).toInt()
                    if (player.hasItemTemplate(obj0, qua0, false)
                            && player.hasItemTemplate(obj1, qua1, false)
                            && player.hasItemTemplate(obj2, qua2, false)) {
                        player.removeItemByTemplateId(obj0, qua0, false)
                        player.removeItemByTemplateId(obj1, qua1, false)
                        player.removeItemByTemplateId(obj2, qua2, false)
                        var newObjAdded: GameObject? = World.world.getObjTemplate(newItem)!!.createNewItem(quaNewItem, false)
                        if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                            World.world.addGameObject(newObjAdded)
                            player.addItem(newObjAdded!!, true)
                        }
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua0.toString() + "~" + obj0)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua1.toString() + "~" + obj1)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua2.toString() + "~" + obj2)
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + quaNewItem.toString() + "~" + newItem)
                    } else {
                        SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
226 -> {if (player.hasItemTemplate(1089, 1, false) && player.hasEquiped(1021)
                        && player.hasEquiped(1019)
                        && player.curMap.id == 1014) {
                    player.removeItemByTemplateId(1019, 1, false)
                    player.removeItemByTemplateId(1021, 1, false)
                    player.removeItemByTemplateId(1089, 1, false)
                    var newObj1: GameObject? = World.world.getObjTemplate(1020)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObj1!!, true, -1)) {
                        World.world.addGameObject(newObj1)
                        player.addItem(newObj1!!, true)
                    }
                    var newObj2: GameObject? = World.world.getObjTemplate(1022)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObj2!!, true, -1)) {
                        World.world.addGameObject(newObj2)
                        player.addItem(newObj2!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 1089)
                    SocketManager.GAME_SEND_GA_PACKET(player.getGameClient()!!, "", "2", player.id.toString() + "", "1")
                    player.teleport(437, 411)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                    return true
                }
                
}
227 -> {var pKamas: Long = player.kamas
                if (pKamas >= 500 && player.curMap.id == 167) {
                    var pNewKamas: Long = pKamas - 500
                    if (pNewKamas < 0)
                        pNewKamas = 0
                    player.kamas = pNewKamas
                    if (player.isOnline)
                        SocketManager.GAME_SEND_STATS_PACKET(player)
                    SocketManager.GAME_SEND_Im_PACKET(player, "046;" + 500)
                    SocketManager.GAME_SEND_GA_PACKET(player.getGameClient()!!, "", "2", player.id.toString() + "", "2")
                    player.teleport(833, 141)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "182")
                }
                
}
229 -> {mapId = Constant.getClassStatueMap(player.classe).toInt()
                var cell: Int = Constant.getClassStatueCell(player.classe)
                SocketManager.GAME_SEND_GA_PACKET(player.getGameClient()!!, "", "2", player.id.toString() + "", "7")
                player.teleport(mapId, cell)
                player.setSavePos(mapId ,cell)
                SocketManager.GAME_SEND_Im_PACKET(player, "06")
                
}
230 -> {try {
                    var pts: Int = args.toInt()
                    var ptsTotal: Long = player.getAccount()!!.points + pts
                    player.getAccount()!!.modPoints(pts.toLong())
                    if (player.isOnline)
                        SocketManager.GAME_SEND_STATS_PACKET(player)
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.shop", pts, ptsTotal))
                    return true
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
231 -> {try {
                    var remove: String = args.split(";")[0]
                    var add: String = args.split(";")[1]
                    var obj: Int = (remove.split(",")[0]).toInt()
                    var qua: Int = (remove.split(",")[1]).toInt()
                    var newItem: Int = (add.split(",")[0]).toInt()
                    var quaNewItem: Int = (add.split(",")[1]).toInt()
                    if (player.hasItemTemplate(obj, qua, false)) {
                        player.removeItemByTemplateId(obj, qua, false)
                        var newObjAdded: GameObject? = World.world.getObjTemplate(newItem)!!.createNewItem(quaNewItem, false)
                        if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                            World.world.addGameObject(newObjAdded)
                            player.addItem(newObjAdded!!, true)
                        }
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + qua.toString() + "~" + obj)
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + quaNewItem.toString() + "~" + newItem)
                    } else {
                        SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
232 -> {if (player.fight != null)
                    return true
                var ValidMobGroup2: String = ""
                var pMap: Int = player.curMap.id
                if (pMap == 10131 || pMap == 10132 || pMap == 10133
                        || pMap == 10134 || pMap == 10135 || pMap == 10136
                        || pMap == 10137 || pMap == 10138) {
                    try {
                        for (MobAndLevel in  args.splitJ("|")) {
                            var monsterID: Int = -1
                            var monsterLevel: Int = -1
                            var MobOrLevel: List<String> = MobAndLevel.split(",")
                            monsterID = MobOrLevel[0].toInt()
                            monsterLevel = MobOrLevel[1].toInt()

                            if (World.world.getMonstre(monsterID) == null
                                    || World.world.getMonstre(monsterID)!!.getGradeByLevel(monsterLevel) == null) {
                                continue
                            }
                            ValidMobGroup2 += monsterID.toString() + "," + monsterLevel.toString() + "," + monsterLevel.toString() + ";"
                        }
                        if (ValidMobGroup2.isEmpty())
                            return true
                        var group: MonsterGroup = MonsterGroup(player.curMap.nextObjectId, player.curMap, player.curCell.getId(), ValidMobGroup2)
                        player.curMap.startFightVersusMonstres(player, group);// Si bug startfight, voir "//Respawn d'un groupe fix" dans fight.java
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                        GameServer.a()
                    }
                } else {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.startfightversusmonster"))
                }
                
}
233 -> {try {
                    var tID: Int = (args.split(",")[0]).toInt()
                    var count: Int = (args.split(",")[1]).toInt()
                    var send: Boolean = true
                    if (args.splitJ(",").size > 2)
                        send = args.split(",")[2].equals("1")
                    var pMap2: Int = player.curMap.id
                    if (pMap2 == 10131 || pMap2 == 10132 || pMap2 == 10133
                            || pMap2 == 10134 || pMap2 == 10135
                            || pMap2 == 10136 || pMap2 == 10137
                            || pMap2 == 10138) {
                        //Si on ajoute
                        if (count > 0) {
                            var T: ObjectTemplate? = World.world.getObjTemplate(tID)
                            if (T == null)
                                return true
                            var O: GameObject? = T!!.createNewItem(count, false)
                            //Si retourne true, on l'ajoute au monde
                            if (player.addItem(O!!, true, false))
                                World.world.addGameObject(O)
                        } else {
                            player.removeItemByTemplateId(tID, -count, false)
                        }
                        //Si en ligne (normalement oui)
                        if (player.isOnline)//on envoie le packet qui indique l'ajout//retrait d'un item
                        {
                            SocketManager.GAME_SEND_Ow_PACKET(player)
                            if (send) {
                                if (count >= 0) {
                                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + count.toString() + "~" + tID)
                                } else if (count < 0) {
                                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + (-count).toString() + "~" + tID)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
234 -> {var IdObj: Int = (args.split(";")[0]).toInt()
                var MapId: Int = (args.split(";")[1]).toInt()
                if (player.curMap.id != MapId)
                    return true
                if (!player.hasItemTemplate(IdObj, 1, false)) {
                    var newObjAdded: GameObject? = World.world.getObjTemplate(IdObj)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                        World.world.addGameObject(newObjAdded)
                        player.addItem(newObjAdded!!, true)
                    }
                } else {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.objalereadyexit"))
                }
                
}
235 -> {if (player.curMap.id == 713) {
                    if (player.hasItemTemplate(757, 1, false)
                            && player.hasItemTemplate(368, 1, false)
                            && player.hasItemTemplate(369, 1, false)
                            && !player.hasItemTemplate(960, 1, false)) {
                        player.removeItemByTemplateId(757, 1, false)
                        player.removeItemByTemplateId(368, 1, false)
                        player.removeItemByTemplateId(369, 1, false)

                        var newObjAdded: GameObject? = World.world.getObjTemplate(960)!!.createNewItem(1, false)
                        if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                            World.world.addGameObject(newObjAdded)
                            player.addItem(newObjAdded!!, true)
                        }
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 757)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 368)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 369)
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 960)
                    } else {
                        SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                    }
                }
                
}
300 -> {if (player.curMap.id == 1559 && player.hasItemTemplate(973, 1, false)) {
                    player.removeItemByTemplateId(973, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 973)
                    player.learnSpell(370, 1, true, true, true)
                }
                
}
239 -> {player.exchangeAction = ExchangeAction(ExchangeAction.FORGETTING_SPELL, 0)
                SocketManager.GAME_SEND_FORGETSPELL_INTERFACE('+', player)
                
}
241 -> {if (player.kamas >= 10
                        && player.curMap.id == 6863) {
                    if (player.hasItemTemplate(6653, 1, false)) {
                        var date: String? = player.getItemTemplate(6653, 1)!!.txtStat[Constant.STATS_DATE]
                        try {
                            var timeStamp: Long = (date!!.split("#")[3]).toLong()

                            if (System.currentTimeMillis() - timeStamp <= 86400000) {
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.ticket.good"))
                                return true
                            } else {
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.ticket.outdated"))
                                player.removeItemByTemplateId(6653, 1, false)
                            }
                        } catch (e: Exception) {
                            SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.ticket.outdated"))
                            player.removeItemByTemplateId(6653, 1, false)
                        }
                    }
                    var rK: Long = player.kamas - 10
                    if (rK < 0)
                        rK = 0
                    player.kamas = rK
                    if (player.isOnline)
                        SocketManager.GAME_SEND_STATS_PACKET(player)
                    var OT: ObjectTemplate? = World.world.getObjTemplate(6653)
                    var obj: GameObject? = OT!!.createNewItem(1, false)
                    if (player.addItem(obj!!, true, false))//Si le joueur n'avait pas d'item similaire
                        World.world.addGameObject(obj)
                    obj!!.refreshStatsObjet("325#0#0#" + System.currentTimeMillis())
                    ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(player)
                    SocketManager.GAME_SEND_Ow_PACKET(player)
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 6653)
                }
                
}
450 -> {if (player.curMap.id == 1844
                        && player.kamas >= 5000
                        && player.hasItemTemplate(363, 5, false)) {
                    player.removeItemByTemplateId(363, 5, false)
                    var rK: Long = player.kamas - 5000
                    if (rK < 0)
                        rK = 0
                    player.kamas = rK
                    if (player.isOnline)
                        SocketManager.GAME_SEND_STATS_PACKET(player)
                    var newObjAdded: GameObject? = World.world.getObjTemplate(998)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                        World.world.addGameObject(newObjAdded)
                        player.addItem(newObjAdded!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 998)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 5 + "~" + 363)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                }
                
}
451 -> {if (player.kamas >= 200
                        && player.curMap.id == 436) {
                    var rK: Long = player.kamas - 200
                    if (rK < 0)
                        rK = 0
                    player.kamas = rK
                    if (player.isOnline)
                        SocketManager.GAME_SEND_STATS_PACKET(player)
                    var newObjAdded: GameObject? = World.world.getObjTemplate(1004)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                        World.world.addGameObject(newObjAdded)
                        player.addItem(newObjAdded!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 1004)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                }
                
}
452 -> {if (player.hasItemTemplate(1000, 6, false)
                        && player.hasItemTemplate(1003, 1, false)
                        && player.hasItemTemplate(1018, 10, false)
                        && player.hasItemTemplate(998, 1, false)
                        && player.hasItemTemplate(1002, 1, false)
                        && player.hasItemTemplate(999, 1, false)
                        && player.hasItemTemplate(1004, 4, false)
                        && player.hasItemTemplate(1001, 2, false)
                        && player.curMap.id == 437) {
                    player.removeItemByTemplateId(1000, 6, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 6 + "~" + 1000)
                    player.removeItemByTemplateId(1003, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 1003)
                    player.removeItemByTemplateId(1018, 10, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 10 + "~" + 1018)
                    player.removeItemByTemplateId(998, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 998)
                    player.removeItemByTemplateId(1002, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 1002)
                    player.removeItemByTemplateId(999, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 99)
                    player.removeItemByTemplateId(1004, 4, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 4 + "~" + 1004)
                    player.removeItemByTemplateId(1001, 2, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 2 + "~" + 1001)
                    var newObjAdded: GameObject? = World.world.getObjTemplate(6716)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                        World.world.addGameObject(newObjAdded)
                        player.addItem(newObjAdded!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 6716)
                    player.teleport(1701, 247)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                }
                
}
453 -> {if (player.hasItemTemplate(1010, 1, false)
                        && player.hasItemTemplate(1011, 1, false)
                        && player.hasItemTemplate(1012, 1, false)
                        && player.hasItemTemplate(1013, 1, false)
                        && player.curMap.id == 1714) {
                    player.removeItemByTemplateId(1010, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 1010)
                    player.removeItemByTemplateId(1011, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 1011)
                    player.removeItemByTemplateId(1012, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 1012)
                    player.removeItemByTemplateId(1013, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 1013)
                    player.teleport(1766, 332)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                }
                
}
454 -> {if (player.hasEquiped(1088) && player.curMap.id == 1764) {
                    player.teleport(1765, 226)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                }
                
}
455 -> {if (player.hasItemTemplate(1006, 1, false)
                        && player.hasItemTemplate(1007, 1, false)
                        && player.hasItemTemplate(1008, 1, false)
                        && player.hasItemTemplate(1009, 1, false)
                        && player.curMap.id == 1838) {
                    player.removeItemByTemplateId(1006, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 1006)
                    player.removeItemByTemplateId(1007, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 1007)
                    player.removeItemByTemplateId(1008, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 1008)
                    player.removeItemByTemplateId(1009, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 1009)
                    var newObjAdded: GameObject? = World.world.getObjTemplate(1086)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                        World.world.addGameObject(newObjAdded)
                        player.addItem(newObjAdded!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 1086)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "14|43")
                }
                
}
457 -> {if (player.kamas >= 1000
                        && player.curMap.id == 1014) {
                    player.kamas = player.kamas - 1000
                    var newObjAdded11: GameObject? = World.world.getObjTemplate(1089)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded11!!, true, -1)) {
                        World.world.addGameObject(newObjAdded11)
                        player.addItem(newObjAdded11!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 1089)
                    SocketManager.GAME_SEND_STATS_PACKET(player)
                }
                
}
500 -> {if (player.curMap.id != 2084)
                    return true
                player.teleport(1856, 226)
                var newObjAdded: GameObject? = World.world.getObjTemplate(1728)!!.createNewItem(1, false)
                if (!player.addObjetSimiler(newObjAdded!!, true, -1)) {
                    World.world.addGameObject(newObjAdded)
                    player.addItem(newObjAdded!!, true)
                }
                SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 1728)
                
}
501 -> {if (player.curMap.id != 9767)
                    return true
                player.teleport(9470, 198)
                var newObjAdded1: GameObject? = World.world.getObjTemplate(8000)!!.createNewItem(1, false)
                if (!player.addObjetSimiler(newObjAdded1!!, true, -1)) {
                    World.world.addGameObject(newObjAdded1)
                    player.addItem(newObjAdded1!!, true)
                }
                SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 8000)
                
}
502 -> {if (player.hasEquiped(969) && player.hasEquiped(970)
                        && player.hasEquiped(971)
                        && player.curMap.id == 1781) {
                    player.teleport(1783, 114)
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "114;")
                }
                
}
503 -> {if (player.curMap.id != 1795)
                    return true
                if (player.hasItemTemplate(969, 1, false)
                        && player.hasItemTemplate(970, 1, false)
                        && player.hasItemTemplate(971, 1, false)) {
                    player.removeItemByTemplateId(969, 1, false)
                    player.removeItemByTemplateId(970, 1, false)
                    player.removeItemByTemplateId(971, 1, false)
                    var newObjAdded11: GameObject? = World.world.getObjTemplate(972)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded11!!, true, -1)) {
                        World.world.addGameObject(newObjAdded11)
                        player.addItem(newObjAdded11!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 972)
                    player.teleport(1781, 227)
                }
                
}
504 -> {if (player.curMap.id == 9717) {
                    var type111: Int = 0
                    try {
                        type111 = args.toInt()
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                    }
                    if (type111 == 1) {
                        var newObjAdded11: GameObject? = World.world.getObjTemplate(7890)!!.createNewItem(1, false)
                        if (!player.addObjetSimiler(newObjAdded11!!, true, -1)) {
                            World.world.addGameObject(newObjAdded11)
                            player.addItem(newObjAdded11!!, true)
                        }
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 7890)
                    }
                    if (type111 == 2) {
                        var newObjAdded11: GameObject? = World.world.getObjTemplate(7889)!!.createNewItem(1, false)
                        if (!player.addObjetSimiler(newObjAdded11!!, true, -1)) {
                            World.world.addGameObject(newObjAdded11)
                            player.addItem(newObjAdded11!!, true)
                        }
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 7889)
                    }
                    if (type111 == 3) {
                        var newObjAdded11: GameObject? = World.world.getObjTemplate(7888)!!.createNewItem(1, false)
                        if (!player.addObjetSimiler(newObjAdded11!!, true, -1)) {
                            World.world.addGameObject(newObjAdded11)
                            player.addItem(newObjAdded11!!, true)
                        }
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 7888)
                    }
                    if (type111 == 4) {
                        var newObjAdded11: GameObject? = World.world.getObjTemplate(7887)!!.createNewItem(1, false)
                        if (!player.addObjetSimiler(newObjAdded11!!, true, -1)) {
                            World.world.addGameObject(newObjAdded11)
                            player.addItem(newObjAdded11!!, true)
                        }
                        SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 7887)
                    }
                    player.teleport(8905, 431)
                }
                
}
505 -> {if (player.curMap.id == 9717) {
                    if (player.hasItemTemplate(7904, 50, false)
                            && player.hasItemTemplate(7903, 50, false)) {
                        player.removeItemByTemplateId(7904, 50, false)
                        player.removeItemByTemplateId(7903, 50, false)
                        player.learnSpell(414, 1, true, true, true)
                    }
                }
                
}
506 -> {if (player.curMap.id == 8905
                        && player.curCell.getId() == 213) {
                    if (player.hasItemTemplate(7908, 1, false)) {
                        player.removeItemByTemplateId(7908, 1, false)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 7908)
                        player.teleport(8950, 408)
                    } else {
                        SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.nokey"))
                    }
                } else {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.nopnj"))
                }
                
}
507 -> {var type3: Int = 0
                if (player.curMap.id == 6823) {
                    try {
                        type3 = args.toInt()
                        if (type3 < 6) {
                            if (player.hasItemTemplate(2433, 15, false)
                                    && player.hasItemTemplate(2432, 15, false)
                                    && player.hasItemTemplate(2431, 15, false)
                                    && player.hasItemTemplate(2430, 15, false)) {
                                type3 = 6
                            } else if (player.hasItemTemplate(2433, 10, false)
                                    && player.hasItemTemplate(2432, 10, false)
                                    && player.hasItemTemplate(2431, 10, false)
                                    && player.hasItemTemplate(2430, 10, false)) {
                                type3 = 5
                            }
                        }
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                    }
                    when (type3) {
1 -> {if (player.hasItemTemplate(2433, 10, false)) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2433).toString() + "~" + 2433)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2432).toString() + "~" + 2432)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2431).toString() + "~" + 2431)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2430).toString() + "~" + 2430)
                                player.removeItemByTemplateId(2430, player.getNbItemTemplate(2430), false)
                                player.removeItemByTemplateId(2431, player.getNbItemTemplate(2431), false)
                                player.removeItemByTemplateId(2432, player.getNbItemTemplate(2432), false)
                                player.removeItemByTemplateId(2433, player.getNbItemTemplate(2433), false)
                                player.teleport(6834, 422)
                            } else {
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.noflaque.menthe"))
                            }
                            
}
2 -> {if (player.hasItemTemplate(2432, 10, false)) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2433).toString() + "~" + 2433)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2432).toString() + "~" + 2432)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2431).toString() + "~" + 2431)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2430).toString() + "~" + 2430)
                                player.removeItemByTemplateId(2430, player.getNbItemTemplate(2430), false)
                                player.removeItemByTemplateId(2431, player.getNbItemTemplate(2431), false)
                                player.removeItemByTemplateId(2432, player.getNbItemTemplate(2432), false)
                                player.removeItemByTemplateId(2433, player.getNbItemTemplate(2433), false)
                                player.teleport(6833, 422)
                            } else {
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.noflaque.fraise"))
                            }
                            
}
3 -> {if (player.hasItemTemplate(2431, 10, false)) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2433).toString() + "~" + 2433)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2432).toString() + "~" + 2432)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2431).toString() + "~" + 2431)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2430).toString() + "~" + 2430)
                                player.removeItemByTemplateId(2430, player.getNbItemTemplate(2430), false)
                                player.removeItemByTemplateId(2431, player.getNbItemTemplate(2431), false)
                                player.removeItemByTemplateId(2432, player.getNbItemTemplate(2432), false)
                                player.removeItemByTemplateId(2433, player.getNbItemTemplate(2433), false)
                                player.teleport(6832, 422)
                            } else {
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.gele.noflaque.citron"))
                            }
                            
}
4 -> {if (player.hasItemTemplate(2430, 10, false)) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2433).toString() + "~" + 2433)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2432).toString() + "~" + 2432)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2431).toString() + "~" + 2431)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2430).toString() + "~" + 2430)
                                player.removeItemByTemplateId(2430, player.getNbItemTemplate(2430), false)
                                player.removeItemByTemplateId(2431, player.getNbItemTemplate(2431), false)
                                player.removeItemByTemplateId(2432, player.getNbItemTemplate(2432), false)
                                player.removeItemByTemplateId(2433, player.getNbItemTemplate(2433), false)
                                player.teleport(6831, 422)
                            } else {
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.gele.noflaque.bleue"))
                            }
                            
}
5 -> {if (player.hasItemTemplate(2433, 10, false)
                                    && player.hasItemTemplate(2432, 10, false)
                                    && player.hasItemTemplate(2431, 10, false)
                                    && player.hasItemTemplate(2430, 10, false)) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2433).toString() + "~" + 2433)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2432).toString() + "~" + 2432)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2431).toString() + "~" + 2431)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2430).toString() + "~" + 2430)
                                player.removeItemByTemplateId(2430, player.getNbItemTemplate(2430), false)
                                player.removeItemByTemplateId(2431, player.getNbItemTemplate(2431), false)
                                player.removeItemByTemplateId(2432, player.getNbItemTemplate(2432), false)
                                player.removeItemByTemplateId(2433, player.getNbItemTemplate(2433), false)
                                player.teleport(6835, 422)
                            } else {
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.gele.noflaque.royal.2"))
                            }
                            
}
6 -> {if (player.hasItemTemplate(2433, 15, false)
                                    && player.hasItemTemplate(2432, 15, false)
                                    && player.hasItemTemplate(2431, 15, false)
                                    && player.hasItemTemplate(2430, 15, false)) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2433).toString() + "~" + 2433)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2432).toString() + "~" + 2432)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2431).toString() + "~" + 2431)
                                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + player.getNbItemTemplate(2430).toString() + "~" + 2430)
                                player.removeItemByTemplateId(2430, player.getNbItemTemplate(2430), false)
                                player.removeItemByTemplateId(2431, player.getNbItemTemplate(2431), false)
                                player.removeItemByTemplateId(2432, player.getNbItemTemplate(2432), false)
                                player.removeItemByTemplateId(2433, player.getNbItemTemplate(2433), false)
                                player.teleport(6836, 422)
                            } else {
                                SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.gele.noflaque.royal.4"))
                            }
                            
}
}
                }
                
}
508 -> {if (player.curMap.id == 8317) {
                    player.teleport(8236, 370)
                    var newObjAdded11: GameObject? = World.world.getObjTemplate(7415)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded11!!, true, -1)) {
                        World.world.addGameObject(newObjAdded11)
                        player.addItem(newObjAdded11!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 7415)
                }
                
}
509 -> {player.teleport(4786, 300)
                var newObjAdded11: GameObject? = World.world.getObjTemplate(6885)!!.createNewItem(1, false)
                if (!player.addObjetSimiler(newObjAdded11!!, true, -1)) {
                    World.world.addGameObject(newObjAdded11)
                    player.addItem(newObjAdded11!!, true)
                }
                SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 6885)
                var newObjAdded12: GameObject? = World.world.getObjTemplate(8388)!!.createNewItem(1, false)
                if (!player.addObjetSimiler(newObjAdded12!!, true, -1)) {
                    World.world.addGameObject(newObjAdded12)
                    player.addItem(newObjAdded12!!, true)
                }
                SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 8388)
                
}
510 -> {if (player.curMap.id == 3373
                        && player.hasItemTemplate(6885, 1, false)) {
                    player.removeItemByTemplateId(6885, 1, false)
                    var newObjAdded121: GameObject? = World.world.getObjTemplate(6887)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded121!!, true, -1)) {
                        World.world.addGameObject(newObjAdded121)
                        player.addItem(newObjAdded121!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 6885)
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 6887)
                }
                
}
512 -> {if (player.curMap.id == 10213) {
                    player.teleport(6536, 273)
                    var newObjAdded111: GameObject? = World.world.getObjTemplate(8476)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded111!!, true, -1)) {
                        World.world.addGameObject(newObjAdded111)
                        player.addItem(newObjAdded111!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 8476)
                }
                
}
513 -> {if (player.curMap.id == 10199) {
                    player.teleport(6738, 213)
                    var newObjAdded111: GameObject? = World.world.getObjTemplate(8477)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded111!!, true, -1)) {
                        World.world.addGameObject(newObjAdded111)
                        player.addItem(newObjAdded111!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 8477)
                }
                
}
514 -> {if (player.curMap.id == 9638
                        && player.hasItemTemplate(8476, 1, false)
                        && player.hasItemTemplate(8477, 1, false)) {
                    player.teleport(10141, 448)
                    player.removeItemByTemplateId(8476, 1, false)
                    player.removeItemByTemplateId(8477, 1, false)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 8476)
                    SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 8477)
                }
                
}
515 -> {if (player.curMap.id == 8497) {
                    player.teleport(8167, 252)
                    var newObjAdded111: GameObject? = World.world.getObjTemplate(7414)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded111!!, true, -1)) {
                        World.world.addGameObject(newObjAdded111)
                        player.addItem(newObjAdded111!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 7414)
                }
                
}
516 -> {if (player.curMap.id == 1140
                        && player.kamas >= 1000) {
                    var newObjAdded111: GameObject? = World.world.getObjTemplate(2239)!!.createNewItem(1, false)
                    if (!player.addObjetSimiler(newObjAdded111!!, true, -1)) {
                        World.world.addGameObject(newObjAdded111)
                        player.addItem(newObjAdded111!!, true)
                    }
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + 2239)
                }
                
}
517 -> {mapId = (args.split(";")[0].split(",")[0]).toInt()
                var cellId: Int = (args.split(";")[0].split(",")[1]).toInt()
                var mapSecu: Short = (args.split(";")[1]).toShort()
                var id: Int = (args.split(";")[2]).toInt()
                if (player.curMap.id != mapSecu.toInt())
                    return true
                if (player.curMap.id == 9052) {
                    if (player.curCell.getId() != 268
                            || player.orientation != 7)
                        return true
                    if (!player.hasItemType(90)) {
                        SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.dj.fami"))
                        return true
                    }
                }
                player.teleport(mapId, cellId)
                player.setFullMorph(id, false, false)
                
}
518 -> {mapId = (args.split(";")[0].split(",")[0]).toInt()
                cellId = (args.split(";")[0].split(",")[1]).toInt()
                mapSecu = (args.split(";")[1]).toShort()
                if (player.curMap.id != mapSecu.toInt())
                    return true
                player.unsetFullMorph()
                player.teleport(mapId, cellId)
                
}
519 -> {mapId = (args.split(";")[0].split(",")[0]).toInt()
                cellId = (args.split(";")[0].split(",")[1]).toInt()
                mapSecu = (args.split(";")[1]).toShort()
                if (player.curMap.id != mapSecu.toInt())
                    return true
                obj1G = World.world.getObjTemplate((args.split(";")[2]).toInt())!!.createNewItem(1, false)
                if (obj1G != null)
                    if (player.addItem(obj1G, true, false))
                        World.world.addGameObject(obj1G)
                player.send("Im021;1~" + args.split(";")[2])
                obj1G = World.world.getObjTemplate((args.split(";")[3]).toInt())!!.createNewItem(1, false)
                if (obj1G != null)
                    if (player.addItem(obj1G, true, false))
                        World.world.addGameObject(obj1G)
                player.send("Im021;1~" + args.split(";")[3])
                player.teleport(mapId, cellId)
                
}
520 -> {if (player.curMap.id != 8497)
                    return true

                obj1G = World.world.getObjTemplate(7414)!!.createNewItem(1, false)
                if (player.addItem(obj1G!!, true, false))
                    World.world.addGameObject(obj1G)
                player.send("Im021;1~7414")
                if (!player.emotes.contains(15)) {
                    obj1G = World.world.getObjTemplate(7413)!!.createNewItem(1, false)
                    if (player.addItem(obj1G!!, true, false))
                        World.world.addGameObject(obj1G)
                    player.send("Im021;1~7413")
                }

                player.teleport(8167, 252)
                
}
521 -> {if (player.curMap.id != 9248)
                    return true

                if (player.hasItemTemplate(7887, 1, false) && player.hasItemTemplate(7888, 1, false) && player.hasItemTemplate(7889, 1, false) && player.hasItemTemplate(7890, 1, false)) {
                    player.removeItemByTemplateId(7887, 1, false)
                    player.removeItemByTemplateId(7888, 1, false)
                    player.removeItemByTemplateId(7889, 1, false)
                    player.removeItemByTemplateId(7890, 1, false)
                    player.send("Im022;1~7887")
                    player.send("Im022;1~7888")
                    player.send("Im022;1~7889")
                    player.send("Im022;1~7890")

                    obj1G = World.world.getObjTemplate(8073)!!.createNewItem(1, false)
                    if (player.addItem(obj1G!!, true, false))
                        World.world.addGameObject(obj1G)
                    player.send("Im021;1~8073")
                } else {
                    player.send("Im119|45")
                }
                
}
522 -> {if (player.curMap.id != 8349)
                    return true

                obj1G = World.world.getObjTemplate(6978)!!.createNewItem(1, false)
                if (player.addItem(obj1G!!, true, false))
                    World.world.addGameObject(obj1G)
                player.send("Im021;1~6978")
                player.teleport(8467, 227)
                
}
527 -> {if (player.curMap.id != 10165)
                    return true

                player.addStaticEmote(19)
                player.teleport(10155, 210)
                
}
967 -> {if(client == null) return true
                if (player.curMap.id != 8736 && player.curMap.id != 8737) return true

                var job: Job? = World.world.getMetier(65)
                if (job == null) return true

                if (player.getMetierByID(job!!.id) != null) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "111")
                    SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                    player.exchangeAction = null
                    return true
                }

                if (player.getMetierByID(2) != null && player.getMetierByID(2)!!.get_lvl() < 30 || player.getMetierByID(11) != null && player.getMetierByID(11)!!.get_lvl() < 30 || player.getMetierByID(13) != null && player.getMetierByID(13)!!.get_lvl() < 30 || player.getMetierByID(14) != null && player.getMetierByID(14)!!.get_lvl() < 30 || player.getMetierByID(15) != null && player.getMetierByID(15)!!.get_lvl() < 30 || player.getMetierByID(16) != null && player.getMetierByID(16)!!.get_lvl() < 30 || player.getMetierByID(17) != null && player.getMetierByID(17)!!.get_lvl() < 30 || player.getMetierByID(18) != null && player.getMetierByID(18)!!.get_lvl() < 30 || player.getMetierByID(19) != null && player.getMetierByID(19)!!.get_lvl() < 30 || player.getMetierByID(20) != null && player.getMetierByID(20)!!.get_lvl() < 30 || player.getMetierByID(24) != null && player.getMetierByID(24)!!.get_lvl() < 30 || player.getMetierByID(25) != null && player.getMetierByID(25)!!.get_lvl() < 30 || player.getMetierByID(26) != null && player.getMetierByID(26)!!.get_lvl() < 30 || player.getMetierByID(27) != null && player.getMetierByID(27)!!.get_lvl() < 30 || player.getMetierByID(28) != null && player.getMetierByID(28)!!.get_lvl() < 30 || player.getMetierByID(31) != null && player.getMetierByID(31)!!.get_lvl() < 30 || player.getMetierByID(36) != null && player.getMetierByID(36)!!.get_lvl() < 30 || player.getMetierByID(41) != null && player.getMetierByID(41)!!.get_lvl() < 30 || player.getMetierByID(56) != null && player.getMetierByID(56)!!.get_lvl() < 30 || player.getMetierByID(58) != null && player.getMetierByID(58)!!.get_lvl() < 30 || player.getMetierByID(60) != null && player.getMetierByID(60)!!.get_lvl() < 30) {
                    SocketManager.send(client, "DQ336|4840")
                    return false
                }

                if (player.totalJobBasic() > 2) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "19")
                    SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                    player.exchangeAction = null
                } else {
                    if (player.hasItemTemplate(459, 20, false) && player.hasItemTemplate(7657, 15, false)) {
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 20 + "~" + 459)
                        player.removeItemByTemplateId(459, 20, false)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 15 + "~" + 7657)
                        player.removeItemByTemplateId(7657, 15, false)
                        player.learnJob(job)
                        SocketManager.send(client, "DQ3153|4840")
                        return false
                    } else {
                        SocketManager.send(client, "DQ3151|4840")
                        return false
                    }
                }
                return true
}
968 -> {if (player.curMap.id == 9877
                        || player.curMap.id == 9881) {
                    player.teleport(9538, 186)
                }
                
}
969 -> {if (player.curMap.id == 8715) // Abra
                {
                    player.teleport(8716, 366)
                    player.setFullMorph(11, false, false)
                } else if (player.curMap.id == 9120) // CM
                {
                    player.teleport(9121, 69)
                    player.setFullMorph(11, false, false)
                }
                
}
970 -> {if (player.curMap.id == 8719) // Abra
                {
                    player.unsetFullMorph()
                    player.teleport(10154, 335)
                } else if (player.curMap.id == 9123) // CM
                {
                    player.unsetFullMorph()
                    player.teleport(9125, 71)
                }
                
}
972 -> {if (player.curMap.id != 8978)
                    return true
                if (!player.hasItemTemplate(7935, 1, false))
                    return true
                if (!player.hasItemTemplate(7936, 1, false))
                    return true
                if (!player.hasItemTemplate(7937, 1, false))
                    return true
                if (!player.hasItemTemplate(7938, 1, false))
                    return true

                var key0: Boolean = false
                if(player.hasItemTemplate(10207, 1, false)) {
                    var stats: String? = player.getItemTemplate(10207)!!.txtStat[Constant.STATS_NAME_DJ]
                    for(key in  stats!!.splitJ(",")) {
                        if (key.toInt(16) == 8073) key0 = true
                    }

                    if(key0){
                        var replace: String = java.lang.Integer.toHexString(8073)
                newStats = ""
                        for (i in  stats!!.splitJ(","))
                            if (!i.equals(replace))
                                newStats += (if (newStats.isEmpty()) i else "," + i)
                        player.getItemTemplate(10207)!!.txtStat.remove(Constant.STATS_NAME_DJ)
                        player.getItemTemplate(10207)!!.txtStat.put(Constant.STATS_NAME_DJ, newStats)
                        SocketManager.GAME_SEND_UPDATE_ITEM(player, player.getItemTemplate(10207)!!)
                    }
                } else return true

                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 7935)
                player.removeItemByTemplateId(7935, 1, false)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 7936)
                player.removeItemByTemplateId(7936, 1, false)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 7937)
                player.removeItemByTemplateId(7937, 1, false)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 7938)
                player.removeItemByTemplateId(7938, 1, false)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 8073)

                var `object`: GameObject? = World.world.getObjTemplate(8072)!!.createNewItem(1, false)

                if (player.addItem(`object`!!, false, false)) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + `object`!!.template!!.id)
                    World.world.addGameObject(`object`!!)
                }

                player.teleport(9503, 357)
                
}
973 -> {if (player.curMap.id != 8978)
                    return true
                if (!player.hasItemTemplate(7935, 1, false))
                    return true
                if (!player.hasItemTemplate(7936, 1, false))
                    return true
                if (!player.hasItemTemplate(7937, 1, false))
                    return true
                if (!player.hasItemTemplate(7938, 1, false))
                    return true
                if (!player.hasItemTemplate(8073, 1, false))
                    return true

                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 7935)
                player.removeItemByTemplateId(7935, 1, false)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 7936)
                player.removeItemByTemplateId(7936, 1, false)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 7937)
                player.removeItemByTemplateId(7937, 1, false)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 7938)
                player.removeItemByTemplateId(7938, 1, false)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + 8073)
                player.removeItemByTemplateId(8073, 1, false)

                var dofus: ObjectTemplate? = World.world.getObjTemplate(8072)
                var obj: GameObject? = dofus!!.createNewItem(1, false)
                if (player.addItem(obj!!, false, false)) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + obj!!.template!!.id)
                    World.world.addGameObject(obj)
                }

                player.teleport(9503, 357)
                
}
974 -> {if (player.curMap.id != 8978)
                    return true
                if (!player.hasItemTemplate(8075, 10, false))
                    return true
                if (!player.hasItemTemplate(8076, 10, false))
                    return true
                if (!player.hasItemTemplate(8077, 10, false))
                    return true
                if (!player.hasItemTemplate(8064, 10, false))
                    return true
                if (player.hasSpell(364))
                    return true

                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 10 + "~" + 8075)
                player.removeItemByTemplateId(8075, 10, false)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 10 + "~" + 8076)
                player.removeItemByTemplateId(8076, 10, false)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 10 + "~" + 8077)
                player.removeItemByTemplateId(8077, 10, false)
                SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 10 + "~" + 8064)
                player.removeItemByTemplateId(8064, 10, false)

                player.learnSpell(364, 1, true, true, true)
                
}
975 -> {if (player.curMap.id != 8973)
                    return true
                if (!player.hasItemTemplate(7935, 1, false) || !player.hasItemTemplate(7936, 1, false) || !player.hasItemTemplate(7937, 1, false) || !player.hasItemTemplate(7938, 1, false))
                    return true

                player.teleport(8977, 448)
                
}
977 -> {try {
                    when (player.curMap.id) {
9553, 9554, 9555, 9556, 9557, 9558, 9559, 9560, 9561, 9562, 9563, 9564, 9565, 9566, 9567, 9568, 9569, 9570, 9571, 9572, 9573, 9574, 9575, 9576, 9577 -> {player.teleport(9876, 287)
                            
}
}
                } catch (e: Exception) {
                    return true
                }
                
}
978 -> {try {
                    when (player.curMap.id) {
9372, 9384, 9380, 9381, 9382, 9383, 9393, 9374, 9394, 9390, 9391, 9392, 9373, 9389, 9385, 9386, 9387, 9388, 9371, 9375, 9376, 9377, 9378, 9379 -> {player.teleport(9396, 387)
                            
}
}
                } catch (e: Exception) {
                    return true
                }
                
}
980 -> {try {
                    mapId = (args.split(",")[0]).toInt()
                    cellId = (args.split(",")[1]).toInt()
                    var item: Int = (args.split(",")[2]).toInt()
                    var item2: Int = (args.split(",")[3]).toInt()
                    mapSecu = (args.split(",")[4]).toShort()

                    if (player.curMap.id != mapSecu.toInt())
                        return true
                    var ok: Boolean = false
                    if(mapSecu.toInt() == 9395 && player.hasItemTemplate(10207, 1, false)) {// DC
                        var statsReplace1: String = stats
                        var statsReplace2: String = ""
                        `object` = player.getItemTemplate(10207)!!
                        stats = `object`!!.txtStat[Constant.STATS_NAME_DJ] ?: ""
                        try {
                            var ok1: Boolean = false
                var ok2 = false
                            for (i in  stats!!.splitJ(",")) {
                                if(java.lang.Integer.toHexString(7511).equals(i)) {
                                    statsReplace1 = i
                                    ok1 = true
                                }
                                if(java.lang.Integer.toHexString(8320).equals(i)) {
                                    statsReplace2 = i
                                    ok2 = true
                                }
                            }
                            ok = ok1 && ok2
                        } catch (e: Exception) {
                            log.error("unexpected error", e)
                        }
                        if(ok) {
                            if (!statsReplace1.isEmpty()) {
                                var newStats: String = ""
                                for (i in  stats!!.splitJ(","))
                                    if (!i.equals(statsReplace1))
                                        newStats += (if (newStats.isEmpty()) i else "," + i)
                                `object`.txtStat.remove(Constant.STATS_NAME_DJ)
                                `object`.txtStat.put(Constant.STATS_NAME_DJ, newStats)
                                SocketManager.GAME_SEND_UPDATE_ITEM(player, player.getItemTemplate(10207)!!)
                            }
                            if (!statsReplace2.isEmpty()) {
                                var newStats: String = ""
                                for (i in  stats!!.splitJ(","))
                                    if (!i.equals(statsReplace2))
                                        newStats += (if (newStats.isEmpty()) i else "," + i)
                                `object`.txtStat.remove(Constant.STATS_NAME_DJ)
                                `object`.txtStat.put(Constant.STATS_NAME_DJ, newStats)
                                SocketManager.GAME_SEND_UPDATE_ITEM(player, player.getItemTemplate(10207)!!)
                            }
                        }
                    }

                    if(!ok) {
                        if (!player.hasItemTemplate(item, 1, false) && !player.hasItemTemplate(item2, 1, false))
                            return true
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + item)
                        player.removeItemByTemplateId(item, 1, false)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + item2)
                        player.removeItemByTemplateId(item2, 1, false)
                    }

                    player.teleport(mapId, cellId)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                
}
982 -> {try {
                    player.setFuneral()
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                
}
985 -> {if(client == null) return true
                try {
                    var item: Int = (args.split(",")[0]).toInt()
                    var item2: Int = (args.split(",")[1]).toInt()
                    var mapCurId: Int = (args.split(",")[2]).toInt()
                    var metierId: Int = (args.split(",")[3]).toInt()

                    if (player.curMap.id != mapCurId)
                        return true
                    var metierArgs: Job? = World.world.getMetier(metierId)
                    if (metierArgs == null)
                        return true

                    if (player.getMetierByID(metierId) != null) {
                        SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                        player.exchangeAction = null
                        SocketManager.GAME_SEND_Im_PACKET(player, "111")
                        return true; // Si on a d�j� le m�tier
                    }

                    var t: ObjectTemplate? = World.world.getObjTemplate(item2)
                    if (t == null)
                        return true

                    if (player.hasItemTemplate(item, 1, false)) {

                        for (entry in  player.metiers.entries) {
                            if (entry.value.get_lvl() < 30
                                    && !entry.value.template.isMaging()) {
                                SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                                player.exchangeAction = null
                                SocketManager.GAME_SEND_Im_PACKET(player, "18;30")
                                return true
                            }
                        }

                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + item)
                        player.removeItemByTemplateId(item, 1, false)
                        obj = t.createNewItem(1, false)
                        obj!!.refreshStatsObjet("325#0#0#" + System.currentTimeMillis())
                        if (player.addItem(obj!!, false, false)) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + obj!!.template!!.id)
                            World.world.addGameObject(obj)
                        }

                        player.learnJob(World.world.getMetier(metierId)!!)
                        ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(player)
                        SocketManager.GAME_SEND_Ow_PACKET(player)
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                
}
986 -> {if(client == null) return true
                try {
                    var mapCurId: Int = (args.split(",")[0]).toInt()
                    var item: Int = (args.split(",")[1]).toInt()
                    var item2: Int = (args.split(",")[2]).toInt()
                    var metierId: Int = (args.split(",")[3]).toInt()

                    if (player.curMap.id != mapCurId)
                        return true
                    var metierArgs: Job? = World.world.getMetier(metierId)
                    if (metierArgs == null)
                        return true

                    if (player.getMetierByID(metierId) != null) {
                        SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                        player.exchangeAction = null
                        SocketManager.GAME_SEND_Im_PACKET(player, "111")
                        return true; // Si on a d�j� le m�tier
                    }

                    if (player.hasItemTemplate(item, 1, false)) {
                        player.removeItemByTemplateId(item, 1, false)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + item)
                        var t: ObjectTemplate? = World.world.getObjTemplate(item2)
                        if (t != null) {
                            obj = t.createNewItem(1, false)
                            obj!!.refreshStatsObjet("325#0#0#" + System.currentTimeMillis())
                            if (player.addItem(obj!!, false, false)) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + obj!!.template!!.id)
                                World.world.addGameObject(obj)
                                ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(player)
                                SocketManager.GAME_SEND_Ow_PACKET(player)
                                return false
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                
}
987 -> {if(client == null) return true
                try {
                    var item: Int = (args.split(",")[0]).toInt()
                    var item2: Int = (args.split(",")[1]).toInt()
                    var item3: Int = (args.split(",")[2]).toInt()
                    var mapCurId: Int = (args.split(",")[3]).toInt()
                    var metierId: Int = (args.split(",")[4]).toInt()

                    if (player.curMap.id != mapCurId)
                        return true
                    var metierArgs: Job? = World.world.getMetier(metierId)
                    if (metierArgs == null)
                        return true

                    if (player.getMetierByID(metierId) != null) {
                        SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                        player.exchangeAction = null
                        SocketManager.GAME_SEND_Im_PACKET(player, "111")
                        return true; // Si on a d�j� le m�tier
                    }

                    if (player.hasItemTemplate(item, 1, false)
                            && player.hasItemTemplate(item2, 1, false)) {
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + item)
                        player.removeItemByTemplateId(item, 1, false)
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + item2)
                        player.removeItemByTemplateId(item2, 1, false)

                        var t: ObjectTemplate? = World.world.getObjTemplate(item3)
                        if (t != null) {
                            obj = t.createNewItem(1, false)
                            if (player.addItem(obj!!, false, false)) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + obj!!.template!!.id)
                                World.world.addGameObject(obj)
                                ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(player)
                                SocketManager.GAME_SEND_Ow_PACKET(player)
                                return false
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                
}
989 -> {if(client == null) return true
                try {
                    var mapCurId: Int = (args.split(",")[0]).toInt()
                    var item: Int = (args.split(",")[1]).toInt()
                    var item2: Int = (args.split(",")[2]).toInt()
                    var metierId: Int = (args.split(",")[3]).toInt()

                    if (player.curMap.id != mapCurId)
                        return true
                    var metierArgs: Job? = World.world.getMetier(metierId)
                    if (metierArgs == null)
                        return true

                    if (player.getMetierByID(metierId) != null) {
                        SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                        player.exchangeAction = null
                        SocketManager.GAME_SEND_Im_PACKET(player, "111")
                        return true; // Si on a d�j� le m�tier
                    }

                    if (player.hasItemTemplate(item, 1, false)) {
                        var t: ObjectTemplate? = World.world.getObjTemplate(item2)
                        if (t != null) {
                            obj = t.createNewItem(1, false)
                            if (player.addItem(obj!!, false, false)) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + obj!!.template!!.id)
                                World.world.addGameObject(obj)
                                ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(player)
                                SocketManager.GAME_SEND_Ow_PACKET(player)
                                return false
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                
}
991 -> {try {
                    var mapCurId: Int = (args.split(",")[0]).toInt()
                    var item: Int = (args.split(",")[1]).toInt()
                    var monstre: Int = (args.split(",")[2]).toInt()
                    var grade: Int = (args.split(",")[3]).toInt()
                    if (player.curMap.id == mapCurId) {
                        if (player.hasItemTemplate(item, 1, false)) {
                            var groupe: String = monstre.toString() + "," + grade.toString() + "," + grade.toString() + ";"
                            var Mgroupe: MonsterGroup = MonsterGroup(player.curMap.nextObjectId, player.curMap, player.curCell.getId(), groupe)
                            player.curMap.startFightVersusMonstres(player, Mgroupe); // Si bug startfight, voir "//Respawn d'un groupe fix" dans fight.java
                        }
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                
}
992 -> {if(client == null) return true
                try {
                    var item1: Int = (args.split(",")[0]).toInt()
                    var item2: Int = (args.split(",")[1]).toInt()
                    var mapCurId: Int = (args.split(",")[2]).toInt()
                    var mId: Int = (args.split(",")[3]).toInt()
                    if (player.curMap.id == mapCurId) {
                        if (player.hasItemTemplate(item1, 1, false)) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + item1)
                            player.removeItemByTemplateId(item1, 1, false)
                        }
                        if (player.hasItemTemplate(item2, 1, false)) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + item2)
                            player.removeItemByTemplateId(item2, 1, false)
                        }

                        var metierArgs: Job? = World.world.getMetier(mId)
                        if (metierArgs == null)
                            return true
                        if (player.getMetierByID(mId) != null) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "111")
                            return true
                        }

                        for (entry in  player.metiers.entries) {
                            if (entry.value.get_lvl() < 30
                                    && !entry.value.template.isMaging()) {
                                SocketManager.GAME_SEND_END_DIALOG_PACKET(client)
                                player.exchangeAction = null
                                SocketManager.GAME_SEND_Im_PACKET(player, "18;30")
                                return true
                            }
                        }

                        player.learnJob(World.world.getMetier(mId)!!)
                        ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(player)
                        SocketManager.GAME_SEND_Ow_PACKET(player)
                        return true
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                
}
993 -> {try {
                    var item1: Int = (args.split(",")[0]).toInt()
                    var item2: Int = (args.split(",")[1]).toInt()
                    if (player.hasItemTemplate(item1, 1, false)) {
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + item1)
                        player.removeItemByTemplateId(item1, 1, false)
                    }
                    if (player.hasItemTemplate(item2, 1, false)) {
                        SocketManager.GAME_SEND_Im_PACKET(player, "022;" + 1 + "~" + item2)
                        player.removeItemByTemplateId(item2, 1, false)
                    }
                    ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(player)
                    SocketManager.GAME_SEND_Ow_PACKET(player)
                    return true
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                
}
994 -> {try {
                    var mapID: Int = (args.split(",")[0]).toInt()
                    var item: Int = (args.split(",")[1]).toInt()
                    var metierId: Int = (args.split(",")[2]).toInt()
                    var metierArgs: Job? = World.world.getMetier(metierId)

                    if (metierArgs == null)
                        return true
                    if (player.getMetierByID(metierId) != null) {
                        SocketManager.GAME_SEND_Im_PACKET(player, "111")
                        return true
                    }

                    var curMapP: GameMap = player.curMap
                    if (curMapP.id == mapID) {
                        if (!player.hasItemTemplate(item, 1, false)) {
                            if (player.getMetierByID(41) != null) {
                                SocketManager.GAME_SEND_Im_PACKET(player, "182")
                                return true
                            }
                            var t: ObjectTemplate? = World.world.getObjTemplate(item)
                            if (t != null) {
                                obj = t.createNewItem(1, false)
                                obj!!.refreshStatsObjet("325#0#0#" + System.currentTimeMillis())
                                if (player.addItem(obj!!, false, false)) {
                                    SocketManager.GAME_SEND_Im_PACKET(player, "021;" + 1 + "~" + obj!!.template!!.id)
                                    World.world.addGameObject(obj)
                                    ((DatabaseManager.get(PlayerData::class.java) as PlayerData)).update(player)
                                    SocketManager.GAME_SEND_Ow_PACKET(player)
                                    return true
                                }
                            }
                        }
                    }
                    return false
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
                
}
995 -> {var curMap2: GameMap = player.curMap
                if (!player.isInPrison()) {
                    if (curMap2.id == 11866) {
                        SocketManager.GAME_SEND_GA_PACKET(player.getGameClient()!!, "", "2", player.id.toString() + "", "6")
                        player.teleport(11862, 253)
                    } else if (curMap2.id == 11862) {
                        SocketManager.GAME_SEND_GA_PACKET(player.getGameClient()!!, "", "2", player.id.toString() + "", "6")
                        player.teleport(11866, 344)
                    } else {
                        SocketManager.GAME_SEND_Im_PACKET(player, "182")
                        return true
                    }
                }
                
}
996 -> {var curMap: GameMap = player.curMap
                var mapSecure: ArrayList<Int> = ArrayList<Int>()
                for (i in  args.splitJ(","))
                    mapSecure.add(i.toInt())

                if (!mapSecure.contains((curMap.id.toInt()))) {
                    SocketManager.GAME_SEND_Im_PACKET(player, "182")
                    return true
                }

                var pKamas4: Long = player.kamas
                if (pKamas4 < 50) {
                    player.teleport(11862, 253)
                    return true
                }

                if (!player.isInPrison()) {
                    SocketManager.GAME_SEND_GA_PACKET(player.getGameClient()!!, "", "2", player.id.toString() + "", "6")
                    var pNewKamas4: Long = pKamas4 - 50
                    if (pNewKamas4 < 0)
                        pNewKamas4 = 0
                    player.kamas = pNewKamas4
                    if (player.isOnline)
                        SocketManager.GAME_SEND_STATS_PACKET(player)
                    SocketManager.GAME_SEND_Im_PACKET(player, "046;" + 50)
                    player.teleport(10256, 211)
                }
                
}
997 -> {try {
                    var metierID: Int = (args.split(",")[0]).toInt()
                    var mapIdargs: Int = (args.split(",")[1]).toInt()
                    var metierArgs: Job? = World.world.getMetier(metierID)

                    if (metierArgs == null)
                        return true; // Si le m�tier n'existe pas
                    if (player.getMetierByID(metierID) != null) {
                        SocketManager.GAME_SEND_Im_PACKET(player, "111")
                        return true; // Si on a d�j� le m�tier
                    }

                    var curMapPerso: GameMap = player.curMap
                    if (curMapPerso.id != mapIdargs)
                        return true; // Map secure

                    if (metierArgs.isMaging()) // Si c'est du FM
                    {
                        var metierBase: JobStat = player.getMetierByID(World.world.getMetierByMaging(metierID))!!
                        if (metierBase == null)
                            return true; // Si la base n'existe pas
                        if (metierBase.get_lvl() < 65) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "111")
                            return true; // Si la base n'est pas assez hl
                        } else if (player.totalJobFM() > 2) {
                            SocketManager.GAME_SEND_Im_PACKET(player, "19")
                            return true; // On compte les m�tiers d�ja acquis si c'est sup�rieur a 2 on ignore
                        } else {
                            player.learnJob(World.world.getMetier(metierID)!!)
                        }
                    }
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                    GameServer.a()
                }
                
}
998 -> {if (player.curMap.id == 10154
                        && player.curCell.getId() == 142) {
                    player.teleport(8721, 395)
                } else {
                    SocketManager.GAME_SEND_MESSAGE(player, player.getLang().trans("other.action.apply.nofacetopnj"))
                }
                
}
999 -> {player.teleport(map!!.id, this.args.toInt())
                
}
1000 -> {mapId = (this.args.split(",")[0]).toInt()
                cell = (this.args.split(",")[1]).toInt()
                player.teleport(mapId, cell)
                player.setSavePos(mapId,cell)
                SocketManager.GAME_SEND_Im_PACKET(player, "06")
                
}
1001 -> {mapId = (this.args.split(",")[0]).toInt()
                cell = (this.args.split(",")[1]).toInt()
                player.teleport(mapId, cell, true)
                
}
1002 -> {for(s in  this.args.splitJ(";")) {
                    var s1: List<String> = s.split(",")
                    var template: ObjectTemplate? = World.world.getObjTemplate(s1[0].toInt())
                    if(template != null) {
                        var o: GameObject? = template!!.createNewItem(s1[1].toInt(), false)
                        if(player.addItem(o!!, true, false))
                            World.world.addGameObject(o)
                    }
                }
                
}
else -> {
}
}
        return true
    }
}
