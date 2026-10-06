package org.starloco.locos.fight.ia

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.util.Function
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.fight.spells.SpellEffect

/**
 * Created by Locos on 09/04/2018.
 */
abstract class AbstractEasyIA(fight: Fight, fighter: Fighter, count: Byte) : AbstractIA(fight, fighter, count) {

    @JvmField
    protected var flag: Byte = 0
    @JvmField
    protected var time: Short = 0
    @JvmField
    protected var attacks: List<Spell.SortStats> = getListSpellOf(fighter, "ATTACK")
    @JvmField
    protected var friendBuffs: List<Spell.SortStats> = getListSpellOf(fighter, "FRIEND-BUFF")
    @JvmField
    protected var enemyBuffs: List<Spell.SortStats> = getListSpellOf(fighter, "ENEMY-BUFF")
    @JvmField
    protected var heals: List<Spell.SortStats> = getListSpellOf(fighter, "HEAL")
    @JvmField
    protected var teleportations: List<Spell.SortStats> = getListSpellOf(fighter, "TP")
    @JvmField
    protected var traps: List<Spell.SortStats> = getListSpellOf(fighter, "TRAP")
    @JvmField
    protected var invocations: List<Spell.SortStats> = getListSpellOf(fighter, "INVOCATION")

    protected fun setNextParams(flag: Int, count: Int, time: Int) {
        this.flag = flag.toByte()
        this.count = count.toByte()
        this.time = time.toShort()
    }

    protected fun get(): Function {
        return Function.getInstance()
    }

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            if (fight.curAction.isNotEmpty()) {
                this.addNext({ this.apply() }, 100)
                return
            }
            if (this.time.toInt() == 0 && this.fighter.getCurPa(this.fight) == 0 && this.fighter.getCurPm(this.fight) == 0) {
                this.stop = true
                time = 1000
            }

            this.run()
            this.flag++
            if (!this.fighter.isDead)
                this.addNext({ this.decrementCount() }, time.toInt())
            else if (this.fight.getFighterByGameOrder() != null && this.fighter.id == this.fight.getFighterByGameOrder()!!.id)
                this.endTurn()
        } else {
            this.stop = true
            this.time = 500
        }
    }

    abstract fun run()

    private fun getListSpellOf(fighter: Fighter, type: String): List<Spell.SortStats> {
        val spells = ArrayList<Spell.SortStats>()

        for (spell in fighter.mob!!.spells.values) {
            if (spells.contains(spell)) continue
            when (type) {
                "ATTACK" -> if (spell.getSpell()!!.type == 0)
                    spells.add(spell)

                "FRIEND-BUFF" -> if (spell.getSpell()!!.type == 1) spells.add(spell)
                "ENEMY-BUFF" -> if (spell.getSpell()!!.type == 2) spells.add(spell)
                "HEAL" -> if (spell.getSpell()!!.type == 3) spells.add(spell)
                "TP" -> if (spell.getSpell()!!.type == 4)
                    spells.add(spell)

                "TRAP" -> if (spell.getSpell()!!.type == 5) spells.add(spell)
                "INVOCATION" -> for (effect in spell.effects)
                    if (effect.effectID == 181) {
                        spells.add(spell)
                        break
                    }
            }
        }
        return spells
    }

    fun getAttacksSpells(): List<Spell.SortStats> = attacks

    fun getFriendBuffsSpells(): List<Spell.SortStats> = friendBuffs

    fun getHealsSpells(): List<Spell.SortStats> = heals

    fun getTeleportations(): List<Spell.SortStats> = teleportations

    fun getInvocations(): List<Spell.SortStats> = invocations
}
