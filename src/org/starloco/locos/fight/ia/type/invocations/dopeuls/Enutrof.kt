package org.starloco.locos.fight.ia.type.invocations.dopeuls

import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.spells.Spell

/**
 * Created by Locos on 09/04/2018.
 */
class Enutrof(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    private val fighters = ArrayList<Fighter>()

    override fun run() {
        time = 200
        when (this.flag.toInt()) {
            0 -> { // Accélération
                val friend = fighter.getInvocator()
                if (get().moveToAttack(fight, fighter, friend!!, get().findSpell(fighter, 55)))
                    this.setNextParams(-1, 5, 1500)
                else if (get().tryCastSpell(fight, fighter, friend!!, 55) == 0)
                    this.time = 2000
            }
            1 -> if (get().tryCastSpell(fight, fighter, fighter, 52) == 0) // Cupidité
                this.time = 2000

            2 -> if (fight.tryCastSpell(fighter, get().findSpell(fighter, 54)!!, fighter.cell!!.cellId) == 0) // Maladresse de masse
                time = 2000

            3, 4 -> { // Lancer de pièce
                val spell = get().findSpell(fighter, 51)
                val nearest = get().getNearestEnnemy(fight, fighter, true)
                var target = get().getEnnemyWithDistance(fight, fighter, 0, 666, fighters)
                if (target == null) {
                    this.fighters.clear()
                    target = get().getEnnemyWithDistance(fight, fighter, 0, 666, fighters)
                }
                if (nearest != null && fight.canLaunchSpell(fighter, spell!!, nearest.cell!!) && get().tryCastSpell(fight, fighter, nearest, 51) == 0) {
                    this.setNextParams(2, 2, 1500)
                    this.fighters.add(nearest)
                } else {
                    if (get().moveToAttack(fight, fighter, target, get().findSpell(fighter, 51)))
                        this.setNextParams(2, 2, 1500)
                    else if (get().tryCastSpell(fight, fighter, target!!, 51) == 0) {
                        this.setNextParams(2, 2, 1500)
                        fighters.add(target!!)
                    } else {
                        get().moveFarIfPossible(fight, fighter)
                        time = 1500
                    }
                }
            }
        }
    }

    private fun hasEnnemiesArround(): Boolean {
        val spell = get().findSpell(fighter, 54)
        if (spell != null) {
            for (t in fight.getFighters(3)) {
                if (t.team != fighter.team) {
                    val dist = PathFinding.getDirBetweenTwoCase(t.cell!!.cellId, fighter.cell!!.cellId, fight.map, true)
                    if (dist.code <= 8) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
