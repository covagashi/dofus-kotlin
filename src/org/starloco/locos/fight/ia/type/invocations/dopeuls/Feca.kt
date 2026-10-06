package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA

/**
 * Created by Locos on 09/04/2018.
 */
class Feca(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    override fun run() {
        when (this.flag.toInt()) {
            0 -> { // Buff Renvoie de sort
                val friend = fighter.getInvocator()
                val spell = get().findSpell(fighter, 4)

                if (spell != null && friend != null) {
                    if (get().moveToAttack(fight, fighter, friend!!, spell))
                        setNextParams(-1, 5, 1500)
                    else if (get().tryCastSpell(fight, fighter, friend!!, spell.spellID) == 0)
                        this.time = 1500
                }
            }
            1 -> { // Buff science du bâton
                val friend = fighter.getInvocator()
                val spell = get().findSpell(fighter, 16)
                if (spell != null && friend != null) {
                    if (get().tryCastSpell(fight, fighter, friend!!, spell.spellID) == 0)
                        this.time = 1500
                }
            }
            2 -> { // Aveuglement
                val target = get().getNearestEnnemy(fight, fighter, true)
                val spell = get().findSpell(fighter, 2)
                if (spell != null && target != null) {
                    if (get().moveToAttack(fight, fighter, target, spell))
                        setNextParams(1, 3, 1500)
                    else if (get().tryCastSpell(fight, fighter, target, spell.spellID) == 0)
                        this.time = 1500
                }
            }
            3 -> { // Attaque naturelle
                val target = get().getNearestEnnemy(fight, fighter, true)
                val spell = get().findSpell(fighter, 3)
                if (spell != null && target != null) {
                    if (get().tryCastSpell(fight, fighter, target, spell.spellID) == 0)
                        setNextParams(2, 2, 1250)
                    else {
                        get().moveFarIfPossible(fight, fighter)
                        this.time = 1500
                    }
                }
            }
        }
    }
}
