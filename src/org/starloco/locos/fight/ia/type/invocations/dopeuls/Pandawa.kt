package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA

/**
 * Created by Locos on 09/04/2018.
 */
class Pandawa(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    override fun run() {
        when (this.flag.toInt()) {
            0 -> { // Buff pandanlku
                val friend = fighter.getInvocator()
                val spell = get().findSpell(fighter, 1677)

                if (spell != null && friend != null) {
                    if (get().moveToAttack(fight, fighter, friend!!, spell)) {
                        setNextParams(-1, 6, 1000)
                        return
                    }
                    if (get().tryCastSpell(fight, fighter, friend!!, spell.spellID) == 0) {
                        this.time = 1500
                    }
                }
            }
            1 -> {
                val target = get().getNearestEnnemy(fight, fighter, true)
                val spell = get().findSpell(fighter, 1678)
                if (get().moveToAttack(fight, fighter, target, spell)) {
                    this.time = 1000
                }
            }
            2 -> if (get().tryCastSpell(fight, fighter, fighter, 1676) == 0) { // Picole
                this.time = 2000
            }
            3 -> { // Souillure
                val target = get().getNearestEnnemy(fight, fighter, true)
                if (get().tryCastSpell(fight, fighter, target!!, 1678) == 0) {
                    this.time = 2000
                }
            }
            4 -> { // Poing enflammé
                val target = get().getNearestEnnemy(fight, fighter, true)
                if (get().tryCastSpell(fight, fighter, target!!, 687) == 0) {
                    this.setNextParams(3, 2, 1500)
                } else {
                    get().moveFarIfPossible(fight, fighter)
                }
            }
        }
    }
}
