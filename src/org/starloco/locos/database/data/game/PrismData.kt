package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.entity.Prism
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException

class PrismData(dataSource: HikariDataSource?) : FunctionDAO<Prism>(dataSource, "prismes") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
                    World.world.addPrisme(Prism(result.getInt("id"), result.getByte("alignement"), result.getInt("level"),
        result.getShort("carte").toInt(), result.getInt("celda"), result.getInt("honor"), result.getInt("area")))
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): Prism {
        throw NotImplementedException()
    }
    override fun insert(entity: Prism): Boolean {
        var p: PreparedStatement? = null
        var ok: Boolean = true
        try {
        p = getPreparedStatement("REPLACE INTO " + getTableName() + " VALUES(?,?,?,?,?,?,?);")
        p?.setInt(1, entity.id)
        p?.setInt(2, entity.alignment)
        p?.setInt(3, entity.level)
        p?.setInt(4, entity.map)
        p?.setInt(5, entity.cell)
        p?.setInt(6, entity.conquestArea)
        p?.setInt(7, entity.honor)
        execute(p)
        } catch (e: SQLException) {
        ok = false
        super.sendError(e)
        } finally {
        close(p)
        }
        return ok
    }
    override fun delete(entity: Prism) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE id = ?;")
        p?.setInt(1, entity.id)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun update(entity: Prism) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `level` = ?, `honor` = ?, `area`= ? WHERE `id` = ?;")
        p?.setInt(1, entity.level)
        p?.setInt(2, entity.honor)
        p?.setInt(3, entity.conquestArea)
        p?.setInt(4, entity.id)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return PrismData::class.java
    }
}