package org.starloco.locos.area.map

import org.starloco.locos.client.Player
import org.starloco.locos.entity.map.InteractiveObject
import org.starloco.locos.entity.map.InteractiveObjectTemplate
import org.starloco.locos.fight.Fighter
import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.GameObject
import java.util.Collections
import java.util.Optional
import java.util.stream.Collectors

//  Temporary proxy class until we fully clean up GameCase usages
class GameCase(val map: GameMap, @JvmField val cellId: Int) {

    fun getId(): Int = cellId

    @Suppress("UNCHECKED_CAST")
    private fun <T : Actor> getActorsOfType(clz: Class<T>): List<T> =
        map.actors.getOrDefault(cellId, emptySet<Actor>()).stream()
            .filter { clz.isInstance(it) }
            .map { clz.cast(it) }
            .collect(Collectors.collectingAndThen(Collectors.toList<T>(), { Collections.unmodifiableList(it) }))

    val players: List<Player>
        get() = getActorsOfType(Player::class.java)

    val fighters: List<Fighter>
        get() = getActorsOfType(Fighter::class.java)

    fun getDroppedItem(delete: Boolean): GameObject? =
        if (delete) map.droppedItems.remove(cellId) else map.droppedItems[cellId]

    fun clearDroppedItem() {
        map.droppedItems.remove(cellId)
    }

    val `object`: InteractiveObject?
        get() = map.interactiveObjects?.get(cellId)

    fun isWalkable(inFight: Boolean): Boolean {
        if (!map.cellsData.active(cellId)) return false
        return when (map.cellsData.movement(cellId)) {
            0 -> false
            1 -> !inFight // Only walkable out of fights
            else -> true // 2+
        }
    }

    fun isWalkableFight(): Boolean = this.isWalkable(true)

    fun isWalkable(checkObject: Boolean, inFight: Boolean, targetCell: Int): Boolean {
        // Official servers:
        // Cells without objects: depends on walkable field
        // Cells with objects:
        // -> Cannot stop on cell if object is ready to harvest
        // -> Can always go through walkable objects
        val ioDef: Optional<InteractiveObjectTemplate> =
            World.world.getObjectBySprite(this.map.cellsData.object2(cellId))

        // Cells with objects:
        if (checkObject && ioDef.isPresent) {
            val isReady = this.map.getAnimationState(cellId) == null || this.map.getAnimationState(cellId) == "ready"
            // -> Cannot stop on cell if object is ready to harvest
            if (this.cellId == targetCell && isReady) return false
            // -> Cannot go through non-walkable objects
            if (!ioDef.get().isWalkable) return false
        }
        return this.isWalkable(inFight)
    }

    private fun <T : Actor> addActor(actor: T) {
        map.actors.computeIfAbsent(cellId) { HashSet() }.add(actor)
    }

    fun addPlayer(player: Player) = addActor(player)

    fun addFighter(init0: Fighter) = addActor(init0)

    private fun <T : Actor> removeActor(actor: T) {
        map.actors[cellId]?.remove(actor)
    }

    fun removePlayer(p: Player) = removeActor(p)

    fun removeFighter(f: Fighter) = removeActor(f)

    val firstFighter: Fighter?
        get() = if (fighters.isEmpty()) null else fighters[0]

    fun tryDropItem(obj: GameObject) {
        if (map.droppedItems.putIfAbsent(cellId, obj) != null) {
            throw IllegalStateException("attempted to drop item on a cell that already has a dropped item")
        }
    }

    fun blockLoS(): Boolean {
        if (fighters == null)
            return map.data.lineOfSight(cellId)
        var hide = true
        for (fighter in fighters)
            if (!fighter.isHidden())
                hide = false
        return map.data.lineOfSight(cellId) && hide
    }
}
