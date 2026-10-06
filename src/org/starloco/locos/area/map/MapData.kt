package org.starloco.locos.area.map

import org.starloco.locos.anims.Animation
import org.starloco.locos.area.Area
import org.starloco.locos.area.SubArea
import org.starloco.locos.client.Player
import org.starloco.locos.common.CryptManager
import org.starloco.locos.entity.monster.MobGroupDef
import org.starloco.locos.entity.monster.MonsterGrade
import org.starloco.locos.fight.Fight
import org.starloco.locos.fight.Fighter
import org.starloco.locos.game.world.World
import org.starloco.locos.util.Pair

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.Optional
import java.util.stream.Collectors

// Holds all static data for maps
abstract class MapData(
    // MapData must only contain final fields. It's a READ ONLY class
    @JvmField val id: Int,
    @JvmField val date: String,
    @JvmField val key: String,
    data: String,
    @JvmField val width: Int,
    @JvmField val height: Int,
    @JvmField val x: Int,
    @JvmField val y: Int,
    @JvmField val subAreaID: Int,
    @JvmField val noSellers: Boolean,
    @JvmField val noCollectors: Boolean,
    @JvmField val noPrisms: Boolean,
    @JvmField val noTp: Boolean,
    @JvmField val noDefy: Boolean,
    @JvmField val noAgro: Boolean,
    @JvmField val noCanal: Boolean,
    @JvmField val mobGroupsMaxCount: Int,
    @JvmField val mobGroupsMinSize: Int,
    @JvmField val mobGroupsMaxSize: Int,
    @JvmField val mobPossibles: List<MonsterGrade>,
    @JvmField val places: List<List<Int>>,
    animations: MutableMap<Int, Animation>
) : CellsDataProvider {

    @JvmField
    val interactiveObjects: Map<Int, Int>
    @JvmField
    val animations: Map<Int, Animation>

    // Temporary variable to be able to copy the map.
    // Eventually, we should split GameCase from CellData, or use neither of those
    @JvmField
    val cellsData: CellsDataProvider.RawCellsDataProvider

    init {
        var data = data
        if (CryptManager.isMapCiphered(data)) {
            try {
                data = CryptManager.decryptMapData(data, key)
            } catch (e: Exception) {
                throw RuntimeException("Cannot decipher mapdata #$id", e)
            }
        }
        // Decode b64
        val dataBytes = ByteArray(data.length)
        for (i in data.indices) {
            dataBytes[i] = CryptManager.getIntByHashedValue(data[i]).toByte()
        }

        this.cellsData = CellsDataProvider.RawCellsDataProvider(dataBytes)

        if (cellsData.cellCount() != cellCount()) {
            throw IllegalStateException(
                ("Map #%d_%s: cellsData length doesn't match map cell count").format( id, date)
            )
        }

        val interactiveObjects = HashMap<Int, Int>()
        val anims = HashMap<Int, Animation>()
        for (cellId in 0 until cellsData.cellCount()) {
            if (!cellsData.object2Interactive(cellId)) continue

            val objectID = cellsData.object2(cellId)
            interactiveObjects[cellId] = objectID

            // Add animation for object
            val anim = World.world.getAnimation(objectID)
            if (anim.isPresent) {
                anims[cellId] = anim.get()
            }
        }
        this.interactiveObjects = Collections.unmodifiableMap(interactiveObjects)
        this.animations = Collections.unmodifiableMap(anims)
    }

    fun getSubArea(): SubArea? {
        return World.world.getSubArea(subAreaID)
    }

    fun getArea(): Area? {
        return getSubArea()?.area
    }

    // TODO: Replace with Pair<List<Integer>,List<Integer>>
    fun getPlaces(): List<List<Int>> = places

    abstract fun getNPCs(): Map<Int, Pair<Int, Int>>

    fun getForbidden(): String {
        return ((if (noSellers) 1 else 0).toString() + ";" + (if (noCollectors) 1 else 0) + ";" + (if (noPrisms) 1 else 0) + ";" + (if (noTp) 1 else 0) +
                ";" + (if (noDefy) 1 else 0) + ";" + (if (noAgro) 1 else 0) + ";" + (if (noCanal) 1 else 0))
    }

    abstract fun getStaticGroups(): List<MobGroupDef>

    abstract fun onMoveEnd(p: Player)

    abstract fun cellHasMoveEndActions(cellId: Int): Boolean

    open fun onFightInit(f: Fight, team0: Collection<Fighter>, team1: Collection<Fighter>) {}
    open fun onFightStart(f: Fight, team0: Collection<Fighter>, team1: Collection<Fighter>) {}

    abstract fun hasFightEndForType(type: Int): Boolean
    abstract fun onFightEnd(f: Fight, p: Player, winTeam: List<Fighter>, looseTeam: List<Fighter>)

    override fun cellCount(): Int {
        return width * height + (width - 1) * (height - 1)
    }

    fun interactiveObjects(): Map<Int, Int> = interactiveObjects

    override fun cellData(cellID: Int): Long {
        return cellsData.cellData(cellID)
    }

    override fun overrideMask(cellID: Int): Int {
        return cellsData.overrideMask(cellID)
    }

    companion object {
        @JvmStatic
        fun decodePositions(strPlaces: String): List<List<Int>> {
            val out = ArrayList<List<Int>>()
            for (p in strPlaces.split("|")) {
                if (p.isEmpty()) continue
                if (p.length % 2 != 0) throw IllegalArgumentException("places length must be pair")

                val teamPlaces = ArrayList<Int>(p.length shr 1)
                var i = 0
                while (i < p.length) {
                    teamPlaces.add((CryptManager.getIntByHashedValue(p[i]) shl 6) + CryptManager.getIntByHashedValue(p[i + 1]))
                    i += 2
                }
                out.add(Collections.unmodifiableList(teamPlaces))
            }
            return Collections.unmodifiableList(out)
        }

        @JvmStatic
        fun encodePositions(positions: List<List<Int>>): String {
            return positions.joinToString("|") { teamPositions ->
                teamPositions.joinToString("") { CryptManager.cellID_To_Code(it) }
            }
        }
    }
}
