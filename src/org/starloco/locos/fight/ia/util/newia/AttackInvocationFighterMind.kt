package org.starloco.locos.fight.ia.util.newia

import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.ia.util.AStarPathFinding
import org.starloco.locos.fight.ia.util.Function
import org.starloco.locos.fight.ia.util.newia.action.AttackAction
import org.starloco.locos.fight.ia.util.newia.action.MoveAction
import org.starloco.locos.fight.spells.Spell
import java.util.ArrayList
import java.util.LinkedList

/**
 * Created by Locos on 28/04/2018.
 */
class AttackInvocationFighterMind(ia: AbstractEasyIA, private val friend: Boolean) : FighterMind(ia) {

    init {
        val fighters = ArrayList<Fighter>()
        fighters.addAll(this.getFriends())
        fighters.addAll(this.getEnemies(false))
        for (target in fighters) {
            val aStar = AStarPathFinding(ia.getFight(), ia.getFighter().cell!!.cellId, target.cell!!.cellId)

            val sortedSpells = LinkedList<Spell.SortStats>()
            for (spell in ia.getAttacksSpells()) {
                if (ia.getFight().canLaunchSpell(ia.getFighter(), spell, target.cell!!))
                    sortedSpells.addLast(spell)
            }

            this.fightersCases.addLast(FighterCase(target, aStar, sortedSpells))
        }

        this.fightersCases.sortWith(Comparator.comparingInt { t0 -> if (t0 != null && t0.shortestPath != null) t0.shortestPath!!.size else 999 })
        this.init()
    }

    override fun init() {
        for (cas in this.fightersCases) {
            if (friend && cas.fighter.team == ia.getFighter().team) continue
            for (spell in cas.sortedSpells) {
                // Si sort de corps-à-corps (fourvoiement)
                val dist = PathFinding.getDistanceBetween(ia.getFight().map, ia.getFighter().cell!!.cellId, cas.fighter.cell!!.cellId)
                // Si il est bien au corps à corps de la cible et qu'il peut le lancer sur lui
                if (dist == 1 && ia.getFight().canCastSpell1(ia.getFighter(), spell, cas.fighter.cell!!, -1)) {
                    this.highPriorityActions.addLast(AttackAction(ia.getFighter(), cas.fighter.cell!!, spell))
                }
                // Pas de sort à distance dans ce genre d'ia
            }
        }
        if (!this.highPriorityActions.isEmpty())
            return
        // Gérer les priorités basse
        for (cas in this.fightersCases) {
            if (friend && cas.fighter.team == ia.getFighter().team) continue
            for (spell in cas.sortedSpells) {
                if (ia.getFight().canLaunchSpell(ia.getFighter(), spell, cas.fighter.cell!!) && Function.getInstance().moveToAttack(ia.getFight(), ia.getFighter(), cas.fighter.cell!!, spell, false)) {
                    this.lowPriorityActions.addLast(MoveAction(ia.getFighter(), spell, cas.fighter.cell!!))
                    this.lowPriorityActions.addLast(AttackAction(ia.getFighter(), cas.fighter.cell!!, spell))
                }
            }
        }
    }
}
