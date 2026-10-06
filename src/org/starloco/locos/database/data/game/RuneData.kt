package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.job.maging.Rune
import java.sql.SQLException

class RuneData(dataSource: HikariDataSource?) : FunctionDAO<Rune>(dataSource, "runes") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        Rune(result.getShort("id"), result.getFloat("weight"), result.getByte("bonus"))
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): Rune {
        throw NotImplementedException()
    }
    override fun insert(entity: Rune): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: Rune) {
        throw NotImplementedException()
    }
    override fun update(entity: Rune) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return RuneData::class.java
    }
}