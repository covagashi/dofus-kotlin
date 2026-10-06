package org.starloco.locos.fight.ia.type.invocations

import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractNeedSpell
import org.starloco.locos.fight.ia.util.Function
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.kernel.Constant

/**
 * Created by Locos on 04/10/2015.
 */
class Tonneau(fight: Fight, fighter: Fighter, count: Byte) : AbstractNeedSpell(fight, fighter, count) {

    private val fighters = ArrayList<Int>()

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            var time = 0
            if (this.fighter.getHoldedBy() != null) {
                if (Function.getInstance().tryCastSpell(fight, fighter, fighter, 1675) == 0)
                    time = 2500
            } else {
                val fighters = this.getFightersInline(Function.getInstance().findSpell(fighter, 1674)!!)
                fighters.removeIf { f -> this.fighters.contains(f.id) }

                if (fighters.size > 0) {
                    if (Function.getInstance().tryCastSpell(fight, fighter, fighters[0], 1674) == 0) {
                        time = 2500
                    }
                    this.fighters.add(fighters[0].id)
                }
            }

            addNext({ this.decrementCount() }, time)
        } else {
            this.stop = true
        }
    }

    private fun getFightersInline(spell: Spell.SortStats): ArrayList<Fighter> {
        val fighters = ArrayList<Fighter>()
        for (target in fight.getFighters(3)) {
            if (target.team != fighter.team || target.haveState(Constant.ETAT_SAOUL)) {
                val c1 = target.cell!!.cellId
                val c2 = fighter.cell!!.cellId
                val dir = PathFinding.getDirBetweenTwoCase(c1, c2, fight.map, true)
                if (!PathFinding.isNextTo(fight.map!!, c1, c2) && PathFinding.casesAreInSameLine(fight.map!!, c1, c2, dir, 666)) {
                    if (fight.canCastSpell1(fighter, spell, target.cell!!, -1))
                        fighters.add(target)
                }
            }
        }
        return fighters
    }
}
