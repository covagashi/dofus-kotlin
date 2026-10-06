package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.DatabaseManager
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.kernel.Config
import java.sql.PreparedStatement
import java.sql.SQLException

class ServerData(dataSource: HikariDataSource?) : FunctionDAO<Long>(dataSource, "world_servers") {
    override fun loadFully() {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + DatabaseManager.get(PlayerData::class.java).getTableName() + " SET `logged` = 0 WHERE `server` = '" + Config.gameServerId + "';")
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun load(id: Int): Long {
        throw NotImplementedException()
    }
    override fun insert(entity: Long): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: Long) {
        throw NotImplementedException()
    }
    override fun update(entity: Long) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `uptime` = ? WHERE `id` = ?;")
        p?.setLong(1, entity)
        p?.setInt(2, Config.gameServerId)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return ServerData::class.java
    }
}