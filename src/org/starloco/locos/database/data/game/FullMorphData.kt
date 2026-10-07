package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.SQLException
import org.starloco.locos.common.splitJ

class FullMorphData(dataSource: HikariDataSource?) : FunctionDAO<Any>(dataSource, "full_morphs") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        var args: Array<String>? = null
                    if ((result.getString("args") ?: "") != "0") {
        args = (result.getString("args") ?: "").split("@")[1].splitJ(",").toTypedArray()
                    }
        World.world.addFullMorph(result.getInt("id"), result.getString("name") ?: "", result.getInt("gfxId"), result.getString("spells"), args)
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
        return FullMorphData::class.java
    }
}