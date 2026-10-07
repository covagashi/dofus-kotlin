package org.starloco.locos.fight.ia.util

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.common.CryptManager
import org.starloco.locos.common.Formulas
import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.fight.CollectorFighter
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.fight.spells.Spell.SortStats
import org.starloco.locos.fight.spells.SpellEffect
import org.starloco.locos.game.action.GameAction
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant

import java.util.*
import java.util.concurrent.atomic.AtomicReference
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(Function::class.java)

/**
 * Created by Locos on 04/10/2015.
 */
class Function private constructor() {

    companion object {
        @JvmField
        val instance: Function = Function()

        @JvmStatic
        fun getInstance(): Function = instance
    }

    fun IfPossibleRasboulvulner(fight: Fight, fighter: Fighter, target: Fighter): Int// 0 = Rien, 5 = EC, 666 = NULL, 10 = SpellNull ou ActionEnCour ou Can'tCastSpell, 0 = AttaqueOK
    {
        if (fight == null || fighter == null)
            return 0
        var SS: SortStats? = null
        for(entry in  fighter.mob!!.spells.entries) {
            var a: SortStats? = entry.value
            if(a!!.spellID == 1039)
                SS = a
        }
        if (target == null)
            return 666
        if(fighter.getPa() < 14)
            return 0
        var attack: Int = fight.tryCastSpell(fighter, SS!!, target.cell!!.getId())

        if (attack != 0) {
            SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 1039, target!!.id.toString() + "", target!!.id.toString() + ",+" + 1)
            return attack
        }
        return 0
    }

    fun invoctantaIfPossible(fight: Fight, fighter: Fighter): Boolean
    {
        if (fight == null || fighter == null)
            return false
        if (fighter.nbInvocation() >= 4)
            return false
        var nearest: Fighter? = getNearestEnnemy(fight, fighter, false)
        if (nearest == null)
            return false
        var nearestCell: Int = fighter.cell!!.getId()
        var limit: Int = 50
        var _loc0_: Int = 0
        var spell: SortStats? = null
        if (fighter.haveState(36))
        {

            spell = World.world.getSort(1110)!!.getStatsByLevel(5)
            fighter.setState(36, 0)
        }
        if (fighter.haveState(37))
        {
            if(this.hasMobInFight(fight, 1091))
                return false
            spell = World.world.getSort(1109)!!.getStatsByLevel(5)
            fighter.setState(37, 0)
        }
        if (fighter.haveState(38))
        {
            if(this.hasMobInFight(fight, 1092))
                return false
            spell = World.world.getSort(1108)!!.getStatsByLevel(5)
            fighter.setState(38, 0)
        }
        if (fighter.haveState(35))
        {
            if(this.hasMobInFight(fight, 424))
                return false
            spell = World.world.getSort(1107)!!.getStatsByLevel(5)
            fighter.setState(35, 0)
        }
        while (_loc0_++ < limit)
        {
            nearestCell = PathFinding.getNearestCellAround(fight.map, nearestCell, nearest.cell!!.getId(), null)
        }
        if (nearestCell == -1) {
            nearestCell = PathFinding.getAvailableCellArround(fight, nearest.cell!!.getId(), null)
            if(nearestCell == 0)
                return false
        }
        if (spell == null)
            return false
        var invoc: Int = fight.tryCastSpell(fighter, spell, nearestCell)
        if (invoc != 0)
            return false
        return true
    }

    fun hasMobInFight(fight: Fight, id: Int): Boolean {
        for(fighter in  fight.getFighters(7))
            if(fighter.mob != null && !fighter.isDead && fighter.mob!!.template != null && fighter.mob!!.template.id == id)
                return true
        return false
    }
    fun tpIfPossibleRasboul(fight: Fight, fighter: Fighter, target: Fighter): Int// 0 = Rien, 5 = EC, 666 = NULL, 10 = SpellNull ou ActionEnCour ou Can'tCastSpell, 0 = AttaqueOK
    {
        if (fight == null || fighter == null)
            return 0
        var SS: SortStats? = null
        for(entry in  fighter.mob!!.spells.entries)
        {
            var a: SortStats? = entry.value
            if(a!!.spellID == 1041)
                SS = a
        }
        if (target == null)
            return 666
        var attack: Int = fight.tryCastSpell(fighter, SS!!, target.cell!!.getId())
        if (attack != 0)
            return attack
        return 0
    }
    fun findSpell(fighter: Fighter, id: Int): SortStats? {
        for(spell in  fighter.mob!!.spells.values) {
            if(spell != null && spell.spellID == id)
                return spell
        }
        return null
    }

    fun moveNearIfPossible(fight: Fight, F: Fighter, T: Fighter): Boolean
    {
        if (fight == null)
            return false
        if (F == null)
            return false
        if (T == null)
            return false
        if (F.getCurPm(fight) <= 0)
            return false
        var map: GameMap? = fight.map
        if (map == null)
            return false
        var cell: GameCase? = F.cell
        if (cell == null)
            return false
        var cell2: GameCase? = T.cell
        if (cell2 == null)
            return false
        if (PathFinding.isNextTo(map, cell.getId(), cell2.getId()))
            return false

        var cellID: Int = PathFinding.getNearestCellAround(map, cell2.getId(), cell.getId(), null)
        //On demande le chemin plus court
        //Mais le chemin le plus court ne prend pas en compte les bords de map.
        if (cellID == -1)
        {
            var ennemys: Map<Int,Fighter> = getLowHpEnnemyList(fight, F)!!
            for (target in  ennemys.entries)
            {
                var cellID2: Int = PathFinding.getNearestCellAround(map, target.value.cell!!.getId(), cell.getId(), null)
                if (cellID2 != -1)
                {
                    cellID = cellID2
                    break
                }
            }
        }
        var path: ArrayList<GameCase>? = AStarPathFinding(fight, cell.getId(), cell2.getId()).getShortestPath()
        if (path == null || path.isEmpty())
            return false
        var finalPath: ArrayList<GameCase> = ArrayList<GameCase>()
        for (a in 0 until F.getCurPm(fight))
        {
            if (path.size == a)
                break
            finalPath.add(path[a])
        }
        var pathstr: String = ""
        try
        {
            var curCaseID: Int = cell.getId()
            var curDir: Int = 0
            for (c in  finalPath)
            {
                var d: Char = PathFinding.getDirBetweenTwoCase(curCaseID, c.getId(), map, true)
                if (d.code == 0)
                    return false//Ne devrait pas arriver :O
                if (curDir != d.code)
                {
                    if (finalPath.indexOf(c) != 0)
                        pathstr += CryptManager.cellID_To_Code(curCaseID)
                    pathstr += d
                }
                curCaseID = c.getId()
            }
            if (curCaseID != cell.getId())
                pathstr += CryptManager.cellID_To_Code(curCaseID)
        }
        catch (e: Exception)
        {
            log.error("unexpected error", e)
                }
        //Cr�ation d'une GameAction
        var GA: GameAction = GameAction(0, 1, "")
        GA.args = pathstr
        var result: Boolean = fight.onFighterMovement(F, GA)

        return result
    }

    fun getMaxCellForTP(fight: Fight, F: Fighter, T: Fighter, dist: Int): Int {
        if (fight == null || F == null || T == null || dist < 1)
            return -1

        var map: GameMap? = fight.map
        var cell: GameCase? = F.cell
        var cell2: GameCase? = T.cell
        var temp: GameCase? = null

        if (map == null || cell == null || cell2 == null || PathFinding.isNextTo(map, cell.getId(), cell2.getId()))
            return -1

        var path: ArrayList<GameCase>? = AStarPathFinding(fight, cell.getId(), cell2.getId()).getShortestPath()

        if (path == null || path.isEmpty())
            return -1

        var cellId: Int = -1

        for (a in 0 until dist) {
            if (path.size == a) break
            temp = path[a]
            if(temp.firstFighter != null || T.id == temp.getId()) continue
                cellId = temp.getId()
        }

        return cellId
    }

    fun attackBondIfPossible(fight: Fight, fighter: Fighter, target: Fighter): Int// 0 = Rien, 5 = EC, 666 = NULL, 10 = SpellNull ou ActionEnCour ou Can'tCastSpell, 0 = AttaqueOK
    {
        if (fight == null || fighter == null)
            return 0
        var cell: Int = 0
        var SS2: SortStats? = null

        if(target == null)
            return 0
        for (S in  fighter.mob!!.spells.entries)
        {
            var cellID: Int = PathFinding.getCaseBetweenEnemy(target.cell!!.getId(), fight.map, fight)
            var effet4: Boolean = false
            var effet6: Boolean = false

            for(f in  S.value.effects)
            {
                if(f.effectID == 4)
                    effet4 = true
                if(f.effectID == 6)
                {
                    effet6 = true
                    effet4 = true
                }
            }
            if(effet4 == false)
                continue
            if(effet6 == false)
            {
                cell = cellID
                SS2 = S.value
            }else
            {
                cell = target.cell!!.getId()
                SS2 = S.value
            }
        }
        if (cell >= 15 && cell <= 463 && SS2 != null)
        {
            var attack: Int = fight.tryCastSpell(fighter, SS2, cell)
            if (attack != 0)
                return SS2.getSpell()!!.duration.toInt()
        }
        else
        {
            if (target == null || SS2 == null)
                return 0
            var attack: Int = fight.tryCastSpell(fighter, SS2, cell)
            if (attack != 0)
                return SS2.getSpell()!!.duration.toInt()
        }
        return 0
    }

   

    fun moveFarIfPossible(fight: Fight, F: Fighter): Int
    {
        if (fight == null || F == null)
            return 0
        if (fight.map == null)
            return 0
        var nbrcase: Int = 0
        //On cr�er une liste de distance entre ennemi et de cellid, nous permet de savoir si un ennemi est coll� a nous
        var dist = intArrayOf(1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000)
        var cell = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        for (i in 0 until 10)//on repete 10 fois pour les 10 joueurs ennemis potentielle
        {
            for (f in  fight.getFighters(3))
            {

                if (f.isDead)
                    continue
                if (f == F || f.team == F.team)
                    continue
                var cellf: Int = f.cell!!.getId()
                if (cellf == cell[0] || cellf == cell[1] || cellf == cell[2]
                        || cellf == cell[3] || cellf == cell[4]
                        || cellf == cell[5] || cellf == cell[6]
                        || cellf == cell[7] || cellf == cell[8]
                        || cellf == cell[9])
                    continue
                var d: Int = 0
                d = PathFinding.getDistanceBetween(fight.map, F.cell!!.getId(), f.cell!!.getId())
                if (d < dist[i])
                {
                    dist[i] = d
                    cell[i] = cellf
                }
                if (dist[i] == 1000)
                {
                    dist[i] = 0
                    cell[i] = F.cell!!.getId()
                }
            }
        }
        //if(dist[0] == 0)return false;//Si ennemi "coll�"

        var dist2 = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        var PM: Int = F.getCurPm(fight)
        var caseDepart: Int = F.cell!!.getId()
        var destCase: Int = F.cell!!.getId()
        var caseUse: ArrayList<Int> = ArrayList<Int>()
        caseUse.add(caseDepart) // On ne revient pas a sa position de d�part
        for (i in 0 .. PM)//Pour chaque PM on analyse la meilleur case a prendre. C'est a dire la plus �liogn�e de tous.
        {
            if (destCase > 0)
                caseDepart = destCase
            var curCase: Int = caseDepart

            /** En +15 **/
            curCase += 15
            var infl: Int = 0
            var inflF: Int = 0
            for (a in 0 until 10)
            {
                if (dist[a] == 0) break
                dist2[a] = PathFinding.getDistanceBetween(fight.map, curCase, cell[a])//pour chaque ennemi on calcul la nouvelle distance depuis cette nouvelle case (curCase)
                if (dist2[a] > dist[a])//Si la cellule (curCase) demander et plus distante que la pr�cedente de l'ennemi alors on dirrige le mouvement vers elle
                    infl++
            }

            if (infl > inflF
                    && curCase >= 15
                    && curCase <= 463
                    && testCotes(destCase, curCase)
                    && fight.map!!.getCase(curCase)!!.isWalkable(false, true, -1)
                    && fight.map!!.getCase(curCase)!!.fighters.isEmpty()
                    && !caseUse.contains(curCase))//Si l'influence (infl) est la plus forte en comparaison avec inflF on garde la case si celle-ci est valide
            {
                inflF = infl
                destCase = curCase
            }
            /** En +15 **/

            /** En +14 **/
            curCase = caseDepart + 14
            infl = 0

            for (a in 0 until 10)
            {
                if (dist[a] == 0) break
                dist2[a] = PathFinding.getDistanceBetween(fight.map, curCase, cell[a])
                if (dist2[a] > dist[a])
                    infl++
            }

            if (infl > inflF
                    && curCase >= 15
                    && curCase <= 463
                    && testCotes(destCase, curCase)
                    && fight.map!!.getCase(curCase)!!.isWalkable(false, true, -1)
                    && fight.map!!.getCase(curCase)!!.fighters.isEmpty()
                    && !caseUse.contains(curCase))
            {
                inflF = infl
                destCase = curCase
            }
            /** En +14 **/

            /** En -15 **/
            curCase = caseDepart - 15
            infl = 0
            for (a in 0 until 10)
            {
                if (dist[a] == 0) break
                dist2[a] = PathFinding.getDistanceBetween(fight.map, curCase, cell[a])
                if (dist2[a] > dist[a])
                    infl++
            }

            if (infl > inflF
                    && curCase >= 15
                    && curCase <= 463
                    && testCotes(destCase, curCase)
                    && fight.map!!.getCase(curCase)!!.isWalkable(false, true, -1)
                    && fight.map!!.getCase(curCase)!!.fighters.isEmpty()
                    && !caseUse.contains(curCase))
            {
                inflF = infl
                destCase = curCase
            }
            /** En -15 **/

            /** En -14 **/
            curCase = caseDepart - 14
            infl = 0
            for (a in 0 until 10)
            {
                if (dist[a] == 0) break
                dist2[a] = PathFinding.getDistanceBetween(fight.map, curCase, cell[a])
                if (dist2[a] > dist[a])
                    infl++
            }

            if (infl > inflF
                    && curCase >= 15
                    && curCase <= 463
                    && testCotes(destCase, curCase)
                    && fight.map!!.getCase(curCase)!!.isWalkable(false, true, -1)
                    && fight.map!!.getCase(curCase)!!.fighters.isEmpty()
                    && !caseUse.contains(curCase))
            {
                inflF = infl
                destCase = curCase
            }
            /** En -14 **/
            caseUse.add(destCase)
        }
        if (destCase < 15
                || destCase > 463
                || destCase == F.cell!!.getId()
                || !fight.map!!.getCase(destCase)!!.isWalkable(false, true, -1))
            return 0

        if (F.getPm() <= 0)
            return 0
        var path: ArrayList<GameCase>? = AStarPathFinding(fight, F.cell!!.getId(), destCase).getShortestPath()
        if (path == null)
            return 0
        var finalPath: ArrayList<GameCase> = ArrayList<GameCase>()
        for (a in 0 until F.getCurPm(fight))
        {
            if (path.size == a)
                break
            finalPath.add(path[a])
        }
        var pathstr: String = ""
        try
        {
            var curCaseID: Int = F.cell!!.getId()
            var curDir: Int = 0
            for (c in  finalPath)
            {
                var d: Char = PathFinding.getDirBetweenTwoCase(curCaseID, c.getId(), fight.map, true)
                if (d.code == 0)
                    return 0//Ne devrait pas arriver :O
                if (curDir != d.code)
                {
                    if (finalPath.indexOf(c) != 0)
                        pathstr += CryptManager.cellID_To_Code(curCaseID)
                    pathstr += d
                }
                curCaseID = c.getId()

                nbrcase = nbrcase + 1
            }
            if (curCaseID != F.cell!!.getId())
                pathstr += CryptManager.cellID_To_Code(curCaseID)
        }
        catch (e: Exception)
        {
            log.error("unexpected error", e)
                }
        //Cr�ation d'une GameAction
        var GA: GameAction = GameAction(0, 1, "")
        GA.args = pathstr

        if(!fight.onFighterMovement(F, GA))
            return 0

        return nbrcase * 500
    }

    fun testCotes(cellWeAre: Int, cellWego: Int): Boolean//Nous permet d'interdire le d�placement du bord vers des cellules hors map
    {
        if (cellWeAre == 15 || cellWeAre == 44 || cellWeAre == 73
                || cellWeAre == 102 || cellWeAre == 131 || cellWeAre == 160
                || cellWeAre == 189 || cellWeAre == 218 || cellWeAre == 247
                || cellWeAre == 276 || cellWeAre == 305 || cellWeAre == 334
                || cellWeAre == 363 || cellWeAre == 392 || cellWeAre == 421
                || cellWeAre == 450)
        {
            if (cellWego == cellWeAre + 14 || cellWego == cellWeAre - 15)
                return false
        }
        if (cellWeAre == 28 || cellWeAre == 57 || cellWeAre == 86
                || cellWeAre == 115 || cellWeAre == 144 || cellWeAre == 173
                || cellWeAre == 202 || cellWeAre == 231 || cellWeAre == 260
                || cellWeAre == 289 || cellWeAre == 318 || cellWeAre == 347
                || cellWeAre == 376 || cellWeAre == 405 || cellWeAre == 434
                || cellWeAre == 463)
        {
            if (cellWego == cellWeAre + 15 || cellWego == cellWeAre - 14)
                return false
        }

        if (cellWeAre >= 451 && cellWeAre <= 462)
        {
            if (cellWego == cellWeAre + 15 || cellWego == cellWeAre + 14)
                return false
        }
        if (cellWeAre >= 16 && cellWeAre <= 27)
        {
            if (cellWego == cellWeAre - 15 || cellWego == cellWeAre - 14)
                return false
        }
        return true
    }

    fun invocIfPossible(fight: Fight, fighter: Fighter): Boolean
    {
        if (fight == null || fighter == null)
            return false
        if (fighter.nbInvocation() >= fighter.getTotalStats().getEffect(Constant.STATS_SUMMON_COUNT))
            return false
        var nearest: Fighter? = getNearestEnnemy(fight, fighter, false)
        if (nearest == null)
            return false
        var nearestCell: Int = fighter.cell!!.getId()
        var limit: Int = 30
        var _loc0_: Int = 0
        var spell: SortStats? = null
        do {
            spell = getInvocSpell(fight, fighter, nearestCell)
        } while (spell == null && _loc0_++ < limit)
        {
            nearestCell = PathFinding.getCaseBetweenEnemy(fighter.cell!!.getId(), fight.map, fight)
        }
        if (nearestCell == -1)
            return false
        if (spell == null)
            return false
        var invoc: Int = fight.tryCastSpell(fighter, spell, nearestCell)
        if (invoc != 0)
            return false
        return true
    }

    fun invocIfPossibleloin(fight: Fight, fighter: Fighter, Spelllist: List<SortStats>): Boolean
    {
        if (fight == null || fighter == null)
            return false
        if (fighter.nbInvocation() >= fighter.getTotalStats().getEffect(Constant.STATS_SUMMON_COUNT))
            return false
        var nearest: Fighter? = getNearestEnnemy(fight, fighter, false)
        if (nearest == null)
            return false
        var nearestCell: Int = fighter.cell!!.getId()
        var limit: Int = 10
        var _loc0_: Int = 0
        var spell: SortStats? = null
        do {
            spell = getInvocSpellDopeul(fight, fighter, nearestCell, Spelllist)
        } while (spell == null && _loc0_++ < limit)
        {
            nearestCell = PathFinding.getNearestCellAround(fight.map,
                    nearestCell, nearest.cell!!.getId(), null)
        }
        if (nearestCell == -1 || spell == null)
            return false
        return fight.tryCastSpell(fighter, spell, nearestCell) == 0
    }

    fun getInvocSpell(fight: Fight, fighter: Fighter, nearestCell: Int): SortStats?
    {
        if (fight == null || fighter == null)
            return null
        if (fighter.mob == null)
            return null
        if (fight.map == null)
            return null
        if (fight.map!!.getCase(nearestCell) == null)
            return null
        for (SS in  fighter.mob!!.spells.entries) {
            if (!fight.canCastSpell1(fighter, SS.value!!, fight.map!!.getCase(nearestCell)!!, -1))
                continue
            for (SE in  SS.value!!.effects)
                if (SE.effectID == 181)
                    return SS.value!!
        }
        return null
    }

    fun getInvocSpellDopeul(fight: Fight, fighter: Fighter, nearestCell: Int, Spelllist: List<SortStats>): SortStats?
    {
        if (fight == null || fighter == null)
            return null
        if (fighter.mob == null)
            return null
        if (fight.map == null)
            return null
        if (fight.map!!.getCase(nearestCell) == null)
            return null
        for (SS in  Spelllist)
        {
            if (!fight.canCastSpell1(fighter, SS, fight.map!!.getCase(nearestCell)!!, -1))
                continue
            for (SE in  SS.effects)
            {
                if (SE.effectID == 181)
                    return SS
            }
        }
        return null
    }

    fun HealIfPossible(fight: Fight, f: Fighter, autoSoin: Boolean, _PDVPERmin: Int): Int//boolean pour choisir entre auto-soin ou soin alli�
    {
        var PDVPERmin = _PDVPERmin
        if (fight == null || f == null)
            return 10
        if (f.isDead)
            return 10
        if (autoSoin && (f.getPdv() * 100) / f.getPdvMax() > 95)
            return 10
        var target: Fighter? = null
        var SS: SortStats? = null
        if (autoSoin)
        {
            var PDVPER: Int = (f.getPdv() * 100) / f.getPdvMax()
            if (PDVPER < PDVPERmin && PDVPER < 95)
            {
                target = f
                SS = getHealSpell(fight, f, target)
            }
        }
        else
        //s�lection joueur ayant le moins de pv
        {
        var curF: Fighter? = null
            //int PDVPERmin = 100;
            var curSS: SortStats? = null
            for (F in  fight.getFighters(3))
            {
                if (f.isDead)
                    continue
                if (F == f)
                    continue
                if (F.isDead)
                    continue
                if (F.team == f.team)
                {
                    var PDVPER: Int = (F.getPdv() * 100) / F.getPdvMax()
                    if (PDVPER < PDVPERmin && PDVPER < 95)
                    {
                        var infl: Int = 0
                        if (f is CollectorFighter)
                        {
                            for (ss in  World.world.getGuild(f.collector.guildId)!!.spells.entries)
                            {
                                if (ss.value == null)
                                    continue
                                if (infl < calculInfluenceHeal(ss.value!!)
                                        && calculInfluenceHeal(ss.value!!) != 0
                                        && fight.canCastSpell1(f, ss.value!!, F.cell!!, -1))//Si le sort est plus interessant
                                {
                                    infl = calculInfluenceHeal(ss.value!!)
                                    curSS = ss.value!!
                                }
                            }
                        }
                        else
                        {
                            for (ss in  f.mob!!.spells.entries)
                            {
                                if (infl < calculInfluenceHeal(ss.value!!)
                                        && calculInfluenceHeal(ss.value!!) != 0
                                        && fight.canCastSpell1(f, ss.value!!, F.cell!!, -1))//Si le sort est plus interessant
                                {
                                    infl = calculInfluenceHeal(ss.value!!)
                                    curSS = ss.value!!
                                }
                            }
                        }
                        if (curSS != SS && curSS != null)
                        {
                            curF = F
                            SS = curSS
                            PDVPERmin = PDVPER
                        }
                    }
                }
            }
            target = curF
        }
        if (target == null)
            return 10
        if (target.isFullPdv())
            return 10
        if (SS == null)
            return 10
        var heal: Int = fight.tryCastSpell(f, SS, target.cell!!.getId())
        if (heal != 0)
            return SS.getSpell()!!.duration.toInt()

        return 0
    }

    fun HealIfPossible(fight: Fight, f: Fighter, autoSoin: Boolean): Boolean//boolean pour choisir entre auto-soin ou soin alli�
    {
        if (fight == null || f == null)
            return false
        if (f.isDead)
            return false
        if (autoSoin && (f.getPdv() * 100) / f.getPdvMax() > 95)
            return false
        var target: Fighter? = null
        var SS: SortStats? = null
        if (autoSoin)
        {
            target = f
            SS = getHealSpell(fight, f, target)
        }
        else
        //s�lection joueur ayant le moins de pv
        {
            var curF: Fighter? = null
            var PDVPERmin: Int = 100
            var curSS: SortStats? = null
            for (F in  fight.getFighters(3))
            {
                if (f.isDead)
                    continue
                if (F == f)
                    continue
                if (F.isDead)
                    continue
                if (F.team == f.team)
                {
                    var PDVPER: Int = (F.getPdv() * 100) / F.getPdvMax()
                    if (PDVPER < PDVPERmin && PDVPER < 95)
                    {
                        var infl: Int = 0
                        if (f is CollectorFighter)
                        {
                            for (ss in  World.world.getGuild(f.collector.guildId)!!.spells.entries)
                            {
                                if (ss.value == null)
                                    continue
                                if (infl < calculInfluenceHeal(ss.value!!)
                                        && calculInfluenceHeal(ss.value!!) != 0
                                        && fight.canCastSpell1(f, ss.value!!, F.cell!!, -1))//Si le sort est plus interessant
                                {
                                    infl = calculInfluenceHeal(ss.value!!)
                                    curSS = ss.value!!
                                }
                            }
                        }
                        else
                        {
                            for (ss in  f.mob!!.spells.entries)
                            {
                                if (infl < calculInfluenceHeal(ss.value!!)
                                        && calculInfluenceHeal(ss.value!!) != 0
                                        && fight.canCastSpell1(f, ss.value!!, F.cell!!, -1))//Si le sort est plus interessant
                                {
                                    infl = calculInfluenceHeal(ss.value!!)
                                    curSS = ss.value!!
                                }
                            }
                        }
                        if (curSS != SS && curSS != null)
                        {
                            curF = F
                            SS = curSS
                            PDVPERmin = PDVPER
                        }
                    }
                }
            }
            target = curF
        }
        if (target == null)
            return false
        if (target.isFullPdv())
            return false
        if (SS == null)
            return false
        var heal: Int = fight.tryCastSpell(f, SS, target.cell!!.getId())
        if (heal != 0)
            return false

        return true
    }

    fun buffIfPossible(fight: Fight, fighter: Fighter, target: Fighter): Boolean
    {
        if (fight == null || fighter == null)
            return false
        if (target == null)
            return false
        var SS: SortStats? = getBuffSpell(fight, fighter, target)
        if (SS == null)
            return false
        var buff: Int = fight.tryCastSpell(fighter, SS, target.cell!!.getId())
        if (buff != 0)
            return false
        return true
    }

    fun getBuffSpell(fight: Fight, F: Fighter, T: Fighter): SortStats?
    {
        if (fight == null || F == null)
            return null
        var infl: Int = -1500000
        var ss: SortStats? = null
        if (F is CollectorFighter)
        {
            for (SS in  World.world.getGuild(F.collector.guildId)!!.spells.entries)
            {
                if (SS.value == null)
                    continue
                if (infl < calculInfluence(SS.value!!, F, T)
                        && calculInfluence(SS.value!!, F, T) > 0
                        && fight.canCastSpell1(F, SS.value!!, T.cell!!, -1))//Si le sort est plus interessant
                {
                    infl = calculInfluence(SS.value!!, F, T)
                    ss = SS.value!!
                }
            }
        }
        else
        {
            for (SS in  F.mob!!.spells.entries)
            {
                var inf: Int = calculInfluence(SS.value!!, F, T)
                if (infl < inf
                        && SS.value!!.getSpell()!!.type == 1
                        && fight.canCastSpell1(F, SS.value!!, T.cell!!, -1))//Si le sort est plus interessant
                {
                    infl = calculInfluence(SS.value!!, F, T)
                    ss = SS.value!!
                }
            }
        }
        return ss
    }

    fun buffIfPossible(fight: Fight, fighter: Fighter, target: Fighter, Spelllist: List<SortStats>): Boolean
    {
        if (fight == null || fighter == null)
            return false
        if (target == null)
            return false
        var SS: SortStats? = getBuffSpellDopeul(fight, fighter, target, Spelllist)
        if (SS == null)
            return false
        var buff: Int = fight.tryCastSpell(fighter, SS, target.cell!!.getId())
        if (buff != 0)
            return true
        return false
    }

    fun getBuffSpellDopeul(fight: Fight, F: Fighter, T: Fighter, Spelllist: List<SortStats>): SortStats?
    {
        if (fight == null || F == null)
            return null
        var infl: Int = -1500000
        var ss: SortStats? = null
        for (SS in  Spelllist)
        {
            var inf: Int = calculInfluence(SS, F, T)

            if (infl < inf && SS.getSpell()!!.type == 1 && fight.canCastSpell1(F, SS, T.cell!!, -1))//Si le sort est plus interessant
            {
                infl = calculInfluence(SS, F, T)
                ss = SS
            }
        }
        return ss
    }

    fun getHealSpell(fight: Fight, F: Fighter, T: Fighter): SortStats?
    {
        if (fight == null || F == null)
            return null
        var infl: Int = 0
        var ss: SortStats? = null
        if (F is CollectorFighter)
        {
            for (SS in  World.world.getGuild(F.collector.guildId)!!.spells.entries)
            {
                if (SS.value == null)
                    continue
                if (infl < calculInfluenceHeal(SS.value!!)
                        && calculInfluenceHeal(SS.value!!) != 0
                        && fight.canCastSpell1(F, SS.value!!, T.cell!!, -1))//Si le sort est plus interessant
                {
                    infl = calculInfluenceHeal(SS.value!!)
                    ss = SS.value!!
                }
            }
        }
        else
        {
            for (SS in  F.mob!!.spells.entries)
            {
                if (SS.value == null)
                    continue
                if (infl < calculInfluenceHeal(SS.value!!)
                        && calculInfluenceHeal(SS.value!!) != 0
                        && fight.canCastSpell1(F, SS.value!!, T.cell!!, -1))//Si le sort est plus interessant
                {
                    infl = calculInfluenceHeal(SS.value!!)
                    ss = SS.value!!
                }
            }
        }
        return ss
    }

    fun moveautourIfPossible(fight: Fight, F: Fighter, T: Fighter): Int
    {
        if (fight == null)
            return 0
        if (F == null)
            return 0
        if (T == null)
            return 0
        if (F.getCurPm(fight) <= 0)
            return 0
        var map: GameMap? = fight.map
        if (map == null)
            return 0
        var cell: GameCase? = F.cell
        if (cell == null)
            return 0
        var cell2: GameCase? = T.cell
        if (cell2 == null)
            return 0
        if (PathFinding.isNextTo(map, cell.getId(), cell2.getId()))
            return 0
        var nbrcase: Int = 0

        var cellID: Int = PathFinding.getNearestCellAroundGA(map, cell2.getId(), cell.getId(), null)
        //On demande le chemin plus court
        //Mais le chemin le plus court ne prend pas en compte les bords de map.
        if (cellID == -1)
        {
            var ennemys: Map<Int,Fighter> = getLowHpEnnemyList(fight, F)!!
            for (target in  ennemys.entries)
            {
                var cellID2: Int = PathFinding.getNearestCellAroundGA(map, target.value.cell!!.getId(), cell.getId(), null)
                if (cellID2 != -1)
                {
                    cellID = cellID2
                    break
                }
            }
        }
        var path: ArrayList<GameCase>? = AStarPathFinding(fight, cell.getId(), cellID).getShortestPath()
        if (path == null || path.isEmpty())
            return 0

        var finalPath: ArrayList<GameCase> = ArrayList<GameCase>()
        for (a in 0 until F.getCurPm(fight))
        {
            if (path.size == a)
                break
            finalPath.add(path[a])
        }
        var pathstr: String = ""
        try
        {
            var curCaseID: Int = cell.getId()
            var curDir: Int = 0
            for (c in  finalPath)
            {
                var d: Char = PathFinding.getDirBetweenTwoCase(curCaseID, c.getId(), map, true)
                if (d.code == 0)
                    return 0//Ne devrait pas arriver :O
                if (curDir != d.code)
                {
                    if (finalPath.indexOf(c) != 0)
                        pathstr += CryptManager.cellID_To_Code(curCaseID)
                    pathstr += d
                }
                curCaseID = c.getId()

                nbrcase = nbrcase + 1
            }
            if (curCaseID != cell.getId())
                pathstr += CryptManager.cellID_To_Code(curCaseID)
        }
        catch (e: Exception)
        {
            log.error("unexpected error", e)
                }
        //Cr�ation d'une GameAction
        var GA: GameAction = GameAction(0, 1, "")
        GA.args = pathstr
        if(!fight.onFighterMovement(F, GA))
            return 0

        return nbrcase * 500
    }

    fun moveenfaceIfPossible(fight: Fight, F: Fighter, T: Fighter, dist: Int): Int
    {
        if (fight == null)
            return 0
        if (F == null)
            return 0
        if (T == null)
            return 0
        if (F.getCurPm(fight) <= 0)
            return 0
        var map: GameMap? = fight.map
        if (map == null)
            return 0
        var cell: GameCase? = F.cell
        if (cell == null)
            return 0
        var cell2: GameCase? = T.cell
        if (cell2 == null)
            return 0
        if (PathFinding.isNextTo(map, cell.getId(), cell2.getId()))
            return 0
        var nbrcase: Int = 0

        var cellID: Int = PathFinding.getNearestligneGA(fight, cell2.getId(), cell.getId(), null, dist)
        //On demande le chemin plus court
        //Mais le chemin le plus court ne prend pas en compte les bords de map.
        if (cellID == -1)
        {
            var ennemys: Map<Int,Fighter> = getLowHpEnnemyList(fight, F)!!
            for (target in  ennemys.entries)
            {
                var cellID2: Int = PathFinding.getNearestligneGA(fight, target.value.cell!!.getId(), cell.getId(), null, dist)
                if (cellID2 != -1)
                {
                    cellID = cellID2
                    break
                }
            }
        }
        var path: ArrayList<GameCase>? = AStarPathFinding(fight, cell.getId(), cellID).getShortestPath()//0pour en ligne
        if (path == null || path.isEmpty())
            return 0

        var finalPath: ArrayList<GameCase> = ArrayList<GameCase>()
        var ligneok: Boolean = false
        for (a in 0 until F.getCurPm(fight))
        {
            if (path.size == a)
                break
            if(ligneok == true)
                break
            if(PathFinding.casesAreInSameLine(fight.map!!, path[a].getId(), T.cell!!.getId(), 'z', 70))
                ligneok = true
            finalPath.add(path[a])
        }
        var pathstr: String = ""
        try
        {
            var curCaseID: Int = cell.getId()
            var curDir: Int = 0
            for (c in  finalPath)
            {
                var d: Char = PathFinding.getDirBetweenTwoCase(curCaseID, c.getId(), map, true)
                if (d.code == 0)
                    return 0//Ne devrait pas arriver :O
                if (curDir != d.code)
                {
                    if (finalPath.indexOf(c) != 0)
                        pathstr += CryptManager.cellID_To_Code(curCaseID)
                    pathstr += d
                }
                curCaseID = c.getId()

                nbrcase = nbrcase + 1
            }
            if (curCaseID != cell.getId())
                pathstr += CryptManager.cellID_To_Code(curCaseID)
        }
        catch (e: Exception)
        {
            log.error("unexpected error", e)
                }
        //Cr�ation d'une GameAction
        var GA: GameAction = GameAction(0, 1, "")
        GA.args = pathstr
        if(!fight.onFighterMovement(F, GA))
            return 0

        return nbrcase * 500
    }

    fun getNearestFriendNoInvok(fight: Fight, fighter: Fighter): Fighter?
    {
        if (fight == null || fighter == null)
            return null
        var dist: Int = 1000
        var curF: Fighter? = null
        for (f in  fight.getFighters(3))
        {
            if (f.isDead || (f.isInvocation() && !fighter.isInvocation()))
                continue
            if (f == fighter)
                continue
            if (f.getTeam2() == fighter.getTeam2() && !f.isInvocation())//Si c'est un ami et si c'est une invocation
            {
                var d: Int = PathFinding.getDistanceBetween(fight.map, fighter.cell!!.getId(), f.cell!!.getId())
                if (d < dist)
                {
                    dist = d
                    curF = f
                }
            }
        }
        return curF
    }

    fun getNearestFriend(fight: Fight, fighter: Fighter): Fighter?
    {
        if (fight == null || fighter == null)
            return null
        var dist: Int = 1000
        var curF: Fighter? = null
        for (f in  fight.getFighters(3))
        {
            if (f.isDead || (f.isInvocation() && !fighter.isInvocation()) || f.isHidden())
                continue
            if (f == fighter)
                continue
            if (f.team == fighter.team)//Si c'est un ami
            {
                var d: Int = PathFinding.getDistanceBetween(fight.map, fighter.cell!!.getId(), f.cell!!.getId())
                if (d < dist)
                {
                    dist = d
                    curF = f
                }
            }
        }
        return curF
    }

    fun getNearestEnnemy(fight: Fight, fighter: Fighter, invocation: Boolean): Fighter?
    {
        if (fight == null || fighter == null)
            return null
        var dist: Int = 1000
        var curF: Fighter? = null
        for (f in  fight.getFighters(3))
        {
            if (f.isDead || (!invocation && f.isInvocation()) || f.isHidden())
                continue
            if (f.getTeam2() != fighter.getTeam2())//Si c'est un ennemis
            {
                var d: Int = PathFinding.getDistanceBetween(fight.map, fighter.cell!!.getId(), f.cell!!.getId())
                if (d < dist)
                {
                    dist = d
                    curF = f
                }
            }
        }
        return curF
    }

    fun getLowHpEnnemyList(fight: Fight, fighter: Fighter): Map<Int,Fighter>?
    {
        if (fight == null || fighter == null)
            return null
        var list: MutableMap<Int,Fighter> = HashMap()
        var ennemy: MutableMap<Int,Fighter> = HashMap()
        for (f in  fight.getFighters(3))
        {
            if (f.isDead || f.isHidden())
                continue
            if (f == fighter)
                continue
            if (f.getTeam2() != fighter.getTeam2())
            {
                ennemy[f.id] = f
            }
        }
        var i: Int = 0
        var i2: Int = ennemy.size
        var curHP: Int = 10000
        var curEnnemy: Fighter? = null

        while (i < i2)
        {
            curHP = 200000
            curEnnemy = null
            for (t in  ennemy.entries)
            {
                if (t.value!!.getPdv() < curHP)
                {
                    curHP = t.value!!.getPdv()
                    curEnnemy = t.value
                }
            }
            list[curEnnemy!!.id] = curEnnemy
            ennemy.remove(curEnnemy!!.id)
            i++
        }
        return list
    }

    fun attackIfPossible(fight: Fight, fighter: Fighter, Spell: List<SortStats>): Int// 0 = Rien, 5 = EC, 666 = NULL, 10 = SpellNull ou ActionEnCour ou Can'tCastSpell, 0 = AttaqueOK
    {
        if (fight == null || fighter == null)
            return 0
        var ennemyList: Map<Int,Fighter> = getLowHpEnnemyList(fight, fighter)!!
        var SS: SortStats? = null
        var target: Fighter? = null
        for (t in  ennemyList.entries)
        {
            SS = getBestSpellForTargetDopeul(fight, fighter, t.value, fighter.cell!!.getId(), Spell)

            if (SS != null)
            {
                target = t.value
                break
            }
        }
        var curTarget: Int = 0
        var cell: Int = 0
        var SS2: SortStats? = null

        for (S in  Spell)
        {
            var targetVal: Int = getBestTargetZone(fight, fighter, S, fighter.cell!!.getId(), false)
            if (targetVal == -1 || targetVal == 0)
                continue
            var nbTarget: Int = targetVal / 1000
            var cellID: Int = targetVal - nbTarget * 1000
            if (nbTarget > curTarget)
            {
                curTarget = nbTarget
                cell = cellID
                SS2 = S
            }
        }
        if (curTarget > 0 && cell >= 15 && cell <= 463 && SS2 != null)
        {
            var attack: Int = fight.tryCastSpell(fighter, SS2, cell)
            if (attack != 0)
                return SS2.getSpell()!!.duration.toInt()
        }
        else
        {
            if (target == null || SS == null)
                return 0
            var attack: Int = fight.tryCastSpell(fighter, SS, target.cell!!.getId())
            if (attack != 0)
                return SS.getSpell()!!.duration.toInt()
        }
        return 0
    }

    fun getInfluence(fight: Fight, SS: SortStats): Int
    {
        if (fight == null)
            return 0
        var inf: Int = 0
        for (SE in  SS.effects)
        {
when (SE.effectID) {
                950 -> inf += 2500
                101 -> inf += 1000 * Formulas.getMaxJet(SE.jet)
                168 -> inf += 1500 * Formulas.getMaxJet(SE.jet)
                91, 92, 93, 94, 95, 96, 97, 98, 99, 100 -> inf += 500 * Formulas.getMiddleJet(SE.jet)
                else -> inf += Formulas.getMiddleJet(SE.jet)
            }
        }
        return inf
    }

    fun getBestSpellForTargetDopeul(fight: Fight, F: Fighter, T: Fighter, launch: Int, listspell: List<SortStats>): SortStats?
    {
        if (fight == null || F == null)
            return null
        var inflMax: Int = 0
        var ss: SortStats? = null


        for (SS in  listspell)
        {
            if (SS.getSpell()!!.type != 0)
                continue
            var curInfl: Int = 0
            var Infl1: Int = 0
            var Infl2: Int = 0
            var PA: Int = F.mob!!.pa
            var usedPA = intArrayOf(0, 0)
            if (!fight.canCastSpell1(F, SS, T.cell!!, launch))
                continue
            curInfl = getInfluence(fight, SS)
            if(curInfl == 0)continue
            if (curInfl > inflMax)
            {
                ss = SS
                usedPA[0] = ss.pACost
                Infl1 = curInfl
                inflMax = Infl1
            }

            for (SS2 in  listspell)
            {
                if (SS2.getSpell()!!.type != 0)
                    continue
                if ((PA - usedPA[0]) < SS2.pACost)
                    continue
                if (!fight.canCastSpell1(F, SS2, T.cell!!, launch))
                    continue
                curInfl = getInfluence(fight, SS2)
                if(curInfl == 0)continue
                if ((Infl1 + curInfl) > inflMax)
                {
                    ss = SS
                    usedPA[1] = SS2.pACost
                    Infl2 = curInfl
                    inflMax = Infl1 + Infl2
                }
                for (SS3 in  listspell)
                {
                    if (SS3.getSpell()!!.type != 0)
                        continue
                    if ((PA - usedPA[0] - usedPA[1]) < SS3.pACost)
                        continue
                    if (!fight.canCastSpell1(F, SS3, T.cell!!, launch))
                        continue

                    curInfl = getInfluence(fight, SS3)
                    if(curInfl == 0)continue
                    if ((curInfl + Infl1 + Infl2) > inflMax)
                    {
                        ss = SS
                        inflMax = curInfl + Infl1 + Infl2
                    }
                }
            }
        }
        return ss
    }

    fun getBestTargetZone(fight: Fight, fighter: Fighter, spell: SortStats, launchCell: Int, line: Boolean): Int
    {
        if (fight == null || fighter == null)
            return 0
        if (spell.porteeType.isEmpty()
                || (spell.porteeType[0] == 'P' && spell.porteeType[1] == 'a')
                || spell.isLineLaunch && !line)
        {
            return 0
        }
        var possibleLaunch: MutableList<GameCase> = ArrayList()
        var CellF: Int = -1
        if (spell.maxPO != 0)
        {
            var arg1: Char = 'C'
            var table: CharArray = charArrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v')
            var arg2: Char = 'a'
            if (spell.maxPO > 20)
            {
                arg2 = 'u'
            }
            else
            {
                arg2 = table[spell.maxPO]
            }
            var args: String = Character.toString(arg1) + Character.toString(arg2)
            possibleLaunch = PathFinding.getCellListFromAreaString(fight.map, launchCell, launchCell, args, 0, false)!!.toMutableList()
        }
        else
        {
            possibleLaunch.add(fight.map!!.getCase(launchCell)!!)
        }

        if (possibleLaunch == null)
        {
            return -1
        }
        var nbTarget: Int = 0
        for (cell in  possibleLaunch)
        {
            try
            {
                if (!fight.canCastSpell1(fighter, spell, cell, launchCell))
                    continue
                var curTarget: Int = 0
                var cells: List<GameCase> = PathFinding.getCellListFromAreaString(fight.map, cell.getId(), launchCell, spell.porteeType, 0, false)
                for (c in  cells)
                {
                    if (c == null)
                        continue
                    if (c.firstFighter == null)
                        continue
                    if (c.firstFighter!!.getTeam2() != fighter.getTeam2())
                        curTarget++
                }
                if (curTarget > nbTarget)
                {
                    if(cell.firstFighter != null && cell.firstFighter!!.team == fighter.team)
                        continue
                    nbTarget = curTarget
                    CellF = cell.getId()
                }
            }
            catch (e: Exception)
            {
                log.error("unexpected error", e)
                }
        }
        if (nbTarget > 0 && CellF != -1)
            return CellF + nbTarget * 1000
        else
            return 0
    }

    fun calculInfluenceHeal(ss: SortStats): Int
    {
        var inf: Int = 0
        for (SE in  ss.effects)
        {
            if (SE.effectID != 108)
                return 0
            inf += 100 * Formulas.getMiddleJet(SE.jet)
        }

        return inf
    }

    fun calculInfluence(ss: SortStats, C: Fighter, T: Fighter): Int
    {
        var infTot: Int = 0
        for (SE in  ss.effects)
        {
            var inf: Int = 0
            when (SE.effectID) {
                5 -> inf = 500 * Formulas.getMiddleJet(SE.jet)
                89 -> inf = 200 * Formulas.getMiddleJet(SE.jet)
                91, 92, 93, 94, 95 -> inf = 150 * Formulas.getMiddleJet(SE.jet)
                96, 97, 98, 99, 100 -> inf = 100 * Formulas.getMiddleJet(SE.jet)
                101, 127 -> inf = 1000 * Formulas.getMiddleJet(SE.jet)
                84, 77 -> inf = 1500 * Formulas.getMiddleJet(SE.jet)
                111, 128 -> inf = -1000 * Formulas.getMiddleJet(SE.jet)
                121 -> inf = -100 * Formulas.getMiddleJet(SE.jet)
                131 -> inf = 300 * Formulas.getMiddleJet(SE.jet)
                132 -> inf = 2000
                138 -> inf = -50 * Formulas.getMiddleJet(SE.jet)
                150 -> inf = -2000
                168, 169 -> inf = 1000 * Formulas.getMiddleJet(SE.jet)
                210, 211, 212, 213, 214 -> inf = -300 * Formulas.getMiddleJet(SE.jet)
                215, 216, 217, 218, 219 -> inf = 300 * Formulas.getMiddleJet(SE.jet)
                265 -> {
                    inf = -250 * Formulas.getMiddleJet(SE.jet)
                    // fallthrough
                    inf = -1000
                }
                765 -> inf = -1000
                786 -> inf = -1000
                106 -> inf = -900
            }

            if (C.team == T.team)
                infTot -= inf
            else
                infTot += inf
        }
        return infTot
    }




    /**
     * NEW IA FUNCTION RESTORED
     */

    fun moveToAttack(fight: Fight, caster: Fighter, target: Fighter?, spell: SortStats?): Boolean {
        return target != null && moveToAttack(fight, caster, target.cell, spell, true)
    }
    /**
     * Move if needed to cast his spell
     * If can launch his spell, he don't move
     */
    fun moveToAttack(fight: Fight, caster: Fighter, cellTarget: GameCase?, spell: SortStats?, doneMove: Boolean): Boolean {
        var cellTarget2: GameCase? = cellTarget
        if (fight == null || caster == null || caster.getCurPm(fight) <= 0)
            return false

        var map: GameMap? = fight.map
        var cell: GameCase? = caster.cell

        if (map == null || cell == null || cellTarget2 == null)
            return false
        if (fight.canCastSpell1(caster, spell, cell, cellTarget2.getId()) || (spell != null && !fight.canLaunchSpell(caster, spell, cellTarget2)) || PathFinding.isNextTo(map, cell.getId(), cellTarget2.getId()))
            return false
        var dist: Int = PathFinding.getDistanceBetweenTwoCase(fight.map, cell, cellTarget2)
        if(spell != null && dist < spell.minPO && spell.minPO > 1) {
            var count: Byte = 0
            var temp: GameCase = cell
            do {
                var cells: List<GameCase> = this.getCellsAvailableAround(fight, temp, true, (0).toByte())
                if(!cells.isEmpty()) {
                    for(c in  cells) {
                        var tmpDist: Int = PathFinding.getDistanceBetweenTwoCase(fight.map, c, cellTarget2)
                        if(tmpDist > dist && fight.canCastSpell1(caster, spell, c, cellTarget2.getId())) {
                            temp = c
                            dist = tmpDist
                            break
                        }
                    }
                }
                count++
            } while(dist < spell.minPO && count < 30)
            if(temp.getId() != cell.getId() && fight.canCastSpell1(caster, spell, temp, cellTarget2.getId()))
                return moveToCell(fight, caster, cell, temp, doneMove)
        }

        var path: ArrayList<GameCase>?
        var finalPath: ArrayList<GameCase> = ArrayList()

        if(spell != null && spell.isLineLaunch && PathFinding.casesAreInSameLine(fight.map!!, cell.getId(), cellTarget2.getId(), 'z', spell.maxPO)) {
            var id: Int = this.getCellToBeInTheSameLine(fight, spell, cell.getId(), cellTarget2.getId())
            if(id != -1)
                cellTarget2 = fight.map!!.getCase(id)
        }


        path = AStarPathFinding(fight, cell.getId(), cellTarget2!!.getId()).getShortestPath()

        if (path == null || path.isEmpty())
            return false

        var stop: GameCase? = null
        var next: GameCase? = null


        var countPm: Int = 0
        var tmp: GameCase? = null
        for(c in  path) {
            countPm ++
            if(countPm <= caster.getCurPm(fight)) {
                if(fight.canCastSpell1(caster, spell, c, cellTarget2.getId())) {
                    stop = c
                    break
                }
            } else {
                stop = tmp
                break
            }
            tmp = c
        }

        if(!doneMove && stop != null) return true
        else if (!doneMove) return false

        for (a in 0 until caster.getCurPm(fight)) {
            if (path.size == a) break
            next = path[a]
            finalPath.add(next)
            if(stop != null && next!!.getId() == stop.getId())
                break
        }

        var str: StringBuilder = StringBuilder()
        try {
            var curCell: Int = cell.getId()

            for (c in  finalPath) {
                var dir: Char = PathFinding.getDirBetweenTwoCase(curCell, c.getId(), map, true)
                if (dir.code == 0) return false //Ne devrait pas arriver :O

                if (finalPath.indexOf(c) != 0)
                    str.append(CryptManager.cellID_To_Code(curCell))
                str.append(dir)
                curCell = c.getId()
            }
            if (curCell != cell.getId())
                str.append(CryptManager.cellID_To_Code(curCell))
        } catch (e: Exception) {
            log.error("unexpected error", e)
                }

        var GA: GameAction = GameAction(0, 1, "")
        GA.args = str.toString()
        return fight.onFighterMovement(caster, GA)
    }

    private fun moveToCell(fight: Fight, fighter: Fighter, cell: GameCase, cellTarget: GameCase, doneMove: Boolean): Boolean {
        var path: ArrayList<GameCase>? = AStarPathFinding(fight, cell.getId(), cellTarget.getId()).getShortestPath()

        if (path == null || path.isEmpty())
            return false

        var str: StringBuilder = StringBuilder()
        try {
            var curCell: Int = cell.getId()

            for (c in  path) {
                var dir: Char = PathFinding.getDirBetweenTwoCase(curCell, c.getId(), fight.map, true)
                if (dir.code == 0) return false //Ne devrait pas arriver :O

                if (path.indexOf(c) != 0)
                    str.append(CryptManager.cellID_To_Code(curCell))
                str.append(dir)
                curCell = c.getId()
            }
            if (curCell != cell.getId())
                str.append(CryptManager.cellID_To_Code(curCell))
        } catch (e: Exception) {
            log.error("unexpected error", e)
                }

        var pathRef: AtomicReference<String> = AtomicReference(str.toString())
        var nStep: Int = PathFinding.isValidPath(fight.map!!, fighter.cell!!.getId(), pathRef, fight, null, -1)
        str = StringBuilder(pathRef.get())

        if (nStep == 0 || nStep > fight.curFighterPm || nStep == -1000)
            return false
        else if(!doneMove)
            return true

        var GA: GameAction = GameAction(0, 1, "")
        GA.args = str.toString()
        return fight.onFighterMovement(fighter, GA)
    }

    fun getCellToBeInTheSameLine(fight: Fight, spell: Spell.SortStats, cellStart: Int, cellEnd: Int): Int {
        val map: MutableMap<Short,List<Int>> = HashMap()

        for (c in  charArrayOf('b', 'd', 'f', 'h')) {
            val cells: MutableList<Int> = ArrayList()
            var id: Int = PathFinding.getCaseIDFromDirrection(cellStart, c, fight.map!!)
            for (i in 0 until spell.maxPO) {
                var cell: GameCase? = fight.map!!.getCase(id)

                if (cell!!.firstFighter == null && cell!!.isWalkable(true, true, -1) && id != cellEnd) {
                    if (PathFinding.casesAreInSameLine(fight.map!!, id, cellEnd, 'z', spell.maxPO)) {
                        if (spell.hasLDV() && !Formulas.checkLos(fight.map, (cell!!.getId().toShort()), (cellEnd.toShort())))
                            break

                        cells.add(id)
                        map[cells.size.toShort()] = cells
                        break
                    } else {
                        cells.add(id)
                        id = PathFinding.getCaseIDFromDirrection(cell!!.getId(), c, fight.map!!)
                    }
                }
            }
        }
        var size: Int = 99
        var chooseCell: Int = -1
        for (entry in  map.entries) {
            if (entry.key.toInt() < size) {
                size = entry.key.toInt()
                chooseCell = entry.value[entry.value.size - 1]
            }
        }
        return chooseCell
    }

    fun getBestBuffSpell(fight: Fight, caster: Fighter, target: Fighter): SortStats? {
        if (fight == null || caster == null)
            return null
        var influence: Int = -1500000
        var ss: SortStats? = null
        var spells: Collection<SortStats?> = (if (caster is CollectorFighter) World.world.getGuild(caster.collector.guildId)!!.spells.values else caster.mob!!.spells.values)

        for (tmp in  spells) {
            var i: Int = calculInfluence(tmp!!, caster, target)
            if (influence < i && tmp!!.getSpell()!!.type == 1) {
                influence = i
                ss = tmp
            }
        }
        return ss
    }

    fun getBestHealSpell(fight: Fight, caster: Fighter, target: Fighter): SortStats? {
        if (fight == null || caster == null)
            return null
        var influence: Int = 0
        var ss: SortStats? = null

        var spells: Collection<SortStats?> = (if (caster is CollectorFighter) World.world.getGuild(caster.collector.guildId)!!.spells.values else caster.mob!!.spells.values)
        for (tmp in  spells) {
            if (influence < calculInfluenceHeal(tmp!!) && calculInfluenceHeal(tmp!!) != 0) {
                influence = calculInfluenceHeal(tmp!!)
                ss = tmp
            }
        }
        return ss
    }

    fun getSpellByPo(caster: Fighter, po: Int): SortStats? {
        var spell: SortStats? = null
        var maxPo: Int = 0
        for (tmp in  caster.mob!!.spells.values) {
            if (tmp != null && tmp.maxPO > maxPo && tmp.maxPO <= po) {
                spell = tmp
                maxPo = tmp.maxPO
            }
        }
        return spell
    }

    fun getEnnemyWithDistance(fight: Fight, fighter: Fighter, min: Int, _max: Int, fighters: List<Fighter>?): Fighter? {
        var max = _max
        if (fight == null || fighter == null)
            return null
        var target: Fighter? = null
        var i: Byte = 0
        var near: Int = 150000

        while((i.toInt() == 0 || i.toInt() == 1) && target == null) { // If we don't found fighter, try to find an invocation
            for (f in  fight.getFighters(3)) {
                if(i.toInt() == 0 && ((f.isInvocation() && !fighter.isInvocation()) ||  f.isStatic()))
                    continue
                // If we want another fighter (limited by the spell)
                if (f.isDead || (fighters != null && fighters.contains(f)) || f.isHidden())
                    continue
                if (f.getTeam2() != fighter.getTeam2()) { // If it's an ennemy
                    var distance: Int = PathFinding.getDistanceBetween(fight.map, fighter.cell!!.getId(), f.cell!!.getId())
                    if (distance <= max && distance >= min && distance < near) {
                        max = distance
                        target = f
                    }
                }
            }
            i++
        }
        return target
    }

    fun tryCastSpell(fight: Fight, fighter: Fighter, target: Fighter, spellId: Int): Int {
        if(target == null)
            return 10
        return tryCastSpell(fight, fighter, target.cell!!, spellId)
    }

    fun tryCastSpell(fight: Fight, fighter: Fighter, target: GameCase, spellId: Int): Int {
        if (fight == null || fighter == null || target == null)
            return 10
        var spell: Spell.SortStats? = Function.getInstance().findSpell(fighter, spellId)
        return fight.tryCastSpell(fighter, spell!!, target.cellId)
    }

    /**
     * ===================================================================================
     *
     * ===================================================================================
     *
     */

    //region Abstract Eazy IA
    fun tryCastSpell(ia: AbstractEasyIA, target: Fighter?, spell: SortStats?): Boolean {
        return ia != null && target != null && ia.getFight().tryCastSpell(ia.getFighter(), spell!!, target.cell!!.getId()) == 0
    }
    //endregion

    //region Trap/Glyph finder

    fun getCellsAround(fight: Fight, launch: GameCase): List<GameCase> {
        var cells: MutableList<GameCase> = ArrayList()
        var available: MutableList<GameCase> = ArrayList()
        var dirs: CharArray = charArrayOf('b', 'd', 'f', 'h')
        if(fight == null) return cells

        for (dir in  dirs) {
            var cell: GameCase? = fight.map!!.getCase(PathFinding.GetCaseIDFromDirection(launch.getId(), dir, fight.map, true))
            if (cell != null && cell.isWalkable(true, true, -1))
                available.add(cell!!)
        }
        return available
    }

    /**
     * Get cell around the target once time or more for trap (size <= 3)
     * @param fight Fight
     * @param once true if you just want the cell around the target, false if you want more than this
     * @param iteration
     * @return list of cells available
     */
    fun getCellsAvailableAround(fight: Fight, launch: GameCase, once: Boolean, iteration: Byte): MutableList<GameCase> {
        var cells: MutableList<GameCase> = ArrayList()
        var available: MutableList<GameCase> = ArrayList()
        var dirs: CharArray = charArrayOf('b', 'd', 'f', 'h')
        if(fight == null || launch == null) return cells

        val map: GameMap? = fight.map
        if(map == null) return available

        for (dir in  dirs) {
            var cell: GameCase? = map.getCase(PathFinding.GetCaseIDFromDirection(launch.getId(), dir, map, true))
            if (once || (cell != null && cellAvailable(fight, cell))) available.add(cell!!)
            if(!once) cells.add(cell!!)
        }
        if(!once) {
            for (c in  cells) {
                if(c == null) continue
                for (dir in  dirs) {
                    var cell: GameCase? = map.getCase(PathFinding.GetCaseIDFromDirection(c.getId(), dir, map, true))
                    if (cell != null && cellAvailable(fight, cell))
                        available.add(cell!!)
                }
            }
        }
        return available
    }

    fun getCellsAvailableAround(target: Fighter, once: Boolean, iteration: Byte): MutableList<GameCase> {
        return this.getCellsAvailableAround(target.fight, target.cell!!, once, iteration)
    }

    private fun cellAvailable(fight: Fight, cell: GameCase): Boolean {
        return cell.firstFighter == null && cell.isWalkable(true, true, cell.getId()) && fight.traps.stream().filter({ t -> t.cell.getId() == cell.getId() }).count() == 0L
    }
    //endregion
}
