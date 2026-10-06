package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.area.SubArea
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException

class SubAreaData(dataSource: HikariDataSource?) : FunctionDAO<SubArea>(dataSource, "subarea_data") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        var id: Int = result.getInt("id")
        val subArea: SubArea? = World.world.getSubArea(id)
                    if (subArea != null) {
        subArea.alignment = result.getByte("alignement").toInt()
        subArea.prism = World.world.getPrisme(result.getInt("Prisme"))
        subArea.conquerable = result.getInt("conquistable") == 0
                    }
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): SubArea {
        throw NotImplementedException()
    }
    override fun insert(entity: SubArea): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: SubArea) {
        throw NotImplementedException()
    }
    override fun update(subarea: SubArea) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `alignement` = ?, `prisme` = ?, `conquistable` = ? WHERE `id` = ?")
        p?.setInt(1, subarea.alignment)
        p?.setInt(2, subarea.prism?.id ?: 0)
        p?.setInt(3, if (subarea.conquerable) 0 else 1)
        p?.setInt(4, subarea.id)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return SubAreaData::class.java
    }
}