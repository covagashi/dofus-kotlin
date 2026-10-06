package org.starloco.locos.fight.ia.type

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractIA

/**
 * Created by Locos on 18/09/2015.
 */
class Blank(fight: Fight, fighter: Fighter) : AbstractIA(fight, fighter, 1) {

    override fun apply() {
        this.stop = true
        this.endTurn()
    }
}
