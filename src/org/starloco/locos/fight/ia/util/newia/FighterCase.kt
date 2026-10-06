package org.starloco.locos.fight.ia.util.newia

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.common.Formulas
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.util.AStarPathFinding
import org.starloco.locos.fight.ia.util.Function
import org.starloco.locos.fight.spells.Spell
import java.util.LinkedList

/**
 * Created by Locos on 28/04/2018.
 */
class FighterCase(
    val fighter: Fighter,
    aStar: AStarPathFinding?,
    val sortedSpells: LinkedList<Spell.SortStats>
) {

    private var discovered = false
    var shortestPath: List<GameCase>? = null

    init {
        if (aStar != null) this.shortestPath = aStar.getShortestPath()
    }

    fun getFighterCell(): GameCase? {
        if (fighter.isHidden() && !discovered) {
            val pm = fighter.getPm().toByte()
            val cells = Function.getInstance().getCellsAvailableAround(fighter, pm.toInt() == 1, pm)
            if (!cells.isEmpty()) {
                val index = Formulas.random.nextInt(cells.size - 1)
                if (index >= 0) {
                    val cell = cells[index]
                    if (cell != null) {
                        if (cell.cellId == fighter.cell!!.cellId)
                            discovered = true
                        return cell
                    }
                }
            }
        }
        return fighter.cell
    }
}
