package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.fight.spells.Spell
import org.starloco.locos.fight.spells.Spell.SortStats
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Main
import java.sql.SQLException

class SpellData(dataSource: HikariDataSource?) : FunctionDAO<Spell>(dataSource, "sorts") {

    override fun loadFully() {
        try {
            getData("SELECT * FROM " + getTableName() + ";") { result ->
                var modif = false

                while (result.next()) {
                    val id = result.getInt("id")
                    var spell = World.world.getSort(id)

                    val l1 = parseSortStats(id, 1, result.getString("lvl1"))
                    val l2 = parseSortStats(id, 2, result.getString("lvl2"))
                    val l3 = parseSortStats(id, 3, result.getString("lvl3"))
                    val l4 = parseSortStats(id, 4, result.getString("lvl4"))
                    var l5: SortStats? = null
                    var l6: SortStats? = null
                    if (!result.getString("lvl5").equals("-1", ignoreCase = true))
                        l5 = parseSortStats(id, 5, result.getString("lvl5"))
                    if (!result.getString("lvl6").equals("-1", ignoreCase = true))
                        l6 = parseSortStats(id, 6, result.getString("lvl6"))


                    if (spell != null) {
                        spell.setInfo(result.getInt("sprite"), result.getString("spriteInfos"), result.getString("effectTarget"), result.getInt("type"), result.getShort("duration"))
                        modif = true
                    } else {
                        spell = Spell(id, result.getString("nom") ?: "", result.getInt("sprite"), result.getString("spriteInfos"), result.getString("effectTarget"), result.getInt("type"), result.getShort("duration"), result.getString("invalid_state"), result.getString("needed_state"))
                        World.world.addSort(spell)
                    }
                    spell.spellsStats.clear()
                    spell.addSpellStats(1, l1)
                    spell.addSpellStats(2, l2)
                    spell.addSpellStats(3, l3)
                    spell.addSpellStats(4, l4)
                    spell.addSpellStats(5, l5)
                    spell.addSpellStats(6, l6)

                }
                if (modif)
                    for (monster in World.world.monstres)
                        monster.grades.values.forEach { it.refresh() }
            }
        } catch (e: SQLException) {
            super.sendError(e)
            Main.stop("Can't load spells")
        }
    }

    override fun load(id: Int): Spell? {
        throw NotImplementedException()
    }

    override fun insert(entity: Spell): Boolean {
        throw NotImplementedException()
    }

    override fun delete(entity: Spell) {
        throw NotImplementedException()
    }

    override fun update(entity: Spell) {
        throw NotImplementedException()
    }

    override fun getReferencedClass(): Class<*> {
        return SpellData::class.java
    }

    private fun parseSortStats(id: Int, lvl: Int, str: String): SortStats? {
        try {
            val stat = str.split(",")
            val effets = stat[0]
            val CCeffets = stat[1]
            var PACOST = 6

            try {
                PACOST = (stat[2].trim()).toInt()
            } catch (ignored: NumberFormatException) {
            }

            val POm = (stat[3].trim()).toInt()
            val POM = (stat[4].trim()).toInt()
            val TCC = (stat[5].trim()).toInt()
            val TEC = (stat[6].trim()).toInt()

            val line = stat[7].trim().equals("true", ignoreCase = true)
            val LDV = stat[8].trim().equals("true", ignoreCase = true)
            val emptyCell = stat[9].trim().equals("true", ignoreCase = true)
            val MODPO = stat[10].trim().equals("true", ignoreCase = true)

            val MaxByTurn = (stat[12].trim()).toInt()
            val MaxByTarget = (stat[13].trim()).toInt()
            val CoolDown = (stat[14].trim()).toInt()
            val type = stat[15].trim()
            val level = (stat[stat.size - 2].trim()).toInt()
            val endTurn = stat[19].trim().equals("true", ignoreCase = true)

            return SortStats(id, lvl, PACOST, POm, POM, TCC, TEC, line, LDV, emptyCell, MODPO, MaxByTurn, MaxByTarget, CoolDown, level, endTurn, effets, CCeffets, type)
        } catch (e: Exception) {
            super.sendError(e)
            return null
        }
    }
}
