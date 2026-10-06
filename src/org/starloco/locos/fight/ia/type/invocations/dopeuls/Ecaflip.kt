package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA

/**
 * Created by Locos on 09/04/2018.
 */
class Ecaflip(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    override fun run() {
        when (this.flag.toInt()) {
            0 -> { // Réflexes
                val spell = get().findSpell(fighter, 118)
                val friend = fighter.getInvocator()
                if (fight.canLaunchSpell(fighter, spell!!, friend!!.cell!!)) {
                    if (get().moveToAttack(fight, fighter, friend!!, spell))
                        setNextParams(-1, 5, 1500)
                    else if (get().tryCastSpell(fight, fighter, friend!!, spell!!.spellID) == 0)
                        this.time = 1500
                }
            }
            1 -> { // Roue de la fortune
                val spell = get().findSpell(fighter, 118)
                val friend = fighter.getInvocator()
                if (fight.canLaunchSpell(fighter, spell!!, friend!!.cell!!)) {
                    if (get().moveToAttack(fight, fighter, friend!!, spell))
                        setNextParams(0, 4, 1500)
                    else if (get().tryCastSpell(fight, fighter, friend!!, spell!!.spellID) == 0)
                        this.time = 1500
                }
            }
            2 -> if (get().tryCastSpell(fight, fighter, fighter, 101) == 0) // Roulette
                this.time = 1500

            3 -> { // Bluff
                val target = get().getNearestEnnemy(fight, fighter, true)
                if (get().moveToAttack(fight, fighter, target, get().findSpell(fighter, 109)))
                    setNextParams(2, 2, 1500)
                else if (get().tryCastSpell(fight, fighter, target!!, 109) == 0)
                    setNextParams(2, 2, 1500)
                else {
                    get().moveFarIfPossible(fight, fighter)
                    time = 1500
                }
            }
        }
    }
}
