package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import org.starloco.locos.`object`.ObjectSet
import java.sql.SQLException

class ObjectSetData(dataSource: HikariDataSource?) : FunctionDAO<ObjectSet>(dataSource, "itemsets") {
    override fun loadFully() {
        try {
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        World.world.addItemSet(ObjectSet(result.getInt("id"), result.getString("items"), result.getString("bonus")))
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        }
    }
    override fun load(id: Int): ObjectSet {
        throw NotImplementedException()
    }
    override fun insert(entity: ObjectSet): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: ObjectSet) {
        throw NotImplementedException()
    }
    override fun update(entity: ObjectSet) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return ObjectSetData::class.java
    }
}