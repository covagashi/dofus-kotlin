package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.area.Area
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.PreparedStatement
import java.sql.SQLException

class AreaData(dataSource: HikariDataSource?) : FunctionDAO<Area>(dataSource, "area_data") {
    override fun loadFully() {
		try	{
        getData("SELECT * FROM " + getTableName() + ";") { result ->
				while (result.next()) {
        var id: Int = result.getInt("id")
        val area: Area? = World.world.getArea(id)
					if (area != null) {
        area.alignement = result.getInt("alignement")
        area.prismId = result.getInt("Prisme")
					}
				}
        }
        } catch (e: SQLException) {
        super.sendError(e)
		}
	}
    override fun load(id: Int): Area {
        throw NotImplementedException()
	}
    override fun insert(entity: Area): Boolean {
        throw NotImplementedException()
	}
    override fun delete(entity: Area) {
        throw NotImplementedException()
	}
    override fun update(area: Area) {
        var p: PreparedStatement? = null
		try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `alignement` = ?, `Prisme` = ? WHERE id = ?")
        p?.setInt(1, area.alignement)
        p?.setInt(2, area.prismId)
        p?.setInt(3, area.id)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
		} finally	{
        close(p)
		}
	}
    override fun getReferencedClass(): Class<*> {
        return AreaData::class.java
	}
}