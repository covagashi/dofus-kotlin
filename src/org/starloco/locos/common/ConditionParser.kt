package org.starloco.locos.common

import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression
import org.starloco.locos.client.Player
import org.starloco.locos.fight.spells.SpellEffect
import org.starloco.locos.game.world.World
import org.starloco.locos.job.JobStat
import org.starloco.locos.kernel.Constant
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.other.Action
import org.starloco.locos.quest.QuestProgress
import java.util.ArrayList
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ConditionParser::class.java)

class ConditionParser {

    fun validConditions(perso: Player?, req: String?): Boolean {
        var req = req
        if (req == null || req == "")
            return true
        if (req.contains("BI") || perso == null)
            return false
        req = req.replace("&", "&&").replace("=", "==").replace("|", "||").replace("!", "!=").replace("~", "==")
        if (req.contains("Sc"))
            return true
        if (req.contains("Pg")) // C'est les dons que l'on gagne lors des quêtes d'alignement, connaissance des potions etc ... ce n'est pas encore codé !
            return false
        if (req.contains("RA"))
            return haveRA(req, perso)
        if (req.contains("RO"))
            return haveRO(req, perso)
        if (req.contains("Mph"))
            return haveMorph(req, perso)
        if (req.contains("PO"))
            req = havePO(req, perso)
        if (req.contains("PN"))
            req = canPN(req, perso)
        if (req.contains("PJ"))
            req = canPJ(req, perso)
        if (req.contains("JOB"))
            req = haveJOB(req, perso)
        if (req.contains("DV"))
            return haveDV()
        if (req.contains("NPC"))
            return haveNPC(req, perso)
        if (req.contains("QEt"))
            return haveQEt(req, perso)
        if (req.contains("QE"))
            return haveQE(req, perso)
        if (req.contains("QT"))
            return haveQT(req, perso)
        if (req.contains("Ce"))
            return haveCe(req, perso)
        if (req.contains("TiT"))
            return haveTiT(req, perso)
        if (req.contains("Ti"))
            return haveTi(req, perso)
        if (req.contains("Qa"))
            return haveQa(req, perso)
        if (req.contains("Pj"))
            return havePj(req, perso)
        if (req.contains("AM"))
            return haveMetier(req, perso)

        try {
            val args = ArrayList<Argument>()
            //Stats stuff compris
            args.add(Argument("CI", perso.getTotalStats(false).getEffect(Constant.STATS_ADD_INTE).toDouble()))
            args.add(Argument("CV", perso.getTotalStats(false).getEffect(Constant.STATS_ADD_VITA).toDouble()))
            args.add(Argument("CA", perso.getTotalStats(false).getEffect(Constant.STATS_ADD_AGIL).toDouble()))
            args.add(Argument("CW", perso.getTotalStats(false).getEffect(Constant.STATS_ADD_SAGE).toDouble()))
            args.add(Argument("CC", perso.getTotalStats(false).getEffect(Constant.STATS_ADD_CHAN).toDouble()))
            args.add(Argument("CS", perso.getTotalStats(false).getEffect(Constant.STATS_ADD_FORC).toDouble()))
            args.add(Argument("CM", perso.getTotalStats(false).getEffect(Constant.STATS_ADD_PM).toDouble()))
            //Stats de bases
            args.add(Argument("Ci", perso.stats.getEffect(Constant.STATS_ADD_INTE).toDouble()))
            args.add(Argument("Cs", perso.stats.getEffect(Constant.STATS_ADD_FORC).toDouble()))
            args.add(Argument("Cv", perso.stats.getEffect(Constant.STATS_ADD_VITA).toDouble()))
            args.add(Argument("Ca", perso.stats.getEffect(Constant.STATS_ADD_AGIL).toDouble()))
            args.add(Argument("Cw", perso.stats.getEffect(Constant.STATS_ADD_SAGE).toDouble()))
            args.add(Argument("Cc", perso.stats.getEffect(Constant.STATS_ADD_CHAN).toDouble()))
            args.add(Argument("PW", perso.getMaxPod().toDouble()))//MaxPod
            if (perso.curMap.subArea != null)
                args.add(Argument("PB", perso.curMap.subArea!!.id.toDouble()))//SubArea
            args.add(Argument("PR", (if (perso.wife > 0) 1 else 0).toDouble()))//Marié ou pas
            args.add(Argument("SI", perso.curMap.id.toDouble()))//Mapid
            args.add(Argument("MiS", perso.id.toDouble()))//Les pierres d'ames sont lancables uniquement par le lanceur.
            args.add(Argument("MA", perso.getAlignMap().toDouble()))//Pandala
            if (req.contains("PSB"))
                args.add(Argument("PSB", perso.getAccount().points.toDouble()))//Points Boutique
            args.add(Argument("CF", (if (perso.getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR) == null) -1 else perso.getObjetByPos(Constant.ITEM_POS_PNJ_SUIVEUR)!!.template!!.id).toDouble()))//Personnage suiveur
            //Autre
            args.add(Argument("Ps", perso.alignment.toDouble()))//Alignement
            args.add(Argument("Pa", perso.aLvl.toDouble()))
            args.add(Argument("PL", perso.level.toDouble()))//Niveau
            args.add(Argument("PK", perso.kamas.toDouble()))//Kamas
            args.add(Argument("PG", perso.classe.toDouble()))//Classe
            args.add(Argument("PS", perso.sexe.toDouble()))//Sexe
            args.add(Argument("PZ", 1.0))//Abonnement
            args.add(Argument("PX", (if (perso.getGroup() != null) 1 else 0).toDouble()))//Niveau GM
            args.add(Argument("PP", perso.getGrade().toDouble()))//Grade

            val expression = Expression(req, *args.toTypedArray())
            return expression.calculate() == 1.0
        } catch (e: Exception) {
            log.error("unexpected error", e)
                }
        return false
    }

    private fun haveMorph(c: String, p: Player): Boolean {
        if (c.equals("", ignoreCase = true))
            return false
        var morph = -1
        try {
            morph = ((if (c.contains("==")) c.split("==")[1] else c.split("!=")[1])).toInt()
        } catch (e: Exception) {
            log.error("unexpected error", e)
                }
        return if (p.morphId == morph)
            c.contains("==")
        else
            !c.contains("==")
    }

    private fun haveMetier(c: String, p: Player): Boolean {
        if (p.metiers == null || p.metiers.isEmpty())
            return false
        for (entry in p.metiers.entries) {
            if (entry.value != null)
                return true
        }
        return false
    }

    private fun havePj(c: String, p: Player): Boolean {
        if (c.equals("", ignoreCase = true))
            return false
        for (s in c.split("||")) {
            val k = s.split("==")
            val id: Int
            try {
                id = (k[1]).toInt()
            } catch (e: Exception) {
                log.error("unexpected error", e)
                continue
            }
            if (p.getMetierByID(id) != null)
                return true
        }
        return false
    }

    //Avoir la quête en cours
    private fun haveQa(req: String, player: Player): Boolean {
        val id = ((if (req.contains("==")) req.split("==")[1] else req.split("!=")[1])).toInt()

        val qp = player.getQuestProgress(id)
        if (qp == null)
            return (!req.contains("=="))

        return !qp.isFinished() || (!req.contains("=="))
    }

    // Etre a l'etape id. Elle ne doit pas être validé et celle d'avant doivent l'être.
    private fun haveQEt(req: String, player: Player): Boolean {
        val id = ((if (req.contains("==")) req.split("==")[1] else req.split("!=")[1])).toInt()

        val oqp = player.getQuestProgressForCurrentStep(id)

        return req.contains("==") && oqp.isPresent()
    }

    private fun haveTiT(req: String, player: Player): Boolean {
        if (req.contains("==")) {
            val split = req.split("==")[1]
            if (split.contains("&&")) {
                val item = (split.split("&&")[0]).toInt()
                val time = (split.split("&&")[1]).toInt()
                val item2 = (split.split("&&")[2]).toInt()
                if (player.hasItemTemplate(item2, 1, false)
                        && player.hasItemTemplate(item, 1, false)) {
                    val timeStamp = player.getItemTemplate(item, 1)!!.txtStat[Constant.STATS_DATE]!!.toLong()
                    if (System.currentTimeMillis() - timeStamp <= time)
                        return true
                }
            }
        }
        return false
    }

    private fun haveTi(req: String, player: Player): Boolean {
        if (req.contains("==")) {
            val split = req.split("==")[1]
            if (split.contains(",")) {
                val split2 = split.split(",")
                val item = (split2[0]).toInt()
                val time = (split2[1]).toInt() * 60 * 1000
                if (player.hasItemTemplate(item, 1, false)) {
                    val timeStamp = player.getItemTemplate(item, 1)!!.txtStat[Constant.STATS_DATE]!!.toLong()
                    if (System.currentTimeMillis() - timeStamp > time)
                        return true
                }
            }
        }
        return false
    }

    private fun haveCe(req: String, player: Player): Boolean {
        val dopeuls: Map<Int, World.Couple<Int, Int>> = Action.getDopeul()
        val map = player.curMap
        if (map.id.toInt() in dopeuls) {
            val couple = dopeuls[map.id.toInt()] ?: return false

            val IDmob = couple.first
            val certificat = Constant.getCertificatByDopeuls(IDmob)

            if (certificat == -1)
                return false

            if (player.hasItemTemplate(certificat, 1, false)) {
                var txt = player.getItemTemplate(certificat, 1)!!.txtStat[Constant.STATS_DATE]!!
                if (txt.contains("#"))
                    txt = txt.split("#")[3]
                val timeStamp = (txt).toLong()
                return System.currentTimeMillis() - timeStamp > 86400000
            } else
                return true
        }
        return false
    }

    // Avoir la quête en cours.
    private fun haveQE(req: String, player: Player?): Boolean {
        if (player == null)
            return false
        val id = ((if (req.contains("==")) req.split("==")[1] else req.split("!=")[1])).toInt()

        val qp = player.getQuestProgress(id)
        if (qp == null)
            return req.contains("==")

        return qp.isFinished()
    }

    private fun haveQT(req: String, player: Player): Boolean {
        val id = ((if (req.contains("==")) req.split("==")[1] else req.split("!=")[1])).toInt()

        val qp = player.getQuestProgress(id)
        if (qp == null)
            return !req.contains("==")

        return !qp.isFinished()
    }

    private fun haveNPC(req: String, perso: Player): Boolean {
        when (perso.curMap.id) {
            9052 -> {
                if (perso.curCell.cellId == 268
                        && perso.orientation == 7)//TODO
                    return true
                // fallthrough like the original Java switch (no break)
                val cell = ArrayList<Int>()
                for (i in "168,197,212,227,242,183,213,214,229,244,245,259".split(","))
                    cell.add((i).toInt())
                if (cell.contains(perso.curCell.cellId))
                    return true
            }
            8905 -> {
                val cell = ArrayList<Int>()
                for (i in "168,197,212,227,242,183,213,214,229,244,245,259".split(","))
                    cell.add((i).toInt())
                if (cell.contains(perso.curCell.cellId))
                    return true
            }
        }
        return false
    }

    private fun haveRO(condition: String, player: Player): Boolean {
        try {
            for (cond in condition.split("&&")) {
                val split = cond.split("==")[1].split(",")
                val id = (split[0]).toInt()
                val qua = (split[1]).toInt()

                if (player.hasItemTemplate(id, qua, false)) {
                    player.removeItemByTemplateId(id, qua, false)
                    return true
                } else {
                    SocketManager.GAME_SEND_Im_PACKET(player, "14")
                    return false
                }
            }
        } catch (e: Exception) {
            log.error("unexpected error", e)
                }
        return false
    }

    private fun haveRA(condition: String, player: Player): Boolean {
        try {
            for (cond in condition.split("&&")) {
                val split = cond.split("==")[1].split(",")
                val id = (split[0]).toInt()
                val qua = (split[1]).toInt()

                if (!player.hasItemTemplate(id, qua, false))
                    return false
            }
        } catch (e: Exception) {
            log.error("unexpected error", e)
                }
        return true
    }

    private fun havePO(cond: String, perso: Player): String//On remplace les PO par leurs valeurs si possession de l'item
    {
        var Jump = false
        var ContainsPO = false
        var CutFinalLenght = true
        var copyCond = ""
        var finalLength = 0

        if (cond.contains("&&")) {
            for (cur in cond.split("&&")) {
                if (cond.contains("==")) {
                    for (cur2 in cur.split("==")) {
                        if (cur2.contains("PO")) {
                            ContainsPO = true
                            continue
                        }
                        if (Jump) {
                            copyCond += cur2
                            Jump = false
                            continue
                        }
                        if (!cur2.contains("PO") && !ContainsPO) {
                            copyCond += "$cur2=="
                            Jump = true
                            continue
                        }
                        if (cur2.contains("!="))
                            continue
                        ContainsPO = false
                        if (perso.hasItemTemplate((cur2).toInt(), 1, true)) {
                            copyCond += "${(cur2).toInt()}==${(cur2).toInt()}"
                        } else {
                            copyCond += "${(cur2).toInt()}==" + 0
                        }
                    }
                }
                if (cond.contains("!=")) {
                    for (cur2 in cur.split("!=")) {
                        if (cur2.contains("PO")) {
                            ContainsPO = true
                            continue
                        }
                        if (Jump) {
                            copyCond += cur2
                            Jump = false
                            continue
                        }
                        if (!cur2.contains("PO") && !ContainsPO) {
                            copyCond += "$cur2!="
                            Jump = true
                            continue
                        }
                        if (cur2.contains("=="))
                            continue
                        ContainsPO = false
                        if (perso.hasItemTemplate((cur2).toInt(), 1, true)) {
                            copyCond += "${(cur2).toInt()}!=${(cur2).toInt()}"
                        } else {
                            copyCond += "${(cur2).toInt()}!=" + 0
                        }
                    }
                }
                copyCond += "&&"
            }
        } else if (cond.contains("||")) {
            for (cur in cond.split("||")) {
                if (cond.contains("==")) {
                    for (cur2 in cur.split("==")) {
                        if (cur2.contains("PO")) {
                            ContainsPO = true
                            continue
                        }
                        if (Jump) {
                            copyCond += cur2
                            Jump = false
                            continue
                        }
                        if (!cur2.contains("PO") && !ContainsPO) {
                            copyCond += "$cur2=="
                            Jump = true
                            continue
                        }
                        if (cur2.contains("!="))
                            continue
                        ContainsPO = false
                        if (perso.hasItemTemplate((cur2).toInt(), 1, true)) {
                            copyCond += "${(cur2).toInt()}==${(cur2).toInt()}"
                        } else {
                            copyCond += "${(cur2).toInt()}==" + 0
                        }
                    }
                }
                if (cond.contains("!=")) {
                    for (cur2 in cur.split("!=")) {
                        if (cur2.contains("PO")) {
                            ContainsPO = true
                            continue
                        }
                        if (Jump) {
                            copyCond += cur2
                            Jump = false
                            continue
                        }
                        if (!cur2.contains("PO") && !ContainsPO) {
                            copyCond += "$cur2!="
                            Jump = true
                            continue
                        }
                        if (cur2.contains("=="))
                            continue
                        ContainsPO = false
                        if (perso.hasItemTemplate((cur2).toInt(), 1, true)) {
                            copyCond += "${(cur2).toInt()}!=${(cur2).toInt()}"
                        } else {
                            copyCond += "${(cur2).toInt()}!=" + 0
                        }
                    }
                }
                copyCond += "||"
            }
        } else {
            CutFinalLenght = false
            if (cond.contains("==")) {
                for (cur in cond.split("==")) {
                    if (cur.contains("PO"))
                        continue
                    if (cur.contains("!="))
                        continue
                    if (perso.hasItemTemplate((cur).toInt(), 1, false))
                        copyCond += "${(cur).toInt()}==${(cur).toInt()}"
                    else
                        copyCond += "${(cur).toInt()}==" + 0
                }
            }
            if (cond.contains("!=")) {
                for (cur in cond.split("!=")) {
                    if (cur.contains("PO"))
                        continue
                    if (cur.contains("=="))
                        continue
                    if (perso.hasItemTemplate((cur).toInt(), 1, false))
                        copyCond += "${(cur).toInt()}!=${(cur).toInt()}"
                    else
                        copyCond += "${(cur).toInt()}!=" + 0
                }
            }
        }
        if (CutFinalLenght) {
            finalLength = (copyCond.length - 2)//On retire les deux derniers carractères (|| ou &&)
            copyCond = copyCond.substring(0, finalLength)
        }
        return copyCond
    }

    fun canPN(cond: String, perso: Player): String//On remplace le PN par 1 et si le nom correspond == 1 sinon == 0
    {
        var copyCond = ""
        for (cur in cond.split("==")) {
            if (cur.contains("PN")) {
                copyCond += "1=="
                continue
            }
            if (perso.name.lowercase().compareTo(cur) == 0)
                copyCond += "1"
            else
                copyCond += "0"
        }
        return copyCond
    }

    fun haveDV(): Boolean {
        return World.world.getMap(325).mobGroups.size > 0
    }

    fun canPJ(cond: String, perso: Player): String//On remplace le PJ par 1 et si le metier correspond == 1 sinon == 0
    {
        var copyCond = ""
        if (cond.contains("==")) {
            val cur = cond.split("==")
            if (perso.getMetierByID((cur[1]).toInt()) != null)
                copyCond = "1==1"
            else
                copyCond = "1==0"
        } else if (cond.contains(">")) {
            if (cond.contains("||")) {
                for (cur in cond.split("||")) {
                    if (!cur.contains(">"))
                        continue
                    val _cur = cur.split(">")
                    if (!_cur[1].contains(","))
                        continue
                    val m = _cur[1].split(",")
                    val js = perso.getMetierByID((m[0]).toInt())
                    if (!copyCond.equals("", ignoreCase = true))
                        copyCond += "||"
                    if (js != null)
                        copyCond += js.get_lvl().toString() + ">" + m[1]
                    else
                        copyCond += "1==0"
                }
            } else {
                val cur = cond.split(">")
                val m = cur[1].split(",")
                val js = perso.getMetierByID((m[0]).toInt())
                if (js != null)
                    copyCond = js.get_lvl().toString() + ">" + m[1]
                else
                    copyCond = "1==0"
            }
        }
        return "1==1"
    }

    fun haveJOB(cond: String, perso: Player): String {
        var copyCond = ""
        if (perso.getMetierByID((cond.split("==")[1]).toInt()) != null)
            copyCond = "1==1"
        else
            copyCond = "0==1"
        return copyCond
    }

    fun stackIfSimilar(item: GameObject, newItem: GameObject, stack: Boolean): Boolean {
        if (item.txtStat[Constant.STATS_MIMIBIOTE] != null || newItem.txtStat[Constant.STATS_MIMIBIOTE] != null)
            return false

        when (item.template!!.id) {
            10275 -> {
                if (item.template!!.id == newItem.template!!.id)
                    return true
            }
            8378 -> {
                if (item.template!!.id == newItem.template!!.id)
                    return false
            }
        }

        for (effect1 in item.effects) {
            var ok = false
            for (effect2 in newItem.effects) {
                if (effect1.effectID == effect2.effectID && effect1.jet == effect2.jet && effect1.args == effect2.args) {
                    ok = true
                    break
                }
            }
            if (!ok)
                return false
        }


        return item.template!!.id == newItem.template!!.id && stack && item.isSameStats(newItem) && !Constant.isIncarnationWeapon(newItem.template!!.id)
                && newItem.template!!.type != Constant.ITEM_TYPE_CERTIFICAT_CHANIL
                && newItem.template!!.type != Constant.ITEM_TYPE_PIERRE_AME_PLEINE
                && newItem.template!!.type != Constant.ITEM_TYPE_OBJET_ELEVAGE
                && newItem.template!!.type != Constant.ITEM_TYPE_CERTIF_MONTURE
                && newItem.template!!.type != Constant.ITEM_TYPE_OBJET_VIVANT
                && newItem.template!!.type != Constant.ITEM_TYPE_FAMILIER
                && newItem.template!!.type != Constant.ITEM_TYPE_FANTOME_FAMILIER
                && (newItem.template!!.type != Constant.ITEM_TYPE_QUETES || Constant.isFlacGelee(item.template!!.id) || Constant.isDoplon(item.template!!.id))
                && item.position == Constant.ITEM_POS_NO_EQUIPED
    }
}
