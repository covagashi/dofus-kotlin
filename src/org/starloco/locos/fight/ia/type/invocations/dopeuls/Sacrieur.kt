package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.ia.util.Function

/**
 * Created by Locos on 09/04/2018.
 */
class Sacrieur(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    override fun run() {
        when (this.flag.toInt()) {
            0 -> { // Chatiment agile
                val spell = get().findSpell(fighter, 437)
                if (spell != null) {
                    if (get().tryCastSpell(fight, fighter, fighter, spell.spellID) == 0)
                        this.time = 1500
                }
            }
            1 -> { // Attirance
                val spell = get().findSpell(fighter, 434)
                val enemy = get().getNearestEnnemy(fight, fighter, true)

                if (enemy != null) {
                    if (get().tryCastSpell(this.fight, this.fighter, enemy, spell!!.getSpell()!!.id) == 0) {
                        this.setNextParams(1, 3, 1500)
                    } else {
                        val cell1 = fighter.cell
                        val cell2 = enemy.cell
                        if (cell1 != null && cell2 != null) {
                            val dir = PathFinding.getDirBetweenTwoCase(cell1.cellId, cell2.cellId, fight.map, true)
                            if (!PathFinding.casesAreInSameLine(fight.map!!, cell1.cellId, cell2.cellId, dir, spell!!.maxPO)) {
                                if (Function.getInstance().moveenfaceIfPossible(fight, fighter, enemy, spell!!.maxPO) > 0) {
                                    setNextParams(0, 4, 1500)
                                }
                            }
                        }
                    }
                }
            }
            2 -> { // Pied du sacrieur
                val target = get().getNearestEnnemy(fight, fighter, true)
                val spell = get().findSpell(fighter, 432)
                if (spell != null && target != null) {
                    if (get().tryCastSpell(fight, fighter, target, spell.spellID) == 0)
                        this.setNextParams(1, 3, 1500)
                }
            }
            3 -> { // Absorption
                val target = get().getNearestEnnemy(fight, fighter, true)
                val spell = get().findSpell(fighter, 442)
                if (spell != null && target != null) {
                    if (get().moveToAttack(fight, fighter, target, spell))
                        setNextParams(2, 2, 1500)
                    else if (get().tryCastSpell(fight, fighter, target, spell.spellID) == 0)
                        setNextParams(2, 2, 1500)
                    else {
                        get().moveFarIfPossible(fight, fighter)
                        this.time = 1500
                    }
                }
            }
        }
    }
}
