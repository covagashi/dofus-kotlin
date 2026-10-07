package org.starloco.locos.`object`

import org.starloco.locos.client.other.Stats
import org.starloco.locos.game.world.World

import java.util.ArrayList
import org.slf4j.LoggerFactory
import org.starloco.locos.common.splitJ

private val log = LoggerFactory.getLogger(ObjectSet::class.java)

class ObjectSet(val id: Int, items: String, bonuses: String) {

    private val effects = ArrayList<Stats>()
    val itemTemplates = ArrayList<ObjectTemplate>()

    init {
        for (str in items.splitJ(",")) {
            try {
                val obj = World.world.getObjTemplate(str.trim().toInt()) ?: continue
                this.itemTemplates.add(obj)
            } catch (e: Exception) {
                log.error("unexpected error", e)
                }
        }

        this.effects.add(Stats())

        for (str in bonuses.splitJ(";")) {
            val S = Stats()
            for (str2 in str.splitJ(",")) {
                if (!str2.equals("", ignoreCase = true)) {
                    try {
                        val infos = str2.split(":")
                        val stat = infos[0].toInt()
                        val value = infos[1].toInt()
                        //on ajoute a la stat
                        S.addOneStat(stat, value)
                    } catch (e: Exception) {
                        log.error("unexpected error", e)
                }
                }
            }
            this.effects.add(S)
        }
    }

    fun getBonusStatByItemNumb(numb: Int): Stats {
        if (numb > this.effects.size)
            return Stats()
        return effects[numb - 1]
    }
}
