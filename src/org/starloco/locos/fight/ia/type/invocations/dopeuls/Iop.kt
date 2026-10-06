package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.ia.util.Function

/**
 * Created by Locos on 09/04/2018.
 */
class Iop(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    override fun run() {
        when (this.flag.toInt()) {
            0 -> if (get().tryCastSpell(fight, fighter, fighter, 147) == 0) // Guide de bravoure
                this.time = 1500

            1 -> { // Puissance
                val spell = get().findSpell(fighter, 153)
                val friend = fighter.getInvocator()
                if (fight.canLaunchSpell(fighter, spell!!, friend!!.cell!!)) {
                    if (get().moveToAttack(fight, fighter, friend!!, spell))
                        setNextParams(0, 4, 1500)
                    else if (get().tryCastSpell(fight, fighter, friend!!, spell!!.spellID) == 0)
                        this.time = 1500
                }
            }
            2 -> if (get().tryCastSpell(fight, fighter, fighter, 148) == 0) // Amplification
                this.time = 1500

            3 -> { // Couper
                val spell = get().findSpell(fighter, 150)
                val enemy = get().getNearestEnnemy(fight, fighter, true)

                if (spell != null && spell.pACost <= this.fighter.getCurPa(this.fight) && enemy != null) {
                    if (Function.getInstance().tryCastSpell(this.fight, this.fighter, enemy, spell.getSpell()!!.id) == 0) {
                        setNextParams(2, 2, 1500)
                        return
                    } else {
                        val cell1 = fighter.cell!!
                        val cell2 = enemy.cell!!
                        val dir = PathFinding.getDirBetweenTwoCase(cell1.cellId, cell2.cellId, fight.map, true)
                        if (!PathFinding.casesAreInSameLine(fight.map!!, cell1.cellId, cell2.cellId, dir, spell.maxPO)) {
                            if (Function.getInstance().moveToAttack(fight, fighter, enemy, spell)) {
                                setNextParams(2, 2, 1750)
                                return
                            }
                        } else {
                            if (Function.getInstance().moveenfaceIfPossible(fight, fighter, enemy, spell.maxPO) > 0) {
                                setNextParams(2, 2, 2000)
                                return
                            }
                        }
                    }
                }
                time = get().moveFarIfPossible(fight, fighter).toShort()
            }
        }
    }
}
