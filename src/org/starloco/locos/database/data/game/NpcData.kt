package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.starloco.locos.util.Pair
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.entity.npc.Npc
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Config
import java.sql.PreparedStatement
import java.sql.SQLException

class NpcData(dataSource: HikariDataSource?) : FunctionDAO<Pair<Npc, Int>>(dataSource, "npcs") {

    override fun loadFully() {
        try {
            getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
                    val mapID = result.getInt("mapid")
                    World.world.getMapData(mapID)!!.orElseThrow { IllegalArgumentException(("unknown map #%d").format( mapID)) }

                    val id = result.getInt("npcid")
                    if (!Config.modeChristmas && id == 795) // PNJ Noel
                        continue
                    if (Config.modeHeroic && (id == 1121 || id == 1127)) // PNJ Traque Heroic
                        continue
                }
            }
        } catch (e: SQLException) {
            super.sendError(e)
        }
    }

    override fun load(id: Int): Pair<Npc, Int>? {
        throw NotImplementedException()
    }

    override fun insert(entity: Pair<Npc, Int>): Boolean {
        var p: PreparedStatement? = null
        var ok = true
        try {
            p = getPreparedStatement("INSERT INTO " + getTableName() + " VALUES (?,?,?,?,?);")
            p?.setInt(1, entity.second)
            p?.setInt(2, entity.first.template!!.id)
            p?.setInt(3, entity.first.cellId)
            p?.setInt(4, entity.first.orientation.toInt())
            p?.setBoolean(5, false)
            execute(p)
        } catch (e: SQLException) {
            ok = false
            super.sendError(e)
        } finally {
            close(p)
        }
        return ok
    }

    override fun delete(entity: Pair<Npc, Int>) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE mapid = ? AND cellid = ?;")
            p?.setInt(1, entity.second)
            p?.setInt(2, entity.first.cellId)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    override fun update(entity: Pair<Npc, Int>) {
        throw NotImplementedException()
    }

    override fun getReferencedClass(): Class<*> {
        return NpcData::class.java
    }
}
