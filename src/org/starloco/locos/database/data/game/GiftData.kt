package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.starloco.locos.util.Pair
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.client.Account
import org.starloco.locos.database.data.FunctionDAO
import java.sql.PreparedStatement
import java.sql.SQLException

class GiftData(dataSource: HikariDataSource?) : FunctionDAO<Pair<Account?, String?>>(dataSource, "gifts") {

    override fun loadFully() {
        throw NotImplementedException()
    }

    override fun load(id: Int): Pair<Account?, String?>? {
        try {
            return Pair(null, getData<String?>("SELECT * FROM " + getTableName() + " WHERE id = '" + id + "';") { result ->
                if (result.next()) result.getString("objects") else null
            })
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return Pair(null, null)
    }

    override fun insert(entity: Pair<Account?, String?>): Boolean {
        var p: PreparedStatement? = null
        var ok = true
        try {
            p = getPreparedStatement("INSERT INTO " + getTableName() + "(`id`, `objects`) VALUES ('" + entity.first!!.id + "', '');")
            execute(p)
        } catch (e: SQLException) {
            ok = false
            super.sendError(e)
        } finally {
            close(p)
        }
        return ok
    }

    override fun delete(entity: Pair<Account?, String?>) {
        throw NotImplementedException()
    }

    override fun update(entity: Pair<Account?, String?>) {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("UPDATE " + getTableName() + " SET `objects` = ? WHERE `id` = ?;")
            p?.setString(1, entity.second)
            p?.setInt(2, entity.first!!.id)
            execute(p)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(p)
        }
    }

    override fun getReferencedClass(): Class<*> {
        return GiftData::class.java
    }
}
