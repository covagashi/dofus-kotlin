package org.starloco.locos.fight.ia.type.boss

import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractIA
import org.starloco.locos.fight.ia.util.Function
import org.starloco.locos.fight.spells.Spell
import java.util.Random

class IA11(fight: Fight, fighter: Fighter, count: Byte) : AbstractIA(fight, fighter, count) {

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            this.tryLaunchSpellKraken()
            var nearestEnnemy = Function.getInstance().getEnnemyWithDistance(this.fight, this.fighter, 0, 10, null)

            if (nearestEnnemy == null)
                nearestEnnemy = Function.getInstance().getEnnemyWithDistance(this.fight, this.fighter, 0, 50, null)

            val arround = PathFinding.getEnemyAround(this.fighter.cell!!.cellId, this.fight.map!!, this.fight)
            if (arround == null)
                Function.getInstance().moveNearIfPossible(this.fight, this.fighter, nearestEnnemy!!)
            this.tryLaunchSpellKraken()
            this.tryLaunchOtherSpell()
            //Function.getInstance().attackIfPossible(this.fight, this.fighter, null);
            this.addNext({ this.decrementCount() }, 1000)
        } else {
            this.stop = true
        }
    }

    private fun tryLaunchOtherSpell() {
        val spells = intArrayOf(261, 1100, 1101, 1102)
        var spell: Spell.SortStats? = null
        for (id in spells) {
            spell = Function.getInstance().findSpell(this.fighter, id)
            if (spell != null) break
        }

        var cell = Function.getInstance().getBestTargetZone(this.fight, this.fighter, spell!!, this.fighter.cell!!.cellId, true)

        if (cell == 0 || cell == -1) {
            val fighters = PathFinding.getEnemyFighterArround(this.fighter.cell!!.cellId, this.fight.map, this.fight, true)
            if (fighters != null && !fighters.isEmpty())
                cell = fighters[Random().nextInt(fighters.size)].cell!!.cellId
        } else {
            val nbTarget = cell / 1000
            cell = cell - nbTarget * 1000
        }
        if (cell != 0)
            this.fight.tryCastSpell(this.fighter, spell, cell)
        //else
        //this.fight.tryCastSpell(this.fighter, spell, this.fighter.cell!!.id);
    }

    private fun tryLaunchSpellKraken(): Boolean {
        if (this.tryLaunchSpellKraken(1096, -1))
            return true
        if (this.tryLaunchSpellKraken(1097, 31))
            return true
        if (this.tryLaunchSpellKraken(1098, 32))
            return true
        if (this.tryLaunchSpellKraken(1099, 33))
            return true
        return false
    }

    private fun tryLaunchSpellKraken(id: Int, state: Int): Boolean {
        var ok = false
        var spell = Function.getInstance().findSpell(this.fighter, id)

        if (spell != null) {
            if (state != -1 && !this.fighter.haveState(state))
                return ok
            if (state != -1 && this.fighter.haveState(state + 1))
                return true

            var cell = 0
            if (id == 1096) {
                if (this.fighter.getCurPa(this.fight) == 4) {
                    spell = Function.getInstance().findSpell(this.fighter, 261)
                    if (spell != null)
                        this.fight.tryCastSpell(this.fighter, spell, this.fighter.cell!!.cellId)
                }
            }

            val fighters = PathFinding.getEnemyFighterArround(this.fighter.cell!!.cellId, this.fight.map, this.fight, true)
            if (fighters != null && !fighters.isEmpty())
                cell = fighters[Random().nextInt(fighters.size)].cell!!.cellId

            if (cell != 0)
                this.fight.tryCastSpell(this.fighter, spell!!, cell)
            this.fight.tryCastSpell(this.fighter, spell!!, this.fighter.cell!!.cellId)
            ok = true
        }
        return ok
    }
}
