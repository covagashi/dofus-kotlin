package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.entity.monster.boss.Bandit
import java.sql.PreparedStatement
import java.sql.SQLException

class GangsterData(dataSource: HikariDataSource?) : FunctionDAO<Bandit>(dataSource, "bandits") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        Bandit(result.getString("mobs"), result.getString("maps"), result.getLong("time"))
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): Bandit {
        throw NotImplementedException()
    }
    override fun insert(entity: Bandit): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: Bandit) {
        throw NotImplementedException()
    }
    override fun update(entity: Bandit) {
        var p: PreparedStatement? = null
        try {
        p = getPreparedStatement("UPDATE " + getTableName() + " SET `time` = ? WHERE `mobs` = '" + entity.parseMobs() + "';")
        p?.setLong(1, entity.time)
        execute(p)
        } catch (e: SQLException) {
        super.sendError(e)
        } finally {
        close(p)
        }
    }
    override fun getReferencedClass(): Class<*> {
        return GangsterData::class.java
    }
}