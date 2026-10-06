package org.starloco.locos.fight.ia.type.invocations

import org.starloco.locos.common.Formulas
import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractNeedSpell
import org.starloco.locos.fight.ia.util.Function
import org.starloco.locos.fight.spells.Spell.SortStats
import java.util.ArrayList

/**
 * Created by Locos on 04/10/2015.
 */
class Chafer(fight: Fight, fighter: Fighter, count: Byte) : AbstractNeedSpell(fight, fighter, count) {

    private var flag = 0
    private var target: Fighter? = null

    init {
        if (fighter.getMob()!! != null && fighter.getMob()!!.template!!.id == 1108)
            this.flag = -1 // Chaferfu lancier to buff himself
    }

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            var time = 0
            val friend = Function.getInstance().getNearestFriend(this.fight, this.fighter)
            val enemy = Function.getInstance().getNearestEnnemy(this.fight, this.fighter, true)

            if (this.target == null) {
                if (friend != null && Formulas.getRandomValue(1, 3) == 1) {
                    target = friend
                } else target = enemy
            }

            if (this.target == null) {
                time = Function.getInstance().moveFarIfPossible(this.fight, this.fighter)
            } else {
                when (this.flag) {
                    -1 -> {
                        val spell = Function.getInstance().getBuffSpell(fight, fighter, fighter)

                        if (spell != null && fight.canLaunchSpell(fighter, spell, fighter.cell!!)) {
                            if (fight.tryCastSpell(this.fighter, spell, fighter.cell!!.cellId) == 0) {
                                time = 1500
                            }
                        }
                    }
                    0 -> {
                        val cell = this.fighter.cell!!.cellId
                        if (PathFinding.getEnemyFighterArround(this.fighter.cell!!.cellId, this.fight.map, this.fight, true) != null) {
                            Function.getInstance().moveautourIfPossible(this.fight, this.fighter, target!!)
                            time = 1500
                            this.flag++
                            if (this.fighter.getCurPa(this.fight) == 0 && this.fighter.getCurPm(this.fight) == 0) {
                                this.stop = true
                                time = 1000
                            }
                            addNext({ this.decrementCount() }, time)
                            return
                        }
                        if (Function.getInstance().moveNearIfPossible(this.fight, this.fighter, this.target!!)) {
                            time = 1500
                            this.flag++
                            if (this.fighter.getCurPa(this.fight) == 0 && this.fighter.getCurPm(this.fight) == 0) {
                                this.stop = true
                                time = 1000
                            }
                            addNext({ this.decrementCount() }, time)
                            return
                        }
                        if (cell == this.fighter.cell!!.cellId) {
                            time = 0
                            if (friend != null && PathFinding.getDistanceBetweenTwoCase(this.fight.map, this.fighter.cell, friend!!.cell!!) > 1) {
                                this.target = enemy
                                this.count = 4
                                this.flag = -1
                            }
                        }
                    }
                    1, 2, 3 -> {
                        val spell = Function.getInstance().getBestSpellForTargetDopeul(this.fight, this.fighter, target!!, this.fighter.cell!!.cellId, ArrayList(this.fighter.getMob()!!.spells.values))
                        if (spell != null && Function.getInstance().tryCastSpell(this.fight, this.fighter, target!!, spell.getSpell()!!.id) == 0) {
                            if (spell.getMaxLaunchByTarget() == 1) {
                                val fighters = PathFinding.getEnemyFighterArround(this.fighter.cell!!.cellId, this.fight.map, this.fight, false)
                                if (fighters != null) {
                                    if (fighters.contains(target))
                                        fighters.remove(target)
                                    val i = fighters.size - 1
                                    if (i >= 0) {
                                        target = fighters[Formulas.random.nextInt(fighters.size)]
                                    } else target = if (target === friend) enemy else friend
                                }

                                this.count = 4
                                this.flag = -1
                            } else {
                                this.count = 4
                                this.flag = -1
                                this.target = null
                                time = 1000
                            }
                        } else {
                            if (Function.getInstance().moveNearIfPossible(this.fight, this.fighter, this.target!!)) {
                                time = 2000
                            } else {
                                this.stop = true
                                time = 1000
                            }
                        }
                    }
                }
                this.flag++
            }


            if (this.fighter.getCurPa(this.fight) == 0 && this.fighter.getCurPm(this.fight) == 0) {
                this.stop = true
                time = 1000
            }

            addNext({ this.decrementCount() }, time)
        } else {
            this.stop = true
        }
    }
}
