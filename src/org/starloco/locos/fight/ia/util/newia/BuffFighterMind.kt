package org.starloco.locos.fight.ia.util.newia

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
class BuffFighterMind(ia: AbstractEasyIA) : FighterMind(ia) {

    init {
        for (target in this.getFriends()) {
            val aStar = AStarPathFinding(ia.getFight(), ia.getFighter().cell!!.cellId, target.cell!!.cellId)

            val sortedSpells = LinkedList<Spell.SortStats>()
            for (spell in ia.getFriendBuffsSpells()) {
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
                // Si sort de corps-à-corps ((buff sois-même))
                if (spell.maxPO == 0 && cas.fighter == ia.getFighter()) {
                    if (ia.getFight().canCastSpell1(ia.getFighter(), spell, ia.getFighter().cell!!, -1)) {
                        this.highPriorityActions.addFirst(AttackAction(ia.getFighter(), ia.getFighter().cell!!, spell))
                    }
                // Sinon, sort de distance
                } else if (ia.getFight().canCastSpell1(ia.getFighter(), spell, cas.fighter.cell!!, -1)) {
                    this.highPriorityActions.addLast(AttackAction(ia.getFighter(), cas.fighter.cell!!, spell))
                }
            }
        }

        if (!this.highPriorityActions.isEmpty())
            return

        // Gérer les priorités basse
        for (cas in this.fightersCases) {
            for (spell in cas.sortedSpells) {
                if (ia.getFight().canLaunchSpell(ia.getFighter(), spell, cas.fighter.cell!!) && Function.getInstance().moveToAttack(ia.getFight(), ia.getFighter(), cas.fighter.cell!!, spell, false)) {
                    this.lowPriorityActions.addLast(MoveAction(ia.getFighter(), spell, cas.fighter.cell!!))
                    this.lowPriorityActions.addLast(AttackAction(ia.getFighter(), cas.fighter.cell!!, spell))
                }
            }
        }
    }
}
