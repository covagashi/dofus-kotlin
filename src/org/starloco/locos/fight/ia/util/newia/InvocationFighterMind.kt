package org.starloco.locos.fight.ia.util.newia

import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.ia.util.AStarPathFinding
import org.starloco.locos.fight.ia.util.Function
import org.starloco.locos.fight.ia.util.newia.action.AttackAction
import org.starloco.locos.fight.ia.util.newia.action.MoveAction
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.kernel.Constant
import java.util.LinkedList

/**
 * Created by Locos on 28/04/2018.
 */
class InvocationFighterMind(ia: AbstractEasyIA) : FighterMind(ia) {

    init {
        if (ia.getFighter().getNbrInvoc() < ia.getFighter().getTotalStats().get(Constant.STATS_SUMMON_COUNT)) {
            setup(ia)
        }
    }

    private fun setup(ia: AbstractEasyIA) {
        for (target in this.getEnemies(false)) {
            val aStar = AStarPathFinding(ia.getFight(), ia.getFighter().cell!!.cellId, target.cell!!.cellId)

            val sortedSpells = LinkedList<Spell.SortStats>()
            for (spell in ia.getInvocations()) {
                if (spell != null && ia.getFight().canLaunchSpell(ia.getFighter(), spell, target.cell!!))
                    sortedSpells.addLast(spell)
            }
            this.fightersCases.addLast(FighterCase(target, aStar, sortedSpells))
        }

        this.fightersCases.sortWith(Comparator.comparingInt { t0 -> if (t0 != null && t0.shortestPath != null) t0.shortestPath!!.size else 999 })
        this.init()
    }

    override fun init() {
        for (cas in this.fightersCases) {
            for (spell in cas.sortedSpells) {
                // Si invocation de corps-à-corps
                if (spell.maxPO <= 1) {
                    val cell = ia.getFight().map!!.getCase(PathFinding.getAvailableCellArround(ia.getFight(), ia.getFighter().cell!!.cellId, null))
                    if (cell != null && cell.isWalkableFight() && cell.firstFighter == null && ia.getFight().canCastSpell1(ia.getFighter(), spell, cell, -1)) {
                        this.highPriorityActions.addFirst(AttackAction(ia.getFighter(), cell, spell))
                    }
                // Sinon, sort de distance
                } else {
                    val cell = ia.getFight().map!!.getCase(PathFinding.getAvailableCellArround(ia.getFight(), cas.fighter.cell!!.cellId, null))
                    if (cell != null && cell.firstFighter == null && cell.isWalkableFight() && ia.getFight().canCastSpell1(ia.getFighter(), spell, cell, -1)) {
                        this.highPriorityActions.addLast(AttackAction(ia.getFighter(), cell, spell))
                    }
                }
            }
        }

        if (!this.highPriorityActions.isEmpty())
            return

        // Gérer les priorités basse
        for (cas in this.fightersCases) {
            for (spell in cas.sortedSpells) {
                val cell = ia.getFight().map!!.getCase(PathFinding.getAvailableCellArround(ia.getFight(), cas.fighter.cell!!.cellId, null))

                if (cell != null && cell.isWalkableFight() && cell.firstFighter == null && ia.getFight().canLaunchSpell(ia.getFighter(), spell, cell) && Function.getInstance().moveToAttack(ia.getFight(), ia.getFighter(), cell, spell, false)) {
                    this.lowPriorityActions.addLast(MoveAction(ia.getFighter(), spell, cell))
                    this.lowPriorityActions.addLast(AttackAction(ia.getFighter(), cell, spell))
                }
            }
        }
    }
}
