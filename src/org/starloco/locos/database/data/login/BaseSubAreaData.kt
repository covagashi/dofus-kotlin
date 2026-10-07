package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.area.SubArea
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.SQLException

class BaseSubAreaData(dataSource: HikariDataSource?) : FunctionDAO<SubArea>(dataSource, "world_base_sub_areas") {
    override fun loadFully() {
		try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
				while (result.next()) {
        var subArea: SubArea = SubArea(result.getInt("id"), result.getString("name") ?: "", result.getInt("area"), result.getString("nearest_sub_areas"))
        World.world.addSubArea(subArea)
					if (subArea.area != null)
        subArea.area.addSubArea(subArea)
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
    override fun update(entity: SubArea) {
        throw NotImplementedException()
	}
    override fun getReferencedClass(): Class<*> {
        return BaseSubAreaData::class.java
	}
}