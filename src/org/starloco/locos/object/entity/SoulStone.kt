package org.starloco.locos.`object`.entity

import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.login.ObjectData
import org.starloco.locos.`object`.GameObject
import org.starloco.locos.util.Pair
import java.util.ArrayList
import java.util.Optional
import java.util.stream.Collectors
import java.util.stream.Stream
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(SoulStone::class.java)

class SoulStone : GameObject {

    private var monsters: ArrayList<Pair<Int, Int>> = ArrayList()

    constructor(id: Int, quantity: Int, template: Int, pos: Int, strStats: String) : super(id, template, quantity, pos, strStats, 0) {
        this.stringToStats(strStats)
        (DatabaseManager.get(ObjectData::class.java) as ObjectData).insert(this)
    }

    constructor(quantity: Int, template: Int, pos: Int, strStats: String) : super(-1, template, quantity, pos, strStats, 0) {
        this.stringToStats(strStats)
        (DatabaseManager.get(ObjectData::class.java) as ObjectData).insert(this)
    }

    fun getMonsters(): List<Pair<Int, Int>> = monsters

    fun getMonsterIDs(): Stream<Int> = monsters.stream().map { it.first }

    private fun stringToStats(m: String) {
        if (!m.equals("", ignoreCase = true)) {
            val split = m.split("|")
            for (s in split) {
                try {
                    val id = Integer.parseInt(s.split(",")[0])
                    val level = Integer.parseInt(s.split(",")[1])
                    val couple = Pair(id, level)
                    this.monsters.add(couple)
                } catch (e: Exception) {
                    log.error("unexpected error", e)
                }
            }
        }
    }

    override fun encodeStats(): String =
        this.monsters.stream().map { "26f####" + it.first.toString(16) }.collect(Collectors.joining(","))

    fun parseGroupData(): String {
        val toReturn = StringBuilder()
        var isFirst = true
        for (curMob in this.monsters) {
            if (!isFirst)
                toReturn.append(";")
            toReturn.append(curMob.first).append(",").append(curMob.second).append(",").append(curMob.second)
            isFirst = false
        }
        return toReturn.toString()
    }

    override fun parseToSave(): String {
        val toReturn = StringBuilder()
        var isFirst = true
        for (curMob in this.monsters) {
            if (!isFirst)
                toReturn.append("|")
            toReturn.append(curMob.first).append(",").append(curMob.second)
            isFirst = false
        }
        return toReturn.toString()
    }

    companion object {
        @JvmStatic
        fun safeCast(obj: GameObject?): Optional<SoulStone> =
            if (obj is SoulStone) Optional.of(obj) else Optional.empty()

        @JvmStatic
        fun isInArenaMap(id: Int): Boolean =
            "10131,10132,10133,10134,10135,10136,10137,10138".contains(id.toString())
    }
}
