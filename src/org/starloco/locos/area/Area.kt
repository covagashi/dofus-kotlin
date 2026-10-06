package org.starloco.locos.area

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.script.Scripted
import org.starloco.locos.script.proxy.SArea
import java.util.ArrayList

class Area(val id: Int, val superArea: Int) : Scripted<SArea> {

    private val scriptVal: SArea = SArea(this)

    var alignement = 0
        set(value) {
            if (field == 1 && value == -1)
                bontarians--
            else if (field == 2 && value == -1)
                brakmarians--
            else if (field == -1 && value == 1)
                bontarians++
            else if (field == -1 && value == 2)
                brakmarians++
            field = value
        }
    var prismId = 0
    private var subAreas = ArrayList<SubArea>()

    fun addSubArea(subArea: SubArea) {
        this.subAreas.add(subArea)
    }

    fun getSubAreas(): ArrayList<SubArea> = subAreas

    fun getMaps(): ArrayList<GameMap> {
        val maps = ArrayList<GameMap>()
        for (subArea in this.subAreas)
            maps.addAll(subArea.getMaps())
        return maps
    }

    override fun scripted(): SArea = scriptVal

    companion object {
        @JvmField
        var bontarians = 0
        @JvmField
        var brakmarians = 0
    }
}
