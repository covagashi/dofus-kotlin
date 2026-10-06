package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.area.Area
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.SQLException

class BaseAreaData(dataSource: HikariDataSource?) : FunctionDAO<Area>(dataSource, "world_base_areas") {
    override fun loadFully() {
		try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
				while (result.next()) {
        var area: Area = Area(result.getInt("id"), result.getInt("superarea"))
        World.world.addArea(area)
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
    override fun update(entity: Area) {
        throw NotImplementedException()
	}
    override fun getReferencedClass(): Class<*> {
        return BaseAreaData::class.java
	}
}