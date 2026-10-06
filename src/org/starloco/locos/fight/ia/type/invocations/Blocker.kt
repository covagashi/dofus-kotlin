package org.starloco.locos.fight.ia.type.invocations

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractIA
import org.starloco.locos.fight.ia.util.Function
import java.util.ArrayList

/**
 * Created by Locos on 04/10/2015.
 */
class Blocker(fight: Fight, fighter: Fighter, count: Byte) : AbstractIA(fight, fighter, count) {

    private var flag: Byte = 0
    //private boolean invocation = false;

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            var time = 0
            val enemy = Function.getInstance().getNearestEnnemy(this.fight, this.fighter, true)

            if (enemy != null) {
                when (this.flag.toInt()) {
                    0 -> if (Function.getInstance().moveNearIfPossible(fight, fighter, enemy))
                        time = 2000
                    else if (Function.getInstance().moveautourIfPossible(fight, fighter, enemy) > 0) {
                        time = 1500
                        this.count = 4
                        this.flag = -1
                    }

                    1, 2, 3 -> if (this.fighter.mob != null) {
                        val spell = Function.getInstance().getBestSpellForTargetDopeul(this.fight, this.fighter, enemy, this.fighter.cell!!.cellId, ArrayList(this.fighter.mob!!.spells.values))
                        if (spell != null && Function.getInstance().tryCastSpell(this.fight, this.fighter, enemy,
                                spell.getSpell()!!.id) == 0) {
                            this.count = 3
                            this.flag = 0
                            time = 2500
                        } else {
                            this.stop = true
                            time = 1000
                        }
                    }
                }
                this.flag++
            }

            addNext({ this.decrementCount() }, time)
        } else {
            this.stop = true
        }
    }
}
