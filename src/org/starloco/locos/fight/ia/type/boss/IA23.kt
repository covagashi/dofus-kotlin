package org.starloco.locos.fight.ia.type.boss

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractIA
import org.starloco.locos.fight.ia.util.Function

/**
 * Created by Locos on 04/10/2015.
 */
class IA23(fight: Fight, fighter: Fighter, count: Byte) : AbstractIA(fight, fighter, count) {

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            val ennemy = Function.getInstance().getNearestFriendNoInvok(this.fight, this.fighter)

            if (!Function.getInstance().moveNearIfPossible(this.fight, this.fighter, ennemy!!))
                Function.getInstance().HealIfPossible(this.fight, this.fighter, false)

            addNext({ this.decrementCount() }, 500)
        } else {
            this.stop = true
        }
    }
}
