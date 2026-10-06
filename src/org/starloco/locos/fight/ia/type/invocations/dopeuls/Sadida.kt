package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA

/**
 * Created by Locos on 09/04/2018.
 */
class Sadida(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    override fun run() {
        when (this.flag.toInt()) {
            0 -> { // Connaissance des poupées
                val spell = get().findSpell(fighter, 199)
                if (spell != null) {
                    if (get().tryCastSpell(fight, fighter, fighter, spell.spellID) == 0)
                        this.time = 1500
                }
            }
            1 -> { // Déplacement invoation bloqueuse + folle  193 & 182
                val spell = get().findSpell(fighter, 193)
                val spell1 = get().findSpell(fighter, 182)

                if (fight.canLaunchSpell(fighter, spell!!, null) || fight.canLaunchSpell(fighter, spell1!!, null)) {
                    val target = get().getNearestEnnemy(fight, fighter, true)
                    this.time = (if (get().moveNearIfPossible(fight, fighter, target!!)) 1500 else 0).toShort()
                }
            }
            2 -> if (get().invocIfPossible(fight, fighter)) // Invocation bloqueuse + folle  193 & 182
                setNextParams(1, 3, 1500)

            3 -> { // Ronce
                val target = get().getNearestEnnemy(fight, fighter, true)
                val spell = get().findSpell(fighter, 183)
                if (spell != null && target != null) {
                    if (get().moveToAttack(fight, fighter, target, spell))
                        setNextParams(1, 2, 1500)
                    else if (get().tryCastSpell(fight, fighter, target, spell.spellID) == 0)
                        setNextParams(1, 2, 1500)
                    else {
                        get().moveFarIfPossible(fight, fighter)
                        time = 1500
                    }
                }
            }
        }
    }
}
