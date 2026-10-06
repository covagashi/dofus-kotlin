package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA

/**
 * Created by Locos on 09/04/2018.
 */
class Eniripsa(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    override fun run() {
        when (this.flag.toInt()) {
            0 -> { // Mot d'épine
                val spell = get().findSpell(fighter, 132)
                val friend = fighter.getInvocator()
                if (fight.canLaunchSpell(fighter, spell!!, friend!!.cell!!)) {
                    if (get().moveToAttack(fight, fighter, friend!!, spell))
                        setNextParams(-1, 5, 1500)
                    else if (get().tryCastSpell(fight, fighter, friend!!, spell!!.spellID) == 0)
                        this.time = 1500
                }
            }
            1 -> if (get().tryCastSpell(fight, fighter, fighter, 2021) == 0) // Mot d'envol
                this.time = 1500

            2 -> { // Mot drainant
                val spell = get().findSpell(fighter, 123)
                val target = get().getNearestEnnemy(fight, fighter, true)
                if (get().moveToAttack(fight, fighter, target, spell))
                    setNextParams(1, 3, 1500)
                else if (get().tryCastSpell(fight, fighter, target!!, spell!!.spellID) == 0)
                    this.time = 1500
            }
            3 -> { // Mot blessant
                val target = get().getNearestEnnemy(fight, fighter, true)
                if (get().moveToAttack(fight, fighter, target, get().findSpell(fighter, 122)))
                    setNextParams(2, 2, 1500)
                else if (get().tryCastSpell(fight, fighter, target!!, 122) == 0)
                    setNextParams(2, 2, 1500)
                else {
                    get().moveFarIfPossible(fight, fighter)
                    time = 1500
                }
            }
        }
    }
}
