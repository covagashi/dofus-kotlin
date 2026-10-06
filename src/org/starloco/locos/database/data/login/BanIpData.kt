package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import java.sql.PreparedStatement
import java.sql.SQLException

class BanIpData(dataSource: HikariDataSource?) : FunctionDAO<String>(dataSource, "administration_ban_ip") {
    override fun loadFully() {
        throw NotImplementedException()
    }
    override fun load(id: Int): String {
        throw NotImplementedException()
    }
    override fun insert(entity: String): Boolean {
        var p: PreparedStatement? = null
        var ok: Boolean = true
        try {
        p = getPreparedStatement("INSERT INTO " + getTableName() + " VALUES (?)")
        p?.setString(1, entity)
        execute(p)
        } catch (e: SQLException) {
        ok = false
        super.sendError(e)
        } finally {
        close(p)
        }
        return ok
    }
    override fun delete(ip: String) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE `ip` = ?")
        p?.setString(1, ip)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun update(entity: String) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return BanIpData::class.java
    }
}