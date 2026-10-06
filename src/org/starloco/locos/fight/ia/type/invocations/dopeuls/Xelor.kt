package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA

/**
 * Created by Locos on 09/04/2018.
 */
class Xelor(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    override fun run() {
        when (this.flag.toInt()) {
            0 -> if (get().invocIfPossible(fight, fighter)) // Invocation Cadran du xélor
                time = 1500

            1 -> { // Protection aveuglante
                val spell = get().findSpell(fighter, 94)
                val friend = fighter.getInvocator()
                if (fight.canLaunchSpell(fighter, spell!!, friend!!.cell!!)) {
                    if (get().moveToAttack(fight, fighter, friend!!, spell)) {
                        setNextParams(0, 4, 1500)
                    } else if (get().tryCastSpell(fight, fighter, friend!!, spell!!.spellID) == 0 || get().tryCastSpell(fight, fighter, fighter, spell!!.spellID) == 0) {
                        this.time = 1500
                    }
                }
            }
            2 -> { // Démotivation
                val target = get().getNearestEnnemy(fight, fighter, true)
                if (get().moveToAttack(fight, fighter, target, get().findSpell(fighter, 87)))
                    setNextParams(1, 3, 1500)
                else if (get().tryCastSpell(fight, fighter, target!!, 87) == 0)
                    this.time = 1500
            }
            3 -> { // Aiguille
                val target = get().getNearestEnnemy(fight, fighter, true)
                if (get().moveToAttack(fight, fighter, target, get().findSpell(fighter, 83)))
                    setNextParams(2, 2, 1500)
                else if (get().tryCastSpell(fight, fighter, target!!, 83) == 0)
                    setNextParams(2, 2, 1500)
                else {
                    get().moveFarIfPossible(fight, fighter)
                    time = 1500
                }
            }
        }
    }
}
