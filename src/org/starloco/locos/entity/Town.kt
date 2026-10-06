package org.starloco.locos.entity

import org.starloco.locos.area.Area
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.game.world.World

class Town(
    val id: Int,
    val area: Area,
    val mainDoorMap: GameMap,
    val mainDoorOpeningDuration: Int,
    val prismRoomMap: GameMap, /*short prs,*/
    val prismRoomOpeningDuration: Int
) {

    val alignment: Int
        get() = area.alignement

    companion object {
        @JvmField
        val TOWNS = arrayOf(
            Town(1, World.world.getArea(23)!!, World.world.getMap(7951), 10, World.world.getMap(7951), 1),
            //new Town(2, World.world.getArea(1), World.world.getMap(1), 10, World.world.getMap(1), 1),
            //new Town(3, World.world.getArea(1), World.world.getMap(1), 10, World.world.getMap(1), 1),
            //new Town(4, World.world.getArea(1), World.world.getMap(1), 10, World.world.getMap(1), 1),
            /*new Town(5, World.world.getArea(1), World.world.getMap(1), 10, World.world.getMap(1), 1),
            new Town(6, World.world.getArea(1), World.world.getMap(1), 10, World.world.getMap(1), 1),
            new Town(6, World.world.getArea(1), World.world.getMap(1), 10, World.world.getMap(1), 1),*/
        )
    }
}
