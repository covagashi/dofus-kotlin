package org.starloco.locos.fight.ia.type.invocations

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractNeedSpell
import org.starloco.locos.fight.ia.util.Function

/**
 * Created by Locos on 04/10/2015.
 */
class Lapino(fight: Fight, fighter: Fighter, count: Byte) : AbstractNeedSpell(fight, fighter, count) {

    private var flag: Byte = 0

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            var time = 0
            val friend = Function.getInstance().getNearestFriendNoInvok(this.fight, this.fighter)

            if (friend == null) {
                time = Function.getInstance().moveFarIfPossible(this.fight, this.fighter)
            } else {
                when (this.flag.toInt()) {
                    0 -> {
                        val spell = Function.getInstance().getBestBuffSpell(fight, fighter, friend)
                        if (fight.canLaunchSpell(fighter, spell!!, friend.cell!!)) {
                            if (Function.getInstance().moveToAttack(this.fight, this.fighter, friend, spell)) {
                                this.count = 4
                                this.flag = -1
                                time = 1500
                            } else if (Function.getInstance().tryCastSpell(fight, fighter, friend, spell!!.getSpell()!!.id) == 0) {
                                time = 1500
                            }
                        }
                    }
                    1, 2 -> {
                        val spell = Function.getInstance().getBestHealSpell(this.fight, this.fighter, friend)
                        if (spell != null) {
                            if (spell.maxPO == 0) {
                                if (Function.getInstance().tryCastSpell(fight, fighter, fighter, spell.spellID) == 0) {
                                    this.count = 4
                                    this.flag = 0
                                    time = 1000
                                    this.flag++
                                    addNext({ this.decrementCount() }, time)
                                    return
                                }
                            }
                            if (Function.getInstance().moveToAttack(this.fight, this.fighter, friend, spell)) {
                                time = 1500
                                this.count = 3
                                this.flag = 0
                            } else if (Function.getInstance().HealIfPossible(this.fight, this.fighter, false, 95) == 0) {
                                this.count = 3
                                this.flag = 0
                                time = 1500
                            } else if (Function.getInstance().HealIfPossible(this.fight, this.fighter, true, 95) == 0) {
                                this.stop = true
                                time = 2000
                            }
                        }
                    }
                    3 -> time = Function.getInstance().moveFarIfPossible(this.fight, this.fighter)
                }
                this.flag++
            }

            addNext({ this.decrementCount() }, time)
        } else {
            this.stop = true
        }
    }
}
