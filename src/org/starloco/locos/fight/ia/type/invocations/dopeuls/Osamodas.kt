package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA

/**
 * Created by Locos on 09/04/2018.
 */
class Osamodas(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    private var spell: Byte = 0

    override fun run() {
        when (this.flag.toInt()) {
            0 -> { // Cri de l'ours
                val friend = fighter.getInvocator()
                val spell = get().findSpell(fighter, 23)
                var cellId = get().getBestTargetZone(fight, fighter, spell!!, fighter.cell!!.cellId, false)
                val nbTarget = cellId / 1000
                cellId = cellId - nbTarget * 1000
                val cell = if (cellId == 0 || cellId == -1) fighter.getInvocator()!!.cell!! else fight.map!!.getCase(cellId)

                if (get().moveToAttack(fight, fighter, friend, get().findSpell(fighter, spell!!.spellID)))
                    this.setNextParams(-1, 5, 1500)
                else if (get().tryCastSpell(fight, fighter, friend!!, 23) == 0)
                    this.setNextParams(0, 4, 1500)
            }
            1 -> { // crocs du mulou
                val friend = fighter.getInvocator()
                val spell = get().findSpell(fighter, 22)
                if (get().moveToAttack(fight, fighter, friend!!, spell))
                    this.setNextParams(0, 4, 1500)
                else if (get().tryCastSpell(fight, fighter, friend!!, spell!!.spellID) == 0)
                    this.setNextParams(1, 3, 1500)
            }
            2 -> { // déplacement félin
                val friend = fighter.getInvocator()
                val spell = get().findSpell(fighter, 29)

                if (get().moveToAttack(fight, fighter, friend!!, spell))
                    this.setNextParams(1, 3, 1500)
                else if (get().tryCastSpell(fight, fighter, friend!!, spell!!.spellID) == 0)
                    this.setNextParams(2, 2, 1500)
            }

            3 -> { // Corbeau
                val target = get().getNearestEnnemy(fight, fighter, true)
                if (get().moveToAttack(fight, fighter, target, get().findSpell(fighter, 24)))
                    this.setNextParams(2, 2, 1500)
                else if (get().tryCastSpell(fight, fighter, target!!, 24) == 0)
                    this.setNextParams(2, 2, 1500)
                else {
                    get().moveFarIfPossible(fight, fighter)
                    time = 1500
                }
            }
        }
    }
}
