package org.starloco.locos.fight.ia.type

import org.starloco.locos.common.Formulas
import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractIA
import org.starloco.locos.fight.ia.util.Function
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.game.world.World
import java.util.ArrayList

/**
 * Created by Locos on 18/09/2015.
 */
class IAPerco(fight: Fight, fighter: Fighter, b: Byte) : AbstractIA(fight, fighter, b) {

    private var flag: Byte = 0
    private val spells: MutableCollection<Spell.SortStats?> = World.world.getGuild(this.fighter.getCollector()!!.guildId)!!.spells.values

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && count > 0) {
            var time = 0
            val enemy = Function.getInstance().getNearestEnnemy(this.fight, this.fighter, true)

            if (enemy == null) {
                time = Function.getInstance().moveFarIfPossible(this.fight, this.fighter)
            } else {
                when (this.flag.toInt()) {
                    0 -> {
                        val buffs = intArrayOf(461, 451, 452, 453, 454)
                        val spell = getBestSpell(buffs, fighter)
                        if (spell != null && fight.tryCastSpell(fighter, spell, fighter.cell!!.cellId) == 0) {
                            time = 1500
                            if (getBestSpell(buffs, fighter) != null) {
                                this.flag = -1
                                this.count = 6
                            }
                        }
                    }
                    1 -> {
                        val target = this.getFightersForDebuffing()
                        if (target != null) {
                            val spell = getBestSpell(intArrayOf(460), fighter)
                            if (spell != null) {
                                if (Function.getInstance().moveToAttack(this.fight, this.fighter, target, spell)) {
                                    time = 2000
                                    this.flag = 0
                                    this.count = 5
                                } else {
                                    if (fight.tryCastSpell(fighter, spell, target.cell!!.cellId) == 0)
                                        time = 1500
                                }
                            }
                        }
                    }
                    2 -> {
                        val attacks = intArrayOf(458, 456, 457, 458, 462)
                        val target = Function.getInstance().getNearestEnnemy(fight, fighter, true)
                        val spell = getBestSpell(attacks, target)

                        if (spell != null) {
                            if (Function.getInstance().moveToAttack(fight, fighter, target, spell)) {
                                time = 2000
                                this.flag = 1
                                this.count = 4
                                this.flag++
                                addNext({ this.decrementCount() }, time)
                                return
                            }
                            if (spell.getSpell()!!.id == 458 || spell.isLineLaunch) { // Rocher
                                var cell = Function.getInstance().getBestTargetZone(fight, fighter, spell, enemy.cell!!.cellId, spell.isLineLaunch)
                                val nbTarget = cell / 1000
                                cell = cell - nbTarget * 1000
                                if (nbTarget > 1) {
                                    if (this.fight.tryCastSpell(this.fighter, spell, cell) == 0) {
                                        time = 3000
                                    }
                                }
                            } else {
                                if (this.fight.tryCastSpell(this.fighter, spell, target!!.cell!!.cellId) == 0) {
                                    time = 3000
                                }
                            }
                        }
                    }
                    3 -> {
                        val spell = getBestSpell(intArrayOf(459), fighter)
                        if (spell != null && fight.tryCastSpell(fighter, spell, fighter.cell!!.cellId) == 0)
                            time = 1500
                    }
                    4 -> if (Function.getInstance().moveFarIfPossible(fight, fighter) > 0) {
                        time = 2500
                    }
                }
                this.flag++
            }

            addNext({ this.decrementCount() }, time)
        } else {
            this.stop = true
        }
    }

    private fun getBestSpell(wantedSpells: IntArray, target: Fighter?): Spell.SortStats? {
        if (target != null) {
            for (spell in spells) {
                for (wanted in wantedSpells) {
                    if (spell != null && wanted == spell.getSpell()!!.id &&
                        fight.canLaunchSpell(fighter, spell, target.cell!!))
                        return spell
                }
            }
        }
        return null
    }

    private fun getFightersForDebuffing(): Fighter? {
        val fightersForDebuffing = ArrayList<Fighter>()
        for (temp in this.fight.getFighters(7))
            if (!temp.isDead && (temp.getFightBuff() != null && temp.getFightBuff().size > 1) && temp.team != this.fighter.team)
                if (PathFinding.getDistanceBetween(fight.map, temp.cell!!.cellId, fighter.cell!!.cellId) <= 12)
                    fightersForDebuffing.add(temp)
        if (fightersForDebuffing.isEmpty())
            return null
        return fightersForDebuffing[Formulas.random.nextInt(fightersForDebuffing.size)]
    }
}
