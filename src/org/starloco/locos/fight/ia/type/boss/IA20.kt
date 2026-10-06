package org.starloco.locos.fight.ia.type.boss

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.fight.ia.AbstractIA
import org.starloco.locos.fight.ia.util.Function
import java.util.stream.Collectors

/**
 * Created by Locos on 04/10/2015.
 */
class IA20(fight: Fight, fighter: Fighter, count: Byte) : AbstractIA(fight, fighter, count) {

    private var coop = false

    override fun apply() {
        if (!this.stop && this.fighter.canPlay() && this.count > 0) {
            var nearestEnnemy = Function.getInstance().getEnnemyWithDistance(this.fight, this.fighter, 0, 10, null)

            if (nearestEnnemy == null)
                nearestEnnemy = Function.getInstance().getEnnemyWithDistance(this.fight, this.fighter, 0, 60, null)

            //int dist = PathFinding.getDistanceBetweenTwoCase(this.fight.getMaps(), this.fighter.cell!!, nearestEnnemy == null ? null : nearestEnnemy.cell);
            Function.getInstance().moveNearIfPossible(this.fight, this.fighter, nearestEnnemy!!)

            if (!this.coop) {
                val cells = this.getGlyphCells()

                if (cells.contains(this.fighter.cell!!.cellId)) {
                    nearestEnnemy = Function.getInstance().getEnnemyWithDistance(this.fight, this.fighter, 0, 10, null)
                    if (this.tpIfPossibleKaskargo(this.fight, this.fighter, nearestEnnemy) != 0) {
                        Function.getInstance().moveNearIfPossible(this.fight, this.fighter, nearestEnnemy!!)
                        this.coop = this.tpIfPossibleKaskargo(this.fight, this.fighter, nearestEnnemy) == 0
                    } else {
                        this.coop = true
                    }
                }
            }

            this.attackIfPossibleKaskargo(nearestEnnemy)
            this.addNext({ this.decrementCount() }, 1000)
        } else {
            this.stop = true
        }
    }

    private fun getGlyphCells(): MutableList<Int> {
        val cells = ArrayList<Int>()
        cells.addAll(this.fight.glyphs
            .stream().filter { glyph -> glyph != null && glyph.caster.id == this.fighter.id }
            .map { glyph -> glyph.cell.cellId }.collect(Collectors.toList()))
        return cells
    }

    private fun tpIfPossibleKaskargo(fight: Fight?, fighter: Fighter?, target: Fighter?): Int {
        if (fight == null || fighter == null || target == null)
            return 0

        return fight.tryCastSpell(fighter, Function.getInstance().findSpell(fighter, 445)!!, target.cell!!.cellId)
    }

    private fun attackIfPossibleKaskargo(ennemy: Fighter?): Int {
        if (ennemy == null) return 666

        val spellStat = Function.getInstance().findSpell(this.fighter, 949)

        val cells = this.getGlyphCells()
        cells.add(ennemy.cell!!.cellId)

        var bestCell: GameCase? = null
        var temp: Int
        val dist = 3

        if (PathFinding.getDistanceBetween(this.fight.map, this.fighter.cell!!.cellId, ennemy.cell!!.cellId) <= 1) {
            bestCell = this.fight.map!!.getCase(PathFinding.getAvailableCellArround(this.fight, ennemy.cell!!.cellId, null))
        } else {
            var path: List<GameCase>? = PathFinding.getShortestPathBetween(this.fight.map!!, this.fighter.cell!!.cellId, ennemy.cell!!.cellId, 3)
            for (cell in path!!) {
                if (cells.contains(cell.cellId)) continue

                temp = PathFinding.getDistanceBetweenTwoCase(this.fight.map, this.fighter.cell!!, cell)
                if (temp < dist && !PathFinding.haveFighterOnThisCell(temp, this.fight, false)) {
                    bestCell = cell
                }
            }

            if (bestCell == null) {
                val dir = PathFinding.getDirEntreDosCeldas(this.fight.map, this.fighter.cell!!.cellId, ennemy.cell!!.cellId)
                path = PathFinding.getCellsByDir(this.fight, this.fighter.cell!!.cellId, dir, 3)
                if (path.size == 0) return 10
                bestCell = path[path.size - 1]
            }
        }

        if (PathFinding.haveFighterOnThisCell(bestCell!!.cellId, this.fight, false))
            return 10
        return fight.tryCastSpell(this.fighter, spellStat!!, bestCell.cellId)
    }
}
