package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.ia.util.Function

/**
 * Created by Locos on 09/04/2018.
 */
class Cra(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    override fun run() {
        when (this.flag.toInt()) {
            0 -> { // Tir éloigné
                val spell = get().findSpell(fighter, 172)
                if (fight.canLaunchSpell(fighter, spell!!, fighter.cell!!)) {
                    if (get().moveNearIfPossible(fight, fighter, fighter.getInvocator()!!))
                        setNextParams(-1, 5, 1500)
                    else if (get().tryCastSpell(fight, fighter, fighter, spell!!.spellID) == 0)
                        this.time = 1500
                }
            }
            1 -> { // Tir puissant
                val spell = get().findSpell(fighter, 166)
                val friend = fighter.getInvocator()
                if (fight.canLaunchSpell(fighter, spell!!, friend!!.cell!!)) {
                    if (get().moveToAttack(fight, fighter, friend!!, spell))
                        setNextParams(0, 4, 1500)
                    else if (get().tryCastSpell(fight, fighter, friend!!, spell!!.spellID) == 0)
                        this.time = 1500
                }
            }
            2 -> { // Flèche ralentissante
                val spell = get().findSpell(fighter, 177)
                val enemy = get().getNearestEnnemy(fight, fighter, true)
                if (spell != null && spell.pACost <= this.fighter.getCurPa(this.fight) && enemy != null) {
                    if (Function.getInstance().tryCastSpell(this.fight, this.fighter, enemy, spell.getSpell()!!.id) == 0) {
                        this.setNextParams(1, 3, 1500)
                    } else {
                        val cell1 = fighter.cell!!
                        val cell2 = enemy.cell!!
                        val dir = PathFinding.getDirBetweenTwoCase(cell1.cellId, cell2.cellId, fight.map, true)
                        if (!PathFinding.casesAreInSameLine(fight.map!!, cell1.cellId, cell2.cellId, dir, spell.maxPO)) {
                            if (Function.getInstance().moveToAttack(fight, fighter, enemy, spell)) {
                                this.setNextParams(1, 3, 1500)
                            }
                        } else {
                            if (Function.getInstance().moveToAttack(fight, fighter, enemy, spell)) {
                                this.setNextParams(1, 3, 1500)
                            }//time = Function.getInstance().moveautourIfPossible(fight, fighter, enemy) /*? 2000 : 0*/;
                        }
                    }
                }
            }
            3 -> { // Flèche harcelante
                val target = get().getNearestEnnemy(fight, fighter, true)
                if (get().moveToAttack(fight, fighter, target, get().findSpell(fighter, 173)))
                    setNextParams(2, 2, 1500)
                else if (get().tryCastSpell(fight, fighter, target!!, 173) == 0)
                    setNextParams(2, 2, 1500)
                else {
                    get().moveFarIfPossible(fight, fighter)
                    time = 1500
                }
            }
        }
    }
}
