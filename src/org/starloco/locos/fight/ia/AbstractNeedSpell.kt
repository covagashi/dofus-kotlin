package org.starloco.locos.fight.ia

import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.spells.Spell.SortStats
import org.starloco.locos.fight.spells.SpellEffect
import java.util.stream.Collectors

/**
 * Created by Locos on 04/10/2015.
 */
abstract class AbstractNeedSpell(fight: Fight, fighter: Fighter, count: Byte) : AbstractIA(fight, fighter, count) {

    @JvmField
    protected var buffs: List<SortStats> = getListSpellOf(fighter, "BUFF")
    @JvmField
    protected var glyphs: List<SortStats> = getListSpellOf(fighter, "GLYPH")
    @JvmField
    protected var invocations: List<SortStats> = getListSpellOf(fighter, "INVOCATION")
    @JvmField
    protected var cacs: List<SortStats> = getListSpellOf(fighter, "CAC")
    @JvmField
    protected var highests: List<SortStats> = getListSpellOf(fighter, "HIGHEST")

    companion object {
        private fun getListSpellOf(fighter: Fighter, type: String): List<SortStats> {
            val spells = ArrayList<SortStats>()

            for (spell in fighter.mob!!.spells.values) {
                if (spells.contains(spell)) continue
                when (type) {
                    "BUFF" -> if (spell.getSpell()!!.type == 1) spells.add(spell)
                    "GLYPH" -> if (spell.getSpell()!!.type == 4) spells.add(spell)
                    "INVOCATION" -> spells.addAll(spell.effects.stream().filter { spellEffect -> spellEffect.effectID == 181 }.map { spell }.collect(Collectors.toList()))
                    "CAC" -> if (spell.getSpell()!!.type == 0) {
                        var effect = false
                        for (spellEffect in spell.effects)
                            if (spellEffect.effectID == 4 || spellEffect.effectID == 6)
                                effect = true
                        if (!effect && spell.maxPO < 3) spells.add(spell)
                    }
                    "HIGHEST" -> if (spell.getSpell()!!.type == 0) {
                        var effect = false
                        for (spellEffect in spell.effects)
                            if (spellEffect.effectID == 4 || spellEffect.effectID == 6)
                                effect = true
                        if (effect && spell.spellID != 805) continue
                        if (spell.maxPO > 1) spells.add(spell)
                    }
                }
            }
            return spells
        }
    }
}
