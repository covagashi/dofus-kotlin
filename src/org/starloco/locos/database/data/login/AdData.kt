package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.scheduler.entity.WorldPub
import org.starloco.locos.kernel.Config
import java.sql.SQLException

class AdData(dataSource: HikariDataSource?) : FunctionDAO<Any>(dataSource, "world_pubs") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + " WHERE `server` = " + Config.gameServerId) { result ->
                while (result.next()) {
        WorldPub.ads.add(result.getString("data"))
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
        return AdData::class.java
    }
}