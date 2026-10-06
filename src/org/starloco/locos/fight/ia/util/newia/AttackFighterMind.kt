package org.starloco.locos.fight.ia.util.newia

import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.ia.util.AStarPathFinding
import org.starloco.locos.fight.ia.util.Function
import org.starloco.locos.fight.ia.util.newia.action.AttackAction
import org.starloco.locos.fight.ia.util.newia.action.MoveAction
import org.starloco.locos.fight.spells.Spell
import java.util.LinkedList

/**
 * Created by Locos on 28/04/2018.
 */
class AttackFighterMind(ia: AbstractEasyIA) : FighterMind(ia) {

    init {
        val attackInvocation = ia.getFighter().mob!!.template!!.id == 676

        for (target in (if (attackInvocation) this.getInvocations() else this.getEnemies(true))) {
            val cas = FighterCase(target, null, LinkedList())
            val cell = cas.getFighterCell() ?: continue
            val aStar = AStarPathFinding(ia.getFight(), ia.getFighter().cell!!.cellId, cell.cellId)

            for (spell in ia.getAttacksSpells()) {
                if (spell.effects.stream().filter { effect -> effect.effectID == 6 }.count() == 1L)
                    if (aStar.getShortestPath()!!.size - 1 <= ia.getFighter().getCurPm(ia.getFight()))
                        continue
                if (spell.effects.stream().filter { effect -> effect.effectID == 4 }.count() == 1L || ia.getFight().canLaunchSpell(ia.getFighter(), spell, target.cell!!))
                    cas.sortedSpells.addLast(spell)
            }

            cas.shortestPath = aStar.getShortestPath()
            this.fightersCases.addLast(cas)
        }

        this.fightersCases.sortWith(Comparator.comparingInt { t0 -> if (t0 != null && t0.shortestPath != null) t0.shortestPath!!.size + (if (t0.fighter.isHidden()) t0.fighter.getPm() else 0) else 999 })
        this.init()
    }

    override fun init() {
        for (cas in this.fightersCases) {
            val fighterCell = cas.getFighterCell() ?: continue

            for (spell in cas.sortedSpells) {
                if (spell.effects.stream().filter { effect -> effect.effectID == 6 }.count() == 1L) {
                    if (PathFinding.casesAreInSameLine(ia.getFight().map!!, ia.getFighter().cell!!, fighterCell, spell.maxPO))
                        this.highPriorityActions.addFirst(AttackAction(ia.getFighter(), fighterCell, spell))
                    continue
                }
                if (spell.effects.stream().filter { effect -> effect.effectID == 4 }.count() == 1L) {
                    if (cas.shortestPath!!.size - 1 > ia.getFighter().getCurPm(ia.getFight())) {
                        val cell = ia.getFight().map!!.getCase(Function.getInstance().getMaxCellForTP(ia.getFight(), ia.getFighter(), cas.fighter, spell.maxPO))
                        if (ia.getFight().canLaunchSpell(ia.getFighter(), spell, cell!!))
                            this.highPriorityActions.addLast(AttackAction(ia.getFighter(), cell, spell))
                    }
                    continue
                }
                // Si sort de corps-à-corps (fourvoiement)
                if (spell.maxPO == 0) {
                    val dist = PathFinding.getDistanceBetween(ia.getFight().map, ia.getFighter().cell!!.cellId, fighterCell.cellId)
                    // Si il est bien au corps à corps de la cible et qu'il peut le lancer sur lui
                    if (dist == 1 && ia.getFight().canCastSpell1(ia.getFighter(), spell, ia.getFighter().cell!!, -1)) {
                        this.highPriorityActions.addLast(AttackAction(ia.getFighter(), ia.getFighter().cell!!, spell))
                        continue
                    }
                }
                // Sinon, sort de distance
                if (ia.getFight().canCastSpell1(ia.getFighter(), spell, fighterCell, -1)) {
                    this.highPriorityActions.addLast(AttackAction(ia.getFighter(), fighterCell, spell))
                }
            }
        }
        if (!this.highPriorityActions.isEmpty())
            return
        // Gérer les priorités basse
        for (cas in this.fightersCases) {
            val fighterCell = cas.getFighterCell() ?: continue

            for (spell in cas.sortedSpells) {
                if (ia.getFight().canLaunchSpell(ia.getFighter(), spell, fighterCell) && Function.getInstance().moveToAttack(ia.getFight(), ia.getFighter(), fighterCell, spell, false)) {
                    this.lowPriorityActions.addLast(MoveAction(ia.getFighter(), spell, fighterCell))
                    this.lowPriorityActions.addLast(AttackAction(ia.getFighter(), fighterCell, spell))
                }
            }
        }
    }
}
