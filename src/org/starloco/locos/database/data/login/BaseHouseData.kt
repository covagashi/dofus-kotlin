package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.entity.map.House
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.SQLException

class BaseHouseData(dataSource: HikariDataSource?) : FunctionDAO<House>(dataSource, "world_base_houses") {
    override fun loadFully() {
		try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
				while (result.next()) {
        World.world.addHouse(House(result.getInt("id"), result.getInt("map_id"), result.getInt("cell_id"), result.getInt("mapid"), result.getInt("caseid")))
				/* Set base price to all houses
        var saleBase: Long = RS.getLong("saleBase")
				DatabaseManager.getDynamics().getHouseData().update(RS.getInt("id"), saleBase);*/
				}
        }
        } catch (e: SQLException) {
        super.sendError(e)
		}
	}
    override fun load(id: Int): House {
        throw NotImplementedException()
	}
    override fun insert(entity: House): Boolean {
        throw NotImplementedException()
	}
    override fun delete(entity: House) {
        throw NotImplementedException()
	}
    override fun update(entity: House) {
        throw NotImplementedException()
	}
    override fun getReferencedClass(): Class<*> {
        return BaseHouseData::class.java
	}
}