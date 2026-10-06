package org.starloco.locos.database.data.login

import com.zaxxer.hikari.HikariDataSource
import org.starloco.locos.util.Pair
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import java.sql.PreparedStatement
import java.sql.SQLException

class ObvijevanData(dataSource: HikariDataSource?) : FunctionDAO<Pair<Int, Int>>(dataSource, "world_obvijevans") {

    override fun loadFully() {
        throw NotImplementedException()
    }

    override fun load(id: Int): Pair<Int, Int> {
        var template = -1
        try {
            template = getData<Int>("SELECT * FROM " + getTableName() + " WHERE `id` = '" + id + "';") { result ->
                if (result.next()) result.getInt("template") else -1
            } ?: -1
        } catch (e: SQLException) {
            super.sendError(e)
        }
        return Pair(template, template)
    }

    /**
     * @param entity <objectId, obvijevanTemplateId>
     */
    override fun insert(entity: Pair<Int, Int>): Boolean {
        var p: PreparedStatement? = null
        try {
            p = getPreparedStatement("INSERT INTO " + getTableName() + "(`id`, `template`) VALUES(?, ?);")
            p?.setInt(1, entity.getFirst())
            p?.setInt(2, entity.getSecond())
            execute(p)
        } catch (e: Exception) {
            super.sendError(e)
        } finally {
            close(p)
        }
        return true
    }

    override fun delete(entity: Pair<Int, Int>) {
        var ps: PreparedStatement? = null
        try {
            ps = getPreparedStatement("DELETE FROM " + getTableName() + " WHERE id = '" + entity.getFirst() + "';")
            execute(ps)
        } catch (e: SQLException) {
            super.sendError(e)
        } finally {
            close(ps)
        }
    }

    override fun update(entity: Pair<Int, Int>) {
        throw NotImplementedException()
    }

    override fun getReferencedClass(): Class<*> {
        return ObvijevanData::class.java
    }
}
