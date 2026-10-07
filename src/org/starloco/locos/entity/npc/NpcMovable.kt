package org.starloco.locos.entity.npc

import org.starloco.locos.common.PathFinding
import org.starloco.locos.common.SocketManager
import org.starloco.locos.game.world.World
import org.slf4j.LoggerFactory
import org.starloco.locos.common.splitJ

private val log = LoggerFactory.getLogger(NpcMovable::class.java)

class NpcMovable(id: Int, cellid: Int, orientation: Byte, private val mapId: Int, template: Int) :
    Npc(id, cellid, orientation, template) {

    private var position = 0
    private var path: Array<String>

    init {
        this.path = this.template!!.legacy!!.path.splitJ(";").toTypedArray()
        movables.add(this)
    }

    private fun move() {
        if (this.position >= this.path.size) return
        val dir = this.path[this.position][0]
        val nbr: Short

        val map = World.world.getMap(mapId)

        if (dir == 'E') {
            nbr = this.path[this.position].substring(1).toShort()

            for (player in map.players)
                player.send("eUK${this.id}|$nbr")
        } else {
            nbr = this.path[this.position][1].toString().toShort()

            var oldCell = this.cellId
            var cell = this.cellId

            for (i in 0..nbr.toInt()) {
                cell = PathFinding.getCaseIDFromDirrection(cell, getDirByChar(dir), map)
                if (!map.getCase(cell)!!.isWalkable(false)) break
                oldCell = cell
            }

            val pathStr: String = try {
                PathFinding.getShortestStringPathBetween(map, this.cellId, oldCell, 25)
            } catch (e: Exception) {
                log.error("unexpected error", e)
                return
            } ?: return

            for (player in map.players)
                SocketManager.GAME_SEND_GA_PACKET(player.gameClient!!, "0", "1", this.id.toString(), pathStr)

            this.cellId = oldCell
        }

        this.position++

        if (this.position == this.path.size) {
            val templatePath = template.legacy!!.path
            this.path =
                if (getPath(this.path) == templatePath) inverseOfPath(templatePath).splitJ(";").toTypedArray() else templatePath.splitJ(
                    ";"
                ).toTypedArray()
            this.position = 0
        }
    }

    companion object {
        private val movables = ArrayList<NpcMovable>()

        private fun inverseOfPath(arg: String): String {
            val split = arg.split(";")
            var `var` = ""

            for (i in split.size - 1 downTo 0) {
                var loc0 = split[i]

                if (loc0.contains("R"))
                    continue

                when (loc0[0]) {
                    'H' -> loc0 = loc0.replace("H", "B")
                    'B' -> loc0 = loc0.replace("B", "H")
                    'G' -> loc0 = loc0.replace("G", "D")
                    'D' -> loc0 = loc0.replace("D", "G")
                }

                `var` += (if (`var`.isEmpty()) "" else ";") + loc0
            }
            return `var`
        }

        private fun getPath(path: Array<String>): String {
            var original = ""

            for (arg in path)
                original += (if (original.isEmpty()) "" else ";") + arg

            return original
        }

        private fun getDirByChar(letter: Char): Char {
            return when (letter) {
                'H' -> 'f'
                'B' -> 'b'
                'G' -> 'd'
                'D' -> 'h'
                else -> '?'
            }
        }

        @JvmStatic
        fun moveAll() {
            movables.forEach { it.move() }
        }
    }
}
