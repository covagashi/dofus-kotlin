package org.starloco.locos.database.data.game

import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.NotImplementedException
import org.starloco.locos.database.data.FunctionDAO
import org.starloco.locos.game.world.World
import org.starloco.locos.kernel.Main
import org.starloco.locos.`object`.ObjectAction
import org.starloco.locos.`object`.ObjectTemplate
import java.sql.SQLException

class ObjectActionData(dataSource: HikariDataSource?) : FunctionDAO<ObjectAction>(dataSource, "objectsactions") {
    override fun loadFully() {
        try {
        World.world.objectsTemplates.values.forEach { template -> template.getOnUseActions().clear() }
        getData("SELECT * FROM " + getTableName() + ";") { result ->
                while (result.next()) {
        var id: Int = result.getInt("template")
        var template: ObjectTemplate? = World.world.getObjTemplate(id)
                    if (template != null)
        template.addAction(ObjectAction(result.getString("type") ?: "", result.getString("args") ?: "", ""))
                }
        }
        } catch (e: SQLException) {
        super.sendError(e)
        Main.stop("Can't load objects actions")
        }
    }
    override fun load(id: Int): ObjectAction {
        throw NotImplementedException()
    }
    override fun insert(entity: ObjectAction): Boolean {
        throw NotImplementedException()
    }
    override fun delete(entity: ObjectAction) {
        throw NotImplementedException()
    }
    override fun update(entity: ObjectAction) {
        throw NotImplementedException()
    }
    override fun getReferencedClass(): Class<*> {
        return ObjectActionData::class.java
    }
}