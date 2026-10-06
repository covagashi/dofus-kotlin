package org.starloco.locos.fight.ia.type.boss

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.ia.util.newia.AttackFighterMind
import org.starloco.locos.fight.ia.util.newia.BuffFighterMind
import org.starloco.locos.fight.ia.util.newia.InvocationFighterMind
import org.starloco.locos.fight.ia.util.newia.action.AttackAction
import org.starloco.locos.fight.ia.util.newia.action.IAAction
import java.util.LinkedList

/**
 * Created by Locos on 04/10/2015.
 */
class IA10(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

    override fun run() {
        when (this.flag.toInt()) {
            1 -> {
                //region Buff
                if (fight.curFighterPa > 0 && fight.map != null) {
                    val mind = BuffFighterMind(this)

                    val actions = if (mind.highPriorityActions.isEmpty()) mind.lowPriorityActions else mind.highPriorityActions
                    val action = mind.executeActions(actions)
                    if (action != null) {
                        this.setNextParams(0, 5, action.getWaitingTime().toInt())
                        return
                    }
                }
                //endregion
            }
            2 -> {
                //region Invocations
                if (fight.curFighterPa > 0 && fight.map != null) {
                    val mind = InvocationFighterMind(this)

                    val actions = if (mind.highPriorityActions.isEmpty()) mind.lowPriorityActions else mind.highPriorityActions

                    val iterator = actions.iterator()
                    while (iterator.hasNext()) {
                        val aa = iterator.next() as AttackAction
                        when (aa.spell.spellID) {
                            1107 -> if (get().hasMobInFight(fight, 424)) iterator.remove() // Primaire
                            1108 -> if (get().hasMobInFight(fight, 1092)) iterator.remove() // Secondaire
                            1109 -> if (get().hasMobInFight(fight, 1091)) iterator.remove() // Tertiaire
                            1110 -> if (get().hasMobInFight(fight, 1090)) iterator.remove() // Quaternaire
                        }
                    }

                    val action = mind.executeActions(actions)
                    if (action != null) {
                        if (get().hasMobInFight(fight, 1092))
                            fighter.setState(38, 0)
                        else if (get().hasMobInFight(fight, 1091))
                            fighter.setState(37, 0)
                        else if (get().hasMobInFight(fight, 1090))
                            fighter.setState(36, 0)
                        else if (get().hasMobInFight(fight, 424))
                            fighter.setState(35, 0)

                        this.setNextParams(1, 4, action.getWaitingTime().toInt() + 300)
                        return
                    }
                }
                //endregion
            }
            3 -> {
                //region Attack
                if (fight.curFighterPa > 0 && fight.map != null) {
                    val mind = AttackFighterMind(this)
                    val action = mind.executeActions(mind.highPriorityActions)
                    if (action != null) {
                        this.setNextParams(2, 3, action.getWaitingTime().toInt())
                        return
                    }
                }
                //endregion
            }
            4 -> {
                //region Buff
                if (fight.curFighterPa > 0 && fight.map != null) {
                    val mind = BuffFighterMind(this)

                    val actions = if (mind.highPriorityActions.isEmpty()) mind.lowPriorityActions else mind.highPriorityActions
                    val action = mind.executeActions(actions)
                    if (action != null) {
                        this.setNextParams(3, 2, action.getWaitingTime().toInt())
                        return
                    }
                }
                //endregion
            }
        }
    }
}
