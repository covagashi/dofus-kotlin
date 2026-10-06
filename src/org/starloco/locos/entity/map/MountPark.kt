package org.starloco.locos.entity.map

import org.starloco.locos.common.Formulas
import org.starloco.locos.entity.mount.Mount
import org.starloco.locos.game.world.World
import org.starloco.locos.guild.Guild
import java.util.concurrent.CopyOnWriteArrayList

class MountPark(
    val map: Int,
    var cell: Int = -1,
    val size: Int,
    val priceBase: Int,
    var placeOfSpawn: Int,
    var door: Int,
    cellOfObjectStr: String,
    val maxObject: Int
) {

    var owner: Int = 0
    var guild: Guild? = null
    var price: Int = 0
    var cellOfObject = ArrayList<Int>()
        set(array) {
            field = ArrayList(array)
        }
    private val cellAndObject = HashMap<Int, Int>()
    val objDurab = HashMap<Int, MutableMap<Int, Int>>()
    private val breedingObject = HashMap<Int, MutableMap<Int, Int>>()
    private val raising = CopyOnWriteArrayList<Int>()
    private val etable = ArrayList<Mount>()

    init {
        if (cellOfObjectStr.isNotEmpty()) {
            for (cases in cellOfObjectStr.split(";")) {
                val cellId = cases.toInt()
                if (cellId > 0)
                    this.cellOfObject.add(cellId)
            }
        }
    }

    fun setData(owner: Int, guild: Int, price: Int, raising: String, objects: String, objDurab: String, etable: String) {
        this.owner = owner
        this.guild = World.world.getGuild(guild)
        this.price = price
        this.parseBreedObjects(objects)
        //chargement de la liste des dragodinde dans l'table
        for (i in raising.split(";")) {
            try {
                val mount = World.world.getMountById(i.toInt())
                if (mount != null) this.etable.add(mount)
            } catch (ignored: Exception) {
            }
        }
        this.parseDurabilityObjects(objDurab)
        if (etable.isNotEmpty())
            for (dd in etable.split(";")) {
                try {
                    this.raising.add(dd.toInt())
                    val mount = World.world.getMountById(dd.toInt())
                    mount!!.mapId = this.map
                    mount.cellId = mount.cellId
                } catch (ignored: Exception) {
                }
            }

        for (firstCut in etable.split(";"))//PosseseurID,DragoID;PosseseurID2,DragoID2;PosseseurID,DragoID3
        {
            try {
                val secondCut = firstCut.split(",")
                val DD = World.world.getMountById(secondCut[1].toInt()) ?: continue
                this.raising.add(secondCut[1].toInt(), secondCut[0].toInt())
            } catch (ignored: Exception) {
            }
        }
    }

    fun getMountcell(): Int = placeOfSpawn

    fun setMountCell(id: Int) {
        this.placeOfSpawn = id
    }

    fun hasEtableFull(id: Int): Boolean {
        if (this.owner == -1) {
            var i = 0
            for (mount in this.etable)
                if (mount.owner == id)
                    i++
            return i >= 100
        } else {
            return this.etable.size >= 100
        }
    }

    fun hasEnclosFull(id: Int): Boolean {
        if (this.owner == -1) {
            var i = 0
            for (mountId in this.raising)
                if (mountId == id)
                    i++
            return i >= this.size
        } else {
            return this.raising.size >= this.size
        }
    }

    fun addCellObject(cell: Int) {
        if (this.cellOfObject.contains(cell))
            return
        if (cell <= 0)
            return
        this.cellOfObject.add(cell)
    }

    private fun parseBreedObjects(objects: String) {
        if (objects.isNotEmpty()) {
            for (obj in objects.split("|")) {
                val info = obj.split(";")
                val cellId = info[0].toInt()
                val objectId = info[1].toInt()
                val proprietor = info[2].toInt()
                val other = HashMap<Int, Int>()
                other[objectId] = proprietor
                this.cellAndObject[cellId] = objectId
                this.breedingObject[cellId] = other
            }
        }
    }

    private fun parseDurabilityObjects(objects: String) {
        if (objects.isNotEmpty()) {
            for (obj in objects.split("|")) {
                val info = obj.split(";")
                val cellId = info[0].toInt()
                val durability = info[1].toInt()
                val durabilityMax = info[2].toInt()
                val inDurab = HashMap<Int, Int>()
                inDurab[durability] = durabilityMax
                this.objDurab[cellId] = inDurab
            }
        }
    }

    fun parseStringCellObject(): String {
        var cell = ""
        var first = true
        for (i in this.cellOfObject) {
            if (first)
                cell += i
            else
                cell += ";$i"
            first = false
        }
        return cell
    }

    fun getCellAndObject(): Map<Int, Int> = this.cellAndObject

    fun addObject(cell: Int, `object`: Int, owner: Int, durability: Int, durabilityMax: Int) {
        if (cell in this.breedingObject) {
            this.breedingObject.remove(cell)
            this.cellAndObject.remove(cell)
        }
        val other = HashMap<Int, Int>()
        other[`object`] = owner

        val inDurab = HashMap<Int, Int>()
        inDurab[durability] = durabilityMax

        this.cellAndObject[cell] = `object`
        this.breedingObject[cell] = other
        this.objDurab[cell] = inDurab
    }

    fun delObject(cell: Int): Boolean {
        if (cell !in this.breedingObject && cell !in this.objDurab)
            return false
        this.objDurab.remove(cell)
        this.breedingObject.remove(cell)
        this.cellAndObject.remove(cell)
        return true
    }


    fun getObject(): Map<Int, MutableMap<Int, Int>> = this.breedingObject

    fun addRaising(id: Int) {
        this.raising.add(id)
    }

    fun delRaising(id: Int) {
        if (this.raising.contains(id))
            this.raising.removeAt(this.raising.indexOf(id))
    }

    fun getListOfRaising(): CopyOnWriteArrayList<Int> = this.raising

    fun getEtable(): ArrayList<Mount> = this.etable

    fun containsMountInList(mounts: List<Mount>, target: Mount?): Mount? {
        if (target != null) {
            for (mount in mounts) {
                if (mount != null && mount.id == target.id)
                    return mount
            }
        }
        return null
    }

    @Synchronized
    fun startMoveMounts() {
        if (this.raising.size > 0) {
            val directions = charArrayOf('b', 'd', 'f', 'h')
            for (id in this.raising) {
                val mount = World.world.getMountById(id)
                mount?.moveMountsAuto(directions[Formulas.getRandomValue(0, 3)], 3, false)
            }
        }
    }

    fun getStringObject(): String {
        var str = ""
        var first = false

        if (this.breedingObject.size == 0)
            return str

        for (entry in this.breedingObject.entries) {
            if (first) str += "|"
            str += entry.key

            for (entry2 in entry.value.entries)
                str += ";" + entry2.key + ";" + entry2.value
            first = true
        }
        return str
    }

    fun getStringObjDurab(): String {
        var str = ""
        var first = false

        if (this.objDurab.size == 0)
            return str

        for (entry in this.objDurab.entries) {
            if (first)
                str += "|"
            str += entry.key
            for (entry2 in entry.value.entries) {
                str += ";" + entry2.key + ";" + entry2.value
            }
            first = true
        }
        return str
    }

    fun parseRaisingToString(): String {
        var str = ""
        var first = true

        if (this.raising.size == 0)
            return ""

        for (id in this.raising) {
            if (!first) str += ";"
            str += id
            first = false
        }
        return str
    }

    fun parseEtableToString(): String {
        var str = ""
        for (mount in this.etable) {
            if (!str.equals("", ignoreCase = true))
                str += ";"
            str += mount.id
        }
        return str
    }
}
