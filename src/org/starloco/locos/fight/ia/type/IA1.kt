package org.starloco.locos.fight.ia.type

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.ia.util.newia.AttackFighterMind
import org.starloco.locos.fight.ia.util.newia.BuffFighterMind
import org.starloco.locos.fight.ia.util.newia.FighterMind
import org.starloco.locos.fight.ia.util.newia.RandomAttackFighterMind
import org.starloco.locos.fight.ia.util.newia.action.AttackAction
import org.starloco.locos.fight.ia.util.newia.action.IAAction
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.game.world.World
import java.util.LinkedList

/**
 * Created by Locos on 01/06/2018.
 */
class IA1(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    private var friend = false
    private var last: World.Couple<Fighter?, Spell.SortStats>? = null

    override fun run() {
        val l = last
        if (l != null && l.second != null && l.first != null && l.first!!.cell != null) {
            this.setNextParams(this.flag - 1, this.count + 1, l.second.getSpell()!!.duration.toInt())
            if (fight.tryCastSpell(fighter, l.second, l.first!!.cell!!.cellId) != 0)
                last = null
        } else {
            last = null
            when (this.flag.toInt()) {
                1 -> {
                    //region Buff
                    if (fight.curFighterPa > 0 && fight.map != null) {
                        val mind = BuffFighterMind(this)

                        val actions = if (mind.highPriorityActions.isEmpty()) mind.lowPriorityActions else mind.highPriorityActions
                        val action = mind.executeActions(actions)
                        if (action != null) {
                            // On reset l'esprit de l'ia si plus d'actions
                            if (action is AttackAction)
                                last = World.Couple(action.cell.firstFighter, action.spell)
                            this.setNextParams(0, 4, action.getWaitingTime().toInt())
                            return
                        }
                    }
                    //endregion
                }
                2 -> {
                    //region Attack
                    if (fight.curFighterPa > 0 && fight.map != null) {
                        val mind: FighterMind = RandomAttackFighterMind(this, friend)
                        val action = mind.executeActions(mind.highPriorityActions)
                        if (action != null) {
                            // On reset l'esprit de l'ia si plus d'actions

                            if (action is AttackAction) {
                                val target = action.cell.firstFighter
                                if (target == null || target.team == this.fighter.team)
                                    friend = true
                                last = World.Couple(target, action.spell)
                            }
                            this.setNextParams(1, 3, action.getWaitingTime().toInt())
                            return
                        }
                    }
                    //endregion
                }
                3 -> {
                    var pm = fighter.getCurPm(fight)
                    if (get().moveToAttack(fight, fighter, get().getNearestEnnemy(fight, fighter, true), null)) {
                        pm -= fighter.getCurPm(fight)
                        time = ((if (pm <= 3) pm * 200 else pm * 100) * 2).toShort()
                    }
                }
            }
        }
    }
}
