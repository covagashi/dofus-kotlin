package org.starloco.locos.`object`.entity

import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.login.ObjectData
import org.starloco.locos.game.world.World.Couple
import org.starloco.locos.`object`.GameObject
import java.util.ArrayList

class Fragment : GameObject {

    val runes: ArrayList<Couple<Int, Int>> = ArrayList()

    constructor(id: Int, runes: String) : super(id) {
        this.parseRunes(runes)
    }

    constructor(runes: String) : super(-1) {
        this.parseRunes(runes)
        (DatabaseManager.get(ObjectData::class.java) as ObjectData).insert(this)
    }

    private fun parseRunes(runes: String) {
        if (runes.isNotEmpty()) {
            for (rune in runes.split(";")) {
                val split = rune.split(":")
                this.runes.add(Couple(Integer.parseInt(split[0]), Integer.parseInt(split[1])))
            }
        }
    }

    fun addRune(id: Int) {
        val rune = this.search(id)

        if (rune == null)
            this.runes.add(Couple(id, 1))
        else
            rune.second += 1
    }

    fun search(id: Int): Couple<Int, Int>? {
        for (couple in this.runes)
            if (couple.first == id)
                return couple
        return null
    }
}
