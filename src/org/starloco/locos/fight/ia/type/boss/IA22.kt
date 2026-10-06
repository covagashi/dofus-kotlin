package org.starloco.locos.fight.ia.type.boss

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractIA
import org.starloco.locos.fight.ia.util.Function

/**
 * Created by Locos on 04/10/2015.
 */
class IA22(fight: Fight, fighter: Fighter, count: Byte) : AbstractIA(fight, fighter, count) {

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            val ennemy = Function.getInstance().getNearestEnnemy(this.fight, this.fighter, false)

            if (Function.getInstance().IfPossibleRasboulvulner(this.fight, this.fighter, this.fighter) == 0) {
                var attack = Function.getInstance().tpIfPossibleRasboul(this.fight, this.fighter, ennemy!!)
                if (attack == 0)
                    Function.getInstance().moveFarIfPossible(this.fight, this.fighter)
                else {
                    Function.getInstance().moveNearIfPossible(this.fight, this.fighter, ennemy!!)
                    attack = Function.getInstance().tpIfPossibleRasboul(this.fight, this.fighter, ennemy!!)
                    if (attack == 0)
                        Function.getInstance().moveFarIfPossible(this.fight, this.fighter)
                }
                Function.getInstance().invocIfPossible(this.fight, this.fighter)
            } else {
                Function.getInstance().buffIfPossible(this.fight, this.fighter, this.fighter)
                Function.getInstance().moveNearIfPossible(this.fight, this.fighter, ennemy!!)
            }

            addNext({ this.decrementCount() }, 1000)
        } else {
            this.stop = true
        }
    }
}
