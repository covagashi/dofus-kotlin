package org.starloco.locos.fight.ia.util.newia.action

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.spells.Spell

/**
 * Created by Locos on 28/04/2018.
 */
class AttackAction(
    private val caster: Fighter,
    val cell: GameCase,
    val spell: Spell.SortStats
) : IAAction {

    override fun getType(): Byte = IAAction.ATTACK

    override fun getWaitingTime(): Short = spell.getSpell()!!.duration

    override fun execute(): Boolean = caster.fight.tryCastSpell(caster, spell, cell.cellId) == 0
}
