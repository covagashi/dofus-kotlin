package org.starloco.locos.entity.monster.boss

import org.starloco.locos.common.Formulas
import org.starloco.locos.game.world.World
import java.util.ArrayList

class MaitreCorbac {
    /*
     * Subarea : 211 Group : 289,120,200;825,90,98;823,90,98;824,80,88
     */
    private var oldMap = 0
    private var map = 0

    init {
        repop(-1)
    }

    fun repop(id: Int) {
        if (this.oldMap == id)
            return

        this.oldMap = id

        val maps = ArrayList(World.world.getSubArea(211)!!.getMaps())
        maps.remove(World.world.getMap(9589))
        maps.remove(World.world.getMap(9604))

        var index = Formulas.random.nextInt(maps.size)
        var map = maps[index]

        while (map.id == id) {
            index = Formulas.random.nextInt(maps.size)
            map = maps[index]
        }

        this.map = map.id
        map.spawnGroupOnCommand(map.randomFreeCellId, "289,120,200;825,90,98;823,90,98;824,80,88", true)
    }

    fun check(): Int {
        return when (this.map) {
            9590, 9594, 9596, 9600 -> 3188
            9592, 9597, 9593, 9598 -> 3193
            9599, 9591, 9595, 9603 -> 3191
            9601, 9723, 9602, 9724 -> 3194
            else -> -1
        }
    }
}
