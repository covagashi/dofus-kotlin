package org.starloco.locos.fight.spells

import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.spells.Spell.SortStats

class LaunchedSpell(private var target: Fighter?, private val spellStats: SortStats, caster: Fighter) {

    private var cooldown = 0

    init {
        if (caster.getType() == 1 && spellStats.spellID in caster.player!!.objectsClassSpell) {
            val modi = caster.player!!.getValueOfClassObject(spellStats.spellID, 286)
            this.cooldown = spellStats.coolDown - modi
        } else {
            this.cooldown = spellStats.coolDown
        }
    }

    fun getTarget(): Fighter? = this.target

    fun getSpellId(): Int = spellStats.spellID

    fun getCooldown(): Int = this.cooldown

    fun decrementCooldown(): Int {
        this.cooldown--
        return cooldown
    }

    companion object {
        @JvmStatic
        fun cooldownGood(fighter: Fighter, id: Int): Boolean {
            for (S in fighter.getLaunchedSorts()) {
                if (S.getSpellId() == id && S.getCooldown() > 0)
                    return false
            }
            return true
        }

        @JvmStatic
        fun getNbLaunch(fighter: Fighter, id: Int): Int {
            var nb = 0
            for (S in fighter.getLaunchedSorts())
                if (S.getSpellId() == id)
                    nb++
            return nb
        }

        @JvmStatic
        fun getNbLaunchTarget(fighter: Fighter, target: Fighter?, id: Int): Int {
            if (target == null)
                return 0

            var nb = 0
            for (S in fighter.getLaunchedSorts())
                if (S.target != null)
                    if (S.getSpellId() == id && S.target!!.id == target.id)
                        nb++
            return nb
        }

        @JvmStatic
        fun haveEffectTarget(f: Map<Int, Fighter>, target: Fighter?, id: Int): Int {
            if (target == null) return 0
            var nb = 0
            for (m in f.values)
                if (m != null)
                    for (S in m.getLaunchedSorts())
                        if (S.target != null && S.target!!.id == target.id)
                            for (e in S.spellStats.effects)
                                if (e.effectID == id)
                                    nb++
            return nb
        }
    }
}
