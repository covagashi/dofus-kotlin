package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import org.starloco.locos.hdv.BigStore
import java.sql.SQLException

class HdvData(dataSource: HikariDataSource?) : FunctionDAO<Any>(dataSource, "hdvs") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + " ORDER BY id ASC;") { result ->
                while (result.next()) {
        World.world.addHdv(BigStore(result.getInt("map"), result.getFloat("sellTaxe"), result.getShort("sellTime"), result.getShort("accountItem"), result.getShort("lvlMax"), result.getString("categories")))
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): Any {
        throw NotImplementedException()
    }
    override fun insert(entity: Any): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: Any) {
        throw NotImplementedException()
    }
    override fun update(entity: Any) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return HdvData::class.java
    }
}