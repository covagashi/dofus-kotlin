package org.starloco.locos.fight.ia.type.boss

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractNeedSpell
import org.starloco.locos.fight.ia.util.Function

/**
 * Created by Locos on 04/10/2015.
 */
class IA17(fight: Fight, fighter: Fighter, count: Byte) : AbstractNeedSpell(fight, fighter, count) {

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            val ennemy = Function.getInstance().getNearestEnnemy(this.fight, this.fighter, false)
            var time = 100
            var maxPo = 1
            var action = false

            for (spellStats in this.highests)
                if (spellStats != null && spellStats.maxPO > maxPo)
                    maxPo = spellStats.maxPO

            var target = Function.getInstance().getEnnemyWithDistance(this.fight, this.fighter, 0, 2, null)

            if (target != null)
                if (target.isHidden())
                    target = null

            if (this.fighter.getCurPa(this.fight) > 0) {
                if (Function.getInstance().invocIfPossibleloin(this.fight, this.fighter, this.invocations)) {
                    time = 4000
                    action = true
                }
            }


            if (!action && this.fighter.getCurPa(this.fight) > 0) {
                if (Function.getInstance().invocIfPossibleloin(this.fight, this.fighter, this.invocations)) {
                    time = 3000
                    action = true
                }
            }

            if (!action && this.fighter.getCurPm(this.fight) > 0 && target == null) {
                val num = Function.getInstance().moveautourIfPossible(this.fight, this.fighter, ennemy!!)
                if (num != 0) {
                    time = num
                    action = true
                    target = Function.getInstance().getEnnemyWithDistance(this.fight, this.fighter, 0, 2, null)
                }
            }

            if (!action && this.fighter.getCurPa(this.fight) > 0 && target == null) {
                val num = Function.getInstance().attackBondIfPossible(this.fight, this.fighter, ennemy!!)
                if (num != 0) {
                    time = num
                    action = true
                    target = Function.getInstance().getEnnemyWithDistance(this.fight, this.fighter, 0, 2, null)//2 = po min 1 + 1;
                }
            }

            if (!action && this.fighter.getCurPa(this.fight) > 0 && target == null) {
                val num = Function.getInstance().attackIfPossible(this.fight, this.fighter, this.highests)
                if (num != 0) {
                    time = num
                    action = true
                }
            } else if (this.fighter.getCurPa(this.fight) > 0 && target != null && !action) {
                val num = Function.getInstance().attackIfPossible(this.fight, this.fighter, this.cacs)
                if (num != 0) {
                    time = num
                    action = true
                }
            }

            if (this.fighter.getCurPm(this.fight) > 0 && !action) {
                val num = Function.getInstance().moveautourIfPossible(this.fight, this.fighter, ennemy!!)
                if (num != 0) time = num
            }

            if (this.fighter.getCurPa(this.fight) == 0 && this.fighter.getCurPm(this.fight) == 0) this.stop = true
            addNext({ this.decrementCount() }, time)
        } else {
            this.stop = true
        }
    }
}
