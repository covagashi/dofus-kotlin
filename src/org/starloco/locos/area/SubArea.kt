package org.starloco.locos.area

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.client.Player
import org.starloco.locos.entity.Prism
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Constant
import org.starloco.locos.script.Scripted
import org.starloco.locos.script.proxy.SSubArea
import java.util.ArrayList
import java.util.HashSet
import java.util.stream.Collectors

class SubArea(
    val id: Int,
    val name: String,
    area: Int,
    nearest: String
) : Scripted<SSubArea> {

    private val scriptVal: SSubArea = SSubArea(this)

    val area: Area? = World.world.getArea(area)
    var alignment = 0
    var prism: Prism? = null
    var conquerable = false

    private val mapIDs = HashSet<Int>()
    private val nearestSubAreas = ArrayList<Short>()

    init {
        if (nearest.isNotEmpty())
            for (i in nearest.split(","))
                this.nearestSubAreas.add(i.toShort())
    }

    fun getMaps(): List<GameMap> =
        mapIDs.stream().map { World.world.getMap(it) }.collect(Collectors.toList())

    fun addMapID(mapID: Int) {
        this.mapIDs.add(mapID)
    }

    fun ownNearestSubArea(player: Player): Boolean {
        for (id in this.nearestSubAreas) {
            val temp = World.world.getSubArea(id.toInt())
            if (temp != null && temp.alignment == player.alignment)
                return true
        }
        return false
    }

    fun isMoreThanEnemies(player: Player): Boolean {
        var bonta: Short = 0
        var brak: Short = 1

        for (map in getMaps()) {
            for (temp in map.players) {
                if (!temp.isOnline) continue
                if (temp.alignment == Constant.ALIGNEMENT_BONTARIEN) bonta++
                else if (temp.alignment == Constant.ALIGNEMENT_BRAKMARIEN) brak++
            }
        }
        return if (player.alignment == Constant.ALIGNEMENT_BRAKMARIEN) brak > bonta else bonta > brak
    }

    override fun scripted(): SSubArea = scriptVal
}
