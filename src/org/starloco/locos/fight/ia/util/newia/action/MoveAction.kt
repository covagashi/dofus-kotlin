package org.starloco.locos.fight.ia.util.newia.action

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.util.Function
import org.starloco.locos.fight.spells.Spell

/**
 * Created by Locos on 28/04/2018.
 */
class MoveAction(
    private val caster: Fighter,
    private val spell: Spell.SortStats,
    private val cell: GameCase
) : IAAction {

    private var pm: Byte = 0

    override fun getType(): Byte = IAAction.MOVE

    override fun getWaitingTime(): Short {
        pm = (pm - caster.getCurPm(caster.fight)).toByte()
        return (if (pm <= 3) pm * 250 else pm * 200).toShort()
    }

    override fun execute(): Boolean {
        pm = caster.getCurPm(caster.fight).toByte()
        return Function.getInstance().moveToAttack(caster.fight, caster, cell, spell, true)
    }
}
