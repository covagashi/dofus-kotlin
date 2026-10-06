package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import java.sql.SQLException
import java.util.ArrayList
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(CraftData::class.java)

class CraftData(dataSource: HikariDataSource?) : FunctionDAO<Any>(dataSource, "crafts") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        var m: ArrayList<World.Couple<Int, Int>> = ArrayList()
        var cont: Boolean = true
        for (str: String in result.getString("craft").split(";")) {
        if (str.isEmpty()) continue
                        try {
        var tID: Int = Integer.parseInt(str.split("*")[0])
        var qua: Int = Integer.parseInt(str.split("*")[1])
        m.add(World.Couple(tID, qua))
        } catch (e: Exception) {
        log.error("unexpected error", e)
                cont = false
                        }
                    }
                    if (!cont) // S'il y a eu une erreur de parsing, on ignore cette recette
        continue
        World.world.addCraft(result.getInt("id"), m)
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
        return CraftData::class.java
    }
}