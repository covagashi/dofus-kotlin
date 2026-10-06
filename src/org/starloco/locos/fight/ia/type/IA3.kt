package org.starloco.locos.fight.ia.type

import org.starloco.locos.common.Formulas
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractEasyIA
import org.starloco.locos.fight.ia.util.Function
import org.starloco.locos.fight.ia.util.newia.AttackFighterMind
import org.starloco.locos.fight.ia.util.newia.BuffFighterMind
import org.starloco.locos.fight.ia.util.newia.HealFighterMind
import org.starloco.locos.fight.ia.util.newia.InvocationFighterMind
import org.starloco.locos.fight.ia.util.newia.action.AttackAction
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.game.world.World

/**
 * Created by Locos on 17/04/2018.
 */
class IA3(fight: Fight, fighter: Fighter, count: Byte) : AbstractEasyIA(fight, fighter, count) {

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
                1 -> if (fight.curFighterPa > 0 && fight.map != null) {
                    val mind = InvocationFighterMind(this)

                    val actions = if (mind.highPriorityActions.isEmpty()) mind.lowPriorityActions else mind.highPriorityActions
                    val action = mind.executeActions(actions)
                    if (action != null) {
                        // On reset l'esprit de l'ia si plus d'actions
                        this.setNextParams(0, 7, action.getWaitingTime().toInt())
                        return
                    }
                }
                2 -> if (fight.curFighterPa > 0 && fight.map != null) {
                    val mind = BuffFighterMind(this)

                    val actions = if (mind.highPriorityActions.isEmpty()) mind.lowPriorityActions else mind.highPriorityActions
                    val action = mind.executeActions(actions)
                    if (action != null) {
                        // On reset l'esprit de l'ia si plus d'actions
                        if (action is AttackAction)
                            last = World.Couple(action.cell.firstFighter, action.spell)
                        this.setNextParams(1, 6, action.getWaitingTime().toInt())
                        return
                    }
                }

                3 -> if (fight.curFighterPa > 0 && fight.map != null) {
                    val mind = AttackFighterMind(this)
                    val action = mind.executeActions(mind.highPriorityActions)
                    if (action != null) {
                        // On reset l'esprit de l'ia si plus d'actions
                        if (action is AttackAction)
                            last = World.Couple(action.cell.firstFighter, action.spell)
                        this.setNextParams(2, 5, action.getWaitingTime().toInt())
                        return
                    }
                }

                4 -> if (fight.curFighterPa > 0 && fight.map != null) {
                    val mind = HealFighterMind(this)

                    val actions = if (mind.highPriorityActions.isEmpty()) mind.lowPriorityActions else mind.highPriorityActions
                    val action = mind.executeActions(actions)
                    if (action != null) {
                        // On reset l'esprit de l'ia si plus d'actions
                        if (action is AttackAction)
                            last = World.Couple(action.cell.firstFighter, action.spell)
                        this.setNextParams(3, 4, action.getWaitingTime().toInt())
                        return
                    }
                }
                5 -> if ((fight.curFighterPa > 0 || fight.curFighterPm > 0) && fight.map != null) {
                    val mind = AttackFighterMind(this)
                    val actions = if (mind.highPriorityActions.isEmpty()) mind.lowPriorityActions else mind.highPriorityActions
                    val action = mind.executeActions(actions)
                    if (action != null) {
                        // On reset l'esprit de l'ia si plus d'actions
                        if (action is AttackAction)
                            last = World.Couple(action.cell.firstFighter, action.spell)
                        this.setNextParams(4, 3, action.getWaitingTime().toInt())
                    }
                }
                6 -> {
                    val target = Function.getInstance().getNearestEnnemy(fight, fighter, true)
                    if (target != null) {
                        if (!tryEnemyBuff()) {
                            if (!tryTrap(target)) {
                                val pm = this.fighter.getCurPm(fight)
                                if (this.fighter.getCurPm(fight) > 0) {
                                    Function.getInstance().moveFarIfPossible(fight, fighter)
                                    this.time = 1250
                                    if (pm != this.fighter.getCurPm(fight) && this.fighter.getCurPm(fight) > 0) {
                                        this.setNextParams(5, 2, 1250)
                                    }
                                }
                            }
                        }
                    } else {
                        Function.getInstance().moveFarIfPossible(fight, fighter)
                        this.time = 1000
                    }
                }
            }
        }
    }

    private fun tryEnemyBuff(): Boolean {
        if (!this.enemyBuffs.isEmpty()) {
            var pm = fighter.getCurPm(fight)
            for (enemy in fight.getFighters(3)) {
                if (enemy.team == fighter.team) continue
                for (s in this.enemyBuffs) {
                    if (get().moveToAttack(fight, fighter, enemy, s)) {
                        pm -= fighter.getCurPm(fight)
                        this.setNextParams(2, 3, if (pm <= 3) pm * 200 else pm * 100)
                        return true
                    } else if (get().tryCastSpell(this, enemy, s)) {
                        this.setNextParams(0, 5, s.getSpell()!!.duration.toInt())
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun tryTrap(target: Fighter): Boolean {
        if (this.traps.isEmpty()) return false
        val cells = get().getCellsAvailableAround(target, false, 1.toByte())
        val cellsCaster = get().getCellsAvailableAround(fighter, true, 0.toByte())
        cells.removeIf { c -> cellsCaster.contains(c) }
        cells.remove(target.cell!!)

        if (!cells.isEmpty()) {
            val cell = cells[Formulas.getRandomValue(0, cells.size - 1)]
            for (spell in this.traps) {
                if (cell != null && fight.tryCastSpell(fighter, spell, cell.cellId) == 0) {
                    this.setNextParams(2, 2, spell.getSpell()!!.duration.toInt())
                    return true
                }
            }
        }
        return false
    }
}
