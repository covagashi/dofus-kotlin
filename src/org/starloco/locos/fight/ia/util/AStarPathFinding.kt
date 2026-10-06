package org.starloco.locos.fight.ia.util

import org.starloco.locos.area.map.GameCase
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.common.PathFinding
import org.starloco.locos.fight.Fight
import java.util.*

class AStarPathFinding {

    private val openList: MutableMap<Int, Node> = HashMap()
    private val closeList: MutableMap<Int, Node> = LinkedHashMap()

    private var fight: Fight? = null
    private var map: GameMap? = null
    private var cellStart = 0
    private var cellEnd = 0

    constructor(map: GameMap?, cellStart: Int, cellEnd: Int) {
        this.map = map
        this.cellStart = cellStart
        this.cellEnd = cellEnd
    }

    constructor(fight: Fight, cellStart: Int, cellEnd: Int) {
        this.fight = fight
        this.map = fight.map
        this.cellStart = cellStart
        this.cellEnd = cellEnd
        val cellEnd1 = if (map != null) fight.map!!.getCase(cellEnd) else null
        if (cellEnd1 != null && Function.getInstance().getCellsAvailableAround(fight, cellEnd1, true, 0.toByte()).isEmpty()) {
            val dist = PathFinding.getDistanceBetween(map, cellStart, cellEnd)
            for (cell in Function.getInstance().getCellsAround(fight, cellEnd1)) {
                val cells2 = Function.getInstance().getCellsAvailableAround(fight, cell, true, 0.toByte())
                for (check in cells2) {
                    val tmp = PathFinding.getDistanceBetween(map, cellStart, check.cellId)
                    if (tmp < dist) {
                        this.cellEnd = check.cellId
                    }
                }
            }
            if (cellEnd1.cellId == this.cellEnd)
                this.cellEnd = -1
        }
    }

    fun getShortestPath(): ArrayList<GameCase>? {
        if (this.cellEnd == -1) return getPath()

        val start = Node(map!!.getCase(cellStart)!!, null)
        openList[cellStart] = start

        while (!openList.isEmpty() && !closeList.containsKey(cellEnd)) {
            val dirs = charArrayOf('b', 'd', 'f', 'h')
            val current = bestNode()

            val around = fight != null && PathFinding.cellArroundCaseIDisOccuped(fight!!, current.cell.cellId)

            if (current.cell.cellId == cellEnd && !around)
                return getPath()

            addListClose(current)
            for (loc0 in 0..3) {
                val cellId = PathFinding.getCaseIDFromDirrection(current.cell.cellId, dirs[loc0], map!!)

                val cell = map!!.getCase(cellId) ?: continue

                val node = Node(cell, current)
                if (node.cell == null || !node.cell.isWalkable(true, true, -1) && cellId != cellEnd)
                    continue

                val occupied = fight != null && PathFinding.haveFighterOnThisCell(cellId, fight!!, true)
                if (occupied && cellId != cellEnd || closeList.containsKey(cellId))
                    continue

                if (openList.containsKey(cellId)) {
                    if (openList[cellId]!!.countG > getCostG(node)) {
                        current.child = openList[cellId]
                        openList[cellId]!!.parent = current
                        openList[cellId]!!.countG = getCostG(node)
                        openList[cellId]!!.heristic = PathFinding.getDistanceBetween(map, cellId, cellEnd) * 10
                        openList[cellId]!!.countF = openList[cellId]!!.countG + openList[cellId]!!.heristic
                    }
                } else {
                    openList[cellId] = node
                    current.child = node
                    node.parent = current
                    node.countG = getCostG(node)
                    node.heristic = PathFinding.getDistanceBetween(map, cellId, cellEnd) * 10
                    node.countF = node.countG + node.heristic
                }
            }
        }
        return getPath()
    }

    private fun getPath(): ArrayList<GameCase>? {
        var current: Node? = getLastNode(closeList) ?: return null

        val path = ArrayList<GameCase>()
        val path0 = HashMap<Int, GameCase>()
        var index = closeList.size
        while (current!!.cell.cellId != cellStart) {
            if (current!!.cell.cellId == cellStart)
                continue
            path0[index] = map!!.getCase(current!!.cell.cellId)!!
            current = current!!.parent
            index--
        }
        var i = -1
        while (path.size != path0.size) {
            i++
            val cell = path0[i]
            if (cell != null) path.add(cell)
        }
        return path
    }

    private fun getLastNode(list: Map<Int, Node>): Node? {
        var node: Node? = null
        for (entry in list.values)
            node = entry
        return node
    }

    private fun bestNode(): Node {
        var bestCountF = 150000
        var bestNode: Node? = null
        for (node in openList.values) {
            if (node.countF < bestCountF) {
                bestCountF = node.countF
                bestNode = node
            }
        }
        return bestNode!!
    }

    private fun addListClose(node: Node) {
        val id = node.cell.cellId
        if (openList.containsKey(id)) openList.remove(id)
        if (!closeList.containsKey(id)) closeList[id] = node
    }

    private fun getCostG(node: Node): Int {
        var n = node
        var costG = 0
        while (n.cell.cellId == cellStart) {
            n = n.parent!!
            costG += 10
        }
        return costG
    }
}
