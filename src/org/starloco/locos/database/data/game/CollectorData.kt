package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.area.map.GameMap
import org.starloco.locos.client.Player
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.entity.Collector
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class CollectorData(dataSource: HikariDataSource?) : FunctionDAO<Collector>(dataSource, "percepteurs") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        var map: GameMap? = World.world.getMap(result.getShort("mapid").toInt())
                    if (map != null) {
        var poseur_id: Int = result.getInt("poseur_id")
        var player: Player? = World.world.getPlayer(poseur_id)
        var date: String = result.getString("date")
        var time: Long = 0
                        if (date != null && date != "") {
        time = date.toLong()
                        }
        World.world.addCollector(Collector(result.getInt("guid"), result.getShort("mapid").toInt(), result.getInt("cellid"), result.getByte("orientation"), result.getInt("guild_id"), result.getShort("N1"), result.getShort("N2"), player, time, result.getString("objets"), result.getLong("kamas"), result.getLong("xp")))
                    }
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): Collector? {
        throw NotImplementedException()
    }
    override fun insert(entity: Collector): Boolean {
        var p: PreparedStatement? = null
        var ok: Boolean = true
        try {
        p = getPreparedStatement("INSERT INTO " + getTableName() + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")
        entity.id = this.getId()
        p?.setInt(1, entity.id)
        p?.setInt(2, entity.map)
        p?.setInt(3, entity.cell)
        p?.setInt(4, 3)
        p?.setInt(5, entity.guildId)
        p?.setInt(6, entity.poseur!!.id)
        p?.setString(7, entity.date.toString())
        p?.setInt(8, entity.n1.toInt())
        p?.setInt(9, entity.n2.toInt())
        p?.setString(10, "")
        p?.setLong(11, 0)
        p?.setLong(12, 0)
        var affectedRows: Int = p?.executeUpdate() ?: 0
            if (affectedRows == 0) {
        ok = false
            } else {
        p?.getGeneratedKeys()?.use { generatedKeys ->
                    if (generatedKeys.next()) {
        entity.id = generatedKeys.getInt(1)
                    } else {
        ok = false
                    }
                }
            }
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
        return ok
    }
    override fun delete(entity: Collector) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE guid = ?")
        p?.setInt(1, entity.id)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun update(entity: Collector) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `objets` = ?,`kamas` = ?,`xp` = ? WHERE guid = ?")
        p?.setString(1, entity.parseItemCollector())
        p?.setLong(2, entity.kamas)
        p?.setLong(3, entity.xp)
        p?.setInt(4, entity.id)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return CollectorData::class.java
    }
    private fun getId(): Int {
        var id = -100 //Pour éviter les conflits avec touts autre NPC
        try {
            id = getData<Int>("SELECT `guid` FROM `percepteurs` ORDER BY `guid` ASC LIMIT 0 , 1") { result ->
                var i = -100
                while (result.next()) {
                    i = result.getInt("guid") - 1
                }
                i
            } ?: -100
        } catch (e: SQLException) {
            super.sendError(e)
        }
        if (id >= -9999)
            id = -10000
        return id
    }
}
